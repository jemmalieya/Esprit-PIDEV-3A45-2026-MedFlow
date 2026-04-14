package tn.esprit.mains;

import tn.esprit.entities.Evenement;
import tn.esprit.entities.User;
import tn.esprit.services.EvenementService;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class MainEvenement {

    public static void main(String[] args) throws Exception {
        EvenementService service = new EvenementService();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // Creating a User object (similarly to how your friend handles User)
        User user = new User();
        user.setId(23);  // Assuming the User has an ID, you can set it dynamically.
        user.setNom("John Doe");
        user.setPrenom("John");

        // Create an Evenement object and set its attributes
        Evenement e = new Evenement();
        e.setTitre_event("Forum Hajour44");
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

        // Set the User for the Evenement object
        e.setUser(user);

        // Create the list of ressources associated with the event
        // This part assumes that the event has some ressources (you can create them as well)
        // e.setRessources(Arrays.asList(ressource1, ressource2));  // Add some ressources if needed

        // Adding the Evenement to the service
        service.ajouter(e);

        System.out.println("\n===== LISTE DES EVENEMENTS =====");
        for (Evenement ev : service.recuperer()) {
            System.out.println(ev);
        }

        // Modify an existing event (find it by ID and modify its details)
        int idEvenementToModify = 32;
        Evenement evenementToModify = findEvenementById(service, idEvenementToModify);
        if (evenementToModify != null) {
            evenementToModify.setTitre_event("Evenement Modifié");
            evenementToModify.setSlug_event("evenement-modifie");
            evenementToModify.setType_event("Atelier");
            evenementToModify.setDescription_event("Description modifiée");
            evenementToModify.setObjectif_event("Nouvel objectif");
            evenementToModify.setStatut_event("Brouillon");
            evenementToModify.setDate_debut_event(sdf.parse("2026-06-01"));
            evenementToModify.setDate_fin_event(sdf.parse("2026-06-05"));
            evenementToModify.setNom_lieu_event("ESPRIT Ghazela");
            evenementToModify.setAdresse_event("Ariana Sup");
            evenementToModify.setVille_event("Sfax");
            evenementToModify.setNb_participants_max_event(250);
            evenementToModify.setInscription_obligatoire_event(false);
            evenementToModify.setDate_limite_inscription_event(sdf.parse("2026-05-28"));
            evenementToModify.setEmail_contact_event("newmail@test.com");
            evenementToModify.setTel_contact_event("99887766");
            evenementToModify.setNom_organisateur_event("Nouvel organisateur");
            evenementToModify.setImage_couverture_event("newimage.jpg");
            evenementToModify.setVisibilite_event("Privé");
            evenementToModify.setDate_mise_a_jour_event(new java.util.Date());

            // Update the Evenement
            service.modifier(evenementToModify);
        }

        System.out.println("\n===== APRES MODIFICATION =====");
        for (Evenement ev : service.recuperer()) {
            System.out.println(ev);
        }

        // Deleting an event by ID
        int idEvenementToDelete = 30;
        Evenement evenementToDelete = findEvenementById(service, idEvenementToDelete);
        if (evenementToDelete != null) {
            service.supprimer(evenementToDelete);
        }

        System.out.println("\n===== APRES SUPPRESSION =====");
        for (Evenement ev : service.recuperer()) {
            System.out.println(ev);
        }
    }

    public static Evenement findEvenementById(EvenementService service, int id) throws SQLException {
        for (Evenement e : service.recuperer()) {
            if (e.getId() == id) {
                return e;
            }
        }
        System.out.println("Evenement avec ID " + id + " non trouvé");
        return null;
    }
}