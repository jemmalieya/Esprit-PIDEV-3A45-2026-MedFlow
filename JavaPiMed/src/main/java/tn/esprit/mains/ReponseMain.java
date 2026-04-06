package tn.esprit.mains;

import tn.esprit.entities.ReponseReclamation;
import tn.esprit.services.ReponseService;

import java.time.LocalDateTime;
import java.util.List;

public class ReponseMain {

    public static void main(String[] args) {

        ReponseService service = new ReponseService();

        // =========================
        // AJOUT
        // =========================
        ReponseReclamation r = new ReponseReclamation();
        r.setId_reclamation(4); // ici on fixe l'id_reclamation statiquement
        r.setMessage("Merci pour votre réclamation");
        r.setType_reponse("Email");
        r.setDate_creation_rep(LocalDateTime.now());
        r.setDate_modification_rep(LocalDateTime.now());
        r.setIs_read(false);

        service.ajouter(r);

        // =========================
        // AFFICHAGE
        // =========================
        System.out.println("\n===== LISTE DES REPONSES =====");
        for (ReponseReclamation rep : service.recuperer()) {
            System.out.println(rep);
        }

        // =========================
        // MODIFICATION
        // =========================
        int idToModify = r.getId_reponse(); // modifier la dernière ajoutée
        ReponseReclamation repToModify = service.findById(idToModify);
        if (repToModify != null) {
            repToModify.setMessage("Message modifié pour test");
            repToModify.setType_reponse("SMS");
            repToModify.setDate_modification_rep(LocalDateTime.now());
            repToModify.setIs_read(true);

            service.modifier(repToModify);
        }

        // =========================
        // AFFICHAGE APRES MODIF
        // =========================
        System.out.println("\n===== APRES MODIFICATION =====");
        for (ReponseReclamation rep : service.recuperer()) {
            System.out.println(rep);
        }

        // =========================
        // SUPPRESSION
        // =========================
        int idToDelete = 4; // supprimer la dernière ajoutée
        ReponseReclamation repToDelete = service.findById(idToDelete);
        if (repToDelete != null) {
            service.supprimer(repToDelete);
        }

        // =========================
        // AFFICHAGE FINAL
        // =========================
        System.out.println("\n===== APRES SUPPRESSION =====");
        for (ReponseReclamation rep : service.recuperer()) {
            System.out.println(rep);
        }
    }
}