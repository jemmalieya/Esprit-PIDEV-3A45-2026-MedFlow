package tn.esprit.session;

import tn.esprit.entities.Produit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CartSession {

    private static final List<Produit> panier = new ArrayList<>();

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