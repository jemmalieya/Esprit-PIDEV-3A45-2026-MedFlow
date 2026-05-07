package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Ressource;
import tn.esprit.entities.User;
import tn.esprit.services.DashboardBIServiceEvenement;
import tn.esprit.services.EvenementService;
import tn.esprit.services.ParticipationDemandeService;
import tn.esprit.services.ParticipationDemandeService.ParticipationDemande;
import tn.esprit.services.RessourceService;
import tn.esprit.tools.SessionManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AdminEvenementController {

    public enum Section {
        LIST_EVENTS,
        PARTICIPANT_REQUESTS,
        RESOURCES,
        EVENT_STATS,
        RESOURCE_STATS
    }

    private static Section nextSection = Section.LIST_EVENTS;

    public static void showSection(Section section) {
        nextSection = section == null ? Section.LIST_EVENTS : section;
    }

    @FXML private BorderPane rootPane;
    @FXML private Label dateLabel;
    @FXML private TextField topSearchField;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label heroTitleLabel;
    @FXML private Label heroSubtitleLabel;
    @FXML private Label heroBadgeLabel;
    @FXML private Label breadcrumbSectionLabel;
    @FXML private Label sectionChipLabel;
    @FXML private Label heroChipOneLabel;
    @FXML private Label heroChipTwoLabel;
    @FXML private Label heroChipThreeLabel;
    @FXML private Label kpi1TitleLabel;
    @FXML private Label kpi1ValueLabel;
    @FXML private Label kpi1SubtitleLabel;
    @FXML private Label kpi2TitleLabel;
    @FXML private Label kpi2ValueLabel;
    @FXML private Label kpi2SubtitleLabel;
    @FXML private Label kpi3TitleLabel;
    @FXML private Label kpi3ValueLabel;
    @FXML private Label kpi3SubtitleLabel;
    @FXML private Label kpi4TitleLabel;
    @FXML private Label kpi4ValueLabel;
    @FXML private Label kpi4SubtitleLabel;
    @FXML private HBox filterCard;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label resultCountLabel;
    @FXML private Label tableTitleLabel;
    @FXML private Label tableHelpLabel;
    @FXML private HBox tableHeader;
    @FXML private VBox rowsContainer;
    @FXML private VBox insightsCard;
    @FXML private Label insightsTitleLabel;
    @FXML private HBox insightsRow;
    @FXML private VBox experienceDeck;
    @FXML private VBox chartsSection;
    @FXML private HBox pulseRow;
    @FXML private HBox operationsRow;
    @FXML private Label chartOneTitleLabel;
    @FXML private Label chartOneSubtitleLabel;
    @FXML private Label chartTwoTitleLabel;
    @FXML private Label chartTwoSubtitleLabel;
    @FXML private Label chartThreeTitleLabel;
    @FXML private Label chartThreeSubtitleLabel;
    @FXML private Label chartFourTitleLabel;
    @FXML private Label chartFourSubtitleLabel;
    @FXML private LineChart<String, Number> eventsLineChart;
    @FXML private PieChart statsPieChart;
    @FXML private BarChart<String, Number> primaryBarChart;
    @FXML private BarChart<String, Number> secondaryBarChart;
    @FXML private Label securityHeadlineLabel;
    @FXML private Label securityPendingLabel;
    @FXML private Label securityDraftLabel;
    @FXML private Label securityDeadlineLabel;
    @FXML private Label securityAdviceLabel;
    @FXML private Label participationHubTitleLabel;
    @FXML private Label participationHubEventLabel;
    @FXML private Label participationHubMetaLabel;
    @FXML private Button participationHubButton;
    @FXML private Button participationDetailsButton;
    @FXML private Label calendarMonthLabel;
    @FXML private Label calendarSelectionHintLabel;
    @FXML private GridPane calendarWeekHeader;
    @FXML private GridPane calendarMonthGrid;

    private final EvenementService evenementService = new EvenementService();
    private final ParticipationDemandeService participationService = new ParticipationDemandeService();
    private final RessourceService ressourceService = new RessourceService();
    private final DashboardBIServiceEvenement dashboardService = new DashboardBIServiceEvenement();

    private final List<Evenement> allEvents = new ArrayList<>();
    private final List<Ressource> allResources = new ArrayList<>();
    private final List<ParticipationRow> allParticipationRows = new ArrayList<>();
    private final Map<Integer, Evenement> eventsById = new LinkedHashMap<>();

    private Section currentSection;
    private DashboardBIServiceEvenement.DashboardData dashboardData;
    private boolean updatingSortOptions;
    private YearMonth currentCalendarMonth = YearMonth.now();
    private Evenement suggestedEvent;

    @FXML
    public void initialize() {
        currentSection = nextSection;
        dateLabel.setText("Admin • Événements • " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> renderCurrentSection());
        }
        if (topSearchField != null) {
            topSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (searchField != null && !Objects.equals(searchField.getText(), newValue)) {
                    searchField.setText(newValue);
                }
            });
        }
        if (sortCombo != null) {
            sortCombo.setOnAction(e -> {
                if (!updatingSortOptions) {
                    renderCurrentSection();
                }
            });
        }
        Platform.runLater(this::loadAllData);
    }

    private void loadAllData() {
        try {
            allEvents.clear();
            allEvents.addAll(evenementService.recuperer());
            eventsById.clear();
            for (Evenement event : allEvents) {
                eventsById.put(event.getId(), event);
            }

            allResources.clear();
            allResources.addAll(ressourceService.recuperer());

            allParticipationRows.clear();
            for (Evenement event : allEvents) {
                for (ParticipationDemande demande : participationService.getDemandes(event)) {
                    allParticipationRows.add(new ParticipationRow(event, demande));
                }
            }

            dashboardData = dashboardService.chargerDonnees(LocalDate.now().minusDays(89), LocalDate.now(), "Toutes les villes");
            renderCurrentSection();
        } catch (Exception e) {
            e.printStackTrace();
            showWarning("Chargement", "Impossible de charger les données événements : " + e.getMessage());
        }
    }

    private void renderCurrentSection() {
        if (currentSection == null) {
            currentSection = Section.LIST_EVENTS;
        }
        nextSection = currentSection;

        switch (currentSection) {
            case LIST_EVENTS -> renderEventsSection();
            case PARTICIPANT_REQUESTS -> renderParticipantsSection();
            case RESOURCES -> renderResourcesSection();
            case EVENT_STATS -> renderEventStatsSection();
            case RESOURCE_STATS -> renderResourceStatsSection();
        }
    }

    private void renderEventsSection() {
        pageTitleLabel.setText("Gestion des événements");
        pageSubtitleLabel.setText("Catalogue complet et participation BADMIN");
        heroTitleLabel.setText("Liste des événements");
        heroSubtitleLabel.setText("Visualisez les événements, consultez les détails et participez depuis l'espace BADMIN sans quitter l'administration.");
        heroBadgeLabel.setText("Section active");
        breadcrumbSectionLabel.setText("Liste des événements");
        sectionChipLabel.setText("Catalogue");

        setKpi(
                "Total événements", String.valueOf(allEvents.size()), "Base admin disponible",
                "Publiés", String.valueOf(countPublishedEvents()), "Événements visibles",
                "Participants", String.valueOf(countAcceptedParticipants()), "Demandes acceptées",
                "En attente", String.valueOf(countPendingParticipants()), "À traiter"
        );

        setSortOptions("Trier...", "Titre A-Z", "Titre Z-A", "Date plus proche", "Date plus lointaine", "Ville A-Z", "Ville Z-A", "Statut");
        searchField.setPromptText("Rechercher un événement, une ville, un type...");
        showFilters(true);
        showInsights(false, List.of());
        showExperienceDeck(false);
        showCharts(false);
        showCharts(false);
        setHeroChips("Catalogue", "Participation", "Suivi");

        List<Evenement> rows = filterEvents();
        resultCountLabel.setText(rows.size() + " événement(s)");
        tableTitleLabel.setText("Liste complète des événements");
        tableHelpLabel.setText("Le détail permet à BADMIN de participer sans passer par le front.");
        buildHeader(List.of(
                new HeaderSpec("IMAGE", 110),
                new HeaderSpec("ÉVÉNEMENT", 250),
                new HeaderSpec("TYPE", 170),
                new HeaderSpec("DATE", 170),
                new HeaderSpec("VILLE", 140),
                new HeaderSpec("STATUT", 150),
                new HeaderSpec("PARTICIPANTS", 150),
                new HeaderSpec("DÉTAILS", 120)
        ));

        rowsContainer.getChildren().clear();
        if (rows.isEmpty()) {
            showEmptyState("Aucun événement trouvé", "Essayez une autre recherche ou changez le tri.");
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            rowsContainer.getChildren().add(createEventRow(rows.get(i), i));
        }
    }

    private void renderParticipantsSection() {
        pageTitleLabel.setText("Gestion des événements");
        pageSubtitleLabel.setText("Vue centralisée des demandes de participation");
        heroTitleLabel.setText("Participants demandes");
        heroSubtitleLabel.setText("Suivez toutes les demandes envoyées sur les événements, leur statut et le dernier état de traitement.");
        heroBadgeLabel.setText("Suivi");
        breadcrumbSectionLabel.setText("Participants demandes");
        sectionChipLabel.setText("Demandes");

        setKpi(
                "Demandes totales", String.valueOf(allParticipationRows.size()), "Toutes les demandes",
                "Acceptées", String.valueOf(countAcceptedParticipants()), "Participation validée",
                "En attente", String.valueOf(countPendingParticipants()), "Décision à prendre",
                "Événements concernés", String.valueOf(countEventsWithDemandes()), "Activité réelle"
        );

        setSortOptions("Trier...", "Plus récentes", "Plus anciennes", "Statut", "Événement A-Z", "Participant A-Z");
        searchField.setPromptText("Rechercher un participant, un événement, un email...");
        showFilters(true);
        showExperienceDeck(false);
        showCharts(false);
        setHeroChips("Suivi des files", "Decisions rapides", "Lecture claire");
        setSortOptions("Tri intelligent", "Plus recentes", "Plus anciennes", "Statut", "Evenement A-Z", "Participant A-Z");
        searchField.setPromptText("Rechercher un participant, un evenement, un email ou un telephone...");
        showInsights(true, buildParticipantInsights());

        List<ParticipationRow> rows = filterParticipationRows();
        resultCountLabel.setText(rows.size() + " demande(s)");
        tableTitleLabel.setText("Demandes de participation");
        tableHelpLabel.setText("Vue lecture seule pour le suivi BADMIN.");
        buildHeader(List.of(
                new HeaderSpec("ÉVÉNEMENT", 240),
                new HeaderSpec("PARTICIPANT", 220),
                new HeaderSpec("EMAIL", 220),
                new HeaderSpec("TÉLÉPHONE", 150),
                new HeaderSpec("STATUT", 120),
                new HeaderSpec("CRÉÉE LE", 170),
                new HeaderSpec("NOTE", 230)
        ));

        rowsContainer.getChildren().clear();
        if (rows.isEmpty()) {
            showEmptyState("Aucune demande trouvée", "Les demandes de participation apparaîtront ici dès qu'elles seront enregistrées.");
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            rowsContainer.getChildren().add(createParticipationRow(rows.get(i), i));
        }
    }

    private void renderResourcesSection() {
        pageTitleLabel.setText("Gestion des événements");
        pageSubtitleLabel.setText("Ressources liées aux événements");
        heroTitleLabel.setText("Ressources");
        heroSubtitleLabel.setText("Consultez les ressources opérationnelles, fichiers, liens et stock associés aux événements depuis une vue BADMIN propre.");
        heroBadgeLabel.setText("Ressources");
        breadcrumbSectionLabel.setText("Ressources");
        sectionChipLabel.setText("Assets");

        setKpi(
                "Total ressources", String.valueOf(allResources.size()), "Catalogue synchronisé",
                "Fichiers", String.valueOf(countResourcesByType("file")), "Documents et médias",
                "Liens", String.valueOf(countResourcesByType("external_link")), "Accès externes",
                "Stock", String.valueOf(countResourcesByType("stock_item")), "Inventaire matériel"
        );

        setSortOptions("Trier...", "Nom A-Z", "Nom Z-A", "Événement A-Z", "Catégorie", "Type", "Quantité décroissante");
        searchField.setPromptText("Rechercher une ressource, un événement, un fournisseur...");
        showFilters(true);
        showExperienceDeck(false);
        showCharts(false);
        setHeroChips("Stock terrain", "Documents utiles", "Vision fournisseur");
        setSortOptions("Tri intelligent", "Nom A-Z", "Nom Z-A", "Evenement A-Z", "Categorie", "Type", "Quantite decroissante");
        searchField.setPromptText("Rechercher une ressource, un evenement, un fournisseur ou une categorie...");
        showInsights(true, buildResourceInsights());

        List<Ressource> rows = filterResources();
        resultCountLabel.setText(rows.size() + " ressource(s)");
        tableTitleLabel.setText("Ressources des événements");
        tableHelpLabel.setText("Vue de consultation admin, sans basculer vers le dashboard ressource externe.");
        buildHeader(List.of(
                new HeaderSpec("RESSOURCE", 250),
                new HeaderSpec("ÉVÉNEMENT", 220),
                new HeaderSpec("CATÉGORIE", 180),
                new HeaderSpec("TYPE", 140),
                new HeaderSpec("QUANTITÉ", 130),
                new HeaderSpec("VISIBILITÉ", 130),
                new HeaderSpec("FOURNISSEUR", 200)
        ));

        rowsContainer.getChildren().clear();
        if (rows.isEmpty()) {
            showEmptyState("Aucune ressource trouvée", "Les ressources liées aux événements s'afficheront ici.");
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            rowsContainer.getChildren().add(createResourceRow(rows.get(i), i));
        }
    }

    private void renderEventStatsSection() {
        if (dashboardData == null) {
            showEmptyState("Statistiques indisponibles", "Impossible de charger les statistiques événements.");
            return;
        }

        pageTitleLabel.setText("Gestion des événements");
        pageSubtitleLabel.setText("Lecture rapide des performances événements");
        heroTitleLabel.setText("Stats événements");
        heroSubtitleLabel.setText("Synthèse BI de la période récente pour aider BADMIN à comprendre l'activité sans quitter l'espace admin.");
        heroBadgeLabel.setText("BI");
        breadcrumbSectionLabel.setText("Stats événements");
        sectionChipLabel.setText("Analyse");

        setKpi(
                "Total période", String.valueOf(dashboardData.totalEvenements), dashboardData.resume,
                "Taux publication", String.format(Locale.US, "%.1f%%", dashboardData.tauxPublication), "Événements publiés",
                "À venir", String.valueOf(dashboardData.evenementsAVenir), "Calendrier futur",
                "Capacité", String.valueOf(dashboardData.capaciteTotale), "Places cumulées"
        );

        setSortOptions("Trier...", "Capacité décroissante", "Capacité croissante", "Titre A-Z", "Ville A-Z");
        searchField.setPromptText("Filtrer les top événements, type ou ville...");
        showFilters(true);
        setHeroChips("Volume", "Capacite", "Recommandations");
        setSortOptions("Tri intelligent", "Capacite decroissante", "Capacite croissante", "Titre A-Z", "Ville A-Z");
        searchField.setPromptText("Filtrer les top evenements, un type ou une ville...");
        refreshExperienceDeckForEvents();
        showCharts(true);
        populateEventCharts(dashboardData);
        showInsights(true, buildEventStatsInsights());

        List<Evenement> rows = filterEventStatsRows();
        resultCountLabel.setText(rows.size() + " ligne(s)");
        tableTitleLabel.setText("Top événements sur la période");
        tableHelpLabel.setText("Lecture synthétique basée sur les données BI internes.");
        buildHeader(List.of(
                new HeaderSpec("ÉVÉNEMENT", 270),
                new HeaderSpec("TYPE", 190),
                new HeaderSpec("VILLE", 170),
                new HeaderSpec("STATUT", 140),
                new HeaderSpec("CAPACITÉ", 130),
                new HeaderSpec("DATES", 220)
        ));

        rowsContainer.getChildren().clear();
        if (rows.isEmpty()) {
            showEmptyState("Aucun résultat pour cette vue", "Ajustez la recherche pour retrouver des événements dans le top.");
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            rowsContainer.getChildren().add(createEventStatsRow(rows.get(i), i));
        }
    }

    private void renderResourceStatsSection() {
        pageTitleLabel.setText("Gestion des événements");
        pageSubtitleLabel.setText("Vue analytique sur les ressources événementielles");
        heroTitleLabel.setText("Stats ressources");
        heroSubtitleLabel.setText("Mesurez la structure du parc de ressources pour piloter les besoins opérationnels des événements.");
        heroBadgeLabel.setText("Insight");
        breadcrumbSectionLabel.setText("Stats ressources");
        sectionChipLabel.setText("Ressources BI");

        ResourceStats stats = buildResourceStats();
        setKpi(
                "Total ressources", String.valueOf(stats.totalResources), "Volume géré",
                "Publiques", String.valueOf(stats.publicResources), "Ressources visibles",
                "Stock cumulé", String.valueOf(stats.totalQuantity), "Unités suivies",
                "Coût estimé", formatMoney(stats.totalEstimatedCost), "Vision budgétaire"
        );

        setSortOptions("Trier...", "Ressources décroissantes", "Ressources croissantes", "Nom A-Z");
        searchField.setPromptText("Filtrer les catégories ou événements ressource...");
        showFilters(true);
        showExperienceDeck(true);
        setHeroChips("Volume", "Cout", "Categorie leader");
        setSortOptions("Tri intelligent", "Ressources decroissantes", "Ressources croissantes", "Nom A-Z");
        searchField.setPromptText("Filtrer les categories ou evenements ressource...");
        refreshExperienceDeckForResourceStats(stats);
        showCharts(true);
        populateResourceCharts(stats);
        showInsights(true, buildResourceStatsInsights(stats));

        List<ResourceAggregateRow> rows = filterResourceAggregateRows(stats.rows);
        resultCountLabel.setText(rows.size() + " groupe(s)");
        tableTitleLabel.setText("Répartition des ressources");
        tableHelpLabel.setText("Synthèse par catégorie et événement pour une lecture managériale rapide.");
        buildHeader(List.of(
                new HeaderSpec("LIBELLÉ", 260),
                new HeaderSpec("TYPE", 170),
                new HeaderSpec("RESSOURCES", 130),
                new HeaderSpec("QUANTITÉ", 130),
                new HeaderSpec("COÛT ESTIMÉ", 150),
                new HeaderSpec("ÉVÉNEMENT LE PLUS ACTIF", 280)
        ));

        rowsContainer.getChildren().clear();
        if (rows.isEmpty()) {
            showEmptyState("Aucune statistique ressource", "Ajoutez des ressources pour alimenter cette vue.");
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            rowsContainer.getChildren().add(createResourceStatsRow(rows.get(i), i));
        }
    }

    private List<Evenement> filterEvents() {
        List<Evenement> rows = new ArrayList<>();
        String keyword = normalized(searchField.getText());
        for (Evenement event : allEvents) {
            if (keyword.isBlank() || contains(event.getTitre_event(), keyword) || contains(event.getType_event(), keyword)
                    || contains(event.getVille_event(), keyword) || contains(event.getStatut_event(), keyword)
                    || contains(event.getDescription_event(), keyword)) {
                rows.add(event);
            }
        }
        String selected = sortCombo.getValue();
        if ("Titre A-Z".equals(selected)) rows.sort(Comparator.comparing(e -> normalized(e.getTitre_event())));
        else if ("Titre Z-A".equals(selected)) rows.sort(Comparator.comparing((Evenement e) -> normalized(e.getTitre_event())).reversed());
        else if ("Date plus proche".equals(selected)) rows.sort(Comparator.comparing(this::eventDateOrMax));
        else if ("Date plus lointaine".equals(selected)) rows.sort(Comparator.comparing(this::eventDateOrMax).reversed());
        else if ("Ville A-Z".equals(selected)) rows.sort(Comparator.comparing(e -> normalized(e.getVille_event())));
        else if ("Ville Z-A".equals(selected)) rows.sort(Comparator.comparing((Evenement e) -> normalized(e.getVille_event())).reversed());
        else if ("Statut".equals(selected)) rows.sort(Comparator.comparing(e -> normalized(e.getStatut_event())));
        else if ("Evenement A-Z".equals(selected)) rows.sort(Comparator.comparing(e -> normalized(e.getTitre_event())));
        return rows;
    }

    private List<ParticipationRow> filterParticipationRows() {
        List<ParticipationRow> rows = new ArrayList<>();
        String keyword = normalized(searchField.getText());
        for (ParticipationRow row : allParticipationRows) {
            if (keyword.isBlank() || contains(row.event.getTitre_event(), keyword) || contains(row.demande.getDisplayName(), keyword)
                    || contains(row.demande.getEmail(), keyword) || contains(row.demande.getTelephone(), keyword)
                    || contains(row.demande.getStatusLabel(), keyword)) {
                rows.add(row);
            }
        }
        String selected = sortCombo.getValue();
        if ("Plus récentes".equals(selected)) rows.sort(Comparator.comparing((ParticipationRow row) -> normalized(row.demande.getCreatedAt())).reversed());
        else if ("Plus anciennes".equals(selected)) rows.sort(Comparator.comparing(row -> normalized(row.demande.getCreatedAt())));
        else if ("Statut".equals(selected)) rows.sort(Comparator.comparing(row -> normalized(row.demande.getStatusLabel())));
        else if ("Événement A-Z".equals(selected)) rows.sort(Comparator.comparing(row -> normalized(row.event.getTitre_event())));
        else if ("Participant A-Z".equals(selected)) rows.sort(Comparator.comparing(row -> normalized(row.demande.getDisplayName())));
        if ("Plus recentes".equals(selected)) rows.sort(Comparator.comparing((ParticipationRow row) -> normalized(row.demande.getCreatedAt())).reversed());
        else if ("Evenement A-Z".equals(selected)) rows.sort(Comparator.comparing(row -> normalized(row.event.getTitre_event())));
        return rows;
    }

    private List<Ressource> filterResources() {
        List<Ressource> rows = new ArrayList<>();
        String keyword = normalized(searchField.getText());
        for (Ressource resource : allResources) {
            String eventTitle = eventTitle(resource.getEvenement() == null ? 0 : resource.getEvenement().getId());
            if (keyword.isBlank() || contains(resource.getNom_ressource(), keyword) || contains(resource.getCategorie_ressource(), keyword)
                    || contains(resource.getType_ressource(), keyword) || contains(resource.getFournisseur_ressource(), keyword)
                    || contains(eventTitle, keyword)) {
                rows.add(resource);
            }
        }
        String selected = sortCombo.getValue();
        if ("Nom A-Z".equals(selected)) rows.sort(Comparator.comparing(r -> normalized(r.getNom_ressource())));
        else if ("Nom Z-A".equals(selected)) rows.sort(Comparator.comparing((Ressource r) -> normalized(r.getNom_ressource())).reversed());
        else if ("Événement A-Z".equals(selected)) rows.sort(Comparator.comparing(r -> normalized(eventTitle(r.getEvenement() == null ? 0 : r.getEvenement().getId()))));
        else if ("Catégorie".equals(selected)) rows.sort(Comparator.comparing(r -> normalized(r.getCategorie_ressource())));
        else if ("Type".equals(selected)) rows.sort(Comparator.comparing(r -> normalized(r.getType_ressource())));
        else if ("Quantité décroissante".equals(selected)) rows.sort(Comparator.comparingInt(Ressource::getQuantite_disponible_ressource).reversed());
        if ("Evenement A-Z".equals(selected)) rows.sort(Comparator.comparing(r -> normalized(eventTitle(r.getEvenement() == null ? 0 : r.getEvenement().getId()))));
        else if ("Categorie".equals(selected)) rows.sort(Comparator.comparing(r -> normalized(r.getCategorie_ressource())));
        else if ("Quantite decroissante".equals(selected)) rows.sort(Comparator.comparingInt(Ressource::getQuantite_disponible_ressource).reversed());
        return rows;
    }

    private List<Evenement> filterEventStatsRows() {
        List<Evenement> rows = new ArrayList<>(dashboardData == null ? List.of() : dashboardData.topEvenements);
        String keyword = normalized(searchField.getText());
        rows.removeIf(event -> !keyword.isBlank() && !contains(event.getTitre_event(), keyword)
                && !contains(event.getType_event(), keyword) && !contains(event.getVille_event(), keyword));
        String selected = sortCombo.getValue();
        if ("Capacité décroissante".equals(selected)) rows.sort(Comparator.comparingInt(Evenement::getNb_participants_max_event).reversed());
        else if ("Capacité croissante".equals(selected)) rows.sort(Comparator.comparingInt(Evenement::getNb_participants_max_event));
        else if ("Titre A-Z".equals(selected)) rows.sort(Comparator.comparing(e -> normalized(e.getTitre_event())));
        else if ("Ville A-Z".equals(selected)) rows.sort(Comparator.comparing(e -> normalized(e.getVille_event())));
        if ("Capacite decroissante".equals(selected)) rows.sort(Comparator.comparingInt(Evenement::getNb_participants_max_event).reversed());
        else if ("Capacite croissante".equals(selected)) rows.sort(Comparator.comparingInt(Evenement::getNb_participants_max_event));
        return rows;
    }

    private List<ResourceAggregateRow> filterResourceAggregateRows(List<ResourceAggregateRow> source) {
        List<ResourceAggregateRow> rows = new ArrayList<>(source);
        String keyword = normalized(searchField.getText());
        rows.removeIf(row -> !keyword.isBlank() && !contains(row.label, keyword) && !contains(row.type, keyword) && !contains(row.topEventName, keyword));
        String selected = sortCombo.getValue();
        if ("Ressources décroissantes".equals(selected)) rows.sort(Comparator.comparingInt((ResourceAggregateRow row) -> row.resourceCount).reversed());
        else if ("Ressources croissantes".equals(selected)) rows.sort(Comparator.comparingInt(row -> row.resourceCount));
        else if ("Nom A-Z".equals(selected)) rows.sort(Comparator.comparing(row -> normalized(row.label)));
        if ("Ressources decroissantes".equals(selected)) rows.sort(Comparator.comparingInt((ResourceAggregateRow row) -> row.resourceCount).reversed());
        return rows;
    }

    private HBox createEventRow(Evenement event, int index) {
        HBox row = baseRow(index);
        row.getChildren().addAll(
                imageCell(loadImage(safe(event.getImage_couverture_event())), "EVT", 110),
                infoCell(250, textLabel(safe(event.getTitre_event()), "product-name"), textLabel("ID: #" + event.getId(), "product-id"), textLabel(shortText(safe(event.getDescription_event()), 42), "product-desc")),
                leftCell(170, badgeLabel(emptyDash(event.getType_event()), "category-badge")),
                leftCell(170, textLabel(formatDateRange(event), "price-text")),
                leftCell(140, badgeLabel(emptyDash(event.getVille_event()), "category-badge")),
                leftCell(150, statusBadge(event.getStatut_event())),
                leftCell(150, participantBadge(event)),
                detailCell(event)
        );
        return row;
    }

    private VBox detailCell(Evenement event) {
        Button detailButton = new Button("Voir");
        detailButton.getStyleClass().add("eye-button");
        detailButton.setOnAction(e -> showEventDetailsPopup(event));
        return leftCell(120, detailButton);
    }

    private Label participantBadge(Evenement event) {
        Label participants = new Label(getAcceptedCount(event) + " / " + maxParticipantsText(event));
        participants.getStyleClass().add(getPendingCount(event) > 0 ? "stock-badge-orange" : "stock-badge-green");
        return participants;
    }

    private HBox createParticipationRow(ParticipationRow rowData, int index) {
        HBox row = baseRow(index);
        row.getChildren().addAll(
                leftCell(240, textLabel(safe(rowData.event.getTitre_event()), "product-name")),
                infoCell(220, textLabel(rowData.demande.getDisplayName(), "product-name"), textLabel("ID utilisateur: " + rowData.demande.getUserId(), "product-id")),
                leftCell(220, textLabel(emptyDash(rowData.demande.getEmail()), "product-desc")),
                leftCell(150, badgeLabel(emptyDash(rowData.demande.getTelephone()), "category-badge")),
                leftCell(120, statusBadge(rowData.demande.getStatusLabel())),
                leftCell(170, textLabel(blankDash(rowData.demande.getCreatedAt()), "product-desc")),
                leftCell(230, textLabel(shortText(blankDash(rowData.demande.getAdminNote()), 42), "product-desc"))
        );
        return row;
    }

    private HBox createResourceRow(Ressource resource, int index) {
        HBox row = baseRow(index);
        row.getChildren().addAll(
                infoCell(250, textLabel(safe(resource.getNom_ressource()), "product-name"), textLabel("ID: #" + resource.getId(), "product-id"), textLabel(shortText(blankDash(resource.getNotes_ressource()), 42), "product-desc")),
                leftCell(220, textLabel(eventTitle(resource.getEvenement() == null ? 0 : resource.getEvenement().getId()), "product-desc")),
                leftCell(180, badgeLabel(emptyDash(resource.getCategorie_ressource()), "category-badge")),
                leftCell(140, typeBadge(resource.getType_ressource())),
                leftCell(130, quantityBadge(resource)),
                leftCell(130, visibilityBadge(resource.isEst_publique_ressource())),
                leftCell(200, textLabel(blankDash(resource.getFournisseur_ressource()), "product-desc"))
        );
        return row;
    }

    private HBox createEventStatsRow(Evenement event, int index) {
        HBox row = baseRow(index);
        row.getChildren().addAll(
                infoCell(270, textLabel(safe(event.getTitre_event()), "product-name"), textLabel("Organisateur: " + blankDash(event.getNom_organisateur_event()), "product-id")),
                leftCell(190, badgeLabel(emptyDash(event.getType_event()), "category-badge")),
                leftCell(170, textLabel(blankDash(event.getVille_event()), "product-desc")),
                leftCell(140, statusBadge(event.getStatut_event())),
                leftCell(130, textLabel(String.valueOf(Math.max(0, event.getNb_participants_max_event())), "price-text")),
                leftCell(220, textLabel(formatDateRange(event), "product-desc"))
        );
        return row;
    }

    private HBox createResourceStatsRow(ResourceAggregateRow rowData, int index) {
        HBox row = baseRow(index);
        row.getChildren().addAll(
                infoCell(260, textLabel(rowData.label, "product-name"), textLabel(rowData.description, "product-id")),
                leftCell(170, typeBadge(rowData.type)),
                leftCell(130, badgeLabel(String.valueOf(rowData.resourceCount), "stock-badge-green")),
                leftCell(130, badgeLabel(String.valueOf(rowData.totalQuantity), "stock-badge-orange")),
                leftCell(150, textLabel(formatMoney(rowData.totalCost), "price-text")),
                leftCell(280, textLabel(rowData.topEventName, "product-desc"))
        );
        return row;
    }

    private HBox baseRow(int index) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().add("product-row");
        if (index % 2 == 0) row.getStyleClass().add("product-row-light");
        return row;
    }

    private StackPane imageCell(Image image, String fallback, double width) {
        StackPane cell = new StackPane();
        cell.setPrefWidth(width);
        cell.setMinWidth(width);
        cell.setAlignment(Pos.CENTER_LEFT);

        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(58, 58);
        imgBox.getStyleClass().add("product-img-box");

        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(52);
            imageView.setFitHeight(52);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imgBox.getChildren().add(imageView);
        } else {
            imgBox.getChildren().add(textLabel(fallback, "image-placeholder-text"));
        }
        cell.getChildren().add(imgBox);
        return cell;
    }

    private VBox leftCell(double width, Node node) {
        VBox box = new VBox(node);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(width);
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox infoCell(double width, Node... nodes) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(width);
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.getChildren().addAll(nodes);
        return box;
    }

    private Label textLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label badgeLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private Label statusBadge(String value) {
        Label label = new Label(blankDash(value));
        String normalized = normalized(value);
        if (normalized.contains("accept")) label.getStyleClass().add("status-dispo");
        else if (normalized.contains("attente") || normalized.contains("waiting") || normalized.contains("pending")) label.getStyleClass().add("stock-badge-orange");
        else if (normalized.contains("refus") || normalized.contains("annul")) label.getStyleClass().add("status-rupture");
        else if (normalized.contains("brouillon")) label.getStyleClass().add("status-indispo");
        else label.getStyleClass().add("status-dispo");
        return label;
    }

    private Label typeBadge(String type) {
        Label label = new Label(blankDash(type));
        String value = safe(type);
        if ("file".equalsIgnoreCase(value)) label.getStyleClass().add("stock-badge-green");
        else if ("external_link".equalsIgnoreCase(value)) label.getStyleClass().add("stock-badge-orange");
        else label.getStyleClass().add("status-indispo");
        return label;
    }

    private Label quantityBadge(Ressource resource) {
        int quantity = Math.max(0, resource.getQuantite_disponible_ressource());
        Label label = new Label(quantity + " " + blankDash(resource.getUnite_ressource()));
        if (quantity <= 0) label.getStyleClass().add("stock-badge-red");
        else if (quantity <= 5) label.getStyleClass().add("stock-badge-orange");
        else label.getStyleClass().add("stock-badge-green");
        return label;
    }

    private Label visibilityBadge(boolean isPublic) {
        Label label = new Label(isPublic ? "Publique" : "Interne");
        label.getStyleClass().add(isPublic ? "status-dispo" : "status-indispo");
        return label;
    }

    private void buildHeader(List<HeaderSpec> specs) {
        tableHeader.getChildren().clear();
        for (HeaderSpec spec : specs) {
            Label label = new Label(spec.title);
            label.getStyleClass().add("header-cell");
            label.setPrefWidth(spec.width);
            label.setMinWidth(0);
            label.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(label, Priority.ALWAYS);
            tableHeader.getChildren().add(label);
        }
    }

    private void setSortOptions(String... options) {
        if (sortCombo == null) {
            return;
        }

        String current = sortCombo.getValue();
        updatingSortOptions = true;
        try {
            sortCombo.setItems(FXCollections.observableArrayList(options));
            if (current != null && sortCombo.getItems().contains(current)) {
                sortCombo.setValue(current);
            } else if (!sortCombo.getItems().isEmpty()) {
                sortCombo.setValue(sortCombo.getItems().get(0));
            } else {
                sortCombo.setValue(null);
            }
        } finally {
            updatingSortOptions = false;
        }
    }

    private void showFilters(boolean visible) {
        filterCard.setVisible(visible);
        filterCard.setManaged(visible);
    }

    private void showInsights(boolean visible, List<InsightCard> cards) {
        insightsCard.setVisible(visible);
        insightsCard.setManaged(visible);
        if (!visible) return;

        insightsTitleLabel.setText("Lecture rapide");
        insightsRow.getChildren().clear();
        for (InsightCard card : cards) {
            VBox box = new VBox(8);
            box.getStyleClass().add("insight-box");
            HBox.setHgrow(box, Priority.ALWAYS);

            box.getChildren().addAll(
                    textLabel(card.title, "insight-title"),
                    textLabel(card.value, "insight-value"),
                    textLabel(card.subtitle, "insight-text")
            );
            insightsRow.getChildren().add(box);
        }
    }

    private void showExperienceDeck(boolean visible) {
        if (experienceDeck == null) {
            return;
        }
        experienceDeck.setVisible(visible);
        experienceDeck.setManaged(visible);
    }

    private void showCharts(boolean visible) {
        if (chartsSection == null) {
            return;
        }
        chartsSection.setVisible(visible);
        chartsSection.setManaged(visible);
    }

    private void refreshExperienceDeckForEvents() {
        showExperienceDeck(true);
        renderPulseCards();
        refreshSecurityCard();
        refreshParticipationHub();
    }

    private void refreshExperienceDeckForResourceStats(ResourceStats stats) {
        showExperienceDeck(true);
        renderResourcePulseCards(stats);
        refreshResourceOperations(stats);
    }

    private void renderPulseCards() {
        if (pulseRow == null) {
            return;
        }
        pulseRow.getChildren().clear();

        int total = Math.max(1, allEvents.size());
        int publicationRate = Math.round((countPublishedEvents() * 100f) / total);
        int pendingRate = Math.round((countPendingParticipants() * 100f) / Math.max(1, allParticipationRows.size()));
        int adminCoverage = Math.round((countBadminDemandes() * 100f) / total);

        pulseRow.getChildren().addAll(
                createPulseCard("Publication", publicationRate + "%", "Evenements visibles", "pulse-blue"),
                createPulseCard("Validation", pendingRate + "%", "Demandes en attente", "pulse-amber"),
                createPulseCard("BADMIN", adminCoverage + "%", "Participation admin", "pulse-green")
        );
    }

    private VBox createPulseCard(String title, String value, String subtitle, String styleClass) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("pulse-card", styleClass);
        HBox.setHgrow(card, Priority.ALWAYS);

        StackPane ring = new StackPane();
        ring.getStyleClass().add("pulse-ring");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("pulse-ring-value");
        ring.getChildren().add(valueLabel);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("pulse-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("pulse-subtitle");
        subtitleLabel.setWrapText(true);

        card.getChildren().addAll(ring, titleLabel, subtitleLabel);
        return card;
    }

    private void renderResourcePulseCards(ResourceStats stats) {
        if (pulseRow == null || stats == null) {
            return;
        }
        pulseRow.getChildren().clear();
        int total = Math.max(1, stats.totalResources);
        int publicRate = Math.round((stats.publicResources * 100f) / total);
        int stockCoverage = Math.round((stats.totalQuantity * 100f) / Math.max(1, stats.totalQuantity + stats.totalResources));
        int costSignal = (int) Math.min(100, Math.round(stats.totalEstimatedCost / Math.max(1d, stats.totalResources)));

        pulseRow.getChildren().addAll(
                createPulseCard("Visibilite", publicRate + "%", "Ressources publiques", "pulse-blue"),
                createPulseCard("Stock", stockCoverage + "%", "Quantite cumulee", "pulse-green"),
                createPulseCard("Budget", costSignal + "%", "Signal cout moyen", "pulse-amber")
        );
    }

    private void refreshSecurityCard() {
        if (securityHeadlineLabel == null) {
            return;
        }

        int drafts = countDraftEvents();
        int expiredDeadlines = countExpiredRegistrationDeadlines();
        int pending = countPendingParticipants();

        securityPendingLabel.setText(pending + " en attente");
        securityDraftLabel.setText(drafts + " fiche(s) a completer");
        securityDeadlineLabel.setText(expiredDeadlines + " urgence(s)");

        if (pending > 0) {
            securityHeadlineLabel.setText("Des validations participant demandent une action rapide.");
            securityAdviceLabel.setText("Priorite : traitez la file d'attente puis verifiez les capacites avant d'ouvrir de nouvelles participations.");
        } else if (expiredDeadlines > 0) {
            securityHeadlineLabel.setText("Certaines dates limites sont depassees alors que des evenements restent visibles.");
            securityAdviceLabel.setText("Controlez les inscriptions encore ouvertes et harmonisez date limite, statut et communication.");
        } else if (drafts > 0) {
            securityHeadlineLabel.setText("Le portefeuille est stable, mais quelques brouillons peuvent bloquer la publication.");
            securityAdviceLabel.setText("Completer les brouillons avec lieu, date, contact et capacite renforcera la qualite du planning.");
        } else {
            securityHeadlineLabel.setText("Aucune alerte critique pour le moment.");
            securityAdviceLabel.setText("Le portefeuille evenementiel est coherent. Continuez la revue des contacts, capacites et delais d'inscription.");
        }
    }

    private void refreshParticipationHub() {
        if (participationHubEventLabel == null) {
            return;
        }

        suggestedEvent = findSuggestedEvent();
        User current = SessionManager.getCurrentUser();

        if (current == null) {
            participationHubButton.setText("Participer maintenant");
            participationDetailsButton.setText("Voir la fiche");
            participationHubTitleLabel.setText("Connexion requise");
            participationHubEventLabel.setText("Connectez un compte admin pour participer directement a un evenement.");
            participationHubMetaLabel.setText("La suggestion automatique apparait des qu'un utilisateur admin est present en session.");
            participationHubButton.setDisable(true);
            participationDetailsButton.setDisable(suggestedEvent == null);
            return;
        }

        if (suggestedEvent == null) {
            participationHubButton.setText("Participer maintenant");
            participationDetailsButton.setText("Voir la fiche");
            participationHubTitleLabel.setText("Aucun choix disponible");
            participationHubEventLabel.setText("Tous les evenements a venir sont deja traites ou votre participation existe deja.");
            participationHubMetaLabel.setText("La carte se reactive des qu'un nouvel evenement publie et accessible est detecte.");
            participationHubButton.setDisable(true);
            participationDetailsButton.setDisable(true);
            return;
        }

        participationHubButton.setText("Participer maintenant");
        participationDetailsButton.setText("Voir la fiche");
        participationHubTitleLabel.setText("Choix recommande pour BADMIN");
        participationHubEventLabel.setText(safe(suggestedEvent.getTitre_event()) + " • " + blankDash(suggestedEvent.getVille_event()));
        participationHubMetaLabel.setText("Prochaine opportunite utile : " + formatDateRange(suggestedEvent)
                + " • " + getAcceptedCount(suggestedEvent) + "/" + maxParticipantsText(suggestedEvent)
                + " participants.");
        participationHubButton.setDisable(!canParticipate(suggestedEvent));
        participationDetailsButton.setDisable(false);
    }

    private void refreshResourceOperations(ResourceStats stats) {
        if (securityHeadlineLabel == null || stats == null) {
            return;
        }

        securityHeadlineLabel.setText("Lecture controle pour les ressources evenementielles.");
        securityPendingLabel.setText(stats.publicResources + " publiques");
        securityDraftLabel.setText(stats.rows.size() + " categories suivies");
        securityDeadlineLabel.setText(stats.totalQuantity + " unites");
        securityAdviceLabel.setText("Priorite : surveiller les categories dominantes, les couts concentres et les ressources encore internes.");

        participationHubTitleLabel.setText("BI ressources");
        participationHubEventLabel.setText(stats.topEvent);
        participationHubMetaLabel.setText("Categorie leader : " + stats.topCategory + " • Type principal : " + stats.topType);
        participationHubButton.setText("Voir les ressources");
        participationHubButton.setDisable(true);
        participationDetailsButton.setText("Section ressources");
        participationDetailsButton.setDisable(true);
    }

    private void populateEventCharts(DashboardBIServiceEvenement.DashboardData data) {
        if (data == null) {
            showCharts(false);
            return;
        }

        chartOneTitleLabel.setText("Evolution des evenements");
        chartOneSubtitleLabel.setText("Volume et capacite sur la periode");
        chartTwoTitleLabel.setText("Repartition des statuts");
        chartTwoSubtitleLabel.setText("Publie, brouillon, annule et autres");
        chartThreeTitleLabel.setText("Top types");
        chartThreeSubtitleLabel.setText("Evenements par type");
        chartFourTitleLabel.setText("Top villes");
        chartFourSubtitleLabel.setText("Concentration geographique");

        if (eventsLineChart != null) {
            eventsLineChart.getData().clear();
            XYChart.Series<String, Number> eventSeries = new XYChart.Series<>();
            eventSeries.setName("Evenements");
            for (Map.Entry<LocalDate, Integer> entry : data.eventsParJour.entrySet()) {
                eventSeries.getData().add(new XYChart.Data<>(entry.getKey().toString(), entry.getValue()));
            }

            XYChart.Series<String, Number> capacitySeries = new XYChart.Series<>();
            capacitySeries.setName("Capacite");
            for (Map.Entry<LocalDate, Integer> entry : data.capaciteParJour.entrySet()) {
                capacitySeries.getData().add(new XYChart.Data<>(entry.getKey().toString(), entry.getValue()));
            }
            eventsLineChart.getData().addAll(eventSeries, capacitySeries);
        }

        if (statsPieChart != null) {
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : data.repartitionStatuts.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
            statsPieChart.setData(pieData);
        }

        fillBarChart(primaryBarChart, "Types", data.parType, false);
        fillBarChart(secondaryBarChart, "Villes", data.parVille, false);
    }

    private void populateResourceCharts(ResourceStats stats) {
        if (stats == null) {
            showCharts(false);
            return;
        }

        chartOneTitleLabel.setText("Volume par categorie");
        chartOneSubtitleLabel.setText("Comparaison des groupes ressources");
        chartTwoTitleLabel.setText("Poids des types");
        chartTwoSubtitleLabel.setText("Fichiers, liens et stock");
        chartThreeTitleLabel.setText("Quantites par categorie");
        chartThreeSubtitleLabel.setText("Unites disponibles");
        chartFourTitleLabel.setText("Cout par categorie");
        chartFourSubtitleLabel.setText("Vision budgetaire");

        if (eventsLineChart != null) {
            eventsLineChart.getData().clear();
            XYChart.Series<String, Number> resourceSeries = new XYChart.Series<>();
            resourceSeries.setName("Ressources");
            XYChart.Series<String, Number> quantitySeries = new XYChart.Series<>();
            quantitySeries.setName("Quantites");
            for (ResourceAggregateRow row : stats.rows) {
                resourceSeries.getData().add(new XYChart.Data<>(row.label, row.resourceCount));
                quantitySeries.getData().add(new XYChart.Data<>(row.label, row.totalQuantity));
            }
            eventsLineChart.getData().addAll(resourceSeries, quantitySeries);
        }

        if (statsPieChart != null) {
            Map<String, Integer> typeMap = new LinkedHashMap<>();
            for (ResourceAggregateRow row : stats.rows) {
                typeMap.put(row.type, typeMap.getOrDefault(row.type, 0) + row.resourceCount);
            }
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : typeMap.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
            statsPieChart.setData(pieData);
        }

        Map<String, Integer> quantityMap = new LinkedHashMap<>();
        Map<String, Integer> costMap = new LinkedHashMap<>();
        for (ResourceAggregateRow row : stats.rows) {
            quantityMap.put(row.label, row.totalQuantity);
            costMap.put(row.label, (int) Math.round(row.totalCost));
        }

        fillBarChart(primaryBarChart, "Quantites", quantityMap, false);
        fillBarChart(secondaryBarChart, "Cout estime", costMap, false);
    }

    private void fillBarChart(BarChart<String, Number> chart, String name, Map<String, Integer> values, boolean legend) {
        if (chart == null) {
            return;
        }
        chart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name);
        int count = 0;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            count++;
            if (count >= 8) {
                break;
            }
        }
        chart.getData().add(series);
        chart.setLegendVisible(legend);
    }

    private Evenement findSuggestedEvent() {
        LocalDate today = LocalDate.now();
        return allEvents.stream()
                .filter(event -> {
                    LocalDate start = toLocalDate(event.getDate_debut_event());
                    return start != null
                            && !start.isBefore(today)
                            && isEventVisible(event)
                            && canParticipate(event);
                })
                .min(Comparator.comparing(this::eventDateOrMax)
                        .thenComparing(event -> normalized(event.getVille_event()))
                        .thenComparing(event -> normalized(event.getTitre_event())))
                .orElse(null);
    }

    private boolean isEventVisible(Evenement event) {
        String status = normalized(event.getStatut_event());
        return status.contains("publi") || status.contains("ligne") || status.contains("actif");
    }

    private void initCalendarScaffold() {
        if (calendarWeekHeader == null || calendarMonthGrid == null) {
            return;
        }

        if (calendarWeekHeader.getColumnConstraints().isEmpty()) {
            for (int i = 0; i < 7; i++) {
                ColumnConstraints constraints = new ColumnConstraints();
                constraints.setPercentWidth(100.0 / 7.0);
                constraints.setHgrow(Priority.ALWAYS);
                calendarWeekHeader.getColumnConstraints().add(constraints);
            }
        }

        if (calendarMonthGrid.getColumnConstraints().isEmpty()) {
            for (int i = 0; i < 7; i++) {
                ColumnConstraints constraints = new ColumnConstraints();
                constraints.setPercentWidth(100.0 / 7.0);
                constraints.setHgrow(Priority.ALWAYS);
                calendarMonthGrid.getColumnConstraints().add(constraints);
            }
        }

        buildCalendarWeekHeader();
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
        VBox cell = new VBox(6);
        cell.getStyleClass().add("calendar-day-cell");
        if (!cellDate.getMonth().equals(currentCalendarMonth.getMonth())) {
            cell.getStyleClass().add("calendar-day-cell-muted");
        }
        if (LocalDate.now().equals(cellDate)) {
            cell.getStyleClass().add("calendar-day-cell-today");
        }

        Label dayNumber = new Label(String.valueOf(cellDate.getDayOfMonth()));
        dayNumber.getStyleClass().add("calendar-day-number");

        VBox eventsBox = new VBox(5);
        VBox.setVgrow(eventsBox, Priority.ALWAYS);

        List<Evenement> events = getEventsForDate(cellDate);
        if (events.isEmpty()) {
            Label empty = new Label("Libre");
            empty.getStyleClass().add("calendar-empty-label");
            eventsBox.getChildren().add(empty);
        } else {
            for (int i = 0; i < Math.min(2, events.size()); i++) {
                eventsBox.getChildren().add(createCalendarEventChip(events.get(i)));
            }
            if (events.size() > 2) {
                Label more = new Label("+" + (events.size() - 2) + " autre(s)");
                more.getStyleClass().add("calendar-more-label");
                eventsBox.getChildren().add(more);
            }
        }

        cell.getChildren().addAll(dayNumber, eventsBox);
        return cell;
    }

    private List<Evenement> getEventsForDate(LocalDate date) {
        List<Evenement> events = new ArrayList<>();
        for (Evenement event : allEvents) {
            LocalDate start = toLocalDate(event.getDate_debut_event());
            LocalDate end = toLocalDate(event.getDate_fin_event());
            if (start == null) {
                continue;
            }
            LocalDate effectiveEnd = end == null ? start : end;
            if ((!date.isBefore(start)) && (!date.isAfter(effectiveEnd))) {
                events.add(event);
            }
        }
        events.sort(Comparator.comparing(this::eventDateOrMax).thenComparing(event -> normalized(event.getTitre_event())));
        return events;
    }

    private Button createCalendarEventChip(Evenement event) {
        Button chip = new Button(shortText(blankDash(event.getTitre_event()), 20));
        chip.getStyleClass().add("calendar-event-chip");
        chip.setMaxWidth(Double.MAX_VALUE);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setOnAction(e -> {
            if (calendarSelectionHintLabel != null) {
                calendarSelectionHintLabel.setText("Selection : " + blankDash(event.getTitre_event())
                        + " • " + formatDateRange(event) + " • " + blankDash(event.getVille_event()));
            }
            showEventDetailsPopup(event);
        });
        return chip;
    }

    private void showEmptyState(String titleText, String subtitleText) {
        rowsContainer.getChildren().clear();
        VBox empty = new VBox(8);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(40));
        Label title = new Label(titleText);
        title.getStyleClass().add("empty-title");
        Label text = new Label(subtitleText);
        text.getStyleClass().add("empty-text");
        text.setWrapText(true);
        empty.getChildren().addAll(title, text);
        rowsContainer.getChildren().add(empty);
    }

    private void setKpi(String t1, String v1, String s1, String t2, String v2, String s2, String t3, String v3, String s3, String t4, String v4, String s4) {
        kpi1TitleLabel.setText(t1);
        kpi1ValueLabel.setText(v1);
        kpi1SubtitleLabel.setText(s1);
        kpi2TitleLabel.setText(t2);
        kpi2ValueLabel.setText(v2);
        kpi2SubtitleLabel.setText(s2);
        kpi3TitleLabel.setText(t3);
        kpi3ValueLabel.setText(v3);
        kpi3SubtitleLabel.setText(s3);
        kpi4TitleLabel.setText(t4);
        kpi4ValueLabel.setText(v4);
        kpi4SubtitleLabel.setText(s4);
    }

    private void setHeroChips(String one, String two, String three) {
        if (heroChipOneLabel != null) {
            heroChipOneLabel.setText(one);
        }
        if (heroChipTwoLabel != null) {
            heroChipTwoLabel.setText(two);
        }
        if (heroChipThreeLabel != null) {
            heroChipThreeLabel.setText(three);
        }
    }

    private List<InsightCard> buildEventControlInsights() {
        return List.of(
                new InsightCard("Priorite terrain", topUpcomingCity(), "Ville a surveiller en premier sur les prochaines dates."),
                new InsightCard("Participation admin", countBadminDemandes() + " demande(s)", "Inscriptions envoyees depuis votre session admin."),
                new InsightCard("Controle securite", buildControlPriority(), "Alerte dominante sur le portefeuille actuel.")
        );
    }

    private List<InsightCard> buildParticipantInsights() {
        return List.of(
                new InsightCard("Plus actif", topEventByDemandes(), "Événement avec le plus de demandes."),
                new InsightCard("BADMIN", String.valueOf(countBadminDemandes()), "Demandes envoyées depuis la session BADMIN."),
                new InsightCard("Dernier statut", latestDecisionText(), "Dernière décision enregistrée ou attente en cours.")
        );
    }

    private List<InsightCard> buildResourceInsights() {
        return List.of(
                new InsightCard("Catégorie dominante", dominantResourceCategory(), "Catégorie la plus représentée dans la liste filtrée."),
                new InsightCard("Événement riche", topResourceEvent(), "Événement qui concentre le plus de ressources."),
                new InsightCard("Visibilité", countPublicResources() + " publiques", "Part des ressources visibles côté public.")
        );
    }

    private List<InsightCard> buildEventStatsInsights() {
        String dominantType = dashboardData.parType.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("-");
        String dominantCity = dashboardData.parVille.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("-");
        String recommendation = dashboardData.recommandations.isEmpty() ? "Aucune recommandation." : dashboardData.recommandations.get(0);
        return List.of(
                new InsightCard("Type dominant", dominantType, "Format le plus fréquent sur la période analysée."),
                new InsightCard("Ville dominante", dominantCity, "Zone la plus active du portefeuille événementiel."),
                new InsightCard("Priorité", recommendation, "Lecture rapide issue du moteur BI.")
        );
    }

    private List<InsightCard> buildResourceStatsInsights(ResourceStats stats) {
        return List.of(
                new InsightCard("Type principal", stats.topType, "Type de ressource le plus représenté."),
                new InsightCard("Catégorie leader", stats.topCategory, "Famille de ressources dominante."),
                new InsightCard("Événement le plus équipé", stats.topEvent, "Événement qui centralise le plus de ressources.")
        );
    }

    private ResourceStats buildResourceStats() {
        ResourceStats stats = new ResourceStats();
        Map<String, ResourceAggregateRow> categoryRows = new LinkedHashMap<>();
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        Map<String, Integer> typeCounts = new LinkedHashMap<>();
        Map<String, Integer> eventCounts = new LinkedHashMap<>();

        for (Ressource resource : allResources) {
            stats.totalResources++;
            stats.totalQuantity += Math.max(0, resource.getQuantite_disponible_ressource());
            stats.totalEstimatedCost += Math.max(0, resource.getCout_estime_ressource());
            if (resource.isEst_publique_ressource()) stats.publicResources++;

            String category = blankDash(resource.getCategorie_ressource());
            String type = blankDash(resource.getType_ressource());
            String eventName = eventTitle(resource.getEvenement() == null ? 0 : resource.getEvenement().getId());

            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
            eventCounts.put(eventName, eventCounts.getOrDefault(eventName, 0) + 1);

            ResourceAggregateRow row = categoryRows.computeIfAbsent(category, key -> new ResourceAggregateRow(key, type, 0, 0, 0, eventName, "Synthèse catégorie"));
            row.resourceCount++;
            row.totalQuantity += Math.max(0, resource.getQuantite_disponible_ressource());
            row.totalCost += Math.max(0, resource.getCout_estime_ressource());
            if (eventCounts.get(eventName) >= eventCounts.getOrDefault(row.topEventName, 0)) row.topEventName = eventName;
        }

        stats.rows = new ArrayList<>(categoryRows.values());
        stats.topCategory = topEntry(categoryCounts);
        stats.topType = topEntry(typeCounts);
        stats.topEvent = topEntry(eventCounts);
        return stats;
    }

    private void showEventDetailsPopup(Evenement event) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails événement");
        dialog.setHeaderText(null);
        dialog.setResizable(false);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);

        URL cssUrl = getClass().getResource("/CSS/admin-produits.css");
        if (cssUrl != null) pane.getStylesheets().add(cssUrl.toExternalForm());
        pane.getStyleClass().add("product-details-dialog");

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("details-root");

        HBox top = new HBox(16);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(90, 90);
        imgBox.getStyleClass().add("details-image-box");

        Image img = loadImage(safe(event.getImage_couverture_event()));
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(78);
            iv.setFitHeight(78);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            imgBox.getChildren().add(iv);
        } else {
            imgBox.getChildren().add(textLabel("EVT", "image-placeholder-text"));
        }

        VBox titleBox = new VBox(8);
        titleBox.getChildren().addAll(
                textLabel(safe(event.getTitre_event()), "details-title"),
                textLabel("ID: #" + event.getId(), "details-id"),
                new HBox(8, badgeLabel(emptyDash(event.getType_event()), "details-category"), statusBadge(event.getStatut_event()))
        );
        top.getChildren().addAll(imgBox, titleBox);

        HBox main = new HBox(22);
        VBox left = new VBox(16);
        left.setPrefWidth(360);

        StackPane bigImage = new StackPane();
        bigImage.setPrefSize(340, 260);
        bigImage.getStyleClass().add("details-big-image");
        if (img != null) {
            ImageView bigIv = new ImageView(img);
            bigIv.setFitWidth(260);
            bigIv.setFitHeight(230);
            bigIv.setPreserveRatio(true);
            bigIv.setSmooth(true);
            bigImage.getChildren().add(bigIv);
        } else {
            bigImage.getChildren().add(textLabel("Image événement", "big-placeholder-text"));
        }

        HBox miniStats = new HBox(12,
                detailsMiniBox("Ville", emptyDash(event.getVille_event()), false),
                detailsMiniBox("Participants", getAcceptedCount(event) + " / " + maxParticipantsText(event), getPendingCount(event) > 0));

        Button participateButton = new Button(buildParticipationButtonText(event));
        participateButton.getStyleClass().add("details-close-button");
        participateButton.setDisable(!canParticipate(event));
        participateButton.setOnAction(e -> {
            submitParticipation(event);
            dialog.close();
        });
        left.getChildren().addAll(bigImage, miniStats, participateButton);

        VBox right = new VBox(16);
        right.setPrefWidth(520);
        VBox descBox = new VBox(10);
        descBox.getStyleClass().add("details-card");
        descBox.getChildren().addAll(
                textLabel("Description", "details-card-title"),
                textLabel(safe(event.getDescription_event()).isBlank() ? "Aucune description disponible." : safe(event.getDescription_event()), "details-desc")
        );

        VBox summary = new VBox(12);
        summary.getStyleClass().add("details-card");
        summary.getChildren().add(textLabel("Résumé rapide", "details-card-title"));
        summary.getChildren().addAll(
                detailsInfoLine("Lieu", emptyDash(event.getNom_lieu_event())),
                detailsInfoLine("Adresse", emptyDash(event.getAdresse_event())),
                detailsInfoLine("Date", formatDateRange(event)),
                detailsInfoLine("Organisateur", emptyDash(event.getNom_organisateur_event())),
                detailsInfoLine("Inscription", event.isInscription_obligatoire_event() ? "Obligatoire" : "Libre"),
                detailsInfoLine("Date limite", event.getDate_limite_inscription_event() == null ? "-" : event.getDate_limite_inscription_event().toString())
        );

        VBox objectif = new VBox(10);
        objectif.getStyleClass().add("details-card");
        objectif.getChildren().addAll(
                textLabel("Objectif", "details-card-title"),
                textLabel(safe(event.getObjectif_event()).isBlank() ? "Aucun objectif renseigné." : safe(event.getObjectif_event()), "details-desc")
        );

        right.getChildren().addAll(descBox, summary, objectif);
        main.getChildren().addAll(left, right);
        root.getChildren().addAll(top, main);
        pane.setContent(root);
        pane.setPrefWidth(980);
        pane.setPrefHeight(700);

        Button close = (Button) pane.lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.getStyleClass().add("details-close-button");
        dialog.showAndWait();
    }

    private VBox detailsMiniBox(String label, String value, boolean warning) {
        VBox box = new VBox(6);
        box.getStyleClass().add("details-mini-box");
        box.getChildren().addAll(
                textLabel(label, "details-mini-label"),
                textLabel(value, warning ? "details-mini-value-danger" : "details-mini-value")
        );
        return box;
    }

    private HBox detailsInfoLine(String label, String value) {
        HBox line = new HBox(12);
        line.setAlignment(Pos.CENTER_LEFT);
        Label left = textLabel(label + " :", "summary-label");
        left.setMinWidth(110);
        Label right = textLabel(value, "summary-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        line.getChildren().addAll(left, right, spacer);
        return line;
    }

    private boolean canParticipate(Evenement event) {
        User user = SessionManager.getCurrentUser();
        return user != null && !participationService.userHasDemande(event, user.getId());
    }

    private String buildParticipationButtonText(Evenement event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) return "Connexion requise";
        if (participationService.userHasDemande(event, user.getId())) return "Participation déjà envoyée";
        return "Participer en tant que BADMIN";
    }

    private void submitParticipation(Evenement event) {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showInfo("Connectez-vous d'abord pour participer à un événement.");
            return;
        }
        if (participationService.userHasDemande(event, user.getId())) {
            showInfo("Votre participation existe déjà pour cet événement.");
            return;
        }

        try {
            ParticipationDemande demande = new ParticipationDemande();
            demande.setUserId(user.getId());
            demande.setNom(safe(user.getNom()));
            demande.setPrenom(safe(user.getPrenom()));
            demande.setEmail(safe(user.getEmailUser()));
            demande.setTelephone(participationService.normalizeTunisianPhone(user.getTelephoneUser()));
            demande.setMotif("Participation depuis l'espace admin BADMIN.");
            ParticipationDemande created = participationService.ajouterDemande(event, demande);
            showInfo(ParticipationDemande.STATUS_ACCEPTED.equals(created.getStatus())
                    ? "Participation acceptée automatiquement."
                    : "Participation enregistrée en attente de validation.");
            loadAllData();
        } catch (Exception e) {
            e.printStackTrace();
            showWarning("Participation", "Impossible d'enregistrer la participation : " + e.getMessage());
        }
    }

    private int getAcceptedCount(Evenement event) {
        int count = 0;
        for (ParticipationDemande demande : participationService.getDemandes(event)) {
            if (ParticipationDemande.STATUS_ACCEPTED.equals(demande.getStatus())) count++;
        }
        return count;
    }

    private int getPendingCount(Evenement event) {
        int count = 0;
        for (ParticipationDemande demande : participationService.getDemandes(event)) {
            if (ParticipationDemande.STATUS_PENDING.equals(demande.getStatus()) || ParticipationDemande.STATUS_WAITING.equals(demande.getStatus())) count++;
        }
        return count;
    }

    private int countDraftEvents() {
        int count = 0;
        for (Evenement event : allEvents) {
            if (normalized(event.getStatut_event()).contains("brouillon")) {
                count++;
            }
        }
        return count;
    }

    private int countExpiredRegistrationDeadlines() {
        int count = 0;
        LocalDate today = LocalDate.now();
        for (Evenement event : allEvents) {
            LocalDate limit = toLocalDate(event.getDate_limite_inscription_event());
            LocalDate start = toLocalDate(event.getDate_debut_event());
            if (limit != null && start != null && limit.isBefore(today) && !start.isBefore(today) && isEventVisible(event)) {
                count++;
            }
        }
        return count;
    }

    private String topUpcomingCity() {
        LocalDate today = LocalDate.now();
        return allEvents.stream()
                .filter(event -> {
                    LocalDate start = toLocalDate(event.getDate_debut_event());
                    return start != null && !start.isBefore(today);
                })
                .sorted(Comparator.comparing(this::eventDateOrMax))
                .map(event -> blankDash(event.getVille_event()))
                .findFirst()
                .orElse("-");
    }

    private String buildControlPriority() {
        int pending = countPendingParticipants();
        int deadlines = countExpiredRegistrationDeadlines();
        int drafts = countDraftEvents();
        if (pending > 0) {
            return pending + " validation(s) en attente";
        }
        if (deadlines > 0) {
            return deadlines + " date(s) limite depassees";
        }
        if (drafts > 0) {
            return drafts + " brouillon(s) a finaliser";
        }
        return "Portefeuille stable";
    }

    private int countPublishedEvents() {
        int count = 0;
        for (Evenement event : allEvents) {
            String status = normalized(event.getStatut_event());
            if (status.contains("publi") || status.contains("ligne")) count++;
        }
        return count;
    }

    private int countAcceptedParticipants() {
        int count = 0;
        for (ParticipationRow row : allParticipationRows) {
            if (ParticipationDemande.STATUS_ACCEPTED.equals(row.demande.getStatus())) count++;
        }
        return count;
    }

    private int countPendingParticipants() {
        int count = 0;
        for (ParticipationRow row : allParticipationRows) {
            String status = row.demande.getStatus();
            if (ParticipationDemande.STATUS_PENDING.equals(status) || ParticipationDemande.STATUS_WAITING.equals(status)) count++;
        }
        return count;
    }

    private int countEventsWithDemandes() {
        int count = 0;
        for (Evenement event : allEvents) {
            if (!participationService.getDemandes(event).isEmpty()) count++;
        }
        return count;
    }

    private int countResourcesByType(String type) {
        int count = 0;
        for (Ressource resource : allResources) {
            if (type.equalsIgnoreCase(safe(resource.getType_ressource()))) count++;
        }
        return count;
    }

    private int countPublicResources() {
        int count = 0;
        for (Ressource resource : allResources) {
            if (resource.isEst_publique_ressource()) count++;
        }
        return count;
    }

    private int countBadminDemandes() {
        User current = SessionManager.getCurrentUser();
        if (current == null) return 0;
        int count = 0;
        for (ParticipationRow row : allParticipationRows) {
            if (row.demande.getUserId() == current.getId()) count++;
        }
        return count;
    }

    private String latestDecisionText() {
        return allParticipationRows.stream()
                .map(row -> blankDash(row.demande.getDecidedAt()))
                .filter(value -> !"-".equals(value))
                .max(String::compareTo)
                .orElse("En attente");
    }

    private String topEventByDemandes() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ParticipationRow row : allParticipationRows) {
            String key = safe(row.event.getTitre_event());
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return topEntry(counts);
    }

    private String dominantResourceCategory() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Ressource resource : allResources) {
            String key = blankDash(resource.getCategorie_ressource());
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return topEntry(counts);
    }

    private String topResourceEvent() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Ressource resource : allResources) {
            String key = eventTitle(resource.getEvenement() == null ? 0 : resource.getEvenement().getId());
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return topEntry(counts);
    }

    private String topEntry(Map<String, Integer> values) {
        return values.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("-");
    }

    private String eventTitle(int eventId) {
        Evenement event = eventsById.get(eventId);
        if (event == null || safe(event.getTitre_event()).isBlank()) return eventId > 0 ? "Événement #" + eventId : "-";
        return event.getTitre_event();
    }

    private String maxParticipantsText(Evenement event) {
        return event.getNb_participants_max_event() <= 0 ? "Illimité" : String.valueOf(event.getNb_participants_max_event());
    }

    private LocalDate eventDateOrMax(Evenement event) {
        LocalDate date = toLocalDate(event.getDate_debut_event());
        return date == null ? LocalDate.MAX : date;
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

    private String formatDateRange(Evenement event) {
        LocalDate start = toLocalDate(event.getDate_debut_event());
        LocalDate end = toLocalDate(event.getDate_fin_event());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (start == null && end == null) return "-";
        if (start != null && end != null) return formatter.format(start) + " → " + formatter.format(end);
        return formatter.format(start != null ? start : end);
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;
        try {
            if (path.startsWith("http://") || path.startsWith("https://")) return new Image(path, true);
            File file = new File(path);
            if (file.exists()) return new Image(file.toURI().toString(), true);
        } catch (Exception ignored) {
        }
        return null;
    }

    private String formatMoney(double value) {
        return String.format(Locale.FRANCE, "%.2f DT", value);
    }

    private boolean contains(String value, String keyword) {
        return normalized(value).contains(keyword);
    }

    private String normalized(String value) {
        return safe(value).trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String emptyDash(String value) {
        return safe(value).isBlank() ? "—" : safe(value);
    }

    private String blankDash(String value) {
        return safe(value).isBlank() ? "-" : safe(value);
    }

    private String shortText(String value, int max) {
        if (value == null || value.length() <= max) return safe(value);
        return value.substring(0, max) + "...";
    }

    @FXML
    private void participateInSuggestedEvent() {
        if (suggestedEvent == null) {
            showInfo("Aucun evenement recommande pour le moment.");
            return;
        }
        submitParticipation(suggestedEvent);
    }

    @FXML
    private void openSuggestedEventDetails() {
        if (suggestedEvent == null) {
            showInfo("Aucun evenement recommande pour le moment.");
            return;
        }
        showEventDetailsPopup(suggestedEvent);
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

    @FXML
    private void openDashboard() {
        goTo("/AdminWelcome.fxml");
    }

    @FXML
    private void openProduits() {
        goTo("/ProduitAdmin.fxml");
    }

    @FXML
    private void openEvents() {
        currentSection = Section.LIST_EVENTS;
        nextSection = currentSection;
        renderCurrentSection();
    }

    @FXML
    private void openEventParticipants() {
        currentSection = Section.PARTICIPANT_REQUESTS;
        nextSection = currentSection;
        renderCurrentSection();
    }

    @FXML
    private void openRessources() {
        currentSection = Section.RESOURCES;
        nextSection = currentSection;
        renderCurrentSection();
    }

    @FXML
    private void openStatsEvents() {
        currentSection = Section.EVENT_STATS;
        nextSection = currentSection;
        renderCurrentSection();
    }

    @FXML
    private void openStatsRessources() {
        currentSection = Section.RESOURCE_STATS;
        nextSection = currentSection;
        renderCurrentSection();
    }

    @FXML
    private void openPatients() {
        showWarning("Navigation", "Cette page n'est pas encore branchée dans votre espace admin.");
    }

    @FXML
    private void openUsers() {
        showWarning("Navigation", "Cette page n'est pas encore branchée dans votre espace admin.");
    }

    @FXML
    private void openRoles() {
        showWarning("Navigation", "Cette page n'est pas encore branchée dans votre espace admin.");
    }

    @FXML
    private void openCommandes() {
        showWarning("Navigation", "Cette page n'est pas encore branchée dans votre espace admin.");
    }

    @FXML
    private void openDetection() {
        showWarning("Navigation", "Cette page n'est pas encore branchée dans votre espace admin.");
    }

    @FXML
    private void openConsultations() {
        showWarning("Navigation", "Cette page n'est pas encore branchée dans votre espace admin.");
    }

    @FXML
    private void openStatsConsultations() {
        showWarning("Navigation", "Cette page n'est pas encore branchée dans votre espace admin.");
    }

    @FXML
    private void openReclamations() {
        showWarning("Navigation", "Cette page n'est pas encore branchée dans votre espace admin.");
    }

    @FXML
    private void openStatsReclamations() {
        showWarning("Navigation", "Cette page n'est pas encore branchée dans votre espace admin.");
    }

    @FXML
    private void openPosts() {
        goTo("/PendingPostsAdmin.fxml");
    }

    @FXML
    private void openBlogComments() {
        goTo("/PendingPostsAdmin.fxml");
    }

    @FXML
    private void openBlogModeration() {
        goTo("/PendingPostsAdmin.fxml");
    }

    @FXML
    private void goBackSite() {
        goTo("/AdminWelcome.fxml");
    }

    @FXML
    private void logout() {
        SessionManager.clear();
        goTo("/FrontFXML/Login.fxml");
    }

    private void goTo(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                showWarning("Navigation", "Fichier introuvable : " + fxmlPath);
                return;
            }
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 850);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showWarning("Navigation", "Impossible d'ouvrir : " + fxmlPath);
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record HeaderSpec(String title, double width) {}
    private record ParticipationRow(Evenement event, ParticipationDemande demande) {}
    private record InsightCard(String title, String value, String subtitle) {}

    private static final class ResourceAggregateRow {
        private final String label;
        private final String type;
        private int resourceCount;
        private int totalQuantity;
        private double totalCost;
        private String topEventName;
        private final String description;

        private ResourceAggregateRow(String label, String type, int resourceCount, int totalQuantity, double totalCost, String topEventName, String description) {
            this.label = label;
            this.type = type;
            this.resourceCount = resourceCount;
            this.totalQuantity = totalQuantity;
            this.totalCost = totalCost;
            this.topEventName = topEventName;
            this.description = description;
        }
    }

    private static final class ResourceStats {
        private int totalResources;
        private int publicResources;
        private int totalQuantity;
        private double totalEstimatedCost;
        private String topCategory = "-";
        private String topType = "-";
        private String topEvent = "-";
        private List<ResourceAggregateRow> rows = new ArrayList<>();
    }
}
