package tn.esprit.entities;

import java.sql.Timestamp;

public class Prescription {

    private int id;
    private int fiche_medicale_id;
    private String nom_medicament;
    private String dose;
    private String frequence;
    private int duree;
    private String instructions;
    private Timestamp created_at;

    // Constructeur vide
    public Prescription() {
    }

    // Constructeur sans id
    public Prescription(int fiche_medicale_id, String nom_medicament, String dose,
                        String frequence, int duree, String instructions, Timestamp created_at) {
        this.fiche_medicale_id = fiche_medicale_id;
        this.nom_medicament = nom_medicament;
        this.dose = dose;
        this.frequence = frequence;
        this.duree = duree;
        this.instructions = instructions;
        this.created_at = created_at;
    }

    // Constructeur avec id
    public Prescription(int id, int fiche_medicale_id, String nom_medicament, String dose,
                        String frequence, int duree, String instructions, Timestamp created_at) {
        this.id = id;
        this.fiche_medicale_id = fiche_medicale_id;
        this.nom_medicament = nom_medicament;
        this.dose = dose;
        this.frequence = frequence;
        this.duree = duree;
        this.instructions = instructions;
        this.created_at = created_at;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public int getFiche_medicale_id() {
        return fiche_medicale_id;
    }

    public void setFiche_medicale_id(int fiche_medicale_id) {
        this.fiche_medicale_id = fiche_medicale_id;
    }

    public String getNom_medicament() {
        return nom_medicament;
    }

    public void setNom_medicament(String nom_medicament) {
        this.nom_medicament = nom_medicament;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
    }

    public String getFrequence() {
        return frequence;
    }

    public void setFrequence(String frequence) {
        this.frequence = frequence;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Timestamp getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Timestamp created_at) {
        this.created_at = created_at;
    }

    @Override
    public String toString() {
        return "Prescription{" +
                "id=" + id +
                ", fiche_medicale_id=" + fiche_medicale_id +
                ", nom_medicament='" + nom_medicament + '\'' +
                ", dose='" + dose + '\'' +
                ", frequence='" + frequence + '\'' +
                ", duree=" + duree +
                ", instructions='" + instructions + '\'' +
                ", created_at=" + created_at +
                '}';
    }
}