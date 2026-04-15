package tn.esprit.controllers;

import javafx.animation.FadeTransition;
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
import tn.esprit.entities.Commande;
import tn.esprit.services.CommandeService;
import tn.esprit.services.DashboardBIService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Produit;
import tn.esprit.services.ProduitService;
import tn.esprit.session.CartSession;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ProduitController {

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
        if (triPrixCombo != null) triPrixCombo.setOnAction(e -> refreshPharmacieGrid());
        if (triStockCombo != null) triStockCombo.setOnAction(e -> refreshPharmacieGrid());
        if (categorieCombo != null) categorieCombo.setOnAction(e -> refreshPharmacieGrid());

        updatePanierButton();
        refreshPharmacieGrid();
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

    private void showProduitDetailsPopup(Produit p) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails du Produit");
        dialog.setHeaderText(null);

        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("product-details-popup");

        // Image
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(200, 200);
        imageContainer.getStyleClass().add("product-image-container");

        String imagePath = safe(p.getImage_produit());
        if (!imagePath.isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    ImageView imageView = new ImageView(new Image(file.toURI().toString()));
                    imageView.setFitWidth(180);
                    imageView.setFitHeight(180);
                    imageView.setPreserveRatio(true);
                    imageContainer.getChildren().add(imageView);
                } else {
                    Label placeholder = new Label("🖼");
                    placeholder.getStyleClass().add("image-placeholder");
                    imageContainer.getChildren().add(placeholder);
                }
            } catch (Exception e) {
                Label placeholder = new Label("🖼");
                placeholder.getStyleClass().add("image-placeholder");
                imageContainer.getChildren().add(placeholder);
            }
        } else {
            Label placeholder = new Label("🖼");
            placeholder.getStyleClass().add("image-placeholder");
            imageContainer.getChildren().add(placeholder);
        }

        // Product Name
        Label nameLabel = new Label(safe(p.getNom_produit()));
        nameLabel.getStyleClass().add("product-name-popup");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);

        // Details Grid
        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(10);
        detailsGrid.setVgap(8);
        detailsGrid.setAlignment(Pos.CENTER);

        Label priceLabel = new Label("Prix:");
        priceLabel.getStyleClass().add("detail-label");
        Label priceValue = new Label(formatPrix(p.getPrix_produit()));
        priceValue.getStyleClass().add("detail-value");

        Label stockLabel = new Label("Stock:");
        stockLabel.getStyleClass().add("detail-label");
        Label stockValue = new Label(String.valueOf(p.getQuantite_produit()));
        stockValue.getStyleClass().add("detail-value");

        Label categoryLabel = new Label("Catégorie:");
        categoryLabel.getStyleClass().add("detail-label");
        Label categoryValue = new Label(safe(p.getCategorie_produit()));
        categoryValue.getStyleClass().add("detail-value");

        Label statusLabel = new Label("Statut:");
        statusLabel.getStyleClass().add("detail-label");
        Label statusValue = new Label(safe(p.getStatus_produit()));
        statusValue.getStyleClass().add("detail-value");

        detailsGrid.add(priceLabel, 0, 0);
        detailsGrid.add(priceValue, 1, 0);
        detailsGrid.add(stockLabel, 0, 1);
        detailsGrid.add(stockValue, 1, 1);
        detailsGrid.add(categoryLabel, 0, 2);
        detailsGrid.add(categoryValue, 1, 2);
        detailsGrid.add(statusLabel, 0, 3);
        detailsGrid.add(statusValue, 1, 3);

        // Description
        Label descTitle = new Label("Description:");
        descTitle.getStyleClass().add("description-title");
        TextArea descArea = new TextArea(safe(p.getDescription_produit()));
        descArea.setEditable(false);
        descArea.setWrapText(true);
        descArea.setPrefRowCount(3);
        descArea.getStyleClass().add("description-area");

        content.getChildren().addAll(imageContainer, nameLabel, detailsGrid, descTitle, descArea);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        dialogPane.setPrefWidth(400);
        dialogPane.setPrefHeight(600);

        // Style the dialog
        dialogPane.getStyleClass().add("product-details-dialog");

        dialog.showAndWait();
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
            stage.setScene(new Scene(root));
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

            Scene scene = new Scene(root, 1600, 950);

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
            StringBuilder errors = new StringBuilder("Veuillez corriger les champs suivants :\n");
            if (!isNomValid) errors.append("- Nom du produit\n");
            if (!isDescValid) errors.append("- Description\n");
            if (!isPrixValid) errors.append("- Prix\n");
            if (!isQuantiteValid) errors.append("- Quantité\n");
            if (!isCategorieValid) errors.append("- Catégorie\n");
            if (!isImageValid) errors.append("- Image\n");

            showAlert(Alert.AlertType.ERROR, "Champs invalides", errors.toString());
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
                        "Produit déjà existant",
                        "Un produit avec le même nom, la même catégorie, le même prix et la même description existe déjà."
                );
                return;
            }

            produitService.ajouter(produit);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit ajouté avec succès !");
            viderChampsPrivate();
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
            StringBuilder errors = new StringBuilder("Veuillez corriger les champs suivants :\n");
            if (!isNomValid) errors.append("- Nom du produit\n");
            if (!isDescValid) errors.append("- Description\n");
            if (!isPrixValid) errors.append("- Prix\n");
            if (!isQuantiteValid) errors.append("- Quantité\n");
            if (!isCategorieValid) errors.append("- Catégorie\n");
            if (!isImageValid) errors.append("- Image\n");
            showAlert(Alert.AlertType.ERROR, "Champs invalides", errors.toString());
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

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Produit modifié avec succès !");
            produitAmodifier = null;
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
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg", "*.webp")
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Suppression du produit");
        confirm.setContentText("Voulez-vous vraiment supprimer le produit : " + safe(produit.getNom_produit()) + " ?");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            produitService.supprimer(produit);
            masterList.remove(produit);
            applyFiltersAndRefresh();
            showAlert(Alert.AlertType.INFORMATION, "Suppression réussie", "Le produit a été supprimé avec succès.");
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
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    @FXML
    private void ouvrirMesCommandes() {
        try {
            URL url = getClass().getResource("/FrontFXML/MesCommandes.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage = (Stage) productGrid.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Mes commandes");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void showSuccessAlert(String produitName, int quantity) {
        Platform.runLater(() -> {
            String text = quantity == 1 ? produitName + " ajouté au panier." : quantity + " x " + produitName + " ajoutés au panier.";
            Label successAlert = new Label(text);
            successAlert.getStyleClass().add("success-alert");
            successAlert.setMaxWidth(400);
            successAlert.setAlignment(Pos.CENTER);

            StackPane alertContainer = new StackPane();
            alertContainer.getChildren().add(successAlert);
            alertContainer.setAlignment(Pos.TOP_CENTER);
            alertContainer.setPadding(new Insets(20, 20, 0, 20));

            BorderPane root = (BorderPane) btnPanier.getScene().getRoot();
            if (!(root.getCenter() instanceof StackPane)) {
                ScrollPane scroll = (ScrollPane) root.getCenter();
                StackPane stack = new StackPane();
                stack.getChildren().add(scroll);
                root.setCenter(stack);
            }
            StackPane stack = (StackPane) root.getCenter();
            stack.getChildren().add(alertContainer);

            successAlert.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), successAlert);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(event -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), successAlert);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> stack.getChildren().remove(alertContainer));
                fadeOut.play();
            });
            delay.play();
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
            Label stockAlert = new Label("Stock insuffisant pour " + produitName + ". Disponible: " + availableStock);
            stockAlert.getStyleClass().add("stock-insuff-alert");
            stockAlert.setMaxWidth(400);
            stockAlert.setAlignment(Pos.CENTER);

            StackPane alertContainer = new StackPane();
            alertContainer.getChildren().add(stockAlert);
            alertContainer.setAlignment(Pos.TOP_CENTER);
            alertContainer.setPadding(new Insets(20, 20, 0, 20));

            BorderPane root = (BorderPane) btnPanier.getScene().getRoot();
            if (!(root.getCenter() instanceof StackPane)) {
                ScrollPane scroll = (ScrollPane) root.getCenter();
                StackPane stack = new StackPane();
                stack.getChildren().add(scroll);
                root.setCenter(stack);
            }
            StackPane stack = (StackPane) root.getCenter();
            stack.getChildren().add(alertContainer);

            stockAlert.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), stockAlert);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(event -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), stockAlert);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> stack.getChildren().remove(alertContainer));
                fadeOut.play();
            });
            delay.play();
        });
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
}