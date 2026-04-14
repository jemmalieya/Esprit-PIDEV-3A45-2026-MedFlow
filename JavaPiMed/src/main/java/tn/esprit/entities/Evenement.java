package tn.esprit.entities;

import java.util.Date;
import java.util.List;

public class Evenement {

    private int id;
    private String demandes_json;
    private String titre_event;
    private String slug_event;
    private String type_event;
    private String description_event;
    private String objectif_event;
    private String statut_event;
    private Date date_debut_event;
    private Date date_fin_event;
    private String nom_lieu_event;
    private String adresse_event;
    private String ville_event;
    private int nb_participants_max_event;
    private boolean inscription_obligatoire_event;
    private Date date_limite_inscription_event;
    private String email_contact_event;
    private String tel_contact_event;
    private String nom_organisateur_event;
    private String image_couverture_event;
    private String visibilite_event;
    private Date date_creation_event;
    private Date date_mise_a_jour_event;

    // Remove 'user_id' and just use 'User' object
    private User user;

    // List of associated Ressources for the Evenement
    private List<Ressource> ressources;

    public Evenement() {}

    // Constructor with User and List of Ressources
    public Evenement(int id, String titre_event, User user, List<Ressource> ressources) {
        this.id = id;
        this.titre_event = titre_event;
        this.user = user;
        this.ressources = ressources;
    }

    // Getters and setters for 'user' and 'ressources'
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Ressource> getRessources() {
        return ressources;
    }

    public void setRessources(List<Ressource> ressources) {
        this.ressources = ressources;
    }

    // Getters and setters for other fields
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDemandes_json() {
        return demandes_json;
    }

    public void setDemandes_json(String demandes_json) {
        this.demandes_json = demandes_json;
    }

    public String getTitre_event() {
        return titre_event;
    }

    public void setTitre_event(String titre_event) {
        this.titre_event = titre_event;
    }

    public String getSlug_event() {
        return slug_event;
    }

    public void setSlug_event(String slug_event) {
        this.slug_event = slug_event;
    }

    public String getType_event() {
        return type_event;
    }

    public void setType_event(String type_event) {
        this.type_event = type_event;
    }

    public String getDescription_event() {
        return description_event;
    }

    public void setDescription_event(String description_event) {
        this.description_event = description_event;
    }

    public String getObjectif_event() {
        return objectif_event;
    }

    public void setObjectif_event(String objectif_event) {
        this.objectif_event = objectif_event;
    }

    public String getStatut_event() {
        return statut_event;
    }

    public void setStatut_event(String statut_event) {
        this.statut_event = statut_event;
    }

    public Date getDate_debut_event() {
        return date_debut_event;
    }

    public void setDate_debut_event(Date date_debut_event) {
        this.date_debut_event = date_debut_event;
    }

    public Date getDate_fin_event() {
        return date_fin_event;
    }

    public void setDate_fin_event(Date date_fin_event) {
        this.date_fin_event = date_fin_event;
    }

    public String getNom_lieu_event() {
        return nom_lieu_event;
    }

    public void setNom_lieu_event(String nom_lieu_event) {
        this.nom_lieu_event = nom_lieu_event;
    }

    public String getAdresse_event() {
        return adresse_event;
    }

    public void setAdresse_event(String adresse_event) {
        this.adresse_event = adresse_event;
    }

    public String getVille_event() {
        return ville_event;
    }

    public void setVille_event(String ville_event) {
        this.ville_event = ville_event;
    }

    public int getNb_participants_max_event() {
        return nb_participants_max_event;
    }

    public void setNb_participants_max_event(int nb_participants_max_event) {
        this.nb_participants_max_event = nb_participants_max_event;
    }

    public boolean isInscription_obligatoire_event() {
        return inscription_obligatoire_event;
    }

    public void setInscription_obligatoire_event(boolean inscription_obligatoire_event) {
        this.inscription_obligatoire_event = inscription_obligatoire_event;
    }

    public Date getDate_limite_inscription_event() {
        return date_limite_inscription_event;
    }

    public void setDate_limite_inscription_event(Date date_limite_inscription_event) {
        this.date_limite_inscription_event = date_limite_inscription_event;
    }

    public String getEmail_contact_event() {
        return email_contact_event;
    }

    public void setEmail_contact_event(String email_contact_event) {
        this.email_contact_event = email_contact_event;
    }

    public String getTel_contact_event() {
        return tel_contact_event;
    }

    public void setTel_contact_event(String tel_contact_event) {
        this.tel_contact_event = tel_contact_event;
    }

    public String getNom_organisateur_event() {
        return nom_organisateur_event;
    }

    public void setNom_organisateur_event(String nom_organisateur_event) {
        this.nom_organisateur_event = nom_organisateur_event;
    }

    public String getImage_couverture_event() {
        return image_couverture_event;
    }

    public void setImage_couverture_event(String image_couverture_event) {
        this.image_couverture_event = image_couverture_event;
    }

    public String getVisibilite_event() {
        return visibilite_event;
    }

    public void setVisibilite_event(String visibilite_event) {
        this.visibilite_event = visibilite_event;
    }

    public Date getDate_creation_event() {
        return date_creation_event;
    }

    public void setDate_creation_event(Date date_creation_event) {
        this.date_creation_event = date_creation_event;
    }

    public Date getDate_mise_a_jour_event() {
        return date_mise_a_jour_event;
    }

    public void setDate_mise_a_jour_event(Date date_mise_a_jour_event) {
        this.date_mise_a_jour_event = date_mise_a_jour_event;
    }

    @Override
    public String toString() {
        return "Evenement{" +
                "id=" + id +
                ", demandes_json='" + demandes_json + '\'' +
                ", titre_event='" + titre_event + '\'' +
                ", slug_event='" + slug_event + '\'' +
                ", type_event='" + type_event + '\'' +
                ", description_event='" + description_event + '\'' +
                ", objectif_event='" + objectif_event + '\'' +
                ", statut_event='" + statut_event + '\'' +
                ", date_debut_event=" + date_debut_event +
                ", date_fin_event=" + date_fin_event +
                ", nom_lieu_event='" + nom_lieu_event + '\'' +
                ", adresse_event='" + adresse_event + '\'' +
                ", ville_event='" + ville_event + '\'' +
                ", nb_participants_max_event=" + nb_participants_max_event +
                ", inscription_obligatoire_event=" + inscription_obligatoire_event +
                ", date_limite_inscription_event=" + date_limite_inscription_event +
                ", email_contact_event='" + email_contact_event + '\'' +
                ", tel_contact_event='" + tel_contact_event + '\'' +
                ", nom_organisateur_event='" + nom_organisateur_event + '\'' +
                ", image_couverture_event='" + image_couverture_event + '\'' +
                ", visibilite_event='" + visibilite_event + '\'' +
                ", date_creation_event=" + date_creation_event +
                ", date_mise_a_jour_event=" + date_mise_a_jour_event +
                '}';
    }
}