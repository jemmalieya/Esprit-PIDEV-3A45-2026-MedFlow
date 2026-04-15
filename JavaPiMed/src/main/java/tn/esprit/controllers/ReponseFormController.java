package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Reclamation;
import tn.esprit.entities.ReponseReclamation;
import tn.esprit.services.ReponseService;

import java.time.LocalDateTime;

public class ReponseFormController {

    @FXML private Label reclamationLabel;
    @FXML private Label contenuLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label typeLabel;
    @FXML private Label statutLabel;

    @FXML private ComboBox<String> typeReponseCombo;
    @FXML private TextArea messageArea;

    @FXML private Label evenementArrow;
    @FXML private VBox submenuVBox;

    private Reclamation reclamation;
    private final ReponseService reponseService = new ReponseService();

    public void setReclamation(Reclamation reclamation) {
        this.reclamation = reclamation;
        loadReclamationDetails();
    }

    @FXML
    public void initialize() {
        typeReponseCombo.setItems(FXCollections.observableArrayList(
                "Réponse standard",
                "Réponse urgente",
                "Réponse finale",
                "Demande d'informations"
        ));
        typeReponseCombo.setValue("Réponse standard");
    }

    private void loadReclamationDetails() {
        if (reclamation != null) {
            reclamationLabel.setText("Réclamation: " + reclamation.getReference_reclamation());
            contenuLabel.setText("Contenu: " + reclamation.getContenu());
            descriptionLabel.setText("Description: " + reclamation.getDescription());
            typeLabel.setText("Type: " + reclamation.getType());
            statutLabel.setText("Statut: " + reclamation.getStatut_reclamation());
        }
    }

    @FXML
    private void onEnvoyer() {
        if (reclamation == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucune réclamation sélectionnée.");
            return;
        }

        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "Veuillez saisir un message.");
            return;
        }

        String typeReponse = typeReponseCombo.getValue();

        ReponseReclamation reponse = new ReponseReclamation();
        reponse.setReclamation(reclamation);
        reponse.setMessage(message);
        reponse.setType_reponse(typeReponse);
        reponse.setDate_creation_rep(LocalDateTime.now());
        reponse.setDate_modification_rep(LocalDateTime.now());
        reponse.setIs_read(false);

        try {
            reponseService.ajouter(reponse);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Réponse envoyée avec succès.");
            // Fermer la fenêtre
            Stage stage = (Stage) messageArea.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'envoi: " + e.getMessage());
        }
    }

    @FXML
    private void onAnnuler() {
        Stage stage = (Stage) messageArea.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onRetour() {
        Stage stage = (Stage) messageArea.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void toggleSubmenu() {
        boolean show = !submenuVBox.isVisible();

        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);

        if (show) {
            evenementArrow.setText("⌃");
        } else {
            evenementArrow.setText("⌄");
        }
    }

    @FXML
    private void onVoirTousEvenements() {
        // Logique pour afficher toutes les réclamations
    }

    @FXML
    private void onTableauBordAdmin() {
        // Logique pour ouvrir le tableau de bord admin
    }

    @FXML
    private void onAutresActions() {
        // Logique pour autres actions
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
