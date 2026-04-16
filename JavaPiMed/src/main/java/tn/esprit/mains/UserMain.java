package tn.esprit.mains;

import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import java.time.LocalDate;

public class UserMain {

    public static void main(String[] args) {

        UserService service = new UserService();

        // =========================
        // CREATE
        // =========================
        User u = new User();
        u.setCin("87654322");
        u.setNom("si");
        u.setPrenom("bachir");
        u.setDateNaissance(LocalDate.of(1990, 1, 1));
        u.setTelephoneUser("20123456");
        u.setEmailUser("bechireljamil.java@test.com");
        u.setAdresseUser("Tunis");
        u.setPassword("password123");
        u.setStatutCompte("ACTIF");
        u.setRoleSysteme("PATIENT");

        service.ajouter(u);

        // =========================
        // READ (after create)
        // =========================
        System.out.println("\n===== LISTE DES UTILISATEURS (APRES AJOUT) =====");
        for (User user : service.recuperer()) {
            System.out.println(user);
        }

        // =========================
        // UPDATE
        // =========================
        int idToModify = u.getId();
        User userToModify = service.findById(idToModify);
        if (userToModify != null) {
            userToModify.setNom("Smith");
            userToModify.setPrenom("Jane");
            userToModify.setEmailUser("jane.smith.java@test.com");

            service.modifier(userToModify);
        }

        System.out.println("\n===== LISTE DES UTILISATEURS (APRES MODIFICATION) =====");
        for (User user : service.recuperer()) {
            System.out.println(user);
        }

        // =========================
        // DELETE
        // =========================
        int idToDelete = u.getId();
        User userToDelete = service.findById(idToDelete);
        if (userToDelete != null) {
            service.supprimer(userToDelete);
        }

        System.out.println("\n===== LISTE DES UTILISATEURS (APRES SUPPRESSION) =====");
        for (User user : service.recuperer()) {
            System.out.println(user);
        }
    }
}