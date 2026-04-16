package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Ressource;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RessourceServiceTest {

    static RessourceService service;
    static EvenementService evenementService;
    static Evenement evenementDeTest;

    @BeforeAll
    static void setup() throws Exception {
        service = new RessourceService();
        evenementService = new EvenementService();


        Evenement ev = new Evenement();
        ev.setTitre_event("EvenementSupportRessourceJUnit");
        ev.setSlug_event("evenement-support-ressource-junit");
        ev.setType_event("Atelier");
        ev.setDescription_event("Événement support pour les tests de ressources JUnit.");
        ev.setObjectif_event("Support test ressource.");
        ev.setStatut_event("Brouillon");
        ev.setDate_debut_event(Date.valueOf("2025-10-01"));
        ev.setDate_fin_event(Date.valueOf("2025-10-05"));
        ev.setNom_lieu_event("Salle test ressource");
        ev.setAdresse_event("456 Rue test ressource");
        ev.setVille_event("Tunis");
        ev.setInscription_obligatoire_event(false);
        ev.setEmail_contact_event("ressource@junit.com");
        ev.setTel_contact_event("22345678");
        ev.setNom_organisateur_event("OrgRessourceJUnit");
        ev.setNb_participants_max_event(50);
        ev.setImage_couverture_event("https://example.com/ressource.jpg");
        ev.setVisibilite_event("Public");
        ev.setDate_creation_event(new java.util.Date());
        ev.setDate_mise_a_jour_event(new java.util.Date());

        evenementService.ajouter(ev);


        evenementDeTest = evenementService.recuperer().stream()
                .filter(e -> "EvenementSupportRessourceJUnit".equals(e.getTitre_event()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Événement support introuvable après insertion."));
    }

    @AfterEach
    void cleanUp() {
        List<Ressource> ressources = service.recuperer();
        for (Ressource r : ressources) {
            if ("RessourceTestJUnit".equals(r.getNom_ressource())
                    || "RessourceModifieeJUnit".equals(r.getNom_ressource())) {
                service.supprimer(r);
            }
        }
    }

    @AfterAll
    static void tearDown() {

        try {
            List<Evenement> evenements = evenementService.recuperer();
            evenements.stream()
                    .filter(e -> "EvenementSupportRessourceJUnit".equals(e.getTitre_event()))
                    .forEach(evenementService::supprimer);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private Ressource buildTestRessource(String nom) {
        Ressource r = new Ressource();
        r.setEvenement(evenementDeTest);
        r.setNom_ressource(nom);
        r.setCategorie_ressource("Matériel");
        r.setType_ressource("stock_item");
        r.setChemin_fichier_ressource("");
        r.setMime_type_ressource("");
        r.setTaille_kb_ressource(0);
        r.setUrl_externe_ressource("");
        r.setQuantite_disponible_ressource(10);
        r.setUnite_ressource("unités");
        r.setFournisseur_ressource("FournisseurTest");
        r.setCout_estime_ressource(50.0);
        r.setEst_publique_ressource(true);
        r.setNotes_ressource("Notes de test JUnit pour la ressource.");
        r.setDate_creation_ressource(new java.util.Date());
        r.setDate_mise_a_jour_ressource(new java.util.Date());
        return r;
    }


    @Test
    @Order(1)
    void testAjouterRessource() {
        Ressource r = buildTestRessource("RessourceTestJUnit");
        service.ajouter(r);

        List<Ressource> ressources = service.recuperer();
        assertFalse(ressources.isEmpty(),
                "La liste des ressources ne doit pas être vide après ajout.");

        boolean trouve = ressources.stream()
                .anyMatch(res -> "RessourceTestJUnit".equals(res.getNom_ressource()));
        assertTrue(trouve, "La ressource ajoutée doit exister dans la base.");
    }


    @Test
    @Order(2)
    void testModifierRessource() {
        Ressource r = buildTestRessource("RessourceTestJUnit");
        service.ajouter(r);

        List<Ressource> ressources = service.recuperer();
        Ressource ressourceAjoutee = ressources.stream()
                .filter(res -> "RessourceTestJUnit".equals(res.getNom_ressource()))
                .findFirst()
                .orElse(null);

        assertNotNull(ressourceAjoutee,
                "La ressource à modifier doit exister avant la modification.");

        ressourceAjoutee.setNom_ressource("RessourceModifieeJUnit");
        ressourceAjoutee.setCategorie_ressource("Équipement");
        ressourceAjoutee.setQuantite_disponible_ressource(25);
        ressourceAjoutee.setFournisseur_ressource("FournisseurModifie");
        ressourceAjoutee.setCout_estime_ressource(99.99);
        ressourceAjoutee.setNotes_ressource("Notes modifiées pour le test JUnit.");
        ressourceAjoutee.setDate_mise_a_jour_ressource(new java.util.Date());

        service.modifier(ressourceAjoutee);

        List<Ressource> ressourcesMaj = service.recuperer();
        boolean trouve = ressourcesMaj.stream()
                .anyMatch(res -> "RessourceModifieeJUnit".equals(res.getNom_ressource()));
        assertTrue(trouve,
                "La ressource modifiée doit exister avec son nouveau nom.");
    }


    @Test
    @Order(3)
    void testSupprimerRessource() {
        Ressource r = buildTestRessource("RessourceTestJUnit");
        service.ajouter(r);

        List<Ressource> ressources = service.recuperer();
        Ressource ressourceAjoutee = ressources.stream()
                .filter(res -> "RessourceTestJUnit".equals(res.getNom_ressource()))
                .findFirst()
                .orElse(null);

        assertNotNull(ressourceAjoutee,
                "La ressource à supprimer doit exister avant la suppression.");

        service.supprimer(ressourceAjoutee);

        List<Ressource> ressourcesApresSuppression = service.recuperer();
        boolean existe = ressourcesApresSuppression.stream()
                .anyMatch(res -> res.getId() == ressourceAjoutee.getId());
        assertFalse(existe,
                "La ressource supprimée ne doit plus exister dans la base.");
    }


    @Test
    @Order(4)
    void testRecupererRessources() {
        List<Ressource> ressources = service.recuperer();
        assertNotNull(ressources,
                "La liste récupérée ne doit pas être null.");
    }


    @Test
    @Order(5)
    void testRessourceExisteDeja() throws Exception {
        Ressource r = buildTestRessource("RessourceTestJUnit");
        service.ajouter(r);

        boolean existe = service.ressourceExisteDeja(
                evenementDeTest.getId(),
                "RessourceTestJUnit",
                "stock_item"
        );
        assertTrue(existe,
                "La méthode doit détecter qu'une ressource avec ce nom, ce type et cet événement existe déjà.");
    }
}