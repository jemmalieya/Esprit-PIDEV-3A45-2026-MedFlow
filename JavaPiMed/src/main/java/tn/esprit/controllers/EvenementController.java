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
import tn.esprit.entities.Evenement;
import tn.esprit.services.EvenementService;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class EvenementController {

    /* ===================== DASHBOARD FIELDS ===================== */
    @FXML private Label totalEventsLabel;
    @FXML private Label publiesLabel;
    @FXML private Label brouillonsLabel;
    @FXML private Label archivesLabel;
    @FXML private Label inventoryCountLabel;
    @FXML private Label evenementArrow;

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

    /* ===================== AJOUT EVENEMENT FIELDS ===================== */
    @FXML private TextField tfTitreEvent;
    @FXML private TextField tfSlugEvent;
    @FXML private TextArea taDescriptionEvent;
    @FXML private TextArea taObjectifEvent;
    @FXML private ComboBox<String> cbTypeEvent;
    @FXML private ComboBox<String> cbStatutEvent;


    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;

    @FXML private TextField tfNomLieuEvent;
    @FXML private TextField tfAdresseEvent;
    @FXML private TextField tfVilleEvent;
    @FXML private TextField tfMaxParticipants;

    @FXML private DatePicker dpDateLimiteInscription;
    @FXML private CheckBox cbInscriptionObligatoire;
    @FXML private ComboBox<String> cbVisibiliteEvent;

    @FXML private TextField tfEmailContact;
    @FXML private TextField tfTelContact;
    @FXML private TextField tfOrganisateurEvent;
    @FXML private TextField tfImageEvent;

    private static Evenement evenementAModifier;
    @FXML
    private void toggleSubmenu() {
        if (submenuVBox == null || evenementArrow == null) return;

        boolean show = !submenuVBox.isVisible();

        submenuVBox.setVisible(show);
        submenuVBox.setManaged(show);
        evenementArrow.setText(show ? "⌃" : "⌄");
    }

    @FXML
    private void onVoirTousEvenements() {
        // TODO: implement later
    }

    @FXML
    private void onTableauBordAdmin() {
        // TODO: implement later
    }

    @FXML
    private void onAutresActions() {
        // TODO: implement later
    }


    /* ===================== DATA ===================== */
    private final EvenementService evenementService = new EvenementService();
    private final ObservableList<Evenement> masterList = FXCollections.observableArrayList();
    private FilteredList<Evenement> filteredList;

    /* ===================== INITIALIZE ===================== */
    @FXML
    public void initialize() {
        initDashboardIfExists();
        initAjoutEvenementIfExists();
        chargerEvenementAModifier();
    }

    /* ===================== DASHBOARD INIT ===================== */
    private void initDashboardIfExists() {
        if (evenementTable == null) return;

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

        descriptionCol.setCellValueFactory(cd -> {
            String d = cd.getValue().getDescription_event();
            if (d == null) d = "";
            if (d.length() > 45) d = d.substring(0, 45) + "...";
            return new SimpleStringProperty(d);
        });

        addActionsColumn();
        evenementTable.setPlaceholder(new Label("Aucun événement trouvé"));
    }

    private void configureSort() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Titre A-Z", "Titre Z-A", "Ville A-Z", "Statut"
        ));
        sortCombo.setOnAction(e -> applySort());
    }

    private void loadData() {
        try {
            masterList.setAll(evenementService.recuperer());
            filteredList = new FilteredList<>(masterList, p -> true);
            evenementTable.setItems(filteredList);
            inventoryCountLabel.setText(masterList.size() + " événement(s)");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void configureSearch() {
        searchField.textProperty().addListener((o, a, b) -> {
            String k = b.toLowerCase(Locale.ROOT);
            filteredList.setPredicate(ev ->
                    k.isEmpty()
                            || safe(ev.getTitre_event()).contains(k)
                            || safe(ev.getVille_event()).contains(k)
                            || safe(ev.getType_event()).contains(k)
            );
            updateStats();
        });
    }

    private void applySort() {
        FXCollections.sort(masterList,
                (a, b) -> safe(a.getTitre_event()).compareToIgnoreCase(safe(b.getTitre_event())));
    }

    private void updateStats() {
        totalEventsLabel.setText(String.valueOf(filteredList.size()));
    }

    private void addActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {

            private final Button viewBtn = new Button("Voir");
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");

            private final ToolBar toolBar = new ToolBar(viewBtn, editBtn, deleteBtn);

            {
                // ✅ Apply CSS styling (already exists in your CSS)
                toolBar.getStyleClass().add("action-toolbar");
                viewBtn.getStyleClass().add("view-btn");
                editBtn.getStyleClass().add("edit-btn");
                deleteBtn.getStyleClass().add("delete-btn");

                // ✅ VOIR
                viewBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());
                    showAlert(
                            Alert.AlertType.INFORMATION,
                            "Détails de l'événement",
                            buildDetail(ev)
                    );
                });

                // ✅ MODIFIER (placeholder for now)
                // ✅ MODIFIER
                editBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());
                    ouvrirPageModification(ev);
                });

                // ✅ SUPPRIMER
                deleteBtn.setOnAction(e -> {
                    Evenement ev = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmation");
                    confirm.setHeaderText(null);
                    confirm.setContentText(
                            "Supprimer l'événement : "
                                    + safe(ev.getTitre_event())
                                    + " ?"
                    );

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

        actionsCol.setCellValueFactory(
                param -> Bindings.createObjectBinding(() -> null)
        );
    }


    private String buildDetail(Evenement e) {
        return "Titre : " + safe(e.getTitre_event()) + "\n"
                + "Type : " + safe(e.getType_event()) + "\n"
                + "Ville : " + safe(e.getVille_event()) + "\n"
                + "Statut : " + safe(e.getStatut_event()) + "\n"
                + "Organisateur : " + safe(e.getNom_organisateur_event()) + "\n"
                + "Description : " + safe(e.getDescription_event());
    }


    /* ===================== AJOUT INIT ===================== */
    private void initAjoutEvenementIfExists() {
        if (cbTypeEvent != null && cbTypeEvent.getItems().isEmpty()) {
            cbTypeEvent.getItems().addAll("Campagne", "Conférence", "Atelier", "Caritatif", "Autre");
        }
        if (cbStatutEvent != null && cbStatutEvent.getItems().isEmpty()) {
            cbStatutEvent.getItems().addAll("Brouillon", "Publié", "Annulé");
            cbStatutEvent.setValue("Brouillon");
        }
        if (cbVisibiliteEvent != null && cbVisibiliteEvent.getItems().isEmpty()) {
            cbVisibiliteEvent.getItems().addAll("Public", "Privé");
        }
    }

    /* ===================== NAVIGATION ===================== */
    @FXML
    private void onNewEvent() {
        loadScene("/AjouterEvenement.fxml", "Nouvel événement");
    }

    @FXML
    private void retourListeEvenements() {
        loadScene("/EvenementDashboard.fxml", "Gestion des Événements");
    }

    private void loadScene(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage)
                    (evenementTable != null ? evenementTable : tfTitreEvent).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ===================== AJOUT EVENEMENT ===================== */
    @FXML
    private void ajouterEvenement() {
        try {
            Evenement e = new Evenement();
            e.setTitre_event(tfTitreEvent.getText());
            e.setSlug_event(tfSlugEvent.getText());
            e.setType_event(cbTypeEvent.getValue());
            e.setStatut_event(cbStatutEvent.getValue());
            e.setDescription_event(taDescriptionEvent.getText());
            e.setObjectif_event(taObjectifEvent.getText());
            e.setDate_debut_event(java.sql.Date.valueOf(dpDateDebut.getValue()));
            e.setDate_fin_event(java.sql.Date.valueOf(dpDateFin.getValue()));
            e.setNom_lieu_event(tfNomLieuEvent.getText());
            e.setAdresse_event(tfAdresseEvent.getText());
            e.setVille_event(tfVilleEvent.getText());
            e.setNb_participants_max_event(Integer.parseInt(tfMaxParticipants.getText()));
            e.setInscription_obligatoire_event(cbInscriptionObligatoire.isSelected());
            if (dpDateLimiteInscription.getValue() != null)
                e.setDate_limite_inscription_event(java.sql.Date.valueOf(dpDateLimiteInscription.getValue()));
            e.setEmail_contact_event(tfEmailContact.getText());
            e.setTel_contact_event(tfTelContact.getText());
            e.setNom_organisateur_event(tfOrganisateurEvent.getText());
            e.setImage_couverture_event(tfImageEvent.getText());
            e.setVisibilite_event(cbVisibiliteEvent.getValue());
            e.setDate_creation_event(new java.util.Date());
            e.setDate_mise_a_jour_event(new java.util.Date());

            evenementService.ajouter(e);
            retourListeEvenements();

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }
    private void ouvrirPageModification(Evenement ev) {
        try {
            evenementAModifier = ev;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierEvenement.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) evenementTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifier événement");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page de modification : " + e.getMessage());
        }
    }

    private void chargerEvenementAModifier() {
        if (evenementAModifier == null) return;

        tfTitreEvent.setText(evenementAModifier.getTitre_event());
        tfSlugEvent.setText(evenementAModifier.getSlug_event());
        taDescriptionEvent.setText(evenementAModifier.getDescription_event());
        taObjectifEvent.setText(evenementAModifier.getObjectif_event());

        cbTypeEvent.setValue(evenementAModifier.getType_event());
        cbStatutEvent.setValue(evenementAModifier.getStatut_event());

        if (evenementAModifier.getDate_debut_event() != null) {
            dpDateDebut.setValue(new java.sql.Date(
                    evenementAModifier.getDate_debut_event().getTime()
            ).toLocalDate());
        }

        if (evenementAModifier.getDate_fin_event() != null) {
            dpDateFin.setValue(new java.sql.Date(
                    evenementAModifier.getDate_fin_event().getTime()
            ).toLocalDate());
        }

        tfNomLieuEvent.setText(evenementAModifier.getNom_lieu_event());
        tfAdresseEvent.setText(evenementAModifier.getAdresse_event());
        tfVilleEvent.setText(evenementAModifier.getVille_event());
        tfMaxParticipants.setText(String.valueOf(evenementAModifier.getNb_participants_max_event()));

        if (evenementAModifier.getDate_limite_inscription_event() != null) {
            dpDateLimiteInscription.setValue(new java.sql.Date(
                    evenementAModifier.getDate_limite_inscription_event().getTime()
            ).toLocalDate());
        }

        cbInscriptionObligatoire.setSelected(evenementAModifier.isInscription_obligatoire_event());
        cbVisibiliteEvent.setValue(evenementAModifier.getVisibilite_event());

        tfEmailContact.setText(evenementAModifier.getEmail_contact_event());
        tfTelContact.setText(evenementAModifier.getTel_contact_event());
        tfOrganisateurEvent.setText(evenementAModifier.getNom_organisateur_event());
        tfImageEvent.setText(evenementAModifier.getImage_couverture_event());
    }



    @FXML
    private void modifierEvenement() {
        try {
            if (evenementAModifier == null) {
                throw new Exception("Aucun événement sélectionné.");
            }

            if (tfTitreEvent.getText() == null || tfTitreEvent.getText().trim().isEmpty()) {
                throw new Exception("Le titre est obligatoire.");
            }

            if (tfSlugEvent.getText() == null || tfSlugEvent.getText().trim().isEmpty()) {
                throw new Exception("Le slug est obligatoire.");
            }

            if (cbTypeEvent.getValue() == null) {
                throw new Exception("Le type est obligatoire.");
            }

            if (cbStatutEvent.getValue() == null) {
                throw new Exception("Le statut est obligatoire.");
            }

            if (dpDateDebut.getValue() == null) {
                throw new Exception("La date de début est obligatoire.");
            }

            if (dpDateFin.getValue() == null) {
                throw new Exception("La date de fin est obligatoire.");
            }

            if (tfMaxParticipants.getText() == null || tfMaxParticipants.getText().trim().isEmpty()) {
                throw new Exception("Le nombre maximum de participants est obligatoire.");
            }

            evenementAModifier.setTitre_event(tfTitreEvent.getText().trim());
            evenementAModifier.setSlug_event(tfSlugEvent.getText().trim());
            evenementAModifier.setType_event(cbTypeEvent.getValue());
            evenementAModifier.setStatut_event(cbStatutEvent.getValue());
            evenementAModifier.setDescription_event(taDescriptionEvent.getText());
            evenementAModifier.setObjectif_event(taObjectifEvent.getText());
            evenementAModifier.setDate_debut_event(java.sql.Date.valueOf(dpDateDebut.getValue()));
            evenementAModifier.setDate_fin_event(java.sql.Date.valueOf(dpDateFin.getValue()));
            evenementAModifier.setNom_lieu_event(tfNomLieuEvent.getText());
            evenementAModifier.setAdresse_event(tfAdresseEvent.getText());
            evenementAModifier.setVille_event(tfVilleEvent.getText());
            evenementAModifier.setNb_participants_max_event(Integer.parseInt(tfMaxParticipants.getText().trim()));
            evenementAModifier.setInscription_obligatoire_event(cbInscriptionObligatoire.isSelected());

            if (dpDateLimiteInscription.getValue() != null) {
                evenementAModifier.setDate_limite_inscription_event(
                        java.sql.Date.valueOf(dpDateLimiteInscription.getValue())
                );
            } else {
                evenementAModifier.setDate_limite_inscription_event(null);
            }

            evenementAModifier.setEmail_contact_event(tfEmailContact.getText());
            evenementAModifier.setTel_contact_event(tfTelContact.getText());
            evenementAModifier.setNom_organisateur_event(tfOrganisateurEvent.getText());
            evenementAModifier.setImage_couverture_event(tfImageEvent.getText());
            evenementAModifier.setVisibilite_event(cbVisibiliteEvent.getValue());
            evenementAModifier.setDate_mise_a_jour_event(new java.util.Date());

            evenementService.modifier(evenementAModifier);

            showAlert(Alert.AlertType.INFORMATION, "Succès", "Événement modifié avec succès.");

            evenementAModifier = null;
            retourListeEvenements();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le nombre maximum de participants doit être un nombre.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur modification", e.getMessage());
        }
    }

    /* ===================== UTIL ===================== */
    private String safe(String v) { return v == null ? "" : v.toLowerCase(Locale.ROOT); }

    private void showAlert(Alert.AlertType t, String title, String c) {
        new Alert(t, c, ButtonType.OK).showAndWait();
    }
}
