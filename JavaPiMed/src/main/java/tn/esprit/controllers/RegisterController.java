package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.mindrot.jbcrypt.BCrypt;

import tn.esprit.entities.User;
import tn.esprit.services.UserService;

public class RegisterController {

    @FXML
    private TextField cinField;

    @FXML
    private TextField nomField;

    @FXML
    private TextField prenomField;

    @FXML
    private DatePicker dateNaissancePicker;

    @FXML
    private TextField telephoneField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField adresseField;

    @FXML
    private PasswordField motDePasseField;

    @FXML
    private PasswordField confirmerMotDePasseField;

    @FXML
    private CheckBox conditionsCheckBox;

    @FXML
    private VBox signupCard;

    @FXML
    private Label signupStatusLabel;

    private final UserService userService = new UserService();

    // Validation flags
    private boolean cinValid;
    private boolean nomValid;
    private boolean prenomValid;
    private boolean dateNaissanceValid;
    private boolean telephoneValid;
    private boolean emailValid;
    private boolean adresseValid = true; // facultatif
    private boolean passwordValid;
    private boolean confirmPasswordValid;

    @FXML
    private void initialize() {
        if (signupCard != null) {
            signupCard.setOpacity(0);
            signupCard.setTranslateY(16);

            FadeTransition fade = new FadeTransition(Duration.millis(260), signupCard);
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(260), signupCard);
            slide.setFromY(16);
            slide.setToY(0);

            ParallelTransition transition = new ParallelTransition(fade, slide);
            transition.play();
        }

        // Live validation listeners
        if (cinField != null) {
            cinField.textProperty().addListener((obs, oldV, newV) -> validateCin());
        }
        if (nomField != null) {
            nomField.textProperty().addListener((obs, o, n) -> validateNom());
        }
        if (prenomField != null) {
            prenomField.textProperty().addListener((obs, o, n) -> validatePrenom());
        }
        if (dateNaissancePicker != null) {
            dateNaissancePicker.valueProperty().addListener((obs, o, n) -> validateDateNaissance());
        }
        if (telephoneField != null) {
            telephoneField.textProperty().addListener((obs, o, n) -> validateTelephone());
        }
        if (emailField != null) {
            emailField.textProperty().addListener((obs, o, n) -> validateEmail());
        }
        if (adresseField != null) {
            adresseField.textProperty().addListener((obs, o, n) -> validateAdresse());
        }
        if (motDePasseField != null) {
            motDePasseField.textProperty().addListener((obs, o, n) -> {
                validatePassword();
                validateConfirmPassword();
            });
        }
        if (confirmerMotDePasseField != null) {
            confirmerMotDePasseField.textProperty().addListener((obs, o, n) -> validateConfirmPassword());
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        // First, validate all fields
        if (!validateAll()) {
            setSignupStatus("Veuillez corriger les champs en rouge avant de continuer.", false);
            return;
        }

        if (!conditionsCheckBox.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "Inscription", "Veuillez accepter les conditions d'utilisation pour continuer.");
            return;
        }

        String password = motDePasseField.getText();

        try {
            User user = new User();
            user.setCin(cinField.getText() != null ? cinField.getText().trim() : null);
            user.setNom(nomField.getText() != null ? nomField.getText().trim() : null);
            user.setPrenom(prenomField.getText() != null ? prenomField.getText().trim() : null);
            if (dateNaissancePicker.getValue() != null) {
                user.setDateNaissance(dateNaissancePicker.getValue());
            }
            user.setTelephoneUser(telephoneField.getText() != null ? telephoneField.getText().trim() : null);
            user.setEmailUser(emailField.getText() != null ? emailField.getText().trim() : null);
            user.setAdresseUser(adresseField.getText() != null ? adresseField.getText().trim() : null);

            // Default profile picture so the column is never null
            user.setProfilePicture("default.png");

            String hashed = BCrypt.hashpw(password, BCrypt.gensalt(13));
            user.setPassword(hashed);

            user.setVerified(true);
            user.setStatutCompte("ACTIF");
            user.setRoleSysteme("PATIENT");

            String dbError = userService.ajouterAvecRetour(user);
            if (dbError == null && user.getId() != 0) {
                clearValidationStyles();
                setSignupStatus("Votre compte MedFlow a été créé. Vous pouvez maintenant vous connecter.", true);
                handleGoToLogin(event);
            } else {
                String message = "Le compte n'a pas pu être enregistré en base. Vérifiez les champs et réessayez.";
                if (dbError != null && !dbError.isEmpty()) {
                    message += "\n\nDétail technique : " + dbError;
                }
                setSignupStatus(message, false);
            }
        } catch (Exception e) {
            setSignupStatus("Une erreur est survenue lors de la création du compte.", false);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToLogin(ActionEvent event) {
        playCardTransitionAndNavigate(event, "/FrontFXML/Login.fxml", "MedFlow - Connexion", true);
    }

    private void navigateTo(Stage stage, String resourcePath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Parent root = loader.load();
            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir la page demandée.");
            e.printStackTrace();
        }
    }

    private void playCardTransitionAndNavigate(ActionEvent event, String resourcePath, String title, boolean toRight) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();

        Node target = signupCard != null ? (Node) signupCard : stage.getScene().getRoot();

        FadeTransition fade = new FadeTransition(Duration.millis(260), target);
        fade.setFromValue(1);
        fade.setToValue(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(260), target);
        slide.setFromX(0);
        slide.setToX(toRight ? 40 : -40);

        ParallelTransition transition = new ParallelTransition(fade, slide);
        transition.setOnFinished(e -> {
            target.setOpacity(1);
            target.setTranslateX(0);
            navigateTo(stage, resourcePath, title);
        });
        transition.play();
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
        String color = success ? "#16a34a" : "#dc2626";
        signupStatusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
    }

    private void markError(TextField field) {
        if (field != null) {
            field.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.2; -fx-background-radius: 6; -fx-border-radius: 6;");
        }
    }

    private void markValid(TextField field) {
        if (field != null) {
            field.setStyle("-fx-border-color: #16a34a; -fx-border-width: 1.2; -fx-background-radius: 6; -fx-border-radius: 6;");
        }
    }

    private void clearValidationStyles() {
        if (cinField != null) cinField.setStyle("");
        if (nomField != null) nomField.setStyle("");
        if (prenomField != null) prenomField.setStyle("");
        if (telephoneField != null) telephoneField.setStyle("");
        if (emailField != null) emailField.setStyle("");
        if (adresseField != null) adresseField.setStyle("");
        if (motDePasseField != null) motDePasseField.setStyle("");
        if (confirmerMotDePasseField != null) confirmerMotDePasseField.setStyle("");
    }

    private void validateCin() {
        String value = cinField.getText() == null ? "" : cinField.getText().trim();
        if (value.matches("\\d{8}")) {
            cinValid = true;
            markValid(cinField);
        } else {
            cinValid = false;
            markError(cinField);
        }
    }

    private void validateNom() {
        String value = nomField.getText() == null ? "" : nomField.getText().trim();
        if (!value.isEmpty()) {
            nomValid = true;
            markValid(nomField);
        } else {
            nomValid = false;
            markError(nomField);
        }
    }

    private void validatePrenom() {
        String value = prenomField.getText() == null ? "" : prenomField.getText().trim();
        if (!value.isEmpty()) {
            prenomValid = true;
            markValid(prenomField);
        } else {
            prenomValid = false;
            markError(prenomField);
        }
    }

    private void validateDateNaissance() {
        if (dateNaissancePicker.getValue() != null) {
            dateNaissanceValid = true;
        } else {
            dateNaissanceValid = false;
        }
    }

    private void validateTelephone() {
        String value = telephoneField.getText() == null ? "" : telephoneField.getText().trim();
        if (value.isEmpty()) {
            telephoneValid = false;
            markError(telephoneField);
            return;
        }
        // 8 digits or +216 followed by 8 digits
        if (value.matches("\\d{8}") || value.matches("\\+216\\d{8}")) {
            telephoneValid = true;
            markValid(telephoneField);
        } else {
            telephoneValid = false;
            markError(telephoneField);
        }
    }

    private void validateEmail() {
        String value = emailField.getText() == null ? "" : emailField.getText().trim();
        if (value.isEmpty()) {
            emailValid = false;
            markError(emailField);
            return;
        }
        if (value.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            emailValid = true;
            markValid(emailField);
        } else {
            emailValid = false;
            markError(emailField);
        }
    }

    private void validateAdresse() {
        String value = adresseField.getText() == null ? "" : adresseField.getText().trim();
        if (value.isEmpty()) {
            // Adresse optionnelle : on considère valide mais sans style particulier
            adresseValid = true;
            if (adresseField != null) adresseField.setStyle("");
        } else {
            adresseValid = true;
            markValid(adresseField);
        }
    }

    private void validatePassword() {
        String value = motDePasseField.getText() == null ? "" : motDePasseField.getText();
        if (value.length() >= 8) {
            passwordValid = true;
            markValid(motDePasseField);
        } else {
            passwordValid = false;
            markError(motDePasseField);
        }
    }

    private void validateConfirmPassword() {
        String pwd = motDePasseField.getText() == null ? "" : motDePasseField.getText();
        String confirm = confirmerMotDePasseField.getText() == null ? "" : confirmerMotDePasseField.getText();
        if (!pwd.isEmpty() && pwd.equals(confirm)) {
            confirmPasswordValid = true;
            markValid(confirmerMotDePasseField);
        } else {
            confirmPasswordValid = false;
            markError(confirmerMotDePasseField);
        }
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

        StringBuilder sb = new StringBuilder();
        if (!cinValid) sb.append("- CIN invalide (8 chiffres requis).\n");
        if (!nomValid) sb.append("- Nom obligatoire.\n");
        if (!prenomValid) sb.append("- Prénom obligatoire.\n");
        if (!dateNaissanceValid) sb.append("- Date de naissance obligatoire.\n");
        if (!telephoneValid) sb.append("- Téléphone invalide.\n");
        if (!emailValid) sb.append("- Adresse e-mail invalide.\n");
        if (!passwordValid) sb.append("- Mot de passe trop court (8 caractères minimum).\n");
        if (!confirmPasswordValid) sb.append("- Les mots de passe ne correspondent pas.\n");

        if (sb.length() > 0) {
            setSignupStatus(sb.toString(), false);
            return false;
        }

        setSignupStatus("", true);
        return true;
    }
}
