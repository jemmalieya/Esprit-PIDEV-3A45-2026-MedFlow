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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.List;

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

    private final ObservableList<RendezVous> doctorRendezVous = FXCollections.observableArrayList();
    private final List<PrescriptionRowControls> prescriptionRows = new ArrayList<>();
    private Integer selectedRendezVousId;
    private Timestamp consultationStartTime;
    private Timeline consultationTimer;

    // Initialize method to set up the combo box options
    @FXML
    public void initialize() {
        if (sortComboBox != null) {
            sortComboBox.getItems().addAll("Date ASC", "Date DESC");
            sortComboBox.setOnAction(this::handleSortSelection);
        }

        if (eventTable != null) {
            eventTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            configureTableColumns();
            loadDoctorRendezVous();
        }
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
        if (sortComboBox == null) {
            return;
        }

        String selectedValue = sortComboBox.getValue();
        if (selectedValue == null) {
            return;
        }

        if ("Date ASC".equals(selectedValue)) {
            FXCollections.sort(doctorRendezVous, Comparator.comparing(RendezVous::getDatetime));
        } else if ("Date DESC".equals(selectedValue)) {
            FXCollections.sort(doctorRendezVous, Comparator.comparing(RendezVous::getDatetime).reversed());
        }
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

    private void loadDoctorRendezVous() {
        List<RendezVous> list = rendezVousService.recupererParStaffId(SESSION_DOCTOR_ID);
        doctorRendezVous.setAll(list);
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

            Prescription prescription = new Prescription();
            prescription.setNom_medicament(nom);
            prescription.setDose(dose);
            prescription.setFrequence(frequence);
            prescription.setDuree(duree);
            prescription.setInstructions(instructions);
            prescription.setCreated_at(createdAt);
            prescriptions.add(prescription);
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

        for (RendezVous rendezVous : doctorRendezVous) {
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
}