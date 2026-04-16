package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class acceuil {

    @FXML
    private void openDashboard(ActionEvent event) {
        try {
            URL dashboardFxml = Objects.requireNonNull(
                    getClass().getResource("/acceuil.fxml"),
                    "FXML introuvable: /acceuil.fxml"
            );

            Parent root = FXMLLoader.load(dashboardFxml);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de charger l'ecran /acceuil.fxml", e);
        }
    }
}
