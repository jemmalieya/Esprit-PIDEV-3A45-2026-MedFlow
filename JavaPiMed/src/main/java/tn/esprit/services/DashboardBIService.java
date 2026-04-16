package tn.esprit.services;

import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardBIService {

    private final ProduitService produitService = new ProduitService();
    private final CommandeService commandeService = new CommandeService();

    public DashboardData chargerDonnees(LocalDate debut, LocalDate fin, String categorie) {
        List<Produit> produits = produitService.recuperer();
        List<Commande> commandes = commandeService.recuperer();

        if (debut == null) debut = LocalDate.now().minusDays(6);
        if (fin == null) fin = LocalDate.now();

        String cat = categorie == null ? "Toutes les catégories" : categorie;

        List<Produit> produitsFiltres = new ArrayList<>(produits);
        if (!cat.equalsIgnoreCase("Toutes les catégories")) {
            produitsFiltres.removeIf(p -> !safe(p.getCategorie_produit()).equalsIgnoreCase(cat));
        }

        List<Commande> commandesFiltrees = new ArrayList<>();
        for (Commande c : commandes) {
            if (c.getDate_creation_commande() != null) {
                LocalDate d = c.getDate_creation_commande().toLocalDate();
                if (!d.isBefore(debut) && !d.isAfter(fin)) {
                    commandesFiltrees.add(c);
                }
            }
        }

        DashboardData data = new DashboardData();
        data.dateDebut = debut;
        data.dateFin = fin;
        data.categorie = cat;

        calculerKpis(data, commandesFiltrees, produitsFiltres);
        calculerSeriesVentes(data, commandesFiltrees, debut, fin);
        calculerStatuts(data, commandesFiltrees);
        calculerParCategorie(data, produitsFiltres);
        calculerTopProduits(data, produitsFiltres);
        calculerStockCritique(data, produitsFiltres);
        calculerRecommandations(data);

        return data;
    }

    private void calculerKpis(DashboardData data, List<Commande> commandes, List<Produit> produits) {
        double ca = 0.0;
        int totalCommandes = commandes.size();
        int quantiteVendue = 0;
        int enAttente = 0;
        int validees = 0;
        int stockCritique = 0;

        for (Commande c : commandes) {
            ca += c.getMontant_total_cents() / 100.0;

            String statut = safe(c.getStatut_commande()).toLowerCase();
            if (statut.contains("attente")) enAttente++;
            if (statut.contains("confirm") || statut.contains("livraison") || statut.contains("final")) validees++;

            if (c.getCommande_produits() != null) {
                for (CommandeProduit cp : c.getCommande_produits()) {
                    quantiteVendue += cp.getQuantite_commandee();
                }
            }
        }

        for (Produit p : produits) {
            if (p.getQuantite_produit() <= 7) {
                stockCritique++;
            }
        }

        double panierMoyen = totalCommandes > 0 ? ca / totalCommandes : 0.0;
        double conversion = totalCommandes > 0 ? (validees * 100.0 / totalCommandes) : 0.0;
        double tauxRupture = produits.isEmpty() ? 0.0 : (stockCritique * 100.0 / produits.size());

        data.chiffreAffaires = ca;
        data.totalCommandes = totalCommandes;
        data.panierMoyen = panierMoyen;
        data.tauxConversion = conversion;
        data.quantiteVendue = quantiteVendue;
        data.enAttente = enAttente;
        data.tauxRupture = tauxRupture;
        data.stockCritiqueCount = stockCritique;
        data.periodeAnalysee = (int) (ChronoUnit.DAYS.between(data.dateDebut, data.dateFin) + 1);

        data.croissanceMessage = "Croissance du CA — Vue synthétique sur la période sélectionnée";
        data.stockCritiqueMessage = "Stock critique — " + stockCritique + " produit(s) ont un stock faible";
    }

    private void calculerSeriesVentes(DashboardData data, List<Commande> commandes, LocalDate debut, LocalDate fin) {
        Map<LocalDate, Double> caParJour = new LinkedHashMap<>();
        Map<LocalDate, Integer> qteParJour = new LinkedHashMap<>();

        LocalDate courant = debut;
        while (!courant.isAfter(fin)) {
            caParJour.put(courant, 0.0);
            qteParJour.put(courant, 0);
            courant = courant.plusDays(1);
        }

        for (Commande c : commandes) {
            if (c.getDate_creation_commande() == null) continue;

            LocalDate d = c.getDate_creation_commande().toLocalDate();
            caParJour.put(d, caParJour.getOrDefault(d, 0.0) + (c.getMontant_total_cents() / 100.0));

            int totalQte = 0;
            if (c.getCommande_produits() != null) {
                for (CommandeProduit cp : c.getCommande_produits()) {
                    totalQte += cp.getQuantite_commandee();
                }
            }
            qteParJour.put(d, qteParJour.getOrDefault(d, 0) + totalQte);
        }

        data.caParJour = caParJour;
        data.quantitesParJour = qteParJour;
    }

    private void calculerStatuts(DashboardData data, List<Commande> commandes) {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Confirmée", 0);
        map.put("En attente", 0);
        map.put("En cours", 0);
        map.put("Autres", 0);

        for (Commande c : commandes) {
            String statut = safe(c.getStatut_commande()).toLowerCase();

            if (statut.contains("confirm")) {
                map.put("Confirmée", map.get("Confirmée") + 1);
            } else if (statut.contains("attente")) {
                map.put("En attente", map.get("En attente") + 1);
            } else if (statut.contains("cours")) {
                map.put("En cours", map.get("En cours") + 1);
            } else {
                map.put("Autres", map.get("Autres") + 1);
            }
        }

        data.repartitionStatuts = map;
    }

    private void calculerParCategorie(DashboardData data, List<Produit> produits) {
        Map<String, Integer> quantites = new LinkedHashMap<>();
        Map<String, Double> ca = new LinkedHashMap<>();

        for (Produit p : produits) {
            String cat = safe(p.getCategorie_produit());
            quantites.put(cat, quantites.getOrDefault(cat, 0) + p.getQuantite_produit());
            ca.put(cat, ca.getOrDefault(cat, 0.0) + (p.getPrix_produit() * p.getQuantite_produit()));
        }

        data.quantitesParCategorie = quantites;
        data.caParCategorie = ca;
    }

    private void calculerTopProduits(DashboardData data, List<Produit> produits) {
        List<Produit> top = new ArrayList<>(produits);
        top.sort(Comparator.comparingInt(Produit::getQuantite_produit).reversed());

        if (top.size() > 10) {
            top = top.subList(0, 10);
        }

        data.topProduits = top;
    }

    private void calculerStockCritique(DashboardData data, List<Produit> produits) {
        List<Produit> critiques = produits.stream()
                .filter(p -> p.getQuantite_produit() <= 7)
                .sorted(Comparator.comparingInt(Produit::getQuantite_produit))
                .collect(Collectors.toList());

        data.stockCritiqueProduits = critiques;
    }

    private void calculerRecommandations(DashboardData data) {
        List<String> recommandations = new ArrayList<>();

        recommandations.add("Simplifier/accélérer le processus de validation pour augmenter la conversion");

        if (data.stockCritiqueCount > 0) {
            recommandations.add("Réapprovisionner rapidement les produits en stock critique");
        }

        if (data.chiffreAffaires > 0) {
            recommandations.add("Mettre en avant les catégories les plus performantes");
        }

        data.recommandations = recommandations;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class DashboardData {
        public LocalDate dateDebut;
        public LocalDate dateFin;
        public String categorie;

        public double chiffreAffaires;
        public int totalCommandes;
        public double panierMoyen;
        public double tauxConversion;
        public int quantiteVendue;
        public int enAttente;
        public double tauxRupture;
        public int periodeAnalysee;
        public int stockCritiqueCount;

        public String croissanceMessage;
        public String stockCritiqueMessage;

        public Map<LocalDate, Double> caParJour = new LinkedHashMap<>();
        public Map<LocalDate, Integer> quantitesParJour = new LinkedHashMap<>();
        public Map<String, Integer> repartitionStatuts = new LinkedHashMap<>();
        public Map<String, Integer> quantitesParCategorie = new LinkedHashMap<>();
        public Map<String, Double> caParCategorie = new LinkedHashMap<>();

        public List<Produit> topProduits = new ArrayList<>();
        public List<Produit> stockCritiqueProduits = new ArrayList<>();
        public List<String> recommandations = new ArrayList<>();
    }
}