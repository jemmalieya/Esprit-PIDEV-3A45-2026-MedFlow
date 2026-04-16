package tn.esprit.mains;

import tn.esprit.entities.Ressource;
import tn.esprit.entities.Evenement;
import tn.esprit.services.RessourceService;

import java.util.Date;

public class MainRessource {
    public static void main(String[] args) throws Exception {
        RessourceService service = new RessourceService();

        // Create an Evenement object (similar to MainEvenement)
        Evenement evenement = new Evenement();
        evenement.setId(32);  // Set the Evenement's ID

        // Create Ressource using the 4-argument constructor
        Ressource r = new Ressource(
                0,                    // ID
                "Vidéo hajer55",       // Nom ressource
                "Éducation",          // Catégorie ressource
                evenement             // Evenement object
        );

        // Set other fields after object creation
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

        // Adding the Ressource
        service.ajouter(r);

        System.out.println("===== Apres ajout =====");
        displayResources(service);

        // Modify the resource
        r.setId(39);
        r.setNom_ressource("Vidéo Tutoriel Mis à Jour");
        r.setQuantite_disponible_ressource(100);

        service.modifier(r);

        System.out.println("===== Apres modif =====");
        displayResources(service);

        // Delete the resource
        service.supprimer(r);

        System.out.println("===== Apres supp =====");
        displayResources(service);
    }

    public static void displayResources(RessourceService service) {
        for (Ressource ressource : service.recuperer()) {
            System.out.println(ressource);
        }
    }
}