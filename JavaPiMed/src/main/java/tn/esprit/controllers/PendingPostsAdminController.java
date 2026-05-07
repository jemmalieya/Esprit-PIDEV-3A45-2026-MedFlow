package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Post;
import tn.esprit.services.MailService;
import tn.esprit.services.PostService;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PendingPostsAdminController {

    @FXML
    private BorderPane rootPane;

    @FXML
    private Label totalPendingLabel;

    @FXML
    private Label approvedTodayLabel;

    @FXML
    private Label rejectedTodayLabel;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Post> pendingPostsTable;

    @FXML
    private TableColumn<Post, String> titleCol;

    @FXML
    private TableColumn<Post, String> categoryCol;

    @FXML
    private TableColumn<Post, String> contentCol;

    @FXML
    private TableColumn<Post, String> dateCol;

    @FXML
    private TableColumn<Post, String> userCol;

    @FXML
    private TableColumn<Post, Void> actionsCol;

    private final PostService postService = new PostService();
    private final MailService mailService = new MailService();

    private List<Post> allPendingPosts = new ArrayList<>();

    private int approvedToday = 0;
    private int rejectedToday = 0;

    @FXML
    public void initialize() {
        setupTable();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> filterTable());
        }

        loadPendingPosts();
    }

    private void setupTable() {
        titleCol.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getTitre()))
        );

        categoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(safeText(data.getValue().getCategorie()))
        );

        contentCol.setCellValueFactory(data -> {
            Post post = data.getValue();

            String contenu = shortText(post.getContenu(), 80);
            String moderationMessage = safeText(post.getModeration_message());

            if (!moderationMessage.isBlank()) {
                return new SimpleStringProperty(contenu + " | " + moderationMessage);
            }

            return new SimpleStringProperty(contenu);
        });

        dateCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDate_creation() != null
                        ? data.getValue().getDate_creation().toString()
                        : "")
        );

        userCol.setCellValueFactory(data ->
                new SimpleStringProperty(getPostAuthorName(data.getValue()))
        );

        actionsCol.setCellFactory(column -> new TableCell<>() {

            private final Button approveBtn = new Button("Approuver");
            private final Button rejectBtn = new Button("Refuser");
            private final HBox box = new HBox(8, approveBtn, rejectBtn);

            {
                box.setAlignment(Pos.CENTER);

                approveBtn.setStyle(
                        "-fx-background-color: #22c55e;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 8;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 7 12;" +
                                "-fx-cursor: hand;"
                );

                rejectBtn.setStyle(
                        "-fx-background-color: #ef4444;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 8;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 7 12;" +
                                "-fx-cursor: hand;"
                );

                approveBtn.setOnAction(e -> {
                    Post post = getCurrentPost();
                    if (post != null) {
                        approvePost(post);
                    }
                });

                rejectBtn.setOnAction(e -> {
                    Post post = getCurrentPost();
                    if (post != null) {
                        rejectPost(post);
                    }
                });
            }

            private Post getCurrentPost() {
                int index = getIndex();

                if (index < 0 || index >= getTableView().getItems().size()) {
                    return null;
                }

                return getTableView().getItems().get(index);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });

        pendingPostsTable.setPlaceholder(new Label("Aucun post en attente."));
    }

    private void loadPendingPosts() {
        try {
            allPendingPosts = postService.getPendingPostsForAdmin();

            if (allPendingPosts == null) {
                allPendingPosts = new ArrayList<>();
            }

            pendingPostsTable.setItems(FXCollections.observableArrayList(allPendingPosts));
            updateStats();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Chargement", "Impossible de charger les posts en attente.");
        }
    }

    private void filterTable() {
        String search = searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase().trim();

        if (search.isEmpty()) {
            pendingPostsTable.setItems(FXCollections.observableArrayList(allPendingPosts));
            return;
        }

        List<Post> filtered = new ArrayList<>();

        for (Post post : allPendingPosts) {
            String titre = safeText(post.getTitre()).toLowerCase();
            String contenu = safeText(post.getContenu()).toLowerCase();
            String categorie = safeText(post.getCategorie()).toLowerCase();
            String auteur = getPostAuthorName(post).toLowerCase();

            if (titre.contains(search)
                    || contenu.contains(search)
                    || categorie.contains(search)
                    || auteur.contains(search)) {
                filtered.add(post);
            }
        }

        pendingPostsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void approvePost(Post post) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Validation");
        confirmation.setHeaderText("Approuver ce post ?");
        confirmation.setContentText("Le post sera visible dans le blog.");

        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    boolean success = postService.approvePost(post.getId());

                    if (success) {
                        approvedToday++;
                        sendModerationEmail(post, true);
                        showInfo("Post approuvé avec succès.");
                        loadPendingPosts();
                    } else {
                        showError("Approuver", "Impossible d'approuver ce post.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Approuver", "Erreur lors de l'approbation du post.");
                }
            }
        });
    }

    private void rejectPost(Post post) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Refus");
        confirmation.setHeaderText("Refuser ce post ?");
        confirmation.setContentText("Le post restera invisible dans le blog.");

        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    boolean success = postService.rejectPost(post.getId());

                    if (success) {
                        rejectedToday++;
                        sendModerationEmail(post, false);
                        showInfo("Post refusé avec succès.");
                        loadPendingPosts();
                    } else {
                        showError("Refuser", "Impossible de refuser ce post.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    showError("Refuser", "Erreur lors du refus du post.");
                }
            }
        });
    }

    private void updateStats() {
        int total = allPendingPosts == null ? 0 : allPendingPosts.size();

        if (totalPendingLabel != null) {
            totalPendingLabel.setText(String.valueOf(total));
        }

        if (approvedTodayLabel != null) {
            approvedTodayLabel.setText(String.valueOf(approvedToday));
        }

        if (rejectedTodayLabel != null) {
            rejectedTodayLabel.setText(String.valueOf(rejectedToday));
        }
    }

    @FXML
    private void refreshPosts() {
        loadPendingPosts();
    }

    private String getPostAuthorName(Post post) {
        if (post == null || post.getUser() == null) {
            return "Utilisateur";
        }

        String prenom = post.getUser().getPrenom() != null
                ? post.getUser().getPrenom().trim()
                : "";

        String nom = post.getUser().getNom() != null
                ? post.getUser().getNom().trim()
                : "";

        String fullName = (prenom + " " + nom).trim();

        if (!fullName.isEmpty()) {
            return fullName;
        }

        if (!nom.isEmpty()) {
            return nom;
        }

        if (!prenom.isEmpty()) {
            return prenom;
        }

        return "Utilisateur";
    }

    private void sendModerationEmail(Post post, boolean approved) {
        try {
            if (post == null || post.getUser() == null) {
                return;
            }

            String email = post.getUser().getEmailUser();

            if (email == null || email.isBlank()) {
                return;
            }

            String prenom = post.getUser().getPrenom() != null
                    ? post.getUser().getPrenom().trim()
                    : "";

            String nom = post.getUser().getNom() != null
                    ? post.getUser().getNom().trim()
                    : "";

            String fullName = (prenom + " " + nom).trim();

            mailService.sendPostModerationEmail(
                    email,
                    fullName,
                    post.getTitre(),
                    approved
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
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
        goTo("/ReclamationsAdmin.fxml");
    }


    @FXML
    private void openStatsReclamations() {
        goTo("/StatsReclamations.fxml");
    }

    @FXML
    private void openPosts() {
        goTo("/PostsAdmin.fxml");
    }

    @FXML
    private void openBlogComments() {
        goToFirst(
                "/BlogCommentsAdmin.fxml",
                "/CommentairesAdmin.fxml",
                "/FXML/CommentairesAdmin.fxml"
        );
    }

    @FXML
    private void openBlogModeration() {
        goTo("/PendingPostsAdmin.fxml");
    }

    @FXML
    private void openPostsEnAttente() {
        goTo("/PendingPostsAdmin.fxml");
    }

    @FXML
    private void openStatsBlog() {
        goTo("/BlogStatAdmin.fxml");
    }

    @FXML
    private void goBackSite() {
        goTo("/FXML/Home.fxml");
    }

    @FXML
    private void logout() {
        goToFirst(
                "/FXML/Login.fxml",
                "/FrontFXML/Login.fxml"
        );
    }

    private void goTo(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);

            if (url == null) {
                showWarning(
                        "Page introuvable : " + fxmlPath +
                                "\n\nVérifie le nom exact du fichier FXML."
                );
                return;
            }

            Parent page = FXMLLoader.load(url);
            rootPane.getScene().setRoot(page);

        } catch (IOException e) {
            e.printStackTrace();
            showWarning("Erreur lors de l'ouverture de la page : " + fxmlPath);
        }
    }

    private void goToFirst(String... fxmlPaths) {
        for (String path : fxmlPaths) {
            URL url = getClass().getResource(path);

            if (url != null) {
                goTo(path);
                return;
            }
        }

        StringBuilder message = new StringBuilder("Aucune page trouvée parmi :\n");

        for (String path : fxmlPaths) {
            message.append("- ").append(path).append("\n");
        }

        showWarning(message.toString());
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String shortText(String value, int max) {
        if (value == null) {
            return "";
        }

        String clean = value.replace("\n", " ").replace("\r", " ").trim();

        while (clean.contains("  ")) {
            clean = clean.replace("  ", " ");
        }

        if (clean.length() <= max) {
            return clean;
        }

        return clean.substring(0, max).trim() + "...";
    }

    private void showInfo(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Navigation");
        alert.setHeaderText(null);
        alert.setContentText(message);
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