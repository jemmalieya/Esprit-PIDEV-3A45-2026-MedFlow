package tn.esprit.entities;

import java.util.Date;

public class Ressource {

    private int id;
    private String nom_ressource;
    private String categorie_ressource;
    private String type_ressource;
    private String chemin_fichier_ressource;
    private String mime_type_ressource;
    private int taille_kb_ressource;
    private String url_externe_ressource;
    private int quantite_disponible_ressource;
    private String unite_ressource;
    private String fournisseur_ressource;
    private double cout_estime_ressource;
    private boolean est_publique_ressource;
    private String notes_ressource;
    private Date date_creation_ressource;
    private Date date_mise_a_jour_ressource;
    private String cloudinary_public_id;
    private String signature_url;
    private String signature_public_id;

    // Replaced 'evenement_id' with Evenement object reference
    private Evenement evenement;

    // Constructor with Evenement object
    public Ressource() {}
    public Ressource(int id, String nom_ressource, String categorie_ressource, Evenement evenement) {
        this.id = id;
        this.nom_ressource = nom_ressource;
        this.categorie_ressource = categorie_ressource;
        this.evenement = evenement;  // Link the Ressource to an Evenement
    }

    // Getters and setters for 'evenement'
    public Evenement getEvenement() {
        return evenement;
    }

    public void setEvenement(Evenement evenement) {
        this.evenement = evenement;
    }

    // Getters and setters for other fields
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom_ressource() {
        return nom_ressource;
    }

    public void setNom_ressource(String nom_ressource) {
        this.nom_ressource = nom_ressource;
    }

    public String getCategorie_ressource() {
        return categorie_ressource;
    }

    public void setCategorie_ressource(String categorie_ressource) {
        this.categorie_ressource = categorie_ressource;
    }

    public String getType_ressource() {
        return type_ressource;
    }

    public void setType_ressource(String type_ressource) {
        this.type_ressource = type_ressource;
    }

    public String getChemin_fichier_ressource() {
        return chemin_fichier_ressource;
    }

    public void setChemin_fichier_ressource(String chemin_fichier_ressource) {
        this.chemin_fichier_ressource = chemin_fichier_ressource;
    }

    public String getMime_type_ressource() {
        return mime_type_ressource;
    }

    public void setMime_type_ressource(String mime_type_ressource) {
        this.mime_type_ressource = mime_type_ressource;
    }

    public int getTaille_kb_ressource() {
        return taille_kb_ressource;
    }

    public void setTaille_kb_ressource(int taille_kb_ressource) {
        this.taille_kb_ressource = taille_kb_ressource;
    }

    public String getUrl_externe_ressource() {
        return url_externe_ressource;
    }

    public void setUrl_externe_ressource(String url_externe_ressource) {
        this.url_externe_ressource = url_externe_ressource;
    }

    public int getQuantite_disponible_ressource() {
        return quantite_disponible_ressource;
    }

    public void setQuantite_disponible_ressource(int quantite_disponible_ressource) {
        this.quantite_disponible_ressource = quantite_disponible_ressource;
    }

    public String getUnite_ressource() {
        return unite_ressource;
    }

    public void setUnite_ressource(String unite_ressource) {
        this.unite_ressource = unite_ressource;
    }

    public String getFournisseur_ressource() {
        return fournisseur_ressource;
    }

    public void setFournisseur_ressource(String fournisseur_ressource) {
        this.fournisseur_ressource = fournisseur_ressource;
    }

    public double getCout_estime_ressource() {
        return cout_estime_ressource;
    }

    public void setCout_estime_ressource(double cout_estime_ressource) {
        this.cout_estime_ressource = cout_estime_ressource;
    }

    public boolean isEst_publique_ressource() {
        return est_publique_ressource;
    }

    public void setEst_publique_ressource(boolean est_publique_ressource) {
        this.est_publique_ressource = est_publique_ressource;
    }

    public String getNotes_ressource() {
        return notes_ressource;
    }

    public void setNotes_ressource(String notes_ressource) {
        this.notes_ressource = notes_ressource;
    }

    public Date getDate_creation_ressource() {
        return date_creation_ressource;
    }

    public void setDate_creation_ressource(Date date_creation_ressource) {
        this.date_creation_ressource = date_creation_ressource;
    }

    public Date getDate_mise_a_jour_ressource() {
        return date_mise_a_jour_ressource;
    }

    public void setDate_mise_a_jour_ressource(Date date_mise_a_jour_ressource) {
        this.date_mise_a_jour_ressource = date_mise_a_jour_ressource;
    }

    public String getCloudinary_public_id() {
        return cloudinary_public_id;
    }

    public void setCloudinary_public_id(String cloudinary_public_id) {
        this.cloudinary_public_id = cloudinary_public_id;
    }

    public String getSignature_url() {
        return signature_url;
    }

    public void setSignature_url(String signature_url) {
        this.signature_url = signature_url;
    }

    public String getSignature_public_id() {
        return signature_public_id;
    }

    public void setSignature_public_id(String signature_public_id) {
        this.signature_public_id = signature_public_id;
    }

    @Override
    public String toString() {
        return "Ressource{" +
                "id=" + id +
                ", nom_ressource='" + nom_ressource + '\'' +
                ", categorie_ressource='" + categorie_ressource + '\'' +
                ", type_ressource='" + type_ressource + '\'' +
                ", chemin_fichier_ressource='" + chemin_fichier_ressource + '\'' +
                ", mime_type_ressource='" + mime_type_ressource + '\'' +
                ", taille_kb_ressource=" + taille_kb_ressource +
                ", url_externe_ressource='" + url_externe_ressource + '\'' +
                ", quantite_disponible_ressource=" + quantite_disponible_ressource +
                ", unite_ressource='" + unite_ressource + '\'' +
                ", fournisseur_ressource='" + fournisseur_ressource + '\'' +
                ", cout_estime_ressource=" + cout_estime_ressource +
                ", est_publique_ressource=" + est_publique_ressource +
                ", notes_ressource='" + notes_ressource + '\'' +
                ", date_creation_ressource=" + date_creation_ressource +
                ", date_mise_a_jour_ressource=" + date_mise_a_jour_ressource +
                ", cloudinary_public_id='" + cloudinary_public_id + '\'' +
                ", signature_url='" + signature_url + '\'' +
                ", signature_public_id='" + signature_public_id + '\'' +
                ", evenement=" + (evenement != null ? evenement.getId() : "null") +  // Show Evenement ID or "null"
                '}';
    }
}