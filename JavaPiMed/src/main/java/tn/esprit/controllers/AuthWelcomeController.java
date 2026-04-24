package tn.esprit.controllers;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.tools.AuthThemeManager;

public class AuthWelcomeController {

    @FXML private AnchorPane rootPane;
    @FXML private VBox welcomeCard;

    // Stagger-animatable child nodes
    @FXML private HBox brandBadge;
    @FXML private javafx.scene.control.Label titleLine1;
    @FXML private javafx.scene.control.Label titleLine2;
    @FXML private javafx.scene.control.Label subtitleLabel;
    @FXML private HBox pillsRow;
    @FXML private VBox pill1;
    @FXML private VBox pill2;
    @FXML private VBox pill3;
    @FXML private VBox pill4;
    @FXML private HBox ctaRow;
    @FXML private javafx.scene.control.Button loginBtn;
    @FXML private javafx.scene.control.Button signupBtn;
    @FXML private javafx.scene.control.Button themeToggleBtn;

    private static final String DARK_CLASS = "welcome-root";
    private static final String LIGHT_CLASS = "welcome-root-light";

    @FXML
    private void initialize() {
        applyThemeToRoot();
        applyThemeButtonLabel();
        playEntranceAnimation();
        startIdlePulse();
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

    // ── Rich stagger entrance ─────────────────────────────────────────────
    private void playEntranceAnimation() {
        if (welcomeCard == null) return;

        // Card itself: fade + scale + rise
        welcomeCard.setOpacity(0);
        welcomeCard.setScaleX(0.97);
        welcomeCard.setScaleY(0.97);
        welcomeCard.setTranslateY(28);

        ParallelTransition cardIn = new ParallelTransition(
            fade(welcomeCard, 0, 1, 480),
            translateY(welcomeCard, 28, 0, 480, Interpolator.SPLINE(0.2, 0.9, 0.2, 1.0)),
            scale(welcomeCard, 0.97, 1.0, 480)
        );

        // Stagger each child element
        Node[] staggerTargets = { brandBadge, titleLine1, titleLine2,
                                   subtitleLabel, pill1, pill2, pill3, pill4, ctaRow };
        int[] delays = { 120, 200, 270, 360, 460, 510, 560, 610, 700 };

        for (Node n : staggerTargets) {
            if (n == null) continue;
            n.setOpacity(0);
            n.setTranslateY(14);
        }

        // Run card and children independently with per-node delays.
        cardIn.play();

        for (int i = 0; i < staggerTargets.length; i++) {
            Node n = staggerTargets[i];
            if (n == null) continue;
            ParallelTransition childIn = new ParallelTransition(
                fade(n, 0, 1, 380),
                translateY(n, 14, 0, 380, Interpolator.SPLINE(0.2, 0.9, 0.2, 1.0))
            );
            childIn.setDelay(Duration.millis(delays[i]));
            childIn.play();
        }
    }

    // ── Idle pulse on CTA button ──────────────────────────────────────────
    private void startIdlePulse() {
        if (loginBtn == null) return;
        PauseTransition wait = new PauseTransition(Duration.millis(1800));
        wait.setOnFinished(e -> {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(700), loginBtn);
            pulse.setFromX(1.0); pulse.setFromY(1.0);
            pulse.setToX(1.035); pulse.setToY(1.035);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(4);
            pulse.setInterpolator(Interpolator.EASE_BOTH);
            pulse.play();
        });
        wait.play();
    }

    // ── Navigation handlers ───────────────────────────────────────────────
    public void handleGoLogin(ActionEvent event) {
        navigate(event, "/FrontFXML/Login.fxml", "MedFlow - Connexion");
    }

    public void handleGoSignup(ActionEvent event) {
        navigate(event, "/FrontFXML/Inscription.fxml", "MedFlow - Inscription");
    }

    private void navigate(ActionEvent event, String fxmlPath, String title) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Parent currentRoot = stage.getScene().getRoot();

            // Exit animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(240), currentRoot);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            ScaleTransition scaleOut = new ScaleTransition(Duration.millis(240), currentRoot);
            scaleOut.setFromX(1); scaleOut.setFromY(1);
            scaleOut.setToX(1.03); scaleOut.setToY(1.03);
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(240), currentRoot);
            slideOut.setFromY(0); slideOut.setToY(-16);
            slideOut.setInterpolator(Interpolator.EASE_IN);

            ParallelTransition out = new ParallelTransition(fadeOut, scaleOut, slideOut);
            out.setOnFinished(done -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                    Parent root = loader.load();
                    root.setOpacity(0);
                    root.setTranslateY(20);
                    root.setScaleX(0.98);
                    root.setScaleY(0.98);

                    stage.setScene(new Scene(root, 1280, 720));
                    stage.setTitle(title);
                    stage.setMaximized(true);
                    stage.show();

                    FadeTransition fadeIn = new FadeTransition(Duration.millis(340), root);
                    fadeIn.setFromValue(0); fadeIn.setToValue(1);
                    TranslateTransition slideIn = new TranslateTransition(Duration.millis(340), root);
                    slideIn.setFromY(20); slideIn.setToY(0);
                    slideIn.setInterpolator(Interpolator.EASE_OUT);
                    ScaleTransition scaleIn = new ScaleTransition(Duration.millis(340), root);
                    scaleIn.setFromX(0.98); scaleIn.setFromY(0.98);
                    scaleIn.setToX(1.0); scaleIn.setToY(1.0);
                    scaleIn.setInterpolator(Interpolator.EASE_OUT);

                    new ParallelTransition(fadeIn, slideIn, scaleIn).play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            out.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Animation helpers ─────────────────────────────────────────────────
    private FadeTransition fade(Node n, double from, double to, double ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), n);
        ft.setFromValue(from); ft.setToValue(to);
        return ft;
    }

    private TranslateTransition translateY(Node n, double from, double to, double ms, Interpolator interp) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), n);
        tt.setFromY(from); tt.setToY(to);
        tt.setInterpolator(interp);
        return tt;
    }

    private ScaleTransition scale(Node n, double from, double to, double ms) {
        ScaleTransition st = new ScaleTransition(Duration.millis(ms), n);
        st.setFromX(from); st.setFromY(from);
        st.setToX(to); st.setToY(to);
        st.setInterpolator(Interpolator.EASE_OUT);
        return st;
    }
}
