package tn.esprit.mains;

import tn.esprit.entities.Produit;
import tn.esprit.services.ProduitService;

public class MainProduit {
    public static void main(String[] args) {

        ProduitService service = new ProduitService();


        Produit p = new Produit();
        p.setNom_produit("fokmatesttestest");
        p.setDescription_produit("test testt test test");
        p.setPrix_produit(100);
        p.setQuantite_produit(3);
        p.setImage_produit("tramadol.jpg");
        p.setCategorie_produit("Vitamines");
        p.setStatus_produit("Disponible");

        service.ajouter(p);

        System.out.println("\n===== LISTE DES PRODUITS =====");
        for (Produit produit : service.recuperer()) {
            System.out.println(produit);
        }


        int idProduitAAfficher = 40;

        Produit produitUnique = service.recupererParId(idProduitAAfficher);

        System.out.println("\n===== PRODUIT PAR ID =====");
        if (produitUnique != null) {
            System.out.println(produitUnique);
        } else {
            System.out.println(" Produit avec ID " + idProduitAAfficher + " non trouvé");
        }


        int idProduitToModify = 40;

        Produit produitToModify = service.recupererParId(idProduitToModify);

        if (produitToModify != null) {
            produitToModify.setNom_produit("help");
            produitToModify.setDescription_produit("not so good");
            produitToModify.setPrix_produit(6.00);
            produitToModify.setQuantite_produit(25);
            produitToModify.setImage_produit("tramadol-modifie.jpg");
            produitToModify.setCategorie_produit("Médicament");
            produitToModify.setStatus_produit("Disponible");

            service.modifier(produitToModify);
        } else {
            System.out.println(" Produit avec ID " + idProduitToModify + " non trouvé");
        }

        /*
        // =========================
        // MODIFIER (sans recupererParId)
        // =========================
        Produit produitToModify = new Produit();
        produitToModify.setId_produit(40);
        produitToModify.setNom_produit("help");
        produitToModify.setDescription_produit("not so good");
        produitToModify.setPrix_produit(6.00);
        produitToModify.setQuantite_produit(25);
        produitToModify.setImage_produit("tramadol-modifie.jpg");
        produitToModify.setCategorie_produit("Médicament");
        produitToModify.setStatus_produit("Disponible");

        service.modifier(produitToModify);
        */

        System.out.println("\n===== APRES MODIFICATION =====");
        for (Produit produit : service.recuperer()) {
            System.out.println(produit);
        }


        int idProduitToDelete = 41;

        Produit produitToDelete = service.recupererParId(idProduitToDelete);

        if (produitToDelete != null) {
            service.supprimer(produitToDelete);
        } else {
            System.out.println(" Produit avec ID " + idProduitToDelete + " non trouvé");
        }

        /*
        // =========================
        // SUPPRIMER (sans recupererParId)
        // =========================
        Produit produitToDelete = new Produit();
        produitToDelete.setId_produit(41);

        service.supprimer(produitToDelete);
        */


        System.out.println("\n===== APRES SUPPRESSION =====");
        for (Produit produit : service.recuperer()) {
            System.out.println(produit);
        }
    }
}