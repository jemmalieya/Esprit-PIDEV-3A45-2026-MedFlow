package tn.esprit.controllers;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import tn.esprit.services.DrugInteractionResult;
import tn.esprit.services.OpenFdaInteractionService;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Produit;
import tn.esprit.services.OrdonnanceExtractorService;
import tn.esprit.session.CartSession;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class PanierController {

    private static final boolean DEBUG_OPENFDA = isDebugEnabled();

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
    @FXML private VBox interactionAlertBox;
    @FXML private Label interactionAlertTitleLabel;
    @FXML private VBox interactionDetailsBox;
    @FXML private HBox ordonnanceBox;
    @FXML private Button btnScanOrdonnance;
    @FXML private Label ordonnanceStatusLabel;

    private final OrdonnanceExtractorService ordonnanceService = new OrdonnanceExtractorService();
    private boolean ordonnanceValidee = false;
    private List<String> medicamentsOrdonnance = new ArrayList<>();

    private List<DrugInteractionResult> dernieresInteractionsDangereuses = Collections.emptyList();
    private boolean interactionDangereusePresente = false;
    private boolean analyseOpenFdaIndisponible = false;
    private String messageAnalyseOpenFda = "";
    private long interactionCheckVersion = 0;
    private String dernierEtatAlerteInteractions = "";

    private final OpenFdaInteractionService openFdaInteractionService = new OpenFdaInteractionService();

    private static boolean isDebugEnabled() {
        String prop = System.getProperty("openfda.debug");
        if (prop != null) {
            return Boolean.parseBoolean(prop);
        }

        String env = System.getenv("OPENFDA_DEBUG");
        return env != null && !env.isBlank() && !"false".equalsIgnoreCase(env);
    }

    private void debug(String message) {
        if (DEBUG_OPENFDA) {
            System.out.println("[Panier/openFDA] " + message);
        }
    }

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

            if (btnViderPanier != null) {
                btnViderPanier.setDisable(true);
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

            dernieresInteractionsDangereuses = Collections.emptyList();
            interactionDangereusePresente = false;
            analyseOpenFdaIndisponible = false;
            messageAnalyseOpenFda = "";
            dernierEtatAlerteInteractions = "vide";
            interactionCheckVersion++;
            masquerAlerteInteractions();

            if (btnValiderCommande != null) {
                btnValiderCommande.setDisable(true);
                btnValiderCommande.setTooltip(null);
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

        if (btnViderPanier != null) {
            btnViderPanier.setDisable(false);
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

        lancerVerificationInteractionsAsynchrone(panier);
    }

    private void lancerVerificationInteractionsAsynchrone(List<Produit> panier) {
        final long version = ++interactionCheckVersion;
        final List<Produit> snapshot = new ArrayList<>(panier);

        debug("Lancement vérification openFDA v" + version + " avec " + snapshot.size() + " produit(s)");

        if (btnValiderCommande != null) {
            btnValiderCommande.setDisable(true);
            btnValiderCommande.setTooltip(new Tooltip("Analyse des interactions en cours..."));
        }

        Task<InteractionCheckOutcome> task = new Task<>() {
            @Override
            protected InteractionCheckOutcome call() {
                OpenFdaInteractionService service = new OpenFdaInteractionService();
                List<DrugInteractionResult> interactions = service.verifierInteractions(snapshot);
                return new InteractionCheckOutcome(
                        interactions,
                        service.isDerniereAnalyseIndisponible(),
                        service.getDernierMessageIndisponibilite()
                );
            }
        };

        task.setOnSucceeded(event -> {
            if (version != interactionCheckVersion) {
                debug("Résultat ignoré (version obsolète) v" + version);
                return;
            }

            InteractionCheckOutcome outcome = task.getValue();
            debug("Résultat openFDA v" + version + ": interactions="
                    + (outcome == null || outcome.interactions == null ? 0 : outcome.interactions.size())
                    + ", indisponible=" + (outcome != null && outcome.analyseIndisponible)
                    + ", message=" + (outcome == null ? "" : outcome.message));
            appliquerEtatInteractions(task.getValue());
        });

        task.setOnFailed(event -> {
            if (version != interactionCheckVersion) {
                debug("Échec ignoré (version obsolète) v" + version);
                return;
            }

            debug("Échec openFDA v" + version + ": " + (event.getSource() instanceof Task ? ((Task<?>) event.getSource()).getException() : "inconnu"));

            dernieresInteractionsDangereuses = Collections.emptyList();
            interactionDangereusePresente = false;
            analyseOpenFdaIndisponible = true;
            messageAnalyseOpenFda = "Erreur lors de l'analyse openFDA.";
            afficherAlerteAnalyseIndisponible(messageAnalyseOpenFda);

            if (btnValiderCommande != null) {
                btnValiderCommande.setDisable(false);
                btnValiderCommande.setTooltip(new Tooltip("Analyse openFDA indisponible: vérifiez manuellement les interactions."));
            }
        });

        Thread thread = new Thread(task, "openfda-panier-check");
        thread.setDaemon(true);
        thread.start();
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
                    "Supprimer"
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
                "Vider"
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

   /* @FXML
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
    }*/
   @FXML
   private void validerCommande() {
       if (CartSession.getPanier().isEmpty()) {
           showAlert("Panier vide", "Ajoutez d'abord des produits.");
           return;
       }

       // Bloquer UNIQUEMENT si interaction ET ordonnance non validée
       if (interactionDangereusePresente && !ordonnanceValidee) {
           showAlert("Interactions dangereuses",
                   "Uploadez une ordonnance valide pour débloquer la commande.");
           return;
       }

       if (analyseOpenFdaIndisponible) {
           boolean continuer = showStyledConfirmationDialog(
                   "⚠ Analyse indisponible",
                   "L'analyse openFDA est indisponible. Voulez-vous quand même continuer ?",
                   "Continuer"
           );
           if (!continuer) return;
       }

       try {
           URL url = getClass().getResource("/FrontFXML/CheckoutCommande.fxml");
           if (url == null) throw new IOException("Fichier introuvable : /FrontFXML/CheckoutCommande.fxml");
           Parent root = FXMLLoader.load(url);
           Stage stage = (Stage) panierContainer.getScene().getWindow();
           stage.setScene(new Scene(root, 1400, 820));
           stage.setTitle("Checkout commande");
           stage.show();
       } catch (Exception e) {
           showAlert("Erreur", "Impossible d'ouvrir la page de paiement : " + e.getMessage());
       }
   }


    private String formatPrix(double prix) {
        return String.format(Locale.US, "%.2f DT", prix);
    }

    private void showAlert(String title, String content) {
        showFloatingToast((title == null || title.isBlank() ? content : title + " : " + content), "toast-info", "ℹ");
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

    private boolean showStyledConfirmationDialog(String title, String message, String confirmText) {
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

        Label icon = new Label("⚠");
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
        cancelButton.setText("Annuler");

        okButton.getStyleClass().add("confirm-danger-btn");
        cancelButton.getStyleClass().add("confirm-cancel-btn");

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }


    private void appliquerEtatInteractions(InteractionCheckOutcome outcome) {
        dernieresInteractionsDangereuses = outcome.interactions == null
                ? Collections.emptyList() : outcome.interactions;
        interactionDangereusePresente = !dernieresInteractionsDangereuses.isEmpty();
        analyseOpenFdaIndisponible = outcome.analyseIndisponible;
        messageAnalyseOpenFda = outcome.message;

        String nouvelEtat = interactionDangereusePresente ? "danger"
                : analyseOpenFdaIndisponible ? "indisponible" : "ok";

        if (nouvelEtat.equals(dernierEtatAlerteInteractions)) {
            if (btnValiderCommande != null) {
                // Débloquer si ordonnance déjà validée
                btnValiderCommande.setDisable(interactionDangereusePresente && !ordonnanceValidee);
            }
            return;
        }
        dernierEtatAlerteInteractions = nouvelEtat;

        if (!interactionDangereusePresente) {
            if (analyseOpenFdaIndisponible) {
                afficherAlerteAnalyseIndisponible(messageAnalyseOpenFda);
            } else {
                afficherAlerteAucuneInteraction();
            }
            if (btnValiderCommande != null) {
                btnValiderCommande.setDisable(false);
                btnValiderCommande.setTooltip(analyseOpenFdaIndisponible
                        ? new Tooltip("Analyse openFDA indisponible.")
                        : null);
            }
            return;
        }

        afficherAlerteInteractions(dernieresInteractionsDangereuses);
        if (btnValiderCommande != null) {
            // Bloquer seulement si ordonnance pas encore validée
            btnValiderCommande.setDisable(!ordonnanceValidee);
            btnValiderCommande.setTooltip(!ordonnanceValidee
                    ? new Tooltip("Uploadez une ordonnance pour débloquer.")
                    : new Tooltip("Ordonnance approuvée — commande autorisée."));
        }
    }

    private static class InteractionCheckOutcome {
        private final List<DrugInteractionResult> interactions;
        private final boolean analyseIndisponible;
        private final String message;

        private InteractionCheckOutcome(List<DrugInteractionResult> interactions, boolean analyseIndisponible, String message) {
            this.interactions = interactions;
            this.analyseIndisponible = analyseIndisponible;
            this.message = message;
        }
    }




    private void afficherAlerteAnalyseIndisponible(String message) {
        if (interactionAlertBox == null || interactionDetailsBox == null) return;

        configurerBlocAlerte(
                "⚠ Analyse openFDA indisponible",
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #92400e;",
                "-fx-background-color: #fffbeb; -fx-border-color: #f59e0b; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 16;"
        );
        interactionDetailsBox.getChildren().clear();

        Label detail = new Label((message == null || message.isBlank())
                ? "Impossible de vérifier automatiquement les interactions pour le moment."
                : message + " Vérifiez manuellement les interactions avant validation.");
        detail.setWrapText(true);
        detail.setStyle("-fx-font-size: 13px; -fx-text-fill: #78350f;");

        interactionDetailsBox.getChildren().add(detail);
        interactionAlertBox.setVisible(true);
        interactionAlertBox.setManaged(true);
    }

    private void afficherAlerteAucuneInteraction() {
        if (interactionAlertBox == null || interactionDetailsBox == null) return;
        // Masquer le bloc ordonnance (pas nécessaire si pas d'interaction)
        if (ordonnanceBox != null) {
            ordonnanceBox.setVisible(false);
            ordonnanceBox.setManaged(false);
        }
        ordonnanceValidee = false;


        configurerBlocAlerte(
                "✅ Aucune interaction dangereuse détectée",
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #166534;",
                "-fx-background-color: #ecfdf5; -fx-border-color: #22c55e; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 16;"
        );

        interactionDetailsBox.getChildren().clear();
        Label detail = new Label("Vérification openFDA terminée: vous pouvez valider la commande.");
        detail.setWrapText(true);
        detail.setStyle("-fx-font-size: 13px; -fx-text-fill: #14532d;");
        interactionDetailsBox.getChildren().add(detail);

        interactionAlertBox.setVisible(true);
        interactionAlertBox.setManaged(true);
    }

    private void configurerBlocAlerte(String titre, String styleTitre, String styleBloc) {
        if (interactionAlertBox != null) {
            interactionAlertBox.setStyle(styleBloc);
        }

        if (interactionAlertTitleLabel != null) {
            interactionAlertTitleLabel.setText(titre);
            interactionAlertTitleLabel.setStyle(styleTitre);
        }
    }


    private void afficherAlerteInteractions(List<DrugInteractionResult> interactions) {
        if (interactionAlertBox == null || interactionDetailsBox == null) return;

        interactionDetailsBox.getChildren().clear();

        for (DrugInteractionResult interaction : interactions) {
            VBox ligne = new VBox(4);

            Label titre = new Label("• " + interaction.getMedicamentA()
                    + " ↔ " + interaction.getMedicamentB());
            titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #991b1b;");

            Label detail = new Label(interaction.getDetailInteraction());
            detail.setWrapText(true);
            detail.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f1d1d;");

            ligne.getChildren().addAll(titre, detail);
            interactionDetailsBox.getChildren().add(ligne);
        }

        // Afficher le bloc ordonnance
        if (ordonnanceBox != null) {
            ordonnanceBox.setVisible(true);
            ordonnanceBox.setManaged(true);
        }

        // Réinitialiser le statut ordonnance si les interactions ont changé
        if (!ordonnanceValidee) {
            if (ordonnanceStatusLabel != null) {
                ordonnanceStatusLabel.setText(
                        "⚠ Commande bloquée. Uploadez une ordonnance pour débloquer.");
                ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #b91c1c;");
            }
        }

        configurerBlocAlerte(
                "⚠ Interactions médicamenteuses dangereuses détectées",
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #b91c1c;",
                "-fx-background-color: #fff1f2; -fx-border-color: #dc2626; "
                        + "-fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 16;"
        );

        interactionAlertBox.setVisible(true);
        interactionAlertBox.setManaged(true);
    }

    @FXML
    private void scannerOrdonnance() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Sélectionner une ordonnance");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter(
                        "Images / PDF", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.tiff", "*.pdf")
        );

        Stage stage = (Stage) panierContainer.getScene().getWindow();
        File fichier = fileChooser.showOpenDialog(stage);
        if (fichier == null) return;

        if (ordonnanceStatusLabel != null) {
            ordonnanceStatusLabel.setText("⏳ Analyse de l'ordonnance en cours...");
            ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1d4ed8;");
        }
        if (btnScanOrdonnance != null) btnScanOrdonnance.setDisable(true);

        // Construire la liste des médicaments attendus depuis les interactions détectées
        final List<String> medicamentsCibles = new ArrayList<>();
        for (DrugInteractionResult interaction : dernieresInteractionsDangereuses) {
            String a = interaction.getMedicamentA();
            String b = interaction.getMedicamentB();
            if (!medicamentsCibles.contains(a)) medicamentsCibles.add(a);
            if (!medicamentsCibles.contains(b)) medicamentsCibles.add(b);
        }

        final File fichierFinal = fichier;

        // Task<String> : on récupère le texte brut OCR
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return ordonnanceService.extraireTexteOcr(fichierFinal);
            }
        };

        task.setOnSucceeded(event -> {
            if (btnScanOrdonnance != null) btnScanOrdonnance.setDisable(false);

            String texteOcr = task.getValue();

            if (texteOcr == null || texteOcr.isBlank()) {
                ordonnanceValidee = false;
                if (ordonnanceStatusLabel != null) {
                    ordonnanceStatusLabel.setText(
                            "✗ Impossible de lire l'ordonnance. Réessayez avec une image plus nette.");
                    ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
                }
                return;
            }

            // Extraire uniquement les médicaments attendus trouvés dans le texte
            medicamentsOrdonnance = ordonnanceService.extraireMedicamentsCibles(texteOcr, medicamentsCibles);
            System.out.println("[Ordonnance] Médicaments cibles trouvés : " + medicamentsOrdonnance);

            traiterResultatOrdonnance(medicamentsOrdonnance);
        });

        task.setOnFailed(event -> {
            if (btnScanOrdonnance != null) btnScanOrdonnance.setDisable(false);
            if (ordonnanceStatusLabel != null) {
                ordonnanceStatusLabel.setText("✗ Erreur lors de l'analyse de l'ordonnance.");
                ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
            }
        });

        Thread t = new Thread(task, "ordonnance-ocr");
        t.setDaemon(true);
        t.start();
    }

    private void traiterResultatOrdonnance(List<String> medicamentsTrouves) {
        System.out.println("[Ordonnance] Médicaments extraits : " + medicamentsTrouves);

        if (medicamentsTrouves == null || medicamentsTrouves.isEmpty()) {
            ordonnanceValidee = false;
            if (ordonnanceStatusLabel != null) {
                ordonnanceStatusLabel.setText(
                        "✗ Aucun médicament reconnu dans l'ordonnance. Réessayez avec une image plus nette.");
                ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
            }
            return;
        }

        // Vérifier si l'ordonnance couvre les interactions
        boolean couvre = ordonnanceService.ordonnanceCouvreInteractions(
                medicamentsTrouves, dernieresInteractionsDangereuses);

        if (couvre) {
            ordonnanceValidee = true;

            // ── Alerte verte ordonnance approuvée ──
            configurerBlocAlerte(
                    "✅ Ordonnance approuvée",
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #166534;",
                    "-fx-background-color: #ecfdf5; -fx-border-color: #22c55e; "
                            + "-fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 16;"
            );

            if (interactionDetailsBox != null) {
                interactionDetailsBox.getChildren().clear();
                Label approuve = new Label(
                        "✅ Ordonnance vérifiée. Médicaments reconnus : "
                                + String.join(", ", medicamentsTrouves));
                approuve.setWrapText(true);
                approuve.setStyle("-fx-font-size: 13px; -fx-text-fill: #166534;");
                interactionDetailsBox.getChildren().add(approuve);
            }

            if (ordonnanceStatusLabel != null) {
                ordonnanceStatusLabel.setText("✅ Ordonnance approuvée — commande débloquée.");
                ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #166534; "
                        + "-fx-font-weight: bold;");
            }

            // Débloquer le bouton valider
            if (btnValiderCommande != null) {
                btnValiderCommande.setDisable(false);
                btnValiderCommande.setTooltip(
                        new Tooltip("Ordonnance approuvée — vous pouvez valider la commande."));
            }

            showSuccessAlert("✅ Ordonnance approuvée ! Vous pouvez valider votre commande.");

        } else {
            ordonnanceValidee = false;

            String medsListe = medicamentsTrouves.isEmpty()
                    ? "aucun médicament reconnu"
                    : String.join(", ", medicamentsTrouves);

            if (ordonnanceStatusLabel != null) {
                ordonnanceStatusLabel.setText(
                        "✗ Ordonnance insuffisante. Reconnus : " + medsListe
                                + ". Les médicaments en interaction doivent tous y figurer.");
                ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #b91c1c;");
            }

            if (btnValiderCommande != null) {
                btnValiderCommande.setDisable(true);
            }

            showFloatingToast(
                    "✗ Ordonnance insuffisante : " + medsListe + " détectés.",
                    "toast-warning", "⚠");
        }
    }
    private void masquerAlerteInteractions() {
        if (interactionAlertBox != null) {
            interactionAlertBox.setVisible(false);
            interactionAlertBox.setManaged(false);
        }
        if (interactionDetailsBox != null) {
            interactionDetailsBox.getChildren().clear();
        }
        // Cacher aussi le bloc ordonnance
        if (ordonnanceBox != null) {
            ordonnanceBox.setVisible(false);
            ordonnanceBox.setManaged(false);
        }
        ordonnanceValidee = false;
        medicamentsOrdonnance = new ArrayList<>();
    }

}