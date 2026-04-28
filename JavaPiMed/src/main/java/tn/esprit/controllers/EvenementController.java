package tn.esprit.controllers;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Ressource;
import tn.esprit.entities.User;
import tn.esprit.services.AccessibilityRoomService;
import tn.esprit.services.AiCancellationCareService;
import tn.esprit.services.AiEventIntelligenceService;
import tn.esprit.services.CloudinaryEventUploadService;
import tn.esprit.services.HuggingFaceCareAiClient;
import tn.esprit.services.TwilioSmsServiceEvent;
import tn.esprit.services.DashboardBIServiceEvenement;
import tn.esprit.services.EvenementService;
import tn.esprit.services.EventLocationService;
import tn.esprit.services.GroqEventAiClient;
import tn.esprit.services.ParticipationDemandeService;
import tn.esprit.services.ParticipationDemandeService.ParticipationDemande;
import tn.esprit.tools.SessionManager;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javafx.stage.FileChooser;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import tn.esprit.services.EvenementPDFService;


public class EvenementController {

    private static final double DEFAULT_SCENE_WIDTH = 1650;
    private static final double DEFAULT_SCENE_HEIGHT = 960;


    /* ===================== DASHBOARD ===================== */
    @FXML private Label totalEventsLabel;
    @FXML private Label publiesLabel;
    @FXML private Label brouillonsLabel;
    @FXML private Label archivesLabel;
    @FXML private Label inventoryCountLabel;
    @FXML private HBox participationAlertBox;
    @FXML private Label participationAlertCountLabel;
    @FXML private Label participationAlertMessageLabel;
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
    @FXML private TextArea taMotifAnnulationEvent;
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
    @FXML private Label biPeriodeLabel;

    @FXML private DatePicker biDateDebutPicker;
    @FXML private DatePicker biDateFinPicker;
    @FXML private ComboBox<String> biVilleCombo;

    @FXML private Label biPublicationLabel;
    @FXML private Label biCapaciteLabel;

    @FXML private Label biTotalEventsLabel;
    @FXML private Label biPubliesLabel;
    @FXML private Label biCapaciteTotaleLabel;
    @FXML private Label biTauxPublicationLabel;

    @FXML private Label biBrouillonsLabel;
    @FXML private Label biAnnulesLabel;
    @FXML private Label biAVenirLabel;
    @FXML private Label biPeriodeAnalyseeLabel;

    @FXML private LineChart<String, Number> eventsLineChart;
    @FXML private PieChart statutPieChart;
    @FXML private BarChart<String, Number> typeBarChart;
    @FXML private BarChart<String, Number> villeBarChart;

    @FXML private TableView<Evenement> topEvenementsTable;
    @FXML private TableColumn<Evenement, String> topTitreCol;
    @FXML private TableColumn<Evenement, String> topTypeCol;
    @FXML private TableColumn<Evenement, String> topVilleCol;
    @FXML private TableColumn<Evenement, Number> topCapaciteCol;
    @FXML private TableColumn<Evenement, String> topStatutCol;

    @FXML private FlowPane recommandationsContainer;
    @FXML private Label aiBackEventTitleLabel;
    @FXML private Label aiBackEventMetaLabel;
    @FXML private Label aiBackStatusLabel;
    @FXML private Label aiBackRiskScoreLabel;
    @FXML private Label aiBackRiskLevelLabel;
    @FXML private ProgressBar aiBackRiskProgress;
    @FXML private VBox aiBackReasonsContainer;
    @FXML private VBox aiBackSuggestionsContainer;
    @FXML private FlowPane aiBackRecommendationsContainer;
    @FXML private TextArea aiBackSummaryArea;

    /* ===================== DATA ===================== */
    private final EvenementService evenementService = new EvenementService();
    private final ParticipationDemandeService participationService = new ParticipationDemandeService();
    private final TwilioSmsServiceEvent smsServiceEvent = new TwilioSmsServiceEvent();
    private final AiCancellationCareService cancellationCareService = new AiCancellationCareService();
    private final AiEventIntelligenceService aiEventIntelligenceService = new AiEventIntelligenceService();
    private final GroqEventAiClient groqEventAiClient = new GroqEventAiClient();
    private final HuggingFaceCareAiClient huggingFaceCareAiClient = new HuggingFaceCareAiClient();
    private final CloudinaryEventUploadService cloudinaryEventUploadService = new CloudinaryEventUploadService();
    private final ObservableList<Evenement> masterList = FXCollections.observableArrayList();
    private final DashboardBIServiceEvenement dashboardService = new DashboardBIServiceEvenement();
    private final EvenementPDFService pdfService = new EvenementPDFService();
    private final EventLocationService eventLocationService = new EventLocationService();
    private final AccessibilityRoomService accessibilityRoomService = new AccessibilityRoomService();

    private static Evenement evenementAModifier;
    private static Evenement evenementSelectionneAiBack;
    private static boolean publicationMode;

    /* ===================== VALIDATION ===================== */
    private Label titreMsg, slugMsg, typeMsg, descriptionMsg, objectifMsg, statutMsg, motifAnnulationMsg, dateDebutMsg,
            dateFinMsg, nomLieuMsg, adresseMsg, villeMsg, emailMsg, telMsg, organisateurMsg,
            nbParticipantsMsg, dateLimiteMsg, imageMsg, visibiliteMsg;

    private boolean isTitreValid = false;
    private boolean isSlugValid = false;
    private boolean isTypeValid = false;
    private boolean isDescriptionValid = false;
    private boolean isObjectifValid = false;
    private boolean isStatutValid = false;
    private boolean isMotifAnnulationValid = true;
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
    @FXML private Label calendarMonthLabel;
    @FXML private Label calendarSelectionHintLabel;
    @FXML private GridPane calendarWeekHeader;
    @FXML private GridPane calendarMonthGrid;
    @FXML private ImageView mapImageView;
    @FXML private Label mapStatusLabel;
    @FXML private Label weatherConditionLabel;
    @FXML private Label weatherTempLabel;
    @FXML private Label weatherHumidityLabel;
    @FXML private Label weatherWindLabel;
    @FXML private FlowPane aiFrontRecommendationsContainer;
    @FXML private FlowPane accessibilityResourcesContainer;
    @FXML private Label accessibilityStatusLabel;
    @FXML private Label aiFrontStatusLabel;
    @FXML private FlowPane participationsContainer;
    @FXML private Label participationSummaryLabel;
    @FXML private VBox eventUpdateToast;
    private EventLocationService.EventLocation currentEventLocation;
    private int currentMapZoom = 16;
    private Timeline participationCareTimeline;
    private final Set<Integer> displayedCancellationCareEvents = new HashSet<>();
    private boolean eventUpdateToastAnimated;
    private boolean floatingEventUpdatePopupShown;
    private YearMonth currentCalendarMonth = YearMonth.now();
    private Evenement draggedCalendarEvent;




    @FXML
    public void initialize() {
        initDashboardIfExists();
        initCalendarSection();
        initFormIfExists();
        initCardsBackIfExists();
        chargerEvenementAModifier();
        initFiltres();
        chargerDetailFrontIfExists();
        initMesParticipationsIfExists();
        initDashboardBIIfExists();
        initAiBackPageIfExists();

        try {
            DashboardBIServiceEvenement.DashboardData data =
                    dashboardService.chargerDonnees(null, null, "Toutes les villes");

            if (totalEventsLabel != null)
                totalEventsLabel.setText(String.valueOf(data.totalEvenements));

            if (publiesLabel != null)
                publiesLabel.setText(String.valueOf(data.publies));

            if (brouillonsLabel != null)
                brouillonsLabel.setText(String.valueOf(data.brouillons));

            if (archivesLabel != null)
                archivesLabel.setText(String.valueOf(data.autres + data.annules));

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
            if (isFront) {
                events = events.stream().filter(this::isVisibleOnFront).toList();
                updateFrontCancellationToast();
            }

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
    private TextArea getMotifAnnulationArea() { return taMotifAnnulationEvent; }
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
        motifAnnulationMsg = createValidationLabel();
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
        insertValidationLabel(getMotifAnnulationArea(), motifAnnulationMsg);
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

        if (getMotifAnnulationArea() != null) {
            getMotifAnnulationArea().textProperty().addListener((obs, oldVal, newVal) -> validateMotifAnnulation());
        }

        if (getStatutCombo() != null) {
            getStatutCombo().valueProperty().addListener((obs, oldVal, newVal) -> {
                syncDraftVisibility();
                validateStatut();
                validateMotifAnnulation();
                validateDescription();
                validateObjectif();
                validateImage();
                validateVisibilite();
            });
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
            getVisibiliteCombo().valueProperty().addListener((obs, oldVal, newVal) -> {
                validateVisibilite();
                validateDescription();
                validateObjectif();
                validateImage();
            });
        }
    }
    private void validateAllFields() {
        validateTitre();
        validateSlug();
        validateType();
        validateDescription();
        validateObjectif();
        validateStatut();
        validateMotifAnnulation();
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

        if (isDraftWorkflow()) {
            clearFieldState(getDescriptionArea(), descriptionMsg, "Description optionnelle pour un brouillon.");
            isDescriptionValid = true;
            return;
        }

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

        if (isDraftWorkflow()) {
            clearFieldState(getObjectifArea(), objectifMsg, "Objectif optionnel pour un brouillon.");
            isObjectifValid = true;
            return;
        }

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

    private void validateMotifAnnulation() {
        if (getMotifAnnulationArea() == null) {
            isMotifAnnulationValid = true;
            return;
        }

        if (evenementAModifier == null) {
            isMotifAnnulationValid = true;
            return;
        }

        String motif = safeValue(getMotifAnnulationArea().getText()).trim();
        if (!isCancelledStatusSelected()) {
            clearFieldState(getMotifAnnulationArea(), motifAnnulationMsg, "Motif requis seulement si l'evenement est annule.");
            isMotifAnnulationValid = true;
            return;
        }

        if (motif.isEmpty()) {
            setFieldError(getMotifAnnulationArea(), motifAnnulationMsg, "Expliquez pourquoi l'evenement est annule.");
            isMotifAnnulationValid = false;
        } else if (motif.length() < 10) {
            setFieldError(getMotifAnnulationArea(), motifAnnulationMsg, "Minimum 10 caracteres.");
            isMotifAnnulationValid = false;
        } else if (motif.length() > 1000) {
            setFieldError(getMotifAnnulationArea(), motifAnnulationMsg, "Maximum 1000 caracteres.");
            isMotifAnnulationValid = false;
        } else {
            setFieldSuccess(getMotifAnnulationArea(), motifAnnulationMsg, "Motif d'annulation valide.");
            isMotifAnnulationValid = true;
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

        if (isDraftWorkflow() && v.isEmpty()) {
            clearFieldState(getImageField(), imageMsg, "Image optionnelle pour un brouillon.");
            isImageValid = true;
            return;
        }

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
    private void clearFieldState(Control field, Label msgLabel, String message) {
        if (field != null) field.setStyle("");
        if (msgLabel != null) {
            msgLabel.setText(message);
            msgLabel.setStyle("-fx-text-fill: #5f6f86; -fx-font-size: 11px;");
        }
    }

    private boolean isDraftStatusSelected() {
        String statut = getStatutCombo() != null ? safeValue(getStatutCombo().getValue()) : "";
        return statut.toLowerCase(Locale.ROOT).contains("brouillon");
    }

    private boolean isCancelledStatusSelected() {
        String statut = getStatutCombo() != null ? safeValue(getStatutCombo().getValue()) : "";
        String normalized = statut.toLowerCase(Locale.ROOT);
        return normalized.contains("annul") || normalized.contains("cancel");
    }

    private boolean isDraftWorkflow() {
        String visibilite = getVisibiliteCombo() != null ? safeValue(getVisibiliteCombo().getValue()) : "";
        return isDraftStatusSelected() || visibilite.toLowerCase(Locale.ROOT).contains("priv");
    }

    private boolean isBrouillon(Evenement ev) {
        return safeValue(ev.getStatut_event()).toLowerCase(Locale.ROOT).contains("brouillon");
    }

    private void syncDraftVisibility() {
        if (getVisibiliteCombo() == null) return;

        if (isDraftStatusSelected()) {
            getVisibiliteCombo().setValue("Privé");
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
            if (!cloudinaryEventUploadService.isEventConfigured()) {
                showAlert(
                        Alert.AlertType.ERROR,
                        "Cloudinary non configure",
                        "Ajoutez CLOUDINARY_EVENT_CLOUD_NAME, CLOUDINARY_EVENT_API_KEY et CLOUDINARY_EVENT_API_SECRET dans .env.local, les variables d'environnement ou VM options."
                );
                return;
            }

            try {
                CloudinaryEventUploadService.UploadResult result = cloudinaryEventUploadService.uploadEventImage(file);
                tfImageEvent.setText(result.secureUrl());
                validateImage();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Cloudinary", "Upload image evenement impossible : " + e.getMessage());
            }
        }
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
                && isStatutValid && isMotifAnnulationValid && isDateDebutValid && isDateFinValid && isNomLieuValid
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
        if (!isMotifAnnulationValid) errors.append("- Motif d'annulation\n");
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
            Date dateDebut = Date.valueOf(getDateDebutPicker().getValue());
            boolean existe = evenementService.evenementExisteDeja(dateDebut);
            if (existe) {
                showAlert(Alert.AlertType.WARNING, "Evenement deja existant",
                        "Un autre evenement existe deja avec la meme date de debut.");
                return;
            }

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
            Date dateDebut = Date.valueOf(getDateDebutPicker().getValue());

            boolean existe = evenementService.evenementExisteDejaPourModification(
                    evenementAModifier.getId(),
                    dateDebut
            );
            if (existe) {
                showAlert(Alert.AlertType.WARNING, "Evenement deja existant",
                        "Un autre evenement existe deja avec la meme date de debut.");
                return;
            }

            if (existe) {
                showAlert(Alert.AlertType.WARNING, "Événement déjà existant",
                        "Un autre événement avec le même titre et la même date de début existe déjà.");
                return;
            }

            remplirEvenementDepuisForm(evenementAModifier);
            evenementAModifier.setDate_mise_a_jour_event(new java.util.Date());

            evenementService.modifier(evenementAModifier);
            if (getMotifAnnulationArea() != null) {
                evenementService.modifierMotifAnnulation(
                        evenementAModifier.getId(),
                        getMotifAnnulationArea().getText()
                );
            }

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
        String visibilite = getVisibiliteCombo() != null ? getVisibiliteCombo().getValue() : null;
        if ((visibilite == null || visibilite.isBlank()) && isDraftStatusSelected()) {
            visibilite = "Privé";
        }
        ev.setVisibilite_event(visibilite);
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
        if (getMotifAnnulationArea() != null) {
            getMotifAnnulationArea().setText(safeValue(evenementService.recupererMotifAnnulation(evenementAModifier.getId())));
        }

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

        if (publicationMode) {
            if (getStatutCombo() != null) getStatutCombo().setValue("Publie");
            if (getVisibiliteCombo() != null) getVisibiliteCombo().setValue("Public");
            publicationMode = false;
        }

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
        if (getMotifAnnulationArea() != null) getMotifAnnulationArea().clear();
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
        evenementTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
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

            refreshCalendarMonth();
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
            refreshCalendarMonth();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void initCalendarSection() {
        if (calendarMonthGrid == null || calendarWeekHeader == null) {
            return;
        }
        ensureCalendarDataLoaded();
        buildCalendarWeekHeader();
        refreshCalendarMonth();
    }

    private void ensureCalendarDataLoaded() {
        if (!masterList.isEmpty()) {
            return;
        }
        try {
            masterList.setAll(evenementService.recuperer());
        } catch (SQLException e) {
            if (calendarSelectionHintLabel != null) {
                calendarSelectionHintLabel.setText("Chargement du calendrier impossible : " + e.getMessage());
            }
        }
    }

    @FXML
    private void showPreviousCalendarMonth() {
        currentCalendarMonth = currentCalendarMonth.minusMonths(1);
        refreshCalendarMonth();
    }

    @FXML
    private void showNextCalendarMonth() {
        currentCalendarMonth = currentCalendarMonth.plusMonths(1);
        refreshCalendarMonth();
    }

    @FXML
    private void showCurrentCalendarMonth() {
        currentCalendarMonth = YearMonth.now();
        refreshCalendarMonth();
    }

    private void buildCalendarWeekHeader() {
        if (calendarWeekHeader == null || !calendarWeekHeader.getChildren().isEmpty()) {
            return;
        }

        List<String> days = List.of("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim");
        for (int column = 0; column < days.size(); column++) {
            Label label = new Label(days.get(column));
            label.getStyleClass().add("calendar-weekday-label");
            label.setMaxWidth(Double.MAX_VALUE);
            label.setAlignment(Pos.CENTER);
            calendarWeekHeader.add(label, column, 0);
        }
    }

    private void refreshCalendarMonth() {
        if (calendarMonthGrid == null || calendarWeekHeader == null) {
            return;
        }

        if (calendarMonthLabel != null) {
            String month = currentCalendarMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRANCE);
            calendarMonthLabel.setText(month + " " + currentCalendarMonth.getYear());
        }

        calendarMonthGrid.getChildren().clear();
        calendarMonthGrid.getRowConstraints().clear();

        for (int row = 0; row < 6; row++) {
            RowConstraints constraints = new RowConstraints();
            constraints.setPercentHeight(16.66);
            constraints.setVgrow(Priority.ALWAYS);
            calendarMonthGrid.getRowConstraints().add(constraints);
        }

        LocalDate firstDay = currentCalendarMonth.atDay(1);
        int offset = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        if (offset < 0) {
            offset += 7;
        }
        LocalDate calendarStart = firstDay.minusDays(offset);

        for (int index = 0; index < 42; index++) {
            LocalDate cellDate = calendarStart.plusDays(index);
            VBox cell = createCalendarDayCell(cellDate);
            calendarMonthGrid.add(cell, index % 7, index / 7);
        }
    }

    private VBox createCalendarDayCell(LocalDate cellDate) {
        VBox cell = new VBox(8);
        cell.getStyleClass().add("calendar-day-cell");
        if (!cellDate.getMonth().equals(currentCalendarMonth.getMonth())) {
            cell.getStyleClass().add("calendar-day-cell-muted");
        }
        if (LocalDate.now().equals(cellDate)) {
            cell.getStyleClass().add("calendar-day-cell-today");
        }

        Label dayNumber = new Label(String.valueOf(cellDate.getDayOfMonth()));
        dayNumber.getStyleClass().add("calendar-day-number");

        VBox eventsBox = new VBox(6);
        eventsBox.setFillWidth(true);
        VBox.setVgrow(eventsBox, Priority.ALWAYS);

        for (Evenement event : getEventsForDate(cellDate)) {
            eventsBox.getChildren().add(createCalendarEventChip(event, cellDate));
        }

        if (eventsBox.getChildren().isEmpty()) {
            Label empty = new Label("Libre");
            empty.getStyleClass().add("calendar-empty-label");
            eventsBox.getChildren().add(empty);
        }

        cell.getChildren().addAll(dayNumber, eventsBox);
        VBox.setVgrow(eventsBox, Priority.ALWAYS);

        cell.setOnDragOver(event -> {
            if (draggedCalendarEvent != null && event.getGestureSource() != cell) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        cell.setOnDragEntered(event -> {
            if (draggedCalendarEvent != null) {
                cell.getStyleClass().add("calendar-day-cell-drop-target");
            }
            event.consume();
        });

        cell.setOnDragExited(event -> {
            cell.getStyleClass().remove("calendar-day-cell-drop-target");
            event.consume();
        });

        cell.setOnDragDropped(event -> {
            boolean success = false;
            if (draggedCalendarEvent != null) {
                success = moveEventFromCalendar(draggedCalendarEvent, cellDate);
            }
            cell.getStyleClass().remove("calendar-day-cell-drop-target");
            event.setDropCompleted(success);
            event.consume();
        });

        return cell;
    }

    private List<Evenement> getEventsForDate(LocalDate date) {
        List<Evenement> events = new ArrayList<>();
        for (Evenement event : masterList) {
            LocalDate start = toLocalDate(event.getDate_debut_event());
            LocalDate end = toLocalDate(event.getDate_fin_event());
            if (start == null || end == null) {
                continue;
            }
            if ((date.isEqual(start) || date.isAfter(start)) && (date.isEqual(end) || date.isBefore(end))) {
                events.add(event);
            }
        }
        events.sort(Comparator.comparing(e -> safeValue(e.getTitre_event()).toLowerCase(Locale.ROOT)));
        return events;
    }

    private Button createCalendarEventChip(Evenement event, LocalDate anchorDate) {
        Button chip = new Button(shortText(valueOrDash(event.getTitre_event()), 24));
        chip.getStyleClass().add("calendar-event-chip");
        chip.setMaxWidth(Double.MAX_VALUE);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setOnAction(e -> {
            if (calendarSelectionHintLabel != null) {
                calendarSelectionHintLabel.setText("Evenement selectionne : " + valueOrDash(event.getTitre_event())
                        + " | Glissez le chip vers un autre jour pour le reprogrammer.");
            }
            if (evenementTable != null) {
                evenementTable.getSelectionModel().select(event);
                evenementTable.scrollTo(event);
            }
        });

        chip.setOnDragDetected(e -> {
            draggedCalendarEvent = event;
            Dragboard dragboard = chip.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(event.getId()));
            dragboard.setContent(content);
            if (calendarSelectionHintLabel != null) {
                calendarSelectionHintLabel.setText("Deplacement de " + valueOrDash(event.getTitre_event())
                        + " depuis le " + anchorDate + ".");
            }
            e.consume();
        });

        chip.setOnDragDone(e -> {
            draggedCalendarEvent = null;
            e.consume();
        });
        return chip;
    }

    private boolean moveEventFromCalendar(Evenement event, LocalDate newStart) {
        try {
            LocalDate currentStart = toLocalDate(event.getDate_debut_event());
            LocalDate currentEnd = toLocalDate(event.getDate_fin_event());
            if (currentStart == null || currentEnd == null) {
                return false;
            }

            long durationDays = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(currentStart, currentEnd));
            LocalDate newEnd = newStart.plusDays(durationDays);

            event.setDate_debut_event(Date.valueOf(newStart));
            event.setDate_fin_event(Date.valueOf(newEnd));
            event.setDate_mise_a_jour_event(new java.util.Date());
            evenementService.modifier(event);

            if (calendarSelectionHintLabel != null) {
                calendarSelectionHintLabel.setText("Evenement reprogramme : " + valueOrDash(event.getTitre_event())
                        + " | " + newStart + " -> " + newEnd + ".");
            }
            if (evenementTable != null) {
                evenementTable.refresh();
            }
            updateStats();
            refreshCalendarMonth();
            return true;
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Calendrier", "Deplacement impossible : " + ex.getMessage());
            return false;
        }
    }

    private LocalDate toLocalDate(java.util.Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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

        long demandesEnAttente = participationService.countPending(masterList);

        if (totalEventsLabel != null) totalEventsLabel.setText(String.valueOf(total));
        if (publiesLabel != null) publiesLabel.setText(String.valueOf(publies));
        if (brouillonsLabel != null) brouillonsLabel.setText(String.valueOf(brouillons));
        if (archivesLabel != null) archivesLabel.setText(String.valueOf(archives));
        if (inventoryCountLabel != null) {
            inventoryCountLabel.setText(total + " evenement(s) | " + demandesEnAttente + " participant(s) en liste d'attente");
        }
        updateParticipationAlert(demandesEnAttente);
    }

    private void updateParticipationAlert(long demandesEnAttente) {
        if (participationAlertBox == null) return;

        boolean hasPending = demandesEnAttente > 0;
        participationAlertBox.setVisible(hasPending);
        participationAlertBox.setManaged(hasPending);

        if (!hasPending) return;

        if (participationAlertCountLabel != null) {
            participationAlertCountLabel.setText(demandesEnAttente + " participant(s) en liste d'attente");
        }
        if (participationAlertMessageLabel != null) {
            participationAlertMessageLabel.setText("Les nouvelles demandes sont acceptees automatiquement tant qu'il reste des places. Utilisez la liste pour refuser ou replacer un participant en liste d'attente.");
        }
    }

    private void showParticipationBackOfficeMessage(String title, String message) {
        if (participationAlertBox == null) {
            showAlert(Alert.AlertType.INFORMATION, title, message);
            return;
        }

        participationAlertBox.setVisible(true);
        participationAlertBox.setManaged(true);
        if (participationAlertCountLabel != null) {
            participationAlertCountLabel.setText(title);
        }
        if (participationAlertMessageLabel != null) {
            participationAlertMessageLabel.setText(message);
        }
    }
    @FXML
    private void ouvrirAlertDemandesParticipation() {
        List<Evenement> eventsWithPending = masterList.stream()
                .filter(ev -> participationService.countPending(ev) > 0)
                .toList();

        if (eventsWithPending.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Demandes", "Aucun participant en liste d'attente.");
            updateParticipationAlert(0);
            return;
        }

        if (eventsWithPending.size() == 1) {
            afficherDemandesParticipation(eventsWithPending.get(0));
            return;
        }

        afficherEvenementsAvecDemandes(eventsWithPending);
    }

    private void afficherEvenementsAvecDemandes(List<Evenement> eventsWithPending) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Demandes de participation");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setMinWidth(700);
        dialog.getDialogPane().setPrefWidth(780);

        VBox content = new VBox(14);
        content.getStyleClass().add("participation-modal-card");

        Label title = new Label("Evenements avec liste d'attente");
        title.setStyle("-fx-text-fill: #123c69; -fx-font-size: 24px; -fx-font-weight: 900;");

        Label subtitle = new Label("Choisissez un evenement pour gerer les participants en liste d'attente.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #e6f9ff; -fx-font-size: 13px; -fx-font-weight: 600;");

        VBox rows = new VBox(10);
        for (Evenement ev : eventsWithPending) {
            long pending = participationService.countPending(ev);
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #d5ebf7; -fx-border-radius: 16; -fx-padding: 14;");

            VBox info = new VBox(3);
            Label name = new Label(valueOrDash(ev.getTitre_event()));
            name.setStyle("-fx-text-fill: #153946; -fx-font-size: 16px; -fx-font-weight: 800;");
            Label meta = new Label(valueOrDash(ev.getVille_event()) + " | " + pending + " participant(s) en liste d'attente");
            meta.setStyle("-fx-text-fill: #4d6d78; -fx-font-size: 12px; -fx-font-weight: 700;");
            info.getChildren().addAll(name, meta);

            Button open = new Button("Traiter");
            open.getStyleClass().add("participation-alert-button");
            open.setOnAction(e -> {
                dialog.close();
                afficherDemandesParticipation(ev);
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(info, spacer, open);
            rows.getChildren().add(row);
        }

        content.getChildren().addAll(title, subtitle, rows);
        dialog.getDialogPane().setContent(content);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.getStyleClass().add("cancel-button");
        dialog.showAndWait();
    }

    private void addActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("Voir");
            private final Button editBtn = new Button("Modifier");
            private final Button demandesBtn = new Button("Demandes");
            private final Button aiBtn = new Button("AI");
            private final Button publishBtn = new Button("Publier");
            private final Button deleteBtn = new Button("Supprimer");
            private final Button archiveBtn = new Button("Historique");
            private final ToolBar toolBar = new ToolBar(viewBtn, editBtn, demandesBtn, aiBtn, publishBtn, deleteBtn, archiveBtn);

            {
                toolBar.getStyleClass().add("action-toolbar");
                viewBtn.getStyleClass().add("view-btn");
                editBtn.getStyleClass().add("edit-btn");
                demandesBtn.getStyleClass().add("publish-btn");
                aiBtn.getStyleClass().add("ai-action-btn");
                publishBtn.getStyleClass().add("publish-btn");
                deleteBtn.getStyleClass().add("delete-btn");
                archiveBtn.getStyleClass().add("delete-btn");

                viewBtn.setOnAction(e -> ouvrirPageCardsBack());

                editBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());
                    ouvrirPageModification(ev);
                });

                demandesBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());
                    afficherDemandesParticipation(ev);
                });

                aiBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());
                    ouvrirPageAiEvenement(ev);
                });

                publishBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());
                    publicationMode = true;
                    ouvrirPageModification(ev);
                });

                deleteBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation de suppression");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Êtes-vous sûr de vouloir supprimer cet événement brouillon : " + safeValue(ev.getTitre_event()) + " ?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        evenementService.supprimer(ev);
                        retourListeEvenements(null);
                    }
                });

                archiveBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Envoyer l'evenement dans l'historique : " + safeValue(ev.getTitre_event()) + " ?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        evenementService.archiver(ev);
                        evenementTable.refresh();
                        updateStats();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                Evenement ev = getTableView().getItems().get(getIndex());
                long pending = participationService.countPending(ev);
                demandesBtn.setText(pending > 0 ? "Demandes (" + pending + ")" : "Demandes");
                toolBar.getItems().setAll(viewBtn, editBtn, demandesBtn, aiBtn);
                if (!isVisibleOnFront(ev)) {
                    toolBar.getItems().add(publishBtn);
                }
                if (isBrouillon(ev)) {
                    toolBar.getItems().add(deleteBtn);
                }
                toolBar.getItems().add(archiveBtn);
                setGraphic(toolBar);
            }
        });

        actionsCol.setCellValueFactory(param -> Bindings.createObjectBinding(() -> null));
    }

    private void afficherDemandesParticipation(Evenement ev) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Demandes de participation");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setMinWidth(920);
        dialog.getDialogPane().setPrefWidth(980);
        dialog.getDialogPane().setStyle("-fx-background-color: transparent; -fx-background-radius: 26; -fx-padding: 0;");

        VBox content = new VBox(18);
        content.getStyleClass().add("participation-modal-card");

        List<ParticipationDemande> demandes = participationService.getDemandes(ev);
        long pending = demandes.stream().filter(d -> ParticipationDemande.STATUS_PENDING.equals(d.getStatus())).count();
        long accepted = demandes.stream().filter(d -> ParticipationDemande.STATUS_ACCEPTED.equals(d.getStatus())).count();
        long refused = demandes.stream().filter(d -> ParticipationDemande.STATUS_REFUSED.equals(d.getStatus())).count();

        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: linear-gradient(to right, #08a6c8, #1f64d8); -fx-background-radius: 22; -fx-padding: 20 22; -fx-effect: dropshadow(gaussian, rgba(28,103,202,0.18), 14, 0.14, 0, 4);");

        VBox titleBox = new VBox(5);
        Label overline = new Label("GESTION DES PARTICIPATIONS");
        overline.setStyle("-fx-text-fill: #dff9ff; -fx-font-size: 11px; -fx-font-weight: 900;");
        Label title = new Label(valueOrDash(ev.getTitre_event()));
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: 900;");
        Label subtitle = new Label("Les participants sont acceptes automatiquement tant qu'il reste des places. Utilisez cette vue pour refuser une demande ou replacer quelqu'un en liste d'attente.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #e6f9ff; -fx-font-size: 13px; -fx-font-weight: 600;");
        titleBox.getChildren().addAll(overline, title, subtitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, headerSpacer);

        HBox stats = new HBox(10,
                createDemandStatChip("Liste d'attente", pending, "#e8f6ff", "#087ea4"),
                createDemandStatChip("Acceptees", accepted, "#eafff4", "#15803d"),
                createDemandStatChip("Refusees", refused, "#fff1f2", "#be123c")
        );
        stats.setAlignment(Pos.CENTER_LEFT);

        VBox rows = new VBox(12);
        if (demandes.isEmpty()) {
            Label empty = new Label("Aucune demande pour cet evenement.");
            empty.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-padding: 24; -fx-text-fill: #6e8c97; -fx-font-size: 15px; -fx-font-weight: 700;");
            rows.getChildren().add(empty);
        } else {
            for (ParticipationDemande demande : demandes) {
                rows.getChildren().add(buildDemandeRow(dialog, ev, demande));
            }
        }

        ScrollPane scrollPane = new ScrollPane(rows);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        content.getChildren().addAll(header, stats, scrollPane);
        dialog.getDialogPane().setContent(content);

        Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setText("Fermer");
        closeButton.getStyleClass().add("cancel-button");

        dialog.showAndWait();
    }

    private Label createDemandStatChip(String label, long value, String background, String textColor) {
        Label chip = new Label(label + " : " + value);
        chip.setStyle("-fx-background-color: " + background + "; -fx-text-fill: " + textColor + "; -fx-background-radius: 999; -fx-padding: 9 15; -fx-font-size: 13px; -fx-font-weight: 900;");
        return chip;
    }

    private HBox buildDemandeRow(Dialog<Void> dialog, Evenement ev, ParticipationDemande demande) {
        VBox info = new VBox(7);
        info.setMaxWidth(Double.MAX_VALUE);

        HBox nameRow = new HBox(10);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label name = new Label(demande.getDisplayName());
        name.setStyle("-fx-text-fill: #153946; -fx-font-size: 17px; -fx-font-weight: 900;");
        Label status = new Label(getParticipationStatusLabel(demande.getStatus()));
        status.setStyle(getStatusChipStyle(demande.getStatus()));
        nameRow.getChildren().addAll(name, status);

        Label contact = new Label(valueOrDash(demande.getEmail()) + "   |   " + valueOrDash(demande.getTelephone()));
        contact.setWrapText(true);
        contact.setStyle("-fx-text-fill: #456779; -fx-font-size: 12px; -fx-font-weight: 700;");

        Label motif = new Label("Motif : " + (safeValue(demande.getMotif()).isBlank() ? "Non renseigne" : demande.getMotif()));
        motif.setWrapText(true);
        motif.setStyle("-fx-text-fill: #5c7280; -fx-font-size: 12px;");

        String ticketText = safeValue(demande.getTicketCode()).isBlank() ? "Ticket : pas encore genere" : "Ticket : " + demande.getTicketCode();
        Label ticket = new Label(ticketText);
        ticket.setStyle("-fx-text-fill: #0b8fb4; -fx-font-size: 12px; -fx-font-weight: 800;");

        info.getChildren().addAll(nameRow, contact, motif, ticket);

        Button acceptBtn = new Button("Accepter");
        acceptBtn.setStyle("-fx-background-color: linear-gradient(to right, #08a6c8, #1f64d8); -fx-text-fill: white; -fx-background-radius: 14; -fx-padding: 10 18; -fx-font-weight: 900; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(31,100,216,0.22), 10, 0.12, 0, 3);");
        styleParticipationActionButton(acceptBtn, 132);
        acceptBtn.setOnAction(e -> {
            dialog.close();
            traiterDecisionParticipation(ev, demande, ParticipationDemande.STATUS_ACCEPTED);
        });

        Button refuseBtn = new Button("Refuser");
        refuseBtn.setStyle("-fx-background-color: white; -fx-text-fill: #be123c; -fx-border-color: #fecdd3; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 10 18; -fx-font-weight: 900; -fx-cursor: hand;");
        styleParticipationActionButton(refuseBtn, 132);
        refuseBtn.setOnAction(e -> {
            dialog.close();
            traiterDecisionParticipation(ev, demande, ParticipationDemande.STATUS_REFUSED);
        });

        Button pendingBtn = new Button("Liste d'attente");
        pendingBtn.setStyle("-fx-background-color: #e8f6ff; -fx-text-fill: #087ea4; -fx-border-color: #9ddff0; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 10 18; -fx-font-weight: 900; -fx-cursor: hand;");
        styleParticipationActionButton(pendingBtn, 160);
        pendingBtn.setOnAction(e -> {
            dialog.close();
            traiterDecisionParticipation(ev, demande, ParticipationDemande.STATUS_WAITING);
        });

        Button smsBtn = new Button("Envoyer SMS");
        smsBtn.setStyle("-fx-background-color: #f5f3ff; -fx-text-fill: #5b21b6; -fx-border-color: #d8ccff; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 10 18; -fx-font-weight: 900; -fx-cursor: hand;");
        styleParticipationActionButton(smsBtn, 148);
        smsBtn.setOnAction(e -> {
            dialog.close();
            envoyerSmsDecision(ev, demande);
            afficherDemandesParticipation(ev);
        });

        FlowPane actions = new FlowPane(8, 8, acceptBtn, refuseBtn, pendingBtn, smsBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPrefWrapLength(320);

        HBox row = new HBox(16, info, actions);
        HBox.setHgrow(info, Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-border-color: #d5ebf7; -fx-border-radius: 18; -fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(15,90,170,0.07), 10, 0.12, 0, 2);");
        return row;
    }

    private void styleParticipationActionButton(Button button, double minWidth) {
        button.setMinWidth(minWidth);
        button.setPrefWidth(minWidth);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setWrapText(false);
    }

    private String getStatusChipStyle(String status) {
        if (ParticipationDemande.STATUS_ACCEPTED.equals(status)) {
            return "-fx-background-color: #eafff4; -fx-text-fill: #15803d; -fx-background-radius: 999; -fx-padding: 5 11; -fx-font-size: 11px; -fx-font-weight: 900;";
        }
        if (ParticipationDemande.STATUS_REFUSED.equals(status)) {
            return "-fx-background-color: #fff1f2; -fx-text-fill: #be123c; -fx-background-radius: 999; -fx-padding: 5 11; -fx-font-size: 11px; -fx-font-weight: 900;";
        }
        if (ParticipationDemande.STATUS_WAITING.equals(status)) {
            return "-fx-background-color: #fff7e8; -fx-text-fill: #b45309; -fx-background-radius: 999; -fx-padding: 5 11; -fx-font-size: 11px; -fx-font-weight: 900;";
        }
        return "-fx-background-color: #e8f6ff; -fx-text-fill: #087ea4; -fx-background-radius: 999; -fx-padding: 5 11; -fx-font-size: 11px; -fx-font-weight: 900;";
    }

    private String getParticipationStatusLabel(String status) {
        if (ParticipationDemande.STATUS_ACCEPTED.equals(status)) {
            return "Acceptee";
        }
        if (ParticipationDemande.STATUS_REFUSED.equals(status)) {
            return "Refusee";
        }
        if (ParticipationDemande.STATUS_WAITING.equals(status) || ParticipationDemande.STATUS_PENDING.equals(status)) {
            return "Liste d'attente";
        }
        return "Statut inconnu";
    }

    private String demanderDecisionParticipation(ParticipationDemande demande, String status) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Decision participation");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Confirmer", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );
        dialog.getDialogPane().setMinWidth(560);

        VBox content = new VBox(14);
        content.getStyleClass().add("participation-modal-card");

        String actionLabel = ParticipationDemande.STATUS_ACCEPTED.equals(status)
                ? "Accepter la demande"
                : ParticipationDemande.STATUS_REFUSED.equals(status)
                ? "Refuser la demande"
                : "Placer en liste d'attente";

        Label overline = new Label("DECISION ADMINISTRATIVE");
        overline.setStyle("-fx-text-fill: #dff9ff; -fx-font-size: 11px; -fx-font-weight: 900;");

        Label title = new Label(actionLabel);
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #123c69; -fx-font-size: 24px; -fx-font-weight: 900;");

        Label participant = new Label(demande.getDisplayName() + " | " + valueOrDash(demande.getTelephone()));
        participant.setWrapText(true);
        participant.setStyle("-fx-text-fill: #4e6d82; -fx-font-size: 13px; -fx-font-weight: 700;");

        TextArea noteArea = new TextArea();
        noteArea.setPromptText(ParticipationDemande.STATUS_REFUSED.equals(status)
                ? "Motif de refus (recommande)"
                : "Note interne optionnelle");
        noteArea.setWrapText(true);
        noteArea.setPrefRowCount(4);
        noteArea.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #bce6f1; -fx-border-radius: 14; -fx-font-size: 13px;");

        Label helper = new Label(ParticipationDemande.STATUS_ACCEPTED.equals(status)
                ? "Un ticket sera genere si une place est disponible. Aucun SMS n'est envoye automatiquement."
                : ParticipationDemande.STATUS_REFUSED.equals(status)
                ? "Le motif sera conserve avec la demande. Aucun SMS n'est envoye automatiquement."
                : "La demande restera sur la liste d'attente.");
        helper.setWrapText(true);
        helper.setStyle("-fx-text-fill: #5c7280; -fx-font-size: 12px; -fx-font-weight: 600;");

        content.getChildren().addAll(overline, title, participant, helper, noteArea);
        dialog.getDialogPane().setContent(content);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
        confirmButton.getStyleClass().add("participation-alert-button");
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Annuler");
        cancelButton.getStyleClass().add("cancel-button");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return null;
        }
        return safeValue(noteArea.getText()).trim();
    }
    private void traiterDecisionParticipation(Evenement ev, ParticipationDemande demande, String status) {
        String note = demanderDecisionParticipation(demande, status);
        if (note == null) {
            afficherDemandesParticipation(ev);
            return;
        }

        try {
            ParticipationDemande updated = participationService.changerStatut(ev, demande.getId(), status, note.trim());

            if (evenementTable != null) {
                evenementTable.refresh();
            }
            updateStats();

            if (ParticipationDemande.STATUS_ACCEPTED.equals(status)) {
                showParticipationBackOfficeMessage("Decision enregistree", "La demande est acceptee. Aucun SMS n'a ete envoye automatiquement. Utilisez le bouton Envoyer SMS si besoin.");
            } else if (ParticipationDemande.STATUS_REFUSED.equals(status)) {
                showParticipationBackOfficeMessage("Decision enregistree", "La demande est refusee. Aucun SMS n'a ete envoye automatiquement. Utilisez le bouton Envoyer SMS si besoin.");
            } else {
                showParticipationBackOfficeMessage("Decision enregistree", "La demande est placee sur la liste d'attente.");
            }

            afficherDemandesParticipation(ev);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Decision non enregistree : " + e.getMessage());
        }
    }

    private void envoyerSmsDecision(Evenement ev, ParticipationDemande demande) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Envoyer SMS");
        confirm.setHeaderText(null);
        confirm.setContentText("Envoyer un SMS manuel a " + demande.getDisplayName() + " pour le statut actuel ?");
        Optional<ButtonType> confirmResult = confirm.showAndWait();
        if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK) {
            return;
        }

        String recipient = participationService.normalizeTunisianPhone(demande.getTelephone());
        if (recipient.isBlank()) {
            showParticipationBackOfficeMessage("Decision enregistree", "Telephone introuvable. SMS non envoye.");
            return;
        }

        String message = participationService.buildDecisionSms(ev, demande);
        TwilioSmsServiceEvent.SmsResult result = smsServiceEvent.sendEvenementSms(recipient, message);
        if (result.success()) {
            String diagnostic = result.deliveryDiagnostic();
            showParticipationBackOfficeMessage(
                    "Decision enregistree",
                    "Decision enregistree. SMS accepte par Twilio pour " + recipient + ". "
                            + (diagnostic.isBlank() ? "" : diagnostic + " ")
                            + "Si le participant ne recoit rien, verifiez ce SID dans les logs Twilio: l'acceptation API ne garantit pas la livraison operateur."
            );
        } else if (result.missingConfiguration()) {
            showParticipationBackOfficeMessage("Decision enregistree", "Configurez TWILIO_SMS_EVENT_ACCOUNT_SID, TWILIO_SMS_EVENT_AUTH_TOKEN et TWILIO_SMS_EVENT_FROM pour envoyer les SMS.");
        } else {
            String diagnostic = result.deliveryDiagnostic();
            showParticipationBackOfficeMessage(
                    "Decision enregistree",
                    "Decision enregistree, mais le SMS a echoue pour " + recipient + " : "
                            + (diagnostic.isBlank() ? result.responseBody() : diagnostic)
            );
        }
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

    private void chargerRecommandationsFrontAi(Evenement ev) {
        if (aiFrontRecommendationsContainer == null) return;

        aiFrontRecommendationsContainer.getChildren().clear();
        if (aiFrontStatusLabel != null) {
            aiFrontStatusLabel.setText(groqEventAiClient.isConfigured()
                    ? "Groq connecte + score local."
                    : "IA locale active. Ajoutez GROQ_EVENT_API_KEY pour Groq.");
        }

        try {
            List<Evenement> allEvents = evenementService.recuperer();
            List<AiEventIntelligenceService.Recommendation> recommendations =
                    aiEventIntelligenceService.recommendForEvent(ev, SessionManager.getCurrentUser(), allEvents, 4);

            if (recommendations.isEmpty()) {
                Label empty = new Label("Aucune alternative fiable pour le moment.");
                empty.setWrapText(true);
                empty.getStyleClass().add("ai-empty-note");
                aiFrontRecommendationsContainer.getChildren().add(empty);
                return;
            }

            for (AiEventIntelligenceService.Recommendation recommendation : recommendations) {
                aiFrontRecommendationsContainer.getChildren().add(createAiFrontRecommendationCard(recommendation));
            }
        } catch (Exception e) {
            Label error = new Label("Recommandations indisponibles: " + e.getMessage());
            error.setWrapText(true);
            error.getStyleClass().add("ai-empty-note");
            aiFrontRecommendationsContainer.getChildren().add(error);
        }
    }

    private VBox createAiFrontRecommendationCard(AiEventIntelligenceService.Recommendation recommendation) {
        Evenement event = recommendation.event();
        VBox card = new VBox(8);
        card.setPrefWidth(245);
        card.getStyleClass().add("ai-reco-card");

        Label score = new Label(String.format(Locale.US, "Score %.1f/10", recommendation.score()));
        score.getStyleClass().add("ai-score-pill");

        Label title = new Label(valueOrDash(event.getTitre_event()));
        title.setWrapText(true);
        title.getStyleClass().add("ai-reco-title");

        Label meta = new Label(valueOrDash(event.getVille_event()) + " | " + dateText(event.getDate_debut_event()));
        meta.setWrapText(true);
        meta.getStyleClass().add("ai-reco-meta");

        Label reasons = new Label(String.join(" ", recommendation.reasons()));
        reasons.setWrapText(true);
        reasons.getStyleClass().add("ai-reco-reasons");

        Button open = new Button("Voir");
        open.getStyleClass().add("event-link-button");
        open.setOnAction(e -> ouvrirDetailEvenementFront(event));

        card.getChildren().addAll(score, title, meta, reasons, open);
        return card;
    }

    @FXML
    private void ouvrirRecommandationsAiBack() {
        Evenement selected = evenementTable != null ? evenementTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            try {
                List<Evenement> events = evenementService.recuperer();
                selected = events.stream()
                        .filter(this::isVisibleOnFront)
                        .findFirst()
                        .orElse(events.isEmpty() ? null : events.get(0));
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "IA Evenements", e.getMessage());
                return;
            }
        }

        if (selected == null) {
            showAlert(Alert.AlertType.INFORMATION, "IA Evenements", "Aucun evenement disponible pour l'analyse.");
            return;
        }

        ouvrirPageAiEvenement(selected);
    }

    private void ouvrirPageAiEvenement(Evenement ev) {
        evenementSelectionneAiBack = ev;
        loadScene("/DashboardEvenementAI.fxml", "Assistant IA - Evenements");
    }

    private void initAiBackPageIfExists() {
        if (aiBackEventTitleLabel == null) return;

        try {
            Evenement selected = evenementSelectionneAiBack;
            if (selected == null) {
                List<Evenement> events = evenementService.recuperer();
                selected = events.stream()
                        .filter(this::isVisibleOnFront)
                        .findFirst()
                        .orElse(events.isEmpty() ? null : events.get(0));
            }

            if (selected == null) {
                aiBackEventTitleLabel.setText("Aucun evenement a analyser");
                aiBackStatusLabel.setText("Ajoutez un evenement pour activer l'assistant.");
                return;
            }

            remplirPageAiBack(selected);
        } catch (Exception e) {
            aiBackStatusLabel.setText("Analyse indisponible: " + e.getMessage());
        }
    }

    private void remplirPageAiBack(Evenement ev) throws SQLException {
        List<Evenement> allEvents = evenementService.recuperer();
        List<ParticipationDemande> demandes = participationService.getDemandes(ev);
        AiEventIntelligenceService.RiskReport risk = aiEventIntelligenceService.analyzeRisk(ev, demandes, allEvents);
        List<AiEventIntelligenceService.Recommendation> recommendations =
                aiEventIntelligenceService.recommendForEvent(ev, SessionManager.getCurrentUser(), allEvents, 6);

        aiBackEventTitleLabel.setText(valueOrDash(ev.getTitre_event()));
        aiBackEventMetaLabel.setText(valueOrDash(ev.getType_event()) + " | " + valueOrDash(ev.getVille_event()) + " | " + dateText(ev.getDate_debut_event()));
        aiBackStatusLabel.setText(groqEventAiClient.publicStatus());

        aiBackRiskScoreLabel.setText(risk.score() + "/100");
        aiBackRiskLevelLabel.setText("Niveau " + risk.level());
        aiBackRiskProgress.setProgress(risk.score() / 100.0);

        fillAiList(aiBackReasonsContainer, risk.reasons());
        fillAiList(aiBackSuggestionsContainer, risk.suggestions());

        aiBackRecommendationsContainer.getChildren().clear();
        if (recommendations.isEmpty()) {
            Label empty = new Label("Aucune recommandation fiable pour le moment.");
            empty.getStyleClass().add("ai-empty-note");
            aiBackRecommendationsContainer.getChildren().add(empty);
        } else {
            for (AiEventIntelligenceService.Recommendation recommendation : recommendations) {
                aiBackRecommendationsContainer.getChildren().add(createAiBackRecommendationCard(recommendation));
            }
        }

        String localSummary = aiEventIntelligenceService.buildRecommendationSummary(ev, recommendations);
        aiBackSummaryArea.setText(localSummary);
        chargerResumeGroqAsync(ev, risk, recommendations, localSummary);
    }

    private void chargerResumeGroqAsync(
            Evenement ev,
            AiEventIntelligenceService.RiskReport risk,
            List<AiEventIntelligenceService.Recommendation> recommendations,
            String fallbackSummary
    ) {
        if (aiBackSummaryArea == null || !groqEventAiClient.isConfigured()) {
            return;
        }

        aiBackSummaryArea.setText("Generation du resume professionnel avec Groq...\n\n" + fallbackSummary);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return groqEventAiClient.generateEventExecutiveSummary(ev, risk, recommendations);
            }
        };

        task.setOnSucceeded(e -> aiBackSummaryArea.setText(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String message = ex == null || ex.getMessage() == null ? "erreur inconnue" : ex.getMessage();
            aiBackSummaryArea.setText(fallbackSummary + "\n\nGroq indisponible: " + message);
        });

        Thread thread = new Thread(task, "groq-event-summary");
        thread.setDaemon(true);
        thread.start();
    }

    private void fillAiList(VBox container, List<String> lines) {
        if (container == null) return;
        container.getChildren().clear();
        if (lines == null || lines.isEmpty()) {
            Label empty = new Label("-");
            empty.getStyleClass().add("ai-page-muted");
            container.getChildren().add(empty);
            return;
        }

        for (String line : lines) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.TOP_LEFT);
            row.getStyleClass().add("ai-page-list-row");

            Label dot = new Label("");
            dot.getStyleClass().add("ai-page-dot");

            Label label = new Label(line);
            label.setWrapText(true);
            label.getStyleClass().add("ai-page-list-text");
            HBox.setHgrow(label, Priority.ALWAYS);

            row.getChildren().addAll(dot, label);
            container.getChildren().add(row);
        }
    }

    private VBox createAiBackRecommendationCard(AiEventIntelligenceService.Recommendation recommendation) {
        Evenement event = recommendation.event();
        VBox card = new VBox(8);
        card.setPrefWidth(270);
        card.getStyleClass().add("ai-reco-card");

        Label score = new Label(String.format(Locale.US, "%.1f/10", recommendation.score()));
        score.getStyleClass().add("ai-score-pill");

        Label title = new Label(valueOrDash(event.getTitre_event()));
        title.setWrapText(true);
        title.getStyleClass().add("ai-reco-title");

        Label meta = new Label(valueOrDash(event.getType_event()) + " | " + valueOrDash(event.getVille_event()) + " | " + dateText(event.getDate_debut_event()));
        meta.setWrapText(true);
        meta.getStyleClass().add("ai-reco-meta");

        Label reasons = new Label(String.join(" ", recommendation.reasons()));
        reasons.setWrapText(true);
        reasons.getStyleClass().add("ai-reco-reasons");

        card.getChildren().addAll(score, title, meta, reasons);
        return card;
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
        loadScene("/DashboardEvenementBI.fxml", "Dashboard Admin BI - Événements");
    }

    @FXML
    private void onAutresActions() {
        loadScene("/RessourceDashboard.fxml", "Gestion des Ressources");
    }

    @FXML
    private void onGoToFront(ActionEvent event) {
        try {
            URL url = getClass().getResource("/FrontFXML/Accueil.fxml");
            if (url == null) {
                throw new IOException("Fichier introuvable : /FrontFXML/Accueil.fxml");
            }

            Parent root = FXMLLoader.load(url);
            Stage stage = resolveCurrentStage();
            if (stage == null) return;

            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle("Accueil");
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Navigation",
                    "Impossible d'ouvrir l'espace front : " + e.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        try {
            SessionManager.clear();

            URL url = getClass().getResource("/FrontFXML/Login.fxml");
            if (url == null) {
                throw new IOException("Fichier introuvable : /FrontFXML/Login.fxml");
            }

            Parent root = FXMLLoader.load(url);
            Stage stage = resolveCurrentStage();
            if (stage == null) return;

            stage.setScene(new Scene(root, 1400, 820));
            stage.setTitle("Connexion");
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Déconnexion",
                    "Impossible de se déconnecter : " + e.getMessage());
        }
    }

    @FXML
    private void onNewEvent() {
        loadScene("/AjouterEvenement.fxml", "Nouvel événement");
    }

    @FXML
    private void ouvrirCalendrierEvenements() {
        loadScene("/EvenementCalendar.fxml", "Calendrier des événements");
    }

    @FXML
    private void onShowDraftEvents() {
        if (filterStatutCombo != null) {
            filterStatutCombo.setValue("Brouillon");
            appliquerRechercheFiltreTri();
        }
    }

    @FXML
    private void onShowAllEvents() {
        if (filterTypeCombo != null) filterTypeCombo.setValue("Tous");
        if (filterStatutCombo != null) filterStatutCombo.setValue("Tous");
        if (filterVilleCombo != null) filterVilleCombo.setValue("Toutes");
        appliquerRechercheFiltreTri();
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
        Control[] controls = {
                evenementTable,
                topEvenementsTable,
                biDateDebutPicker,
                biDateFinPicker,
                biVilleCombo,
                searchField,
                sortCombo,
                btnAjouterEvent,
                btnModifierEvent,
                btnResetEvent,
                calendarMonthLabel,
                calendarSelectionHintLabel,
                aiBackEventTitleLabel,
                aiBackSummaryArea
        };

        for (Control c : controls) {
            if (c != null && c.getScene() != null) {
                return (Stage) c.getScene().getWindow();
            }
        }

        if (biPeriodeLabel != null && biPeriodeLabel.getScene() != null) {
            return (Stage) biPeriodeLabel.getScene().getWindow();
        }
        if (eventsLineChart != null && eventsLineChart.getScene() != null) {
            return (Stage) eventsLineChart.getScene().getWindow();
        }
        if (cardsContainer != null && cardsContainer.getScene() != null) {
            return (Stage) cardsContainer.getScene().getWindow();
        }
        if (calendarMonthGrid != null && calendarMonthGrid.getScene() != null) {
            return (Stage) calendarMonthGrid.getScene().getWindow();
        }
        if (calendarWeekHeader != null && calendarWeekHeader.getScene() != null) {
            return (Stage) calendarWeekHeader.getScene().getWindow();
        }
        if (participationsContainer != null && participationsContainer.getScene() != null) {
            return (Stage) participationsContainer.getScene().getWindow();
        }
        if (titreDetailLabel != null && titreDetailLabel.getScene() != null) {
            return (Stage) titreDetailLabel.getScene().getWindow();
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

    public static Evenement getEvenementSelectionneFront() {
        return evenementSelectionneFront;
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

        chargerCarteEtMeteo(ev);
        chargerRecommandationsFrontAi(ev);
        chargerAccessibiliteFront(ev);
    }

    private void chargerAccessibiliteFront(Evenement ev) {
        if (accessibilityStatusLabel != null) {
            accessibilityStatusLabel.setText(accessibilityRoomService.hasConfiguredRoomLink(ev)
                    ? "Lien d'appel video configure, transcription et liens d'assistance rattaches aux ressources."
                    : "Aucun lien d'appel video configure pour cet evenement.");
        }

        if (accessibilityResourcesContainer == null) {
            return;
        }

        accessibilityRoomService.ensureRoomResource(ev);
        List<Ressource> resources = accessibilityRoomService.getAccessibilityResources(ev.getId());

        accessibilityResourcesContainer.getChildren().clear();

        if (resources.isEmpty()) {
            Label empty = new Label("Aucune ressource accessibilite pour cet evenement.");
            empty.setStyle("-fx-text-fill: #5b7280; -fx-font-size: 13px;");
            accessibilityResourcesContainer.getChildren().add(empty);
            return;
        }

        for (Ressource resource : resources) {
            Label chip = new Label(valueOrDash(resource.getNom_ressource()) + " | " + valueOrDash(resource.getType_ressource()));
            chip.setStyle(
                    "-fx-background-color: #eef9ff;" +
                            "-fx-background-radius: 999;" +
                            "-fx-padding: 8 14;" +
                            "-fx-text-fill: #0e6077;" +
                            "-fx-font-weight: 700;"
            );
            accessibilityResourcesContainer.getChildren().add(chip);
        }
    }

    @FXML
    private void ouvrirSalleAccessibilite() {
        if (evenementSelectionneFront == null) {
            showAlert(Alert.AlertType.ERROR, "Accessibilite", "Aucun evenement n'est selectionne.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/EventAccessibilityRoom.fxml"));
            Parent root = loader.load();

            Stage stage = resolveCurrentStage();
            if (stage == null) {
                throw new IllegalStateException("Fenetre actuelle introuvable.");
            }

            stage.setScene(new Scene(root, 1500, 920));
            stage.setTitle("Salle d'accessibilite");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Accessibilite", "Impossible d'ouvrir la salle : " + e.getMessage());
        }
    }

    private void chargerCarteEtMeteo(Evenement ev) {
        if (mapImageView == null && weatherConditionLabel == null) return;

        currentEventLocation = null;
        currentMapZoom = 16;
        if (mapStatusLabel != null) {
            mapStatusLabel.setText("Chargement de la carte...");
            mapStatusLabel.setVisible(true);
        }
        if (weatherConditionLabel != null) weatherConditionLabel.setText("Chargement de la meteo...");
        if (weatherTempLabel != null) weatherTempLabel.setText("-");
        if (weatherHumidityLabel != null) weatherHumidityLabel.setText("-");
        if (weatherWindLabel != null) weatherWindLabel.setText("-");

        Thread worker = new Thread(() -> {
            try {
                EventLocationService.EventLocation location = eventLocationService.resolve(ev);
                EventLocationService.WeatherInfo weather = eventLocationService.getWeather(location);
                String mapUrl = eventLocationService.buildStaticMapUrl(location, currentMapZoom);

                Platform.runLater(() -> {
                    currentEventLocation = location;
                    if (mapImageView != null) {
                        mapImageView.setImage(new Image(mapUrl, true));
                    }
                    if (mapStatusLabel != null) {
                        mapStatusLabel.setText(location.query() + " | Zoom " + currentMapZoom);
                        mapStatusLabel.setVisible(true);
                    }
                    if (weatherConditionLabel != null) weatherConditionLabel.setText(weather.condition());
                    if (weatherTempLabel != null) weatherTempLabel.setText(weather.temperature());
                    if (weatherHumidityLabel != null) weatherHumidityLabel.setText("Humidite " + weather.humidity());
                    if (weatherWindLabel != null) weatherWindLabel.setText("Vent " + weather.wind());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (mapStatusLabel != null) {
                        mapStatusLabel.setText("Carte indisponible pour cette adresse.");
                    }
                    if (weatherConditionLabel != null) weatherConditionLabel.setText("Meteo indisponible");
                    if (weatherTempLabel != null) weatherTempLabel.setText("-");
                    if (weatherHumidityLabel != null) weatherHumidityLabel.setText("-");
                    if (weatherWindLabel != null) weatherWindLabel.setText("Verifiez la connexion internet ou l'adresse de l'evenement.");
                });
            }
        }, "event-map-weather-loader");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void ouvrirCarteEvenement() {
        if (currentEventLocation == null) {
            showAlert(Alert.AlertType.INFORMATION, "Carte", "La carte n'est pas encore disponible.");
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                showAlert(Alert.AlertType.INFORMATION, "Carte", eventLocationService.buildInteractiveMapUrl(currentEventLocation));
                return;
            }
            Desktop.getDesktop().browse(URI.create(eventLocationService.buildInteractiveMapUrl(currentEventLocation)));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Carte", "Impossible d'ouvrir la carte : " + e.getMessage());
        }
    }

    @FXML
    private void zoomAvantCarteEvenement() {
        changerZoomCarte(1);
    }

    @FXML
    private void zoomArriereCarteEvenement() {
        changerZoomCarte(-1);
    }

    @FXML
    private void ouvrirItineraireEvenement() {
        if (currentEventLocation == null) {
            showAlert(Alert.AlertType.INFORMATION, "Itineraire", "L'itineraire n'est pas encore disponible.");
            return;
        }

        afficherItineraireDansApplication();
    }

    private void afficherItineraireDansApplication() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Itineraire vers l'evenement");
        dialog.setHeaderText(null);
        applyBookingStyles(dialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().add("directions-dialog");

        VBox content = new VBox(14);
        content.setPrefWidth(980);
        content.getStyleClass().add("directions-card");

        Label badge = new Label("ITINERAIRE");
        badge.getStyleClass().add("participation-request-overline");

        Label title = new Label("Choisir un point de depart");
        title.getStyleClass().add("participation-request-title");

        Label helper = new Label("Saisissez votre adresse ou votre ville. L'application prepare l'itineraire et garde la carte detaillee visible ici.");
        helper.setWrapText(true);
        helper.getStyleClass().add("participation-request-subtitle");

        TextField originField = new TextField();
        originField.setPromptText("Exemple : Esprit Ghazela, Ariana");
        originField.getStyleClass().add("modern-text-field");

        ImageView previewMap = new ImageView(new Image(eventLocationService.buildStaticMapUrl(currentEventLocation, 17), true));
        previewMap.setFitWidth(940);
        previewMap.setFitHeight(520);
        previewMap.setPreserveRatio(false);
        previewMap.setSmooth(true);

        Label routeStatus = new Label("Destination : " + currentEventLocation.query());
        routeStatus.setWrapText(true);
        routeStatus.getStyleClass().add("event-map-status");

        Button previewBtn = new Button("Preparer le trajet");
        previewBtn.getStyleClass().add("confirm-button");
        previewBtn.setOnAction(e -> {
            String origin = safeValue(originField.getText()).trim();
            if (origin.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Depart requis", "Entrez votre position ou votre adresse de depart.");
                return;
            }
            routeStatus.setText("Calcul du trajet...");
            final boolean[] routeFinished = {false};
            Thread timeoutWorker = new Thread(() -> {
                try {
                    Thread.sleep(12000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                Platform.runLater(() -> {
                    if (!routeFinished[0]) {
                        routeStatus.setText("Le calcul routier prend trop de temps. Utilisez Ouvrir dans navigateur pour le guidage complet.");
                    }
                });
            }, "event-route-preview-timeout");
            timeoutWorker.setDaemon(true);
            timeoutWorker.start();

            Thread routeWorker = new Thread(() -> {
                try {
                    EventLocationService.EventLocation routeOrigin = eventLocationService.resolveForRouteOrigin(origin);
                    String directMapUrl = eventLocationService.buildDirectLineStaticMapUrl(routeOrigin, currentEventLocation);
                    Platform.runLater(() -> {
                        routeFinished[0] = true;
                        previewMap.setImage(new Image(directMapUrl, true));
                        routeStatus.setText("Trace directe affichee. Calcul du trajet routier en option...");
                    });

                    String routeMapUrl = eventLocationService.buildRouteStaticMapUrl(routeOrigin, currentEventLocation);
                    Platform.runLater(() -> {
                        routeFinished[0] = true;
                        previewMap.setImage(new Image(routeMapUrl, true));
                        routeStatus.setText("Depart : " + origin + " | Destination : " + currentEventLocation.query());
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        routeFinished[0] = true;
                        String message = ex.getMessage() == null ? "" : ex.getMessage();
                        if (message.contains("HTTP 429")) {
                            routeStatus.setText("Quota LocationIQ depasse. Reessayez dans quelques minutes ou ouvrez le trajet dans le navigateur.");
                            showAlert(Alert.AlertType.WARNING, "Quota LocationIQ", "LocationIQ a refuse la requete avec HTTP 429. Cela signifie trop de requetes ou quota gratuit depasse.");
                        } else {
                            routeStatus.setText("Trajet routier indisponible. Verifiez l'adresse de depart ou la cle LocationIQ.");
                            showAlert(Alert.AlertType.WARNING, "Itineraire", message);
                        }
                    });
                }
            }, "event-route-preview-loader");
            routeWorker.setDaemon(true);
            routeWorker.start();
        });

        Button browserBtn = new Button("Ouvrir dans navigateur");
        browserBtn.getStyleClass().add("event-link-button");
        browserBtn.setOnAction(e -> ouvrirItineraireNavigateur(safeValue(originField.getText()).trim()));

        HBox controls = new HBox(10, originField, previewBtn, browserBtn);
        controls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(originField, Priority.ALWAYS);

        StackPane webFrame = new StackPane(previewMap, routeStatus);
        webFrame.getStyleClass().add("directions-web-frame");

        content.getChildren().addAll(badge, title, helper, controls, webFrame);
        dialog.getDialogPane().setContent(content);

        Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setText("Fermer");
        closeButton.getStyleClass().add("cancel-button");
        dialog.showAndWait();
    }

    private void ouvrirItineraireNavigateur(String origin) {
        try {
            String directionsUrl = eventLocationService.buildDirectionsUrl(origin, currentEventLocation);
            if (!Desktop.isDesktopSupported()) {
                showAlert(Alert.AlertType.INFORMATION, "Itineraire", directionsUrl);
                return;
            }
            Desktop.getDesktop().browse(URI.create(directionsUrl));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Itineraire", "Impossible d'ouvrir l'itineraire : " + e.getMessage());
        }
    }

    private void changerZoomCarte(int delta) {
        if (currentEventLocation == null) {
            showAlert(Alert.AlertType.INFORMATION, "Carte", "La carte n'est pas encore disponible.");
            return;
        }
        currentMapZoom = Math.max(3, Math.min(18, currentMapZoom + delta));
        if (mapImageView != null) {
            String mapUrl = eventLocationService.buildStaticMapUrl(currentEventLocation, currentMapZoom);
            mapImageView.setImage(new Image(mapUrl, true));
        }
        if (mapStatusLabel != null) {
            mapStatusLabel.setText(currentEventLocation.query() + " | Zoom " + currentMapZoom);
            mapStatusLabel.setVisible(true);
        }
    }

    @FXML
    private void ouvrirMesParticipations() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Connexion requise", "Connectez-vous pour voir vos participations.");
            return;
        }

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/FrontFXML/MesParticipationsEvenement.fxml"));
            Stage stage = resolveCurrentStage();
            if (stage != null) {
                stage.setScene(new Scene(root, 1400, 820));
                stage.setTitle("Mes participations evenement");
                stage.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir vos participations : " + e.getMessage());
        }
    }

    private void initMesParticipationsIfExists() {
        if (participationsContainer == null) return;

        User user = SessionManager.getCurrentUser();
        participationsContainer.getChildren().clear();
        if (user == null) {
            if (participationSummaryLabel != null) {
                participationSummaryLabel.setText("Connectez-vous pour consulter vos participations.");
            }
            participationsContainer.getChildren().add(createEmptyParticipationCard("Connexion requise"));
            return;
        }

        try {
            List<ParticipationEntry> entries = new ArrayList<>();
            List<Evenement> allEvents = evenementService.recuperer();
            for (Evenement ev : allEvents) {
                for (ParticipationDemande demande : participationService.getDemandes(ev)) {
                    if (demande.getUserId() == user.getId()) {
                        entries.add(new ParticipationEntry(ev, demande));
                    }
                }
            }

            long accepted = entries.stream()
                    .filter(e -> ParticipationDemande.STATUS_ACCEPTED.equals(e.demande().getStatus()))
                    .count();
            if (participationSummaryLabel != null) {
                participationSummaryLabel.setText(entries.size() + " demande(s) | " + accepted + " ticket(s) disponible(s)");
            }

            if (entries.isEmpty()) {
                participationsContainer.getChildren().add(createEmptyParticipationCard("Aucune participation pour le moment."));
                return;
            }

            for (ParticipationEntry entry : entries) {
                participationsContainer.getChildren().add(createParticipationCard(entry.event(), entry.demande(), user, allEvents));
            }
            startParticipationCareRealtime(user);
            showFirstCancellationCareIfNeeded(entries, user, allEvents);
        } catch (Exception e) {
            participationsContainer.getChildren().add(createEmptyParticipationCard("Impossible de charger vos participations."));
        }
    }

    private void updateFrontCancellationToast() {
        if (eventUpdateToast == null) return;

        User user = SessionManager.getCurrentUser();
        if (user == null) {
            eventUpdateToast.setVisible(false);
            eventUpdateToast.setManaged(false);
            return;
        }

        try {
            boolean hasCancelledParticipation = false;
            for (Evenement ev : evenementService.recuperer()) {
                if (!cancellationCareService.isCancelled(ev)) {
                    continue;
                }
                for (ParticipationDemande demande : participationService.getDemandes(ev)) {
                    if (demande.getUserId() == user.getId() && isCareEligibleParticipation(demande)) {
                        hasCancelledParticipation = true;
                        break;
                    }
                }
                if (hasCancelledParticipation) {
                    break;
                }
            }

            showFrontCancellationToast(hasCancelledParticipation);
        } catch (Exception ignored) {
            showFrontCancellationToast(false);
        }
    }

    private void showFrontCancellationToast(boolean show) {
        if (eventUpdateToast == null) return;

        eventUpdateToast.setVisible(show);
        eventUpdateToast.setManaged(show);
        if (!show) {
            eventUpdateToastAnimated = false;
            floatingEventUpdatePopupShown = false;
            eventUpdateToast.setOpacity(1);
            eventUpdateToast.setTranslateX(0);
            return;
        }

        showFloatingEventUpdatePopup();

        if (eventUpdateToastAnimated) {
            return;
        }
        eventUpdateToastAnimated = true;
        eventUpdateToast.setOpacity(0);
        eventUpdateToast.setTranslateX(180);

        FadeTransition fade = new FadeTransition(Duration.millis(520), eventUpdateToast);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(520), eventUpdateToast);
        slide.setFromX(180);
        slide.setToX(0);
        slide.setCycleCount(1);

        fade.play();
        slide.play();
    }

    private void showFloatingEventUpdatePopup() {
        if (floatingEventUpdatePopupShown || eventUpdateToast == null || eventUpdateToast.getScene() == null) {
            return;
        }
        floatingEventUpdatePopupShown = true;

        Stage stage = resolveCurrentStage();
        if (stage == null) return;

        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);

        VBox card = new VBox(6);
        card.getStyleClass().add("event-floating-toast");
        card.setPrefWidth(390);
        card.setOnMouseClicked(e -> {
            popup.hide();
            ouvrirMesParticipations();
        });

        Label title = new Label("Mise a jour importante");
        title.getStyleClass().add("event-floating-toast-title");

        Label message = new Label("Un evenement de vos participations a ete annule. Cliquez ici pour consulter Mes participations.");
        message.setWrapText(true);
        message.getStyleClass().add("event-floating-toast-message");

        card.getChildren().addAll(title, message);

        URL css = getClass().getResource("/CSS/evenement.css");
        if (css != null) {
            card.getStylesheets().add(css.toExternalForm());
        }

        card.setOpacity(0);
        card.setTranslateX(220);
        popup.getContent().add(card);

        double x = stage.getX() + stage.getWidth() - 430;
        double y = stage.getY() + 92;
        popup.show(stage, Math.max(stage.getX() + 20, x), y);

        FadeTransition fade = new FadeTransition(Duration.millis(520), card);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(520), card);
        slide.setFromX(220);
        slide.setToX(0);

        fade.play();
        slide.play();

        PauseTransition pause = new PauseTransition(Duration.seconds(8));
        pause.setOnFinished(e -> {
            if (popup.isShowing()) {
                FadeTransition out = new FadeTransition(Duration.millis(260), card);
                out.setFromValue(card.getOpacity());
                out.setToValue(0);
                out.setOnFinished(done -> popup.hide());
                out.play();
            }
        });
        pause.play();
    }

    private VBox createEmptyParticipationCard(String message) {
        VBox card = new VBox(10);
        card.setPrefWidth(420);
        card.getStyleClass().add("participation-front-card");
        Label title = new Label(message);
        title.setWrapText(true);
        title.getStyleClass().add("participation-front-title");
        card.getChildren().add(title);
        return card;
    }

    private VBox createParticipationCard(Evenement ev, ParticipationDemande demande, User user, List<Evenement> allEvents) {
        VBox card = new VBox(12);
        card.setPrefWidth(350);
        card.getStyleClass().add("participation-front-card");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label status = new Label(getParticipationStatusLabel(demande.getStatus()));
        status.setStyle(getStatusChipStyle(demande.getStatus()));
        Label type = new Label(valueOrDash(ev.getType_event()));
        type.getStyleClass().add("participation-type-chip");
        top.getChildren().addAll(status, type);

        Label title = new Label(valueOrDash(ev.getTitre_event()));
        title.setWrapText(true);
        title.getStyleClass().add("participation-front-title");

        Label meta = new Label(valueOrDash(ev.getVille_event()) + " | " + dateText(ev.getDate_debut_event()) + " -> " + dateText(ev.getDate_fin_event()));
        meta.setWrapText(true);
        meta.getStyleClass().add("participation-front-meta");

        Label ticket = new Label(safeValue(demande.getTicketCode()).isBlank()
                ? "Ticket QR disponible apres acceptation"
                : "Ticket : " + demande.getTicketCode());
        ticket.setWrapText(true);
        ticket.getStyleClass().add("participation-front-ticket");

        HBox actions = new HBox(10);
        Button detailBtn = new Button("Detail");
        detailBtn.getStyleClass().add("event-link-button");
        detailBtn.setOnAction(e -> ouvrirDetailEvenementFront(ev));
        actions.getChildren().add(detailBtn);

        if (cancellationCareService.isCancelled(ev) && isCareEligibleParticipation(demande)) {
            Button careBtn = new Button("Care Assistant");
            careBtn.getStyleClass().add("care-assistant-button");
            careBtn.setOnAction(e -> ouvrirCancellationCareRoom(ev, demande, user, allEvents));
            actions.getChildren().add(careBtn);
        }

        if (ParticipationDemande.STATUS_ACCEPTED.equals(demande.getStatus())) {
            Button pdfBtn = new Button("PDF QR");
            pdfBtn.getStyleClass().add("confirm-button");
            pdfBtn.setOnAction(e -> telechargerTicketParticipation(ev, demande, user));
            actions.getChildren().add(pdfBtn);
        }

        card.getChildren().addAll(top, title, meta, ticket, actions);
        return card;
    }

    private void telechargerTicketParticipation(Evenement ev, ParticipationDemande demande, User user) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Telecharger le ticket QR");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
            String ticket = safeValue(demande.getTicketCode()).isBlank() ? "ticket" : demande.getTicketCode();
            fileChooser.setInitialFileName("ticket_" + ticket.replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf");

            Stage stage = resolveCurrentStage();
            File file = fileChooser.showSaveDialog(stage);
            if (file == null) return;

            pdfService.genererTicketParticipation(ev, demande, user, file.getAbsolutePath());
            showAlert(Alert.AlertType.INFORMATION, "Ticket genere", "Votre ticket PDF avec QR code est pret.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Ticket", "Impossible de generer le ticket : " + e.getMessage());
        }
    }

    private void startParticipationCareRealtime(User user) {
        if (participationsContainer == null || user == null) return;

        if (participationCareTimeline != null) {
            participationCareTimeline.stop();
        }

        participationCareTimeline = new Timeline(new KeyFrame(Duration.seconds(8), e -> detectCancellationCareRealtime(user)));
        participationCareTimeline.setCycleCount(Timeline.INDEFINITE);
        participationCareTimeline.play();
    }

    private void detectCancellationCareRealtime(User user) {
        if (participationsContainer == null || participationsContainer.getScene() == null) {
            if (participationCareTimeline != null) {
                participationCareTimeline.stop();
            }
            return;
        }

        try {
            List<Evenement> allEvents = evenementService.recuperer();
            List<ParticipationEntry> entries = new ArrayList<>();
            for (Evenement ev : allEvents) {
                for (ParticipationDemande demande : participationService.getDemandes(ev)) {
                    if (demande.getUserId() == user.getId()) {
                        entries.add(new ParticipationEntry(ev, demande));
                    }
                }
            }
            showFirstCancellationCareIfNeeded(entries, user, allEvents);
        } catch (Exception ignored) {
            // Real-time care polling must never block the participations page.
        }
    }

    private void showFirstCancellationCareIfNeeded(List<ParticipationEntry> entries, User user, List<Evenement> allEvents) {
        if (entries == null || user == null) return;

        for (ParticipationEntry entry : entries) {
            if (!cancellationCareService.isCancelled(entry.event()) || !isCareEligibleParticipation(entry.demande())) {
                continue;
            }
            int eventId = entry.event().getId();
            if (displayedCancellationCareEvents.add(eventId)) {
                Platform.runLater(() -> ouvrirCancellationCareRoom(entry.event(), entry.demande(), user, allEvents));
                return;
            }
        }
    }

    private void ouvrirCancellationCareRoom(Evenement ev, ParticipationDemande demande, User user, List<Evenement> allEvents) {
        List<Evenement> alternatives = cancellationCareService.recommendAlternatives(ev, allEvents, 3);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("MedFlow Care Assistant");
        dialog.setHeaderText(null);
        applyBookingStyles(dialog);
        dialog.getDialogPane().getStyleClass().add("participation-request-dialog");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPrefWidth(620);
        content.getStyleClass().add("care-room-card");

        Label badge = new Label("AI CANCELLATION CARE ROOM");
        badge.getStyleClass().add("care-room-overline");

        Label title = new Label("Votre evenement est annule. MedFlow vous accompagne.");
        title.setWrapText(true);
        title.getStyleClass().add("care-room-title");

        Label aiStatus = new Label(huggingFaceCareAiClient.isConfigured()
                ? "Mode IA cloud: Hugging Face actif | FR / EN / AR"
                : "Mode IA local: " + huggingFaceCareAiClient.getConfigurationStatus());
        aiStatus.setWrapText(true);
        aiStatus.getStyleClass().add("care-room-ai-status");

        TextArea conversation = new TextArea();
        conversation.setEditable(false);
        conversation.setWrapText(true);
        conversation.setPrefRowCount(9);
        conversation.setPrefHeight(260);
        conversation.getStyleClass().add("care-room-chat");
        String cancellationReason = evenementService.recupererMotifAnnulation(ev.getId());
        conversation.setText(cancellationCareService.buildOpeningMessage(ev, demande, user, alternatives, cancellationReason));

        Label emotionLabel = new Label("Comment vous sentez-vous ?");
        emotionLabel.getStyleClass().add("participation-request-subtitle");

        HBox emotions = new HBox(10);
        emotions.setAlignment(Pos.CENTER_LEFT);
        Button disappointedBtn = createCareEmotionButton(":-/ Decu", "care-emotion-sad");
        Button neutralBtn = createCareEmotionButton("OK Calme", "care-emotion-calm");
        Button angryBtn = createCareEmotionButton("! En colere", "care-emotion-angry");
        emotions.getChildren().addAll(disappointedBtn, neutralBtn, angryBtn);

        disappointedBtn.setOnAction(e -> sendCareMessage(
                "Je suis decu / disappointed / محبط", ev, user, alternatives, conversation, aiStatus, null));
        neutralBtn.setOnAction(e -> sendCareMessage(
                "Je suis calme, donne-moi la solution / I am calm / انا هادئ", ev, user, alternatives, conversation, aiStatus, null));
        angryBtn.setOnAction(e -> sendCareMessage(
                "Je suis en colere / angry / غاضب", ev, user, alternatives, conversation, aiStatus, null));

        TextField input = new TextField();
        input.setPromptText("FR / EN / AR: compensation, alternative, why cancelled, علاش تلغى؟");
        input.getStyleClass().add("modern-text-field");

        Button send = new Button("Envoyer");
        send.getStyleClass().add("confirm-button");
        send.setOnAction(e -> {
            String message = safeValue(input.getText()).trim();
            if (message.isBlank()) return;
            sendCareMessage(message, ev, user, alternatives, conversation, aiStatus, send);
            input.clear();
        });
        input.setOnAction(e -> send.fire());

        HBox inputRow = new HBox(10, input, send);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(input, Priority.ALWAYS);

        Label compensation = new Label("Code prioritaire: " + cancellationCareService.buildCompensationCode(ev, user));
        compensation.setWrapText(true);
        compensation.getStyleClass().add("care-room-compensation");

        content.getChildren().addAll(badge, title, aiStatus, conversation, emotionLabel, emotions, inputRow, compensation);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(660);

        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.getStyleClass().add("cancel-button");

        dialog.showAndWait();
    }

    private Button createCareEmotionButton(String label, String accentStyleClass) {
        Button button = new Button(label);
        button.getStyleClass().add("care-emotion-button");
        button.getStyleClass().add(accentStyleClass);
        return button;
    }

    private void sendCareMessage(
            String message,
            Evenement ev,
            User user,
            List<Evenement> alternatives,
            TextArea conversation,
            Label aiStatus,
            Button sendButton
    ) {
        appendCareReply(conversation, "Vous: " + message);
        String cancellationReason = evenementService.recupererMotifAnnulation(ev.getId());

        if (!huggingFaceCareAiClient.isConfigured()) {
            appendCareReply(conversation, cancellationCareService.replyToMessage(message, ev, user, alternatives, cancellationReason));
            String configStatus = huggingFaceCareAiClient.getConfigurationStatus();
            aiStatus.setText("Mode IA local actif: " + configStatus);
            appendCareReply(conversation, "Diagnostic IA: " + configStatus);
            return;
        }

        aiStatus.setText("Hugging Face reflechit en temps reel...");
        if (sendButton != null) {
            sendButton.setDisable(true);
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return huggingFaceCareAiClient.generateCareReply(
                        cancellationCareService.buildCloudSystemPrompt(),
                        cancellationCareService.buildCloudUserPrompt(message, ev, user, alternatives, cancellationReason)
                );
            }
        };

        task.setOnSucceeded(e -> {
            String localFallback = cancellationCareService.replyToMessage(message, ev, user, alternatives, cancellationReason);
            appendCareReply(conversation, "MedFlow AI: " + sanitizeAiReply(task.getValue(), localFallback));
            aiStatus.setText("Mode IA cloud: Hugging Face actif | FR / EN / AR");
            if (sendButton != null) {
                sendButton.setDisable(false);
            }
        });

        task.setOnFailed(e -> {
            Throwable error = task.getException();
            String reason = buildAiDiagnostic(error);
            if (huggingFaceCareAiClient.isCloudRequired()) {
                appendCareReply(conversation, "MedFlow AI: Hugging Face est configure comme obligatoire, mais la connexion a echoue. "
                        + "Verifiez internet, le token, le modele et l'endpoint Hugging Face, puis reessayez.");
                aiStatus.setText("Hugging Face requis mais indisponible: " + shortText(reason, 90));
            } else {
                appendCareReply(conversation, "MedFlow local: "
                        + cancellationCareService.replyToMessage(message, ev, user, alternatives, cancellationReason));
                aiStatus.setText("Hugging Face indisponible, reponse locale utilisee: " + shortText(reason, 90));
            }
            if (sendButton != null) {
                sendButton.setDisable(false);
            }
        });

        Thread worker = new Thread(task, "medflow-care-ai");
        worker.setDaemon(true);
        worker.start();
    }

    private String sanitizeAiReply(String rawReply, String localFallback) {
        String reply = safeValue(rawReply).trim();
        if (reply.isBlank()) {
            return localFallback;
        }

        String[] markers = {
                "Possible response:",
                "Final answer:",
                "Final response:",
                "Participant-facing answer:",
                "Answer:",
                "Response:"
        };
        for (String marker : markers) {
            int index = reply.toLowerCase(Locale.ROOT).lastIndexOf(marker.toLowerCase(Locale.ROOT));
            if (index >= 0) {
                reply = reply.substring(index + marker.length()).trim();
                break;
            }
        }

        reply = reply.replaceAll("(?is)<think>.*?</think>", "").trim();
        if (reply.isBlank()) {
            return localFallback;
        }

        String[] reasoningStarts = {
                "Okay,", "First,", "I need to", "The user", "Let me", "Looking at", "I notice",
                "Possible response", "Wait,", "*checks", "Important details", "The context"
        };
        boolean looksLikeReasoning = false;
        for (String start : reasoningStarts) {
            if (reply.startsWith(start)) {
                looksLikeReasoning = true;
                break;
            }
        }

        if (looksLikeReasoning) {
            return localFallback;
        }

        return reply;
    }

    private String buildAiDiagnostic(Throwable error) {
        if (error == null) {
            return "Erreur inconnue: aucune exception recue.";
        }

        String message = safeValue(error.getMessage());
        String type = error.getClass().getSimpleName();
        if (message.isBlank()) {
            Throwable cause = error.getCause();
            if (cause != null) {
                String causeMessage = safeValue(cause.getMessage());
                return type + " cause " + cause.getClass().getSimpleName()
                        + (causeMessage.isBlank() ? "" : ": " + causeMessage);
            }
            return type + ": message vide. Verifiez internet, modele active et permissions Hugging Face.";
        }
        return type + ": " + message;
    }

    private boolean isCareEligibleParticipation(ParticipationDemande demande) {
        if (demande == null) return false;
        String status = safeValue(demande.getStatus());
        return ParticipationDemande.STATUS_ACCEPTED.equals(status)
                || ParticipationDemande.STATUS_PENDING.equals(status)
                || status.isBlank();
    }

    private void appendCareReply(TextArea conversation, String message) {
        conversation.appendText("\n\n" + message);
        conversation.setScrollTop(Double.MAX_VALUE);
    }

    private record ParticipationEntry(Evenement event, ParticipationDemande demande) {
    }

    @FXML
    private void ouvrirFormulaireParticipation() {
        if (evenementSelectionneFront == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun evenement selectionne.");
            return;
        }

        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showAlert(Alert.AlertType.WARNING, "Connexion requise", "Veuillez vous connecter avant de participer a cet evenement.");
            return;
        }

        if (participationService.userHasDemande(evenementSelectionneFront, user.getId())) {
            showParticipationNotice(
                    "Demande deja envoyee",
                    "Votre participation est deja enregistree pour cet evenement.",
                    "Vous pouvez suivre son statut depuis la page Mes participations."
            );
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Participation evenement");
        dialog.setHeaderText(null);
        applyBookingStyles(dialog);
        dialog.getDialogPane().getStyleClass().add("participation-request-dialog");
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Oui, envoyer", ButtonBar.ButtonData.OK_DONE),
                ButtonType.CANCEL
        );

        VBox form = new VBox(18);
        form.setPrefWidth(620);
        form.getStyleClass().add("participation-request-card");

        Label overline = new Label("DEMANDE DE PARTICIPATION");
        overline.getStyleClass().add("participation-request-overline");

        Label question = new Label("Souhaitez-vous participer a cet evenement ?");
        question.setWrapText(true);
        question.getStyleClass().add("participation-request-title");

        Label subtitle = new Label(valueOrDash(evenementSelectionneFront.getTitre_event()) + " | Votre participation sera acceptee automatiquement s'il reste des places.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("participation-request-subtitle");

        TextField nomField = createParticipationField((safeValue(user.getPrenom()) + " " + safeValue(user.getNom())).trim());
        nomField.setEditable(false);

        TextField emailField = createParticipationField(safeValue(user.getEmailUser()));
        emailField.setEditable(false);

        TextField telField = createParticipationField(participationService.normalizeTunisianPhone(user.getTelephoneUser()));
        telField.setPromptText("+216XXXXXXXX");

        TextArea motifArea = new TextArea();
        motifArea.setPromptText("Motif de participation (optionnel)");
        motifArea.setWrapText(true);
        motifArea.setPrefRowCount(4);
        motifArea.getStyleClass().add("modern-text-area");

        form.getChildren().addAll(
                overline,
                question,
                subtitle,
                buildParticipationField("Nom et prenom", nomField),
                buildParticipationField("Email", emailField),
                buildParticipationField("Telephone", telField),
                buildParticipationField("Motif", motifArea)
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setMinWidth(680);

        Button sendButton = (Button) dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
        sendButton.getStyleClass().add("confirm-button");
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Annuler");
        cancelButton.getStyleClass().add("cancel-button");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        if (participationService.userHasDemande(evenementSelectionneFront, user.getId())) {
            showParticipationNotice(
                    "Demande deja envoyee",
                    "Votre participation est deja enregistree pour cet evenement.",
                    "Aucune nouvelle demande n'a ete creee."
            );
            return;
        }

        String phone = participationService.normalizeTunisianPhone(telField.getText());

        ParticipationDemande demande = new ParticipationDemande();
        demande.setUserId(user.getId());
        demande.setNom(safeValue(user.getNom()));
        demande.setPrenom(safeValue(user.getPrenom()));
        demande.setEmail(safeValue(user.getEmailUser()));
        demande.setTelephone(phone);
        demande.setMotif(safeValue(motifArea.getText()).trim());

        try {
            ParticipationDemande created = participationService.ajouterDemande(evenementSelectionneFront, demande);
            if (ParticipationDemande.STATUS_ACCEPTED.equals(created.getStatus())) {
                showParticipationNotice(
                        "Participation acceptee",
                        "Votre participation est acceptee automatiquement.",
                        "Une place etait disponible pour cet evenement."
                );
            } else {
                showParticipationNotice(
                        "Liste d'attente",
                        "La capacite maximale est atteinte.",
                        "Votre demande a ete ajoutee a la liste d'attente."
                );
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'enregistrer la demande : " + e.getMessage());
        }
    }

    private VBox buildParticipationField(String labelText, Control field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        VBox box = new VBox(6, label, field);
        return box;
    }

    private TextField createParticipationField(String value) {
        TextField field = new TextField(value == null ? "" : value);
        field.getStyleClass().add("modern-text-field");
        return field;
    }

    private void showParticipationNotice(String titleText, String messageText, String detailText) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(titleText);
        dialog.setHeaderText(null);
        applyBookingStyles(dialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getStyleClass().add("participation-request-dialog");

        VBox content = new VBox(12);
        content.setPrefWidth(500);
        content.getStyleClass().add("participation-notice-card");

        Label badge = new Label("PARTICIPATION");
        badge.getStyleClass().add("participation-request-overline");

        Label title = new Label(titleText);
        title.setWrapText(true);
        title.getStyleClass().add("participation-request-title");

        Label message = new Label(messageText);
        message.setWrapText(true);
        message.getStyleClass().add("participation-request-subtitle");

        Label detail = new Label(detailText);
        detail.setWrapText(true);
        detail.getStyleClass().add("participation-front-ticket");

        content.getChildren().addAll(badge, title, message, detail);
        dialog.getDialogPane().setContent(content);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("Compris");
        okButton.getStyleClass().add("confirm-button");
        dialog.showAndWait();
    }

    private void applyBookingStyles(Dialog<?> dialog) {
        URL css = getClass().getResource("/CSS/booking.css");
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        URL eventCss = getClass().getResource("/CSS/evenement.css");
        if (eventCss != null) {
            dialog.getDialogPane().getStylesheets().add(eventCss.toExternalForm());
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
            Parent root = FXMLLoader.load(getClass().getResource("/FrontFXML/Événement.fxml"));
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


    @FXML
    private void retourVersListeFrontFixe() {
        try {
            URL resource = getClass().getResource("/FrontFXML/Événement.fxml");
            if (resource == null) {
                throw new IOException("Fichier /FrontFXML/Événement.fxml introuvable.");
            }

            Parent root = FXMLLoader.load(resource);
            Stage stage = resolveCurrentStage();
            if (stage != null) {
                applySceneToStage(stage, root, "Evenements");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir a la liste : " + e.getMessage());
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

    private void initCardsFrontIfExists() {
        if (cardsContainer == null) return;


        try {
            List<Evenement> events = evenementService.recuperer();
            cardsContainer.getChildren().clear();

            for (Evenement ev : events) {
                if (isVisibleOnFront(ev)) {
                    cardsContainer.getChildren().add(createEventCardFront(ev));
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }
    private void initDashboardBIIfExists() {
        if (biPeriodeLabel == null) return;

        if (biDateFinPicker != null && biDateFinPicker.getValue() == null) {
            biDateFinPicker.setValue(LocalDate.now());
        }

        if (biDateDebutPicker != null && biDateDebutPicker.getValue() == null) {
            biDateDebutPicker.setValue(LocalDate.now().minusDays(6));
        }

        if (biVilleCombo != null && biVilleCombo.getItems().isEmpty()) {
            biVilleCombo.getItems().addAll(
                    "Toutes les villes", "Tunis", "Sfax", "Sousse", "Ariana"
            );
            biVilleCombo.setValue("Toutes les villes");
        }

        initTopEvenementsTable();
        chargerDashboardBI();
    }

    @FXML
    private void filtrerDashboardBI() {
        chargerDashboardBI();
    }

    @FXML
    private void chargerPeriode7j() {
        if (biDateFinPicker != null) biDateFinPicker.setValue(LocalDate.now());
        if (biDateDebutPicker != null) biDateDebutPicker.setValue(LocalDate.now().minusDays(6));
        chargerDashboardBI();
    }

    @FXML
    private void chargerPeriode30j() {
        if (biDateFinPicker != null) biDateFinPicker.setValue(LocalDate.now());
        if (biDateDebutPicker != null) biDateDebutPicker.setValue(LocalDate.now().minusDays(29));
        chargerDashboardBI();
    }

    @FXML
    private void chargerPeriode90j() {
        if (biDateFinPicker != null) biDateFinPicker.setValue(LocalDate.now());
        if (biDateDebutPicker != null) biDateDebutPicker.setValue(LocalDate.now().minusDays(89));
        chargerDashboardBI();
    }

    private void chargerDashboardBI() {
        try {
            LocalDate debut = biDateDebutPicker != null ? biDateDebutPicker.getValue() : LocalDate.now().minusDays(6);
            LocalDate fin = biDateFinPicker != null ? biDateFinPicker.getValue() : LocalDate.now();
            String ville = biVilleCombo != null ? biVilleCombo.getValue() : "Toutes les villes";

            DashboardBIServiceEvenement.DashboardData data =
                    dashboardService.chargerDonnees(debut, fin, ville);

            if (biPeriodeLabel != null) biPeriodeLabel.setText("Période : " + data.dateDebut + " → " + data.dateFin);

            if (biPublicationLabel != null) biPublicationLabel.setText(data.publicationMessage);
            if (biCapaciteLabel != null) biCapaciteLabel.setText(data.capaciteMessage);

            if (biTotalEventsLabel != null) biTotalEventsLabel.setText(String.valueOf(data.totalEvenements));
            if (biPubliesLabel != null) biPubliesLabel.setText(String.valueOf(data.publies));
            if (biCapaciteTotaleLabel != null) biCapaciteTotaleLabel.setText(String.valueOf(data.capaciteTotale));
            if (biTauxPublicationLabel != null) biTauxPublicationLabel.setText(String.format(Locale.US, "%.1f%%", data.tauxPublication));

            if (biBrouillonsLabel != null) biBrouillonsLabel.setText(String.valueOf(data.brouillons));
            if (biAnnulesLabel != null) biAnnulesLabel.setText(String.valueOf(data.annules));
            if (biAVenirLabel != null) biAVenirLabel.setText(String.valueOf(data.evenementsAVenir));
            if (biPeriodeAnalyseeLabel != null) biPeriodeAnalyseeLabel.setText(String.valueOf(data.periodeAnalysee));

            remplirLineChartBI(data);
            remplirPieChartBI(data);
            remplirTypeChartBI(data);
            remplirVilleChartBI(data);

            if (topEvenementsTable != null) {
                topEvenementsTable.setItems(FXCollections.observableArrayList(data.topEvenements));
            }

            remplirRecommandationsBI(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTopEvenementsTable() {
        if (topEvenementsTable == null) return;

        if (topTitreCol != null) {
            topTitreCol.setCellValueFactory(cell -> new SimpleStringProperty(safeValue(cell.getValue().getTitre_event())));
        }
        if (topTypeCol != null) {
            topTypeCol.setCellValueFactory(cell -> new SimpleStringProperty(safeValue(cell.getValue().getType_event())));
        }
        if (topVilleCol != null) {
            topVilleCol.setCellValueFactory(cell -> new SimpleStringProperty(safeValue(cell.getValue().getVille_event())));
        }
        if (topCapaciteCol != null) {
            topCapaciteCol.setCellValueFactory(cell -> Bindings.createIntegerBinding(
                    () -> cell.getValue().getNb_participants_max_event()
            ));
        }
        if (topStatutCol != null) {
            topStatutCol.setCellValueFactory(cell -> new SimpleStringProperty(safeValue(cell.getValue().getStatut_event())));
        }
    }

    private void remplirLineChartBI(DashboardBIServiceEvenement.DashboardData data) {
        if (eventsLineChart == null) return;

        eventsLineChart.getData().clear();

        XYChart.Series<String, Number> serieEvents = new XYChart.Series<>();
        serieEvents.setName("Événements");

        XYChart.Series<String, Number> serieCapacite = new XYChart.Series<>();
        serieCapacite.setName("Capacité");

        for (var entry : data.eventsParJour.entrySet()) {
            serieEvents.getData().add(new XYChart.Data<>(entry.getKey().toString(), entry.getValue()));
        }

        for (var entry : data.capaciteParJour.entrySet()) {
            serieCapacite.getData().add(new XYChart.Data<>(entry.getKey().toString(), entry.getValue()));
        }

        eventsLineChart.getData().addAll(serieEvents, serieCapacite);
    }

    private void remplirPieChartBI(DashboardBIServiceEvenement.DashboardData data) {
        if (statutPieChart == null) return;

        statutPieChart.getData().clear();

        for (var entry : data.repartitionStatuts.entrySet()) {
            statutPieChart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
    }

    private void remplirTypeChartBI(DashboardBIServiceEvenement.DashboardData data) {
        if (typeBarChart == null) return;

        typeBarChart.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Types");

        for (var entry : data.parType.entrySet()) {
            serie.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        typeBarChart.getData().add(serie);
    }

    private void remplirVilleChartBI(DashboardBIServiceEvenement.DashboardData data) {
        if (villeBarChart == null) return;

        villeBarChart.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Villes");

        for (var entry : data.parVille.entrySet()) {
            serie.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        villeBarChart.getData().add(serie);
    }

    private void remplirRecommandationsBI(DashboardBIServiceEvenement.DashboardData data) {
        if (recommandationsContainer == null) return;

        recommandationsContainer.getChildren().clear();

        for (String rec : data.recommandations) {
            Label lbl = new Label("💡 " + rec);
            lbl.getStyleClass().add("recommendation-chip");
            recommandationsContainer.getChildren().add(lbl);
        }
    }

    @FXML
    private void handleExportPDF() {
        try {
            Evenement selectedEvent = evenementTable != null ? evenementTable.getSelectionModel().getSelectedItem() : null;
            if (selectedEvent == null) {
                showAlert(Alert.AlertType.WARNING, "Selection requise", "Choisissez d'abord un evenement dans le tableau.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer la fiche PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf")
            );
            String baseName = safeValue(selectedEvent.getSlug_event()).isBlank() ? "evenement" : safeValue(selectedEvent.getSlug_event());
            fileChooser.setInitialFileName("fiche_" + baseName.replaceAll("\\s+", "_") + ".pdf");

            Stage stage = resolveCurrentStage();
            File file = fileChooser.showSaveDialog(stage);

            if (file == null) return;

            pdfService.genererFicheEvenement(selectedEvent, file.getAbsolutePath());

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Rapport PDF généré avec succès !");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de générer le PDF : " + e.getMessage());
        }
    }

    private boolean isVisibleOnFront(Evenement evenement) {
        String statut = safeValue(evenement.getStatut_event()).toLowerCase(Locale.ROOT);
        String visibilite = safeValue(evenement.getVisibilite_event()).toLowerCase(Locale.ROOT);
        return statut.contains("publi") && visibilite.contains("public");
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private String dateText(java.util.Date date) {
        return date == null ? "-" : date.toString();
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





