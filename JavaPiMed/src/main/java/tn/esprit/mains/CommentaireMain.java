package tn.esprit.mains;

import tn.esprit.entities.Commentaire;
import tn.esprit.services.CommentaireService;

import java.time.LocalDateTime;

public class CommentaireMain {

    public static void main(String[] args) {

        CommentaireService service = new CommentaireService();

        // =========================
        // AJOUT
        // =========================
        Commentaire c = new Commentaire();
        c.setPost_id(1); // id d'un post existant
        c.setUser_id(15); // id d'un user existant
        c.setContenu("Ceci est un commentaire test");
        c.setDate_creation(LocalDateTime.now());
        c.setEst_anonyme(false);
        c.setParametres_confidentialite("Public");
        c.setStatus("Publié");

        service.ajouter(c);

        // =========================
        // AFFICHAGE
        // =========================
        System.out.println("\n===== LISTE DES COMMENTAIRES =====");
        for (Commentaire comm : service.recuperer()) {
            System.out.println(comm);
        }

        // =========================
        // MODIFICATION
        // =========================
        int idToModify = c.getId();
        Commentaire commToModify = service.findById(idToModify);
        if (commToModify != null) {
            commToModify.setContenu("Commentaire modifié");
            commToModify.setStatus("Modéré");
            commToModify.setModerated_at(LocalDateTime.now());

            service.modifier(commToModify);
        }

        // =========================
        // AFFICHAGE APRES MODIF
        // =========================
        System.out.println("\n===== APRES MODIFICATION =====");
        for (Commentaire comm : service.recuperer()) {
            System.out.println(comm);
        }

        // =========================
        // SUPPRESSION
        // =========================
        int idToDelete = c.getId();
        Commentaire commToDelete = service.findById(idToDelete);
        if (commToDelete != null) {
            service.supprimer(commToDelete);
        }

        // =========================
        // AFFICHAGE FINAL
        // =========================
        System.out.println("\n===== APRES SUPPRESSION =====");
        for (Commentaire comm : service.recuperer()) {
            System.out.println(comm);
        }
    }
}