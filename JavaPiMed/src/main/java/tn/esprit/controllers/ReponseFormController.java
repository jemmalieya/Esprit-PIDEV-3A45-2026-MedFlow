package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Reclamation;
import tn.esprit.entities.ReponseReclamation;
import tn.esprit.services.ReponseService;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

import java.time.LocalDateTime;
import tn.esprit.services.ReclamationService;

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
    @FXML private Label errorLabel;

    private Reclamation reclamation;
    private final ReponseService reponseService = new ReponseService();

    private final ReclamationService reclamationService = new ReclamationService();

    private Runnable afterSave;
    private boolean validateForm() {

        boolean valid = true;

        // TYPE
        if (typeReponseCombo.getValue() == null || typeReponseCombo.getValue().isEmpty()) {
            markError(typeReponseCombo);
            shake(typeReponseCombo);
            valid = false;
        } else {
            markOk(typeReponseCombo);
        }

        // MESSAGE
        String message = messageArea.getText() == null ? "" : messageArea.getText().trim();

        if (message.isEmpty() || message.length() < 5) {
            markError(messageArea);
            shake(messageArea);
            valid = false;
        } else {
            markOk(messageArea);
        }

        if (!valid) {
            errorLabel.setText("Veuillez corriger les champs en rouge.");
        } else {
            errorLabel.setText("");
        }

        return valid;
    }
    public void setAfterSave(Runnable afterSave) {
        this.afterSave = afterSave;
    }

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
        messageArea.textProperty().addListener((obs, oldV, newV) -> validateForm());
        typeReponseCombo.valueProperty().addListener((obs, oldV, newV) -> validateForm());
        typeReponseCombo.valueProperty().addListener((obs, o, n) -> validateForm());
        messageArea.textProperty().addListener((obs, o, n) -> validateForm());
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
        if (!validateForm()) {
            return;
        }

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
        reponse.setAuteur("ADMIN");
        reponse.setRole_emetteur("ADMIN");
        reponse.setLu_par_admin(true);
        reponse.setLu_par_patient(false);

        try {
            if (reponseService.existeDoublon(reponse)) {
                showAlert(Alert.AlertType.WARNING,
                        "Doublon détecté",
                        "Une réponse identique existe déjà pour cette réclamation.");
                return;
            }
            if (reponseService.hasFinalResponseForReclamation(reclamation.getId_reclamation())) {
                showAlert(Alert.AlertType.WARNING,
                        "Action refusée",
                        "Une réponse finale existe déjà pour cette réclamation.");
                return;
            }

            reponseService.ajouter(reponse);

            // 🔥 ÉTAPE 7 : réactiver la notification pour le user
            reclamationService.updateNotificationEnvoyee(reclamation.getId_reclamation(), false);

            // 🔥 mettre à jour le statut selon le type choisi
            if ("Réponse finale".equalsIgnoreCase(typeReponse)) {
                reclamationService.updateStatut(reclamation.getId_reclamation(), "Clôturée");
            } else {
                reclamationService.updateStatut(reclamation.getId_reclamation(), "Répondu");
            }

            if (afterSave != null) {
                afterSave.run();
            }

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Réponse envoyée avec succès.");

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
    private void markError(Control c) {
        c.getStyleClass().removeAll("field-ok");
        if (!c.getStyleClass().contains("field-error")) {
            c.getStyleClass().add("field-error");
        }
    }

    private void markOk(Control c) {
        c.getStyleClass().removeAll("field-error");
        if (!c.getStyleClass().contains("field-ok")) {
            c.getStyleClass().add("field-ok");
        }
    }

    private void shake(Control node) {

        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);

        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);

        tt.play();
    }
}
