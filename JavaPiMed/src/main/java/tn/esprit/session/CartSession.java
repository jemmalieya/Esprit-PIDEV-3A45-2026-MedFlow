package tn.esprit.session;

import tn.esprit.entities.Produit;
import java.util.ArrayList;
import java.util.List;

public class CartSession {

    private static final List<Produit> panier = new ArrayList<>();

    public static void ajouterProduit(Produit produit) {
        panier.add(produit);
    }

    public static void supprimerProduit(Produit produit) {
        panier.remove(produit);
    }

    public static List<Produit> getPanier() {
        return panier;
    }

    public static void viderPanier() {
        panier.clear();
    }

    public static int getNombreArticles() {
        return panier.size();
    }
}