package tn.esprit.entities;

import java.util.List;

public class Produit {
    private int id_produit;
    private String nom_produit;
    private String description_produit;
    private double prix_produit;
    private int quantite_produit;
    private String image_produit;
    private String categorie_produit;
    private String status_produit;

    private List<CommandeProduit> commande_produits;

    public Produit() {
    }

    public Produit(String nom_produit, String description_produit, double prix_produit,
                   int quantite_produit, String image_produit,
                   String categorie_produit, String status_produit) {
        this.nom_produit = nom_produit;
        this.description_produit = description_produit;
        this.prix_produit = prix_produit;
        this.quantite_produit = quantite_produit;
        this.image_produit = image_produit;
        this.categorie_produit = categorie_produit;
        this.status_produit = status_produit;
    }

    public Produit(int id_produit, String nom_produit, String description_produit, double prix_produit,
                   int quantite_produit, String image_produit,
                   String categorie_produit, String status_produit) {
        this.id_produit = id_produit;
        this.nom_produit = nom_produit;
        this.description_produit = description_produit;
        this.prix_produit = prix_produit;
        this.quantite_produit = quantite_produit;
        this.image_produit = image_produit;
        this.categorie_produit = categorie_produit;
        this.status_produit = status_produit;
    }

    public Produit(int produitId) {
    }

    public int getId_produit() {
        return id_produit;
    }

    public void setId_produit(int id_produit) {
        this.id_produit = id_produit;
    }

    public String getNom_produit() {
        return nom_produit;
    }

    public void setNom_produit(String nom_produit) {
        this.nom_produit = nom_produit;
    }

    public String getDescription_produit() {
        return description_produit;
    }

    public void setDescription_produit(String description_produit) {
        this.description_produit = description_produit;
    }

    public double getPrix_produit() {
        return prix_produit;
    }

    public void setPrix_produit(double prix_produit) {
        this.prix_produit = prix_produit;
    }

    public int getQuantite_produit() {
        return quantite_produit;
    }

    public void setQuantite_produit(int quantite_produit) {
        this.quantite_produit = quantite_produit;
    }

    public String getImage_produit() {
        return image_produit;
    }

    public void setImage_produit(String image_produit) {
        this.image_produit = image_produit;
    }

    public String getCategorie_produit() {
        return categorie_produit;
    }

    public void setCategorie_produit(String categorie_produit) {
        this.categorie_produit = categorie_produit;
    }

    public String getStatus_produit() {
        return status_produit;
    }

    public void setStatus_produit(String status_produit) {
        this.status_produit = status_produit;
    }

    public List<CommandeProduit> getCommande_produits() {
        return commande_produits;
    }

    public void setCommande_produits(List<CommandeProduit> commande_produits) {
        this.commande_produits = commande_produits;
    }

    @Override
    public String toString() {
        return "Produit{" +
                "id_produit=" + id_produit +
                ", nom_produit='" + nom_produit + '\'' +
                ", description_produit='" + description_produit + '\'' +
                ", prix_produit=" + prix_produit +
                ", quantite_produit=" + quantite_produit +
                ", image_produit='" + image_produit + '\'' +
                ", categorie_produit='" + categorie_produit + '\'' +
                ", status_produit='" + status_produit + '\'' +
                ", nombre_commande_produits=" + (commande_produits != null ? commande_produits.size() : 0) +
                '}';
    }
}