package tn.esprit.services;

import tn.esprit.entities.Evenement;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class DashboardBIServiceEvenement {

    private final EvenementService evenementService = new EvenementService();

    public DashboardData chargerDonnees(LocalDate debut, LocalDate fin) throws SQLException {

        List<Evenement> events = evenementService.recuperer();

        if (debut == null) debut = LocalDate.now().minusDays(30);
        if (fin == null) fin = LocalDate.now();

        List<Evenement> filtres = new ArrayList<>();

        for (Evenement e : events) {
            if (e.getDate_debut_event() != null) {

                LocalDate d = ((java.sql.Date) e.getDate_debut_event()).toLocalDate();

                if (!d.isBefore(debut) && !d.isAfter(fin)) {
                    filtres.add(e);
                }
            }
        }

        DashboardData data = new DashboardData();
        data.dateDebut = debut;
        data.dateFin = fin;

        calculerKpis(data, filtres);
        calculerParType(data, filtres);
        calculerParVille(data, filtres);
        calculerTopEvenements(data, filtres);

        return data;
    }

    // ================= KPI =================
    private void calculerKpis(DashboardData data, List<Evenement> events) {

        int total = events.size();
        int publies = 0;
        int brouillons = 0;
        int archives = 0;

        for (Evenement e : events) {
            String statut = safe(e.getStatut_event()).toLowerCase();

            if (statut.contains("publi")) publies++;
            else if (statut.contains("brouillon")) brouillons++;
            else archives++;
        }

        data.totalEvenements = total;
        data.publies = publies;
        data.brouillons = brouillons;
        data.archives = archives;

        data.resume = "Analyse des événements entre "
                + data.dateDebut + " et " + data.dateFin;
    }

    // ================= PAR TYPE =================
    private void calculerParType(DashboardData data, List<Evenement> events) {

        Map<String, Integer> map = new LinkedHashMap<>();

        for (Evenement e : events) {
            String type = safe(e.getType_event());
            map.put(type, map.getOrDefault(type, 0) + 1);
        }

        data.parType = map;
    }

    // ================= PAR VILLE =================
    private void calculerParVille(DashboardData data, List<Evenement> events) {

        Map<String, Integer> map = new LinkedHashMap<>();

        for (Evenement e : events) {
            String ville = safe(e.getVille_event());
            map.put(ville, map.getOrDefault(ville, 0) + 1);
        }

        data.parVille = map;
    }

    // ================= TOP EVENEMENTS =================
    private void calculerTopEvenements(DashboardData data, List<Evenement> events) {

        List<Evenement> top = new ArrayList<>(events);

        // 🔥 On trie par nb_participants_max_event
        top.sort(Comparator.comparingInt(Evenement::getNb_participants_max_event).reversed());

        if (top.size() > 5) {
            top = top.subList(0, 5);
        }

        data.topEvenements = top;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    // ================= DATA CLASS =================
    public static class DashboardData {

        public LocalDate dateDebut;
        public LocalDate dateFin;

        public int totalEvenements;
        public int publies;
        public int brouillons;
        public int archives;

        public String resume;

        public Map<String, Integer> parType = new LinkedHashMap<>();
        public Map<String, Integer> parVille = new LinkedHashMap<>();

        public List<Evenement> topEvenements = new ArrayList<>();
    }
}