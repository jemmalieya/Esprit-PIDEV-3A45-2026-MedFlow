package tn.esprit.mains;

import tn.esprit.entities.Ressource;
import tn.esprit.services.RessourceService;

import java.util.Date;

public class MainRessource {
    public static void main(String[] args) throws Exception {
        RessourceService service = new RessourceService();

        // =========================
        // AJOUTER UNE RESSOURCE
        // =========================
        Ressource r = new Ressource();
        r.setNom_ressource("Vidéo mauy");
        r.setCategorie_ressource("Éducation");
        r.setType_ressource("Vidéo");
        r.setChemin_fichier_ressource("/path/to/file");
        r.setMime_type_ressource("video/mp4");
        r.setTaille_kb_ressource(500);
        r.setUrl_externe_ressource("http://example.com/video");
        r.setQuantite_disponible_ressource(10);
        r.setUnite_ressource("Unité");
        r.setFournisseur_ressource("Fournisseur1");
        r.setCout_estime_ressource(100.0);
        r.setEst_publique_ressource(true);
        r.setNotes_ressource("Vidéo éducative");
        r.setDate_creation_ressource(new Date());
        r.setDate_mise_a_jour_ressource(new Date());

        // Set the evenement_id (choose the appropriate value for your case)
        r.setEvenement_id(16);  // Example: setting evenement_id to 1

        // Add the resource to the database
        service.ajouter(r);
        System.out.println("===== After Adding =====");
        displayResources(service);

        // =========================
        // MODIFIER UNE RESSOURCE
        // =========================
        // Assume you want to update the resource just added or another resource
        // Retrieve the resource by ID (or any other criteria)
        r.setId(13);  // Modify with the appropriate ID
        r.setNom_ressource("Vidéo Tutoriel Mis à Jour");
        r.setQuantite_disponible_ressource(20); // Example: updating quantity

        // Modify the resource in the database
        service.modifier(r);
        System.out.println("===== After Modifying =====");
        displayResources(service);

        // =========================
        // SUPPRIMER UNE RESSOURCE
        // =========================
        // Example: Delete the resource (e.g., based on ID)
        service.supprimer(r);
        System.out.println("===== After Deleting =====");
        displayResources(service);
    }

    // Method to display all resources
    public static void displayResources(RessourceService service) {
        for (Ressource ressource : service.recuperer()) {
            System.out.println(ressource);
        }
    }
}