package tn.esprit.services;

import tn.esprit.entities.Evenement;
import tn.esprit.entities.User;
import tn.esprit.services.ParticipationDemandeService.ParticipationDemande;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AiEventIntelligenceService {

    public RiskReport analyzeRisk(Evenement event, List<ParticipationDemande> demandes, List<Evenement> allEvents) {
        int score = 8;
        List<String> reasons = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (event == null) {
            return new RiskReport(0, "Indisponible", List.of("Evenement introuvable."), List.of());
        }

        String status = normalize(event.getStatut_event());
        String visibility = normalize(event.getVisibilite_event());
        LocalDate start = toLocalDate(event.getDate_debut_event());
        LocalDate end = toLocalDate(event.getDate_fin_event());
        LocalDate limit = toLocalDate(event.getDate_limite_inscription_event());
        LocalDate today = LocalDate.now();

        if (status.contains("annul") || status.contains("cancel")) {
            score += 55;
            reasons.add("Evenement annule: risque maximum pour l'experience participant.");
            suggestions.add("Afficher clairement le motif, proposer une compensation et des alternatives proches.");
        } else if (status.contains("brouillon") || status.contains("draft")) {
            score += 18;
            reasons.add("Evenement encore en brouillon: visibilite et inscriptions incertaines.");
            suggestions.add("Finaliser le contenu, les dates, le lieu et publier uniquement apres verification.");
        } else if (!status.contains("publi")) {
            score += 12;
            reasons.add("Statut non publie ou ambigu.");
            suggestions.add("Clarifier le statut avant de le recommander aux utilisateurs.");
        }

        if (!visibility.contains("public")) {
            score += 12;
            reasons.add("Visibilite non publique: l'audience front peut etre limitee.");
            suggestions.add("Basculer en public si l'evenement doit recevoir des demandes.");
        }

        if (start == null || end == null) {
            score += 22;
            reasons.add("Dates incompletes.");
            suggestions.add("Renseigner une date de debut et une date de fin fiables.");
        } else {
            if (end.isBefore(start)) {
                score += 35;
                reasons.add("Date de fin avant la date de debut.");
                suggestions.add("Corriger la plage de dates avant publication.");
            }
            if (end.isBefore(today)) {
                score += 20;
                reasons.add("Evenement deja passe.");
                suggestions.add("Archiver l'evenement ou le dupliquer avec de nouvelles dates.");
            } else {
                long daysLeft = ChronoUnit.DAYS.between(today, start);
                if (daysLeft <= 2) {
                    score += 14;
                    reasons.add("Evenement tres proche: peu de temps pour traiter les demandes.");
                    suggestions.add("Prioriser la validation des participants et la communication logistique.");
                } else if (daysLeft <= 7) {
                    score += 7;
                    reasons.add("Evenement proche: preparation operationnelle a surveiller.");
                }
            }
        }

        if (limit != null && limit.isBefore(today) && start != null && !start.isBefore(today)) {
            score += 9;
            reasons.add("Date limite d'inscription depassee alors que l'evenement arrive bientot.");
            suggestions.add("Fermer clairement les inscriptions ou prolonger la date limite.");
        }

        int pending = countByStatus(demandes, ParticipationDemande.STATUS_PENDING);
        int accepted = countByStatus(demandes, ParticipationDemande.STATUS_ACCEPTED);
        int refused = countByStatus(demandes, ParticipationDemande.STATUS_REFUSED);
        int capacity = Math.max(0, event.getNb_participants_max_event());

        if (pending > 0) {
            score += Math.min(20, pending * 4);
            reasons.add(pending + " demande(s) en attente.");
            suggestions.add("Traiter les demandes en attente pour eviter les retards de confirmation.");
        }
        if (capacity > 0 && accepted > capacity) {
            score += 28;
            reasons.add("Participants acceptes superieurs a la capacite declaree.");
            suggestions.add("Verifier la capacite du lieu ou limiter les nouvelles validations.");
        } else if (capacity > 0 && accepted >= Math.ceil(capacity * 0.90)) {
            score += 9;
            reasons.add("Capacite presque atteinte.");
            suggestions.add("Preparer une liste d'attente ou augmenter la capacite si possible.");
        }
        if (refused > accepted && refused >= 3) {
            score += 8;
            reasons.add("Refus nombreux par rapport aux acceptations.");
            suggestions.add("Analyser les motifs de refus pour ameliorer les criteres d'inscription.");
        }

        long sameDay = countSameDayEvents(event, allEvents);
        if (sameDay > 0) {
            score += (int) Math.min(15, sameDay * 5);
            reasons.add(sameDay + " autre(s) evenement(s) le meme jour.");
            suggestions.add("Verifier les conflits de planning et la disponibilite du staff.");
        }

        if (safe(event.getVille_event()).isBlank() || safe(event.getAdresse_event()).isBlank()) {
            score += 10;
            reasons.add("Lieu incomplet.");
            suggestions.add("Completer la ville et l'adresse pour ameliorer carte, meteo et recommandations.");
        }

        if (reasons.isEmpty()) {
            reasons.add("Aucun signal critique detecte sur les donnees disponibles.");
            suggestions.add("Maintenir le suivi des demandes et verifier la logistique avant l'evenement.");
        }

        int bounded = Math.max(0, Math.min(100, score));
        return new RiskReport(bounded, riskLevel(bounded), reasons, distinctLimit(suggestions, 5));
    }

    public List<Recommendation> recommendForEvent(Evenement current, User user, List<Evenement> allEvents, int limit) {
        List<Recommendation> recommendations = new ArrayList<>();
        if (current == null || allEvents == null) {
            return recommendations;
        }

        for (Evenement candidate : allEvents) {
            if (candidate == null || candidate.getId() == current.getId() || !isRecommendable(candidate)) {
                continue;
            }

            double score = 1.0;
            List<String> reasons = new ArrayList<>();

            if (same(current.getType_event(), candidate.getType_event())) {
                score += 3.2;
                reasons.add("Meme type d'evenement.");
            }
            if (same(current.getVille_event(), candidate.getVille_event())) {
                score += 2.4;
                reasons.add("Meme ville, donc deplacement plus simple.");
            }

            LocalDate currentDate = toLocalDate(current.getDate_debut_event());
            LocalDate candidateDate = toLocalDate(candidate.getDate_debut_event());
            if (candidateDate != null && !candidateDate.isBefore(LocalDate.now())) {
                score += 1.3;
                reasons.add("Date encore disponible.");
            }
            if (currentDate != null && candidateDate != null) {
                long gap = Math.abs(ChronoUnit.DAYS.between(currentDate, candidateDate));
                if (gap <= 14) {
                    score += 1.2;
                    reasons.add("Planning proche de l'evenement consulte.");
                } else if (gap <= 45) {
                    score += 0.6;
                }
            }

            if (user != null && same(user.getPrenom(), candidate.getNom_organisateur_event())) {
                score += 0.4;
                reasons.add("Signal faible de profil utilisateur compatible.");
            }

            int accepted = countByStatus(new ParticipationDemandeService().getDemandes(candidate), ParticipationDemande.STATUS_ACCEPTED);
            if (accepted > 0) {
                score += Math.min(1.0, accepted / 8.0);
                reasons.add("Evenement deja attractif pour les participants.");
            }

            if (reasons.isEmpty()) {
                reasons.add("Alternative publique disponible.");
            }

            recommendations.add(new Recommendation(candidate, Math.min(10.0, score), distinctLimit(reasons, 3)));
        }

        return recommendations.stream()
                .sorted(Comparator.comparingDouble(Recommendation::score).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    public String buildRecommendationSummary(Evenement current, List<Recommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "Aucune recommandation fiable trouvee pour cet evenement avec les donnees actuelles.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Analyse IA locale pour \"").append(safe(current == null ? "" : current.getTitre_event())).append("\".\n\n");
        int index = 1;
        for (Recommendation rec : recommendations) {
            Evenement event = rec.event();
            builder.append(index++)
                    .append(". ")
                    .append(safe(event.getTitre_event()))
                    .append(" - score ")
                    .append(String.format(Locale.US, "%.1f/10", rec.score()))
                    .append("\n   ")
                    .append(String.join(" ", rec.reasons()))
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private boolean isRecommendable(Evenement event) {
        String status = normalize(event.getStatut_event());
        String visibility = normalize(event.getVisibilite_event());
        LocalDate date = toLocalDate(event.getDate_debut_event());
        return visibility.contains("public")
                && status.contains("publi")
                && (date == null || !date.isBefore(LocalDate.now()));
    }

    private long countSameDayEvents(Evenement event, List<Evenement> allEvents) {
        LocalDate date = toLocalDate(event.getDate_debut_event());
        if (date == null || allEvents == null) {
            return 0;
        }
        return allEvents.stream()
                .filter(other -> other != null && other.getId() != event.getId())
                .filter(other -> date.equals(toLocalDate(other.getDate_debut_event())))
                .count();
    }

    private int countByStatus(List<ParticipationDemande> demandes, String status) {
        if (demandes == null) {
            return 0;
        }
        int count = 0;
        for (ParticipationDemande demande : demandes) {
            if (demande != null && status.equals(demande.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private String riskLevel(int score) {
        if (score >= 70) return "Eleve";
        if (score >= 40) return "Modere";
        return "Faible";
    }

    private List<String> distinctLimit(List<String> values, int limit) {
        List<String> out = new ArrayList<>();
        if (values == null) {
            return out;
        }
        for (String value : values) {
            String clean = safe(value);
            if (!clean.isBlank() && !out.contains(clean)) {
                out.add(clean);
            }
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    private boolean same(String a, String b) {
        String left = normalize(a);
        String right = normalize(b);
        return !left.isBlank() && left.equals(right);
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String normalize(String value) {
        String clean = safe(value).toLowerCase(Locale.ROOT);
        return Normalizer.normalize(clean, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record RiskReport(int score, String level, List<String> reasons, List<String> suggestions) {
    }

    public record Recommendation(Evenement event, double score, List<String> reasons) {
    }
}
