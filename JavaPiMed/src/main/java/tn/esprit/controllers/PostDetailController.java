package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Post;
import tn.esprit.services.PostService;

import java.net.URL;
import java.util.Optional;

public class PostDetailController {

    @FXML private ImageView postImage;
    @FXML private Label postCategory;
    @FXML private Label postTitle;
    @FXML private Label postSummary;
    @FXML private Label postDate;
    @FXML private Label postLocation;
    @FXML private Label postAuthor;

    @FXML private Label postFullContent;
    @FXML private Label reactionsLabel;
    @FXML private Label commentsLabel;
    @FXML private Label visibilityLabel;
    @FXML private Label moodLabel;
    @FXML private Label anonymousLabel;
    @FXML private VBox hashtagsBox;

    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private Post post;
    private final PostService postService = new PostService();

    public void setPost(Post post) {
        this.post = post;
        updateUI();
    }

    private void updateUI() {
        if (post == null) {
            return;
        }

        postTitle.setText(safeText(post.getTitre()));
        postSummary.setText(safeText(post.getContenu()));

        String categorie = safeText(post.getCategorie());
        postCategory.setText(categorie.isBlank() ? "Sans catégorie" : categorie);

        postDate.setText(post.getDate_creation() != null ? post.getDate_creation().toString() : "Date inconnue");
        postLocation.setText(safeText(post.getLocalisation()).isBlank() ? "Lieu non précisé" : post.getLocalisation());

        String auteur = "MedFlow Admin";
        if (post.getUser() != null && post.getUser().getNom() != null && !post.getUser().getNom().trim().isEmpty()) {
            auteur = post.getUser().getNom().trim();
        }
        if (post.isEst_anonyme()) {
            auteur = "Anonyme";
        }
        postAuthor.setText(auteur);

        postFullContent.setText(safeText(post.getContenu()).isBlank()
                ? "Aucun contenu disponible."
                : post.getContenu());

        reactionsLabel.setText(post.getNbr_reactions() + " réaction(s)");
        commentsLabel.setText(post.getNbr_commentaires() + " commentaire(s)");

        visibilityLabel.setText("Visibilité : " +
                (safeText(post.getVisibilite()).isBlank() ? "Non définie" : post.getVisibilite()));

        moodLabel.setText("Humeur : " +
                (safeText(post.getHumeur()).isBlank() ? "Non définie" : post.getHumeur()));

        anonymousLabel.setText("Publication : " +
                (post.isEst_anonyme() ? "Anonyme" : "Non anonyme"));

        loadPostImage();
        loadHashtags();
    }

    private void loadPostImage() {
        try {
            String imagePath = safeText(post.getImg_post());

            if (!imagePath.isBlank()) {
                if (imagePath.startsWith("http://")
                        || imagePath.startsWith("https://")
                        || imagePath.startsWith("file:")) {
                    postImage.setImage(new Image(imagePath, true));
                    return;
                }

                URL directResource = getClass().getResource(imagePath);
                if (directResource != null) {
                    postImage.setImage(new Image(directResource.toExternalForm(), true));
                    return;
                }

                URL uploadsResource = getClass().getResource("/uploads/" + imagePath);
                if (uploadsResource != null) {
                    postImage.setImage(new Image(uploadsResource.toExternalForm(), true));
                    return;
                }

                URL imagesResource = getClass().getResource("/images/" + imagePath);
                if (imagesResource != null) {
                    postImage.setImage(new Image(imagesResource.toExternalForm(), true));
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            URL defaultImg = getClass().getResource("/images/logo.png");
            if (defaultImg != null) {
                postImage.setImage(new Image(defaultImg.toExternalForm(), true));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadHashtags() {
        hashtagsBox.getChildren().clear();

        String hashtags = safeText(post.getHashtags());
        if (hashtags.isBlank()) {
            Label emptyTag = new Label("#aucun");
            emptyTag.getStyleClass().add("hashtag-chip");
            hashtagsBox.getChildren().add(emptyTag);
            return;
        }

        String[] tags = hashtags.split("[,\\s]+");
        for (String tagText : tags) {
            if (!tagText.isBlank()) {
                Label tag = new Label(tagText.startsWith("#") ? tagText : "#" + tagText);
                tag.getStyleClass().add("hashtag-chip");
                hashtagsBox.getChildren().add(tag);
            }
        }
    }

    @FXML
    private void handleEditPost() {
        if (post == null) {
            showError("Aucun post à modifier.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/AddPost.fxml"));
            Parent root = loader.load();

            AddPostController controller = loader.getController();
            controller.setPostToEdit(post);

            Stage stage = (Stage) postTitle.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle("Modifier le Post");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible d'ouvrir la page de modification.");
        }
    }

    @FXML
    private void handleDeletePost() {
        if (post == null) {
            showError("Aucun post à supprimer.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer ce post");
        confirmation.setContentText("Voulez-vous vraiment supprimer ce post ?");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                postService.supprimer(post);

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Succès");
                success.setHeaderText(null);
                success.setContentText("Post supprimé avec succès.");
                success.showAndWait();

                goBackToBlog();

            } catch (Exception e) {
                e.printStackTrace();
                showError("Impossible de supprimer ce post.");
            }
        }
    }

    @FXML
    private void goBackToBlog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/Blog.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) postTitle.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle("Blog");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Impossible de retourner à la page Blog.");
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}