package tn.esprit.controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.entities.User;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserService;
import tn.esprit.tools.AuthThemeManager;
import tn.esprit.tools.SessionManager;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginController {

    public static boolean autoRevealOnLoad = false;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Label statusLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label passwordErrorLabel;

    // Legacy fields (kept for compatibility – may be null in new fxml)
    @FXML private StackPane shellRoot;
    @FXML private AnchorPane shapeOne;
    @FXML private AnchorPane shapeTwo;
    @FXML private VBox loginDrawer;
    @FXML private VBox welcomeCard;
    @FXML private Label panelTitleLabel;
    @FXML private Label panelSubtitleLabel;
    @FXML private HBox modeButtonRow;

    // New layout fx:ids
    @FXML private AnchorPane rootPane;
    @FXML private HBox mainContent;
    @FXML private VBox visualPanel;
    @FXML private VBox formPanel;
    @FXML private Button themeToggleBtn;

    private static final String DARK_CLASS = "auth-page-login";
    private static final String LIGHT_CLASS = "auth-page-light";

    private final UserService userService = new UserService();
    private boolean emailValid;
    private boolean passwordValid;
    private boolean panelRevealed;

    @FXML
    private void initialize() {
        applyThemeToRoot();
        applyThemeButtonLabel();

        // ── New layout entrance (split panels) ──
        if (mainContent != null) {
            playPanelEntranceAnimation();
        } else if (shellRoot != null) {
            // Legacy shell-root entrance
            shellRoot.setOpacity(0);
            shellRoot.setScaleX(0.98);
            shellRoot.setScaleY(0.98);
            shellRoot.setTranslateY(18);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(360), shellRoot);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(360), shellRoot);
            scaleIn.setFromX(0.98); scaleIn.setFromY(0.98);
            scaleIn.setToX(1); scaleIn.setToY(1);
            TranslateTransition liftIn = new TranslateTransition(Duration.millis(360), shellRoot);
            liftIn.setFromY(18); liftIn.setToY(0);
            liftIn.setInterpolator(Interpolator.EASE_BOTH);
            new ParallelTransition(fadeIn, scaleIn, liftIn).play();
        }

        animateBackgroundShape(shapeOne, true);
        animateBackgroundShape(shapeTwo, false);
        animateWelcomeCardPresence();

        if (loginDrawer != null) loginDrawer.setMouseTransparent(true);

        if (emailField != null) emailField.textProperty().addListener((obs, o, n) -> validateEmail());
        if (passwordField != null) passwordField.textProperty().addListener((obs, o, n) -> validatePassword());

        if (autoRevealOnLoad) {
            autoRevealOnLoad = false;
            Platform.runLater(this::revealLoginPanel);
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
        // Visual panel slides in from left
        if (visualPanel != null) {
            visualPanel.setOpacity(0);
            visualPanel.setTranslateX(-40);
            FadeTransition vFade = new FadeTransition(Duration.millis(500), visualPanel);
            vFade.setFromValue(0); vFade.setToValue(1);
            TranslateTransition vSlide = new TranslateTransition(Duration.millis(500), visualPanel);
            vSlide.setFromX(-40); vSlide.setToX(0);
            vSlide.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1.0));
            new ParallelTransition(vFade, vSlide).play();
        }

        // Form panel slides in from right with slight delay
        if (formPanel != null) {
            formPanel.setOpacity(0);
            formPanel.setTranslateX(40);
            FadeTransition fFade = new FadeTransition(Duration.millis(500), formPanel);
            fFade.setFromValue(0); fFade.setToValue(1);
            fFade.setDelay(Duration.millis(110));
            TranslateTransition fSlide = new TranslateTransition(Duration.millis(500), formPanel);
            fSlide.setFromX(40); fSlide.setToX(0);
            fSlide.setDelay(Duration.millis(110));
            fSlide.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1.0));
            new ParallelTransition(fFade, fSlide).play();
        }
    }

    private void animateBackgroundShape(AnchorPane shape, boolean upwards) {
        if (shape == null) return;
        TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(8), shape);
        floatAnim.setByY(upwards ? -40 : 40);
        floatAnim.setInterpolator(Interpolator.SPLINE(0.42, 0.0, 0.24, 1.0));
        floatAnim.setAutoReverse(true);
        floatAnim.setCycleCount(TranslateTransition.INDEFINITE);

        ScaleTransition scaleAnim = new ScaleTransition(Duration.seconds(8), shape);
        scaleAnim.setFromX(1.0);
        scaleAnim.setFromY(1.0);
        scaleAnim.setToX(1.07);
        scaleAnim.setToY(1.07);
        scaleAnim.setInterpolator(Interpolator.SPLINE(0.42, 0.0, 0.24, 1.0));
        scaleAnim.setAutoReverse(true);
        scaleAnim.setCycleCount(ScaleTransition.INDEFINITE);

        new ParallelTransition(floatAnim, scaleAnim).play();
    }

    private void animateWelcomeCardPresence() {
        if (welcomeCard == null || modeButtonRow == null) return;

        welcomeCard.setOpacity(0);
        welcomeCard.setTranslateY(24);
        modeButtonRow.setOpacity(0);

        FadeTransition welcomeFade = new FadeTransition(Duration.millis(460), welcomeCard);
        welcomeFade.setFromValue(0);
        welcomeFade.setToValue(1);

        TranslateTransition welcomeRise = new TranslateTransition(Duration.millis(460), welcomeCard);
        welcomeRise.setFromY(24);
        welcomeRise.setToY(0);
        welcomeRise.setInterpolator(Interpolator.EASE_BOTH);

        PauseTransition delay = new PauseTransition(Duration.millis(120));
        FadeTransition buttonsFade = new FadeTransition(Duration.millis(280), modeButtonRow);
        buttonsFade.setFromValue(0);
        buttonsFade.setToValue(1);

        new SequentialTransition(new ParallelTransition(welcomeFade, welcomeRise), delay, buttonsFade).play();
    }

    @FXML
    private void handleRevealLoginPanel(ActionEvent event) {
        revealLoginPanel();
    }

    private void revealLoginPanel() {
        if (panelRevealed || welcomeCard == null || loginDrawer == null) return;
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

        FadeTransition formFade = new FadeTransition(Duration.millis(360), loginDrawer);
        formFade.setFromValue(0);
        formFade.setToValue(1);
        formFade.setDelay(Duration.millis(150));
        formFade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition formSlide = new TranslateTransition(Duration.millis(360), loginDrawer);
        formSlide.setFromX(112);
        formSlide.setToX(0);
        formSlide.setDelay(Duration.millis(150));
        formSlide.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1));

        ScaleTransition formPop = new ScaleTransition(Duration.millis(360), loginDrawer);
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
            loginDrawer.setMouseTransparent(false);
            if (panelTitleLabel != null) panelTitleLabel.setText("Espace Connexion");
            if (panelSubtitleLabel != null) panelSubtitleLabel.setText("Connectez-vous pour acceder rapidement a votre compte MedFlow.");
            if (!welcomeCard.getStyleClass().contains("welcome-master-card-compact")) {
                welcomeCard.getStyleClass().add("welcome-master-card-compact");
            }
        });
        p.play();
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        if (!validateForm()) {
            showStatus("Veuillez corriger les champs en rouge.", false);
            return;
        }

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        User user = userService.authenticate(email, password);
        if (user == null) {
            showStatus("Identifiants invalides.", false);
            return;
        }
        if (!user.isVerified()) {
            showStatus("Votre compte n'est pas encore verifie.", false);
            return;
        }

        SessionManager.setCurrentUser(user);
        redirectAfterLogin(event, user);
    }

    @FXML
    private void handleClear(ActionEvent event) {
        emailField.clear();
        passwordField.clear();
        rememberMeCheckBox.setSelected(false);
        emailField.setStyle("");
        passwordField.setStyle("");
        showFieldError(emailErrorLabel, "");
        showFieldError(passwordErrorLabel, "");
        hideStatus();
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        // ── Etape 1 : demander l'email ──────────────────────────────────────
        Dialog<String> emailDialog = new Dialog<>();
        emailDialog.setTitle("Mot de passe oublié");
        emailDialog.setHeaderText("Reinitialisation du mot de passe");
        styleDialog(emailDialog, "#2563eb");

        ButtonType sendCodeBtn = new ButtonType("Envoyer le code", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn   = new ButtonType("Annuler",          ButtonBar.ButtonData.CANCEL_CLOSE);
        emailDialog.getDialogPane().getButtonTypes().addAll(sendCodeBtn, cancelBtn);
        styleDialogButtons(emailDialog, sendCodeBtn, cancelBtn, "#2563eb");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 16 24 8 24;");

        TextField emailInput = new TextField();
        emailInput.setPromptText("exemple@email.com");
        emailInput.setPrefWidth(320);
        styleTextInput(emailInput);

        Label title = new Label("Entrez votre adresse e-mail");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Label infoLabel = new Label("Un code a 6 chiffres sera envoye sur votre adresse.");
        infoLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");

        Label dialogErrorLabel = new Label();
        dialogErrorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px; -fx-font-weight: 600;");
        dialogErrorLabel.setVisible(false);
        dialogErrorLabel.setManaged(false);

        grid.add(title, 0, 0);
        grid.add(emailInput, 0, 1);
        grid.add(infoLabel, 0, 2);
        grid.add(dialogErrorLabel, 0, 3);

        emailDialog.getDialogPane().setContent(grid);
        javafx.scene.Node sendNode = emailDialog.getDialogPane().lookupButton(sendCodeBtn);
        sendNode.setDisable(true);

        Runnable validateEmailInput = () -> {
            String value = emailInput.getText() == null ? "" : emailInput.getText().trim();
            boolean valid = value.matches("^[^@]+@[^@]+\\.[^@]+$");
            sendNode.setDisable(!valid);
            if (value.isEmpty()) {
                dialogErrorLabel.setVisible(false);
                dialogErrorLabel.setManaged(false);
            } else if (!valid) {
                dialogErrorLabel.setText("Format email invalide.");
                dialogErrorLabel.setVisible(true);
                dialogErrorLabel.setManaged(true);
            } else {
                dialogErrorLabel.setVisible(false);
                dialogErrorLabel.setManaged(false);
            }
        };
        emailInput.textProperty().addListener((obs, oldVal, newVal) -> validateEmailInput.run());

        // Pre-remplir avec l'email en cours de saisie
        String currentEmail = emailField != null ? emailField.getText() : "";
        if (currentEmail != null && !currentEmail.isBlank()) {
            emailInput.setText(currentEmail.trim());
        }
        validateEmailInput.run();

        emailDialog.setResultConverter(btn -> btn == sendCodeBtn ? emailInput.getText() : null);
        Optional<String> emailResult = emailDialog.showAndWait();

        if (emailResult.isEmpty() || emailResult.get().isBlank()) return;

        String targetEmail = emailResult.get().trim();

        // Verifier que l'email existe
        User found = userService.findByEmail(targetEmail);
        if (found == null) {
            showAlertInfo("Email introuvable",
                    "Aucun compte n'est associé à l'adresse : " + targetEmail);
            return;
        }

        // Generer un code a 6 chiffres
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        boolean saved = userService.saveResetToken(found.getId(), code, expiresAt);
        if (!saved) {
            showAlertInfo("Erreur", "Impossible de générer le code. Veuillez réessayer.");
            return;
        }

        // Envoyer l'email
        EmailService.sendPasswordResetEmail(targetEmail, code);

        showAlertInfo("Code envoye",
                "Un code de reinitialisation a ete envoye a :\n" + targetEmail
                        + "\n\nVerifiez votre boite mail (et les spams).");

        // ── Etape 2 : saisir le code + nouveau mot de passe ─────────────────
        Dialog<Void> resetDialog = new Dialog<>();
        resetDialog.setTitle("Réinitialisation");
        resetDialog.setHeaderText("Entrez le code recu par email");
        styleDialog(resetDialog, "#0f766e");

        ButtonType confirmBtn  = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn2  = new ButtonType("Annuler",   ButtonBar.ButtonData.CANCEL_CLOSE);
        resetDialog.getDialogPane().getButtonTypes().addAll(confirmBtn, cancelBtn2);
        styleDialogButtons(resetDialog, confirmBtn, cancelBtn2, "#0f766e");

        GridPane resetGrid = new GridPane();
        resetGrid.setHgap(10);
        resetGrid.setVgap(12);
        resetGrid.setStyle("-fx-padding: 16 24 8 24;");

        TextField codeInput       = new TextField();
        codeInput.setPromptText("Code a 6 chiffres");
        codeInput.setPrefWidth(320);
        styleTextInput(codeInput);

        PasswordField newPass  = new PasswordField();
        newPass.setPromptText("Nouveau mot de passe");
        newPass.setPrefWidth(320);
        styleTextInput(newPass);

        Label passwordRulesLabel = new Label("8+ caracteres, 1 majuscule, 1 minuscule, 1 chiffre, 1 symbole");
        passwordRulesLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        PasswordField confirmPass = new PasswordField();
        confirmPass.setPromptText("Confirmer le mot de passe");
        confirmPass.setPrefWidth(320);
        styleTextInput(confirmPass);

        Label codeHint = new Label("Code valide 15 min - verifiez vos spams si absent");
        codeHint.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px;");

        Label resetErrorLabel = new Label();
        resetErrorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px; -fx-font-weight: 600;");
        resetErrorLabel.setVisible(false);
        resetErrorLabel.setManaged(false);

        Label stepTitle = new Label("Securisez votre compte");
        stepTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        Label codeLabel = new Label("Code recu par email :");
        codeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        Label passLabel = new Label("Nouveau mot de passe :");
        passLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");
        Label confirmLabel = new Label("Confirmer le mot de passe :");
        confirmLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #334155;");

        resetGrid.add(stepTitle, 0, 0);
        resetGrid.add(codeLabel, 0, 1);
        resetGrid.add(codeInput, 0, 2);
        resetGrid.add(codeHint, 0, 3);
        resetGrid.add(passLabel, 0, 4);
        resetGrid.add(newPass, 0, 5);
        resetGrid.add(passwordRulesLabel, 0, 6);
        resetGrid.add(confirmLabel, 0, 7);
        resetGrid.add(confirmPass, 0, 8);
        resetGrid.add(resetErrorLabel, 0, 9);

        resetDialog.getDialogPane().setContent(resetGrid);

        // Bloquer la confirmation si les champs ne sont pas valides
        javafx.scene.Node confirmNode = resetDialog.getDialogPane().lookupButton(confirmBtn);
        confirmNode.setDisable(true);
        Runnable validate = () -> {
            String passwordError = validateNewPassword(newPass.getText());
            boolean ok = !codeInput.getText().isBlank()
                    && passwordError == null
                    && newPass.getText().equals(confirmPass.getText());
            confirmNode.setDisable(!ok);
            if (passwordError != null && !newPass.getText().isBlank()) {
                resetErrorLabel.setText(passwordError);
                resetErrorLabel.setVisible(true);
                resetErrorLabel.setManaged(true);
            } else if (!confirmPass.getText().isBlank() && !newPass.getText().equals(confirmPass.getText())) {
                resetErrorLabel.setText("Les mots de passe ne correspondent pas.");
                resetErrorLabel.setVisible(true);
                resetErrorLabel.setManaged(true);
            } else {
                resetErrorLabel.setVisible(false);
                resetErrorLabel.setManaged(false);
            }

            boolean strong = passwordError == null && !newPass.getText().isBlank();
            passwordRulesLabel.setStyle(strong
                    ? "-fx-text-fill: #16a34a; -fx-font-size: 11px; -fx-font-weight: 700;"
                    : "-fx-text-fill: #64748b; -fx-font-size: 11px;");
        };
        codeInput.textProperty().addListener((o, ol, n) -> validate.run());
        newPass.textProperty().addListener((o, ol, n) -> validate.run());
        confirmPass.textProperty().addListener((o, ol, n) -> validate.run());

        resetDialog.setResultConverter(btn -> null);
        resetDialog.getDialogPane().lookupButton(confirmBtn).addEventFilter(
                javafx.event.ActionEvent.ACTION, e -> {
                    String enteredCode = codeInput.getText().trim();
                    String newPassword = newPass.getText();

                    String passwordError = validateNewPassword(newPassword);
                    if (passwordError != null) {
                        e.consume();
                        resetErrorLabel.setText(passwordError);
                        resetErrorLabel.setVisible(true);
                        resetErrorLabel.setManaged(true);
                        return;
                    }

                    // Verifier le code en base
                    User tokenUser = userService.findByResetToken(enteredCode);
                    if (tokenUser == null || tokenUser.getId() != found.getId()) {
                        e.consume(); // empêcher la fermeture
                        resetErrorLabel.setText("Code invalide ou expire.");
                        resetErrorLabel.setVisible(true);
                        resetErrorLabel.setManaged(true);
                        return;
                    }

                    // Hasher et mettre a jour
                    String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
                    boolean updated = userService.updatePasswordAndClearToken(found.getId(), hashed);
                    if (!updated) {
                        e.consume();
                        resetErrorLabel.setText("Erreur lors de la mise a jour. Reessayez.");
                        resetErrorLabel.setVisible(true);
                        resetErrorLabel.setManaged(true);
                        return;
                    }
                    // Succes - laisser la fenetre se fermer
                }
        );

        resetDialog.showAndWait();

        // Verifier si le mot de passe a ete mis a jour avec succes
        // (on tente de retrouver l'utilisateur avec le token - si null = token efface = succes)
        User check = userService.findByResetToken(codeInput.getText().trim());
        if (check == null && !codeInput.getText().isBlank()) {
            showAlertInfo("Mot de passe mis a jour",
                    "Votre mot de passe a ete modifie avec succes.\nVous pouvez maintenant vous connecter.");
        }
    }

    private void styleDialog(Dialog<?> dialog, String accentColor) {
        if (dialog == null || dialog.getDialogPane() == null) {
            return;
        }
        dialog.getDialogPane().setStyle(
                "-fx-background-color: linear-gradient(to bottom, #f8fbff, #ffffff);"
                        + "-fx-border-color: " + accentColor + "33;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 14;"
                        + "-fx-background-radius: 14;"
                        + "-fx-padding: 8;"
        );
    }

    private void styleDialogButtons(Dialog<?> dialog, ButtonType primaryBtn, ButtonType secondaryBtn, String accentColor) {
        if (dialog == null || dialog.getDialogPane() == null) {
            return;
        }
        Node primary = dialog.getDialogPane().lookupButton(primaryBtn);
        Node secondary = dialog.getDialogPane().lookupButton(secondaryBtn);
        if (primary != null) {
            primary.setStyle(
                    "-fx-background-color: " + accentColor + ";"
                            + "-fx-text-fill: white;"
                            + "-fx-font-weight: 700;"
                            + "-fx-background-radius: 10;"
                            + "-fx-padding: 8 16 8 16;"
            );
        }
        if (secondary != null) {
            secondary.setStyle(
                    "-fx-background-color: #e2e8f0;"
                            + "-fx-text-fill: #0f172a;"
                            + "-fx-font-weight: 700;"
                            + "-fx-background-radius: 10;"
                            + "-fx-padding: 8 16 8 16;"
            );
        }
    }

    private void styleTextInput(TextField field) {
        if (field == null) {
            return;
        }
        field.setStyle(
                "-fx-background-color: #ffffff;"
                        + "-fx-border-color: #cbd5e1;"
                        + "-fx-border-radius: 10;"
                        + "-fx-background-radius: 10;"
                        + "-fx-padding: 10 12 10 12;"
                        + "-fx-font-size: 13px;"
        );
    }

    private String validateNewPassword(String password) {
        if (password == null || password.isBlank()) {
            return "Le nouveau mot de passe est obligatoire.";
        }
        if (password.length() < 8) {
            return "Minimum 8 caracteres requis.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Ajoutez au moins 1 lettre majuscule.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Ajoutez au moins 1 lettre minuscule.";
        }
        if (!password.matches(".*\\d.*")) {
            return "Ajoutez au moins 1 chiffre.";
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return "Ajoutez au moins 1 caractere special.";
        }
        return null;
    }

    private void showAlertInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleOpenRegister(ActionEvent event) {
        RegisterController.autoRevealOnLoad = true;
        playCardTransitionAndNavigate(event, "/FrontFXML/Inscription.fxml", "MedFlow - Inscription", false);
    }

    @FXML
    private void handleGoogleLogin(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        showStatus("Connexion Google en cours...", true);

        CompletableFuture.supplyAsync(() -> {
            try {
                GoogleProfile profile = fetchGoogleProfile();
                User user = userService.findByEmail(profile.email);
                if (user == null) {
                    user = createGoogleUser(profile);
                }
                if (user == null) {
                    throw new IllegalStateException("Impossible de finaliser la connexion Google.");
                }
                return user;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).thenAccept(user -> Platform.runLater(() -> {
            hideStatus();
            SessionManager.setCurrentUser(user);
            redirectAfterLoginStage(stage, user);
        })).exceptionally(ex -> {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            Platform.runLater(() -> showStatus("Google login echoue: " + cause.getMessage(), false));
            return null;
        });
    }
    
    @FXML
    private void handleFaceLogin(ActionEvent event) {
        String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
        if (email.isEmpty()) {
            showStatus("Enter your email first, then use face login.", false);
            return;
        }

        User targetUser = userService.findByEmail(email);
        if (targetUser == null) {
            showStatus("No account found for this email.", false);
            return;
        }
        if (!hasEnrolledFace(targetUser)) {
            showStatus("Aucun visage enregistre. Faites l'enregistrement du visage pendant l'inscription.", false);
            return;
        }

        showStatus("Loading facial recognition...", true);
        
        try {
            openFaceLoginWindow(targetUser);
            hideStatus();
        } catch (IOException e) {
            showStatus("Error: " + e.getMessage(), false);
        }
    }

    private boolean hasEnrolledFace(User user) {
        return user != null
                && user.isFaceLoginEnabled()
                && user.getFaceReferenceEmbedding() != null
                && !user.getFaceReferenceEmbedding().isBlank();
    }


    private void openFaceLoginWindow(User targetUser) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/FaceLogin.fxml"));
        Parent root = loader.load();

        FaceLoginController faceController = loader.getController();
        faceController.setTargetUser(targetUser);

        Scene scene = new Scene(root);
        Stage faceStage = new Stage();
        faceStage.setTitle("MedFlow - Face Login");
        faceStage.setScene(scene);
        faceStage.show();
    }

    private User createGoogleUser(GoogleProfile profile) {
        User newUser = new User();
        newUser.setCin(generateUniqueCin());
        newUser.setProfilePicture("default.png");
        newUser.setNom(profile.familyName == null || profile.familyName.isBlank() ? "Google" : profile.familyName);
        newUser.setPrenom(profile.givenName == null || profile.givenName.isBlank() ? "User" : profile.givenName);
        newUser.setDateNaissance((LocalDate) null);
        newUser.setTelephoneUser("00000000");
        newUser.setEmailUser(profile.email);
        newUser.setAdresseUser("Google OAuth");
        newUser.setPassword(BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt(12)));
        newUser.setVerified(true);
        newUser.setStatutCompte("ACTIF");
        newUser.setRoleSysteme("PATIENT");
        newUser.setGoogleId(profile.sub);

        String dbError = userService.ajouterAvecRetour(newUser);
        if (dbError != null) {
            throw new IllegalStateException(dbError);
        }
        return newUser;
    }

    private String generateUniqueCin() {
        for (int i = 0; i < 40; i++) {
            int value = 10000000 + (int) (Math.random() * 90000000);
            String candidate = String.valueOf(value);
            if (!userService.existsByCin(candidate)) {
                return candidate;
            }
        }
        return String.valueOf(Math.abs(UUID.randomUUID().hashCode())).substring(0, 8);
    }

    private GoogleProfile fetchGoogleProfile() throws Exception {
        String clientId = readGoogleSetting("GOOGLE_OAUTH_CLIENT_ID", "google.oauth.clientId");
        String clientSecret = readGoogleSetting("GOOGLE_OAUTH_CLIENT_SECRET", "google.oauth.clientSecret");
        String configuredRedirectUri = readGoogleSetting("GOOGLE_OAUTH_REDIRECT_URI", "google.oauth.redirectUri");

        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("Configure GOOGLE_OAUTH_CLIENT_ID et GOOGLE_OAUTH_CLIENT_SECRET.");
        }

        String redirectUri = (configuredRedirectUri == null || configuredRedirectUri.isBlank())
                ? "http://127.0.0.1:8765/oauth2callback"
                : configuredRedirectUri.trim();
        URI redirect = URI.create(redirectUri);
        int port = redirect.getPort();
        if (port <= 0) {
            throw new IllegalStateException("GOOGLE_OAUTH_REDIRECT_URI invalide: port requis (ex: http://127.0.0.1:8765/oauth2callback)");
        }
        String callbackPath = (redirect.getPath() == null || redirect.getPath().isBlank()) ? "/oauth2callback" : redirect.getPath();
        String state = UUID.randomUUID().toString();

        CompletableFuture<Map<String, String>> callbackFuture = new CompletableFuture<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
        server.setExecutor(serverExecutor);
        server.createContext(callbackPath, exchange -> handleOAuthCallback(exchange, callbackFuture));
        server.start();

        try {
            String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + urlEncode(clientId)
                    + "&redirect_uri=" + urlEncode(redirectUri)
                    + "&response_type=code"
                    + "&scope=" + urlEncode("openid email profile")
                    + "&access_type=offline"
                    + "&prompt=select_account"
                    + "&state=" + urlEncode(state);

            openBrowser(authUrl);

            Map<String, String> callbackParams = callbackFuture.get(120, TimeUnit.SECONDS);
            String oauthError = callbackParams.get("error");
            if (oauthError != null && !oauthError.isBlank()) {
                String oauthDescription = callbackParams.get("error_description");
                throw new IllegalStateException("Google OAuth error: " + oauthError
                        + (oauthDescription == null || oauthDescription.isBlank() ? "" : " (" + oauthDescription + ")"));
            }

            String callbackState = callbackParams.get("state");
            String code = callbackParams.get("code");

            if (code == null || code.isBlank()) {
                throw new IllegalStateException("Code OAuth absent.");
            }
            if (callbackState == null || !state.equals(callbackState)) {
                throw new IllegalStateException("Etat OAuth invalide.");
            }

            String accessToken = exchangeCodeForAccessToken(code, clientId, clientSecret, redirectUri);
            return fetchUserInfo(accessToken);
        } catch (java.util.concurrent.TimeoutException timeout) {
            throw new IllegalStateException("Timeout OAuth (120s). Verifiez l'URI autorisee dans Google Cloud: " + redirectUri);
        } finally {
            server.stop(0);
            serverExecutor.shutdownNow();
        }
    }

    private String exchangeCodeForAccessToken(String code, String clientId, String clientSecret, String redirectUri) throws Exception {
        String body = "code=" + urlEncode(code)
                + "&client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Token Google refuse (" + response.statusCode() + "): " + truncate(response.body(), 260));
        }

        String accessToken = extractJsonValue(response.body(), "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("access_token introuvable.");
        }
        return accessToken;
    }

    private GoogleProfile fetchUserInfo(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/oauth2/v3/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("UserInfo Google refuse (" + response.statusCode() + "): " + truncate(response.body(), 260));
        }

        GoogleProfile profile = new GoogleProfile();
        profile.email = extractJsonValue(response.body(), "email");
        profile.givenName = extractJsonValue(response.body(), "given_name");
        profile.familyName = extractJsonValue(response.body(), "family_name");
        profile.sub = extractJsonValue(response.body(), "sub");

        if (profile.email == null || profile.email.isBlank()) {
            throw new IllegalStateException("Email Google introuvable.");
        }
        return profile;
    }

    private void handleOAuthCallback(HttpExchange exchange, CompletableFuture<Map<String, String>> callbackFuture) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        callbackFuture.complete(params);

        String html = "<html><body style='font-family:Segoe UI;padding:20px;'>Connexion Google reussie. Vous pouvez fermer cette fenetre.</body></html>";
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) {
            return map;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = urlDecode(kv[0]);
            String value = kv.length > 1 ? urlDecode(kv[1]) : "";
            map.put(key, value);
        }
        return map;
    }

    private String extractJsonValue(String json, String key) {
        if (json == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String readGoogleSetting(String envKey, String propKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String propValue = System.getProperty(propKey);
        if (propValue != null && !propValue.isBlank()) {
            return propValue;
        }
        return null;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String urlDecode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen) + "...";
    }

    private void openBrowser(String url) throws Exception {
        if (!Desktop.isDesktopSupported()) {
            throw new IllegalStateException("Desktop non supporte pour ouvrir le navigateur.");
        }
        Desktop.getDesktop().browse(URI.create(url));
    }

    private void redirectAfterLoginStage(Stage stage, User user) {
        String role = user.getRoleSysteme() == null ? "PATIENT" : user.getRoleSysteme().trim().toUpperCase();
        if ("PATIENT".equals(role) || role.isBlank()) {
            navigateToStage(stage, "/FrontFXML/Accueil.fxml", "MedFlow - Espace Patient");
            return;
        }
        if ("ADMIN".equals(role)) {
            navigateToStage(stage, "/AdminDashboard.fxml", "MedFlow - Tableau de bord Admin");
            return;
        }
        navigateToStage(stage, "/EvenementDashboard.fxml", "MedFlow - Tableau de bord");
    }

    private static class GoogleProfile {
        private String email;
        private String givenName;
        private String familyName;
        private String sub;
    }

    private void playCardTransitionAndNavigate(ActionEvent event, String resourcePath, String title, boolean toRight) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        Node target = shellRoot != null ? shellRoot : stage.getScene().getRoot();

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
        p.setOnFinished(e -> navigateToStage(stage, resourcePath, title));
        p.play();
    }

    private void navigateToStage(Stage stage, String resourcePath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Parent root = loader.load();
            root.setOpacity(0);
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
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
            showStatus("Impossible d'ouvrir la page suivante.", false);
            e.printStackTrace();
        }
    }

    private void redirectAfterLogin(ActionEvent event, User user) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        redirectAfterLoginStage(stage, user);
    }

    private void navigateTo(ActionEvent event, String resourcePath, String title) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        navigateToStage(stage, resourcePath, title);
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
            showFieldError(emailErrorLabel, "Adresse e-mail obligatoire.");
        } else if (value.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            emailValid = true;
            markValid(emailField);
            showFieldError(emailErrorLabel, "");
        } else {
            emailValid = false;
            markError(emailField);
            showFieldError(emailErrorLabel, "Format invalide.");
        }
    }

    private void validatePassword() {
        String value = passwordField.getText() == null ? "" : passwordField.getText();
        if (value.isEmpty()) {
            passwordValid = false;
            markError(passwordField);
            showFieldError(passwordErrorLabel, "Mot de passe obligatoire.");
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

    private void showStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
        statusLabel.getStyleClass().add(success ? "status-success" : "status-error");

        if (!success) {
            // Shake animation on error
            TranslateTransition shake = new TranslateTransition(Duration.millis(60), statusLabel);
            shake.setFromX(0); shake.setByX(7);
            shake.setCycleCount(6);
            shake.setAutoReverse(true);
            shake.setInterpolator(Interpolator.EASE_BOTH);
            shake.play();
        } else {
            FadeTransition pop = new FadeTransition(Duration.millis(200), statusLabel);
            pop.setFromValue(0); pop.setToValue(1);
            pop.play();
        }
    }

    private void hideStatus() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        statusLabel.getStyleClass().removeAll("status-error", "status-success");
    }
}
