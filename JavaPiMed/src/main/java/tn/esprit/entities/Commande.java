package tn.esprit.entities;

import java.time.LocalDateTime;
import java.util.List;

public class Commande {
    private int id_commande;
    private User user;
    private LocalDateTime date_creation_commande;
    private String statut_commande;
    private String stripe_session_id;
    private LocalDateTime paid_at;
    private int montant_total_cents;

    private List<CommandeProduit> commande_produits;

    public Commande() {
    }

    public Commande(User user, LocalDateTime date_creation_commande, String statut_commande,
                    String stripe_session_id, LocalDateTime paid_at, int montant_total_cents) {
        this.user = user;
        this.date_creation_commande = date_creation_commande;
        this.statut_commande = statut_commande;
        this.stripe_session_id = stripe_session_id;
        this.paid_at = paid_at;
        this.montant_total_cents = montant_total_cents;
    }

    public Commande(int id_commande, User user, LocalDateTime date_creation_commande, String statut_commande,
                    String stripe_session_id, LocalDateTime paid_at, int montant_total_cents) {
        this.id_commande = id_commande;
        this.user = user;
        this.date_creation_commande = date_creation_commande;
        this.statut_commande = statut_commande;
        this.stripe_session_id = stripe_session_id;
        this.paid_at = paid_at;
        this.montant_total_cents = montant_total_cents;
    }

    public int getId_commande() {
        return id_commande;
    }

    public void setId_commande(int id_commande) {
        this.id_commande = id_commande;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getDate_creation_commande() {
        return date_creation_commande;
    }

    public void setDate_creation_commande(LocalDateTime date_creation_commande) {
        this.date_creation_commande = date_creation_commande;
    }

    public String getStatut_commande() {
        return statut_commande;
    }

    public void setStatut_commande(String statut_commande) {
        this.statut_commande = statut_commande;
    }

    public String getStripe_session_id() {
        return stripe_session_id;
    }

    public void setStripe_session_id(String stripe_session_id) {
        this.stripe_session_id = stripe_session_id;
    }

    public LocalDateTime getPaid_at() {
        return paid_at;
    }

    public void setPaid_at(LocalDateTime paid_at) {
        this.paid_at = paid_at;
    }

    public int getMontant_total_cents() {
        return montant_total_cents;
    }

    public void setMontant_total_cents(int montant_total_cents) {
        this.montant_total_cents = montant_total_cents;
    }

    public List<CommandeProduit> getCommande_produits() {
        return commande_produits;
    }

    public void setCommande_produits(List<CommandeProduit> commande_produits) {
        this.commande_produits = commande_produits;
    }

    @Override
    public String toString() {
        return "Commande{" +
                "id_commande=" + id_commande +
                ", user_id=" + (user != null ? user.getId() : null) +
                ", date_creation_commande=" + date_creation_commande +
                ", statut_commande='" + statut_commande + '\'' +
                ", stripe_session_id='" + stripe_session_id + '\'' +
                ", paid_at=" + paid_at +
                ", montant_total_cents=" + montant_total_cents +
                ", nombre_commande_produits=" + (commande_produits != null ? commande_produits.size() : 0) +
                '}';
    }
}