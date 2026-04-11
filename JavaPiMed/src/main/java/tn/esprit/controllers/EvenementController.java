package tn.esprit.controllers;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EvenementService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class EvenementController {

    @FXML private Label totalEventsLabel;
    @FXML private Label publiesLabel;
    @FXML private Label brouillonsLabel;
    @FXML private Label archivesLabel;
    @FXML private Label inventoryCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;

    @FXML private TableView<Evenement> evenementTable;
    @FXML private TableColumn<Evenement, String> titreCol;
    @FXML private TableColumn<Evenement, String> typeCol;
    @FXML private TableColumn<Evenement, String> villeCol;
    @FXML private TableColumn<Evenement, String> statutCol;
    @FXML private TableColumn<Evenement, String> organisateurCol;
    @FXML private TableColumn<Evenement, String> descriptionCol;
    @FXML private TableColumn<Evenement, Void> actionsCol;

    @FXML private VBox submenuVBox;
    @FXML private Button voirTousBtn;

    private final EvenementService evenementService = new EvenementService();
    private final ObservableList<Evenement> masterList = FXCollections.observableArrayList();
    private FilteredList<Evenement> filteredList;




    @FXML
    private Label evenementArrow;

    @FXML
    public void initialize() {
        configureTable();
        configureSort();
        loadData();
        configureSearch();
        updateStats();
    }

    private void configureTable() {
        titreCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("titre_event"));
        typeCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("type_event"));
        villeCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("ville_event"));
        statutCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("statut_event"));
        organisateurCol.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nom_organisateur_event"));

        descriptionCol.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescription_event();
            if (desc == null) desc = "";
            if (desc.length() > 45) {
                desc = desc.substring(0, 45) + "...";
            }
            return new SimpleStringProperty(desc);
        });

        addActionsColumn();
        evenementTable.setPlaceholder(new Label("Aucun événement trouvé"));
    }

    private void configureSort() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Trier...",
                "Titre A-Z",
                "Titre Z-A",
                "Ville A-Z",
                "Statut"
        ));
        sortCombo.setValue("Trier...");
        sortCombo.setOnAction(e -> applySort());
    }

    private void loadData() {
        List<Evenement> events = evenementService.recuperer();
        masterList.setAll(events);

        filteredList = new FilteredList<>(masterList, p -> true);
        evenementTable.setItems(filteredList);

        inventoryCountLabel.setText(masterList.size() + " événement(s) dans votre espace");
    }

    private void configureSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String keyword = newVal == null ? "" : newVal.trim().toLowerCase(Locale.ROOT);

            filteredList.setPredicate(ev -> {
                if (keyword.isEmpty()) return true;

                return contains(ev.getTitre_event(), keyword)
                        || contains(ev.getType_event(), keyword)
                        || contains(ev.getVille_event(), keyword)
                        || contains(ev.getStatut_event(), keyword)
                        || contains(ev.getNom_organisateur_event(), keyword)
                        || contains(ev.getDescription_event(), keyword);
            });

            updateStats();
        });
    }

    private void applySort() {
        String selected = sortCombo.getValue();
        if (selected == null) return;

        switch (selected) {
            case "Titre A-Z" ->
                    FXCollections.sort(masterList, (a, b) -> safe(a.getTitre_event()).compareToIgnoreCase(safe(b.getTitre_event())));
            case "Titre Z-A" ->
                    FXCollections.sort(masterList, (a, b) -> safe(b.getTitre_event()).compareToIgnoreCase(safe(a.getTitre_event())));
            case "Ville A-Z" ->
                    FXCollections.sort(masterList, (a, b) -> safe(a.getVille_event()).compareToIgnoreCase(safe(b.getVille_event())));
            case "Statut" ->
                    FXCollections.sort(masterList, (a, b) -> safe(a.getStatut_event()).compareToIgnoreCase(safe(b.getStatut_event())));
            default -> {
            }
        }
    }

    private void updateStats() {
        int total = filteredList.size();
        int publies = 0;
        int brouillons = 0;
        int archives = 0;

        for (Evenement e : filteredList) {
            String statut = safe(e.getStatut_event()).toLowerCase(Locale.ROOT);

            if (statut.contains("publi")) {
                publies++;
            } else if (statut.contains("brouillon")) {
                brouillons++;
            } else {
                archives++;
            }
        }

        totalEventsLabel.setText(String.valueOf(total));
        publiesLabel.setText(String.valueOf(publies));
        brouillonsLabel.setText(String.valueOf(brouillons));
        archivesLabel.setText(String.valueOf(archives));
        inventoryCountLabel.setText(total + " événement(s) dans votre espace");
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
                    Evenement ev = getTableView().getItems().get(getIndex());
                    showAlert(Alert.AlertType.INFORMATION, "Détail", buildDetail(ev));
                });

                editBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());
                    showAlert(Alert.AlertType.INFORMATION, "Modification",
                            "Tu peux relier ce bouton plus tard vers une page modifier.\n\nÉvénement : " + safe(ev.getTitre_event()));
                });

                deleteBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Supprimer l'événement : " + safe(ev.getTitre_event()) + " ?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        evenementService.supprimer(ev);
                        masterList.remove(ev);
                        updateStats();
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

    @FXML
    private void onNewEvent() {
        showAlert(Alert.AlertType.INFORMATION, "Nouveau Événement",
                "Ici tu peux ouvrir la page d'ajout d'événement.");
    }

    @FXML
    private void onDashboard() {
        showAlert(Alert.AlertType.INFORMATION, "Dashboard", "Bouton Dashboard cliqué.");
    }

    @FXML
    private void onGestionEvents() {
        showAlert(Alert.AlertType.INFORMATION, "Événements", "Tu es déjà sur la page événements.");
    }

    @FXML
    private void onLogout() {
        showAlert(Alert.AlertType.INFORMATION, "Déconnexion", "Bouton Déconnexion cliqué.");
    }

    @FXML
    private void toggleSubmenu(javafx.event.ActionEvent event) {
        boolean show = !submenuVBox.isVisible();
        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);
        // Mettre en avant le bouton "Voir tous les événements" si le sous-menu est ouvert
        if (show) {
            voirTousBtn.getStyleClass().add("submenu-link-selected");
        } else {
            voirTousBtn.getStyleClass().remove("submenu-link-selected");
        }
    }

    @FXML
    private void onVoirTousEvenements(javafx.event.ActionEvent event) {
        // Mettre en avant le bouton quand il est cliqué
        voirTousBtn.getStyleClass().add("submenu-link-selected");
        // Logique pour afficher tous les événements
        // À implémenter selon le besoin
    }

    private boolean contains(String value, String keyword) {
        return safe(value).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String buildDetail(Evenement e) {
        return "Titre : " + safe(e.getTitre_event()) + "\n"
                + "Type : " + safe(e.getType_event()) + "\n"
                + "Ville : " + safe(e.getVille_event()) + "\n"
                + "Statut : " + safe(e.getStatut_event()) + "\n"
                + "Organisateur : " + safe(e.getNom_organisateur_event()) + "\n"
                + "Description : " + safe(e.getDescription_event());
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Méthodes ajoutées pour le FXML
    @FXML
    private void onTableauBordAdmin(javafx.event.ActionEvent event) {
        // Logique pour ouvrir le tableau de bord admin
        // À implémenter selon le besoin
    }

    @FXML
    private void onAutresActions(javafx.event.ActionEvent event) {
        // Logique pour autres actions
        // À implémenter selon le besoin
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
}