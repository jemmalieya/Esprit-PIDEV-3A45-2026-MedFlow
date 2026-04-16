package tn.esprit.controllers;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tn.esprit.entities.Evenement;
import tn.esprit.services.DashboardBIServiceEvenement;
import tn.esprit.services.EvenementService;

import java.io.File;
import java.io.FileWriter;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javafx.stage.FileChooser;




public class EvenementController {

    private static final double DEFAULT_SCENE_WIDTH = 1650;
    private static final double DEFAULT_SCENE_HEIGHT = 960;

    /* ===================== DASHBOARD ===================== */
    @FXML private Label totalEventsLabel;
    @FXML private Label publiesLabel;
    @FXML private Label brouillonsLabel;
    @FXML private Label archivesLabel;
    @FXML private Label inventoryCountLabel;
    @FXML private Label evenementArrow;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    @FXML private TableView<Evenement> evenementTable;
    @FXML private TableColumn<Evenement, String> titreCol;
    @FXML private TableColumn<Evenement, String> typeCol;
    @FXML private TableColumn<Evenement, String> villeCol;
    @FXML private TableColumn<Evenement, String> statutCol;
    @FXML private TableColumn<Evenement, String> organisateurCol;
    @FXML private TableColumn<Evenement, String> descriptionCol;
    @FXML private TableColumn<Evenement, Void> actionsCol;

    @FXML private VBox submenuVBox;
 //   @FXML private Button voirTousBtn;

    //el container mta3 el events
    @FXML private FlowPane cardsContainer;

    /* ===================== FORMULAIRE AJOUT / MODIF ) ===================== */
    @FXML private Button btnAjouterEvent;
    @FXML private Button btnModifierEvent;
    @FXML private Button btnResetEvent;

    @FXML private TextField tfTitreEvent;
    @FXML private TextField tfSlugEvent;
    @FXML private ComboBox<String> cbTypeEvent;
    @FXML private TextArea taDescriptionEvent;
    @FXML private TextArea taObjectifEvent;
    @FXML private ComboBox<String> cbStatutEvent;

    @FXML private TextField tfNomLieuEvent;
    @FXML private TextField tfAdresseEvent;
    @FXML private TextField tfVilleEvent;
    @FXML private CheckBox cbInscriptionObligatoireEvent;
    @FXML private TextField tfEmailContactEvent;

    @FXML private ComboBox<String> cbVisibiliteEvent;

    /* ===================== FORMULAIRE AJOUT / MODIF (ANCIENS fx:id) ===================== */
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private TextField tfMaxParticipants;
    @FXML private DatePicker dpDateLimiteInscription;
    @FXML private CheckBox cbInscriptionObligatoire;
    @FXML private TextField tfEmailContact;
    @FXML private TextField tfTelContact;
    @FXML private TextField tfOrganisateurEvent;
    @FXML private TextField tfImageEvent;
    @FXML private Label frontPageMarker;

    /* ===================== DATA ===================== */
    private final EvenementService evenementService = new EvenementService();
    private final ObservableList<Evenement> masterList = FXCollections.observableArrayList();
    private final DashboardBIServiceEvenement dashboardService = new DashboardBIServiceEvenement();

    private static Evenement evenementAModifier;

    /* ===================== VALIDATION ===================== */
    private Label titreMsg, slugMsg, typeMsg, descriptionMsg, objectifMsg, statutMsg, dateDebutMsg,
            dateFinMsg, nomLieuMsg, adresseMsg, villeMsg, emailMsg, telMsg, organisateurMsg,
            nbParticipantsMsg, dateLimiteMsg, imageMsg, visibiliteMsg;

    private boolean isTitreValid = false;
    private boolean isSlugValid = false;
    private boolean isTypeValid = false;
    private boolean isDescriptionValid = false;
    private boolean isObjectifValid = false;
    private boolean isStatutValid = false;
    private boolean isDateDebutValid = false;
    private boolean isDateFinValid = false;
    private boolean isNomLieuValid = false;
    private boolean isAdresseValid = false;
    private boolean isVilleValid = false;
    private boolean isEmailValid = false;
    private boolean isTelValid = false;
    private boolean isOrganisateurValid = false;
    private boolean isNbParticipantsValid = true;
    private boolean isDateLimiteValid = true;
    private boolean isImageValid = true;
    private boolean isVisibiliteValid = true;
    private boolean isLoading = false;
    private static Evenement evenementSelectionneFront;

    /* ===================== DETAIL FRONT ===================== */
    @FXML private ImageView detailImageView;

    @FXML private Label titreDetailLabel;
    @FXML private Label villeDetailLabel;
    @FXML private Label adresseDetailLabel;
    @FXML private Label visibiliteDetailLabel;

    @FXML private Label descriptionDetailLabel;
    @FXML private Label objectifDetailLabel;

    @FXML private Label organisateurDetailLabel;
    @FXML private Label emailDetailLabel;
    @FXML private Label telDetailLabel;
    @FXML private Label nbPlacesDetailLabel;
    @FXML private Label inscriptionDetailLabel;
    @FXML private Label dateLimiteDetailLabel;

    @FXML private Label typeBadge;
    @FXML private Label statutBadge;
    @FXML private Label dateBadge;
    @FXML private ComboBox<String> filterTypeCombo;
    @FXML private ComboBox<String> filterStatutCombo;
    @FXML private ComboBox<String> filterVilleCombo;




    @FXML
    public void initialize() {
        initDashboardIfExists();
        initFormIfExists();
        initCardsBackIfExists();
        chargerEvenementAModifier();
        initFiltres();
        chargerDetailFrontIfExists();

        try {
            DashboardBIServiceEvenement.DashboardData data =
                    dashboardService.chargerDonnees(null, null);

            if (totalEventsLabel != null)
                totalEventsLabel.setText(String.valueOf(data.totalEvenements));

            if (publiesLabel != null)
                publiesLabel.setText(String.valueOf(data.publies));

            if (brouillonsLabel != null)
                brouillonsLabel.setText(String.valueOf(data.brouillons));

            if (archivesLabel != null)
                archivesLabel.setText(String.valueOf(data.archives));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void initDashboardIfExists() {
        if (evenementTable == null) return;

        configureTable();
        configureSort();
        loadData();
        configureSearch();
        updateStats();
    }

    private void initFormIfExists() {
        if (getTypeCombo() == null) return;

        if (getTypeCombo().getItems().isEmpty()) {
            getTypeCombo().setItems(FXCollections.observableArrayList(
                    "Campagne", "Conférence", "Atelier", "Caritatif", "Autre"
            ));
        }

        if (getStatutCombo() != null && getStatutCombo().getItems().isEmpty()) {
            getStatutCombo().setItems(FXCollections.observableArrayList(
                    "Brouillon", "Publié", "Annulé"
            ));
        }

        if (getVisibiliteCombo() != null && getVisibiliteCombo().getItems().isEmpty()) {
            getVisibiliteCombo().setItems(FXCollections.observableArrayList(
                    "Public", "Privé"
            ));
        }

        installValidationLabels();
        installValidationListeners();
        validateAllFields();
    }

    private void initCardsBackIfExists() {
        if (cardsContainer == null) return;

        try {
            List<Evenement> events = evenementService.recuperer();
            cardsContainer.getChildren().clear();

            boolean isFront = (frontPageMarker != null);

            for (Evenement ev : events) {
                cardsContainer.getChildren().add(
                        isFront ? createEventCardFront(ev) : createEventCardBack(ev)
                );
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }



    private TextField getTitreField() { return tfTitreEvent; }
    private TextField getSlugField() { return tfSlugEvent; }
    private ComboBox<String> getTypeCombo() { return cbTypeEvent; }
    private TextArea getDescriptionArea() { return taDescriptionEvent; }
    private TextArea getObjectifArea() { return taObjectifEvent; }
    private ComboBox<String> getStatutCombo() { return cbStatutEvent; }

    private DatePicker getDateDebutPicker() { return dpDateDebut; }

    private DatePicker getDateFinPicker() { return dpDateFin; }
    private TextField getNomLieuField() { return tfNomLieuEvent; }
    private TextField getAdresseField() { return tfAdresseEvent; }
    private TextField getVilleField() { return tfVilleEvent; }

    private CheckBox getInscriptionObligatoireCheck() {
        return cbInscriptionObligatoireEvent != null ? cbInscriptionObligatoireEvent : cbInscriptionObligatoire;
    }
    private TextField getEmailField() { return tfEmailContact; }

    private TextField getTelField() { return tfTelContact; }

    private TextField getOrganisateurField() { return tfOrganisateurEvent; }
    private TextField getNbParticipantsField() { return tfMaxParticipants; }


    private DatePicker getDateLimitePicker() { return dpDateLimiteInscription; }

    private TextField getImageField() { return tfImageEvent; }

    private ComboBox<String> getVisibiliteCombo() { return cbVisibiliteEvent; }



    private void installValidationLabels() {
        titreMsg = createValidationLabel();
        slugMsg = createValidationLabel();
        typeMsg = createValidationLabel();
        descriptionMsg = createValidationLabel();
        objectifMsg = createValidationLabel();
        statutMsg = createValidationLabel();
        dateDebutMsg = createValidationLabel();
        dateFinMsg = createValidationLabel();
        nomLieuMsg = createValidationLabel();
        adresseMsg = createValidationLabel();
        villeMsg = createValidationLabel();
        emailMsg = createValidationLabel();
        telMsg = createValidationLabel();
        organisateurMsg = createValidationLabel();
        nbParticipantsMsg = createValidationLabel();
        dateLimiteMsg = createValidationLabel();
        imageMsg = createValidationLabel();
        visibiliteMsg = createValidationLabel();

        insertValidationLabel(getTitreField(), titreMsg);
        insertValidationLabel(getSlugField(), slugMsg);
        insertValidationLabel(getTypeCombo(), typeMsg);
        insertValidationLabel(getDescriptionArea(), descriptionMsg);
        insertValidationLabel(getObjectifArea(), objectifMsg);
        insertValidationLabel(getStatutCombo(), statutMsg);
        insertValidationLabel(getDateDebutPicker(), dateDebutMsg);
        insertValidationLabel(getDateFinPicker(), dateFinMsg);
        insertValidationLabel(getNomLieuField(), nomLieuMsg);
        insertValidationLabel(getAdresseField(), adresseMsg);
        insertValidationLabel(getVilleField(), villeMsg);
        insertValidationLabel(getEmailField(), emailMsg);
        insertValidationLabel(getTelField(), telMsg);
        insertValidationLabel(getOrganisateurField(), organisateurMsg);
        insertValidationLabel(getNbParticipantsField(), nbParticipantsMsg);
        insertValidationLabel(getDateLimitePicker(), dateLimiteMsg);
        insertValidationLabel(getImageField(), imageMsg);
        insertValidationLabel(getVisibiliteCombo(), visibiliteMsg);
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
        if (getTitreField() != null) {
            getTitreField().textProperty().addListener((obs, oldVal, newVal) -> {
                validateTitre();
            });
        }

        if (getSlugField() != null) {
            getSlugField().textProperty().addListener((obs, oldVal, newVal) -> validateSlug());
        }

        if (getTypeCombo() != null) {
            getTypeCombo().valueProperty().addListener((obs, oldVal, newVal) -> validateType());
        }

        if (getDescriptionArea() != null) {
            getDescriptionArea().textProperty().addListener((obs, oldVal, newVal) -> validateDescription());
        }

        if (getObjectifArea() != null) {
            getObjectifArea().textProperty().addListener((obs, oldVal, newVal) -> validateObjectif());
        }

        if (getStatutCombo() != null) {
            getStatutCombo().valueProperty().addListener((obs, oldVal, newVal) -> validateStatut());
        }

        if (getDateDebutPicker() != null) {
            getDateDebutPicker().valueProperty().addListener((obs, oldVal, newVal) -> {
                validateDateDebut();
                validateDateFin();
                validateDateLimite();
            });
        }

        if (getDateFinPicker() != null) {
            getDateFinPicker().valueProperty().addListener((obs, oldVal, newVal) -> {
                validateDateFin();
                validateDateLimite();
            });
        }

        if (getNomLieuField() != null) {
            getNomLieuField().textProperty().addListener((obs, oldVal, newVal) -> validateNomLieu());
        }

        if (getAdresseField() != null) {
            getAdresseField().textProperty().addListener((obs, oldVal, newVal) -> validateAdresse());
        }

        if (getVilleField() != null) {
            getVilleField().textProperty().addListener((obs, oldVal, newVal) -> validateVille());
        }

        if (getEmailField() != null) {
            getEmailField().textProperty().addListener((obs, oldVal, newVal) -> validateEmail());
        }

        if (getTelField() != null) {
            getTelField().textProperty().addListener((obs, oldVal, newVal) -> validateTel());
        }

        if (getOrganisateurField() != null) {
            getOrganisateurField().textProperty().addListener((obs, oldVal, newVal) -> validateOrganisateur());
        }

        if (getNbParticipantsField() != null) {
            getNbParticipantsField().textProperty().addListener((obs, oldVal, newVal) -> validateNbParticipants());
        }

        if (getDateLimitePicker() != null) {
            getDateLimitePicker().valueProperty().addListener((obs, oldVal, newVal) -> validateDateLimite());
        }

        if (getImageField() != null) {
            getImageField().textProperty().addListener((obs, oldVal, newVal) -> validateImage());
        }

        if (getVisibiliteCombo() != null) {
            getVisibiliteCombo().valueProperty().addListener((obs, oldVal, newVal) -> validateVisibilite());
        }
    }
    private void validateAllFields() {
        validateTitre();
        validateSlug();
        validateType();
        validateDescription();
        validateObjectif();
        validateStatut();
        validateDateDebut();
        validateDateFin();
        validateNomLieu();
        validateAdresse();
        validateVille();
        validateEmail();
        validateTel();
        validateOrganisateur();
        validateNbParticipants();
        validateDateLimite();
        validateImage();
        validateVisibilite();
    }

    private void autoGenerateSlug() {
        if (getSlugField() == null || getTitreField() == null) return;

        String titre = safeValue(getTitreField().getText()).trim().toLowerCase(Locale.ROOT);
        String slug = titre.replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");

        getSlugField().setText(slug);
    }

    private void validateTitre() {

        if (getTitreField() == null) return;
        String v = safeValue(getTitreField().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getTitreField(), titreMsg, "Le titre est obligatoire.");
            isTitreValid = false;
        } else if (v.length() < 5) {
            setFieldError(getTitreField(), titreMsg, "Minimum 5 caractères.");
            isTitreValid = false;
        } else if (v.length() > 120) {
            setFieldError(getTitreField(), titreMsg, "Maximum 120 caractères.");
            isTitreValid = false;
        } else {
            setFieldSuccess(getTitreField(), titreMsg, "Titre valide.");
            isTitreValid = true;
        }
    }

    private void validateSlug() {
        if (getSlugField() == null) return;
        String v = safeValue(getSlugField().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getSlugField(), slugMsg, "Le slug est obligatoire.");
            isSlugValid = false;
        } else if (!v.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            setFieldError(getSlugField(), slugMsg, "Slug invalide.");
            isSlugValid = false;
        } else {
            setFieldSuccess(getSlugField(), slugMsg, "Slug valide.");
            isSlugValid = true;
        }
    }

    private void validateType() {
        if (getTypeCombo() == null) return;

        if (getTypeCombo().getValue() == null || getTypeCombo().getValue().isBlank()) {
            setFieldError(getTypeCombo(), typeMsg, "Le type est obligatoire.");
            isTypeValid = false;
        } else {
            setFieldSuccess(getTypeCombo(), typeMsg, "Type valide.");
            isTypeValid = true;
        }
    }

    private void validateDescription() {
        if (getDescriptionArea() == null) return;
        String v = safeValue(getDescriptionArea().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getDescriptionArea(), descriptionMsg, "La description est obligatoire.");
            isDescriptionValid = false;
        } else if (v.length() < 20) {
            setFieldError(getDescriptionArea(), descriptionMsg, "Minimum 20 caractères.");
            isDescriptionValid = false;
        } else if (v.length() > 255) {
            setFieldError(getDescriptionArea(), descriptionMsg, "Maximum 255 caractères.");
            isDescriptionValid = false;
        } else {
            setFieldSuccess(getDescriptionArea(), descriptionMsg, "Description valide.");
            isDescriptionValid = true;
        }
    }

    private void validateObjectif() {
        if (getObjectifArea() == null) return;
        String v = safeValue(getObjectifArea().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getObjectifArea(), objectifMsg, "L'objectif est obligatoire.");
            isObjectifValid = false;
        } else if (v.length() < 10) {
            setFieldError(getObjectifArea(), objectifMsg, "Minimum 10 caractères.");
            isObjectifValid = false;
        } else {
            setFieldSuccess(getObjectifArea(), objectifMsg, "Objectif valide.");
            isObjectifValid = true;
        }
    }

    private void validateStatut() {
        if (getStatutCombo() == null) return;

        if (getStatutCombo().getValue() == null || getStatutCombo().getValue().isBlank()) {
            setFieldError(getStatutCombo(), statutMsg, "Le statut est obligatoire.");
            isStatutValid = false;
        } else {
            setFieldSuccess(getStatutCombo(), statutMsg, "Statut valide.");
            isStatutValid = true;
        }
    }

    private void validateDateDebut() {
        if (getDateDebutPicker() == null) return;

        if (getDateDebutPicker().getValue() == null) {
            setFieldError(getDateDebutPicker(), dateDebutMsg, "La date début est obligatoire.");
            isDateDebutValid = false;
        } else {
            setFieldSuccess(getDateDebutPicker(), dateDebutMsg, "Date début valide.");
            isDateDebutValid = true;
        }
    }

    private void validateDateFin() {
        if (getDateFinPicker() == null) return;

        LocalDate debut = getDateDebutPicker() != null ? getDateDebutPicker().getValue() : null;
        LocalDate fin = getDateFinPicker().getValue();

        if (fin == null) {
            setFieldError(getDateFinPicker(), dateFinMsg, "La date fin est obligatoire.");
            isDateFinValid = false;
        } else if (debut != null && fin.isBefore(debut)) {
            setFieldError(getDateFinPicker(), dateFinMsg, "La date fin doit être après ou égale à la date début.");
            isDateFinValid = false;
        } else {
            setFieldSuccess(getDateFinPicker(), dateFinMsg, "Date fin valide.");
            isDateFinValid = true;
        }
    }

    private void validateNomLieu() {
        if (getNomLieuField() == null) return;
        String v = safeValue(getNomLieuField().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getNomLieuField(), nomLieuMsg, "Le nom du lieu est obligatoire.");
            isNomLieuValid = false;
        } else if (v.length() < 3) {
            setFieldError(getNomLieuField(), nomLieuMsg, "Minimum 3 caractères.");
            isNomLieuValid = false;
        } else {
            setFieldSuccess(getNomLieuField(), nomLieuMsg, "Nom du lieu valide.");
            isNomLieuValid = true;
        }
    }

    private void validateAdresse() {
        if (getAdresseField() == null) return;
        String v = safeValue(getAdresseField().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getAdresseField(), adresseMsg, "L'adresse est obligatoire.");
            isAdresseValid = false;
        } else if (v.length() < 5) {
            setFieldError(getAdresseField(), adresseMsg, "Minimum 5 caractères.");
            isAdresseValid = false;
        } else {
            setFieldSuccess(getAdresseField(), adresseMsg, "Adresse valide.");
            isAdresseValid = true;
        }
    }

    private void validateVille() {
        if (getVilleField() == null) return;
        String v = safeValue(getVilleField().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getVilleField(), villeMsg, "La ville est obligatoire.");
            isVilleValid = false;
        } else if (!v.matches("^[\\p{L}\\s'-]+$")) {
            setFieldError(getVilleField(), villeMsg, "Ville invalide.");
            isVilleValid = false;
        } else {
            setFieldSuccess(getVilleField(), villeMsg, "Ville valide.");
            isVilleValid = true;
        }
    }

    private void validateEmail() {
        if (getEmailField() == null) return;
        String v = safeValue(getEmailField().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getEmailField(), emailMsg, "L'email est obligatoire.");
            isEmailValid = false;
        } else if (!v.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            setFieldError(getEmailField(), emailMsg, "Email invalide.");
            isEmailValid = false;
        } else {
            setFieldSuccess(getEmailField(), emailMsg, "Email valide.");
            isEmailValid = true;
        }
    }

    private void validateTel() {
        if (getTelField() == null) return;
        String v = safeValue(getTelField().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getTelField(), telMsg, "Le téléphone est obligatoire.");
            isTelValid = false;
        } else if (!v.matches("^\\+?\\d{8,15}$")) {
            setFieldError(getTelField(), telMsg, "Téléphone invalide.");
            isTelValid = false;
        } else {
            setFieldSuccess(getTelField(), telMsg, "Téléphone valide.");
            isTelValid = true;
        }
    }

    private void validateOrganisateur() {
        if (getOrganisateurField() == null) return;
        String v = safeValue(getOrganisateurField().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getOrganisateurField(), organisateurMsg, "Le nom de l'organisateur est obligatoire.");
            isOrganisateurValid = false;
        } else if (v.length() < 2) {
            setFieldError(getOrganisateurField(), organisateurMsg, "Minimum 2 caractères.");
            isOrganisateurValid = false;
        } else {
            setFieldSuccess(getOrganisateurField(), organisateurMsg, "Nom organisateur valide.");
            isOrganisateurValid = true;
        }
    }

    private void validateNbParticipants() {
        if (getNbParticipantsField() == null) return;

        String v = safeValue(getNbParticipantsField().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getNbParticipantsField(), nbParticipantsMsg,
                    "Le nombre max de participants est obligatoire.");
            isNbParticipantsValid = false;
            return;
        }

        if (!v.matches("^\\d+$")) {
            setFieldError(getNbParticipantsField(), nbParticipantsMsg,
                    "Le nombre max doit être un entier positif.");
            isNbParticipantsValid = false;
            return;
        }

        try {
            int nb = Integer.parseInt(v);

            if (nb <= 0) {
                setFieldError(getNbParticipantsField(), nbParticipantsMsg,
                        "Le nombre max doit être supérieur à 0.");
                isNbParticipantsValid = false;
                return;
            }

            if (nb > 100000) {
                setFieldError(getNbParticipantsField(), nbParticipantsMsg,
                        "Le nombre max est trop grand.");
                isNbParticipantsValid = false;
                return;
            }

            setFieldSuccess(getNbParticipantsField(), nbParticipantsMsg,
                    "Nombre de participants valide.");
            isNbParticipantsValid = true;

        } catch (NumberFormatException e) {
            setFieldError(getNbParticipantsField(), nbParticipantsMsg,
                    "Le nombre max est invalide.");
            isNbParticipantsValid = false;
        }
    }

    private void validateDateLimite() {
        if (getDateLimitePicker() == null) return;

        LocalDate dateLimite = getDateLimitePicker().getValue();
        LocalDate debut = getDateDebutPicker() != null ? getDateDebutPicker().getValue() : null;

        if (dateLimite == null) {
            getDateLimitePicker().setStyle("");
            dateLimiteMsg.setText("");
            isDateLimiteValid = true;
            return;
        }

        if (debut != null && dateLimite.isAfter(debut)) {
            setFieldError(getDateLimitePicker(), dateLimiteMsg, "La date limite doit être avant ou égale à la date début.");
            isDateLimiteValid = false;
        } else {
            setFieldSuccess(getDateLimitePicker(), dateLimiteMsg, "Date limite valide.");
            isDateLimiteValid = true;
        }
    }

    private void validateImage() {
        if (getImageField() == null) return;

        String v = safeValue(getImageField().getText()).trim();

        if (v.isEmpty()) {
            setFieldError(getImageField(), imageMsg, "L'image de couverture est obligatoire.");
            isImageValid = false;
            return;
        }

        boolean isUrl = v.startsWith("http://") || v.startsWith("https://");
        boolean isLocalFile = new File(v).exists();

        if (!isUrl && !isLocalFile) {
            setFieldError(getImageField(), imageMsg,
                    "L'image doit être une URL valide ou un fichier local existant.");
            isImageValid = false;
            return;
        }

        String lower = v.toLowerCase();

        if (!(lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp"))) {
            setFieldError(getImageField(), imageMsg,
                    "Format image invalide (jpg, jpeg, png, webp).");
            isImageValid = false;
            return;
        }

        setFieldSuccess(getImageField(), imageMsg, "Image de couverture valide.");
        isImageValid = true;
    }
    private void validateVisibilite() {
        if (getVisibiliteCombo() == null) return;

        String visibilite = getVisibiliteCombo().getValue();

        if (visibilite == null || visibilite.trim().isEmpty()) {
            setFieldError(getVisibiliteCombo(), visibiliteMsg,
                    "La visibilité est obligatoire.");
            isVisibiliteValid = false;
            return;
        }

        setFieldSuccess(getVisibiliteCombo(), visibiliteMsg,
                "Visibilité valide.");
        isVisibiliteValid = true;
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

    private boolean isFormValid() {
        validateAllFields();
        return isTitreValid && isSlugValid && isTypeValid && isDescriptionValid && isObjectifValid
                && isStatutValid && isDateDebutValid && isDateFinValid && isNomLieuValid
                && isAdresseValid && isVilleValid && isEmailValid && isTelValid
                && isOrganisateurValid && isNbParticipantsValid && isDateLimiteValid
                && isImageValid && isVisibiliteValid;
    }

    private void showInvalidFieldsMessage() {
        StringBuilder errors = new StringBuilder("Veuillez corriger les champs suivants :\n");

        if (!isTitreValid) errors.append("- Titre\n");
        if (!isSlugValid) errors.append("- Slug\n");
        if (!isTypeValid) errors.append("- Type\n");
        if (!isDescriptionValid) errors.append("- Description\n");
        if (!isObjectifValid) errors.append("- Objectif\n");
        if (!isStatutValid) errors.append("- Statut\n");
        if (!isDateDebutValid) errors.append("- Date début\n");
        if (!isDateFinValid) errors.append("- Date fin\n");
        if (!isNomLieuValid) errors.append("- Nom du lieu\n");
        if (!isAdresseValid) errors.append("- Adresse\n");
        if (!isVilleValid) errors.append("- Ville\n");
        if (!isEmailValid) errors.append("- Email\n");
        if (!isTelValid) errors.append("- Téléphone\n");
        if (!isOrganisateurValid) errors.append("- Organisateur\n");
        if (!isNbParticipantsValid) errors.append("- Nb participants max\n");
        if (!isDateLimiteValid) errors.append("- Date limite inscription\n");
        if (!isImageValid) errors.append("- Image couverture\n");
        if (!isVisibiliteValid) errors.append("- Visibilité\n");

        showAlert(Alert.AlertType.ERROR, "Champs invalides", errors.toString());
    }



    @FXML
    void ajouterEvenement(ActionEvent event) {
        if (!isFormValid()) {
            showInvalidFieldsMessage();
            return;
        }

        try {
            String titre = safeValue(getTitreField().getText()).trim();


            boolean existe = evenementService.evenementExisteDeja(titre);

            if (existe) {
                showAlert(Alert.AlertType.WARNING, "Événement déjà existant",
                        "Un événement avec le même titre .");
                return;
            }

            Evenement ev = new Evenement();
            remplirEvenementDepuisForm(ev);
            ev.setDate_creation_event(new java.util.Date());
            ev.setDate_mise_a_jour_event(new java.util.Date());

            evenementService.ajouter(ev);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Événement ajouté avec succès.");
            viderChamps(event);
            retourListeEvenements(event);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    @FXML
    void modifierEvenement(ActionEvent event) {
        if (evenementAModifier == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun événement sélectionné pour la modification.");
            return;
        }

        if (!isFormValid()) {
            showInvalidFieldsMessage();
            return;
        }

        try {
            String titre = safeValue(getTitreField().getText()).trim();
            Date dateDebut = Date.valueOf(getDateDebutPicker().getValue());

            boolean existe = evenementService.evenementExisteDejaPourModification(
                    evenementAModifier.getId(),
                    titre,
                    dateDebut
            );

            if (existe) {
                showAlert(Alert.AlertType.WARNING, "Événement déjà existant",
                        "Un autre événement avec le même titre et la même date de début existe déjà.");
                return;
            }

            remplirEvenementDepuisForm(evenementAModifier);
            evenementAModifier.setDate_mise_a_jour_event(new java.util.Date());

            evenementService.modifier(evenementAModifier);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Événement modifié avec succès.");
            evenementAModifier = null;
            retourListeEvenements(event);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification : " + e.getMessage());
        }
    }

    private void remplirEvenementDepuisForm(Evenement ev) {
        ev.setTitre_event(getTitreField() != null ? getTitreField().getText().trim() : "");
        ev.setSlug_event(getSlugField() != null ? getSlugField().getText().trim() : "");
        ev.setType_event(getTypeCombo() != null ? getTypeCombo().getValue() : null);
        ev.setDescription_event(getDescriptionArea() != null ? getDescriptionArea().getText().trim() : "");
        ev.setObjectif_event(getObjectifArea() != null ? getObjectifArea().getText().trim() : "");
        ev.setStatut_event(getStatutCombo() != null ? getStatutCombo().getValue() : null);
        ev.setDate_debut_event(getDateDebutPicker() != null && getDateDebutPicker().getValue() != null
                ? Date.valueOf(getDateDebutPicker().getValue()) : null);
        ev.setDate_fin_event(getDateFinPicker() != null && getDateFinPicker().getValue() != null
                ? Date.valueOf(getDateFinPicker().getValue()) : null);
        ev.setNom_lieu_event(getNomLieuField() != null ? getNomLieuField().getText().trim() : "");
        ev.setAdresse_event(getAdresseField() != null ? getAdresseField().getText().trim() : "");
        ev.setVille_event(getVilleField() != null ? getVilleField().getText().trim() : "");
        ev.setInscription_obligatoire_event(getInscriptionObligatoireCheck() != null && getInscriptionObligatoireCheck().isSelected());
        ev.setEmail_contact_event(getEmailField() != null ? getEmailField().getText().trim() : "");
        ev.setTel_contact_event(getTelField() != null ? getTelField().getText().trim() : "");
        ev.setNom_organisateur_event(getOrganisateurField() != null ? getOrganisateurField().getText().trim() : "");

        if (getNbParticipantsField() != null && !safeValue(getNbParticipantsField().getText()).trim().isEmpty()) {
            ev.setNb_participants_max_event(Integer.parseInt(getNbParticipantsField().getText().trim()));
        } else {
            ev.setNb_participants_max_event(0);
        }

        if (getDateLimitePicker() != null && getDateLimitePicker().getValue() != null) {
            ev.setDate_limite_inscription_event(Date.valueOf(getDateLimitePicker().getValue()));
        } else {
            ev.setDate_limite_inscription_event(null);
        }

        ev.setImage_couverture_event(getImageField() != null ? getImageField().getText().trim() : "");
        ev.setVisibilite_event(getVisibiliteCombo() != null ? getVisibiliteCombo().getValue() : null);
    }

    private void chargerEvenementAModifier() {
        if (evenementAModifier == null || getTitreField() == null) return;

        isLoading = true;

        getTitreField().setText(safeValue(evenementAModifier.getTitre_event()));
        if (getSlugField() != null) getSlugField().setText(safeValue(evenementAModifier.getSlug_event()));
        if (getTypeCombo() != null) getTypeCombo().setValue(safeValue(evenementAModifier.getType_event()));
        if (getDescriptionArea() != null) getDescriptionArea().setText(safeValue(evenementAModifier.getDescription_event()));
        if (getObjectifArea() != null) getObjectifArea().setText(safeValue(evenementAModifier.getObjectif_event()));
        if (getStatutCombo() != null) getStatutCombo().setValue(safeValue(evenementAModifier.getStatut_event()));

        if (evenementAModifier.getDate_debut_event() != null && getDateDebutPicker() != null) {
            getDateDebutPicker().setValue(convertToLocalDate(evenementAModifier.getDate_debut_event()));
        }

        if (evenementAModifier.getDate_fin_event() != null && getDateFinPicker() != null) {
            getDateFinPicker().setValue(convertToLocalDate(evenementAModifier.getDate_fin_event()));
        }

        if (getNomLieuField() != null) getNomLieuField().setText(safeValue(evenementAModifier.getNom_lieu_event()));
        if (getAdresseField() != null) getAdresseField().setText(safeValue(evenementAModifier.getAdresse_event()));
        if (getVilleField() != null) getVilleField().setText(safeValue(evenementAModifier.getVille_event()));

        if (getInscriptionObligatoireCheck() != null) {
            getInscriptionObligatoireCheck().setSelected(evenementAModifier.isInscription_obligatoire_event());
        }

        if (getEmailField() != null) getEmailField().setText(safeValue(evenementAModifier.getEmail_contact_event()));
        if (getTelField() != null) getTelField().setText(safeValue(evenementAModifier.getTel_contact_event()));
        if (getOrganisateurField() != null) getOrganisateurField().setText(safeValue(evenementAModifier.getNom_organisateur_event()));

        if (getNbParticipantsField() != null) {
            getNbParticipantsField().setText(String.valueOf(evenementAModifier.getNb_participants_max_event()));
        }

        if (evenementAModifier.getDate_limite_inscription_event() != null && getDateLimitePicker() != null) {
            getDateLimitePicker().setValue(convertToLocalDate(evenementAModifier.getDate_limite_inscription_event()));
        }

        if (getImageField() != null) getImageField().setText(safeValue(evenementAModifier.getImage_couverture_event()));
        if (getVisibiliteCombo() != null) getVisibiliteCombo().setValue(safeValue(evenementAModifier.getVisibilite_event()));

        isLoading = false;
    }
    private LocalDate convertToLocalDate(java.util.Date date) {
        if (date == null) return null;


        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }


        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    @FXML
    public void viderChamps(ActionEvent event) {
        if (getTitreField() != null) getTitreField().clear();
        if (getSlugField() != null) getSlugField().clear();
        if (getTypeCombo() != null) getTypeCombo().setValue(null);
        if (getDescriptionArea() != null) getDescriptionArea().clear();
        if (getObjectifArea() != null) getObjectifArea().clear();
        if (getStatutCombo() != null) getStatutCombo().setValue(null);
        if (getDateDebutPicker() != null) getDateDebutPicker().setValue(null);
        if (getDateFinPicker() != null) getDateFinPicker().setValue(null);
        if (getNomLieuField() != null) getNomLieuField().clear();
        if (getAdresseField() != null) getAdresseField().clear();
        if (getVilleField() != null) getVilleField().clear();
        if (getInscriptionObligatoireCheck() != null) getInscriptionObligatoireCheck().setSelected(false);
        if (getEmailField() != null) getEmailField().clear();
        if (getTelField() != null) getTelField().clear();
        if (getOrganisateurField() != null) getOrganisateurField().clear();
        if (getNbParticipantsField() != null) getNbParticipantsField().clear();
        if (getDateLimitePicker() != null) getDateLimitePicker().setValue(null);
        if (getImageField() != null) getImageField().clear();
        if (getVisibiliteCombo() != null) getVisibiliteCombo().setValue(null);

        validateAllFields();
    }

    /* =========================================================
       ===================== DASHBOARD ==========================
       ========================================================= */

    private void configureTable() {
        titreCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("titre_event"));
        typeCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("type_event"));
        villeCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("ville_event"));
        statutCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("statut_event"));
        organisateurCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nom_organisateur_event"));

        descriptionCol.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescription_event();
            if (desc == null) desc = "";
            if (desc.length() > 45) desc = desc.substring(0, 45) + "...";
            return new SimpleStringProperty(desc);
        });

        addActionsColumn();
        evenementTable.setPlaceholder(new Label("Aucun événement trouvé"));
    }
    private void configureSort() {
        if (sortCombo == null) return;

        sortCombo.setItems(FXCollections.observableArrayList(
                "Titre A-Z", "Titre Z-A", "Ville A-Z", "Ville Z-A", "Statut", "Type", "Organisateur"
        ));

        sortCombo.setOnAction(e -> appliquerRechercheFiltreTri());
    }
    private void loadData() {
        try {
            masterList.setAll(evenementService.recuperer());

            if (evenementTable != null) {
                evenementTable.setItems(masterList);
            }

            if (inventoryCountLabel != null) {
                inventoryCountLabel.setText(masterList.size() + " événement(s)");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void configureSearch() {
        if (searchField == null) return;

        searchField.textProperty().addListener((obs, oldVal, newVal) -> appliquerRechercheFiltreTri());

        if (filterTypeCombo != null) {
            filterTypeCombo.setOnAction(e -> appliquerRechercheFiltreTri());
        }

        if (filterStatutCombo != null) {
            filterStatutCombo.setOnAction(e -> appliquerRechercheFiltreTri());
        }

        if (filterVilleCombo != null) {
            filterVilleCombo.setOnAction(e -> appliquerRechercheFiltreTri());
        }
    }

    private void appliquerRechercheFiltreTri() {
        try {
            List<Evenement> resultat = evenementService.recuperer();

            String keyword = searchField != null ? searchField.getText() : "";
            String type = filterTypeCombo != null ? filterTypeCombo.getValue() : "";
            String statut = filterStatutCombo != null ? filterStatutCombo.getValue() : "";
            String ville = filterVilleCombo != null ? filterVilleCombo.getValue() : "";
            String tri = sortCombo != null ? sortCombo.getValue() : "";

            resultat = evenementService.rechercherEvenements(resultat, keyword);
            resultat = evenementService.filtrerEvenements(resultat, type, statut, ville);
            resultat = evenementService.trierEvenements(resultat, tri);

            masterList.setAll(resultat);

            if (evenementTable != null) {
                evenementTable.setItems(masterList);
            }

            updateStats();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }
    private void updateStats() {
        int total = masterList.size();
        int publies = 0;
        int brouillons = 0;
        int archives = 0;

        for (Evenement e : masterList) {
            String statut = safeValue(e.getStatut_event()).toLowerCase(Locale.ROOT);
            if (statut.contains("publi")) publies++;
            else if (statut.contains("brouillon")) brouillons++;
            else archives++;
        }

        if (totalEventsLabel != null) totalEventsLabel.setText(String.valueOf(total));
        if (publiesLabel != null) publiesLabel.setText(String.valueOf(publies));
        if (brouillonsLabel != null) brouillonsLabel.setText(String.valueOf(brouillons));
        if (archivesLabel != null) archivesLabel.setText(String.valueOf(archives));
        if (inventoryCountLabel != null) inventoryCountLabel.setText(total + " événement(s)");
    }

    private void addActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("Voir");
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final ToolBar toolBar = new ToolBar(viewBtn, editBtn, deleteBtn);

            {
                toolBar.getStyleClass().add("action-toolbar");
                viewBtn.getStyleClass().add("view-btn");
                editBtn.getStyleClass().add("edit-btn");
                deleteBtn.getStyleClass().add("delete-btn");

                viewBtn.setOnAction(e -> ouvrirPageCardsBack());

                editBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());
                    ouvrirPageModification(ev);
                });

                deleteBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Supprimer l'événement : " + safeValue(ev.getTitre_event()) + " ?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        evenementService.supprimer(ev);
                        masterList.remove(ev);
                        updateStats();
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

    /* =========================================================
       ===================== CARDS ==============================
       ========================================================= */

    private VBox createEventCardBack(Evenement ev) {
        VBox card = new VBox(14);
        card.setPrefWidth(320);
        card.setMinWidth(320);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: #dfe6ee;" +
                        "-fx-border-radius: 22;" +
                        "-fx-padding: 0;"
        );

        DropShadow baseShadow = new DropShadow();
        baseShadow.setRadius(12);
        baseShadow.setSpread(0.08);
        baseShadow.setOffsetY(3);
        baseShadow.setColor(Color.rgb(17, 34, 68, 0.10));
        card.setEffect(baseShadow);

        card.setOnMouseEntered(e -> {
            DropShadow hoverShadow = new DropShadow();
            hoverShadow.setRadius(26);
            hoverShadow.setSpread(0.12);
            hoverShadow.setOffsetY(10);
            hoverShadow.setColor(Color.rgb(18, 152, 183, 0.28));
            card.setEffect(hoverShadow);
            card.setTranslateY(-6);
        });

        card.setOnMouseExited(e -> {
            card.setEffect(baseShadow);
            card.setTranslateY(0);
        });

        StackPane imageHeader = buildEventImageHeader(ev);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 0 18 18 18;");

        Label titre = new Label(valueOrDash(ev.getTitre_event()));
        titre.setWrapText(true);
        titre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0b2c6f;");

        Label type = new Label("🏷 Type : " + valueOrDash(ev.getType_event()));
        Label ville = new Label("📍 Ville : " + valueOrDash(ev.getVille_event()));
        Label statut = new Label("✔ Statut : " + valueOrDash(ev.getStatut_event()));
        Label organisateur = new Label("👤 Organisateur : " + valueOrDash(ev.getNom_organisateur_event()));

        Label desc = new Label(shortText(valueOrDash(ev.getDescription_event()), 110));
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #5f6f86; -fx-font-size: 13px;");

        for (Label meta : List.of(type, ville, statut, organisateur)) {
            meta.setWrapText(true);
            meta.setStyle("-fx-text-fill: #4c5f7a; -fx-font-size: 13px; -fx-font-weight: 600;");
        }

        Button detailsBtn = new Button("Details");
        detailsBtn.setMaxWidth(Double.MAX_VALUE);
        detailsBtn.setStyle(
                "-fx-background-color: #11a8c9;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 10 16;" +
                        "-fx-font-weight: bold;"
        );
        detailsBtn.setOnAction(e -> afficherPopupDetailsSimple(ev));

        content.getChildren().addAll(titre, type, ville, statut, organisateur, desc, detailsBtn);
        card.getChildren().addAll(imageHeader, content);

        return card;
    }

    private StackPane buildEventImageHeader(Evenement ev) {
        StackPane imageHeader = new StackPane();
        imageHeader.setPrefHeight(180);
        imageHeader.setMinHeight(180);
        imageHeader.setStyle(
                "-fx-background-color: linear-gradient(to right, #dff7fb, #dce8ff);" +
                        "-fx-background-radius: 22 22 0 0;"
        );

        Image image = loadEventImage(ev.getImage_couverture_event());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(320);
            imageView.setFitHeight(180);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imageHeader.getChildren().add(imageView);
        } else {
            Label placeholder = new Label("Aucune image");
            placeholder.setStyle(
                    "-fx-text-fill: #2e6f8d;" +
                            "-fx-font-size: 18px;" +
                            "-fx-font-weight: bold;"
            );
            imageHeader.getChildren().add(placeholder);
        }

        Label badge = new Label(valueOrDash(ev.getVisibilite_event()));
        badge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.92);" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 6 12;" +
                        "-fx-text-fill: #0b2c6f;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;"
        );
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(14, 14, 0, 0));
        imageHeader.getChildren().add(badge);

        return imageHeader;
    }

    private Image loadEventImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return null;

        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://") || imagePath.startsWith("file:/")) {
                return new Image(imagePath, true);
            }

            File file = new File(imagePath);
            if (file.exists()) {
                return new Image(file.toURI().toString(), true);
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private void afficherPopupDetailsSimple(Evenement ev) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Details evenement");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setMinWidth(430);
        dialog.getDialogPane().setPrefWidth(430);

        VBox modalCard = new VBox(18);
        modalCard.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 24;" +
                        "-fx-padding: 24;" +
                        "-fx-border-color: #e6eef5;" +
                        "-fx-border-radius: 24;"
        );

        Label overline = new Label("Event details");
        overline.setStyle("-fx-text-fill: #11a8c9; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label title = new Label(valueOrDash(ev.getTitre_event()));
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #16325c; -fx-font-size: 24px; -fx-font-weight: bold;");

        VBox metaBox = new VBox(10,
                createDetailRow("Type", valueOrDash(ev.getType_event())),
                createDetailRow("City", valueOrDash(ev.getVille_event())),
                createDetailRow("Status", valueOrDash(ev.getStatut_event())),
                createDetailRow("Organizer", valueOrDash(ev.getNom_organisateur_event()))
        );
        metaBox.setStyle("-fx-background-color: #f7fbfd; -fx-background-radius: 18; -fx-padding: 16;");

        Label descriptionTitle = new Label("Description");
        descriptionTitle.setStyle("-fx-text-fill: #16325c; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label description = new Label(valueOrDash(ev.getDescription_event()));
        description.setWrapText(true);
        description.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-border-color: #e6eef5;" +
                        "-fx-border-radius: 16;" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 14;" +
                        "-fx-text-fill: #5b6b84;" +
                        "-fx-font-size: 13px;"
        );

        modalCard.getChildren().addAll(overline, title, metaBox, descriptionTitle, description);
        dialog.getDialogPane().setContent(modalCard);

        Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setText("Close");
        closeButton.setStyle(
                "-fx-background-color: #11a8c9;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 10 22;"
        );

        dialog.showAndWait();
    }

    private HBox createDetailRow(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: #7a8aa2; -fx-font-size: 12px; -fx-font-weight: bold;");
        label.setMinWidth(82);

        Label value = new Label(valueText);
        value.setWrapText(true);
        value.setMaxWidth(Double.MAX_VALUE);
        value.setStyle("-fx-text-fill: #183153; -fx-font-size: 13px; -fx-font-weight: 600;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new HBox(10, label, spacer, value);
    }

    private void ouvrirPageCardsBack() {
        loadScene("/EvenementCardsBack.fxml", "Événements - Vue Cards");
    }



    @FXML
    private void toggleSubmenu() {
        if (submenuVBox == null || evenementArrow == null) return;

        boolean show = !submenuVBox.isVisible();
        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);
        evenementArrow.setText(show ? "⌃" : "⌄");
    }

    @FXML
    private void onVoirTousEvenements() {
        retourListeEvenements(null);
    }

    @FXML
    private void onTableauBordAdmin() {
        showAlert(Alert.AlertType.INFORMATION, "Dashboard Admin BI", "Tu peux relier ici le dashboard Admin BI.");
    }

    @FXML
    private void onAutresActions() {
        loadScene("/RessourceDashboard.fxml", "Gestion des Ressources");
    }

    @FXML
    private void onNewEvent() {
        loadScene("/AjouterEvenement.fxml", "Nouvel événement");
    }

    @FXML
    private void retourListeEvenements(ActionEvent event) {
        evenementAModifier = null;
        loadScene("/EvenementDashboard.fxml", "Gestion des Événements");
    }
    private void ouvrirPageModification(Evenement ev) {
        try {
            evenementAModifier = evenementService.recupererParId(ev.getId());

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ModifierEvenement.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) evenementTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifier événement");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur navigation", e.getMessage());
        }
    }

    private void loadScene(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = resolveCurrentStage();

            if (stage == null) {
                throw new Exception("Impossible de récupérer la fenêtre actuelle.");
            }

            applySceneToStage(stage, root, title);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation échouée : " + e.getMessage());
        }
    }

    private Stage resolveCurrentStage() {
        if (evenementTable != null && evenementTable.getScene() != null) {
            return (Stage) evenementTable.getScene().getWindow();
        }
        if (getTitreField() != null && getTitreField().getScene() != null) {
            return (Stage) getTitreField().getScene().getWindow();
        }
        if (cardsContainer != null && cardsContainer.getScene() != null) {
            return (Stage) cardsContainer.getScene().getWindow();
        }
        if (btnAjouterEvent != null && btnAjouterEvent.getScene() != null) {
            return (Stage) btnAjouterEvent.getScene().getWindow();
        }
        if (btnModifierEvent != null && btnModifierEvent.getScene() != null) {
            return (Stage) btnModifierEvent.getScene().getWindow();
        }
        if (btnResetEvent != null && btnResetEvent.getScene() != null) {
            return (Stage) btnResetEvent.getScene().getWindow();
        }
        return null;
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




    //FronT
    private void ouvrirDetailEvenementFront(Evenement ev) {
        try {
            evenementSelectionneFront = ev;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/EvenementDetail.fxml"));
            Parent root = loader.load();

            Stage stage = null;

            if (cardsContainer != null && cardsContainer.getScene() != null) {
                stage = (Stage) cardsContainer.getScene().getWindow();
            } else {
                stage = resolveCurrentStage();
            }

            if (stage != null) {
                stage.setScene(new Scene(root, 1400, 820));
                stage.setTitle("Détail Événement");
                stage.show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le détail front : " + e.getMessage());
        }
    }

    private void chargerDetailFrontIfExists() {
        if (titreDetailLabel == null || evenementSelectionneFront == null) return;

        Evenement ev = evenementSelectionneFront;

        titreDetailLabel.setText(valueOrDash(ev.getTitre_event()));
        villeDetailLabel.setText("📍 " + valueOrDash(ev.getVille_event()));
        adresseDetailLabel.setText("📌 " + valueOrDash(ev.getAdresse_event()));
        visibiliteDetailLabel.setText("👁 " + valueOrDash(ev.getVisibilite_event()));

        descriptionDetailLabel.setText(valueOrDash(ev.getDescription_event()));
        objectifDetailLabel.setText(valueOrDash(ev.getObjectif_event()));

        organisateurDetailLabel.setText("👤 " + valueOrDash(ev.getNom_organisateur_event()));
        emailDetailLabel.setText("✉ " + valueOrDash(ev.getEmail_contact_event()));
        telDetailLabel.setText("☎ " + valueOrDash(ev.getTel_contact_event()));
        nbPlacesDetailLabel.setText("👥 Participants max : " + ev.getNb_participants_max_event());
        inscriptionDetailLabel.setText("📝 Inscription obligatoire : " + (ev.isInscription_obligatoire_event() ? "Oui" : "Non"));

        if (ev.getDate_limite_inscription_event() != null) {
            dateLimiteDetailLabel.setText("⏳ Date limite inscription : " + ev.getDate_limite_inscription_event().toString());
        } else {
            dateLimiteDetailLabel.setText("⏳ Date limite inscription : -");
        }

        if (typeBadge != null) {
            typeBadge.setText(valueOrDash(ev.getType_event()));
        }

        if (statutBadge != null) {
            statutBadge.setText(valueOrDash(ev.getStatut_event()));
        }

        if (dateBadge != null) {
            String debut = ev.getDate_debut_event() != null ? ev.getDate_debut_event().toString() : "-";
            String fin = ev.getDate_fin_event() != null ? ev.getDate_fin_event().toString() : "-";
            dateBadge.setText(debut + " → " + fin);
        }

        if (detailImageView != null) {
            Image image = loadEventImage(ev.getImage_couverture_event());
            if (image != null) {
                detailImageView.setImage(image);
            }
        }
    }
    private void initFiltres() {
        if (filterTypeCombo != null) {
            filterTypeCombo.setItems(FXCollections.observableArrayList(
                    "Tous", "Campagne", "Conférence", "Atelier", "Caritatif", "Autre"
            ));
            filterTypeCombo.setValue("Tous");
        }

        if (filterStatutCombo != null) {
            filterStatutCombo.setItems(FXCollections.observableArrayList(
                    "Tous", "Publié", "Brouillon", "Annulé"
            ));
            filterStatutCombo.setValue("Tous");
        }

        if (filterVilleCombo != null) {
            filterVilleCombo.setItems(FXCollections.observableArrayList(
                    "Toutes", "Tunis", "Sfax", "Sousse", "Ariana"
            ));
            filterVilleCombo.setValue("Toutes");
        }
    }

    @FXML
    private void retourVersListeFront() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontFXML/Evenement.fxml"));
            Stage stage = resolveCurrentStage();

            if (stage != null) {
                stage.setScene(new Scene(root, 1400, 820));
                stage.setTitle("Événements");
                stage.show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir à la liste : " + e.getMessage());
        }
    }


    private VBox createEventCardFront(Evenement ev) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: rgba(14,143,176,0.18);" +
                        "-fx-border-radius: 22;" +
                        "-fx-padding: 0 0 18 0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 18, 0.15, 0, 6);"
        );

        StackPane imageHeader = new StackPane();
        imageHeader.setPrefHeight(180);
        imageHeader.setStyle("-fx-background-color: linear-gradient(to right, #dff7fb, #dce8ff); -fx-background-radius: 22 22 0 0;");

        Image image = loadEventImage(ev.getImage_couverture_event());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(320);
            imageView.setFitHeight(180);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imageHeader.getChildren().add(imageView);
        } else {
            Label noImage = new Label("Aucune image");
            noImage.setStyle("-fx-text-fill: #2e6f8d; -fx-font-size: 18px; -fx-font-weight: bold;");
            imageHeader.getChildren().add(noImage);
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(14, 16, 0, 16));

        Label titre = new Label(valueOrDash(ev.getTitre_event()));
        titre.setWrapText(true);
        titre.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #153946;");

        Label ville = new Label("📍 " + valueOrDash(ev.getVille_event()));
        ville.setStyle("-fx-text-fill: #4d6d78; -fx-font-size: 13px;");

        Label date = new Label("📅 " + (ev.getDate_debut_event() != null ? ev.getDate_debut_event().toString() : "-"));
        date.setStyle("-fx-text-fill: #4d6d78; -fx-font-size: 13px;");

        Label desc = new Label(shortText(valueOrDash(ev.getDescription_event()), 90));
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #5f6f86; -fx-font-size: 13px;");

        HBox actions = new HBox();
        actions.setPadding(new Insets(8, 16, 0, 16));

        Button detailsBtn = new Button("Détails");
        detailsBtn.getStyleClass().add("confirm-button");
        detailsBtn.setOnAction(e -> ouvrirDetailEvenementFront(ev));

        actions.getChildren().add(detailsBtn);

        content.getChildren().addAll(titre, ville, date, desc);
        card.getChildren().addAll(imageHeader, content, actions);

        return card;
    }
    @FXML
    private void handleExportPDF() {
        try {
            DashboardBIServiceEvenement service = new DashboardBIServiceEvenement();

            DashboardBIServiceEvenement.DashboardData data =
                    service.chargerDonnees(null, null);

            // 🔥 simple PDF (version basique)
            FileWriter writer = new FileWriter("evenements.pdf");

            writer.write("===== RAPPORT EVENEMENTS =====\n\n");
            writer.write("Total: " + data.totalEvenements + "\n");
            writer.write("Publies: " + data.publies + "\n");
            writer.write("Brouillons: " + data.brouillons + "\n");
            writer.write("Archives: " + data.archives + "\n");

            writer.close();

            showAlert(Alert.AlertType.INFORMATION, "Succès", "PDF généré avec succès !");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur génération PDF");
        }
    }
    @FXML
    private void choisirImageDepuisPC() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        Stage stage = resolveCurrentStage();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            tfImageEvent.setText(file.getAbsolutePath());
        }
    }
    private void initCardsFrontIfExists() {
        if (cardsContainer == null) return;


        try {
            List<Evenement> events = evenementService.recuperer();
            cardsContainer.getChildren().clear();

            for (Evenement ev : events) {
                cardsContainer.getChildren().add(createEventCardFront(ev));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }


    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private String shortText(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max) + "...";
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}