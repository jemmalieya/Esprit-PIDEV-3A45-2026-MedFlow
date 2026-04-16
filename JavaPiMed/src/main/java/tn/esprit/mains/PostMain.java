package tn.esprit.mains;

import tn.esprit.entities.Post;
import tn.esprit.services.PostService;

import java.time.LocalDateTime;

public class PostMain {

    public static void main(String[] args) {

        PostService service = new PostService();

        // =========================
        // AJOUT
        // =========================
        Post p = new Post();
        p.setUser_id(15); // à changer selon un user existant
        p.setTitre("Mon premier post");
        p.setContenu("Ceci est le contenu du post");
        p.setLocalisation("Tunis");
        p.setImg_post("post1.jpg");
        p.setHashtags("#test #java");
        p.setVisibilite("Public");
        p.setDate_creation(LocalDateTime.now());
        p.setEst_anonyme(false);
        p.setCategorie("Actualité");
        p.setHumeur("Heureux");
        p.setNbr_reactions(0);
        p.setNbr_commentaires(0);
        p.setIs_approved(true);
        p.setModeration_status("OK");
        p.setModeration_message("");
        p.setModeration_seen(true);

        service.ajouter(p);

        // =========================
        // AFFICHAGE
        // =========================
        System.out.println("\n===== LISTE DES POSTS =====");
        for (Post post : service.recuperer()) {
            System.out.println(post);
        }

        // =========================
        // MODIFICATION
        // =========================
        int idToModify = 15; // modifier la dernière ajoutée
        Post postToModify = service.findById(idToModify);
        if (postToModify != null) {
            postToModify.setTitre("Post modifié");
            postToModify.setContenu("Contenu modifié");
            postToModify.setDate_modification(LocalDateTime.now());

            service.modifier(postToModify);
        }

        // =========================
        // AFFICHAGE APRES MODIF
        // =========================
        System.out.println("\n===== APRES MODIFICATION =====");
        for (Post post : service.recuperer()) {
            System.out.println(post);
        }

        // =========================
        // SUPPRESSION
        // =========================
        int idToDelete = p.getId();
        Post postToDelete = service.findById(idToDelete);
        if (postToDelete != null) {
            service.supprimer(postToDelete);
        }

        // =========================
        // AFFICHAGE FINAL
        // =========================
        System.out.println("\n===== APRES SUPPRESSION =====");
        for (Post post : service.recuperer()) {
            System.out.println(post);
        }
    }
}