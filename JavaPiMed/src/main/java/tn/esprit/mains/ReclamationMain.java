package tn.esprit.mains;

import tn.esprit.entities.Reclamation;
import tn.esprit.services.ReclamationService;

import java.sql.SQLException;

public class ReclamationMain {
    public static void main(String[] args) throws Exception {

        ReclamationService service = new ReclamationService();

        // =========================
        // AJOUT
        // =========================
        Reclamation r = new Reclamation();

        r.setReference_reclamation("REC002");
        r.setContenu("Produit expiré");
        r.setDescription("Le produit a dépassé la date de péremption");
        r.setType("Produit");
        r.setStatut_reclamation("En attente");
        r.setPriorite("Moyenne");

       // service.ajouter(r);

        // =========================
        // AFFICHAGE
        // =========================
        System.out.println("\n===== LISTE DES RECLAMATIONS =====");
        for (Reclamation rec : service.recuperer()) {
            System.out.println(rec);
        }

        // =========================
        // MODIFICATION (choisir une réclamation par ID)
        // =========================
        int idReclamationToModify = 4;  // Choisis ici l'ID réel de la réclamation à modifier

        Reclamation recToModify = findReclamationById(service, idReclamationToModify);
        if (recToModify != null) {
            recToModify.setContenu("Produit cassé");
            recToModify.setDescription("Le produit est inutilisable");
            recToModify.setStatut_reclamation("Traité");
            recToModify.setPriorite("Haute");

            service.modifier(recToModify);
        }

        // =========================
        // AFFICHAGE APRES MODIF
        // =========================
        System.out.println("\n===== APRES MODIFICATION =====");
        for (Reclamation rec : service.recuperer()) {
            System.out.println(rec);
        }

        // =========================
        // SUPPRESSION (choisir une réclamation par ID)
        // =========================
        int idReclamationToDelete = 41;  // Choisis ici l'ID réel de la réclamation à supprimer

        Reclamation recToDelete = findReclamationById(service, idReclamationToDelete);
        if (recToDelete != null) {
            service.supprimer(recToDelete);
        }

        // =========================
        // AFFICHAGE FINAL
        // =========================
        System.out.println("\n===== APRES SUPPRESSION =====");
        for (Reclamation rec : service.recuperer()) {
            System.out.println(rec);
        }
    }

    // Méthode pour trouver une réclamation par ID
    public static Reclamation findReclamationById(ReclamationService service, int id) {
        try {
            for (Reclamation r : service.recuperer()) {
                if (r.getId_reclamation() == id) {
                    return r;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("❌ Réclamation avec ID " + id + " non trouvée");
        return null;
    }
}