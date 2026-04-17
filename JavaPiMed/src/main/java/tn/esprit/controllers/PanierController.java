package tn.esprit.controllers;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
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
import tn.esprit.tools.SessionManager;

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
            boolean confirmed = showStyledConfirmationDialog(
                    "🗑 Supprimer l'article",
                    "Voulez-vous retirer \"" + produit.getNom_produit() + "\" du panier ?",
                    "Supprimer",
                    "Annuler",
                    "danger"
            );

            if (confirmed) {
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
        boolean confirmed = showStyledConfirmationDialog(
                "🧹 Vider le panier",
                "Voulez-vous vraiment vider le panier ? Tous les articles seront supprimés.",
                "Vider",
                "Annuler",
                "danger"
        );

        if (confirmed) {
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

        boolean confirmed = showStyledConfirmationDialog(
                "✅ Confirmer la commande",
                "Voulez-vous vraiment confirmer cette commande ?",
                "Confirmer",
                "Annuler",
                "success"
        );

        if (!confirmed) {
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

        User user = SessionManager.getCurrentUser();
        if (user == null || user.getId() <= 0) {
            showFloatingToast("Veuillez vous connecter avant de valider une commande.", "toast-warning", "⚠");
            return;
        }

        boolean succes = commandeService.validerCommandeDepuisPanier(user, lignes);

        if (succes) {
            showSuccessAlert("Commande validée avec succès.");
            CartSession.viderPanier();
            refreshPanier();
        } else {
            showFloatingToast("Échec lors de la validation de la commande.", "toast-danger", "✖");
        }
    }

    private String formatPrix(double prix) {
        return String.format(Locale.US, "%.2f DT", prix);
    }

    private void showAlert(String title, String content) {
        showFloatingToast(content, "toast-info", "ℹ");
    }

    private void showStockInsufficiencyAlert(String productName, int availableQuantity) {
        String message = "Stock insuffisant pour " + productName + " (reste : " + availableQuantity + ")";
        showFloatingToast(message, "toast-warning", "⚠");
    }


    private void showSuccessAlert(String message) {
        showFloatingToast(message, "toast-success", "✅");
    }

    private void showFloatingToast(String message, String styleClass, String iconText) {
        if (panierContainer == null || panierContainer.getScene() == null) return;

        Scene scene = panierContainer.getScene();
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

        URL cssUrl = getClass().getResource("/CSS/panier.css");
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

        PauseTransition pause = new PauseTransition(Duration.seconds(2.1));

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

    private boolean showStyledConfirmationDialog(String title, String message, String confirmText, String cancelText, String variant) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        URL cssUrl = getClass().getResource("/CSS/panier.css");
        if (cssUrl != null) {
            pane.getStylesheets().add(cssUrl.toExternalForm());
        }
        pane.getStyleClass().add("confirm-dialog");

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));

        Label icon = new Label(
                "danger".equals(variant) ? "⚠" :
                        "success".equals(variant) ? "✅" : "ℹ"
        );
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
}