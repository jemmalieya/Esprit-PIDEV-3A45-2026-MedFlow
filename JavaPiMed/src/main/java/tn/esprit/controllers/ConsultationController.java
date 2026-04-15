
package tn.esprit.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Prescription;
import tn.esprit.entities.FicheMedicale;
import tn.esprit.entities.RendezVous;
import tn.esprit.entities.User;
import tn.esprit.services.FicheMedicaleService;
import tn.esprit.services.PrescriptionService;
import tn.esprit.services.RendezVousService;
import tn.esprit.services.UserService;

import java.net.URL;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.ResourceBundle;
import javafx.util.Duration;

public class ConsultationController implements Initializable {

    private static final int SESSION_PATIENT_ID = 1;

    @FXML
    private TextField doctorField;
    @FXML
    private TextField doctorField1;
    @FXML
    private TextField patientIdField;
    @FXML
    private TextArea motifArea;
    @FXML
    private ComboBox<String> modeComboBox;

    @FXML
    private VBox bookingPage;
    @FXML
    private VBox myBookingsPage;
    @FXML
    private Button bookingPageBtn;
    @FXML
    private Button myBookingsPageBtn;
@FXML
    private Button myRecordsPageBtn;

    @FXML
    private Label monthYearLabel;
    @FXML
    private HBox calendarHeaderRow;
    @FXML
    private VBox timeSlotGrid;
    @FXML
    private Label selectedDateLabel;
    @FXML
    private Label selectedSlotLabel;
    @FXML
    private Label selectedModeLabel;
    @FXML
    private VBox doctorCardsContainer;

    @FXML
    private TableView<RendezVous> myBookingsTable;
    @FXML
    private TextField myBookingsSearchField;
    @FXML
    private ComboBox<String> myBookingsSortComboBox;
    @FXML
    private TextField myRecordsSearchField;
    @FXML
    private ComboBox<String> myRecordsSortComboBox;
    @FXML
    private VBox myRecordsPage;
    @FXML
    private TableView<FicheMedicale> myRecordsTable;
    @FXML
    private TableColumn<FicheMedicale, Number> recordColId;
    @FXML
    private TableColumn<FicheMedicale, Number> recordColRdvId;
    @FXML
    private TableColumn<FicheMedicale, String> recordColDateTime;
    @FXML
    private TableColumn<FicheMedicale, String> recordColDiagnostic;
    @FXML
    private TableColumn<FicheMedicale, String> recordColObservations;
    @FXML
    private TableColumn<FicheMedicale, String> recordColResults;
    @FXML
    private TableColumn<FicheMedicale, String> recordColDuration;
    @FXML
    private TableColumn<FicheMedicale, String> recordColSignature;
    @FXML
    private TableColumn<FicheMedicale, Void> recordColActions;
    @FXML
    private VBox recordDetailsPanel;
    @FXML
    private Label recordDetailsSummaryLabel;
    @FXML
    private Label recordNoPrescriptionsLabel;
    @FXML
    private TableView<Prescription> recordPrescriptionsTable;
    @FXML
    private TableColumn<Prescription, String> recordPrescriptionNomColumn;
    @FXML
    private TableColumn<Prescription, String> recordPrescriptionDoseColumn;
    @FXML
    private TableColumn<Prescription, String> recordPrescriptionFrequenceColumn;
    @FXML
    private TableColumn<Prescription, String> recordPrescriptionInstructionsColumn;
    @FXML
    private TableColumn<RendezVous, Number> colId;
    @FXML
    private TableColumn<RendezVous, String> colDateTime;
    @FXML
    private TableColumn<RendezVous, String> colMode;
    @FXML
    private TableColumn<RendezVous, String> colMotif;
    @FXML
    private TableColumn<RendezVous, Number> colStaff;
    @FXML
    private TableColumn<RendezVous, String> colStatut;
    @FXML
    private TableColumn<RendezVous, Void> colActions;

    @FXML
    private VBox modifyFormCard;
    @FXML
    private TextField modifyPatientIdField;
    @FXML
    private TextField modifyStaffIdField;
    @FXML
    private ComboBox<String> modifyModeComboBox;
    @FXML
    private TextField modifyDateTimeField;
    @FXML
    private TextArea modifyMotifArea;

    private Integer editingRendezVousId;
    private final ObservableList<RendezVous> myBookingsData = FXCollections.observableArrayList();
    private final ObservableList<RendezVous> allMyBookingsData = FXCollections.observableArrayList();
    private final ObservableList<FicheMedicale> myRecordsData = FXCollections.observableArrayList();
    private final ObservableList<Prescription> recordPrescriptionsData = FXCollections.observableArrayList();
    private final HashMap<Integer, Boolean> expandedRecordState = new HashMap<>();
    private final RendezVousService rendezVousService = new RendezVousService();
    private final UserService userService = new UserService();
    private final FicheMedicaleService ficheMedicaleService = new FicheMedicaleService();
    private final PrescriptionService prescriptionService = new PrescriptionService();

    private LocalDate currentWeekStart;
    private LocalDateTime selectedDateTime;

    private final DateTimeFormatter weekRangeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("EEE dd MMM yyyy 'at' HH:mm", Locale.ENGLISH);
    private final DateTimeFormatter modifyDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (modeComboBox != null) {
            modeComboBox.setItems(FXCollections.observableArrayList("Distanciel", "Présentiel"));
        }
        if (modifyModeComboBox != null) {
            modifyModeComboBox.setItems(FXCollections.observableArrayList("Distanciel", "Présentiel"));
        }

        if (patientIdField != null) {
            patientIdField.setText(String.valueOf(SESSION_PATIENT_ID));
            patientIdField.setEditable(false);
        }
        if (modifyPatientIdField != null) {
            modifyPatientIdField.setText(String.valueOf(SESSION_PATIENT_ID));
            modifyPatientIdField.setEditable(false);
        }

        if (myBookingsSortComboBox != null) {
            myBookingsSortComboBox.getItems().addAll(
                    "Date ASC",
                    "Date DESC",
                    "Statut: Demande",
                    "Statut: Confirmé",
                    "Statut: Terminé"
            );
            myBookingsSortComboBox.setOnAction(e -> applyMyBookingsFilterAndSort());
        }

        if (myBookingsSearchField != null) {
            myBookingsSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyMyBookingsFilterAndSort());
        }

        if (myRecordsSortComboBox != null) {
            myRecordsSortComboBox.getItems().addAll(
                    "Date ASC",
                    "Date DESC",
                    "Diagnostic A-Z",
                    "Diagnostic Z-A"
            );
            myRecordsSortComboBox.setOnAction(e -> applyMyRecordsFilterAndSort());
        }

        if (myRecordsSearchField != null) {
            myRecordsSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyMyRecordsFilterAndSort());
        }

        setupMyRecordsTable();
        setupRecordPrescriptionsTable();

        hideModifyForm();
        setupMyBookingsTable();
        loadMyBookings();

        showBookingPage();
        currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        refreshCalendar();
        loadDoctorCards();
    }

    @FXML
    private void handleShowBookingPage(ActionEvent event) {
        showBookingPage();
    }

    @FXML
    private void handleShowMyBookingsPage(ActionEvent event) {
        showMyBookingsPage();
    }
    @FXML
    private void handleShowMyRecords(ActionEvent event) {
        showMyRecordsPage();
    }

    @FXML
    private void handleModeSelection(ActionEvent event) {
        if (selectedModeLabel != null) {
            String selectedMode = modeComboBox != null ? modeComboBox.getValue() : null;
            selectedModeLabel.setText(selectedMode == null || selectedMode.isBlank() ? "No mode selected" : selectedMode);
        }
    }

    @FXML
    private void handlePreviousWeek(ActionEvent event) {
        currentWeekStart = currentWeekStart.minusWeeks(1);
        refreshCalendar();
    }

    @FXML
    private void handleNextWeek(ActionEvent event) {
        currentWeekStart = currentWeekStart.plusWeeks(1);
        refreshCalendar();
    }

    @FXML
    private void handleConfirmAppointment(ActionEvent event) {
        try {
            int idStaff = parseRequiredInt(doctorField1, "Doctor/Staff ID");

            if (selectedDateTime == null) {
                throw new IllegalArgumentException("Please select a date and time slot from the calendar.");
            }

            String mode = modeComboBox != null ? modeComboBox.getValue() : null;
            if (mode == null || mode.isBlank()) {
                throw new IllegalArgumentException("Please choose a booking mode.");
            }

            String motif = motifArea != null ? motifArea.getText() : null;
            if (motif == null || motif.isBlank()) {
                throw new IllegalArgumentException("Please enter a motif.");
            }
            motif = motif.trim();

            if (selectedDateTime.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("The appointment date/time must be in the future.");
            }

            if (hasDuplicateRendezVous(null, Timestamp.valueOf(selectedDateTime), SESSION_PATIENT_ID, idStaff, mode, motif)) {
                throw new IllegalArgumentException("A booking with the same date/time, doctor, mode and motif already exists.");
            }

            RendezVous rendezVous = new RendezVous(
                    Timestamp.valueOf(selectedDateTime),
                    "Demande",
                    mode,
                    motif,
                    new Timestamp(System.currentTimeMillis()),
                    SESSION_PATIENT_ID,
                    idStaff,
                    null
            );

            rendezVousService.ajouter(rendezVous);
            showInfo("Success", "Appointment saved successfully.");
            loadMyBookings();
        } catch (IllegalArgumentException ex) {
            showError("Validation error", ex.getMessage());
        } catch (Exception ex) {
            showError("Unexpected error", "Failed to save appointment: " + ex.getMessage());
        }
    }

    @FXML
    private void handleConfirmModify(ActionEvent event) {
        try {
            if (editingRendezVousId == null) {
                throw new IllegalArgumentException("No booking selected for modification.");
            }

            int idStaff = parseRequiredInt(modifyStaffIdField, "Doctor/Staff ID");
            String mode = modifyModeComboBox != null ? modifyModeComboBox.getValue() : null;
            if (mode == null || mode.isBlank()) {
                throw new IllegalArgumentException("Mode is required.");
            }

            String motif = modifyMotifArea != null ? modifyMotifArea.getText() : null;
            if (motif == null || motif.isBlank()) {
                throw new IllegalArgumentException("Motif is required.");
            }
            motif = motif.trim();

            String rawDateTime = modifyDateTimeField != null ? modifyDateTimeField.getText() : null;
            if (rawDateTime == null || rawDateTime.isBlank()) {
                throw new IllegalArgumentException("Date & time is required (format: yyyy-MM-dd HH:mm).");
            }

            LocalDateTime dateTime;
            try {
                dateTime = LocalDateTime.parse(rawDateTime.trim(), modifyDateTimeFormatter);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd HH:mm.");
            }

            if (dateTime.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("The appointment date/time must be in the future.");
            }

            if (hasDuplicateRendezVous(editingRendezVousId, Timestamp.valueOf(dateTime), SESSION_PATIENT_ID, idStaff, mode, motif)) {
                throw new IllegalArgumentException("Another booking with the same date/time, doctor, mode and motif already exists.");
            }

            RendezVous rendezVous = new RendezVous(
                    editingRendezVousId,
                    Timestamp.valueOf(dateTime),
                    "Demande",
                    mode,
                    motif,
                    new Timestamp(System.currentTimeMillis()),
                    SESSION_PATIENT_ID,
                    idStaff,
                    null
            );

            rendezVousService.modifier(rendezVous);
            showInfo("Updated", "Appointment updated successfully.");
            editingRendezVousId = null;
            clearModifyForm();
            hideModifyForm();
            loadMyBookings();
        } catch (IllegalArgumentException ex) {
            showError("Validation error", ex.getMessage());
        } catch (Exception ex) {
            showError("Unexpected error", "Failed to update appointment: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancelModify(ActionEvent event) {
        editingRendezVousId = null;
        clearModifyForm();
        hideModifyForm();
    }

    private void showBookingPage() {
        bookingPage.setVisible(true);
        bookingPage.setManaged(true);
        myBookingsPage.setVisible(false);
        myBookingsPage.setManaged(false);

        bookingPageBtn.getStyleClass().remove("page-nav-button");
        if (!bookingPageBtn.getStyleClass().contains("page-nav-button-active")) {
            bookingPageBtn.getStyleClass().add("page-nav-button-active");
        }
        myBookingsPageBtn.getStyleClass().remove("page-nav-button-active");
        if (!myBookingsPageBtn.getStyleClass().contains("page-nav-button")) {
            myBookingsPageBtn.getStyleClass().add("page-nav-button");
        }
    }

    private void showMyBookingsPage() {
        bookingPage.setVisible(false);
        bookingPage.setManaged(false);
        myBookingsPage.setVisible(true);
        myBookingsPage.setManaged(true);
        if (myRecordsPage != null) {
            myRecordsPage.setVisible(false);
            myRecordsPage.setManaged(false);
        }

        bookingPageBtn.getStyleClass().remove("page-nav-button-active");
        if (!bookingPageBtn.getStyleClass().contains("page-nav-button")) {
            bookingPageBtn.getStyleClass().add("page-nav-button");
        }
        myBookingsPageBtn.getStyleClass().remove("page-nav-button");
        if (!myBookingsPageBtn.getStyleClass().contains("page-nav-button-active")) {
            myBookingsPageBtn.getStyleClass().add("page-nav-button-active");
        }

        loadMyBookings();
    }

    private void showMyRecordsPage() {
        bookingPage.setVisible(false);
        bookingPage.setManaged(false);
        myBookingsPage.setVisible(false);
        myBookingsPage.setManaged(false);
        if (myRecordsPage != null) {
            myRecordsPage.setVisible(true);
            myRecordsPage.setManaged(true);
        }

        bookingPageBtn.getStyleClass().remove("page-nav-button-active");
        if (!bookingPageBtn.getStyleClass().contains("page-nav-button")) {
            bookingPageBtn.getStyleClass().add("page-nav-button");
        }
        myBookingsPageBtn.getStyleClass().remove("page-nav-button-active");
        if (!myBookingsPageBtn.getStyleClass().contains("page-nav-button")) {
            myBookingsPageBtn.getStyleClass().add("page-nav-button");
        }
        if (myRecordsPageBtn != null) {
            myRecordsPageBtn.getStyleClass().remove("page-nav-button");
            if (!myRecordsPageBtn.getStyleClass().contains("page-nav-button-active")) {
                myRecordsPageBtn.getStyleClass().add("page-nav-button-active");
            }
        }

        applyMyRecordsFilterAndSort();
    }

    private void refreshCalendar() {
        if (monthYearLabel != null) {
            LocalDate weekEnd = currentWeekStart.plusDays(4);
            monthYearLabel.setText(currentWeekStart.format(weekRangeFormatter) + " - " + weekEnd.format(weekRangeFormatter));
        }
        List<LocalDate> weekdays = getWeekdaysOfWeek(currentWeekStart);
        buildCalendarHeader(weekdays);
        buildCalendarSlots(weekdays);
    }

    private List<LocalDate> getWeekdaysOfWeek(LocalDate weekStart) {
        List<LocalDate> weekdays = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            weekdays.add(weekStart.plusDays(i));
        }
        return weekdays;
    }

    private void buildCalendarHeader(List<LocalDate> weekdays) {
        if (calendarHeaderRow == null) {
            return;
        }
        calendarHeaderRow.getChildren().clear();

        Label timeHeader = new Label("Time");
        timeHeader.getStyleClass().add("calendar-time-header");
        timeHeader.setMinWidth(70);
        timeHeader.setPrefWidth(70);
        timeHeader.setMaxWidth(70);
        calendarHeaderRow.getChildren().add(timeHeader);

        DateTimeFormatter headerFmt = DateTimeFormatter.ofPattern("EEE dd", Locale.ENGLISH);
        for (LocalDate date : weekdays) {
            Label dayHeader = new Label(date.format(headerFmt));
            dayHeader.getStyleClass().add("calendar-day-header");
            dayHeader.setMinWidth(84);
            dayHeader.setPrefWidth(84);
            dayHeader.setMaxWidth(84);
            calendarHeaderRow.getChildren().add(dayHeader);
        }
    }

    private void buildCalendarSlots(List<LocalDate> weekdays) {
        if (timeSlotGrid == null) {
            return;
        }
        timeSlotGrid.getChildren().clear();

        for (int hour = 8; hour <= 16; hour++) {
            HBox row = new HBox(6);
            Label timeLabel = new Label(String.format("%02d:00", hour));
            timeLabel.getStyleClass().add("calendar-time-cell");
            timeLabel.setMinWidth(70);
            timeLabel.setPrefWidth(70);
            timeLabel.setMaxWidth(70);
            row.getChildren().add(timeLabel);

            for (LocalDate date : weekdays) {
                Button slotBtn = new Button();
                slotBtn.getStyleClass().add("calendar-slot-button");
                slotBtn.setMinWidth(84);
                slotBtn.setPrefWidth(84);
                slotBtn.setMaxWidth(84);
                slotBtn.setPrefHeight(30);
                slotBtn.setMaxHeight(30);

                final int selectedHour = hour;
                slotBtn.setOnAction(e -> handleSlotSelection(date, selectedHour));
                HBox.setHgrow(slotBtn, Priority.NEVER);
                row.getChildren().add(slotBtn);
            }
            timeSlotGrid.getChildren().add(row);
        }
    }

    private void handleSlotSelection(LocalDate date, int hour) {
        selectedDateTime = date.atTime(hour, 0);
        if (selectedDateLabel != null) {
            selectedDateLabel.setText(dateTimeFormatter.format(selectedDateTime));
        }
        if (selectedSlotLabel != null) {
            selectedSlotLabel.setText(String.format("%02d:00", hour));
        }
    }

    private void loadDoctorCards() {
        if (doctorCardsContainer == null) {
            return;
        }

        doctorCardsContainer.getChildren().clear();
        List<User> doctors = userService.getStaffByRoleAndType("STAFF", "RESP_PATIENTS");

        if (doctors.isEmpty()) {
            Label emptyLabel = new Label("No staff found for role STAFF and type RESP_PATIENTS.");
            emptyLabel.getStyleClass().add("helper-text");
            doctorCardsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (User doctor : doctors) {
            doctorCardsContainer.getChildren().add(createDoctorCard(doctor));
        }
    }

    private HBox createDoctorCard(User doctor) {
        HBox card = new HBox(12);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.getStyleClass().add("doctor-card");

        VBox imagePlaceholder = new VBox();
        imagePlaceholder.setPrefHeight(56);
        imagePlaceholder.setPrefWidth(75);
        imagePlaceholder.getStyleClass().add("doctor-image-placeholder");

        Label avatarLabel = new Label("DR");
        avatarLabel.getStyleClass().add("doctor-avatar-label");
        imagePlaceholder.getChildren().add(avatarLabel);

        VBox details = new VBox(2);
        Label nameLabel = new Label(buildDoctorDisplayName(doctor));
        nameLabel.getStyleClass().add("doctor-name-label");

        Label specialityLabel = new Label(valueOrDash(doctor.getTypeStaff()));
        specialityLabel.getStyleClass().add("doctor-speciality-label");

        HBox starsBox = new HBox(2);
        Label starsLabel = new Label("★ ★ ★ ★ ★");
        starsLabel.getStyleClass().add("doctor-stars-label");
        starsBox.getChildren().add(starsLabel);

        Label availabilityLabel = new Label("Available");
        availabilityLabel.getStyleClass().add("doctor-available-label");

        details.getChildren().addAll(nameLabel, specialityLabel, starsBox, availabilityLabel);

        Button selectBtn = new Button("Select");
        selectBtn.getStyleClass().add("doctor-select-button");
        selectBtn.setOnAction(e -> handleSelectDoctor(doctor));

        card.getChildren().addAll(imagePlaceholder, details, selectBtn);
        return card;
    }

    private void handleSelectDoctor(User doctor) {
        if (doctorField != null) {
            doctorField.setText(buildDoctorDisplayName(doctor));
        }
        if (doctorField1 != null) {
            doctorField1.setText(String.valueOf(doctor.getId()));
        }
    }

    private String buildDoctorDisplayName(User doctor) {
        String prenom = doctor.getPrenom() == null ? "" : doctor.getPrenom().trim();
        String nom = doctor.getNom() == null ? "" : doctor.getNom().trim();
        String fullName = (prenom + " " + nom).trim();
        if (fullName.isBlank()) {
            return "Staff #" + doctor.getId();
        }
        return "Dr. " + fullName;
    }

    private void setupMyBookingsTable() {
        if (myBookingsTable == null) {
            return;
        }

        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));
        colDateTime.setCellValueFactory(data -> {
            Timestamp ts = data.getValue().getDatetime();
            String formatted = ts == null ? "-" : dateTimeFormatter.format(ts.toLocalDateTime());
            return new SimpleStringProperty(formatted);
        });
        colMode.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getMode())));
        colMotif.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getMotif())));
        colStaff.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getIdStaff()));
        colStatut.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getStatut())));

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button modifyBtn = new Button("Modify");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(8, modifyBtn, deleteBtn);

            {
                modifyBtn.getStyleClass().add("table-modify-button");
                deleteBtn.getStyleClass().add("table-delete-button");

                modifyBtn.setOnAction(e -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    startEditing(rdv);
                });

                deleteBtn.setOnAction(e -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    rendezVousService.supprimer(rdv);
                    loadMyBookings();
                    showInfo("Deleted", "Appointment deleted successfully.");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        myBookingsTable.setItems(myBookingsData);
        myBookingsTable.setPlaceholder(new Label("No appointments found for this patient."));
    }

    private void loadMyBookings() {
        if (myBookingsTable == null) {
            return;
        }
        List<RendezVous> all = rendezVousService.recuperer();
        allMyBookingsData.setAll(all.stream().filter(r -> r.getIdPatient() == SESSION_PATIENT_ID).toList());
        applyMyBookingsFilterAndSort();
        loadMyRecords();
    }

    private void setupMyRecordsTable() {
        if (myRecordsTable == null) {
            return;
        }

        recordColId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));
        recordColRdvId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getRendez_vous_id() == null ? 0 : data.getValue().getRendez_vous_id()));
        recordColDateTime.setCellValueFactory(data -> {
            Timestamp ts = findRendezVousTimestamp(data.getValue().getRendez_vous_id());
            String formatted = ts == null ? "-" : dateTimeFormatter.format(ts.toLocalDateTime());
            return new SimpleStringProperty(formatted);
        });
        recordColDiagnostic.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getDiagnostic())));
        recordColObservations.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getObservations())));
        recordColResults.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getResultats_examens())));
        recordColDuration.setCellValueFactory(data -> {
            Integer duration = data.getValue().getDuree_minutes();
            return new SimpleStringProperty(duration == null ? "-" : duration + " min");
        });
        recordColSignature.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getSignature())));

        recordColActions.setCellFactory(col -> new TableCell<>() {
            private final Button toggleBtn = new Button("▼");

            {
                toggleBtn.getStyleClass().add("table-modify-button");
                toggleBtn.setOnAction(e -> {
                    FicheMedicale ficheMedicale = getTableView().getItems().get(getIndex());
                    toggleRecordPrescriptions(ficheMedicale);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                FicheMedicale ficheMedicale = getTableView().getItems().get(getIndex());
                boolean expanded = ficheMedicale != null && expandedRecordState.getOrDefault(ficheMedicale.getId(), false);
                toggleBtn.setText(expanded ? "▲" : "▼");
                setGraphic(toggleBtn);
            }
        });

        myRecordsTable.setItems(myRecordsData);
        myRecordsTable.setPlaceholder(new Label("No medical records found for this patient."));
    }

    private void setupRecordPrescriptionsTable() {
        if (recordPrescriptionsTable == null) {
            return;
        }

        recordPrescriptionsTable.setItems(recordPrescriptionsData);
        recordPrescriptionNomColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getNom_medicament())));
        recordPrescriptionDoseColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getDose())));
        recordPrescriptionFrequenceColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getFrequence())));
        recordPrescriptionInstructionsColumn.setCellValueFactory(data -> new SimpleStringProperty(valueOrDash(data.getValue().getInstructions())));
        recordPrescriptionsTable.setPlaceholder(new Label("No prescriptions found."));
    }

    private void loadMyRecords() {
        if (myRecordsTable == null) {
            return;
        }

        Set<Integer> patientRendezVousIds = new HashSet<>();
        for (RendezVous rendezVous : allMyBookingsData) {
            patientRendezVousIds.add(rendezVous.getId());
        }

        List<FicheMedicale> allRecords = ficheMedicaleService.recuperer();
        List<FicheMedicale> filtered = new ArrayList<>();
        for (FicheMedicale ficheMedicale : allRecords) {
            Integer rendezVousId = ficheMedicale.getRendez_vous_id();
            if (rendezVousId != null && patientRendezVousIds.contains(rendezVousId)) {
                filtered.add(ficheMedicale);
            }
        }

        myRecordsData.setAll(filtered);
        applyMyRecordsFilterAndSort();
    }

    private void showRecordPrescriptions(FicheMedicale ficheMedicale) {
        if (ficheMedicale == null) {
            return;
        }

        List<Prescription> prescriptions = prescriptionService.getByFicheMedicaleId(ficheMedicale.getId());
        recordPrescriptionsData.setAll(prescriptions);

        if (recordDetailsSummaryLabel != null) {
            recordDetailsSummaryLabel.setText("Fiche #" + ficheMedicale.getId() + " linked to rendez-vous #" + valueOrDash(String.valueOf(ficheMedicale.getRendez_vous_id())));
        }

        showRecordDetailsPanel(!recordPrescriptionsData.isEmpty());
    }

    private void toggleRecordPrescriptions(FicheMedicale ficheMedicale) {
        if (ficheMedicale == null) {
            return;
        }

        boolean currentlyExpanded = expandedRecordState.getOrDefault(ficheMedicale.getId(), false);
        if (currentlyExpanded) {
            collapseRecordDetailsPanel(ficheMedicale.getId());
            expandedRecordState.put(ficheMedicale.getId(), false);
            return;
        }

        expandedRecordState.replaceAll((key, value) -> false);
        expandedRecordState.put(ficheMedicale.getId(), true);
        showRecordPrescriptions(ficheMedicale);
        animateRecordPanelOpen();
    }

    private void showRecordDetailsPanel(boolean hasPrescriptions) {
        if (recordDetailsPanel != null) {
            recordDetailsPanel.setVisible(true);
            recordDetailsPanel.setManaged(true);
        }
        if (recordPrescriptionsTable != null) {
            recordPrescriptionsTable.setVisible(hasPrescriptions);
            recordPrescriptionsTable.setManaged(hasPrescriptions);
        }
        if (recordNoPrescriptionsLabel != null) {
            recordNoPrescriptionsLabel.setVisible(!hasPrescriptions);
            recordNoPrescriptionsLabel.setManaged(!hasPrescriptions);
        }
    }

    private void collapseRecordDetailsPanel(int ficheId) {
        if (recordDetailsPanel == null) {
            return;
        }

        Timeline collapseTimeline = new Timeline(
                new KeyFrame(Duration.millis(180), e -> {
                    recordDetailsPanel.setVisible(false);
                    recordDetailsPanel.setManaged(false);
                    recordPrescriptionsData.clear();
                })
        );
        collapseTimeline.play();
    }

    private void animateRecordPanelOpen() {
        if (recordDetailsPanel == null) {
            return;
        }

        recordDetailsPanel.setOpacity(0.0);
        recordDetailsPanel.setVisible(true);
        recordDetailsPanel.setManaged(true);

        Timeline openTimeline = new Timeline(
                new KeyFrame(Duration.millis(1), e -> recordDetailsPanel.setOpacity(0.0)),
                new KeyFrame(Duration.millis(220), e -> recordDetailsPanel.setOpacity(1.0))
        );
        openTimeline.play();
    }

    private void applyMyRecordsFilterAndSort() {
        String query = myRecordsSearchField == null || myRecordsSearchField.getText() == null
                ? ""
                : myRecordsSearchField.getText().trim().toLowerCase();

        List<FicheMedicale> filtered = new ArrayList<>();
        for (FicheMedicale ficheMedicale : myRecordsData) {
            if (matchesRecordSearch(ficheMedicale, query)) {
                filtered.add(ficheMedicale);
            }
        }

        if (myRecordsSortComboBox != null) {
            String selectedSort = myRecordsSortComboBox.getValue();
            if ("Date ASC".equals(selectedSort)) {
                filtered.sort(Comparator.comparing(this::recordTimestampForSort, Comparator.nullsLast(Comparator.naturalOrder())));
            } else if ("Date DESC".equals(selectedSort)) {
                filtered.sort(Comparator.comparing(this::recordTimestampForSort, Comparator.nullsLast(Comparator.reverseOrder())));
            } else if ("Diagnostic A-Z".equals(selectedSort)) {
                filtered.sort(Comparator.comparing(f -> valueOrDash(f.getDiagnostic()).toLowerCase()));
            } else if ("Diagnostic Z-A".equals(selectedSort)) {
                filtered.sort(Comparator.comparing((FicheMedicale f) -> valueOrDash(f.getDiagnostic()).toLowerCase()).reversed());
            }
        }

        myRecordsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private boolean matchesRecordSearch(FicheMedicale ficheMedicale, String query) {
        if (ficheMedicale == null) {
            return false;
        }
        if (query == null || query.isBlank()) {
            return true;
        }

        String id = String.valueOf(ficheMedicale.getId());
        String rdvId = ficheMedicale.getRendez_vous_id() == null ? "" : String.valueOf(ficheMedicale.getRendez_vous_id());
        String dateTime = recordTimestampForSort(ficheMedicale) == null ? "" : dateTimeFormatter.format(recordTimestampForSort(ficheMedicale).toLocalDateTime()).toLowerCase();
        String diagnostic = valueOrDash(ficheMedicale.getDiagnostic()).toLowerCase();
        String observations = valueOrDash(ficheMedicale.getObservations()).toLowerCase();
        String results = valueOrDash(ficheMedicale.getResultats_examens()).toLowerCase();
        String signature = valueOrDash(ficheMedicale.getSignature()).toLowerCase();

        return id.contains(query)
                || rdvId.contains(query)
                || dateTime.contains(query)
                || diagnostic.contains(query)
                || observations.contains(query)
                || results.contains(query)
                || signature.contains(query);
    }

    private Timestamp recordTimestampForSort(FicheMedicale ficheMedicale) {
        if (ficheMedicale == null) {
            return null;
        }

        Timestamp createdAt = ficheMedicale.getCreated_at();
        if (createdAt != null) {
            return createdAt;
        }

        return findRendezVousTimestamp(ficheMedicale.getRendez_vous_id());
    }

    private Timestamp findRendezVousTimestamp(Integer rendezVousId) {
        if (rendezVousId == null) {
            return null;
        }

        for (RendezVous rendezVous : allMyBookingsData) {
            if (rendezVous != null && rendezVous.getId() == rendezVousId) {
                return rendezVous.getDatetime();
            }
        }
        return null;
    }

    private void applyMyBookingsFilterAndSort() {
        String query = myBookingsSearchField == null || myBookingsSearchField.getText() == null
                ? ""
                : myBookingsSearchField.getText().trim().toLowerCase();

        List<RendezVous> filtered = new ArrayList<>();
        for (RendezVous rendezVous : allMyBookingsData) {
            if (matchesBookingSearch(rendezVous, query)) {
                filtered.add(rendezVous);
            }
        }

        if (myBookingsSortComboBox != null) {
            String selectedSort = myBookingsSortComboBox.getValue();
            if ("Date ASC".equals(selectedSort)) {
                filtered.sort(Comparator.comparing(RendezVous::getDatetime, Comparator.nullsLast(Comparator.naturalOrder())));
            } else if ("Date DESC".equals(selectedSort)) {
                filtered.sort(Comparator.comparing(RendezVous::getDatetime, Comparator.nullsLast(Comparator.reverseOrder())));
            } else if ("Statut: Demande".equals(selectedSort)) {
                filtered.sort(Comparator.comparingInt(r -> "Demande".equalsIgnoreCase(valueOrDash(r.getStatut())) ? 0 : 1));
            } else if ("Statut: Confirmé".equals(selectedSort)) {
                filtered.sort(Comparator.comparingInt(r -> "Confirmé".equalsIgnoreCase(valueOrDash(r.getStatut())) ? 0 : 1));
            } else if ("Statut: Terminé".equals(selectedSort)) {
                filtered.sort(Comparator.comparingInt(r -> "Terminé".equalsIgnoreCase(valueOrDash(r.getStatut())) ? 0 : 1));
            }
        }

        myBookingsData.setAll(filtered);
    }

    private boolean matchesBookingSearch(RendezVous rdv, String query) {
        if (rdv == null) {
            return false;
        }
        if (query == null || query.isBlank()) {
            return true;
        }

        String dateTime = rdv.getDatetime() == null ? "" : rdv.getDatetime().toString().toLowerCase();
        String mode = valueOrDash(rdv.getMode()).toLowerCase();
        String motif = valueOrDash(rdv.getMotif()).toLowerCase();
        String statut = valueOrDash(rdv.getStatut()).toLowerCase();
        String staff = String.valueOf(rdv.getIdStaff());
        String id = String.valueOf(rdv.getId());

        return id.contains(query)
                || dateTime.contains(query)
                || mode.contains(query)
                || motif.contains(query)
                || statut.contains(query)
                || staff.contains(query);
    }

    private void startEditing(RendezVous rdv) {
        editingRendezVousId = rdv.getId();

        if (modifyPatientIdField != null) {
            modifyPatientIdField.setText(String.valueOf(SESSION_PATIENT_ID));
        }
        if (modifyStaffIdField != null) {
            modifyStaffIdField.setText(String.valueOf(rdv.getIdStaff()));
        }
        if (modifyModeComboBox != null) {
            modifyModeComboBox.setValue(rdv.getMode());
        }
        if (modifyMotifArea != null) {
            modifyMotifArea.setText(rdv.getMotif());
        }
        if (modifyDateTimeField != null && rdv.getDatetime() != null) {
            modifyDateTimeField.setText(modifyDateTimeFormatter.format(rdv.getDatetime().toLocalDateTime()));
        }

        showModifyForm();
    }

    private void showModifyForm() {
        if (modifyFormCard != null) {
            modifyFormCard.setManaged(true);
            modifyFormCard.setVisible(true);
        }
    }

    private void hideModifyForm() {
        if (modifyFormCard != null) {
            modifyFormCard.setManaged(false);
            modifyFormCard.setVisible(false);
        }
    }

    private void clearModifyForm() {
        if (modifyStaffIdField != null) {
            modifyStaffIdField.clear();
        }
        if (modifyModeComboBox != null) {
            modifyModeComboBox.setValue(null);
        }
        if (modifyDateTimeField != null) {
            modifyDateTimeField.clear();
        }
        if (modifyMotifArea != null) {
            modifyMotifArea.clear();
        }
    }

    private int parseRequiredInt(TextField field, String fieldName) {
        String raw = field != null ? field.getText() : null;
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }

    private boolean hasDuplicateRendezVous(Integer excludedId, Timestamp datetime, int patientId, int staffId, String mode, String motif) {
        List<RendezVous> all = rendezVousService.recuperer();
        for (RendezVous existing : all) {
            if (existing == null) {
                continue;
            }
            if (excludedId != null && existing.getId() == excludedId) {
                continue;
            }

            boolean sameDate = existing.getDatetime() != null && existing.getDatetime().equals(datetime);
            boolean samePatient = existing.getIdPatient() == patientId;
            boolean sameStaff = existing.getIdStaff() == staffId;
            boolean sameMode = equalsIgnoreCaseTrim(existing.getMode(), mode);
            boolean sameMotif = equalsIgnoreCaseTrim(existing.getMotif(), motif);

            if (sameDate && samePatient && sameStaff && sameMode && sameMotif) {
                return true;
            }
        }
        return false;
    }

    private boolean equalsIgnoreCaseTrim(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

