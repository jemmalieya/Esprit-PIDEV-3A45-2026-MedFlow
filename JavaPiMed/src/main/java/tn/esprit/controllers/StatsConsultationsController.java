package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import tn.esprit.entities.FicheMedicale;
import tn.esprit.entities.Prescription;
import tn.esprit.entities.RendezVous;
import tn.esprit.entities.User;
import tn.esprit.services.FicheMedicaleService;
import tn.esprit.services.PrescriptionService;
import tn.esprit.services.RendezVousService;
import tn.esprit.services.UserService;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsConsultationsController {

    private static final String CHART_TEXT_COLOR = "#0f2742";

    private final UserService userService = new UserService();
    private final RendezVousService rendezVousService = new RendezVousService();
    private final FicheMedicaleService ficheMedicaleService = new FicheMedicaleService();
    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private BorderPane rootPane;

    @FXML
    private TextField searchField;

    @FXML
    private Label dateLabel;

    @FXML
    private Label totalDoctorsLabel;

    @FXML
    private Label totalRendezVousLabel;

    @FXML
    private Label totalFichesLabel;

    @FXML
    private Label totalPrescriptionsLabel;

    @FXML
    private Label averageDurationLabel;

    @FXML
    private Label urgentLabel;

    @FXML
    private Label ficheCoverageLabel;

    @FXML
    private Label prescriptionCoverageLabel;

    @FXML
    private Label consultationInsightLabel;

    @FXML
    private Label topDoctorLabel;

    @FXML
    private Label busiestMonthLabel;

    @FXML
    private LineChart<String, Number> monthlyAppointmentsChart;

    @FXML
    private PieChart statusPieChart;

    @FXML
    private PieChart modePieChart;

    @FXML
    private PieChart medicinePieChart;

    @FXML
    private BarChart<String, Number> urgencyBarChart;

    @FXML
    private BarChart<String, Number> doctorLoadBarChart;

    @FXML
    public void initialize() {
        ensureStylesheetsLoaded();
        if (dateLabel != null) {
            dateLabel.setText("Statistiques consultations • " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        setupCharts();
        setupSearch();
        loadDashboard();
        Platform.runLater(this::animateVisibleSections);
    }

    private void ensureStylesheetsLoaded() {
        if (rootPane == null) {
            return;
        }

        addStylesheetIfPresent("/CSS/admin-welcome.css");
        addStylesheetIfPresent("/CSS/stats-consultations.css");
    }

    private void addStylesheetIfPresent(String resourcePath) {
        URL stylesheetUrl = getClass().getResource(resourcePath);
        if (stylesheetUrl == null) {
            return;
        }

        String stylesheet = stylesheetUrl.toExternalForm();
        if (!rootPane.getStylesheets().contains(stylesheet)) {
            rootPane.getStylesheets().add(stylesheet);
        }
    }

    private void setupSearch() {
        if (searchField == null) {
            return;
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> loadDashboard());
    }

    private void setupCharts() {
        if (monthlyAppointmentsChart != null) {
            monthlyAppointmentsChart.setAnimated(true);
            monthlyAppointmentsChart.setCreateSymbols(true);
        }
        if (urgencyBarChart != null) {
            urgencyBarChart.setAnimated(true);
        }
        if (doctorLoadBarChart != null) {
            doctorLoadBarChart.setAnimated(true);
        }
    }

    private void loadDashboard() {
        List<User> doctors = userService.getStaffByRoleAndType("STAFF", "RESP_PATIENTS");
        List<RendezVous> rendezVousList = rendezVousService.recuperer();
        List<FicheMedicale> fiches = ficheMedicaleService.recuperer();
        List<Prescription> prescriptions = prescriptionService.recuperer();

        String query = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (!query.isBlank()) {
            doctors = doctors.stream()
                    .filter(doctor -> buildDoctorDisplayName(doctor).toLowerCase(Locale.ROOT).contains(query)
                            || String.valueOf(doctor.getId()).contains(query)
                            || valueOrDash(doctor.getTypeStaff()).toLowerCase(Locale.ROOT).contains(query))
                    .toList();
        }

        Map<Integer, User> doctorById = new HashMap<>();
        for (User doctor : userService.getStaffByRoleAndType("STAFF", "RESP_PATIENTS")) {
            doctorById.put(doctor.getId(), doctor);
        }

        Map<Integer, List<RendezVous>> rendezVousByDoctor = new HashMap<>();
        for (RendezVous rendezVous : rendezVousList) {
            rendezVousByDoctor.computeIfAbsent(rendezVous.getIdStaff(), key -> new ArrayList<>()).add(rendezVous);
        }

        Map<Integer, Integer> ficheCountByDoctor = new HashMap<>();
        Map<Integer, Integer> prescriptionCountByDoctor = new HashMap<>();
        Map<Integer, String> rendezVousToDoctorName = new HashMap<>();
        Map<Integer, Integer> fichesByRendezVous = new HashMap<>();
        for (FicheMedicale ficheMedicale : fiches) {
            if (ficheMedicale == null || ficheMedicale.getRendez_vous_id() == null) {
                continue;
            }
            fichesByRendezVous.put(ficheMedicale.getRendez_vous_id(), ficheMedicale.getId());
        }

        for (RendezVous rendezVous : rendezVousList) {
            User doctor = doctorById.get(rendezVous.getIdStaff());
            rendezVousToDoctorName.put(rendezVous.getId(), doctor == null ? "Staff #" + rendezVous.getIdStaff() : buildDoctorDisplayName(doctor));
        }

        Map<Integer, Integer> prescriptionCountByFiche = new HashMap<>();
        for (Prescription prescription : prescriptions) {
            prescriptionCountByFiche.put(prescription.getFiche_medicale_id(), prescriptionCountByFiche.getOrDefault(prescription.getFiche_medicale_id(), 0) + 1);
        }

        for (FicheMedicale ficheMedicale : fiches) {
            if (ficheMedicale == null || ficheMedicale.getRendez_vous_id() == null) {
                continue;
            }
            RendezVous rendezVous = findRendezVousById(rendezVousList, ficheMedicale.getRendez_vous_id());
            if (rendezVous != null) {
                ficheCountByDoctor.put(rendezVous.getIdStaff(), ficheCountByDoctor.getOrDefault(rendezVous.getIdStaff(), 0) + 1);
                prescriptionCountByDoctor.put(rendezVous.getIdStaff(), prescriptionCountByDoctor.getOrDefault(rendezVous.getIdStaff(), 0) + prescriptionCountByFiche.getOrDefault(ficheMedicale.getId(), 0));
            }
        }

        int urgentCount = 0;
        int completedCount = 0;
        int demandCount = 0;
        int presentialCount = 0;
        int remoteCount = 0;
        int fichesWithDuration = 0;
        int totalDuration = 0;

        Map<String, Integer> statusDistribution = new LinkedHashMap<>();
        Map<String, Integer> modeDistribution = new LinkedHashMap<>();
        Map<String, Integer> urgencyDistribution = new LinkedHashMap<>();
        Map<String, Integer> medicationDistribution = new LinkedHashMap<>();
        Map<String, Integer> monthlyAppointments = new LinkedHashMap<>();
        Map<String, Integer> doctorLoad = new LinkedHashMap<>();

        YearMonth now = YearMonth.now();
        List<YearMonth> window = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            window.add(now.minusMonths(i));
        }
        for (YearMonth ym : window) {
            monthlyAppointments.put(ym.format(monthFormatter), 0);
        }

        for (RendezVous rendezVous : rendezVousList) {
            if (rendezVous == null) {
                continue;
            }

            String status = valueOrDash(rendezVous.getStatut());
            String mode = valueOrDash(rendezVous.getMode());
            String urgency = valueOrDash(rendezVous.getUrgency_level());

            statusDistribution.put(status, statusDistribution.getOrDefault(status, 0) + 1);
            modeDistribution.put(mode, modeDistribution.getOrDefault(mode, 0) + 1);
            urgencyDistribution.put(urgency, urgencyDistribution.getOrDefault(urgency, 0) + 1);

            if (containsIgnoreCase(status, "term")) {
                completedCount++;
            }
            if (containsIgnoreCase(status, "demand") || containsIgnoreCase(status, "pending")) {
                demandCount++;
            }
            if (containsIgnoreCase(urgency, "urgent") || containsIgnoreCase(urgency, "high")) {
                urgentCount++;
            }
            if (containsIgnoreCase(mode, "pres") || containsIgnoreCase(mode, "onsite")) {
                presentialCount++;
            } else {
                remoteCount++;
            }

            Timestamp timestamp = rendezVous.getCreated_at() != null ? rendezVous.getCreated_at() : rendezVous.getDatetime();
            if (timestamp != null) {
                YearMonth ym = YearMonth.from(timestamp.toLocalDateTime());
                String bucket = ym.format(monthFormatter);
                if (monthlyAppointments.containsKey(bucket)) {
                    monthlyAppointments.put(bucket, monthlyAppointments.get(bucket) + 1);
                }
            }

            String doctorName = rendezVousToDoctorName.getOrDefault(rendezVous.getId(), "Staff #" + rendezVous.getIdStaff());
            doctorLoad.put(doctorName, doctorLoad.getOrDefault(doctorName, 0) + 1);
        }

        for (FicheMedicale ficheMedicale : fiches) {
            if (ficheMedicale == null) {
                continue;
            }
            if (ficheMedicale.getDuree_minutes() != null) {
                fichesWithDuration++;
                totalDuration += ficheMedicale.getDuree_minutes();
            }
        }

        for (Prescription prescription : prescriptions) {
            if (prescription == null) {
                continue;
            }
            String medication = valueOrDash(prescription.getNom_medicament());
            medicationDistribution.put(medication, medicationDistribution.getOrDefault(medication, 0) + 1);
        }

        updateLabels(doctors.size(), rendezVousList.size(), fiches.size(), prescriptions.size(), urgentCount, averageDuration(fichesWithDuration, totalDuration), ficheCoverage(fiches.size(), rendezVousList.size()), prescriptionCoverage(prescriptions.size(), fiches.size()));

        if (consultationInsightLabel != null) {
            consultationInsightLabel.setText(buildInsightText(statusDistribution, modeDistribution, urgentCount, doctorLoad));
        }

        if (topDoctorLabel != null) {
            topDoctorLabel.setText(determineTopDoctor(doctorLoad));
        }

        if (busiestMonthLabel != null) {
            busiestMonthLabel.setText(determineBusiestMonth(monthlyAppointments));
        }

        fillLineChart(monthlyAppointmentsChart, monthlyAppointments);
        fillPieChart(statusPieChart, keepTopEntries(statusDistribution, 6));
        fillPieChart(modePieChart, modeDistribution);
        fillPieChart(medicinePieChart, keepTopEntries(medicationDistribution, 7));
        fillBarChart(urgencyBarChart, orderByValue(urgencyDistribution));
        fillBarChart(doctorLoadBarChart, keepTopEntries(doctorLoad, 8));
    }

    private void updateLabels(int doctors, int rendezVousCount, int fichesCount, int prescriptionsCount, int urgentCount, String avgDuration, String ficheCoverage, String prescriptionCoverage) {
        if (totalDoctorsLabel != null) {
            totalDoctorsLabel.setText(String.valueOf(doctors));
        }
        if (totalRendezVousLabel != null) {
            totalRendezVousLabel.setText(String.valueOf(rendezVousCount));
        }
        if (totalFichesLabel != null) {
            totalFichesLabel.setText(String.valueOf(fichesCount));
        }
        if (totalPrescriptionsLabel != null) {
            totalPrescriptionsLabel.setText(String.valueOf(prescriptionsCount));
        }
        if (averageDurationLabel != null) {
            averageDurationLabel.setText(avgDuration);
        }
        if (urgentLabel != null) {
            urgentLabel.setText(String.valueOf(urgentCount));
        }
        if (ficheCoverageLabel != null) {
            ficheCoverageLabel.setText(ficheCoverage);
        }
        if (prescriptionCoverageLabel != null) {
            prescriptionCoverageLabel.setText(prescriptionCoverage);
        }
    }

    private String averageDuration(int count, int totalDuration) {
        if (count <= 0) {
            return "-";
        }
        double average = totalDuration / (double) count;
        return String.format(Locale.US, "%.1f min", average);
    }

    private String ficheCoverage(int fiches, int rendezVous) {
        if (rendezVous <= 0) {
            return "0% couverture";
        }
        return String.format(Locale.US, "%.1f%% couverture", (fiches * 100.0) / rendezVous);
    }

    private String prescriptionCoverage(int prescriptions, int fiches) {
        if (fiches <= 0) {
            return "0% couverture";
        }
        return String.format(Locale.US, "%.1f%% prescription", (prescriptions * 100.0) / fiches);
    }

    private String buildInsightText(Map<String, Integer> statusDistribution, Map<String, Integer> modeDistribution, int urgentCount, Map<String, Integer> doctorLoad) {
        String busiestStatus = pickTopKey(statusDistribution);
        String dominantMode = pickTopKey(modeDistribution);
        String busiestDoctor = pickTopKey(doctorLoad);
        return "Le statut dominant est " + busiestStatus + ", la modalité la plus utilisée est " + dominantMode + ", et le docteur le plus sollicité est " + busiestDoctor + ". "
                + "Les rendez-vous urgents représentent " + urgentCount + " cas surveillés par le tableau de bord.";
    }

    private String determineTopDoctor(Map<String, Integer> doctorLoad) {
        return "Docteur le plus sollicité: " + pickTopKey(doctorLoad);
    }

    private String determineBusiestMonth(Map<String, Integer> monthlyAppointments) {
        return "Mois le plus actif: " + pickTopKey(monthlyAppointments);
    }

    private String pickTopKey(Map<String, Integer> map) {
        return map.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");
    }

    private void fillLineChart(LineChart<String, Number> chart, Map<String, Integer> values) {
        if (chart == null) {
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Rendez-vous créés");
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chart.getData().clear();
        chart.getData().add(series);

        Platform.runLater(() -> {
            if (!series.getData().isEmpty()) {
                series.getNode().setStyle("-fx-stroke: linear-gradient(to right, #0ea5e9, #14b8a6); -fx-stroke-width: 3px;");
            }
            applyChartTextStyling(chart);
        });
    }

    private void fillBarChart(BarChart<String, Number> chart, Map<String, Integer> values) {
        if (chart == null) {
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chart.getData().clear();
        chart.getData().add(series);

        Platform.runLater(() -> {
            applyBarStyling(series, chart);
            applyChartTextStyling(chart);
        });
    }

    private void applyBarStyling(XYChart.Series<String, Number> series, BarChart<String, Number> chart) {
        int index = 0;
        for (XYChart.Data<String, Number> data : series.getData()) {
            Node bar = data.getNode();
            if (bar != null) {
                String style = switch (index % 4) {
                    case 0 -> "-fx-bar-fill: #0ea5e9;";
                    case 1 -> "-fx-bar-fill: #14b8a6;";
                    case 2 -> "-fx-bar-fill: #fb7185;";
                    default -> "-fx-bar-fill: #f59e0b;";
                };
                bar.setStyle(style + " -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.18), 10, 0, 0, 3);");
            }
            index++;
        }
    }

    private void fillPieChart(PieChart chart, Map<String, Integer> values) {
        if (chart == null) {
            return;
        }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        if (values.isEmpty()) {
            data.add(new PieChart.Data("Aucune donnée", 1));
        } else {
            for (Map.Entry<String, Integer> entry : values.entrySet()) {
                data.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
        }
        chart.setData(data);
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);

        Platform.runLater(() -> {
            applyPieStyling(chart);
            applyChartTextStyling(chart);
        });
    }

    private void applyPieStyling(PieChart chart) {
        List<String> colors = List.of("#0ea5e9", "#14b8a6", "#8b5cf6", "#f59e0b", "#fb7185", "#22c55e", "#64748b", "#2563eb");
        int index = 0;
        for (PieChart.Data data : chart.getData()) {
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-pie-color: " + colors.get(index % colors.size()) + "; -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.16), 10, 0, 0, 3);");
            }
            index++;
        }
    }

    private void applyChartTextStyling(Node chart) {
        if (chart == null) {
            return;
        }

        applyChartTextStylingRecursively(chart);
    }

    private void applyChartTextStylingRecursively(Node node) {
        if (node instanceof Text text) {
            text.setFill(Color.web(CHART_TEXT_COLOR));
            addChartTextStyle(text);
        } else if (node instanceof Label label) {
            label.setTextFill(Color.web(CHART_TEXT_COLOR));
            addChartTextStyle(label);
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyChartTextStylingRecursively(child);
            }
        }
    }

    private void addChartTextStyle(Node node) {
        String currentStyle = node.getStyle();
        if (currentStyle != null && currentStyle.contains(CHART_TEXT_COLOR)) {
            return;
        }

        String prefix = currentStyle == null || currentStyle.isBlank() ? "" : currentStyle + "; ";
        node.setStyle(prefix + "-fx-text-fill: " + CHART_TEXT_COLOR + "; -fx-fill: " + CHART_TEXT_COLOR + ";");
    }

    private Map<String, Integer> keepTopEntries(Map<String, Integer> input, int limit) {
        return input.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);
    }

    private Map<String, Integer> orderByValue(Map<String, Integer> input) {
        return input.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);
    }

    private RendezVous findRendezVousById(List<RendezVous> rendezVousList, int rendezVousId) {
        for (RendezVous rendezVous : rendezVousList) {
            if (rendezVous != null && rendezVous.getId() == rendezVousId) {
                return rendezVous;
            }
        }
        return null;
    }

    private String buildDoctorDisplayName(User doctor) {
        String prenom = doctor.getPrenom() == null ? "" : doctor.getPrenom().trim();
        String nom = doctor.getNom() == null ? "" : doctor.getNom().trim();
        String fullName = (prenom + " " + nom).trim();
        if (fullName.isBlank()) {
            return "Staff #" + doctor.getId();
        }
        return "Dr. " + fullName;
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private boolean containsIgnoreCase(String value, String fragment) {
        return value != null && fragment != null && value.toLowerCase(Locale.ROOT).contains(fragment.toLowerCase(Locale.ROOT));
    }

    private void animateVisibleSections() {
        if (rootPane == null) {
            return;
        }

        for (Node node : rootPane.lookupAll(".consultation-stat-panel")) {
            node.setOpacity(0);
            node.setTranslateY(18);
            node.setOpacity(1);
            node.setTranslateY(0);
        }
    }

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
        goTo("/FXML/ConsultationsParDocteur.fxml");
    }

    @FXML
    private void openStatsConsultations() {
        loadDashboard();
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
        goTo("/FXML/StatsReclamations.fxml");
    }

    @FXML
    private void openPosts() {
        goTo("/FXML/PostsAdmin.fxml");
    }

    @FXML
    private void openBlogComments() {
        goTo("/FXML/CommentairesAdmin.fxml");
    }

    @FXML
    private void openBlogModeration() {
        goTo("/FXML/ModerationBlog.fxml");
    }

    @FXML
    private void openPostsEnAttente() {
        goTo("/FXML/PostsEnAttente.fxml");
    }

    @FXML
    private void openStatsBlog() {
        goTo("/FXML/StatsBlog.fxml");
    }

    @FXML
    private void openNewProduct() {
        goTo("/FXML/AjoutProduit.fxml");
    }

    @FXML
    private void openNewEvent() {
        goTo("/FXML/AjoutEvenement.fxml");
    }

    @FXML
    private void openNewUser() {
        goTo("/FXML/AjoutUtilisateur.fxml");
    }

    @FXML
    private void openReports() {
        goTo("/FXML/DashboardStatsAdmin.fxml");
    }

    @FXML
    private void goBackSite() {
        goTo("/FXML/Home.fxml");
    }

    @FXML
    private void logout() {
        goTo("/FXML/Login.fxml");
    }

    private void goTo(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                showWarning("Page introuvable : " + fxmlPath);
                return;
            }

            Parent page = FXMLLoader.load(url);
            if (rootPane != null && rootPane.getScene() != null) {
                rootPane.getScene().setRoot(page);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showWarning("Erreur lors de l'ouverture de la page : " + fxmlPath);
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Navigation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
