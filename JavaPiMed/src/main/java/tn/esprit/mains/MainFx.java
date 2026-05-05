package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.tools.SystemNotification;

public class MainFx extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        SystemNotification.initTray();

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

    public static void main(String[] args) {
        // Vérifier les variables d'environnement critiques au démarrage
        System.out.println("\n════════════════════════════════════════════════════════");
        System.out.println("        MedFlow - Vérification des variables d'environnement");
        System.out.println("════════════════════════════════════════════════════════");
        
        String geminiKey = System.getenv("GEMINI_API_KEY");
        String perspectiveKey = System.getenv("PERSPECTIVE_API_KEY");
        String stripeSecret = System.getenv("STRIPE_SECRET_KEY");
        
        System.out.println("✓ GEMINI_API_KEY: " + (geminiKey != null && !geminiKey.isBlank() ? "✓ Trouvée" : "✗ MANQUANTE"));
        System.out.println("✓ PERSPECTIVE_API_KEY: " + (perspectiveKey != null && !perspectiveKey.isBlank() ? "✓ Trouvée" : "✗ MANQUANTE"));
        System.out.println("✓ STRIPE_SECRET_KEY: " + (stripeSecret != null && !stripeSecret.isBlank() ? "✓ Trouvée" : "✗ MANQUANTE"));
        
        System.out.println("════════════════════════════════════════════════════════\n");
        
        launch(args);
    }
}