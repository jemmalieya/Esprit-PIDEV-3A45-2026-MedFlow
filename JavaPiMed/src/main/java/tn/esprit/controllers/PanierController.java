package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.entities.User;
import tn.esprit.services.CommandeService;
import tn.esprit.session.CartSession;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class PanierController {

    @FXML private VBox panierContainer;
    @FXML private Label panierTitleLabel;
    @FXML private Label panierSubtitleLabel;
    @FXML private Label articleCountLabel;
    @FXML private Label sousTotalLabel;
    @FXML private Label totalLabel;
    @FXML private VBox emptyCartBox;
    @FXML private HBox cartContentBox;
    @FXML private Button btnValiderCommande;
    @FXML private Button btnViderPanier;

    private final CommandeService commandeService = new CommandeService();

    @FXML
    public void initialize() {
        refreshPanier();
    }

    private void refreshPanier() {
        List<Produit> panier = CartSession.getPanier();

        if (panierTitleLabel != null) {
            panierTitleLabel.setText("Mon Panier");
        }

        if (panier.isEmpty()) {
            if (emptyCartBox != null) {
                emptyCartBox.setVisible(true);
                emptyCartBox.setManaged(true);
            }

            if (cartContentBox != null) {
                cartContentBox.setVisible(false);
                cartContentBox.setManaged(false);
            }

            if (panierSubtitleLabel != null) {
                panierSubtitleLabel.setText("Votre panier est vide");
            }

            if (articleCountLabel != null) {
                articleCountLabel.setText("Articles (0)");
            }

            if (sousTotalLabel != null) {
                sousTotalLabel.setText("0.00 DT");
            }

            if (totalLabel != null) {
                totalLabel.setText("0.00 DT");
            }

            if (panierContainer != null) {
                panierContainer.getChildren().clear();
            }

            return;
        }

        if (emptyCartBox != null) {
            emptyCartBox.setVisible(false);
            emptyCartBox.setManaged(false);
        }

        if (cartContentBox != null) {
            cartContentBox.setVisible(true);
            cartContentBox.setManaged(true);
        }

        if (panierSubtitleLabel != null) {
            panierSubtitleLabel.setText("Vous avez " + panier.size() + " article(s) dans votre panier");
        }

        Map<Integer, Produit> produitsUniques = new LinkedHashMap<>();
        for (Produit p : panier) {
            produitsUniques.putIfAbsent(p.getId_produit(), p);
        }

        if (panierContainer != null) {
            panierContainer.getChildren().clear();
        }

        double totalGeneral = 0.0;

        for (Produit produit : produitsUniques.values()) {
            int quantite = CartSession.getQuantiteProduit(produit);
            double totalProduit = produit.getPrix_produit() * quantite;
            totalGeneral += totalProduit;

            HBox ligne = createPanierRow(produit, quantite, totalProduit);
            if (panierContainer != null) {
                panierContainer.getChildren().add(ligne);
            }
        }

        if (articleCountLabel != null) {
            articleCountLabel.setText("Articles (" + produitsUniques.size() + ")");
        }

        if (sousTotalLabel != null) {
            sousTotalLabel.setText(formatPrix(totalGeneral));
        }

        if (totalLabel != null) {
            totalLabel.setText(formatPrix(totalGeneral));
        }
    }

    private HBox createPanierRow(Produit produit, int quantite, double totalProduit) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("cart-row");
        row.setPadding(new Insets(18));

        StackPane imageBox = new StackPane();
        imageBox.getStyleClass().add("cart-image-box");
        imageBox.setPrefSize(74, 74);

        String imagePath = produit.getImage_produit();
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    ImageView imageView = new ImageView(new Image(file.toURI().toString()));
                    imageView.setFitWidth(60);
                    imageView.setFitHeight(60);
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

        VBox infoBox = new VBox(6);
        infoBox.setPrefWidth(220);

        Label nomLabel = new Label(produit.getNom_produit());
        nomLabel.getStyleClass().add("cart-product-name");

        Label categorieLabel = new Label(produit.getCategorie_produit());
        categorieLabel.getStyleClass().add("cart-product-category");

        infoBox.getChildren().addAll(nomLabel, categorieLabel);

        VBox prixBox = new VBox(6);
        prixBox.setAlignment(Pos.CENTER_LEFT);
        prixBox.setPrefWidth(120);

        Label prixUnitText = new Label("Prix unitaire");
        prixUnitText.getStyleClass().add("cart-small-label");

        Label prixUnitValue = new Label(formatPrix(produit.getPrix_produit()));
        prixUnitValue.getStyleClass().add("cart-price-value");

        prixBox.getChildren().addAll(prixUnitText, prixUnitValue);

        VBox qteBox = new VBox(6);
        qteBox.setAlignment(Pos.CENTER);
        qteBox.setPrefWidth(170);

        Label qteLabel = new Label("Quantité");
        qteLabel.getStyleClass().add("cart-small-label");

        HBox qteControls = new HBox(12);
        qteControls.setAlignment(Pos.CENTER);
        qteControls.getStyleClass().add("qty-box");

        Button minusBtn = new Button("−");
        minusBtn.getStyleClass().add("qty-btn");
        minusBtn.setOnAction(e -> {
            CartSession.supprimerUneOccurrence(produit);
            refreshPanier();
        });

        Label qteValue = new Label(String.valueOf(quantite));
        qteValue.getStyleClass().add("qty-value");

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("qty-btn");
        plusBtn.setOnAction(e -> {
            CartSession.ajouterProduit(produit);
            refreshPanier();
        });

        qteControls.getChildren().addAll(minusBtn, qteValue, plusBtn);
        qteBox.getChildren().addAll(qteLabel, qteControls);

        VBox totalBox = new VBox(6);
        totalBox.setAlignment(Pos.CENTER_RIGHT);
        totalBox.setPrefWidth(120);

        Label totalText = new Label("Total");
        totalText.getStyleClass().add("cart-small-label");

        Label totalValue = new Label(formatPrix(totalProduit));
        totalValue.getStyleClass().add("cart-total-value");

        totalBox.getChildren().addAll(totalText, totalValue);

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("delete-cart-btn");
        deleteBtn.setOnAction(e -> {
            CartSession.supprimerToutesOccurrences(produit);
            refreshPanier();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(imageBox, infoBox, prixBox, qteBox, spacer, totalBox, deleteBtn);
        return row;
    }

    @FXML
    private void viderPanier() {
        CartSession.viderPanier();
        refreshPanier();
    }

    @FXML
    private void retourPharmacie() {
        try {
            URL url = getClass().getResource("/FrontFXML/Pharmacie.fxml");
            if (url == null) {
                throw new IOException("Fichier introuvable : /FrontFXML/Pharmacie.fxml");
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            Stage stage = (Stage) panierContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Pharmacie");
            stage.show();

        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la page Pharmacie : " + e.getMessage());
        }
    }

    @FXML
    private void validerCommande() {
        if (CartSession.getPanier().isEmpty()) {
            showAlert("Panier vide", "Ajoutez d'abord des produits.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Validation de la commande");
        confirm.setContentText("Voulez-vous vraiment confirmer cette commande ?");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        Map<Integer, Produit> produitsUniques = new LinkedHashMap<>();
        for (Produit p : CartSession.getPanier()) {
            produitsUniques.putIfAbsent(p.getId_produit(), p);
        }

        List<CommandeProduit> lignes = new ArrayList<>();
        for (Produit produit : produitsUniques.values()) {
            int quantite = CartSession.getQuantiteProduit(produit);

            CommandeProduit cp = new CommandeProduit();
            cp.setProduit(produit);
            cp.setQuantite_commandee(quantite);

            lignes.add(cp);
        }

        User user = new User();
        user.setId(1); // adapte ici avec l'utilisateur connecté

        boolean succes = commandeService.validerCommandeDepuisPanier(user, lignes);

        if (succes) {
            showAlert("Succès", "Commande validée et enregistrée avec succès.");
            CartSession.viderPanier();
            refreshPanier();
        } else {
            showAlert("Erreur", "Échec lors de la validation de la commande.");
        }
    }

    private String formatPrix(double prix) {
        return String.format(Locale.US, "%.2f DT", prix);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}