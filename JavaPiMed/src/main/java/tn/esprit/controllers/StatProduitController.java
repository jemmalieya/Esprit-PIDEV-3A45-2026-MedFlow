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
import tn.esprit.entities.User;
import tn.esprit.services.CommandeService;
import tn.esprit.services.ProduitService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StatProduitController {

    @FXML private BorderPane rootPane;
    @FXML private Label dateLabel;
    @FXML private TextField topSearchField;

    @FXML private Label usersCountLabel;
    @FXML private Label productsCountLabel;
    @FXML private Label ordersCountLabel;
    @FXML private Label revenueLabel;
    @FXML private Label revenueMonthLabel;
    @FXML private Label yearLabel;

    @FXML private StackPane caChartContainer;
    @FXML private StackPane statusChartContainer;
    @FXML private StackPane topProductsChartContainer;

    @FXML private VBox stockFaibleRowsContainer;
    @FXML private VBox latestCommandesRowsContainer;

    private final CommandeService commandeService = new CommandeService();
    private final ProduitService produitService = new ProduitService();

    private final List<Commande> commandes = new ArrayList<>();
    private final List<Produit> produits = new ArrayList<>();

    @FXML
    public void initialize() {
        if (dateLabel != null) {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            dateLabel.setText("Admin • Statistiques Produits • " + date);
        }

        if (yearLabel != null) {
            yearLabel.setText(String.valueOf(LocalDate.now().getYear()));
        }

        Platform.runLater(this::loadDashboard);
    }

    private void loadDashboard() {
        try {
            commandes.clear();
            produits.clear();

            commandes.addAll(commandeService.recuperer());
            produits.addAll(produitService.recuperer());

            updateKpis();
            buildCAChart();
            buildStatusChart();
            buildTopProductsChart();
            renderStockFaible();
            renderLatestCommandes();

        } catch (Exception e) {
            e.printStackTrace();
            showWarning("Erreur", "Impossible de charger les statistiques : " + e.getMessage());
        }
    }

    private void updateKpis() {
        int usersCount = countUsersReflect();
        int productsCount = produits.size();
        int ordersCount = commandes.size();

        double caTotal = 0;
        double caMois = 0;
        YearMonth currentMonth = YearMonth.now();

        for (Commande c : commandes) {
            if (isPayee(c)) {
                double montant = c.getMontant_total_cents() / 100.0;
                caTotal += montant;

                YearMonth ym = extractYearMonth(c);
                if (ym != null && ym.equals(currentMonth)) {
                    caMois += montant;
                }
            }
        }

        if (usersCountLabel != null) usersCountLabel.setText(String.valueOf(usersCount));
        if (productsCountLabel != null) productsCountLabel.setText(String.valueOf(productsCount));
        if (ordersCountLabel != null) ordersCountLabel.setText(String.valueOf(ordersCount));
        if (revenueLabel != null) revenueLabel.setText(formatMoney(caTotal));
        if (revenueMonthLabel != null) revenueMonthLabel.setText("Ce mois : " + formatMoney(caMois));
    }

    private void buildCAChart() {
        if (caChartContainer == null) return;

        caChartContainer.getChildren().clear();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setCreateSymbols(true);
        chart.getStyleClass().add("clean-chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        YearMonth start = YearMonth.now().minusMonths(11);
        Map<YearMonth, Double> caParMois = new LinkedHashMap<>();

        for (int i = 0; i < 12; i++) {
            caParMois.put(start.plusMonths(i), 0.0);
        }

        for (Commande c : commandes) {
            if (!isPayee(c)) continue;

            YearMonth ym = extractYearMonth(c);
            if (ym != null && caParMois.containsKey(ym)) {
                caParMois.put(ym, caParMois.get(ym) + (c.getMontant_total_cents() / 100.0));
            }
        }

        for (Map.Entry<YearMonth, Double> entry : caParMois.entrySet()) {
            String month = entry.getKey().getMonth().toString().substring(0, 3);
            series.getData().add(new XYChart.Data<>(month, entry.getValue()));
        }

        chart.getData().add(series);
        caChartContainer.getChildren().add(chart);
    }

    private void buildStatusChart() {
        if (statusChartContainer == null) return;

        statusChartContainer.getChildren().clear();

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("En attente", 0);
        counts.put("En cours", 0);
        counts.put("Livraison", 0);
        counts.put("Livrée", 0);
        counts.put("Annulée", 0);

        for (Commande c : commandes) {
            String statut = normalizeStatut(c.getStatut_commande()).toLowerCase(Locale.ROOT);

            if (statut.contains("attente")) {
                counts.put("En attente", counts.get("En attente") + 1);
            } else if (statut.contains("cours")) {
                counts.put("En cours", counts.get("En cours") + 1);
            } else if (statut.contains("livraison")) {
                counts.put("Livraison", counts.get("Livraison") + 1);
            } else if (statut.contains("termin") || statut.contains("final") || statut.contains("livrée")) {
                counts.put("Livrée", counts.get("Livrée") + 1);
            } else if (statut.contains("annul")) {
                counts.put("Annulée", counts.get("Annulée") + 1);
            } else {
                counts.put("En attente", counts.get("En attente") + 1);
            }
        }

        PieChart pie = new PieChart();
        pie.setLegendVisible(true);
        pie.setLabelsVisible(true);
        pie.setAnimated(true);
        pie.getStyleClass().add("status-pie");

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 0) {
                pie.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
        }

        if (pie.getData().isEmpty()) {
            pie.getData().add(new PieChart.Data("Aucune commande", 1));
        }

        statusChartContainer.getChildren().add(pie);
    }

    private void buildTopProductsChart() {
        if (topProductsChartContainer == null) return;

        topProductsChartContainer.getChildren().clear();

        Map<String, Integer> ventes = new HashMap<>();

        for (Commande c : commandes) {
            List<CommandeProduit> lignes = c.getCommande_produits();

            if (lignes == null) {
                continue;
            }

            for (CommandeProduit cp : lignes) {
                Produit p = cp.getProduit();
                String nom = p == null ? "Produit" : displayValue(p.getNom_produit());
                ventes.put(nom, ventes.getOrDefault(nom, 0) + cp.getQuantite_commandee());
            }
        }

        List<Map.Entry<String, Integer>> top = new ArrayList<>(ventes.entrySet());
        top.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.getStyleClass().add("clean-chart");

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        int limit = Math.min(5, top.size());

        for (int i = 0; i < limit; i++) {
            String name = shortText(top.get(i).getKey(), 12);
            series.getData().add(new XYChart.Data<>(name, top.get(i).getValue()));
        }

        if (limit == 0) {
            series.getData().add(new XYChart.Data<>("Aucune vente", 0));
        }

        chart.getData().add(series);
        topProductsChartContainer.getChildren().add(chart);
    }

    private void renderStockFaible() {
        if (stockFaibleRowsContainer == null) return;

        stockFaibleRowsContainer.getChildren().clear();

        List<Produit> stockFaible = new ArrayList<>();

        for (Produit p : produits) {
            if (p.getQuantite_produit() <= 5) {
                stockFaible.add(p);
            }
        }

        stockFaible.sort(Comparator.comparingInt(Produit::getQuantite_produit));

        int limit = Math.min(6, stockFaible.size());

        if (limit == 0) {
            Label empty = new Label("Aucun produit en stock faible.");
            empty.getStyleClass().add("empty-text");
            stockFaibleRowsContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < limit; i++) {
            stockFaibleRowsContainer.getChildren().add(createStockRow(stockFaible.get(i), i));
        }
    }

    private HBox createStockRow(Produit p, int index) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("stock-row");

        if (index % 2 == 0) {
            row.getStyleClass().add("row-light");
        }

        VBox produitBox = new VBox(3);
        produitBox.setPrefWidth(220);
        produitBox.setMinWidth(220);
        produitBox.setMaxWidth(220);

        Label nom = new Label(shortText(displayValue(p.getNom_produit()), 20));
        nom.getStyleClass().add("stock-product-name");

        Label id = new Label("ID: " + getProductIdText(p));
        id.getStyleClass().add("stock-product-id");

        produitBox.getChildren().addAll(nom, id);

        Label categorie = new Label(displayValue(p.getCategorie_produit()));
        categorie.setPrefWidth(130);
        categorie.getStyleClass().add("category-badge");

        Label stock = new Label(String.valueOf(p.getQuantite_produit()));
        stock.setPrefWidth(70);
        stock.getStyleClass().add(p.getQuantite_produit() <= 0 ? "stock-badge-red" : "stock-badge-orange");

        Label statut = new Label(displayValue(p.getStatus_produit()));
        statut.setPrefWidth(100);

        String s = displayValue(p.getStatus_produit()).toLowerCase(Locale.ROOT);
        statut.getStyleClass().add(s.contains("rupture") ? "status-rupture" : "status-dispo");

        row.getChildren().addAll(produitBox, categorie, stock, statut);

        return row;
    }

    private void renderLatestCommandes() {
        if (latestCommandesRowsContainer == null) return;

        latestCommandesRowsContainer.getChildren().clear();

        List<Commande> latest = new ArrayList<>(commandes);
        latest.sort((a, b) -> Integer.compare(b.getId_commande(), a.getId_commande()));

        int limit = Math.min(5, latest.size());

        if (limit == 0) {
            Label empty = new Label("Aucune commande récente.");
            empty.getStyleClass().add("empty-text");
            latestCommandesRowsContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < limit; i++) {
            latestCommandesRowsContainer.getChildren().add(createLatestCommandeRow(latest.get(i), i));
        }
    }

    private HBox createLatestCommandeRow(Commande c, int index) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("latest-row");

        if (index % 2 == 0) {
            row.getStyleClass().add("row-light");
        }

        Label id = new Label("#" + c.getId_commande());
        id.setPrefWidth(70);
        id.getStyleClass().add("latest-id");

        VBox clientBox = new VBox(3);
        clientBox.setPrefWidth(300);
        clientBox.setMinWidth(300);
        clientBox.setMaxWidth(300);

        User user = c.getUser();

        Label email = new Label(user == null ? "Client inconnu" : displayValue(user.getEmailUser()));
        email.setMaxWidth(285);
        email.setWrapText(false);
        email.getStyleClass().add("latest-client");

        Label userId = new Label(user == null ? "User ID: —" : "User ID: " + user.getId());
        userId.getStyleClass().add("latest-user-id");

        clientBox.getChildren().addAll(email, userId);

        String[] parts = splitDateTime(String.valueOf(c.getDate_creation_commande()));

        VBox dateBox = new VBox(3);
        dateBox.setPrefWidth(150);
        dateBox.setMinWidth(150);
        dateBox.setMaxWidth(150);

        Label date = new Label(parts[0]);
        date.getStyleClass().add("latest-date");

        Label time = new Label(parts[1]);
        time.getStyleClass().add("latest-time");

        dateBox.getChildren().addAll(date, time);

        Label montant = new Label(formatMoney(c.getMontant_total_cents() / 100.0));
        montant.setPrefWidth(140);
        montant.getStyleClass().add("latest-money");

        Label paiement = new Label(isPayee(c) ? "Payée" : "Non payée");
        paiement.setPrefWidth(150);
        paiement.getStyleClass().add(isPayee(c) ? "payment-paid" : "payment-unpaid");

        Label statut = new Label(normalizeStatut(c.getStatut_commande()));
        statut.setPrefWidth(130);
        statut.getStyleClass().addAll("latest-status", getStatusClass(c.getStatut_commande()));

        row.getChildren().addAll(id, clientBox, dateBox, montant, paiement, statut);

        return row;
    }

    private int countUsersReflect() {
        String[] classNames = {
                "tn.esprit.services.UserService",
                "tn.esprit.services.ServiceUser",
                "tn.esprit.services.UtilisateurService"
        };

        String[] methodNames = {
                "recuperer",
                "afficher",
                "getAll",
                "findAll"
        };

        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                Object service = clazz.getDeclaredConstructor().newInstance();

                for (String methodName : methodNames) {
                    try {
                        Method m = clazz.getMethod(methodName);
                        Object result = m.invoke(service);

                        if (result instanceof Collection<?>) {
                            return ((Collection<?>) result).size();
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return 0;
    }

    private boolean isPayee(Commande c) {
        String statut = safe(c.getStatut_commande()).toLowerCase(Locale.ROOT);

        if (statut.contains("pay")) {
            return true;
        }

        Object paidAt = readObjectReflect(c, "getPaid_at", "getPaidAt", "getDatePaiement", "getPaidAtCommande");

        return paidAt != null
                && !String.valueOf(paidAt).isBlank()
                && !"null".equalsIgnoreCase(String.valueOf(paidAt));
    }

    private Object readObjectReflect(Object target, String... methodNames) {
        if (target == null) return null;

        for (String methodName : methodNames) {
            try {
                Method m = target.getClass().getMethod(methodName);
                return m.invoke(target);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private YearMonth extractYearMonth(Commande c) {
        try {
            String raw = String.valueOf(c.getDate_creation_commande());

            if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("null")) {
                return null;
            }

            raw = raw.replace("T", " ");

            if (raw.length() >= 10) {
                LocalDate date = LocalDate.parse(raw.substring(0, 10));
                return YearMonth.from(date);
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private String normalizeStatut(String statut) {
        if (statut == null || statut.isBlank()) return "En attente";

        String s = statut.toLowerCase(Locale.ROOT);

        if (s.contains("pay")) return "Payée";
        if (s.contains("confirm")) return "Confirmée";
        if (s.contains("cours")) return "En cours";
        if (s.contains("attente")) return "En attente";
        if (s.contains("livraison")) return "Livraison";
        if (s.contains("final")) return "Finalisée";
        if (s.contains("termin")) return "Terminée";
        if (s.contains("annul")) return "Annulée";

        return statut;
    }

    private String getStatusClass(String statut) {
        String s = safe(statut).toLowerCase(Locale.ROOT);

        if (s.contains("pay") || s.contains("confirm")) return "status-confirmed";
        if (s.contains("cours")) return "status-progress";
        if (s.contains("attente")) return "status-pending";
        if (s.contains("livraison")) return "status-delivery";
        if (s.contains("final") || s.contains("termin")) return "status-final";
        if (s.contains("annul")) return "status-cancel";

        return "status-pending";
    }

    private String[] splitDateTime(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("null")) {
            return new String[]{"—", ""};
        }

        String text = raw.replace("T", " ");

        if (text.contains(" ")) {
            String[] parts = text.split(" ");
            String d = parts.length > 0 ? parts[0] : text;
            String t = parts.length > 1 ? parts[1] : "";

            if (t.length() >= 5) {
                t = t.substring(0, 5);
            }

            return new String[]{d, t};
        }

        return new String[]{text, ""};
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

    private String formatMoney(double value) {
        return String.format(Locale.FRANCE, "%.2f DT", value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String displayValue(String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return "—";
        }

        return value.trim();
    }

    private String shortText(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max) + "...";
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
    private void openCommandes() {
        goTo("/CommandeAdmin.fxml");
    }

    @FXML
    private void openStatsProduits() {
        loadDashboard();
    }

    @FXML
    private void openDetection() {
        goTo("/DetectionEpidemie.fxml");
    }

    @FXML
    private void openPatients() {
        goTo("/PatientsAdmin.fxml");
    }

    @FXML
    private void openUsers() {
        goTo("/UtilisateursAdmin.fxml");
    }

    @FXML
    private void openRoles() {
        goTo("/DemandesRole.fxml");
    }

    @FXML
    private void openEvents() {
        goTo("/EvenementsAdmin.fxml");
    }

    @FXML
    private void openEventParticipants() {
        goTo("/ParticipantsEvenement.fxml");
    }

    @FXML
    private void openRessources() {
        goTo("/RessourcesAdmin.fxml");
    }

    @FXML
    private void openStatsEvents() {
        goTo("/StatsEvenements.fxml");
    }

    @FXML
    private void openConsultations() {
        goTo("/ConsultationsParDocteur.fxml");
    }

    @FXML
    private void openStatsConsultations() {
        goTo("/StatsConsultations.fxml");
    }

    @FXML
    private void openReclamations() {
        goTo("/ReclamationsAdmin.fxml");
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
        goTo("/CommentairesAdmin.fxml");
    }

    @FXML
    private void openBlogModeration() {
        goTo("/ModerationBlog.fxml");
    }

    @FXML
    private void goBackSite() {
        goTo("/FrontFXML/Accueil.fxml");
    }

    @FXML
    private void logout() {
        goTo("/FrontFXML/Login.fxml");
    }

    private void goTo(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);

            if (url == null) {
                showWarning("Page introuvable", "Fichier introuvable : " + fxmlPath + "\nChange le chemin dans StatProduitController.");
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
}