package tn.esprit.mains;

import tn.esprit.entities.Evenement;
import tn.esprit.services.EvenementService;

import java.text.SimpleDateFormat;

public class MainEvenement {
    public static void main(String[] args) throws Exception {

        EvenementService service = new EvenementService();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // =========================
        // AJOUT
        // =========================
        Evenement e = new Evenement();

        e.setTitre_event("Forum santé");
        e.setSlug_event("forum-sante");
        e.setType_event("Conférence");
        e.setDescription_event("Evenement sur la santé");
        e.setObjectif_event("Sensibilisation");
        e.setStatut_event("Publié");

        e.setDate_debut_event(sdf.parse("2026-05-01"));
        e.setDate_fin_event(sdf.parse("2026-05-03"));

        e.setNom_lieu_event("ESPRIT");
        e.setAdresse_event("Ariana");
        e.setVille_event("Tunis");

        e.setNb_participants_max_event(100);
        e.setInscription_obligatoire_event(true);

        e.setDate_limite_inscription_event(sdf.parse("2026-04-28"));

        e.setEmail_contact_event("contact@medflow.com");
        e.setTel_contact_event("22123456");
        e.setNom_organisateur_event("MedFlow");

        e.setImage_couverture_event("image.jpg");
        e.setVisibilite_event("Public");

        e.setDate_creation_event(new java.util.Date());
        e.setDate_mise_a_jour_event(new java.util.Date());

        service.ajouter(e);

        // =========================
        // AFFICHAGE
        // =========================
        System.out.println("\n===== LISTE DES EVENEMENTS =====");
        for (Evenement ev : service.recuperer()) {
            System.out.println(ev);
        }

        // =========================
        // MODIFICATION
        // =========================
        if (!service.recuperer().isEmpty()) {
            Evenement first = service.recuperer().get(0);

            first.setTitre_event("Evenement modifié");
            first.setSlug_event("evenement-modifie");
            first.setType_event("Atelier");
            first.setDescription_event("Description modifiée");
            first.setObjectif_event("Nouvel objectif");
            first.setStatut_event("Brouillon");

            first.setDate_debut_event(sdf.parse("2026-06-01"));
            first.setDate_fin_event(sdf.parse("2026-06-05"));

            first.setNom_lieu_event("ESPRIT Ghazela");
            first.setAdresse_event("Ariana Sup");
            first.setVille_event("Sfax");

            first.setNb_participants_max_event(250);
            first.setInscription_obligatoire_event(false);

            first.setDate_limite_inscription_event(sdf.parse("2026-05-28"));

            first.setEmail_contact_event("newmail@test.com");
            first.setTel_contact_event("99887766");
            first.setNom_organisateur_event("Nouvel organisateur");

            first.setImage_couverture_event("newimage.jpg");
            first.setVisibilite_event("Privé");

            first.setDate_mise_a_jour_event(new java.util.Date());

            service.modifier(first);
        }

        // =========================
        // AFFICHAGE APRES MODIF
        // =========================
        System.out.println("\n===== APRES MODIFICATION =====");
        for (Evenement ev : service.recuperer()) {
            System.out.println(ev);
        }

        // =========================
        // SUPPRESSION
        // =========================
        if (!service.recuperer().isEmpty()) {
            Evenement first = service.recuperer().get(0);
            service.supprimer(first);
        }

        // =========================
        // AFFICHAGE FINAL
        // =========================
        System.out.println("\n===== APRES SUPPRESSION =====");
        for (Evenement ev : service.recuperer()) {
            System.out.println(ev);
        }
    }
}