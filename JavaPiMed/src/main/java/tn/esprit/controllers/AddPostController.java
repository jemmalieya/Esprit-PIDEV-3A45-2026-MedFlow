package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Post;
import tn.esprit.services.PostService;

import java.io.FileOutputStream;
import java.util.List;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.time.LocalDateTime;

public class AddPostController {

    @FXML private TextField titreField;
    @FXML private ComboBox<String> categorieBox;
    @FXML private TextField localisationField;
    @FXML private ComboBox<String> humeurBox;
    @FXML private TextField imageField;
    @FXML private TextField hashtagsField;
    @FXML private TextArea contenuArea;
    @FXML private CheckBox anonymeCheck;

    @FXML private Label titreError;
    @FXML private Label contenuError;
    @FXML private Label hashtagsError;
    @FXML private Label imageError;
    @FXML private Label localisationError;

    @FXML private ImageView previewImage;
    @FXML private Label previewCategory;
    @FXML private Label previewTitle;
    @FXML private Label previewContent;
    @FXML private Label previewMeta;
    @FXML private FlowPane previewTagsPane;
    private final PostService postService = new PostService();

    private Post postToEdit = null;
    private boolean editMode = false;

    @FXML
    private Button publishButton;
    @FXML private Label formTitle;




    @FXML
    public void initialize() {
        categorieBox.getItems().addAll("Actualité", "Conseils", "Service", "Santé", "Urgence");
        humeurBox.getItems().addAll("Heureux", "Motivé", "Calme", "Neutre");

        if (categorieBox.getValue() == null) {
            categorieBox.setValue("Conseils");
        }

        if (humeurBox.getValue() == null) {
            humeurBox.setValue("Heureux");
        }

        if (localisationField.getText() == null || localisationField.getText().isBlank()) {
            localisationField.setText("Tunis");
        }

        addPreviewListeners();
        addValidationListeners();
        refreshPreview();
    }
    private boolean validateForm() {
        boolean isValid = true;

        // reset erreurs
        clearError(titreField, titreError);
        clearError(contenuArea, contenuError);
        clearError(hashtagsField, hashtagsError);
        clearError(imageField, imageError);
        clearError(localisationField, localisationError);

        // Titre
        if (isBlank(titreField.getText())) {
            setError(titreField, titreError, "Titre obligatoire");
            isValid = false;
        } else if (titreField.getText().length() < 5) {
            setError(titreField, titreError, "Min 5 caractères");
            isValid = false;
        }

        // Contenu
        if (isBlank(contenuArea.getText())) {
            setError(contenuArea, contenuError, "Contenu obligatoire");
            isValid = false;
        } else if (contenuArea.getText().length() < 10) {
            setError(contenuArea, contenuError, "Min 10 caractères");
            isValid = false;
        }

        // Hashtags obligatoires
        if (isBlank(hashtagsField.getText())) {
            setError(hashtagsField, hashtagsError, "Hashtags obligatoires");
            isValid = false;
        } else {
            String regex = "^(#\\w+)([\\s,]+#\\w+)*$";

            if (!hashtagsField.getText().matches(regex)) {
                setError(hashtagsField, hashtagsError, "Format: #mot #mot");
                isValid = false;
            }
        }

        // Image URL
        // Image obligatoire
        if (isBlank(imageField.getText())) {
            setError(imageField, imageError, "Image obligatoire");
            isValid = false;
        } else {
            if (!imageField.getText().startsWith("http")) {
                setError(imageField, imageError, "URL invalide (http/https)");
                isValid = false;
            }
        }

        // Localisation
        if (!isBlank(localisationField.getText())) {
            if (localisationField.getText().length() < 2) {
                setError(localisationField, localisationError, "Localisation invalide");
                isValid = false;
            }
        }

        return isValid;
    }

    private void setError(Control field, Label errorLabel, String message) {
        if (!field.getStyleClass().contains("error-field")) {
            field.getStyleClass().add("error-field");
        }
        errorLabel.setText(message);
    }

    private void clearError(Control field, Label errorLabel) {
        field.getStyleClass().remove("error-field");
        errorLabel.setText("");
    }

    public void setPostToEdit(Post post) {
        this.postToEdit = post;
        this.editMode = true;

        titreField.setText(post.getTitre());
        contenuArea.setText(post.getContenu());
        localisationField.setText(post.getLocalisation());
        imageField.setText(post.getImg_post());
        hashtagsField.setText(post.getHashtags());

        if (post.getCategorie() != null) {
            categorieBox.setValue(post.getCategorie());
        }

        if (post.getHumeur() != null) {
            humeurBox.setValue(post.getHumeur());
        }

        anonymeCheck.setSelected(post.isEst_anonyme());

        if (publishButton != null) {
            publishButton.setText("Modifier");
        }

        if (formTitle != null) {
            formTitle.setText("Modifier le Post");
        }

        refreshPreview();
    }
    private void addPreviewListeners() {
        titreField.textProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        contenuArea.textProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        localisationField.textProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        imageField.textProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        hashtagsField.textProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        categorieBox.valueProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        humeurBox.valueProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        anonymeCheck.selectedProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
    }

    private void refreshPreview() {
        previewCategory.setText(isBlank(categorieBox.getValue()) ? "Catégorie" : categorieBox.getValue());
        previewTitle.setText(isBlank(titreField.getText()) ? "Titre du post" : titreField.getText());

        String contenu = contenuArea.getText();
        if (isBlank(contenu)) {
            previewContent.setText("Le contenu du post apparaîtra ici...");
        } else {
            previewContent.setText(contenu.length() > 140 ? contenu.substring(0, 140) + "..." : contenu);
        }

        String auteur = anonymeCheck.isSelected() ? "Anonyme" : "MedFlow Admin";
        String localisation = isBlank(localisationField.getText()) ? "Tunis" : localisationField.getText();
        previewMeta.setText("Aujourd’hui · " + localisation + " · " + auteur);

        updatePreviewImage();
        updatePreviewTags();
    }

    private void updatePreviewImage() {
        try {
            String url = imageField.getText();
            if (!isBlank(url)) {
                previewImage.setImage(new Image(url));
            } else {
                previewImage.setImage(null);
            }
        } catch (Exception e) {
            previewImage.setImage(null);
        }
    }

    private void updatePreviewTags() {
        previewTagsPane.getChildren().clear();

        String raw = hashtagsField.getText();
        if (isBlank(raw)) return;

        String[] tags = raw.split("[,\\s]+");
        for (String tagText : tags) {
            if (!tagText.isBlank()) {
                Label tag = new Label(tagText.startsWith("#") ? tagText : "#" + tagText);
                tag.getStyleClass().add("preview-tag");
                previewTagsPane.getChildren().add(tag);
            }
        }
    }
    private void addValidationListeners() {

        // Titre
        titreField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isBlank(newVal) || newVal.length() < 5) {
                titreField.setStyle("-fx-border-color: red;");
            } else {
                titreField.setStyle(null);
                titreError.setText("");
            }
        });

        // Contenu
        contenuArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isBlank(newVal) || newVal.length() < 10) {
                contenuArea.setStyle("-fx-border-color: red;");
            } else {
                contenuArea.setStyle(null);
                contenuError.setText("");
            }
        });

        // Hashtags
        hashtagsField.textProperty().addListener((obs, oldVal, newVal) -> {
            String regex = "^(#\\w+)([\\s,]+#\\w+)*$";

            if (!isBlank(newVal) && !newVal.matches(regex)) {
                hashtagsField.setStyle("-fx-border-color: red;");
            } else {
                hashtagsField.setStyle(null);
                hashtagsError.setText("");
            }
        });

        // Image
        imageField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isBlank(newVal) && !newVal.startsWith("http")) {
                imageField.setStyle("-fx-border-color: red;");
            } else {
                imageField.setStyle(null);
                imageError.setText("");
            }
        });

        // Localisation
        localisationField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isBlank(newVal) && newVal.length() < 2) {
                localisationField.setStyle("-fx-border-color: red;");
            } else {
                localisationField.setStyle(null);
                localisationError.setText("");
            }
        });

        // ComboBox Catégorie
        categorieBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isBlank(newVal)) {
                categorieBox.setStyle("-fx-border-color: red;");
            } else {
                categorieBox.setStyle(null);
            }
        });

        // ComboBox Humeur
        humeurBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isBlank(newVal)) {
                humeurBox.setStyle("-fx-border-color: red;");
            } else {
                humeurBox.setStyle(null);
            }
        });
    }
    @FXML
    private void handlePublish() {
        if (!validateForm()) {
            return;
        }

        try {
            if (editMode && postToEdit != null) {
                // MODE MODIFICATION
                postToEdit.setTitre(titreField.getText().trim());
                postToEdit.setContenu(contenuArea.getText().trim());
                postToEdit.setCategorie(categorieBox.getValue());
                postToEdit.setLocalisation(isBlank(localisationField.getText()) ? "Tunis" : localisationField.getText().trim());
                postToEdit.setImg_post(isBlank(imageField.getText()) ? "" : imageField.getText().trim());
                postToEdit.setHashtags(isBlank(hashtagsField.getText()) ? "" : hashtagsField.getText().trim());
                postToEdit.setEst_anonyme(anonymeCheck.isSelected());
                postToEdit.setHumeur(humeurBox.getValue());
                postToEdit.setDate_modification(java.time.LocalDateTime.now());

                if (postToEdit.getVisibilite() == null || postToEdit.getVisibilite().isBlank()) {
                    postToEdit.setVisibilite("Public");
                }

                boolean success = postService.modifier(postToEdit);

                if (!success) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Doublon détecté");
                    alert.setHeaderText("Modification bloquée");
                    alert.setContentText("Un autre post identique existe déjà. Impossible de modifier ce post.");
                    alert.showAndWait();
                    return;
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                alert.setHeaderText(null);
                alert.setContentText("Post modifié avec succès.");
                alert.showAndWait();

            } else {
                // MODE AJOUT
                Post post = new Post();

                post.setUser_id(13);
                post.setTitre(titreField.getText().trim());
                post.setContenu(contenuArea.getText().trim());
                post.setCategorie(categorieBox.getValue());
                post.setLocalisation(isBlank(localisationField.getText()) ? "Tunis" : localisationField.getText().trim());
                post.setImg_post(isBlank(imageField.getText()) ? "" : imageField.getText().trim());
                post.setHashtags(isBlank(hashtagsField.getText()) ? "" : hashtagsField.getText().trim());
                post.setVisibilite("Public");
                post.setDate_creation(java.time.LocalDateTime.now());
                post.setEst_anonyme(anonymeCheck.isSelected());
                post.setHumeur(humeurBox.getValue());

                post.setNbr_reactions(0);
                post.setNbr_commentaires(0);
                post.setIs_approved(true);
                post.setModeration_status("approved");
                post.setModeration_message("");
                post.setModeration_seen(true);
                boolean success = postService.ajouter(post);

                if (!success) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Doublon détecté");
                    alert.setHeaderText("Ajout bloqué");
                    alert.setContentText("Un post identique existe déjà. Impossible d'ajouter ce post.");
                    alert.showAndWait();
                    return;
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Succès");
                alert.setHeaderText(null);
                alert.setContentText("Post ajouté avec succès.");
                alert.showAndWait();
            }

            goBackToBlog();

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Opération impossible");
            alert.setContentText("Une erreur est survenue.");
            alert.showAndWait();
        }
    }

    @FXML
    private void goBackToBlog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/Blog.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) titreField.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle("Blog");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}