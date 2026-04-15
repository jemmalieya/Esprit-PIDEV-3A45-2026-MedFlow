package tn.esprit.entities;

import java.time.LocalDateTime;

public class ReponseReclamation {

    private int id_reponse;
    private Reclamation reclamation; // ✅ objet complet
    private String message;
    private String type_reponse;
    private LocalDateTime date_creation_rep;
    private LocalDateTime date_modification_rep;
    private boolean is_read;

    public ReponseReclamation() {
    }

    public ReponseReclamation(Reclamation reclamation, String message, String type_reponse,
                              LocalDateTime date_creation_rep, LocalDateTime date_modification_rep,
                              boolean is_read) {
        this.reclamation = reclamation;
        this.message = message;
        this.type_reponse = type_reponse;
        this.date_creation_rep = date_creation_rep;
        this.date_modification_rep = date_modification_rep;
        this.is_read = is_read;
    }

    public ReponseReclamation(int id_reponse, Reclamation reclamation, String message, String type_reponse,
                              LocalDateTime date_creation_rep, LocalDateTime date_modification_rep,
                              boolean is_read) {
        this.id_reponse = id_reponse;
        this.reclamation = reclamation;
        this.message = message;
        this.type_reponse = type_reponse;
        this.date_creation_rep = date_creation_rep;
        this.date_modification_rep = date_modification_rep;
        this.is_read = is_read;
    }

    // ================= GETTERS / SETTERS =================

    public int getId_reponse() {
        return id_reponse;
    }

    public void setId_reponse(int id_reponse) {
        this.id_reponse = id_reponse;
    }

    public Reclamation getReclamation() {
        return reclamation;
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType_reponse() {
        return type_reponse;
    }

    public void setType_reponse(String type_reponse) {
        this.type_reponse = type_reponse;
    }

    public LocalDateTime getDate_creation_rep() {
        return date_creation_rep;
    }

    public void setDate_creation_rep(LocalDateTime date_creation_rep) {
        this.date_creation_rep = date_creation_rep;
    }

    public LocalDateTime getDate_modification_rep() {
        return date_modification_rep;
    }

    public void setDate_modification_rep(LocalDateTime date_modification_rep) {
        this.date_modification_rep = date_modification_rep;
    }

    public boolean isIs_read() {
        return is_read;
    }

    public void setIs_read(boolean is_read) {
        this.is_read = is_read;
    }

    @Override
    public String toString() {
        return "ReponseReclamation{" +
                "id_reponse=" + id_reponse +
                ", reclamation=" + (reclamation != null ? reclamation.getId_reclamation() : null) +
                ", message='" + message + '\'' +
                ", type_reponse='" + type_reponse + '\'' +
                ", date_creation_rep=" + date_creation_rep +
                ", date_modification_rep=" + date_modification_rep +
                ", is_read=" + is_read +
                '}';
    }
}