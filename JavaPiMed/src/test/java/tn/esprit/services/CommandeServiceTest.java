package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.entities.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommandeServiceTest {

    static CommandeService service;
    static ProduitService produitService;

    // ⚠ mets ici un ID user qui existe vraiment dans ta base
    static final int ID_USER_TEST = 1;

    @BeforeAll
    static void setup() {
        service = new CommandeService();
        produitService = new ProduitService();
    }

    @AfterEach
    void cleanUp() {
        List<Commande> commandes = service.recuperer();

        for (Commande c : commandes) {
            if (c.getStatut_commande() != null &&
                    (c.getStatut_commande().equals("TestCommandeJUnit")
                            || c.getStatut_commande().equals("CommandeModifieeJUnit"))) {
                service.supprimer(c);
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterCommande() {
        List<Produit> produits = produitService.recuperer();
        assertFalse(produits.isEmpty(), "Il faut au moins un produit existant dans la base pour tester la commande.");

        Produit produit = produits.get(0);

        CommandeProduit ligne = new CommandeProduit();
        ligne.setProduit(produit);
        ligne.setQuantite_commandee(1);

        List<CommandeProduit> lignes = new ArrayList<>();
        lignes.add(ligne);

        User user = new User();
        user.setId(ID_USER_TEST);

        Commande commande = new Commande();
        commande.setUser(user);
        commande.setDate_creation_commande(LocalDateTime.now());
        commande.setStatut_commande("TestCommandeJUnit");
        commande.setStripe_session_id(null);
        commande.setPaid_at(null);
        commande.setCommande_produits(lignes);

        service.ajouter(commande);

        List<Commande> commandes = service.recuperer();

        assertFalse(commandes.isEmpty(), "La liste des commandes ne doit pas être vide après ajout.");

        boolean trouve = commandes.stream()
                .anyMatch(c -> "TestCommandeJUnit".equals(c.getStatut_commande()));

        assertTrue(trouve, "La commande ajoutée doit exister dans la base.");
    }

    @Test
    @Order(2)
    void testModifierCommande() {
        List<Produit> produits = produitService.recuperer();
        assertFalse(produits.isEmpty(), "Il faut au moins un produit existant dans la base pour tester la modification.");

        Produit produit = produits.get(0);

        CommandeProduit ligne = new CommandeProduit();
        ligne.setProduit(produit);
        ligne.setQuantite_commandee(1);

        List<CommandeProduit> lignes = new ArrayList<>();
        lignes.add(ligne);

        User user = new User();
        user.setId(ID_USER_TEST);

        Commande commande = new Commande();
        commande.setUser(user);
        commande.setDate_creation_commande(LocalDateTime.now());
        commande.setStatut_commande("TestCommandeJUnit");
        commande.setStripe_session_id(null);
        commande.setPaid_at(null);
        commande.setCommande_produits(lignes);

        service.ajouter(commande);

        List<Commande> commandes = service.recuperer();
        Commande commandeAjoutee = commandes.stream()
                .filter(c -> "TestCommandeJUnit".equals(c.getStatut_commande()))
                .findFirst()
                .orElse(null);

        assertNotNull(commandeAjoutee, "La commande à modifier doit exister.");

        commandeAjoutee.setStatut_commande("CommandeModifieeJUnit");

        service.modifier(commandeAjoutee);

        List<Commande> commandesMaj = service.recuperer();

        boolean trouve = commandesMaj.stream()
                .anyMatch(c -> "CommandeModifieeJUnit".equals(c.getStatut_commande()));

        assertTrue(trouve, "La commande modifiée doit exister avec son nouveau statut.");
    }

    @Test
    @Order(3)
    void testSupprimerCommande() {
        List<Produit> produits = produitService.recuperer();
        assertFalse(produits.isEmpty(), "Il faut au moins un produit existant dans la base pour tester la suppression.");

        Produit produit = produits.get(0);

        CommandeProduit ligne = new CommandeProduit();
        ligne.setProduit(produit);
        ligne.setQuantite_commandee(1);

        List<CommandeProduit> lignes = new ArrayList<>();
        lignes.add(ligne);

        User user = new User();
        user.setId(ID_USER_TEST);

        Commande commande = new Commande();
        commande.setUser(user);
        commande.setDate_creation_commande(LocalDateTime.now());
        commande.setStatut_commande("TestCommandeJUnit");
        commande.setStripe_session_id(null);
        commande.setPaid_at(null);
        commande.setCommande_produits(lignes);

        service.ajouter(commande);

        List<Commande> commandes = service.recuperer();
        Commande commandeAjoutee = commandes.stream()
                .filter(c -> "TestCommandeJUnit".equals(c.getStatut_commande()))
                .findFirst()
                .orElse(null);

        assertNotNull(commandeAjoutee, "La commande à supprimer doit exister.");

        service.supprimer(commandeAjoutee);

        List<Commande> commandesApresSuppression = service.recuperer();

        boolean existe = commandesApresSuppression.stream()
                .anyMatch(c -> c.getId_commande() == commandeAjoutee.getId_commande());

        assertFalse(existe, "La commande supprimée ne doit plus exister dans la base.");
    }
}