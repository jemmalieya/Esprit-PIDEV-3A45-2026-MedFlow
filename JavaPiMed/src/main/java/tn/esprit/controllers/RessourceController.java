package tn.esprit.controllers;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Ressource;
import tn.esprit.services.CloudinaryEventUploadService;
import tn.esprit.services.EvenementService;
import tn.esprit.services.RessourceService;
import tn.esprit.tools.SessionManager;

import java.io.File;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class RessourceController {

    private static final double DEFAULT_SCENE_WIDTH = 1650;
    private static final double DEFAULT_SCENE_HEIGHT = 960;

    /* ===================== SIDEBAR / DASHBOARD ===================== */
    @FXML private VBox submenuVBox;
    @FXML private BorderPane resourceDashboardRoot;
    @FXML private Label ressourceArrow;
    @FXML private Label totalResourcesLabel;
    @FXML private Label fileResourcesLabel;
    @FXML private Label linkResourcesLabel;
    @FXML private Label stockResourcesLabel;
    @FXML private Label accessibilityResourcesLabel;
    @FXML private Label historiqueResourcesLabel;
    @FXML private Label inventoryCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ComboBox<String> categoryFilterCombo;

    @FXML private TableView<Ressource> ressourceTable;
    @FXML private TableColumn<Ressource, String> eventCol;
    @FXML private TableColumn<Ressource, String> nomCol;
    @FXML private TableColumn<Ressource, String> categorieCol;
    @FXML private TableColumn<Ressource, String> typeCol;
    @FXML private TableColumn<Ressource, String> quantiteCol;
    @FXML private TableColumn<Ressource, String> fournisseurCol;
    @FXML private TableColumn<Ressource, String> notesCol;
    @FXML private TableColumn<Ressource, Void> actionsCol;

    /* ===================== FORM ===================== */
    @FXML private ComboBox<Evenement> cbEvenement;
    @FXML private TextField tfNomRessource;
    @FXML private TextField tfCategorieRessource;
    @FXML private ComboBox<String> cbTypeRessource;
    @FXML private TextField tfCheminFichier;
    @FXML private TextField tfMimeType;
    @FXML private TextField tfTailleKb;
    @FXML private TextField tfUrlExterne;
    @FXML private TextField tfQuantite;
    @FXML private TextField tfUnite;
    @FXML private TextField tfFournisseur;
    @FXML private TextField tfCoutEstime;
    @FXML private CheckBox cbEstPublique;
    @FXML private TextArea taNotesRessource;

    /* ===================== SERVICES / DATA ===================== */
    private final RessourceService ressourceService = new RessourceService();
    private final EvenementService evenementService = new EvenementService();
    private final CloudinaryEventUploadService cloudinaryEventUploadService = new CloudinaryEventUploadService();
    private final ObservableList<Ressource> masterList = FXCollections.observableArrayList();
    private final ObservableList<Evenement> evenements = FXCollections.observableArrayList();
    private final Map<Integer, String> eventTitlesById = new HashMap<>();
    private FilteredList<Ressource> filteredList;

    private static Ressource ressourceAModifier;

    /* ===================== VALIDATION LABELS ===================== */
    private Label evenementMsg, nomMsg, categorieMsg, typeMsg, cheminMsg, mimeMsg, tailleMsg,
            urlMsg, quantiteMsg, uniteMsg, fournisseurMsg, coutMsg, notesMsg;

    /* ===================== VALIDATION FLAGS ===================== */
    private boolean isLoading = false;

    private boolean isEvenementValid = false;
    private boolean isNomValid = false;
    private boolean isCategorieValid = false;
    private boolean isTypeValid = false;
    private boolean isCheminValid = false;
    private boolean isMimeValid = false;
    private boolean isTailleValid = false;
    private boolean isUrlValid = false;
    private boolean isQuantiteValid = false;
    private boolean isUniteValid = false;
    private boolean isFournisseurValid = false;
    private boolean isCoutValid = false;
    private boolean isNotesValid = false;

    @FXML
    public void initialize() {
        initEventOptions();
        initDashboardIfExists();
        initFormIfExists();
        chargerRessourceAModifier();
    }

    /* =========================================================
       ===================== INIT ===============================
       ========================================================= */

    private void initDashboardIfExists() {
        if (ressourceTable == null) return;

        configureTable();
        configureSort();
        loadData();
        configureSearch();
        updateStats();
    }

    private void initFormIfExists() {
        if (cbTypeRessource == null) return;

        cbTypeRessource.setItems(FXCollections.observableArrayList(
                "file",
                "external_link",
                "stock_item"
        ));

        configureEventComboBox();
        installValidationLabels();
        installValidationListeners();

        if (ressourceAModifier == null) {
            validateAllFields();
        }
    }

    private void initEventOptions() {
        try {
            evenements.setAll(evenementService.recuperer());
            eventTitlesById.clear();

            for (Evenement event : evenements) {
                eventTitlesById.put(event.getId(), text(event.getTitre_event()));
            }
        } catch (SQLException e) {
            eventTitlesById.clear();
        }
    }

    /* =========================================================
       ===================== NAVIGATION =========================
       ========================================================= */

    @FXML
    private void toggleSubmenu() {
        if (submenuVBox == null || ressourceArrow == null) return;

        boolean show = !submenuVBox.isVisible();
        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);
        ressourceArrow.setText(show ? "⌃" : "⌄");
    }

    @FXML
    private void retourListeRessources() {
        ressourceAModifier = null;
        loadScene("/RessourceDashboard.fxml", "Gestion des Ressources");
    }

    @FXML
    private void onVoirTousRessources() {
        retourListeRessources();
    }

    @FXML
    private void onNewRessource() {
        ressourceAModifier = null;
        loadScene("/AjouterRessource.fxml", "Nouvelle Ressource");
    }

    @FXML
    private void onGestionEvenements() {
        loadScene("/EvenementDashboard.fxml", "Gestion des Evenements");
    }

    @FXML
    private void onLogout() {
        SessionManager.clear();
        loadScene("/FrontFXML/Login.fxml", "Connexion");
    }

    /* =========================================================
       ===================== ACTIONS AJOUT / MODIF ==============
       ========================================================= */

    @FXML
    private void ajouterRessource() {
        saveRessource(false);
    }

    @FXML
    private void ajouterRessourceEtNouvelle() {
        saveRessource(true);
    }

    @FXML
    private void modifierRessource() {
        if (ressourceAModifier == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucune ressource sélectionnée.");
            return;
        }

        if (!isFormValid()) {
            showInvalidFieldsMessage();
            return;
        }

        try {
            boolean existe = ressourceService.ressourceExisteDejaPourModification(
                    ressourceAModifier.getId(),
                    cbEvenement.getValue().getId(),
                    tfNomRessource.getText().trim(),
                    cbTypeRessource.getValue()
            );

            if (existe) {
                showAlert(
                        Alert.AlertType.WARNING,
                        "Ressource déjà existante",
                        "Une autre ressource avec le même nom, le même type et le même événement existe déjà."
                );
                return;
            }

            remplirRessourceDepuisForm(ressourceAModifier);
            ressourceAModifier.setDate_mise_a_jour_ressource(new Date());
            ressourceService.modifier(ressourceAModifier);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Ressource modifiée avec succès.");
            ressourceAModifier = null;
            retourListeRessources();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void choisirFichierCloudinary() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Uploader une ressource vers Cloudinary");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents et images",
                        "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.webp", "*.doc", "*.docx", "*.ppt", "*.pptx", "*.xls", "*.xlsx"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File file = fileChooser.showOpenDialog(resolveCurrentStage());
        if (file == null) {
            return;
        }

        if (!cloudinaryEventUploadService.isConfigured()) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Cloudinary non configure",
                    "Ajoutez CLOUDINARY_EVENT_CLOUD_NAME, CLOUDINARY_EVENT_API_KEY et CLOUDINARY_EVENT_API_SECRET dans .env.local, les variables d'environnement ou VM options."
            );
            return;
        }

        try {
            CloudinaryEventUploadService.UploadResult result = cloudinaryEventUploadService.uploadResourceFile(file);
            cbTypeRessource.setValue("file");
            tfCheminFichier.setText(result.secureUrl());

            String mime = Files.probeContentType(file.toPath());
            tfMimeType.setText(mime == null || mime.isBlank() ? "application/octet-stream" : mime);
            tfTailleKb.setText(String.valueOf(Math.max(1, Math.round(file.length() / 1024.0f))));

            validateConditionalFields();
            showAlert(Alert.AlertType.INFORMATION, "Cloudinary", "Ressource uploadee avec succes vers Cloudinary.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Cloudinary", "Upload impossible : " + e.getMessage());
        }
    }

    private void saveRessource(boolean stayOnForm) {
        if (!isFormValid()) {
            showInvalidFieldsMessage();
            return;
        }

        try {
            boolean existe = ressourceService.ressourceExisteDeja(
                    cbEvenement.getValue().getId(),
                    tfNomRessource.getText().trim(),
                    cbTypeRessource.getValue()
            );

            if (existe) {
                showAlert(
                        Alert.AlertType.WARNING,
                        "Ressource déjà existante",
                        "Une ressource avec le même nom, le même type et le même événement existe déjà."
                );
                return;
            }

            Ressource ressource = new Ressource();
            remplirRessourceDepuisForm(ressource);
            ressource.setDate_creation_ressource(new Date());
            ressource.setDate_mise_a_jour_ressource(new Date());

            ressourceService.ajouter(ressource);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Ressource ajoutée avec succès.");

            if (stayOnForm) {
                Evenement selectedEvent = cbEvenement.getValue();
                clearForm();
                cbEvenement.setValue(selectedEvent);
            } else {
                retourListeRessources();
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    /* =========================================================
       ===================== FORM MAPPING =======================
       ========================================================= */

    private void remplirRessourceDepuisForm(Ressource ressource) throws Exception {
        String type = cbTypeRessource.getValue();

        ressource.setEvenement(cbEvenement.getValue());
        ressource.setNom_ressource(tfNomRessource.getText().trim());
        ressource.setCategorie_ressource(tfCategorieRessource.getText().trim());
        ressource.setType_ressource(type);
        ressource.setEst_publique_ressource(cbEstPublique.isSelected());
        ressource.setNotes_ressource(trimValue(taNotesRessource.getText()));

        // reset selon type
        ressource.setChemin_fichier_ressource("");
        ressource.setMime_type_ressource("");
        ressource.setTaille_kb_ressource(0);
        ressource.setUrl_externe_ressource("");
        ressource.setQuantite_disponible_ressource(0);
        ressource.setUnite_ressource("");
        ressource.setFournisseur_ressource("");
        ressource.setCout_estime_ressource(0);

        if ("file".equals(type)) {
            ressource.setChemin_fichier_ressource(trimValue(tfCheminFichier.getText()));
            ressource.setMime_type_ressource(trimValue(tfMimeType.getText()));
            ressource.setTaille_kb_ressource(parseInt(tfTailleKb));
        }

        if ("external_link".equals(type)) {
            ressource.setUrl_externe_ressource(trimValue(tfUrlExterne.getText()));
        }

        if ("stock_item".equals(type)) {
            ressource.setQuantite_disponible_ressource(parseInt(tfQuantite));
            ressource.setUnite_ressource(trimValue(tfUnite.getText()));
            ressource.setFournisseur_ressource(trimValue(tfFournisseur.getText()));
            ressource.setCout_estime_ressource(parseDouble(tfCoutEstime));
        }
    }

    private void clearForm() {
        tfNomRessource.clear();
        tfCategorieRessource.clear();
        cbTypeRessource.setValue(null);
        tfCheminFichier.clear();
        tfMimeType.clear();
        tfTailleKb.clear();
        tfUrlExterne.clear();
        tfQuantite.clear();
        tfUnite.clear();
        tfFournisseur.clear();
        tfCoutEstime.clear();
        cbEstPublique.setSelected(false);
        taNotesRessource.clear();

        validateAllFields();
    }

    private void chargerRessourceAModifier() {
        if (ressourceAModifier == null || tfNomRessource == null) return;

        isLoading = true;

        cbEvenement.setValue(
                findEventById(
                        ressourceAModifier.getEvenement() == null ? 0 : ressourceAModifier.getEvenement().getId()
                )
        );

        tfNomRessource.setText(trimValue(ressourceAModifier.getNom_ressource()));
        tfCategorieRessource.setText(trimValue(ressourceAModifier.getCategorie_ressource()));
        cbTypeRessource.setValue(trimValue(ressourceAModifier.getType_ressource()));

        tfCheminFichier.setText(trimValue(ressourceAModifier.getChemin_fichier_ressource()));
        tfMimeType.setText(trimValue(ressourceAModifier.getMime_type_ressource()));
        tfTailleKb.setText(ressourceAModifier.getTaille_kb_ressource() <= 0 ? "" : String.valueOf(ressourceAModifier.getTaille_kb_ressource()));

        tfUrlExterne.setText(trimValue(ressourceAModifier.getUrl_externe_ressource()));

        tfQuantite.setText(ressourceAModifier.getQuantite_disponible_ressource() <= 0 ? "" : String.valueOf(ressourceAModifier.getQuantite_disponible_ressource()));
        tfUnite.setText(trimValue(ressourceAModifier.getUnite_ressource()));
        tfFournisseur.setText(trimValue(ressourceAModifier.getFournisseur_ressource()));
        tfCoutEstime.setText(ressourceAModifier.getCout_estime_ressource() <= 0 ? "" : String.valueOf(ressourceAModifier.getCout_estime_ressource()));

        cbEstPublique.setSelected(ressourceAModifier.isEst_publique_ressource());
        taNotesRessource.setText(trimValue(ressourceAModifier.getNotes_ressource()));

        isLoading = false;
        validateAllFields();
    }

    /* =========================================================
       ===================== VALIDATION =========================
       ========================================================= */

    private void installValidationLabels() {
        evenementMsg = createValidationLabel();
        nomMsg = createValidationLabel();
        categorieMsg = createValidationLabel();
        typeMsg = createValidationLabel();
        cheminMsg = createValidationLabel();
        mimeMsg = createValidationLabel();
        tailleMsg = createValidationLabel();
        urlMsg = createValidationLabel();
        quantiteMsg = createValidationLabel();
        uniteMsg = createValidationLabel();
        fournisseurMsg = createValidationLabel();
        coutMsg = createValidationLabel();
        notesMsg = createValidationLabel();

        insertValidationLabel(cbEvenement, evenementMsg);
        insertValidationLabel(tfNomRessource, nomMsg);
        insertValidationLabel(tfCategorieRessource, categorieMsg);
        insertValidationLabel(cbTypeRessource, typeMsg);
        insertValidationLabel(tfCheminFichier, cheminMsg);
        insertValidationLabel(tfMimeType, mimeMsg);
        insertValidationLabel(tfTailleKb, tailleMsg);
        insertValidationLabel(tfUrlExterne, urlMsg);
        insertValidationLabel(tfQuantite, quantiteMsg);
        insertValidationLabel(tfUnite, uniteMsg);
        insertValidationLabel(tfFournisseur, fournisseurMsg);
        insertValidationLabel(tfCoutEstime, coutMsg);
        insertValidationLabel(taNotesRessource, notesMsg);
    }

    private Label createValidationLabel() {
        Label label = new Label();
        label.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        return label;
    }

    private void insertValidationLabel(Control field, Label label) {
        if (field == null || field.getParent() == null) return;

        if (field.getParent() instanceof VBox parent) {
            int index = parent.getChildren().indexOf(field);
            if (index != -1 && !parent.getChildren().contains(label)) {
                parent.getChildren().add(index + 1, label);
            }
        }
    }

    private void installValidationListeners() {
        cbEvenement.valueProperty().addListener((obs, oldVal, newVal) -> validateEvenement());
        tfNomRessource.textProperty().addListener((obs, oldVal, newVal) -> validateNom());
        tfCategorieRessource.textProperty().addListener((obs, oldVal, newVal) -> validateCategorie());

        cbTypeRessource.valueProperty().addListener((obs, oldVal, newVal) -> {
            validateType();
            validateConditionalFields();
        });

        tfCheminFichier.textProperty().addListener((obs, oldVal, newVal) -> validateConditionalFields());
        tfMimeType.textProperty().addListener((obs, oldVal, newVal) -> validateConditionalFields());
        tfTailleKb.textProperty().addListener((obs, oldVal, newVal) -> validateConditionalFields());
        tfUrlExterne.textProperty().addListener((obs, oldVal, newVal) -> validateConditionalFields());
        tfQuantite.textProperty().addListener((obs, oldVal, newVal) -> validateConditionalFields());
        tfUnite.textProperty().addListener((obs, oldVal, newVal) -> validateConditionalFields());
        tfFournisseur.textProperty().addListener((obs, oldVal, newVal) -> validateConditionalFields());
        tfCoutEstime.textProperty().addListener((obs, oldVal, newVal) -> validateConditionalFields());
        taNotesRessource.textProperty().addListener((obs, oldVal, newVal) -> validateNotes());
    }

    private void validateAllFields() {
        validateEvenement();
        validateNom();
        validateCategorie();
        validateType();
        validateConditionalFields();
        validateNotes();
    }

    private void validateEvenement() {
        if (isLoading) return;

        if (cbEvenement.getValue() == null) {
            setFieldError(cbEvenement, evenementMsg, "L'événement est obligatoire.");
            isEvenementValid = false;
        } else {
            setFieldSuccess(cbEvenement, evenementMsg, "Événement valide.");
            isEvenementValid = true;
        }
    }

    private void validateNom() {
        if (isLoading) return;

        String nom = trimValue(tfNomRessource.getText());

        if (nom.isEmpty()) {
            setFieldError(tfNomRessource, nomMsg, "Le nom de la ressource est obligatoire.");
            isNomValid = false;
            return;
        }

        if (nom.length() < 3) {
            setFieldError(tfNomRessource, nomMsg, "Minimum 3 caractères.");
            isNomValid = false;
            return;
        }

        if (nom.length() > 255) {
            setFieldError(tfNomRessource, nomMsg, "Maximum 255 caractères.");
            isNomValid = false;
            return;
        }

        if (!nom.matches("^[\\p{L}\\p{N}\\s\\-_'().]+$")) {
            setFieldError(tfNomRessource, nomMsg, "Nom invalide.");
            isNomValid = false;
            return;
        }

        setFieldSuccess(tfNomRessource, nomMsg, "Nom valide.");
        isNomValid = true;
    }

    private void validateCategorie() {
        if (isLoading) return;

        String categorie = trimValue(tfCategorieRessource.getText());

        if (categorie.isEmpty()) {
            setFieldError(tfCategorieRessource, categorieMsg, "La catégorie est obligatoire.");
            isCategorieValid = false;
            return;
        }

        if (categorie.length() < 2) {
            setFieldError(tfCategorieRessource, categorieMsg, "Minimum 2 caractères.");
            isCategorieValid = false;
            return;
        }

        if (categorie.length() > 100) {
            setFieldError(tfCategorieRessource, categorieMsg, "Maximum 100 caractères.");
            isCategorieValid = false;
            return;
        }

        if (!categorie.matches("^[\\p{L}\\p{N}\\s\\-_'().]+$")) {
            setFieldError(tfCategorieRessource, categorieMsg, "Catégorie invalide.");
            isCategorieValid = false;
            return;
        }

        setFieldSuccess(tfCategorieRessource, categorieMsg, "Catégorie valide.");
        isCategorieValid = true;
    }

    private void validateType() {
        if (isLoading) return;

        String type = cbTypeRessource.getValue();

        if (type == null || type.isBlank()) {
            setFieldError(cbTypeRessource, typeMsg, "Le type est obligatoire.");
            isTypeValid = false;
        } else {
            setFieldSuccess(cbTypeRessource, typeMsg, "Type valide.");
            isTypeValid = true;
        }
    }

    private void validateConditionalFields() {
        if (isLoading) return;

        String type = cbTypeRessource.getValue();

        resetField(tfCheminFichier, cheminMsg);
        resetField(tfMimeType, mimeMsg);
        resetField(tfTailleKb, tailleMsg);
        resetField(tfUrlExterne, urlMsg);
        resetField(tfQuantite, quantiteMsg);
        resetField(tfUnite, uniteMsg);
        resetField(tfFournisseur, fournisseurMsg);
        resetField(tfCoutEstime, coutMsg);

        isCheminValid = false;
        isMimeValid = false;
        isTailleValid = false;
        isUrlValid = false;
        isQuantiteValid = false;
        isUniteValid = false;
        isFournisseurValid = false;
        isCoutValid = false;

        if (type == null || type.isBlank()) {
            return;
        }

        if ("file".equals(type)) {
            validateFileFields();
            return;
        }

        if ("external_link".equals(type)) {
            validateExternalLinkFields();
            return;
        }

        if ("stock_item".equals(type)) {
            validateStockFields();
        }
    }

    private void validateFileFields() {
        String chemin = trimValue(tfCheminFichier.getText());
        String mime = trimValue(tfMimeType.getText());
        String taille = trimValue(tfTailleKb.getText());

        if (chemin.isEmpty()) {
            setFieldError(tfCheminFichier, cheminMsg, "Le chemin fichier est obligatoire.");
            isCheminValid = false;
        } else if (chemin.length() > 255) {
            setFieldError(tfCheminFichier, cheminMsg, "Maximum 255 caractères.");
            isCheminValid = false;
        } else {
            setFieldSuccess(tfCheminFichier, cheminMsg, "Chemin valide.");
            isCheminValid = true;
        }

        if (mime.isEmpty()) {
            setFieldError(tfMimeType, mimeMsg, "Le mime type est obligatoire.");
            isMimeValid = false;
        } else if (!mime.matches("^[a-zA-Z0-9.+-]+/[a-zA-Z0-9.+-]+$")) {
            setFieldError(tfMimeType, mimeMsg, "Mime type invalide.");
            isMimeValid = false;
        } else {
            setFieldSuccess(tfMimeType, mimeMsg, "Mime type valide.");
            isMimeValid = true;
        }

        if (taille.isEmpty()) {
            setFieldError(tfTailleKb, tailleMsg, "La taille KB est obligatoire.");
            isTailleValid = false;
        } else if (!taille.matches("^\\d+$")) {
            setFieldError(tfTailleKb, tailleMsg, "La taille doit être un entier positif.");
            isTailleValid = false;
        } else if (Integer.parseInt(taille) <= 0) {
            setFieldError(tfTailleKb, tailleMsg, "La taille doit être supérieure à 0.");
            isTailleValid = false;
        } else {
            setFieldSuccess(tfTailleKb, tailleMsg, "Taille valide.");
            isTailleValid = true;
        }

        isUrlValid = true;
        isQuantiteValid = true;
        isUniteValid = true;
        isFournisseurValid = true;
        isCoutValid = true;
    }

    private void validateExternalLinkFields() {
        String url = trimValue(tfUrlExterne.getText());

        if (url.isEmpty()) {
            setFieldError(tfUrlExterne, urlMsg, "L'URL est obligatoire.");
            isUrlValid = false;

        } else if (!url.matches("^(https?|file)://.+$")) {
            setFieldError(tfUrlExterne, urlMsg, "URL invalide.");
            isUrlValid = false;

        } else {
            setFieldSuccess(tfUrlExterne, urlMsg, "URL valide.");
            isUrlValid = true;
        }
        isCheminValid = true;
        isMimeValid = true;
        isTailleValid = true;
        isQuantiteValid = true;
        isUniteValid = true;
        isFournisseurValid = true;
        isCoutValid = true;
    }

    private void validateStockFields() {
        String quantite = trimValue(tfQuantite.getText());
        String unite = trimValue(tfUnite.getText());
        String fournisseur = trimValue(tfFournisseur.getText());
        String cout = trimValue(tfCoutEstime.getText());

        if (quantite.isEmpty()) {
            setFieldError(tfQuantite, quantiteMsg, "La quantité est obligatoire.");
            isQuantiteValid = false;
        } else if (!quantite.matches("^\\d+$")) {
            setFieldError(tfQuantite, quantiteMsg, "La quantité doit être un entier positif.");
            isQuantiteValid = false;
        } else if (Integer.parseInt(quantite) <= 0) {
            setFieldError(tfQuantite, quantiteMsg, "La quantité doit être supérieure à 0.");
            isQuantiteValid = false;
        } else {
            setFieldSuccess(tfQuantite, quantiteMsg, "Quantité valide.");
            isQuantiteValid = true;
        }

        if (unite.isEmpty()) {
            setFieldError(tfUnite, uniteMsg, "L'unité est obligatoire.");
            isUniteValid = false;
        } else if (unite.length() < 2) {
            setFieldError(tfUnite, uniteMsg, "Minimum 2 caractères.");
            isUniteValid = false;
        } else if (!unite.matches("^[\\p{L}\\s\\-_.]+$")) {
            setFieldError(tfUnite, uniteMsg, "Unité invalide.");
            isUniteValid = false;
        } else {
            setFieldSuccess(tfUnite, uniteMsg, "Unité valide.");
            isUniteValid = true;
        }

        if (fournisseur.isEmpty()) {
            setFieldError(tfFournisseur, fournisseurMsg, "Le fournisseur est obligatoire.");
            isFournisseurValid = false;
        } else if (fournisseur.length() < 2) {
            setFieldError(tfFournisseur, fournisseurMsg, "Minimum 2 caractères.");
            isFournisseurValid = false;
        } else if (!fournisseur.matches("^[\\p{L}\\p{N}\\s\\-_'().]+$")) {
            setFieldError(tfFournisseur, fournisseurMsg, "Fournisseur invalide.");
            isFournisseurValid = false;
        } else {
            setFieldSuccess(tfFournisseur, fournisseurMsg, "Fournisseur valide.");
            isFournisseurValid = true;
        }

        if (cout.isEmpty()) {
            setFieldError(tfCoutEstime, coutMsg, "Le coût estimé est obligatoire.");
            isCoutValid = false;
        } else if (!cout.matches("^\\d+(\\.\\d{1,3})?$")) {
            setFieldError(tfCoutEstime, coutMsg, "Coût invalide.");
            isCoutValid = false;
        } else if (Double.parseDouble(cout) < 0) {
            setFieldError(tfCoutEstime, coutMsg, "Le coût doit être positif.");
            isCoutValid = false;
        } else {
            setFieldSuccess(tfCoutEstime, coutMsg, "Coût valide.");
            isCoutValid = true;
        }

        isCheminValid = true;
        isMimeValid = true;
        isTailleValid = true;
        isUrlValid = true;
    }

    private void validateNotes() {
        if (isLoading) return;

        String notes = trimValue(taNotesRessource.getText());

        if (notes.isEmpty()) {
            setFieldError(taNotesRessource, notesMsg, "Les notes sont obligatoires.");
            isNotesValid = false;
            return;
        }

        if (notes.length() < 5) {
            setFieldError(taNotesRessource, notesMsg, "Minimum 5 caractères.");
            isNotesValid = false;
            return;
        }

        if (notes.length() > 2000) {
            setFieldError(taNotesRessource, notesMsg, "Maximum 2000 caractères.");
            isNotesValid = false;
            return;
        }

        setFieldSuccess(taNotesRessource, notesMsg, "Notes valides.");
        isNotesValid = true;
    }

    private void setFieldError(Control field, Label msgLabel, String message) {
        if (field != null) field.setStyle("-fx-border-color: red;");
        if (msgLabel != null) {
            msgLabel.setText(message);
            msgLabel.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        }
    }

    private void setFieldSuccess(Control field, Label msgLabel, String message) {
        if (field != null) field.setStyle("-fx-border-color: green;");
        if (msgLabel != null) {
            msgLabel.setText(message);
            msgLabel.setStyle("-fx-text-fill: green; -fx-font-size: 11px;");
        }
    }

    private void resetField(Control field, Label msgLabel) {
        if (field != null) field.setStyle("");
        if (msgLabel != null) msgLabel.setText("");
    }

    private boolean isFormValid() {
        validateAllFields();

        return isEvenementValid
                && isNomValid
                && isCategorieValid
                && isTypeValid
                && isCheminValid
                && isMimeValid
                && isTailleValid
                && isUrlValid
                && isQuantiteValid
                && isUniteValid
                && isFournisseurValid
                && isCoutValid
                && isNotesValid;
    }

    private void showInvalidFieldsMessage() {
        StringBuilder errors = new StringBuilder("Veuillez corriger les champs suivants :\n");

        if (!isEvenementValid) errors.append("- Événement\n");
        if (!isNomValid) errors.append("- Nom de la ressource\n");
        if (!isCategorieValid) errors.append("- Catégorie\n");
        if (!isTypeValid) errors.append("- Type\n");
        if (!isCheminValid) errors.append("- Chemin fichier\n");
        if (!isMimeValid) errors.append("- Mime type\n");
        if (!isTailleValid) errors.append("- Taille KB\n");
        if (!isUrlValid) errors.append("- URL externe\n");
        if (!isQuantiteValid) errors.append("- Quantité\n");
        if (!isUniteValid) errors.append("- Unité\n");
        if (!isFournisseurValid) errors.append("- Fournisseur\n");
        if (!isCoutValid) errors.append("- Coût estimé\n");
        if (!isNotesValid) errors.append("- Notes\n");

        showAlert(Alert.AlertType.ERROR, "Champs invalides", errors.toString());
    }

    /* =========================================================
       ===================== DASHBOARD TABLE ====================
       ========================================================= */

    private void configureTable() {
        eventCol.setCellValueFactory(cd -> new SimpleStringProperty(getEventTitle(cd.getValue())));
        nomCol.setCellValueFactory(cd -> new SimpleStringProperty(text(cd.getValue().getNom_ressource())));
        categorieCol.setCellValueFactory(cd -> new SimpleStringProperty(text(cd.getValue().getCategorie_ressource())));
        typeCol.setCellValueFactory(cd -> new SimpleStringProperty(text(cd.getValue().getType_ressource())));
        quantiteCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getQuantite_disponible_ressource())));
        fournisseurCol.setCellValueFactory(cd -> new SimpleStringProperty(text(cd.getValue().getFournisseur_ressource())));
        notesCol.setCellValueFactory(cd -> new SimpleStringProperty(shortText(text(cd.getValue().getNotes_ressource()), 45)));

        addActionsColumn();
        ressourceTable.setPlaceholder(new Label("Aucune ressource trouvee"));
        ressourceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void configureSort() {
        if (sortCombo == null) return;

        sortCombo.setItems(FXCollections.observableArrayList(
                "Nom A-Z",
                "Categorie A-Z",
                "Type",
                "Evenement"
        ));
        sortCombo.setValue("Nom A-Z");

        sortCombo.setOnAction(e -> applySort());

        if (categoryFilterCombo != null) {
            categoryFilterCombo.setItems(FXCollections.observableArrayList(
                    "Toutes categories",
                    "Accessibilite",
                    "Document",
                    "Materiel",
                    "Stock"
            ));
            categoryFilterCombo.setValue("Toutes categories");
            categoryFilterCombo.setOnAction(e -> applyFilters());
        }
    }

    private void loadData() {
        masterList.setAll(ressourceService.recuperer());
        filteredList = new FilteredList<>(masterList, r -> true);
        ressourceTable.setItems(filteredList);
        applyFilters();

        if (inventoryCountLabel != null) {
            inventoryCountLabel.setText(masterList.size() + " ressource(s) dans votre espace");
        }
    }

    private void configureSearch() {
        if (searchField == null) return;

        searchField.textProperty().addListener((o, a, b) -> {
            applyFilters();
        });
    }

    private void applyFilters() {
        if (filteredList == null) return;

        String keyword = normalize(searchField == null ? "" : searchField.getText());
        String category = categoryFilterCombo == null ? "Toutes categories" : text(categoryFilterCombo.getValue());
        String normalizedCategory = normalize(category);

        filteredList.setPredicate(r -> {
            boolean matchesKeyword = keyword.isEmpty()
                    || normalize(r.getNom_ressource()).contains(keyword)
                    || normalize(r.getCategorie_ressource()).contains(keyword)
                    || normalize(r.getType_ressource()).contains(keyword)
                    || normalize(getEventTitle(r)).contains(keyword)
                    || normalize(r.getFournisseur_ressource()).contains(keyword)
                    || normalize(r.getNotes_ressource()).contains(keyword);

            boolean matchesCategory = normalizedCategory.isBlank()
                    || normalizedCategory.equals("toutes categories")
                    || normalize(r.getCategorie_ressource()).contains(normalizedCategory);

            return matchesKeyword && matchesCategory;
        });

        updateStats();
    }

    @FXML
    private void showAccessibilityOnly() {
        if (categoryFilterCombo != null) {
            categoryFilterCombo.setValue("Accessibilite");
        }
        applyFilters();
    }

    @FXML
    private void showFileResourcesOnly() {
        if (categoryFilterCombo != null) {
            categoryFilterCombo.setValue("Toutes categories");
        }
        if (searchField != null) {
            searchField.setText("file");
        }
        applyFilters();
    }

    @FXML
    private void showExternalLinksOnly() {
        if (categoryFilterCombo != null) {
            categoryFilterCombo.setValue("Toutes categories");
        }
        if (searchField != null) {
            searchField.setText("external_link");
        }
        applyFilters();
    }

    @FXML
    private void showStockResourcesOnly() {
        if (categoryFilterCombo != null) {
            categoryFilterCombo.setValue("Stock");
        }
        if (searchField != null) {
            searchField.setText("");
        }
        applyFilters();
    }

    @FXML
    private void resetDashboardFilters() {
        if (categoryFilterCombo != null) {
            categoryFilterCombo.setValue("Toutes categories");
        }
        if (searchField != null) {
            searchField.setText("");
        }
        if (sortCombo != null) {
            sortCombo.setValue("Nom A-Z");
        }
        applySort();
        applyFilters();
    }

    private void applySort() {
        if (sortCombo == null || sortCombo.getValue() == null) return;

        switch (sortCombo.getValue()) {
            case "Categorie A-Z" ->
                    FXCollections.sort(masterList, (a, b) ->
                            text(a.getCategorie_ressource()).compareToIgnoreCase(text(b.getCategorie_ressource()))
                    );
            case "Type" ->
                    FXCollections.sort(masterList, (a, b) ->
                            text(a.getType_ressource()).compareToIgnoreCase(text(b.getType_ressource()))
                    );
            case "Evenement" ->
                    FXCollections.sort(masterList, (a, b) ->
                            getEventTitle(a).compareToIgnoreCase(getEventTitle(b))
                    );
            default ->
                    FXCollections.sort(masterList, (a, b) ->
                            text(a.getNom_ressource()).compareToIgnoreCase(text(b.getNom_ressource()))
                    );
        }
    }

    private void updateStats() {
        if (filteredList == null) return;

        if (totalResourcesLabel != null) {
            totalResourcesLabel.setText(String.valueOf(filteredList.size()));
        }

        if (fileResourcesLabel != null) {
            fileResourcesLabel.setText(String.valueOf(
                    filteredList.stream().filter(r -> "file".equalsIgnoreCase(text(r.getType_ressource()))).count()
            ));
        }

        if (linkResourcesLabel != null) {
            linkResourcesLabel.setText(String.valueOf(
                    filteredList.stream().filter(r -> "external_link".equalsIgnoreCase(text(r.getType_ressource()))).count()
            ));
        }

        if (stockResourcesLabel != null) {
            stockResourcesLabel.setText(String.valueOf(
                    filteredList.stream().filter(r -> "stock_item".equalsIgnoreCase(text(r.getType_ressource()))).count()
            ));
        }

        if (accessibilityResourcesLabel != null) {
            accessibilityResourcesLabel.setText(String.valueOf(
                    filteredList.stream().filter(r -> normalize(r.getCategorie_ressource()).contains("accessibilite")).count()
            ));
        }

        if (historiqueResourcesLabel != null) {
            historiqueResourcesLabel.setText(String.valueOf(
                    filteredList.stream().filter(r -> !r.isEst_publique_ressource()).count()
            ));
        }
    }

    private void addActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("Voir");
            private final Button editBtn = new Button("Modifier");
            private final Button archiveBtn = new Button("Historique");
            private final ToolBar toolBar = new ToolBar(viewBtn, editBtn, archiveBtn);

            {
                toolBar.getStyleClass().add("action-toolbar");
                viewBtn.getStyleClass().add("view-btn");
                editBtn.getStyleClass().add("edit-btn");
                archiveBtn.getStyleClass().add("delete-btn");

                viewBtn.setOnAction(e -> {
                    Ressource r = getTableView().getItems().get(getIndex());
                    afficherPopupDetails(r);
                });

                editBtn.setOnAction(e -> {
                    Ressource r = getTableView().getItems().get(getIndex());
                    ouvrirPageModification(r);
                });

                archiveBtn.setOnAction(e -> {
                    Ressource r = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Envoyer la ressource dans l'historique : " + text(r.getNom_ressource()) + " ?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        ressourceService.archiver(r);
                        ressourceTable.refresh();
                        updateStats();

                        if (inventoryCountLabel != null) {
                            inventoryCountLabel.setText(masterList.size() + " ressource(s) dans votre espace");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : toolBar);
            }
        });

        actionsCol.setCellValueFactory(param -> Bindings.createObjectBinding(() -> null));
    }

    private void ouvrirPageModification(Ressource ressource) {
        ressourceAModifier = ressource;
        loadScene("/ModifierRessource.fxml", "Modifier Ressource");
    }

    private void afficherPopupDetails(Ressource r) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Details ressource");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().setMinWidth(440);
        dialog.getDialogPane().setPrefWidth(440);

        VBox card = new VBox(16);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 24; -fx-padding: 24; -fx-border-color: #e6eef5; -fx-border-radius: 24;");

        Label title = new Label(text(r.getNom_ressource()));
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #16325c;");

        VBox infoBox = new VBox(10,
                createDetailRow("Evenement", getEventTitle(r)),
                createDetailRow("Categorie", text(r.getCategorie_ressource())),
                createDetailRow("Type", text(r.getType_ressource())),
                createDetailRow("Chemin", text(r.getChemin_fichier_ressource())),
                createDetailRow("Mime", text(r.getMime_type_ressource())),
                createDetailRow("Taille KB", String.valueOf(r.getTaille_kb_ressource())),
                createDetailRow("URL", text(r.getUrl_externe_ressource())),
                createDetailRow("Quantite", String.valueOf(r.getQuantite_disponible_ressource())),
                createDetailRow("Unite", text(r.getUnite_ressource())),
                createDetailRow("Fournisseur", text(r.getFournisseur_ressource())),
                createDetailRow("Coût", String.valueOf(r.getCout_estime_ressource()))
        );
        infoBox.setStyle("-fx-background-color: #f7fbfd; -fx-background-radius: 18; -fx-padding: 16;");

        Label notesTitle = new Label("Notes");
        notesTitle.setStyle("-fx-text-fill: #16325c; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label notes = new Label(text(r.getNotes_ressource()));
        notes.setWrapText(true);
        notes.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e6eef5; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 14; -fx-text-fill: #5b6b84; -fx-font-size: 13px;");

        card.getChildren().addAll(title, infoBox, notesTitle, notes);
        dialog.getDialogPane().setContent(card);
        dialog.showAndWait();
    }

    private HBox createDetailRow(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.setMinWidth(92);
        label.setStyle("-fx-text-fill: #7a8aa2; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label value = new Label(text(valueText));
        value.setWrapText(true);
        value.setMaxWidth(Double.MAX_VALUE);
        value.setStyle("-fx-text-fill: #183153; -fx-font-size: 13px; -fx-font-weight: 600;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new HBox(10, label, spacer, value);
    }

    /* =========================================================
       ===================== SCENE HELPERS ======================
       ========================================================= */

    private void loadScene(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = resolveCurrentStage();

            if (stage == null) {
                throw new Exception("Impossible de recuperer la fenetre actuelle.");
            }

            applySceneToStage(stage, root, title);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation echouee : " + e.getMessage());
        }
    }

    private void applySceneToStage(Stage stage, Parent root, String title) {
        boolean wasMaximized = stage.isMaximized();
        double sceneWidth = stage.getWidth() > 0 ? stage.getWidth() : DEFAULT_SCENE_WIDTH;
        double sceneHeight = stage.getHeight() > 0 ? stage.getHeight() : DEFAULT_SCENE_HEIGHT;

        stage.setScene(new Scene(root, sceneWidth, sceneHeight));
        stage.setTitle(title);
        stage.setResizable(true);
        stage.setWidth(sceneWidth);
        stage.setHeight(sceneHeight);
        stage.setMaximized(wasMaximized);
        stage.centerOnScreen();
        stage.show();
    }

    private Stage resolveCurrentStage() {
        if (resourceDashboardRoot != null && resourceDashboardRoot.getScene() != null) {
            return (Stage) resourceDashboardRoot.getScene().getWindow();
        }
        if (ressourceTable != null && ressourceTable.getScene() != null) {
            return (Stage) ressourceTable.getScene().getWindow();
        }
        if (searchField != null && searchField.getScene() != null) {
            return (Stage) searchField.getScene().getWindow();
        }
        if (sortCombo != null && sortCombo.getScene() != null) {
            return (Stage) sortCombo.getScene().getWindow();
        }
        if (categoryFilterCombo != null && categoryFilterCombo.getScene() != null) {
            return (Stage) categoryFilterCombo.getScene().getWindow();
        }
        if (inventoryCountLabel != null && inventoryCountLabel.getScene() != null) {
            return (Stage) inventoryCountLabel.getScene().getWindow();
        }
        if (tfNomRessource != null && tfNomRessource.getScene() != null) {
            return (Stage) tfNomRessource.getScene().getWindow();
        }
        if (cbEvenement != null && cbEvenement.getScene() != null) {
            return (Stage) cbEvenement.getScene().getWindow();
        }
        return null;
    }

    /* =========================================================
       ===================== EVENT COMBO ========================
       ========================================================= */

    private void configureEventComboBox() {
        if (cbEvenement == null) return;

        cbEvenement.setItems(evenements);
        cbEvenement.setConverter(new StringConverter<>() {
            @Override
            public String toString(Evenement event) {
                return event == null ? "" : text(event.getTitre_event());
            }

            @Override
            public Evenement fromString(String string) {
                return null;
            }
        });

        cbEvenement.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Evenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : text(item.getTitre_event()));
            }
        });

        cbEvenement.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Evenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : text(item.getTitre_event()));
            }
        });
    }

    private Evenement findEventById(int eventId) {
        return evenements.stream()
                .filter(event -> event.getId() == eventId)
                .findFirst()
                .orElse(null);
    }

    /* =========================================================
       ===================== PARSERS / UTILS ====================
       ========================================================= */

    private int parseInt(TextField field) throws Exception {
        String value = trimValue(field.getText());

        if (value.isBlank()) return 0;

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new Exception("Valeur numerique invalide : " + value);
        }
    }

    private double parseDouble(TextField field) throws Exception {
        String value = trimValue(field.getText());

        if (value.isBlank()) return 0;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new Exception("Cout estime invalide : " + value);
        }
    }

    private String getEventTitle(Ressource ressource) {
        if (ressource == null || ressource.getEvenement() == null) return "-";
        return eventTitlesById.getOrDefault(
                ressource.getEvenement().getId(),
                "Evenement #" + ressource.getEvenement().getId()
        );
    }

    private String shortText(String value, int max) {
        if (value == null || value.length() <= max) return text(value);
        return value.substring(0, max) + "...";
    }

    private String trimValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
