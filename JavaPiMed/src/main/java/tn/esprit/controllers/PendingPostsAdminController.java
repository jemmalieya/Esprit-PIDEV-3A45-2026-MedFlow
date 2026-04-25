package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.Post;
import tn.esprit.services.MailService;
import tn.esprit.services.PostService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PendingPostsAdminController {

    @FXML private Label pendingCountLabel;
    @FXML private Label totalPendingLabel;
    @FXML private Label approvedTodayLabel;
    @FXML private Label rejectedTodayLabel;

    @FXML private TextField searchField;

    @FXML private TableView<Post> pendingPostsTable;
    @FXML private TableColumn<Post, String> titleCol;
    @FXML private TableColumn<Post, String> categoryCol;
    @FXML private TableColumn<Post, String> contentCol;
    @FXML private TableColumn<Post, String> dateCol;
    @FXML private TableColumn<Post, String> userCol;
    @FXML private TableColumn<Post, Void> actionsCol;

    private final PostService postService = new PostService();
    private List<Post> allPendingPosts = new ArrayList<>();
    private final MailService mailService = new MailService();

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

        contentCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        shortText(data.getValue().getContenu(), 80)
                                + " | " +
                                safeText(data.getValue().getModeration_message())
                )
        );

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

                approveBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 7 12;");
                rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 7 12;");

                approveBtn.setOnAction(e -> {
                    Post post = getTableView().getItems().get(getIndex());
                    approvePost(post);
                });

                rejectBtn.setOnAction(e -> {
                    Post post = getTableView().getItems().get(getIndex());
                    rejectPost(post);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadPendingPosts() {
        allPendingPosts = postService.getPendingPostsForAdmin();

        pendingPostsTable.setItems(FXCollections.observableArrayList(allPendingPosts));

        updateStats();
    }

    private void filterTable() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        if (search.isEmpty()) {
            pendingPostsTable.setItems(FXCollections.observableArrayList(allPendingPosts));
            return;
        }

        List<Post> filtered = new ArrayList<>();

        for (Post post : allPendingPosts) {
            String titre = safeText(post.getTitre()).toLowerCase();
            String contenu = safeText(post.getContenu()).toLowerCase();
            String categorie = safeText(post.getCategorie()).toLowerCase();

            if (titre.contains(search) || contenu.contains(search) || categorie.contains(search)) {
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
                boolean success = postService.approvePost(post.getId());

                if (success) {
                    approvedToday++;
                    sendModerationEmail(post, true);
                    showInfo("Succès", "Post approuvé avec succès.");
                    loadPendingPosts();
                } else {
                    showError("Erreur", "Impossible d’approuver ce post.");
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
                boolean success = postService.rejectPost(post.getId());

                if (success) {
                    rejectedToday++;
                    sendModerationEmail(post, false);
                    showInfo("Succès", "Post refusé avec succès.");
                    loadPendingPosts();
                } else {
                    showError("Erreur", "Impossible de refuser ce post.");
                }
            }
        });
    }

    private void updateStats() {
        int total = allPendingPosts == null ? 0 : allPendingPosts.size();

        if (pendingCountLabel != null) {
            pendingCountLabel.setText(total + " post(s) en attente");
        }

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

    @FXML
    private void goBackToBlog() {
        navigateTo("/FrontFXML/Blog.fxml", "MedFlow - Blog");
    }

    @FXML
    private void openBlogStats() {
        navigateTo("/BlogStats.fxml", "MedFlow - Statistiques Blog");
    }

    @FXML
    private void handleLogout() {
        navigateTo("/FrontFXML/Login.fxml", "MedFlow - Connexion");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) pendingPostsTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1650, 960));
            stage.setTitle(title);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Navigation", "Impossible d'ouvrir la page demandée.");
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String shortText(String value, int max) {
        if (value == null) return "";

        String clean = value.replace("\n", " ").replace("\r", " ").trim();

        if (clean.length() <= max) {
            return clean;
        }

        return clean.substring(0, max).trim() + "...";
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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

    private String getPostAuthorName(Post post) {
        if (post == null || post.getUser() == null) {
            return "Utilisateur";
        }

        String prenom = post.getUser().getPrenom() != null ? post.getUser().getPrenom().trim() : "";
        String nom = post.getUser().getNom() != null ? post.getUser().getNom().trim() : "";

        String fullName = (prenom + " " + nom).trim();

        if (!fullName.isEmpty()) {
            return fullName;
        }

        return "Utilisateur";
    }

    private void sendModerationEmail(Post post, boolean approved) {
        try {
            if (post == null || post.getUser() == null) {
                return;
            }

            String email = post.getUser().getEmailUser();

            String prenom = post.getUser().getPrenom() != null ? post.getUser().getPrenom().trim() : "";
            String nom = post.getUser().getNom() != null ? post.getUser().getNom().trim() : "";
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
}