package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Post;
import tn.esprit.services.PostService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class BlogController {

    @FXML
    private VBox postsContainer;

    @FXML
    private VBox rightSidebar;

    @FXML private ComboBox<String> sortBox;
    @FXML private TextField searchField;

    private List<Post> allPosts = null;

    private final PostService postService = new PostService();

    @FXML
    public void initialize() {
        // Initialisation du tri
        if (sortBox != null) {
            sortBox.getItems().addAll("Date (plus récent)", "Date (plus ancien)", "Titre (A-Z)", "Titre (Z-A)", "Catégorie");
            sortBox.setValue("Date (plus récent)");
            sortBox.valueProperty().addListener((obs, oldVal, newVal) -> loadPosts());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> loadPosts());
        }
        loadPosts();
    }

    private void loadPosts() {
        allPosts = new PostService().getAllPosts();
        List<Post> posts = new java.util.ArrayList<>(allPosts);

        // Filtrage par recherche en temps réel
        if (searchField != null && searchField.getText() != null && !searchField.getText().isBlank()) {
            String search = searchField.getText().toLowerCase().trim();

            posts.removeIf(post ->
                    (post.getTitre() == null || !post.getTitre().toLowerCase().contains(search)) &&
                            (post.getContenu() == null || !post.getContenu().toLowerCase().contains(search)) &&
                            (post.getCategorie() == null || !post.getCategorie().toLowerCase().contains(search)) &&
                            (post.getLocalisation() == null || !post.getLocalisation().toLowerCase().contains(search)) &&
                            (post.getUser() == null || post.getUser().getNom() == null || !post.getUser().getNom().toLowerCase().contains(search))
            );
        }

        // Tri
        if (sortBox != null && sortBox.getValue() != null) {
            switch (sortBox.getValue()) {
                case "Date (plus récent)":
                    posts.sort((a, b) -> b.getDate_creation().compareTo(a.getDate_creation()));
                    break;
                case "Date (plus ancien)":
                    posts.sort((a, b) -> a.getDate_creation().compareTo(b.getDate_creation()));
                    break;
                case "Titre (A-Z)":
                    posts.sort((a, b) -> a.getTitre().compareToIgnoreCase(b.getTitre()));
                    break;
                case "Titre (Z-A)":
                    posts.sort((a, b) -> b.getTitre().compareToIgnoreCase(a.getTitre()));
                    break;
                case "Catégorie":
                    posts.sort((a, b) -> {
                        String ca = a.getCategorie() == null ? "" : a.getCategorie();
                        String cb = b.getCategorie() == null ? "" : b.getCategorie();
                        return ca.compareToIgnoreCase(cb);
                    });
                    break;
            }
        }

        postsContainer.getChildren().clear();
        if (rightSidebar != null) {
            rightSidebar.getChildren().clear();
        }

        if (posts.isEmpty()) {
            postsContainer.getChildren().add(createEmptyState());
            return;
        }

        // colonne droite
        if (rightSidebar != null) {
            rightSidebar.getChildren().add(createRecentPostCard(posts.get(0)));
            rightSidebar.getChildren().add(createTagsCard(posts));
        }

        // colonne gauche
        for (Post post : posts) {
            postsContainer.getChildren().add(createPostCard(post));
        }
    }
    @FXML
    private void openAddPostPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/AddPost.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) postsContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle("Nouveau Post");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir la page");
            alert.setContentText("La page d'ajout du post n'a pas pu être chargée.");
            alert.showAndWait();
        }
    }
    private Node createPostCard(Post post) {
        VBox card = new VBox(14);
        card.getStyleClass().add("post-row-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setFillWidth(true);
        card.setMaxWidth(Double.MAX_VALUE);

        HBox topSection = new HBox(18);
        topSection.setAlignment(Pos.TOP_LEFT);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(220);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("post-row-image");

        Image image = loadPostImage(post);
        if (image != null) {
            imageView.setImage(image);
        }

        VBox contentBox = new VBox(10);
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.getStyleClass().add("post-row-content");

        Label category = new Label(safeText(post.getCategorie()));
        category.getStyleClass().add("post-category-badge");
        boolean hasCategory = !safeText(post.getCategorie()).isBlank();
        category.setManaged(hasCategory);
        category.setVisible(hasCategory);

        Label title = new Label(safeText(post.getTitre()));
        title.getStyleClass().add("post-row-title");
        title.setWrapText(true);

        Label summary = new Label(getShortContent(post.getContenu(), 90));
        summary.getStyleClass().add("post-row-summary");
        summary.setWrapText(true);

        Label meta = new Label(buildMeta(post));
        meta.getStyleClass().add("post-row-meta");
        meta.setWrapText(true);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("small-edit-btn");

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("small-delete-btn");

        actions.getChildren().addAll(editBtn, deleteBtn);

        contentBox.getChildren().addAll(category, title, summary, meta, actions);

        topSection.getChildren().addAll(imageView, contentBox);

        Region separator = new Region();
        separator.getStyleClass().add("soft-separator");
        separator.setPrefHeight(1);
        separator.setMaxWidth(Double.MAX_VALUE);

        HBox footer = new HBox(22);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("post-row-footer");

        Label reactionLabel = new Label("👍 " + post.getNbr_reactions() + " reaction(s)");
        reactionLabel.getStyleClass().add("footer-meta");

        Label commentLabel = new Label("💬 " + post.getNbr_commentaires() + " commentaire(s)");
        commentLabel.getStyleClass().add("footer-meta");


        footer.getChildren().addAll(reactionLabel, commentLabel);

        card.getChildren().addAll(topSection, separator, footer);

        card.setOnMouseClicked(e -> openPostDetail(post, e));

        editBtn.setOnAction(e -> {
            e.consume();
            openEditPost(post);
        });

        deleteBtn.setOnAction(e -> {
            e.consume();
            deletePost(post);
        });

        editBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        deleteBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        return card;
    }
    private Node createRecentPostCard(Post post) {
        VBox card = new VBox(12);
        card.getStyleClass().add("sidebar-card");

        Label title = new Label("Recent post");
        title.getStyleClass().add("sidebar-title");

        Label recentTitle = new Label(safeText(post.getTitre()));
        recentTitle.getStyleClass().add("sidebar-link-title");
        recentTitle.setWrapText(true);

        Label date = new Label(formatDate(post));
        date.getStyleClass().add("sidebar-date");

        Label viewAll = new Label("View all latest news");
        viewAll.getStyleClass().add("sidebar-link");

        viewAll.setOnMouseClicked(e -> {
            e.consume();
            // tu peux ici faire une navigation si tu veux
        });

        card.getChildren().addAll(title, recentTitle, date, viewAll);
        return card;
    }

    private Node createTagsCard(List<Post> posts) {
        VBox card = new VBox(14);
        card.getStyleClass().add("sidebar-card");

        Label title = new Label("Tags");
        title.getStyleClass().add("sidebar-title");

        FlowPane tagsPane = new FlowPane();
        tagsPane.setHgap(8);
        tagsPane.setVgap(8);

        int maxTags = Math.min(posts.size(), 6);
        for (int i = 0; i < maxTags; i++) {
            String categorie = safeText(posts.get(i).getCategorie());
            if (!categorie.isBlank()) {
                Label tag = new Label("#" + categorie.toLowerCase());
                tag.getStyleClass().add("tag-chip");
                tagsPane.getChildren().add(tag);
            }
        }

        card.getChildren().addAll(title, tagsPane);
        return card;
    }

    private Image loadPostImage(Post post) {
        try {
            String imagePath = safeText(post.getImg_post());

            if (!imagePath.isBlank()) {
                if (imagePath.startsWith("http://")
                        || imagePath.startsWith("https://")
                        || imagePath.startsWith("file:")) {
                    return new Image(imagePath, true);
                }

                URL directResource = getClass().getResource(imagePath);
                if (directResource != null) {
                    return new Image(directResource.toExternalForm(), true);
                }

                URL uploadsResource = getClass().getResource("/uploads/" + imagePath);
                if (uploadsResource != null) {
                    return new Image(uploadsResource.toExternalForm(), true);
                }

                URL imagesResource = getClass().getResource("/images/" + imagePath);
                if (imagesResource != null) {
                    return new Image(imagesResource.toExternalForm(), true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            URL defaultImg = getClass().getResource("/images/logo.png");
            if (defaultImg != null) {
                return new Image(defaultImg.toExternalForm(), true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String buildMeta(Post post) {
        String date = formatDate(post);
        String localisation = safeText(post.getLocalisation()).isBlank() ? "Tunis" : post.getLocalisation();

        String auteur = "MedFlow Admin";
        try {
            if (post.getUser() != null
                    && post.getUser().getNom() != null
                    && !post.getUser().getNom().trim().isEmpty()) {
                auteur = post.getUser().getNom().trim();
            }
        } catch (Exception ignored) {
        }

        return date + " · " + localisation + " · " + auteur;
    }

    private String formatDate(Post post) {
        try {
            if (post.getDate_creation() != null) {
                return post.getDate_creation().toString();
            }
        } catch (Exception ignored) {
        }
        return "Date inconnue";
    }

    private String getShortContent(String text, int maxLength) {
        String clean = safeText(text).replace("\n", " ").replace("\r", " ").trim();

        while (clean.contains("  ")) {
            clean = clean.replace("  ", " ");
        }

        if (clean.length() <= maxLength) {
            return clean;
        }

        return clean.substring(0, maxLength).trim() + "...";
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private Node createEmptyState() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.getStyleClass().add("sidebar-card");

        Label title = new Label("Aucun post disponible");
        title.getStyleClass().add("sidebar-title");

        Label subtitle = new Label("Il n’y a encore aucun article à afficher.");
        subtitle.getStyleClass().add("post-row-summary");

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private Node createErrorState() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.getStyleClass().add("sidebar-card");

        Label title = new Label("Erreur");
        title.getStyleClass().add("sidebar-title");

        Label subtitle = new Label("Impossible de charger les posts.");
        subtitle.getStyleClass().add("post-row-summary");

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private void deletePost(Post post) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer ce post");
        confirmation.setContentText("Voulez-vous vraiment supprimer ce post ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                postService.supprimer(post);
                loadPosts();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openPostDetail(Post post, MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/PostDetail.fxml"));
            Parent root = loader.load();

            PostDetailController controller = loader.getController();
            controller.setPost(post);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle("Détail du Post");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditPost(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/AddPost.fxml"));
            Parent root = loader.load();

            AddPostController controller = loader.getController();
            controller.setPostToEdit(post);

            Stage stage = (Stage) postsContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle("Modifier le Post");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
