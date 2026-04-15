package tn.esprit.controllers;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Reclamation;
import tn.esprit.services.ReclamationService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ReponseController {

    @FXML private Label totalReclamationsLabel;
    @FXML private Label reponduesLabel;
    @FXML private Label enAttenteLabel;
    @FXML private Label nonReponduesLabel;
    @FXML private Label inventoryCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    @FXML private TableView<Reclamation> reclamationTable;
    @FXML private TableColumn<Reclamation, String> referenceCol;
    @FXML private TableColumn<Reclamation, String> typeCol;
    @FXML private TableColumn<Reclamation, String> statutCol;
    @FXML private TableColumn<Reclamation, String> prioriteCol;
    @FXML private TableColumn<Reclamation, String> descriptionCol;
    @FXML private TableColumn<Reclamation, Void> actionsCol;

    @FXML private VBox submenuVBox;
    @FXML private Button voirTousBtn;

    private final ReclamationService reclamationService = new ReclamationService();
    private final ObservableList<Reclamation> masterList = FXCollections.observableArrayList();
    private FilteredList<Reclamation> filteredList;

    @FXML private Label evenementArrow;

    @FXML
    public void initialize() {
        configureTable();
        configureSort();
        loadData();
        configureSearch();
        updateStats();
    }

    private void configureTable() {
        referenceCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("reference_reclamation"));
        typeCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("type"));
        statutCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("statut_reclamation"));
        prioriteCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("priorite"));

        descriptionCol.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescription();
            if (desc == null) desc = "";
            if (desc.length() > 45) {
                desc = desc.substring(0, 45) + "...";
            }
            return new SimpleStringProperty(desc);
        });

        addActionsColumn();
        reclamationTable.setPlaceholder(new Label("Aucune réclamation trouvée"));
    }

    private void configureSort() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier...",
                "Référence A-Z",
                "Type A-Z",
                "Statut",
                "Priorité"
        ));
        sortCombo.setValue("Trier...");
        sortCombo.setOnAction(e -> applySort());
    }

    private void loadData() {
        try {
            List<Reclamation> reclamations = reclamationService.recuperer();
            masterList.setAll(reclamations);

            filteredList = new FilteredList<>(masterList, p -> true);
            reclamationTable.setItems(filteredList);

            inventoryCountLabel.setText(masterList.size() + " réclamation(s) en attente");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors du chargement des données: " + e.getMessage());
        }
    }

    private void configureSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String keyword = newVal == null ? "" : newVal.trim().toLowerCase(Locale.ROOT);

            filteredList.setPredicate(rec -> {
                if (keyword.isEmpty()) return true;

                return contains(rec.getReference_reclamation(), keyword)
                        || contains(rec.getType(), keyword)
                        || contains(rec.getStatut_reclamation(), keyword)
                        || contains(rec.getPriorite(), keyword)
                        || contains(rec.getDescription(), keyword);
            });

            updateStats();
        });
    }

    private void applySort() {
        String selected = sortCombo.getValue();
        if (selected == null) return;

        switch (selected) {
            case "Référence A-Z" ->
                    FXCollections.sort(masterList, (a, b) -> safe(a.getReference_reclamation()).compareToIgnoreCase(safe(b.getReference_reclamation())));
            case "Type A-Z" ->
                    FXCollections.sort(masterList, (a, b) -> safe(a.getType()).compareToIgnoreCase(safe(b.getType())));
            case "Statut" ->
                    FXCollections.sort(masterList, (a, b) -> safe(a.getStatut_reclamation()).compareToIgnoreCase(safe(b.getStatut_reclamation())));
            case "Priorité" ->
                    FXCollections.sort(masterList, (a, b) -> safe(a.getPriorite()).compareToIgnoreCase(safe(b.getPriorite())));
            default -> {
            }
        }
    }

    private void updateStats() {
        int total = filteredList.size();
        int repondues = 0;
        int nonRepondues = 0;

        for (Reclamation r : filteredList) {
            String statut = safe(r.getStatut_reclamation()).toLowerCase(Locale.ROOT);
            if (statut.contains("traite")) {
                repondues++;
            } else {
                nonRepondues++;
            }
        }

        totalReclamationsLabel.setText(String.valueOf(total));
        reponduesLabel.setText(String.valueOf(repondues));
        enAttenteLabel.setText("0"); // Toujours 0 selon la demande
        nonReponduesLabel.setText(String.valueOf(nonRepondues));
        inventoryCountLabel.setText(total + " réclamation(s) en attente");
    }

    private void addActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button repondreBtn = new Button("Répondre");
            private final ToolBar toolBar = new ToolBar(repondreBtn);

            {
                toolBar.getStyleClass().add("action-toolbar");
                repondreBtn.getStyleClass().add("edit-btn");

                repondreBtn.setOnAction(e -> {
                    Reclamation rec = getTableView().getItems().get(getIndex());
                    ouvrirFormulaireReponse(rec);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : toolBar);
            }
        });

        actionsCol.setCellValueFactory(param -> Bindings.createObjectBinding(() -> null));
    }

    private void ouvrirFormulaireReponse(Reclamation reclamation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FormulaireReponse.fxml"));
            Parent root = loader.load();

            ReponseFormController controller = loader.getController();
            controller.setReclamation(reclamation);

            Stage stage = new Stage();
            stage.setTitle("Formulaire de Réponse");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    @FXML
    private void toggleSubmenu() {
        boolean show = !submenuVBox.isVisible();

        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);

        if (show) {
            evenementArrow.setText("⌃");
        } else {
            evenementArrow.setText("⌄");
        }
    }

    @FXML
    private void onVoirTousEvenements() {
        // Logique pour afficher toutes les réclamations
    }

    @FXML
    private void onTableauBordAdmin() {
        // Logique pour ouvrir le tableau de bord admin
    }

    @FXML
    private void onAutresActions() {
        // Logique pour autres actions
    }

    private boolean contains(String value, String keyword) {
        return safe(value).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
