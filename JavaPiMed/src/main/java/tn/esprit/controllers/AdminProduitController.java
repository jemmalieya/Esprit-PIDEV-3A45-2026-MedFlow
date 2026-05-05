package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.entities.Produit;
import tn.esprit.services.ProduitService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AdminProduitController {

    @FXML private BorderPane rootPane;

    @FXML private Label dateLabel;
    @FXML private TextField topSearchField;

    @FXML private Label totalProduitsLabel;
    @FXML private Label disponiblesLabel;
    @FXML private Label ruptureLabel;
    @FXML private Label stockTotalLabel;
    @FXML private Label resultCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private VBox produitsRowsContainer;

    private final ProduitService produitService = new ProduitService();
    private final List<Produit> allProduits = new ArrayList<>();
    private final List<Produit> filteredProduits = new ArrayList<>();

    @FXML
    public void initialize() {
        if (dateLabel != null) {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            dateLabel.setText("Admin • Produits • " + date);
        }

        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList(
                    "Trier...",
                    "Nom A-Z",
                    "Nom Z-A",
                    "Prix croissant",
                    "Prix décroissant",
                    "Stock croissant",
                    "Stock décroissant",
                    "Statut"
            ));
            sortCombo.setValue("Trier...");
            sortCombo.setOnAction(e -> applyFilters());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }

        if (topSearchField != null) {
            topSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (searchField != null && !Objects.equals(searchField.getText(), newValue)) {
                    searchField.setText(newValue);
                }
            });
        }

        Platform.runLater(this::loadProduits);
    }

    private void loadProduits() {
        try {
            allProduits.clear();
            allProduits.addAll(produitService.recuperer());
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showWarning("Erreur", "Impossible de charger les produits : " + e.getMessage());
        }
    }

    private void applyFilters() {
        filteredProduits.clear();

        String keyword = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase(Locale.ROOT)
                : "";

        for (Produit p : allProduits) {
            if (keyword.isEmpty()
                    || safe(p.getNom_produit()).toLowerCase(Locale.ROOT).contains(keyword)
                    || safe(p.getCategorie_produit()).toLowerCase(Locale.ROOT).contains(keyword)
                    || safe(p.getStatus_produit()).toLowerCase(Locale.ROOT).contains(keyword)
                    || safe(p.getDescription_produit()).toLowerCase(Locale.ROOT).contains(keyword)) {
                filteredProduits.add(p);
            }
        }

        applySort();
        updateStats();
        renderRows();
    }

    private void applySort() {
        if (sortCombo == null || sortCombo.getValue() == null) return;

        String selected = sortCombo.getValue();

        switch (selected) {
            case "Nom A-Z" ->
                    filteredProduits.sort(Comparator.comparing(p -> safe(p.getNom_produit()).toLowerCase(Locale.ROOT)));

            case "Nom Z-A" ->
                    filteredProduits.sort(Comparator.comparing((Produit p) -> safe(p.getNom_produit()).toLowerCase(Locale.ROOT)).reversed());

            case "Prix croissant" ->
                    filteredProduits.sort(Comparator.comparingDouble(Produit::getPrix_produit));

            case "Prix décroissant" ->
                    filteredProduits.sort(Comparator.comparingDouble(Produit::getPrix_produit).reversed());

            case "Stock croissant" ->
                    filteredProduits.sort(Comparator.comparingInt(Produit::getQuantite_produit));

            case "Stock décroissant" ->
                    filteredProduits.sort(Comparator.comparingInt(Produit::getQuantite_produit).reversed());

            case "Statut" ->
                    filteredProduits.sort(Comparator.comparing(p -> safe(p.getStatus_produit()).toLowerCase(Locale.ROOT)));
        }
    }

    private void updateStats() {
        int total = filteredProduits.size();
        int disponibles = 0;
        int rupture = 0;
        int stockTotal = 0;

        for (Produit p : filteredProduits) {
            String statut = safe(p.getStatus_produit()).toLowerCase(Locale.ROOT);

            if (statut.contains("rupture") || p.getQuantite_produit() <= 0) {
                rupture++;
            } else if (statut.contains("disponible")) {
                disponibles++;
            }

            stockTotal += Math.max(0, p.getQuantite_produit());
        }

        if (totalProduitsLabel != null) totalProduitsLabel.setText(String.valueOf(total));
        if (disponiblesLabel != null) disponiblesLabel.setText(String.valueOf(disponibles));
        if (ruptureLabel != null) ruptureLabel.setText(String.valueOf(rupture));
        if (stockTotalLabel != null) stockTotalLabel.setText(String.valueOf(stockTotal));
        if (resultCountLabel != null) resultCountLabel.setText(total + " produit(s)");
    }

    private void renderRows() {
        if (produitsRowsContainer == null) return;

        produitsRowsContainer.getChildren().clear();

        if (filteredProduits.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));

            Label title = new Label("Aucun produit trouvé");
            title.getStyleClass().add("empty-title");

            Label text = new Label("Essayez une autre recherche ou changez le tri.");
            text.getStyleClass().add("empty-text");

            empty.getChildren().addAll(title, text);
            produitsRowsContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < filteredProduits.size(); i++) {
            produitsRowsContainer.getChildren().add(createRow(filteredProduits.get(i), i));
        }
    }

    private HBox createRow(Produit p, int index) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("product-row");

        if (index % 2 == 0) {
            row.getStyleClass().add("product-row-light");
        }

        StackPane imageCell = new StackPane();
        imageCell.setPrefWidth(110);
        imageCell.setMinWidth(110);
        imageCell.setAlignment(Pos.CENTER_LEFT);

        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(58, 58);
        imgBox.getStyleClass().add("product-img-box");

        Image img = loadImage(safe(p.getImage_produit()));
        if (img != null) {
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(52);
            imageView.setFitHeight(52);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imgBox.getChildren().add(imageView);
        } else {
            Label placeholder = new Label("IMG");
            placeholder.getStyleClass().add("image-placeholder-text");
            imgBox.getChildren().add(placeholder);
        }

        imageCell.getChildren().add(imgBox);

        VBox produitCell = new VBox(4);
        produitCell.setPrefWidth(250);
        produitCell.setMinWidth(250);

        Label nom = new Label(safe(p.getNom_produit()));
        nom.getStyleClass().add("product-name");

        Label id = new Label("ID: " + getProductIdText(p));
        id.getStyleClass().add("product-id");

        Label desc = new Label(shortText(safe(p.getDescription_produit()), 38));
        desc.getStyleClass().add("product-desc");

        produitCell.getChildren().addAll(nom, id, desc);

        Label categorie = new Label(emptyDash(p.getCategorie_produit()));
        categorie.getStyleClass().add("category-badge");
        VBox categorieCell = cell(categorie, 170);

        Label prix = new Label(formatPrix(p.getPrix_produit()));
        prix.getStyleClass().add("price-text");
        VBox prixCell = cell(prix, 120);

        Label stock = new Label(p.getQuantite_produit() + (p.getQuantite_produit() <= 1 ? " unité" : " unités"));
        String statutLower = safe(p.getStatus_produit()).toLowerCase(Locale.ROOT);
        if (p.getQuantite_produit() <= 0 || statutLower.contains("rupture")) {
            stock.getStyleClass().add("stock-badge-red");
        } else if (p.getQuantite_produit() <= 5) {
            stock.getStyleClass().add("stock-badge-orange");
        } else {
            stock.getStyleClass().add("stock-badge-green");
        }
        VBox stockCell = cell(stock, 140);

        Label statut = new Label(emptyDash(p.getStatus_produit()));
        if (statutLower.contains("rupture")) {
            statut.getStyleClass().add("status-rupture");
        } else if (statutLower.contains("indisponible")) {
            statut.getStyleClass().add("status-indispo");
        } else {
            statut.getStyleClass().add("status-dispo");
        }
        VBox statutCell = cell(statut, 180);

        Button eyeBtn = new Button("👁");
        eyeBtn.getStyleClass().add("eye-button");
        eyeBtn.setOnAction(e -> showProduitDetailsPopup(p));

        VBox detailsCell = cell(eyeBtn, 120);
        detailsCell.setAlignment(Pos.CENTER_LEFT);

        row.getChildren().addAll(
                imageCell,
                produitCell,
                categorieCell,
                prixCell,
                stockCell,
                statutCell,
                detailsCell
        );

        return row;
    }

    private VBox cell(javafx.scene.Node node, double width) {
        VBox box = new VBox(node);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(width);
        box.setMinWidth(width);
        box.setMaxWidth(width);
        return box;
    }

    private void showProduitDetailsPopup(Produit p) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails produit");
        dialog.setHeaderText(null);
        dialog.setResizable(false);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);

        URL cssUrl = getClass().getResource("/CSS/admin-produits.css");
        if (cssUrl != null) {
            pane.getStylesheets().add(cssUrl.toExternalForm());
        }

        pane.getStyleClass().add("product-details-dialog");

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("details-root");

        HBox top = new HBox(16);
        top.setAlignment(Pos.CENTER_LEFT);

        StackPane imgBox = new StackPane();
        imgBox.setPrefSize(90, 90);
        imgBox.getStyleClass().add("details-image-box");

        Image img = loadImage(safe(p.getImage_produit()));
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(78);
            iv.setFitHeight(78);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            imgBox.getChildren().add(iv);
        } else {
            Label placeholder = new Label("IMG");
            placeholder.getStyleClass().add("image-placeholder-text");
            imgBox.getChildren().add(placeholder);
        }

        VBox titleBox = new VBox(8);
        Label title = new Label(safe(p.getNom_produit()));
        title.getStyleClass().add("details-title");

        Label id = new Label("ID: " + getProductIdText(p));
        id.getStyleClass().add("details-id");

        HBox badges = new HBox(8);
        Label categorie = new Label(emptyDash(p.getCategorie_produit()));
        categorie.getStyleClass().add("details-category");

        Label statut = new Label(emptyDash(p.getStatus_produit()));
        String statutLower = safe(p.getStatus_produit()).toLowerCase(Locale.ROOT);
        if (statutLower.contains("rupture")) {
            statut.getStyleClass().add("status-rupture");
        } else if (statutLower.contains("indisponible")) {
            statut.getStyleClass().add("status-indispo");
        } else {
            statut.getStyleClass().add("status-dispo");
        }

        badges.getChildren().addAll(categorie, statut);
        titleBox.getChildren().addAll(title, id, badges);

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
            Label ph = new Label("Image produit");
            ph.getStyleClass().add("big-placeholder-text");
            bigImage.getChildren().add(ph);
        }

        HBox miniStats = new HBox(12);
        VBox priceBox = detailsMiniBox("Prix", formatPrix(p.getPrix_produit()), false);
        VBox stockBox = detailsMiniBox("Stock", p.getQuantite_produit() + " unité(s)", p.getQuantite_produit() <= 0);
        miniStats.getChildren().addAll(priceBox, stockBox);

        left.getChildren().addAll(bigImage, miniStats);

        VBox right = new VBox(16);
        right.setPrefWidth(520);

        VBox descBox = new VBox(10);
        descBox.getStyleClass().add("details-card");
        Label descTitle = new Label("Description");
        descTitle.getStyleClass().add("details-card-title");

        Label desc = new Label(safe(p.getDescription_produit()).isBlank()
                ? "Aucune description disponible."
                : safe(p.getDescription_produit()));
        desc.setWrapText(true);
        desc.getStyleClass().add("details-desc");

        descBox.getChildren().addAll(descTitle, desc);

        VBox summary = new VBox(12);
        summary.getStyleClass().add("details-card");
        Label summaryTitle = new Label("Résumé rapide");
        summaryTitle.getStyleClass().add("details-card-title");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.add(detailsSmallItem("Catégorie", emptyDash(p.getCategorie_produit())), 0, 0);
        grid.add(detailsSmallItem("Statut", emptyDash(p.getStatus_produit())), 1, 0);
        grid.add(detailsSmallItem("Prix", formatPrix(p.getPrix_produit())), 0, 1);
        grid.add(detailsSmallItem("Stock", p.getQuantite_produit() + " unité(s)"), 1, 1);

        summary.getChildren().addAll(summaryTitle, grid);

        VBox conseil = new VBox(6);
        conseil.getStyleClass().add("details-advice");
        Label conseilTitle = new Label("Conseil");
        conseilTitle.getStyleClass().add("advice-title");

        Label conseilText = new Label(getConseilProduit(p));
        conseilText.setWrapText(true);
        conseilText.getStyleClass().add("advice-text");

        conseil.getChildren().addAll(conseilTitle, conseilText);

        right.getChildren().addAll(descBox, summary, conseil);
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

    private VBox detailsMiniBox(String label, String value, boolean danger) {
        VBox box = new VBox(6);
        box.getStyleClass().add("details-mini-box");

        Label l = new Label(label);
        l.getStyleClass().add("details-mini-label");

        Label v = new Label(value);
        v.getStyleClass().add(danger ? "details-mini-value-danger" : "details-mini-value");

        box.getChildren().addAll(l, v);
        return box;
    }

    private VBox detailsSmallItem(String label, String value) {
        VBox box = new VBox(6);
        box.setPrefWidth(230);
        box.getStyleClass().add("summary-item");

        Label l = new Label(label);
        l.getStyleClass().add("summary-label");

        Label v = new Label(value);
        v.getStyleClass().add("summary-value");

        box.getChildren().addAll(l, v);
        return box;
    }

    private String getConseilProduit(Produit p) {
        String statut = safe(p.getStatus_produit()).toLowerCase(Locale.ROOT);
        if (statut.contains("rupture") || p.getQuantite_produit() <= 0) {
            return "Ce produit est en rupture. Pensez à mettre à jour le stock avant de le rendre disponible.";
        }
        if (p.getQuantite_produit() <= 5) {
            return "Vérifiez le stock faible pour éviter la rupture.";
        }
        return "Le produit est disponible. Continuez à surveiller le stock régulièrement.";
    }

    private Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;

        try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                return new Image(path, true);
            }

            File file = new File(path);
            if (file.exists()) {
                return new Image(file.toURI().toString(), true);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getProductIdText(Produit p) {
        String[] methods = {"getId_produit", "getIdProduit", "getId", "getReference_produit"};

        for (String methodName : methods) {
            try {
                Method m = p.getClass().getMethod(methodName);
                Object value = m.invoke(p);
                if (value != null) return "#" + value;
            } catch (Exception ignored) {
            }
        }

        return "—";
    }

    private String formatPrix(double prix) {
        return String.format(Locale.FRANCE, "%.2f DT", prix);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String emptyDash(String value) {
        return safe(value).isBlank() ? "—" : safe(value);
    }

    private String shortText(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max) + "...";
    }

    @FXML
    private void openAjouterProduit() {
        goTo("/AjouterProduit.fxml");
    }

    @FXML
    private void openDashboard() {
        goTo("/AdminWelcome.fxml");
    }

    @FXML
    private void openProduits() {
        loadProduits();
    }

    @FXML
    private void openCommandes() {
        goTo("/CommandeAdmin.fxml");
    }

    @FXML
    private void openStatsProduits() {
        goTo("/DashboardAdminBI.fxml");
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
                showWarning("Page introuvable", "Fichier introuvable : " + fxmlPath + "\nChange le chemin dans AdminProduitController.");
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