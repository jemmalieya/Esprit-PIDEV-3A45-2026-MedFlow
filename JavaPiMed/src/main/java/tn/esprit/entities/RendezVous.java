package tn.esprit.entities;

import java.sql.Timestamp;

public class RendezVous {
    private int id;
    private Timestamp datetime;
    private String statut;
    private String mode;
    private String motif;
    private Timestamp created_at;
    private int idPatient;
    private int idStaff;
    private String urgency_level;

    // Constructeur vide
    public RendezVous() {
    }

    // Constructeur sans id
    public RendezVous(Timestamp datetime, String statut, String mode, String motif,
                      Timestamp created_at, int idPatient, int idStaff, String urgency_level) {
        this.datetime = datetime;
        this.statut = statut;
        this.mode = mode;
        this.motif = motif;
        this.created_at = created_at;
        this.idPatient = idPatient;
        this.idStaff = idStaff;
        this.urgency_level = urgency_level;
    }

    // Constructeur avec id
    public RendezVous(int id, Timestamp datetime, String statut, String mode, String motif,
                      Timestamp created_at, int idPatient, int idStaff, String urgency_level) {
        this.id = id;
        this.datetime = datetime;
        this.statut = statut;
        this.mode = mode;
        this.motif = motif;
        this.created_at = created_at;
        this.idPatient = idPatient;
        this.idStaff = idStaff;
        this.urgency_level = urgency_level;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public Timestamp getDatetime() {
        return datetime;
    }

    public void setDatetime(Timestamp datetime) {
        this.datetime = datetime;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    public int getIdPatient() {
        return idPatient;
    }

    public void setIdPatient(int idPatient) {
        this.idPatient = idPatient;
    }

    public int getIdStaff() {
        return idStaff;
    }

    public void setIdStaff(int idStaff) {
        this.idStaff = idStaff;
    }

    public String getUrgency_level() {
        return urgency_level;
    }

    public void setUrgency_level(String urgency_level) {
        this.urgency_level = urgency_level;
    }

    @Override
    public String toString() {
        return "RendezVous{" +
                "id=" + id +
                ", datetime=" + datetime +
                ", statut='" + statut + '\'' +
                ", mode='" + mode + '\'' +
                ", motif='" + motif + '\'' +
                ", created_at=" + created_at +
                ", idPatient=" + idPatient +
                ", idStaff=" + idStaff +
                ", urgency_level='" + urgency_level + '\'' +
                '}';
    }
}
