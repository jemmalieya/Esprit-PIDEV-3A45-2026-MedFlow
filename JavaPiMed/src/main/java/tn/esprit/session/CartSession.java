package tn.esprit.session;

import tn.esprit.entities.Produit;
import tn.esprit.services.OpenFdaInteractionService;
import tn.esprit.services.DrugInteractionResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CartSession {

    private static final List<Produit> panier = new ArrayList<>();
    private static final OpenFdaInteractionService openFdaService = new OpenFdaInteractionService();

    public static void ajouterProduit(Produit produit) {
        panier.add(produit);

    }

    public static void supprimerProduit(Produit produit) {
        supprimerToutesOccurrences(produit);

    }

    public static void supprimerUneOccurrence(Produit produit) {
        Iterator<Produit> iterator = panier.iterator();
        while (iterator.hasNext()) {
            Produit p = iterator.next();
            if (p.getId_produit() == produit.getId_produit()) {
                iterator.remove();
                break;
            }
        }

    }

    public static void supprimerToutesOccurrences(Produit produit) {
        Iterator<Produit> iterator = panier.iterator();
        while (iterator.hasNext()) {
            Produit p = iterator.next();
            if (p.getId_produit() == produit.getId_produit()) {
                iterator.remove();
            }
        }

    }

    // Vérifie les interactions médicamenteuses
    private static void verifierInteractions() {
        List<DrugInteractionResult> interactions = openFdaService.verifierInteractions(panier);

        if (!interactions.isEmpty()) {
            // Si des interactions dangereuses sont détectées, gère les alertes
            System.out.println("Interactions dangereuses détectées :");
            for (DrugInteractionResult interaction : interactions) {
                System.out.println("Interaction entre " + interaction.getMedicamentA() + " et " + interaction.getMedicamentB());
                System.out.println("Détail : " + interaction.getDetailInteraction());
            }
        } else {
            System.out.println("Aucune interaction dangereuse détectée.");
        }
    }

    public static int getQuantiteProduit(Produit produit) {
        int count = 0;
        for (Produit p : panier) {
            if (p.getId_produit() == produit.getId_produit()) {
                count++;
            }
        }
        return count;
    }

    public static List<Produit> getPanier() {
        return new ArrayList<>(panier);
    }

    public static void viderPanier() {
        panier.clear();

    }

    public static int getNombreArticles() {
        return panier.size();
    }
}