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
import tn.esprit.services.GeminiPostAssistantService;
import tn.esprit.services.PostService;
import tn.esprit.entities.User;
import tn.esprit.tools.SessionManager;

import java.io.FileOutputStream;
import java.util.List;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.time.LocalDateTime;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import tn.esprit.entities.PostAiSuggestion;


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
    private final GeminiPostAssistantService geminiPostAssistantService = new GeminiPostAssistantService();

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
                // MODE AJOUT
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
                post.setImg_post(isBlank(imageField.getText()) ? "" : imageField.getText().trim());
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

            javafx.application.Platform.runLater(() -> {
                closeDialogForce(loadingDialog);
                showAiSuggestionPopup(suggestion);
            });
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            ex.printStackTrace();

            javafx.application.Platform.runLater(() -> {
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

        task.setOnCancelled(event -> {
            javafx.application.Platform.runLater(() -> closeDialogForce(loadingDialog));
        });

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

        dialog.setOnCloseRequest(e -> {
            closeDialogForce(dialog);
        });

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
        HBox.setHgrow(topCards, Priority.ALWAYS);

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
}