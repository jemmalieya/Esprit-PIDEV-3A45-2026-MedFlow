package tn.esprit.controllers;

import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import tn.esprit.entities.FicheMedicale;
import tn.esprit.entities.Prescription;
import tn.esprit.entities.RendezVous;
import tn.esprit.entities.User;
import tn.esprit.services.*;
import tn.esprit.tools.SessionManager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OcrUploadController {

    @FXML
    private ComboBox<String> ocrDocumentTypeCombo;
    
    @FXML
    private ComboBox<String> ocrReferenceCombo;
    
    @FXML
    private StackPane ocrPdfViewerContainer;
    
    @FXML
    private VBox ocrFormContainer;
    
    @FXML
    private Label ocrUploadStatusLabel;
    
    @FXML
    private Button ocrConfirmButton;
    
    @FXML
    private VBox ocrUploadSelectionBox;
    
    // Services
    private final RendezVousService rendezVousService = new RendezVousService();
    private final FicheMedicaleService ficheMedicaleService = new FicheMedicaleService();
    private final PrescriptionService prescriptionService = new PrescriptionService();
    private final UserService userService = new UserService();
    
    // State variables
    private File selectedPdfFile;
    private String selectedDocumentType;
    private String selectedReference;
    private JsonObject extractedData = new JsonObject();
    private Map<String, Control> formFields = new HashMap<>();
    private int selectedReferenceId = -1;
    
    @FXML
    public void initialize() {
        setupDocumentTypeCombo();
        setupReferenceCombo();
        ocrConfirmButton.setDisable(true);
    }
    
    private void setupDocumentTypeCombo() {
        ObservableList<String> documentTypes = FXCollections.observableArrayList(
            "Fiche Medicale",
            "Prescription"
        );
        ocrDocumentTypeCombo.setItems(documentTypes);
        ocrDocumentTypeCombo.setOnAction(event -> handleDocumentTypeChanged());
    }
    
    private void handleDocumentTypeChanged() {
        selectedDocumentType = ocrDocumentTypeCombo.getValue();
        if (selectedDocumentType != null) {
            loadReferenceList();
            ocrUploadStatusLabel.setText("Type sélectionné: " + selectedDocumentType + ". Choisissez une référence.");
        }
        resetUploadState();
    }
    
    private void setupReferenceCombo() {
        ocrReferenceCombo.setOnAction(event -> {
            selectedReference = ocrReferenceCombo.getValue();
            if (selectedReference != null) {
                // Extract ID from combo value (format: "ID - Description")
                try {
                    String[] parts = selectedReference.split(" - ", 2);
                    selectedReferenceId = Integer.parseInt(parts[0].trim());
                } catch (Exception e) {
                    selectedReferenceId = -1;
                }
                ocrUploadStatusLabel.setText("Référence sélectionnée. Cliquez sur 'Parcourir...' pour choisir un fichier PDF.");
            }
        });
    }
    
    private void loadReferenceList() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showError("Erreur", "Utilisateur non authentifié");
            return;
        }
        
        int userId = currentUser.getId();
        ObservableList<String> references = FXCollections.observableArrayList();
        
        if ("Fiche Medicale".equalsIgnoreCase(selectedDocumentType)) {
            // Get only rendez-vous that do not already have a fiche medicale
            List<RendezVous> rendezVousList = rendezVousService.recuperer();
            for (RendezVous rv : rendezVousList) {
                if (rv.getIdStaff() == userId && ficheMedicaleService.getByRendezVousId(rv.getId()) == null) {
                    references.add(rv.getId() + " - " +
                        rv.getDatetime() + " (" + rv.getMode() + ")");
                }
            }

            if (references.isEmpty()) {
                ocrUploadStatusLabel.setText("Aucun rendez-vous disponible: tous les rendez-vous de ce docteur ont déjà une fiche médicale.");
            }
        } else if ("Prescription".equalsIgnoreCase(selectedDocumentType)) {
            // Get list of fiche medicale for this user
            List<FicheMedicale> fiches = ficheMedicaleService.recuperer();
            for (FicheMedicale fiche : fiches) {
                references.add(fiche.getId() + " - " +
                    fiche.getDiagnostic() + " (" + fiche.getRendez_vous_id() + ")");
            }
        }
        
        ocrReferenceCombo.setItems(references);
        ocrReferenceCombo.setPrefWidth(300);
    }
    
    @FXML
    private void handleSelectPdfFile() {
        if (selectedDocumentType == null || selectedReference == null) {
            showError("Validation", "Veuillez sélectionner un type de document et une référence.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier PDF");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        
        Window window = ocrUploadSelectionBox.getScene().getWindow();
        File file = fileChooser.showOpenDialog(window);
        
        if (file != null && file.exists()) {
            selectedPdfFile = file;
            performOcrExtraction(file);
        }
    }
    
    private void performOcrExtraction(File pdfFile) {
        ocrUploadStatusLabel.setText("Extraction OCR en cours... Veuillez patienter.");
        ocrConfirmButton.setDisable(true);
        
        Task<String> ocrTask = new Task<String>() {
            @Override
            protected String call() {
                String extractedText = OcrExtractionService.extractTextFromPdf(pdfFile);
                return extractedText != null ? extractedText : "";
            }
        };
        
        ocrTask.setOnSucceeded(event -> {
            String extractedText = ocrTask.getValue();
            if (extractedText.isEmpty()) {
                showError("Erreur OCR", "Impossible d'extraire le texte du PDF.\n" + OcrExtractionService.getLastError());
                resetUploadState();
            } else {
                extractedData = OcrExtractionService.parseOcrTextByType(extractedText, selectedDocumentType);
                generateFormFromExtractedData();
                displayRenderedPdfPreview(pdfFile);
                ocrUploadStatusLabel.setText("Extraction réussie. Vérifiez et modifiez les données si nécessaire.");
                ocrConfirmButton.setDisable(false);
            }
        });
        
        ocrTask.setOnFailed(event -> {
            showError("Erreur", "Erreur lors de l'extraction: " + ocrTask.getException().getMessage());
            resetUploadState();
        });
        
        Thread ocrThread = new Thread(ocrTask);
        ocrThread.setDaemon(true);
        ocrThread.start();
    }
    
    private void generateFormFromExtractedData() {
        Platform.runLater(() -> {
            ocrFormContainer.getChildren().clear();
            formFields.clear();
            
            if ("Fiche Medicale".equalsIgnoreCase(selectedDocumentType)) {
                addFormField("Diagnostic", "diagnostic");
                addFormField("Observations", "observations");
                addFormField("Résultats Examens", "resultatsExamens");
                addFormField("Durée (minutes)", "dureeMinutes");
            } else if ("Prescription".equalsIgnoreCase(selectedDocumentType)) {
                addFormField("Nom du Médicament", "nomMedicament");
                addFormField("Dose", "dose");
                addFormField("Fréquence", "frequence");
                addFormField("Durée", "duree");
                addFormField("Instructions", "instructions");
            }
        });
    }
    
    private void addFormField(String label, String fieldKey) {
        VBox fieldBox = new VBox(6);
        fieldBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d8e3ef; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 8 10 10 10;");
        
        Label fieldLabel = new Label(label);
        fieldLabel.setMinHeight(20);
        fieldLabel.setMaxWidth(Double.MAX_VALUE);
        fieldLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #1f3f83; -fx-padding: 0 0 2 2;");
        
        String extractedValue = "";
        if (extractedData.has(fieldKey)) {
            extractedValue = extractedData.get(fieldKey).getAsString();
        }
        
        Control inputControl;
        
        // Use TextArea for larger text fields
        if (fieldKey.equals("observations") || fieldKey.equals("resultatsExamens") || fieldKey.equals("instructions")) {
            TextArea textArea = new TextArea(extractedValue);
            textArea.setWrapText(true);
            textArea.setPrefRowCount(3);
            textArea.setStyle("-fx-control-inner-background: #f9f9f9; -fx-padding: 6;");
            inputControl = textArea;
        } else {
            TextField textField = new TextField(extractedValue);
            textField.setStyle("-fx-padding: 6; -fx-font-size: 11;");
            inputControl = textField;
        }
        
        formFields.put(fieldKey, inputControl);
        fieldBox.getChildren().addAll(fieldLabel, inputControl);
        ocrFormContainer.getChildren().add(fieldBox);
    }

    private void displayRenderedPdfPreview(File pdfFile) {
        ocrPdfViewerContainer.getChildren().clear();

        Label loadingLabel = new Label("Chargement de l'aperçu PDF...");
        loadingLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f3f83;");
        ocrPdfViewerContainer.getChildren().add(loadingLabel);

        Task<List<WritableImage>> previewTask = new Task<List<WritableImage>>() {
            @Override
            protected List<WritableImage> call() throws Exception {
                return renderPdfPages(pdfFile);
            }
        };

        previewTask.setOnSucceeded(event -> {
            ocrPdfViewerContainer.getChildren().setAll(createPdfPreview(previewTask.getValue()));
        });

        previewTask.setOnFailed(event -> {
            Label errorLabel = new Label("Impossible d'afficher le PDF: " + previewTask.getException().getMessage());
            errorLabel.setWrapText(true);
            errorLabel.setStyle("-fx-text-fill: #b42318; -fx-font-weight: bold;");
            ocrPdfViewerContainer.getChildren().setAll(errorLabel);
        });

        Thread previewThread = new Thread(previewTask, "ocr-pdf-preview");
        previewThread.setDaemon(true);
        previewThread.start();
    }

    private List<WritableImage> renderPdfPages(File pdfFile) throws IOException {
        List<WritableImage> pages = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, 100);
                pages.add(SwingFXUtils.toFXImage(pageImage, null));
            }
        }
        return pages;
    }

    private VBox createPdfPreview(List<WritableImage> pages) {
        final double minZoom = 0.35;
        final double maxZoom = 2.5;
        final double zoomStep = 0.15;
        final double[] zoom = {0.75};

        Button zoomOutButton = new Button("-");
        Button resetZoomButton = new Button("100%");
        Button zoomInButton = new Button("+");
        Label zoomLabel = new Label();

        zoomOutButton.setStyle("-fx-font-weight: bold; -fx-min-width: 34;");
        resetZoomButton.setStyle("-fx-font-weight: bold; -fx-min-width: 58;");
        zoomInButton.setStyle("-fx-font-weight: bold; -fx-min-width: 34;");
        zoomLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f3f83; -fx-min-width: 48;");

        HBox toolbar = new HBox(8, zoomOutButton, resetZoomButton, zoomInButton, zoomLabel);
        toolbar.setPadding(new Insets(8, 10, 0, 10));
        toolbar.setStyle("-fx-alignment: center-left; -fx-background-color: #edf3f8;");

        VBox pagesBox = new VBox(14);
        pagesBox.setPadding(new Insets(14));
        pagesBox.setStyle("-fx-background-color: #edf3f8; -fx-alignment: top-center;");

        List<ImageView> pageViews = new ArrayList<>();

        for (WritableImage fxImage : pages) {
            ImageView pageView = new ImageView(fxImage);
            pageView.setPreserveRatio(true);
            pageView.setSmooth(true);
            pageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 10, 0.12, 0, 2);");
            pageViews.add(pageView);
            pagesBox.getChildren().add(pageView);
        }

        ScrollPane scrollPane = new ScrollPane(pagesBox);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Runnable applyZoom = () -> {
            zoomLabel.setText(Math.round(zoom[0] * 100) + "%");
            for (ImageView pageView : pageViews) {
                pageView.setFitWidth(pageView.getImage().getWidth() * zoom[0]);
            }
        };

        zoomOutButton.setOnAction(event -> {
            zoom[0] = Math.max(minZoom, zoom[0] - zoomStep);
            applyZoom.run();
        });
        zoomInButton.setOnAction(event -> {
            zoom[0] = Math.min(maxZoom, zoom[0] + zoomStep);
            applyZoom.run();
        });
        resetZoomButton.setOnAction(event -> {
            zoom[0] = 1.0;
            applyZoom.run();
        });
        applyZoom.run();

        VBox previewBox = new VBox(0, toolbar, scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);
        return previewBox;
    }
    
    private void displayPdfPreview(File pdfFile) {
        // Display a label with PDF file info
        // Note: Full PDF rendering requires PDFBox viewer, which is complex
        // For now, show basic file information
        ocrPdfViewerContainer.getChildren().clear();
        
        VBox pdfInfoBox = new VBox(8);
        pdfInfoBox.setPadding(new Insets(12));
        pdfInfoBox.setStyle("-fx-alignment: center; -fx-text-alignment: center;");
        
        Label titleLabel = new Label("📄 Fichier PDF");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        
        Label fileNameLabel = new Label("Nom: " + pdfFile.getName());
        fileNameLabel.setStyle("-fx-font-size: 11;");
        
        long fileSizeKB = pdfFile.length() / 1024;
        Label fileSizeLabel = new Label("Taille: " + fileSizeKB + " KB");
        fileSizeLabel.setStyle("-fx-font-size: 11;");
        
        Label pathLabel = new Label("Chemin: " + pdfFile.getAbsolutePath());
        pathLabel.setWrapText(true);
        pathLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
        
        pdfInfoBox.getChildren().addAll(titleLabel, fileNameLabel, fileSizeLabel, pathLabel);
        ocrPdfViewerContainer.getChildren().add(pdfInfoBox);
    }
    
    @FXML
    private void handleConfirmOcrUpload() {
        if (selectedReferenceId <= 0) {
            showError("Validation", "Référence invalide.");
            return;
        }
        
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showError("Erreur", "Utilisateur non authentifié.");
            return;
        }
        
        try {
            if ("Fiche Medicale".equalsIgnoreCase(selectedDocumentType)) {
                saveFicheMedicaleFromForm(currentUser);
            } else if ("Prescription".equalsIgnoreCase(selectedDocumentType)) {
                savePrescriptionFromForm(currentUser);
            }
            
            showSuccess("Succès", "Les données ont été enregistrées avec succès.");
            resetUploadState();
            if (selectedDocumentType != null) {
                loadReferenceList();
            }
        } catch (Exception e) {
            showError("Erreur d'enregistrement", e.getMessage());
        }
    }
    
    private void saveFicheMedicaleFromForm(User currentUser) throws Exception {
        String diagnostic = getFormFieldValue("diagnostic");
        String observations = getFormFieldValue("observations");
        String resultatsExamens = getFormFieldValue("resultatsExamens");
        String dureeStr = getFormFieldValue("dureeMinutes");
        int duree = 0;
        try {
            duree = Integer.parseInt(dureeStr.isEmpty() ? "0" : dureeStr);
        } catch (NumberFormatException e) {
            duree = 0;
        }
        
        FicheMedicale fiche = new FicheMedicale();
        fiche.setRendez_vous_id(selectedReferenceId);
        fiche.setDiagnostic(diagnostic);
        fiche.setObservations(observations);
        fiche.setResultats_examens(resultatsExamens);
        fiche.setDuree_minutes(duree);
        fiche.setSignature(""); // No signature from OCR
        fiche.setCreated_at(Timestamp.valueOf(LocalDateTime.now()));
        
        ficheMedicaleService.ajouter(fiche);
    }
    
    private void savePrescriptionFromForm(User currentUser) throws Exception {
        String nomMedicament = getFormFieldValue("nomMedicament");
        String dose = getFormFieldValue("dose");
        String frequence = getFormFieldValue("frequence");
        String dureeStr = getFormFieldValue("duree");
        String instructions = getFormFieldValue("instructions");
        int duree = 0;
        try {
            duree = Integer.parseInt(dureeStr.isEmpty() ? "0" : dureeStr);
        } catch (NumberFormatException e) {
            duree = 0;
        }
        
        Prescription prescription = new Prescription();
        prescription.setFiche_medicale_id(selectedReferenceId);
        prescription.setNom_medicament(nomMedicament);
        prescription.setDose(dose);
        prescription.setFrequence(frequence);
        prescription.setDuree(duree);
        prescription.setInstructions(instructions);
        prescription.setCreated_at(Timestamp.valueOf(LocalDateTime.now()));
        
        prescriptionService.ajouter(prescription);
    }
    
    private String getFormFieldValue(String fieldKey) {
        Control control = formFields.get(fieldKey);
        if (control instanceof TextField) {
            return ((TextField) control).getText().trim();
        } else if (control instanceof TextArea) {
            return ((TextArea) control).getText().trim();
        }
        return "";
    }
    
    @FXML
    private void handleCancelOcrUpload() {
        resetUploadState();
        ocrUploadStatusLabel.setText("Upload annulé. Sélectionnez un nouveau document et une référence.");
    }
    
    private void resetUploadState() {
        selectedPdfFile = null;
        selectedReference = null;
        selectedReferenceId = -1;
        extractedData = new JsonObject();
        formFields.clear();
        ocrFormContainer.getChildren().clear();
        ocrPdfViewerContainer.getChildren().clear();
        ocrConfirmButton.setDisable(true);
        ocrReferenceCombo.setValue(null);
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
