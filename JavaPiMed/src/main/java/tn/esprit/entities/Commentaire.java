package tn.esprit.entities;

import java.time.LocalDateTime;

public class Commentaire {

    private int id;

    // objets au lieu de juste stocker les ids
    private Post post;
    private User user;

    private String contenu;
    private LocalDateTime date_creation;
    private boolean est_anonyme;
    private String parametres_confidentialite;
    private String status;
    private Double moderation_score;
    private String moderation_label;
    private LocalDateTime moderated_at;

    public Commentaire() {
    }

    public Commentaire(Post post, User user, String contenu, LocalDateTime date_creation,
                       boolean est_anonyme, String parametres_confidentialite, String status) {
        this.post = post;
        this.user = user;
        this.contenu = contenu;
        this.date_creation = date_creation;
        this.est_anonyme = est_anonyme;
        this.parametres_confidentialite = parametres_confidentialite;
        this.status = status;
    }

    public Commentaire(int id, Post post, User user, String contenu, LocalDateTime date_creation,
                       boolean est_anonyme, String parametres_confidentialite, String status,
                       Double moderation_score, String moderation_label, LocalDateTime moderated_at) {
        this.id = id;
        this.post = post;
        this.user = user;
        this.contenu = contenu;
        this.date_creation = date_creation;
        this.est_anonyme = est_anonyme;
        this.parametres_confidentialite = parametres_confidentialite;
        this.status = status;
        this.moderation_score = moderation_score;
        this.moderation_label = moderation_label;
        this.moderated_at = moderated_at;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // compatibilité si tu veux encore accéder à l'id directement
    public int getPost_id() {
        return post != null ? post.getId() : 0;
    }

    public int getUser_id() {
        return user != null ? user.getId() : 0;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDate_creation() {
        return date_creation;
    }

    public void setDate_creation(LocalDateTime date_creation) {
        this.date_creation = date_creation;
    }

    public boolean isEst_anonyme() {
        return est_anonyme;
    }

    public void setEst_anonyme(boolean est_anonyme) {
        this.est_anonyme = est_anonyme;
    }

    public String getParametres_confidentialite() {
        return parametres_confidentialite;
    }

    public void setParametres_confidentialite(String parametres_confidentialite) {
        this.parametres_confidentialite = parametres_confidentialite;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getModeration_score() {
        return moderation_score;
    }

    public void setModeration_score(Double moderation_score) {
        this.moderation_score = moderation_score;
    }

    public String getModeration_label() {
        return moderation_label;
    }

    public void setModeration_label(String moderation_label) {
        this.moderation_label = moderation_label;
    }

    public LocalDateTime getModerated_at() {
        return moderated_at;
    }

    public void setModerated_at(LocalDateTime moderated_at) {
        this.moderated_at = moderated_at;
    }

    @Override
    public String toString() {
        return "Commentaire{" +
                "id=" + id +
                ", post=" + (post != null ? post.getId() : null) +
                ", user=" + (user != null ? user.getId() : null) +
                ", contenu='" + contenu + '\'' +
                ", date_creation=" + date_creation +
                ", est_anonyme=" + est_anonyme +
                ", parametres_confidentialite='" + parametres_confidentialite + '\'' +
                ", status='" + status + '\'' +
                ", moderation_score=" + moderation_score +
                ", moderation_label='" + moderation_label + '\'' +
                ", moderated_at=" + moderated_at +
                '}';
    }
}