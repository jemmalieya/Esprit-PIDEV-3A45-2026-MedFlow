package tn.esprit.controllers;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import tn.esprit.entities.AdminActionLog;
import tn.esprit.entities.User;
import tn.esprit.services.AdminActionLogService;
import tn.esprit.services.UserService;
import tn.esprit.tools.SessionManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class BAdminRiskJournalController {

    private static final Map<String, String> FXML_ALIASES = Map.of(
            "/FXML/AjoutProduit.fxml", "/AjouterProduit.fxml",
            "/FXML/AjoutEvenement.fxml", "/AjouterEvenement.fxml",
            "/FXML/Login.fxml", "/FrontFXML/Login.fxml",
            "/FXML/PostsEnAttente.fxml", "/PendingPostsAdmin.fxml",
            "/FXML/StatsBlog.fxml", "/BlogStats.fxml",
            "/FXML/StatsReclamations.fxml", "/ReclamationStats.fxml",
            "/FXML/ConsultationsParDocteur.fxml", "/FXML/ConsultationsParDocteur.fxml"
    );

    @FXML private BorderPane rootPane;
    @FXML private TextField searchField;

    @FXML private Label riskUsersCountLabel;
    @FXML private Label lockedAccountsCountLabel;
    @FXML private Label faceFailCountLabel;
    @FXML private Label suspectGeoCountLabel;
    @FXML private Label journalCountLabel;

    @FXML private TableView<RiskRow> riskTable;
    @FXML private TableColumn<RiskRow, String> riskIdColumn;
    @FXML private TableColumn<RiskRow, String> riskUserColumn;
    @FXML private TableColumn<RiskRow, String> riskEmailColumn;
    @FXML private TableColumn<RiskRow, Number> riskScoreColumn;
    @FXML private TableColumn<RiskRow, String> riskSignalsColumn;
    @FXML private TableColumn<RiskRow, String> riskLastLoginColumn;
    @FXML private TableColumn<RiskRow, String> riskLockColumn;

    @FXML private TableView<AdminActionLog> journalTable;
    @FXML private TableColumn<AdminActionLog, String> journalWhenColumn;
    @FXML private TableColumn<AdminActionLog, String> journalAdminColumn;
    @FXML private TableColumn<AdminActionLog, String> journalActionColumn;
    @FXML private TableColumn<AdminActionLog, String> journalTargetColumn;
    @FXML private TableColumn<AdminActionLog, String> journalDetailsColumn;

    private final UserService userService = new UserService();
    private final AdminActionLogService actionLogService = new AdminActionLogService();

    private final ObservableList<RiskRow> allRisks = FXCollections.observableArrayList();
    private final ObservableList<AdminActionLog> allLogs = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        actionLogService.logAction(resolveAdminLabel(), "VIEW_RISK_JOURNAL", "PAGE", 0, "Ouverture de la page Risque & Journal");
        configureRiskTable();
        configureJournalTable();
        loadData();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter(newValue));
        }
    }

    private void configureRiskTable() {
        if (riskTable == null) {
            return;
        }
        riskIdColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id));
        riskUserColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().fullName));
        riskEmailColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().email));
        riskScoreColumn.setCellValueFactory(cell -> new ReadOnlyIntegerWrapper(cell.getValue().riskScore));
        riskSignalsColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().signals));
        riskLastLoginColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().loginInfo));
        riskLockColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().lockInfo));
        riskTable.setPlaceholder(new Label("Aucun compte risque detecte."));
        riskTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void configureJournalTable() {
        if (journalTable == null) {
            return;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        journalWhenColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().getTimestamp() == null ? "-" : cell.getValue().getTimestamp().format(fmt)));
        journalAdminColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(safe(cell.getValue().getAdminName())));
        journalActionColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(safe(cell.getValue().getAction())));
        journalTargetColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                safe(cell.getValue().getTargetType()) + " #" + cell.getValue().getTargetId()));
        journalDetailsColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(safe(cell.getValue().getDetails())));
        journalTable.setPlaceholder(new Label("Aucune action admin enregistree."));
        journalTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadData() {
        List<User> users = userService.findRiskUsers();
        List<RiskRow> scoredRisks = users.stream()
                .map(this::toRiskRow)
                .filter(r -> r.riskScore > 0)
                .sorted((a, b) -> Integer.compare(b.riskScore, a.riskScore))
                .collect(Collectors.toList());

        List<RiskRow> risks;
        if (!scoredRisks.isEmpty()) {
            risks = scoredRisks;
        } else {
            risks = users.stream()
                    .map(this::toRiskRow)
                    .sorted((a, b) -> Integer.compare(b.riskScore, a.riskScore))
                    .limit(50)
                    .collect(Collectors.toList());
        }

        allRisks.setAll(risks);
        riskTable.setItems(FXCollections.observableArrayList(risks));

        List<AdminActionLog> logs = actionLogService.readRecent(200);
        allLogs.setAll(logs);
        journalTable.setItems(FXCollections.observableArrayList(logs));

        updateKpis(risks, logs);
    }

    private void applyFilter(String query) {
        String keyword = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (keyword.isBlank()) {
            riskTable.setItems(FXCollections.observableArrayList(allRisks));
            journalTable.setItems(FXCollections.observableArrayList(allLogs));
            return;
        }

        List<RiskRow> filteredRisks = allRisks.stream()
                .filter(r -> contains(r.fullName, keyword)
                        || contains(r.email, keyword)
                        || contains(r.signals, keyword)
                        || contains(r.loginInfo, keyword)
                        || contains(r.lockInfo, keyword)
                        || contains(r.id, keyword))
                .collect(Collectors.toList());

        List<AdminActionLog> filteredLogs = allLogs.stream()
                .filter(l -> contains(l.getAdminName(), keyword)
                        || contains(l.getAction(), keyword)
                        || contains(l.getTargetType(), keyword)
                        || contains(l.getDetails(), keyword)
                        || String.valueOf(l.getTargetId()).contains(keyword))
                .collect(Collectors.toList());

        riskTable.setItems(FXCollections.observableArrayList(filteredRisks));
        journalTable.setItems(FXCollections.observableArrayList(filteredLogs));
    }

    private void updateKpis(List<RiskRow> risks, List<AdminActionLog> logs) {
        long locked = risks.stream().filter(r -> contains(r.lockInfo, "verrouille")).count();
        long faceFails = risks.stream().filter(r -> contains(r.signals, "face")).count();
        long suspectGeo = risks.stream().filter(r -> contains(r.signals, "ip") || contains(r.signals, "pays")).count();

        if (riskUsersCountLabel != null) riskUsersCountLabel.setText(String.valueOf(risks.size()));
        if (lockedAccountsCountLabel != null) lockedAccountsCountLabel.setText(String.valueOf(locked));
        if (faceFailCountLabel != null) faceFailCountLabel.setText(String.valueOf(faceFails));
        if (suspectGeoCountLabel != null) suspectGeoCountLabel.setText(String.valueOf(suspectGeo));
        if (journalCountLabel != null) journalCountLabel.setText(String.valueOf(logs.size()));
    }

    private RiskRow toRiskRow(User user) {
        LocalDateTime now = LocalDateTime.now();
        int score = 0;
        List<String> signals = new ArrayList<>();

        int faceFails = user.getFaceFailedAttempts();
        if (faceFails >= 3) {
            score += Math.min(40, faceFails * 8);
            signals.add("Face: " + faceFails + " echecs");
        } else if (faceFails > 0) {
            score += 10;
            signals.add("Face: echecs moderes");
        }

        LocalDateTime lockedUntil = user.getFaceLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(now)) {
            score += 50;
            signals.add("Compte verrouille");
        }

        String ip = safe(user.getLastLoginIp());
        String country = safe(user.getLastLoginCountry());
        boolean ipSuspect = ip.isBlank() || ip.equals("127.0.0.1") || ip.equals("::1") || ip.equals("localhost");
        boolean countrySuspect = country.isBlank() || country.equalsIgnoreCase("unknown") || country.equalsIgnoreCase("n/a");
        if (ipSuspect || countrySuspect) {
            score += 20;
            signals.add("IP/Pays suspect");
        }

        LocalDateTime lastLoginAt = user.getLastLoginAt();
        if (lastLoginAt == null) {
            score += 10;
            signals.add("Historique connexion incomplet");
        }

        String lockInfo = lockedUntil == null
                ? "Aucun verrou actif"
                : (lockedUntil.isAfter(now)
                ? "Verrouille jusqu'au " + lockedUntil.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                : "Verrou expiré");

        String loginInfo = (ip.isBlank() ? "IP inconnue" : ip)
                + " | " + (country.isBlank() ? "Pays inconnu" : country)
                + " | " + (lastLoginAt == null ? "Derniere conn. inconnue" : lastLoginAt.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));

        String fullName = (safe(user.getPrenom()) + " " + safe(user.getNom())).trim();
        if (fullName.isBlank()) {
            fullName = "Utilisateur #" + user.getId();
        }

        return new RiskRow(
                String.valueOf(user.getId()),
                fullName,
                safe(user.getEmailUser()),
                score,
                signals.isEmpty() ? "Aucun signal" : String.join(" | ", signals),
                loginInfo,
                lockInfo
        );
    }

    @FXML
    private void onRefreshRiskData() {
        loadData();
    }

    @FXML
    private void onRefreshJournalData() {
        loadData();
    }

    @FXML
    private void openDashboard() {
        goTo("/AdminWelcome.fxml");
    }

    @FXML
    private void openUsers() {
        goTo("/BackFXML/BAdminUsers.fxml");
    }

    @FXML
    private void openRiskJournal() {
        // Deja sur la page.
    }

    @FXML
    private void openPatients() {
        goTo("/FXML/PatientsAdmin.fxml");
    }

    @FXML
    private void openRoles() {
        goTo("/FXML/DemandesRole.fxml");
    }

    @FXML
    private void openUserRoles() {
        goTo("/FXML/UserRoles.fxml");
    }

    @FXML
    private void openProduits() {
        goTo("/ProduitDashboard.fxml");
    }

    @FXML
    private void openCommandes() {
        goTo("/MesCommandesBack.fxml");
    }

    @FXML
    private void openDetection() {
        goTo("/DetectionEpidemie.fxml");
    }

    @FXML
    private void openStockRupture() {
        goTo("/FXML/StockRupture.fxml");
    }

    @FXML
    private void openStatsProduits() {
        goTo("/StatProduit.fxml");
    }

    @FXML
    private void openEvents() {
        AdminEvenementController.showSection(AdminEvenementController.Section.LIST_EVENTS);
        goTo("/AdminEvenement.fxml");
    }

    @FXML
    private void openEventParticipants() {
        AdminEvenementController.showSection(AdminEvenementController.Section.PARTICIPANT_REQUESTS);
        goTo("/AdminEvenement.fxml");
    }

    @FXML
    private void openRessources() {
        AdminEvenementController.showSection(AdminEvenementController.Section.RESOURCES);
        goTo("/AdminEvenement.fxml");
    }

    @FXML
    private void openStatsEvents() {
        AdminEvenementController.showSection(AdminEvenementController.Section.EVENT_STATS);
        goTo("/AdminEvenement.fxml");
    }

    @FXML
    private void openStatsRessources() {
        AdminEvenementController.showSection(AdminEvenementController.Section.RESOURCE_STATS);
        goTo("/AdminEvenement.fxml");
    }

    @FXML
    private void openConsultations() {
        goTo("/FXML/ConsultationsParDocteur.fxml");
    }

    @FXML
    private void openStatsConsultations() {
        goTo("/FXML/StatsConsultations.fxml");
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
    private void openPostsEnAttente() {
        goTo("/PendingPostsAdmin.fxml");
    }

    @FXML
    private void openStatsBlog() {
        goTo("/BlogStatAdmin.fxml");
    }

    @FXML
    private void goBackSite() {
        goTo("/FrontFXML/Accueil.fxml");
    }

    @FXML
    private void logout() {
        SessionManager.setCurrentUser(null);
        goTo("/FrontFXML/Login.fxml");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean contains(String source, String keyword) {
        return safe(source).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void goTo(String resourcePath) {
        try {
            String resolvedPath = FXML_ALIASES.getOrDefault(resourcePath, resourcePath);
            URL url = getClass().getResource(resolvedPath);
            if (url == null) {
                showAlert(Alert.AlertType.WARNING, "Navigation", "Page introuvable: " + resourcePath);
                return;
            }
            Parent root = FXMLLoader.load(url);
            rootPane.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir: " + resourcePath);
        }
    }

    private String resolveAdminLabel() {
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            return "Admin MedFlow";
        }
        String fullName = (safe(current.getNom()) + " " + safe(current.getPrenom())).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        String email = safe(current.getEmailUser());
        return email.isBlank() ? "Admin MedFlow" : email;
    }

    public static class RiskRow {
        private final String id;
        private final String fullName;
        private final String email;
        private final int riskScore;
        private final String signals;
        private final String loginInfo;
        private final String lockInfo;

        public RiskRow(String id, String fullName, String email, int riskScore, String signals, String loginInfo, String lockInfo) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.riskScore = riskScore;
            this.signals = signals;
            this.loginInfo = loginInfo;
            this.lockInfo = lockInfo;
        }
    }
}


