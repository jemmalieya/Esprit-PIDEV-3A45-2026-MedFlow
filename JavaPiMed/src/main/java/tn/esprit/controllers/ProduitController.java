package tn.esprit.controllers;


import tn.esprit.entities.Commande;
import tn.esprit.services.CommandeService;
import tn.esprit.services.DashboardBIService;

import java.time.LocalDate;
import java.util.*;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import tn.esprit.entities.Produit;
import tn.esprit.services.HuggingFaceRecommendationService;
import tn.esprit.services.ProduitService;
import tn.esprit.services.ProduitSpeechService;
import tn.esprit.session.CartSession;
import tn.esprit.tools.SessionManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.awt.image.BufferedImage;

public class ProduitController {

    private static final String PHARMACY_NAME = "Pharmacie Esprit Ariana";
    private static final String PHARMACY_PHONE = "+216 22 123 456";
    private static final String PHARMACY_EMAIL = "contact@medflow.tn";
    private static final String PHARMACY_ADDRESS = "Pharmacie Esprit Ariana, Ariana, Tunisie";
    private final ProduitSpeechService produitSpeechService = new ProduitSpeechService();

    // AJOUT / MODIF
    @FXML private Button btnAjouterProduit;
    @FXML private Button btnModifierProduit;
    @FXML private Button btnResetProduit;
    @FXML private Button btnUploadProduit;
    @FXML private ComboBox<String> cbCategorieProduit;
    @FXML private Label lblImageProduit;
    @FXML private RadioButton rbDisponibleProduit;
    @FXML private RadioButton rbIndisponibleProduit;
    @FXML private RadioButton rbRuptureProduit;
    @FXML private TextArea taDescriptionProduit;
    @FXML private TextField tfNomProduit;
    @FXML private TextField tfPrixProduit;
    @FXML private TextField tfQuantiteProduit;

    // DASHBOARD
    @FXML private Label totalProduitsLabel;
    @FXML private Label disponiblesLabel;
    @FXML private Label ruptureLabel;
    @FXML private Label indisponiblesLabel;
    @FXML private Label inventoryCountLabel;
    @FXML private Label alerteInventaireLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private VBox produitsListContainer;
    @FXML private VBox submenuVBox;
    @FXML private Label produitArrow;

    // PHARMACIE FRONT
    @FXML private FlowPane productGrid;
    @FXML private TextField searchProduitField;
    @FXML private ComboBox<String> triPrixCombo;
    @FXML private ComboBox<String> triStockCombo;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private Label resultCountLabel;
    @FXML private Button btnPanier;

    // DASHBOARD ADMIN BI
    @FXML private Label biPeriodeLabel;

    @FXML private DatePicker biDateDebutPicker;
    @FXML private DatePicker biDateFinPicker;
    @FXML private ComboBox<String> biCategorieCombo;

    @FXML private Label biCroissanceLabel;
    @FXML private Label biStockCritiqueLabel;

    @FXML private Label biCaLabel;
    @FXML private Label biCommandesLabel;
    @FXML private Label biPanierMoyenLabel;
    @FXML private Label biConversionLabel;

    @FXML private Label biQuantiteVendueLabel;
    @FXML private Label biEnAttenteLabel;
    @FXML private Label biTauxRuptureLabel;
    @FXML private Label biPeriodeAnalyseeLabel;

    @FXML private LineChart<String, Number> ventesLineChart;
    @FXML private PieChart statutPieChart;
    @FXML private BarChart<String, Number> categorieBarChart;
    @FXML private BarChart<String, Number> caCategorieBarChart;

    @FXML private TableView<Produit> topProduitsTable;
    @FXML private TableColumn<Produit, String> topNomCol;
    @FXML private TableColumn<Produit, String> topCategorieCol;
    @FXML private TableColumn<Produit, Number> topQteCol;
    @FXML private TableColumn<Produit, String> topCaCol;
    @FXML private TableColumn<Produit, Number> topStockCol;

    @FXML private TableView<Produit> stockCritiqueTable;
    @FXML private TableColumn<Produit, String> stockNomCol;
    @FXML private TableColumn<Produit, String> stockCategorieCol;
    @FXML private TableColumn<Produit, Number> stockStockCol;
    @FXML private TableColumn<Produit, String> stockStatutCol;

    @FXML private FlowPane recommandationsContainer;
    private final HuggingFaceRecommendationService huggingFaceService =
            new HuggingFaceRecommendationService();


    private final DashboardBIService dashboardBIService = new DashboardBIService();
    private final ProduitService produitService = new ProduitService();
    private final ObservableList<Produit> masterList = FXCollections.observableArrayList();
    private final ObservableList<Produit> filteredList = FXCollections.observableArrayList();
    private final List<Produit> allProduitsFront = new ArrayList<>();

    private static Produit produitAmodifier;
    private String imagePath = "";

    private Label nomMsg, descMsg, prixMsg, quantiteMsg, categorieMsg, imageMsg, statusMsg;

    private boolean isNomValid = false, isDescValid = false, isPrixValid = false, isQuantiteValid = false, isCategorieValid = false, isImageValid = false, isStatusValid = true;

    @FXML
    public void initialize() {
        initAjoutPageIfExists();
        initDashboardIfExists();
        initModifierPageIfExists();
        initPharmacieIfExists();
        initDashboardBIIfExists();



    }

    private void initAjoutPageIfExists() {
        if (cbCategorieProduit != null && cbCategorieProduit.getItems().isEmpty()) {
            cbCategorieProduit.getItems().addAll("Médicament", "Parapharmacie", "Matériel");
            cbCategorieProduit.setValue("Médicament");
        }

        if (lblImageProduit != null && lblImageProduit.getText() == null) {
            lblImageProduit.setText("Aucun fichier choisi");
        }

        if (rbDisponibleProduit != null
                && !rbDisponibleProduit.isSelected()
                && rbRuptureProduit != null
                && !rbRuptureProduit.isSelected()
                && rbIndisponibleProduit != null
                && !rbIndisponibleProduit.isSelected()) {
            rbDisponibleProduit.setSelected(true);
        }

        // Add validation labels and listeners
        if (tfNomProduit != null) {
            VBox parent = (VBox) tfNomProduit.getParent();
            int index = parent.getChildren().indexOf(tfNomProduit);
            if (index + 1 < parent.getChildren().size() && parent.getChildren().get(index + 1) instanceof Label && ((Label)parent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                nomMsg = (Label) parent.getChildren().get(index + 1);
            } else {
                nomMsg = new Label();
                nomMsg.getStyleClass().add("validation-message");
                parent.getChildren().add(index + 1, nomMsg);
            }
            tfNomProduit.textProperty().addListener((obs, old, newVal) -> validateNom(newVal));
        }

        if (taDescriptionProduit != null) {
            VBox parent = (VBox) taDescriptionProduit.getParent();
            int index = parent.getChildren().indexOf(taDescriptionProduit);
            if (index + 1 < parent.getChildren().size() && parent.getChildren().get(index + 1) instanceof Label && ((Label)parent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                descMsg = (Label) parent.getChildren().get(index + 1);
            } else {
                descMsg = new Label();
                descMsg.getStyleClass().add("validation-message");
                parent.getChildren().add(index + 1, descMsg);
            }
            taDescriptionProduit.textProperty().addListener((obs, old, newVal) -> validateDescription(newVal));
        }

        if (tfPrixProduit != null) {
            HBox parent = (HBox) tfPrixProduit.getParent();
            VBox grandParent = (VBox) parent.getParent();
            int index = grandParent.getChildren().indexOf(parent);
            if (index + 1 < grandParent.getChildren().size() && grandParent.getChildren().get(index + 1) instanceof Label && ((Label)grandParent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                prixMsg = (Label) grandParent.getChildren().get(index + 1);
            } else {
                prixMsg = new Label();
                prixMsg.getStyleClass().add("validation-message");
                grandParent.getChildren().add(index + 1, prixMsg);
            }
            tfPrixProduit.textProperty().addListener((obs, old, newVal) -> validatePrix(newVal));
        }

        if (tfQuantiteProduit != null) {
            VBox parent = (VBox) tfQuantiteProduit.getParent();
            int index = parent.getChildren().indexOf(tfQuantiteProduit);
            if (index + 1 < parent.getChildren().size() && parent.getChildren().get(index + 1) instanceof Label && ((Label)parent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                quantiteMsg = (Label) parent.getChildren().get(index + 1);
            } else {
                quantiteMsg = new Label();
                quantiteMsg.getStyleClass().add("validation-message");
                parent.getChildren().add(index + 1, quantiteMsg);
            }
            tfQuantiteProduit.textProperty().addListener((obs, old, newVal) -> validateQuantite(newVal));
        }

        if (cbCategorieProduit != null) {
            VBox parent = (VBox) cbCategorieProduit.getParent();
            int index = parent.getChildren().indexOf(cbCategorieProduit);
            if (index + 1 < parent.getChildren().size() && parent.getChildren().get(index + 1) instanceof Label && ((Label)parent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                categorieMsg = (Label) parent.getChildren().get(index + 1);
            } else {
                categorieMsg = new Label();
                categorieMsg.getStyleClass().add("validation-message");
                parent.getChildren().add(index + 1, categorieMsg);
            }
            cbCategorieProduit.valueProperty().addListener((obs, old, newVal) -> validateCategorie(newVal));
        }

        if (lblImageProduit != null) {
            VBox parent = (VBox) lblImageProduit.getParent().getParent();
            int index = parent.getChildren().indexOf(lblImageProduit.getParent());
            if (index + 1 < parent.getChildren().size() && parent.getChildren().get(index + 1) instanceof Label && ((Label)parent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                imageMsg = (Label) parent.getChildren().get(index + 1);
            } else {
                imageMsg = new Label();
                imageMsg.getStyleClass().add("validation-message");
                parent.getChildren().add(index + 1, imageMsg);
            }
            // Listener for image change
            lblImageProduit.textProperty().addListener((obs, old, newVal) -> validateImage(newVal));
        }

        // Initial validation
        if (tfNomProduit != null) validateNom(tfNomProduit.getText());
        if (taDescriptionProduit != null) validateDescription(taDescriptionProduit.getText());
        if (tfPrixProduit != null) validatePrix(tfPrixProduit.getText());
        if (tfQuantiteProduit != null) validateQuantite(tfQuantiteProduit.getText());
        if (cbCategorieProduit != null) validateCategorie(cbCategorieProduit.getValue());
        if (lblImageProduit != null) validateImage(lblImageProduit.getText());
    }

    private void initModifierPageIfExists() {
        if (btnModifierProduit == null || produitAmodifier == null) return;

        if (cbCategorieProduit != null) {
            cbCategorieProduit.getItems().clear();
            cbCategorieProduit.getItems().addAll("Médicament", "Parapharmacie", "Matériel");
        }

        tfNomProduit.setText(safe(produitAmodifier.getNom_produit()));
        taDescriptionProduit.setText(safe(produitAmodifier.getDescription_produit()));
        tfPrixProduit.setText(String.valueOf(produitAmodifier.getPrix_produit()));
        tfQuantiteProduit.setText(String.valueOf(produitAmodifier.getQuantite_produit()));
        cbCategorieProduit.setValue(safe(produitAmodifier.getCategorie_produit()));

        String img = safe(produitAmodifier.getImage_produit());
        if (!img.isEmpty()) {
            imagePath = img;
            if (lblImageProduit != null) {
                File f = new File(img);
                lblImageProduit.setText(f.exists() ? f.getName() : img);
            }
        }

        String statut = safe(produitAmodifier.getStatus_produit()).toLowerCase(Locale.ROOT);
        if (statut.contains("rupture")) {
            rbRuptureProduit.setSelected(true);
        } else if (statut.contains("indisponible")) {
            rbIndisponibleProduit.setSelected(true);
        } else {
            rbDisponibleProduit.setSelected(true);
        }

        // Add validation labels and listeners for modifier page
        if (tfNomProduit != null) {
            VBox parent = (VBox) tfNomProduit.getParent();
            int index = parent.getChildren().indexOf(tfNomProduit);
            if (index + 1 < parent.getChildren().size() && parent.getChildren().get(index + 1) instanceof Label && ((Label)parent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                nomMsg = (Label) parent.getChildren().get(index + 1);
            } else {
                nomMsg = new Label();
                nomMsg.getStyleClass().add("validation-message");
                parent.getChildren().add(index + 1, nomMsg);
            }
            tfNomProduit.textProperty().addListener((obs, old, newVal) -> validateNom(newVal));
        }

        if (taDescriptionProduit != null) {
            VBox parent = (VBox) taDescriptionProduit.getParent();
            int index = parent.getChildren().indexOf(taDescriptionProduit);
            if (index + 1 < parent.getChildren().size() && parent.getChildren().get(index + 1) instanceof Label && ((Label)parent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                descMsg = (Label) parent.getChildren().get(index + 1);
            } else {
                descMsg = new Label();
                descMsg.getStyleClass().add("validation-message");
                parent.getChildren().add(index + 1, descMsg);
            }
            taDescriptionProduit.textProperty().addListener((obs, old, newVal) -> validateDescription(newVal));
        }

        if (tfPrixProduit != null) {
            HBox parent = (HBox) tfPrixProduit.getParent();
            VBox grandParent = (VBox) parent.getParent();
            int index = grandParent.getChildren().indexOf(parent);
            if (index + 1 < grandParent.getChildren().size() && grandParent.getChildren().get(index + 1) instanceof Label && ((Label)grandParent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                prixMsg = (Label) grandParent.getChildren().get(index + 1);
            } else {
                prixMsg = new Label();
                prixMsg.getStyleClass().add("validation-message");
                grandParent.getChildren().add(index + 1, prixMsg);
            }
            tfPrixProduit.textProperty().addListener((obs, old, newVal) -> validatePrix(newVal));
        }

        if (tfQuantiteProduit != null) {
            VBox parent = (VBox) tfQuantiteProduit.getParent();
            int index = parent.getChildren().indexOf(tfQuantiteProduit);
            if (index + 1 < parent.getChildren().size() && parent.getChildren().get(index + 1) instanceof Label && ((Label)parent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                quantiteMsg = (Label) parent.getChildren().get(index + 1);
            } else {
                quantiteMsg = new Label();
                quantiteMsg.getStyleClass().add("validation-message");
                parent.getChildren().add(index + 1, quantiteMsg);
            }
            tfQuantiteProduit.textProperty().addListener((obs, old, newVal) -> validateQuantite(newVal));
        }

        if (cbCategorieProduit != null) {
            VBox parent = (VBox) cbCategorieProduit.getParent();
            int index = parent.getChildren().indexOf(cbCategorieProduit);
            if (index + 1 < parent.getChildren().size() && parent.getChildren().get(index + 1) instanceof Label && ((Label)parent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                categorieMsg = (Label) parent.getChildren().get(index + 1);
            } else {
                categorieMsg = new Label();
                categorieMsg.getStyleClass().add("validation-message");
                parent.getChildren().add(index + 1, categorieMsg);
            }
            cbCategorieProduit.valueProperty().addListener((obs, old, newVal) -> validateCategorie(newVal));
        }

        if (lblImageProduit != null) {
            VBox parent = (VBox) lblImageProduit.getParent().getParent();
            int index = parent.getChildren().indexOf(lblImageProduit.getParent());
            if (index + 1 < parent.getChildren().size() && parent.getChildren().get(index + 1) instanceof Label && ((Label)parent.getChildren().get(index + 1)).getStyleClass().contains("validation-message")) {
                imageMsg = (Label) parent.getChildren().get(index + 1);
            } else {
                imageMsg = new Label();
                imageMsg.getStyleClass().add("validation-message");
                parent.getChildren().add(index + 1, imageMsg);
            }
            lblImageProduit.textProperty().addListener((obs, old, newVal) -> validateImage(newVal));
        }

        // Initial validation
        if (tfNomProduit != null) validateNom(tfNomProduit.getText());
        if (taDescriptionProduit != null) validateDescription(taDescriptionProduit.getText());
        if (tfPrixProduit != null) validatePrix(tfPrixProduit.getText());
        if (tfQuantiteProduit != null) validateQuantite(tfQuantiteProduit.getText());
        if (cbCategorieProduit != null) validateCategorie(cbCategorieProduit.getValue());
        if (lblImageProduit != null) validateImage(lblImageProduit.getText());
    }

    private void initDashboardIfExists() {
        if (produitsListContainer == null) return;

        if (sortCombo != null) {
            sortCombo.getItems().addAll(
                    "Trier...",
                    "Nom A-Z",
                    "Nom Z-A",
                    "Prix croissant",
                    "Prix décroissant",
                    "Quantité croissante",
                    "Quantité décroissante",
                    "Statut"
            );
            sortCombo.setValue("Trier...");
            sortCombo.setOnAction(e -> applyFiltersAndRefresh());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFiltersAndRefresh());
        }

        loadProduits();
    }

    private void loadProduits() {
        List<Produit> produits = produitService.recuperer();
        masterList.setAll(produits);
        applyFiltersAndRefresh();
    }

    /*private void applyFiltersAndRefresh() {
        filteredList.setAll(masterList);

        String keyword = "";
        if (searchField != null && searchField.getText() != null) {
            keyword = searchField.getText().trim().toLowerCase(Locale.ROOT);
        }

        if (!keyword.isEmpty()) {
            List<Produit> temp = new ArrayList<>();
            for (Produit p : filteredList) {
                if (contains(p.getNom_produit(), keyword)
                        || contains(p.getDescription_produit(), keyword)
                        || contains(p.getCategorie_produit(), keyword)
                        || contains(p.getStatus_produit(), keyword)
                        || contains(String.valueOf(p.getPrix_produit()), keyword)
                        || contains(String.valueOf(p.getQuantite_produit()), keyword)) {
                    temp.add(p);
                }
            }
            filteredList.setAll(temp);
        }

        applySort();
        refreshRows();
        updateStats();
    }
*/
    private void applyFiltersAndRefresh() {

        List<Produit> liste = produitService.recuperer();

        String keyword = searchField != null ? searchField.getText() : "";
        String categorie = ""; // tu peux ajouter ComboBox plus tard
        String statut = "";    // idem
        String tri = sortCombo != null ? sortCombo.getValue() : "";

        // 🔥 APPELS SERVICE
        liste = produitService.rechercherProduits(liste, keyword);
        liste = produitService.filtrerProduits(liste, categorie, statut);
        liste = produitService.trierProduits(liste, tri);

        filteredList.setAll(liste);

        refreshRows();
        updateStats();
    }
    private void applySort() {
        if (sortCombo == null || sortCombo.getValue() == null) return;

        String selected = sortCombo.getValue();
        Comparator<Produit> comparator = null;

        switch (selected) {
            case "Nom A-Z" -> comparator = Comparator.comparing(p -> safe(p.getNom_produit()).toLowerCase());
            case "Nom Z-A" -> comparator = Comparator.comparing((Produit p) -> safe(p.getNom_produit()).toLowerCase()).reversed();
            case "Prix croissant" -> comparator = Comparator.comparingDouble(Produit::getPrix_produit);
            case "Prix décroissant" -> comparator = Comparator.comparingDouble(Produit::getPrix_produit).reversed();
            case "Quantité croissante" -> comparator = Comparator.comparingInt(Produit::getQuantite_produit);
            case "Quantité décroissante" -> comparator = Comparator.comparingInt(Produit::getQuantite_produit).reversed();
            case "Statut" -> comparator = Comparator.comparing(p -> safe(p.getStatus_produit()).toLowerCase());
        }

        if (comparator != null) {
            FXCollections.sort(filteredList, comparator);
        }
    }

    private void refreshRows() {
        if (produitsListContainer == null) return;

        produitsListContainer.getChildren().clear();

        if (filteredList.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.getStyleClass().add("empty-box");

            Label title = new Label("Aucun produit trouvé");
            title.getStyleClass().add("empty-title");

            Label text = new Label("Essayez une autre recherche ou changez le tri.");
            text.getStyleClass().add("empty-text");

            emptyBox.getChildren().addAll(title, text);
            produitsListContainer.getChildren().add(emptyBox);
            return;
        }

        for (int i = 0; i < filteredList.size(); i++) {
            Produit produit = filteredList.get(i);
            HBox row = createProduitRow(produit, i);
            produitsListContainer.getChildren().add(row);
        }
    }


    private HBox createProduitRow(Produit p, int index) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(0);
        row.setPadding(new Insets(14, 8, 14, 8));
        row.getStyleClass().add("product-row");
        row.setMaxWidth(Double.MAX_VALUE);

        if (index % 2 == 1) {
            row.getStyleClass().add("product-row-even");
        }

        // IMAGE
        StackPane imageBox = new StackPane();
        imageBox.setPrefWidth(90);
        imageBox.setMinWidth(90);
        imageBox.setMaxWidth(90);
        imageBox.setAlignment(Pos.CENTER_LEFT);

        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("row-image-box");
        thumb.setPrefSize(52, 52);

        String imagePath = safe(p.getImage_produit());
        if (!imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    ImageView imageView = new ImageView(new Image(file.toURI().toString()));
                    imageView.setFitWidth(45);
                    imageView.setFitHeight(45);
                    imageView.setPreserveRatio(true);
                    thumb.getChildren().add(imageView);
                } else {
                    Label imgIcon = new Label("🖼");
                    imgIcon.getStyleClass().add("row-image-icon");
                    thumb.getChildren().add(imgIcon);
                }
            } catch (Exception e) {
                Label imgIcon = new Label("🖼");
                imgIcon.getStyleClass().add("row-image-icon");
                thumb.getChildren().add(imgIcon);
            }
        } else {
            Label imgIcon = new Label("🖼");
            imgIcon.getStyleClass().add("row-image-icon");
            thumb.getChildren().add(imgIcon);
        }

        imageBox.getChildren().add(thumb);

        // NOM
        Label nomLabel = new Label(safe(p.getNom_produit()));
        nomLabel.getStyleClass().add("row-main-text");
        nomLabel.setWrapText(true);
        nomLabel.setMaxWidth(160);
        VBox nomBox = wrapProduitCell(nomLabel, 180, Pos.CENTER_LEFT);

        // PRIX
        Label prixLabel = new Label(String.format("%.2f Dt", p.getPrix_produit()));
        prixLabel.getStyleClass().add("row-price-text");
        VBox prixBox = wrapProduitCell(prixLabel, 120, Pos.CENTER_LEFT);

        // QUANTITE
        Label qteLabel = new Label(String.valueOf(p.getQuantite_produit()));
        if (p.getQuantite_produit() <= 0) {
            qteLabel.getStyleClass().add("qty-badge-red");
        } else if (p.getQuantite_produit() <= 5) {
            qteLabel.getStyleClass().add("qty-badge-soft-red");
        } else {
            qteLabel.getStyleClass().add("qty-badge-blue");
        }
        VBox qteBox = wrapProduitCell(qteLabel, 120, Pos.CENTER_LEFT);

        // CATEGORIE
        Label catLabel = new Label(safe(p.getCategorie_produit()));
        catLabel.getStyleClass().add("category-badge");
        VBox catBox = wrapProduitCell(catLabel, 160, Pos.CENTER_LEFT);

        // STATUT
        Label statutLabel = new Label(safe(p.getStatus_produit()));
        String statut = safe(p.getStatus_produit()).toLowerCase(Locale.ROOT);

        if (statut.contains("disponible")) {
            statutLabel.getStyleClass().add("statut-badge-disponible");
        } else if (statut.contains("rupture")) {
            statutLabel.getStyleClass().add("statut-badge-rupture");
        } else {
            statutLabel.getStyleClass().add("statut-badge-indisponible");
        }

        VBox statutBox = wrapProduitCell(statutLabel, 150, Pos.CENTER_LEFT);

        // DESCRIPTION
        Label descLabel = new Label(shortText(safe(p.getDescription_produit()), 45));
        descLabel.getStyleClass().add("row-description-text");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(240);
        VBox descBox = wrapProduitCell(descLabel, 260, Pos.CENTER_LEFT);

        // ACTIONS
        Button editBtn = new Button("✏");
        editBtn.getStyleClass().add("action-blue-btn");
        editBtn.setOnAction(e -> ouvrirPageModifier(p));

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("action-red-btn");
        deleteBtn.setOnAction(e -> supprimerProduitAvecConfirmation(p));

        HBox actionsContent = new HBox(8, editBtn, deleteBtn);
        actionsContent.setAlignment(Pos.CENTER_LEFT);
        VBox actionsWrap = wrapProduitCell(actionsContent, 130, Pos.CENTER_LEFT);

        row.getChildren().addAll(
                imageBox,
                nomBox,
                prixBox,
                qteBox,
                catBox,
                statutBox,
                descBox,
                actionsWrap
        );

        return row;
    }
    private VBox wrapProduitCell(Node node, double width, Pos alignment) {
        VBox box = new VBox(node);
        box.setAlignment(alignment);
        box.setPrefWidth(width);
        box.setMinWidth(width);
        box.setMaxWidth(width);
        box.setFillWidth(true);
        return box;
    }
    private VBox wrapBox(javafx.scene.Node node, double width) {
        VBox box = new VBox(node);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(width);
        box.setMinWidth(width);
        return box;
    }
    private void updateStats() {
        int total = filteredList.size();
        int disponibles = 0;
        int rupture = 0;
        int indisponibles = 0;

        for (Produit p : filteredList) {
            String statut = safe(p.getStatus_produit()).toLowerCase(Locale.ROOT);
            if (statut.contains("rupture")) rupture++;
            else if (statut.contains("indisponible")) indisponibles++;
            else disponibles++;
        }

        if (totalProduitsLabel != null) totalProduitsLabel.setText(String.valueOf(total));
        if (disponiblesLabel != null) disponiblesLabel.setText(String.valueOf(disponibles));
        if (ruptureLabel != null) ruptureLabel.setText(String.valueOf(rupture));
        if (indisponiblesLabel != null) indisponiblesLabel.setText(String.valueOf(indisponibles));
        if (inventoryCountLabel != null) inventoryCountLabel.setText(total + " produit(s) dans votre inventaire");
        if (alerteInventaireLabel != null) alerteInventaireLabel.setText("🔔 Alerte Inventaire " + rupture);
    }

    private void initPharmacieIfExists() {
        if (productGrid == null) return;

        if (triPrixCombo != null) {
            triPrixCombo.setItems(FXCollections.observableArrayList(
                    "Trier prix", "Prix croissant", "Prix décroissant"
            ));
            triPrixCombo.setValue("Trier prix");
        }

        if (triStockCombo != null) {
            triStockCombo.setItems(FXCollections.observableArrayList(
                    "Trier stock", "Stock croissant", "Stock décroissant"
            ));
            triStockCombo.setValue("Trier stock");
        }

        if (categorieCombo != null) {
            categorieCombo.setItems(FXCollections.observableArrayList(
                    "Toutes catégories", "Médicament", "Parapharmacie", "Matériel"
            ));
            categorieCombo.setValue("Toutes catégories");
        }

        allProduitsFront.clear();
        allProduitsFront.addAll(produitService.recuperer());

        if (searchProduitField != null) {
            searchProduitField.textProperty().addListener((obs, oldV, newV) -> refreshPharmacieGrid());
        }
        if (triPrixCombo  != null) triPrixCombo.setOnAction(e -> refreshPharmacieGrid());
        if (triStockCombo != null) triStockCombo.setOnAction(e -> refreshPharmacieGrid());
        if (categorieCombo != null) categorieCombo.setOnAction(e -> refreshPharmacieGrid());

        updatePanierButton();
        refreshPharmacieGrid();

        // ── Charger les recommandations IA en arrière-plan ────────────────────
        chargerRecommandationsIA();
    }


    private void refreshPharmacieGrid() {
        if (productGrid == null) return;

        productGrid.getChildren().clear();

        List<Produit> filtered = new ArrayList<>(allProduitsFront);

        String keyword;
        if (searchProduitField != null && searchProduitField.getText() != null) {
            keyword = searchProduitField.getText().trim().toLowerCase(Locale.ROOT);
        } else {
            keyword = "";
        }

        if (!keyword.isEmpty()) {
            filtered.removeIf(p ->
                    !safe(p.getNom_produit()).toLowerCase(Locale.ROOT).contains(keyword)
                            && !safe(p.getCategorie_produit()).toLowerCase(Locale.ROOT).contains(keyword)
                            && !safe(p.getDescription_produit()).toLowerCase(Locale.ROOT).contains(keyword)
            );
        }

        String categorie = categorieCombo != null ? categorieCombo.getValue() : null;
        if (categorie != null && !categorie.equals("Toutes catégories")) {
            filtered.removeIf(p -> !safe(p.getCategorie_produit()).equalsIgnoreCase(categorie));
        }

        if (triPrixCombo != null) {
            if ("Prix croissant".equals(triPrixCombo.getValue())) {
                filtered.sort(Comparator.comparingDouble(Produit::getPrix_produit));
            } else if ("Prix décroissant".equals(triPrixCombo.getValue())) {
                filtered.sort(Comparator.comparingDouble(Produit::getPrix_produit).reversed());
            }
        }

        if (triStockCombo != null) {
            if ("Stock croissant".equals(triStockCombo.getValue())) {
                filtered.sort(Comparator.comparingInt(Produit::getQuantite_produit));
            } else if ("Stock décroissant".equals(triStockCombo.getValue())) {
                filtered.sort(Comparator.comparingInt(Produit::getQuantite_produit).reversed());
            }
        }

        for (Produit produit : filtered) {
            productGrid.getChildren().add(createProductCard(produit));
        }

        if (resultCountLabel != null) {
            resultCountLabel.setText(filtered.size() + " produit(s) trouvé(s)");
        }
    }

    private VBox createProductCard(Produit produit) {
        VBox card = new VBox();
        card.getStyleClass().add("product-card");
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setSpacing(10);

        // Image Box
        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("card-image-box");
        imageBox.setPrefHeight(150);

        Label topBadge = new Label();
        StackPane.setAlignment(topBadge, Pos.TOP_LEFT);
        StackPane.setMargin(topBadge, new Insets(10, 0, 0, 10));

        Label priceBadge = new Label(formatPrix(produit.getPrix_produit()));
        priceBadge.getStyleClass().add("price-badge-top");
        StackPane.setAlignment(priceBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(priceBadge, new Insets(10, 10, 0, 0));

        if (safe(produit.getStatus_produit()).toLowerCase(Locale.ROOT).contains("rupture")) {
            topBadge.setText("RUPTURE");
            topBadge.getStyleClass().add("stock-badge-top");
        }

        String imagePath = safe(produit.getImage_produit());
        if (!imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    ImageView imageView = new ImageView(new Image(file.toURI().toString()));
                    imageView.setFitWidth(120);
                    imageView.setFitHeight(120);
                    imageView.setPreserveRatio(true);
                    imageBox.getChildren().add(imageView);
                } else {
                    imageBox.getChildren().add(new Label("🖼"));
                }
            } catch (Exception e) {
                imageBox.getChildren().add(new Label("🖼"));
            }
        } else {
            imageBox.getChildren().add(new Label("🖼"));
        }

        imageBox.getChildren().addAll(topBadge, priceBadge);

        // Content
        VBox content = new VBox(8);
        content.getStyleClass().add("card-content");
        content.setPadding(new Insets(10));

        Label nom = new Label(safe(produit.getNom_produit()));
        nom.getStyleClass().add("product-name");
        nom.setWrapText(true);

        HBox categoryStock = new HBox(10);
        Label categorie = new Label("🏷 " + safe(produit.getCategorie_produit()));
        categorie.getStyleClass().add("category-chip");
        Label stock = new Label("📦 " + produit.getQuantite_produit());
        stock.getStyleClass().add("stock-text");
        categoryStock.getChildren().addAll(categorie, stock);

        Label statut = new Label(safe(produit.getStatus_produit()));
        if (safe(produit.getStatus_produit()).toLowerCase(Locale.ROOT).contains("rupture")) {
            statut.getStyleClass().add("status-chip-ko");
        } else {
            statut.getStyleClass().add("status-chip-ok");
        }

        // Quantity and Buttons
        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Spinner<Integer> quantitySpinner = new Spinner<>(1, 99, 1);
        quantitySpinner.setPrefWidth(60);
        quantitySpinner.getStyleClass().add("quantity-spinner");

        Button addBtn = new Button("Ajouter");
        if (safe(produit.getStatus_produit()).toLowerCase(Locale.ROOT).contains("rupture")) {
            addBtn.getStyleClass().add("add-btn-disabled");
            addBtn.setDisable(true);
            quantitySpinner.setDisable(true);
        } else {
            addBtn.getStyleClass().add("add-btn");
            addBtn.setOnAction(e -> ajouterProduitAuPanier(produit, quantitySpinner.getValue()));
        }

        Button infoBtn = new Button("ℹ");
        infoBtn.getStyleClass().add("icon-btn");
        infoBtn.setOnAction(e -> showProduitDetailsPopup(produit));

        bottomRow.getChildren().addAll(new Label("Qté:"), quantitySpinner, addBtn, infoBtn);

        content.getChildren().addAll(nom, categoryStock, statut, bottomRow);
        card.getChildren().addAll(imageBox, content);

        return card;
    }

    /*private HBox createInfoRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("popup-info-row");

        Label lbl = new Label(label);
        lbl.getStyleClass().add("popup-info-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label val = new Label(value);
        val.getStyleClass().add("popup-info-value");
        val.setWrapText(true);

        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }*/

    private HBox createInfoRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("popup-info-row");

        Label lbl = new Label(label);
        lbl.getStyleClass().add("popup-info-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label val = new Label(value);
        val.getStyleClass().add("popup-info-value");
        val.setWrapText(true);

        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }
    private void ajouterProduitAuPanier(Produit produit, int quantity) {
        checkStockAndShowAlert(produit, quantity);
        updatePanierButton();
    }

    private void updatePanierButton() {
        if (btnPanier != null) {
            btnPanier.setText("🛒 Panier (" + CartSession.getNombreArticles() + ")");
        }
    }

    @FXML
    private void ouvrirPanierPopup() {
        try {
            URL url = getClass().getResource("/FrontFXML/Panier.fxml"); // ✅ adapte au vrai chemin

            if (url == null) {
                throw new IOException("Fichier introuvable : /FrontFXML/Panier.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage = (Stage) btnPanier.getScene().getWindow();
            //stage.setScene(new Scene(root));
            stage.setScene(new Scene(root, 1400, 850));
            stage.setTitle("Mon Panier");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Navigation",
                    "Impossible d'ouvrir Panier.fxml : " + e.getMessage());
        }
    }

    private void showProduitDetails(Produit p) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails Produit");
        alert.setHeaderText(p.getNom_produit());
        alert.setContentText(
                "Prix : " + p.getPrix_produit() + " DT\n" +
                        "Stock : " + p.getQuantite_produit() + "\n" +
                        "Catégorie : " + p.getCategorie_produit() + "\n" +
                        "Statut : " + p.getStatus_produit() + "\n" +
                        "Description : " + p.getDescription_produit()
        );
        alert.showAndWait();
    }

    @FXML
    private void onNewProduit() {
        try {
            URL url = getClass().getResource("/AjouterProduit.fxml");
            if (url == null) throw new IOException("Fichier introuvable : /FrontFXML/AjouterProduit.fxml");

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage = (Stage) produitsListContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter Produit");
            stage.show();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Navigation",
                    "Impossible d'ouvrir AjouterProduit.fxml : " + e.getMessage());
        }
    }

    private void ouvrirPageModifier(Produit produit) {
        try {
            produitAmodifier = produit;

            URL url = getClass().getResource("/ModifierProduit.fxml");
            if (url == null) throw new IOException("Fichier introuvable : /FrontFXML/ModifierProduit.fxml");

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage;
            if (produitsListContainer != null) {
                stage = (Stage) produitsListContainer.getScene().getWindow();
            } else {
                stage = (Stage) btnResetProduit.getScene().getWindow();
            }

            stage.setScene(new Scene(root));
            stage.setTitle("Modifier Produit");
            stage.show();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Navigation",
                    "Impossible d'ouvrir ModifierProduit.fxml : " + e.getMessage());
        }
    }

    @FXML
    private void onVoirCommandes() {
        try {
            URL url = getClass().getResource("/MesCommandesBack.fxml");
            if (url == null) {
                throw new IOException("Fichier introuvable : /MesCommandesBack.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage = (Stage) produitsListContainer.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 850);

            URL css1 = getClass().getResource("/CSS/produit-dashboard.css");
            if (css1 != null) scene.getStylesheets().add(css1.toExternalForm());

            URL css2 = getClass().getResource("/CSS/commandes-back.css");
            if (css2 != null) scene.getStylesheets().add(css2.toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Gestion des commandes");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Navigation",
                    "Impossible d'ouvrir la page des commandes : " + e.getMessage());
        }
    }

    @FXML
    private void onLogout(ActionEvent event) {
        try {
            SessionManager.clear();

            URL url = getClass().getResource("/FrontFXML/Login.fxml");
            if (url == null) {
                throw new IOException("Fichier introuvable : /FrontFXML/Login.fxml");
            }

            Parent root = FXMLLoader.load(url);
            Stage stage = resolveCurrentStage(event);
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
    private void onGoToFront(ActionEvent event) {
        try {
            URL url = getClass().getResource("/FrontFXML/Accueil.fxml");
            if (url == null) {
                throw new IOException("Fichier introuvable : /FrontFXML/Accueil.fxml");
            }

            Parent root = FXMLLoader.load(url);
            Stage stage = resolveCurrentStage(event);
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

    private Stage resolveCurrentStage(ActionEvent event) {
        if (event != null && event.getSource() instanceof Node source && source.getScene() != null) {
            return (Stage) source.getScene().getWindow();
        }
        if (produitsListContainer != null && produitsListContainer.getScene() != null) {
            return (Stage) produitsListContainer.getScene().getWindow();
        }
        if (productGrid != null && productGrid.getScene() != null) {
            return (Stage) productGrid.getScene().getWindow();
        }
        return null;
    }

    @FXML
    private void onVoirTousProduits() {
        retourProduitSimple();
    }

    @FXML
    private void onTableauBordAdmin() {
        try {
            URL url = getClass().getResource("/DashboardAdminBI.fxml");
            if (url == null) {
                throw new IOException("Fichier introuvable : /DashboardAdminBI.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage = null;

            if (produitsListContainer != null && produitsListContainer.getScene() != null) {
                stage = (Stage) produitsListContainer.getScene().getWindow();
            } else if (btnAjouterProduit != null && btnAjouterProduit.getScene() != null) {
                stage = (Stage) btnAjouterProduit.getScene().getWindow();
            } else if (productGrid != null && productGrid.getScene() != null) {
                stage = (Stage) productGrid.getScene().getWindow();
            }

            if (stage == null) return;

            Scene scene = new Scene(root, 1400, 850);

            URL css = getClass().getResource("/CSS/dashboard_admin_bi.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("Dashboard Admin BI");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Navigation",
                    "Impossible d'ouvrir DashboardAdminBI.fxml : " + e.getMessage());
        }
    }

    @FXML
    private void onAutresActions() {
        showAlert(Alert.AlertType.INFORMATION, "Autres actions", "Ajoute ici tes autres actions.");
    }

    @FXML
    private void toggleSubmenu() {
        if (submenuVBox == null) return;

        boolean show = !submenuVBox.isVisible();
        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);

        if (produitArrow != null) {
            produitArrow.setText(show ? "⌃" : "⌄");
        }
    }

    @FXML
    void ajouterProduit(ActionEvent event) {
        if (!isNomValid || !isDescValid || !isPrixValid || !isQuantiteValid || !isCategorieValid || !isImageValid) {
            boolean allEmpty =
                    safe(tfNomProduit.getText()).trim().isEmpty() &&
                            safe(taDescriptionProduit.getText()).trim().isEmpty() &&
                            safe(tfPrixProduit.getText()).trim().isEmpty() &&
                            safe(tfQuantiteProduit.getText()).trim().isEmpty() &&
                            (cbCategorieProduit == null || cbCategorieProduit.getValue() == null || cbCategorieProduit.getValue().trim().isEmpty()) &&
                            (lblImageProduit == null || safe(lblImageProduit.getText()).trim().isEmpty() || "Aucun fichier choisi".equals(lblImageProduit.getText()));

            if (allEmpty) {
                showAlert(
                        Alert.AlertType.WARNING,
                        "Formulaire incomplet",
                        "Tous les champs obligatoires doivent être remplis avant d’ajouter un produit."
                );
            } else {
                StringBuilder errors = new StringBuilder("Veuillez corriger les champs suivants avant de continuer :\n\n");
                if (!isNomValid) errors.append("• Nom du produit\n");
                if (!isDescValid) errors.append("• Description\n");
                if (!isPrixValid) errors.append("• Prix\n");
                if (!isQuantiteValid) errors.append("• Quantité\n");
                if (!isCategorieValid) errors.append("• Catégorie\n");
                if (!isImageValid) errors.append("• Image\n");

                showAlert(
                        Alert.AlertType.ERROR,
                        "Champs à corriger",
                        errors.toString()
                );
            }
            return;
        }
        try {
            String nom = tfNomProduit.getText().trim();
            String description = taDescriptionProduit.getText().trim();
            double prix = Double.parseDouble(tfPrixProduit.getText().trim());
            int quantite = Integer.parseInt(tfQuantiteProduit.getText().trim());
            String categorie = cbCategorieProduit.getValue();
            String statut = getSelectedStatut();
            String image = imagePath.isEmpty() ? lblImageProduit.getText() : imagePath;

            Produit produit = new Produit(nom, description, prix, quantite, image, categorie, statut);


            // TEST D’UNICITÉ
            boolean existeDeja = produitService.produitExisteDeja(nom, categorie, prix, description);

            if (existeDeja) {
                showAlert(
                        Alert.AlertType.WARNING,
                        "Doublon détecté",
                        "Ce produit semble déjà exister dans votre inventaire.\n\n" +
                                "Vérifiez le nom, la catégorie, le prix et la description avant d’ajouter un nouveau produit."
                );
                return;
            }

            produitService.ajouter(produit);

            showDashboardToast("Produit ajouté avec succès !", "dashboard-toast-success", "✅");
            retourProduit(event);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout : " + e.getMessage());
        }
    }
    @FXML
    void modifierProduit(ActionEvent event) {
        if (produitAmodifier == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun produit sélectionné pour la modification.");
            return;
        }

        // Check validation
        if (!isNomValid || !isDescValid || !isPrixValid || !isQuantiteValid || !isCategorieValid || !isImageValid) {
            boolean allEmpty =
                    safe(tfNomProduit.getText()).trim().isEmpty() &&
                            safe(taDescriptionProduit.getText()).trim().isEmpty() &&
                            safe(tfPrixProduit.getText()).trim().isEmpty() &&
                            safe(tfQuantiteProduit.getText()).trim().isEmpty() &&
                            (cbCategorieProduit == null || cbCategorieProduit.getValue() == null || cbCategorieProduit.getValue().trim().isEmpty()) &&
                            (lblImageProduit == null || safe(lblImageProduit.getText()).trim().isEmpty() || "Aucun fichier choisi".equals(lblImageProduit.getText()));

            if (allEmpty) {
                showAlert(
                        Alert.AlertType.WARNING,
                        "Formulaire incomplet",
                        "Tous les champs obligatoires doivent être remplis avant de modifier le produit."
                );
            } else {
                StringBuilder errors = new StringBuilder("Veuillez corriger les champs suivants avant de continuer :\n\n");
                if (!isNomValid) errors.append("• Nom du produit\n");
                if (!isDescValid) errors.append("• Description\n");
                if (!isPrixValid) errors.append("• Prix\n");
                if (!isQuantiteValid) errors.append("• Quantité\n");
                if (!isCategorieValid) errors.append("• Catégorie\n");
                if (!isImageValid) errors.append("• Image\n");

                showAlert(
                        Alert.AlertType.ERROR,
                        "Champs à corriger",
                        errors.toString()
                );
            }
            return;
        }
        try {
            String nom = tfNomProduit.getText().trim();
            String description = taDescriptionProduit.getText().trim();
            double prix = Double.parseDouble(tfPrixProduit.getText().trim());
            int quantite = Integer.parseInt(tfQuantiteProduit.getText().trim());
            String categorie = cbCategorieProduit.getValue();
            String statut = getSelectedStatut();
            String image = imagePath.isEmpty() ? lblImageProduit.getText() : imagePath;

            produitAmodifier.setNom_produit(nom);
            produitAmodifier.setDescription_produit(description);
            produitAmodifier.setPrix_produit(prix);
            produitAmodifier.setQuantite_produit(quantite);
            produitAmodifier.setCategorie_produit(categorie);
            produitAmodifier.setStatus_produit(statut);
            produitAmodifier.setImage_produit(image);

            produitService.modifier(produitAmodifier);

            showDashboardToast("Produit modifié avec succès !", "dashboard-toast-info", "✏");
            retourProduit(event);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification : " + e.getMessage());
        }
    }

    private String getSelectedStatut() {
        if (rbDisponibleProduit != null && rbDisponibleProduit.isSelected()) return "Disponible";
        else if (rbRuptureProduit != null && rbRuptureProduit.isSelected()) return "Rupture";
        else return "Indisponible";
    }

    @FXML
    void choisirImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images (*.jpg, *.png, *.jpeg, *.webp)", "*.jpg", "*.png", "*.jpeg", "*.webp")
        );

        File fichier = fileChooser.showOpenDialog(btnUploadProduit.getScene().getWindow());

        if (fichier != null) {
            imagePath = fichier.getAbsolutePath();
            lblImageProduit.setText(fichier.getName());
        } else {
            lblImageProduit.setText("Aucun fichier choisi");
        }
    }

    @FXML
    void retourProduit(ActionEvent event) {
        retourProduitSimple();
    }



    private void retourProduitSimple() {
        try {
            produitAmodifier = null;

            URL url = getClass().getResource("/ProduitDashboard.fxml");
            if (url == null) {
                throw new IOException("Fichier introuvable : /ProduitDashboard.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage = null;

            if (biPeriodeLabel != null && biPeriodeLabel.getScene() != null) {
                stage = (Stage) biPeriodeLabel.getScene().getWindow();
            } else if (produitsListContainer != null && produitsListContainer.getScene() != null) {
                stage = (Stage) produitsListContainer.getScene().getWindow();
            } else if (btnResetProduit != null && btnResetProduit.getScene() != null) {
                stage = (Stage) btnResetProduit.getScene().getWindow();
            } else if (btnAjouterProduit != null && btnAjouterProduit.getScene() != null) {
                stage = (Stage) btnAjouterProduit.getScene().getWindow();
            } else if (btnModifierProduit != null && btnModifierProduit.getScene() != null) {
                stage = (Stage) btnModifierProduit.getScene().getWindow();
            } else if (productGrid != null && productGrid.getScene() != null) {
                stage = (Stage) productGrid.getScene().getWindow();
            }

            if (stage == null) return;

            Scene scene = new Scene(root, 1400, 850);

            URL css = getClass().getResource("/CSS/produit-dashboard.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des Produits");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Navigation",
                    "Impossible de retourner à la page produits : " + e.getMessage());
        }
    }

    @FXML
    public void viderChamps(ActionEvent event) {
        viderChampsPrivate();
    }

    private void viderChampsPrivate() {
        if (tfNomProduit != null) tfNomProduit.clear();
        if (taDescriptionProduit != null) taDescriptionProduit.clear();
        if (tfPrixProduit != null) tfPrixProduit.clear();
        if (tfQuantiteProduit != null) tfQuantiteProduit.clear();
        if (cbCategorieProduit != null) cbCategorieProduit.setValue("Médicament");
        if (lblImageProduit != null) lblImageProduit.setText("Aucun fichier choisi");
        imagePath = "";
        if (rbDisponibleProduit != null) rbDisponibleProduit.setSelected(true);
    }

    private void supprimerProduitAvecConfirmation(Produit produit) {
        boolean confirmed = showStyledConfirmationDialog(
                "🗑 Supprimer le produit",
                "Voulez-vous vraiment supprimer \"" + safe(produit.getNom_produit()) + "\" ?",
                "Supprimer",
                "Annuler",
                "danger"
        );

        if (!confirmed) return;

        try {
            produitService.supprimer(produit);
            masterList.remove(produit);
            applyFiltersAndRefresh();

            showDashboardToast("Produit supprimé avec succès.", "dashboard-toast-danger", "🗑");

        } catch (Exception e) {
            e.printStackTrace();
            showDashboardToast("Erreur lors de la suppression.", "dashboard-toast-danger", "✖");
        }
    }
    private void validateNom(String value) {
        String nom = value == null ? "" : value.trim();

        // Champ vide
        if (nom.isEmpty()) {
            tfNomProduit.setStyle("-fx-border-color: red;");
            nomMsg.setText("Nom du produit est obligatoire");
            nomMsg.setStyle("-fx-text-fill: red;");
            isNomValid = false;
            return;
        }

        // Minimum 3 caractères
        if (nom.length() < 3) {
            tfNomProduit.setStyle("-fx-border-color: red;");
            nomMsg.setText("Minimum 3 caractères sont requis pour le nom du produit");
            nomMsg.setStyle("-fx-text-fill: red;");
            isNomValid = false;
            return;
        }

        // 🔥 caractères autorisés seulement
        if (!nom.matches("^[a-zA-ZÀ-ÿ0-9 ]+$")) {
            tfNomProduit.setStyle("-fx-border-color: red;");
            nomMsg.setText("Caractères invalides (lettres et chiffres uniquement)");
            nomMsg.setStyle("-fx-text-fill: red;");
            isNomValid = false;
            return;
        }

        // Maximum
        if (nom.length() > 100) {
            tfNomProduit.setStyle("-fx-border-color: red;");
            nomMsg.setText("Maximum 100 caractères  sont retuis pour le nom du produit");
            nomMsg.setStyle("-fx-text-fill: red;");
            isNomValid = false;
            return;
        }

        // ✅ OK
        tfNomProduit.setStyle("-fx-border-color: green;");
        nomMsg.setText("Nom du produit valide");
        nomMsg.setStyle("-fx-text-fill: green;");
        isNomValid = true;
    }

    private void validateDescription(String value) {
        String desc = value == null ? "" : value.trim();

        if (desc.isEmpty()) {
            taDescriptionProduit.setStyle("-fx-border-color: red;");
            descMsg.setText("Description du produit est obligatoire");
            descMsg.setStyle("-fx-text-fill: red;");
            isDescValid = false;
            return;
        }

        if (desc.length() < 10) {
            taDescriptionProduit.setStyle("-fx-border-color: red;");
            descMsg.setText("La description doit contenir au moins 10 caractères");
            descMsg.setStyle("-fx-text-fill: red;");
            isDescValid = false;
            return;
        }

        if (desc.length() > 500) {
            taDescriptionProduit.setStyle("-fx-border-color: red;");
            descMsg.setText("La description ne doit pas dépasser 500 caractères");
            descMsg.setStyle("-fx-text-fill: red;");
            isDescValid = false;
            return;
        }

        taDescriptionProduit.setStyle("-fx-border-color: green;");
        descMsg.setText("Description du produit valide");
        descMsg.setStyle("-fx-text-fill: green;");
        isDescValid = true;
    }
    private void validatePrix(String value) {
        String prixTexte = value == null ? "" : value.trim();

        if (prixTexte.isEmpty()) {
            tfPrixProduit.setStyle("-fx-border-color: red;");
            prixMsg.setText("Prix est obligatoire");
            prixMsg.setStyle("-fx-text-fill: red;");
            isPrixValid = false;
            return;
        }

        if (!prixTexte.matches("^\\d+(\\.\\d{1,2})?$")) {
            tfPrixProduit.setStyle("-fx-border-color: red;");
            prixMsg.setText("Le prix doit être un nombre valide (max 2 décimales)");
            prixMsg.setStyle("-fx-text-fill: red;");
            isPrixValid = false;
            return;
        }

        try {
            double prix = Double.parseDouble(prixTexte);

            if (prix <= 0) {
                tfPrixProduit.setStyle("-fx-border-color: red;");
                prixMsg.setText("Le prix doit être supérieur à 0");
                prixMsg.setStyle("-fx-text-fill: red;");
                isPrixValid = false;
                return;
            }

            if (prix > 9999) {
                tfPrixProduit.setStyle("-fx-border-color: red;");
                prixMsg.setText("Le prix ne doit pas dépasser 9999 DT");
                prixMsg.setStyle("-fx-text-fill: red;");
                isPrixValid = false;
                return;
            }

            tfPrixProduit.setStyle("-fx-border-color: green;");
            prixMsg.setText("Prix valide");
            prixMsg.setStyle("-fx-text-fill: green;");
            isPrixValid = true;

        } catch (NumberFormatException e) {
            tfPrixProduit.setStyle("-fx-border-color: red;");
            prixMsg.setText("Le prix est invalide");
            prixMsg.setStyle("-fx-text-fill: red;");
            isPrixValid = false;
        }
    }
    private void validateQuantite(String value) {
        String quantiteTexte = value == null ? "" : value.trim();

        if (quantiteTexte.isEmpty()) {
            tfQuantiteProduit.setStyle("-fx-border-color: red;");
            quantiteMsg.setText("Quantité est obligatoire");
            quantiteMsg.setStyle("-fx-text-fill: red;");
            isQuantiteValid = false;
            return;
        }

        if (!quantiteTexte.matches("^\\d+$")) {
            tfQuantiteProduit.setStyle("-fx-border-color: red;");
            quantiteMsg.setText("La quantité doit être un nombre entier");
            quantiteMsg.setStyle("-fx-text-fill: red;");
            isQuantiteValid = false;
            return;
        }

        try {
            int quantite = Integer.parseInt(quantiteTexte);

            if (quantite < 0) {
                tfQuantiteProduit.setStyle("-fx-border-color: red;");
                quantiteMsg.setText("La quantité ne peut pas être négative");
                quantiteMsg.setStyle("-fx-text-fill: red;");
                isQuantiteValid = false;
                return;
            }

            if (quantite > 100000) {
                tfQuantiteProduit.setStyle("-fx-border-color: red;");
                quantiteMsg.setText("La quantité est trop grande");
                quantiteMsg.setStyle("-fx-text-fill: red;");
                isQuantiteValid = false;
                return;
            }

            tfQuantiteProduit.setStyle("-fx-border-color: green;");
            quantiteMsg.setText("Quantité valide");
            quantiteMsg.setStyle("-fx-text-fill: green;");
            isQuantiteValid = true;

        } catch (NumberFormatException e) {
            tfQuantiteProduit.setStyle("-fx-border-color: red;");
            quantiteMsg.setText("La quantité est invalide");
            quantiteMsg.setStyle("-fx-text-fill: red;");
            isQuantiteValid = false;
        }
    }

    private void validateCategorie(String value) {
        String categorie = value == null ? "" : value.trim();

        if (categorie.isEmpty()) {
            cbCategorieProduit.setStyle("-fx-border-color: red;");
            categorieMsg.setText("Catégorie est obligatoire");
            categorieMsg.setStyle("-fx-text-fill: red;");
            isCategorieValid = false;
            return;
        }

        cbCategorieProduit.setStyle("-fx-border-color: green;");
        categorieMsg.setText("Catégorie valide");
        categorieMsg.setStyle("-fx-text-fill: green;");
        isCategorieValid = true;
    }
    private void validateImage(String value) {
        String imageValeur = value == null ? "" : value.trim();

        if (imageValeur.isEmpty() || "Aucun fichier choisi".equals(imageValeur)) {
            lblImageProduit.getParent().setStyle("-fx-border-color: red;");
            imageMsg.setText("Image du produit est requise");
            imageMsg.setStyle("-fx-text-fill: red;");
            isImageValid = false;
            return;
        }

        String lower = imageValeur.toLowerCase();
        if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp"))) {
            lblImageProduit.getParent().setStyle("-fx-border-color: red;");
            imageMsg.setText("Format image invalide (jpg, jpeg, png, webp)");
            imageMsg.setStyle("-fx-text-fill: red;");
            isImageValid = false;
            return;
        }

        lblImageProduit.getParent().setStyle("-fx-border-color: green;");
        imageMsg.setText("Image du produit valide");
        imageMsg.setStyle("-fx-text-fill: green;");
        isImageValid = true;
    }

    private boolean contains(String value, String keyword) {
        return safe(value).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String shortText(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max) + "...";
    }

    private String formatPrix(double prix) {
        return String.format(Locale.US, "%.2f DT", prix);
    }

    private String getStatutIcon(String statut) {
        String s = safe(statut).toLowerCase(Locale.ROOT);
        if (s.contains("rupture")) return "⚠";
        if (s.contains("indisponible")) return "✖";
        return "✔";
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        String variant = "info";

        if (type == Alert.AlertType.ERROR) {
            variant = "danger";
        } else if (type == Alert.AlertType.WARNING) {
            variant = "warning";
        } else if (type == Alert.AlertType.INFORMATION) {
            variant = "success";
        }

        showStyledMessageDialog(title, content, variant);
    }

    private void showStyledMessageDialog(String title, String message, String variant) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.OK);

        URL cssUrl = getClass().getResource("/CSS/produit-dashboard.css");
        if (cssUrl != null) {
            pane.getStylesheets().add(cssUrl.toExternalForm());
        }
        pane.getStyleClass().add("confirm-dialog");

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));

        String iconText = "ℹ";
        if ("danger".equals(variant)) iconText = "⚠";
        else if ("warning".equals(variant)) iconText = "⚠";
        else if ("success".equals(variant)) iconText = "✅";

        Label icon = new Label(iconText);
        icon.getStyleClass().add("confirm-dialog-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("confirm-dialog-title");
        titleLabel.setWrapText(true);

        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("confirm-dialog-message");
        msgLabel.setWrapText(true);

        content.getChildren().addAll(icon, titleLabel, msgLabel);
        pane.setContent(content);

        Button okButton = (Button) pane.lookupButton(ButtonType.OK);
        okButton.setText("Compris");

        if ("danger".equals(variant)) {
            okButton.getStyleClass().add("confirm-danger-btn");
        } else if ("warning".equals(variant)) {
            okButton.getStyleClass().add("confirm-warning-btn");
        } else {
            okButton.getStyleClass().add("confirm-success-btn");
        }

        dialog.showAndWait();
    }
    @FXML
    private void ouvrirMesCommandes() {
        try {
            URL url = getClass().getResource("/FrontFXML/MesCommandes.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage = (Stage) productGrid.getScene().getWindow();
            //stage.setScene(new Scene(root));
            stage.setScene(new Scene(root, 1400, 850));
            stage.setTitle("Mes commandes");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void ouvrirContactPharmaciePopup() {
        try {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Contacter la pharmacie");
            dialog.setHeaderText(null);
            dialog.setResizable(false);

            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getButtonTypes().add(ButtonType.CLOSE);

            URL cssUrl = getClass().getResource("/CSS/pharmacie.css");
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            }

            VBox content = new VBox(16);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(18));
            content.setMinWidth(540);
            content.getStyleClass().add("contact-popup");

            Label title = new Label("📇 Contact rapide de la pharmacie");
            title.getStyleClass().add("contact-popup-title");
            title.setWrapText(true);
            title.setAlignment(Pos.CENTER);

            Label subtitle = new Label("Scannez le QR code pour enregistrer le contact dans votre téléphone.");
            subtitle.getStyleClass().add("contact-popup-subtitle");
            subtitle.setWrapText(true);
            subtitle.setAlignment(Pos.CENTER);

            ImageView qrView = new ImageView(SwingFXUtils.toFXImage(createContactQrImage(buildPharmacyVCard(), 240, 240), null));
            qrView.setFitWidth(240);
            qrView.setFitHeight(240);
            qrView.setPreserveRatio(true);

            Label qrCaption = new Label("📱 Ouvrez l’app appareil photo pour scanner");
            qrCaption.getStyleClass().add("contact-qr-caption");

            VBox qrBox = new VBox(10, qrView, qrCaption);
            qrBox.setAlignment(Pos.CENTER);
            qrBox.getStyleClass().add("contact-qr-box");

            GridPane infoGrid = new GridPane();
            infoGrid.setHgap(12);
            infoGrid.setVgap(10);
            infoGrid.setPadding(new Insets(4, 6, 4, 6));

            infoGrid.add(createContactLabel("Nom"), 0, 0);
            infoGrid.add(createContactValue(PHARMACY_NAME), 1, 0);
            infoGrid.add(createContactLabel("Téléphone"), 0, 1);
            infoGrid.add(createContactValue(PHARMACY_PHONE), 1, 1);
            infoGrid.add(createContactLabel("Email"), 0, 2);
            infoGrid.add(createContactValue(PHARMACY_EMAIL), 1, 2);
            infoGrid.add(createContactLabel("Adresse"), 0, 3);
            infoGrid.add(createContactValue(PHARMACY_ADDRESS), 1, 3);

            ColumnConstraints labelCol = new ColumnConstraints();
            labelCol.setMinWidth(110);
            ColumnConstraints valueCol = new ColumnConstraints();
            valueCol.setHgrow(Priority.ALWAYS);
            infoGrid.getColumnConstraints().addAll(labelCol, valueCol);

            VBox infoCard = new VBox(infoGrid);
            infoCard.getStyleClass().add("contact-info-card");

            content.getChildren().addAll(title, subtitle, qrBox, infoCard);
            dialogPane.setContent(content);
            dialogPane.setPrefWidth(600);
            dialogPane.setPrefHeight(620);

            Button closeButton = (Button) dialogPane.lookupButton(ButtonType.CLOSE);
            closeButton.setText("Fermer");
            closeButton.getStyleClass().add("contact-close-btn");
            closeButton.setDefaultButton(true);

            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur contact", "Impossible d'afficher le contact pharmacie : " + e.getMessage());
        }
    }

    private Label createContactLabel(String text) {
        Label label = new Label(text + " :");
        label.getStyleClass().add("contact-info-label");
        return label;
    }

    private Label createContactValue(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("contact-info-value");
        return label;
    }

    private String buildPharmacyVCard() {
        return "BEGIN:VCARD\n" +
                "VERSION:3.0\n" +
                "FN:" + PHARMACY_NAME + "\n" +
                "ORG:MedFlow\n" +
                "TEL;TYPE=CELL:" + PHARMACY_PHONE + "\n" +
                "EMAIL;TYPE=WORK:" + PHARMACY_EMAIL + "\n" +
                "ADR;TYPE=WORK:;;" + PHARMACY_ADDRESS + "\n" +
                "END:VCARD";
    }

    private BufferedImage createContactQrImage(String content, int width, int height) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    private void showSuccessAlert(String produitName, int quantity) {
        Platform.runLater(() -> {
            String text = quantity == 1
                    ? produitName + " ajouté au panier"
                    : quantity + " x " + produitName + " ajoutés au panier";

            showFloatingToast(text, "toast-success", "✅");
        });
    }
    private void checkStockAndShowAlert(Produit produit, int quantityToAdd) {
        int availableStock = produit.getQuantite_produit();
        int currentQty = CartSession.getQuantiteProduit(produit);

        if (currentQty + quantityToAdd > availableStock) {
            // Show red alert for stock insufficiency
            showStockInsufficiencyAlert(produit.getNom_produit(), availableStock - currentQty);
        } else {
            // Add to cart
            for (int i = 0; i < quantityToAdd; i++) {
                CartSession.ajouterProduit(produit);
            }
            showSuccessAlert(produit.getNom_produit(), quantityToAdd);
        }
    }

    private void showStockInsufficiencyAlert(String produitName, int availableStock) {
        Platform.runLater(() -> {
            String text = "⚠ Stock insuffisant !\n" + produitName +
                    " disponible : " + availableStock + " unités";
            showFloatingToast(text, "toast-warning", "⚠");
        });
    }

    private void showFloatingToast(String message, String styleClass, String iconText) {
        if (btnPanier == null) return;

        Scene scene = btnPanier.getScene();
        if (scene == null) return;

        Window window = scene.getWindow();
        if (window == null) return;

        HBox toast = new HBox(10);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getStyleClass().addAll("floating-toast", styleClass);
        toast.setMaxWidth(340);

        Label icon = new Label(iconText);
        icon.getStyleClass().add("toast-icon");

        Label textLabel = new Label(message);
        textLabel.getStyleClass().add("toast-text");
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(280);

        toast.getChildren().addAll(icon, textLabel);

        Popup popup = new Popup();
        popup.getContent().add(toast);
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
        popup.setAutoFix(true);

        double x = window.getX() + scene.getWidth() - 380;
        double y = window.getY() + scene.getHeight() - 120;

        popup.show(window, x, y);

        // ✅ très important : charger le CSS sur la scène du popup
        URL cssUrl = getClass().getResource("/CSS/pharmacie.css");
        if (cssUrl != null && popup.getScene() != null) {
            popup.getScene().getStylesheets().add(cssUrl.toExternalForm());
        }

        toast.applyCss();
        toast.layout();

        toast.setOpacity(0);
        toast.setTranslateY(18);
        icon.setScaleX(0.7);
        icon.setScaleY(0.7);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        javafx.animation.TranslateTransition slideIn =
                new javafx.animation.TranslateTransition(Duration.millis(220), toast);
        slideIn.setFromY(18);
        slideIn.setToY(0);

        javafx.animation.ScaleTransition popIcon =
                new javafx.animation.ScaleTransition(Duration.millis(260), icon);
        popIcon.setFromX(0.7);
        popIcon.setFromY(0.7);
        popIcon.setToX(1.0);
        popIcon.setToY(1.0);

        javafx.animation.ParallelTransition enter =
                new javafx.animation.ParallelTransition(fadeIn, slideIn, popIcon);

        PauseTransition pause = new PauseTransition(Duration.seconds(2.2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        javafx.animation.TranslateTransition slideOut =
                new javafx.animation.TranslateTransition(Duration.millis(220), toast);
        slideOut.setFromY(0);
        slideOut.setToY(10);

        javafx.animation.ParallelTransition exit =
                new javafx.animation.ParallelTransition(fadeOut, slideOut);

        javafx.animation.SequentialTransition sequence =
                new javafx.animation.SequentialTransition(enter, pause, exit);

        sequence.setOnFinished(e -> popup.hide());
        sequence.play();
    }
    // =========================================================
    // DASHBOARD ADMIN BI
    // =========================================================
    private void initDashboardBIIfExists() {
        if (biPeriodeLabel == null) return;

        if (biDateFinPicker != null && biDateFinPicker.getValue() == null) {
            biDateFinPicker.setValue(LocalDate.now());
        }

        if (biDateDebutPicker != null && biDateDebutPicker.getValue() == null) {
            biDateDebutPicker.setValue(LocalDate.now().minusDays(6));
        }

        if (biCategorieCombo != null && biCategorieCombo.getItems().isEmpty()) {
            biCategorieCombo.getItems().addAll(
                    "Toutes les catégories",
                    "Médicament",
                    "Parapharmacie",
                    "Matériel",
                    "Vitamines",
                    "Beauté & Cosmétique"
            );
            biCategorieCombo.setValue("Toutes les catégories");
        }

        initTopProduitsTable();
        initStockCritiqueTable();
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
            String categorie = biCategorieCombo != null ? biCategorieCombo.getValue() : "Toutes les catégories";

            DashboardBIService.DashboardData data = dashboardBIService.chargerDonnees(debut, fin, categorie);

            if (biPeriodeLabel != null) {
                biPeriodeLabel.setText("Période : " + data.dateDebut + " → " + data.dateFin);
            }

            if (biCroissanceLabel != null) biCroissanceLabel.setText(data.croissanceMessage);
            if (biStockCritiqueLabel != null) biStockCritiqueLabel.setText(data.stockCritiqueMessage);

            if (biCaLabel != null) biCaLabel.setText(formatNombreBI(data.chiffreAffaires) + " DT");
            if (biCommandesLabel != null) biCommandesLabel.setText(String.valueOf(data.totalCommandes));
            if (biPanierMoyenLabel != null) biPanierMoyenLabel.setText(formatNombreBI(data.panierMoyen));
            if (biConversionLabel != null) biConversionLabel.setText(String.format(Locale.US, "%.1f%%", data.tauxConversion));

            if (biQuantiteVendueLabel != null) biQuantiteVendueLabel.setText(String.valueOf(data.quantiteVendue));
            if (biEnAttenteLabel != null) biEnAttenteLabel.setText(String.valueOf(data.enAttente));
            if (biTauxRuptureLabel != null) biTauxRuptureLabel.setText(String.format(Locale.US, "%.1f%%", data.tauxRupture));
            if (biPeriodeAnalyseeLabel != null) biPeriodeAnalyseeLabel.setText(String.valueOf(data.periodeAnalysee));

            remplirLineChartBI(data);
            remplirPieChartBI(data);
            remplirCategorieChartBI(data);
            remplirCaCategorieChartBI(data);

            if (topProduitsTable != null) {
                topProduitsTable.setItems(FXCollections.observableArrayList(data.topProduits));
            }

            if (stockCritiqueTable != null) {
                stockCritiqueTable.setItems(FXCollections.observableArrayList(data.stockCritiqueProduits));
            }

            remplirRecommandationsBI(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTopProduitsTable() {
        if (topProduitsTable == null) return;

        if (topNomCol != null) {
            topNomCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getNom_produit())));
        }
        if (topCategorieCol != null) {
            topCategorieCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getCategorie_produit())));
        }
        if (topQteCol != null) {
            topQteCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantite_produit()));
        }
        if (topCaCol != null) {
            topCaCol.setCellValueFactory(cell ->
                    new SimpleStringProperty(formatNombreBI(cell.getValue().getPrix_produit() * cell.getValue().getQuantite_produit())));
        }
        if (topStockCol != null) {
            topStockCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantite_produit()));
        }
    }

    private void initStockCritiqueTable() {
        if (stockCritiqueTable == null) return;

        if (stockNomCol != null) {
            stockNomCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getNom_produit())));
        }
        if (stockCategorieCol != null) {
            stockCategorieCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getCategorie_produit())));
        }
        if (stockStockCol != null) {
            stockStockCol.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getQuantite_produit()));
        }
        if (stockStatutCol != null) {
            stockStatutCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getStatus_produit())));
        }
    }

    private void remplirLineChartBI(DashboardBIService.DashboardData data) {
        if (ventesLineChart == null) return;

        ventesLineChart.getData().clear();

        XYChart.Series<String, Number> serieCA = new XYChart.Series<>();
        serieCA.setName("CA (DT)");

        XYChart.Series<String, Number> serieQte = new XYChart.Series<>();
        serieQte.setName("Quantités");

        for (Map.Entry<java.time.LocalDate, Double> entry : data.caParJour.entrySet()) {
            serieCA.getData().add(new XYChart.Data<>(entry.getKey().toString(), entry.getValue()));
        }

        for (Map.Entry<java.time.LocalDate, Integer> entry : data.quantitesParJour.entrySet()) {
            serieQte.getData().add(new XYChart.Data<>(entry.getKey().toString(), entry.getValue()));
        }

        ventesLineChart.getData().addAll(serieCA, serieQte);
    }

    private void remplirPieChartBI(DashboardBIService.DashboardData data) {
        if (statutPieChart == null) return;

        statutPieChart.getData().clear();

        for (Map.Entry<String, Integer> entry : data.repartitionStatuts.entrySet()) {
            statutPieChart.getData().add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
    }

    private void remplirCategorieChartBI(DashboardBIService.DashboardData data) {
        if (categorieBarChart == null) return;

        categorieBarChart.getData().clear();

        XYChart.Series<String, Number> serieQte = new XYChart.Series<>();
        serieQte.setName("Quantité");

        XYChart.Series<String, Number> serieCA = new XYChart.Series<>();
        serieCA.setName("CA (DT)");

        for (Map.Entry<String, Integer> entry : data.quantitesParCategorie.entrySet()) {
            serieQte.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        for (Map.Entry<String, Double> entry : data.caParCategorie.entrySet()) {
            serieCA.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        categorieBarChart.getData().addAll(serieQte, serieCA);
    }

    private void remplirCaCategorieChartBI(DashboardBIService.DashboardData data) {
        if (caCategorieBarChart == null) return;

        caCategorieBarChart.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Chiffre d'affaires");

        for (Map.Entry<String, Double> entry : data.caParCategorie.entrySet()) {
            serie.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        caCategorieBarChart.getData().add(serie);
    }

    private void remplirRecommandationsBI(DashboardBIService.DashboardData data) {
        if (recommandationsContainer == null) return;

        recommandationsContainer.getChildren().clear();

        for (String rec : data.recommandations) {
            Label lbl = new Label("💡 " + rec);
            lbl.getStyleClass().add("recommendation-chip");
            recommandationsContainer.getChildren().add(lbl);
        }
    }

    private String formatNombreBI(double value) {
        return String.format(Locale.FRANCE, "%,.2f", value);
    }

    private void showProduitDetailsPopup(Produit p) {
        if (p == null) return;

        try {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détails du produit");
            dialog.setHeaderText(null);
            dialog.setResizable(false);

            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.getButtonTypes().add(ButtonType.CLOSE);

            URL cssUrl = getClass().getResource("/CSS/pharmacie.css");
            if (cssUrl != null) {
                dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            }

            dialogPane.getStyleClass().add("product-details-dialog");
            dialogPane.setStyle("-fx-background-color: transparent;");

            VBox content = new VBox(14);
            content.setPadding(new Insets(18));
            content.setAlignment(Pos.TOP_CENTER);
            content.getStyleClass().add("product-details-popup");

            StackPane imageWrapper = new StackPane();
            imageWrapper.setPrefSize(220, 180);
            imageWrapper.getStyleClass().add("product-image-container");

            String imgPath = safe(p.getImage_produit());
            if (!imgPath.isEmpty()) {
                try {
                    File file = new File(imgPath);
                    if (file.exists()) {
                        ImageView imageView = new ImageView(new Image(file.toURI().toString()));
                        imageView.setFitWidth(150);
                        imageView.setFitHeight(150);
                        imageView.setPreserveRatio(true);
                        imageView.getStyleClass().add("popup-product-image");
                        imageWrapper.getChildren().add(imageView);
                    } else {
                        Label placeholder = new Label("🖼");
                        placeholder.getStyleClass().add("image-placeholder");
                        imageWrapper.getChildren().add(placeholder);
                    }
                } catch (Exception e) {
                    Label placeholder = new Label("🖼");
                    placeholder.getStyleClass().add("image-placeholder");
                    imageWrapper.getChildren().add(placeholder);
                }
            } else {
                Label placeholder = new Label("🖼");
                placeholder.getStyleClass().add("image-placeholder");
                imageWrapper.getChildren().add(placeholder);
            }

            HBox badgesRow = new HBox(10);
            badgesRow.setAlignment(Pos.CENTER);

            Label prixBadge = new Label("💰 " + formatPrix(p.getPrix_produit()));
            prixBadge.getStyleClass().add("popup-price-badge");

            Label stockBadge = new Label("📦 Stock : " + p.getQuantite_produit());
            stockBadge.getStyleClass().add("popup-stock-badge");

            badgesRow.getChildren().addAll(prixBadge, stockBadge);

            Label nameLabel = new Label(safe(p.getNom_produit()));
            nameLabel.getStyleClass().add("product-name-popup");
            nameLabel.setWrapText(true);
            nameLabel.setAlignment(Pos.CENTER);

            Button voiceBtn = new Button("🔊");
            voiceBtn.setTooltip(new Tooltip("Lire tous les détails du produit"));
            voiceBtn.getStyleClass().add("popup-voice-btn");
            voiceBtn.setOnAction(e -> lireDetailsProduit(p));

            HBox titleRow = new HBox(12);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            titleRow.getChildren().addAll(nameLabel, voiceBtn);

            HBox tagsRow = new HBox(10);
            tagsRow.setAlignment(Pos.CENTER);

            Label categorieTag = new Label("🏷 " + safe(p.getCategorie_produit()));
            categorieTag.getStyleClass().add("popup-category-tag");

            Label statusTag = new Label(getStatutIcon(p.getStatus_produit()) + " " + safe(p.getStatus_produit()));
            String statut = safe(p.getStatus_produit()).toLowerCase(Locale.ROOT);
            if (statut.contains("rupture")) {
                statusTag.getStyleClass().add("popup-status-ko");
            } else if (statut.contains("indisponible")) {
                statusTag.getStyleClass().add("popup-status-warn");
            } else {
                statusTag.getStyleClass().add("popup-status-ok");
            }

            tagsRow.getChildren().addAll(categorieTag, statusTag);

            VBox detailsCard = new VBox(10);
            detailsCard.getStyleClass().add("popup-details-card");

            detailsCard.getChildren().addAll(
                    createInfoRow("Nom", safe(p.getNom_produit())),
                    createInfoRow("Catégorie", safe(p.getCategorie_produit())),
                    createInfoRow("Prix", formatPrix(p.getPrix_produit())),
                    createInfoRow("Quantité", String.valueOf(p.getQuantite_produit())),
                    createInfoRow("Statut", safe(p.getStatus_produit()))
            );

            VBox descCard = new VBox(8);
            descCard.getStyleClass().add("popup-description-card");

            Label descTitle = new Label("📝 Description");
            descTitle.getStyleClass().add("description-title");

            Label descText = new Label(
                    safe(p.getDescription_produit()).isEmpty()
                            ? "Aucune description disponible."
                            : safe(p.getDescription_produit())
            );
            descText.setWrapText(true);
            descText.getStyleClass().add("popup-description-text");

            descCard.getChildren().addAll(descTitle, descText);

            content.getChildren().addAll(
                    imageWrapper,
                    badgesRow,
                    titleRow,
                    tagsRow,
                    detailsCard,
                    descCard
            );

            dialogPane.setContent(content);
            dialogPane.setPrefWidth(520);
            dialogPane.setPrefHeight(640);
            dialogPane.setMaxHeight(640);

            Button closeButton = (Button) dialogPane.lookupButton(ButtonType.CLOSE);
            closeButton.setText("Fermer");
            closeButton.getStyleClass().add("popup-close-btn");
            closeButton.setDefaultButton(true);

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur popup", "Impossible d'afficher les détails : " + e.getMessage());
        }
    }

    private void lireDetailsProduit(Produit produit) {
        if (produit == null) return;

        String texte = construireTexteLectureProduit(produit);
        produitSpeechService.parlerAsync(texte);
    }

    private String construireTexteLectureProduit(Produit produit) {
        String description = safe(produit.getDescription_produit()).isBlank()
                ? "Aucune description disponible"
                : safe(produit.getDescription_produit());

        return "Produit " + safe(produit.getNom_produit()) + ". " +
                "Catégorie " + safe(produit.getCategorie_produit()) + ". " +
                "Prix " + formatPrix(produit.getPrix_produit()) + ". " +
                "Quantité en stock " + produit.getQuantite_produit() + ". " +
                "Statut " + safe(produit.getStatus_produit()) + ". " +
                "Description " + description + ".";
    }

    private boolean showStyledConfirmationDialog(String title, String message, String confirmText, String cancelText, String variant) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        URL cssUrl = getClass().getResource("/CSS/produit-dashboard.css");
        if (cssUrl != null) {
            pane.getStylesheets().add(cssUrl.toExternalForm());
        }
        pane.getStyleClass().add("confirm-dialog");

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));

        Label icon = new Label("danger".equals(variant) ? "⚠" : "✅");
        icon.getStyleClass().add("confirm-dialog-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("confirm-dialog-title");
        titleLabel.setWrapText(true);

        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("confirm-dialog-message");
        msgLabel.setWrapText(true);

        content.getChildren().addAll(icon, titleLabel, msgLabel);
        pane.setContent(content);

        Button okButton = (Button) pane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) pane.lookupButton(ButtonType.CANCEL);

        okButton.setText(confirmText);
        cancelButton.setText(cancelText);

        okButton.getStyleClass().add(
                "danger".equals(variant) ? "confirm-danger-btn" : "confirm-success-btn"
        );
        cancelButton.getStyleClass().add("confirm-cancel-btn");

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showDashboardToast(String message, String styleClass, String iconText) {
        Node anchor = null;

        if (produitsListContainer != null && produitsListContainer.getScene() != null) {
            anchor = produitsListContainer;
        } else if (btnAjouterProduit != null && btnAjouterProduit.getScene() != null) {
            anchor = btnAjouterProduit;
        } else if (btnModifierProduit != null && btnModifierProduit.getScene() != null) {
            anchor = btnModifierProduit;
        }

        if (anchor == null) return;

        Scene scene = anchor.getScene();
        if (scene == null) return;

        javafx.stage.Window window = scene.getWindow();
        if (window == null) return;

        HBox toast = new HBox(10);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getStyleClass().addAll("floating-toast", styleClass);
        toast.setMaxWidth(360);

        Label icon = new Label(iconText);
        icon.getStyleClass().add("toast-icon");

        Label textLabel = new Label(message);
        textLabel.getStyleClass().add("toast-text");
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(290);

        toast.getChildren().addAll(icon, textLabel);

        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.getContent().add(toast);
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
        popup.setAutoFix(true);

        popup.show(window);

        URL cssUrl = getClass().getResource("/CSS/produit-dashboard.css");
        if (cssUrl != null && popup.getScene() != null) {
            popup.getScene().getStylesheets().add(cssUrl.toExternalForm());
        }

        toast.applyCss();
        toast.layout();

        double popupWidth = Math.max(320, toast.prefWidth(-1));
        double x = window.getX() + scene.getWidth() - popupWidth - 24;
        double y = window.getY() + scene.getHeight() - 110;

        popup.setX(x);
        popup.setY(y);

        toast.setOpacity(0);
        toast.setTranslateY(16);
        icon.setScaleX(0.7);
        icon.setScaleY(0.7);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        javafx.animation.TranslateTransition slideIn =
                new javafx.animation.TranslateTransition(Duration.millis(220), toast);
        slideIn.setFromY(16);
        slideIn.setToY(0);

        javafx.animation.ScaleTransition popIcon =
                new javafx.animation.ScaleTransition(Duration.millis(260), icon);
        popIcon.setFromX(0.7);
        popIcon.setFromY(0.7);
        popIcon.setToX(1.0);
        popIcon.setToY(1.0);

        javafx.animation.ParallelTransition enter =
                new javafx.animation.ParallelTransition(fadeIn, slideIn, popIcon);

        PauseTransition pause = new PauseTransition(Duration.seconds(2.0));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        javafx.animation.TranslateTransition slideOut =
                new javafx.animation.TranslateTransition(Duration.millis(220), toast);
        slideOut.setFromY(0);
        slideOut.setToY(10);

        javafx.animation.ParallelTransition exit =
                new javafx.animation.ParallelTransition(fadeOut, slideOut);

        javafx.animation.SequentialTransition sequence =
                new javafx.animation.SequentialTransition(enter, pause, exit);

        sequence.setOnFinished(e -> popup.hide());
        sequence.play();
    }
// ─────────────────────────────────────────────────────────────────────────
// ── 3. AJOUTER cette nouvelle méthode dans ProduitController ─────────────
// ─────────────────────────────────────────────────────────────────────────

    /**
     * Charge les recommandations IA en arrière-plan (thread séparé)
     * et met à jour l'UI sur le JavaFX thread.
     */
    private void chargerRecommandationsIA() {
        // Récupérer l'ID du user connecté
        tn.esprit.entities.User user = SessionManager.getCurrentUser();
        final int userId = (user != null && user.getId() > 0) ? user.getId() : 0;

        // Afficher un état de chargement dans le FlowPane recommandations
        // (si tu as un recommandationsContainer dans Pharmacie.fxml, sinon skip)
        // Pour la pharmacie on injecte directement les cartes dans productGrid section

        // Lancer en arrière-plan pour ne pas bloquer l'UI
        Thread thread = new Thread(() -> {
            System.out.println("[Pharmacie] Chargement recommandations IA pour user " + userId);

            List<String> recommandationsTexte;
            List<tn.esprit.entities.Produit> produitsBestSellers;

            try {
                // Appel IA (peut prendre quelques secondes)
                recommandationsTexte = huggingFaceService.genererRecommandations(userId);
                // Best sellers complets avec objets Produit pour les cartes
                produitsBestSellers  = huggingFaceService.getProduitsBestSellersComplets(4);
            } catch (Exception e) {
                System.err.println("[Pharmacie] Erreur IA : " + e.getMessage());
                recommandationsTexte = List.of(
                        "💊 Recommandations en cours de chargement...",
                        "🔥 Découvrez nos best sellers"
                );
                produitsBestSellers = new ArrayList<>();
            }

            final List<String>   finalRecoTexte    = recommandationsTexte;
            final List<tn.esprit.entities.Produit> finalBestSellers = produitsBestSellers;

            // Mettre à jour l'UI sur le JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                afficherRecommandationsIA(finalRecoTexte, finalBestSellers);
            });

        }, "huggingface-reco-thread");
        thread.setDaemon(true);
        thread.start();
    }

// ─────────────────────────────────────────────────────────────────────────
// ── 4. AJOUTER cette méthode d'affichage ─────────────────────────────────
// ─────────────────────────────────────────────────────────────────────────

    /**
     * Affiche les recommandations IA dans une section dédiée
     * au-dessus du productGrid.
     */
    private void afficherRecommandationsIA(
            List<String> recommandationsTexte,
            List<tn.esprit.entities.Produit> bestSellers) {

        if (productGrid == null) return;

        // Récupérer le parent ScrollPane content VBox
        javafx.scene.Parent parent = productGrid.getParent();
        if (!(parent instanceof javafx.scene.layout.VBox mainVBox)) return;

        // Supprimer l'ancienne section recommandations si elle existe
        mainVBox.getChildren().removeIf(n -> "reco-section".equals(n.getId()));

        // ── Construire la section recommandations ─────────────────────────────
        javafx.scene.layout.VBox recoSection = new javafx.scene.layout.VBox(14);
        recoSection.setId("reco-section");
        recoSection.getStyleClass().add("reco-section");
        recoSection.setPadding(new javafx.geometry.Insets(0, 0, 8, 0));

        // Titre section
        javafx.scene.layout.HBox titreBox = new javafx.scene.layout.HBox(10);
        titreBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.control.Label titreLbl = new javafx.scene.control.Label("🤖 Recommandés pour vous");
        titreLbl.getStyleClass().add("reco-title");

        javafx.scene.control.Label badgeLbl = new javafx.scene.control.Label("IA");
        badgeLbl.getStyleClass().add("reco-badge");

        titreBox.getChildren().addAll(titreLbl, badgeLbl);
        recoSection.getChildren().add(titreBox);

        List<Produit> produitsRecommandes = findRecommendedProducts(recommandationsTexte, bestSellers);
        if (produitsRecommandes.isEmpty()) {
            produitsRecommandes.addAll(bestSellers.stream().limit(6).toList());
        }

        javafx.scene.control.Label recoProductsTitle = new javafx.scene.control.Label("Produits recommandés");
        recoProductsTitle.getStyleClass().add("reco-strip-title");
        recoSection.getChildren().add(recoProductsTitle);

        javafx.scene.Node recoCarousel = createAutoCarousel(produitsRecommandes, false);
        recoSection.getChildren().add(recoCarousel);

        javafx.scene.control.Label bsTitle = new javafx.scene.control.Label("Best sellers");
        bsTitle.getStyleClass().add("reco-strip-title");
        recoSection.getChildren().add(bsTitle);

        javafx.scene.Node bestCarousel = createAutoCarousel(bestSellers, true);
        recoSection.getChildren().add(bestCarousel);

        // Inserer la section en haut, juste avant la barre de recherche/filtres.
        int filterIndex = -1;
        for (int i = 0; i < mainVBox.getChildren().size(); i++) {
            javafx.scene.Node child = mainVBox.getChildren().get(i);
            if (child.getStyleClass().contains("filter-card")) {
                filterIndex = i;
                break;
            }
        }

        if (filterIndex >= 0) {
            mainVBox.getChildren().add(filterIndex, recoSection);
        } else {
            // Fallback: avant la grille produits si le bloc filtres est absent.
            int gridIndex = mainVBox.getChildren().indexOf(productGrid);
            if (gridIndex >= 0) {
                mainVBox.getChildren().add(gridIndex, recoSection);
            } else {
                mainVBox.getChildren().add(recoSection);
            }
        }

        System.out.println("[Pharmacie] Recommandations IA affichées : " + recommandationsTexte.size());
    }

    private List<Produit> findRecommendedProducts(List<String> recommandationsTexte, List<Produit> bestSellers) {
        Map<String, Produit> byName = new LinkedHashMap<>();
        for (Produit p : allProduitsFront) {
            byName.put(normalizeRecoText(p.getNom_produit()), p);
        }

        LinkedHashSet<Produit> ordered = new LinkedHashSet<>();
        for (String reco : recommandationsTexte) {
            String normReco = normalizeRecoText(reco);
            for (Map.Entry<String, Produit> e : byName.entrySet()) {
                if (!e.getKey().isBlank() && normReco.contains(e.getKey())) {
                    ordered.add(e.getValue());
                    break;
                }
            }
            if (ordered.size() >= 8) {
                break;
            }
        }

        if (ordered.isEmpty()) {
            ordered.addAll(bestSellers);
        }
        return new ArrayList<>(ordered);
    }

    private Node createAutoCarousel(List<Produit> produits, boolean bestSellerMode) {
        List<Produit> safe = new ArrayList<>(produits == null ? List.of() : produits);
        if (safe.isEmpty()) {
            return new Label("Aucun produit à afficher");
        }

        StackPane host = new StackPane();
        host.getStyleClass().add("reco-auto-carousel");
        host.setPrefHeight(220);

        Integer[] index = {0};
        Runnable render = () -> {
            HBox row = new HBox(24);
            row.setAlignment(Pos.CENTER);
            row.getStyleClass().add("reco-carousel-row");

            int maxVisible = Math.min(3, safe.size());
            for (int offset = 0; offset < maxVisible; offset++) {
                Produit current = safe.get(Math.floorMod(index[0] + offset, safe.size()));
                Node card = createRecoProduitCard(current, bestSellerMode);
                row.getChildren().add(card);
            }

            row.setOpacity(0);
            row.setTranslateX(32);
            host.getChildren().setAll(row);

            FadeTransition fade = new FadeTransition(Duration.millis(260), row);
            fade.setFromValue(0);
            fade.setToValue(1);

            javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(Duration.millis(260), row);
            slide.setFromX(32);
            slide.setToX(0);

            new javafx.animation.ParallelTransition(fade, slide).play();
        };

        Timeline loop = new Timeline(new KeyFrame(Duration.seconds(2.0), e -> {
            index[0] = index[0] + 1;
            render.run();
        }));
        loop.setCycleCount(Timeline.INDEFINITE);

        render.run();
        loop.play();

        host.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                loop.stop();
            }
        });

        return host;
    }

    private Node createRecoProduitCard(Produit produit, boolean bestSellerMode) {
        VBox card = new VBox(8);
        card.getStyleClass().add("reco-product-card");
        card.setPrefWidth(230);
        card.setMaxWidth(230);
        if (bestSellerMode) {
            card.getStyleClass().add("reco-best-card");
        }

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("reco-product-image-box");
        String imgPath = produit.getImage_produit() == null ? "" : produit.getImage_produit().trim();
        if (!imgPath.isEmpty()) {
            try {
                File f = new File(imgPath);
                if (f.exists()) {
                    ImageView iv = new ImageView(new Image(f.toURI().toString()));
                    iv.setFitWidth(72);
                    iv.setFitHeight(72);
                    iv.setPreserveRatio(true);
                    imageBox.getChildren().add(iv);
                } else {
                    imageBox.getChildren().add(new Label("🖼"));
                }
            } catch (Exception e) {
                imageBox.getChildren().add(new Label("🖼"));
            }
        } else {
            imageBox.getChildren().add(new Label("🖼"));
        }

        Label nom = new Label(produit.getNom_produit() == null ? "Produit" : produit.getNom_produit());
        nom.getStyleClass().add("reco-product-name");
        nom.setWrapText(true);

        Label prix = new Label(String.format(Locale.US, "%.2f DT", produit.getPrix_produit()));
        prix.getStyleClass().add("reco-product-price");

        card.getChildren().addAll(imageBox, nom, prix);
        return card;
    }

    private String normalizeRecoText(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

// ─────────────────────────────────────────────────────────────────────────
// ── 5. AJOUTER cette méthode de carte best seller ────────────────────────
// ─────────────────────────────────────────────────────────────────────────

    private javafx.scene.layout.VBox createBestSellerCard(tn.esprit.entities.Produit produit) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(8);
        card.setPrefWidth(200);
        card.setMaxWidth(200);
        card.setStyle(
                "-fx-background-color: white; -fx-border-color: #fca5a5; "
                        + "-fx-border-radius: 12; -fx-background-radius: 12; "
                        + "-fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);"
        );

        // Badge best seller
        javafx.scene.control.Label badgeLbl = new javafx.scene.control.Label("🔥 Best Seller");
        badgeLbl.setStyle(
                "-fx-background-color: #fef2f2; -fx-text-fill: #dc2626; "
                        + "-fx-font-size: 10px; -fx-font-weight: bold; "
                        + "-fx-background-radius: 6; -fx-padding: 2 6;"
        );

        // Image
        javafx.scene.layout.StackPane imgBox = new javafx.scene.layout.StackPane();
        imgBox.setPrefHeight(80);
        String imgPath = produit.getImage_produit() == null ? "" : produit.getImage_produit();
        if (!imgPath.isEmpty()) {
            try {
                java.io.File f = new java.io.File(imgPath);
                if (f.exists()) {
                    javafx.scene.image.ImageView iv =
                            new javafx.scene.image.ImageView(
                                    new javafx.scene.image.Image(f.toURI().toString()));
                    iv.setFitWidth(70);
                    iv.setFitHeight(70);
                    iv.setPreserveRatio(true);
                    imgBox.getChildren().add(iv);
                } else {
                    imgBox.getChildren().add(new javafx.scene.control.Label("🖼"));
                }
            } catch (Exception e) {
                imgBox.getChildren().add(new javafx.scene.control.Label("🖼"));
            }
        } else {
            imgBox.getChildren().add(new javafx.scene.control.Label("🖼"));
        }

        // Nom
        javafx.scene.control.Label nomLbl = new javafx.scene.control.Label(
                produit.getNom_produit() == null ? "" : produit.getNom_produit());
        nomLbl.setWrapText(true);
        nomLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        // Prix
        javafx.scene.control.Label prixLbl = new javafx.scene.control.Label(
                String.format(Locale.US, "%.2f DT", produit.getPrix_produit()));
        prixLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #059669; -fx-font-weight: bold;");

        // Bouton ajouter
        javafx.scene.control.Button addBtn = new javafx.scene.control.Button("+ Ajouter");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setStyle(
                "-fx-background-color: #dc2626; -fx-text-fill: white; "
                        + "-fx-font-size: 12px; -fx-font-weight: bold; "
                        + "-fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 10;"
        );

        boolean rupture = "Rupture".equalsIgnoreCase(produit.getStatus_produit());
        if (rupture) {
            addBtn.setDisable(true);
            addBtn.setText("Rupture");
            addBtn.setStyle(addBtn.getStyle() + "-fx-background-color: #9ca3af;");
        } else {
            addBtn.setOnAction(e -> {
                ajouterProduitAuPanier(produit, 1);
                updatePanierButton();
            });
        }

        card.getChildren().addAll(badgeLbl, imgBox, nomLbl, prixLbl, addBtn);
        return card;
    }


}