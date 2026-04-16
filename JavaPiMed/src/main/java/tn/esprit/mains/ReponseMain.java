package tn.esprit.mains;

import tn.esprit.entities.Reclamation;
import tn.esprit.entities.ReponseReclamation;
import tn.esprit.services.ReclamationService;
import tn.esprit.services.ReponseService;

import java.time.LocalDateTime;

public class ReponseMain {

    public static void main(String[] args) {

        ReponseService service = new ReponseService();
        ReclamationService reclamationService = new ReclamationService();

        // =========================
        // AJOUT
        // =========================

        // ✅ on récupère la vraie reclamation depuis la base
        Reclamation reclamation = reclamationService.recupererParId(4);

        if (reclamation == null) {
            System.out.println("Reclamation introuvable !");
            return;
        }

        ReponseReclamation r = new ReponseReclamation();
        r.setReclamation(reclamation); // ✅ FIX IMPORTANT
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
        int idToModify = r.getId_reponse();
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
        int idToDelete = r.getId_reponse();
        ReponseReclamation repToDelete = service.findById(idToDelete);

        if (repToDelete != null) {
            service.supprimer(repToDelete);
        }

        // =========================
        // FINAL
        // =========================
        System.out.println("\n===== APRES SUPPRESSION =====");
        for (ReponseReclamation rep : service.recuperer()) {
            System.out.println(rep);
        }
    }
}