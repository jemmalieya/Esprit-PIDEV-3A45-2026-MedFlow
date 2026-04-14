package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
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
            int currentQty = CartSession.getQuantiteProduit(produit);
            if (currentQty - 1 <= 0) {
                CartSession.supprimerToutesOccurrences(produit);
                showSuccessAlert("Article supprimé du panier : " + produit.getNom_produit());
            } else {
                CartSession.supprimerUneOccurrence(produit);
                showSuccessAlert("Quantité diminuée pour " + produit.getNom_produit());
            }
            refreshPanier();
        });

        Label qteValue = new Label(String.valueOf(quantite));
        qteValue.getStyleClass().add("qty-value");

        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("qty-btn");
        plusBtn.setOnAction(e -> {
            int currentQty = CartSession.getQuantiteProduit(produit);
            if (currentQty + 1 > produit.getQuantite_produit()) {
                showStockInsufficiencyAlert(produit.getNom_produit(), produit.getQuantite_produit());
            } else {
                CartSession.ajouterProduit(produit);
                showSuccessAlert("Quantité augmentée pour " + produit.getNom_produit());
                refreshPanier();
            }
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
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Suppression de l'article");
            confirm.setContentText("Voulez-vous vraiment supprimer cet article du panier : " + produit.getNom_produit() + " ?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                CartSession.supprimerToutesOccurrences(produit);
                showSuccessAlert("Article supprimé du panier : " + produit.getNom_produit());
                refreshPanier();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(imageBox, infoBox, prixBox, qteBox, spacer, totalBox, deleteBtn);
        return row;
    }

    @FXML
    private void viderPanier() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Vider le panier");
        confirm.setContentText("Voulez-vous vraiment vider le panier ? Tous les articles seront supprimés.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            CartSession.viderPanier();
            showSuccessAlert("Panier vidé avec succès.");
            refreshPanier();
        }
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
        Platform.runLater(() -> {
            // Custom overlay alert instead of modal dialog
            Label alertLabel = new Label(content);
            alertLabel.getStyleClass().add("info-alert");
            alertLabel.setMaxWidth(400);
            alertLabel.setAlignment(Pos.CENTER);

            StackPane alertContainer = new StackPane();
            alertContainer.getChildren().add(alertLabel);
            alertContainer.setAlignment(Pos.TOP_CENTER);
            alertContainer.setPadding(new Insets(20, 20, 0, 20));

            BorderPane root = (BorderPane) panierContainer.getScene().getRoot();
            ScrollPane scroll = (ScrollPane) root.getCenter();
            VBox panierMain = (VBox) scroll.getContent();
            panierMain.getChildren().add(0, alertContainer);

            alertLabel.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), alertLabel);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(event -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), alertLabel);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> panierMain.getChildren().remove(alertContainer));
                fadeOut.play();
            });
            delay.play();
        });
    }

    private void showStockInsufficiencyAlert(String productName, int availableQuantity) {
        Platform.runLater(() -> {
            Label stockAlert = new Label("Stock insuffisant pour " + productName + ". Disponible: " + availableQuantity);
            stockAlert.getStyleClass().add("stock-insuff-alert");
            stockAlert.setMaxWidth(400);
            stockAlert.setAlignment(Pos.CENTER);

            StackPane alertContainer = new StackPane();
            alertContainer.getChildren().add(stockAlert);
            alertContainer.setAlignment(Pos.TOP_CENTER);
            alertContainer.setPadding(new Insets(20, 20, 0, 20));

            BorderPane root = (BorderPane) panierContainer.getScene().getRoot();
            ScrollPane scroll = (ScrollPane) root.getCenter();
            VBox panierMain = (VBox) scroll.getContent();
            panierMain.getChildren().add(0, alertContainer);

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
                fadeOut.setOnFinished(e -> panierMain.getChildren().remove(alertContainer));
                fadeOut.play();
            });
            delay.play();
        });
    }

    private void showSuccessAlert(String message) {
        Platform.runLater(() -> {
            Label successAlert = new Label(message);
            successAlert.getStyleClass().add("success-alert");
            successAlert.setMaxWidth(400);
            successAlert.setAlignment(Pos.CENTER);

            StackPane alertContainer = new StackPane();
            alertContainer.getChildren().add(successAlert);
            alertContainer.setAlignment(Pos.TOP_CENTER);
            alertContainer.setPadding(new Insets(20, 20, 0, 20));

            BorderPane root = (BorderPane) panierContainer.getScene().getRoot();
            ScrollPane scroll = (ScrollPane) root.getCenter();
            VBox panierMain = (VBox) scroll.getContent();
            panierMain.getChildren().add(0, alertContainer);

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
                fadeOut.setOnFinished(e -> panierMain.getChildren().remove(alertContainer));
                fadeOut.play();
            });
            delay.play();
        });
    }
}
