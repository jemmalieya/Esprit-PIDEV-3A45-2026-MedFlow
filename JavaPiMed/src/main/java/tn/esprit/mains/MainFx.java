package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.tools.EnvLoader;
import tn.esprit.tools.SystemNotification;

public class MainFx extends Application {
    private static final double APP_WIDTH = 1400;
    private static final double APP_HEIGHT = 820;
    private boolean applyingWindowSize;
    private boolean preferredMaximized;
    @Override
    public void start(Stage stage) throws Exception {
        SystemNotification.initTray();
        configurePrimaryStage(stage);


        FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/AuthWelcome.fxml"));
        Parent root = loader.load();

               //FrontFXML/AuthWelcome.fxml
        ///AdminWelcome.fxml

        Scene scene = new Scene(root, 1280, 720);

        stage.setScene(scene);
        stage.setTitle("MedFlow - Bienvenue");
        stage.setMaximized(true);
        stage.show();
    }
    private void configurePrimaryStage(Stage stage) {
        stage.setMinWidth(APP_WIDTH);
        stage.setMinHeight(APP_HEIGHT);
        stage.sceneProperty().addListener((obs, oldScene, newScene) -> applyStandardWindowSize(stage));
        stage.maximizedProperty().addListener((obs, wasMaximized, isMaximized) -> {
            if (!applyingWindowSize) {
                preferredMaximized = isMaximized;
            }
        });
        stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (!applyingWindowSize
                    && !preferredMaximized
                    && !stage.isMaximized()
                    && Math.abs(newWidth.doubleValue() - APP_WIDTH) > 0.5) {
                applyStandardWindowSize(stage);
            }
        });
        stage.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            if (!applyingWindowSize
                    && !preferredMaximized
                    && !stage.isMaximized()
                    && Math.abs(newHeight.doubleValue() - APP_HEIGHT) > 0.5) {
                applyStandardWindowSize(stage);
            }
        });
    }

    private void applyStandardWindowSize(Stage stage) {
        if (applyingWindowSize) {
            return;
        }

        applyingWindowSize = true;
        try {
            if (preferredMaximized) {
                if (!stage.isMaximized()) {
                    stage.setMaximized(true);
                }
                return;
            }

            if (stage.isMaximized()) {
                stage.setMaximized(false);
            }
            stage.setWidth(APP_WIDTH);
            stage.setHeight(APP_HEIGHT);
            stage.centerOnScreen();
        } finally {
            applyingWindowSize = false;
        }
    }

    public static void main(String[] args) {
        // Charger les variables d'environnement depuis .env.local
        EnvLoader.load();

        // Vérifier les variables d'environnement critiques au démarrage
        System.out.println("\n════════════════════════════════════════════════════════");
        System.out.println("        MedFlow - Vérification des variables d'environnement");
        System.out.println("════════════════════════════════════════════════════════");
        
        String geminiKey = readStartupSetting("GEMINI_API_KEY", "gemini.api.key");
        String perspectiveKey = readStartupSetting("PERSPECTIVE_API_KEY", "perspective.api.key");
        String stripeSecret = readStartupSetting("STRIPE_SECRET_KEY", "stripe.secret.key");
        String recaptchaSiteKey = readStartupSetting("RECAPTCHA_SITE_KEY", "recaptcha.siteKey");
        String recaptchaSecretKey = readStartupSetting("RECAPTCHA_SECRET_KEY", "recaptcha.secretKey");
        String brevoApiKey = readStartupSetting("BREVO_API_KEY", "brevo.api.key");
        String brevoSenderEmail = readStartupSetting("BREVO_SENDER_EMAIL", "brevo.sender.email");
        
        System.out.println("✓ GEMINI_API_KEY: " + (geminiKey != null && !geminiKey.isBlank() ? "✓ Trouvée" : "✗ MANQUANTE"));
        System.out.println("✓ PERSPECTIVE_API_KEY: " + (perspectiveKey != null && !perspectiveKey.isBlank() ? "✓ Trouvée" : "✗ MANQUANTE"));
        System.out.println("✓ STRIPE_SECRET_KEY: " + (stripeSecret != null && !stripeSecret.isBlank() ? "✓ Trouvée" : "✗ MANQUANTE"));
        System.out.println("✓ RECAPTCHA_SITE_KEY: " + (recaptchaSiteKey != null && !recaptchaSiteKey.isBlank() ? "✓ Trouvée" : "✗ MANQUANTE"));
        System.out.println("✓ RECAPTCHA_SECRET_KEY: " + (recaptchaSecretKey != null && !recaptchaSecretKey.isBlank() ? "✓ Trouvée" : "✗ MANQUANTE"));
        System.out.println("✓ BREVO_API_KEY: " + (brevoApiKey != null && !brevoApiKey.isBlank() ? "✓ Trouvée" : "✗ MANQUANTE"));
        System.out.println("✓ BREVO_SENDER_EMAIL: " + (brevoSenderEmail != null && !brevoSenderEmail.isBlank() ? "✓ Trouvée" : "✗ MANQUANTE"));
        
        System.out.println("════════════════════════════════════════════════════════\n");
        
        launch(args);
    }

    private static String readStartupSetting(String envKey, String propKey) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env;
        }
        String prop = System.getProperty(propKey);
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        String fallback = System.getProperty(envKey);
        return (fallback != null && !fallback.isBlank()) ? fallback : null;
    }
}
