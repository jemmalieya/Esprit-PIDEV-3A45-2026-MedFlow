package tn.esprit.services;

import tn.esprit.entities.Evenement;
import tn.esprit.tools.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParticipationDemandeService {
    private final Connection cnx;

    public ParticipationDemandeService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    public List<ParticipationDemande> getDemandes(Evenement evenement) {
        return parseDemandes(evenement == null ? null : evenement.getDemandes_json());
    }

    public boolean userHasDemande(Evenement evenement, int userId) {
        if (userId <= 0) return false;
        return getDemandes(evenement).stream().anyMatch(d -> d.getUserId() == userId);
    }

    public ParticipationDemande ajouterDemande(Evenement evenement, ParticipationDemande demande) throws SQLException {
        List<ParticipationDemande> demandes = getDemandes(evenement);
        if (demande != null && demande.getUserId() > 0) {
            for (ParticipationDemande existing : demandes) {
                if (existing.getUserId() == demande.getUserId()) {
                    throw new SQLException("Vous avez deja envoye une demande pour cet evenement.");
                }
            }
        }
        demande.setId(UUID.randomUUID().toString());
        demande.setStatus(hasRemainingCapacity(evenement, demandes)
                ? ParticipationDemande.STATUS_ACCEPTED
                : ParticipationDemande.STATUS_WAITING);
        demande.markCreatedNow();
        if (ParticipationDemande.STATUS_ACCEPTED.equals(demande.getStatus()) && safe(demande.getTicketCode()).isBlank()) {
            demande.setTicketCode(generateTicketCode(evenement));
            demande.markDecidedNow();
        }
        demandes.add(demande);
        persist(evenement, demandes);
        return demande;
    }

    public ParticipationDemande changerStatut(
            Evenement evenement,
            String demandeId,
            String status,
            String adminNote
    ) throws SQLException {
        List<ParticipationDemande> demandes = getDemandes(evenement);
        ParticipationDemande selected = null;

        for (ParticipationDemande demande : demandes) {
            if (safe(demande.getId()).equals(demandeId)) {
                String currentStatus = safe(demande.getStatus());
                if (ParticipationDemande.STATUS_ACCEPTED.equals(status)
                        && !ParticipationDemande.STATUS_ACCEPTED.equals(currentStatus)
                        && !hasRemainingCapacity(evenement, demandes)) {
                    throw new SQLException("Capacite maximale atteinte. Placez ce participant en liste d'attente ou liberez une place.");
                }
                demande.setStatus(status);
                demande.setAdminNote(adminNote);
                demande.markDecidedNow();
                if (ParticipationDemande.STATUS_ACCEPTED.equals(status) && safe(demande.getTicketCode()).isBlank()) {
                    demande.setTicketCode(generateTicketCode(evenement));
                }
                selected = demande;
                break;
            }
        }

        if (selected == null) {
            throw new SQLException("Demande introuvable.");
        }

        if (!ParticipationDemande.STATUS_ACCEPTED.equals(selected.getStatus())) {
            promoteFirstWaiting(evenement, demandes);
        }

        persist(evenement, demandes);
        return selected;
    }

    public long countPending(Evenement evenement) {
        return getDemandes(evenement).stream()
                .filter(d -> ParticipationDemande.STATUS_PENDING.equals(safe(d.getStatus()))
                        || ParticipationDemande.STATUS_WAITING.equals(safe(d.getStatus())))
                .count();
    }

    public long countPending(List<Evenement> evenements) {
        if (evenements == null) return 0;
        return evenements.stream().mapToLong(this::countPending).sum();
    }

    public String normalizeTunisianPhone(String phone) {
        String value = safe(phone).trim().replace(" ", "").replace("-", "");
        if (value.isBlank()) return "";
        if (value.startsWith("+")) return value;
        if (value.startsWith("216")) return "+" + value;
        return "+216" + value;
    }

    public String buildDecisionSms(Evenement evenement, ParticipationDemande demande) {
        String title = safe(evenement.getTitre_event()).isBlank() ? "votre evenement" : evenement.getTitre_event();
        String dates = evenement.getDate_debut_event() == null ? "" : " Date: " + evenement.getDate_debut_event();

        if (ParticipationDemande.STATUS_ACCEPTED.equals(demande.getStatus())) {
            String ticket = safe(demande.getTicketCode()).isBlank() ? "" : " Code: " + demande.getTicketCode() + ".";
            return "MedFlow: Bonjour " + demande.getDisplayName()
                    + ", votre demande de participation a " + title + " est acceptee." + ticket + dates;
        }

        if (ParticipationDemande.STATUS_WAITING.equals(demande.getStatus())) {
            return "MedFlow: Bonjour " + demande.getDisplayName()
                    + ", vous etes actuellement sur liste d attente pour " + title + ".";
        }

        String note = safe(demande.getAdminNote()).isBlank() ? "" : " Motif: " + demande.getAdminNote();
        return "MedFlow: Bonjour " + demande.getDisplayName()
                + ", votre demande de participation a " + title + " est refusee." + note;
    }

    private boolean hasRemainingCapacity(Evenement evenement, List<ParticipationDemande> demandes) {
        if (evenement == null) return true;
        int capacity = evenement.getNb_participants_max_event();
        if (capacity <= 0) return true;
        long accepted = demandes.stream()
                .filter(d -> ParticipationDemande.STATUS_ACCEPTED.equals(safe(d.getStatus())))
                .count();
        return accepted < capacity;
    }

    private void promoteFirstWaiting(Evenement evenement, List<ParticipationDemande> demandes) {
        if (!hasRemainingCapacity(evenement, demandes)) {
            return;
        }
        for (ParticipationDemande demande : demandes) {
            String status = safe(demande.getStatus());
            if (ParticipationDemande.STATUS_WAITING.equals(status) || ParticipationDemande.STATUS_PENDING.equals(status)) {
                demande.setStatus(ParticipationDemande.STATUS_ACCEPTED);
                if (safe(demande.getTicketCode()).isBlank()) {
                    demande.setTicketCode(generateTicketCode(evenement));
                }
                demande.setAdminNote("Promotion automatique depuis la liste d'attente.");
                demande.markDecidedNow();
                return;
            }
        }
    }

    private void persist(Evenement evenement, List<ParticipationDemande> demandes) throws SQLException {
        String json = toJson(demandes);
        String sql = "UPDATE evenement SET demandes_json = ?, date_mise_a_jour_event = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, json);
            ps.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
            ps.setInt(3, evenement.getId());
            ps.executeUpdate();
        }
        evenement.setDemandes_json(json);
        evenement.setDate_mise_a_jour_event(java.sql.Date.valueOf(LocalDate.now()));
    }

    private String generateTicketCode(Evenement evenement) {
        return ("MF-" + evenement.getId() + "-" + UUID.randomUUID().toString().substring(0, 6))
                .toUpperCase(Locale.ROOT);
    }

    private List<ParticipationDemande> parseDemandes(String json) {
        List<ParticipationDemande> demandes = new ArrayList<>();
        String value = safe(json).trim();
        if (value.isBlank() || value.equals("[]")) return demandes;

        Matcher objectMatcher = Pattern.compile("\\{([^}]*)}").matcher(value);
        while (objectMatcher.find()) {
            Map<String, String> map = new LinkedHashMap<>();
            Matcher fieldMatcher = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")
                    .matcher(objectMatcher.group(1));
            while (fieldMatcher.find()) {
                map.put(fieldMatcher.group(1), jsonUnescape(fieldMatcher.group(2)));
            }
            if (!map.isEmpty()) {
                demandes.add(ParticipationDemande.fromMap(map));
            }
        }

        return demandes;
    }

    private String toJson(List<ParticipationDemande> demandes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < demandes.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('{');
            int fieldIndex = 0;
            for (Map.Entry<String, String> entry : demandes.get(i).toMap().entrySet()) {
                if (fieldIndex++ > 0) sb.append(',');
                sb.append('"').append(jsonEscape(entry.getKey())).append("\":\"")
                        .append(jsonEscape(entry.getValue())).append('"');
            }
            sb.append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String jsonUnescape(String value) {
        if (value == null) return "";
        return value.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class ParticipationDemande {
        public static final String STATUS_PENDING = "pending";
        public static final String STATUS_WAITING = "waiting";
        public static final String STATUS_ACCEPTED = "accepted";
        public static final String STATUS_REFUSED = "refused";

        private String id;
        private int userId;
        private String nom;
        private String prenom;
        private String email;
        private String telephone;
        private String motif;
        private String status = STATUS_PENDING;
        private String adminNote;
        private String createdAt;
        private String decidedAt;
        private String ticketCode;

        public static ParticipationDemande fromMap(Map<String, String> data) {
            ParticipationDemande demande = new ParticipationDemande();
            demande.setId(data.get("id"));
            demande.setUserId(parseInt(data.get("user_id")));
            demande.setNom(data.get("nom"));
            demande.setPrenom(data.get("prenom"));
            demande.setEmail(data.get("email"));
            demande.setTelephone(data.get("telephone"));
            demande.setMotif(data.get("motif"));
            demande.setStatus(blankToDefault(data.get("status"), STATUS_PENDING));
            demande.setAdminNote(data.get("admin_note"));
            demande.setCreatedAt(data.get("created_at"));
            demande.setDecidedAt(data.get("decided_at"));
            demande.setTicketCode(data.get("ticket_code"));
            return demande;
        }

        public Map<String, String> toMap() {
            Map<String, String> data = new LinkedHashMap<>();
            data.put("id", nullToEmpty(id));
            data.put("user_id", String.valueOf(userId));
            data.put("nom", nullToEmpty(nom));
            data.put("prenom", nullToEmpty(prenom));
            data.put("email", nullToEmpty(email));
            data.put("telephone", nullToEmpty(telephone));
            data.put("motif", nullToEmpty(motif));
            data.put("status", nullToEmpty(status));
            data.put("admin_note", nullToEmpty(adminNote));
            data.put("created_at", nullToEmpty(createdAt));
            data.put("decided_at", nullToEmpty(decidedAt));
            data.put("ticket_code", nullToEmpty(ticketCode));
            return data;
        }

        public String getDisplayName() {
            String fullName = (nullToEmpty(prenom) + " " + nullToEmpty(nom)).trim();
            return fullName.isBlank() ? "Participant" : fullName;
        }

        public String getStatusLabel() {
            return switch (nullToEmpty(status)) {
                case STATUS_ACCEPTED -> "Acceptée";
                case STATUS_REFUSED -> "Refusée";
                default -> "En attente";
            };
        }

        public void markCreatedNow() {
            this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        public void markDecidedNow() {
            this.decidedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        private static int parseInt(String value) {
            try {
                return value == null || value.isBlank() ? 0 : Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private static String blankToDefault(String value, String defaultValue) {
            return value == null || value.isBlank() ? defaultValue : value;
        }

        private static String nullToEmpty(String value) {
            return value == null ? "" : value;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }
        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getTelephone() { return telephone; }
        public void setTelephone(String telephone) { this.telephone = telephone; }
        public String getMotif() { return motif; }
        public void setMotif(String motif) { this.motif = motif; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getAdminNote() { return adminNote; }
        public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getDecidedAt() { return decidedAt; }
        public void setDecidedAt(String decidedAt) { this.decidedAt = decidedAt; }
        public String getTicketCode() { return ticketCode; }
        public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }
    }
}
