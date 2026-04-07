package tn.esprit.mains;

import tn.esprit.entities.Produit;
import tn.esprit.services.ProduitService;

public class MainProduit {
    public static void main(String[] args) throws Exception {

        ProduitService service = new ProduitService();

        // =========================
        // AJOUT
        // =========================
        Produit p = new Produit();

        p.setNom_produit("fokmatest");
        p.setDescription_produit("test testt test test");
        p.setPrix_produit(100);
        p.setQuantite_produit(3);
        p.setImage_produit("tramadol.jpg");
        p.setCategorie_produit("Vitamines");
        p.setStatus_produit("Disponible");

        service.ajouter(p);

        // =========================
        // AFFICHAGE
        // =========================
        System.out.println("\n===== LISTE DES PRODUITS =====");
        for (Produit produit : service.recuperer()) {
            System.out.println(produit);
        }

        // =========================
        // MODIFICATION (choisir un produit par ID)
        // =========================
        int idProduitToModify = 6;  // Choisis ici l'ID du produit que tu veux modifier

        Produit produitToModify = findProduitById(service, idProduitToModify);
        if (produitToModify != null) {
            produitToModify.setNom_produit("Aspirin");
            produitToModify.setDescription_produit("not so good");
            produitToModify.setPrix_produit(6.00);
            produitToModify.setQuantite_produit(25);
            produitToModify.setImage_produit("tramadol-modifie.jpg");
            produitToModify.setCategorie_produit("Médicament");
            produitToModify.setStatus_produit("Disponible");

            service.modifier(produitToModify);
        }

        // =========================
        // AFFICHAGE APRES MODIF
        // =========================
        System.out.println("\n===== APRES MODIFICATION =====");
        for (Produit produit : service.recuperer()) {
            System.out.println(produit);
        }

        // =========================
        // SUPPRESSION (choisir un produit par ID)
        // =========================
        int idProduitToDelete =34;  // Choisis ici l'ID du produit que tu veux supprimer

        Produit produitToDelete = findProduitById(service, idProduitToDelete);
        if (produitToDelete != null) {
            service.supprimer(produitToDelete);
        }

        // =========================
        // AFFICHAGE FINAL
        // =========================
        System.out.println("\n===== APRES SUPPRESSION =====");
        for (Produit produit : service.recuperer()) {
            System.out.println(produit);
        }
    }

    // Méthode pour trouver un produit par ID
    public static Produit findProduitById(ProduitService service, int id) {
        for (Produit p : service.recuperer()) {
            if (p.getId_produit() == id) {
                return p;
            }
        }
        System.out.println("❌ Produit avec ID " + id + " non trouvé");
        return null;
    }
}