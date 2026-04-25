package tn.esprit.controllers;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.util.LinkedHashMap;
import java.util.Map;

import java.io.FileOutputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import java.awt.Desktop;
import java.io.File;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import javafx.scene.control.TextArea;
import tn.esprit.entities.Commentaire;
import tn.esprit.entities.User;
import tn.esprit.services.CommentaireService;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import java.time.LocalDateTime;
import java.util.ArrayList;


import tn.esprit.tools.SessionManager;
import javafx.scene.control.ScrollPane;

import javafx.scene.layout.StackPane;

public class BlogController {

    @FXML
    private VBox postsContainer;
    @FXML private Label reclamationArrow;
    @FXML private VBox submenuVBox;

    @FXML private Label totalPostsLabel;
    @FXML private Label totalCommentairesLabel;
    @FXML private Label postsAnonymesLabel;
    @FXML private Label categoriesLabel;
    @FXML
    private VBox rightSidebar;
    @FXML private BarChart<String, Number> categoryBarChart;
    @FXML private PieChart anonymousPieChart;
    @FXML private LineChart<String, Number> postsLineChart;
    @FXML private ComboBox<String> sortBox;
    @FXML private TextField searchField;

    @FXML
    private Label notificationBadgeLabel;

    @FXML
    private Button notificationButton;

    private List<Post> allPosts = null;

    private final PostService postService = new PostService();

    private final CommentaireService commentaireService = new CommentaireService();


    @FXML
    public void initialize() {
        // Page Blog.fxml
        if (postsContainer != null) {
            if (sortBox != null) {
                sortBox.getItems().addAll("Date (plus récent)", "Date (plus ancien)", "Titre (A-Z)", "Titre (Z-A)", "Catégorie");
                sortBox.setValue("Date (plus récent)");
                sortBox.valueProperty().addListener((obs, oldVal, newVal) -> loadPosts());
            }

            if (searchField != null) {
                searchField.textProperty().addListener((obs, oldVal, newVal) -> loadPosts());
            }

            loadPosts();
            updateNotificationBadge();
            javafx.application.Platform.runLater(this::showAutoModerationNotificationIfNeeded);
        }

        // Page BlogStats.fxml
        if (totalPostsLabel != null) {
            loadBlogStatsPage();
        }
    }

    private void loadPosts() {
        allPosts = new PostService().getApprovedPosts();
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

        Label summary = new Label(getShortContent(post.getContenu(), 140));
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

        boolean isOwner = isPostOwner(post);

        editBtn.setVisible(isOwner);
        editBtn.setManaged(isOwner);

        deleteBtn.setVisible(isOwner);
        deleteBtn.setManaged(isOwner);

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

        VBox commentsSection = createCommentsSection(post, commentLabel);
        commentsSection.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        card.getChildren().addAll(topSection, separator, footer, commentsSection);

        card.setOnMouseClicked(e -> openPostDetail(post, e));

        editBtn.setOnAction(e -> {
            e.consume();

            if (!isPostOwner(post)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Accès refusé");
                alert.setHeaderText(null);
                alert.setContentText("Vous ne pouvez modifier que vos propres posts.");
                alert.showAndWait();
                return;
            }

            openEditPost(post);
        });

        deleteBtn.setOnAction(e -> {
            e.consume();

            if (!isPostOwner(post)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Accès refusé");
                alert.setHeaderText(null);
                alert.setContentText("Vous ne pouvez supprimer que vos propres posts.");
                alert.showAndWait();
                return;
            }

            deletePost(post);
        });

        editBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        deleteBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        return card;
    }

    private VBox createCommentsSection(Post post, Label commentLabel) {
        VBox commentsWrapper = new VBox(10);
        commentsWrapper.getStyleClass().add("comments-wrapper");
        commentsWrapper.setFillWidth(true);

        Label commentsTitle = new Label("Commentaires");
        commentsTitle.getStyleClass().add("comments-title");

        VBox commentsListBox = new VBox(10);
        commentsListBox.getStyleClass().add("comments-list");

        refreshCommentsList(post, commentsListBox, commentLabel);

        HBox addCommentBox = new HBox(10);
        addCommentBox.setAlignment(Pos.CENTER_LEFT);

        TextArea commentInput = new TextArea();
        commentInput.setPromptText("Écrire un commentaire...");
        commentInput.setWrapText(true);
        commentInput.setPrefRowCount(2);
        commentInput.getStyleClass().add("comment-input");
        HBox.setHgrow(commentInput, javafx.scene.layout.Priority.ALWAYS);

        Button sendBtn = new Button("Publier");
        sendBtn.getStyleClass().add("comment-send-btn");

        sendBtn.setOnAction(e -> {
            e.consume();

            String contenu = commentInput.getText() == null ? "" : commentInput.getText().trim();
            if (contenu.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Commentaire");
                alert.setHeaderText(null);
                alert.setContentText("Le commentaire ne peut pas être vide.");
                alert.showAndWait();
                return;
            }

            try {
                User currentUser = SessionManager.getCurrentUser();

                if (currentUser == null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Session expirée");
                    alert.setHeaderText(null);
                    alert.setContentText("Veuillez vous reconnecter pour ajouter un commentaire.");
                    alert.showAndWait();
                    return;
                }

                Commentaire commentaire = new Commentaire();

                Post p = new Post();
                p.setId(post.getId());
                commentaire.setPost(p);

                commentaire.setUser(currentUser);
                commentaire.setContenu(contenu);
                commentaire.setDate_creation(LocalDateTime.now());
                commentaire.setEst_anonyme(false);
                commentaire.setParametres_confidentialite("public");
                commentaire.setStatus("visible");
                commentaire.setModeration_score(null);
                commentaire.setModeration_label(null);
                commentaire.setModerated_at(null);

                commentaireService.ajouter(commentaire);
                commentInput.clear();

                refreshCommentsList(post, commentsListBox, commentLabel);

            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Ajout impossible");
                alert.setContentText("Le commentaire n'a pas pu être ajouté.");
                alert.showAndWait();
            }
        });

        addCommentBox.getChildren().addAll(commentInput, sendBtn);
        commentsWrapper.getChildren().addAll(commentsTitle, commentsListBox, addCommentBox);

        return commentsWrapper;
    }

    private void refreshCommentsList(Post post, VBox commentsListBox, Label commentLabel) {
        commentsListBox.getChildren().clear();

        List<Commentaire> commentaires = commentaireService.recupererParPost(post.getId());

        if (commentLabel != null) {
            commentLabel.setText("💬 " + commentaires.size() + " commentaire(s)");
        }

        if (commentaires.isEmpty()) {
            Label empty = new Label("Aucun commentaire pour le moment.");
            empty.getStyleClass().add("comment-empty-label");
            commentsListBox.getChildren().add(empty);
            return;
        }

        for (Commentaire commentaire : commentaires) {
            commentsListBox.getChildren().add(createSingleCommentNode(post, commentaire, commentsListBox, commentLabel));
        }
    }

    private Node createSingleCommentNode(Post post, Commentaire commentaire, VBox commentsListBox, Label commentLabel) {
        VBox commentCard = new VBox(6);
        commentCard.getStyleClass().add("comment-card");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label author = new Label(getCommentAuthorName(commentaire));
        author.getStyleClass().add("comment-author");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button editBtn = new Button("Modifier");
        editBtn.getStyleClass().add("comment-action-btn");

        Button deleteBtn = new Button("Supprimer");
        deleteBtn.getStyleClass().add("comment-delete-btn");

        boolean isOwner = isCommentOwner(commentaire);

        editBtn.setVisible(isOwner);
        editBtn.setManaged(isOwner);

        deleteBtn.setVisible(isOwner);
        deleteBtn.setManaged(isOwner);

        header.getChildren().addAll(author, spacer, editBtn, deleteBtn);

        Label content = new Label(safeText(commentaire.getContenu()));
        content.setWrapText(true);
        content.getStyleClass().add("comment-content");

        Label date = new Label(commentaire.getDate_creation() != null ? commentaire.getDate_creation().toString() : "");
        date.getStyleClass().add("comment-date");

        commentCard.getChildren().addAll(header, content, date);

        editBtn.setOnAction(e -> {
            e.consume();

            if (!isCommentOwner(commentaire)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Accès refusé");
                alert.setHeaderText(null);
                alert.setContentText("Vous ne pouvez modifier que vos propres commentaires.");
                alert.showAndWait();
                return;
            }

            showEditCommentDialog(commentaire, post, commentsListBox, commentLabel);
        });

        deleteBtn.setOnAction(e -> {
            e.consume();

            if (!isCommentOwner(commentaire)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Accès refusé");
                alert.setHeaderText(null);
                alert.setContentText("Vous ne pouvez supprimer que vos propres commentaires.");
                alert.showAndWait();
                return;
            }

            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Suppression");
            confirmation.setHeaderText("Supprimer ce commentaire");
            confirmation.setContentText("Voulez-vous vraiment supprimer ce commentaire ?");

            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                commentaireService.supprimer(commentaire);
                refreshCommentsList(post, commentsListBox, commentLabel);
            }
        });

        return commentCard;
    }

    private void showEditCommentDialog(Commentaire commentaire, Post post, VBox commentsListBox, Label commentLabel) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Modifier le commentaire");
        dialog.setHeaderText("Modifier votre commentaire");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefRowCount(4);
        textArea.setText(commentaire.getContenu());

        VBox box = new VBox(10, textArea);
        box.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return textArea.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newText -> {
            String value = newText == null ? "" : newText.trim();
            if (!value.isEmpty()) {
                commentaire.setContenu(value);
                commentaireService.modifier(commentaire);
                refreshCommentsList(post, commentsListBox, commentLabel);
            }
        });
    }

    private String getCommentAuthorName(Commentaire commentaire) {
        if (commentaire.isEst_anonyme()) {
            return "Anonyme";
        }

        try {
            if (commentaire.getUser() != null) {
                String nom = commentaire.getUser().getNom() != null ? commentaire.getUser().getNom().trim() : "";
                String prenom = commentaire.getUser().getPrenom() != null ? commentaire.getUser().getPrenom().trim() : "";

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
            }
        } catch (Exception ignored) {
        }

        return "Utilisateur";
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

        String auteur = getPostAuthorName(post);

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

    @FXML
    private void handleExportPdf() {
        try {
            List<Post> posts = postService.recuperer();

            if (posts == null || posts.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Export PDF");
                alert.setHeaderText("Aucun post");
                alert.setContentText("Il n'y a aucun post à exporter.");
                alert.showAndWait();
                return;
            }

            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, new FileOutputStream("posts_medflow.pdf"));
            document.open();

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, new BaseColor(41, 128, 185));
            Font subTitleFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
            Font postTitleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, new BaseColor(33, 37, 41));
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(52, 73, 94));
            Font textFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
            Font metaFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY);
            Font badgeFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

            Paragraph title = new Paragraph("Liste des posts MedFlow", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6f);
            document.add(title);

            Paragraph subtitle = new Paragraph("Export généré automatiquement", subTitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20f);
            document.add(subtitle);

            for (Post p : posts) {
                PdfPTable card = new PdfPTable(1);
                card.setWidthPercentage(100);
                card.setSpacingAfter(12f);

                PdfPCell cardCell = new PdfPCell();
                cardCell.setPadding(14f);
                cardCell.setBorderColor(new BaseColor(220, 220, 220));
                cardCell.setBorderWidth(1f);
                cardCell.setBackgroundColor(new BaseColor(250, 250, 250));

                PdfPTable badgeTable = new PdfPTable(1);
                badgeTable.setWidthPercentage(28);

                PdfPCell badgeCell = new PdfPCell(new Phrase(
                        p.getCategorie() != null ? p.getCategorie() : "Sans catégorie",
                        badgeFont
                ));
                badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                badgeCell.setPadding(6f);
                badgeCell.setBorder(Rectangle.NO_BORDER);
                badgeCell.setBackgroundColor(new BaseColor(52, 152, 219));
                badgeTable.addCell(badgeCell);

                cardCell.addElement(badgeTable);
                cardCell.addElement(new Paragraph(" "));

                Paragraph postTitle = new Paragraph(
                        p.getTitre() != null ? p.getTitre() : "Sans titre",
                        postTitleFont
                );
                postTitle.setSpacingAfter(8f);
                cardCell.addElement(postTitle);

                if (p.getImg_post() != null && !p.getImg_post().trim().isEmpty()) {
                    try {
                        com.itextpdf.text.Image postImage;

                        if (p.getImg_post().startsWith("http://") || p.getImg_post().startsWith("https://")) {
                            postImage = com.itextpdf.text.Image.getInstance(new URL(p.getImg_post()));
                        } else {
                            postImage = com.itextpdf.text.Image.getInstance(p.getImg_post());
                        }

                        postImage.scaleToFit(460, 220);
                        postImage.setAlignment(Element.ALIGN_CENTER);
                        cardCell.addElement(postImage);
                        cardCell.addElement(new Paragraph(" "));
                    } catch (Exception imgEx) {
                        Paragraph noImg = new Paragraph("Image non disponible", metaFont);
                        noImg.setSpacingAfter(8f);
                        cardCell.addElement(noImg);
                    }
                }

                cardCell.addElement(new Paragraph("Contenu", sectionFont));

                Paragraph content = new Paragraph(
                        p.getContenu() != null ? p.getContenu() : "",
                        textFont
                );
                content.setSpacingAfter(10f);
                cardCell.addElement(content);

                String localisation = p.getLocalisation() != null ? p.getLocalisation() : "-";
                String hashtags = p.getHashtags() != null ? p.getHashtags() : "-";
                String humeur = p.getHumeur() != null ? p.getHumeur() : "-";

                Paragraph meta1 = new Paragraph("Localisation : " + localisation, textFont);
                Paragraph meta2 = new Paragraph("Hashtags : " + hashtags, textFont);
                Paragraph meta3 = new Paragraph("Humeur : " + humeur, textFont);

                meta1.setSpacingAfter(4f);
                meta2.setSpacingAfter(4f);
                meta3.setSpacingAfter(6f);

                cardCell.addElement(meta1);
                cardCell.addElement(meta2);
                cardCell.addElement(meta3);

                if (p.getDate_creation() != null) {
                    Paragraph date = new Paragraph("Créé le : " + p.getDate_creation(), metaFont);
                    cardCell.addElement(date);
                }

                card.addCell(cardCell);
                document.add(card);
            }

            document.close();

            File file = new File("posts_medflow.pdf");
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            }

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur PDF");
            alert.setHeaderText("Export impossible");
            alert.setContentText("Une erreur est survenue lors de la génération du PDF.");
            alert.showAndWait();
        }
    }

    private boolean isCommentOwner(Commentaire commentaire) {
        try {
            User currentUser = SessionManager.getCurrentUser();

            return currentUser != null
                    && commentaire != null
                    && commentaire.getUser() != null
                    && commentaire.getUser().getId() == currentUser.getId();
        } catch (Exception e) {
            return false;
        }
    }

    private void loadBlogStatsPage() {
        try {
            List<Post> posts = postService.recuperer();
            List<Commentaire> commentaires = commentaireService.recuperer();

            int totalPosts = posts.size();
            int totalCommentaires = commentaires.size();
            int postsAnonymes = 0;

            java.util.Set<String> categoriesSet = new java.util.HashSet<>();
            Map<String, Integer> categoryCounts = new LinkedHashMap<>();
            Map<String, Integer> dateCounts = new LinkedHashMap<>();

            for (Post p : posts) {
                if (p.isEst_anonyme()) {
                    postsAnonymes++;
                }

                String categorie = (p.getCategorie() == null || p.getCategorie().isBlank())
                        ? "Sans catégorie"
                        : p.getCategorie().trim();

                categoriesSet.add(categorie.toLowerCase());
                categoryCounts.put(categorie, categoryCounts.getOrDefault(categorie, 0) + 1);

                String dateKey = "Sans date";
                try {
                    if (p.getDate_creation() != null) {
                        dateKey = p.getDate_creation().toLocalDate().toString();
                    }
                } catch (Exception ignored) {
                }

                dateCounts.put(dateKey, dateCounts.getOrDefault(dateKey, 0) + 1);
            }

            if (totalPostsLabel != null) totalPostsLabel.setText(String.valueOf(totalPosts));
            if (totalCommentairesLabel != null) totalCommentairesLabel.setText(String.valueOf(totalCommentaires));
            if (postsAnonymesLabel != null) postsAnonymesLabel.setText(String.valueOf(postsAnonymes));
            if (categoriesLabel != null) categoriesLabel.setText(String.valueOf(categoriesSet.size()));

            if (categoryBarChart != null) {
                categoryBarChart.getData().clear();

                XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
                barSeries.setName("Posts");

                for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                    barSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                }

                categoryBarChart.getData().add(barSeries);
                categoryBarChart.setLegendVisible(false);
            }

            if (anonymousPieChart != null) {
                int nonAnonymes = totalPosts - postsAnonymes;

                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                        new PieChart.Data("Anonymes", postsAnonymes),
                        new PieChart.Data("Non anonymes", nonAnonymes)
                );

                anonymousPieChart.setData(pieData);
                anonymousPieChart.setLegendVisible(true);
                anonymousPieChart.setLabelsVisible(true);
            }

            if (postsLineChart != null) {
                postsLineChart.getData().clear();

                XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
                lineSeries.setName("Évolution");

                for (Map.Entry<String, Integer> entry : dateCounts.entrySet()) {
                    lineSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                }

                postsLineChart.getData().add(lineSeries);
                postsLineChart.setLegendVisible(false);
            }

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de charger les statistiques du blog.");
            alert.showAndWait();
        }
    }

    @FXML
    private void toggleSubmenu() {
        boolean show = !submenuVBox.isVisible();
        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);

        if (reclamationArrow != null) {
            reclamationArrow.setText(show ? "⌃" : "⌄");
        }
    }

    @FXML
    private void openReclamationStats() {
        navigateTo("/ReclamationStats.fxml", "MedFlow - Statistiques Réclamations");
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

            Stage stage;
            if (postsContainer != null) {
                stage = (Stage) postsContainer.getScene().getWindow();
            } else {
                stage = (Stage) totalPostsLabel.getScene().getWindow();
            }

            stage.setScene(new Scene(root, 1650, 960));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir la page demandée.");
            alert.showAndWait();
        }
    }

    @FXML
    private void showMyPendingPostsPopup() {
        try {
            User currentUser = SessionManager.getCurrentUser();

            if (currentUser == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Session expirée");
                alert.setHeaderText(null);
                alert.setContentText("Veuillez vous reconnecter pour voir vos posts en attente.");
                alert.showAndWait();
                return;
            }

            List<Post> pendingPosts = postService.getPendingPostsByUser(currentUser.getId());

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Mes posts en attente");
            dialog.setHeaderText(null);

            ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(closeButton);

            VBox root = new VBox(18);
            root.setPrefWidth(760);
            root.setPrefHeight(560);
            root.setStyle("-fx-background-color: #f5f8fc; -fx-padding: 0;");

            HBox header = new HBox(16);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setStyle(
                    "-fx-background-color: linear-gradient(to right, #0891b2, #3b82f6);" +
                            "-fx-padding: 24;" +
                            "-fx-background-radius: 14 14 0 0;"
            );

            StackPane iconBox = new StackPane();
            iconBox.setPrefSize(64, 64);
            iconBox.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.20);" +
                            "-fx-background-radius: 18;" +
                            "-fx-border-color: rgba(255,255,255,0.30);" +
                            "-fx-border-radius: 18;"
            );

            Label icon = new Label("⏳");
            icon.setStyle("-fx-font-size: 30px;");
            iconBox.getChildren().add(icon);

            VBox titleBox = new VBox(6);

            Label title = new Label("Mes posts en attente");
            title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

            Label subtitle = new Label(pendingPosts.size() + " post(s) non encore validé(s)");
            subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.90); -fx-font-size: 15px;");

            titleBox.getChildren().addAll(title, subtitle);
            header.getChildren().addAll(iconBox, titleBox);

            VBox listBox = new VBox(14);
            listBox.setPadding(new Insets(20));

            if (pendingPosts.isEmpty()) {
                VBox emptyBox = new VBox(10);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPrefHeight(320);
                emptyBox.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 16;" +
                                "-fx-border-color: #e5e7eb;" +
                                "-fx-border-radius: 16;" +
                                "-fx-padding: 30;"
                );

                Label emptyIcon = new Label("✅");
                emptyIcon.setStyle("-fx-font-size: 42px;");

                Label emptyTitle = new Label("Aucun post en attente");
                emptyTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #111827;");

                Label emptyText = new Label("Tous vos posts ont déjà été traités par l’administrateur.");
                emptyText.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");

                emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyText);
                listBox.getChildren().add(emptyBox);

            } else {
                for (Post post : pendingPosts) {
                    VBox card = new VBox(10);
                    card.setStyle(
                            "-fx-background-color: white;" +
                                    "-fx-padding: 18;" +
                                    "-fx-background-radius: 16;" +
                                    "-fx-border-color: #dbeafe;" +
                                    "-fx-border-radius: 16;" +
                                    "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 12, 0, 0, 4);"
                    );

                    HBox top = new HBox(10);
                    top.setAlignment(Pos.CENTER_LEFT);

                    Label postTitle = new Label(safeText(post.getTitre()));
                    postTitle.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #111827;");
                    postTitle.setWrapText(true);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                    Label badge = new Label("En attente");
                    badge.setStyle(
                            "-fx-background-color: #fef3c7;" +
                                    "-fx-text-fill: #d97706;" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-padding: 6 12;" +
                                    "-fx-background-radius: 999;"
                    );

                    top.getChildren().addAll(postTitle, spacer, badge);

                    Label category = new Label("Catégorie : " + safeText(post.getCategorie()));
                    category.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");

                    Label content = new Label(getShortContent(post.getContenu(), 220));
                    content.setWrapText(true);
                    content.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px;");

                    Label date = new Label(post.getDate_creation() != null ? "Créé le : " + post.getDate_creation() : "");
                    date.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

                    card.getChildren().addAll(top, category, content, date);
                    listBox.getChildren().add(card);
                }
            }

            ScrollPane scrollPane = new ScrollPane(listBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            scrollPane.setPrefHeight(450);

            root.getChildren().addAll(header, scrollPane);

            dialog.getDialogPane().setContent(root);
            dialog.getDialogPane().setStyle("-fx-background-color: #f5f8fc; -fx-padding: 0;");
            dialog.getDialogPane().getScene().getWindow().setOnCloseRequest(e -> dialog.close());

            Button close = (Button) dialog.getDialogPane().lookupButton(closeButton);
            close.setStyle(
                    "-fx-background-color: #0891b2;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 8 18;"
            );

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de charger vos posts en attente.");
            alert.showAndWait();
        }
    }
    @FXML
    private void openPendingPostsAdminPage() {
        navigateTo("/PendingPostsAdmin.fxml", "MedFlow - Validation des posts");
    }
    @FXML
    private void backToReponses() {
        navigateTo("/reponse.fxml", "MedFlow - Gestion des réclamations");
    }

    private String getPostAuthorName(Post post) {
        if (post == null || post.isEst_anonyme()) {
            return "Anonyme";
        }

        try {
            if (post.getUser() != null) {
                String prenom = post.getUser().getPrenom() != null ? post.getUser().getPrenom().trim() : "";
                String nom = post.getUser().getNom() != null ? post.getUser().getNom().trim() : "";

                String fullName = (prenom + " " + nom).trim();

                if (!fullName.isEmpty()) {
                    return fullName;
                }
            }
        } catch (Exception ignored) {
        }

        return "Utilisateur";
    }

    private boolean isPostOwner(Post post) {
        try {
            User currentUser = SessionManager.getCurrentUser();

            return currentUser != null
                    && post != null
                    && post.getUser() != null
                    && post.getUser().getId() == currentUser.getId();

        } catch (Exception e) {
            return false;
        }
    }

    private void updateNotificationBadge() {
        try {
            User currentUser = SessionManager.getCurrentUser();

            if (currentUser == null || notificationBadgeLabel == null) {
                return;
            }

            int count = postService.countUnseenModerationNotificationsByUser(currentUser.getId());

            notificationBadgeLabel.setText(String.valueOf(count));

            boolean hasNotifications = count > 0;
            notificationBadgeLabel.setVisible(hasNotifications);
            notificationBadgeLabel.setManaged(hasNotifications);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showModerationNotificationsPopup() {
        showModerationNotificationsPopup(false);
    }

    private void showModerationNotificationsPopup(boolean autoOpen) {
        try {
            User currentUser = SessionManager.getCurrentUser();

            if (currentUser == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Session expirée");
                alert.setHeaderText(null);
                alert.setContentText("Veuillez vous reconnecter pour voir vos notifications.");
                alert.showAndWait();
                return;
            }

            List<Post> notifications = postService.getUnseenModerationNotificationsByUser(currentUser.getId());

            if (notifications.isEmpty()) {
                if (!autoOpen) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Notifications");
                    alert.setHeaderText(null);
                    alert.setContentText("Vous n’avez aucune nouvelle notification.");
                    alert.showAndWait();
                }
                updateNotificationBadge();
                return;
            }

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Notifications MedFlow");
            dialog.setHeaderText(null);

            ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType markAsReadButton = new ButtonType("Marquer comme lu", ButtonBar.ButtonData.OK_DONE);

            dialog.getDialogPane().getButtonTypes().addAll(markAsReadButton, closeButton);

            VBox root = new VBox(18);
            root.setPrefWidth(780);
            root.setPrefHeight(560);
            root.setStyle("-fx-background-color: #f5f8fc; -fx-padding: 0;");

            HBox header = new HBox(16);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setStyle(
                    "-fx-background-color: linear-gradient(to right, #0891b2, #2563eb);" +
                            "-fx-padding: 24;" +
                            "-fx-background-radius: 14 14 0 0;"
            );

            StackPane iconBox = new StackPane();
            iconBox.setPrefSize(64, 64);
            iconBox.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.22);" +
                            "-fx-background-radius: 18;" +
                            "-fx-border-color: rgba(255,255,255,0.35);" +
                            "-fx-border-radius: 18;"
            );

            Label icon = new Label("🔔");
            icon.setStyle("-fx-font-size: 32px;");
            iconBox.getChildren().add(icon);

            VBox titleBox = new VBox(6);

            Label title = new Label("Notifications de modération");
            title.setStyle("-fx-text-fill: white; -fx-font-size: 27px; -fx-font-weight: bold;");

            Label subtitle = new Label(notifications.size() + " nouvelle(s) notification(s)");
            subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.90); -fx-font-size: 15px;");

            titleBox.getChildren().addAll(title, subtitle);
            header.getChildren().addAll(iconBox, titleBox);

            VBox listBox = new VBox(14);
            listBox.setPadding(new Insets(20));

            for (Post post : notifications) {
                boolean approved = "approved".equalsIgnoreCase(post.getModeration_status());

                VBox card = new VBox(10);
                card.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-padding: 18;" +
                                "-fx-background-radius: 16;" +
                                "-fx-border-color: " + (approved ? "#bbf7d0;" : "#fecaca;") +
                                "-fx-border-radius: 16;" +
                                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.08), 12, 0, 0, 4);"
                );

                HBox top = new HBox(10);
                top.setAlignment(Pos.CENTER_LEFT);

                Label statusIcon = new Label(approved ? "✅" : "❌");
                statusIcon.setStyle("-fx-font-size: 24px;");

                Label postTitle = new Label(safeText(post.getTitre()));
                postTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
                postTitle.setWrapText(true);

                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                Label badge = new Label(approved ? "Approuvé" : "Refusé");
                badge.setStyle(
                        "-fx-background-color: " + (approved ? "#dcfce7;" : "#fee2e2;") +
                                "-fx-text-fill: " + (approved ? "#15803d;" : "#b91c1c;") +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 6 12;" +
                                "-fx-background-radius: 999;"
                );

                top.getChildren().addAll(statusIcon, postTitle, spacer, badge);

                Label message = new Label(safeText(post.getModeration_message()));
                message.setWrapText(true);
                message.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px;");

                Label date = new Label(post.getDate_modification() != null
                        ? "Traité le : " + post.getDate_modification()
                        : "Notification récente");
                date.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

                card.getChildren().addAll(top, message, date);
                listBox.getChildren().add(card);
            }

            ScrollPane scrollPane = new ScrollPane(listBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            scrollPane.setPrefHeight(450);

            root.getChildren().addAll(header, scrollPane);

            dialog.getDialogPane().setContent(root);
            dialog.getDialogPane().setStyle("-fx-background-color: #f5f8fc; -fx-padding: 0;");

            Button markBtn = (Button) dialog.getDialogPane().lookupButton(markAsReadButton);
            markBtn.setStyle(
                    "-fx-background-color: #0891b2;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 8 18;"
            );

            Button closeBtn = (Button) dialog.getDialogPane().lookupButton(closeButton);
            closeBtn.setStyle(
                    "-fx-background-color: #e5e7eb;" +
                            "-fx-text-fill: #111827;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 10;" +
                            "-fx-padding: 8 18;"
            );

            dialog.setResultConverter(button -> {
                if (button == markAsReadButton) {
                    postService.markAllModerationNotificationsAsSeen(currentUser.getId());
                    updateNotificationBadge();
                }
                return null;
            });

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de charger vos notifications.");
            alert.showAndWait();
        }
    }

    private void showAutoModerationNotificationIfNeeded() {
        try {
            User currentUser = SessionManager.getCurrentUser();

            if (currentUser == null) {
                return;
            }

            int count = postService.countUnseenModerationNotificationsByUser(currentUser.getId());

            if (count > 0) {
                showSystemTrayNotification(
                        "MedFlow Blog",
                        "Vous avez " + count + " nouvelle(s) notification(s) de modération."
                );

                showModerationNotificationsPopup(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSystemTrayNotification(String title, String message) {
        try {
            if (!java.awt.SystemTray.isSupported()) {
                return;
            }

            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();

            java.awt.Image image = java.awt.Toolkit.getDefaultToolkit().createImage("");

            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(image, "MedFlow");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("MedFlow Notification");

            tray.add(trayIcon);
            trayIcon.displayMessage(title, message, java.awt.TrayIcon.MessageType.INFO);

            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    tray.remove(trayIcon);
                } catch (Exception ignored) {
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
