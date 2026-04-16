package tn.esprit.entities;

import java.time.LocalDateTime;

public class Reclamation {

    private int id_reclamation;
    private User user;

    private String reference_reclamation;
    private String contenu;
    private String description;
    private String type;
    private String piece_jointe_path;
    private String statut_reclamation;
    private String priorite;

    private LocalDateTime date_limite;
    private LocalDateTime date_creation_r;
    private LocalDateTime date_modification_r;
    private LocalDateTime date_cloture_r;

    private boolean notification_envoyee;

    private String piece_jointe_resource_type;
    private String piece_jointe_format;
    private int piece_jointe_bytes;
    private String piece_jointe_original_name;

    private String contenu_original;
    private String description_original;
    private String langue_originale;

    private String contenu_francais;
    private String description_francais;

    private int urgence_score;
    private String sentiment;

    private LocalDateTime translated_at;
    private LocalDateTime analysis_at;

    // 🔹 Constructeur vide
    public Reclamation() {
    }

    // 🔹 Constructeur sans ID
    public Reclamation(User user, String reference_reclamation, String contenu, String description,
                       String type, String statut_reclamation, String priorite) {
        this.user = user;
        this.reference_reclamation = reference_reclamation;
        this.contenu = contenu;
        this.description = description;
        this.type = type;
        this.statut_reclamation = statut_reclamation;
        this.priorite = priorite;
    }

    // 🔹 Constructeur complet
    public Reclamation(int id_reclamation, User user, String reference_reclamation, String contenu,
                       String description, String type, String piece_jointe_path,
                       String statut_reclamation, String priorite, LocalDateTime date_limite,
                       LocalDateTime date_creation_r, LocalDateTime date_modification_r,
                       LocalDateTime date_cloture_r, boolean notification_envoyee,
                       String piece_jointe_resource_type, String piece_jointe_format,
                       int piece_jointe_bytes, String piece_jointe_original_name,
                       String contenu_original, String description_original, String langue_originale,
                       String contenu_francais, String description_francais,
                       int urgence_score, String sentiment,
                       LocalDateTime translated_at, LocalDateTime analysis_at) {

        this.id_reclamation = id_reclamation;
        this.user = user;
        this.reference_reclamation = reference_reclamation;
        this.contenu = contenu;
        this.description = description;
        this.type = type;
        this.piece_jointe_path = piece_jointe_path;
        this.statut_reclamation = statut_reclamation;
        this.priorite = priorite;
        this.date_limite = date_limite;
        this.date_creation_r = date_creation_r;
        this.date_modification_r = date_modification_r;
        this.date_cloture_r = date_cloture_r;
        this.notification_envoyee = notification_envoyee;
        this.piece_jointe_resource_type = piece_jointe_resource_type;
        this.piece_jointe_format = piece_jointe_format;
        this.piece_jointe_bytes = piece_jointe_bytes;
        this.piece_jointe_original_name = piece_jointe_original_name;
        this.contenu_original = contenu_original;
        this.description_original = description_original;
        this.langue_originale = langue_originale;
        this.contenu_francais = contenu_francais;
        this.description_francais = description_francais;
        this.urgence_score = urgence_score;
        this.sentiment = sentiment;
        this.translated_at = translated_at;
        this.analysis_at = analysis_at;
    }

    // 🔹 GETTERS & SETTERS

    public int getId_reclamation() {
        return id_reclamation;
    }

    public void setId_reclamation(int id_reclamation) {
        this.id_reclamation = id_reclamation;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getReference_reclamation() {
        return reference_reclamation;
    }

    public void setReference_reclamation(String reference_reclamation) {
        this.reference_reclamation = reference_reclamation;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPiece_jointe_path() {
        return piece_jointe_path;
    }

    public void setPiece_jointe_path(String piece_jointe_path) {
        this.piece_jointe_path = piece_jointe_path;
    }

    public String getStatut_reclamation() {
        return statut_reclamation;
    }

    public void setStatut_reclamation(String statut_reclamation) {
        this.statut_reclamation = statut_reclamation;
    }

    public String getPriorite() {
        return priorite;
    }

    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }

    public LocalDateTime getDate_limite() {
        return date_limite;
    }

    public void setDate_limite(LocalDateTime date_limite) {
        this.date_limite = date_limite;
    }

    public LocalDateTime getDate_creation_r() {
        return date_creation_r;
    }

    public void setDate_creation_r(LocalDateTime date_creation_r) {
        this.date_creation_r = date_creation_r;
    }

    public LocalDateTime getDate_modification_r() {
        return date_modification_r;
    }

    public void setDate_modification_r(LocalDateTime date_modification_r) {
        this.date_modification_r = date_modification_r;
    }

    public LocalDateTime getDate_cloture_r() {
        return date_cloture_r;
    }

    public void setDate_cloture_r(LocalDateTime date_cloture_r) {
        this.date_cloture_r = date_cloture_r;
    }

    public boolean isNotification_envoyee() {
        return notification_envoyee;
    }

    public void setNotification_envoyee(boolean notification_envoyee) {
        this.notification_envoyee = notification_envoyee;
    }

    public String getPiece_jointe_resource_type() {
        return piece_jointe_resource_type;
    }

    public void setPiece_jointe_resource_type(String piece_jointe_resource_type) {
        this.piece_jointe_resource_type = piece_jointe_resource_type;
    }

    public String getPiece_jointe_format() {
        return piece_jointe_format;
    }

    public void setPiece_jointe_format(String piece_jointe_format) {
        this.piece_jointe_format = piece_jointe_format;
    }

    public int getPiece_jointe_bytes() {
        return piece_jointe_bytes;
    }

    public void setPiece_jointe_bytes(int piece_jointe_bytes) {
        this.piece_jointe_bytes = piece_jointe_bytes;
    }

    public String getPiece_jointe_original_name() {
        return piece_jointe_original_name;
    }

    public void setPiece_jointe_original_name(String piece_jointe_original_name) {
        this.piece_jointe_original_name = piece_jointe_original_name;
    }

    public String getContenu_original() {
        return contenu_original;
    }

    public void setContenu_original(String contenu_original) {
        this.contenu_original = contenu_original;
    }

    public String getDescription_original() {
        return description_original;
    }

    public void setDescription_original(String description_original) {
        this.description_original = description_original;
    }

    public String getLangue_originale() {
        return langue_originale;
    }

    public void setLangue_originale(String langue_originale) {
        this.langue_originale = langue_originale;
    }

    public String getContenu_francais() {
        return contenu_francais;
    }

    public void setContenu_francais(String contenu_francais) {
        this.contenu_francais = contenu_francais;
    }

    public String getDescription_francais() {
        return description_francais;
    }

    public void setDescription_francais(String description_francais) {
        this.description_francais = description_francais;
    }

    public int getUrgence_score() {
        return urgence_score;
    }

    public void setUrgence_score(int urgence_score) {
        this.urgence_score = urgence_score;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public LocalDateTime getTranslated_at() {
        return translated_at;
    }

    public void setTranslated_at(LocalDateTime translated_at) {
        this.translated_at = translated_at;
    }

    public LocalDateTime getAnalysis_at() {
        return analysis_at;
    }

    public void setAnalysis_at(LocalDateTime analysis_at) {
        this.analysis_at = analysis_at;
    }

    // 🔹 toString
    @Override
    public String toString() {
        return "Reclamation{" +
                "id_reclamation=" + id_reclamation +
                ", user_id=" + (user != null ? user.getId() : null) +
                ", reference_reclamation='" + reference_reclamation + '\'' +
                ", contenu='" + contenu + '\'' +
                ", description='" + description + '\'' +
                ", type='" + type + '\'' +
                ", statut_reclamation='" + statut_reclamation + '\'' +
                ", priorite='" + priorite + '\'' +
                '}';
    }
}