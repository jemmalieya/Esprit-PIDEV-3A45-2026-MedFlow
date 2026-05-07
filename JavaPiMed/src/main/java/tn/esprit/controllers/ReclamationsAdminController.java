package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import tn.esprit.entities.Reclamation;
import tn.esprit.services.ReclamationService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReclamationsAdminController {

    @FXML private BorderPane rootPane;

    @FXML private Label totalReclamationsLabel;
    @FXML private Label enAttenteLabel;
    @FXML private Label traiteesLabel;
    @FXML private Label critiquesLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> priorityFilter;

    @FXML private TableView<Reclamation> reclamationTable;
    @FXML private TableColumn<Reclamation, String> referenceCol;
    @FXML private TableColumn<Reclamation, String> typeCol;
    @FXML private TableColumn<Reclamation, String> statutCol;
    @FXML private TableColumn<Reclamation, String> prioriteCol;
    @FXML private TableColumn<Reclamation, String> descriptionCol;
    @FXML private TableColumn<Reclamation, String> dateCol;

    private final ReclamationService reclamationService = new ReclamationService();

    private final ObservableList<Reclamation> masterList = FXCollections.observableArrayList();
    private FilteredList<Reclamation> filteredList;

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadData();
    }

    private void setupTable() {
        referenceCol.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getReference_reclamation()))
        );

        typeCol.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getType()))
        );

        statutCol.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getStatut_reclamation()))
        );

        prioriteCol.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getPriorite()))
        );

        descriptionCol.setCellValueFactory(data ->
                new SimpleStringProperty(shortText(
                        firstNonBlank(data.getValue().getDescription(), data.getValue().getContenu()),
                        120
                ))
        );

        dateCol.setCellValueFactory(data ->
                new SimpleStringProperty(formatDate(data.getValue()))
        );

        setupStatusBadge();
        setupPriorityBadge();

        reclamationTable.setPlaceholder(new Label("Aucune réclamation trouvée."));
    }

    private void setupStatusBadge() {
        statutCol.setCellFactory(column -> new TableCell<Reclamation, String>() {
            private final Label badge = new Label();

            {
                badge.setMinWidth(95);
                badge.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);

                if (empty || statut == null || statut.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                badge.setText(statut);

                String s = statut.toLowerCase(Locale.ROOT);

                if (s.contains("attente")) {
                    badge.getStyleClass().setAll("rec-badge", "badge-orange");
                } else if (s.contains("trait")) {
                    badge.getStyleClass().setAll("rec-badge", "badge-green");
                } else {
                    badge.getStyleClass().setAll("rec-badge", "badge-blue");
                }

                setText(null);
                setGraphic(badge);
            }
        });
    }

    private void setupPriorityBadge() {
        prioriteCol.setCellFactory(column -> new TableCell<Reclamation, String>() {
            private final Label badge = new Label();

            {
                badge.setMinWidth(90);
                badge.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String priorite, boolean empty) {
                super.updateItem(priorite, empty);

                if (empty || priorite == null || priorite.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                badge.setText(priorite);

                if (priorite.equalsIgnoreCase("Élevée") || priorite.equalsIgnoreCase("Elevee")) {
                    badge.getStyleClass().setAll("rec-badge", "badge-red");
                } else if (priorite.equalsIgnoreCase("Moyenne")) {
                    badge.getStyleClass().setAll("rec-badge", "badge-orange");
                } else {
                    badge.getStyleClass().setAll("rec-badge", "badge-green");
                }

                setText(null);
                setGraphic(badge);
            }
        });
    }

    private void setupFilters() {
        statusFilter.setItems(FXCollections.observableArrayList(
                "Tous", "En attente", "Traitée", "Autre"
        ));
        statusFilter.setValue("Tous");

        priorityFilter.setItems(FXCollections.observableArrayList(
                "Toutes", "Élevée", "Moyenne", "Faible"
        ));
        priorityFilter.setValue("Toutes");

        filteredList = new FilteredList<>(masterList, r -> true);
        reclamationTable.setItems(filteredList);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        priorityFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void loadData() {
        try {
            List<Reclamation> list = loadAllReclamationsFromService();

            if (list == null) {
                list = new ArrayList<>();
            }

            masterList.setAll(list);
            applyFilters();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les réclamations.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Reclamation> loadAllReclamationsFromService() throws Exception {
        try {
            Method getAllMethod = reclamationService.getClass().getMethod("getAll");
            Object result = getAllMethod.invoke(reclamationService);

            if (result instanceof List<?>) {
                return (List<Reclamation>) result;
            }
        } catch (NoSuchMethodException ignored) {
        }

        try {
            Method recupererMethod = reclamationService.getClass().getMethod("recuperer");
            Object result = recupererMethod.invoke(reclamationService);

            if (result instanceof List<?>) {
                return (List<Reclamation>) result;
            }
        } catch (NoSuchMethodException ignored) {
        }

        throw new IllegalStateException("Aucune méthode getAll() ou recuperer() trouvée dans ReclamationService.");
    }

    private void applyFilters() {
        if (filteredList == null) {
            return;
        }

        String keyword = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        String selectedStatus = statusFilter.getValue() == null ? "Tous" : statusFilter.getValue();
        String selectedPriority = priorityFilter.getValue() == null ? "Toutes" : priorityFilter.getValue();

        filteredList.setPredicate(rec -> {
            boolean matchesSearch =
                    keyword.isEmpty()
                            || contains(rec.getReference_reclamation(), keyword)
                            || contains(rec.getType(), keyword)
                            || contains(rec.getStatut_reclamation(), keyword)
                            || contains(rec.getPriorite(), keyword)
                            || contains(rec.getDescription(), keyword)
                            || contains(rec.getContenu(), keyword);

            boolean matchesStatus =
                    selectedStatus.equals("Tous")
                            || (selectedStatus.equals("En attente")
                            && safe(rec.getStatut_reclamation()).toLowerCase(Locale.ROOT).contains("attente"))
                            || (selectedStatus.equals("Traitée")
                            && safe(rec.getStatut_reclamation()).toLowerCase(Locale.ROOT).contains("trait"))
                            || (selectedStatus.equals("Autre") && isOtherStatus(rec));

            boolean matchesPriority =
                    selectedPriority.equals("Toutes")
                            || safe(rec.getPriorite()).equalsIgnoreCase(selectedPriority);

            return matchesSearch && matchesStatus && matchesPriority;
        });

        updateStats();
    }

    private void updateStats() {
        int total = filteredList == null ? 0 : filteredList.size();
        int attente = 0;
        int traitees = 0;
        int critiques = 0;

        if (filteredList != null) {
            for (Reclamation r : filteredList) {
                String statut = safe(r.getStatut_reclamation()).toLowerCase(Locale.ROOT);
                String priorite = safe(r.getPriorite());

                if (statut.contains("attente")) {
                    attente++;
                }

                if (statut.contains("trait")) {
                    traitees++;
                }

                if (priorite.equalsIgnoreCase("Élevée") || priorite.equalsIgnoreCase("Elevee")) {
                    critiques++;
                }
            }
        }

        if (totalReclamationsLabel != null) {
            totalReclamationsLabel.setText(String.valueOf(total));
        }

        if (enAttenteLabel != null) {
            enAttenteLabel.setText(String.valueOf(attente));
        }

        if (traiteesLabel != null) {
            traiteesLabel.setText(String.valueOf(traitees));
        }

        if (critiquesLabel != null) {
            critiquesLabel.setText(String.valueOf(critiques));
        }
    }

    @FXML
    private void refreshData() {
        loadData();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean isOtherStatus(Reclamation rec) {
        String statut = safe(rec.getStatut_reclamation()).toLowerCase(Locale.ROOT);
        return !statut.contains("attente") && !statut.contains("trait");
    }

    private String formatDate(Reclamation rec) {
        try {
            if (rec != null && rec.getDate_creation_r() != null) {
                return rec.getDate_creation_r().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
        } catch (Exception ignored) {
        }

        return "-";
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isBlank()) {
            return a.trim();
        }

        if (b != null && !b.trim().isBlank()) {
            return b.trim();
        }

        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String shortText(String value, int max) {
        if (value == null) {
            return "";
        }

        String clean = value.replace("\n", " ").replace("\r", " ").trim();

        while (clean.contains("  ")) {
            clean = clean.replace("  ", " ");
        }

        if (clean.length() <= max) {
            return clean;
        }

        return clean.substring(0, max).trim() + "...";
    }

    /* ================= NAVIGATION SIDEBAR ================= */

    @FXML
    private void openDashboard() {
        goTo("/AdminWelcome.fxml");
    }

    @FXML
    private void openPatients() {
        goTo("/FXML/PatientsAdmin.fxml");
    }

    @FXML
    private void openUsers() {
        goTo("/FXML/UtilisateursAdmin.fxml");
    }

    @FXML
    private void openRoles() {
        goTo("/FXML/DemandesRole.fxml");
    }

    @FXML
    private void openUserRoles() {
        goTo("/FXML/UserRoles.fxml");
    }

    @FXML
    private void openProduits() {
        goTo("/ProduitAdmin.fxml");
    }

    @FXML
    private void openCommandes() {
        goTo("/CommandeAdmin.fxml");
    }

    @FXML
    private void openDetection() {
        goTo("/DetectionEpidemie.fxml");
    }

    @FXML
    private void openStockRupture() {
        goTo("/FXML/StockRupture.fxml");
    }

    @FXML
    private void openStatsProduits() {
        goTo("/StatProduit.fxml");
    }

    @FXML
    private void openEvents() {
        goTo("/FXML/EvenementsAdmin.fxml");
    }

    @FXML
    private void openEventParticipants() {
        goTo("/FXML/ParticipantsEvenement.fxml");
    }

    @FXML
    private void openRessources() {
        goTo("/FXML/RessourcesAdmin.fxml");
    }

    @FXML
    private void openStatsEvents() {
        goTo("/FXML/StatsEvenements.fxml");
    }

    @FXML
    private void openStatsRessources() {
        goTo("/FXML/StatsRessources.fxml");
    }

    @FXML
    private void openConsultations() {
        goTo("/FXML/ConsultationsParDocteur.fxml");
    }

    @FXML
    private void openRendezVous() {
        goTo("/FXML/RendezVousAdmin.fxml");
    }

    @FXML
    private void openStatsConsultations() {
        goTo("/FXML/StatsConsultations.fxml");
    }

    @FXML
    private void openReclamations() {
        goTo("/FXML/ReclamationsAdmin.fxml");
    }

    @FXML
    private void openReponsesReclamations() {
        goTo("/FXML/ReponsesReclamationsAdmin.fxml");
    }

    @FXML
    private void openReclamationsUrgentes() {
        goTo("/FXML/ReclamationsUrgentes.fxml");
    }

    @FXML
    private void openStatsReclamations() {
        goTo("/StatsReclamations.fxml");
    }

    @FXML
    private void openPosts() {
        goTo("/PostsAdmin.fxml");
    }

    @FXML
    private void openBlogComments() {
        goToFirst(
                "/BlogCommentsAdmin.fxml",
                "/CommentairesAdmin.fxml",
                "/FXML/CommentairesAdmin.fxml"
        );
    }

    @FXML
    private void openBlogModeration() {
        goTo("/PendingPostsAdmin.fxml");
    }

    @FXML
    private void openPostsEnAttente() {
        goTo("/PendingPostsAdmin.fxml");
    }

    @FXML
    private void openStatsBlog() {
        goTo("/BlogStatAdmin.fxml");
    }

    @FXML
    private void goBackSite() {
        goTo("/FXML/Home.fxml");
    }

    @FXML
    private void logout() {
        goToFirst("/FXML/Login.fxml", "/FrontFXML/Login.fxml");
    }

    private void goTo(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);

            if (url == null) {
                showWarning("Navigation", "Page introuvable : " + fxmlPath);
                return;
            }

            Parent page = FXMLLoader.load(url);
            rootPane.getScene().setRoot(page);

        } catch (IOException e) {
            e.printStackTrace();
            showWarning("Navigation", "Erreur lors de l'ouverture de la page : " + fxmlPath);
        }
    }

    private void goToFirst(String... paths) {
        for (String path : paths) {
            if (getClass().getResource(path) != null) {
                goTo(path);
                return;
            }
        }

        showWarning("Navigation", "Aucun fichier FXML trouvé pour cette page.");
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}