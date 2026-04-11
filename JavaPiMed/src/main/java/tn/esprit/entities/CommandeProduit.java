package tn.esprit.entities;

public class CommandeProduit {
    private int id_ligne_commande;
    private int quantite_commandee;

    private Commande commande;
    private Produit produit;

    public CommandeProduit() {
    }

    public CommandeProduit(int quantite_commandee, Commande commande, Produit produit) {
        this.quantite_commandee = quantite_commandee;
        this.commande = commande;
        this.produit = produit;
    }

    public CommandeProduit(int id_ligne_commande, int quantite_commandee, Commande commande, Produit produit) {
        this.id_ligne_commande = id_ligne_commande;
        this.quantite_commandee = quantite_commandee;
        this.commande = commande;
        this.produit = produit;
    }

    public int getId_ligne_commande() {
        return id_ligne_commande;
    }

    public void setId_ligne_commande(int id_ligne_commande) {
        this.id_ligne_commande = id_ligne_commande;
    }

    public int getQuantite_commandee() {
        return quantite_commandee;
    }

    public void setQuantite_commandee(int quantite_commandee) {
        this.quantite_commandee = quantite_commandee;
    }

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    @Override
    public String toString() {
        return "CommandeProduit{" +
                "id_ligne_commande=" + id_ligne_commande +
                ", quantite_commandee=" + quantite_commandee +
                ", commande_id=" + (commande != null ? commande.getId_commande() : null) +
                ", produit_id=" + (produit != null ? produit.getId_produit() : null) +
                '}';
    }
}