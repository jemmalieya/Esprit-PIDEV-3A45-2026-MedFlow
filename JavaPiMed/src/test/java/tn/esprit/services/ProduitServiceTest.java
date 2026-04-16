package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Produit;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProduitServiceTest {

    static ProduitService service;

    @BeforeAll
    static void setup() {
        service = new ProduitService();
    }

    @AfterEach
    void cleanUp() {
        List<Produit> produits = service.recuperer();

        for (Produit p : produits) {
            if ("ProduitTestJUnit".equals(p.getNom_produit())
                    || "ProduitModifieJUnit".equals(p.getNom_produit())) {
                service.supprimer(p);
            }
        }
    }

    @Test
    @Order(1)
    void testAjouterProduit() {
        Produit p = new Produit(
                "ProduitTestJUnit",
                "Description test junit",
                12.5,
                10,
                "test.jpg",
                "Médicament",
                "Disponible"
        );

        service.ajouter(p);

        List<Produit> produits = service.recuperer();

        assertFalse(produits.isEmpty(), "La liste des produits ne doit pas être vide après ajout.");

        boolean trouve = produits.stream()
                .anyMatch(prod -> "ProduitTestJUnit".equals(prod.getNom_produit()));

        assertTrue(trouve, "Le produit ajouté doit exister dans la base.");
    }

    @Test
    @Order(2)
    void testModifierProduit() {
        Produit p = new Produit(
                "ProduitTestJUnit",
                "Description test junit",
                12.5,
                10,
                "test.jpg",
                "Médicament",
                "Disponible"
        );

        service.ajouter(p);

        List<Produit> produits = service.recuperer();

        Produit produitAjoute = produits.stream()
                .filter(prod -> "ProduitTestJUnit".equals(prod.getNom_produit()))
                .findFirst()
                .orElse(null);

        assertNotNull(produitAjoute, "Le produit à modifier doit exister avant la modification.");

        produitAjoute.setNom_produit("ProduitModifieJUnit");
        produitAjoute.setDescription_produit("Description modifiée");
        produitAjoute.setPrix_produit(20.0);
        produitAjoute.setQuantite_produit(5);
        produitAjoute.setCategorie_produit("Parapharmacie");
        produitAjoute.setStatus_produit("Indisponible");

        service.modifier(produitAjoute);

        List<Produit> produitsMaj = service.recuperer();

        boolean trouve = produitsMaj.stream()
                .anyMatch(prod -> "ProduitModifieJUnit".equals(prod.getNom_produit()));

        assertTrue(trouve, "Le produit modifié doit exister avec son nouveau nom.");
    }

    @Test
    @Order(3)
    void testSupprimerProduit() {
        Produit p = new Produit(
                "ProduitTestJUnit",
                "Description test junit",
                12.5,
                10,
                "test.jpg",
                "Médicament",
                "Disponible"
        );

        service.ajouter(p);

        List<Produit> produits = service.recuperer();

        Produit produitAjoute = produits.stream()
                .filter(prod -> "ProduitTestJUnit".equals(prod.getNom_produit()))
                .findFirst()
                .orElse(null);

        assertNotNull(produitAjoute, "Le produit à supprimer doit exister avant la suppression.");

        service.supprimer(produitAjoute);

        List<Produit> produitsApresSuppression = service.recuperer();

        boolean existe = produitsApresSuppression.stream()
                .anyMatch(prod -> prod.getId_produit() == produitAjoute.getId_produit());

        assertFalse(existe, "Le produit supprimé ne doit plus exister dans la base.");
    }
}