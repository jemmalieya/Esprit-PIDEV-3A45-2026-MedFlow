package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.transform.Rotate;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.tools.SessionManager;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheckBox;

    @FXML
    private Label statusLabel;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private HBox shellRoot;

    @FXML
    private AnchorPane shapeOne;

    @FXML
    private AnchorPane shapeTwo;

    @FXML
    private VBox loginDrawer;

    private final UserService userService = new UserService();

    private boolean drawerOpen = false;
    private boolean emailValid;
    private boolean passwordValid;

    @FXML
    private void initialize() {
        if (shellRoot != null) {
            shellRoot.setOpacity(0);
            shellRoot.setScaleX(0.96);
            shellRoot.setScaleY(0.96);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(480), shellRoot);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(480), shellRoot);
            scaleIn.setFromX(0.96);
            scaleIn.setFromY(0.96);
            scaleIn.setToX(1);
            scaleIn.setToY(1);

            ParallelTransition animation = new ParallelTransition(fadeIn, scaleIn);
            animation.play();
        }

        animateBackgroundShape(shapeOne, true);
        animateBackgroundShape(shapeTwo, false);

        if (emailField != null) {
            emailField.textProperty().addListener((obs, o, n) -> validateEmail());
        }
        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, o, n) -> validatePassword());
        }
    }

    private void animateBackgroundShape(AnchorPane shape, boolean upwards) {
        if (shape == null) {
            return;
        }

        TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(8), shape);
        floatAnim.setByY(upwards ? -40 : 40);
        floatAnim.setAutoReverse(true);
        floatAnim.setCycleCount(TranslateTransition.INDEFINITE);

        ScaleTransition scaleAnim = new ScaleTransition(Duration.seconds(8), shape);
        scaleAnim.setFromX(1.0);
        scaleAnim.setFromY(1.0);
        scaleAnim.setToX(1.08);
        scaleAnim.setToY(1.08);
        scaleAnim.setAutoReverse(true);
        scaleAnim.setCycleCount(ScaleTransition.INDEFINITE);

        ParallelTransition combo = new ParallelTransition(floatAnim, scaleAnim);
        combo.play();
    }

    @FXML
    private void toggleLoginDrawer(ActionEvent event) {
        if (!validateForm()) {
            showStatus("Veuillez corriger les champs en rouge.", false);
            return;
        }

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showStatus("Veuillez renseigner votre e-mail et votre mot de passe.", false);
            return;
        }
        if (emailField != null) {
            emailField.setStyle("");
        }
        if (passwordField != null) {
            passwordField.setStyle("");
        }

        User user = userService.authenticate(email, password);
        if (user == null) {
            showStatus("Identifiants invalides. Verifiez vos informations puis reessayez.", false);
            return;
        }

        if (!user.isVerified()) {
            showStatus("Votre compte n'est pas encore verifie.", false);
            return;
        }

        // Enregistrer l'utilisateur dans la session
        SessionManager.setCurrentUser(user);

        // Redirection en fonction du rôle
        redirectAfterLogin(event, user);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        toggleLoginDrawer(event);
    }

    private void markError(TextField field) {
        if (field != null) {
            field.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.2; -fx-background-radius: 6; -fx-border-radius: 6; -fx-text-fill: #e5e7eb; -fx-prompt-text-fill: #9ca3af;");
        }
    }

    private void markValid(TextField field) {
        if (field != null) {
            field.setStyle("-fx-border-color: #16a34a; -fx-border-width: 1.2; -fx-background-radius: 6; -fx-border-radius: 6; -fx-text-fill: #e5e7eb; -fx-prompt-text-fill: #9ca3af;");
        }
    }

    private void showFieldError(Label label, String message) {
        if (label == null) return;
        label.setText(message == null ? "" : message);
        boolean visible = message != null && !message.isBlank();
        label.setVisible(visible);
        label.setManaged(visible);
    }

    private void validateEmail() {
        String value = emailField.getText() == null ? "" : emailField.getText().trim();
        if (value.isEmpty()) {
            emailValid = false;
            markError(emailField);
            showFieldError(emailErrorLabel, "⚠ Adresse e-mail obligatoire.");
        } else if (value.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            emailValid = true;
            markValid(emailField);
            showFieldError(emailErrorLabel, "");
        } else {
            emailValid = false;
            markError(emailField);
            showFieldError(emailErrorLabel, "⚠ Format invalide. Ex\u00a0: vous@example.com");
        }
    }

    private void validatePassword() {
        String value = passwordField.getText() == null ? "" : passwordField.getText();
        if (value.isEmpty()) {
            passwordValid = false;
            markError(passwordField);
            showFieldError(passwordErrorLabel, "⚠ Mot de passe obligatoire.");
        } else {
            passwordValid = true;
            markValid(passwordField);
            showFieldError(passwordErrorLabel, "");
        }
    }

    private boolean validateForm() {
        validateEmail();
        validatePassword();
        return emailValid && passwordValid;
    }

    @FXML
    private void handleClear(ActionEvent event) {
        emailField.clear();
        passwordField.clear();
        rememberMeCheckBox.setSelected(false);
        if (emailField != null) emailField.setStyle("");
        if (passwordField != null) passwordField.setStyle("");
        showFieldError(emailErrorLabel, "");
        showFieldError(passwordErrorLabel, "");
        hideStatus();
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Récupération du mot de passe");
        alert.setHeaderText("Fonctionnalité à connecter");
        alert.setContentText("Vous pourrez ici ajouter votre logique de réinitialisation pour envoyer un lien ou un code de vérification.");
        alert.showAndWait();
    }

    @FXML
    private void handleOpenRegister(ActionEvent event) {
        playCardTransitionAndNavigate(event, "/FrontFXML/Inscription.fxml", "MedFlow - Inscription", false);
    }

    private void navigateTo(ActionEvent event, String resourcePath, String title) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        navigateToStage(stage, resourcePath, title);
    }

    private void navigateToStage(Stage stage, String resourcePath, String title) {
        try {
            if (resourcePath == null || resourcePath.isBlank()) {
                showStatus("Redirection non configurée pour ce rôle (FXML manquant).", false);
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1400, 820);

            // Ensure admin dashboard keeps its dedicated stylesheet when opened from login.
            if ("/EvenementDashboard.fxml".equals(resourcePath)) {
                String css = getClass().getResource("/evenement-dashboard.css") != null
                        ? getClass().getResource("/evenement-dashboard.css").toExternalForm()
                        : null;
                if (css != null && !scene.getStylesheets().contains(css)) {
                    scene.getStylesheets().add(css);
                }
            }

            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            showStatus("Connexion reussie, mais la page suivante n'a pas pu etre ouverte.", false);
            e.printStackTrace();
        }
    }

    private void redirectAfterLogin(ActionEvent event, User user) {
        String role = user.getRoleSysteme();
        String title;
        if (role != null) {
            role = role.trim().toUpperCase();
        }

        // Patients (ou rôle non défini) : vont directement vers le front
        if (role == null || role.isBlank() || "PATIENT".equals(role)) {
            title = "MedFlow - Espace Patient";
            navigateTo(event, "/FrontFXML/Accueil.fxml", title);
            return;
        }

        // Tous les autres rôles (ADMIN, STAFF, ...) : d'abord le back-office
        if ("ADMIN".equals(role)) {
            navigateTo(event, "/AdminDashboard.fxml", "MedFlow - Tableau de bord Admin");
        } else if ("STAFF".equals(role)) {
            String typeStaff = user.getTypeStaff();
            if (typeStaff != null) {
                typeStaff = typeStaff.trim().toUpperCase();
            }

            if ("RESP_EVEN".equals(typeStaff)) {
                // Responsable événements : back-office événements (existe déjà)
                navigateTo(event, "/EvenementDashboard.fxml", "MedFlow - Gestion des événements");
            } else if ("RESP_PRODUCTS".equals(typeStaff)) {
                // Responsable produits : back-office produits (existe déjà)
                navigateTo(event, "/ProduitDashboard.fxml", "MedFlow - Gestion des produits");
            } else if ("RESP_BLOG".equals(typeStaff) || "RESP_RECLAMATION".equals(typeStaff)) {
                // Responsable blog / réclamations : back-office réclamations.
                // TODO: remplace la chaîne vide par le chemin FXML réel
                //       (ex: "/ReclamationDashboard.fxml").
                navigateTo(event, "/reponse.fxml", "MedFlow - Gestion des réclamations (back-office)");
            } else if ("RESP_PATIENTS".equals(typeStaff)) {
                // Responsable patients : back-office fiches/patients.
                // TODO: remplace la chaîne vide par le chemin FXML réel
                //       (ex: "/PatientsDashboard.fxml").
                navigateTo(event, "/ConsultationDocteur.fxml", "MedFlow - Espace Patients (back-office)");
            } else if ("RESP_USERS".equals(typeStaff)) {
                // Responsable utilisateurs : back-office gestion utilisateurs.
                // TODO: remplace la chaîne vide par le chemin FXML réel
                //       (ex: "/UsersDashboard.fxml").
                navigateTo(event, "", "MedFlow - Gestion des utilisateurs (back-office)");
            } else {
                // Staff générique ou type non géré : tableau de bord admin commun.
                // TODO: si tu crées un FXML générique staff,
                //       remplace "/EvenementDashboard.fxml" par ce fichier.
                navigateTo(event, "/EvenementDashboard.fxml", "MedFlow - Espace Staff");
            }
        } else {
            // Rôle inconnu mais non patient : on le considère comme back-office générique
            navigateTo(event, "/EvenementDashboard.fxml", "MedFlow - Tableau de bord");
        }
    }

    private void showStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(success ? "status-success" : "status-error");

        // Stronger feedback when login fails: small shake animation
        if (!success) {
            javafx.animation.TranslateTransition shake = new javafx.animation.TranslateTransition(Duration.millis(140), statusLabel);
            shake.setFromX(-4);
            shake.setByX(8);
            shake.setCycleCount(4);
            shake.setAutoReverse(true);
            shake.play();
        }
    }

    private void hideStatus() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
    }

    private void playCardTransitionAndNavigate(ActionEvent event, String resourcePath, String title, boolean toRight) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();

        // Animate only the login card when present
        Node target = loginDrawer != null ? (Node) loginDrawer : stage.getScene().getRoot();

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
            navigateToStage(stage, resourcePath, title);
        });
        transition.play();
    }
}