package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.services.CommandeService;
import tn.esprit.services.ProduitService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DetectionEpidemieController {

    @FXML private BorderPane rootPane;
    @FXML private Label dateLabel;
    @FXML private TextField topSearchField;

    @FXML private Label riskLevelLabel;
    @FXML private ProgressBar riskProgress;
    @FXML private Label scoreLabel;
    @FXML private Label diseasesCountLabel;
    @FXML private Label signalCountLabel;
    @FXML private Label weatherRiskLabel;
    @FXML private Label weatherSmallLabel;

    @FXML private Label weatherTempLabel;
    @FXML private Label weatherDescriptionLabel;
    @FXML private Label weatherDetailsLabel;
    @FXML private Label weatherAdviceLabel;

    @FXML private Label yearLabel;
    @FXML private VBox diseaseSignalsContainer;
    @FXML private StackPane diseaseChartContainer;
    @FXML private StackPane riskPieContainer;
    @FXML private TextArea geminiAnalysisText;

    private final CommandeService commandeService = new CommandeService();
    private final ProduitService produitService = new ProduitService();

    private final List<Commande> commandes = new ArrayList<>();
    private final List<Produit> produits = new ArrayList<>();
    private final List<DiseaseSignal> signals = new ArrayList<>();

    private WeatherData currentWeather = new WeatherData();
    private static final String GEMINI_API_KEY =
            System.getenv("GEMINI_API_KEY_EPIDEMIE");

    private static final String GEMINI_MODEL =
            cleanGeminiModel(System.getenv().getOrDefault("GEMINI_MODEL_EPIDEMIE", "gemini-2.5-flash-lite"));

    private static String cleanGeminiModel(String model) {
        if (model == null || model.isBlank()) {
            return "gemini-2.5-flash-lite";
        }

        model = model.trim();

        if (model.contains("/models/")) {
            model = model.substring(model.indexOf("/models/") + "/models/".length());
        }

        if (model.contains(":generateContent")) {
            model = model.substring(0, model.indexOf(":generateContent"));
        }

        if (model.contains("?key=")) {
            model = model.substring(0, model.indexOf("?key="));
        }

        return model.trim();
    }
    private static final String OPENWEATHER_API_KEY = System.getenv("OPENWEATHER_API_KEY_EPIDEMIE");
    private static final String OPENWEATHER_CITY = System.getenv().getOrDefault("OPENWEATHER_CITY_EPIDEMIE", "Tunis,TN");

    @FXML
    public void initialize() {
        if (dateLabel != null) {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            dateLabel.setText("Admin • Détection Épidémie • " + date);
        }

        if (yearLabel != null) {
            yearLabel.setText(String.valueOf(LocalDate.now().getYear()));
        }

        Platform.runLater(this::loadDashboard);
    }

    @FXML
    private void refreshDashboard() {
        loadDashboard();
    }

    private void loadDashboard() {
        try {
            commandes.clear();
            produits.clear();

            commandes.addAll(commandeService.recuperer());
            produits.addAll(produitService.recuperer());

            buildSignals();
            updateKpis();
            renderSignals();
            buildDiseaseChart();
            buildRiskPie();

            loadWeatherAsync();

        } catch (Exception e) {
            e.printStackTrace();
            showWarning("Erreur", "Impossible de charger la détection épidémie : " + e.getMessage());
        }
    }

    private void buildSignals() {
        signals.clear();

        signals.add(new DiseaseSignal("Grippe / Rhume", "GR", List.of("grippe", "rhume", "fièvre", "fievre", "paracetamol", "doliprane", "efferalgan", "sirop", "vitamine", "covid")));
        signals.add(new DiseaseSignal("Infections respiratoires", "IR", List.of("respiratoire", "toux", "bronchite", "asthme", "inhalateur", "ventoline", "antibiotique")));
        signals.add(new DiseaseSignal("Infections cutanées", "IC", List.of("peau", "cutané", "cutanee", "eczema", "pommade", "crème", "creme", "antiseptique", "brulure")));
        signals.add(new DiseaseSignal("Troubles digestifs", "TD", List.of("digestif", "diarrhée", "diarrhee", "vomissement", "nausée", "estomac", "spasfon", "charbon")));
        signals.add(new DiseaseSignal("Douleur / Inflammation", "DI", List.of("douleur", "inflammation", "ibuprofene", "ibuprofène", "aspirin", "aspirine", "tramadol", "anti-inflammatoire")));
        signals.add(new DiseaseSignal("Allergies", "AL", List.of("allergie", "allergique", "antihistaminique", "cetirizine", "loratadine", "rhinite")));
        signals.add(new DiseaseSignal("Infections urinaires", "IU", List.of("urinaire", "cystite", "infection urinaire", "antibiotique")));
        for (DiseaseSignal signal : signals) {
            int current7 = countSalesForSignal(signal, 7, 0);
            int previous28 = countSalesForSignal(signal, 28, 7);
            double baseline = previous28 / 4.0;

            signal.currentSales = current7;
            signal.baseline = baseline;

            if (baseline <= 0 && current7 > 0) {
                signal.changePercent = 100;
            } else if (baseline <= 0) {
                signal.changePercent = 0;
            } else {
                signal.changePercent = ((current7 - baseline) / baseline) * 100.0;
            }

            if (signal.changePercent >= 120 && current7 >= 3) {
                signal.status = "Élevé";
            } else if (signal.changePercent >= 60 && current7 >= 2) {
                signal.status = "Moyen";
            } else {
                signal.status = "Normal";
            }
        }
    }

    private int countSalesForSignal(DiseaseSignal signal, int daysWindow, int daysOffset) {
        int total = 0;

        LocalDate today = LocalDate.now();
        LocalDate endExclusive = today.minusDays(daysOffset);
        LocalDate startInclusive = endExclusive.minusDays(daysWindow);

        for (Commande c : commandes) {
            LocalDate date = extractLocalDate(c);
            if (date == null) continue;

            if (date.isBefore(startInclusive) || !date.isBefore(endExclusive.plusDays(1))) {
                continue;
            }

            List<CommandeProduit> lignes = c.getCommande_produits();
            if (lignes == null) continue;

            for (CommandeProduit cp : lignes) {
                Produit p = cp.getProduit();
                if (p == null) continue;

                if (matchesSignal(p, signal)) {
                    total += Math.max(0, cp.getQuantite_commandee());
                }
            }
        }

        return total;
    }

    private boolean matchesSignal(Produit p, DiseaseSignal signal) {
        String text = (
                safe(p.getNom_produit()) + " " +
                        safe(p.getCategorie_produit()) + " " +
                        safe(p.getDescription_produit())
        ).toLowerCase(Locale.ROOT);

        for (String keyword : signal.keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private void updateKpis() {
        int score = 0;
        int strongSignals = 0;

        for (DiseaseSignal signal : signals) {
            if ("Élevé".equals(signal.status)) {
                score += 3;
                strongSignals++;
            } else if ("Moyen".equals(signal.status)) {
                score += 2;
            } else {
                score += 0;
            }
        }

        int weatherScore = computeWeatherScore();
        score += weatherScore;

        String level;
        if (score >= 14) {
            level = "Élevé";
        } else if (score >= 7) {
            level = "Moyen";
        } else {
            level = "Faible";
        }

        if (riskLevelLabel != null) {
            riskLevelLabel.setText(level);
            riskLevelLabel.getStyleClass().removeAll("risk-low", "risk-medium", "risk-high");

            if ("Élevé".equals(level)) {
                riskLevelLabel.getStyleClass().add("risk-high");
            } else if ("Moyen".equals(level)) {
                riskLevelLabel.getStyleClass().add("risk-medium");
            } else {
                riskLevelLabel.getStyleClass().add("risk-low");
            }
        }

        if (riskProgress != null) {
            riskProgress.setProgress(Math.min(1.0, score / 21.0));
        }

        if (scoreLabel != null) {
            scoreLabel.setText("Score " + score + "/21");
        }

        if (diseasesCountLabel != null) {
            diseasesCountLabel.setText(String.valueOf(signals.size()));
        }

        if (signalCountLabel != null) {
            signalCountLabel.setText(String.valueOf(strongSignals));
        }
    }

    private int computeWeatherScore() {
        int score = 0;

        if (currentWeather.tempC <= 10 || currentWeather.tempC >= 32) {
            score += 2;
        }

        if (currentWeather.humidity >= 75) {
            score += 2;
        }

        if (currentWeather.description.toLowerCase(Locale.ROOT).contains("pluie")) {
            score += 1;
        }

        return score;
    }

    private void renderSignals() {
        if (diseaseSignalsContainer == null) return;

        diseaseSignalsContainer.getChildren().clear();

        for (DiseaseSignal signal : signals) {
            diseaseSignalsContainer.getChildren().add(createSignalRow(signal));
        }
    }

    private HBox createSignalRow(DiseaseSignal signal) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("signal-row");

        if ("Élevé".equals(signal.status)) {
            row.getStyleClass().add("signal-high");
        } else if ("Moyen".equals(signal.status)) {
            row.getStyleClass().add("signal-medium");
        } else {
            row.getStyleClass().add("signal-normal");
        }

        Label emoji = new Label(signal.emoji);
        emoji.getStyleClass().add("signal-emoji");

        VBox textBox = new VBox(4);
        textBox.setPrefWidth(300);
        textBox.setMinWidth(300);
        textBox.setMaxWidth(300);
        Label title = new Label(signal.name);
        title.getStyleClass().add("signal-title");

        Label subtitle = new Label(signal.currentSales + " unités vendues • baseline " + String.format(Locale.FRANCE, "%.1f", signal.baseline));
        subtitle.getStyleClass().add("signal-subtitle");

        textBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox ratioBox = new VBox(3);
        ratioBox.setAlignment(Pos.CENTER_RIGHT);

        String arrow = signal.changePercent > 0 ? "↑" : signal.changePercent < 0 ? "↓" : "→";
        Label change = new Label(arrow + " " + String.format(Locale.FRANCE, "%.0f", Math.abs(signal.changePercent)) + "%");
        change.getStyleClass().add(signal.changePercent >= 60 ? "change-danger" : "change-normal");

        Label ratio = new Label("ratio " + String.format(Locale.FRANCE, "%.1f", signal.baseline <= 0 ? 0 : signal.currentSales / signal.baseline));
        ratio.getStyleClass().add("ratio-label");

        ratioBox.getChildren().addAll(change, ratio);

        Label status = new Label(signal.status);
        if ("Élevé".equals(signal.status)) {
            status.getStyleClass().add("status-high");
        } else if ("Moyen".equals(signal.status)) {
            status.getStyleClass().add("status-medium");
        } else {
            status.getStyleClass().add("status-normal");
        }

        row.getChildren().addAll(emoji, textBox, spacer, ratioBox, status);

        return row;
    }

    private void buildDiseaseChart() {
        if (diseaseChartContainer == null) return;

        diseaseChartContainer.getChildren().clear();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(true);
        chart.setCreateSymbols(true);
        chart.setAnimated(true);
        chart.getStyleClass().add("clean-chart");

        YearMonth start = YearMonth.now().minusMonths(5);

        for (DiseaseSignal signal : signals) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(signal.name);

            for (int i = 0; i < 6; i++) {
                YearMonth ym = start.plusMonths(i);
                int count = countSalesForSignalByMonth(signal, ym);
                series.getData().add(new XYChart.Data<>(ym.getMonth().toString().substring(0, 3) + " " + ym.getYear(), count));
            }

            chart.getData().add(series);
        }

        diseaseChartContainer.getChildren().add(chart);
    }

    private int countSalesForSignalByMonth(DiseaseSignal signal, YearMonth ym) {
        int total = 0;

        for (Commande c : commandes) {
            LocalDate date = extractLocalDate(c);
            if (date == null || !YearMonth.from(date).equals(ym)) continue;

            List<CommandeProduit> lignes = c.getCommande_produits();
            if (lignes == null) continue;

            for (CommandeProduit cp : lignes) {
                Produit p = cp.getProduit();
                if (p != null && matchesSignal(p, signal)) {
                    total += Math.max(0, cp.getQuantite_commandee());
                }
            }
        }

        return total;
    }

    private void buildRiskPie() {
        if (riskPieContainer == null) return;

        riskPieContainer.getChildren().clear();

        int normal = 0;
        int moyen = 0;
        int eleve = 0;

        for (DiseaseSignal s : signals) {
            if ("Élevé".equals(s.status)) {
                eleve++;
            } else if ("Moyen".equals(s.status)) {
                moyen++;
            } else {
                normal++;
            }
        }

        PieChart pie = new PieChart();
        pie.setLegendVisible(true);
        pie.setLabelsVisible(false);
        pie.setAnimated(true);
        pie.getStyleClass().add("risk-pie");

        if (normal > 0) {
            pie.getData().add(new PieChart.Data("Normal : " + normal, normal));
        }

        if (moyen > 0) {
            pie.getData().add(new PieChart.Data("Moyen : " + moyen, moyen));
        }

        if (eleve > 0) {
            pie.getData().add(new PieChart.Data("Élevé : " + eleve, eleve));
        }

        if (pie.getData().isEmpty()) {
            pie.getData().add(new PieChart.Data("Aucun signal", 1));
        }

        riskPieContainer.getChildren().add(pie);
    }
    private void loadWeatherAsync() {
        new Thread(() -> {
            WeatherData weather = fetchWeatherFromOpenWeather();

            Platform.runLater(() -> {
                currentWeather = weather;
                updateWeatherUI();
                updateKpis();
            });
        }).start();
    }

    private WeatherData fetchWeatherFromOpenWeather() {
        WeatherData data = new WeatherData();

        if (OPENWEATHER_API_KEY == null || OPENWEATHER_API_KEY.isBlank()) {
            data.description = "OpenWeather non configuré";
            data.error = true;
            return data;
        }

        try {
            String cityEncoded = URLEncoder.encode(OPENWEATHER_CITY, StandardCharsets.UTF_8);
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cityEncoded
                    + "&appid=" + OPENWEATHER_API_KEY
                    + "&units=metric&lang=fr";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();

            data.tempC = extractDouble(json, "\"temp\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)", 0);
            data.humidity = (int) extractDouble(json, "\"humidity\"\\s*:\\s*(\\d+)", 0);
            data.description = extractString(json, "\"description\"\\s*:\\s*\"([^\"]+)\"", "Météo chargée");
            data.error = false;

        } catch (Exception e) {
            data.description = "Erreur météo";
            data.error = true;
        }

        return data;
    }

    private void updateWeatherUI() {
        if (weatherTempLabel != null) {
            weatherTempLabel.setText(String.format(Locale.FRANCE, "%.2f°C", currentWeather.tempC));
        }

        if (weatherDescriptionLabel != null) {
            weatherDescriptionLabel.setText(currentWeather.description);
        }

        if (weatherDetailsLabel != null) {
            weatherDetailsLabel.setText("Humidité : " + currentWeather.humidity + "% • " + OPENWEATHER_CITY);
        }

        String risk = computeWeatherScore() >= 3 ? "Moyen" : "Faible";

        if (weatherRiskLabel != null) {
            weatherRiskLabel.setText(risk);
        }

        if (weatherSmallLabel != null) {
            weatherSmallLabel.setText(String.format(Locale.FRANCE, "%.2f°C — %d%% humidité", currentWeather.tempC, currentWeather.humidity));
        }

        if (weatherAdviceLabel != null) {
            if (currentWeather.error) {
                weatherAdviceLabel.setText("Ajoutez OPENWEATHER_API_KEY_EPIDEMIE dans IntelliJ pour activer la météo.");
            } else if (computeWeatherScore() >= 3) {
                weatherAdviceLabel.setText("La météo peut favoriser certains symptômes saisonniers. Surveillez grippe, allergies et infections respiratoires.");
            } else {
                weatherAdviceLabel.setText("La météo ne montre pas un risque fort actuellement. Continuez à surveiller les ventes des produits sensibles.");
            }
        }
    }

    @FXML
    private void runGeminiAnalysis() {
        if (geminiAnalysisText != null) {
            geminiAnalysisText.setText("Analyse Gemini en cours...");
        }

        new Thread(() -> {
            String result = callGemini();

            Platform.runLater(() -> {
                if (geminiAnalysisText != null) {
                    geminiAnalysisText.setText(result);
                }
            });
        }).start();
    }

    private String callGemini() {
        if (GEMINI_API_KEY == null || GEMINI_API_KEY.isBlank()) {
            return "Gemini non configuré.\n\nAjoutez dans IntelliJ :\n"
                    + "GEMINI_API_KEY_EPIDEMIE=ta_clé\n"
                    + "GEMINI_MODEL_EPIDEMIE=gemini-2.5-flash-lite";
        }

        String result = callGeminiWithModel(GEMINI_MODEL);

        if (result.startsWith("Erreur Gemini HTTP 404") && !"gemini-2.5-flash".equals(GEMINI_MODEL)) {
            return callGeminiWithModel("gemini-2.5-flash");
        }

        return result;
    }

    private String callGeminiWithModel(String modelName) {
        try {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + modelName
                    + ":generateContent?key="
                    + GEMINI_API_KEY;

            System.out.println("Gemini model utilisé = " + modelName);
            System.out.println("Gemini endpoint = https://generativelanguage.googleapis.com/v1beta/models/"
                    + modelName + ":generateContent?key=HIDDEN");

            String prompt = buildGeminiPrompt();

            String jsonBody = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "%s"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(escapeJson(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "Erreur Gemini HTTP " + response.statusCode()
                        + "\nModèle utilisé : " + modelName
                        + "\n\n" + response.body();
            }

            String text = extractGeminiText(response.body());

            if (text.isBlank()) {
                return "Gemini a répondu, mais le texte n'a pas été trouvé.\n\nRéponse brute :\n" + response.body();
            }

            return text;

        } catch (Exception e) {
            return "Erreur Gemini : " + e.getMessage();
        }
    }
    private String buildGeminiPrompt() {
        StringBuilder sb = new StringBuilder();

        sb.append("Tu es un assistant médical administratif pour une pharmacie.\n");
        sb.append("Analyse le risque épidémique local à partir des ventes et de la météo.\n");
        sb.append("Réponds en français simple, clair et court.\n\n");

        sb.append("Météo OpenWeather :\n");
        sb.append("- Ville : ").append(OPENWEATHER_CITY).append("\n");
        sb.append("- Température : ").append(String.format(Locale.FRANCE, "%.2f", currentWeather.tempC)).append("°C\n");
        sb.append("- Humidité : ").append(currentWeather.humidity).append("%\n");
        sb.append("- Description : ").append(currentWeather.description).append("\n\n");

        sb.append("Signaux ventes 7 derniers jours :\n");
        for (DiseaseSignal s : signals) {
            sb.append("- ").append(s.name)
                    .append(" : ventes=").append(s.currentSales)
                    .append(", baseline=").append(String.format(Locale.FRANCE, "%.1f", s.baseline))
                    .append(", changement=").append(String.format(Locale.FRANCE, "%.0f", s.changePercent)).append("%")
                    .append(", statut=").append(s.status)
                    .append("\n");
        }

        sb.append("\nDonne exactement ces sections :\n");
        sb.append("1) Risque global : Faible/Moyen/Élevé\n");
        sb.append("2) Maladie la plus probable\n");
        sb.append("3) Explication courte\n");
        sb.append("4) Conseils admin\n");
        sb.append("5) Produits à surveiller\n");

        return sb.toString();
    }

    private String extractGeminiText(String json) {
        Matcher matcher = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);

        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }

        return "";
    }

    private LocalDate extractLocalDate(Commande c) {
        try {
            String raw = String.valueOf(c.getDate_creation_commande());

            if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("null")) {
                return null;
            }

            raw = raw.replace("T", " ");

            if (raw.length() >= 10) {
                return LocalDate.parse(raw.substring(0, 10));
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private double extractDouble(String text, String regex, double defaultValue) {
        try {
            Matcher matcher = Pattern.compile(regex).matcher(text);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception ignored) {
        }

        return defaultValue;
    }

    private String extractString(String text, String regex, String defaultValue) {
        try {
            Matcher matcher = Pattern.compile(regex).matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }

        return defaultValue;
    }

    private String getProductIdText(Produit p) {
        String[] methods = {"getId_produit", "getIdProduit", "getId", "getReference_produit"};

        for (String methodName : methods) {
            try {
                Method m = p.getClass().getMethod(methodName);
                Object value = m.invoke(p);

                if (value != null) {
                    return "#" + value;
                }
            } catch (Exception ignored) {
            }
        }

        return "—";
    }

    private String escapeJson(String s) {
        if (s == null) return "";

        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private String unescapeJson(String s) {
        if (s == null) return "";

        return s
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @FXML private void openDashboard() { goTo("/AdminWelcome.fxml"); }
    @FXML private void openProduits() { goTo("/ProduitAdmin.fxml"); }
    @FXML private void openCommandes() { goTo("/CommandeAdmin.fxml"); }
    @FXML private void openDetection() { loadDashboard(); }
    @FXML private void openStatsProduits() { goTo("/StatProduit.fxml"); }
    @FXML private void openPatients() { goTo("/PatientsAdmin.fxml"); }
    @FXML private void openUsers() { goTo("/UtilisateursAdmin.fxml"); }
    @FXML private void openRoles() { goTo("/DemandesRole.fxml"); }
    @FXML private void openEvents() { goTo("/EvenementsAdmin.fxml"); }
    @FXML private void openEventParticipants() { goTo("/ParticipantsEvenement.fxml"); }
    @FXML private void openRessources() { goTo("/RessourcesAdmin.fxml"); }
    @FXML private void openStatsEvents() { goTo("/StatsEvenements.fxml"); }
    @FXML private void openConsultations() { goTo("/ConsultationsParDocteur.fxml"); }
    @FXML private void openStatsConsultations() { goTo("/StatsConsultations.fxml"); }
    @FXML private void openReclamations() { goTo("/ReclamationsAdmin.fxml"); }
    @FXML private void openStatsReclamations() { goTo("/StatsReclamations.fxml"); }
    @FXML private void openPosts() { goTo("/PostsAdmin.fxml"); }
    @FXML private void openBlogComments() { goTo("/CommentairesAdmin.fxml"); }
    @FXML private void openBlogModeration() { goTo("/ModerationBlog.fxml"); }
    @FXML private void goBackSite() { goTo("/FrontFXML/Accueil.fxml"); }
    @FXML private void logout() { goTo("/FrontFXML/Login.fxml"); }

    private void goTo(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);

            if (url == null) {
                showWarning("Page introuvable", "Fichier introuvable : " + fxmlPath + "\nChange le chemin dans DetectionEpidemieController.");
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
            showWarning("Erreur navigation", "Impossible d'ouvrir : " + fxmlPath);
        }
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class DiseaseSignal {
        String name;
        String emoji;
        List<String> keywords;
        int currentSales;
        double baseline;
        double changePercent;
        String status = "Normal";

        DiseaseSignal(String name, String emoji, List<String> keywords) {
            this.name = name;
            this.emoji = emoji;
            this.keywords = keywords;
        }
    }

    private static class WeatherData {
        double tempC = 0;
        int humidity = 0;
        String description = "Météo en attente";
        boolean error = false;
    }
}