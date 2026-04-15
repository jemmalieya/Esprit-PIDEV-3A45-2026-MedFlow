package tn.esprit.controllers;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.awt.Color;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
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
import javafx.util.Duration;
import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.User;
import tn.esprit.services.CommandePDFService;
import tn.esprit.services.CommandeService;
import tn.esprit.session.CartSession;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommandeController {

    // =======================
    // DONNEES COMMUNES
    // =======================
    private final CommandeService service = new CommandeService();
    private final CommandePDFService pdfService = new CommandePDFService();

    private static Commande commandeSelectionneeFront;
    private static Commande commandeSelectionneeBack;

    private final List<Commande> allCommandesBack = new ArrayList<>();

    // =======================
    // FRONT - LISTE
    // =======================
    @FXML private FlowPane commandeContainer;
    @FXML private Button btnPanierCommandes;
    @FXML private Label lblNbCommandes;
    @FXML private Label statTotal;
    @FXML private Label statEnCours;
    @FXML private Label statLivraison;
    @FXML private Label statFinalise;
    @FXML private VBox emptyState;

    // =======================
    // FRONT - DETAIL
    // =======================
    @FXML private Label lblNumCommande;
    @FXML private Label lblDateCommande;
    @FXML private Label lblStatutPill;
    @FXML private VBox produitsContainer;
    @FXML private Label lblTotal;
    @FXML private Label lblNbProduits;
    @FXML private Label lblAdresse;
    @FXML private Label lblVille;
    @FXML private Label lblTelephone;
    @FXML private Label lblNote;
    @FXML private Label resumeNum;
    @FXML private Label resumeDate;
    @FXML private Label resumeArticles;
    @FXML private Label resumeTotal;
    @FXML private Button btnPanierDetail;

    // =======================
    // BACK - LISTE
    // =======================
    @FXML private VBox commandesRowsContainer;
    @FXML private Label totalCommandesLabel;
    @FXML private TextField searchCommandeField;
    @FXML private ComboBox<String> filtreStatutCombo;

    // =======================
    // BACK - DETAIL
    // =======================
    @FXML private Label detailCommandeNumeroLabel;
    @FXML private Label detailDateLabel;

    @FXML private Label clientNomLabel;
    @FXML private Label clientCinLabel;
    @FXML private Label clientEmailLabel;
    @FXML private Label clientTelephoneLabel;
    @FXML private Label clientAdresseLabel;

    @FXML private Label detailMontantLabel;
    @FXML private Label detailStatutLabel;
    @FXML private GridPane produitsGrid;

    @FXML private ComboBox<String> nouveauStatutCombo;
    @FXML private Button btnMettreAJourStatut;
    @FXML private Button btnTelechargerFacture;
    @FXML private Button btnSupprimerCommande;

    @FXML
    public void initialize() {
        // FRONT LISTE
        if (commandeContainer != null) {
            loadCommandesFront();
            updatePanierButtonFront();
        }

        // FRONT DETAIL
        if (lblNumCommande != null) {
            initFrontDetail();
            updatePanierButtonDetail();
        }

        // BACK LISTE
        if (commandesRowsContainer != null) {
            initBackList();
        }

        // BACK DETAIL
        if (detailCommandeNumeroLabel != null) {
            initBackDetail();
        }
    }

    // =========================================================
    // FRONT LISTE
    // =========================================================
    private void loadCommandesFront() {
        if (commandeContainer == null) return;

        List<Commande> commandes = service.recuperer();
        commandeContainer.getChildren().clear();

        if (lblNbCommandes != null) lblNbCommandes.setText(commandes.size() + " commande(s)");
        if (statTotal != null) statTotal.setText(String.valueOf(commandes.size()));

        int enCours = 0, enLivraison = 0, finalise = 0;

        for (Commande c : commandes) {
            String statut = c.getStatut_commande();
            if (statut != null) {
                String s = statut.toLowerCase();
                if (s.contains("cours")) enCours++;
                else if (s.contains("livraison")) enLivraison++;
                else if (s.contains("final")) finalise++;
            }
        }

        if (statEnCours != null) statEnCours.setText(String.valueOf(enCours));
        if (statLivraison != null) statLivraison.setText(String.valueOf(enLivraison));
        if (statFinalise != null) statFinalise.setText(String.valueOf(finalise));

        if (commandes.isEmpty()) {
            if (emptyState != null) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
            }
            return;
        }

        if (emptyState != null) {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
        }

        for (Commande c : commandes) {
            commandeContainer.getChildren().add(createCommandeCard(c));
        }
    }

    private VBox createCommandeCard(Commande c) {
        VBox card = new VBox(0);
        card.getStyleClass().add("cmd-card");

        VBox topSection = new VBox(10);
        topSection.getStyleClass().add("card-top");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label titleIcon = new Label("🧾");
        titleIcon.getStyleClass().add("card-title-icon");

        Label title = new Label("Commande #" + c.getId_commande());
        title.getStyleClass().add("card-title");

        titleBox.getChildren().addAll(titleIcon, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label(normalizeStatut(c.getStatut_commande()).toUpperCase());
        status.getStyleClass().addAll("badge", getBadgeClass(c.getStatut_commande()));

        header.getChildren().addAll(titleBox, spacer, status);

        Label date = new Label("  " + c.getDate_creation_commande());
        date.getStyleClass().add("card-date");

        Separator sep = new Separator();

        Label prodsLabel = new Label("PRODUITS");
        prodsLabel.getStyleClass().add("products-label");

        HBox imagesBox = new HBox(8);
        imagesBox.setAlignment(Pos.CENTER_LEFT);

        List<CommandeProduit> produits = c.getCommande_produits();
        if (produits != null) {
            for (CommandeProduit cp : produits) {
                imagesBox.getChildren().add(buildImageMini(cp));
            }
        }

        topSection.getChildren().addAll(header, date, sep, prodsLabel, imagesBox);

        VBox bottomSection = new VBox(8);
        bottomSection.getStyleClass().add("card-bottom");

        HBox totalRow = new HBox();
        totalRow.setAlignment(Pos.CENTER_LEFT);

        Label totalLabel = new Label("Total");
        totalLabel.getStyleClass().add("total-label");

        Region totalSpacer = new Region();
        HBox.setHgrow(totalSpacer, Priority.ALWAYS);

        double montant = c.getMontant_total_cents() / 100.0;
        Label total = new Label(String.format("%.2f Dt", montant));
        total.getStyleClass().add("cmd-total");

        totalRow.getChildren().addAll(totalLabel, totalSpacer, total);

        Button btnFacture = new Button("⬇  Facture PDF");
        btnFacture.getStyleClass().add("btn-primary");
        btnFacture.setMaxWidth(Double.MAX_VALUE);
        btnFacture.setOnAction(e -> handleFacturePDFFromFront(c));

        HBox btnGrid = new HBox(8);

        Button btnSuivi = new Button("🚚  Suivi livraison");
        btnSuivi.getStyleClass().add("btn-secondary");
        btnSuivi.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnSuivi, Priority.ALWAYS);
        btnSuivi.setOnAction(e -> handleSuiviLivraison(c));

        Button btnDetails = new Button("👁  Voir les détails");
        btnDetails.getStyleClass().add("btn-secondary");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDetails, Priority.ALWAYS);
        btnDetails.setOnAction(e -> ouvrirDetailsCommandeFront(c));

        btnGrid.getChildren().addAll(btnSuivi, btnDetails);

        bottomSection.getChildren().addAll(totalRow, btnFacture, btnGrid);
        card.getChildren().addAll(topSection, bottomSection);

        return card;
    }

    private StackPane buildImageMini(CommandeProduit cp) {
        StackPane imgWrap = new StackPane();

        try {
            String path = cp.getProduit().getImage_produit();
            if (path != null && !path.isEmpty()) {
                File file = new File(path);
                if (file.exists()) {
                    ImageView img = new ImageView(new Image(file.toURI().toString()));
                    img.setFitWidth(52);
                    img.setFitHeight(52);
                    img.setPreserveRatio(false);
                    imgWrap.getStyleClass().add("product-img-wrap");
                    imgWrap.getChildren().add(img);
                    return imgWrap;
                }
            }
        } catch (Exception ignored) {
        }

        imgWrap.getStyleClass().add("product-img-placeholder");
        imgWrap.getChildren().add(new Label("🖼"));
        return imgWrap;
    }

    private void ouvrirDetailsCommandeFront(Commande c) {
        try {
            commandeSelectionneeFront = c;

            URL url = getClass().getResource("/FrontFXML/CommandeDetail.fxml");
            if (url == null) throw new IOException("Fichier introuvable : /FrontFXML/CommandeDetail.fxml");

            Parent root = FXMLLoader.load(url);

            Stage stage = (Stage) commandeContainer.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 820);
            scene.getStylesheets().addAll(commandeContainer.getScene().getStylesheets());

            stage.setScene(scene);
            stage.setTitle("Détail commande");
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleFacturePDFFromFront(Commande c) {
        telechargerFactureCommande(c);
    }

    private void handleSuiviLivraison(Commande c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Suivi livraison");
        a.setHeaderText(null);
        a.setContentText("Suivi livraison pour la commande #" + c.getId_commande());
        a.showAndWait();
    }

    @FXML
    private void filtrerToutes() {
        if (commandeContainer != null) {
            loadCommandesFront();
        } else if (commandesRowsContainer != null) {
            allCommandesBack.clear();
            allCommandesBack.addAll(service.recuperer());
            refreshBackCommandeRows();
            updateStatsBack(allCommandesBack);
        }
    }

    @FXML
    private void filtrerEnCours() {
        if (commandeContainer != null) {
            filterByStatusFront("cours");
        } else if (commandesRowsContainer != null) {
            filterByStatusBack("cours");
        }
    }

    @FXML
    private void filtrerLivraison() {
        if (commandeContainer != null) {
            filterByStatusFront("livraison");
        } else if (commandesRowsContainer != null) {
            filterByStatusBack("livraison");
        }
    }

    @FXML
    private void filtrerFinalise() {
        if (commandeContainer != null) {
            filterByStatusFront("final");
        } else if (commandesRowsContainer != null) {
            filterByStatusBack("final");
        }
    }

    @FXML
    private void filtrerAttente() {
        if (commandeContainer != null) {
            filterByStatusFront("attente");
        } else if (commandesRowsContainer != null) {
            filterByStatusBack("attente");
        }
    }

    private void filterByStatusFront(String statusKeyword) {
        if (commandeContainer == null) return;

        commandeContainer.getChildren().clear();
        List<Commande> commandes = service.recuperer();
        List<Commande> filtered = new ArrayList<>();

        for (Commande c : commandes) {
            if (c.getStatut_commande() != null &&
                    c.getStatut_commande().toLowerCase().contains(statusKeyword)) {
                filtered.add(c);
            }
        }

        if (lblNbCommandes != null) {
            lblNbCommandes.setText(filtered.size() + " commande(s)");
        }

        if (emptyState != null) {
            boolean vide = filtered.isEmpty();
            emptyState.setVisible(vide);
            emptyState.setManaged(vide);
        }

        for (Commande c : filtered) {
            commandeContainer.getChildren().add(createCommandeCard(c));
        }
    }

    private void filterByStatusBack(String statusKeyword) {
        if (commandesRowsContainer == null) return;

        List<Commande> commandes = service.recuperer();
        List<Commande> filtered = new ArrayList<>();

        for (Commande c : commandes) {
            if (c.getStatut_commande() != null &&
                    c.getStatut_commande().toLowerCase().contains(statusKeyword)) {
                filtered.add(c);
            }
        }

        allCommandesBack.clear();
        allCommandesBack.addAll(filtered);

        refreshBackCommandeRows();
        updateStatsBack(filtered);
    }

    @FXML
    private void actualiserCommandes() {
        if (commandeContainer != null) {
            loadCommandesFront();
        } else if (commandesRowsContainer != null) {
            allCommandesBack.clear();
            allCommandesBack.addAll(service.recuperer());
            refreshBackCommandeRows();
            updateStatsBack(allCommandesBack);
        }
    }

    // =========================================================
    // FRONT DETAIL
    // =========================================================
    private void initFrontDetail() {
        if (commandeSelectionneeFront == null) return;
        populateFrontDetail();
    }

    private void populateFrontDetail() {
        Commande commande = commandeSelectionneeFront;
        if (commande == null) return;

        if (lblNumCommande != null) lblNumCommande.setText("Commande #" + commande.getId_commande());
        if (lblDateCommande != null) lblDateCommande.setText("📅  " + commande.getDate_creation_commande());

        String statut = commande.getStatut_commande();
        if (lblStatutPill != null) {
            lblStatutPill.setText(normalizeStatut(statut).toUpperCase());
            lblStatutPill.getStyleClass().setAll("badge-detail", getBadgeClass(statut));
        }

        if (produitsContainer != null) {
            produitsContainer.getChildren().clear();
            List<CommandeProduit> produits = commande.getCommande_produits();
            if (produits == null) produits = new ArrayList<>();

            if (lblNbProduits != null) lblNbProduits.setText(produits.size() + " article(s)");

            for (CommandeProduit cp : produits) {
                produitsContainer.getChildren().add(buildFrontProductRow(cp));
            }
        }

        double montant = commande.getMontant_total_cents() / 100.0;
        if (lblTotal != null) lblTotal.setText(String.format("%.2f Dt", montant));

        if (resumeNum != null) resumeNum.setText("#" + commande.getId_commande());
        if (resumeDate != null) resumeDate.setText(String.valueOf(commande.getDate_creation_commande()));
        if (resumeArticles != null) resumeArticles.setText(String.valueOf(
                commande.getCommande_produits() == null ? 0 : commande.getCommande_produits().size()
        ));
        if (resumeTotal != null) resumeTotal.setText(String.format("%.2f Dt", montant));
    }

    private HBox buildFrontProductRow(CommandeProduit cp) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("product-row");

        StackPane thumb = new StackPane();
        thumb.setPrefSize(52, 52);
        thumb.setMaxSize(52, 52);

        try {
            String path = cp.getProduit().getImage_produit();
            if (path != null && !path.isEmpty()) {
                File f = new File(path);
                if (f.exists()) {
                    ImageView iv = new ImageView(new Image(f.toURI().toString()));
                    iv.setFitWidth(46);
                    iv.setFitHeight(46);
                    iv.setPreserveRatio(false);
                    thumb.getStyleClass().add("product-img-wrap");
                    thumb.getChildren().add(iv);
                } else {
                    thumb.getStyleClass().add("product-img-placeholder");
                    thumb.getChildren().add(new Label("🖼"));
                }
            } else {
                thumb.getStyleClass().add("product-img-placeholder");
                thumb.getChildren().add(new Label("🖼"));
            }
        } catch (Exception e) {
            thumb.getStyleClass().add("product-img-placeholder");
            thumb.getChildren().add(new Label("🖼"));
        }

        VBox nameBox = new VBox(2);
        nameBox.setPrefWidth(188);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.setPadding(new Insets(0, 0, 0, 10));

        Label name = new Label(cp.getProduit().getNom_produit());
        name.getStyleClass().add("product-name");

        Label cat = new Label(cp.getProduit().getCategorie_produit() != null
                ? cp.getProduit().getCategorie_produit()
                : "Médicament");
        cat.getStyleClass().add("product-cat");

        nameBox.getChildren().addAll(name, cat);

        Label qty = new Label(String.valueOf(cp.getQuantite_commandee()));
        qty.getStyleClass().add("qty-badge");
        HBox qtyBox = new HBox(qty);
        qtyBox.setAlignment(Pos.CENTER);
        qtyBox.setPrefWidth(90);

        Label price = new Label(String.format("%.2f Dt", cp.getProduit().getPrix_produit()));
        HBox priceBox = new HBox(price);
        priceBox.setAlignment(Pos.CENTER_RIGHT);
        priceBox.setPrefWidth(100);

        double sub = cp.getProduit().getPrix_produit() * cp.getQuantite_commandee();
        Label subtotal = new Label(String.format("%.2f Dt", sub));
        HBox subBox = new HBox(subtotal);
        subBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(subBox, Priority.ALWAYS);

        row.getChildren().addAll(thumb, nameBox, qtyBox, priceBox, subBox);
        return row;
    }

    @FXML
    private void retourCommandesFront() {
        try {
            URL url = getClass().getResource("/FrontFXML/MesCommandes.fxml");
            if (url == null) throw new IOException("Fichier introuvable : /FrontFXML/MesCommandes.fxml");

            Parent root = FXMLLoader.load(url);

            Stage stage = (Stage) lblNumCommande.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 820);
            scene.getStylesheets().addAll(lblNumCommande.getScene().getStylesheets());

            stage.setScene(scene);
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleContinuer() {
        try {
            URL url = getClass().getResource("/FrontFXML/Pharmacie.fxml");
            if (url == null) throw new IOException("Fichier introuvable : /FrontFXML/Pharmacie.fxml");

            Parent root = FXMLLoader.load(url);

            Stage stage = (Stage) lblNumCommande.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 820);
            scene.getStylesheets().addAll(lblNumCommande.getScene().getStylesheets());

            stage.setScene(scene);
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void telechargerFactureFront() {
        if (commandeSelectionneeFront != null) {
            telechargerFactureCommande(commandeSelectionneeFront);
        }
    }

    // =========================================================
    // BACK LISTE
    // =========================================================
    private void initBackList() {
        if (filtreStatutCombo != null) {
            filtreStatutCombo.getItems().setAll(
                    "Tous", "Confirmée", "En cours", "En attente",
                    "Livraison", "Finalisée", "Annulée"
            );
            filtreStatutCombo.setValue("Tous");
            filtreStatutCombo.setOnAction(e -> refreshBackCommandeRows());
        }

        if (searchCommandeField != null) {
            searchCommandeField.textProperty().addListener((obs, oldVal, newVal) -> refreshBackCommandeRows());
        }

        allCommandesBack.clear();
        allCommandesBack.addAll(service.recuperer());
        refreshBackCommandeRows();
        updateStatsBack(allCommandesBack);
    }

    private void refreshBackCommandeRows() {
        if (commandesRowsContainer == null) return;

        commandesRowsContainer.getChildren().clear();
        List<Commande> filtered = new ArrayList<>(allCommandesBack);

        String keyword = searchCommandeField != null && searchCommandeField.getText() != null
                ? searchCommandeField.getText().trim().toLowerCase()
                : "";

        String statutChoisi = filtreStatutCombo != null ? filtreStatutCombo.getValue() : "Tous";

        if (!keyword.isEmpty()) {
            filtered.removeIf(c ->
                    !String.valueOf(c.getId_commande()).toLowerCase().contains(keyword)
                            && !safe(readStringReflect(c, "getNom_client", "getNomClient", "getNom")).toLowerCase().contains(keyword)
                            && !safe(readStringReflect(c, "getEmail_client", "getEmailClient", "getEmail")).toLowerCase().contains(keyword)
                            && !safe(c.getStatut_commande()).toLowerCase().contains(keyword)
            );
        }

        if (statutChoisi != null && !"Tous".equalsIgnoreCase(statutChoisi)) {
            filtered.removeIf(c -> !normalizeStatut(c.getStatut_commande()).equalsIgnoreCase(statutChoisi));
        }

        if (totalCommandesLabel != null) {
            totalCommandesLabel.setText(filtered.size() + " commande(s) au total");
        }

        if (emptyState != null) {
            boolean vide = filtered.isEmpty();
            emptyState.setVisible(vide);
            emptyState.setManaged(vide);
        }

        if (filtered.isEmpty()) {
            return;
        }

        for (Commande c : filtered) {
            commandesRowsContainer.getChildren().add(createBackCommandeRow(c));
        }
    }

    private void updateStatsBack(List<Commande> commandes) {
        if (commandes == null) commandes = new ArrayList<>();

        if (totalCommandesLabel != null) {
            totalCommandesLabel.setText(commandes.size() + " commande(s) au total");
        }

        if (statTotal != null) {
            statTotal.setText(String.valueOf(commandes.size()));
        }

        int enCours = 0;
        int enLivraison = 0;
        int finalise = 0;

        for (Commande c : commandes) {
            String statut = c.getStatut_commande();
            if (statut != null) {
                String s = statut.toLowerCase();
                if (s.contains("cours")) enCours++;
                else if (s.contains("livraison")) enLivraison++;
                else if (s.contains("final")) finalise++;
            }
        }

        if (statEnCours != null) statEnCours.setText(String.valueOf(enCours));
        if (statLivraison != null) statLivraison.setText(String.valueOf(enLivraison));
        if (statFinalise != null) statFinalise.setText(String.valueOf(finalise));
    }
    private HBox createBackCommandeRow(Commande c) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(0);
        row.setPadding(new Insets(14, 8, 14, 8));
        row.getStyleClass().add("commande-row");
        row.setMaxWidth(Double.MAX_VALUE);

        // ID
        Label idLabel = new Label("#" + c.getId_commande());
        idLabel.getStyleClass().add("row-id-label");
        VBox idBox = wrapBackCell(idLabel, 60, Pos.CENTER_LEFT);

        // DATE
        Label dateLabel = new Label(String.valueOf(c.getDate_creation_commande()));
        dateLabel.getStyleClass().add("row-date-label");
        VBox dateBox = wrapBackCell(dateLabel, 170, Pos.CENTER_LEFT);

        // CLIENT
// CLIENT
        VBox clientContent = new VBox(4);
        clientContent.setAlignment(Pos.CENTER_LEFT);

        User u = c.getUser();

        String nomComplet = "Client inconnu";
        String email = "-";

        if (u != null) {
            String prenom = u.getPrenom() != null ? u.getPrenom() : "";
            String nom = u.getNom() != null ? u.getNom() : "";
            String mail = u.getEmailUser() != null ? u.getEmailUser() : "-";

            String full = (prenom + " " + nom).trim();
            if (!full.isEmpty()) {
                nomComplet = full;
            }
            email = mail;
        }

// NOM
        Label nomLabel = new Label(nomComplet);
        nomLabel.getStyleClass().add("client-name-label");

// EMAIL
        Label emailLabel = new Label(email);
        emailLabel.getStyleClass().add("client-email-label");

        clientContent.getChildren().addAll(nomLabel, emailLabel);
        VBox clientBox = wrapBackCell(clientContent, 220, Pos.CENTER_LEFT);

        // MONTANT
        double montant = c.getMontant_total_cents() / 100.0;
        Label montantLabel = new Label(String.format("%.2f Dt", montant));
        montantLabel.getStyleClass().add("montant-value-label");
        VBox montantBox = wrapBackCell(montantLabel, 120, Pos.CENTER_LEFT);

        // STATUT
        Label statutBadge = new Label(normalizeStatut(c.getStatut_commande()));
        statutBadge.getStyleClass().addAll("status-badge", getStatusClass(c.getStatut_commande()));
        VBox statutBox = wrapBackCell(statutBadge, 140, Pos.CENTER_LEFT);

        // PRODUITS
        int nbProduits = c.getCommande_produits() != null ? c.getCommande_produits().size() : 0;
        Label produitsBadge = new Label(nbProduits + " produit(s)");
        produitsBadge.getStyleClass().add("products-count-badge");
        VBox produitsBox = wrapBackCell(produitsBadge, 120, Pos.CENTER_LEFT);

        // ACTIONS
        Button btnDetails = new Button("Détails");
        btnDetails.getStyleClass().add("btn-detail");
        btnDetails.setOnAction(e -> ouvrirDetailsCommandeBack(c));

        Button btnFacture = new Button("Facture");
        btnFacture.getStyleClass().add("btn-facture");
        btnFacture.setOnAction(e -> telechargerFactureCommande(c));

        HBox actionsContent = new HBox(8, btnDetails, btnFacture);
        actionsContent.setAlignment(Pos.CENTER_LEFT);
        VBox actionsBox = wrapBackCell(actionsContent, 180, Pos.CENTER_LEFT);

        row.getChildren().addAll(
                idBox,
                dateBox,
                clientBox,
                montantBox,
                statutBox,
                produitsBox,
                actionsBox
        );

        return row;
    }


    private VBox wrapBackCell(javafx.scene.Node node, double width, Pos alignment) {
        VBox box = new VBox(node);
        box.setAlignment(alignment);
        box.setPrefWidth(width);
        box.setMinWidth(width);
        box.setMaxWidth(width);
        box.setFillWidth(true);
        box.getStyleClass().add("table-cell-box");
        return box;
    }
    private void ouvrirDetailsCommandeBack(Commande c) {
        try {
            commandeSelectionneeBack = c;

            URL url = getClass().getResource("/CommandeBackDetail.fxml");
            if (url == null) throw new IOException("Fichier introuvable : /CommandeBackDetail.fxml");

            Parent root = FXMLLoader.load(url);

            Stage stage = (Stage) commandesRowsContainer.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 850);

            URL css = getClass().getResource("/CSS/commande_back.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("Détails commande");
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // =========================================================
    // BACK DETAIL
    // =========================================================
    private void initBackDetail() {
        if (commandeSelectionneeBack == null) return;

        if (nouveauStatutCombo != null) {
            nouveauStatutCombo.getItems().setAll(
                    "En attente", "Confirmée", "En cours",
                    "Livraison", "Finalisée", "Annulée"
            );
            nouveauStatutCombo.setValue(normalizeStatut(commandeSelectionneeBack.getStatut_commande()));
        }

        populateBackDetails();
    }

    private void populateBackDetails() {
        if (commandeSelectionneeBack == null) return;

        detailCommandeNumeroLabel.setText("Commande #" + commandeSelectionneeBack.getId_commande());
        detailDateLabel.setText(String.valueOf(commandeSelectionneeBack.getDate_creation_commande()));

        clientNomLabel.setText(safe(readStringReflect(commandeSelectionneeBack, "getNom_client", "getNomClient", "getNom")));
        clientCinLabel.setText(safe(readStringReflect(commandeSelectionneeBack, "getCin_client", "getCinClient", "getCin")));
        clientEmailLabel.setText(safe(readStringReflect(commandeSelectionneeBack, "getEmail_client", "getEmailClient", "getEmail")));
        clientTelephoneLabel.setText(safe(readStringReflect(commandeSelectionneeBack, "getTelephone_client", "getTelephoneClient", "getTelephone")));
        clientAdresseLabel.setText(safe(readStringReflect(commandeSelectionneeBack, "getAdresse_client", "getAdresseClient", "getAdresse")));

        detailMontantLabel.setText(String.format("%.2f Dt", commandeSelectionneeBack.getMontant_total_cents() / 100.0));

        detailStatutLabel.setText(normalizeStatut(commandeSelectionneeBack.getStatut_commande()));
        detailStatutLabel.getStyleClass().removeAll(
                "status-confirmed", "status-progress", "status-pending",
                "status-delivery", "status-final", "status-cancel"
        );
        detailStatutLabel.getStyleClass().addAll("status-badge", getStatusClass(commandeSelectionneeBack.getStatut_commande()));

        fillProduitsGridBack();
    }

    private void fillProduitsGridBack() {
        if (produitsGrid == null || commandeSelectionneeBack == null) return;

        produitsGrid.getChildren().clear();

        addHeaderCellBack("Produit", 0, 0);
        addHeaderCellBack("Quantité", 1, 0);
        addHeaderCellBack("Prix unitaire", 2, 0);
        addHeaderCellBack("Sous-total", 3, 0);

        List<CommandeProduit> items = commandeSelectionneeBack.getCommande_produits();
        if (items == null) items = new ArrayList<>();

        int rowIndex = 1;
        for (CommandeProduit cp : items) {
            HBox produitCell = new HBox(10);
            produitCell.setAlignment(Pos.CENTER_LEFT);

            StackPane imgBox = new StackPane();
            imgBox.setPrefSize(52, 52);
            imgBox.getStyleClass().add("mini-image-box");

            try {
                String path = cp.getProduit().getImage_produit();
                if (path != null && !path.isEmpty() && new File(path).exists()) {
                    ImageView iv = new ImageView(new Image(new File(path).toURI().toString()));
                    iv.setFitWidth(42);
                    iv.setFitHeight(42);
                    iv.setPreserveRatio(true);
                    imgBox.getChildren().add(iv);
                } else {
                    imgBox.getChildren().add(new Label("🖼"));
                }
            } catch (Exception e) {
                imgBox.getChildren().add(new Label("🖼"));
            }

            VBox produitInfos = new VBox(4);
            Label nom = new Label(safe(cp.getProduit().getNom_produit()));
            Label cat = new Label(safe(cp.getProduit().getCategorie_produit()));
            produitInfos.getChildren().addAll(nom, cat);

            produitCell.getChildren().addAll(imgBox, produitInfos);

            Label qte = new Label(String.valueOf(cp.getQuantite_commandee()));
            Label prix = new Label(String.format("%.2f Dt", cp.getProduit().getPrix_produit()));
            Label sub = new Label(String.format("%.2f Dt", cp.getProduit().getPrix_produit() * cp.getQuantite_commandee()));

            addBodyCellBack(produitCell, 0, rowIndex);
            addBodyCellBack(centerNodeBack(qte), 1, rowIndex);
            addBodyCellBack(prix, 2, rowIndex);
            addBodyCellBack(sub, 3, rowIndex);

            rowIndex++;
        }

        Label totalTitle = new Label("TOTAL :");
        Label totalValue = new Label(String.format("%.2f Dt", commandeSelectionneeBack.getMontant_total_cents() / 100.0));

        addFooterCellBack(totalTitle, 0, rowIndex, 3);
        addFooterCellBack(totalValue, 3, rowIndex, 1);
    }

    private void addHeaderCellBack(String text, int col, int row) {
        Label label = new Label(text);
        produitsGrid.add(label, col, row);
        GridPane.setHgrow(label, Priority.ALWAYS);
        GridPane.setFillWidth(label, true);
        label.setMaxWidth(Double.MAX_VALUE);
    }

    private void addBodyCellBack(javafx.scene.Node node, int col, int row) {
        StackPane wrapper = new StackPane(node);
        wrapper.setAlignment(col == 1 ? Pos.CENTER : Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(12));
        produitsGrid.add(wrapper, col, row);
        GridPane.setHgrow(wrapper, Priority.ALWAYS);
        GridPane.setFillWidth(wrapper, true);
    }

    private void addFooterCellBack(javafx.scene.Node node, int col, int row, int colspan) {
        StackPane wrapper = new StackPane(node);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(12));
        produitsGrid.add(wrapper, col, row, colspan, 1);
        GridPane.setHgrow(wrapper, Priority.ALWAYS);
        GridPane.setFillWidth(wrapper, true);
    }

    private StackPane centerNodeBack(javafx.scene.Node node) {
        StackPane sp = new StackPane(node);
        sp.setAlignment(Pos.CENTER);
        return sp;
    }

    @FXML
    private void retourListeCommandes() {
        try {
            URL url = getClass().getResource("/MesCommandesBack.fxml");
            if (url == null) throw new IOException("Fichier introuvable : /MesCommandesBack.fxml");

            Parent root = FXMLLoader.load(url);

            Stage stage = (Stage) detailCommandeNumeroLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 850);

            URL css = getClass().getResource("/CSS/commande_back.css");
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("Gestion des commandes");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void mettreAJourStatutCommande() {
        if (commandeSelectionneeBack == null || nouveauStatutCombo == null) return;

        try {
            commandeSelectionneeBack.setStatut_commande(nouveauStatutCombo.getValue());
            service.modifier(commandeSelectionneeBack);

            showCommandeToast("Statut mis à jour avec succès.", "commande-toast-success", "✔");
            populateBackDetails();

        } catch (Exception e) {
            e.printStackTrace();
            showCommandeToast("Erreur lors de la mise à jour du statut.", "commande-toast-danger", "✖");
        }
    }

    @FXML
    private void supprimerCommande() {
        if (commandeSelectionneeBack == null) return;

        boolean confirmed = showStyledCommandeConfirmationDialog(
                "🗑 Supprimer la commande",
                "Voulez-vous vraiment supprimer cette commande ? Cette action est irréversible.",
                "Supprimer",
                "Annuler",
                "danger"
        );

        if (!confirmed) {
            return;
        }

        try {
            service.supprimer(commandeSelectionneeBack);
            showCommandeToast("Commande supprimée avec succès.", "commande-toast-danger", "🗑");
            retourListeCommandes();

        } catch (Exception e) {
            e.printStackTrace();
            showCommandeToast("Erreur lors de la suppression de la commande.", "commande-toast-danger", "✖");
        }
    }

    @FXML
    private void telechargerFacture() {
        if (commandeSelectionneeBack != null) {
            telechargerFactureCommande(commandeSelectionneeBack);
        }
    }

    // =========================================================
    // PANIER / NAVIGATION
    // =========================================================
    private void updatePanierButtonFront() {
        if (btnPanierCommandes != null) {
            btnPanierCommandes.setText("🛒 Panier (" + CartSession.getNombreArticles() + ")");
        }
    }

    private void updatePanierButtonDetail() {
        if (btnPanierDetail != null) {
            btnPanierDetail.setText("🛒 Panier (" + CartSession.getNombreArticles() + ")");
        }
    }

    @FXML
    private void ouvrirPanierPopup() {
        try {
            URL url = getClass().getResource("/FrontFXML/Panier.fxml");
            if (url == null) throw new IOException("Fichier introuvable : /FrontFXML/Panier.fxml");

            Parent root = FXMLLoader.load(url);

            Stage stage;
            if (btnPanierCommandes != null && btnPanierCommandes.getScene() != null) {
                stage = (Stage) btnPanierCommandes.getScene().getWindow();
            } else if (btnPanierDetail != null && btnPanierDetail.getScene() != null) {
                stage = (Stage) btnPanierDetail.getScene().getWindow();
            } else {
                return;
            }

            Scene scene = new Scene(root, 1400, 820);
            stage.setScene(scene);
            stage.setTitle("Mon Panier");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retourPharmacie() {
        try {
            URL url = getClass().getResource("/FrontFXML/Pharmacie.fxml");
            if (url == null) throw new IOException("Fichier introuvable : /FrontFXML/Pharmacie.fxml");

            Parent root = FXMLLoader.load(url);

            Stage stage = (Stage) btnPanierCommandes.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 820);
            stage.setScene(scene);
            stage.setTitle("Pharmacie");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retourProduitDashboard() {
        try {
            URL url = getClass().getResource("/ProduitDashboard.fxml");
            if (url == null) throw new IOException("Fichier introuvable : /ProduitDashboard.fxml");

            Parent root = FXMLLoader.load(url);

            Stage stage = null;

            if (btnPanierCommandes != null && btnPanierCommandes.getScene() != null) {
                stage = (Stage) btnPanierCommandes.getScene().getWindow();
            } else if (commandeContainer != null && commandeContainer.getScene() != null) {
                stage = (Stage) commandeContainer.getScene().getWindow();
            } else if (commandesRowsContainer != null && commandesRowsContainer.getScene() != null) {
                stage = (Stage) commandesRowsContainer.getScene().getWindow();
            } else if (detailCommandeNumeroLabel != null && detailCommandeNumeroLabel.getScene() != null) {
                stage = (Stage) detailCommandeNumeroLabel.getScene().getWindow();
            }

            if (stage == null) return;

            Scene scene = new Scene(root, 1400, 820);
            stage.setScene(scene);
            stage.setTitle("Dashboard Produits");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // FACTURE
    // =========================================================
    /*private void telechargerFactureCommande(Commande commande) {
        if (commande == null) return;

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer la facture PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf")
            );
            fileChooser.setInitialFileName("facture_commande_" + commande.getId_commande() + ".pdf");

            Stage stage = null;
            if (btnTelechargerFacture != null && btnTelechargerFacture.getScene() != null) {
                stage = (Stage) btnTelechargerFacture.getScene().getWindow();
            } else if (commandesRowsContainer != null && commandesRowsContainer.getScene() != null) {
                stage = (Stage) commandesRowsContainer.getScene().getWindow();
            } else if (commandeContainer != null && commandeContainer.getScene() != null) {
                stage = (Stage) commandeContainer.getScene().getWindow();
            }

            File file = fileChooser.showSaveDialog(stage);
            if (file == null) return;

            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // ===== Couleurs =====
            Color primary = new Color(19, 74, 122);
            Color softBlue = new Color(237, 245, 255);
            Color lightGray = new Color(245, 247, 250);
            Color lineGray = new Color(220, 226, 233);
            Color green = new Color(22, 163, 74);
            Color orange = new Color(245, 158, 11);
            Color red = new Color(220, 38, 38);
            Color blue = new Color(37, 99, 235);

            // ===== Fonts =====
            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, primary);
            Font subtitleFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.GRAY);
            Font sectionFont = new Font(Font.HELVETICA, 12, Font.BOLD, primary);
            Font normalFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
            Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD, primary);
            Font totalFont = new Font(Font.HELVETICA, 16, Font.BOLD, green);
            Font whiteBold = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

            // ===== HEADER =====
            Paragraph title = new Paragraph("FACTURE", titleFont);
            title.setSpacingAfter(6f);
            document.add(title);

            Paragraph cmdRef = new Paragraph("Commande #" + commande.getId_commande(), subtitleFont);
            cmdRef.setSpacingAfter(12f);
            document.add(cmdRef);

            PdfPTable lineTable = new PdfPTable(1);
            lineTable.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell(new Phrase(""));
            lineCell.setBorder(Rectangle.BOTTOM);
            lineCell.setBorderColor(primary);
            lineCell.setBorderWidth(2f);
            lineCell.setFixedHeight(8f);
            lineCell.setPadding(0);
            lineCell.setBackgroundColor(Color.WHITE);
            lineTable.addCell(lineCell);
            lineTable.setSpacingAfter(18f);
            document.add(lineTable);

            // ===== BLOC INFOS =====
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1.2f, 1f});
            infoTable.setSpacingAfter(16f);

            PdfPCell leftInfo = new PdfPCell();
            leftInfo.setBorder(Rectangle.NO_BORDER);
            leftInfo.setBackgroundColor(lightGray);
            leftInfo.setPadding(16f);

            leftInfo.addElement(new Paragraph("Date : " +
                    (commande.getDate_creation_commande() != null
                            ? commande.getDate_creation_commande().format(formatter)
                            : "-"), boldFont));

            Paragraph statutP = new Paragraph("Statut : " + normalizeStatut(commande.getStatut_commande()), boldFont);
            statutP.setSpacingBefore(8f);
            leftInfo.addElement(statutP);

            User user = commande.getUser();
            String clientNom = "Client";
            String clientEmail = "-";

            if (user != null) {
                String prenom = user.getPrenom() != null ? user.getPrenom() : "";
                String nom = user.getNom() != null ? user.getNom() : "";
                String full = (prenom + " " + nom).trim();
                if (!full.isEmpty()) clientNom = full;
                if (user.getEmailUser() != null && !user.getEmailUser().isBlank()) {
                    clientEmail = user.getEmailUser();
                }
            }

            PdfPCell rightInfo = new PdfPCell();
            rightInfo.setBorder(Rectangle.NO_BORDER);
            rightInfo.setBackgroundColor(softBlue);
            rightInfo.setPadding(16f);

            rightInfo.addElement(new Paragraph("Client", sectionFont));
            rightInfo.addElement(new Paragraph(clientNom, boldFont));
            rightInfo.addElement(new Paragraph(clientEmail, normalFont));

            infoTable.addCell(leftInfo);
            infoTable.addCell(rightInfo);
            document.add(infoTable);

            // ===== TABLE PRODUITS =====
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4.5f, 1.6f, 1.2f, 1.8f});
            table.setSpacingBefore(6f);
            table.setSpacingAfter(14f);

            addHeaderCell(table, "PRODUIT", primary);
            addHeaderCell(table, "PRIX U.", primary);
            addHeaderCell(table, "QTÉ", primary);
            addHeaderCell(table, "SOUS-TOTAL", primary);

            double total = 0.0;

            List<CommandeProduit> lignes = commande.getCommande_produits();
            if (lignes != null) {
                for (CommandeProduit cp : lignes) {
                    String nomProduit = cp.getProduit() != null ? safe(cp.getProduit().getNom_produit()) : "Produit";
                    double prix = cp.getProduit() != null ? cp.getProduit().getPrix_produit() : 0.0;
                    int qte = cp.getQuantite_commandee();
                    double sousTotal = prix * qte;
                    total += sousTotal;

                    addBodyCell(table, nomProduit, normalFont, Element.ALIGN_LEFT);
                    addBodyCell(table, String.format("%.2f Dt", prix), normalFont, Element.ALIGN_RIGHT);
                    addBodyCell(table, String.valueOf(qte), normalFont, Element.ALIGN_CENTER);
                    addBodyCell(table, String.format("%.2f Dt", sousTotal), boldFont, Element.ALIGN_RIGHT);
                }
            }

            document.add(table);

            // ===== TOTAL =====
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{4f, 1.5f});

            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL", new Font(Font.HELVETICA, 14, Font.BOLD, primary)));
            totalLabelCell.setBorder(Rectangle.TOP);
            totalLabelCell.setBorderColor(primary);
            totalLabelCell.setBorderWidth(2f);
            totalLabelCell.setPaddingTop(12f);
            totalLabelCell.setPaddingBottom(10f);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabelCell.setBackgroundColor(Color.WHITE);

            PdfPCell totalValueCell = new PdfPCell(new Phrase(String.format("%.2f Dt", total), totalFont));
            totalValueCell.setBorder(Rectangle.TOP);
            totalValueCell.setBorderColor(primary);
            totalValueCell.setBorderWidth(2f);
            totalValueCell.setPaddingTop(12f);
            totalValueCell.setPaddingBottom(10f);
            totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValueCell.setBackgroundColor(Color.WHITE);

            totalTable.addCell(totalLabelCell);
            totalTable.addCell(totalValueCell);
            totalTable.setSpacingAfter(28f);
            document.add(totalTable);

            // ===== BADGE STATUT =====
            PdfPTable badgeTable = new PdfPTable(1);
            badgeTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            badgeTable.setWidthPercentage(25);

            Color badgeColor = blue;
            String s = commande.getStatut_commande() == null ? "" : commande.getStatut_commande().toLowerCase();
            if (s.contains("final")) badgeColor = green;
            else if (s.contains("attente")) badgeColor = orange;
            else if (s.contains("annul")) badgeColor = red;
            else if (s.contains("confirm")) badgeColor = blue;
            else if (s.contains("cours")) badgeColor = blue;
            else if (s.contains("livraison")) badgeColor = blue;

            PdfPCell badgeCell = new PdfPCell(new Phrase(normalizeStatut(commande.getStatut_commande()), whiteBold));
            badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            badgeCell.setBackgroundColor(badgeColor);
            badgeCell.setBorder(Rectangle.NO_BORDER);
            badgeCell.setPadding(8f);
            badgeTable.addCell(badgeCell);

            document.add(badgeTable);

            // ===== FOOTER =====
            Paragraph footerSpace = new Paragraph(" ");
            footerSpace.setSpacingBefore(22f);
            document.add(footerSpace);

            PdfPTable footerLine = new PdfPTable(1);
            footerLine.setWidthPercentage(100);
            PdfPCell footerLineCell = new PdfPCell(new Phrase(""));
            footerLineCell.setBorder(Rectangle.BOTTOM);
            footerLineCell.setBorderColor(lineGray);
            footerLineCell.setBorderWidth(1f);
            footerLineCell.setFixedHeight(8f);
            footerLineCell.setPadding(0);
            footerLineCell.setBackgroundColor(Color.WHITE);
            footerLine.addCell(footerLineCell);
            footerLine.setSpacingAfter(18f);
            document.add(footerLine);

            Paragraph thanks = new Paragraph("Merci pour votre confiance", subtitleFont);
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(thanks);

            document.close();

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Facture générée");
            ok.setHeaderText(null);
            ok.setContentText("La facture PDF a été enregistrée avec succès.");
            ok.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();

            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur PDF");
            err.setHeaderText(null);
            err.setContentText("Impossible de générer la facture PDF : " + e.getMessage());
            err.showAndWait();
        }
    }
*/
    // =========================================================
    // UTILITAIRES
    // =========================================================
    private String normalizeStatut(String statut) {
        if (statut == null || statut.isBlank()) return "En attente";

        String s = statut.toLowerCase();
        if (s.contains("confirm")) return "Confirmée";
        if (s.contains("cours")) return "En cours";
        if (s.contains("attente")) return "En attente";
        if (s.contains("livraison")) return "Livraison";
        if (s.contains("final")) return "Finalisée";
        if (s.contains("annul")) return "Annulée";

        return statut;
    }

    private String getBadgeClass(String statut) {
        if (statut == null) return "badge-default";

        String s = statut.toLowerCase();
        if (s.contains("livraison")) return "badge-livraison";
        if (s.contains("cours")) return "badge-cours";
        if (s.contains("final")) return "badge-finalise";

        return "badge-default";
    }

    private String getStatusClass(String statut) {
        String s = statut == null ? "" : statut.toLowerCase();
        if (s.contains("confirm")) return "status-confirmed";
        if (s.contains("cours")) return "status-progress";
        if (s.contains("attente")) return "status-pending";
        if (s.contains("livraison")) return "status-delivery";
        if (s.contains("final")) return "status-final";
        if (s.contains("annul")) return "status-cancel";
        return "status-pending";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String readStringReflect(Object target, String... methodNames) {
        if (target == null) return "";

        for (String methodName : methodNames) {
            try {
                java.lang.reflect.Method m = target.getClass().getMethod(methodName);
                Object value = m.invoke(target);
                return value == null ? "" : String.valueOf(value);
            } catch (Exception ignored) {
            }
        }

        return "";
    }
    private void addHeaderCell(PdfPTable table, String text, Color bg) {
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10f);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10f);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new Color(230, 235, 240));
        cell.setBackgroundColor(Color.WHITE);
        table.addCell(cell);
    }


    private void telechargerFactureCommande(Commande commande) {
        if (commande == null) return;

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer la facture PDF");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf")
            );
            fileChooser.setInitialFileName("facture_commande_" + commande.getId_commande() + ".pdf");

            Stage stage = null;
            if (btnTelechargerFacture != null && btnTelechargerFacture.getScene() != null) {
                stage = (Stage) btnTelechargerFacture.getScene().getWindow();
            } else if (commandesRowsContainer != null && commandesRowsContainer.getScene() != null) {
                stage = (Stage) commandesRowsContainer.getScene().getWindow();
            } else if (commandeContainer != null && commandeContainer.getScene() != null) {
                stage = (Stage) commandeContainer.getScene().getWindow();
            }

            File file = fileChooser.showSaveDialog(stage);
            if (file == null) return;

            pdfService.genererFactureCommande(commande, file.getAbsolutePath());

            showCommandeToast("Facture téléchargée avec succès !", "commande-toast-warning", "📄");

        } catch (Exception e) {
            e.printStackTrace();
            showCommandeToast("Erreur lors du téléchargement de la facture.", "commande-toast-danger", "✖");
        }
    }


    private boolean showStyledCommandeConfirmationDialog(String title, String message, String confirmText, String cancelText, String variant) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        URL cssUrl = getClass().getResource("/CSS/commande_back.css");
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

    private void showCommandeToast(String message, String styleClass, String iconText) {
        Node anchor = null;

        if (commandesRowsContainer != null && commandesRowsContainer.getScene() != null) {
            anchor = commandesRowsContainer;
        } else if (btnTelechargerFacture != null && btnTelechargerFacture.getScene() != null) {
            anchor = btnTelechargerFacture;
        } else if (btnSupprimerCommande != null && btnSupprimerCommande.getScene() != null) {
            anchor = btnSupprimerCommande;
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

        URL cssUrl = getClass().getResource("/CSS/commande_back.css");
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
}