package tn.esprit.services;

import tn.esprit.entities.Evenement;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DashboardBIServiceEvenement {

    private final EvenementService evenementService = new EvenementService();

    public DashboardData chargerDonnees(LocalDate debut, LocalDate fin, String ville) throws SQLException {
        List<Evenement> events = evenementService.recuperer();

        if (debut == null) debut = LocalDate.now().minusDays(6);
        if (fin == null) fin = LocalDate.now();
        if (ville == null || ville.isBlank()) ville = "Toutes les villes";

        List<Evenement> filtres = new ArrayList<>();

        for (Evenement e : events) {
            if (e.getDate_debut_event() == null) continue;

            LocalDate d = ((java.sql.Date) e.getDate_debut_event()).toLocalDate();

            if (d.isBefore(debut) || d.isAfter(fin)) continue;

            String villeEvent = safe(e.getVille_event());
            if (!ville.equalsIgnoreCase("Toutes les villes")
                    && !villeEvent.equalsIgnoreCase(ville)) {
                continue;
            }

            filtres.add(e);
        }

        DashboardData data = new DashboardData();
        data.dateDebut = debut;
        data.dateFin = fin;
        data.ville = ville;

        calculerKpis(data, filtres);
        calculerSeries(data, filtres, debut, fin);
        calculerStatuts(data, filtres);
        calculerParType(data, filtres);
        calculerParVille(data, filtres);
        calculerTopEvenements(data, filtres);
        calculerRecommandations(data);

        return data;
    }

    private void calculerKpis(DashboardData data, List<Evenement> events) {
        int total = events.size();
        int publies = 0;
        int brouillons = 0;
        int annules = 0;
        int autres = 0;
        int aVenir = 0;
        int capaciteTotale = 0;

        LocalDate today = LocalDate.now();

        for (Evenement e : events) {
            String statut = safe(e.getStatut_event()).toLowerCase();

            if (statut.contains("publi") || statut.contains("ligne")) publies++;
            else if (statut.contains("brouillon")) brouillons++;
            else if (statut.contains("annul")) annules++;
            else autres++;

            if (e.getDate_debut_event() != null) {
                LocalDate d = ((java.sql.Date) e.getDate_debut_event()).toLocalDate();
                if (!d.isBefore(today)) {
                    aVenir++;
                }
            }

            capaciteTotale += Math.max(0, e.getNb_participants_max_event());
        }

        double tauxPublication = total > 0 ? (publies * 100.0 / total) : 0.0;

        data.totalEvenements = total;
        data.publies = publies;
        data.brouillons = brouillons;
        data.annules = annules;
        data.autres = autres;
        data.evenementsAVenir = aVenir;
        data.capaciteTotale = capaciteTotale;
        data.tauxPublication = tauxPublication;
        data.periodeAnalysee = (int) (ChronoUnit.DAYS.between(data.dateDebut, data.dateFin) + 1);

        data.publicationMessage = "Taux de publication — " + String.format(Locale.US, "%.1f%%", tauxPublication);
        data.capaciteMessage = "Capacité totale — " + capaciteTotale + " places";
        data.resume = "Analyse des événements entre " + data.dateDebut + " et " + data.dateFin;
    }

    private void calculerSeries(DashboardData data, List<Evenement> events, LocalDate debut, LocalDate fin) {
        Map<LocalDate, Integer> eventsParJour = new LinkedHashMap<>();
        Map<LocalDate, Integer> capaciteParJour = new LinkedHashMap<>();

        LocalDate courant = debut;
        while (!courant.isAfter(fin)) {
            eventsParJour.put(courant, 0);
            capaciteParJour.put(courant, 0);
            courant = courant.plusDays(1);
        }

        for (Evenement e : events) {
            if (e.getDate_debut_event() == null) continue;

            LocalDate d = ((java.sql.Date) e.getDate_debut_event()).toLocalDate();
            eventsParJour.put(d, eventsParJour.getOrDefault(d, 0) + 1);
            capaciteParJour.put(d, capaciteParJour.getOrDefault(d, 0) + Math.max(0, e.getNb_participants_max_event()));
        }

        data.eventsParJour = eventsParJour;
        data.capaciteParJour = capaciteParJour;
    }

    private void calculerStatuts(DashboardData data, List<Evenement> events) {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Publié", 0);
        map.put("Brouillon", 0);
        map.put("Annulé", 0);
        map.put("Autres", 0);

        for (Evenement e : events) {
            String statut = safe(e.getStatut_event()).toLowerCase();

            if (statut.contains("publi") || statut.contains("ligne")) map.put("Publié", map.get("Publié") + 1);
            else if (statut.contains("brouillon")) map.put("Brouillon", map.get("Brouillon") + 1);
            else if (statut.contains("annul")) map.put("Annulé", map.get("Annulé") + 1);
            else map.put("Autres", map.get("Autres") + 1);
        }

        data.repartitionStatuts = map;
    }

    private void calculerParType(DashboardData data, List<Evenement> events) {
        Map<String, Integer> map = new LinkedHashMap<>();

        for (Evenement e : events) {
            String type = safe(e.getType_event());
            if (type.isBlank()) type = "Non défini";
            map.put(type, map.getOrDefault(type, 0) + 1);
        }

        data.parType = map;
    }

    private void calculerParVille(DashboardData data, List<Evenement> events) {
        Map<String, Integer> map = new LinkedHashMap<>();

        for (Evenement e : events) {
            String ville = safe(e.getVille_event());
            if (ville.isBlank()) ville = "Non définie";
            map.put(ville, map.getOrDefault(ville, 0) + 1);
        }

        data.parVille = map;
    }

    private void calculerTopEvenements(DashboardData data, List<Evenement> events) {
        List<Evenement> top = new ArrayList<>(events);
        top.sort(Comparator.comparingInt(Evenement::getNb_participants_max_event).reversed());

        if (top.size() > 10) {
            top = top.subList(0, 10);
        }

        data.topEvenements = top;
    }

    private void calculerRecommandations(DashboardData data) {
        List<String> recommandations = new ArrayList<>();

        if (data != null) {
            if (data.totalEvenements == 0) {
                recommandations.add("Aucun evenement sur cette periode: elargir le filtre ou planifier une nouvelle programmation.");
                data.recommandations = recommandations;
                return;
            }

            if (data.brouillons > 0) {
                recommandations.add(data.brouillons + " evenement(s) en brouillon: finaliser les fiches et publier les plus complets.");
            }
            if (data.annules > 0) {
                recommandations.add(data.annules + " annulation(s): verifier les motifs recurrents avant de relancer des evenements similaires.");
            }
            if (data.evenementsAVenir < 3) {
                recommandations.add("Calendrier faible: programmer au moins 3 evenements a venir pour maintenir l'activite.");
            } else {
                recommandations.add("Calendrier actif: prioriser la promotion des evenements les plus proches.");
            }
            if (data.tauxPublication < 50.0) {
                recommandations.add("Taux de publication bas: reduire le temps entre creation, validation et mise en ligne.");
            } else if (data.tauxPublication >= 80.0) {
                recommandations.add("Bon taux de publication: concentrer l'effort sur les inscriptions et la visibilite.");
            }
            if (data.capaciteTotale > 0) {
                int capaciteMoyenne = Math.round(data.capaciteTotale / (float) data.totalEvenements);
                recommandations.add("Capacite moyenne " + capaciteMoyenne + " places: ajuster la promotion selon le remplissage attendu.");
            }

            Map.Entry<String, Integer> typeDominant = plusGrandSegment(data.parType);
            if (typeDominant != null && typeDominant.getValue() >= 2) {
                recommandations.add("Type dominant \"" + typeDominant.getKey() + "\": tester un format different pour diversifier l'offre.");
            }

            Map.Entry<String, Integer> villeDominante = plusGrandSegment(data.parVille);
            if (villeDominante != null && villeDominante.getValue() >= 2) {
                recommandations.add("Ville la plus active \"" + villeDominante.getKey() + "\": renforcer la communication locale.");
            }

            if (!data.topEvenements.isEmpty()) {
                Evenement top = data.topEvenements.get(0);
                if (top.getNb_participants_max_event() > 0) {
                    recommandations.add("Evenement prioritaire: \"" + safe(top.getTitre_event()) + "\" avec "
                            + top.getNb_participants_max_event() + " places a valoriser.");
                }
            }

            if (recommandations.isEmpty()) {
                recommandations.add("Les indicateurs sont stables.");
            }

            data.recommandations = recommandations.stream()
                    .filter(rec -> rec != null && !rec.isBlank())
                    .distinct()
                    .limit(6)
                    .toList();
            return;
        }
    }

    private Map.Entry<String, Integer> plusGrandSegment(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .max(Map.Entry.comparingByValue())
                .orElse(null);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class DashboardData {
        public LocalDate dateDebut;
        public LocalDate dateFin;
        public String ville;

        public int totalEvenements;
        public int publies;
        public int brouillons;
        public int annules;
        public int autres;
        public int evenementsAVenir;
        public int capaciteTotale;
        public int periodeAnalysee;
        public double tauxPublication;

        public String resume;
        public String publicationMessage;
        public String capaciteMessage;

        public Map<LocalDate, Integer> eventsParJour = new LinkedHashMap<>();
        public Map<LocalDate, Integer> capaciteParJour = new LinkedHashMap<>();
        public Map<String, Integer> repartitionStatuts = new LinkedHashMap<>();
        public Map<String, Integer> parType = new LinkedHashMap<>();
        public Map<String, Integer> parVille = new LinkedHashMap<>();

        public List<Evenement> topEvenements = new ArrayList<>();
        public List<String> recommandations = new ArrayList<>();
    }
}

