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
import javafx.scene.layout.*;
import javafx.stage.Stage;
import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.entities.User;
import tn.esprit.services.CommandeService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdminCommandeController {

    @FXML private BorderPane rootPane;

    @FXML private Label dateLabel;
    @FXML private TextField topSearchField;

    @FXML private Label totalCommandesValueLabel;
    @FXML private Label attenteLabel;
    @FXML private Label coursLabel;
    @FXML private Label livraisonLabel;
    @FXML private Label livreesLabel;
    @FXML private Label annuleesLabel;
    @FXML private Label caTotalLabel;
    @FXML private Label caMoisLabel;
    @FXML private Label resultCountLabel;

    @FXML private TextField searchCommandeField;
    @FXML private ComboBox<String> filtreStatutCombo;
    @FXML private VBox commandesRowsContainer;

    private final CommandeService commandeService = new CommandeService();
    private final List<Commande> allCommandes = new ArrayList<>();
    private final List<Commande> filteredCommandes = new ArrayList<>();

    @FXML
    public void initialize() {
        if (dateLabel != null) {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            dateLabel.setText("Admin • Commandes • " + date);
        }

        if (filtreStatutCombo != null) {
            filtreStatutCombo.setItems(FXCollections.observableArrayList(
                    "Tous",
                    "En attente",
                    "En cours",
                    "Livraison",
                    "Terminée",
                    "Annulée",
                    "Payée",
                    "Non payée"
            ));
            filtreStatutCombo.setValue("Tous");
            filtreStatutCombo.setOnAction(e -> applyFilters());
        }

        if (searchCommandeField != null) {
            searchCommandeField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        }

        if (topSearchField != null) {
            topSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (searchCommandeField != null && !Objects.equals(searchCommandeField.getText(), newValue)) {
                    searchCommandeField.setText(newValue);
                }
            });
        }

        Platform.runLater(this::loadCommandes);
    }

    private void loadCommandes() {
        try {
            allCommandes.clear();
            allCommandes.addAll(commandeService.recuperer());
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
            showWarning("Erreur", "Impossible de charger les commandes : " + e.getMessage());
        }
    }

    private void applyFilters() {
        filteredCommandes.clear();

        String keyword = searchCommandeField != null && searchCommandeField.getText() != null
                ? searchCommandeField.getText().trim().toLowerCase(Locale.ROOT)
                : "";

        String filtre = filtreStatutCombo != null && filtreStatutCombo.getValue() != null
                ? filtreStatutCombo.getValue()
                : "Tous";

        for (Commande c : allCommandes) {
            if (!matchesKeyword(c, keyword)) {
                continue;
            }

            if (!matchesFilter(c, filtre)) {
                continue;
            }

            filteredCommandes.add(c);
        }

        updateStats(filteredCommandes);
        renderRows();
    }

    private boolean matchesKeyword(Commande c, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String id = String.valueOf(c.getId_commande());
        String statut = safe(c.getStatut_commande());
        User user = c.getUser();
        String email = user == null ? "" : safe(user.getEmailUser());
        String nom = user == null ? "" : safe(user.getPrenom()) + " " + safe(user.getNom());

        String all = (id + " " + statut + " " + email + " " + nom).toLowerCase(Locale.ROOT);
        return all.contains(keyword);
    }

    private boolean matchesFilter(Commande c, String filtre) {
        if (filtre == null || filtre.equalsIgnoreCase("Tous")) {
            return true;
        }

        String statut = normalizeStatut(c.getStatut_commande()).toLowerCase(Locale.ROOT);

        switch (filtre) {
            case "En attente":
                return statut.contains("attente");
            case "En cours":
                return statut.contains("cours");
            case "Livraison":
                return statut.contains("livraison");
            case "Terminée":
                return statut.contains("termin") || statut.contains("final") || statut.contains("livrée");
            case "Annulée":
                return statut.contains("annul");
            case "Payée":
                return isPayee(c);
            case "Non payée":
                return !isPayee(c);
            default:
                return true;
        }
    }

    private void updateStats(List<Commande> commandes) {
        int total = commandes.size();
        int attente = 0;
        int cours = 0;
        int livraison = 0;
        int livrees = 0;
        int annulees = 0;

        double caTotal = 0;
        double caMois = 0;

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        for (Commande c : commandes) {
            String statut = normalizeStatut(c.getStatut_commande()).toLowerCase(Locale.ROOT);

            if (statut.contains("attente")) attente++;
            else if (statut.contains("cours")) cours++;
            else if (statut.contains("livraison")) livraison++;
            else if (statut.contains("termin") || statut.contains("final") || statut.contains("livrée")) livrees++;
            else if (statut.contains("annul")) annulees++;

            double montant = c.getMontant_total_cents() / 100.0;

            if (isPayee(c)) {
                caTotal += montant;

                String dateText = String.valueOf(c.getDate_creation_commande());
                if (dateText.startsWith(currentMonth)) {
                    caMois += montant;
                }
            }
        }

        if (totalCommandesValueLabel != null) totalCommandesValueLabel.setText(String.valueOf(total));
        if (attenteLabel != null) attenteLabel.setText(String.valueOf(attente));
        if (coursLabel != null) coursLabel.setText(String.valueOf(cours));
        if (livraisonLabel != null) livraisonLabel.setText(String.valueOf(livraison));
        if (livreesLabel != null) livreesLabel.setText(String.valueOf(livrees));
        if (annuleesLabel != null) annuleesLabel.setText(String.valueOf(annulees));
        if (caTotalLabel != null) caTotalLabel.setText(formatMoney(caTotal));
        if (caMoisLabel != null) caMoisLabel.setText(formatMoney(caMois));
        if (resultCountLabel != null) resultCountLabel.setText(total + " commande(s)");
    }

    private void renderRows() {
        if (commandesRowsContainer == null) return;

        commandesRowsContainer.getChildren().clear();

        if (filteredCommandes.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(45));

            Label title = new Label("Aucune commande trouvée");
            title.getStyleClass().add("empty-title");

            Label text = new Label("Essayez une autre recherche ou changez le filtre.");
            text.getStyleClass().add("empty-text");

            empty.getChildren().addAll(title, text);
            commandesRowsContainer.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < filteredCommandes.size(); i++) {
            commandesRowsContainer.getChildren().add(createRow(filteredCommandes.get(i), i));
        }
    }

    private HBox createRow(Commande c, int index) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("commande-row");

        if (index % 2 == 0) {
            row.getStyleClass().add("commande-row-light");
        }

        Label id = new Label("#" + c.getId_commande());
        id.getStyleClass().add("cmd-id");
        VBox idCell = cell(id, 64);

        VBox clientBox = new VBox(4);
        clientBox.setPrefWidth(260);
        clientBox.setMinWidth(260);
        clientBox.setMaxWidth(260);

        User user = c.getUser();
        String email = user == null ? "Client inconnu" : displayValue(user.getEmailUser());
        String userId = user == null ? "—" : String.valueOf(user.getId());

        Label clientEmail = new Label(email);
        clientEmail.getStyleClass().add("client-email");
        clientEmail.setMaxWidth(245);
        clientEmail.setWrapText(false);

        Label clientId = new Label("ID User: " + userId);
        clientId.getStyleClass().add("client-id");

        clientBox.getChildren().addAll(clientEmail, clientId);

        VBox dateBox = new VBox(4);
        dateBox.setPrefWidth(125);
        dateBox.setMinWidth(125);
        dateBox.setMaxWidth(125);

        String[] dateParts = splitDateTime(String.valueOf(c.getDate_creation_commande()));

        Label date = new Label(dateParts[0]);
        date.getStyleClass().add("date-main");

        Label time = new Label(dateParts[1]);
        time.getStyleClass().add("date-sub");

        dateBox.getChildren().addAll(date, time);

        Label montant = new Label(formatMoney(c.getMontant_total_cents() / 100.0));
        montant.getStyleClass().add("montant-text");
        VBox montantCell = cell(montant, 112);

        Label statut = new Label(normalizeStatut(c.getStatut_commande()));
        statut.getStyleClass().addAll("statut-badge", getStatusClass(c.getStatut_commande()));
        VBox statutCell = cell(statut, 125);

        Label paiement = new Label(isPayee(c) ? "Payée" : "Non payée");
        paiement.getStyleClass().add(isPayee(c) ? "payment-paid" : "payment-unpaid");
        VBox paiementCell = cell(paiement, 120);



        Button eyeBtn = new Button("👁");
        eyeBtn.getStyleClass().add("eye-button");
        eyeBtn.setOnAction(e -> showCommandeDetailsPopup(c));

        VBox detailsCell = cell(eyeBtn, 78);

        row.getChildren().addAll(
                idCell,
                clientBox,
                dateBox,
                montantCell,
                statutCell,
                paiementCell,
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

    private void showCommandeDetailsPopup(Commande c) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails commande");
        dialog.setHeaderText(null);
        dialog.setResizable(false);

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);

        URL cssUrl = getClass().getResource("/CSS/admin-commandes.css");
        if (cssUrl != null) {
            pane.getStylesheets().add(cssUrl.toExternalForm());
        }

        pane.getStyleClass().add("commande-details-dialog");

        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("details-root");

        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(70, 70);
        iconBox.getStyleClass().add("details-icon-box");

        Label icon = new Label("CMD");
        icon.getStyleClass().add("details-icon-text");
        iconBox.getChildren().add(icon);

        VBox titleBox = new VBox(8);

        HBox titleLine = new HBox(10);
        titleLine.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Détails commande");
        title.getStyleClass().add("details-title");

        Label orderId = new Label("Commande #" + c.getId_commande());
        orderId.getStyleClass().add("details-id");

        Label montant = new Label(formatMoney(c.getMontant_total_cents() / 100.0));
        montant.getStyleClass().add("details-money");

        titleLine.getChildren().addAll(title, orderId, montant);

        HBox badges = new HBox(8);
        Label statut = new Label(normalizeStatut(c.getStatut_commande()));
        statut.getStyleClass().addAll("statut-badge", getStatusClass(c.getStatut_commande()));

        Label paiement = new Label(isPayee(c) ? "Payée" : "Non payée");
        paiement.getStyleClass().add(isPayee(c) ? "payment-paid" : "payment-unpaid");

        Label date = new Label(String.valueOf(c.getDate_creation_commande()));
        date.getStyleClass().add("details-date");

        badges.getChildren().addAll(statut, paiement, date);
        titleBox.getChildren().addAll(titleLine, badges);

        header.getChildren().addAll(iconBox, titleBox);

        HBox body = new HBox(24);

        VBox left = new VBox(18);
        left.setPrefWidth(420);

        VBox clientCard = new VBox(14);
        clientCard.getStyleClass().add("details-card");

        Label clientTitle = new Label("Client");
        clientTitle.getStyleClass().add("details-card-title");

        User user = c.getUser();

        VBox clientInfoBox = new VBox(4);
        clientInfoBox.getStyleClass().add("client-info-box");

        Label clientEmail = new Label(user == null ? "Client inconnu" : displayValue(user.getEmailUser()));
        clientEmail.getStyleClass().add("details-client-email");

        Label clientId = new Label(user == null ? "ID User: —" : "ID User: " + user.getId());
        clientId.getStyleClass().add("details-client-id");

        clientInfoBox.getChildren().addAll(clientEmail, clientId);

        Label paymentDate = new Label("Date commande : " + displayValue(String.valueOf(c.getDate_creation_commande())));
        paymentDate.getStyleClass().add("details-small-text");

        clientCard.getChildren().addAll(clientTitle, clientInfoBox, paymentDate);

        VBox advice = new VBox(8);
        advice.getStyleClass().add("details-advice");

        Label adviceTitle = new Label("Conseil");
        adviceTitle.getStyleClass().add("advice-title");

        Label adviceText = new Label("Vérifiez les quantités et le statut avant de valider la livraison.");
        adviceText.setWrapText(true);
        adviceText.getStyleClass().add("advice-text");

        advice.getChildren().addAll(adviceTitle, adviceText);

        left.getChildren().addAll(clientCard, advice);

        VBox right = new VBox(14);
        right.setPrefWidth(760);
        right.getStyleClass().add("details-card");

        Label articlesTitle = new Label("Articles commandés");
        articlesTitle.getStyleClass().add("details-card-title");

        HBox tableHeader = new HBox();
        tableHeader.getStyleClass().add("details-table-header");

        Label hProduit = new Label("PRODUIT");
        hProduit.setPrefWidth(300);
        hProduit.getStyleClass().add("details-header-cell");

        Label hQte = new Label("QTÉ");
        hQte.setPrefWidth(110);
        hQte.getStyleClass().add("details-header-cell");

        Label hPrix = new Label("PRIX");
        hPrix.setPrefWidth(150);
        hPrix.getStyleClass().add("details-header-cell");

        Label hSous = new Label("SOUS-TOTAL");
        hSous.setPrefWidth(170);
        hSous.getStyleClass().add("details-header-cell");

        tableHeader.getChildren().addAll(hProduit, hQte, hPrix, hSous);

        VBox articlesContainer = new VBox(0);

        double total = 0.0;
        List<CommandeProduit> lignes = c.getCommande_produits();

        if (lignes != null) {
            for (CommandeProduit cp : lignes) {
                Produit p = cp.getProduit();
                String nom = p == null ? "Produit" : displayValue(p.getNom_produit());
                int qte = cp.getQuantite_commandee();
                double prix = p == null ? 0.0 : p.getPrix_produit();
                double sousTotal = prix * qte;
                total += sousTotal;

                articlesContainer.getChildren().add(createDetailArticleRow(nom, qte, prix, sousTotal));
            }
        }

        HBox totalRow = new HBox();
        totalRow.getStyleClass().add("details-total-row");
        totalRow.setAlignment(Pos.CENTER_LEFT);

        VBox totalTextBox = new VBox(4);
        Label totalTitle = new Label("Total commande");
        totalTitle.getStyleClass().add("details-total-title");

        Label totalSub = new Label("Montant total (DT)");
        totalSub.getStyleClass().add("details-total-sub");

        totalTextBox.getChildren().addAll(totalTitle, totalSub);

        Region totalSpacer = new Region();
        HBox.setHgrow(totalSpacer, Priority.ALWAYS);

        Label totalValue = new Label(formatMoney(total));
        totalValue.getStyleClass().add("details-total-value");

        totalRow.getChildren().addAll(totalTextBox, totalSpacer, totalValue);

        right.getChildren().addAll(articlesTitle, tableHeader, articlesContainer, totalRow);

        body.getChildren().addAll(left, right);
        root.getChildren().addAll(header, body);

        pane.setContent(root);
        pane.setPrefWidth(1320);
        pane.setPrefHeight(720);

        Button close = (Button) pane.lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.getStyleClass().add("details-close-button");

        dialog.showAndWait();
    }

    private HBox createDetailArticleRow(String nom, int qte, double prix, double sousTotal) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("details-article-row");

        Label nomLabel = new Label(nom);
        nomLabel.setPrefWidth(300);
        nomLabel.getStyleClass().add("details-product-name");

        Label qteLabel = new Label(String.valueOf(qte));
        qteLabel.setPrefWidth(110);
        qteLabel.getStyleClass().add("details-qte-badge");

        Label prixLabel = new Label(formatMoney(prix));
        prixLabel.setPrefWidth(150);
        prixLabel.getStyleClass().add("details-price");

        Label sousLabel = new Label(formatMoney(sousTotal));
        sousLabel.setPrefWidth(170);
        sousLabel.getStyleClass().add("details-subtotal");

        row.getChildren().addAll(nomLabel, qteLabel, prixLabel, sousLabel);
        return row;
    }

    private boolean isPayee(Commande c) {
        String statut = safe(c.getStatut_commande()).toLowerCase(Locale.ROOT);
        if (statut.contains("pay")) {
            return true;
        }

        Object paidAt = readObjectReflect(c, "getPaid_at", "getPaidAt", "getDatePaiement", "getPaidAtCommande");
        if (paidAt != null && !String.valueOf(paidAt).isBlank() && !"null".equalsIgnoreCase(String.valueOf(paidAt))) {
            return true;
        }

        return false;
    }

    private Object readObjectReflect(Object target, String... methodNames) {
        if (target == null) return null;

        for (String methodName : methodNames) {
            try {
                Method m = target.getClass().getMethod(methodName);
                return m.invoke(target);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String normalizeStatut(String statut) {
        if (statut == null || statut.isBlank()) return "En attente";

        String s = statut.toLowerCase(Locale.ROOT);

        if (s.contains("pay")) return "Payée";
        if (s.contains("confirm")) return "Confirmée";
        if (s.contains("cours")) return "En cours";
        if (s.contains("attente")) return "En attente";
        if (s.contains("livraison")) return "Livraison";
        if (s.contains("final")) return "Finalisée";
        if (s.contains("termin")) return "Terminée";
        if (s.contains("annul")) return "Annulée";

        return statut;
    }

    private String getStatusClass(String statut) {
        String s = safe(statut).toLowerCase(Locale.ROOT);

        if (s.contains("pay") || s.contains("confirm")) return "status-confirmed";
        if (s.contains("cours")) return "status-progress";
        if (s.contains("attente")) return "status-pending";
        if (s.contains("livraison")) return "status-delivery";
        if (s.contains("final") || s.contains("termin")) return "status-final";
        if (s.contains("annul")) return "status-cancel";

        return "status-pending";
    }

    private String[] splitDateTime(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("null")) {
            return new String[]{"—", ""};
        }

        String text = raw.replace("T", " ");

        if (text.contains(" ")) {
            String[] parts = text.split(" ");
            String d = parts.length > 0 ? parts[0] : text;
            String t = parts.length > 1 ? parts[1] : "";
            if (t.length() >= 5) t = t.substring(0, 5);
            return new String[]{d, t};
        }

        return new String[]{text, ""};
    }

    private String formatMoney(double value) {
        return String.format(Locale.FRANCE, "%.2f DT", value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String displayValue(String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return "—";
        }
        return value.trim();
    }

    @FXML
    private void openDashboard() {
        goTo("/AdminWelcome.fxml");
    }

    @FXML
    private void openProduits() {
        goTo("/ProduitAdmin.fxml");
    }

    @FXML
    private void openCommandes() {
        loadCommandes();
    }

    @FXML
    private void openStatsProduits() {
        goTo("/StatProduit.fxml");
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
                showWarning("Page introuvable", "Fichier introuvable : " + fxmlPath + "\nChange le chemin dans AdminCommandeController.");
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