package tn.esprit.entities;

import java.sql.Timestamp;

public class FicheMedicale {

    private int id;
    private Integer rendez_vous_id;
    private String diagnostic;
    private String observations;
    private String resultats_examens;
    private Timestamp start_time;
    private Timestamp end_time;
    private Integer duree_minutes;
    private Timestamp created_at;
    private String signature;

    // Constructeur vide
    public FicheMedicale() {
    }

    // Constructeur sans id
    public FicheMedicale(Integer rendez_vous_id, String diagnostic, String observations,
                         String resultats_examens, Timestamp start_time, Timestamp end_time,
                         Integer duree_minutes, Timestamp created_at, String signature) {
        this.rendez_vous_id = rendez_vous_id;
        this.diagnostic = diagnostic;
        this.observations = observations;
        this.resultats_examens = resultats_examens;
        this.start_time = start_time;
        this.end_time = end_time;
        this.duree_minutes = duree_minutes;
        this.created_at = created_at;
        this.signature = signature;
    }

    // Constructeur avec id
    public FicheMedicale(int id, Integer rendez_vous_id, String diagnostic, String observations,
                         String resultats_examens, Timestamp start_time, Timestamp end_time,
                         Integer duree_minutes, Timestamp created_at, String signature) {
        this.id = id;
        this.rendez_vous_id = rendez_vous_id;
        this.diagnostic = diagnostic;
        this.observations = observations;
        this.resultats_examens = resultats_examens;
        this.start_time = start_time;
        this.end_time = end_time;
        this.duree_minutes = duree_minutes;
        this.created_at = created_at;
        this.signature = signature;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public Integer getRendez_vous_id() {
        return rendez_vous_id;
    }

    public void setRendez_vous_id(Integer rendez_vous_id) {
        this.rendez_vous_id = rendez_vous_id;
    }

    public String getDiagnostic() {
        return diagnostic;
    }

    public void setDiagnostic(String diagnostic) {
        this.diagnostic = diagnostic;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getResultats_examens() {
        return resultats_examens;
    }

    public void setResultats_examens(String resultats_examens) {
        this.resultats_examens = resultats_examens;
    }

    public Timestamp getStart_time() {
        return start_time;
    }

    public void setStart_time(Timestamp start_time) {
        this.start_time = start_time;
    }

    public Timestamp getEnd_time() {
        return end_time;
    }

    public void setEnd_time(Timestamp end_time) {
        this.end_time = end_time;
    }

    public Integer getDuree_minutes() {
        return duree_minutes;
    }

    public void setDuree_minutes(Integer duree_minutes) {
        this.duree_minutes = duree_minutes;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "FicheMedicale{" +
                "id=" + id +
                ", rendez_vous_id=" + rendez_vous_id +
                ", diagnostic='" + diagnostic + '\'' +
                ", observations='" + observations + '\'' +
                ", resultats_examens='" + resultats_examens + '\'' +
                ", start_time=" + start_time +
                ", end_time=" + end_time +
                ", duree_minutes=" + duree_minutes +
                ", created_at=" + created_at +
                ", signature='" + signature + '\'' +
                '}';
    }
}