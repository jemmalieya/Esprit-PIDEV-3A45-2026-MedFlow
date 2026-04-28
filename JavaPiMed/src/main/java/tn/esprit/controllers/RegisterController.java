package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.tools.AuthThemeManager;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RegisterController {

    private static final Gson GSON = new Gson();

    public static boolean autoRevealOnLoad = false;

    @FXML private TextField cinField;
    @FXML private AnchorPane rootPane;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private TextField telephoneField;
    @FXML private TextField emailField;
    @FXML private TextField adresseField;
    @FXML private PasswordField motDePasseField;
    @FXML private PasswordField confirmerMotDePasseField;
    @FXML private CheckBox conditionsCheckBox;
    @FXML private VBox signupCard;
    @FXML private VBox welcomeCard;
    @FXML private Label panelTitleLabel;
    @FXML private Label panelSubtitleLabel;
    @FXML private HBox modeButtonRow;
    @FXML private Label signupStatusLabel;
    @FXML private HBox registerButtonBox;
    @FXML private HBox successButtonBox;
    @FXML private ComboBox<String> registrationTypeCombo;
    @FXML private Label staffInfoLabel;
    @FXML private Button skipAfterSignupBtn;
    @FXML private Button faceEnrollBtn;
    @FXML private Label cinErrorLabel;
    @FXML private Label nomErrorLabel;
    @FXML private Label prenomErrorLabel;
    @FXML private Label dateNaissanceErrorLabel;
    @FXML private Label telephoneErrorLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label adresseErrorLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private Label confirmPasswordErrorLabel;
    @FXML private VBox staffSectionBox;
    @FXML private ComboBox<String> staffTypeCombo;
    @FXML private TextField staffSpecialityField;
    @FXML private TextField staffExperienceField;
    @FXML private TextField staffEstablishmentField;
    @FXML private TextField staffAuthorizationField;
    @FXML private TextArea staffMessageArea;
    @FXML private Label staffTypeErrorLabel;
    @FXML private Label staffExperienceErrorLabel;
    @FXML private Label staffEstablishmentErrorLabel;
    @FXML private Label staffAuthorizationErrorLabel;
    @FXML private Label identityProofNameLabel;
    @FXML private Label diplomaProofNameLabel;
    @FXML private Label orderProofNameLabel;
    @FXML private Label professionalPhotoNameLabel;
    @FXML private Label otherDocumentsNameLabel;
    @FXML private Label identityProofErrorLabel;
    @FXML private Label diplomaProofErrorLabel;
    @FXML private Label orderProofErrorLabel;
    @FXML private Label professionalPhotoErrorLabel;

    private final UserService userService = new UserService();
    private User createdUser = null;

    private boolean cinValid;
    private boolean nomValid;
    private boolean prenomValid;
    private boolean dateNaissanceValid;
    private boolean telephoneValid;
    private boolean emailValid;
    private boolean adresseValid = false;
    private boolean passwordValid;
    private boolean confirmPasswordValid;
    private boolean staffTypeValid = true;
    private boolean staffExperienceValid = true;
    private boolean staffEstablishmentValid = true;
    private boolean staffAuthorizationValid = true;
    private boolean identityProofValid = true;
    private boolean diplomaProofValid = true;
    private boolean orderProofValid = true;
    private boolean professionalPhotoValid = true;
    private boolean panelRevealed;

    @FXML private HBox mainContent;
    @FXML private VBox visualPanel;
    @FXML private Button themeToggleBtn;

    private File selectedIdentityProof;
    private File selectedDiplomaProof;
    private File selectedOrderProof;
    private File selectedProfessionalPhoto;
    private final List<File> selectedOtherDocuments = new ArrayList<>();

    private static final String DARK_CLASS = "auth-page-signup";
    private static final String LIGHT_CLASS = "auth-page-light";
    private static final String TYPE_PATIENT = "PATIENT";
    private static final String TYPE_STAFF = "STAFF (demande d'acces)";
    private static final Map<String, String> STAFF_TYPE_CODES = createStaffTypeCodes();

    private static Map<String, String> createStaffTypeCodes() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Responsable patients", "RESP_PATIENTS");
        map.put("Responsable événements", "RESP_EVEN");
        map.put("Responsable produits", "RESP_PRODUCTS");
        map.put("Responsable réclamations / blog", "RESP_BLOG");
        map.put("Responsable utilisateurs", "RESP_USERS");
        return map;
    }

    @FXML
    private void initialize() {
        applyThemeToRoot();
        applyThemeButtonLabel();
        setupRegistrationType();
        setupStaffRequestForm();

        if (cinField != null) cinField.textProperty().addListener((obs, o, n) -> validateCin());
        if (nomField != null) nomField.textProperty().addListener((obs, o, n) -> validateNom());
        if (prenomField != null) prenomField.textProperty().addListener((obs, o, n) -> validatePrenom());
        if (dateNaissancePicker != null) dateNaissancePicker.valueProperty().addListener((obs, o, n) -> validateDateNaissance());
        if (telephoneField != null) telephoneField.textProperty().addListener((obs, o, n) -> validateTelephone());
        if (emailField != null) emailField.textProperty().addListener((obs, o, n) -> validateEmail());
        if (adresseField != null) adresseField.textProperty().addListener((obs, o, n) -> validateAdresse());
        if (motDePasseField != null) motDePasseField.textProperty().addListener((obs, o, n) -> { validatePassword(); validateConfirmPassword(); });
        if (confirmerMotDePasseField != null) confirmerMotDePasseField.textProperty().addListener((obs, o, n) -> validateConfirmPassword());

        if (autoRevealOnLoad) {
            autoRevealOnLoad = false;
            Platform.runLater(this::revealSignupPanel);
        }

        // ── New split-panel entrance ──
        if (mainContent != null) {
            playPanelEntranceAnimation();
        } else if (welcomeCard != null && modeButtonRow != null) {
            // Legacy entrance
            welcomeCard.setOpacity(0);
            welcomeCard.setTranslateY(24);
            modeButtonRow.setOpacity(0);
            FadeTransition cardFade = new FadeTransition(Duration.millis(460), welcomeCard);
            cardFade.setFromValue(0); cardFade.setToValue(1);
            TranslateTransition cardRise = new TranslateTransition(Duration.millis(460), welcomeCard);
            cardRise.setFromY(24); cardRise.setToY(0);
            cardRise.setInterpolator(Interpolator.EASE_BOTH);
            PauseTransition delay = new PauseTransition(Duration.millis(120));
            FadeTransition buttonsFade = new FadeTransition(Duration.millis(280), modeButtonRow);
            buttonsFade.setFromValue(0); buttonsFade.setToValue(1);
            new SequentialTransition(new ParallelTransition(cardFade, cardRise), delay, buttonsFade).play();
        }
    }

    private void setupRegistrationType() {
        if (registrationTypeCombo == null) return;
        registrationTypeCombo.getItems().setAll(TYPE_PATIENT, TYPE_STAFF);
        registrationTypeCombo.getSelectionModel().select(TYPE_PATIENT);
        registrationTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshRegistrationTypeUI());
        refreshRegistrationTypeUI();
    }

    private void setupStaffRequestForm() {
        if (staffTypeCombo != null) {
            staffTypeCombo.getItems().setAll(STAFF_TYPE_CODES.keySet());
            staffTypeCombo.valueProperty().addListener((obs, oldValue, newValue) -> validateStaffType());
        }
        if (staffExperienceField != null) {
            staffExperienceField.textProperty().addListener((obs, oldValue, newValue) -> validateStaffExperience());
        }
        if (staffEstablishmentField != null) {
            staffEstablishmentField.textProperty().addListener((obs, oldValue, newValue) -> validateStaffEstablishment());
        }
        if (staffAuthorizationField != null) {
            staffAuthorizationField.textProperty().addListener((obs, oldValue, newValue) -> validateStaffAuthorization());
        }
        updateSelectedFileLabel(identityProofNameLabel, null, false);
        updateSelectedFileLabel(diplomaProofNameLabel, null, false);
        updateSelectedFileLabel(orderProofNameLabel, null, false);
        updateSelectedFileLabel(professionalPhotoNameLabel, null, false);
        updateSelectedFileLabel(otherDocumentsNameLabel, null, true);
    }

    private boolean isStaffRegistration() {
        return registrationTypeCombo != null
                && TYPE_STAFF.equals(registrationTypeCombo.getSelectionModel().getSelectedItem());
    }

    private void refreshRegistrationTypeUI() {
        boolean staffMode = isStaffRegistration();
        if (staffInfoLabel != null) {
            staffInfoLabel.setVisible(staffMode);
            staffInfoLabel.setManaged(staffMode);
        }
        if (staffSectionBox != null) {
            staffSectionBox.setVisible(staffMode);
            staffSectionBox.setManaged(staffMode);
        }
        if (!staffMode) {
            clearStaffValidationFeedback();
        }
    }

    @FXML
    private void handleToggleTheme(ActionEvent event) {
        AuthThemeManager.toggle();
        applyThemeToRoot();
        applyThemeButtonLabel();
    }

    private void applyThemeToRoot() {
        if (rootPane == null) return;
        rootPane.getStyleClass().removeAll(DARK_CLASS, LIGHT_CLASS);
        rootPane.getStyleClass().add(AuthThemeManager.isLightMode() ? LIGHT_CLASS : DARK_CLASS);
    }

    private void applyThemeButtonLabel() {
        if (themeToggleBtn == null) return;
        themeToggleBtn.setText(AuthThemeManager.isLightMode() ? "Mode sombre" : "Mode clair");
    }

    private void playPanelEntranceAnimation() {
        if (visualPanel != null) {
            visualPanel.setOpacity(0);
            visualPanel.setTranslateX(-40);
            FadeTransition vFade = new FadeTransition(Duration.millis(520), visualPanel);
            vFade.setFromValue(0); vFade.setToValue(1);
            TranslateTransition vSlide = new TranslateTransition(Duration.millis(520), visualPanel);
            vSlide.setFromX(-40); vSlide.setToX(0);
            vSlide.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1.0));
            new ParallelTransition(vFade, vSlide).play();
        }
        if (signupCard != null) {
            signupCard.setOpacity(0);
            signupCard.setTranslateX(40);
            FadeTransition fFade = new FadeTransition(Duration.millis(520), signupCard);
            fFade.setFromValue(0); fFade.setToValue(1);
            fFade.setDelay(Duration.millis(120));
            TranslateTransition fSlide = new TranslateTransition(Duration.millis(520), signupCard);
            fSlide.setFromX(40); fSlide.setToX(0);
            fSlide.setDelay(Duration.millis(120));
            fSlide.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1.0));
            new ParallelTransition(fFade, fSlide).play();
        }
    }

    @FXML
    private void handleRevealSignupPanel(ActionEvent event) {
        revealSignupPanel();
    }

    private void revealSignupPanel() {
        if (panelRevealed || welcomeCard == null || signupCard == null) return;
        panelRevealed = true;

        FadeTransition btnFade = new FadeTransition(Duration.millis(140), modeButtonRow);
        btnFade.setFromValue(1);
        btnFade.setToValue(0);

        TranslateTransition cardSlide = new TranslateTransition(Duration.millis(520), welcomeCard);
        cardSlide.setFromX(0);
        cardSlide.setToX(-248);
        cardSlide.setInterpolator(Interpolator.SPLINE(0.18, 0.84, 0.24, 1.0));

        ScaleTransition cardSettle = new ScaleTransition(Duration.millis(520), welcomeCard);
        cardSettle.setFromX(1.0);
        cardSettle.setFromY(1.0);
        cardSettle.setToX(0.985);
        cardSettle.setToY(0.985);
        cardSettle.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition formFade = new FadeTransition(Duration.millis(360), signupCard);
        formFade.setFromValue(0);
        formFade.setToValue(1);
        formFade.setDelay(Duration.millis(150));
        formFade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition formSlide = new TranslateTransition(Duration.millis(360), signupCard);
        formSlide.setFromX(112);
        formSlide.setToX(0);
        formSlide.setDelay(Duration.millis(150));
        formSlide.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1));

        ScaleTransition formPop = new ScaleTransition(Duration.millis(360), signupCard);
        formPop.setFromX(0.97);
        formPop.setFromY(0.97);
        formPop.setToX(1.0);
        formPop.setToY(1.0);
        formPop.setDelay(Duration.millis(150));
        formPop.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition p = new ParallelTransition(btnFade, cardSlide, cardSettle, formFade, formSlide, formPop);
        p.setOnFinished(e -> {
            if (modeButtonRow != null) {
                modeButtonRow.setVisible(false);
                modeButtonRow.setManaged(false);
            }
            signupCard.setMouseTransparent(false);
            if (panelTitleLabel != null) panelTitleLabel.setText("Espace Inscription");
            if (panelSubtitleLabel != null) panelSubtitleLabel.setText("Remplissez vos informations pour creer votre compte MedFlow.");
            if (!welcomeCard.getStyleClass().contains("welcome-master-card-compact")) {
                welcomeCard.getStyleClass().add("welcome-master-card-compact");
            }
        });
        p.play();
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        if (!validateAll()) {
            setSignupStatus("Veuillez corriger les champs en rouge avant de continuer.", false);
            return;
        }
        if (!conditionsCheckBox.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "Inscription", "Veuillez accepter les conditions d'utilisation.");
            return;
        }

        try {
            boolean staffMode = isStaffRegistration();

            User user = new User();
            user.setCin(cinField.getText().trim());
            user.setNom(nomField.getText().trim());
            user.setPrenom(prenomField.getText().trim());
            user.setDateNaissance(dateNaissancePicker.getValue());
            user.setTelephoneUser(telephoneField.getText().trim());
            user.setEmailUser(emailField.getText().trim().toLowerCase());
            user.setAdresseUser(adresseField.getText() == null ? null : adresseField.getText().trim());
            user.setProfilePicture("default.png");
            user.setPassword(BCrypt.hashpw(motDePasseField.getText(), BCrypt.gensalt(13)));
            user.setVerified(!staffMode);
            user.setStatutCompte(staffMode ? "EN_ATTENTE_VALIDATION" : "ACTIF");
            user.setRoleSysteme(staffMode ? "STAFF" : "PATIENT");
            if (staffMode) {
                user.setTypeStaff(getSelectedStaffTypeCode());
                user.setStaffRequestStatus("PENDING");
                user.setStaffRequestType("STAFF_SIGNUP");
                user.setStaffRequestMessage(normalizeOptionalText(staffMessageArea == null ? null : staffMessageArea.getText()));
                user.setStaffRequestReason(buildStaffRequestReason());
                // La piece principale de revue admin devient le CV
                user.setStaffRequestProofPath(getAbsolutePath(selectedOrderProof));
                user.setStaffDocuments(buildStaffDocumentsPayload());
                user.setStaffRequestedAt(LocalDateTime.now());
            }

            String dbError = userService.ajouterAvecRetour(user);
            if (dbError == null) {
                if (user.getId() == 0) {
                    User persistedUser = userService.findByEmail(user.getEmailUser());
                    if (persistedUser != null) {
                        user.setId(persistedUser.getId());
                    }
                }
            }

            if (dbError == null && user.getId() != 0) {
                createdUser = user;
                clearValidationStyles();
                if (staffMode) {
                    setSignupStatus("Demande staff envoyee. Un administrateur validera votre acces.", true);
                } else {
                    setSignupStatus("Compte cree avec succes. Enregistrez votre visage ou connectez-vous.", true);
                }
                if (registerButtonBox != null) { registerButtonBox.setVisible(false); registerButtonBox.setManaged(false); }
                if (successButtonBox != null) { successButtonBox.setVisible(true); successButtonBox.setManaged(true); }

                if (skipAfterSignupBtn != null) {
                    skipAfterSignupBtn.setText(staffMode ? "Aller a la connexion" : "Passer");
                }
                if (faceEnrollBtn != null) {
                    faceEnrollBtn.setVisible(!staffMode);
                    faceEnrollBtn.setManaged(!staffMode);
                }
            } else {
                setSignupStatus("Echec enregistrement: " + (dbError == null ? "verifiez vos donnees." : dbError), false);
            }
        } catch (Exception e) {
            setSignupStatus("Une erreur est survenue lors de la creation du compte.", false);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToLogin(ActionEvent event) {
        LoginController.autoRevealOnLoad = true;
        playCardTransitionAndNavigate(event, "/FrontFXML/Login.fxml", "MedFlow - Connexion", true);
    }

    @FXML
    private void handleFaceEnrollmentSignup(ActionEvent event) {
        if (createdUser == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "L'utilisateur créé est introuvable.");
            handleGoToLogin(event);
            return;
        }

        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/FaceEnrollment.fxml"));
            Parent root = loader.load();

            FaceEnrollmentController enrollmentController = loader.getController();
            enrollmentController.setOnEnrollmentComplete(new FaceEnrollmentController.OnEnrollmentComplete() {
                @Override
                public void onSuccess(String faceEmbedding) {
                    createdUser.setFaceReferenceEmbedding(faceEmbedding);
                    createdUser.setFaceLoginEnabled(true);
                    createdUser.setFaceEnrolledAt(java.time.LocalDateTime.now());

                    if (createdUser.getId() <= 0 && createdUser.getEmailUser() != null) {
                        User refreshed = userService.findByEmail(createdUser.getEmailUser());
                        if (refreshed != null) {
                            createdUser.setId(refreshed.getId());
                        }
                    }

                    boolean saved = userService.updateFaceEnrollment(createdUser.getId(), faceEmbedding);
                    if (!saved) {
                        String reason = userService.getLastFaceEnrollmentError();
                        String details = (reason == null || reason.isBlank()) ? "Cause inconnue." : reason;
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'enregistrer le visage. " + details));
                        return;
                    }

                    Platform.runLater(() -> {
                        LoginController.autoRevealOnLoad = true;
                        try {
                            FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/FrontFXML/Login.fxml"));
                            Parent loginRoot = loginLoader.load();
                            Scene scene = new Scene(loginRoot, 1400, 820);
                            stage.setScene(scene);
                            stage.setTitle("MedFlow - Connexion");
                            stage.show();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

                @Override
                public void onSkip() {
                    Platform.runLater(() -> handleGoToLogin(event));
                }
            });

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("MedFlow - Enregistrement du visage");
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'enregistrement du visage: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void playCardTransitionAndNavigate(ActionEvent event, String resourcePath, String title, boolean toRight) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        Node target = stage.getScene().getRoot();

        FadeTransition fade = new FadeTransition(Duration.millis(260), target);
        fade.setFromValue(1);
        fade.setToValue(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(260), target);
        slide.setFromX(0);
        slide.setToX(toRight ? 120 : -120);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scale = new ScaleTransition(Duration.millis(260), target);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.97);
        scale.setToY(0.97);

        ParallelTransition p = new ParallelTransition(fade, slide, scale);
        p.setOnFinished(e -> navigateTo(stage, resourcePath, title));
        p.play();
    }

    private void navigateTo(Stage stage, String resourcePath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Parent root = loader.load();
            root.setOpacity(0);
            stage.setScene(new Scene(root, 1280, 720));
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.show();
            FadeTransition fadeIn = new FadeTransition(Duration.millis(320), root);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);
            TranslateTransition slideIn = new TranslateTransition(Duration.millis(320), root);
            slideIn.setFromY(18); slideIn.setToY(0);
            slideIn.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fadeIn, slideIn).play();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir la page demandee.");
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setSignupStatus(String message, boolean success) {
        if (signupStatusLabel == null) return;
        signupStatusLabel.setText(message == null ? "" : message);
        boolean visible = message != null && !message.isEmpty();
        signupStatusLabel.setVisible(visible);
        signupStatusLabel.setManaged(visible);
        signupStatusLabel.getStyleClass().removeAll("status-error", "status-success");
        if (visible) signupStatusLabel.getStyleClass().add(success ? "status-success" : "status-error");
    }

    private void markError(Control field) {
        if (field != null) field.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.2; -fx-background-radius: 6; -fx-border-radius: 6;");
    }

    private void markValid(Control field) {
        if (field != null) field.setStyle("-fx-border-color: #16a34a; -fx-border-width: 1.2; -fx-background-radius: 6; -fx-border-radius: 6;");
    }

    private void showFieldError(Label label, String message) {
        if (label == null) return;
        label.setText(message == null ? "" : message);
        boolean visible = message != null && !message.isBlank();
        label.setVisible(visible);
        label.setManaged(visible);
    }

    private void clearValidationStyles() {
        if (cinField != null) cinField.setStyle("");
        if (nomField != null) nomField.setStyle("");
        if (prenomField != null) prenomField.setStyle("");
        if (dateNaissancePicker != null) dateNaissancePicker.setStyle("");
        if (telephoneField != null) telephoneField.setStyle("");
        if (emailField != null) emailField.setStyle("");
        if (adresseField != null) adresseField.setStyle("");
        if (motDePasseField != null) motDePasseField.setStyle("");
        if (confirmerMotDePasseField != null) confirmerMotDePasseField.setStyle("");
        clearStaffValidationFeedback();

        showFieldError(cinErrorLabel, "");
        showFieldError(nomErrorLabel, "");
        showFieldError(prenomErrorLabel, "");
        showFieldError(dateNaissanceErrorLabel, "");
        showFieldError(telephoneErrorLabel, "");
        showFieldError(emailErrorLabel, "");
        showFieldError(adresseErrorLabel, "");
        showFieldError(passwordErrorLabel, "");
        showFieldError(confirmPasswordErrorLabel, "");
    }

    private void clearStaffValidationFeedback() {
        staffTypeValid = true;
        staffExperienceValid = true;
        staffEstablishmentValid = true;
        staffAuthorizationValid = true;
        identityProofValid = true;
        diplomaProofValid = true;
        orderProofValid = true;
        professionalPhotoValid = true;

        if (staffTypeCombo != null) staffTypeCombo.setStyle("");
        if (staffExperienceField != null) staffExperienceField.setStyle("");
        if (staffEstablishmentField != null) staffEstablishmentField.setStyle("");
        if (staffAuthorizationField != null) staffAuthorizationField.setStyle("");

        showFieldError(staffTypeErrorLabel, "");
        showFieldError(staffExperienceErrorLabel, "");
        showFieldError(staffEstablishmentErrorLabel, "");
        showFieldError(staffAuthorizationErrorLabel, "");
        showFieldError(identityProofErrorLabel, "");
        showFieldError(diplomaProofErrorLabel, "");
        showFieldError(orderProofErrorLabel, "");
        showFieldError(professionalPhotoErrorLabel, "");
    }

    private void validateCin() {
        String value = cinField.getText() == null ? "" : cinField.getText().trim();
        if (value.matches("\\d{8}")) {
            cinValid = true;
            markValid(cinField);
            showFieldError(cinErrorLabel, "");
        } else {
            cinValid = false;
            markError(cinField);
            showFieldError(cinErrorLabel, value.isEmpty() ? "Le CIN est obligatoire." : "Le CIN doit contenir exactement 8 chiffres.");
        }
    }

    private void validateNom() {
        String value = nomField.getText() == null ? "" : nomField.getText().trim();
        if (value.isEmpty()) {
            nomValid = false;
            markError(nomField);
            showFieldError(nomErrorLabel, "Le nom est obligatoire.");
        } else {
            nomValid = true;
            markValid(nomField);
            showFieldError(nomErrorLabel, "");
        }
    }

    private void validatePrenom() {
        String value = prenomField.getText() == null ? "" : prenomField.getText().trim();
        if (value.isEmpty()) {
            prenomValid = false;
            markError(prenomField);
            showFieldError(prenomErrorLabel, "Le prenom est obligatoire.");
        } else {
            prenomValid = true;
            markValid(prenomField);
            showFieldError(prenomErrorLabel, "");
        }
    }

    private void validateDateNaissance() {
        if (dateNaissancePicker.getValue() != null && !dateNaissancePicker.getValue().isAfter(java.time.LocalDate.now())) {
            dateNaissanceValid = true;
            markValid(dateNaissancePicker);
            showFieldError(dateNaissanceErrorLabel, "");
        } else {
            dateNaissanceValid = false;
            markError(dateNaissancePicker);
            showFieldError(dateNaissanceErrorLabel, "Date de naissance invalide.");
        }
    }

    private void validateTelephone() {
        String value = telephoneField.getText() == null ? "" : telephoneField.getText().trim();
        if (value.matches("\\d{8}") || value.matches("\\+216\\d{8}")) {
            telephoneValid = true;
            markValid(telephoneField);
            showFieldError(telephoneErrorLabel, "");
        } else {
            telephoneValid = false;
            markError(telephoneField);
            showFieldError(telephoneErrorLabel, "Format invalide.");
        }
    }

    private void validateEmail() {
        String value = emailField.getText() == null ? "" : emailField.getText().trim();
        if (value.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            emailValid = true;
            markValid(emailField);
            showFieldError(emailErrorLabel, "");
        } else {
            emailValid = false;
            markError(emailField);
            showFieldError(emailErrorLabel, "Adresse e-mail invalide.");
        }
    }

    private void validateAdresse() {
        String value = adresseField == null || adresseField.getText() == null ? "" : adresseField.getText().trim();
        if (value.isEmpty()) {
            adresseValid = false;
            markError(adresseField);
            showFieldError(adresseErrorLabel, "L'adresse est obligatoire.");
        } else {
            adresseValid = true;
            markValid(adresseField);
            showFieldError(adresseErrorLabel, "");
        }
    }

    private void validatePassword() {
        String value = motDePasseField.getText() == null ? "" : motDePasseField.getText();
        if (value.length() >= 8) {
            passwordValid = true;
            markValid(motDePasseField);
            showFieldError(passwordErrorLabel, "");
        } else {
            passwordValid = false;
            markError(motDePasseField);
            showFieldError(passwordErrorLabel, "Minimum 8 caracteres.");
        }
    }

    private void validateConfirmPassword() {
        String pwd = motDePasseField.getText() == null ? "" : motDePasseField.getText();
        String confirm = confirmerMotDePasseField.getText() == null ? "" : confirmerMotDePasseField.getText();
        if (!pwd.isEmpty() && pwd.equals(confirm)) {
            confirmPasswordValid = true;
            markValid(confirmerMotDePasseField);
            showFieldError(confirmPasswordErrorLabel, "");
        } else {
            confirmPasswordValid = false;
            markError(confirmerMotDePasseField);
            showFieldError(confirmPasswordErrorLabel, "Les mots de passe ne correspondent pas.");
        }
    }

    private void validateStaffType() {
        if (!isStaffRegistration()) {
            staffTypeValid = true;
            if (staffTypeCombo != null) staffTypeCombo.setStyle("");
            showFieldError(staffTypeErrorLabel, "");
            return;
        }
        String value = getSelectedStaffTypeCode();
        if (value == null || value.isBlank()) {
            staffTypeValid = false;
            markError(staffTypeCombo);
            showFieldError(staffTypeErrorLabel, "Sélectionnez un type de staff.");
        } else {
            staffTypeValid = true;
            markValid(staffTypeCombo);
            showFieldError(staffTypeErrorLabel, "");
        }
    }

    private void validateStaffExperience() {
        if (!isStaffRegistration()) {
            staffExperienceValid = true;
            if (staffExperienceField != null) staffExperienceField.setStyle("");
            showFieldError(staffExperienceErrorLabel, "");
            return;
        }
        String value = staffExperienceField == null || staffExperienceField.getText() == null ? "" : staffExperienceField.getText().trim();
        if (value.isEmpty()) {
            staffExperienceValid = true;
            if (staffExperienceField != null) staffExperienceField.setStyle("");
            showFieldError(staffExperienceErrorLabel, "");
            return;
        }
        if (value.matches("\\d{1,2}") && Integer.parseInt(value) <= 60) {
            staffExperienceValid = true;
            markValid(staffExperienceField);
            showFieldError(staffExperienceErrorLabel, "");
        } else {
            staffExperienceValid = false;
            markError(staffExperienceField);
            showFieldError(staffExperienceErrorLabel, "Indiquez un nombre d'années valide (0 à 60).");
        }
    }

    private void validateStaffEstablishment() {
        if (!isStaffRegistration()) {
            staffEstablishmentValid = true;
            if (staffEstablishmentField != null) staffEstablishmentField.setStyle("");
            showFieldError(staffEstablishmentErrorLabel, "");
            return;
        }
        String value = staffEstablishmentField == null || staffEstablishmentField.getText() == null ? "" : staffEstablishmentField.getText().trim();
        if (value.isBlank()) {
            staffEstablishmentValid = false;
            markError(staffEstablishmentField);
            showFieldError(staffEstablishmentErrorLabel, "L'établissement actuel est obligatoire.");
        } else {
            staffEstablishmentValid = true;
            markValid(staffEstablishmentField);
            showFieldError(staffEstablishmentErrorLabel, "");
        }
    }

    private void validateStaffAuthorization() {
        if (!isStaffRegistration()) {
            staffAuthorizationValid = true;
            if (staffAuthorizationField != null) staffAuthorizationField.setStyle("");
            showFieldError(staffAuthorizationErrorLabel, "");
            return;
        }
        String value = staffAuthorizationField == null || staffAuthorizationField.getText() == null ? "" : staffAuthorizationField.getText().trim();
        if (value.length() < 4) {
            staffAuthorizationValid = false;
            markError(staffAuthorizationField);
            showFieldError(staffAuthorizationErrorLabel, "Le numéro d'autorisation est obligatoire.");
        } else {
            staffAuthorizationValid = true;
            markValid(staffAuthorizationField);
            showFieldError(staffAuthorizationErrorLabel, "");
        }
    }

    private void validateIdentityProof() {
        if (!isStaffRegistration()) {
            identityProofValid = true;
            showFieldError(identityProofErrorLabel, "");
            return;
        }
        identityProofValid = selectedIdentityProof != null;
        showFieldError(identityProofErrorLabel, identityProofValid ? "" : "La pièce d'identité est obligatoire.");
    }

    private void validateDiplomaProof() {
        if (!isStaffRegistration()) {
            diplomaProofValid = true;
            showFieldError(diplomaProofErrorLabel, "");
            return;
        }
        diplomaProofValid = selectedDiplomaProof != null;
        showFieldError(diplomaProofErrorLabel, diplomaProofValid ? "" : "Le diplôme médical est obligatoire.");
    }

    private void validateOrderProof() {
        if (!isStaffRegistration()) {
            orderProofValid = true;
            showFieldError(orderProofErrorLabel, "");
            return;
        }
        orderProofValid = selectedOrderProof != null;
        showFieldError(orderProofErrorLabel, orderProofValid ? "" : "Le CV est obligatoire.");
    }

    private void validateProfessionalPhoto() {
        // La photo professionnelle devient optionnelle dans le flow CV-first.
        professionalPhotoValid = true;
        showFieldError(professionalPhotoErrorLabel, "");
    }

    @FXML
    private void handleSelectIdentityProof(ActionEvent event) {
        selectedIdentityProof = chooseSingleFile("Sélectionner la pièce d'identité", true);
        updateSelectedFileLabel(identityProofNameLabel, selectedIdentityProof, false);
        validateIdentityProof();
    }

    @FXML
    private void handleSelectDiplomaProof(ActionEvent event) {
        selectedDiplomaProof = chooseSingleFile("Sélectionner le diplôme médical", true);
        updateSelectedFileLabel(diplomaProofNameLabel, selectedDiplomaProof, false);
        validateDiplomaProof();
    }

    @FXML
    private void handleSelectOrderProof(ActionEvent event) {
        selectedOrderProof = chooseCvFile();
        updateSelectedFileLabel(orderProofNameLabel, selectedOrderProof, false);
        validateOrderProof();
    }

    @FXML
    private void handleSelectProfessionalPhoto(ActionEvent event) {
        selectedProfessionalPhoto = chooseSingleFile("Sélectionner la photo professionnelle", false);
        updateSelectedFileLabel(professionalPhotoNameLabel, selectedProfessionalPhoto, false);
        validateProfessionalPhoto();
    }

    @FXML
    private void handleSelectOtherDocuments(ActionEvent event) {
        List<File> files = chooseMultipleFiles("Sélectionner d'autres documents");
        selectedOtherDocuments.clear();
        if (files != null) {
            selectedOtherDocuments.addAll(files);
        }
        updateSelectedFileLabel(otherDocumentsNameLabel, selectedOtherDocuments.isEmpty() ? null : selectedOtherDocuments.get(0), true);
    }

    private File chooseSingleFile(String title, boolean allowDocumentsAndImages) {
        FileChooser chooser = buildFileChooser(title, allowDocumentsAndImages);
        Stage stage = rootPane != null && rootPane.getScene() != null ? (Stage) rootPane.getScene().getWindow() : null;
        return stage == null ? null : chooser.showOpenDialog(stage);
    }

    private List<File> chooseMultipleFiles(String title) {
        FileChooser chooser = buildFileChooser(title, true);
        Stage stage = rootPane != null && rootPane.getScene() != null ? (Stage) rootPane.getScene().getWindow() : null;
        return stage == null ? null : chooser.showOpenMultipleDialog(stage);
    }

    private File chooseCvFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selectionner le CV");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CV (PDF/DOC/DOCX)", "*.pdf", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Word", "*.doc", "*.docx")
        );
        Stage stage = rootPane != null && rootPane.getScene() != null ? (Stage) rootPane.getScene().getWindow() : null;
        return stage == null ? null : chooser.showOpenDialog(stage);
    }

    private FileChooser buildFileChooser(String title, boolean allowDocumentsAndImages) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        if (allowDocumentsAndImages) {
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Documents staff", "*.pdf", "*.png", "*.jpg", "*.jpeg"),
                    new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
            );
        } else {
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
            );
        }
        return chooser;
    }

    private void updateSelectedFileLabel(Label label, File file, boolean multiple) {
        if (label == null) {
            return;
        }
        if (multiple) {
            label.setText(selectedOtherDocuments.isEmpty()
                    ? "Aucun fichier choisi"
                    : selectedOtherDocuments.size() + " fichier(s) sélectionné(s)");
            return;
        }
        label.setText(file == null ? "Aucun fichier choisi" : file.getName());
    }

    private String getSelectedStaffTypeCode() {
        if (staffTypeCombo == null) {
            return null;
        }
        return STAFF_TYPE_CODES.get(staffTypeCombo.getValue());
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildStaffRequestReason() {
        List<String> details = new ArrayList<>();
        String speciality = normalizeOptionalText(staffSpecialityField == null ? null : staffSpecialityField.getText());
        String experience = normalizeOptionalText(staffExperienceField == null ? null : staffExperienceField.getText());
        String establishment = normalizeOptionalText(staffEstablishmentField == null ? null : staffEstablishmentField.getText());
        String authorization = normalizeOptionalText(staffAuthorizationField == null ? null : staffAuthorizationField.getText());

        String staffTypeCode = getSelectedStaffTypeCode();
        if (staffTypeCode != null) details.add("type=" + staffTypeCode);
        if (speciality != null) details.add("specialite=" + speciality);
        if (experience != null) details.add("experience=" + experience + " ans");
        if (establishment != null) details.add("etablissement=" + establishment);
        if (authorization != null) details.add("autorisation=" + authorization);

        return details.isEmpty() ? null : String.join(" | ", details);
    }

    private String buildStaffDocumentsPayload() {
        List<String> documents = new ArrayList<>();
        // Ordre de revue admin: CV, piece d'identite, diplome, puis pieces optionnelles.
        addPathIfPresent(documents, selectedOrderProof);
        addPathIfPresent(documents, selectedIdentityProof);
        addPathIfPresent(documents, selectedDiplomaProof);
        addPathIfPresent(documents, selectedProfessionalPhoto);
        for (File file : selectedOtherDocuments) {
            addPathIfPresent(documents, file);
        }
        return documents.isEmpty() ? null : GSON.toJson(documents);
    }

    private void addPathIfPresent(List<String> documents, File file) {
        if (file != null) {
            documents.add(file.getAbsolutePath());
        }
    }

    private String getAbsolutePath(File file) {
        return file == null ? null : file.getAbsolutePath();
    }

    private boolean validateAll() {
        validateCin();
        validateNom();
        validatePrenom();
        validateDateNaissance();
        validateTelephone();
        validateEmail();
        validateAdresse();
        validatePassword();
        validateConfirmPassword();
        if (isStaffRegistration()) {
            validateStaffType();
            validateStaffExperience();
            validateStaffEstablishment();
            validateStaffAuthorization();
            validateIdentityProof();
            validateDiplomaProof();
            validateOrderProof();
            validateProfessionalPhoto();
        }
        return cinValid
                && nomValid
                && prenomValid
                && dateNaissanceValid
                && telephoneValid
                && emailValid
                && adresseValid
                && passwordValid
                && confirmPasswordValid
                && staffTypeValid
                && staffExperienceValid
                && staffEstablishmentValid
                && staffAuthorizationValid
                && identityProofValid
                && diplomaProofValid
                && orderProofValid
                && professionalPhotoValid;
    }
}
