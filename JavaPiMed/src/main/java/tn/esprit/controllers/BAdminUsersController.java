package tn.esprit.controllers;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import tn.esprit.entities.User;
import tn.esprit.services.AdminActionLogService;
import tn.esprit.services.DecisionPdfService;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserService;
import tn.esprit.tools.SessionManager;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BAdminUsersController {

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
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> idColumn;
    @FXML private TableColumn<User, String> nomColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> verifiedColumn;
    @FXML private TableColumn<User, String> statutColumn;
    @FXML private TableColumn<User, Void> actionColumn;
    @FXML private TextField searchField;
    @FXML private Label totalUsersCountLabel;
    @FXML private Label activeUsersCountLabel;
    @FXML private Label bannedUsersCountLabel;
    @FXML private Label staffUsersCountLabel;

    private final UserService userService = new UserService();
    private final AdminActionLogService actionLogService = new AdminActionLogService();
    private final ObservableList<User> allUsers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().getId())));
        nomColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                (safe(cell.getValue().getPrenom()) + " " + safe(cell.getValue().getNom())).trim()));
        emailColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(safe(cell.getValue().getEmailUser())));
        roleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(safe(cell.getValue().getRoleSysteme())));
        if (verifiedColumn != null) {
            verifiedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().isVerified() ? "VERIFIE" : "NON_VERIFIE"));
        }
        statutColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(normalizeStatus(cell.getValue().getStatutCompte())));

        configureBadgeColumns();
        usersTable.setPlaceholder(new Label("Aucun utilisateur a afficher."));
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        configureActionColumn();
        reloadUsers();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldText, newText) -> applyFilter(newText));
        }
    }

    @FXML
    private void handleRefresh() {
        reloadUsers();
    }

    @FXML
    private void handleLogout() {
        logout();
    }

    @FXML
    private void openDashboard() {
        goTo("/AdminWelcome.fxml");
    }

    @FXML
    private void openUsers() {
        // Deja sur la page utilisateurs.
    }

    @FXML
    private void openRiskJournal() {
        goTo("/BackFXML/BAdminRiskJournal.fxml");
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

    private void reloadUsers() {
        List<User> users = userService.recuperer();
        allUsers.setAll(users);
        applyFilter(searchField == null ? "" : searchField.getText());
        updateSummaryCards(users);
    }

    private void applyFilter(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            List<User> users = List.copyOf(allUsers);
            usersTable.setItems(FXCollections.observableArrayList(users));
            updateSummaryCards(users);
            return;
        }

        List<User> filtered = allUsers.stream()
                .filter(u -> contains(u.getNom(), normalized)
                        || contains(u.getPrenom(), normalized)
                        || contains(u.getEmailUser(), normalized)
                        || contains(u.getRoleSysteme(), normalized)
                        || contains(u.getStatutCompte(), normalized)
                        || String.valueOf(u.getId()).contains(normalized))
                .collect(Collectors.toList());
        usersTable.setItems(FXCollections.observableArrayList(filtered));
        updateSummaryCards(filtered);
    }

    private void configureBadgeColumns() {
        roleColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(formatRole(item));
                badge.getStyleClass().addAll("table-pill", "role-pill", resolveRoleClass(item));
                setGraphic(badge);
            }
        });

        statutColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(formatStatus(item));
                badge.getStyleClass().addAll("table-pill", "status-pill", resolveStatusClass(item));
                setGraphic(badge);
            }
        });

        if (verifiedColumn != null) {
            verifiedColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        return;
                    }
                    boolean verified = "VERIFIE".equals(item);
                    Label badge = new Label(verified ? "Verifie" : "Non verifie");
                    badge.getStyleClass().addAll("table-pill", "verified-pill", verified ? "pill-ok" : "pill-warn");
                    setGraphic(badge);
                }
            });
        }
    }

    private void configureActionColumn() {
        actionColumn.setStyle("-fx-alignment: CENTER;");
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button banBtn = new Button();

            {
                banBtn.getStyleClass().add("ban-btn");
                banBtn.setPrefWidth(96);
                banBtn.setMinWidth(96);
                banBtn.setOnAction(e -> {
                    User target = getTableView().getItems().get(getIndex());
                    toggleBan(target);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                User target = getTableView().getItems().get(getIndex());
                boolean banned = isBanned(target);
                banBtn.getStyleClass().removeAll("ban-btn", "unban-btn", "self-btn");

                User current = SessionManager.getCurrentUser();
                boolean self = current != null && current.getId() == target.getId();
                banBtn.setDisable(self);
                if (self) {
                    banBtn.setText("Vous");
                    banBtn.getStyleClass().add("self-btn");
                } else {
                    banBtn.setText(banned ? "Debannir" : "Bannir");
                    banBtn.getStyleClass().add(banned ? "unban-btn" : "ban-btn");
                }

                setGraphic(banBtn);
            }
        });
    }

    private void toggleBan(User target) {
        if (target == null) {
            return;
        }

        String current = normalizeStatus(target.getStatutCompte());
        boolean banned = current.contains("BAN");
        String nextStatus = banned ? "ACTIF" : "BANNI";
        String reason = null;

        if (!banned) {
            reason = promptMandatoryReason(
                    "Motif de bannissement",
                    "Veuillez saisir la raison du bannissement",
                    "Ex: non-respect des regles, comportement abusif, etc.",
                    ""
            );
            if (reason == null) {
                return;
            }
        }

        boolean ok = userService.updateStatutCompte(target.getId(), nextStatus, reason);
        if (!ok) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre a jour le statut pour l'utilisateur #" + target.getId());
            return;
        }

        target.setStatutCompte(nextStatus);
        String adminName = resolveAdminSignatureName();

        try {
            if (!banned) {
                byte[] pdf = DecisionPdfService.buildAccountBanPdf(target, reason, adminName);
                EmailService.sendAccountBanEmail(target, reason, pdf, buildDecisionPdfName("ban", target));
                actionLogService.logAction(adminName, "BAN", "USER", target.getId(),
                        "Compte banni. Motif: " + safe(reason));
            } else {
                EmailService.sendSimpleUnbanEmail(target.getEmailUser());
                actionLogService.logAction(adminName, "UNBAN", "USER", target.getId(), "Compte debloque.");
            }
        } catch (Exception ignored) {
            showAlert(Alert.AlertType.WARNING, "Email", "Le statut a ete mis a jour, mais l'email n'a pas pu etre envoye.");
        }

        usersTable.refresh();
    }

    private boolean isBanned(User user) {
        return normalizeStatus(user == null ? null : user.getStatutCompte()).contains("BAN");
    }

    private String resolveAdminSignatureName() {
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

    private String buildDecisionPdfName(String prefix, User user) {
        String idPart = user == null ? "unknown" : String.valueOf(user.getId());
        return "decision_" + prefix + "_" + idPart + ".pdf";
    }

    private boolean contains(String source, String needle) {
        return safe(source).toLowerCase(Locale.ROOT).contains(needle);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIF";
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String formatRole(String role) {
        return switch (safe(role).toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> "Admin";
            case "STAFF" -> "Staff";
            case "PATIENT" -> "Patient";
            default -> safe(role);
        };
    }

    private String formatStatus(String status) {
        String normalized = normalizeStatus(status);
        if (normalized.contains("BAN")) return "Banni";
        if (normalized.contains("REFUSE")) return "Refuse";
        if (normalized.contains("ATTENTE")) return "En attente";
        return "Actif";
    }

    private String resolveRoleClass(String role) {
        return switch (safe(role).toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> "pill-admin";
            case "STAFF" -> "pill-staff";
            default -> "pill-patient";
        };
    }

    private String resolveStatusClass(String status) {
        String normalized = normalizeStatus(status);
        if (normalized.contains("BAN") || normalized.contains("REFUSE")) return "pill-danger";
        if (normalized.contains("ATTENTE")) return "pill-warn";
        return "pill-ok";
    }

    private void updateSummaryCards(List<User> source) {
        if (source == null) {
            return;
        }

        long total = source.size();
        long active = source.stream().filter(u -> !isBanned(u)).count();
        long banned = source.stream().filter(this::isBanned).count();
        long staff = source.stream()
                .filter(u -> "STAFF".equals(safe(u.getRoleSysteme()).toUpperCase(Locale.ROOT)))
                .count();

        if (totalUsersCountLabel != null) totalUsersCountLabel.setText(String.valueOf(total));
        if (activeUsersCountLabel != null) activeUsersCountLabel.setText(String.valueOf(active));
        if (bannedUsersCountLabel != null) bannedUsersCountLabel.setText(String.valueOf(banned));
        if (staffUsersCountLabel != null) staffUsersCountLabel.setText(String.valueOf(staff));
    }

    private String promptMandatoryReason(String title, String header, String prompt, String initialValue) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextArea reasonArea = new TextArea(safe(initialValue));
        reasonArea.setPromptText(prompt);
        reasonArea.setWrapText(true);
        reasonArea.setPrefRowCount(5);
        reasonArea.setPrefColumnCount(40);
        dialog.getDialogPane().setContent(reasonArea);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return null;
        }

        String reason = safe(reasonArea.getText()).trim();
        if (reason.isBlank()) {
            showAlert(Alert.AlertType.ERROR, title, "Le motif est obligatoire.");
            return null;
        }
        return reason;
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
}


