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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Reclamation;
import tn.esprit.entities.ReponseReclamation;
import tn.esprit.services.ReclamationService;
import tn.esprit.services.ReponseService;

import javafx.collections.ObservableList;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.util.LinkedHashMap;
import java.util.Map;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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

    @FXML private Label reclamationArrow;

    @FXML private Label totalLabel;
    @FXML private Label traiteesLabel;
    @FXML private Label enAttenteStatsLabel;
    @FXML private Label critiquesLabel;

    @FXML private BarChart<String, Number> typeBarChart;
    @FXML private PieChart statusPieChart;
    @FXML private LineChart<String, Number> reclamationLineChart;

    private final ReclamationService reclamationService = new ReclamationService();
    private final ObservableList<Reclamation> masterList = FXCollections.observableArrayList();
    private FilteredList<Reclamation> filteredList;



    @FXML
    public void initialize() {
        // Page reponse.fxml
        if (reclamationTable != null) {
            configureTable();
            configureSort();
            loadData();
            configureSearch();
            updateStats();
        }

        // Page ReclamationStats.fxml
        if (totalLabel != null) {
            loadReclamationStatsPage();
        }
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
        nonReponduesLabel.setText(String.valueOf(nonRepondues));
        inventoryCountLabel.setText(total + " réclamation(s) en attente");
    }

    private void addActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {

            private final Button repondreBtn = new Button("Répondre");
            private final Button voirBtn = new Button("Voir réponses");

            private final ToolBar toolBar = new ToolBar(repondreBtn, voirBtn);

            {
                toolBar.getStyleClass().add("action-toolbar");
                repondreBtn.getStyleClass().add("edit-btn");
                voirBtn.getStyleClass().add("view-btn");

                repondreBtn.setOnAction(e -> {
                    Reclamation rec = getTableView().getItems().get(getIndex());
                    ouvrirFormulaireReponse(rec);
                });

                voirBtn.setOnAction(e -> {
                    Reclamation rec = getTableView().getItems().get(getIndex());
                    ouvrirPopupReponses(rec);
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
    private void ouvrirPopupReponses(Reclamation reclamation) {
        try {
            Stage stage = new Stage();
            stage.setTitle("Réponses");

            VBox root = new VBox(20);
            root.setPadding(new javafx.geometry.Insets(25));
            root.getStyleClass().add("popup-container");

            // HEADER
            Label title = new Label("📨 Réponses");
            title.getStyleClass().add("popup-title");

            Label subtitle = new Label("Réclamation #" + reclamation.getReference_reclamation());
            subtitle.getStyleClass().add("popup-subtitle");

            VBox header = new VBox(5, title, subtitle);

            // TABLE
            TableView<ReponseReclamation> table = new TableView<>();
            table.getStyleClass().add("modern-table");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<ReponseReclamation, String> msgCol = new TableColumn<>("Message");
            msgCol.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().getMessage())
            );

            TableColumn<ReponseReclamation, String> dateCol = new TableColumn<>("Date");
            dateCol.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            data.getValue().getDate_creation_rep() != null
                                    ? data.getValue().getDate_creation_rep().toString().replace("T", " ")
                                    : ""
                    )
            );

            TableColumn<ReponseReclamation, String> statusCol = new TableColumn<>("Statut");
            statusCol.setCellValueFactory(data ->
                    new SimpleStringProperty(data.getValue().isIs_read() ? "Lu" : "Non lu")
            );

            TableColumn<ReponseReclamation, Void> actionCol = new TableColumn<>("Actions");

            actionCol.setCellFactory(col -> new TableCell<>() {

                private final Button edit = new Button("✏");
                private final Button delete = new Button("🗑");

                private final HBox box = new HBox(8, edit, delete);

                {
                    edit.getStyleClass().add("btn-edit");
                    delete.getStyleClass().add("btn-delete");

                    edit.setOnAction(e -> {
                        ReponseReclamation r = getTableView().getItems().get(getIndex());
                        ouvrirModificationReponse(r, table);
                    });

                    delete.setOnAction(e -> {
                        ReponseReclamation r = getTableView().getItems().get(getIndex());

                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setHeaderText("Supprimer cette réponse ?");
                        confirm.setContentText(r.getMessage());

                        confirm.showAndWait().ifPresent(res -> {
                            if (res == ButtonType.OK) {
                                new ReponseService().supprimer(r);
                                reloadReclamations();
                                table.getItems().remove(r);
                            }
                        });
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });

            table.getColumns().addAll(msgCol, dateCol, statusCol, actionCol);

            table.setPlaceholder(new Label("Aucune réponse 💬"));

            VBox card = new VBox(table);
            card.getStyleClass().add("card");

            // DATA
            List<ReponseReclamation> list =
                    new ReponseService().getByReclamationId(reclamation.getId_reclamation());

            table.setItems(FXCollections.observableArrayList(list));

            root.getChildren().addAll(header, card);

            Scene scene = new Scene(root, 850, 500);
            var css = getClass().getResource("/tn/esprit/CSS/popup.css");

            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            } else {
                System.out.println("❌ CSS introuvable !");
            }
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void ouvrirModificationReponse(ReponseReclamation r,TableView<ReponseReclamation> table) {
        try {
            Stage stage = new Stage();
            stage.setTitle("Modifier");

            VBox root = new VBox(20);
            root.setPadding(new javafx.geometry.Insets(25));
            root.getStyleClass().add("popup-container");

            Label title = new Label("✏ Modifier la réponse");
            title.getStyleClass().add("popup-title");

            // MESSAGE
            TextArea messageField = new TextArea(r.getMessage());
            messageField.getStyleClass().add("input-area");

            // TYPE
            ComboBox<String> typeCombo = new ComboBox<>();
            typeCombo.getItems().addAll("Email", "Téléphone", "Système");
            typeCombo.setValue(r.getType_reponse());
            typeCombo.getStyleClass().add("input-field");

            // READ
            CheckBox readCheck = new CheckBox("Lu");
            readCheck.setSelected(r.isIs_read());

            // BUTTONS
            Button save = new Button("✔ Enregistrer");
            Button cancel = new Button("Annuler");

            save.getStyleClass().add("btn-primary");
            cancel.getStyleClass().add("btn-secondary");

            HBox buttons = new HBox(10, save, cancel);

            save.setOnAction(e -> {
                r.setMessage(messageField.getText());
                r.setType_reponse(typeCombo.getValue());
                r.setIs_read(readCheck.isSelected());
                r.setDate_modification_rep(java.time.LocalDateTime.now());

                new ReponseService().modifier(r);
                reloadReclamations();
                table.refresh();

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Modifié !");
                stage.close();

            });

            cancel.setOnAction(e -> stage.close());

            root.getChildren().addAll(
                    title,
                    new Label("Message"), messageField,
                    new Label("Type"), typeCombo,
                    readCheck,
                    buttons
            );

            Scene scene = new Scene(root, 500, 420);
            var css = getClass().getResource("/tn/esprit/CSS/popup.css");

            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            } else {
                System.out.println("❌ CSS introuvable !");
            }

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void ouvrirFormulaireReponse(Reclamation reclamation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FormulaireReponse.fxml"));
            Parent root = loader.load();

            ReponseFormController controller = loader.getController();
            controller.setReclamation(reclamation);

            Scene scene = new Scene(root, 1100, 720);

            // Ajout CSS seulement s'il existe vraiment
            String cssPath = "/tn/esprit/CSS/reponseform.css";
            var cssUrl = getClass().getResource(cssPath);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.out.println("CSS introuvable : " + cssPath);
            }

            controller.setAfterSave(() -> reloadReclamations());

            Stage stage = new Stage();
            stage.setTitle("Formulaire de Réponse");
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(650);
            stage.setResizable(true);
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur inattendue : " + e.getMessage());
        }
    }

    @FXML
    private void toggleSubmenu() {
        boolean show = !submenuVBox.isVisible();

        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);

        if (show) {
            reclamationArrow.setText("⌃");
        } else {
            reclamationArrow.setText("⌄");
        }
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
    private void reloadReclamations() {
        try {
            List<Reclamation> reclamations = reclamationService.recuperer();

            masterList.setAll(reclamations);

            if (filteredList != null) {
                filteredList.setPredicate(filteredList.getPredicate());
            }

            updateStats();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadReclamationStatsPage() {
        try {
            List<Reclamation> reclamations = reclamationService.recuperer();

            int total = reclamations.size();
            int traitees = 0;
            int enAttente = 0;
            int critiques = 0;

            Map<String, Integer> typeCounts = new LinkedHashMap<>();
            Map<String, Integer> statusCounts = new LinkedHashMap<>();
            Map<String, Integer> dateCounts = new LinkedHashMap<>();

            for (Reclamation r : reclamations) {
                String statut = safe(r.getStatut_reclamation()).toLowerCase(Locale.ROOT);
                String priorite = safe(r.getPriorite()).toLowerCase(Locale.ROOT);
                String type = safe(r.getType()).isBlank() ? "Inconnu" : r.getType();

                if (statut.contains("traite")) {
                    traitees++;
                } else {
                    enAttente++;
                }

                if (priorite.contains("critique")) {
                    critiques++;
                }

                typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);

                String statusLabel = safe(r.getStatut_reclamation()).isBlank() ? "Inconnu" : r.getStatut_reclamation();
                statusCounts.put(statusLabel, statusCounts.getOrDefault(statusLabel, 0) + 1);

                String dateKey = "Sans date";
                try {
                    if (r.getDate_creation_r() != null) {
                        dateKey = r.getDate_creation_r().toLocalDate().toString();
                    }
                } catch (Exception ignored) {
                }
                dateCounts.put(dateKey, dateCounts.getOrDefault(dateKey, 0) + 1);
            }

            if (totalLabel != null) totalLabel.setText(String.valueOf(total));
            if (traiteesLabel != null) traiteesLabel.setText(String.valueOf(traitees));
            if (enAttenteStatsLabel != null) enAttenteStatsLabel.setText(String.valueOf(enAttente));
            if (critiquesLabel != null) critiquesLabel.setText(String.valueOf(critiques));

            if (typeBarChart != null) {
                typeBarChart.getData().clear();

                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Réclamations");

                for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                    series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                }

                typeBarChart.getData().add(series);
                typeBarChart.setLegendVisible(false);
            }

            if (statusPieChart != null) {
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

                for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
                    pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
                }

                statusPieChart.setData(pieData);
                statusPieChart.setLegendVisible(true);
                statusPieChart.setLabelsVisible(true);
            }

            if (reclamationLineChart != null) {
                reclamationLineChart.getData().clear();

                XYChart.Series<String, Number> lineSeries = new XYChart.Series<>();
                lineSeries.setName("Évolution");

                for (Map.Entry<String, Integer> entry : dateCounts.entrySet()) {
                    lineSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                }

                reclamationLineChart.getData().add(lineSeries);
                reclamationLineChart.setLegendVisible(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les statistiques des réclamations.");
        }
    }

    @FXML
    private void openReclamationStats() {
        navigateTo("/ReclamationStats.fxml", "MedFlow - Statistiques Réclamations");
    }

    @FXML
    private void openBlogStats() {
        navigateTo("/BlogStats.fxml", "MedFlow - Statistiques Blog");
    }

    @FXML
    private void handleLogout() {
        navigateTo("/FrontFXML/Login.fxml", "MedFlow - Connexion");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            var resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                showAlert(Alert.AlertType.ERROR, "Navigation", "FXML introuvable : " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Stage stage;
            if (reclamationTable != null) {
                stage = (Stage) reclamationTable.getScene().getWindow();
            } else {
                stage = (Stage) totalLabel.getScene().getWindow();
            }

            stage.setScene(new Scene(root, 1650, 960));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation", "Impossible d'ouvrir la page demandée : " + e.getMessage());
        }
    }

    @FXML
    private void backToReponses() {
        navigateTo("/reponse.fxml", "MedFlow - Gestion des réclamations");
    }

}
