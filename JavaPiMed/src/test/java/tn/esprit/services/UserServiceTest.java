package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.entities.User;

import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserServiceTest {

    static UserService service;

    private static final String EMAIL_PREFIX = "autotest_user_";
    private static final String EMAIL_DOMAIN = "@junit-local.test";
    private static final String CIN_PREFIX = "CJ";

    @BeforeAll
    static void setup() {
        service = new UserService();
    }

    @AfterEach
    void cleanUp() {
        List<User> users = service.recuperer();

        for (User user : users) {
            String email = user.getEmailUser();
            String cin = user.getCin();
            if ((email != null && email.startsWith(EMAIL_PREFIX) && email.endsWith(EMAIL_DOMAIN))
                    || (cin != null && cin.startsWith(CIN_PREFIX))) {
                service.supprimer(user);
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterUser() {
        String token = uniqueToken();
        User user = buildTestUser(token);

        service.ajouter(user);

        List<User> users = service.recuperer();
        assertFalse(users.isEmpty(), "La liste des utilisateurs ne doit pas etre vide apres ajout.");

        boolean trouve = users.stream()
                .anyMatch(u -> user.getEmailUser().equals(u.getEmailUser()));

        assertTrue(trouve, "L'utilisateur ajoute doit exister dans la base.");
    }

    @Test
    @Order(2)
    void testModifierUser() {
        String token = uniqueToken();
        User user = buildTestUser(token);
        service.ajouter(user);

        List<User> users = service.recuperer();
        User userAjoute = users.stream()
                .filter(u -> user.getEmailUser().equals(u.getEmailUser()))
                .findFirst()
                .orElse(null);

        assertNotNull(userAjoute, "L'utilisateur a modifier doit exister avant la modification.");

        String modifiedToken = uniqueToken();
        userAjoute.setCin(uniqueCin());
        userAjoute.setNom("UserModifieJUnit");
        userAjoute.setPrenom("PrenomModifieJUnit");
        userAjoute.setTelephoneUser("22000000");
        userAjoute.setEmailUser(EMAIL_PREFIX + "mod_" + modifiedToken + EMAIL_DOMAIN);
        userAjoute.setAdresseUser("Adresse modifiee");
        userAjoute.setPassword("Password123!");
        userAjoute.setStatutCompte("inactif");
        userAjoute.setRoleSysteme("ROLE_PATIENT");

        service.modifier(userAjoute);

        List<User> usersMaj = service.recuperer();
        boolean trouve = usersMaj.stream()
                .anyMatch(u -> userAjoute.getEmailUser().equals(u.getEmailUser())
                        && "UserModifieJUnit".equals(u.getNom()));

        assertTrue(trouve, "L'utilisateur modifie doit exister avec son nouvel email et nom.");
    }

    @Test
    @Order(3)
    void testSupprimerUser() {
        String token = uniqueToken();
        User user = buildTestUser(token);
        service.ajouter(user);

        List<User> users = service.recuperer();
        User userAjoute = users.stream()
                .filter(u -> user.getEmailUser().equals(u.getEmailUser()))
                .findFirst()
                .orElse(null);

        assertNotNull(userAjoute, "L'utilisateur a supprimer doit exister avant la suppression.");

        service.supprimer(userAjoute);

        List<User> usersApresSuppression = service.recuperer();
        boolean existe = usersApresSuppression.stream()
                .anyMatch(u -> u.getId() == userAjoute.getId());

        assertFalse(existe, "L'utilisateur supprime ne doit plus exister dans la base.");
    }

    @Test
    @Order(4)
    void testExistsByEmailEtCin() {
        String token = uniqueToken();
        User user = buildTestUser(token);
        service.ajouter(user);

        assertTrue(service.existsByEmail(user.getEmailUser()), "existsByEmail doit retourner true pour l'email ajoute.");
        assertTrue(service.existsByCin(user.getCin()), "existsByCin doit retourner true pour le CIN ajoute.");

        assertFalse(service.existsByEmail(EMAIL_PREFIX + "absent_" + uniqueToken() + EMAIL_DOMAIN),
                "existsByEmail doit retourner false pour un email absent.");
        assertFalse(service.existsByCin(uniqueCin()),
                "existsByCin doit retourner false pour un CIN absent.");
    }

    @Test
    @Order(5)
    void testAjouterAvecRetourEmailDejaExistant() {
        String token = uniqueToken();
        User premierUser = buildTestUser(token);
        service.ajouter(premierUser);

        User deuxiemeUser = buildTestUser(uniqueToken());
        deuxiemeUser.setEmailUser(premierUser.getEmailUser());

        String erreur = service.ajouterAvecRetour(deuxiemeUser);

        assertNotNull(erreur, "ajouterAvecRetour doit retourner un message d'erreur pour email duplique.");
        String erreurNormalisee = erreur.toLowerCase(Locale.ROOT);
        assertTrue(erreurNormalisee.contains("email") || erreurNormalisee.contains("e-mail"),
                "Le message d'erreur doit mentionner l'email.");
    }

    @Test
    @Order(6)
    void testAjouterAvecRetourCinDejaExistant() {
        String token = uniqueToken();
        User premierUser = buildTestUser(token);
        service.ajouter(premierUser);

        User deuxiemeUser = buildTestUser(uniqueToken());
        deuxiemeUser.setCin(premierUser.getCin());

        String erreur = service.ajouterAvecRetour(deuxiemeUser);

        assertNotNull(erreur, "ajouterAvecRetour doit retourner un message d'erreur pour CIN duplique.");
        assertTrue(erreur.toLowerCase().contains("cin"),
                "Le message d'erreur doit mentionner le CIN.");
    }

    private User buildTestUser(String token) {
        User user = new User();
        user.setCin(uniqueCin());
        user.setProfilePicture("avatar-test.png");
        user.setNom("UserTestJUnit");
        user.setPrenom("PrenomTestJUnit");
        user.setDateNaissance(LocalDate.of(1995, 1, 1));
        user.setTelephoneUser("21000000");
        user.setEmailUser(EMAIL_PREFIX + token + EMAIL_DOMAIN);
        user.setAdresseUser("Adresse test JUnit");
        user.setPassword("Password123!");
        user.setVerified(false);
        user.setStatutCompte("actif");
        user.setRoleSysteme("ROLE_PATIENT");
        return user;
    }

    private String uniqueToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String uniqueCin() {
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
        return CIN_PREFIX + uuidPart;
    }
}