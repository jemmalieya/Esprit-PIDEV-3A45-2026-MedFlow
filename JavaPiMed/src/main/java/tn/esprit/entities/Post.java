package tn.esprit.entities;

import java.time.LocalDateTime;

public class Post {

    private int id;
    private Integer user_id;
    private String titre;
    private String contenu;
    private String localisation;
    private String img_post;
    private String hashtags;
    private String visibilite;
    private LocalDateTime date_creation;
    private LocalDateTime date_modification;
    private boolean est_anonyme;
    private String categorie;
    private String humeur;
    private int nbr_reactions;
    private int nbr_commentaires;
    private boolean is_approved;
    private String moderation_status;
    private String moderation_message;
    private boolean moderation_seen;

    public Post() {
    }

    public Post(Integer user_id, String titre, String contenu, String localisation, String img_post,
                String hashtags, String visibilite, LocalDateTime date_creation, boolean est_anonyme,
                String categorie, String humeur, int nbr_reactions, int nbr_commentaires,
                boolean is_approved, String moderation_status, String moderation_message,
                boolean moderation_seen) {
        this.user_id = user_id;
        this.titre = titre;
        this.contenu = contenu;
        this.localisation = localisation;
        this.img_post = img_post;
        this.hashtags = hashtags;
        this.visibilite = visibilite;
        this.date_creation = date_creation;
        this.est_anonyme = est_anonyme;
        this.categorie = categorie;
        this.humeur = humeur;
        this.nbr_reactions = nbr_reactions;
        this.nbr_commentaires = nbr_commentaires;
        this.is_approved = is_approved;
        this.moderation_status = moderation_status;
        this.moderation_message = moderation_message;
        this.moderation_seen = moderation_seen;
    }

    public Post(int id, Integer user_id, String titre, String contenu, String localisation, String img_post,
                String hashtags, String visibilite, LocalDateTime date_creation,
                LocalDateTime date_modification, boolean est_anonyme, String categorie,
                String humeur, int nbr_reactions, int nbr_commentaires, boolean is_approved,
                String moderation_status, String moderation_message, boolean moderation_seen) {
        this.id = id;
        this.user_id = user_id;
        this.titre = titre;
        this.contenu = contenu;
        this.localisation = localisation;
        this.img_post = img_post;
        this.hashtags = hashtags;
        this.visibilite = visibilite;
        this.date_creation = date_creation;
        this.date_modification = date_modification;
        this.est_anonyme = est_anonyme;
        this.categorie = categorie;
        this.humeur = humeur;
        this.nbr_reactions = nbr_reactions;
        this.nbr_commentaires = nbr_commentaires;
        this.is_approved = is_approved;
        this.moderation_status = moderation_status;
        this.moderation_message = moderation_message;
        this.moderation_seen = moderation_seen;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getLocalisation() {
        return localisation;
    }

    public void setLocalisation(String localisation) {
        this.localisation = localisation;
    }

    public String getImg_post() {
        return img_post;
    }

    public void setImg_post(String img_post) {
        this.img_post = img_post;
    }

    public String getHashtags() {
        return hashtags;
    }

    public void setHashtags(String hashtags) {
        this.hashtags = hashtags;
    }

    public String getVisibilite() {
        return visibilite;
    }

    public void setVisibilite(String visibilite) {
        this.visibilite = visibilite;
    }

    public LocalDateTime getDate_creation() {
        return date_creation;
    }

    public void setDate_creation(LocalDateTime date_creation) {
        this.date_creation = date_creation;
    }

    public LocalDateTime getDate_modification() {
        return date_modification;
    }

    public void setDate_modification(LocalDateTime date_modification) {
        this.date_modification = date_modification;
    }

    public boolean isEst_anonyme() {
        return est_anonyme;
    }

    public void setEst_anonyme(boolean est_anonyme) {
        this.est_anonyme = est_anonyme;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getHumeur() {
        return humeur;
    }

    public void setHumeur(String humeur) {
        this.humeur = humeur;
    }

    public int getNbr_reactions() {
        return nbr_reactions;
    }

    public void setNbr_reactions(int nbr_reactions) {
        this.nbr_reactions = nbr_reactions;
    }

    public int getNbr_commentaires() {
        return nbr_commentaires;
    }

    public void setNbr_commentaires(int nbr_commentaires) {
        this.nbr_commentaires = nbr_commentaires;
    }

    public boolean isIs_approved() {
        return is_approved;
    }

    public void setIs_approved(boolean is_approved) {
        this.is_approved = is_approved;
    }

    public String getModeration_status() {
        return moderation_status;
    }

    public void setModeration_status(String moderation_status) {
        this.moderation_status = moderation_status;
    }

    public String getModeration_message() {
        return moderation_message;
    }

    public void setModeration_message(String moderation_message) {
        this.moderation_message = moderation_message;
    }

    public boolean isModeration_seen() {
        return moderation_seen;
    }

    public void setModeration_seen(boolean moderation_seen) {
        this.moderation_seen = moderation_seen;
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", user_id=" + user_id +
                ", titre='" + titre + '\'' +
                ", contenu='" + contenu + '\'' +
                ", localisation='" + localisation + '\'' +
                ", img_post='" + img_post + '\'' +
                ", hashtags='" + hashtags + '\'' +
                ", visibilite='" + visibilite + '\'' +
                ", date_creation=" + date_creation +
                ", date_modification=" + date_modification +
                ", est_anonyme=" + est_anonyme +
                ", categorie='" + categorie + '\'' +
                ", humeur='" + humeur + '\'' +
                ", nbr_reactions=" + nbr_reactions +
                ", nbr_commentaires=" + nbr_commentaires +
                ", is_approved=" + is_approved +
                ", moderation_status='" + moderation_status + '\'' +
                ", moderation_message='" + moderation_message + '\'' +
                ", moderation_seen=" + moderation_seen +
                '}';
    }
}