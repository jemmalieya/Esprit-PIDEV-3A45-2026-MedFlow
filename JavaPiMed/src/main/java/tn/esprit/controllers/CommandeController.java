package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.services.CommandeService;

import java.io.File;
import java.util.List;

public class CommandeController {

    @FXML
    private FlowPane commandeContainer;

    private final CommandeService service = new CommandeService();

    @FXML
    public void initialize() {
        loadCommandes();
    }

    private void loadCommandes() {
        List<Commande> commandes = service.recuperer();
        commandeContainer.getChildren().clear();
        for (Commande c : commandes) {
            VBox card = createCommandeCard(c);
            commandeContainer.getChildren().add(card);
        }
    }

    private VBox createCommandeCard(Commande c) {
        VBox card = new VBox(0);
        card.getStyleClass().add("cmd-card");

        // ── TOP SECTION ──
        VBox topSection = new VBox(10);
        topSection.getStyleClass().add("card-top");

        // Header: titre + badge statut
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

        Label status = new Label(c.getStatut_commande().toUpperCase());
        status.getStyleClass().addAll("badge", getBadgeClass(c.getStatut_commande()));

        header.getChildren().addAll(titleBox, spacer, status);

        // Date
        Label date = new Label("  " + c.getDate_creation_commande().toString());
        date.getStyleClass().add("card-date");

        // Séparateur
        Separator sep = new Separator();
        sep.getStyleClass().add("card-sep");

        // Label "PRODUITS"
        Label prodsLabel = new Label("PRODUITS");
        prodsLabel.getStyleClass().add("products-label");

        // Images produits côte à côte (toutes affichées, sans limite)
        HBox imagesBox = new HBox(8);
        imagesBox.setAlignment(Pos.CENTER_LEFT);

        List<CommandeProduit> produits = c.getCommande_produits();
        for (CommandeProduit cp : produits) {
            try {
                String path = cp.getProduit().getImage_produit();
                StackPane imgWrap = new StackPane();

                if (path != null && !path.isEmpty()) {
                    File file = new File(path);
                    if (file.exists()) {
                        ImageView img = new ImageView(new Image(file.toURI().toString()));
                        img.setFitWidth(52);
                        img.setFitHeight(52);
                        img.setPreserveRatio(false);
                        imgWrap.getStyleClass().add("product-img-wrap");
                        imgWrap.getChildren().add(img);
                    } else {
                        imgWrap.getStyleClass().add("product-img-placeholder");
                        imgWrap.getChildren().add(new Label("🖼"));
                    }
                } else {
                    imgWrap.getStyleClass().add("product-img-placeholder");
                    imgWrap.getChildren().add(new Label("🖼"));
                }

                imagesBox.getChildren().add(imgWrap);

            } catch (Exception e) {
                StackPane ph = new StackPane(new Label("🖼"));
                ph.getStyleClass().add("product-img-placeholder");
                imagesBox.getChildren().add(ph);
            }
        }

        topSection.getChildren().addAll(header, date, sep, prodsLabel, imagesBox);

        // ── BOTTOM SECTION ──
        VBox bottomSection = new VBox(8);
        bottomSection.getStyleClass().add("card-bottom");

        // Total
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

        // Bouton Facture PDF (pleine largeur)
        Button btnFacture = new Button("⬇  Facture PDF");
        btnFacture.getStyleClass().add("btn-primary");
        btnFacture.setMaxWidth(Double.MAX_VALUE);
        btnFacture.setOnAction(e -> handleFacturePDF(c));

        // Boutons Suivi + Détails (grille 50/50)
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
        btnDetails.setOnAction(e -> handleVoirDetails(c));

        btnGrid.getChildren().addAll(btnSuivi, btnDetails);

        bottomSection.getChildren().addAll(totalRow, btnFacture, btnGrid);

        card.getChildren().addAll(topSection, bottomSection);
        return card;
    }

    // ── Handlers (à implémenter selon votre logique) ──

    private void handleFacturePDF(Commande c) {
        // TODO: générer et ouvrir la facture PDF
        System.out.println("Facture PDF pour commande #" + c.getId_commande());
    }

    private void handleSuiviLivraison(Commande c) {
        // TODO: ouvrir la vue suivi livraison
        System.out.println("Suivi livraison pour commande #" + c.getId_commande());
    }

    private void handleVoirDetails(Commande c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/CommandeDetail.fxml"));
            loader.setController(new CommandeDetailFusionController(c));
            Parent root = loader.load();
            Stage stage = (Stage) commandeContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ── Contrôleur fusionné pour la vue détail ──
    public static class CommandeDetailFusionController {
        @FXML private Label lblOrderNum;
        @FXML private Label lblOrderDate;
        @FXML private Label lblStatut;
        @FXML private VBox  produitsContainer;
        @FXML private Label lblTotal;
        @FXML private Label lblResStatut;
        @FXML private Label lblResMontant;
        @FXML private Label lblResArticles;
        @FXML private Label lblNextStep;
        @FXML private Button btnDownload;

        private final Commande commande;

        public CommandeDetailFusionController(Commande c) {
            this.commande = c;
        }

        @FXML
        public void initialize() {
            populate();
        }

        private void populate() {
            lblOrderNum.setText("Commande #" + commande.getId_commande());
            lblOrderDate.setText("📅  " + commande.getDate_creation_commande());
            String statut = commande.getStatut_commande();
            lblStatut.setText(statut != null ? statut.toUpperCase() : "—");
            lblStatut.getStyleClass().setAll("badge-detail", "badge-attente");

            produitsContainer.getChildren().clear();
            List<CommandeProduit> produits = commande.getCommande_produits();
            for (CommandeProduit cp : produits) {
                produitsContainer.getChildren().add(buildProductRow(cp));
            }
            double montant = commande.getMontant_total_cents() / 100.0;
            lblTotal.setText(String.format("%.2f Dt", montant));
            lblResStatut.setText(statut != null ? statut : "—");
            lblResMontant.setText(String.format("%.2f Dt", montant));
            lblResArticles.setText(String.valueOf(produits.size()));
            lblNextStep.setText(getNextStep(statut));
        }

        private HBox buildProductRow(CommandeProduit cp) {
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
                        placeholder(thumb);
                    }
                } else {
                    placeholder(thumb);
                }
            } catch (Exception e) {
                placeholder(thumb);
            }
            VBox nameBox = new VBox(2);
            nameBox.setPrefWidth(188);
            nameBox.setAlignment(Pos.CENTER_LEFT);
            nameBox.setPadding(new javafx.geometry.Insets(0, 0, 0, 10));
            Label name = new Label(cp.getProduit().getNom_produit());
            name.getStyleClass().add("product-name");
            Label cat  = new Label(cp.getProduit().getCategorie_produit() != null ? cp.getProduit().getCategorie_produit() : "Médicament");
            cat.getStyleClass().add("product-cat");
            nameBox.getChildren().addAll(name, cat);
            Label qty = new Label(String.valueOf(cp.getQuantite_commandee()));
            qty.getStyleClass().add("qty-badge");
            HBox qtyBox = new HBox(qty);
            qtyBox.setAlignment(Pos.CENTER);
            qtyBox.setPrefWidth(90);
            Label price = new Label(String.format("%.2f Dt", cp.getProduit().getPrix_produit()));
            price.getStyleClass().add("price-lbl");
            HBox priceBox = new HBox(price);
            priceBox.setAlignment(Pos.CENTER_RIGHT);
            priceBox.setPrefWidth(100);
            double sub = cp.getProduit().getPrix_produit() * cp.getQuantite_commandee();
            Label subtotal = new Label(String.format("%.2f Dt", sub));
            subtotal.getStyleClass().add("subtotal-lbl");
            HBox subBox = new HBox(subtotal);
            subBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(subBox, Priority.ALWAYS);
            row.getChildren().addAll(thumb, nameBox, qtyBox, priceBox, subBox);
            return row;
        }

        private void placeholder(StackPane p) {
            p.getStyleClass().add("product-img-placeholder");
            p.getChildren().add(new Label("🖼"));
        }

        private String getNextStep(String statut) {
            if (statut == null) return "—";
            String s = statut.toLowerCase();
            if (s.contains("attente"))   return "En attente de confirmation.";
            if (s.contains("cours"))     return "Votre commande est en cours de préparation.";
            if (s.contains("livraison")) return "Votre commande est en route !";
            if (s.contains("final"))     return "Commande finalisée. Merci !";
            return "—";
        }

        @FXML
        private void handleDownloadFacture() {
            System.out.println("Download facture for commande #" + commande.getId_commande());
        }

        @FXML
        private void handleBack() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/MesCommandes.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) lblOrderNum.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @FXML
        private void handleContinuer() {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/Pharmacie.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) lblOrderNum.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ── Utilitaire badge CSS ──

    private String getBadgeClass(String statut) {
        if (statut == null) return "badge-default";
        statut = statut.toLowerCase();
        if (statut.contains("livraison")) return "badge-livraison";
        if (statut.contains("cours"))     return "badge-cours";
        if (statut.contains("final"))     return "badge-finalise";
        return "badge-default";
    }
}