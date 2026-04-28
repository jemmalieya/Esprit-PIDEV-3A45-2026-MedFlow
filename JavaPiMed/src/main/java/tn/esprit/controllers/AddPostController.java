package tn.esprit.controllers;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import tn.esprit.entities.Post;
import tn.esprit.entities.PostAiSuggestion;
import tn.esprit.entities.User;
import tn.esprit.services.GeminiPostAssistantService;
import tn.esprit.services.PostService;
import tn.esprit.tools.SessionManager;
import tn.esprit.services.CloudinaryPostService;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.io.File;

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

    @FXML private Button publishButton;
    @FXML private Label formTitle;

    private final PostService postService = new PostService();
    private final GeminiPostAssistantService geminiPostAssistantService = new GeminiPostAssistantService();

    private Post postToEdit = null;
    private boolean editMode = false;

    private File selectedImageFile;
    private CloudinaryPostService cloudinaryPostService;

    private double selectedLatitude = 36.8065;
    private double selectedLongitude = 10.1815;
    private String selectedAddress = "Tunis, Tunisie";
    private String lastSearchAddressLabel = null;

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

        clearError(titreField, titreError);
        clearError(contenuArea, contenuError);
        clearError(hashtagsField, hashtagsError);
        clearError(imageField, imageError);
        clearError(localisationField, localisationError);

        if (isBlank(titreField.getText())) {
            setError(titreField, titreError, "Titre obligatoire");
            isValid = false;
        } else if (titreField.getText().length() < 5) {
            setError(titreField, titreError, "Min 5 caractères");
            isValid = false;
        }

        if (isBlank(contenuArea.getText())) {
            setError(contenuArea, contenuError, "Contenu obligatoire");
            isValid = false;
        } else if (contenuArea.getText().length() < 10) {
            setError(contenuArea, contenuError, "Min 10 caractères");
            isValid = false;
        }

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

        if (isBlank(imageField.getText())) {
            setError(imageField, imageError, "Image obligatoire");
            isValid = false;
        } else {
            String img = imageField.getText().trim();

            boolean isUrl = img.startsWith("http://") || img.startsWith("https://");
            boolean isSelectedLocalFile = selectedImageFile != null && selectedImageFile.exists();
            boolean isLocalPath = new File(img).exists();

            if (!isUrl && !isSelectedLocalFile && !isLocalPath) {
                setError(imageField, imageError, "Choisissez une image ou utilisez une URL valide");
                isValid = false;
            }
        }

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
            String value = imageField.getText();

            if (isBlank(value)) {
                previewImage.setImage(null);
                return;
            }

            value = value.trim();

            if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("file:")) {
                previewImage.setImage(new Image(value, true));
                return;
            }

            File localFile = new File(value);

            if (localFile.exists()) {
                previewImage.setImage(new Image(localFile.toURI().toString(), true));
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
        titreField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isBlank(newVal) || newVal.length() < 5) {
                titreField.setStyle("-fx-border-color: red;");
            } else {
                titreField.setStyle(null);
                titreError.setText("");
            }
        });

        contenuArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isBlank(newVal) || newVal.length() < 10) {
                contenuArea.setStyle("-fx-border-color: red;");
            } else {
                contenuArea.setStyle(null);
                contenuError.setText("");
            }
        });

        hashtagsField.textProperty().addListener((obs, oldVal, newVal) -> {
            String regex = "^(#\\w+)([\\s,]+#\\w+)*$";

            if (!isBlank(newVal) && !newVal.matches(regex)) {
                hashtagsField.setStyle("-fx-border-color: red;");
            } else {
                hashtagsField.setStyle(null);
                hashtagsError.setText("");
            }
        });

        imageField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isBlank(newVal)) {
                imageField.setStyle(null);
                imageError.setText("");
                return;
            }

            String img = newVal.trim();

            boolean isUrl = img.startsWith("http://") || img.startsWith("https://");
            boolean isLocalPath = new File(img).exists();

            if (!isUrl && !isLocalPath) {
                imageField.setStyle("-fx-border-color: red;");
            } else {
                imageField.setStyle(null);
                imageError.setText("");
            }
        });

        localisationField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isBlank(newVal) && newVal.length() < 2) {
                localisationField.setStyle("-fx-border-color: red;");
            } else {
                localisationField.setStyle(null);
                localisationError.setText("");
            }
        });

        categorieBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isBlank(newVal)) {
                categorieBox.setStyle("-fx-border-color: red;");
            } else {
                categorieBox.setStyle(null);
            }
        });

        humeurBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isBlank(newVal)) {
                humeurBox.setStyle("-fx-border-color: red;");
            } else {
                humeurBox.setStyle(null);
            }
        });
    }
    private String uploadImageUrlVersCloudinary() throws Exception {
        String imageUrl = imageField.getText() == null ? "" : imageField.getText().trim();

        if (imageUrl.isBlank()) {
            throw new IllegalStateException("Image obligatoire.");
        }

        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            throw new IllegalStateException("L'image doit être une URL web valide.");
        }

        imageError.setStyle("-fx-text-fill: #2563eb;");
        imageError.setText("Upload vers Cloudinary en cours...");

        String cloudinaryUrl = getCloudinaryPostService().uploadPostImageFromUrl(imageUrl);

        imageField.setText(cloudinaryUrl);
        imageError.setStyle("-fx-text-fill: green;");
        imageError.setText("Image envoyée vers Cloudinary avec succès.");

        previewImage.setImage(new Image(cloudinaryUrl, true));

        return cloudinaryUrl;
    }



    @FXML
    private void handlePublish() {
        if (!validateForm()) {
            return;
        }

        try {
            String imageCloudinaryUrl = uploadImageUrlVersCloudinary();
            if (editMode && postToEdit != null) {
                postToEdit.setTitre(titreField.getText().trim());
                postToEdit.setContenu(contenuArea.getText().trim());
                postToEdit.setCategorie(categorieBox.getValue());
                postToEdit.setLocalisation(isBlank(localisationField.getText()) ? "Tunis" : localisationField.getText().trim());
                postToEdit.setImg_post(imageCloudinaryUrl);
                postToEdit.setHashtags(isBlank(hashtagsField.getText()) ? "" : hashtagsField.getText().trim());
                postToEdit.setEst_anonyme(anonymeCheck.isSelected());
                postToEdit.setHumeur(humeurBox.getValue());
                postToEdit.setDate_modification(java.time.LocalDateTime.now());

                if (postToEdit.getVisibilite() == null || postToEdit.getVisibilite().isBlank()) {
                    postToEdit.setVisibilite("Public");
                }

                postToEdit.setIs_approved(false);
                postToEdit.setModeration_status("pending");
                postToEdit.setModeration_message("Modification en attente de validation par l'administrateur");
                postToEdit.setModeration_seen(false);

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
                alert.setTitle("Modification envoyée");
                alert.setHeaderText(null);
                alert.setContentText("Votre modification a été envoyée à l’administrateur pour validation.");
                alert.showAndWait();

            } else {
                Post post = new Post();

                User currentUser = SessionManager.getCurrentUser();

                if (currentUser == null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Session expirée");
                    alert.setHeaderText(null);
                    alert.setContentText("Veuillez vous reconnecter pour publier un post.");
                    alert.showAndWait();
                    return;
                }

                post.setUser(currentUser);
                post.setTitre(titreField.getText().trim());
                post.setContenu(contenuArea.getText().trim());
                post.setCategorie(categorieBox.getValue());
                post.setLocalisation(isBlank(localisationField.getText()) ? "Tunis" : localisationField.getText().trim());
                post.setImg_post(imageCloudinaryUrl);
                post.setHashtags(isBlank(hashtagsField.getText()) ? "" : hashtagsField.getText().trim());
                post.setVisibilite("Public");
                post.setDate_creation(java.time.LocalDateTime.now());
                post.setEst_anonyme(anonymeCheck.isSelected());
                post.setHumeur(humeurBox.getValue());
                post.setNbr_reactions(0);
                post.setNbr_commentaires(0);
                post.setIs_approved(false);
                post.setModeration_status("pending");
                post.setModeration_message("En attente de validation par l'administrateur");
                post.setModeration_seen(false);

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
                alert.setContentText("Votre post a été envoyé à l’administrateur pour validation.");
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

    @FXML
    private void handleImproveWithAi() {
        String contenu = contenuArea.getText() == null ? "" : contenuArea.getText().trim();

        if (contenu.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Assistant IA");
            alert.setHeaderText(null);
            alert.setContentText("Écrivez d’abord un contenu avant d’utiliser l’assistant IA.");
            alert.showAndWait();
            return;
        }

        if (contenu.length() < 20) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Assistant IA");
            alert.setHeaderText(null);
            alert.setContentText("Le contenu est trop court. Ajoutez quelques détails pour obtenir une meilleure suggestion.");
            alert.showAndWait();
            return;
        }

        Dialog<Void> loadingDialog = createLoadingDialog();
        loadingDialog.show();

        Task<PostAiSuggestion> task = new Task<>() {
            @Override
            protected PostAiSuggestion call() {
                return geminiPostAssistantService.improvePost(contenu);
            }
        };

        task.setOnSucceeded(event -> {
            PostAiSuggestion suggestion = task.getValue();

            Platform.runLater(() -> {
                closeDialogForce(loadingDialog);
                showAiSuggestionPopup(suggestion);
            });
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            ex.printStackTrace();

            Platform.runLater(() -> {
                closeDialogForce(loadingDialog);

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Assistant IA");
                alert.setHeaderText("Impossible de générer la suggestion");
                alert.setContentText(
                        "L’assistant IA n’a pas pu générer une suggestion correcte.\n" +
                                "Veuillez réessayer avec un contenu plus simple ou plus court.\n\n" +
                                "Détail : " + ex.getMessage()
                );
                alert.showAndWait();
            });
        });

        task.setOnCancelled(event -> Platform.runLater(() -> closeDialogForce(loadingDialog)));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private Dialog<Void> createLoadingDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Assistant IA");
        dialog.setHeaderText(null);

        VBox root = new VBox(14);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28));
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 18;");

        Label icon = new Label("✨");
        icon.setStyle("-fx-font-size: 42px;");

        Label title = new Label("Assistant IA MedFlow");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label("Analyse du contenu et génération des suggestions...");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");
        subtitle.setWrapText(true);

        ProgressIndicator loader = new ProgressIndicator();
        loader.setPrefSize(46, 46);

        root.getChildren().addAll(icon, title, subtitle, loader);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().clear();
        dialog.getDialogPane().setStyle("-fx-background-color: white;");

        dialog.setOnCloseRequest(e -> closeDialogForce(dialog));

        return dialog;
    }

    private void showAiSuggestionPopup(PostAiSuggestion suggestion) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Suggestion IA");
        dialog.setHeaderText(null);

        ButtonType applyAllButton = new ButtonType("Appliquer tout", ButtonBar.ButtonData.OK_DONE);
        ButtonType applyContentButton = new ButtonType("Appliquer contenu seulement", ButtonBar.ButtonData.APPLY);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(applyAllButton, applyContentButton, cancelButton);

        VBox root = new VBox(18);
        root.setPrefWidth(820);
        root.setPrefHeight(620);
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

        Label icon = new Label("✨");
        icon.setStyle("-fx-font-size: 32px;");
        iconBox.getChildren().add(icon);

        VBox titleBox = new VBox(6);

        Label title = new Label("Assistant IA MedFlow");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        Label subtitle = new Label("Suggestions pour rendre votre article plus clair, utile et responsable.");
        subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.90); -fx-font-size: 15px;");
        subtitle.setWrapText(true);

        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(iconBox, titleBox);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f5f8fc;");

        HBox topCards = new HBox(14);
        topCards.getChildren().addAll(
                createAiInfoCard("Titre recommandé", suggestion.getTitre(), true),
                createAiInfoCard("Catégorie proposée", suggestion.getCategorie(), false),
                createAiInfoCard("Ton", suggestion.getTon(), false)
        );

        VBox hashtagsCard = createAiInfoCard("Hashtags proposés", suggestion.getHashtags(), true);
        VBox resumeCard = createAiInfoCard("Résumé court", suggestion.getResume(), true);
        VBox contenuCard = createAiInfoCard("Contenu amélioré", suggestion.getContenuReformule(), true);
        VBox conseilCard = createAiInfoCard("Conseil de publication responsable", suggestion.getConseilResponsable(), true);

        content.getChildren().addAll(topCards, hashtagsCard, resumeCard, contenuCard, conseilCard);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setPrefHeight(500);

        root.getChildren().addAll(header, scrollPane);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f8fc; -fx-padding: 0;");

        Button applyAll = (Button) dialog.getDialogPane().lookupButton(applyAllButton);
        applyAll.setStyle("-fx-background-color: #0891b2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 18;");

        Button applyContent = (Button) dialog.getDialogPane().lookupButton(applyContentButton);
        applyContent.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 18;");

        Button cancel = (Button) dialog.getDialogPane().lookupButton(cancelButton);
        cancel.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #111827; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 18;");

        dialog.showAndWait().ifPresent(result -> {
            if (result == applyAllButton) {
                applyAiSuggestion(suggestion, true);
            } else if (result == applyContentButton) {
                applyAiSuggestion(suggestion, false);
            }
        });
    }

    private VBox createAiInfoCard(String title, String value, boolean wide) {
        VBox card = new VBox(8);

        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: #dbeafe;" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.08), 12, 0, 0, 4);"
        );

        if (!wide) {
            card.setPrefWidth(240);
        } else {
            card.setMaxWidth(Double.MAX_VALUE);
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: #2563eb;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;"
        );

        Label valueLabel = new Label(value == null || value.isBlank() ? "-" : value);
        valueLabel.setWrapText(true);
        valueLabel.setStyle(
                "-fx-text-fill: #111827;" +
                        "-fx-font-size: 14px;" +
                        "-fx-line-spacing: 3px;"
        );

        card.getChildren().addAll(titleLabel, valueLabel);

        return card;
    }

    private void applyAiSuggestion(PostAiSuggestion suggestion, boolean applyAll) {
        if (suggestion == null) {
            return;
        }

        if (applyAll) {
            if (suggestion.getTitre() != null && !suggestion.getTitre().isBlank()) {
                titreField.setText(suggestion.getTitre());
            }

            if (suggestion.getCategorie() != null && !suggestion.getCategorie().isBlank()) {
                categorieBox.setValue(normalizeCategory(suggestion.getCategorie()));
            }

            if (suggestion.getHashtags() != null && !suggestion.getHashtags().isBlank()) {
                hashtagsField.setText(suggestion.getHashtags());
            }
        }

        if (suggestion.getContenuReformule() != null && !suggestion.getContenuReformule().isBlank()) {
            contenuArea.setText(suggestion.getContenuReformule());
        }

        refreshPreview();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Assistant IA");
        alert.setHeaderText(null);
        alert.setContentText(applyAll
                ? "Toutes les suggestions IA ont été appliquées."
                : "Le contenu amélioré a été appliqué.");
        alert.showAndWait();
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return "Conseils";
        }

        String c = category.trim().toLowerCase();

        if (c.contains("actualité") || c.contains("actualite")) {
            return "Actualité";
        }

        if (c.contains("service")) {
            return "Service";
        }

        if (c.contains("santé") || c.contains("sante")) {
            return "Santé";
        }

        if (c.contains("urgence")) {
            return "Urgence";
        }

        return "Conseils";
    }

    private void closeDialogForce(Dialog<?> dialog) {
        try {
            if (dialog == null) {
                return;
            }

            dialog.close();
            dialog.hide();

            if (dialog.getDialogPane() != null
                    && dialog.getDialogPane().getScene() != null
                    && dialog.getDialogPane().getScene().getWindow() != null) {
                dialog.getDialogPane().getScene().getWindow().hide();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenMapPicker() {
        showNativeMapPopup(false);
    }

    @FXML
    private void handleUseCurrentLocation() {
        showNativeMapPopup(true);
    }

    private void showNativeMapPopup(boolean useCurrentLocation) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Choisir une localisation");
        dialog.setHeaderText(null);
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType confirmButton = new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, cancelButton);

        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        dialog.getDialogPane().setPrefHeight(665);

        int mapWidth = 1000;
        int mapHeight = 320;
        int zoom = 15;

        final double[] center = new double[]{
                selectedLatitude,
                selectedLongitude
        };

        VBox root = new VBox(0);
        root.setPrefSize(1080, 625);
        root.setStyle("-fx-background-color: #f5f8fc; -fx-background-radius: 18;");

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16));
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #0891b2, #2563eb);" +
                        "-fx-background-radius: 18 18 0 0;"
        );

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(48, 48);
        iconBox.setStyle(
                "-fx-background-color: rgba(255,255,255,0.22);" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-color: rgba(255,255,255,0.35);" +
                        "-fx-border-radius: 15;"
        );

        Label icon = new Label("🗺");
        icon.setStyle("-fx-font-size: 24px;");
        iconBox.getChildren().add(icon);

        VBox titleBox = new VBox(4);

        Label title = new Label("Choisir une localisation");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label subtitle = new Label("Recherchez une adresse ou cliquez sur la carte.");
        subtitle.setStyle("-fx-text-fill: rgba(255,255,255,0.90); -fx-font-size: 14px;");
        subtitle.setWrapText(true);

        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(iconBox, titleBox);

        ImageView mapImage = new ImageView();
        mapImage.setFitWidth(mapWidth);
        mapImage.setFitHeight(mapHeight);
        mapImage.setPreserveRatio(false);
        mapImage.setSmooth(true);
        mapImage.setStyle("-fx-cursor: hand;");

        Label selectedLabel = new Label("Localisation sélectionnée : cliquez sur la carte");
        selectedLabel.setWrapText(true);
        selectedLabel.setMaxWidth(Double.MAX_VALUE);
        selectedLabel.setMinHeight(45);
        selectedLabel.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 10;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-font-size: 13px;" +
                        "-fx-border-color: #dbeafe;" +
                        "-fx-border-radius: 12;"
        );

        TextField searchField = new TextField();
        searchField.setPromptText("Exemple : Tunis, Ariana, Lac 2, Sousse...");
        searchField.setPrefWidth(550);
        searchField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #dbeafe;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 10;" +
                        "-fx-font-size: 13px;"
        );

        Button searchBtn = new Button("Rechercher");
        searchBtn.setStyle(
                "-fx-background-color: #2563eb;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 9 15;"
        );

        Button currentLocationBtn = new Button("Utiliser ma position approximative");
        currentLocationBtn.setStyle(
                "-fx-background-color: #0891b2;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 9 15;"
        );

        HBox searchBar = new HBox(10, searchField, searchBtn, currentLocationBtn);
        searchBar.setAlignment(Pos.CENTER);

        VBox mapCard = new VBox(mapImage);
        mapCard.setAlignment(Pos.CENTER);
        mapCard.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15, 23, 42, 0.12), 14, 0, 0, 5);"
        );

        VBox content = new VBox(12);
        content.setPadding(new Insets(14));
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(searchBar, mapCard, selectedLabel);

        root.getChildren().addAll(header, content);

        final Runnable[] refreshMap = new Runnable[1];

        refreshMap[0] = () -> {
            selectedLabel.setText("Chargement de la carte...");

            Task<Image> task = new Task<>() {
                @Override
                protected Image call() throws Exception {
                    return buildNativeMapImage(center[0], center[1], zoom, mapWidth, mapHeight);
                }
            };

            task.setOnSucceeded(e -> {
                mapImage.setImage(task.getValue());

                if (selectedAddress != null && !selectedAddress.isBlank()) {
                    selectedLabel.setText("Localisation sélectionnée : " + selectedAddress);
                } else {
                    selectedLabel.setText(
                            String.format(
                                    Locale.US,
                                    "Localisation sélectionnée : %.6f, %.6f",
                                    selectedLatitude,
                                    selectedLongitude
                            )
                    );
                }
            });

            task.setOnFailed(e -> {
                task.getException().printStackTrace();
                selectedLabel.setText("Impossible de charger la carte. Vérifiez votre connexion Internet.");
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        };

        mapImage.setOnMouseClicked((MouseEvent event) -> {
            double[] clicked = pixelToLatLng(
                    event.getX(),
                    event.getY(),
                    center[0],
                    center[1],
                    zoom,
                    mapWidth,
                    mapHeight
            );

            selectedLatitude = clicked[0];
            selectedLongitude = clicked[1];

            center[0] = selectedLatitude;
            center[1] = selectedLongitude;

            selectedAddress = String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    selectedLatitude,
                    selectedLongitude
            );

            selectedLabel.setText("Recherche de l’adresse exacte...");
            reverseGeocodeNative(selectedLatitude, selectedLongitude, selectedLabel);
            refreshMap[0].run();
        });

        searchBtn.setOnAction(e -> {
            String query = searchField.getText();

            if (query == null || query.trim().isEmpty()) {
                selectedLabel.setText("Écrivez une adresse ou une ville à rechercher.");
                return;
            }

            selectedLabel.setText("Recherche de la localisation...");

            Task<double[]> task = new Task<>() {
                @Override
                protected double[] call() throws Exception {
                    return searchAddressCoordinates(query.trim());
                }
            };

            task.setOnSucceeded(ev -> {
                double[] coords = task.getValue();

                selectedLatitude = coords[0];
                selectedLongitude = coords[1];

                center[0] = selectedLatitude;
                center[1] = selectedLongitude;

                if (lastSearchAddressLabel != null && !lastSearchAddressLabel.isBlank()) {
                    selectedAddress = lastSearchAddressLabel;
                } else {
                    selectedAddress = query.trim();
                }

                refreshMap[0].run();
                reverseGeocodeNative(selectedLatitude, selectedLongitude, selectedLabel);
            });

            task.setOnFailed(ev -> {
                task.getException().printStackTrace();
                selectedLabel.setText("Adresse introuvable. Essayez avec une ville plus simple.");
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        currentLocationBtn.setOnAction(e -> {
            selectedLabel.setText("Détection approximative par réseau Internet...");

            Task<double[]> task = new Task<>() {
                @Override
                protected double[] call() throws Exception {
                    return getApproxLocationByIp();
                }
            };

            task.setOnSucceeded(ev -> {
                double[] coords = task.getValue();

                selectedLatitude = coords[0];
                selectedLongitude = coords[1];

                center[0] = selectedLatitude;
                center[1] = selectedLongitude;

                selectedAddress = String.format(
                        Locale.US,
                        "%.6f, %.6f",
                        selectedLatitude,
                        selectedLongitude
                );

                refreshMap[0].run();
                reverseGeocodeNative(selectedLatitude, selectedLongitude, selectedLabel);
            });

            task.setOnFailed(ev -> {
                task.getException().printStackTrace();
                selectedLabel.setText(
                        "Impossible de détecter votre position. Recherchez une adresse ou cliquez sur la carte."
                );
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        Button confirm = (Button) dialog.getDialogPane().lookupButton(confirmButton);
        confirm.setStyle(
                "-fx-background-color: #059669;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 10 26;"
        );

        Button cancel = (Button) dialog.getDialogPane().lookupButton(cancelButton);
        cancel.setStyle(
                "-fx-background-color: #e5e7eb;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 10 24;"
        );

        dialog.setOnShown(e -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setWidth(1120);
            stage.setHeight(675);
            stage.centerOnScreen();
            stage.setResizable(false);

            if (useCurrentLocation) {
                currentLocationBtn.fire();
            } else {
                refreshMap[0].run();
            }
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result == confirmButton) {
                localisationField.setText(selectedAddress);
                refreshPreview();
            }
        });
    }

    private Image buildNativeMapImage(double centerLat, double centerLng, int zoom, int width, int height) throws Exception {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();

        g.setColor(new Color(229, 231, 235));
        g.fillRect(0, 0, width, height);

        double[] centerPixel = latLngToPixel(centerLat, centerLng, zoom);

        double topLeftX = centerPixel[0] - width / 2.0;
        double topLeftY = centerPixel[1] - height / 2.0;

        int tileSize = 256;

        int startTileX = (int) Math.floor(topLeftX / tileSize);
        int startTileY = (int) Math.floor(topLeftY / tileSize);

        int endTileX = (int) Math.floor((topLeftX + width) / tileSize);
        int endTileY = (int) Math.floor((topLeftY + height) / tileSize);

        int maxTile = (int) Math.pow(2, zoom);

        for (int x = startTileX; x <= endTileX; x++) {
            for (int y = startTileY; y <= endTileY; y++) {
                if (y < 0 || y >= maxTile) {
                    continue;
                }

                int wrappedX = ((x % maxTile) + maxTile) % maxTile;

                double tilePixelX = x * tileSize;
                double tilePixelY = y * tileSize;

                int drawX = (int) Math.round(tilePixelX - topLeftX);
                int drawY = (int) Math.round(tilePixelY - topLeftY);

                try {
                    BufferedImage tile = loadMapTile(wrappedX, y, zoom);
                    if (tile != null) {
                        g.drawImage(tile, drawX, drawY, null);
                    }
                } catch (Exception ignored) {
                    g.setColor(new Color(229, 231, 235));
                    g.fillRect(drawX, drawY, tileSize, tileSize);
                }
            }
        }

        drawCenterMarker(g, width, height);

        g.dispose();

        return SwingFXUtils.toFXImage(result, null);
    }

    private BufferedImage loadMapTile(int x, int y, int zoom) throws Exception {
        String[] servers = {"a", "b", "c", "d"};
        String server = servers[Math.abs(x + y) % servers.length];

        String urlText = "https://" + server +
                ".basemaps.cartocdn.com/rastertiles/voyager/" +
                zoom + "/" + x + "/" + y + ".png";

        URL url = new URL(urlText);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        return ImageIO.read(connection.getInputStream());
    }

    private void drawCenterMarker(Graphics2D g, int width, int height) {
        int x = width / 2;
        int y = height / 2;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(37, 99, 235));
        g.fillOval(x - 13, y - 34, 26, 26);

        g.setColor(Color.WHITE);
        g.fillOval(x - 5, y - 26, 10, 10);

        g.setColor(new Color(37, 99, 235));
        int[] triangleX = {x - 8, x + 8, x};
        int[] triangleY = {y - 12, y - 12, y + 8};
        g.fillPolygon(triangleX, triangleY, 3);

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawOval(x - 13, y - 34, 26, 26);
    }

    private double[] latLngToPixel(double lat, double lng, int zoom) {
        double sinLat = Math.sin(Math.toRadians(lat));
        double scale = 256.0 * Math.pow(2, zoom);

        double x = (lng + 180.0) / 360.0 * scale;
        double y = (0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI)) * scale;

        return new double[]{x, y};
    }

    private double[] pixelToLatLng(double clickX, double clickY, double centerLat, double centerLng, int zoom, int width, int height) {
        double[] centerPixel = latLngToPixel(centerLat, centerLng, zoom);

        double globalX = centerPixel[0] - width / 2.0 + clickX;
        double globalY = centerPixel[1] - height / 2.0 + clickY;

        double scale = 256.0 * Math.pow(2, zoom);

        double lng = globalX / scale * 360.0 - 180.0;

        double n = Math.PI - 2.0 * Math.PI * globalY / scale;
        double lat = Math.toDegrees(Math.atan(Math.sinh(n)));

        return new double[]{lat, lng};
    }

    private double[] searchAddressCoordinates(String query) throws Exception {
        lastSearchAddressLabel = null;

        double[] knownLocation = getKnownTunisianLocation(query);

        if (knownLocation != null) {
            return knownLocation;
        }

        String finalQuery = query.trim();

        if (!finalQuery.toLowerCase().contains("tunisie")
                && !finalQuery.toLowerCase().contains("tunisia")) {
            finalQuery = finalQuery + ", Tunisie";
        }

        String encoded = URLEncoder.encode(finalQuery, StandardCharsets.UTF_8);

        String urlText =
                "https://nominatim.openstreetmap.org/search?format=jsonv2" +
                        "&limit=5" +
                        "&addressdetails=1" +
                        "&accept-language=fr" +
                        "&countrycodes=tn" +
                        "&q=" + encoded;

        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "MedFlow-JavaFX-App/1.0");

        String json = readResponse(connection);

        if (json == null || json.equals("[]")) {
            throw new RuntimeException("Adresse introuvable");
        }

        String firstObject = extractFirstJsonObject(json);

        String latText = extractStringFromJson(firstObject, "lat");
        String lonText = extractStringFromJson(firstObject, "lon");

        try {
            lastSearchAddressLabel = extractStringFromJson(firstObject, "display_name");
        } catch (Exception e) {
            lastSearchAddressLabel = finalQuery;
        }

        return new double[]{
                Double.parseDouble(latText),
                Double.parseDouble(lonText)
        };
    }

    private double[] getKnownTunisianLocation(String query) {
        if (query == null) {
            return null;
        }

        String q = normalizeLocationText(query);

        switch (q) {
            case "tunis":
            case "tunisia":
            case "tunisie":
                lastSearchAddressLabel = "Tunis, Tunisie";
                return new double[]{36.8065, 10.1815};

            case "ariana":
            case "aryana":
                lastSearchAddressLabel = "Ariana, Gouvernorat Ariana, Tunisie";
                return new double[]{36.8665, 10.1647};

            case "la soukra":
            case "soukra":
                lastSearchAddressLabel = "La Soukra, Gouvernorat Ariana, Tunisie";
                return new double[]{36.8792, 10.2473};

            case "raoued":
                lastSearchAddressLabel = "Raoued, Gouvernorat Ariana, Tunisie";
                return new double[]{36.9492, 10.1822};

            case "el menzah":
            case "menzah":
                lastSearchAddressLabel = "El Menzah, Tunis, Tunisie";
                return new double[]{36.8429, 10.1647};

            case "lac 1":
            case "les berges du lac 1":
                lastSearchAddressLabel = "Les Berges du Lac 1, Tunis, Tunisie";
                return new double[]{36.8336, 10.2370};

            case "lac 2":
            case "les berges du lac 2":
                lastSearchAddressLabel = "Les Berges du Lac 2, Tunis, Tunisie";
                return new double[]{36.8482, 10.2797};

            case "manouba":
            case "la manouba":
                lastSearchAddressLabel = "La Manouba, Tunisie";
                return new double[]{36.8080, 10.0972};

            case "ben arous":
                lastSearchAddressLabel = "Ben Arous, Tunisie";
                return new double[]{36.7531, 10.2189};

            case "sousse":
                lastSearchAddressLabel = "Sousse, Tunisie";
                return new double[]{35.8256, 10.63699};

            case "sfax":
                lastSearchAddressLabel = "Sfax, Tunisie";
                return new double[]{34.7406, 10.7603};

            case "nabeul":
                lastSearchAddressLabel = "Nabeul, Tunisie";
                return new double[]{36.4561, 10.7376};

            case "bizerte":
                lastSearchAddressLabel = "Bizerte, Tunisie";
                return new double[]{37.2744, 9.8739};

            case "monastir":
                lastSearchAddressLabel = "Monastir, Tunisie";
                return new double[]{35.7770, 10.8262};

            case "mahdia":
                lastSearchAddressLabel = "Mahdia, Tunisie";
                return new double[]{35.5047, 11.0622};

            case "kairouan":
                lastSearchAddressLabel = "Kairouan, Tunisie";
                return new double[]{35.6781, 10.0963};

            default:
                return null;
        }
    }

    private String normalizeLocationText(String value) {
        return value.trim()
                .toLowerCase()
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("à", "a")
                .replace("â", "a")
                .replace("î", "i")
                .replace("ï", "i")
                .replace("ô", "o")
                .replace("ù", "u")
                .replace("ç", "c")
                .replaceAll("\\s+", " ");
    }

    private String extractFirstJsonObject(String jsonArray) {
        int start = jsonArray.indexOf("{");

        if (start == -1) {
            throw new RuntimeException("Aucun objet JSON trouvé");
        }

        int depth = 0;

        for (int i = start; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;

                if (depth == 0) {
                    return jsonArray.substring(start, i + 1);
                }
            }
        }

        throw new RuntimeException("Objet JSON incomplet");
    }

    private void reverseGeocodeNative(double lat, double lng, Label selectedLabel) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                String urlText = String.format(
                        Locale.US,
                        "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%.8f&lon=%.8f&zoom=18&addressdetails=1&accept-language=fr",
                        lat,
                        lng
                );

                HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", "MedFlow-JavaFX-App/1.0");

                String json = readResponse(connection);

                try {
                    return extractStringFromJson(json, "display_name");
                } catch (Exception e) {
                    return String.format(Locale.US, "%.6f, %.6f", lat, lng);
                }
            }
        };

        task.setOnSucceeded(e -> {
            String address = task.getValue();

            if (address == null || address.isBlank()) {
                selectedAddress = String.format(Locale.US, "%.6f, %.6f", lat, lng);
            } else {
                selectedAddress = address;
            }

            selectedLabel.setText("Localisation sélectionnée : " + selectedAddress);
        });

        task.setOnFailed(e -> {
            selectedAddress = String.format(Locale.US, "%.6f, %.6f", lat, lng);
            selectedLabel.setText("Localisation sélectionnée : " + selectedAddress);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private double[] getApproxLocationByIp() throws Exception {
        try {
            return getLocationFromIpApiCo();
        } catch (Exception e) {
            return getLocationFromIpApiCom();
        }
    }

    private double[] getLocationFromIpApiCo() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://ipapi.co/json/").openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        String json = readResponse(connection);

        double lat = extractDoubleFromJson(json, "latitude");
        double lng = extractDoubleFromJson(json, "longitude");

        return new double[]{lat, lng};
    }

    private double[] getLocationFromIpApiCom() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://ip-api.com/json/").openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        String json = readResponse(connection);

        double lat = extractDoubleFromJson(json, "lat");
        double lng = extractDoubleFromJson(json, "lon");

        return new double[]{lat, lng};
    }

    private String readResponse(HttpURLConnection connection) throws Exception {
        StringBuilder response = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    private double extractDoubleFromJson(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);

        if (start == -1) {
            throw new RuntimeException("Clé introuvable : " + key);
        }

        start += search.length();

        int end = json.indexOf(",", start);
        if (end == -1) {
            end = json.indexOf("}", start);
        }

        String value = json.substring(start, end).trim();
        return Double.parseDouble(value);
    }

    private String extractStringFromJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);

        if (start == -1) {
            throw new RuntimeException("Clé introuvable : " + key);
        }

        start += search.length();

        int end = json.indexOf("\"", start);

        while (end > 0 && json.charAt(end - 1) == '\\') {
            end = json.indexOf("\"", end + 1);
        }

        if (end == -1) {
            throw new RuntimeException("Fin de valeur introuvable : " + key);
        }

        return json.substring(start, end)
                .replace("\\\"", "\"")
                .replace("\\/", "/")
                .replace("\\n", " ")
                .replace("\\t", " ");
    }

    private CloudinaryPostService getCloudinaryPostService() {
        if (cloudinaryPostService == null) {
            cloudinaryPostService = new CloudinaryPostService();
        }
        return cloudinaryPostService;
    }
}