package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.entities.FicheMedicale;
import tn.esprit.entities.Prescription;
import tn.esprit.entities.RendezVous;
import tn.esprit.entities.User;
import tn.esprit.services.FicheMedicaleService;
import tn.esprit.services.PrescriptionService;
import tn.esprit.services.RendezVousService;
import tn.esprit.services.UserService;

import java.io.IOException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConsultationsParDocteurController {

    private enum ConsultationView {
        RENDEZVOUS,
        FICHES,
        PRESCRIPTIONS
    }

    private final UserService userService = new UserService();
    private final RendezVousService rendezVousService = new RendezVousService();
    private final FicheMedicaleService ficheMedicaleService = new FicheMedicaleService();
    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final List<User> allDoctors = new ArrayList<>();

    private ConsultationView currentView = ConsultationView.RENDEZVOUS;
    private Integer selectedDoctorId;

    @FXML
    private BorderPane rootPane;

    @FXML
    private TextField searchField;

    @FXML
    private Label dateLabel;

    @FXML
    private Label doctorCountLabel;

    @FXML
    private Label selectedDoctorLabel;

    @FXML
    private Label recordsSummaryLabel;

    @FXML
    private FlowPane doctorCardsContainer;

    @FXML
    private VBox recordsContainer;

    @FXML
    private Button rendezVousButton;

    @FXML
    private Button fichesButton;

    @FXML
    private Button prescriptionsButton;

    @FXML
    private ScrollPane recordsScrollPane;

    @FXML
    public void initialize() {
        if (dateLabel != null) {
            dateLabel.setText("Espace consultations par docteur");
        }

        if (rootPane != null) {
            URL extraStylesheet = getClass().getResource("/CSS/consultations-par-docteur.css");
            if (extraStylesheet != null) {
                String stylesheet = extraStylesheet.toExternalForm();
                if (!rootPane.getStylesheets().contains(stylesheet)) {
                    rootPane.getStylesheets().add(stylesheet);
                }
            }
        }

        setupSearchFiltering();
        setupViewButtons();
        reloadConsultationData();
        Platform.runLater(this::animateVisibleSections);
    }

    private void setupSearchFiltering() {
        if (searchField == null) {
            return;
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> renderDoctorCards());
    }

    private void setupViewButtons() {
        if (rendezVousButton != null) {
            rendezVousButton.setOnAction(event -> switchView(ConsultationView.RENDEZVOUS));
        }
        if (fichesButton != null) {
            fichesButton.setOnAction(event -> switchView(ConsultationView.FICHES));
        }
        if (prescriptionsButton != null) {
            prescriptionsButton.setOnAction(event -> switchView(ConsultationView.PRESCRIPTIONS));
        }
        refreshViewButtonStyles();
    }

    private void reloadConsultationData() {
        allDoctors.clear();
        allDoctors.addAll(userService.getStaffByRoleAndType("STAFF", "RESP_PATIENTS"));

        if (doctorCountLabel != null) {
            doctorCountLabel.setText(String.valueOf(allDoctors.size()));
        }

        if (selectedDoctorId == null && !allDoctors.isEmpty()) {
            selectedDoctorId = allDoctors.get(0).getId();
        }

        renderDoctorCards();
        renderCurrentRecords();
    }

    private void renderDoctorCards() {
        if (doctorCardsContainer == null) {
            return;
        }

        doctorCardsContainer.getChildren().clear();

        String query = searchField == null || searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<User> visibleDoctors = new ArrayList<>();

        for (User doctor : allDoctors) {
            if (doctor == null) {
                continue;
            }

            String displayName = buildDoctorDisplayName(doctor).toLowerCase(Locale.ROOT);
            String speciality = valueOrDash(doctor.getTypeStaff()).toLowerCase(Locale.ROOT);
            if (query.isBlank() || displayName.contains(query) || speciality.contains(query) || String.valueOf(doctor.getId()).contains(query)) {
                visibleDoctors.add(doctor);
            }
        }

        if (visibleDoctors.isEmpty()) {
            Label emptyLabel = new Label("Aucun docteur ne correspond à votre recherche.");
            emptyLabel.getStyleClass().add("consultation-empty-label");
            doctorCardsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (User doctor : visibleDoctors) {
            doctorCardsContainer.getChildren().add(createDoctorCard(doctor));
        }
    }

    private VBox createDoctorCard(User doctor) {
        VBox card = new VBox(10);
        card.getStyleClass().add("doctor-card");
        if (selectedDoctorId != null && selectedDoctorId == doctor.getId()) {
            card.getStyleClass().add("doctor-card-selected");
        }

        HBox header = new HBox(12);
        header.getStyleClass().add("doctor-card-header");

        VBox avatar = new VBox();
        avatar.getStyleClass().add("doctor-avatar");
        Label avatarText = new Label("DR");
        avatarText.getStyleClass().add("doctor-avatar-text");
        avatar.getChildren().add(avatarText);

        VBox details = new VBox(4);
        Label nameLabel = new Label(buildDoctorDisplayName(doctor));
        nameLabel.getStyleClass().add("doctor-name");

        Label specialityLabel = new Label(valueOrDash(doctor.getTypeStaff()));
        specialityLabel.getStyleClass().add("doctor-speciality");

        Label idLabel = new Label("ID: " + doctor.getId());
        idLabel.getStyleClass().add("doctor-id-pill");

        details.getChildren().addAll(nameLabel, specialityLabel, idLabel);
        header.getChildren().addAll(avatar, details);

        Label noteLabel = new Label("Cliquez pour charger ses rendez-vous, fiches médicales et prescriptions.");
        noteLabel.setWrapText(true);
        noteLabel.getStyleClass().add("doctor-note");

        Button selectButton = new Button("Voir les données");
        selectButton.getStyleClass().add("doctor-select-button");
        selectButton.setOnAction(event -> selectDoctor(doctor));

        card.getChildren().addAll(header, noteLabel, selectButton);
        card.setOnMouseClicked(event -> selectDoctor(doctor));
        return card;
    }

    private void selectDoctor(User doctor) {
        if (doctor == null) {
            return;
        }

        selectedDoctorId = doctor.getId();
        updateSelectedDoctorLabel(doctor);
        renderDoctorCards();
        renderCurrentRecords();
    }

    private void updateSelectedDoctorLabel(User doctor) {
        if (selectedDoctorLabel == null) {
            return;
        }

        if (doctor == null) {
            selectedDoctorLabel.setText("Aucun docteur sélectionné");
        } else {
            selectedDoctorLabel.setText("Docteur sélectionné: " + buildDoctorDisplayName(doctor) + " (#" + doctor.getId() + ")");
        }
    }

    private void switchView(ConsultationView view) {
        currentView = view;
        refreshViewButtonStyles();
        renderCurrentRecords();
    }

    private void refreshViewButtonStyles() {
        updateViewButtonStyle(rendezVousButton, currentView == ConsultationView.RENDEZVOUS);
        updateViewButtonStyle(fichesButton, currentView == ConsultationView.FICHES);
        updateViewButtonStyle(prescriptionsButton, currentView == ConsultationView.PRESCRIPTIONS);
    }

    private void updateViewButtonStyle(Button button, boolean active) {
        if (button == null) {
            return;
        }

        button.getStyleClass().removeAll("record-switch-button-active");
        if (active) {
            if (!button.getStyleClass().contains("record-switch-button-active")) {
                button.getStyleClass().add("record-switch-button-active");
            }
        }
    }

    private void renderCurrentRecords() {
        if (recordsContainer == null) {
            return;
        }

        recordsContainer.getChildren().clear();

        if (selectedDoctorId == null) {
            recordsContainer.getChildren().add(createEmptyState("Sélectionnez un docteur pour afficher ses données."));
            updateRecordsSummary(0, "Aucun docteur sélectionné");
            return;
        }

        if (currentView == ConsultationView.RENDEZVOUS) {
            List<RendezVous> rendezVousList = rendezVousService.recupererParStaffId(selectedDoctorId);
            updateRecordsSummary(rendezVousList.size(), "Rendez-vous du docteur sélectionné");
            if (rendezVousList.isEmpty()) {
                recordsContainer.getChildren().add(createEmptyState("Aucun rendez-vous trouvé pour ce docteur."));
                return;
            }
            for (RendezVous rendezVous : rendezVousList) {
                recordsContainer.getChildren().add(createRendezVousCard(rendezVous));
            }
        } else if (currentView == ConsultationView.FICHES) {
            List<FicheMedicale> fiches = getFichesForSelectedDoctor();
            updateRecordsSummary(fiches.size(), "Fiches médicales du docteur sélectionné");
            if (fiches.isEmpty()) {
                recordsContainer.getChildren().add(createEmptyState("Aucune fiche médicale trouvée pour ce docteur."));
                return;
            }
            for (FicheMedicale ficheMedicale : fiches) {
                recordsContainer.getChildren().add(createFicheCard(ficheMedicale));
            }
        } else {
            List<Prescription> prescriptions = getPrescriptionsForSelectedDoctor();
            updateRecordsSummary(prescriptions.size(), "Prescriptions du docteur sélectionné");
            if (prescriptions.isEmpty()) {
                recordsContainer.getChildren().add(createEmptyState("Aucune prescription trouvée pour ce docteur."));
                return;
            }
            for (Prescription prescription : prescriptions) {
                recordsContainer.getChildren().add(createPrescriptionCard(prescription));
            }
        }
    }

    private List<FicheMedicale> getFichesForSelectedDoctor() {
        List<FicheMedicale> fiches = new ArrayList<>();
        if (selectedDoctorId == null) {
            return fiches;
        }

        Set<Integer> rendezVousIds = new HashSet<>();
        for (RendezVous rendezVous : rendezVousService.recupererParStaffId(selectedDoctorId)) {
            if (rendezVous != null) {
                rendezVousIds.add(rendezVous.getId());
            }
        }

        for (FicheMedicale ficheMedicale : ficheMedicaleService.recuperer()) {
            if (ficheMedicale != null && ficheMedicale.getRendez_vous_id() != null && rendezVousIds.contains(ficheMedicale.getRendez_vous_id())) {
                fiches.add(ficheMedicale);
            }
        }

        return fiches;
    }

    private List<Prescription> getPrescriptionsForSelectedDoctor() {
        List<Prescription> prescriptions = new ArrayList<>();
        if (selectedDoctorId == null) {
            return prescriptions;
        }

        Set<Integer> ficheIds = new HashSet<>();
        for (FicheMedicale ficheMedicale : getFichesForSelectedDoctor()) {
            if (ficheMedicale != null) {
                ficheIds.add(ficheMedicale.getId());
            }
        }

        for (Prescription prescription : prescriptionService.recuperer()) {
            if (prescription != null && ficheIds.contains(prescription.getFiche_medicale_id())) {
                prescriptions.add(prescription);
            }
        }

        return prescriptions;
    }

    private VBox createRendezVousCard(RendezVous rendezVous) {
        VBox card = new VBox(8);
        card.getStyleClass().add("record-card");

        Label title = new Label("Rendez-vous #" + rendezVous.getId());
        title.getStyleClass().add("record-card-title");

        Label meta = new Label(buildRendezVousMeta(rendezVous));
        meta.setWrapText(true);
        meta.getStyleClass().add("record-card-meta");

        Label motif = new Label("Motif: " + valueOrDash(rendezVous.getMotif()));
        motif.setWrapText(true);
        motif.getStyleClass().add("record-card-text");

        card.getChildren().addAll(title, meta, motif);
        return card;
    }

    private VBox createFicheCard(FicheMedicale ficheMedicale) {
        VBox card = new VBox(8);
        card.getStyleClass().add("record-card");

        Label title = new Label("Fiche médicale #" + ficheMedicale.getId());
        title.getStyleClass().add("record-card-title");

        Label meta = new Label("Rendez-vous #" + valueOrDash(String.valueOf(ficheMedicale.getRendez_vous_id())) + " • Durée: " + valueOrDash(formatDuration(ficheMedicale.getDuree_minutes())));
        meta.setWrapText(true);
        meta.getStyleClass().add("record-card-meta");

        Label diagnostic = new Label("Diagnostic: " + valueOrDash(ficheMedicale.getDiagnostic()));
        diagnostic.setWrapText(true);
        diagnostic.getStyleClass().add("record-card-text");

        Label observations = new Label("Observations: " + valueOrDash(ficheMedicale.getObservations()));
        observations.setWrapText(true);
        observations.getStyleClass().add("record-card-text");

        card.getChildren().addAll(title, meta, diagnostic, observations);
        return card;
    }

    private VBox createPrescriptionCard(Prescription prescription) {
        VBox card = new VBox(8);
        card.getStyleClass().add("record-card");

        Label title = new Label("Prescription #" + prescription.getId());
        title.getStyleClass().add("record-card-title");

        Label meta = new Label("Fiche médicale #" + prescription.getFiche_medicale_id() + " • Durée: " + prescription.getDuree() + " min");
        meta.setWrapText(true);
        meta.getStyleClass().add("record-card-meta");

        Label medicament = new Label("Médicament: " + valueOrDash(prescription.getNom_medicament()));
        medicament.setWrapText(true);
        medicament.getStyleClass().add("record-card-text");

        Label dosage = new Label("Dose: " + valueOrDash(prescription.getDose()) + " • Fréquence: " + valueOrDash(prescription.getFrequence()));
        dosage.setWrapText(true);
        dosage.getStyleClass().add("record-card-text");

        Label instructions = new Label("Instructions: " + valueOrDash(prescription.getInstructions()));
        instructions.setWrapText(true);
        instructions.getStyleClass().add("record-card-text");

        card.getChildren().addAll(title, meta, medicament, dosage, instructions);
        return card;
    }

    private VBox createEmptyState(String message) {
        VBox box = new VBox();
        box.getStyleClass().add("record-empty-state");
        Label label = new Label(message);
        label.setWrapText(true);
        label.getStyleClass().add("record-empty-text");
        box.getChildren().add(label);
        return box;
    }

    private void updateRecordsSummary(int count, String label) {
        if (recordsSummaryLabel != null) {
            recordsSummaryLabel.setText(label + " • " + count + " élément(s)");
        }
    }

    private String buildRendezVousMeta(RendezVous rendezVous) {
        StringBuilder builder = new StringBuilder();
        builder.append(formatTimestamp(rendezVous.getDatetime()));
        builder.append(" • ").append(valueOrDash(rendezVous.getMode()));
        builder.append(" • ").append(valueOrDash(rendezVous.getStatut()));
        builder.append(" • Patient #").append(rendezVous.getIdPatient());
        return builder.toString();
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return dateTimeFormatter.format(timestamp.toLocalDateTime());
    }

    private String formatDuration(Integer duration) {
        return duration == null ? "-" : duration + " min";
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

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void animateVisibleSections() {
        if (rootPane == null) {
            return;
        }

        for (Node node : rootPane.lookupAll(".consultation-animate")) {
            node.setOpacity(0);
            node.setTranslateY(18);
            node.setOpacity(1);
            node.setTranslateY(0);
        }
    }

    @FXML
    private void openDashboard() {
        goTo("/AdminWelcome.fxml");
    }

    @FXML
    private void openPatients() {
        goTo("/FXML/PatientsAdmin.fxml");
    }

    @FXML
    private void openUsers() {
        goTo("/FXML/UtilisateursAdmin.fxml");
    }

    @FXML
    private void openRoles() {
        goTo("/FXML/DemandesRole.fxml");
    }

    @FXML
    private void openUserRoles() {
        goTo("/FXML/UserRoles.fxml");
    }

    @FXML
    private void openProduits() {
        goTo("/ProduitAdmin.fxml");
    }

    @FXML
    private void openCommandes() {
        goTo("/CommandeAdmin.fxml");
    }

    @FXML
    private void openDetection() {
        goTo("/DetectionEpidemie.fxml");
    }

    @FXML
    private void openStockRupture() {
        goTo("/FXML/StockRupture.fxml");
    }

    @FXML
    private void openStatsProduits() {
        goTo("/StatProduit.fxml");
    }

    @FXML
    private void openEvents() {
        goTo("/FXML/EvenementsAdmin.fxml");
    }

    @FXML
    private void openEventParticipants() {
        goTo("/FXML/ParticipantsEvenement.fxml");
    }

    @FXML
    private void openRessources() {
        goTo("/FXML/RessourcesAdmin.fxml");
    }

    @FXML
    private void openStatsEvents() {
        goTo("/FXML/StatsEvenements.fxml");
    }

    @FXML
    private void openStatsRessources() {
        goTo("/FXML/StatsRessources.fxml");
    }

    @FXML
    private void openConsultations() {
        reloadConsultationData();
    }

    @FXML
    private void openStatsConsultations() {
        goTo("/FXML/StatsConsultations.fxml");
    }

    @FXML
    private void openReclamations() {
        goTo("/FXML/ReclamationsAdmin.fxml");
    }

    @FXML
    private void openReponsesReclamations() {
        goTo("/FXML/ReponsesReclamationsAdmin.fxml");
    }

    @FXML
    private void openReclamationsUrgentes() {
        goTo("/FXML/ReclamationsUrgentes.fxml");
    }

    @FXML
    private void openStatsReclamations() {
        goTo("/FXML/StatsReclamations.fxml");
    }

    @FXML
    private void openPosts() {
        goTo("/FXML/PostsAdmin.fxml");
    }

    @FXML
    private void openBlogComments() {
        goTo("/FXML/CommentairesAdmin.fxml");
    }

    @FXML
    private void openBlogModeration() {
        goTo("/FXML/ModerationBlog.fxml");
    }

    @FXML
    private void openPostsEnAttente() {
        goTo("/FXML/PostsEnAttente.fxml");
    }

    @FXML
    private void openStatsBlog() {
        goTo("/FXML/StatsBlog.fxml");
    }

    @FXML
    private void openNewProduct() {
        goTo("/FXML/AjoutProduit.fxml");
    }

    @FXML
    private void openNewEvent() {
        goTo("/FXML/AjoutEvenement.fxml");
    }

    @FXML
    private void openNewUser() {
        goTo("/FXML/AjoutUtilisateur.fxml");
    }

    @FXML
    private void openReports() {
        goTo("/FXML/DashboardStatsAdmin.fxml");
    }

    @FXML
    private void goBackSite() {
        goTo("/FXML/Home.fxml");
    }

    @FXML
    private void logout() {
        goTo("/FXML/Login.fxml");
    }

    private void goTo(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                showWarning("Page introuvable : " + fxmlPath + "\n\nRemplace ce chemin par le vrai nom de ton fichier FXML.");
                return;
            }

            Parent page = FXMLLoader.load(url);
            if (rootPane != null && rootPane.getScene() != null) {
                rootPane.getScene().setRoot(page);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showWarning("Erreur lors de l'ouverture de la page : " + fxmlPath);
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Navigation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}