package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Evenement;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EvenementServiceTest {

    static EvenementService service;

    @BeforeAll
    static void setup() {
        service = new EvenementService();
    }

    @AfterEach
    void cleanUp() {
        try {
            List<Evenement> evenements = service.recuperer();
            for (Evenement e : evenements) {
                if ("EvenementTestJUnit".equals(e.getTitre_event())
                        || "EvenementModifieJUnit".equals(e.getTitre_event())) {
                    service.supprimer(e);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private Evenement buildTestEvenement(String titre) {
        Evenement ev = new Evenement();
        ev.setTitre_event(titre);
        ev.setSlug_event("evenement-test-junit");
        ev.setType_event("Conférence");
        ev.setDescription_event("Description de test JUnit pour les tests unitaires.");
        ev.setObjectif_event("Objectif de test JUnit.");
        ev.setStatut_event("Brouillon");
        ev.setDate_debut_event(Date.valueOf("2025-09-01"));
        ev.setDate_fin_event(Date.valueOf("2025-09-05"));
        ev.setNom_lieu_event("Salle de test");
        ev.setAdresse_event("123 Rue de test");
        ev.setVille_event("Tunis");
        ev.setInscription_obligatoire_event(false);
        ev.setEmail_contact_event("test@junit.com");
        ev.setTel_contact_event("21234567");
        ev.setNom_organisateur_event("OrgTestJUnit");
        ev.setNb_participants_max_event(100);
        ev.setDate_limite_inscription_event(Date.valueOf("2025-08-25"));
        ev.setImage_couverture_event("https://example.com/test.jpg");
        ev.setVisibilite_event("Public");
        ev.setDate_creation_event(new java.util.Date());
        ev.setDate_mise_a_jour_event(new java.util.Date());
        return ev;
    }


    @Test
    @Order(1)
    void testAjouterEvenement() throws Exception {
        Evenement ev = buildTestEvenement("EvenementTestJUnit");
        service.ajouter(ev);

        List<Evenement> evenements = service.recuperer();
        assertFalse(evenements.isEmpty(),
                "La liste des événements ne doit pas être vide après ajout.");

        boolean trouve = evenements.stream()
                .anyMatch(e -> "EvenementTestJUnit".equals(e.getTitre_event()));
        assertTrue(trouve, "L'événement ajouté doit exister dans la base.");
    }


    @Test
    @Order(2)
    void testModifierEvenement() throws Exception {
        Evenement ev = buildTestEvenement("EvenementTestJUnit");
        service.ajouter(ev);

        List<Evenement> evenements = service.recuperer();
        Evenement evenementAjoute = evenements.stream()
                .filter(e -> "EvenementTestJUnit".equals(e.getTitre_event()))
                .findFirst()
                .orElse(null);

        assertNotNull(evenementAjoute,
                "L'événement à modifier doit exister avant la modification.");

        evenementAjoute.setTitre_event("EvenementModifieJUnit");
        evenementAjoute.setDescription_event("Description modifiée pour le test JUnit.");
        evenementAjoute.setVille_event("Sfax");
        evenementAjoute.setStatut_event("Publié");
        evenementAjoute.setDate_mise_a_jour_event(new java.util.Date());

        service.modifier(evenementAjoute);

        List<Evenement> evenementsMaj = service.recuperer();
        boolean trouve = evenementsMaj.stream()
                .anyMatch(e -> "EvenementModifieJUnit".equals(e.getTitre_event()));
        assertTrue(trouve,
                "L'événement modifié doit exister avec son nouveau titre.");
    }


    @Test
    @Order(3)
    void testSupprimerEvenement() throws Exception {
        Evenement ev = buildTestEvenement("EvenementTestJUnit");
        service.ajouter(ev);

        List<Evenement> evenements = service.recuperer();
        Evenement evenementAjoute = evenements.stream()
                .filter(e -> "EvenementTestJUnit".equals(e.getTitre_event()))
                .findFirst()
                .orElse(null);

        assertNotNull(evenementAjoute,
                "L'événement à supprimer doit exister avant la suppression.");

        service.supprimer(evenementAjoute);

        List<Evenement> evenementsApresSuppression = service.recuperer();
        boolean existe = evenementsApresSuppression.stream()
                .anyMatch(e -> e.getId() == evenementAjoute.getId());
        assertFalse(existe,
                "L'événement supprimé ne doit plus exister dans la base.");
    }


    @Test
    @Order(4)
    void testRecupererEvenements() throws Exception {
        List<Evenement> evenements = service.recuperer();
        assertNotNull(evenements,
                "La liste récupérée ne doit pas être null.");
    }


    @Test
    @Order(5)
    void testEvenementExisteDeja() throws Exception {
        Evenement ev = buildTestEvenement("EvenementTestJUnit");
        service.ajouter(ev);

        boolean existe = service.evenementExisteDeja("EvenementTestJUnit");
        assertTrue(existe,
                "La méthode doit détecter qu'un événement avec ce titre existe déjà.");
    }
}