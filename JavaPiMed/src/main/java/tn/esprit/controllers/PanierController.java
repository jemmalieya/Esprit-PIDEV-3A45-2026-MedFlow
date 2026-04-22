package tn.esprit.controllers;

import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import tn.esprit.entities.Prescription;
import tn.esprit.entities.Produit;
import tn.esprit.services.DrugInteractionResult;
import tn.esprit.services.OpenFdaInteractionService;
import tn.esprit.services.OrdonnanceExtractorService;
import tn.esprit.services.ProduitPrescriptionService;
import tn.esprit.session.CartSession;
import tn.esprit.tools.SessionManager;
import tn.esprit.entities.User;
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class PanierController {

    private static final boolean DEBUG_OPENFDA = isDebugEnabled();

    // ── FXML ─────────────────────────────────────────────────────────────────
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

    // ── Services ──────────────────────────────────────────────────────────────
    private final OrdonnanceExtractorService ordonnanceService         = new OrdonnanceExtractorService();
    private final ProduitPrescriptionService produitPrescriptionService = new ProduitPrescriptionService();

    // ── État ordonnance / prescription ────────────────────────────────────────
    private boolean ordonnanceValidee            = false;
    private boolean prescriptionNumeriqueValidee = false;
    private List<String> medicamentsOrdonnance   = new ArrayList<>();

    // ── État interactions openFDA ─────────────────────────────────────────────
    private List<DrugInteractionResult> dernieresInteractionsDangereuses = Collections.emptyList();
    private boolean interactionDangereusePresente = false;
    private boolean analyseOpenFdaIndisponible    = false;
    private String  messageAnalyseOpenFda         = "";
    private long    interactionCheckVersion       = 0;
    private String  dernierEtatAlerteInteractions = "";
    private volatile Task<InteractionCheckOutcome> interactionTaskEnCours;

    // ─────────────────────────────────────────────────────────────────────────
    // DEBUG
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean isDebugEnabled() {
        String prop = System.getProperty("openfda.debug");
        if (prop != null) return Boolean.parseBoolean(prop);
        String env = System.getenv("OPENFDA_DEBUG");
        return env != null && !env.isBlank() && !"false".equalsIgnoreCase(env);
    }

    private void debug(String message) {
        if (DEBUG_OPENFDA) System.out.println("[Panier/openFDA] " + message);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALISATION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        refreshPanier();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REFRESH PANIER
    // ─────────────────────────────────────────────────────────────────────────

    private void refreshPanier() {
        List<Produit> panier = CartSession.getPanier();

        if (panierTitleLabel != null) panierTitleLabel.setText("Mon Panier");

        if (panier.isEmpty()) {
            if (emptyCartBox   != null) { emptyCartBox.setVisible(true);    emptyCartBox.setManaged(true);    }
            if (cartContentBox != null) { cartContentBox.setVisible(false); cartContentBox.setManaged(false); }
            if (btnViderPanier != null) btnViderPanier.setDisable(true);
            if (panierSubtitleLabel != null) panierSubtitleLabel.setText("Votre panier est vide");
            if (articleCountLabel   != null) articleCountLabel.setText("Articles (0)");
            if (sousTotalLabel != null) sousTotalLabel.setText("0.00 DT");
            if (totalLabel     != null) totalLabel.setText("0.00 DT");
            if (panierContainer != null) panierContainer.getChildren().clear();

            dernieresInteractionsDangereuses = Collections.emptyList();
            interactionDangereusePresente    = false;
            analyseOpenFdaIndisponible       = false;
            messageAnalyseOpenFda            = "";
            dernierEtatAlerteInteractions    = "vide";
            interactionCheckVersion++;
            masquerAlerteInteractions();

            if (btnValiderCommande != null) {
                btnValiderCommande.setDisable(true);
                btnValiderCommande.setTooltip(null);
            }
            return;
        }

        if (emptyCartBox   != null) { emptyCartBox.setVisible(false);  emptyCartBox.setManaged(false);  }
        if (cartContentBox != null) { cartContentBox.setVisible(true); cartContentBox.setManaged(true); }
        if (btnViderPanier != null) btnViderPanier.setDisable(false);

        if (panierSubtitleLabel != null)
            panierSubtitleLabel.setText("Vous avez " + panier.size() + " article(s) dans votre panier");

        Map<Integer, Produit> produitsUniques = new LinkedHashMap<>();
        for (Produit p : panier) produitsUniques.putIfAbsent(p.getId_produit(), p);

        if (panierContainer != null) panierContainer.getChildren().clear();

        double totalGeneral = 0.0;
        for (Produit produit : produitsUniques.values()) {
            int    quantite   = CartSession.getQuantiteProduit(produit);
            double totalProd  = produit.getPrix_produit() * quantite;
            totalGeneral     += totalProd;
            if (panierContainer != null)
                panierContainer.getChildren().add(createPanierRow(produit, quantite, totalProd));
        }

        if (articleCountLabel != null) articleCountLabel.setText("Articles (" + produitsUniques.size() + ")");
        if (sousTotalLabel    != null) sousTotalLabel.setText(formatPrix(totalGeneral));
        if (totalLabel        != null) totalLabel.setText(formatPrix(totalGeneral));

        lancerVerificationInteractionsAsynchrone(panier);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFICATION INTERACTIONS openFDA (asynchrone)
    // ─────────────────────────────────────────────────────────────────────────

    private void lancerVerificationInteractionsAsynchrone(List<Produit> panier) {
        final long version           = ++interactionCheckVersion;
        final List<Produit> snapshot = new ArrayList<>(panier);

        Task<InteractionCheckOutcome> ancienneTask = interactionTaskEnCours;
        if (ancienneTask != null && ancienneTask.isRunning()) {
            ancienneTask.cancel();
        }

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
            if (version != interactionCheckVersion) return;
            appliquerEtatInteractions(task.getValue());
            interactionTaskEnCours = null;
        });

        task.setOnFailed(event -> {
            if (version != interactionCheckVersion) return;
            dernieresInteractionsDangereuses = Collections.emptyList();
            interactionDangereusePresente    = false;
            analyseOpenFdaIndisponible       = true;
            messageAnalyseOpenFda            = "Erreur lors de l'analyse openFDA.";
            afficherAlerteAnalyseIndisponible(messageAnalyseOpenFda);
            if (btnValiderCommande != null) {
                btnValiderCommande.setDisable(false);
                btnValiderCommande.setTooltip(new Tooltip("Analyse openFDA indisponible : vérifiez manuellement."));
            }
            interactionTaskEnCours = null;
        });

        task.setOnCancelled(event -> {
            if (interactionTaskEnCours == task) {
                interactionTaskEnCours = null;
            }
        });

        interactionTaskEnCours = task;

        Thread thread = new Thread(task, "openfda-panier-check");
        thread.setDaemon(true);
        thread.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIGNE PANIER
    // ─────────────────────────────────────────────────────────────────────────

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
        Label prixUnitText  = new Label("Prix unitaire");
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
        Label totalText  = new Label("Total");
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

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIONS FXML
    // ─────────────────────────────────────────────────────────────────────────

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
            if (url == null) throw new IOException("Fichier introuvable : /FrontFXML/Pharmacie.fxml");
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

        // Bloquer si interaction ET ni ordonnance ni prescription validée
        if (interactionDangereusePresente && !ordonnanceValidee && !prescriptionNumeriqueValidee) {
            showAlert("Interactions dangereuses",
                    "Uploadez une ordonnance ou utilisez votre prescription pour débloquer.");
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

    // ─────────────────────────────────────────────────────────────────────────
    // SCANNER ORDONNANCE (image / PDF)
    // ─────────────────────────────────────────────────────────────────────────

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

        final List<String> medicamentsCibles = construireMedicamentsCibles();
        final File fichierFinal = fichier;

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
        if (medicamentsTrouves == null || medicamentsTrouves.isEmpty()) {
            ordonnanceValidee = false;
            if (ordonnanceStatusLabel != null) {
                ordonnanceStatusLabel.setText(
                        "✗ Aucun médicament reconnu dans l'ordonnance. Réessayez avec une image plus nette.");
                ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
            }
            return;
        }

        boolean couvre = ordonnanceService.ordonnanceCouvreInteractions(
                medicamentsTrouves, dernieresInteractionsDangereuses);

        if (couvre) {
            ordonnanceValidee = true;
            configurerBlocAlerte(
                    "✅ Ordonnance approuvée",
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #166534;",
                    "-fx-background-color: #ecfdf5; -fx-border-color: #22c55e; "
                            + "-fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 16;"
            );
            if (interactionDetailsBox != null) {
                interactionDetailsBox.getChildren().clear();
                Label approuve = new Label("✅ Ordonnance vérifiée. Médicaments reconnus : "
                        + String.join(", ", medicamentsTrouves));
                approuve.setWrapText(true);
                approuve.setStyle("-fx-font-size: 13px; -fx-text-fill: #166534;");
                interactionDetailsBox.getChildren().add(approuve);
            }
            if (ordonnanceStatusLabel != null) {
                ordonnanceStatusLabel.setText("✅ Ordonnance approuvée — commande débloquée.");
                ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #166534; -fx-font-weight: bold;");
            }
            if (btnValiderCommande != null) {
                btnValiderCommande.setDisable(false);
                btnValiderCommande.setTooltip(new Tooltip("Ordonnance approuvée — vous pouvez valider la commande."));
            }
            showSuccessAlert("✅ Ordonnance approuvée ! Vous pouvez valider votre commande.");

        } else {
            ordonnanceValidee = false;
            List<String> cibles    = construireMedicamentsCibles();
            List<String> manquants = new ArrayList<>();
            for (String cible : cibles) {
                boolean trouve = medicamentsTrouves.stream()
                        .anyMatch(m -> produitPrescriptionService.correspondanceFloue(m, cible));
                if (!trouve) manquants.add(cible);
            }
            String msgManquants = manquants.isEmpty()
                    ? "médicaments non reconnus"
                    : "Manquants : " + String.join(", ", manquants);

            if (ordonnanceStatusLabel != null) {
                ordonnanceStatusLabel.setText("✗ Ordonnance insuffisante. " + msgManquants
                        + ". Tous les médicaments en interaction doivent y figurer.");
                ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #b91c1c;");
            }
            if (btnValiderCommande != null) btnValiderCommande.setDisable(true);
            showFloatingToast("✗ Ordonnance insuffisante. " + msgManquants, "toast-warning", "⚠");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRESCRIPTION NUMÉRIQUE — bouton "💊 Utiliser ma prescription"
    // ─────────────────────────────────────────────────────────────────────────

    private void chargerPrescriptionNumerique() {
        // Récupérer l'utilisateur connecté (même pattern que CommandeController)
        User user = SessionManager.getCurrentUser();
        if (user == null || user.getId() <= 0) {
            showFloatingToast("Veuillez vous connecter pour utiliser votre prescription.",
                    "toast-warning", "⚠");
            return;
        }

        List<Prescription> prescriptions =
                produitPrescriptionService.getPrescriptionsParPatient(user.getId());

        if (prescriptions.isEmpty()) {
            showFloatingToast("Aucune prescription trouvée dans votre dossier médical.",
                    "toast-warning", "⚠");
            return;
        }

        // Médicaments attendus = ceux des interactions détectées
        List<String> medicamentsCibles = construireMedicamentsCibles();

        // Tous les médicaments prescrits (noms normalisés)
        Set<String> medsPrescritsTous = prescriptions.stream()
                .map(p -> p.getNom_medicament().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        System.out.println("[Prescription] Prescrits : " + medsPrescritsTous);
        System.out.println("[Prescription] Cibles    : " + medicamentsCibles);

        // Vérifier si tous les médicaments en interaction sont couverts
        List<String> medsNonCouverts = new ArrayList<>();
        for (String cible : medicamentsCibles) {
            boolean trouve = medsPrescritsTous.stream()
                    .anyMatch(prescrit -> produitPrescriptionService.correspondanceFloue(prescrit, cible));
            if (!trouve) medsNonCouverts.add(cible);
        }

        if (!medsNonCouverts.isEmpty()) {
            // Prescription insuffisante
            prescriptionNumeriqueValidee = false;
            if (ordonnanceStatusLabel != null) {
                ordonnanceStatusLabel.setText(
                        "✗ Prescription insuffisante. Médicaments manquants dans votre dossier : "
                                + String.join(", ", medsNonCouverts));
                ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #b91c1c;");
            }
            showFloatingToast("✗ Prescription insuffisante. Manquants : "
                    + String.join(", ", medsNonCouverts), "toast-warning", "⚠");
            return;
        }

        // ── Prescription valide : ajouter les médicaments au panier ──────────
        prescriptionNumeriqueValidee = true;
        ordonnanceValidee            = true;

        List<String> ajoutesAuPanier = new ArrayList<>();
        List<String> dejaPresents    = new ArrayList<>();
        List<String> introuvables    = new ArrayList<>();

        for (Prescription pr : prescriptions) {
            // Vérifier si ce médicament est une cible (en interaction)
            boolean estCible = medicamentsCibles.stream()
                    .anyMatch(c -> produitPrescriptionService.correspondanceFloue(pr.getNom_medicament(), c));
            if (!estCible) continue;

            // Chercher le produit correspondant en stock
            Produit produit = produitPrescriptionService.trouverProduitParNom(pr.getNom_medicament());

            if (produit != null && produit.getQuantite_produit() > 0) {
                boolean dejaPresent = CartSession.getPanier().stream()
                        .anyMatch(p -> p.getId_produit() == produit.getId_produit());
                if (dejaPresent) {
                    dejaPresents.add(produit.getNom_produit());
                } else {
                    CartSession.ajouterProduit(produit);
                    ajoutesAuPanier.add(produit.getNom_produit());
                }
            } else {
                introuvables.add(pr.getNom_medicament());
            }
        }

        // Rafraîchir l'affichage si des produits ont été ajoutés
        if (!ajoutesAuPanier.isEmpty()) {
            refreshPanier();
        }

        // ── Mettre à jour le bloc alerte ──────────────────────────────────────
        configurerBlocAlerte(
                "✅ Prescription médicale approuvée",
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #166534;",
                "-fx-background-color: #ecfdf5; -fx-border-color: #22c55e; "
                        + "-fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 16;"
        );

        if (interactionDetailsBox != null) {
            interactionDetailsBox.getChildren().clear();

            // Détail des prescriptions couvrantes
            VBox detailBox = new VBox(4);
            for (Prescription pr : prescriptions) {
                boolean estCible = medicamentsCibles.stream()
                        .anyMatch(c -> produitPrescriptionService.correspondanceFloue(pr.getNom_medicament(), c));
                if (!estCible) continue;

                Label lignePr = new Label("💊 " + pr.getNom_medicament()
                        + "  —  dose : " + pr.getDose()
                        + "  |  " + pr.getFrequence()
                        + "  |  " + pr.getDuree() + " jours");
                lignePr.setStyle("-fx-font-size: 13px; -fx-text-fill: #14532d;");
                detailBox.getChildren().add(lignePr);
            }
            interactionDetailsBox.getChildren().add(detailBox);

            if (!ajoutesAuPanier.isEmpty()) {
                Label lblAjoutes = new Label("🛒 Ajouté(s) au panier : " + String.join(", ", ajoutesAuPanier));
                lblAjoutes.setWrapText(true);
                lblAjoutes.setStyle("-fx-font-size: 13px; -fx-text-fill: #166534; -fx-font-weight: bold;");
                interactionDetailsBox.getChildren().add(lblAjoutes);
            }
            if (!dejaPresents.isEmpty()) {
                Label lblDeja = new Label("ℹ Déjà dans le panier : " + String.join(", ", dejaPresents));
                lblDeja.setWrapText(true);
                lblDeja.setStyle("-fx-font-size: 13px; -fx-text-fill: #1d4ed8;");
                interactionDetailsBox.getChildren().add(lblDeja);
            }
            if (!introuvables.isEmpty()) {
                Label lblIntro = new Label("⚠ Introuvables en pharmacie : " + String.join(", ", introuvables));
                lblIntro.setWrapText(true);
                lblIntro.setStyle("-fx-font-size: 13px; -fx-text-fill: #92400e;");
                interactionDetailsBox.getChildren().add(lblIntro);
            }
        }

        if (ordonnanceStatusLabel != null) {
            ordonnanceStatusLabel.setText("✅ Prescription approuvée — commande débloquée.");
            ordonnanceStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #166534; -fx-font-weight: bold;");
        }

        if (btnValiderCommande != null) {
            btnValiderCommande.setDisable(false);
            btnValiderCommande.setTooltip(new Tooltip("Prescription médicale approuvée — commande autorisée."));
        }

        String toastMsg = ajoutesAuPanier.isEmpty()
                ? "✅ Prescription approuvée — médicaments déjà dans le panier."
                : "✅ Ajouté au panier : " + String.join(", ", ajoutesAuPanier);
        showSuccessAlert(toastMsg);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ALERTES INTERACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    private void appliquerEtatInteractions(InteractionCheckOutcome outcome) {
        dernieresInteractionsDangereuses = outcome.interactions == null
                ? Collections.emptyList() : outcome.interactions;
        interactionDangereusePresente = !dernieresInteractionsDangereuses.isEmpty();
        analyseOpenFdaIndisponible    = outcome.analyseIndisponible;
        messageAnalyseOpenFda         = outcome.message;

        String nouvelEtat = interactionDangereusePresente ? "danger"
                : analyseOpenFdaIndisponible ? "indisponible" : "ok";

        if (nouvelEtat.equals(dernierEtatAlerteInteractions)) {
            if (btnValiderCommande != null)
                btnValiderCommande.setDisable(
                        interactionDangereusePresente && !ordonnanceValidee && !prescriptionNumeriqueValidee);
            return;
        }
        dernierEtatAlerteInteractions = nouvelEtat;

        if (!interactionDangereusePresente) {
            if (analyseOpenFdaIndisponible) afficherAlerteAnalyseIndisponible(messageAnalyseOpenFda);
            else afficherAlerteAucuneInteraction();
            if (btnValiderCommande != null) {
                btnValiderCommande.setDisable(false);
                btnValiderCommande.setTooltip(analyseOpenFdaIndisponible
                        ? new Tooltip("Analyse openFDA indisponible.") : null);
            }
            return;
        }

        afficherAlerteInteractions(dernieresInteractionsDangereuses);
        boolean bloquer = !ordonnanceValidee && !prescriptionNumeriqueValidee;
        if (btnValiderCommande != null) {
            btnValiderCommande.setDisable(bloquer);
            btnValiderCommande.setTooltip(bloquer
                    ? new Tooltip("Uploadez une ordonnance ou utilisez votre prescription.")
                    : new Tooltip("Commande autorisée."));
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
        if (ordonnanceBox != null) { ordonnanceBox.setVisible(false); ordonnanceBox.setManaged(false); }
        ordonnanceValidee            = false;
        prescriptionNumeriqueValidee = false;

        configurerBlocAlerte(
                "✅ Aucune interaction dangereuse détectée",
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #166534;",
                "-fx-background-color: #ecfdf5; -fx-border-color: #22c55e; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 16;"
        );
        interactionDetailsBox.getChildren().clear();
        Label detail = new Label("Vérification openFDA terminée : vous pouvez valider la commande.");
        detail.setWrapText(true);
        detail.setStyle("-fx-font-size: 13px; -fx-text-fill: #14532d;");
        interactionDetailsBox.getChildren().add(detail);
        interactionAlertBox.setVisible(true);
        interactionAlertBox.setManaged(true);
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

        // Afficher le bloc ordonnance + ajouter le bouton prescription si absent
        if (ordonnanceBox != null) {
            ordonnanceBox.setVisible(true);
            ordonnanceBox.setManaged(true);
            ajouterBoutonPrescriptionSiAbsent(ordonnanceBox);
        }

        if (!ordonnanceValidee && !prescriptionNumeriqueValidee) {
            if (ordonnanceStatusLabel != null) {
                ordonnanceStatusLabel.setText(
                        "⚠ Commande bloquée. Uploadez une ordonnance ou utilisez votre prescription.");
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

    /**
     * Ajoute le bouton "💊 Utiliser ma prescription" dans ordonnanceBox
     * seulement s'il n'est pas déjà présent.
     */
    private void ajouterBoutonPrescriptionSiAbsent(HBox ordonnanceBox) {
        boolean dejaPresent = ordonnanceBox.getChildren().stream()
                .anyMatch(n -> "btnPrescription".equals(n.getId()));
        if (dejaPresent) return;

        Button btnPrescription = new Button("💊 Utiliser ma prescription");
        btnPrescription.setId("btnPrescription");
        btnPrescription.setStyle(
                "-fx-background-color: #7c3aed; -fx-text-fill: white; "
                        + "-fx-font-size: 13px; -fx-font-weight: bold; "
                        + "-fx-background-radius: 8; -fx-padding: 10 18; -fx-cursor: hand;"
        );
        btnPrescription.setOnAction(e -> chargerPrescriptionNumerique());
        ordonnanceBox.getChildren().add(btnPrescription);
    }

    private void configurerBlocAlerte(String titre, String styleTitre, String styleBloc) {
        if (interactionAlertBox     != null) interactionAlertBox.setStyle(styleBloc);
        if (interactionAlertTitleLabel != null) {
            interactionAlertTitleLabel.setText(titre);
            interactionAlertTitleLabel.setStyle(styleTitre);
        }
    }

    private void masquerAlerteInteractions() {
        if (interactionAlertBox != null) {
            interactionAlertBox.setVisible(false);
            interactionAlertBox.setManaged(false);
        }
        if (interactionDetailsBox != null) interactionDetailsBox.getChildren().clear();
        if (ordonnanceBox != null) { ordonnanceBox.setVisible(false); ordonnanceBox.setManaged(false); }
        ordonnanceValidee            = false;
        prescriptionNumeriqueValidee = false;
        medicamentsOrdonnance        = new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Construit la liste des médicaments attendus
     * à partir des interactions détectées (sans doublons).
     */
    private List<String> construireMedicamentsCibles() {
        List<String> cibles = new ArrayList<>();
        for (DrugInteractionResult interaction : dernieresInteractionsDangereuses) {
            String a = interaction.getMedicamentA();
            String b = interaction.getMedicamentB();
            if (!cibles.contains(a)) cibles.add(a);
            if (!cibles.contains(b)) cibles.add(b);
        }
        return cibles;
    }

    private String formatPrix(double prix) {
        return String.format(Locale.US, "%.2f DT", prix);
    }

    private void showAlert(String title, String content) {
        showFloatingToast((title == null || title.isBlank() ? content : title + " : " + content),
                "toast-info", "ℹ");
    }

    private void showStockInsufficiencyAlert(String productName, int availableQuantity) {
        showFloatingToast("Stock insuffisant pour " + productName
                + " (reste : " + availableQuantity + ")", "toast-warning", "⚠");
    }

    private void showSuccessAlert(String message) {
        showFloatingToast(message, "toast-success", "✅");
    }

    private void showFloatingToast(String message, String styleClass, String iconText) {
        if (panierContainer == null || panierContainer.getScene() == null) return;
        Scene scene = panierContainer.getScene();
        javafx.stage.Window window = scene.getWindow();
        if (window == null) return;

        HBox toast = new HBox(10);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getStyleClass().addAll("floating-toast", styleClass);
        toast.setMaxWidth(360);

        Label icon      = new Label(iconText);
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
        if (cssUrl != null && popup.getScene() != null)
            popup.getScene().getStylesheets().add(cssUrl.toExternalForm());

        toast.applyCss();
        toast.layout();

        double popupWidth = Math.max(320, toast.prefWidth(-1));
        popup.setX(window.getX() + scene.getWidth() - popupWidth - 24);
        popup.setY(window.getY() + scene.getHeight() - 110);

        toast.setOpacity(0);
        toast.setTranslateY(16);
        icon.setScaleX(0.7);
        icon.setScaleY(0.7);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), toast);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);

        javafx.animation.TranslateTransition slideIn =
                new javafx.animation.TranslateTransition(Duration.millis(220), toast);
        slideIn.setFromY(16); slideIn.setToY(0);

        javafx.animation.ScaleTransition popIcon =
                new javafx.animation.ScaleTransition(Duration.millis(260), icon);
        popIcon.setFromX(0.7); popIcon.setFromY(0.7);
        popIcon.setToX(1.0);  popIcon.setToY(1.0);

        javafx.animation.ParallelTransition enter =
                new javafx.animation.ParallelTransition(fadeIn, slideIn, popIcon);

        PauseTransition pause = new PauseTransition(Duration.seconds(2.1));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(220), toast);
        fadeOut.setFromValue(1); fadeOut.setToValue(0);

        javafx.animation.TranslateTransition slideOut =
                new javafx.animation.TranslateTransition(Duration.millis(220), toast);
        slideOut.setFromY(0); slideOut.setToY(10);

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
        if (cssUrl != null) pane.getStylesheets().add(cssUrl.toExternalForm());
        pane.getStyleClass().add("confirm-dialog");

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));

        Label icon      = new Label("⚠");
        icon.getStyleClass().add("confirm-dialog-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("confirm-dialog-title");
        titleLabel.setWrapText(true);
        Label msgLabel   = new Label(message);
        msgLabel.getStyleClass().add("confirm-dialog-message");
        msgLabel.setWrapText(true);

        content.getChildren().addAll(icon, titleLabel, msgLabel);
        pane.setContent(content);

        Button okButton     = (Button) pane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) pane.lookupButton(ButtonType.CANCEL);
        okButton.setText(confirmText);
        cancelButton.setText("Annuler");
        okButton.getStyleClass().add("confirm-danger-btn");
        cancelButton.getStyleClass().add("confirm-cancel-btn");

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INNER CLASS
    // ─────────────────────────────────────────────────────────────────────────

    private static class InteractionCheckOutcome {
        private final List<DrugInteractionResult> interactions;
        private final boolean analyseIndisponible;
        private final String  message;

        private InteractionCheckOutcome(List<DrugInteractionResult> interactions,
                                        boolean analyseIndisponible, String message) {
            this.interactions       = interactions;
            this.analyseIndisponible = analyseIndisponible;
            this.message            = message;
        }
    }
}
