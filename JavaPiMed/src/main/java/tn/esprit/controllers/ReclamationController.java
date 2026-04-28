package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.entities.Reclamation;
import tn.esprit.services.ReclamationService;

import java.util.UUID;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;
import java.time.LocalDateTime;

import tn.esprit.tools.SessionManager;
import tn.esprit.entities.User;
import java.time.LocalDateTime;

import tn.esprit.services.AudioRecorderServiceReclamation;
import tn.esprit.services.GroqSpeechToTextServiceReclamation;
import tn.esprit.services.GroqTranslationServiceReclamation;
import tn.esprit.services.VoiceTranslationResult;

import java.io.File;

public class ReclamationController {

    // ===== FORM =====
    @FXML private TextField tfContenu;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<String> cbType;

    @FXML private Label lblMessage;
    private Reclamation reclamationToEdit = null;
    private boolean isEditMode = false;
    @FXML private Label errContenu;
    @FXML private Label errDescription;
    @FXML private Label errType;

    @FXML private Label lblVoiceStatus;
    private final AudioRecorderServiceReclamation audioRecorderService = new AudioRecorderServiceReclamation();
    private final GroqSpeechToTextServiceReclamation speechService = new GroqSpeechToTextServiceReclamation();
    private final GroqTranslationServiceReclamation translationService = new GroqTranslationServiceReclamation();

    private String contenuOriginalVoice;
    private String descriptionOriginalVoice;
    private String langueContenuVoice;
    private String langueDescriptionVoice;


    private final ReclamationService reclamationService = new ReclamationService();
    public void setReclamation(Reclamation r) {
        this.reclamationToEdit = r;
        this.isEditMode = true;

        // 🔥 remplir champs
        tfContenu.setText(r.getContenu());
        taDescription.setText(r.getDescription());
        cbType.setValue(r.getType());

    }
    // ===== INIT =====
    @FXML
    public void initialize() {

        cbType.setItems(FXCollections.observableArrayList(
                "Technique", "Service", "Paiement", "Livraison", "Autre"
        ));

        tfContenu.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() >= 3) {
                tfContenu.setStyle("-fx-border-color: green;");
            }
        });

        taDescription.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() >= 10) {
                taDescription.setStyle("-fx-border-color: green;");
            }
        });

    }

    @FXML
    private void dicterContenu() {
        dicterEtTraduire(true);
    }

    @FXML
    private void dicterDescription() {
        dicterEtTraduire(false);
    }

    private void dicterEtTraduire(boolean isContenu) {

        showMessage("🎤 Parlez maintenant pendant 7 secondes...", "#0f766e");

        if (lblVoiceStatus != null) {
            lblVoiceStatus.setText("🎤 Enregistrement en cours...");
            lblVoiceStatus.setStyle("-fx-text-fill: #0f766e; -fx-font-weight: bold;");
        }

        new Thread(() -> {
            File audioFile = null;

            try {
                audioFile = audioRecorderService.enregistrerWavPendant(7);

                String texteOriginal = speechService.transcrire(audioFile);

                VoiceTranslationResult result =
                        translationService.traduireVersFrancais(texteOriginal);

                Platform.runLater(() -> {

                    if (isContenu) {
                        tfContenu.setText(result.getTexteFrancais());
                        contenuOriginalVoice = result.getTexteOriginal();
                        langueContenuVoice = result.getLangueSource();
                    } else {
                        taDescription.setText(result.getTexteFrancais());
                        descriptionOriginalVoice = result.getTexteOriginal();
                        langueDescriptionVoice = result.getLangueSource();
                    }

                    showMessage("✅ Voix détectée et traduite en français", "green");

                    if (lblVoiceStatus != null) {
                        lblVoiceStatus.setText(
                                "Texte original : " + result.getTexteOriginal()
                                        + "\nLangue : " + result.getLangueSource()
                                        + "\nTraduction : " + result.getTexteFrancais()
                        );
                        lblVoiceStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    showMessage("❌ Erreur API voix : " + e.getMessage(), "red");

                    if (lblVoiceStatus != null) {
                        lblVoiceStatus.setText("Erreur : " + e.getMessage());
                        lblVoiceStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                });

            } finally {
                if (audioFile != null && audioFile.exists()) {
                    audioFile.delete();
                }
            }
        }).start();
    }

    // ===== AJOUT =====
    @FXML
    private void ajouterReclamation() {
        if (!validerFormulaire()) {
            return; // ❌ stop si erreur
        }

        if (tfContenu.getText().isEmpty()
                || taDescription.getText().isEmpty()
                || cbType.getValue() == null) {

            showMessage("Veuillez remplir tous les champs", "red");
            return;
        }

        new Thread(() -> {
            try {

                if (isEditMode) {
                    // 🔥 UPDATE
                    reclamationToEdit.setContenu(tfContenu.getText());
                    reclamationToEdit.setDescription(taDescription.getText());
                    reclamationToEdit.setType(cbType.getValue());


                    reclamationService.modifier(reclamationToEdit);

                    Platform.runLater(() -> {

                        // ✅ Alerte succès
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Succès");
                        alert.setHeaderText(null);
                        alert.setContentText("✅ Réclamation modifiée avec succès !");
                        alert.showAndWait();

                        // 🔥 Redirection après OK
                        goToListPage();

                    });

                } else {
                    // 🔥 AJOUT
                    User currentUser = SessionManager.getCurrentUser();

                    if (currentUser == null) {
                        Platform.runLater(() ->
                                showMessage("❌ Aucun utilisateur connecté. Veuillez vous reconnecter.", "red")
                        );
                        return;
                    }

                    Reclamation r = new Reclamation();
                    r.setUser(currentUser);
                    r.setReference_reclamation(generateRef());
                    r.setContenu(tfContenu.getText());
                    r.setDescription(taDescription.getText());
                    r.setType(cbType.getValue());

                    r.setStatut_reclamation("En attente");
                    r.setDate_creation_r(LocalDateTime.now());
                    r.setNotification_envoyee(false);

                    reclamationService.ajouter(r);
                    Platform.runLater(() -> {
                        showMessage("✅ Réclamation ajoutée", "green");
                        clearFields();
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() ->
                        showMessage("❌ Erreur: " + e.getMessage(), "red")
                );
            }
        }).start();
    }
    private void resetErrors() {
        errContenu.setText("");
        errDescription.setText("");
        errType.setText("");

    }
    private boolean validerFormulaire() {

        boolean isValid = true;
        resetErrors();

        // 🔴 CONTENU
        if (tfContenu.getText().isEmpty()) {
            errContenu.setText("Le contenu est obligatoire");
            isValid = false;
        } else if (tfContenu.getText().length() < 3) {
            errContenu.setText("Minimum 3 caractères");
            isValid = false;
        }

        // 🔴 DESCRIPTION
        if (taDescription.getText().isEmpty()) {
            errDescription.setText("La description est obligatoire");
            isValid = false;
        } else if (taDescription.getText().length() < 10) {
            errDescription.setText("Minimum 10 caractères");
            isValid = false;
        }

        // 🔴 TYPE
        if (cbType.getValue() == null) {
            errType.setText("Veuillez choisir un type");
            isValid = false;
        }



        // 🔥 MESSAGE GLOBAL
        if (!isValid) {
            lblMessage.setText("⚠ Corrige les erreurs affichées");
            lblMessage.setStyle("-fx-text-fill: red;");
        } else {
            lblMessage.setText("");
        }

        return isValid;
    }
    private void setError(Control field, String message) {
        field.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        field.setTooltip(new Tooltip(message));
    }

    private void resetStyles() {
        tfContenu.setStyle(null);
        taDescription.setStyle(null);
        cbType.setStyle(null);

    }
    private void goToListPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/FrontFXML/ReclamationList.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) tfContenu.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ===== CLEAR =====
    @FXML
    private void viderChamps() {
        clearFields();
    }

    private void clearFields() {
        tfContenu.clear();
        taDescription.clear();
        cbType.setValue(null);

    }

    // ===== NAVIGATION =====
    @FXML
    private void goToList(ActionEvent event) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/FrontFXML/ReclamationList.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Erreur navigation", "red");
        }
    }

    // ===== UTILS =====
    private String generateRef() {
        return "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void showMessage(String msg, String color) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold;");
    }
}