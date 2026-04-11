package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.entities.Reclamation;
import tn.esprit.services.ReclamationService;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ReclamationController {

    @FXML
    private TextField tfContenu;

    @FXML
    private TextArea taDescription;

    @FXML
    private ComboBox<String> cbType;

    @FXML
    private ComboBox<String> cbPriorite;

    @FXML
    private Label lblMessage;

    @FXML
    private TableView<Reclamation> tableReclamations;

    @FXML
    private TableColumn<Reclamation, Integer> colId;

    @FXML
    private TableColumn<Reclamation, String> colReference;

    @FXML
    private TableColumn<Reclamation, String> colContenu;

    @FXML
    private TableColumn<Reclamation, String> colDescription;

    @FXML
    private TableColumn<Reclamation, String> colType;

    @FXML
    private TableColumn<Reclamation, String> colStatut;

    @FXML
    private TableColumn<Reclamation, String> colPriorite;

    private final ReclamationService reclamationService = new ReclamationService();
    private ObservableList<Reclamation> reclamationList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cbType.setItems(FXCollections.observableArrayList(
                "Technique",
                "Service",
                "Paiement",
                "Livraison",
                "Autre"
        ));

        cbPriorite.setItems(FXCollections.observableArrayList(
                "Faible",
                "Moyenne",
                "Élevée"
        ));

        colId.setCellValueFactory(new PropertyValueFactory<>("id_reclamation"));
        colReference.setCellValueFactory(new PropertyValueFactory<>("reference_reclamation"));
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut_reclamation"));
        colPriorite.setCellValueFactory(new PropertyValueFactory<>("priorite"));

        afficherReclamations();

        tableReclamations.getSelectionModel().selectedItemProperty().addListener((obs, oldRec, newRec) -> {
            if (newRec != null) {
                remplirChamps(newRec);
            }
        });
    }

    @FXML
    private void ajouterReclamation() {
        try {
            if (tfContenu.getText().isEmpty() || taDescription.getText().isEmpty()
                    || cbType.getValue() == null || cbPriorite.getValue() == null) {
                afficherMessage("Veuillez remplir tous les champs.", "red");
                return;
            }

            Reclamation r = new Reclamation();
            r.setReference_reclamation(genererReference());
            r.setContenu(tfContenu.getText());
            r.setDescription(taDescription.getText());
            r.setType(cbType.getValue());
            r.setStatut_reclamation("En attente");
            r.setPriorite(cbPriorite.getValue());

            reclamationService.ajouter(r);
            afficherMessage("Réclamation ajoutée avec succès.", "green");
            afficherReclamations();
            viderChamps();

        } catch (Exception e) {
            afficherMessage("Erreur lors de l'ajout : " + e.getMessage(), "red");
        }
    }

    @FXML
    private void modifierReclamation() {
        Reclamation selected = tableReclamations.getSelectionModel().getSelectedItem();

        if (selected == null) {
            afficherMessage("Veuillez sélectionner une réclamation à modifier.", "red");
            return;
        }

        try {
            if (tfContenu.getText().isEmpty() || taDescription.getText().isEmpty()
                    || cbType.getValue() == null || cbPriorite.getValue() == null) {
                afficherMessage("Veuillez remplir tous les champs.", "red");
                return;
            }

            selected.setContenu(tfContenu.getText());
            selected.setDescription(taDescription.getText());
            selected.setType(cbType.getValue());
            selected.setPriorite(cbPriorite.getValue());

            reclamationService.modifier(selected);
            afficherMessage("Réclamation modifiée avec succès.", "green");
            afficherReclamations();
            viderChamps();

        } catch (Exception e) {
            afficherMessage("Erreur lors de la modification : " + e.getMessage(), "red");
        }
    }

    @FXML
    private void supprimerReclamation() {
        Reclamation selected = tableReclamations.getSelectionModel().getSelectedItem();

        if (selected == null) {
            afficherMessage("Veuillez sélectionner une réclamation à supprimer.", "red");
            return;
        }

        try {
            reclamationService.supprimer(selected);
            afficherMessage("Réclamation supprimée avec succès.", "green");
            afficherReclamations();
            viderChamps();

        } catch (Exception e) {
            afficherMessage("Erreur lors de la suppression : " + e.getMessage(), "red");
        }
    }

    @FXML
    private void viderChamps() {
        tfContenu.clear();
        taDescription.clear();
        cbType.setValue(null);
        cbPriorite.setValue(null);
        tableReclamations.getSelectionModel().clearSelection();
        lblMessage.setText("");
    }

    private void afficherReclamations() {
        try {
            List<Reclamation> list = reclamationService.recuperer();
            reclamationList = FXCollections.observableArrayList(list);
            tableReclamations.setItems(reclamationList);
        } catch (SQLException e) {
            afficherMessage("Erreur chargement tableau : " + e.getMessage(), "red");
        }
    }

    private void remplirChamps(Reclamation r) {
        tfContenu.setText(r.getContenu());
        taDescription.setText(r.getDescription());
        cbType.setValue(r.getType());
        cbPriorite.setValue(r.getPriorite());
    }

    private String genererReference() {
        return "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void afficherMessage(String message, String color) {
        lblMessage.setText(message);
        lblMessage.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: bold;");
    }
}