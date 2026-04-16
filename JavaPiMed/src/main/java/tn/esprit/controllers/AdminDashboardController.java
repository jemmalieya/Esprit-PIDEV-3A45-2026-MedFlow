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
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.tools.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;

public class AdminDashboardController {

    @FXML private Label totalUsersLabel;
    @FXML private Label adminUsersLabel;
    @FXML private Label staffUsersLabel;
    @FXML private Label patientUsersLabel;
    @FXML private Label inventoryCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Button sortDirectionBtn;
    @FXML private Label reportHintLabel;

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

    @FXML private VBox submenuVBox;
    @FXML private Label userArrow;
    @FXML private Button voirTousUsersBtn;

    private final UserService userService = new UserService();
    private final ObservableList<User> masterList = FXCollections.observableArrayList();
    private FilteredList<User> filteredList;
    private boolean sortAscending = false;

    @FXML
    public void initialize() {
        configureTable();
        configureSort();
        loadData();
        applySort();
        configureSearch();
        updateStats();
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

        // Keep all columns visible in the available width (no horizontal scroll needed).
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
            case "Tri: Nom" -> comparator = Comparator.comparing(u -> safe(u.getNom()).toLowerCase(Locale.ROOT));
            case "Tri: Prénom" -> comparator = Comparator.comparing(u -> safe(u.getPrenom()).toLowerCase(Locale.ROOT));
            case "Tri: Email" -> comparator = Comparator.comparing(u -> safe(u.getEmailUser()).toLowerCase(Locale.ROOT));
            case "Tri: Rôle" -> comparator = Comparator.comparing(u -> safe(u.getRoleSysteme()).toLowerCase(Locale.ROOT));
            case "Tri: Statut" -> comparator = Comparator.comparing(u -> safe(u.getStatutCompte()).toLowerCase(Locale.ROOT));
            case "Tri: Vérification" -> comparator = Comparator.comparing(User::isVerified);
            default -> comparator = Comparator.comparingInt(User::getId);
        }

        if (!sortAscending) {
            comparator = comparator.reversed();
        }

        FXCollections.sort(masterList, comparator);
    }

    private void addActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button banBtn = new Button();
            private final Button deleteBtn = new Button("🗑");
            private final ToolBar toolBar = new ToolBar(banBtn, deleteBtn);

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
                    confirm.setContentText("Voulez-vous " + actionText + " l'utilisateur : " + safe(user.getNom()) + " " + safe(user.getPrenom()) + " ?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        boolean updated = userService.updateStatutCompte(user.getId(), nextStatus);
                        if (updated) {
                            user.setStatutCompte(nextStatus);
                            applySort();
                            userTable.refresh();
                            updateStats();
                        } else {
                            showError("Action impossible", "La mise a jour du statut a echoue.");
                        }
                    }
                });

                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Supprimer l'utilisateur : " + safe(user.getNom()) + " " + safe(user.getPrenom()) + " ?");

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
                banBtn.setText(isBanned(user) ? "🔓" : "🚫");
                setGraphic(toolBar);
            }
        });

        actionsCol.setCellValueFactory(param -> Bindings.createObjectBinding(() -> null));
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

                // Smart query mode supports tokens like:
                // role:admin verified:yes status:active name:ahmed
                String[] tokens = keyword.split("\\s+");
                for (String token : tokens) {
                    if (token.isBlank()) {
                        continue;
                    }

                    if (token.startsWith("role:")) {
                        String value = token.substring(5);
                        if (!contains(user.getRoleSysteme(), value)) {
                            return false;
                        }
                    } else if (token.startsWith("verified:")) {
                        String value = token.substring(9);
                        boolean expected = value.equals("yes") || value.equals("oui") || value.equals("true") || value.equals("1");
                        if (user.isVerified() != expected) {
                            return false;
                        }
                    } else if (token.startsWith("status:")) {
                        String value = token.substring(7);
                        if (!contains(user.getStatutCompte(), value)) {
                            return false;
                        }
                    } else if (token.startsWith("name:")) {
                        String value = token.substring(5);
                        if (!(contains(user.getNom(), value) || contains(user.getPrenom(), value))) {
                            return false;
                        }
                    } else if (token.startsWith("email:")) {
                        String value = token.substring(6);
                        if (!contains(user.getEmailUser(), value)) {
                            return false;
                        }
                    } else {
                        // Global fallback token (matches any main user field)
                        boolean globalMatch = contains(user.getCin(), token)
                                || contains(user.getNom(), token)
                                || contains(user.getPrenom(), token)
                                || contains(user.getEmailUser(), token)
                                || contains(user.getTelephoneUser(), token)
                                || contains(user.getRoleSysteme(), token)
                                || contains(user.getStatutCompte(), token);
                        if (!globalMatch) {
                            return false;
                        }
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
        int adminCount = 0;
        int staffCount = 0;
        int patientCount = 0;

        for (User user : filteredList) {
            String role = safe(user.getRoleSysteme()).toUpperCase(Locale.ROOT);
            if ("ADMIN".equals(role)) {
                adminCount++;
            } else if ("STAFF".equals(role)) {
                staffCount++;
            } else {
                patientCount++;
            }
        }

        totalUsersLabel.setText(String.valueOf(total));
        adminUsersLabel.setText(String.valueOf(adminCount));
        staffUsersLabel.setText(String.valueOf(staffCount));
        patientUsersLabel.setText(String.valueOf(patientCount));
        inventoryCountLabel.setText(total + " utilisateur(s) dans votre espace");
    }

    @FXML
    private void toggleSubmenu(ActionEvent event) {
        boolean show = !submenuVBox.isVisible();
        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);
        userArrow.setText(show ? "⌃" : "⌄");
    }

    @FXML
    private void onVoirTousUsers(ActionEvent event) {
        if (searchField != null) {
            searchField.clear();
        }
        sortAscending = false;
        if (sortDirectionBtn != null) {
            sortDirectionBtn.setText("↓ Desc");
        }
        if (sortCombo != null) {
            sortCombo.setValue("Tri: Plus récents (ID)");
        }
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
    private void onExportSmartReport(ActionEvent event) {
        List<User> source = new ArrayList<>(filteredList);
        if (source.isEmpty()) {
            showError("Export PDF", "Aucune donnee a exporter dans le contexte courant.");
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
            ok.setHeaderText("Rapport genere avec succes");
            ok.setContentText("Fichier: " + file.getAbsolutePath());
            ok.showAndWait();
        } catch (Exception e) {
            showError("Export PDF", "Impossible de generer le rapport PDF.");
            e.printStackTrace();
        }
    }

    private void exportAuditReportPdf(List<User> users, File file) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, fos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font h2Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font danger = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            danger.setColor(186, 29, 29);

            Paragraph title = new Paragraph("MedFlow - Rapport Audit Utilisateurs", titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            document.add(title);
            document.add(new Paragraph("Genere le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normal));
            document.add(new Paragraph(" "));

            int total = users.size();
            int verified = 0;
            int banned = 0;
            int missingPhone = 0;
            int missingAddress = 0;
            int adminCount = 0;
            int staffCount = 0;
            int patientCount = 0;

            Map<String, Integer> emailFreq = new HashMap<>();
            Map<String, Integer> cinFreq = new HashMap<>();
            Map<String, Integer> phoneFreq = new HashMap<>();

            for (User user : users) {
                if (user.isVerified()) {
                    verified++;
                }
                if (isBanned(user)) {
                    banned++;
                }
                if (safe(user.getTelephoneUser()).isBlank()) {
                    missingPhone++;
                }
                if (safe(user.getAdresseUser()).isBlank()) {
                    missingAddress++;
                }

                String role = safe(user.getRoleSysteme()).toUpperCase(Locale.ROOT);
                if ("ADMIN".equals(role)) {
                    adminCount++;
                } else if ("STAFF".equals(role)) {
                    staffCount++;
                } else {
                    patientCount++;
                }

                addFrequency(emailFreq, safe(user.getEmailUser()));
                addFrequency(cinFreq, safe(user.getCin()));
                addFrequency(phoneFreq, safe(user.getTelephoneUser()));
            }

            int duplicateEmailUsers = countUsersInDuplicateKeys(users, emailFreq, User::getEmailUser);
            int duplicateCinUsers = countUsersInDuplicateKeys(users, cinFreq, User::getCin);
            int duplicatePhoneUsers = countUsersInDuplicateKeys(users, phoneFreq, User::getTelephoneUser);

            double verificationRate = total == 0 ? 0 : (verified * 100.0 / total);
            int riskPoints = (total - verified) * 2 + missingPhone + missingAddress + duplicateEmailUsers + duplicateCinUsers + duplicatePhoneUsers;
            int riskScore = total == 0 ? 0 : Math.min(100, (int) Math.round((riskPoints * 100.0) / (total * 5.0)));

            document.add(new Paragraph("1) Synthese Audit", h2Font));
            PdfPTable summary = new PdfPTable(4);
            summary.setWidthPercentage(100);
            summary.setSpacingBefore(8);
            summary.setSpacingAfter(8);
            summary.addCell(metricCell("Total utilisateurs", String.valueOf(total), normal));
            summary.addCell(metricCell("Taux verification", String.format(Locale.US, "%.1f%%", verificationRate), normal));
            summary.addCell(metricCell("Comptes bannis", String.valueOf(banned), normal));
            summary.addCell(metricCell("Indice risque", riskScore + "/100", danger));
            summary.addCell(metricCell("Admins", String.valueOf(adminCount), normal));
            summary.addCell(metricCell("Staff", String.valueOf(staffCount), normal));
            summary.addCell(metricCell("Patients", String.valueOf(patientCount), normal));
            summary.addCell(metricCell("Sans telephone", String.valueOf(missingPhone), normal));
            document.add(summary);

            document.add(new Paragraph("2) Anomalies non visibles rapidement a l'ecran", h2Font));
            document.add(new Paragraph("- Utilisateurs avec email duplique: " + duplicateEmailUsers, normal));
            document.add(new Paragraph("- Utilisateurs avec CIN duplique: " + duplicateCinUsers, normal));
            document.add(new Paragraph("- Utilisateurs avec telephone duplique: " + duplicatePhoneUsers, normal));
            document.add(new Paragraph("- Utilisateurs sans adresse: " + missingAddress, normal));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("3) Plan d'action recommande", h2Font));
            document.add(new Paragraph(buildActionLine(total - verified, "Verifier les comptes non verifies", "Equipe Support / IAM"), normal));
            document.add(new Paragraph(buildActionLine(duplicateEmailUsers + duplicateCinUsers + duplicatePhoneUsers, "Enquete anti-doublon (email/CIN/telephone)", "Equipe Data / Support"), normal));
            document.add(new Paragraph(buildActionLine(missingPhone + missingAddress, "Completer les profils incomplets", "Equipe Operations"), normal));
            document.add(new Paragraph(buildActionLine(banned, "Revue des comptes bannis", "Equipe Securite"), normal));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("4) Top comptes a traiter en priorite", h2Font));
            PdfPTable actionsTable = new PdfPTable(new float[]{1.2f, 2.2f, 3.0f, 1.6f, 2.8f, 2.8f});
            actionsTable.setWidthPercentage(100);
            actionsTable.setSpacingBefore(8);
            addHeader(actionsTable, "ID", normal);
            addHeader(actionsTable, "Nom", normal);
            addHeader(actionsTable, "Email", normal);
            addHeader(actionsTable, "Role", normal);
            addHeader(actionsTable, "Probleme", normal);
            addHeader(actionsTable, "Action", normal);

            List<User> priorityUsers = computePriorityUsers(users, emailFreq, cinFreq, phoneFreq);
            int max = Math.min(priorityUsers.size(), 20);
            for (int i = 0; i < max; i++) {
                User user = priorityUsers.get(i);
                actionsTable.addCell(dataCell(String.valueOf(user.getId()), normal));
                actionsTable.addCell(dataCell((safe(user.getNom()) + " " + safe(user.getPrenom())).trim(), normal));
                actionsTable.addCell(dataCell(safe(user.getEmailUser()), normal));
                actionsTable.addCell(dataCell(safe(user.getRoleSysteme()), normal));
                actionsTable.addCell(dataCell(computeIssueLabel(user, emailFreq, cinFreq, phoneFreq), normal));
                actionsTable.addCell(dataCell(computeRecommendedAction(user, emailFreq, cinFreq, phoneFreq), normal));
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
                computeRiskWeight(a, emailFreq, cinFreq, phoneFreq)
        ));
        return priority;
    }

    private int computeRiskWeight(User user,
                                  Map<String, Integer> emailFreq,
                                  Map<String, Integer> cinFreq,
                                  Map<String, Integer> phoneFreq) {
        int score = 0;
        if (!user.isVerified()) score += 4;
        if (safe(user.getTelephoneUser()).isBlank()) score += 2;
        if (safe(user.getAdresseUser()).isBlank()) score += 1;
        if (isDuplicateValue(user.getEmailUser(), emailFreq)) score += 3;
        if (isDuplicateValue(user.getCin(), cinFreq)) score += 3;
        if (isDuplicateValue(user.getTelephoneUser(), phoneFreq)) score += 2;
        return score;
    }

    private String computeIssueLabel(User user,
                                     Map<String, Integer> emailFreq,
                                     Map<String, Integer> cinFreq,
                                     Map<String, Integer> phoneFreq) {
        List<String> issues = new ArrayList<>();
        if (!user.isVerified()) issues.add("non verifie");
        if (isDuplicateValue(user.getEmailUser(), emailFreq)) issues.add("email duplique");
        if (isDuplicateValue(user.getCin(), cinFreq)) issues.add("CIN duplique");
        if (isDuplicateValue(user.getTelephoneUser(), phoneFreq)) issues.add("tel duplique");
        if (safe(user.getTelephoneUser()).isBlank()) issues.add("tel manquant");
        if (safe(user.getAdresseUser()).isBlank()) issues.add("adresse manquante");
        if (issues.isEmpty()) issues.add("RAS");
        return String.join(", ", issues);
    }

    private String computeRecommendedAction(User user,
                                            Map<String, Integer> emailFreq,
                                            Map<String, Integer> cinFreq,
                                            Map<String, Integer> phoneFreq) {
        if (!user.isVerified()) return "Relancer verification";
        if (isDuplicateValue(user.getCin(), cinFreq)) return "Verifier unicite CIN";
        if (isDuplicateValue(user.getEmailUser(), emailFreq)) return "Verifier doublon email";
        if (isDuplicateValue(user.getTelephoneUser(), phoneFreq)) return "Verifier doublon telephone";
        if (safe(user.getTelephoneUser()).isBlank() || safe(user.getAdresseUser()).isBlank()) return "Completer profil";
        return "Surveillance standard";
    }

    private void addFrequency(Map<String, Integer> map, String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return;
        }
        map.put(normalized, map.getOrDefault(normalized, 0) + 1);
    }

    private int countUsersInDuplicateKeys(List<User> users,
                                          Map<String, Integer> freq,
                                          java.util.function.Function<User, String> extractor) {
        int count = 0;
        for (User user : users) {
            if (isDuplicateValue(extractor.apply(user), freq)) {
                count++;
            }
        }
        return count;
    }

    private boolean isDuplicateValue(String value, Map<String, Integer> freq) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return false;
        }
        return freq.getOrDefault(normalized, 0) > 1;
    }

    private boolean isBanned(User user) {
        return safe(user.getStatutCompte()).toUpperCase(Locale.ROOT).contains("BAN");
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

    private boolean contains(String value, String keyword) {
        return safe(value).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
