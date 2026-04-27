
package tn.esprit.controllers;

import javafx.application.Platform;
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
import javafx.scene.control.Control;
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
import tn.esprit.services.DialogflowAssistantService;
import tn.esprit.services.HuggingFaceUrgencyService;
import tn.esprit.services.SpeechToTextService;
import tn.esprit.services.FicheMedicaleService;
import tn.esprit.services.PrescriptionService;
import tn.esprit.services.RendezVousService;
import tn.esprit.services.UserService;
import tn.esprit.tools.SessionManager;

import java.net.URL;
import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.util.Duration;

public class ConsultationController implements Initializable {
    private Integer sessionPatientId;

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
    private Label appointmentDateValidationLabel;
    @FXML
    private Label patientIdValidationLabel;
    @FXML
    private Label doctorIdValidationLabel;
    @FXML
    private Label modeValidationLabel;
    @FXML
    private Label modeFieldValidationLabel;
    @FXML
    private Label motifValidationLabel;

    // Modify form validation labels
    @FXML
    private Label modifyStaffIdValidationLabel;
    @FXML
    private Label modifyModeValidationLabel;
    @FXML
    private Label modifyDateTimeValidationLabel;
    @FXML
    private Label modifyMotifValidationLabel;

    @FXML
    private VBox bookingPage;
    @FXML
    private VBox myBookingsPage;
    @FXML
    private VBox myRecordsPage;
    @FXML
    private VBox aiAssistantPage;
    @FXML
    private Button bookingPageBtn;
    @FXML
    private Button myBookingsPageBtn;
    @FXML
    private Button myRecordsPageBtn;
    @FXML
    private Button aiAssistantPageBtn;
    @FXML
    private VBox aiConversationHistory;
    @FXML
    private TextField aiMessageInput;
    @FXML
    private Label aiAssistantStatusLabel;
    @FXML
    private Button aiSendMessageButton;
    @FXML
    private Button aiVoiceMessageButton;
    @FXML
    private Label aiRecordingIndicator;

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
    private final Set<LocalDateTime> selectedDoctorReservedSlots = new HashSet<>();
    private final RendezVousService rendezVousService = new RendezVousService();
    private final UserService userService = new UserService();
    private final FicheMedicaleService ficheMedicaleService = new FicheMedicaleService();
    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final HuggingFaceUrgencyService urgencyService = new HuggingFaceUrgencyService();
    private final DialogflowAssistantService dialogflowAssistantService = new DialogflowAssistantService();
    private final SpeechToTextService speechToTextService = new SpeechToTextService();
    private volatile boolean isRecordingAudio = false;
    private Timeline recordingAutoStopTimeline;
    private static final int RECORDING_AUTO_STOP_SECONDS = 15;

    private LocalDate currentWeekStart;
    private LocalDateTime selectedDateTime;
    private Integer selectedDoctorId;

    private final DateTimeFormatter weekRangeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("EEE dd MMM yyyy 'at' HH:mm", Locale.ENGLISH);
    private final DateTimeFormatter modifyDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern ISO_DATE_TIME_EMBEDDED_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}(?::\\d{2})?(?:Z|[+-]\\d{2}:\\d{2})?)");
    private static final Pattern DATE_TIME_AM_PM_PATTERN = Pattern.compile("(?i)(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{1,2})(?::(\\d{2}))?\\s*([ap]m)\\b");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sessionPatientId = resolveSessionPatientId();

        if (modeComboBox != null) {
            modeComboBox.setItems(FXCollections.observableArrayList("Distanciel", "Présentiel"));
        }
        if (modifyModeComboBox != null) {
            modifyModeComboBox.setItems(FXCollections.observableArrayList("Distanciel", "Présentiel"));
        }

        if (patientIdField != null) {
            patientIdField.setText(sessionPatientId == null ? "" : String.valueOf(sessionPatientId));
            patientIdField.setEditable(false);
        }
        if (modifyPatientIdField != null) {
            modifyPatientIdField.setText(sessionPatientId == null ? "" : String.valueOf(sessionPatientId));
            modifyPatientIdField.setEditable(false);
        }

        if (doctorField1 != null) {
            doctorField1.textProperty().addListener((obs, oldValue, newValue) -> {
                syncSelectedDoctorFromField();
                validateDoctorStaffField();
                validateAppointmentSelection();
            });
        }

        if (modeComboBox != null) {
            modeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
                updateModeSummary();
                validateModeSelection();
            });
        }

        if (motifArea != null) {
            motifArea.textProperty().addListener((obs, oldValue, newValue) -> validateMotif());
        }

        // Modify form listeners
        if (modifyStaffIdField != null) {
            modifyStaffIdField.textProperty().addListener((obs, oldValue, newValue) -> validateModifyStaffField());
        }

        if (modifyModeComboBox != null) {
            modifyModeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> validateModifyMode());
        }

        if (modifyMotifArea != null) {
            modifyMotifArea.textProperty().addListener((obs, oldValue, newValue) -> validateModifyMotif());
        }

        if (modifyDateTimeField != null) {
            modifyDateTimeField.textProperty().addListener((obs, oldValue, newValue) -> validateModifyDateTime());
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
        validateAllBookingInputs();
        initializeAiAssistantConversation();
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
    private void handleShowAiAssistantPage(ActionEvent event) {
        showAiAssistantPage();
    }

    @FXML
    private void handleSendAiMessage(ActionEvent event) {
        sendAiMessageFromComposer();
    }

    @FXML
    private void handleAiVoiceMessage(ActionEvent event) {
        // Toggle recording: first press starts, second press stops and transcribes
        if (!isRecordingAudio) {
            try {
                speechToTextService.startRecording();
                isRecordingAudio = true;
                setAiAssistantStatus("Recording... click again to stop");
                if (aiVoiceMessageButton != null) {
                    aiVoiceMessageButton.setText("Stop");
                }
                if (aiRecordingIndicator != null) {
                    aiRecordingIndicator.setVisible(true);
                }

                // schedule auto-stop
                if (recordingAutoStopTimeline != null) {
                    recordingAutoStopTimeline.stop();
                }
                recordingAutoStopTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(RECORDING_AUTO_STOP_SECONDS), e -> stopRecordingFromAuto()));
                recordingAutoStopTimeline.setCycleCount(1);
                recordingAutoStopTimeline.play();
            } catch (LineUnavailableException ex) {
                showError("Audio error", "Microphone is unavailable: " + ex.getMessage());
            }
            return;
        }

        // stop recording and transcribe asynchronously
        // manual stop
        if (recordingAutoStopTimeline != null) {
            recordingAutoStopTimeline.stop();
            recordingAutoStopTimeline = null;
        }
        isRecordingAudio = false;
        setAiAssistantStatus("Processing audio...");
        if (aiVoiceMessageButton != null) {
            aiVoiceMessageButton.setText("Voice");
        }
        if (aiRecordingIndicator != null) {
            aiRecordingIndicator.setVisible(false);
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return speechToTextService.stopRecordingAndTranscribe();
            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }).whenComplete((transcript, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                setAiAssistantStatus("Audio processing failed");
                appendAiAssistantMessage("Voice transcription failed: " + throwable.getMessage(), false);
                return;
            }

            if (transcript == null || transcript.isBlank()) {
                setAiAssistantStatus("No speech detected");
                appendAiAssistantMessage("I could not detect speech. Try again.", false);
                return;
            }

            setAiAssistantStatus("You: " + (transcript.length() > 40 ? transcript.substring(0, 40) + "..." : transcript));
            appendAiAssistantMessage(transcript, true);
            // send transcript to Dialogflow and handle reply
            setAiAssistantControlsDisabled(true);
            setAiAssistantStatus("Sending to Dialogflow...");
            CompletableFuture.supplyAsync(() -> dialogflowAssistantService.detectIntent(transcript, buildDialogflowSessionId()))
                    .whenComplete((reply, ex) -> Platform.runLater(() -> {
                        setAiAssistantControlsDisabled(false);
                        if (ex != null) {
                            setAiAssistantStatus("Dialogflow unavailable");
                            appendAiAssistantMessage("I could not reach Dialogflow. Check the service account path and project ID.", false);
                            showError("Dialogflow error", ex.getMessage());
                            return;
                        }
                        handleDialogflowReply(transcript, reply);
                    }));
        }));
    }

    private void stopRecordingFromAuto() {
        // called on JavaFX thread by timeline
        if (!isRecordingAudio) return;
        // reuse existing stop flow
        handleAiVoiceMessage(new ActionEvent());
    }

    @FXML
    private void handleModeSelection(ActionEvent event) {
        updateModeSummary();
        validateModeSelection();
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
            int patientId = requireSessionPatientId();
            int idStaff = parseRequiredInt(doctorField1, "Doctor/Staff ID");
            String mode = modeComboBox != null ? modeComboBox.getValue() : null;
            String motif = motifArea != null ? motifArea.getText() : null;

            saveAppointment(patientId, idStaff, selectedDateTime, mode, motif);
            reloadReservedSlotsForSelectedDoctor();
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
            int patientId = requireSessionPatientId();

            if (editingRendezVousId == null) {
                throw new IllegalArgumentException("No booking selected for modification.");
            }

            // Run live validation first
            if (!validateModifyAllInputs()) {
                throw new IllegalArgumentException("Please fix the highlighted fields before confirming.");
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
            String urgencyLevel = determineUrgencyLevel(motif);

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

            if (hasDuplicateRendezVous(editingRendezVousId, Timestamp.valueOf(dateTime), patientId, idStaff, mode, motif)) {
                throw new IllegalArgumentException("Another booking with the same date/time, doctor, mode and motif already exists.");
            }

            RendezVous rendezVous = new RendezVous(
                    editingRendezVousId,
                    Timestamp.valueOf(dateTime),
                    "Demande",
                    mode,
                    motif,
                    new Timestamp(System.currentTimeMillis()),
                        patientId,
                    idStaff,
                    urgencyLevel
            );

            rendezVousService.modifier(rendezVous);
            reloadReservedSlotsForSelectedDoctor();
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

    private void saveAppointment(int patientId, int idStaff, LocalDateTime dateTime, String mode, String motif) {
        if (dateTime == null) {
            throw new IllegalArgumentException("Please select a date and time slot from the calendar.");
        }
        if (mode == null || mode.isBlank()) {
            throw new IllegalArgumentException("Please choose a booking mode.");
        }
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Please enter a motif.");
        }

        String cleanedMode = mode.trim();
        String cleanedMotif = motif.trim();

        if (!validateAllBookingInputs()) {
            throw new IllegalArgumentException("Please fix the highlighted fields before confirming the appointment.");
        }

        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The appointment date/time must be in the future.");
        }

        if (hasDuplicateRendezVous(null, Timestamp.valueOf(dateTime), patientId, idStaff, cleanedMode, cleanedMotif)) {
            throw new IllegalArgumentException("A booking with the same date/time, doctor, mode and motif already exists.");
        }

        String urgencyLevel = determineUrgencyLevel(cleanedMotif);
        RendezVous rendezVous = new RendezVous(
                Timestamp.valueOf(dateTime),
                "Demande",
                cleanedMode,
                cleanedMotif,
                new Timestamp(System.currentTimeMillis()),
                patientId,
                idStaff,
                urgencyLevel
        );

        rendezVousService.ajouter(rendezVous);
    }

    private void initializeAiAssistantConversation() {
        if (aiConversationHistory == null || !aiConversationHistory.getChildren().isEmpty()) {
            return;
        }

        appendAiAssistantMessage("Welcome. Type a RendezVous phrase or use the booking intent, and Dialogflow will extract the fields.", false);
        appendAiAssistantMessage("Example: I want to book a Presentiel appointment with Doctor Ahmed Chrigui for 11 October 2024 at 8 am.", false);
        setAiAssistantStatus("Ready for Dialogflow intent detection");
    }

    private void sendAiMessageFromComposer() {
        if (aiMessageInput == null) {
            return;
        }

        String userMessage = aiMessageInput.getText() == null ? "" : aiMessageInput.getText().trim();
        if (userMessage.isBlank()) {
            showInfo("AI Assistant", "Type a message first.");
            return;
        }

        appendAiAssistantMessage(userMessage, true);
        aiMessageInput.clear();
        setAiAssistantStatus("Sending to Dialogflow...");
        setAiAssistantControlsDisabled(true);

        CompletableFuture
                .supplyAsync(() -> dialogflowAssistantService.detectIntent(userMessage, buildDialogflowSessionId()))
                .whenComplete((reply, throwable) -> Platform.runLater(() -> {
                    setAiAssistantControlsDisabled(false);

                    if (throwable != null) {
                        setAiAssistantStatus("Dialogflow unavailable");
                        appendAiAssistantMessage("I could not reach Dialogflow. Check the service account path and project ID.", false);
                        showError("Dialogflow error", throwable.getMessage());
                        return;
                    }

                    handleDialogflowReply(userMessage, reply);
                }));
    }

    private void handleDialogflowReply(String userMessage, DialogflowAssistantService.DialogflowReply reply) {
        if (reply == null) {
            setAiAssistantStatus("No Dialogflow reply");
            appendAiAssistantMessage("Dialogflow returned an empty response.", false);
            return;
        }

        String intentName = reply.intentName();
        if (intentName != null && !intentName.isBlank()) {
            setAiAssistantStatus("Intent detected: " + intentName);
        } else {
            setAiAssistantStatus("Dialogflow reply received");
        }

        String fulfillmentText = reply.fulfillmentText();
        if (fulfillmentText != null && !fulfillmentText.isBlank()) {
            appendAiAssistantMessage(fulfillmentText, false);
        }

        if ("RendezVous".equalsIgnoreCase(intentName)) {
            processRendezVousIntent(userMessage, reply.parameters());
        } else if ("SayHello".equalsIgnoreCase(intentName)) {
            handleSayHelloIntent();
        } else if ("RemindUpcomingRendezVous".equalsIgnoreCase(intentName)) {
            handleRemindUpcomingRendezVousIntent();
        } else if ((fulfillmentText == null || fulfillmentText.isBlank()) && intentName != null && !intentName.isBlank()) {
            appendAiAssistantMessage("Intent detected: " + intentName + ".", false);
        }
    }

    private void handleSayHelloIntent() {
        appendAiAssistantMessage("Hello. I can help you book an appointment or remind you of your upcoming rendez-vous.", false);
    }

    private void handleRemindUpcomingRendezVousIntent() {
        Integer patientId = resolveSessionPatientId();
        if (patientId == null || patientId <= 0) {
            appendAiAssistantMessage("I could not find your active session. Please log in again to fetch your upcoming appointments.", false);
            return;
        }

        List<RendezVous> allAppointments = rendezVousService.recuperer();
        List<RendezVous> upcomingAppointments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (RendezVous appointment : allAppointments) {
            if (appointment == null || appointment.getDatetime() == null) {
                continue;
            }
            if (appointment.getIdPatient() != patientId) {
                continue;
            }

            LocalDateTime appointmentDateTime = appointment.getDatetime().toLocalDateTime();
            if (appointmentDateTime.isBefore(now)) {
                continue;
            }

            upcomingAppointments.add(appointment);
        }

        upcomingAppointments.sort(Comparator.comparing(rdv -> rdv.getDatetime().toLocalDateTime()));

        if (upcomingAppointments.isEmpty()) {
            appendAiAssistantMessage("You have no upcoming appointments.", false);
            return;
        }

        StringBuilder message = new StringBuilder("Here are your upcoming appointments:\n");
        int limit = Math.min(3, upcomingAppointments.size());
        for (int i = 0; i < limit; i++) {
            RendezVous rdv = upcomingAppointments.get(i);
            String dateText = dateTimeFormatter.format(rdv.getDatetime().toLocalDateTime());
            String mode = valueOrDash(rdv.getMode());
            String status = valueOrDash(rdv.getStatut());
            message.append("- ").append(dateText)
                    .append(" | mode: ").append(mode)
                    .append(" | status: ").append(status)
                    .append("\n");
        }

        if (upcomingAppointments.size() > limit) {
            message.append("And ").append(upcomingAppointments.size() - limit).append(" more...");
        }

        appendAiAssistantMessage(message.toString().trim(), false);
    }

    private void processRendezVousIntent(String userMessage, Map<String, String> parameters) {
        Map<String, String> safeParameters = parameters == null ? Map.of() : parameters;

        String extractedMode = normalizeMode(safeParameters.get("mode"));
        String extractedPerson = firstNonBlank(safeParameters.get("person"), safeParameters.get("doctor"), safeParameters.get("staff"));
        String extractedMotif = firstNonBlank(safeParameters.get("motif"), safeParameters.get("reason"), userMessage);
        String extractedDateTime = firstNonBlank(
            safeParameters.get("date-time"),
            safeParameters.get("date_time"),
            safeParameters.get("datetime"),
            findDateTimeValueFromAnyParameter(safeParameters)
        );

        if (extractedMode != null && modeComboBox != null) {
            modeComboBox.setValue(extractedMode);
            updateModeSummary();
        }

        if (extractedPerson != null) {
            Integer staffId = resolveDialogflowStaffId(extractedPerson);
            if (staffId != null && doctorField1 != null) {
                doctorField1.setText(String.valueOf(staffId));
                selectedDoctorId = staffId;
                validateDoctorStaffField();
            }
        }

        LocalDateTime parsedDateTime = parseDialogflowDateTime(extractedDateTime);
        if (parsedDateTime == null && !safeParameters.isEmpty()) {
            parsedDateTime = parseDialogflowDateTime(findDateTimeValueFromAnyParameter(safeParameters));
        }

        if (parsedDateTime != null) {
            applyAiSelectedDateTime(parsedDateTime);
        }

        if (extractedMotif != null && motifArea != null) {
            motifArea.setText(extractedMotif.trim());
            validateMotif();
        }

        if (canAutoBookRendezVous()) {
            try {
                confirmAppointmentFromAi();
                appendAiAssistantMessage("Your rendez-vous request was booked from the Dialogflow intent.", false);
                setAiAssistantStatus("RendezVous booked");
            } catch (IllegalArgumentException ex) {
                appendAiAssistantMessage(ex.getMessage(), false);
                setAiAssistantStatus("More details needed");
            } catch (Exception ex) {
                appendAiAssistantMessage("I detected the intent, but booking failed: " + ex.getMessage(), false);
                setAiAssistantStatus("Booking failed");
            }
            return;
        }

        List<String> missingFields = new ArrayList<>();
        if (doctorField1 == null || doctorField1.getText() == null || doctorField1.getText().isBlank()) {
            missingFields.add("doctor");
        }
        if (modeComboBox == null || modeComboBox.getValue() == null || modeComboBox.getValue().isBlank()) {
            missingFields.add("mode");
        }
        if (selectedDateTime == null) {
            missingFields.add("date/time");
        }
        if (motifArea == null || motifArea.getText() == null || motifArea.getText().isBlank()) {
            missingFields.add("motif");
        }

        if (missingFields.isEmpty()) {
            appendAiAssistantMessage("The intent was detected. I filled the form, but one of the values is still invalid.", false);
        } else {
            appendAiAssistantMessage("I detected RendezVous and filled what I could. Still missing: " + String.join(", ", missingFields) + ".", false);
        }
    }

    private void confirmAppointmentFromAi() {
        int patientId = requireSessionPatientId();
        int idStaff = parseRequiredInt(doctorField1, "Doctor/Staff ID");
        String mode = modeComboBox != null ? modeComboBox.getValue() : null;
        String motif = "Reservé par IA";
        if (motifArea != null) {
            motifArea.setText(motif);
            validateMotif();
        }
        saveAppointment(patientId, idStaff, selectedDateTime, mode, motif);
        reloadReservedSlotsForSelectedDoctor();
        loadMyBookings();
    }

    private void appendAiAssistantMessage(String message, boolean fromUser) {
        if (aiConversationHistory == null || message == null || message.isBlank()) {
            return;
        }

        HBox row = new HBox(12);
        row.getStyleClass().add(fromUser ? "ai-message-row-user" : "ai-message-row-ai");

        VBox messageColumn = new VBox(4);
        messageColumn.setFillWidth(true);
        HBox.setHgrow(messageColumn, Priority.ALWAYS);

        Label nameLabel = new Label(fromUser ? "You" : "AI Assistant");
        nameLabel.getStyleClass().add("ai-message-name");
        if (fromUser) {
            nameLabel.getStyleClass().add("ai-message-name-user");
        }

        Label bubble = new Label(message);
        bubble.setWrapText(true);
        bubble.getStyleClass().addAll("ai-bubble", fromUser ? "ai-bubble-user" : "ai-bubble-ai");
        bubble.setMaxWidth(640);

        if (fromUser) {
            messageColumn.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
            messageColumn.getChildren().addAll(nameLabel, bubble);
            row.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
            row.getChildren().addAll(messageColumn, createAiAvatar("Me", true));
        } else {
            messageColumn.getChildren().addAll(nameLabel, bubble);
            row.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            row.getChildren().addAll(createAiAvatar("AI", false), messageColumn);
        }

        aiConversationHistory.getChildren().add(row);
    }

    private VBox createAiAvatar(String text, boolean userAvatar) {
        Label avatarLabel = new Label(text);
        avatarLabel.getStyleClass().add("ai-avatar-text");

        VBox avatar = new VBox(avatarLabel);
        avatar.getStyleClass().addAll("ai-avatar", userAvatar ? "ai-avatar-user" : "ai-avatar-ai");
        avatar.setMinSize(42, 42);
        avatar.setPrefSize(42, 42);
        avatar.setMaxSize(42, 42);
        avatar.setAlignment(javafx.geometry.Pos.CENTER);
        return avatar;
    }

    private void setAiAssistantStatus(String message) {
        if (aiAssistantStatusLabel != null) {
            aiAssistantStatusLabel.setText(message);
        }
    }

    private void setAiAssistantControlsDisabled(boolean disabled) {
        if (aiSendMessageButton != null) {
            aiSendMessageButton.setDisable(disabled);
        }
        if (aiVoiceMessageButton != null) {
            aiVoiceMessageButton.setDisable(disabled);
        }
        if (aiMessageInput != null) {
            aiMessageInput.setDisable(disabled);
        }
    }

    private void applyAiSelectedDateTime(LocalDateTime dateTime) {
        selectedDateTime = dateTime;

        if (selectedDateLabel != null) {
            selectedDateLabel.setText(dateTime.format(DateTimeFormatter.ofPattern("EEE dd MMM yyyy", Locale.ENGLISH)));
        }
        if (selectedSlotLabel != null) {
            selectedSlotLabel.setText(dateTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)));
        }

        validateAppointmentSelection();
    }

    private boolean canAutoBookRendezVous() {
        return doctorField1 != null && doctorField1.getText() != null && !doctorField1.getText().isBlank()
                && modeComboBox != null && modeComboBox.getValue() != null && !modeComboBox.getValue().isBlank()
                && selectedDateTime != null
                && motifArea != null && motifArea.getText() != null && !motifArea.getText().isBlank();
    }

    private LocalDateTime parseDialogflowDateTime(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String trimmed = rawValue.trim();

        // Dialogflow can return nested JSON-like values. Extract an ISO token when embedded.
        Matcher embeddedMatcher = ISO_DATE_TIME_EMBEDDED_PATTERN.matcher(trimmed);
        if (embeddedMatcher.find()) {
            trimmed = embeddedMatcher.group(1).replace(' ', 'T');
        }

        // Support natural forms like: 2026-04-28 10am / 2026-04-28 10:30 pm
        Matcher amPmMatcher = DATE_TIME_AM_PM_PATTERN.matcher(trimmed);
        if (amPmMatcher.find()) {
            try {
                LocalDate date = LocalDate.parse(amPmMatcher.group(1));
                int hour = Integer.parseInt(amPmMatcher.group(2));
                int minute = amPmMatcher.group(3) == null ? 0 : Integer.parseInt(amPmMatcher.group(3));
                String amPm = amPmMatcher.group(4).toLowerCase(Locale.ROOT);

                if (hour < 1 || hour > 12 || minute < 0 || minute > 59) {
                    return null;
                }

                int hour24 = hour % 12;
                if ("pm".equals(amPm)) {
                    hour24 += 12;
                }

                return date.atTime(hour24, minute);
            } catch (DateTimeParseException | NumberFormatException ignored) {
            }
        }

        try {
            return OffsetDateTime.parse(trimmed).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(trimmed, modifyDateTimeFormatter);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String findDateTimeValueFromAnyParameter(Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null || value.isBlank()) {
                continue;
            }

            String normalizedKey = normalizeSearchText(key);
            if (!normalizedKey.contains("date") && !normalizedKey.contains("time")) {
                continue;
            }

            if (parseDialogflowDateTime(value) != null) {
                return value;
            }
        }

        return null;
    }

    private String normalizeMode(String modeValue) {
        if (modeValue == null || modeValue.isBlank()) {
            return null;
        }

        String normalized = normalizeSearchText(modeValue);
        if (normalized.contains("present") || normalized.contains("onsite") || normalized.contains("in person")) {
            return "Présentiel";
        }
        if (normalized.contains("distanc") || normalized.contains("remote") || normalized.contains("online") || normalized.contains("virtual")) {
            return "Distanciel";
        }
        return modeValue.trim();
    }

    private Integer resolveDialogflowStaffId(String personValue) {
        if (personValue == null || personValue.isBlank()) {
            return null;
        }

        String trimmed = personValue.trim();
        try {
            int parsedId = Integer.parseInt(trimmed);
            return userService.findById(parsedId) == null ? null : parsedId;
        } catch (NumberFormatException ignored) {
        }

        String normalizedCandidate = normalizeSearchText(trimmed);
        List<User> doctors = userService.getStaffByRoleAndType("STAFF", "RESP_PATIENTS");
        for (User doctor : doctors) {
            String displayName = buildDoctorDisplayName(doctor);
            String normalizedDisplayName = normalizeSearchText(displayName);
            String normalizedRawName = normalizeSearchText((doctor.getPrenom() == null ? "" : doctor.getPrenom()) + " " + (doctor.getNom() == null ? "" : doctor.getNom()));
            if (normalizedDisplayName.contains(normalizedCandidate)
                    || normalizedRawName.contains(normalizedCandidate)
                    || normalizedCandidate.contains(normalizedRawName)) {
                return doctor.getId();
            }
        }

        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private String buildDialogflowSessionId() {
        Integer patientId = resolveSessionPatientId();
        if (patientId == null || patientId <= 0) {
            return "consultation-guest";
        }
        return "consultation-" + patientId;
    }

    private void showBookingPage() {
        setVisiblePage(bookingPage);
        updatePageNavigationState(bookingPageBtn);
    }

    private void showMyBookingsPage() {
        setVisiblePage(myBookingsPage);
        updatePageNavigationState(myBookingsPageBtn);

        loadMyBookings();
    }

    private void showMyRecordsPage() {
        setVisiblePage(myRecordsPage);
        updatePageNavigationState(myRecordsPageBtn);

        applyMyRecordsFilterAndSort();
    }

    private void showAiAssistantPage() {
        setVisiblePage(aiAssistantPage);
        updatePageNavigationState(aiAssistantPageBtn);
    }

    private void setVisiblePage(VBox visiblePage) {
        if (bookingPage != null) {
            boolean isVisible = bookingPage == visiblePage;
            bookingPage.setVisible(isVisible);
            bookingPage.setManaged(isVisible);
        }
        if (myBookingsPage != null) {
            boolean isVisible = myBookingsPage == visiblePage;
            myBookingsPage.setVisible(isVisible);
            myBookingsPage.setManaged(isVisible);
        }
        if (myRecordsPage != null) {
            boolean isVisible = myRecordsPage == visiblePage;
            myRecordsPage.setVisible(isVisible);
            myRecordsPage.setManaged(isVisible);
        }
        if (aiAssistantPage != null) {
            boolean isVisible = aiAssistantPage == visiblePage;
            aiAssistantPage.setVisible(isVisible);
            aiAssistantPage.setManaged(isVisible);
        }
    }

    private void updatePageNavigationState(Button activeButton) {
        Button[] pageButtons = {bookingPageBtn, myBookingsPageBtn, myRecordsPageBtn, aiAssistantPageBtn};
        for (Button button : pageButtons) {
            if (button == null) {
                continue;
            }
            boolean isActive = button == activeButton;
            button.getStyleClass().remove(isActive ? "page-nav-button" : "page-nav-button-active");
            String targetStyle = isActive ? "page-nav-button-active" : "page-nav-button";
            if (!button.getStyleClass().contains(targetStyle)) {
                button.getStyleClass().add(targetStyle);
            }
        }
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
                LocalDateTime slotDateTime = date.atTime(selectedHour, 0);
                boolean reserved = selectedDoctorReservedSlots.contains(slotDateTime);
                if (reserved) {
                    slotBtn.getStyleClass().add("calendar-slot-button-reserved");
                    slotBtn.setText("occupé(e)");
                    slotBtn.setDisable(true);
                }
                slotBtn.setOnAction(e -> handleSlotSelection(date, selectedHour));
                HBox.setHgrow(slotBtn, Priority.NEVER);
                row.getChildren().add(slotBtn);
            }
            timeSlotGrid.getChildren().add(row);
        }
    }

    private void handleSlotSelection(LocalDate date, int hour) {
        LocalDateTime candidate = date.atTime(hour, 0);
        if (selectedDoctorReservedSlots.contains(candidate)) {
            showError("Slot indisponible", "Ce creneau est deja reserve pour ce medecin.");
            return;
        }

        selectedDateTime = candidate;
        if (selectedDateLabel != null) {
            selectedDateLabel.setText(dateTimeFormatter.format(selectedDateTime));
        }
        if (selectedSlotLabel != null) {
            selectedSlotLabel.setText(String.format("%02d:00", hour));
        }
        validateAppointmentSelection();
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

    private void syncSelectedDoctorFromField() {
        if (doctorField1 == null) {
            return;
        }

        String raw = doctorField1.getText();
        if (raw == null || raw.isBlank()) {
            selectedDoctorId = null;
            selectedDoctorReservedSlots.clear();
            refreshCalendar();
            return;
        }

        try {
            int parsedId = Integer.parseInt(raw.trim());
            if (parsedId <= 0) {
                selectedDoctorId = null;
                selectedDoctorReservedSlots.clear();
                refreshCalendar();
                return;
            }

            if (selectedDoctorId == null || selectedDoctorId != parsedId) {
                selectedDoctorId = parsedId;
                reloadReservedSlotsForSelectedDoctor();
            }
        } catch (NumberFormatException ex) {
            selectedDoctorId = null;
            selectedDoctorReservedSlots.clear();
            refreshCalendar();
        }
    }

    private void reloadReservedSlotsForSelectedDoctor() {
        selectedDoctorReservedSlots.clear();

        if (selectedDoctorId == null || selectedDoctorId <= 0) {
            refreshCalendar();
            return;
        }

        List<RendezVous> doctorAppointments = rendezVousService.recupererParStaffId(selectedDoctorId);
        for (RendezVous rendezVous : doctorAppointments) {
            if (rendezVous == null || rendezVous.getDatetime() == null) {
                continue;
            }

            LocalDateTime slot = rendezVous.getDatetime().toLocalDateTime()
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
            selectedDoctorReservedSlots.add(slot);
        }

        if (selectedDateTime != null) {
            LocalDateTime selectedSlotHour = selectedDateTime
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
            if (selectedDoctorReservedSlots.contains(selectedSlotHour)) {
                selectedDateTime = null;
                if (selectedDateLabel != null) {
                    selectedDateLabel.setText("No date selected");
                }
                if (selectedSlotLabel != null) {
                    selectedSlotLabel.setText("No slot selected");
                }
            }
        }

        validateAppointmentSelection();

        refreshCalendar();
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
                if (empty) {
                    setGraphic(null);
                    return;
                }

                RendezVous rdv = getTableView().getItems().get(getIndex());
                boolean canEditOrDelete = rdv != null && "Demande".equalsIgnoreCase(valueOrDash(rdv.getStatut()));
                modifyBtn.setDisable(!canEditOrDelete);
                deleteBtn.setDisable(!canEditOrDelete);
                setGraphic(box);
            }
        });

        myBookingsTable.setItems(myBookingsData);
        myBookingsTable.setPlaceholder(new Label("No appointments found for this patient."));
    }

    private void loadMyBookings() {
        if (myBookingsTable == null) {
            return;
        }

        Integer patientId = resolveSessionPatientId();
        if (patientId == null || patientId <= 0) {
            allMyBookingsData.clear();
            myBookingsData.clear();
            loadMyRecords();
            return;
        }

        List<RendezVous> all = rendezVousService.recuperer();
        allMyBookingsData.setAll(all.stream().filter(r -> r.getIdPatient() == patientId).toList());
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
            Integer patientId = resolveSessionPatientId();
            modifyPatientIdField.setText(patientId == null ? "" : String.valueOf(patientId));
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
        validateModifyAllInputs(); // Show initial validation state
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

    private void updateModeSummary() {
        if (selectedModeLabel != null) {
            String selectedMode = modeComboBox != null ? modeComboBox.getValue() : null;
            selectedModeLabel.setText(selectedMode == null || selectedMode.isBlank() ? "No mode selected" : selectedMode);
        }
    }

    private boolean validateAllBookingInputs() {
        boolean valid = true;
        valid &= validatePatientIdField();
        valid &= validateDoctorStaffField();
        valid &= validateModeSelection();
        valid &= validateMotif();
        valid &= validateAppointmentSelection();
        return valid;
    }

    private boolean validatePatientIdField() {
        if (patientIdField == null) {
            return true;
        }

        Integer patientId = resolveSessionPatientId();
        if (patientId == null || patientId <= 0) {
            applyValidationState(patientIdField, patientIdValidationLabel, false, "No logged-in user found. Please log in again.");
            return false;
        }

        applyValidationState(patientIdField, patientIdValidationLabel, true, "Patient ID loaded: " + patientId + ".");
        return true;
    }

    private Integer resolveSessionPatientId() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || currentUser.getId() <= 0) {
            return null;
        }
        sessionPatientId = currentUser.getId();
        return sessionPatientId;
    }

    private int requireSessionPatientId() {
        Integer patientId = resolveSessionPatientId();
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("No logged-in user found. Please log in again.");
        }
        return patientId;
    }

    private boolean validateDoctorStaffField() {
        if (doctorField1 == null) {
            return true;
        }

        String raw = doctorField1.getText();
        if (raw == null || raw.isBlank()) {
            applyValidationState(doctorField1, doctorIdValidationLabel, false, "Doctor/Staff ID is required. Select a doctor card or enter a numeric ID.");
            return false;
        }

        int parsedId;
        try {
            parsedId = Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            applyValidationState(doctorField1, doctorIdValidationLabel, false, "Doctor/Staff ID must contain digits only.");
            return false;
        }

        if (parsedId <= 0) {
            applyValidationState(doctorField1, doctorIdValidationLabel, false, "Doctor/Staff ID must be greater than 0.");
            return false;
        }

        User doctor = userService.findById(parsedId);
        if (doctor == null) {
            applyValidationState(doctorField1, doctorIdValidationLabel, false, "No staff member was found with ID " + parsedId + ".");
            return false;
        }

        applyValidationState(doctorField1, doctorIdValidationLabel, true, "Doctor selected: " + buildDoctorDisplayName(doctor) + ".");
        return true;
    }

    private boolean validateModeSelection() {
        if (modeComboBox == null) {
            return true;
        }

        String selectedMode = modeComboBox.getValue();
        if (selectedMode == null || selectedMode.isBlank()) {
            applyValidationState(modeComboBox, modeValidationLabel, false, "Choose a booking mode: Distanciel or Présentiel.");
            if (modeFieldValidationLabel != null) {
                applyValidationState(null, modeFieldValidationLabel, false, modeValidationLabel.getText());
            }
            return false;
        }

        applyValidationState(modeComboBox, modeValidationLabel, true, "Mode selected: " + selectedMode + ".");
        if (modeFieldValidationLabel != null) {
            applyValidationState(null, modeFieldValidationLabel, true, modeValidationLabel.getText());
        }
        return true;
    }

    private boolean validateMotif() {
        if (motifArea == null) {
            return true;
        }

        String motif = motifArea.getText();
        if (motif == null || motif.isBlank()) {
            applyValidationState(motifArea, motifValidationLabel, false, "Motif is required. Explain why the consultation is needed.");
            return false;
        }

        String trimmed = motif.trim();
        if (trimmed.length() < 10) {
            applyValidationState(motifArea, motifValidationLabel, false, "Motif is too short. Write at least 10 characters.");
            return false;
        }

        if (trimmed.length() > 255) {
            applyValidationState(motifArea, motifValidationLabel, false, "Motif is too long. Keep it under 255 characters.");
            return false;
        }

        applyValidationState(motifArea, motifValidationLabel, true, "Motif looks valid.");
        return true;
    }

    private boolean validateAppointmentSelection() {
        if (appointmentDateValidationLabel == null) {
            return true;
        }

        if (selectedDateTime == null) {
            applyValidationState(null, appointmentDateValidationLabel, false, "Select a date and time slot from the calendar.");
            return false;
        }

        if (selectedDateTime.isBefore(LocalDateTime.now())) {
            applyValidationState(null, appointmentDateValidationLabel, false, "The selected slot is in the past. Choose a future date and time.");
            return false;
        }

        applyValidationState(null, appointmentDateValidationLabel, true, "Date and time selected: " + dateTimeFormatter.format(selectedDateTime) + ".");
        return true;
    }

    private void applyValidationState(Control field, Label messageLabel, boolean valid, String message) {
        if (field != null) {
            field.getStyleClass().removeAll("validation-success", "validation-error");
            field.getStyleClass().add(valid ? "validation-success" : "validation-error");
        }

        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.getStyleClass().removeAll("validation-message-error", "validation-message-success");
            messageLabel.getStyleClass().add(valid ? "validation-message-success" : "validation-message-error");
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

    private String determineUrgencyLevel(String motif) {
        String urgencyLevel = urgencyService.classifyUrgencyLevel(motif);
        if (urgencyLevel == null || urgencyLevel.isBlank()) {
            return "mid";
        }
        return urgencyLevel;
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

    // Modify form validation helpers
    private boolean validateModifyAllInputs() {
        return validateModifyStaffField() && validateModifyMode() && validateModifyDateTime() && validateModifyMotif();
    }

    private boolean validateModifyStaffField() {
        if (modifyStaffIdField == null) {
            return true;
        }

        String input = modifyStaffIdField.getText().trim();

        if (input.isEmpty()) {
            applyValidationState(modifyStaffIdField, modifyStaffIdValidationLabel, false, "Doctor/Staff ID is required.");
            return false;
        }

        try {
            int staffId = Integer.parseInt(input);
            if (staffId <= 0) {
                applyValidationState(modifyStaffIdField, modifyStaffIdValidationLabel, false, "Doctor/Staff ID must be a positive number.");
                return false;
            }

            // Check if staff exists in database
            if (userService.findById(staffId) == null) {
                applyValidationState(modifyStaffIdField, modifyStaffIdValidationLabel, false, "No staff member was found with ID " + staffId + ".");
                return false;
            }

            applyValidationState(modifyStaffIdField, modifyStaffIdValidationLabel, true, "Doctor/Staff ID is valid.");
            return true;
        } catch (NumberFormatException ex) {
            applyValidationState(modifyStaffIdField, modifyStaffIdValidationLabel, false, "Doctor/Staff ID must be a numeric value.");
            return false;
        }
    }

    private boolean validateModifyMode() {
        if (modifyModeComboBox == null) {
            return true;
        }

        String mode = modifyModeComboBox.getValue();

        if (mode == null || mode.isBlank()) {
            applyValidationState(modifyModeComboBox, modifyModeValidationLabel, false, "Consultation mode is required.");
            return false;
        }

        applyValidationState(modifyModeComboBox, modifyModeValidationLabel, true, "Mode: " + mode);
        return true;
    }

    private boolean validateModifyMotif() {
        if (modifyMotifArea == null) {
            return true;
        }

        String motif = modifyMotifArea.getText().trim();

        if (motif.isEmpty()) {
            applyValidationState(modifyMotifArea, modifyMotifValidationLabel, false, "Motif is required. Explain the reason for modification.");
            return false;
        }

        if (motif.length() < 10) {
            applyValidationState(modifyMotifArea, modifyMotifValidationLabel, false, "Motif is too short. Write at least 10 characters.");
            return false;
        }

        if (motif.length() > 255) {
            applyValidationState(modifyMotifArea, modifyMotifValidationLabel, false, "Motif is too long. Keep it under 255 characters.");
            return false;
        }

        applyValidationState(modifyMotifArea, modifyMotifValidationLabel, true, "Motif looks valid.");
        return true;
    }

    private boolean validateModifyDateTime() {
        if (modifyDateTimeField == null) {
            return true;
        }

        String input = modifyDateTimeField.getText().trim();

        if (input.isEmpty()) {
            applyValidationState(modifyDateTimeField, modifyDateTimeValidationLabel, false, "Date & time is required (format: yyyy-MM-dd HH:mm).");
            return false;
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(input, modifyDateTimeFormatter);

            if (dateTime.isBefore(LocalDateTime.now())) {
                applyValidationState(modifyDateTimeField, modifyDateTimeValidationLabel, false, "The appointment date/time must be in the future.");
                return false;
            }

            applyValidationState(modifyDateTimeField, modifyDateTimeValidationLabel, true, "Date/time: " + modifyDateTimeFormatter.format(dateTime));
            return true;
        } catch (DateTimeParseException ex) {
            applyValidationState(modifyDateTimeField, modifyDateTimeValidationLabel, false, "Invalid date format. Use yyyy-MM-dd HH:mm.");
            return false;
        }
    }
}


