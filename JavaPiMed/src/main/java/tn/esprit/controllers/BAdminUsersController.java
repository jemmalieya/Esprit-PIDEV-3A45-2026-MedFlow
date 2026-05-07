package tn.esprit.controllers;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import tn.esprit.entities.User;
import tn.esprit.services.DecisionPdfService;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserService;
import tn.esprit.tools.SessionManager;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BAdminUsersController {

    @FXML private BorderPane rootPane;
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> idColumn;
    @FXML private TableColumn<User, String> nomColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statutColumn;
    @FXML private TableColumn<User, Void> actionColumn;
    @FXML private TextField searchField;

    private final UserService userService = new UserService();
    private final ObservableList<User> allUsers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().getId())));
        nomColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                (safe(cell.getValue().getPrenom()) + " " + safe(cell.getValue().getNom())).trim()));
        emailColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(safe(cell.getValue().getEmailUser())));
        roleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(safe(cell.getValue().getRoleSysteme())));
        statutColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(normalizeStatus(cell.getValue().getStatutCompte())));

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
    private void openProduits() {
        goTo("/ProduitDashboard.fxml");
    }

    @FXML
    private void openCommandes() {
        goTo("/MesCommandesBack.fxml");
    }

    @FXML
    private void openEvents() {
        goTo("/EvenementDashboard.fxml");
    }

    @FXML
    private void openReclamations() {
        goTo("/reponse.fxml");
    }

    @FXML
    private void openPosts() {
        goTo("/PendingPostsAdmin.fxml");
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
    }

    private void applyFilter(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            usersTable.setItems(FXCollections.observableArrayList(allUsers));
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
    }

    private void configureActionColumn() {
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button banBtn = new Button();

            {
                banBtn.getStyleClass().add("ban-btn");
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
                banBtn.setText(banned ? "Debannir" : "Bannir");
                banBtn.getStyleClass().removeAll("ban-btn", "unban-btn");
                banBtn.getStyleClass().add(banned ? "unban-btn" : "ban-btn");

                User current = SessionManager.getCurrentUser();
                boolean self = current != null && current.getId() == target.getId();
                banBtn.setDisable(self);
                if (self) {
                    banBtn.setText("Vous");
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

        boolean ok = userService.updateStatutCompte(target.getId(), nextStatus);
        if (!ok) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de mettre a jour le statut pour l'utilisateur #" + target.getId());
            return;
        }

        target.setStatutCompte(nextStatus);

        try {
            if (!banned) {
                String adminName = resolveAdminSignatureName();
                String reason = "Decision administrative";
                byte[] pdf = DecisionPdfService.buildAccountBanPdf(target, reason, adminName);
                EmailService.sendAccountBanEmail(target, reason, pdf, buildDecisionPdfName("ban", target));
            } else {
                EmailService.sendSimpleUnbanEmail(target.getEmailUser());
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void goTo(String resourcePath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(resourcePath));
            rootPane.getScene().setRoot(root);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir: " + resourcePath);
        }
    }
}


