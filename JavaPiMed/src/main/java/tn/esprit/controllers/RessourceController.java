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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Ressource;
import tn.esprit.services.EvenementService;
import tn.esprit.services.RessourceService;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class RessourceController {
    private static final double DEFAULT_SCENE_WIDTH = 1650;
    private static final double DEFAULT_SCENE_HEIGHT = 960;

    @FXML private VBox submenuVBox;
    @FXML private Label ressourceArrow;
    @FXML private Label totalResourcesLabel;
    @FXML private Label fileResourcesLabel;
    @FXML private Label linkResourcesLabel;
    @FXML private Label stockResourcesLabel;
    @FXML private Label inventoryCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    @FXML private TableView<Ressource> ressourceTable;
    @FXML private TableColumn<Ressource, String> eventCol;
    @FXML private TableColumn<Ressource, String> nomCol;
    @FXML private TableColumn<Ressource, String> categorieCol;
    @FXML private TableColumn<Ressource, String> typeCol;
    @FXML private TableColumn<Ressource, String> quantiteCol;
    @FXML private TableColumn<Ressource, String> fournisseurCol;
    @FXML private TableColumn<Ressource, String> notesCol;
    @FXML private TableColumn<Ressource, Void> actionsCol;

    @FXML private ComboBox<Evenement> cbEvenement;
    @FXML private TextField tfNomRessource;
    @FXML private TextField tfCategorieRessource;
    @FXML private ComboBox<String> cbTypeRessource;
    @FXML private TextField tfCheminFichier;
    @FXML private TextField tfMimeType;
    @FXML private TextField tfTailleKb;
    @FXML private TextField tfUrlExterne;
    @FXML private TextField tfQuantite;
    @FXML private TextField tfUnite;
    @FXML private TextField tfFournisseur;
    @FXML private TextField tfCoutEstime;
    @FXML private CheckBox cbEstPublique;
    @FXML private TextArea taNotesRessource;

    private final RessourceService ressourceService = new RessourceService();
    private final EvenementService evenementService = new EvenementService();
    private final ObservableList<Ressource> masterList = FXCollections.observableArrayList();
    private final ObservableList<Evenement> evenements = FXCollections.observableArrayList();
    private final Map<Integer, String> eventTitlesById = new HashMap<>();
    private FilteredList<Ressource> filteredList;

    private static Ressource ressourceAModifier;

    @FXML
    public void initialize() {
        initEventOptions();
        initDashboardIfExists();
        initFormIfExists();
        chargerRessourceAModifier();
    }

    @FXML
    private void toggleSubmenu() {
        if (submenuVBox == null || ressourceArrow == null) return;

        boolean show = !submenuVBox.isVisible();
        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);
        ressourceArrow.setText(show ? "⌃" : "⌄");
    }

    @FXML
    private void retourListeRessources() {
        ressourceAModifier = null;
        loadScene("/RessourceDashboard.fxml", "Gestion des Ressources");
    }

    @FXML
    private void onVoirTousRessources() {
        retourListeRessources();
    }

    @FXML
    private void onNewRessource() {
        ressourceAModifier = null;
        loadScene("/AjouterRessource.fxml", "Nouvelle Ressource");
    }

    @FXML
    private void onGestionEvenements() {
        loadScene("/EvenementDashboard.fxml", "Gestion des Evenements");
    }

    @FXML
    private void ajouterRessource() {
        saveRessource(false);
    }

    @FXML
    private void ajouterRessourceEtNouvelle() {
        saveRessource(true);
    }

    @FXML
    private void modifierRessource() {
        if (ressourceAModifier == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Aucune ressource selectionnee.");
            return;
        }

        try {
            remplirRessourceDepuisForm(ressourceAModifier);
            ressourceAModifier.setDate_mise_a_jour_ressource(new Date());
            ressourceService.modifier(ressourceAModifier);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Ressource modifiee avec succes.");
            ressourceAModifier = null;
            retourListeRessources();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void initDashboardIfExists() {
        if (ressourceTable == null) return;

        configureTable();
        configureSort();
        loadData();
        configureSearch();
        updateStats();
    }

    private void initFormIfExists() {
        if (cbTypeRessource == null) return;

        cbTypeRessource.setItems(FXCollections.observableArrayList(
                "file", "external_link", "stock_item"
        ));
        configureEventComboBox();
    }

    private void initEventOptions() {
        try {
            evenements.setAll(evenementService.recuperer());
            eventTitlesById.clear();
            for (Evenement event : evenements) {
                eventTitlesById.put(event.getId(), text(event.getTitre_event()));
            }
        } catch (SQLException e) {
            eventTitlesById.clear();
        }
    }

    private void configureTable() {
        eventCol.setCellValueFactory(cd -> new SimpleStringProperty(getEventTitle(cd.getValue())));
        nomCol.setCellValueFactory(cd -> new SimpleStringProperty(text(cd.getValue().getNom_ressource())));
        categorieCol.setCellValueFactory(cd -> new SimpleStringProperty(text(cd.getValue().getCategorie_ressource())));
        typeCol.setCellValueFactory(cd -> new SimpleStringProperty(text(cd.getValue().getType_ressource())));
        quantiteCol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getQuantite_disponible_ressource())));
        fournisseurCol.setCellValueFactory(cd -> new SimpleStringProperty(text(cd.getValue().getFournisseur_ressource())));
        notesCol.setCellValueFactory(cd -> new SimpleStringProperty(shortText(text(cd.getValue().getNotes_ressource()), 45)));

        addActionsColumn();
        ressourceTable.setPlaceholder(new Label("Aucune ressource trouvee"));
    }

    private void configureSort() {
        if (sortCombo == null) return;
        sortCombo.setItems(FXCollections.observableArrayList(
                "Nom A-Z", "Categorie A-Z", "Type", "Evenement"
        ));
        sortCombo.setOnAction(e -> applySort());
    }

    private void loadData() {
        masterList.setAll(ressourceService.recuperer());
        filteredList = new FilteredList<>(masterList, r -> true);
        ressourceTable.setItems(filteredList);
        if (inventoryCountLabel != null) {
            inventoryCountLabel.setText(masterList.size() + " ressource(s) dans votre espace");
        }
    }

    private void configureSearch() {
        if (searchField == null) return;
        searchField.textProperty().addListener((o, a, b) -> {
            String keyword = normalize(b);
            filteredList.setPredicate(r ->
                    keyword.isEmpty()
                            || normalize(r.getNom_ressource()).contains(keyword)
                            || normalize(r.getCategorie_ressource()).contains(keyword)
                            || normalize(r.getType_ressource()).contains(keyword)
                            || normalize(getEventTitle(r)).contains(keyword)
            );
            updateStats();
        });
    }

    private void applySort() {
        if (sortCombo == null || sortCombo.getValue() == null) return;

        switch (sortCombo.getValue()) {
            case "Categorie A-Z" ->
                    FXCollections.sort(masterList, (a, b) -> text(a.getCategorie_ressource()).compareToIgnoreCase(text(b.getCategorie_ressource())));
            case "Type" ->
                    FXCollections.sort(masterList, (a, b) -> text(a.getType_ressource()).compareToIgnoreCase(text(b.getType_ressource())));
            case "Evenement" ->
                    FXCollections.sort(masterList, (a, b) -> getEventTitle(a).compareToIgnoreCase(getEventTitle(b)));
            default ->
                    FXCollections.sort(masterList, (a, b) -> text(a.getNom_ressource()).compareToIgnoreCase(text(b.getNom_ressource())));
        }
    }

    private void updateStats() {
        if (filteredList == null) return;

        totalResourcesLabel.setText(String.valueOf(filteredList.size()));
        fileResourcesLabel.setText(String.valueOf(filteredList.stream().filter(r -> "file".equalsIgnoreCase(text(r.getType_ressource()))).count()));
        linkResourcesLabel.setText(String.valueOf(filteredList.stream().filter(r -> "external_link".equalsIgnoreCase(text(r.getType_ressource()))).count()));
        stockResourcesLabel.setText(String.valueOf(filteredList.stream().filter(r -> "stock_item".equalsIgnoreCase(text(r.getType_ressource()))).count()));
    }

    private void addActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("Voir");
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final ToolBar toolBar = new ToolBar(viewBtn, editBtn, deleteBtn);

            {
                toolBar.getStyleClass().add("action-toolbar");
                viewBtn.getStyleClass().add("view-btn");
                editBtn.getStyleClass().add("edit-btn");
                deleteBtn.getStyleClass().add("delete-btn");

                viewBtn.setOnAction(e -> {
                    Ressource r = getTableView().getItems().get(getIndex());
                    afficherPopupDetails(r);
                });

                editBtn.setOnAction(e -> {
                    Ressource r = getTableView().getItems().get(getIndex());
                    ouvrirPageModification(r);
                });

                deleteBtn.setOnAction(e -> {
                    Ressource r = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Supprimer la ressource : " + text(r.getNom_ressource()) + " ?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        ressourceService.supprimer(r);
                        masterList.remove(r);
                        updateStats();
                        if (inventoryCountLabel != null) {
                            inventoryCountLabel.setText(masterList.size() + " ressource(s) dans votre espace");
                        }
                    }
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

    private void ouvrirPageModification(Ressource ressource) {
        ressourceAModifier = ressource;
        loadScene("/ModifierRessource.fxml", "Modifier Ressource");
    }

    private void chargerRessourceAModifier() {
        if (ressourceAModifier == null || tfNomRessource == null) return;

        cbEvenement.setValue(findEventById(ressourceAModifier.getEvenement() == null ? 0 : ressourceAModifier.getEvenement().getId()));
        tfNomRessource.setText(trimValue(ressourceAModifier.getNom_ressource()));
        tfCategorieRessource.setText(trimValue(ressourceAModifier.getCategorie_ressource()));
        cbTypeRessource.setValue(trimValue(ressourceAModifier.getType_ressource()).isBlank() ? null : trimValue(ressourceAModifier.getType_ressource()));
        tfCheminFichier.setText(trimValue(ressourceAModifier.getChemin_fichier_ressource()));
        tfMimeType.setText(trimValue(ressourceAModifier.getMime_type_ressource()));
        tfTailleKb.setText(ressourceAModifier.getTaille_kb_ressource() == 0 ? "" : String.valueOf(ressourceAModifier.getTaille_kb_ressource()));
        tfUrlExterne.setText(trimValue(ressourceAModifier.getUrl_externe_ressource()));
        tfQuantite.setText(ressourceAModifier.getQuantite_disponible_ressource() == 0 ? "" : String.valueOf(ressourceAModifier.getQuantite_disponible_ressource()));
        tfUnite.setText(trimValue(ressourceAModifier.getUnite_ressource()));
        tfFournisseur.setText(trimValue(ressourceAModifier.getFournisseur_ressource()));
        tfCoutEstime.setText(ressourceAModifier.getCout_estime_ressource() == 0 ? "" : String.valueOf(ressourceAModifier.getCout_estime_ressource()));
        cbEstPublique.setSelected(ressourceAModifier.isEst_publique_ressource());
        taNotesRessource.setText(trimValue(ressourceAModifier.getNotes_ressource()));
    }

    private void saveRessource(boolean stayOnForm) {
        try {
            Ressource ressource = new Ressource();
            remplirRessourceDepuisForm(ressource);
            ressource.setDate_creation_ressource(new Date());
            ressource.setDate_mise_a_jour_ressource(new Date());
            ressourceService.ajouter(ressource);

            showAlert(Alert.AlertType.INFORMATION, "Succes", "Ressource ajoutee avec succes.");

            if (stayOnForm) {
                Evenement selectedEvent = cbEvenement.getValue();
                clearForm();
                cbEvenement.setValue(selectedEvent);
            } else {
                retourListeRessources();
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void remplirRessourceDepuisForm(Ressource ressource) throws Exception {
        if (cbEvenement == null || cbEvenement.getValue() == null) {
            throw new Exception("L'evenement est obligatoire.");
        }
        if (tfNomRessource.getText() == null || tfNomRessource.getText().isBlank()) {
            throw new Exception("Le nom de la ressource est obligatoire.");
        }
        if (cbTypeRessource.getValue() == null || cbTypeRessource.getValue().isBlank()) {
            throw new Exception("Le type de ressource est obligatoire.");
        }

        ressource.setEvenement(cbEvenement.getValue());
        ressource.setNom_ressource(tfNomRessource.getText().trim());
        ressource.setCategorie_ressource(trimValue(tfCategorieRessource.getText()));
        ressource.setType_ressource(cbTypeRessource.getValue());
        ressource.setChemin_fichier_ressource(trimValue(tfCheminFichier.getText()));
        ressource.setMime_type_ressource(trimValue(tfMimeType.getText()));
        ressource.setTaille_kb_ressource(parseInt(tfTailleKb));
        ressource.setUrl_externe_ressource(trimValue(tfUrlExterne.getText()));
        ressource.setQuantite_disponible_ressource(parseInt(tfQuantite));
        ressource.setUnite_ressource(trimValue(tfUnite.getText()));
        ressource.setFournisseur_ressource(trimValue(tfFournisseur.getText()));
        ressource.setCout_estime_ressource(parseDouble(tfCoutEstime));
        ressource.setEst_publique_ressource(cbEstPublique.isSelected());
        ressource.setNotes_ressource(trimValue(taNotesRessource.getText()));
    }

    private void clearForm() {
        tfNomRessource.clear();
        tfCategorieRessource.clear();
        cbTypeRessource.setValue(null);
        tfCheminFichier.clear();
        tfMimeType.clear();
        tfTailleKb.clear();
        tfUrlExterne.clear();
        tfQuantite.clear();
        tfUnite.clear();
        tfFournisseur.clear();
        tfCoutEstime.clear();
        cbEstPublique.setSelected(false);
        taNotesRessource.clear();
    }

    private void afficherPopupDetails(Ressource r) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Details ressource");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialog.getDialogPane().setMinWidth(440);
        dialog.getDialogPane().setPrefWidth(440);

        VBox card = new VBox(16);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 24;" +
                        "-fx-padding: 24;" +
                        "-fx-border-color: #e6eef5;" +
                        "-fx-border-radius: 24;"
        );

        Label title = new Label(text(r.getNom_ressource()));
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #16325c;");

        VBox infoBox = new VBox(10,
                createDetailRow("Evenement", getEventTitle(r)),
                createDetailRow("Categorie", text(r.getCategorie_ressource())),
                createDetailRow("Type", text(r.getType_ressource())),
                createDetailRow("Quantite", String.valueOf(r.getQuantite_disponible_ressource())),
                createDetailRow("Fournisseur", text(r.getFournisseur_ressource())),
                createDetailRow("URL", text(r.getUrl_externe_ressource()))
        );
        infoBox.setStyle("-fx-background-color: #f7fbfd; -fx-background-radius: 18; -fx-padding: 16;");

        Label notesTitle = new Label("Notes");
        notesTitle.setStyle("-fx-text-fill: #16325c; -fx-font-size: 14px; -fx-font-weight: bold;");

        Label notes = new Label(text(r.getNotes_ressource()));
        notes.setWrapText(true);
        notes.setStyle(
                "-fx-background-color: #ffffff;" +
                        "-fx-border-color: #e6eef5;" +
                        "-fx-border-radius: 16;" +
                        "-fx-background-radius: 16;" +
                        "-fx-padding: 14;" +
                        "-fx-text-fill: #5b6b84;" +
                        "-fx-font-size: 13px;"
        );

        card.getChildren().addAll(title, infoBox, notesTitle, notes);
        dialog.getDialogPane().setContent(card);
        dialog.showAndWait();
    }

    private HBox createDetailRow(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.setMinWidth(92);
        label.setStyle("-fx-text-fill: #7a8aa2; -fx-font-size: 12px; -fx-font-weight: bold;");

        Label value = new Label(text(valueText));
        value.setWrapText(true);
        value.setMaxWidth(Double.MAX_VALUE);
        value.setStyle("-fx-text-fill: #183153; -fx-font-size: 13px; -fx-font-weight: 600;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new HBox(10, label, spacer, value);
    }

    private void loadScene(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = resolveCurrentStage();
            if (stage == null) {
                throw new Exception("Impossible de recuperer la fenetre actuelle.");
            }
            applySceneToStage(stage, root, title);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation echouee : " + e.getMessage());
        }
    }

    private void applySceneToStage(Stage stage, Parent root, String title) {
        boolean wasMaximized = stage.isMaximized();
        double sceneWidth = stage.getWidth() > 0 ? stage.getWidth() : DEFAULT_SCENE_WIDTH;
        double sceneHeight = stage.getHeight() > 0 ? stage.getHeight() : DEFAULT_SCENE_HEIGHT;

        stage.setScene(new Scene(root, sceneWidth, sceneHeight));
        stage.setTitle(title);
        stage.setResizable(true);
        stage.setWidth(sceneWidth);
        stage.setHeight(sceneHeight);
        stage.setMaximized(wasMaximized);
        stage.centerOnScreen();
        stage.show();
    }

    private Stage resolveCurrentStage() {
        if (ressourceTable != null && ressourceTable.getScene() != null) {
            return (Stage) ressourceTable.getScene().getWindow();
        }
        if (tfNomRessource != null && tfNomRessource.getScene() != null) {
            return (Stage) tfNomRessource.getScene().getWindow();
        }
        if (cbEvenement != null && cbEvenement.getScene() != null) {
            return (Stage) cbEvenement.getScene().getWindow();
        }
        return null;
    }

    private void configureEventComboBox() {
        if (cbEvenement == null) return;

        cbEvenement.setItems(evenements);
        cbEvenement.setConverter(new StringConverter<>() {
            @Override
            public String toString(Evenement event) {
                return event == null ? "" : text(event.getTitre_event());
            }

            @Override
            public Evenement fromString(String string) {
                return null;
            }
        });
        cbEvenement.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Evenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : text(item.getTitre_event()));
            }
        });
        cbEvenement.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Evenement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : text(item.getTitre_event()));
            }
        });
    }

    private Evenement findEventById(int eventId) {
        return evenements.stream()
                .filter(event -> event.getId() == eventId)
                .findFirst()
                .orElse(null);
    }

    private int parseInt(TextField field) throws Exception {
        String value = trimValue(field.getText());
        if (value.isBlank()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new Exception("Valeur numerique invalide : " + value);
        }
    }

    private double parseDouble(TextField field) throws Exception {
        String value = trimValue(field.getText());
        if (value.isBlank()) return 0;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new Exception("Cout estime invalide : " + value);
        }
    }

    private String getEventTitle(Ressource ressource) {
        if (ressource == null || ressource.getEvenement() == null) return "-";
        return eventTitlesById.getOrDefault(ressource.getEvenement().getId(), "Evenement #" + ressource.getEvenement().getId());
    }

    private String shortText(String value, int max) {
        if (value == null || value.length() <= max) return text(value);
        return value.substring(0, max) + "...";
    }

    private String trimValue(String value) {
        return value == null ? "" : value.trim();
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        new Alert(type, content, ButtonType.OK).showAndWait();
    }
}
