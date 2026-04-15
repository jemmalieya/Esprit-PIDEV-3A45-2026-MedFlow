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
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EvenementService;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class EvenementController {
    private static final double DEFAULT_SCENE_WIDTH = 1650;
    private static final double DEFAULT_SCENE_HEIGHT = 960;

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
    private javafx.scene.layout.FlowPane cardsContainer;


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
        initCardsBackIfExists();
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
                    ouvrirPageCardsBack();
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
        evenementAModifier = null;
        loadScene("/EvenementDashboard.fxml", "Gestion des Événements");
    }
    private void loadScene(String fxml, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));

            Stage stage = resolveCurrentStage();

            if (stage == null) {
                throw new Exception("Impossible de récupérer la fenêtre actuelle.");
            }

            applySceneToStage(stage, root, title);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation échouée : " + e.getMessage());
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

            Stage stage = resolveCurrentStage();
            if (stage == null) {
                throw new Exception("Impossible de rÃ©cupÃ©rer la fenÃªtre actuelle.");
            }
            stage.setTitle("Modifier événement");
            applySceneToStage(stage, root, "Modifier Ã©vÃ©nement");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page de modification : " + e.getMessage());
        }
    }

    private void chargerEvenementAModifier() {
        if (evenementAModifier == null) return;

        if (tfTitreEvent == null) return;

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

    private void initCardsBackIfExists() {
        if (cardsContainer == null) return;

        try {
            List<Evenement> events = evenementService.recuperer();
            cardsContainer.getChildren().clear();

            for (Evenement ev : events) {
                cardsContainer.getChildren().add(createEventCardBack(ev));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }
    private VBox createEventCardBack(Evenement ev) {
        VBox card = new VBox(14);
        card.setPrefWidth(320);
        card.setMinWidth(320);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: #dfe6ee;" +
                        "-fx-border-radius: 22;" +
                        "-fx-padding: 0;"
        );

        DropShadow baseShadow = new DropShadow();
        baseShadow.setRadius(12);
        baseShadow.setSpread(0.08);
        baseShadow.setOffsetY(3);
        baseShadow.setColor(Color.rgb(17, 34, 68, 0.10));
        card.setEffect(baseShadow);

        card.setOnMouseEntered(e -> {
            DropShadow hoverShadow = new DropShadow();
            hoverShadow.setRadius(26);
            hoverShadow.setSpread(0.12);
            hoverShadow.setOffsetY(10);
            hoverShadow.setColor(Color.rgb(18, 152, 183, 0.28));
            card.setEffect(hoverShadow);
            card.setTranslateY(-6);
            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 22;" +
                            "-fx-border-color: #9fdceb;" +
                            "-fx-border-radius: 22;" +
                            "-fx-padding: 0;"
            );
        });

        card.setOnMouseExited(e -> {
            card.setEffect(baseShadow);
            card.setTranslateY(0);
            card.setStyle(
                    "-fx-background-color: white;" +
                            "-fx-background-radius: 22;" +
                            "-fx-border-color: #dfe6ee;" +
                            "-fx-border-radius: 22;" +
                            "-fx-padding: 0;"
            );
        });

        StackPane imageHeader = buildEventImageHeader(ev);
        VBox.setVgrow(imageHeader, Priority.NEVER);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 0 18 18 18;");

        Label titre = new Label(valueOrDash(ev.getTitre_event()));
        titre.setWrapText(true);
        titre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0b2c6f;");

        Label type = new Label("🏷 Type : " + safe(ev.getType_event()));
        Label ville = new Label("📍 Ville : " + safe(ev.getVille_event()));
        Label statut = new Label("✔ Statut : " + safe(ev.getStatut_event()));
        Label organisateur = new Label("👤 Organisateur : " + safe(ev.getNom_organisateur_event()));

        Label desc = new Label(shortText(valueOrDash(ev.getDescription_event()), 110));
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill: #5f6f86; -fx-font-size: 13px;");

        for (Label meta : List.of(type, ville, statut, organisateur)) {
            meta.setWrapText(true);
            meta.setStyle("-fx-text-fill: #4c5f7a; -fx-font-size: 13px; -fx-font-weight: 600;");
        }

        Button detailsBtn = new Button("Détails");
        detailsBtn.setStyle(
                "-fx-background-color: #11a8c9;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 8 16;" +
                        "-fx-font-weight: bold;"
        );
        detailsBtn.setText("Details");
        detailsBtn.setMaxWidth(Double.MAX_VALUE);
        detailsBtn.setStyle(
                "-fx-background-color: #11a8c9;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 10 16;" +
                        "-fx-font-weight: bold;"
        );
        detailsBtn.setOnAction(e -> afficherPopupDetails(ev));

        content.getChildren().addAll(titre, type, ville, statut, organisateur, desc, detailsBtn);
        card.getChildren().addAll(imageHeader, content);

        return card;
    }

    private StackPane buildEventImageHeader(Evenement ev) {
        StackPane imageHeader = new StackPane();
        imageHeader.setPrefHeight(180);
        imageHeader.setMinHeight(180);
        imageHeader.setStyle(
                "-fx-background-color: linear-gradient(to right, #dff7fb, #dce8ff);" +
                        "-fx-background-radius: 22 22 0 0;"
        );

        Image image = loadEventImage(ev.getImage_couverture_event());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(320);
            imageView.setFitHeight(180);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imageHeader.getChildren().add(imageView);
        } else {
            Label placeholder = new Label("Aucune image");
            placeholder.setStyle(
                    "-fx-text-fill: #2e6f8d;" +
                            "-fx-font-size: 18px;" +
                            "-fx-font-weight: bold;"
            );
            imageHeader.getChildren().add(placeholder);
        }

        Label badge = new Label(valueOrDash(ev.getVisibilite_event()));
        badge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.92);" +
                        "-fx-background-radius: 999;" +
                        "-fx-padding: 6 12;" +
                        "-fx-text-fill: #0b2c6f;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;"
        );
        StackPane.setMargin(badge, new javafx.geometry.Insets(14, 14, 0, 0));
        StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_RIGHT);
        imageHeader.getChildren().add(badge);

        return imageHeader;
    }

    private Image loadEventImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        try {
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://") || imagePath.startsWith("file:/")) {
                return new Image(imagePath, true);
            }

            File file = new File(imagePath);
            if (file.exists()) {
                return new Image(file.toURI().toString(), true);
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private String valueOrDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private void afficherPopupDetails(Evenement ev) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'événement");
        alert.setHeaderText(safe(ev.getTitre_event()));

        String details =
                "Type : " + safe(ev.getType_event()) + "\n" +
                        "Ville : " + safe(ev.getVille_event()) + "\n" +
                        "Statut : " + safe(ev.getStatut_event()) + "\n" +
                        "Organisateur : " + safe(ev.getNom_organisateur_event()) + "\n" +
                        "Description : " + safe(ev.getDescription_event());

        alert.setContentText(details);
        alert.showAndWait();
    }
    private String shortText(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max) + "...";
    }
    private void ouvrirPageCardsBack() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/EvenementCardsBack.fxml"));

            Stage stage = resolveCurrentStage();
            if (stage == null) {
                throw new Exception("Impossible de rÃ©cupÃ©rer la fenÃªtre actuelle.");
            }
            stage.setTitle("Événements - Vue Cards");
            applySceneToStage(stage, root, "Ã‰vÃ©nements - Vue Cards");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la vue cards : " + e.getMessage());
        }
    }

    private Stage resolveCurrentStage() {
        if (evenementTable != null && evenementTable.getScene() != null) {
            return (Stage) evenementTable.getScene().getWindow();
        }
        if (tfTitreEvent != null && tfTitreEvent.getScene() != null) {
            return (Stage) tfTitreEvent.getScene().getWindow();
        }
        if (cardsContainer != null && cardsContainer.getScene() != null) {
            return (Stage) cardsContainer.getScene().getWindow();
        }
        return null;
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



    /* ===================== UTIL ===================== */
    private String safe(String v) { return v == null ? "" : v.toLowerCase(Locale.ROOT); }

    private void showAlert(Alert.AlertType t, String title, String c) {
        new Alert(t, c, ButtonType.OK).showAndWait();
    }
}
