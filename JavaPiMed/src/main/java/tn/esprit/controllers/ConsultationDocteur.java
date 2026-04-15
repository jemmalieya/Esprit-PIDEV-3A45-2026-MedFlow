package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleStringProperty;
import javafx.util.converter.DefaultStringConverter;
import tn.esprit.entities.FicheMedicale;
import tn.esprit.entities.Prescription;
import tn.esprit.entities.RendezVous;
import tn.esprit.services.FicheMedicaleService;
import tn.esprit.services.PrescriptionService;
import tn.esprit.services.RendezVousService;
import tn.esprit.tools.MyDataBase;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ConsultationDocteur {

    private static final int SESSION_DOCTOR_ID = 18;
    private final RendezVousService rendezVousService = new RendezVousService();
    private final FicheMedicaleService ficheMedicaleService = new FicheMedicaleService();
    private final PrescriptionService prescriptionService = new PrescriptionService();

    // FXML fields mapped to UI components
    @FXML
    private TextField doctorField;

    @FXML
    private TextField doctorField1;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private TextField searchField;

    @FXML
    private Label selectedModeLabel;

    @FXML
    private Button bookingPageBtn;

    @FXML
    private Button pageOneBtn;

    @FXML
    private Button consultationPageBtn;

    @FXML
    private VBox pageOneContainer;

    @FXML
    private VBox consultationPageContainer;

    @FXML
    private TableView<RendezVous> eventTable;

    @FXML
    private TextField diagnosticField;

    @FXML
    private TextArea observationsField;

    @FXML
    private TextArea resultatsExamensField;

    @FXML
    private VBox prescriptionRowsContainer;

    @FXML
    private Label selectedRendezVousIdLabel;
    @FXML
    private Label consultationStartTimeLabel;
    @FXML
    private Label consultationEndTimeLabel;
    @FXML
    private Button saveFicheButton;
    @FXML
    private Label consultationDurationLabel;
    @FXML
    private Label consultationTimerLabel;

    // Detail panel fields
    @FXML
    private Label detailDateTimeLabel;
    @FXML
    private TextField detailDateTimeEditField;
    @FXML
    private Label detailModeLabel;
    @FXML
    private TextField detailModeEditField;
    @FXML
    private Label detailMotifLabel;
    @FXML
    private TextField detailMotifEditField;
    @FXML
    private Label detailPatientLabel;
    @FXML
    private TextField detailPatientEditField;
    @FXML
    private Label detailStatutLabel;
    @FXML
    private TextField detailStatutEditField;
    @FXML
    private Label detailUrgenceLabel;
    @FXML
    private TextField detailUrgenceEditField;
    @FXML
    private VBox detailFicheMedicaleBox;
    @FXML
    private Label detailFicheMedicaleMessage;
    @FXML
    private Label detailDiagnosticTitleLabel;
    @FXML
    private Label detailDiagnosticLabel;
    @FXML
    private TextArea detailDiagnosticEditArea;
    @FXML
    private HBox detailDiagnosticBox;
    @FXML
    private Label detailObservationsTitleLabel;
    @FXML
    private Label detailObservationsLabel;
    @FXML
    private TextArea detailObservationsEditArea;
    @FXML
    private HBox detailObservationsBox;
    @FXML
    private Label detailResultatsTitleLabel;
    @FXML
    private Label detailResultatsLabel;
    @FXML
    private TextArea detailResultatsEditArea;
    @FXML
    private HBox detailResultatsBox;
    @FXML
    private Label detailDureeTitleLabel;
    @FXML
    private Label detailDureeLabel;
    @FXML
    private TextField detailDureeEditField;
    @FXML
    private HBox detailDureeBox;
    @FXML
    private Button ficheModifyButton;
    @FXML
    private Button ficheCancelButton;
    @FXML
    private Button ficheConfirmButton;
    @FXML
    private Button ficheDeleteButton;
    @FXML
    private TableView<Prescription> detailPrescriptionsTable;
    @FXML
    private TableColumn<Prescription, String> detailPrescriptionNameColumn;
    @FXML
    private TableColumn<Prescription, String> detailPrescriptionDoseColumn;
    @FXML
    private TableColumn<Prescription, String> detailPrescriptionFrequenceColumn;
    @FXML
    private TableColumn<Prescription, String> detailPrescriptionInstructionsColumn;
    @FXML
    private Label detailNoPrescriptionsLabel;
    @FXML
    private Button prescriptionModifyButton;
    @FXML
    private Button prescriptionCancelButton;
    @FXML
    private Button prescriptionConfirmButton;
    @FXML
    private Button prescriptionDeleteButton;

    private final ObservableList<RendezVous> doctorRendezVous = FXCollections.observableArrayList();
    private final ObservableList<RendezVous> allDoctorRendezVous = FXCollections.observableArrayList();
    private final ObservableList<Prescription> detailPrescriptions = FXCollections.observableArrayList();
    private final List<PrescriptionRowControls> prescriptionRows = new ArrayList<>();
    private Integer selectedRendezVousId;
    private Timestamp consultationStartTime;
    private Timeline consultationTimer;
    private RendezVous currentDetailRendezVous;
    private FicheMedicale currentDetailFiche;
    private boolean ficheEditMode;
    private boolean prescriptionsEditMode;
    private List<Prescription> prescriptionsSnapshot = new ArrayList<>();

    // Initialize method to set up the combo box options
    @FXML
    public void initialize() {
        if (sortComboBox != null) {
            sortComboBox.getItems().addAll(
                    "Date ASC",
                    "Date DESC",
                    "Statut: Demande",
                    "Statut: Confirmé",
                    "Statut: Terminé"
            );
            sortComboBox.setOnAction(this::handleSortSelection);
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> applySearchAndSort());
        }

        if (eventTable != null) {
            eventTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            configureTableColumns();
            loadDoctorRendezVous();
            
            // Add table selection listener for detail panel
            eventTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    displayDetailPanel(newVal);
                } else {
                    clearDetailPanel();
                }
            });
        }

        configureDetailPrescriptionsTable();
        clearDetailPanel();
        setSaveButtonEnabled(false);

        if (doctorField != null) {
            doctorField.setText("Dr. John Doe");
        }
        if (doctorField1 != null) {
            doctorField1.setText("12345");
        }

        updateConsultationTimingLabels(null, null, null, "00:00:00");

        showPage(true);
    }

    // Handle doctor selection
    @FXML
    private void handleSelectDoctor1(ActionEvent event) {
        if (doctorField != null) {
            doctorField.setText("Dr. Wael - Cardiologist");
        }
        if (doctorField1 != null) {
            doctorField1.setText("101");
        }
    }

    @FXML
    private void handleSelectDoctor2(ActionEvent event) {
        if (doctorField != null) {
            doctorField.setText("Dr. Amira - Dermatologist");
        }
        if (doctorField1 != null) {
            doctorField1.setText("102");
        }
    }

    @FXML
    private void handleSelectDoctor3(ActionEvent event) {
        if (doctorField != null) {
            doctorField.setText("Dr. Sami - Pediatrician");
        }
        if (doctorField1 != null) {
            doctorField1.setText("103");
        }
    }

    // Handle the combo box selection
    @FXML
    private void handleSortSelection(ActionEvent event) {
        applySearchAndSort();
    }

    // Display error alert
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Handle booking page navigation
    @FXML
    private void handleShowBookingPage(ActionEvent event) {
        // Logic to switch to booking page (if needed)
        System.out.println("Navigating to booking page...");
    }

    // Handle other actions like canceling, etc.
    @FXML
    private void handleCancelBooking(ActionEvent event) {
        if (doctorField != null) {
            doctorField.clear();
        }
        if (doctorField1 != null) {
            doctorField1.clear();
        }
        if (sortComboBox != null) {
            sortComboBox.setValue(null);
        }
    }

    @FXML
    private void handleShowPageOne(ActionEvent event) {
        showPage(true);
    }

    @FXML
    private void handleShowConsultationPage(ActionEvent event) {
        showPage(false);
    }

    @FXML
    private void handleAddPrescriptionRow(ActionEvent event) {
        addPrescriptionRow();
    }

    private void addPrescriptionRow() {
        if (prescriptionRowsContainer == null) {
            return;
        }

        TextField nomField = new TextField();
        nomField.setPromptText("Nom médicament");
        nomField.getStyleClass().add("fiche-input");
        nomField.setPrefWidth(170);

        TextField doseField = new TextField();
        doseField.setPromptText("Dose");
        doseField.getStyleClass().add("fiche-input");
        doseField.setPrefWidth(120);

        TextField frequenceField = new TextField();
        frequenceField.setPromptText("Fréquence");
        frequenceField.getStyleClass().add("fiche-input");
        frequenceField.setPrefWidth(135);

        TextField dureeField = new TextField();
        dureeField.setPromptText("Durée (jours)");
        dureeField.getStyleClass().add("fiche-input");
        dureeField.setPrefWidth(130);

        TextField instructionsField = new TextField();
        instructionsField.setPromptText("Instructions");
        instructionsField.getStyleClass().add("fiche-input");
        instructionsField.setPrefWidth(180);

        Button deleteButton = new Button("x");
        deleteButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 6; -fx-min-width: 30; -fx-min-height: 30;");

        HBox row = new HBox(8, nomField, doseField, frequenceField, dureeField, instructionsField, deleteButton);
        row.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8;");

        PrescriptionRowControls controls = new PrescriptionRowControls(row, nomField, doseField, frequenceField, dureeField, instructionsField);

        deleteButton.setOnAction(e -> {
            prescriptionRows.remove(controls);
            prescriptionRowsContainer.getChildren().remove(row);
        });

        prescriptionRows.add(controls);
        prescriptionRowsContainer.getChildren().add(row);
    }

    @SuppressWarnings("unchecked")
    private void configureTableColumns() {
        if (eventTable.getColumns().size() < 8) {
            return;
        }

        TableColumn<RendezVous, String> col0 = (TableColumn<RendezVous, String>) eventTable.getColumns().get(0);
        TableColumn<RendezVous, String> col1 = (TableColumn<RendezVous, String>) eventTable.getColumns().get(1);
        TableColumn<RendezVous, String> col2 = (TableColumn<RendezVous, String>) eventTable.getColumns().get(2);
        TableColumn<RendezVous, String> col3 = (TableColumn<RendezVous, String>) eventTable.getColumns().get(3);
        TableColumn<RendezVous, String> col4 = (TableColumn<RendezVous, String>) eventTable.getColumns().get(4);
        TableColumn<RendezVous, String> col5 = (TableColumn<RendezVous, String>) eventTable.getColumns().get(5);
        TableColumn<RendezVous, String> col6 = (TableColumn<RendezVous, String>) eventTable.getColumns().get(6);
        TableColumn<RendezVous, Void> col7 = (TableColumn<RendezVous, Void>) eventTable.getColumns().get(7);

        col0.setText("ID");
        col1.setText("DATE/TIME");
        col2.setText("STATUT");
        col3.setText("MODE");
        col4.setText("MOTIF");
        col5.setText("PATIENT");
        col6.setText("URGENCE");
        col7.setText("ACTION");

        col0.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getId())));
        col1.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getDatetime())));
        col2.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getStatut())));
        col3.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getMode())));
        col4.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getMotif())));
        col5.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getIdPatient())));
        col6.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getUrgency_level())));
        col7.setCellFactory(column -> new TableCell<>() {
            private final Button confirmButton = new Button("Confirm");
            private final Button startButton = new Button("Start");
            private final HBox actionBox = new HBox(8, confirmButton, startButton);

            {
                confirmButton.setOnAction(event -> {
                    RendezVous rendezVous = getTableView().getItems().get(getIndex());
                    confirmRendezVous(rendezVous);
                });

                startButton.setOnAction(event -> {
                    RendezVous rendezVous = getTableView().getItems().get(getIndex());
                    startConsultation(rendezVous);
                });

                confirmButton.getStyleClass().add("action-confirm-button");
                startButton.getStyleClass().add("action-start-button");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                RendezVous rendezVous = getTableView().getItems().get(getIndex());
                boolean canConfirm = "Demande".equalsIgnoreCase(nullSafe(rendezVous.getStatut()));
                boolean canStart = "Confirmé".equalsIgnoreCase(nullSafe(rendezVous.getStatut()));
                confirmButton.setDisable(!canConfirm);
                startButton.setDisable(!canStart);
                setGraphic(actionBox);
            }
        });

        eventTable.setItems(doctorRendezVous);
    }

    private void configureDetailPrescriptionsTable() {
        if (detailPrescriptionsTable == null) {
            return;
        }

        detailPrescriptionsTable.setItems(detailPrescriptions);
        detailPrescriptionsTable.setEditable(false);
        detailPrescriptionsTable.getSelectionModel().setCellSelectionEnabled(true);

        if (detailPrescriptionNameColumn != null) {
            detailPrescriptionNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getNom_medicament())));
            detailPrescriptionNameColumn.setEditable(true);
            detailPrescriptionNameColumn.setCellFactory(column -> createReliableEditableCell());
            detailPrescriptionNameColumn.setOnEditCommit(event -> {
                Prescription row = event.getRowValue();
                if (row != null) {
                    row.setNom_medicament(event.getNewValue());
                }
            });
        }

        if (detailPrescriptionDoseColumn != null) {
            detailPrescriptionDoseColumn.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getDose())));
            detailPrescriptionDoseColumn.setEditable(true);
            detailPrescriptionDoseColumn.setCellFactory(column -> createReliableEditableCell());
            detailPrescriptionDoseColumn.setOnEditCommit(event -> {
                Prescription row = event.getRowValue();
                if (row != null) {
                    row.setDose(event.getNewValue());
                }
            });
        }

        if (detailPrescriptionFrequenceColumn != null) {
            detailPrescriptionFrequenceColumn.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getFrequence())));
            detailPrescriptionFrequenceColumn.setEditable(true);
            detailPrescriptionFrequenceColumn.setCellFactory(column -> createReliableEditableCell());
            detailPrescriptionFrequenceColumn.setOnEditCommit(event -> {
                Prescription row = event.getRowValue();
                if (row != null) {
                    row.setFrequence(event.getNewValue());
                }
            });
        }

        if (detailPrescriptionInstructionsColumn != null) {
            detailPrescriptionInstructionsColumn.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getInstructions())));
            detailPrescriptionInstructionsColumn.setEditable(true);
            detailPrescriptionInstructionsColumn.setCellFactory(column -> createReliableEditableCell());
            detailPrescriptionInstructionsColumn.setOnEditCommit(event -> {
                Prescription row = event.getRowValue();
                if (row != null) {
                    row.setInstructions(event.getNewValue());
                }
            });
        }
    }

    private TableCell<Prescription, String> createReliableEditableCell() {
        return new TextFieldTableCell<>(new DefaultStringConverter()) {
            private TextField textField;

            @Override
            public void startEdit() {
                if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
                    return;
                }
                super.startEdit();

                if (textField == null) {
                    textField = new TextField(getItem());
                    textField.setOnAction(e -> commitEdit(textField.getText()));
                    textField.focusedProperty().addListener((obs, oldValue, newValue) -> {
                        if (!newValue && isEditing()) {
                            commitEdit(textField.getText());
                        }
                    });
                    textField.setOnKeyPressed(e -> {
                        if (e.getCode() == KeyCode.ESCAPE) {
                            cancelEdit();
                        }
                    });
                }

                textField.setText(getItem());
                setText(null);
                setGraphic(textField);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem());
                setGraphic(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }

            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(item);
                    }
                    setText(null);
                    setGraphic(textField);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                } else {
                    setText(item);
                    setGraphic(null);
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                }
            }
        };
    }

    private void loadDoctorRendezVous() {
        List<RendezVous> list = rendezVousService.recupererParStaffId(SESSION_DOCTOR_ID);
        allDoctorRendezVous.setAll(list);
        applySearchAndSort();
    }

    private void applySearchAndSort() {
        String query = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        List<RendezVous> filtered = new ArrayList<>();
        for (RendezVous rendezVous : allDoctorRendezVous) {
            if (matchesSearch(rendezVous, query)) {
                filtered.add(rendezVous);
            }
        }

        if (sortComboBox != null) {
            String selectedValue = sortComboBox.getValue();
            if ("Date ASC".equals(selectedValue)) {
                filtered.sort(Comparator.comparing(RendezVous::getDatetime));
            } else if ("Date DESC".equals(selectedValue)) {
                filtered.sort(Comparator.comparing(RendezVous::getDatetime).reversed());
            } else if ("Statut: Demande".equals(selectedValue)) {
                filtered.sort(
                        Comparator.comparingInt((RendezVous r) -> "Demande".equalsIgnoreCase(nullSafe(r.getStatut())) ? 0 : 1)
                                .thenComparing(RendezVous::getDatetime, Comparator.nullsLast(Comparator.reverseOrder()))
                );
            } else if ("Statut: Confirmé".equals(selectedValue)) {
                filtered.sort(
                        Comparator.comparingInt((RendezVous r) -> "Confirmé".equalsIgnoreCase(nullSafe(r.getStatut())) ? 0 : 1)
                                .thenComparing(RendezVous::getDatetime, Comparator.nullsLast(Comparator.reverseOrder()))
                );
            } else if ("Statut: Terminé".equals(selectedValue)) {
                filtered.sort(
                        Comparator.comparingInt((RendezVous r) -> "Terminé".equalsIgnoreCase(nullSafe(r.getStatut())) ? 0 : 1)
                                .thenComparing(RendezVous::getDatetime, Comparator.nullsLast(Comparator.reverseOrder()))
                );
            }
        }

        doctorRendezVous.setAll(filtered);
    }

    private boolean matchesSearch(RendezVous rendezVous, String query) {
        if (rendezVous == null) {
            return false;
        }

        if (query == null || query.isEmpty()) {
            return true;
        }

        String dateTime = rendezVous.getDatetime() == null ? "" : rendezVous.getDatetime().toString().toLowerCase();
        String statut = nullSafe(rendezVous.getStatut()).toLowerCase();
        String mode = nullSafe(rendezVous.getMode()).toLowerCase();
        String motif = nullSafe(rendezVous.getMotif()).toLowerCase();
        String urgence = nullSafe(rendezVous.getUrgency_level()).toLowerCase();
        String id = String.valueOf(rendezVous.getId());
        String patient = String.valueOf(rendezVous.getIdPatient());

        return id.contains(query)
                || dateTime.contains(query)
                || statut.contains(query)
                || mode.contains(query)
                || motif.contains(query)
                || urgence.contains(query)
                || patient.contains(query);
    }

    private String nullSafe(String value) {
        return value == null ? "-" : value;
    }

    private void confirmRendezVous(RendezVous rendezVous) {
        if (rendezVous == null) {
            return;
        }

        rendezVous.setStatut("Confirmé");
        rendezVousService.modifier(rendezVous);
        eventTable.refresh();
    }

    private void startConsultation(RendezVous rendezVous) {
        if (rendezVous == null) {
            return;
        }

        selectedRendezVousId = rendezVous.getId();
        consultationStartTime = Timestamp.valueOf(LocalDateTime.now());
        updateSelectedRendezVousLabel();
        updateConsultationTimingLabels(consultationStartTime, null, null, "00:00:00");
        setSaveButtonEnabled(true);
        startConsultationTimer();
        showPage(false);
    }

    private void showPage(boolean firstPage) {
        if (pageOneContainer != null) {
            pageOneContainer.setVisible(firstPage);
            pageOneContainer.setManaged(firstPage);
        }

        if (consultationPageContainer != null) {
            consultationPageContainer.setVisible(!firstPage);
            consultationPageContainer.setManaged(!firstPage);
        }

        updatePageButtons(firstPage);
    }

    private void updatePageButtons(boolean firstPage) {
        setButtonActive(pageOneBtn, firstPage);
        setButtonActive(consultationPageBtn, !firstPage);
    }

    private void setButtonActive(Button button, boolean active) {
        if (button == null) {
            return;
        }

        button.getStyleClass().remove("page-nav-button-active");
        if (active && !button.getStyleClass().contains("page-nav-button-active")) {
            button.getStyleClass().add("page-nav-button-active");
        }
    }

    @FXML
    private void handleSaveFicheMedicale(ActionEvent event) {
        if (selectedRendezVousId == null) {
            showError("Selection requise", "Cliquez sur Start dans la colonne ACTIONS pour choisir le rendez-vous lie a cette fiche.");
            return;
        }

        if (consultationStartTime == null) {
            showError("Start requis", "Cliquez sur Start avant d'enregistrer la fiche medicale.");
            return;
        }

        if (ficheMedicaleService.getByRendezVousId(selectedRendezVousId) != null) {
            showError("Doublon detecte", "Une fiche medicale existe deja pour ce rendez-vous.");
            return;
        }

        String diagnostic = diagnosticField == null ? "" : diagnosticField.getText().trim();
        String observations = observationsField == null ? "" : observationsField.getText().trim();
        String resultatsExamens = resultatsExamensField == null ? "" : resultatsExamensField.getText().trim();

        if (diagnostic.isEmpty() || observations.isEmpty() || resultatsExamens.isEmpty()) {
            showError("Champs obligatoires", "Diagnostic, observations et resultats examens sont obligatoires.");
            return;
        }

        Timestamp endTime = new Timestamp(System.currentTimeMillis());
        int durationMinutes = calculateDurationMinutes(consultationStartTime, endTime);

        List<Prescription> prescriptionsToSave;
        try {
            prescriptionsToSave = buildPrescriptionRows(endTime);
        } catch (IllegalArgumentException ex) {
            showError("Validation error", ex.getMessage());
            return;
        }

        Connection connection = MyDataBase.getInstance().getCnx();
        boolean previousAutoCommit = true;

        try {
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            FicheMedicale fiche = new FicheMedicale();
            fiche.setRendez_vous_id(selectedRendezVousId);
            fiche.setDiagnostic(diagnostic);
            fiche.setObservations(observations);
            fiche.setResultats_examens(resultatsExamens);
            fiche.setStart_time(consultationStartTime);
            fiche.setEnd_time(endTime);
            fiche.setDuree_minutes(durationMinutes);
            fiche.setCreated_at(endTime);
            fiche.setSignature("Dr-" + SESSION_DOCTOR_ID);

            int ficheMedicaleId = ficheMedicaleService.ajouter(fiche, connection);
            if (ficheMedicaleId <= 0) {
                throw new SQLException("Unable to create fiche medicale.");
            }

            for (Prescription prescription : prescriptionsToSave) {
                prescription.setFiche_medicale_id(ficheMedicaleId);
                prescriptionService.ajouter(prescription, connection);
            }

            RendezVous savedRendezVous = findSelectedRendezVous();
            if (savedRendezVous != null) {
                savedRendezVous.setStatut("Terminé");
                rendezVousService.modifier(savedRendezVous, connection);
            }

            connection.commit();

            updateConsultationTimingLabels(consultationStartTime, endTime, durationMinutes, formatDuration(durationMinutes));
            stopConsultationTimer();
            setSaveButtonEnabled(false);
            showInfo("Succes", "Fiche medicale et prescription(s) ajoutees avec succes.");
            clearFicheForm();
            consultationStartTime = null;
            showPage(true);
            loadDoctorRendezVous();
            eventTable.refresh();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            showError("Erreur", "Impossible de sauvegarder la fiche et la prescription: " + ex.getMessage());
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleClearFicheForm(ActionEvent event) {
        stopConsultationTimer();
        consultationStartTime = null;
        selectedRendezVousId = null;
        updateSelectedRendezVousLabel();
        updateConsultationTimingLabels(null, null, null, "00:00:00");
        setSaveButtonEnabled(false);
        clearFicheForm();
        showPage(true);
    }

    private void clearFicheForm() {
        if (diagnosticField != null) {
            diagnosticField.clear();
        }
        if (observationsField != null) {
            observationsField.clear();
        }
        if (resultatsExamensField != null) {
            resultatsExamensField.clear();
        }
        prescriptionRows.clear();
        if (prescriptionRowsContainer != null) {
            prescriptionRowsContainer.getChildren().clear();
        }
    }

    private List<Prescription> buildPrescriptionRows(Timestamp createdAt) {
        List<Prescription> prescriptions = new ArrayList<>();
        int rowNumber = 0;

        for (PrescriptionRowControls row : prescriptionRows) {
            rowNumber++;
            String nom = text(row.nomField);
            String dose = text(row.doseField);
            String frequence = text(row.frequenceField);
            String dureeRaw = text(row.dureeField);
            String instructions = text(row.instructionsField);

            boolean allEmpty = nom.isEmpty() && dose.isEmpty() && frequence.isEmpty() && dureeRaw.isEmpty() && instructions.isEmpty();
            if (allEmpty) {
                continue;
            }

            boolean complete = !nom.isEmpty() && !dose.isEmpty() && !frequence.isEmpty() && !dureeRaw.isEmpty() && !instructions.isEmpty();
            if (!complete) {
                throw new IllegalArgumentException("La ligne prescription #" + rowNumber + " est incomplete.");
            }

            int duree;
            try {
                duree = Integer.parseInt(dureeRaw);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("La duree de la ligne prescription #" + rowNumber + " doit etre un nombre.");
            }

            if (duree <= 0) {
                throw new IllegalArgumentException("La duree de la ligne prescription #" + rowNumber + " doit etre superieure a 0.");
            }

            Prescription prescription = new Prescription();
            prescription.setNom_medicament(nom);
            prescription.setDose(dose);
            prescription.setFrequence(frequence);
            prescription.setDuree(duree);
            prescription.setInstructions(instructions);
            prescription.setCreated_at(createdAt);
            prescriptions.add(prescription);
        }

        Set<String> uniquenessSet = new HashSet<>();
        for (Prescription prescription : prescriptions) {
            String key = prescriptionKey(
                    prescription.getNom_medicament(),
                    prescription.getDose(),
                    prescription.getFrequence(),
                    prescription.getInstructions()
            );
            if (!uniquenessSet.add(key)) {
                throw new IllegalArgumentException("Doublon detecte: la meme prescription ne peut pas etre ajoutee deux fois dans la meme fiche.");
            }
        }

        return prescriptions;
    }

    private String text(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private static class PrescriptionRowControls {
        private final HBox row;
        private final TextField nomField;
        private final TextField doseField;
        private final TextField frequenceField;
        private final TextField dureeField;
        private final TextField instructionsField;

        private PrescriptionRowControls(HBox row, TextField nomField, TextField doseField, TextField frequenceField, TextField dureeField, TextField instructionsField) {
            this.row = row;
            this.nomField = nomField;
            this.doseField = doseField;
            this.frequenceField = frequenceField;
            this.dureeField = dureeField;
            this.instructionsField = instructionsField;
        }
    }

    private RendezVous findSelectedRendezVous() {
        if (selectedRendezVousId == null) {
            return null;
        }

        for (RendezVous rendezVous : allDoctorRendezVous) {
            if (rendezVous != null && rendezVous.getId() == selectedRendezVousId) {
                return rendezVous;
            }
        }

        return null;
    }

    private void setSaveButtonEnabled(boolean enabled) {
        if (saveFicheButton != null) {
            saveFicheButton.setDisable(!enabled);
        }
    }

    private void startConsultationTimer() {
        stopConsultationTimer();

        consultationTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            if (consultationStartTime == null || consultationTimerLabel == null) {
                return;
            }

            long elapsedSeconds = java.time.Duration.between(
                    consultationStartTime.toLocalDateTime(),
                    LocalDateTime.now()
            ).getSeconds();
            consultationTimerLabel.setText(formatElapsedTime(elapsedSeconds));
        }));
        consultationTimer.setCycleCount(Timeline.INDEFINITE);
        consultationTimer.play();
    }

    private void stopConsultationTimer() {
        if (consultationTimer != null) {
            consultationTimer.stop();
            consultationTimer = null;
        }
    }

    private int calculateDurationMinutes(Timestamp startTime, Timestamp endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }

        long seconds = Duration.between(startTime.toLocalDateTime(), endTime.toLocalDateTime()).getSeconds();
        if (seconds < 0) {
            return 0;
        }
        return (int) Math.max(1, Math.ceil(seconds / 60.0));
    }

    private String formatDuration(int durationMinutes) {
        long totalSeconds = durationMinutes * 60L;
        return formatElapsedTime(totalSeconds);
    }

    private String formatElapsedTime(long elapsedSeconds) {
        long minutes = Math.max(0, elapsedSeconds / 60);
        long seconds = Math.max(0, elapsedSeconds % 60);
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return String.format("%02d:%02d:%02d", hours, remainingMinutes, seconds);
    }

    private void updateConsultationTimingLabels(Timestamp startTime, Timestamp endTime, Integer durationMinutes, String timerText) {
        if (consultationStartTimeLabel != null) {
            consultationStartTimeLabel.setText(startTime == null ? "Start time: -" : "Start time: " + startTime.toLocalDateTime());
        }
        if (consultationEndTimeLabel != null) {
            consultationEndTimeLabel.setText(endTime == null ? "End time: -" : "End time: " + endTime.toLocalDateTime());
        }
        if (consultationDurationLabel != null) {
            consultationDurationLabel.setText(durationMinutes == null ? "Duration: -" : "Duration: " + formatDuration(durationMinutes));
        }
        if (consultationTimerLabel != null) {
            consultationTimerLabel.setText(timerText == null ? "00:00:00" : timerText);
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateSelectedRendezVousLabel() {
        if (selectedRendezVousIdLabel == null) {
            return;
        }

        if (selectedRendezVousId == null) {
            selectedRendezVousIdLabel.setText("Rendez vous id: -");
        } else {
            selectedRendezVousIdLabel.setText("Rendez vous id: " + selectedRendezVousId);
        }
    }

    // ============ Detail Panel Methods ============

    private void displayDetailPanel(RendezVous rendezVous) {
        if (rendezVous == null) {
            clearDetailPanel();
            return;
        }

        currentDetailRendezVous = rendezVous;
        setFicheEditMode(false);
        setPrescriptionsEditMode(false);

        // Display rendez-vous details
        if (detailDateTimeLabel != null) {
            detailDateTimeLabel.setText(rendezVous.getDatetime() != null ? rendezVous.getDatetime().toString() : "-");
        }
        if (detailModeLabel != null) {
            detailModeLabel.setText(nullSafe(rendezVous.getMode()));
        }
        if (detailMotifLabel != null) {
            detailMotifLabel.setText(nullSafe(rendezVous.getMotif()));
        }
        if (detailPatientLabel != null) {
            detailPatientLabel.setText(String.valueOf(rendezVous.getIdPatient()));
        }
        if (detailStatutLabel != null) {
            detailStatutLabel.setText(nullSafe(rendezVous.getStatut()));
        }
        if (detailUrgenceLabel != null) {
            detailUrgenceLabel.setText(nullSafe(rendezVous.getUrgency_level()));
        }

        // Load and display fiche medicale and prescriptions
        loadFicheMedicale(rendezVous.getId());
        loadPrescriptions(rendezVous.getId());
    }

    private void clearDetailPanel() {
        currentDetailRendezVous = null;
        currentDetailFiche = null;
        setFicheEditMode(false);
        setPrescriptionsEditMode(false);

        if (detailDateTimeLabel != null) detailDateTimeLabel.setText("-");
        if (detailModeLabel != null) detailModeLabel.setText("-");
        if (detailMotifLabel != null) detailMotifLabel.setText("-");
        if (detailPatientLabel != null) detailPatientLabel.setText("-");
        if (detailStatutLabel != null) detailStatutLabel.setText("-");
        if (detailUrgenceLabel != null) detailUrgenceLabel.setText("-");

        if (detailFicheMedicaleMessage != null) {
            detailFicheMedicaleMessage.setVisible(true);
            detailFicheMedicaleMessage.setManaged(true);
        }

        hideFicheMedicaleDetails();

        detailPrescriptions.clear();

        if (detailPrescriptionsTable != null) {
            detailPrescriptionsTable.setVisible(false);
            detailPrescriptionsTable.setManaged(false);
        }
        if (detailNoPrescriptionsLabel != null) {
            detailNoPrescriptionsLabel.setVisible(true);
            detailNoPrescriptionsLabel.setManaged(true);
        }
    }

    private void loadFicheMedicale(int rendezVousId) {
        hideFicheMedicaleDetails();

        FicheMedicale fiche = ficheMedicaleService.getByRendezVousId(rendezVousId);
        currentDetailFiche = fiche;

        if (fiche == null) {
            if (detailFicheMedicaleMessage != null) {
                detailFicheMedicaleMessage.setVisible(true);
                detailFicheMedicaleMessage.setManaged(true);
            }
        } else {
            if (detailFicheMedicaleMessage != null) {
                detailFicheMedicaleMessage.setVisible(false);
                detailFicheMedicaleMessage.setManaged(false);
            }

            // Show fiche details
            if (detailDiagnosticBox != null) {
                if (detailDiagnosticLabel != null) {
                    detailDiagnosticLabel.setText(nullSafe(fiche.getDiagnostic()));
                }
                detailDiagnosticBox.setVisible(true);
                detailDiagnosticBox.setManaged(true);
            }

            if (detailObservationsBox != null) {
                if (detailObservationsLabel != null) {
                    detailObservationsLabel.setText(nullSafe(fiche.getObservations()));
                }
                detailObservationsBox.setVisible(true);
                detailObservationsBox.setManaged(true);
            }

            if (detailResultatsBox != null) {
                if (detailResultatsLabel != null) {
                    detailResultatsLabel.setText(nullSafe(fiche.getResultats_examens()));
                }
                detailResultatsBox.setVisible(true);
                detailResultatsBox.setManaged(true);
            }

            if (detailDureeBox != null) {
                if (detailDureeLabel != null) {
                    detailDureeLabel.setText(fiche.getDuree_minutes() != null ? fiche.getDuree_minutes() + " min" : "-");
                }
                detailDureeBox.setVisible(true);
                detailDureeBox.setManaged(true);
            }
        }
    }

    private void hideFicheMedicaleDetails() {
        if (detailDiagnosticBox != null) {
            detailDiagnosticBox.setVisible(false);
            detailDiagnosticBox.setManaged(false);
        }
        if (detailObservationsBox != null) {
            detailObservationsBox.setVisible(false);
            detailObservationsBox.setManaged(false);
        }
        if (detailResultatsBox != null) {
            detailResultatsBox.setVisible(false);
            detailResultatsBox.setManaged(false);
        }
        if (detailDureeBox != null) {
            detailDureeBox.setVisible(false);
            detailDureeBox.setManaged(false);
        }
    }

    private void loadPrescriptions(int rendezVousId) {
        if (detailPrescriptionsTable == null) {
            return;
        }

        FicheMedicale fiche = ficheMedicaleService.getByRendezVousId(rendezVousId);
        List<Prescription> prescriptions = new ArrayList<>();

        if (fiche != null && fiche.getId() > 0) {
            prescriptions = prescriptionService.getByFicheMedicaleId(fiche.getId());
        }

        detailPrescriptions.setAll(prescriptions);

        if (detailPrescriptions.isEmpty()) {
            detailPrescriptionsTable.setVisible(false);
            detailPrescriptionsTable.setManaged(false);

            if (detailNoPrescriptionsLabel != null) {
                detailNoPrescriptionsLabel.setVisible(true);
                detailNoPrescriptionsLabel.setManaged(true);
            }
        } else {
            if (detailNoPrescriptionsLabel != null) {
                detailNoPrescriptionsLabel.setVisible(false);
                detailNoPrescriptionsLabel.setManaged(false);
            }
            detailPrescriptionsTable.setVisible(true);
            detailPrescriptionsTable.setManaged(true);
        }
    }

    @FXML
    private void handleEditFicheSection(ActionEvent event) {
        if (currentDetailFiche == null) {
            showError("Fiche absente", "Aucune fiche medicale a modifier pour ce rendez-vous.");
            return;
        }

        if (detailDiagnosticEditArea != null) detailDiagnosticEditArea.setText(nullSafe(currentDetailFiche.getDiagnostic()));
        if (detailObservationsEditArea != null) detailObservationsEditArea.setText(nullSafe(currentDetailFiche.getObservations()));
        if (detailResultatsEditArea != null) detailResultatsEditArea.setText(nullSafe(currentDetailFiche.getResultats_examens()));
        if (detailDureeEditField != null) detailDureeEditField.setText(currentDetailFiche.getDuree_minutes() == null ? "" : String.valueOf(currentDetailFiche.getDuree_minutes()));

        setFicheEditMode(true);
    }

    @FXML
    private void handleCancelEditFicheSection(ActionEvent event) {
        setFicheEditMode(false);
        if (currentDetailRendezVous != null) {
            loadFicheMedicale(currentDetailRendezVous.getId());
        }
    }

    @FXML
    private void handleConfirmEditFicheSection(ActionEvent event) {
        if (currentDetailFiche == null) {
            return;
        }

        try {
            String diagnostic = requiredText(detailDiagnosticEditArea, "Le diagnostic est obligatoire.");
            String observations = requiredText(detailObservationsEditArea, "Les observations sont obligatoires.");
            String resultats = requiredText(detailResultatsEditArea, "Les resultats d'examens sont obligatoires.");

            String dureeRaw = detailDureeEditField == null ? "" : detailDureeEditField.getText().trim();
            if (dureeRaw.isEmpty()) {
                throw new IllegalArgumentException("La duree de consultation est obligatoire.");
            }

            int duree;
            try {
                duree = Integer.parseInt(dureeRaw);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("La duree doit etre un nombre entier.");
            }

            if (duree <= 0) {
                throw new IllegalArgumentException("La duree doit etre superieure a 0.");
            }

            currentDetailFiche.setDiagnostic(diagnostic);
            currentDetailFiche.setObservations(observations);
            currentDetailFiche.setResultats_examens(resultats);
            currentDetailFiche.setDuree_minutes(duree);

            ficheMedicaleService.modifier(currentDetailFiche);
            setFicheEditMode(false);
            if (currentDetailRendezVous != null) {
                loadFicheMedicale(currentDetailRendezVous.getId());
            }
            showInfo("Succes", "Fiche medicale modifiee avec succes.");
        } catch (IllegalArgumentException ex) {
            showError("Valeur invalide", ex.getMessage());
        }
    }

    @FXML
    private void handleDeleteFicheSection(ActionEvent event) {
        if (currentDetailFiche == null) {
            showError("Fiche absente", "Aucune fiche medicale a supprimer.");
            return;
        }

        if (!confirmAction("Supprimer Fiche", "Supprimer cette fiche medicale et ses prescriptions ?")) {
            return;
        }

        List<Prescription> prescriptions = prescriptionService.getByFicheMedicaleId(currentDetailFiche.getId());
        for (Prescription prescription : prescriptions) {
            prescriptionService.supprimer(prescription);
        }
        ficheMedicaleService.supprimer(currentDetailFiche);

        currentDetailFiche = null;
        setFicheEditMode(false);
        setPrescriptionsEditMode(false);
        if (currentDetailRendezVous != null) {
            loadFicheMedicale(currentDetailRendezVous.getId());
            loadPrescriptions(currentDetailRendezVous.getId());
        }
        showInfo("Succes", "Fiche medicale supprimee avec succes.");
    }

    @FXML
    private void handleEditPrescriptionsSection(ActionEvent event) {
        if (detailPrescriptions.isEmpty()) {
            showError("Prescriptions absentes", "Aucune prescription a modifier.");
            return;
        }

        prescriptionsSnapshot = copyPrescriptions(detailPrescriptions);
        setPrescriptionsEditMode(true);
    }

    @FXML
    private void handleCancelEditPrescriptionsSection(ActionEvent event) {
        detailPrescriptions.setAll(copyPrescriptions(prescriptionsSnapshot));
        setPrescriptionsEditMode(false);
    }

    @FXML
    private void handleConfirmEditPrescriptionsSection(ActionEvent event) {
        if (detailPrescriptions.isEmpty()) {
            setPrescriptionsEditMode(false);
            return;
        }

        if (detailPrescriptionsTable != null) {
            detailPrescriptionsTable.requestFocus();
        }

        try {
            Set<String> uniquenessSet = new HashSet<>();
            int rowIndex = 1;
            for (Prescription prescription : detailPrescriptions) {
                validatePrescriptionRow(prescription, rowIndex);

                String key = prescriptionKey(
                        prescription.getNom_medicament(),
                        prescription.getDose(),
                        prescription.getFrequence(),
                        prescription.getInstructions()
                );
                if (!uniquenessSet.add(key)) {
                    throw new IllegalArgumentException("La prescription de la ligne #" + rowIndex + " est en doublon.");
                }
                rowIndex++;
            }
        } catch (IllegalArgumentException ex) {
            showError("Validation prescriptions", ex.getMessage());
            return;
        }

        for (Prescription prescription : detailPrescriptions) {
            prescriptionService.modifier(prescription);
        }

        setPrescriptionsEditMode(false);
        showInfo("Succes", "Prescriptions modifiees avec succes.");
    }

    @FXML
    private void handleDeletePrescriptionsSection(ActionEvent event) {
        if (detailPrescriptions.isEmpty()) {
            showError("Prescriptions absentes", "Aucune prescription a supprimer.");
            return;
        }

        if (!confirmAction("Supprimer Prescriptions", "Supprimer toutes les prescriptions de cette fiche ?")) {
            return;
        }

        List<Prescription> toDelete = new ArrayList<>(detailPrescriptions);
        for (Prescription prescription : toDelete) {
            prescriptionService.supprimer(prescription);
        }

        detailPrescriptions.clear();
        setPrescriptionsEditMode(false);
        if (detailPrescriptionsTable != null) {
            detailPrescriptionsTable.setVisible(false);
            detailPrescriptionsTable.setManaged(false);
        }
        if (detailNoPrescriptionsLabel != null) {
            detailNoPrescriptionsLabel.setVisible(true);
            detailNoPrescriptionsLabel.setManaged(true);
        }
        showInfo("Succes", "Prescriptions supprimees avec succes.");
    }

    private void setFicheEditMode(boolean enabled) {
        ficheEditMode = enabled;
        toggleEditableArea(detailDiagnosticLabel, detailDiagnosticEditArea, enabled);
        toggleEditableArea(detailObservationsLabel, detailObservationsEditArea, enabled);
        toggleEditableArea(detailResultatsLabel, detailResultatsEditArea, enabled);
        toggleEditableField(detailDureeLabel, detailDureeEditField, enabled);

        if (ficheModifyButton != null) {
            ficheModifyButton.setVisible(!enabled);
            ficheModifyButton.setManaged(!enabled);
        }
        if (ficheDeleteButton != null) {
            ficheDeleteButton.setVisible(!enabled);
            ficheDeleteButton.setManaged(!enabled);
        }
        if (ficheCancelButton != null) {
            ficheCancelButton.setVisible(enabled);
            ficheCancelButton.setManaged(enabled);
        }
        if (ficheConfirmButton != null) {
            ficheConfirmButton.setVisible(enabled);
            ficheConfirmButton.setManaged(enabled);
        }
    }

    private void setPrescriptionsEditMode(boolean enabled) {
        prescriptionsEditMode = enabled;
        if (detailPrescriptionsTable != null) {
            detailPrescriptionsTable.setEditable(enabled);
        }

        if (prescriptionModifyButton != null) {
            prescriptionModifyButton.setVisible(!enabled);
            prescriptionModifyButton.setManaged(!enabled);
        }
        if (prescriptionDeleteButton != null) {
            prescriptionDeleteButton.setVisible(!enabled);
            prescriptionDeleteButton.setManaged(!enabled);
        }
        if (prescriptionCancelButton != null) {
            prescriptionCancelButton.setVisible(enabled);
            prescriptionCancelButton.setManaged(enabled);
        }
        if (prescriptionConfirmButton != null) {
            prescriptionConfirmButton.setVisible(enabled);
            prescriptionConfirmButton.setManaged(enabled);
        }
    }

    private void toggleEditableField(Label label, TextField field, boolean enabled) {
        if (label != null) {
            label.setVisible(!enabled);
            label.setManaged(!enabled);
        }
        if (field != null) {
            field.setVisible(enabled);
            field.setManaged(enabled);
        }
    }

    private void toggleEditableArea(Label label, TextArea area, boolean enabled) {
        if (label != null) {
            label.setVisible(!enabled);
            label.setManaged(!enabled);
        }
        if (area != null) {
            area.setVisible(enabled);
            area.setManaged(enabled);
        }
    }

    private String textOrNull(TextField field) {
        if (field == null || field.getText() == null) {
            return null;
        }
        String value = field.getText().trim();
        return value.isEmpty() ? null : value;
    }

    private String textOrNull(TextArea area) {
        if (area == null || area.getText() == null) {
            return null;
        }
        String value = area.getText().trim();
        return value.isEmpty() ? null : value;
    }

    private List<Prescription> copyPrescriptions(List<Prescription> source) {
        List<Prescription> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }

        for (Prescription item : source) {
            if (item == null) {
                continue;
            }
            Prescription clone = new Prescription();
            clone.setId(item.getId());
            clone.setFiche_medicale_id(item.getFiche_medicale_id());
            clone.setNom_medicament(item.getNom_medicament());
            clone.setDose(item.getDose());
            clone.setFrequence(item.getFrequence());
            clone.setDuree(item.getDuree());
            clone.setInstructions(item.getInstructions());
            clone.setCreated_at(item.getCreated_at());
            copy.add(clone);
        }
        return copy;
    }

    private boolean confirmAction(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private String requiredText(TextArea area, String errorMessage) {
        String value = textOrNull(area);
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private void validatePrescriptionRow(Prescription prescription, int rowIndex) {
        if (prescription == null) {
            throw new IllegalArgumentException("La ligne de prescription #" + rowIndex + " est invalide.");
        }
        if (isBlank(prescription.getNom_medicament())) {
            throw new IllegalArgumentException("Le nom du medicament est obligatoire (ligne #" + rowIndex + ").");
        }
        if (isBlank(prescription.getDose())) {
            throw new IllegalArgumentException("La dose est obligatoire (ligne #" + rowIndex + ").");
        }
        if (isBlank(prescription.getFrequence())) {
            throw new IllegalArgumentException("La frequence est obligatoire (ligne #" + rowIndex + ").");
        }
        if (isBlank(prescription.getInstructions())) {
            throw new IllegalArgumentException("Les instructions sont obligatoires (ligne #" + rowIndex + ").");
        }
        if (prescription.getDuree() <= 0) {
            throw new IllegalArgumentException("La duree doit etre superieure a 0 (ligne #" + rowIndex + ").");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String prescriptionKey(String nom, String dose, String frequence, String instructions) {
        return normalizeKeyPart(nom) + "|" + normalizeKeyPart(dose) + "|" + normalizeKeyPart(frequence) + "|" + normalizeKeyPart(instructions);
    }

    private String normalizeKeyPart(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}