package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import tn.esprit.entities.Commentaire;
import tn.esprit.entities.Post;
import tn.esprit.services.CommentaireService;
import tn.esprit.services.PostService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BlogStatsAdminController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label totalPostsLabel;

    @FXML
    private Label totalCommentairesLabel;

    @FXML
    private Label postsAnonymesLabel;

    @FXML
    private Label categoriesLabel;

    @FXML
    private BarChart<String, Number> categoryBarChart;

    @FXML
    private PieChart anonymousPieChart;

    @FXML
    private LineChart<String, Number> postsLineChart;

    private final PostService postService = new PostService();
    private final CommentaireService commentaireService = new CommentaireService();

    @FXML
    public void initialize() {
        configureCharts();
        loadBlogStats();
    }

    private void configureCharts() {
        if (categoryBarChart != null) {
            categoryBarChart.setAnimated(false);
            categoryBarChart.setLegendVisible(false);
            categoryBarChart.setCategoryGap(28);
            categoryBarChart.setBarGap(6);
        }

        if (postsLineChart != null) {
            postsLineChart.setAnimated(false);
            postsLineChart.setLegendVisible(false);
            postsLineChart.setCreateSymbols(true);
        }

        if (anonymousPieChart != null) {
            anonymousPieChart.setLegendVisible(true);
            anonymousPieChart.setLabelsVisible(true);
        }
    }

    private void loadBlogStats() {
        try {
            List<Post> posts = postService.recuperer();
            List<Commentaire> commentaires = commentaireService.recuperer();

            int totalPosts = posts == null ? 0 : posts.size();
            int totalCommentaires = commentaires == null ? 0 : commentaires.size();

            int postsAnonymes = 0;

            Map<String, Integer> categoryCounts = new LinkedHashMap<>();
            Map<String, Integer> dateCounts = new LinkedHashMap<>();

            if (posts != null) {
                for (Post post : posts) {
                    if (post == null) {
                        continue;
                    }

                    if (post.isEst_anonyme()) {
                        postsAnonymes++;
                    }

                    String categorie = safe(post.getCategorie()).trim();

                    if (categorie.isBlank()) {
                        categorie = "Sans catégorie";
                    }

                    categoryCounts.put(categorie, categoryCounts.getOrDefault(categorie, 0) + 1);

                    String dateKey = "Sans date";

                    try {
                        if (post.getDate_creation() != null) {
                            LocalDate date = post.getDate_creation().toLocalDate();
                            dateKey = date.toString();
                        }
                    } catch (Exception ignored) {
                    }

                    dateCounts.put(dateKey, dateCounts.getOrDefault(dateKey, 0) + 1);
                }
            }

            totalPostsLabel.setText(String.valueOf(totalPosts));
            totalCommentairesLabel.setText(String.valueOf(totalCommentaires));
            postsAnonymesLabel.setText(String.valueOf(postsAnonymes));
            categoriesLabel.setText(String.valueOf(categoryCounts.size()));

            updateCategoryBarChart(categoryCounts);
            updateAnonymousPieChart(totalPosts, postsAnonymes);
            updatePostsLineChart(dateCounts);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les statistiques du blog.");
        }
    }

    private void updateCategoryBarChart(Map<String, Integer> categoryCounts) {
        if (categoryBarChart == null) {
            return;
        }

        categoryBarChart.getData().clear();
        categoryBarChart.setAnimated(false);
        categoryBarChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Posts");

        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            XYChart.Data<String, Number> data =
                    new XYChart.Data<>(shortLabel(entry.getKey(), 18), entry.getValue());

            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    applyBarStyle(newNode);
                }
            });

            series.getData().add(data);
        }

        categoryBarChart.getData().add(series);

        Platform.runLater(() -> {
            for (XYChart.Series<String, Number> s : categoryBarChart.getData()) {
                for (XYChart.Data<String, Number> d : s.getData()) {
                    if (d.getNode() != null) {
                        applyBarStyle(d.getNode());
                    }
                }
            }
        });
    }

    private void updateAnonymousPieChart(int totalPosts, int postsAnonymes) {
        if (anonymousPieChart == null) {
            return;
        }

        int nonAnonymes = Math.max(totalPosts - postsAnonymes, 0);

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Anonymes", postsAnonymes),
                new PieChart.Data("Non anonymes", nonAnonymes)
        );

        anonymousPieChart.setData(pieData);
        anonymousPieChart.setLegendVisible(true);
        anonymousPieChart.setLabelsVisible(true);
    }

    private void updatePostsLineChart(Map<String, Integer> dateCounts) {
        if (postsLineChart == null) {
            return;
        }

        postsLineChart.getData().clear();
        postsLineChart.setAnimated(false);
        postsLineChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Évolution");

        for (Map.Entry<String, Integer> entry : dateCounts.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        postsLineChart.getData().add(series);
    }

    private void applyBarStyle(Node node) {
        node.setStyle(
                "-fx-bar-fill: #2563eb;" +
                        "-fx-background-color: linear-gradient(to top, #0891b2, #38bdf8);" +
                        "-fx-background-radius: 8 8 0 0;"
        );
    }

    private String shortLabel(String value, int max) {
        if (value == null) {
            return "";
        }

        String clean = value.trim();

        if (clean.length() <= max) {
            return clean;
        }

        return clean.substring(0, max).trim() + "...";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    /* ================= NAVIGATION SIDEBAR ================= */

    @FXML
    private void openDashboard() {
        goTo("/AdminWelcome.fxml");
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
        goTo("/DetectionEpidemie.fxml");
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
        goTo("/PostsAdmin.fxml");
    }

    @FXML
    private void openPendingPostsAdminPage() {
        goTo("/PendingPostsAdmin.fxml");
    }

    @FXML
    private void openPostsEnAttente() {
        goTo("/PendingPostsAdmin.fxml");
    }

    @FXML
    private void openBlogStats() {
        loadBlogStats();
    }

    @FXML
    private void openStatsBlog() {
        goTo("/BlogStatAdmin.fxml");
    }

    @FXML
    private void openReclamationStats() {
        goTo("/StatsReclamations.fxml");
    }

    @FXML
    private void handleLogout() {
        goToFirst("/FXML/Login.fxml", "/FrontFXML/Login.fxml");
    }

    @FXML
    private void logout() {
        goToFirst("/FXML/Login.fxml", "/FrontFXML/Login.fxml");
    }

    @FXML
    private void goBackSite() {
        goTo("/FXML/Home.fxml");
    }

    private void goTo(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);

            if (url == null) {
                showWarning("Navigation", "Page introuvable : " + fxmlPath);
                return;
            }

            Parent page = FXMLLoader.load(url);
            rootPane.getScene().setRoot(page);

        } catch (IOException e) {
            e.printStackTrace();
            showWarning("Navigation", "Erreur lors de l'ouverture de la page : " + fxmlPath);
        }
    }

    private void goToFirst(String... paths) {
        for (String path : paths) {
            if (getClass().getResource(path) != null) {
                goTo(path);
                return;
            }
        }

        showWarning("Navigation", "Aucun fichier FXML trouvé pour cette page.");
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}