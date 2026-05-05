package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminWelcomeController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private TextField searchField;

    @FXML
    private Label dateLabel;

    @FXML
    private Label totalProduitsLabel;

    @FXML
    private Label totalCommandesLabel;

    @FXML
    private Label totalEventsLabel;

    @FXML
    private Label totalReclamationsLabel;

    @FXML
    private Label totalUsersLabel;

    @FXML
    public void initialize() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        if (dateLabel != null) {
            dateLabel.setText("Bienvenue dans votre espace d'administration • " + date);
        }

        if (totalProduitsLabel != null) {
            totalProduitsLabel.setText("40");
        }

        if (totalCommandesLabel != null) {
            totalCommandesLabel.setText("128");
        }

        if (totalEventsLabel != null) {
            totalEventsLabel.setText("24");
        }

        if (totalReclamationsLabel != null) {
            totalReclamationsLabel.setText("12");
        }

        if (totalUsersLabel != null) {
            totalUsersLabel.setText("256");
        }

        Platform.runLater(this::playEntranceAnimation);
    }

    private void playEntranceAnimation() {
        if (rootPane == null) {
            return;
        }

        int index = 0;

        for (Node node : rootPane.lookupAll(".animated-card")) {
            node.setOpacity(0);
            node.setTranslateY(25);

            FadeTransition fade = new FadeTransition(Duration.millis(450), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(index * 80));

            TranslateTransition slide = new TranslateTransition(Duration.millis(450), node);
            slide.setFromY(25);
            slide.setToY(0);
            slide.setDelay(Duration.millis(index * 80));

            fade.play();
            slide.play();

            index++;
        }
    }

    @FXML
    private void openDashboard() {
        showInfo("Vous êtes déjà dans la page d'accueil admin.");
    }

    @FXML
    private void openPatients() {
        goTo("/FXML/PatientsAdmin.fxml");
    }

    @FXML
    private void openUsers() {
        goTo("/FXML/UtilisateursAdmin.fxml");
    }

    @FXML
    private void openRoles() {
        goTo("/FXML/DemandesRole.fxml");
    }

    @FXML
    private void openUserRoles() {
        goTo("/FXML/UserRoles.fxml");
    }

    @FXML
    private void openProduits() {
        goTo("/ProduitAdmin.fxml");
    }

    @FXML
    private void openCommandes() {
        goTo("/CommandeAdmin.fxml");
    }

    @FXML
    private void openDetection() {
        goTo("/FXML/DetectionEpidemie.fxml");
    }

    @FXML
    private void openStockRupture() {
        goTo("/FXML/StockRupture.fxml");
    }

    @FXML
    private void openStatsProduits() {
        goTo("/StatProduit.fxml");
    }

    @FXML
    private void openEvents() {
        goTo("/FXML/EvenementsAdmin.fxml");
    }

    @FXML
    private void openEventParticipants() {
        goTo("/FXML/ParticipantsEvenement.fxml");
    }

    @FXML
    private void openRessources() {
        goTo("/FXML/RessourcesAdmin.fxml");
    }

    @FXML
    private void openStatsEvents() {
        goTo("/FXML/StatsEvenements.fxml");
    }

    @FXML
    private void openStatsRessources() {
        goTo("/FXML/StatsRessources.fxml");
    }

    @FXML
    private void openConsultations() {
        goTo("/FXML/ConsultationsParDocteur.fxml");
    }

    @FXML
    private void openRendezVous() {
        goTo("/FXML/RendezVousAdmin.fxml");
    }

    @FXML
    private void openStatsConsultations() {
        goTo("/FXML/StatsConsultations.fxml");
    }

    @FXML
    private void openReclamations() {
        goTo("/FXML/ReclamationsAdmin.fxml");
    }

    @FXML
    private void openReponsesReclamations() {
        goTo("/FXML/ReponsesReclamationsAdmin.fxml");
    }

    @FXML
    private void openReclamationsUrgentes() {
        goTo("/FXML/ReclamationsUrgentes.fxml");
    }

    @FXML
    private void openStatsReclamations() {
        goTo("/FXML/StatsReclamations.fxml");
    }

    @FXML
    private void openPosts() {
        goTo("/FXML/PostsAdmin.fxml");
    }

    @FXML
    private void openBlogComments() {
        goTo("/FXML/CommentairesAdmin.fxml");
    }

    @FXML
    private void openBlogModeration() {
        goTo("/FXML/ModerationBlog.fxml");
    }

    @FXML
    private void openPostsEnAttente() {
        goTo("/FXML/PostsEnAttente.fxml");
    }

    @FXML
    private void openStatsBlog() {
        goTo("/FXML/StatsBlog.fxml");
    }

    @FXML
    private void openNewProduct() {
        goTo("/FXML/AjoutProduit.fxml");
    }

    @FXML
    private void openNewEvent() {
        goTo("/FXML/AjoutEvenement.fxml");
    }

    @FXML
    private void openNewUser() {
        goTo("/FXML/AjoutUtilisateur.fxml");
    }

    @FXML
    private void openReports() {
        goTo("/FXML/DashboardStatsAdmin.fxml");
    }

    @FXML
    private void goBackSite() {
        goTo("/FXML/Home.fxml");
    }

    @FXML
    private void logout() {
        goTo("/FXML/Login.fxml");
    }

    private void goTo(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);

            if (url == null) {
                showWarning("Page introuvable : " + fxmlPath + "\n\nRemplace ce chemin par le vrai nom de ton fichier FXML.");
                return;
            }

            Parent page = FXMLLoader.load(url);
            rootPane.getScene().setRoot(page);

        } catch (IOException e) {
            e.printStackTrace();
            showWarning("Erreur lors de l'ouverture de la page : " + fxmlPath);
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Navigation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}