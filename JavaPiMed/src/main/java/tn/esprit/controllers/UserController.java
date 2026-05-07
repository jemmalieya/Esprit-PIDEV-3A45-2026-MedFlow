package tn.esprit.controllers;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.User;
import tn.esprit.services.AdminActionLogService;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserService;
import tn.esprit.services.StaffRequestAIAnalysisService;
import tn.esprit.services.DecisionPdfService;
import tn.esprit.tools.SessionManager;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Unified controller for AdminDashboard.fxml and AdminStatsDashboard.fxml.
 * initialize() detects which page was loaded via null-checks on key @FXML fields.
 */
public class UserController {

    // ── AdminDashboard fields ──────────────────────────────────────────────────

    @FXML private Label totalUsersLabel;
    @FXML private Label adminUsersLabel;
    @FXML private Label staffUsersLabel;
    @FXML private Label patientUsersLabel;
    @FXML private Label inventoryCountLabel;
    @FXML private Label verifiedUsersLabel;
    @FXML private Label bannedUsersLabel;
    @FXML private Label incompleteUsersLabel;
    @FXML private Label duplicateUsersLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button sortDirectionBtn;
    @FXML private Label reportHintLabel;
    @FXML private Label statsPageIndicatorLabel;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> idCol;
    @FXML private TableColumn<User, String> cinCol;
    @FXML private TableColumn<User, String> nomCol;
    @FXML private TableColumn<User, String> prenomCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> phoneCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> statutCol;
    @FXML private TableColumn<User, String> verifiedCol;
    @FXML private TableColumn<User, Void> actionsCol;
    @FXML private TableView<User> staffRequestTable;
    @FXML private TableColumn<User, String> staffReqIdCol;
    @FXML private TableColumn<User, String> staffReqNameCol;
    @FXML private TableColumn<User, String> staffReqEmailCol;
    @FXML private TableColumn<User, String> staffReqTypeCol;
    @FXML private TableColumn<User, String> staffReqRequestedAtCol;
    @FXML private TableColumn<User, String> staffReqReasonCol;
    @FXML private TableColumn<User, Void> staffReqActionsCol;
    @FXML private Label staffRequestsCountLabel;
    @FXML private Label staffRequestHintLabel;

    @FXML private VBox submenuVBox;
    @FXML private Label userArrow;
    @FXML private Button voirTousUsersBtn;
    @FXML private Button navUsersBtn;
    @FXML private Button navStaffRequestsBtn;
    @FXML private VBox usersPageContainer;
    @FXML private VBox staffPageContainer;
    @FXML private Button prevStatsPageBtn;
    @FXML private Button nextStatsPageBtn;
    @FXML private HBox statsPageOne;
    @FXML private HBox statsPageTwo;

    @FXML private ProgressBar totalUsersBar;
    @FXML private ProgressBar adminUsersBar;
    @FXML private ProgressBar staffUsersBar;
    @FXML private ProgressBar patientUsersBar;
    @FXML private ProgressBar verifiedUsersBar;
    @FXML private ProgressBar bannedUsersBar;
    @FXML private ProgressBar incompleteUsersBar;
    @FXML private ProgressBar duplicateUsersBar;

    // ── AdminStatsDashboard fields ─────────────────────────────────────────────

    @FXML private PieChart roleDonutChart;
    @FXML private PieChart healthDonutChart;
    @FXML private BarChart<String, Number> qualityBarChart;

    @FXML private Label verificationRateLabel;
    @FXML private Label riskScoreLabel;
    @FXML private Label roleDonutCenterLabel;
    @FXML private Label healthDonutCenterLabel;
    @FXML private Label insightTitleLabel;
    @FXML private Label insightBodyLabel;

    // ── Shared state ───────────────────────────────────────────────────────────

    private final UserService userService = new UserService();
    private final AdminActionLogService actionLogService = new AdminActionLogService();
    private final ObservableList<User> masterList = FXCollections.observableArrayList();
    private final ObservableList<User> pendingStaffRequests = FXCollections.observableArrayList();
    private FilteredList<User> filteredList;
    private boolean sortAscending = false;
    private int currentStatsPage = 0;
    private boolean statsFlipRunning = false;

    // ══════════════════════════════════════════════════════════════════════════
    // initialize — detects which FXML was loaded
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        if (userTable != null) {
            // AdminDashboard.fxml
            configureTable();
            configureStaffRequestTable();
            configureSort();
            loadData();
            loadPendingStaffRequests();
            applySort();
            configureStatsBook();
            configureSearch();
            updateStats();
            switchDashboardPage(true);
        } else if (roleDonutChart != null) {
            // AdminStatsDashboard.fxml
            List<User> users = userService.recuperer();
            buildMetrics(users);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  AdminDashboard logic
    // ══════════════════════════════════════════════════════════════════════════

    private void configureStatsBook() {
        if (statsPageOne == null || statsPageTwo == null) {
            return;
        }
        statsPageOne.setVisible(true);
        statsPageOne.setManaged(true);
        statsPageTwo.setVisible(false);
        statsPageTwo.setManaged(false);
        currentStatsPage = 0;
        updateStatsPageUi();
    }

    @FXML
    private void onPrevStatsPage(ActionEvent event) {
        flipStatsPage(0, -1);
    }

    @FXML
    private void onNextStatsPage(ActionEvent event) {
        flipStatsPage(1, 1);
    }

    private void flipStatsPage(int targetPage, int direction) {
        if (statsFlipRunning || targetPage == currentStatsPage || statsPageOne == null || statsPageTwo == null) {
            return;
        }

        HBox currentNode = currentStatsPage == 0 ? statsPageOne : statsPageTwo;
        HBox nextNode = targetPage == 0 ? statsPageOne : statsPageTwo;

        statsFlipRunning = true;
        nextNode.setVisible(true);
        nextNode.setManaged(true);
        nextNode.setRotationAxis(Rotate.Y_AXIS);
        nextNode.setRotate(90 * direction);

        currentNode.setRotationAxis(Rotate.Y_AXIS);
        RotateTransition rotateOut = new RotateTransition(Duration.millis(240), currentNode);
        rotateOut.setInterpolator(Interpolator.EASE_IN);
        rotateOut.setFromAngle(0);
        rotateOut.setToAngle(-90 * direction);

        RotateTransition rotateIn = new RotateTransition(Duration.millis(240), nextNode);
        rotateIn.setInterpolator(Interpolator.EASE_OUT);
        rotateIn.setFromAngle(90 * direction);
        rotateIn.setToAngle(0);

        rotateOut.setOnFinished(e -> {
            currentNode.setVisible(false);
            currentNode.setManaged(false);
            currentNode.setRotate(0);
            rotateIn.play();
        });

        rotateIn.setOnFinished(e -> {
            nextNode.setRotate(0);
            currentStatsPage = targetPage;
            statsFlipRunning = false;
            updateStatsPageUi();
        });

        rotateOut.play();
    }

    private void updateStatsPageUi() {
        if (statsPageIndicatorLabel != null) {
            statsPageIndicatorLabel.setText(currentStatsPage == 0 ? "Page 1/2 • Répartition" : "Page 2/2 • Santé des comptes");
        }
        if (prevStatsPageBtn != null) {
            prevStatsPageBtn.setDisable(currentStatsPage == 0);
        }
        if (nextStatsPageBtn != null) {
            nextStatsPageBtn.setDisable(currentStatsPage == 1);
        }
    }

    private void configureTable() {
        idCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getId())));
        cinCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getCin())));
        nomCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getNom())));
        prenomCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getPrenom())));
        emailCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getEmailUser())));
        phoneCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getTelephoneUser())));
        roleCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getRoleSysteme())));
        statutCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getStatutCompte())));
        verifiedCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isVerified() ? "Oui" : "Non"));
        addActionsColumn();
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        userTable.setPlaceholder(new Label("Aucun utilisateur trouvé"));
    }

    private void configureSort() {
        if (sortCombo == null) {
            return;
        }
        sortCombo.setItems(FXCollections.observableArrayList(
                "Tri: Plus récents (ID)",
                "Tri: Nom",
                "Tri: Prénom",
                "Tri: Email",
                "Tri: Rôle",
                "Tri: Statut",
                "Tri: Vérification"
        ));
        sortCombo.setValue("Tri: Plus récents (ID)");
        sortCombo.setOnAction(e -> applySort());
        if (sortDirectionBtn != null) {
            sortDirectionBtn.setText("↓ Desc");
        }
    }

    @FXML
    private void toggleSortDirection(ActionEvent event) {
        sortAscending = !sortAscending;
        if (sortDirectionBtn != null) {
            sortDirectionBtn.setText(sortAscending ? "↑ Asc" : "↓ Desc");
        }
        applySort();
    }

    private void applySort() {
        if (sortCombo == null || sortCombo.getValue() == null) {
            return;
        }
        Comparator<User> comparator;
        String selected = sortCombo.getValue();
        switch (selected) {
            case "Tri: Nom"          -> comparator = Comparator.comparing(u -> safe(u.getNom()).toLowerCase(Locale.ROOT));
            case "Tri: Prénom"       -> comparator = Comparator.comparing(u -> safe(u.getPrenom()).toLowerCase(Locale.ROOT));
            case "Tri: Email"        -> comparator = Comparator.comparing(u -> safe(u.getEmailUser()).toLowerCase(Locale.ROOT));
            case "Tri: Rôle"         -> comparator = Comparator.comparing(u -> safe(u.getRoleSysteme()).toLowerCase(Locale.ROOT));
            case "Tri: Statut"       -> comparator = Comparator.comparing(u -> safe(u.getStatutCompte()).toLowerCase(Locale.ROOT));
            case "Tri: Vérification" -> comparator = Comparator.comparing(User::isVerified);
            default                  -> comparator = Comparator.comparingInt(User::getId);
        }
        if (!sortAscending) {
            comparator = comparator.reversed();
        }
        FXCollections.sort(masterList, comparator);
    }

    private void addActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button banBtn    = new Button();
            private final Button deleteBtn = new Button("🗑 Supprimer");
            private final ToolBar toolBar  = new ToolBar(banBtn, deleteBtn);

            {
                toolBar.getStyleClass().add("action-toolbar");
                banBtn.getStyleClass().add("ban-btn");
                deleteBtn.getStyleClass().add("delete-btn");

                banBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    boolean banned = isBanned(user);
                    String nextStatus = banned ? "ACTIVE" : "BANNED";
                    String actionText = banned ? "débloquer" : "bannir";

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Voulez-vous " + actionText + " l'utilisateur : "
                            + safe(user.getNom()) + " " + safe(user.getPrenom()) + " ?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        String reason = null;
                        if (!banned) {
                            reason = promptMandatoryReason(
                                    "Motif de bannissement",
                                    "Veuillez saisir la raison du bannissement",
                                    "Ex: non-respect des regles, documents frauduleux, etc.",
                                    ""
                            );
                            if (reason == null) {
                                return;
                            }
                        }

                        boolean updated = userService.updateStatutCompte(user.getId(), nextStatus, reason);
                        if (updated) {
                            user.setStatutCompte(nextStatus);
                            if (!banned) {
                                String adminName = resolveAdminSignatureName();
                                byte[] pdf = DecisionPdfService.buildAccountBanPdf(user, reason, adminName);
                                EmailService.sendAccountBanEmail(user, reason, pdf, buildDecisionPdfName("ban", user));
                            }
                            // Envoi email lors du déban
                            if (banned) {
                                EmailService.sendSimpleUnbanEmail(user.getEmailUser());
                            }
                            applySort();
                            userTable.refresh();
                            updateStats();
                        } else {
                            showError("Action impossible", "La mise à jour du statut a échoué.");
                        }
                    }
                });

                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Supprimer l'utilisateur : "
                            + safe(user.getNom()) + " " + safe(user.getPrenom()) + " ?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        userService.supprimer(user);
                        masterList.remove(user);
                        applySort();
                        updateStats();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                User user = getTableView().getItems().get(getIndex());
                banBtn.setText(isBanned(user) ? "🔓 Debloquer" : "🚫 Bloquer");
                setGraphic(toolBar);
            }
        });

        actionsCol.setCellValueFactory(param -> Bindings.createObjectBinding(() -> null));
    }

    private void configureStaffRequestTable() {
        if (staffRequestTable == null) {
            return;
        }

        if (staffReqIdCol != null) {
            staffReqIdCol.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getId())));
        }
        if (staffReqNameCol != null) {
            staffReqNameCol.setCellValueFactory(cell -> new SimpleStringProperty(
                    (safe(cell.getValue().getNom()) + " " + safe(cell.getValue().getPrenom())).trim()));
        }
        if (staffReqEmailCol != null) {
            staffReqEmailCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getEmailUser())));
        }
        if (staffReqTypeCol != null) {
            staffReqTypeCol.setCellValueFactory(cell -> new SimpleStringProperty(safe(cell.getValue().getTypeStaff())));
        }
        if (staffReqRequestedAtCol != null) {
            staffReqRequestedAtCol.setCellValueFactory(cell -> {
                LocalDateTime requestedAt = cell.getValue().getStaffRequestedAt();
                String value = requestedAt == null
                        ? "-"
                        : requestedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                return new SimpleStringProperty(value);
            });
        }
        if (staffReqReasonCol != null) {
            staffReqReasonCol.setCellValueFactory(cell -> {
                String reason = safe(cell.getValue().getStaffRequestReason());
                if (reason.isBlank()) {
                    reason = safe(cell.getValue().getStaffRequestMessage());
                }
                if (reason.length() > 120) {
                    reason = reason.substring(0, 117) + "...";
                }
                return new SimpleStringProperty(reason);
            });
        }

        // ── Per-row actions column ─────────────────────────────────────────
        if (staffReqActionsCol != null) {
            staffReqActionsCol.setCellFactory(col -> new TableCell<>() {
                private final Button detailsBtn = new Button("👁 Voir Détails");
                private final Button approveBtn = new Button("✅");
                private final Button rejectBtn  = new Button("❌");
                private final HBox   hbox       = new HBox(6, detailsBtn, approveBtn, rejectBtn);

                {
                    detailsBtn.getStyleClass().add("staff-details-btn");
                    approveBtn.getStyleClass().add("staff-approve-btn");
                    rejectBtn.getStyleClass().add("staff-reject-btn");
                    hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    detailsBtn.setOnAction(e -> {
                        User u = getTableView().getItems().get(getIndex());
                        openStaffDetails(u);
                    });
                    approveBtn.setOnAction(e -> {
                        User u = getTableView().getItems().get(getIndex());
                        doReviewStaffRequest(u, true);
                    });
                    rejectBtn.setOnAction(e -> {
                        User u = getTableView().getItems().get(getIndex());
                        doReviewStaffRequest(u, false);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : hbox);
                }
            });
            staffReqActionsCol.setCellValueFactory(p -> javafx.beans.binding.Bindings.createObjectBinding(() -> null));
        }

        staffRequestTable.setItems(pendingStaffRequests);
        staffRequestTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        staffRequestTable.setPlaceholder(new Label("Aucune demande staff en attente"));
        staffRequestTable.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, selectedUser) -> {
            if (staffRequestHintLabel == null) {
                return;
            }
            if (selectedUser == null) {
                staffRequestHintLabel.setText("Sélectionnez une demande pour valider, refuser ou voir les pièces.");
                return;
            }
            int count = extractStaffDocuments(selectedUser).size();
            staffRequestHintLabel.setText(count == 0
                    ? "Aucune pièce détectée pour cette demande."
                    : count + " pièce(s) détectée(s). Cliquez sur \"Voir Détails\".");
        });
    }

    private void loadPendingStaffRequests() {
        if (staffRequestTable == null) {
            return;
        }
        List<User> requests = userService.findPendingStaffRequests();
        pendingStaffRequests.setAll(requests);
        if (staffRequestsCountLabel != null) {
            staffRequestsCountLabel.setText(String.valueOf(requests.size()));
        }
        if (staffRequestHintLabel != null) {
            staffRequestHintLabel.setText(requests.isEmpty()
                    ? "Aucune demande staff en attente."
                    : "Sélectionnez une demande pour valider ou refuser.");
        }
    }

    @FXML
    private void onRefreshStaffRequests(ActionEvent event) {
        loadPendingStaffRequests();
    }

    @FXML
    private void onViewStaffDocuments(ActionEvent event) {
        if (staffRequestTable == null) {
            return;
        }
        User selected = staffRequestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Demandes staff", "Veuillez sélectionner une demande d'abord.");
            return;
        }
        openStaffDetails(selected);
    }

    private List<String> extractStaffDocuments(User user) {
        List<String> paths = new ArrayList<>();
        addDocumentPath(paths, user.getStaffRequestProofPath());
        addSerializedDocumentPaths(paths, user.getStaffDocuments());
        return paths;
    }

    private void addSerializedDocumentPaths(List<String> paths, String rawDocuments) {
        if (rawDocuments == null || rawDocuments.isBlank()) {
            return;
        }
        String normalized = rawDocuments
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace("\r", "\n");
        String[] tokens = normalized.split("\\s*;\\s*|\\s*,\\s*|\\n+");
        for (String token : tokens) {
            addDocumentPath(paths, token);
        }
    }

    private void addDocumentPath(List<String> paths, String value) {
        if (value == null) {
            return;
        }
        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return;
        }
        if (!paths.contains(cleaned)) {
            paths.add(cleaned);
        }
    }

    @FXML
    private void onApproveStaffRequest(ActionEvent event) {
        reviewSelectedStaffRequest(true);
    }

    @FXML
    private void onRejectStaffRequest(ActionEvent event) {
        reviewSelectedStaffRequest(false);
    }

    private void reviewSelectedStaffRequest(boolean approve) {
        if (staffRequestTable == null) return;
        User selected = staffRequestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Demandes staff", "Veuillez sélectionner une demande d'abord.");
            return;
        }
        doReviewStaffRequest(selected, approve);
    }

    /** Action partagée entre les boutons globaux et les boutons par ligne. */
    private void doReviewStaffRequest(User selected, boolean approve) {
        String actionLabel = approve ? "approuver" : "refuser";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Validation demande staff");
        confirm.setHeaderText(null);
        confirm.setContentText("Confirmer l'action : " + actionLabel + " la demande de "
                + safe(selected.getNom()) + " " + safe(selected.getPrenom()) + " ?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String decisionReason = approve ? resolveStaffDecisionReason(selected) : promptMandatoryReason(
                "Motif de refus",
                "Veuillez saisir le motif du refus",
                "Ex: fichiers manquants, diplome non lisible, informations incompletes...",
                resolveStaffDecisionReason(selected)
        );
        if (!approve && decisionReason == null) {
            return;
        }

        Integer reviewerId = null;
        User current = SessionManager.getCurrentUser();
        if (current != null && current.getId() > 0) reviewerId = current.getId();

        boolean ok = userService.reviewStaffRequest(selected.getId(), approve, reviewerId, decisionReason);
        if (!ok) {
            showError("Demandes staff", "Impossible de mettre à jour cette demande.");
            return;
        }

        try {
            if (approve) {
                String adminName = resolveAdminSignatureName();
                byte[] pdf = DecisionPdfService.buildStaffApprovalPdf(selected, adminName, decisionReason);
                EmailService.sendStaffApprovalEmail(selected, pdf, buildDecisionPdfName("approbation_staff", selected));
                actionLogService.logAction(adminName, "STAFF_APPROVE", "USER", selected.getId(),
                        "Demande staff approuvee. Motif: " + safe(decisionReason));
            } else {
                String adminName = resolveAdminSignatureName();
                byte[] pdf = DecisionPdfService.buildStaffRejectionPdf(selected, adminName, decisionReason);
                EmailService.sendStaffRejectionEmail(selected, decisionReason, pdf, buildDecisionPdfName("refus_staff", selected));
                actionLogService.logAction(adminName, "STAFF_REJECT", "USER", selected.getId(),
                        "Demande staff refusee. Motif: " + safe(decisionReason));
            }
        } catch (Exception emailEx) {
            System.err.println("[UserController] Erreur envoi email staff : " + emailEx.getMessage());
        }

        loadData();
        applySort();
        updateStats();
        loadPendingStaffRequests();
    }

    /** Ouvre un écran de révision staff directement depuis UserController (sans contrôleur dédié). */
    private void openStaffDetails(User user) {
        showStaffReviewDialog(user);
    }

    private void showStaffReviewDialog(User selected) {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Révision demande staff");
            dialog.setHeaderText(null);
            dialog.getDialogPane().setPrefSize(1160, 760);
            dialog.getDialogPane().getStyleClass().add("staff-review-dialog");

            // Force loading CSS on DialogPane (scene styles are not always inherited by JavaFX dialogs).
            if (getClass().getResource("/admin-dashboard.css") != null) {
                String css = getClass().getResource("/admin-dashboard.css").toExternalForm();
                if (!dialog.getDialogPane().getStylesheets().contains(css)) {
                    dialog.getDialogPane().getStylesheets().add(css);
                }
            }
            if (getClass().getResource("/evenement-dashboard.css") != null) {
                String css = getClass().getResource("/evenement-dashboard.css").toExternalForm();
                if (!dialog.getDialogPane().getStylesheets().contains(css)) {
                    dialog.getDialogPane().getStylesheets().add(css);
                }
            }

            String fullName = (safe(selected.getNom()) + " " + safe(selected.getPrenom())).trim();
            if (fullName.isBlank()) {
                fullName = "Candidat #" + selected.getId();
            }

            Label avatar = new Label(buildUserInitials(selected));
            avatar.getStyleClass().add("staff-review-avatar");
            avatar.setMinSize(36, 36);
            avatar.setPrefSize(36, 36);
            avatar.setAlignment(Pos.CENTER);
            Label name = new Label(fullName);
            name.getStyleClass().add("staff-review-name");
            Label meta = new Label("Type: " + safe(selected.getTypeStaff()) + "  |  Email: " + safe(selected.getEmailUser()));
            meta.getStyleClass().add("staff-review-meta");
            VBox textHead = new VBox(2, name, meta);
            HBox header = new HBox(10, avatar, textHead);
            header.setAlignment(Pos.CENTER_LEFT);
            header.getStyleClass().add("staff-review-header");
            dialog.getDialogPane().setHeader(header);

            ButtonType approveType = new ButtonType("Approuver", ButtonBar.ButtonData.YES);
            ButtonType rejectType = new ButtonType("Refuser", ButtonBar.ButtonData.NO);
            dialog.getDialogPane().getButtonTypes().addAll(approveType, rejectType, ButtonType.CLOSE);

            TabPane tabPane = new TabPane();
            tabPane.getStyleClass().add("staff-review-tabs");
            Tab profileTab = new Tab("Profil & Pièces", buildStaffDocumentsPane(selected));
            profileTab.setClosable(false);
            Tab analysisTab = new Tab("Fiche de décision", buildStaffAnalysisPane(selected));
            analysisTab.setClosable(false);
            tabPane.getTabs().addAll(profileTab, analysisTab);
            dialog.getDialogPane().setContent(tabPane);

            Optional<ButtonType> decision = dialog.showAndWait();
            if (decision.isPresent() && decision.get() == approveType) {
                doReviewStaffRequest(selected, true);
            } else if (decision.isPresent() && decision.get() == rejectType) {
                doReviewStaffRequest(selected, false);
            }
        } catch (Exception e) {
            showError("Navigation", "Impossible d'ouvrir les détails de cette demande.");
            e.printStackTrace();
        }
    }

    private VBox buildStaffDocumentsPane(User selected) {
        VBox wrapper = new VBox(12);
        wrapper.getStyleClass().add("staff-review-pane");

        Label title = new Label("Pièces et informations saisies");
        title.getStyleClass().add("staff-review-section-title");

        VBox info = new VBox(6,
                buildInfoRow("ID", String.valueOf(selected.getId())),
                buildInfoRow("Type staff", safe(selected.getTypeStaff())),
                buildInfoRow("Email", safe(selected.getEmailUser())),
                buildInfoRow("Téléphone", safe(selected.getTelephoneUser())),
                buildInfoRow("CIN", safe(selected.getCin())),
                buildInfoRow("Motif", safe(selected.getStaffRequestReason()))
        );
        info.getStyleClass().add("staff-review-card");

        VBox docsBox = new VBox(6);
        docsBox.getStyleClass().add("staff-review-card");
        List<String> paths = extractStaffDocuments(selected);
        Label docsTitle = new Label("Documents détectés: " + paths.size());
        docsTitle.getStyleClass().add("staff-review-section-title");
        docsBox.getChildren().add(docsTitle);

        if (paths.isEmpty()) {
            docsBox.getChildren().add(new Label("Aucun document détecté."));
        } else {
            int index = 1;
            for (String path : paths) {
                File file = new File(path);
                Label name = new Label(index + ". " + (file.getName().isEmpty() ? path : file.getName()));
                name.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(name, javafx.scene.layout.Priority.ALWAYS);

                Label status = new Label(file.exists() ? "Disponible" : "Absent");
                status.getStyleClass().add(file.exists() ? "staff-doc-ok" : "staff-doc-missing");
                Button openBtn = new Button("Ouvrir");
                openBtn.getStyleClass().add("slim-dir-btn");
                openBtn.setDisable(!file.exists());
                openBtn.setOnAction(e -> openFilePath(path));

                HBox row = new HBox(8, name, status, openBtn);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("staff-doc-row");
                docsBox.getChildren().add(row);
                index++;
            }
        }

        ScrollPane docsScroll = new ScrollPane(docsBox);
        docsScroll.getStyleClass().add("staff-review-scroll");
        docsScroll.setFitToWidth(true);
        docsScroll.setPrefHeight(480);

        wrapper.getChildren().addAll(title, info, new Separator(), docsScroll);
        return wrapper;
    }

    private VBox buildStaffAnalysisPane(User selected) {
        VBox wrapper = new VBox(12);
        wrapper.getStyleClass().add("staff-review-pane");

        Label title = new Label("Fiche de décision RH");
        title.getStyleClass().add("staff-review-section-title");
        Label status = new Label("Traitement du dossier...");
        status.getStyleClass().add("staff-review-status");
        Label score = new Label("Score: --/100");
        score.getStyleClass().add("staff-review-chip");
        Label verdict = new Label("Verdict: --");
        verdict.getStyleClass().addAll("staff-review-chip", "staff-review-verdict");
        HBox summary = new HBox(16, status, score, verdict);
        summary.getStyleClass().add("staff-review-summary");
        summary.setAlignment(Pos.CENTER_LEFT);

        TextArea recommendationArea = new TextArea("Recommandation administrative...");
        recommendationArea.setEditable(false);
        recommendationArea.setWrapText(true);
        recommendationArea.setPrefRowCount(2);
        recommendationArea.getStyleClass().add("staff-review-reco");

        TextArea analysisArea = new TextArea("Préparation de l'analyse...");
        analysisArea.setWrapText(true);
        analysisArea.setEditable(false);
        analysisArea.setPrefRowCount(24);
        analysisArea.getStyleClass().add("staff-review-analysis-area");

        runDeepStaffAnalysis(selected, status, score, verdict, recommendationArea, analysisArea);
        wrapper.getChildren().addAll(title, summary, recommendationArea, analysisArea);
        return wrapper;
    }

    private void runDeepStaffAnalysis(User selected,
                                      Label status,
                                      Label score,
                                      Label verdict,
                                      TextArea recommendationArea,
                                      TextArea analysisArea) {
        Task<StaffRequestAIAnalysisService.DeepStaffAnalysisResult> task = new Task<>() {
            @Override
            protected StaffRequestAIAnalysisService.DeepStaffAnalysisResult call() {
                updateMessage("Vérification des pièces et consolidation du dossier...");
                StaffRequestAIAnalysisService service = new StaffRequestAIAnalysisService();
                return service.performDeepAnalysis(selected);
            }
        };

        task.messageProperty().addListener((obs, oldValue, message) ->
                Platform.runLater(() -> status.setText(message)));

        task.setOnSucceeded(evt -> {
            StaffRequestAIAnalysisService.DeepStaffAnalysisResult result = task.getValue();
            Platform.runLater(() -> {
                status.setText("Fiche prête pour décision");
                score.setText("Score: " + result.confidenceScore + "/100");
                verdict.setText("Verdict: " + verdictLabelFr(result.verdict));
                verdict.getStyleClass().removeAll("verdict-approve", "verdict-pending", "verdict-reject");
                verdict.getStyleClass().add(verdictCssClass(result.verdict));
                recommendationArea.setText(safe(result.recommendation));
                analysisArea.setText(buildDeepAnalysisText(result));
            });
        });

        task.setOnFailed(evt -> Platform.runLater(() -> {
            Throwable ex = task.getException();
            status.setText("Synthèse indisponible");
            recommendationArea.setText("-");
            analysisArea.setText("Erreur pendant la préparation de la fiche: " + (ex == null ? "inconnue" : ex.getMessage()));
        }));

        Thread worker = new Thread(task, "staff-review-deep-analysis");
        worker.setDaemon(true);
        worker.start();
    }

    private String buildDeepAnalysisText(StaffRequestAIAnalysisService.DeepStaffAnalysisResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Résumé du dossier\n");
        for (String line : summarizeDossierLines(result.cvTextPreview, 5)) {
            sb.append("- ").append(sanitizeReportText(line)).append("\n");
        }

        sb.append("\nÉléments vérifiés\n");
        for (String point : normalizeVerifiedPoints(result.cvKeyPoints)) {
            sb.append("- ").append(point).append("\n");
        }

        sb.append("\nContrôles de cohérence\n");
        for (StaffRequestAIAnalysisService.ComparisonRow row : result.comparisonTable) {
            sb.append("- ").append(row.field).append(": ")
                    .append("Saisie=").append(sanitizeReportText(row.userInput))
                    .append(" | CV=").append(sanitizeReportText(row.cvExtracted))
                    .append(" | Statut=").append(mapStatusFr(row.status))
                    .append("\n");
        }

        sb.append("\nPoints favorables\n");
        for (String value : result.strengths) {
            sb.append("- ").append(sanitizeReportText(value)).append("\n");
        }

        sb.append("\nPoints à clarifier\n");
        if (result.concerns == null || result.concerns.isEmpty()) {
            sb.append("- Aucun point bloquant identifié à ce stade.\n");
        } else {
            for (String value : result.concerns) {
                sb.append("- ").append(sanitizeReportText(value)).append("\n");
            }
        }

        if (!result.adminQuestions.isEmpty()) {
            sb.append("\nPoints de vérification complémentaires\n");
            for (String value : result.adminQuestions) {
                sb.append("- ").append(sanitizeReportText(value)).append("\n");
            }
        }
        return sb.toString();
    }

    private List<String> summarizeDossierLines(String text, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("Aucune information textuelle exploitable n'a été consolidée.");
            return lines;
        }

        String normalized = text.replace("\r", "\n");
        String first = null;
        String second = null;
        for (String raw : normalized.split("\n+")) {
            String line = raw.replaceAll("\\s+", " ").trim();
            if (line.isBlank() || line.startsWith("[")) {
                continue;
            }

            line = normalizeSpacedCapitalLetters(line);
            if (line.length() < 6) {
                continue;
            }

            if (first == null) {
                first = line;
                continue;
            }
            if (second == null) {
                second = line;
                continue;
            }

            String shortened = line.length() > 120 ? line.substring(0, 117) + "..." : line;
            if (!lines.contains(shortened)) {
                lines.add(shortened);
            }
            if (lines.size() >= maxLines) {
                break;
            }
        }

        if (first != null) {
            String identity = first;
            if (second != null && second.length() <= 24 && !second.matches(".*\\d.*")) {
                identity = first + " " + second;
            }
            lines.add(0, sanitizeReportText(identity));
        }

        if (lines.isEmpty()) {
            lines.add("Les pièces ont été reçues, mais nécessitent une lecture manuelle pour synthèse.");
        }
        while (lines.size() > maxLines) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    private List<String> normalizeVerifiedPoints(List<String> points) {
        List<String> cleaned = new ArrayList<>();
        if (points == null) {
            return cleaned;
        }
        for (String point : points) {
            String value = sanitizeReportText(point)
                    .replaceAll("\\s+en$", "")
                    .replaceAll("\\s+de$", "")
                    .replaceAll("\\s+du$", "")
                    .trim();
            if (value.length() < 8) {
                continue;
            }
            if (!cleaned.contains(value)) {
                cleaned.add(value);
            }
            if (cleaned.size() >= 8) {
                break;
            }
        }
        return cleaned;
    }

    private String normalizeSpacedCapitalLetters(String text) {
        String out = text;
        for (int i = 0; i < 3; i++) {
            out = out.replaceAll("(?<=\\b\\p{Lu})\\s+(?=\\p{Lu}\\b)", "");
        }
        return out;
    }

    private String cleanDecisionLine(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text
                .replace("✔", "")
                .replace("•", "")
                .replace("→", "")
                .replace("⚠", "")
                .replace("ℹ", "")
                .replace("🎓", "")
                .replace("🏥", "")
                .replace("⏱", "")
                .replace("🏛", "")
                .replace("📧", "")
                .replace("🏅", "")
                .replace("️", "")
                .trim();
        return cleaned.replaceAll("\\s+", " ");
    }

    private String sanitizeReportText(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = cleanDecisionLine(text)
                .replace("✓", "")
                .replace("✔", "")
                .replace("✗", "")
                .replace("✘", "")
                .replace("—", "-")
                .trim();
        return cleaned.replaceAll("\\s+", " ");
    }

    private String mapStatusFr(String status) {
        return switch (safe(status)) {
            case "MATCH" -> "Conforme";
            case "PARTIAL" -> "À vérifier";
            case "MISMATCH" -> "Écart";
            default -> "Non confirmé";
        };
    }

    private HBox buildInfoRow(String label, String value) {
        Label keyLabel = new Label(label + ":");
        keyLabel.getStyleClass().add("staff-review-key");
        Label valueLabel = new Label(value == null || value.isBlank() ? "-" : value);
        valueLabel.setWrapText(true);
        valueLabel.getStyleClass().add("staff-review-value");
        HBox row = new HBox(8, keyLabel, valueLabel);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private String buildUserInitials(User user) {
        String nom = safe(user.getNom());
        String prenom = safe(user.getPrenom());
        String initials = "";
        if (!nom.isBlank()) initials += Character.toUpperCase(nom.charAt(0));
        if (!prenom.isBlank()) initials += Character.toUpperCase(prenom.charAt(0));
        return initials.isBlank() ? "?" : initials;
    }

    private String verdictLabelFr(String verdict) {
        return switch (safe(verdict)) {
            case "APPROVE" -> "APPROUVER";
            case "PENDING_REVIEW" -> "VÉRIFICATION";
            case "REJECT" -> "REFUSER";
            default -> safe(verdict);
        };
    }

    private String verdictCssClass(String verdict) {
        return switch (safe(verdict)) {
            case "APPROVE" -> "verdict-approve";
            case "PENDING_REVIEW" -> "verdict-pending";
            default -> "verdict-reject";
        };
    }

    private void openFilePath(String path) {
        if (!Desktop.isDesktopSupported()) {
            showError("Pièces staff", "Ouverture de fichiers non supportée sur cet environnement.");
            return;
        }
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception ex) {
            showError("Pièces staff", "Impossible d'ouvrir le fichier:\n" + path);
        }
    }

    private void loadData() {
        List<User> users = userService.recuperer();
        masterList.setAll(users);
        filteredList = new FilteredList<>(masterList, user -> true);
        userTable.setItems(filteredList);
        inventoryCountLabel.setText(masterList.size() + " utilisateur(s) dans votre espace");
    }

    private void configureSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String keyword = newVal == null ? "" : newVal.trim().toLowerCase(Locale.ROOT);

            filteredList.setPredicate(user -> {
                if (keyword.isEmpty()) {
                    return true;
                }
                String[] tokens = keyword.split("\\s+");
                for (String token : tokens) {
                    if (token.isBlank()) continue;
                    if (token.startsWith("role:")) {
                        if (!contains(user.getRoleSysteme(), token.substring(5))) return false;
                    } else if (token.startsWith("verified:")) {
                        String v = token.substring(9);
                        boolean expected = v.equals("yes") || v.equals("oui") || v.equals("true") || v.equals("1");
                        if (user.isVerified() != expected) return false;
                    } else if (token.startsWith("status:")) {
                        if (!contains(user.getStatutCompte(), token.substring(7))) return false;
                    } else if (token.startsWith("name:")) {
                        String v = token.substring(5);
                        if (!contains(user.getNom(), v) && !contains(user.getPrenom(), v)) return false;
                    } else if (token.startsWith("email:")) {
                        if (!contains(user.getEmailUser(), token.substring(6))) return false;
                    } else {
                        boolean globalMatch = contains(user.getCin(), token)
                                || contains(user.getNom(), token)
                                || contains(user.getPrenom(), token)
                                || contains(user.getEmailUser(), token)
                                || contains(user.getTelephoneUser(), token)
                                || contains(user.getRoleSysteme(), token)
                                || contains(user.getStatutCompte(), token);
                        if (!globalMatch) return false;
                    }
                }
                return true;
            });

            updateStats();
        });

        if (reportHintLabel != null) {
            reportHintLabel.setText("Astuce recherche: role:admin  verified:yes/no  status:active  name:ahmed");
        }
    }

    private void updateStats() {
        int total = filteredList.size();
        int adminCount = 0, staffCount = 0, patientCount = 0;
        int verifiedCount = 0, bannedCount = 0, incompleteCount = 0;

        Map<String, Integer> emailFreq = new HashMap<>();
        Map<String, Integer> cinFreq   = new HashMap<>();
        Map<String, Integer> phoneFreq = new HashMap<>();

        for (User user : filteredList) {
            String role = safe(user.getRoleSysteme()).toUpperCase(Locale.ROOT);
            if ("ADMIN".equals(role))       adminCount++;
            else if ("STAFF".equals(role))  staffCount++;
            else                            patientCount++;

            if (user.isVerified()) verifiedCount++;
            if (isBanned(user))   bannedCount++;
            if (safe(user.getEmailUser()).isBlank()
                    || safe(user.getTelephoneUser()).isBlank()
                    || safe(user.getAdresseUser()).isBlank()) incompleteCount++;

            addFrequency(emailFreq, safe(user.getEmailUser()));
            addFrequency(cinFreq,   safe(user.getCin()));
            addFrequency(phoneFreq, safe(user.getTelephoneUser()));
        }

        int duplicateUsers = 0;
        for (User user : filteredList) {
            if (isDuplicateValue(user.getEmailUser(), emailFreq)
                    || isDuplicateValue(user.getCin(), cinFreq)
                    || isDuplicateValue(user.getTelephoneUser(), phoneFreq)) {
                duplicateUsers++;
            }
        }

        if (totalUsersLabel     != null) totalUsersLabel.setText(String.valueOf(total));
        if (adminUsersLabel     != null) adminUsersLabel.setText(String.valueOf(adminCount));
        if (staffUsersLabel     != null) staffUsersLabel.setText(String.valueOf(staffCount));
        if (patientUsersLabel   != null) patientUsersLabel.setText(String.valueOf(patientCount));
        if (verifiedUsersLabel  != null) verifiedUsersLabel.setText(String.valueOf(verifiedCount));
        if (bannedUsersLabel    != null) bannedUsersLabel.setText(String.valueOf(bannedCount));
        if (incompleteUsersLabel!= null) incompleteUsersLabel.setText(String.valueOf(incompleteCount));
        if (duplicateUsersLabel != null) duplicateUsersLabel.setText(String.valueOf(duplicateUsers));

        setProgress(totalUsersBar,     total > 0 ? 1.0 : 0.0);
        setProgress(adminUsersBar,     ratio(adminCount,     total));
        setProgress(staffUsersBar,     ratio(staffCount,     total));
        setProgress(patientUsersBar,   ratio(patientCount,   total));
        setProgress(verifiedUsersBar,  ratio(verifiedCount,  total));
        setProgress(bannedUsersBar,    ratio(bannedCount,    total));
        setProgress(incompleteUsersBar,ratio(incompleteCount,total));
        setProgress(duplicateUsersBar, ratio(duplicateUsers, total));

        if (inventoryCountLabel != null) {
            inventoryCountLabel.setText(total + " utilisateur(s) dans votre espace");
        }
    }

    private void setProgress(ProgressBar bar, double value) {
        if (bar != null) {
            bar.setProgress(Math.max(0, Math.min(1, value)));
        }
    }

    @FXML
    private void toggleSubmenu(ActionEvent event) {
        if (submenuVBox == null || userArrow == null) {
            return;
        }
        boolean show = !submenuVBox.isVisible();
        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);
        userArrow.setText(show ? "⌃" : "⌄");
    }

    @FXML
    private void onShowUsersPage(ActionEvent event) {
        switchDashboardPage(true);
    }

    @FXML
    private void onShowStaffRequestsPage(ActionEvent event) {
        loadPendingStaffRequests();
        switchDashboardPage(false);
    }

    private void switchDashboardPage(boolean showUsersPage) {
        if (usersPageContainer != null) {
            usersPageContainer.setVisible(showUsersPage);
            usersPageContainer.setManaged(showUsersPage);
        }
        if (staffPageContainer != null) {
            staffPageContainer.setVisible(!showUsersPage);
            staffPageContainer.setManaged(!showUsersPage);
        }
        setNavButtonState(navUsersBtn, showUsersPage);
        setNavButtonState(navStaffRequestsBtn, !showUsersPage);
    }

    private void setNavButtonState(Button button, boolean active) {
        if (button == null) {
            return;
        }
        if (active) {
            if (!button.getStyleClass().contains("sidebar-nav-btn-active")) {
                button.getStyleClass().add("sidebar-nav-btn-active");
            }
        } else {
            button.getStyleClass().remove("sidebar-nav-btn-active");
        }
    }

    @FXML
    private void onVoirTousUsers(ActionEvent event) {
        if (searchField != null) searchField.clear();
        sortAscending = false;
        if (sortDirectionBtn != null) sortDirectionBtn.setText("↓ Desc");
        if (sortCombo != null) sortCombo.setValue("Tri: Plus récents (ID)");
        applySort();
        if (voirTousUsersBtn != null && !voirTousUsersBtn.getStyleClass().contains("submenu-link-selected")) {
            voirTousUsersBtn.getStyleClass().add("submenu-link-selected");
        }
        filteredList.setPredicate(user -> true);
        updateStats();
    }

    @FXML
    private void onLogout(ActionEvent event) {
        SessionManager.setCurrentUser(null);
        navigate(event, "/FrontFXML/Login.fxml", "MedFlow - Connexion");
    }

    @FXML
    private void onGoToFront(ActionEvent event) {
        navigate(event, "/FrontFXML/Accueil.fxml", "MedFlow - Espace Patient");
    }

    @FXML
    private void onOpenStatsPage(ActionEvent event) {
        navigate(event, "/AdminStatsDashboard.fxml", "MedFlow - Smart User Stats");
    }

    @FXML
    private void onExportSmartReport(ActionEvent event) {
        List<User> source = new ArrayList<>(filteredList);
        if (source.isEmpty()) {
            showError("Export PDF", "Aucune donnée à exporter dans le contexte courant.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le rapport audit utilisateurs");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("rapport_audit_users_" + LocalDate.now() + ".pdf");

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            exportAuditReportPdf(source, file);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export PDF");
            ok.setHeaderText("Rapport généré avec succès");
            ok.setContentText("Fichier: " + file.getAbsolutePath());
            ok.showAndWait();
        } catch (Exception e) {
            showError("Export PDF", "Impossible de générer le rapport PDF.");
            e.printStackTrace();
        }
    }

    private void exportAuditReportPdf(List<User> users, File file) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, fos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font h2Font    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font normal    = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font danger    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            danger.setColor(186, 29, 29);

            Paragraph title = new Paragraph("MedFlow - Rapport Audit Utilisateurs", titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            document.add(title);
            document.add(new Paragraph("Généré le " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normal));
            document.add(new Paragraph(" "));

            int total = users.size();
            int verified = 0, banned = 0, missingPhone = 0, missingAddress = 0;
            int adminCount = 0, staffCount = 0, patientCount = 0;

            Map<String, Integer> emailFreq = new HashMap<>();
            Map<String, Integer> cinFreq   = new HashMap<>();
            Map<String, Integer> phoneFreq = new HashMap<>();

            for (User user : users) {
                if (user.isVerified())                       verified++;
                if (isBanned(user))                          banned++;
                if (safe(user.getTelephoneUser()).isBlank()) missingPhone++;
                if (safe(user.getAdresseUser()).isBlank())   missingAddress++;

                String role = safe(user.getRoleSysteme()).toUpperCase(Locale.ROOT);
                if ("ADMIN".equals(role))      adminCount++;
                else if ("STAFF".equals(role)) staffCount++;
                else                           patientCount++;

                addFrequency(emailFreq, safe(user.getEmailUser()));
                addFrequency(cinFreq,   safe(user.getCin()));
                addFrequency(phoneFreq, safe(user.getTelephoneUser()));
            }

            int dupEmail = countUsersInDuplicateKeys(users, emailFreq, User::getEmailUser);
            int dupCin   = countUsersInDuplicateKeys(users, cinFreq,   User::getCin);
            int dupPhone = countUsersInDuplicateKeys(users, phoneFreq, User::getTelephoneUser);

            double verificationRate = total == 0 ? 0 : (verified * 100.0 / total);
            int riskPoints = (total - verified) * 2 + missingPhone + missingAddress + dupEmail + dupCin + dupPhone;
            int riskScore  = total == 0 ? 0 : Math.min(100, (int) Math.round((riskPoints * 100.0) / (total * 5.0)));

            document.add(new Paragraph("1) Synthèse Audit", h2Font));
            PdfPTable summary = new PdfPTable(4);
            summary.setWidthPercentage(100);
            summary.setSpacingBefore(8);
            summary.setSpacingAfter(8);
            summary.addCell(metricCell("Total utilisateurs", String.valueOf(total), normal));
            summary.addCell(metricCell("Taux vérification", String.format(Locale.US, "%.1f%%", verificationRate), normal));
            summary.addCell(metricCell("Comptes bannis", String.valueOf(banned), normal));
            summary.addCell(metricCell("Indice risque", riskScore + "/100", danger));
            summary.addCell(metricCell("Admins", String.valueOf(adminCount), normal));
            summary.addCell(metricCell("Staff", String.valueOf(staffCount), normal));
            summary.addCell(metricCell("Patients", String.valueOf(patientCount), normal));
            summary.addCell(metricCell("Sans téléphone", String.valueOf(missingPhone), normal));
            document.add(summary);

            document.add(new Paragraph("2) Anomalies", h2Font));
            document.add(new Paragraph("- Utilisateurs avec email dupliqué: " + dupEmail, normal));
            document.add(new Paragraph("- Utilisateurs avec CIN dupliqué: " + dupCin, normal));
            document.add(new Paragraph("- Utilisateurs avec téléphone dupliqué: " + dupPhone, normal));
            document.add(new Paragraph("- Utilisateurs sans adresse: " + missingAddress, normal));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("3) Plan d'action recommandé", h2Font));
            document.add(new Paragraph(buildActionLine(total - verified, "Vérifier les comptes non vérifiés", "Equipe Support"), normal));
            document.add(new Paragraph(buildActionLine(dupEmail + dupCin + dupPhone, "Enquête anti-doublon", "Equipe Data"), normal));
            document.add(new Paragraph(buildActionLine(missingPhone + missingAddress, "Compléter les profils incomplets", "Equipe Operations"), normal));
            document.add(new Paragraph(buildActionLine(banned, "Revue des comptes bannis", "Equipe Sécurité"), normal));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("4) Top comptes à traiter en priorité", h2Font));
            PdfPTable actionsTable = new PdfPTable(new float[]{1.2f, 2.2f, 3.0f, 1.6f, 2.8f, 2.8f});
            actionsTable.setWidthPercentage(100);
            actionsTable.setSpacingBefore(8);
            addHeader(actionsTable, "ID", normal);
            addHeader(actionsTable, "Nom", normal);
            addHeader(actionsTable, "Email", normal);
            addHeader(actionsTable, "Rôle", normal);
            addHeader(actionsTable, "Problème", normal);
            addHeader(actionsTable, "Action", normal);

            List<User> priorityUsers = computePriorityUsers(users, emailFreq, cinFreq, phoneFreq);
            int max = Math.min(priorityUsers.size(), 20);
            for (int i = 0; i < max; i++) {
                User u = priorityUsers.get(i);
                actionsTable.addCell(dataCell(String.valueOf(u.getId()), normal));
                actionsTable.addCell(dataCell((safe(u.getNom()) + " " + safe(u.getPrenom())).trim(), normal));
                actionsTable.addCell(dataCell(safe(u.getEmailUser()), normal));
                actionsTable.addCell(dataCell(safe(u.getRoleSysteme()), normal));
                actionsTable.addCell(dataCell(computeIssueLabel(u, emailFreq, cinFreq, phoneFreq), normal));
                actionsTable.addCell(dataCell(computeRecommendedAction(u, emailFreq, cinFreq, phoneFreq), normal));
            }
            document.add(actionsTable);
            document.close();
        }
    }

    private PdfPCell metricCell(String label, String value, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8f);
        cell.addElement(new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        cell.addElement(new Paragraph(value, valueFont));
        return cell;
    }

    private void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell header = new PdfPCell(new Phrase(text, font));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setPadding(7f);
        table.addCell(header);
    }

    private PdfPCell dataCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(6f);
        return cell;
    }

    private String buildActionLine(int count, String action, String owner) {
        return "- [" + count + "] " + action + " | Responsable: " + owner;
    }

    private List<User> computePriorityUsers(List<User> users,
                                            Map<String, Integer> emailFreq,
                                            Map<String, Integer> cinFreq,
                                            Map<String, Integer> phoneFreq) {
        List<User> priority = new ArrayList<>(users);
        priority.sort((a, b) -> Integer.compare(
                computeRiskWeight(b, emailFreq, cinFreq, phoneFreq),
                computeRiskWeight(a, emailFreq, cinFreq, phoneFreq)));
        return priority;
    }

    private int computeRiskWeight(User user,
                                  Map<String, Integer> emailFreq,
                                  Map<String, Integer> cinFreq,
                                  Map<String, Integer> phoneFreq) {
        int score = 0;
        if (!user.isVerified())                              score += 4;
        if (safe(user.getTelephoneUser()).isBlank())         score += 2;
        if (safe(user.getAdresseUser()).isBlank())           score += 1;
        if (isDuplicateValue(user.getEmailUser(), emailFreq))score += 3;
        if (isDuplicateValue(user.getCin(), cinFreq))        score += 3;
        if (isDuplicateValue(user.getTelephoneUser(), phoneFreq)) score += 2;
        return score;
    }

    private String computeIssueLabel(User user,
                                     Map<String, Integer> emailFreq,
                                     Map<String, Integer> cinFreq,
                                     Map<String, Integer> phoneFreq) {
        List<String> issues = new ArrayList<>();
        if (!user.isVerified())                                   issues.add("non vérifié");
        if (isDuplicateValue(user.getEmailUser(), emailFreq))     issues.add("email dupliqué");
        if (isDuplicateValue(user.getCin(), cinFreq))             issues.add("CIN dupliqué");
        if (isDuplicateValue(user.getTelephoneUser(), phoneFreq)) issues.add("tél dupliqué");
        if (safe(user.getTelephoneUser()).isBlank())               issues.add("tél manquant");
        if (safe(user.getAdresseUser()).isBlank())                 issues.add("adresse manquante");
        if (issues.isEmpty())                                      issues.add("RAS");
        return String.join(", ", issues);
    }

    private String computeRecommendedAction(User user,
                                            Map<String, Integer> emailFreq,
                                            Map<String, Integer> cinFreq,
                                            Map<String, Integer> phoneFreq) {
        if (!user.isVerified())                                    return "Relancer vérification";
        if (isDuplicateValue(user.getCin(), cinFreq))              return "Vérifier unicité CIN";
        if (isDuplicateValue(user.getEmailUser(), emailFreq))      return "Vérifier doublon email";
        if (isDuplicateValue(user.getTelephoneUser(), phoneFreq))  return "Vérifier doublon tél";
        if (safe(user.getTelephoneUser()).isBlank()
                || safe(user.getAdresseUser()).isBlank())           return "Compléter profil";
        return "Surveillance standard";
    }

    // ══════════════════════════════════════���═══════════════════════════════════
    //  AdminStatsDashboard logic
    // ══════════════════════════════════════════════════════════════════════════

    private void buildMetrics(List<User> users) {
        int total = users.size();
        int admins = 0, staff = 0, patients = 0;
        int verified = 0, banned = 0;
        int missingEmail = 0, missingPhone = 0, missingAddress = 0, missingCin = 0;

        Map<String, Integer> emailFreq = new HashMap<>();
        Map<String, Integer> cinFreq   = new HashMap<>();
        Map<String, Integer> phoneFreq = new HashMap<>();

        for (User user : users) {
            String role = safe(user.getRoleSysteme()).toUpperCase(Locale.ROOT);
            if ("ADMIN".equals(role))      admins++;
            else if ("STAFF".equals(role)) staff++;
            else                           patients++;

            if (user.isVerified())                       verified++;
            if (isBanned(user))                          banned++;
            if (safe(user.getEmailUser()).isBlank())      missingEmail++;
            if (safe(user.getTelephoneUser()).isBlank())  missingPhone++;
            if (safe(user.getAdresseUser()).isBlank())    missingAddress++;
            if (safe(user.getCin()).isBlank())            missingCin++;

            addFrequency(emailFreq, user.getEmailUser());
            addFrequency(cinFreq,   user.getCin());
            addFrequency(phoneFreq, user.getTelephoneUser());
        }

        int duplicateUsers = 0;
        Set<Integer> issueUsers = new HashSet<>();
        for (User user : users) {
            boolean duplicate = isDuplicateValue(user.getEmailUser(), emailFreq)
                    || isDuplicateValue(user.getCin(), cinFreq)
                    || isDuplicateValue(user.getTelephoneUser(), phoneFreq);
            if (duplicate) {
                duplicateUsers++;
                issueUsers.add(user.getId());
            }
            if (safe(user.getEmailUser()).isBlank() || safe(user.getTelephoneUser()).isBlank()
                    || safe(user.getAdresseUser()).isBlank() || safe(user.getCin()).isBlank()
                    || isBanned(user) || !user.isVerified()) {
                issueUsers.add(user.getId());
            }
        }

        int incompleteUsers = 0;
        for (User user : users) {
            if (safe(user.getEmailUser()).isBlank() || safe(user.getTelephoneUser()).isBlank()
                    || safe(user.getAdresseUser()).isBlank() || safe(user.getCin()).isBlank()) {
                incompleteUsers++;
            }
        }

        int healthyUsers    = Math.max(0, total - issueUsers.size());
        double verificationRate = ratio(verified, total) * 100.0;
        int riskPoints = (total - verified) * 2 + banned * 3 + incompleteUsers * 2 + duplicateUsers * 3;
        int riskScore  = total == 0 ? 0 : Math.min(100, (int) Math.round((riskPoints * 100.0) / (total * 10.0)));

        if (totalUsersLabel       != null) totalUsersLabel.setText(String.valueOf(total));
        if (verificationRateLabel != null) verificationRateLabel.setText(String.format(Locale.US, "%.1f%%", verificationRate));
        if (riskScoreLabel        != null) riskScoreLabel.setText(riskScore + "/100");
        if (duplicateUsersLabel   != null) duplicateUsersLabel.setText(String.valueOf(duplicateUsers));
        if (incompleteUsersLabel  != null) incompleteUsersLabel.setText(String.valueOf(incompleteUsers));
        if (roleDonutCenterLabel  != null) roleDonutCenterLabel.setText(total + "\nusers");
        if (healthDonutCenterLabel!= null) healthDonutCenterLabel.setText(healthyUsers + "\nhealthy");

        roleDonutChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Admins", admins),
                new PieChart.Data("Staff", staff),
                new PieChart.Data("Patients", patients)));
        roleDonutChart.setLabelsVisible(false);

        healthDonutChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Sains", healthyUsers),
                new PieChart.Data("Non vérifiés", Math.max(0, total - verified)),
                new PieChart.Data("Bannis", banned),
                new PieChart.Data("Incomplets", incompleteUsers),
                new PieChart.Data("Doublons", duplicateUsers)));
        healthDonutChart.setLabelsVisible(false);

        XYChart.Series<String, Number> qualitySeries = new XYChart.Series<>();
        qualitySeries.setName("Qualité des profils");
        qualitySeries.getData().add(new XYChart.Data<>("Email manquant",    missingEmail));
        qualitySeries.getData().add(new XYChart.Data<>("Téléphone manquant",missingPhone));
        qualitySeries.getData().add(new XYChart.Data<>("Adresse manquante", missingAddress));
        qualitySeries.getData().add(new XYChart.Data<>("CIN manquant",      missingCin));
        qualityBarChart.getData().clear();
        qualityBarChart.getData().add(qualitySeries);

        String mainIssue;
        int maxIssue = Math.max(Math.max(missingEmail, missingPhone), Math.max(missingAddress, missingCin));
        if (maxIssue == 0 && duplicateUsers == 0 && banned == 0) {
            mainIssue = "Base comptes saine";
            if (insightBodyLabel != null) insightBodyLabel.setText("Aucun signal critique détecté. Priorité: maintenir le taux de vérification élevé et surveiller les doublons chaque semaine.");
        } else if (duplicateUsers >= maxIssue) {
            mainIssue = "Risque de doublons";
            if (insightBodyLabel != null) insightBodyLabel.setText("Des identités potentiellement dupliquées existent (CIN/email/téléphone). Priorité: lancer un audit anti-doublon pour éviter fraude ou erreurs métiers.");
        } else if (missingPhone == maxIssue) {
            mainIssue = "Téléphones manquants";
            if (insightBodyLabel != null) insightBodyLabel.setText("La donnée téléphone est le principal point faible. Priorité: campagne de complétion pour améliorer contact patient et support opérationnel.");
        } else if (missingAddress == maxIssue) {
            mainIssue = "Adresses incomplètes";
            if (insightBodyLabel != null) insightBodyLabel.setText("Les adresses manquantes impactent logistique et traçabilité. Priorité: rendre l'adresse obligatoire pour nouveaux updates profil.");
        } else if (missingEmail == maxIssue) {
            mainIssue = "Emails manquants";
            if (insightBodyLabel != null) insightBodyLabel.setText("Les emails manquants limitent vérification et notifications. Priorité: collecte ciblée sur comptes actifs sans email.");
        } else {
            mainIssue = "CIN manquants";
            if (insightBodyLabel != null) insightBodyLabel.setText("Le CIN manquant augmente le risque de non-conformité identité. Priorité: contrôle KYC léger à la prochaine connexion.");
        }
        if (insightTitleLabel != null) insightTitleLabel.setText("Insight principal: " + mainIssue);
    }

    @FXML
    private void onBackToUsers(ActionEvent event) {
        navigate(event, "/AdminDashboard.fxml", "MedFlow - Tableau Admin Utilisateurs");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Shared utilities
    // ══════════════════════════════════════════════════════════════════════════

    private void addFrequency(Map<String, Integer> map, String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return;
        map.put(normalized, map.getOrDefault(normalized, 0) + 1);
    }

    private int countUsersInDuplicateKeys(List<User> users,
                                          Map<String, Integer> freq,
                                          java.util.function.Function<User, String> extractor) {
        int count = 0;
        for (User user : users) {
            if (isDuplicateValue(extractor.apply(user), freq)) count++;
        }
        return count;
    }

    private boolean isDuplicateValue(String value, Map<String, Integer> freq) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return false;
        return freq.getOrDefault(normalized, 0) > 1;
    }

    private boolean isBanned(User user) {
        return safe(user.getStatutCompte()).toUpperCase(Locale.ROOT).contains("BAN");
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

    private String resolveStaffDecisionReason(User user) {
        if (user == null) {
            return "Non specifie";
        }
        String reason = safe(user.getStaffRequestReason());
        if (reason.isBlank()) {
            reason = safe(user.getStaffRequestMessage());
        }
        return reason.isBlank() ? "Non specifie" : reason;
    }

    private String buildDecisionPdfName(String prefix, User user) {
        String idPart = user == null ? "unknown" : String.valueOf(user.getId());
        return "decision_" + prefix + "_" + idPart + ".pdf";
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
            showError(title, "Le motif est obligatoire.");
            return null;
        }
        return reason;
    }

    private double ratio(int part, int total) {
        return total <= 0 ? 0 : (double) part / total;
    }

    private boolean contains(String value, String keyword) {
        return safe(value).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }


    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void navigate(ActionEvent event, String fxmlPath, String title) {
        try {
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1400, 820);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir la page demandée.");
            alert.showAndWait();
            e.printStackTrace();
        }
    }
}
