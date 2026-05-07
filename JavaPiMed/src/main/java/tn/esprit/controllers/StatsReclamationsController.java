package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import tn.esprit.entities.Reclamation;
import tn.esprit.services.ReclamationService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

import javafx.application.Platform;
import javafx.scene.Node;

public class StatsReclamationsController {

    @FXML private BorderPane rootPane;

    @FXML private Label totalLabel;
    @FXML private Label pendingLabel;
    @FXML private Label doneLabel;
    @FXML private Label highPriorityLabel;
    @FXML private Label typesLabel;
    @FXML private Label todayLabel;
    @FXML private Label resolutionRateLabel;

    @FXML private ComboBox<String> periodFilter;

    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> priorityBarChart;
    @FXML private BarChart<String, Number> typeBarChart;
    @FXML private LineChart<String, Number> evolutionLineChart;

    private final ReclamationService reclamationService = new ReclamationService();
    private final List<Reclamation> allReclamations = new ArrayList<>();

    @FXML
    public void initialize() {
        setupFilters();
        configureCharts();
        loadData();
    }
    private void configureCharts() {
        if (priorityBarChart != null) {
            priorityBarChart.setAnimated(false);
            priorityBarChart.setCategoryGap(28);
            priorityBarChart.setBarGap(6);
        }

        if (typeBarChart != null) {
            typeBarChart.setAnimated(false);
            typeBarChart.setCategoryGap(24);
            typeBarChart.setBarGap(6);
        }

        if (evolutionLineChart != null) {
            evolutionLineChart.setAnimated(false);
        }
    }

    private void setupFilters() {
        periodFilter.setItems(FXCollections.observableArrayList(
                "Toutes les périodes",
                "Aujourd'hui",
                "7 derniers jours",
                "30 derniers jours"
        ));
        periodFilter.setValue("Toutes les périodes");

        periodFilter.valueProperty().addListener((obs, oldVal, newVal) -> refreshStats());
    }

    private void loadData() {
        try {
            List<Reclamation> list = loadAllReclamationsFromService();

            allReclamations.clear();

            if (list != null) {
                allReclamations.addAll(list);
            }

            refreshStats();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les statistiques des réclamations.");
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

    private void refreshStats() {
        List<Reclamation> filtered = filterByPeriod(allReclamations);

        updateKpis(filtered);
        updateStatusPieChart(filtered);
        updatePriorityBarChart(filtered);
        updateTypeBarChart(filtered);
        updateEvolutionLineChart(filtered);
    }

    private List<Reclamation> filterByPeriod(List<Reclamation> source) {
        String period = periodFilter.getValue() == null ? "Toutes les périodes" : periodFilter.getValue();

        if (period.equals("Toutes les périodes")) {
            return new ArrayList<>(source);
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case "Aujourd'hui":
                startDate = today;
                break;
            case "7 derniers jours":
                startDate = today.minusDays(6);
                break;
            case "30 derniers jours":
                startDate = today.minusDays(29);
                break;
            default:
                return new ArrayList<>(source);
        }

        List<Reclamation> filtered = new ArrayList<>();

        for (Reclamation r : source) {
            LocalDate date = getReclamationDate(r);

            if (date != null && !date.isBefore(startDate) && !date.isAfter(today)) {
                filtered.add(r);
            }
        }

        return filtered;
    }

    private void updateKpis(List<Reclamation> list) {
        int total = list.size();
        int pending = 0;
        int done = 0;
        int high = 0;
        int today = 0;

        Set<String> types = new HashSet<>();
        LocalDate now = LocalDate.now();

        for (Reclamation r : list) {
            String statut = safe(r.getStatut_reclamation()).toLowerCase(Locale.ROOT);
            String priorite = safe(r.getPriorite());

            if (statut.contains("attente")) {
                pending++;
            }

            if (statut.contains("trait")) {
                done++;
            }

            if (priorite.equalsIgnoreCase("Élevée") || priorite.equalsIgnoreCase("Elevee")) {
                high++;
            }

            if (!safe(r.getType()).isBlank()) {
                types.add(r.getType().trim().toLowerCase(Locale.ROOT));
            }

            LocalDate date = getReclamationDate(r);
            if (date != null && date.isEqual(now)) {
                today++;
            }
        }

        int rate = total == 0 ? 0 : (int) Math.round((done * 100.0) / total);

        totalLabel.setText(String.valueOf(total));
        pendingLabel.setText(String.valueOf(pending));
        doneLabel.setText(String.valueOf(done));
        highPriorityLabel.setText(String.valueOf(high));
        typesLabel.setText(String.valueOf(types.size()));
        todayLabel.setText(String.valueOf(today));
        resolutionRateLabel.setText(rate + "%");
    }

    private void updateStatusPieChart(List<Reclamation> list) {
        int pending = 0;
        int done = 0;
        int other = 0;

        for (Reclamation r : list) {
            String statut = safe(r.getStatut_reclamation()).toLowerCase(Locale.ROOT);

            if (statut.contains("attente")) {
                pending++;
            } else if (statut.contains("trait")) {
                done++;
            } else {
                other++;
            }
        }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("En attente", pending),
                new PieChart.Data("Traitée", done),
                new PieChart.Data("Autre", other)
        );

        statusPieChart.setData(data);
    }

    private void updatePriorityBarChart(List<Reclamation> list) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Élevée", 0);
        counts.put("Moyenne", 0);
        counts.put("Faible", 0);
        counts.put("Autre", 0);

        for (Reclamation r : list) {
            String p = safe(r.getPriorite()).trim();

            if (p.equalsIgnoreCase("Élevée") || p.equalsIgnoreCase("Elevee")) {
                counts.put("Élevée", counts.get("Élevée") + 1);
            } else if (p.equalsIgnoreCase("Moyenne")) {
                counts.put("Moyenne", counts.get("Moyenne") + 1);
            } else if (p.equalsIgnoreCase("Faible")) {
                counts.put("Faible", counts.get("Faible") + 1);
            } else {
                counts.put("Autre", counts.get("Autre") + 1);
            }
        }

        priorityBarChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Priorités");

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(entry.getKey(), entry.getValue());

            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle(
                            "-fx-bar-fill: #2563eb;" +
                                    "-fx-background-color: linear-gradient(to top, #0891b2, #38bdf8);" +
                                    "-fx-background-radius: 8 8 0 0;"
                    );
                }
            });

            series.getData().add(data);
        }

        priorityBarChart.getData().add(series);

        Platform.runLater(() -> forceBarStyle(priorityBarChart));
    }

    private void updateTypeBarChart(List<Reclamation> list) {
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (Reclamation r : list) {
            String type = safe(r.getType()).trim();

            if (type.isBlank()) {
                type = "Sans type";
            }

            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }

        typeBarChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Types");

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(shortLabel(entry.getKey(), 16), entry.getValue());

            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle(
                            "-fx-bar-fill: #14b8a6;" +
                                    "-fx-background-color: linear-gradient(to top, #14b8a6, #67e8f9);" +
                                    "-fx-background-radius: 8 8 0 0;"
                    );
                }
            });

            series.getData().add(data);
        }

        typeBarChart.getData().add(series);

        Platform.runLater(() -> forceBarStyle(typeBarChart));
    }

    private void updateEvolutionLineChart(List<Reclamation> list) {
        Map<String, Integer> counts = new TreeMap<>();

        for (Reclamation r : list) {
            LocalDate date = getReclamationDate(r);
            String key = date == null ? "Sans date" : date.toString();
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }

        evolutionLineChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        evolutionLineChart.getData().add(series);
    }

    private LocalDate getReclamationDate(Reclamation r) {
        try {
            if (r != null && r.getDate_creation_r() != null) {
                return r.getDate_creation_r().toLocalDate();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    @FXML
    private void refreshData() {
        loadData();
    }

    private String shortLabel(String value, int max) {
        if (value == null) {
            return "";
        }

        String clean = value.trim();

        if (clean.length() <= max) {
            return clean;
        }

        return clean.substring(0, max).trim() + "...";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    /* ================= NAVIGATION SIDEBAR ================= */

    @FXML private void openDashboard() { goTo("/AdminWelcome.fxml"); }
    @FXML private void openPatients() { goTo("/FXML/PatientsAdmin.fxml"); }
    @FXML private void openUsers() { goTo("/FXML/UtilisateursAdmin.fxml"); }
    @FXML private void openRoles() { goTo("/FXML/DemandesRole.fxml"); }
    @FXML private void openUserRoles() { goTo("/FXML/UserRoles.fxml"); }
    @FXML private void openProduits() { goTo("/ProduitAdmin.fxml"); }
    @FXML private void openCommandes() { goTo("/CommandeAdmin.fxml"); }
    @FXML private void openDetection() { goTo("/DetectionEpidemie.fxml"); }
    @FXML private void openStockRupture() { goTo("/FXML/StockRupture.fxml"); }
    @FXML private void openStatsProduits() { goTo("/StatProduit.fxml"); }
    @FXML private void openEvents() { goTo("/FXML/EvenementsAdmin.fxml"); }
    @FXML private void openEventParticipants() { goTo("/FXML/ParticipantsEvenement.fxml"); }
    @FXML private void openRessources() { goTo("/FXML/RessourcesAdmin.fxml"); }
    @FXML private void openStatsEvents() { goTo("/FXML/StatsEvenements.fxml"); }
    @FXML private void openStatsRessources() { goTo("/FXML/StatsRessources.fxml"); }
    @FXML private void openConsultations() { goTo("/FXML/ConsultationsParDocteur.fxml"); }
    @FXML private void openRendezVous() { goTo("/FXML/RendezVousAdmin.fxml"); }
    @FXML private void openStatsConsultations() { goTo("/FXML/StatsConsultations.fxml"); }
    @FXML private void openReclamations() { goTo("/ReclamationsAdmin.fxml"); }
    @FXML private void openStatsReclamations() { goTo("/StatsReclamations.fxml"); }
    @FXML private void openPosts() { goTo("/PostsAdmin.fxml"); }
    @FXML private void openPostsEnAttente() { goTo("/PendingPostsAdmin.fxml"); }
    @FXML private void openStatsBlog() { goTo("/BlogStatAdmin.fxml"); }
    @FXML private void goBackSite() { goTo("/FXML/Home.fxml"); }
    @FXML private void logout() { goToFirst("/FXML/Login.fxml", "/FrontFXML/Login.fxml"); }

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

    private void forceBarStyle(BarChart<String, Number> chart) {
        if (chart == null || chart.getData() == null) {
            return;
        }

        for (XYChart.Series<String, Number> series : chart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                Node node = data.getNode();

                if (node != null) {
                    node.setStyle(
                            "-fx-bar-fill: #2563eb;" +
                                    "-fx-background-color: linear-gradient(to top, #0891b2, #38bdf8);" +
                                    "-fx-background-radius: 8 8 0 0;"
                    );
                }
            }
        }
    }
}