package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFx extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProduitDashboard.fxml"));
        //FrontFXML/Accueil.fxml
        //ProduitDashboard.fxml
        Parent root = loader.load();

        // Scene scene = new Scene(root);
        Scene scene = new Scene(root, 1300, 780);
        scene.getStylesheets().add(getClass().getResource("/evenement-dashboard.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("MedFlow - Front Office");
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}