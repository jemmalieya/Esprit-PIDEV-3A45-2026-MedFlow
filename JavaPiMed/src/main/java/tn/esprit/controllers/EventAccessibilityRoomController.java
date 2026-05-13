package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.embed.swing.SwingNode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Ressource;
import tn.esprit.entities.User;
import tn.esprit.services.AccessibilityRoomService;
import tn.esprit.services.EmbeddedChromiumService;
import tn.esprit.tools.SessionManager;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class EventAccessibilityRoomController {

    private static final double DEFAULT_SCENE_WIDTH = 1500;
    private static final double DEFAULT_SCENE_HEIGHT = 920;

    @FXML private Label eventTitleLabel;
    @FXML private Label eventMetaLabel;
    @FXML private Label roomStatusLabel;
    @FXML private Label roomUrlLabel;
    @FXML private TextField roomUrlInput;
    @FXML private VBox roomLinkEditorBox;
    @FXML private javafx.scene.control.Button saveRoomLinkButton;
    @FXML private StackPane roomBrowserHost;
    @FXML private TextArea transcriptArea;
    @FXML private FlowPane resourcesFlow;

    private final AccessibilityRoomService accessibilityRoomService = new AccessibilityRoomService();
    private final EmbeddedChromiumService embeddedChromiumService = new EmbeddedChromiumService();
    private Evenement evenement;
    private String roomUrl;
    private EmbeddedChromiumService.BrowserWindowSession roomBrowserSession;
    private EmbeddedChromiumService.BrowserSession captionsBrowserSession;
    private Stage captionsStage;
    private boolean roomManagerAccess;
    private boolean roomJoinAccess;

    @FXML
    public void initialize() {
        evenement = EvenementController.getEvenementSelectionneFront();
        if (evenement == null) {
            showAlert(Alert.AlertType.ERROR, "Salle accessibilite", "Aucun evenement n'est selectionne.");
            return;
        }

        roomUrl = accessibilityRoomService.buildRoomUrl(evenement);
        accessibilityRoomService.ensureRoomResource(evenement);
        roomManagerAccess = isCurrentUserEventManager();
        roomJoinAccess = isCurrentUserAllowedToJoin();
        if (!roomJoinAccess) {
            roomUrl = "";
        }

        if (eventTitleLabel != null) {
            eventTitleLabel.setText(text(evenement.getTitre_event()));
        }
        if (eventMetaLabel != null) {
            eventMetaLabel.setText(text(evenement.getVille_event()) + " | " + dateText(evenement));
        }
        if (roomStatusLabel != null) {
            roomStatusLabel.setText(!roomJoinAccess
                    ? "La salle live est disponible pour les patients uniquement le jour de l'evenement."
                    : roomUrl.isBlank()
                    ? "Aucun lien d'appel video n'est configure pour cet evenement."
                    : "Appel pret. Rejoignez la reunion dans l'application.");
        }
        if (roomUrlLabel != null) {
            roomUrlLabel.setText(roomUrl.isBlank() ? "Aucun lien configure" : roomUrl);
        }
        if (roomUrlInput != null) {
            roomUrlInput.setText(roomUrl);
            roomUrlInput.setEditable(roomManagerAccess);
            roomUrlInput.setDisable(!roomManagerAccess);
            roomUrlInput.setVisible(roomManagerAccess);
            roomUrlInput.setManaged(roomManagerAccess);
        }
        if (roomLinkEditorBox != null) {
            roomLinkEditorBox.setVisible(roomJoinAccess);
            roomLinkEditorBox.setManaged(roomJoinAccess);
        }
        if (saveRoomLinkButton != null) {
            saveRoomLinkButton.setVisible(roomManagerAccess);
            saveRoomLinkButton.setManaged(roomManagerAccess);
        }

        if (roomBrowserHost != null) {
            Label placeholder = new Label(roomUrl.isBlank()
                    ? "Aucun lien d'appel video n'est configure pour cet evenement. Ajoutez-le dans la fiche evenement."
                    : "Cliquez sur \"Rejoindre l'appel\" pour ouvrir l'appel video dans une fenetre Chromium MedFlow separee.");
            placeholder.setWrapText(true);
            placeholder.setStyle(
                    "-fx-text-fill: #173745;" +
                            "-fx-font-size: 16px;" +
                            "-fx-font-weight: 700;" +
                            "-fx-background-color: #eef9ff;" +
                            "-fx-background-radius: 20;" +
                            "-fx-padding: 24;"
            );
            roomBrowserHost.getChildren().setAll(placeholder);
        }
        refreshResources();
    }

    @FXML
    private void appendQuickPhraseBonjour() {
        appendTranscriptLine("Bonjour, je suis connecte.");
    }

    @FXML
    private void appendQuickPhraseOui() {
        appendTranscriptLine("Oui.");
    }

    @FXML
    private void appendQuickPhraseNon() {
        appendTranscriptLine("Non.");
    }

    @FXML
    private void appendQuickPhraseRepeat() {
        appendTranscriptLine("Pouvez-vous repeter plus lentement s'il vous plait ?");
    }

    @FXML
    private void appendQuickPhraseHelp() {
        appendTranscriptLine("J'ai besoin d'assistance accessibilite.");
    }

    @FXML
    private void saveRoomLink() {
        if (!roomManagerAccess) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul un responsable connecte peut enregistrer le lien de la salle.");
            return;
        }
        try {
            String candidate = roomUrlInput == null ? "" : roomUrlInput.getText();
            accessibilityRoomService.saveRoomLink(evenement, candidate);
            roomUrl = accessibilityRoomService.buildRoomUrl(evenement);
            if (roomUrlLabel != null) {
                roomUrlLabel.setText(roomUrl.isBlank() ? "Aucun lien configure" : roomUrl);
            }
            roomStatusLabel.setText(roomUrl.isBlank()
                    ? "Lien d'appel supprime pour cet evenement."
                    : "Lien d'appel enregistre pour cet evenement.");
            refreshResources();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lien d'appel", "Enregistrement impossible : " + e.getMessage());
        }
    }

    @FXML
    private void openInBrowser() {
        if (!roomJoinAccess) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "La salle live est disponible pour les patients uniquement le jour de l'evenement.");
            return;
        }
        if (roomUrl == null || roomUrl.isBlank()) {
            showAlert(Alert.AlertType.INFORMATION, "Lien salle", "Aucun lien d'appel video n'est configure pour cet evenement.");
            return;
        }
        try {
            if (!Desktop.isDesktopSupported()) {
                showAlert(Alert.AlertType.INFORMATION, "Lien salle", roomUrl);
                return;
            }
            Desktop.getDesktop().browse(URI.create(roomUrl));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Salle accessibilite", "Impossible d'ouvrir le navigateur : " + e.getMessage());
        }
    }

    @FXML
    private void launchRoom() {
        if (!roomJoinAccess) {
            roomStatusLabel.setText("La salle live est disponible pour les patients uniquement le jour de l'evenement.");
            return;
        }
        if (isPastEvent()) {
            roomStatusLabel.setText("La salle live n'est plus disponible car la date de l'evenement est depassee.");
            return;
        }
        if (!isOnlineStatus()) {
            roomStatusLabel.setText("Passez d'abord l'evenement au statut En ligne pour ouvrir la salle.");
            return;
        }
        if (roomUrl == null || roomUrl.isBlank()) {
            roomStatusLabel.setText("Ajoutez d'abord un lien d'appel video dans la fiche evenement.");
            return;
        }
        if (!accessibilityRoomService.isEmbeddableJitsiUrl(roomUrl)) {
            openInBrowser();
            roomStatusLabel.setText("Pour le mode integre Java, utilisez un lien meet.jit.si. Ce lien s'ouvre dans votre navigateur.");
            return;
        }
        roomStatusLabel.setText("Ouverture de l'appel dans l'application...");
        loadRoom();
    }

    @FXML
    private void copyRoomLink() {
        if (!roomJoinAccess) {
            roomStatusLabel.setText("Acces indisponible hors jour d'evenement.");
            return;
        }
        if (roomUrl == null || roomUrl.isBlank()) {
            roomStatusLabel.setText("Aucun lien d'appel video a copier.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(roomUrl);
        Clipboard.getSystemClipboard().setContent(content);
        roomStatusLabel.setText("Lien de salle copie dans le presse-papiers.");
    }

    @FXML
    private void openCaptionAssistant() {
        try {
            if (captionsStage != null) {
                captionsStage.requestFocus();
                return;
            }

            AccessibilityRoomService.URIData uriData = accessibilityRoomService.buildCaptionAssistantUriData(evenement);
            SwingNode captionsNode = new SwingNode();
            captionsBrowserSession = embeddedChromiumService.createBrowserSession(uriData.uri(), message -> {
            });
            embeddedChromiumService.attachToSwingNode(captionsNode, captionsBrowserSession);

            captionsStage = new Stage();
            captionsStage.setTitle("Sous-titres live");
            captionsStage.setScene(new Scene(new StackPane(captionsNode), 1100, 820));
            captionsStage.setOnHidden(event -> {
                if (captionsBrowserSession != null) {
                    captionsBrowserSession.close();
                    captionsBrowserSession = null;
                }
                captionsStage = null;
            });
            captionsStage.show();
            roomStatusLabel.setText("Assistant sous-titres ouvert dans l'application.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Assistant sous-titres", "Ouverture impossible : " + e.getMessage());
        }
    }

    @FXML
    private void pasteClipboardToTranscript() {
        String clipboardText = Clipboard.getSystemClipboard().getString();
        if (clipboardText == null || clipboardText.isBlank()) {
            showAlert(Alert.AlertType.INFORMATION, "Presse-papiers", "Aucun texte disponible a coller.");
            return;
        }

        if (transcriptArea == null || transcriptArea.getText() == null || transcriptArea.getText().isBlank()) {
            transcriptArea.setText(clipboardText.trim());
        } else {
            transcriptArea.appendText(System.lineSeparator() + clipboardText.trim());
        }
        roomStatusLabel.setText("Transcription collee depuis le presse-papiers.");
    }

    @FXML
    private void saveTranscript() {
        String transcript = transcriptArea == null ? "" : transcriptArea.getText().trim();
        if (transcript.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Transcription", "Ajoutez du texte avant d'enregistrer.");
            return;
        }

        try {
            Ressource resource = accessibilityRoomService.saveTranscript(evenement, transcript);
            roomStatusLabel.setText("Transcription enregistree dans Ressource: " + text(resource.getChemin_fichier_ressource()));
            refreshResources();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Transcription", "Enregistrement impossible : " + e.getMessage());
        }
    }

    @FXML
    private void clearTranscript() {
        if (transcriptArea != null) {
            transcriptArea.clear();
        }
        roomStatusLabel.setText("Zone de transcription videe.");
    }

    @FXML
    private void backToEventDetail() {
        try {
            closeEmbeddedBrowsers();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontFXML/EvenementDetail.fxml"));
            Parent root = loader.load();

            Stage stage = resolveCurrentStage();
            if (stage == null) {
                throw new IllegalStateException("Fenetre introuvable.");
            }

            stage.setScene(new Scene(root, DEFAULT_SCENE_WIDTH, DEFAULT_SCENE_HEIGHT));
            stage.setTitle("Detail evenement");
            stage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Retour impossible : " + e.getMessage());
        }
    }

    private void appendTranscriptLine(String line) {
        if (transcriptArea == null) {
            return;
        }

        String current = transcriptArea.getText();
        if (current == null || current.isBlank()) {
            transcriptArea.setText(line);
        } else {
            transcriptArea.appendText(System.lineSeparator() + line);
        }
    }

    private void closeEmbeddedBrowsers() {
        if (roomBrowserSession != null) {
            roomBrowserSession.close();
            roomBrowserSession = null;
        }
        if (captionsBrowserSession != null) {
            captionsBrowserSession.close();
            captionsBrowserSession = null;
        }
        if (captionsStage != null) {
            captionsStage.close();
            captionsStage = null;
        }
    }

    private void loadRoom() {
        try {
            Path embeddedRoomPage = accessibilityRoomService.createEmbeddedRoomPage(evenement);
            String targetUri = embeddedRoomPage.toUri().toString();

            if (roomBrowserSession != null) {
                roomBrowserSession.load(targetUri);
                roomBrowserSession.show();
                roomStatusLabel.setText("Appel ouvert dans une fenetre Chromium MedFlow.");
                return;
            }

            roomBrowserSession = embeddedChromiumService.createBrowserWindowSession(
                    "MedFlow Video Room",
                    targetUri,
                    message -> {
                        if (roomStatusLabel != null && message != null && !message.isBlank()) {
                            roomStatusLabel.setText(message);
                        }
                    },
                    this::handleEmbeddedQuery
            );
            roomBrowserSession.show();
            if (roomBrowserHost != null) {
                Label placeholder = new Label("L'appel video s'ouvre dans une fenetre Chromium MedFlow separee pour une meilleure compatibilite camera/micro.");
                placeholder.setWrapText(true);
                placeholder.setStyle(
                        "-fx-text-fill: #173745;" +
                                "-fx-font-size: 16px;" +
                                "-fx-font-weight: 700;" +
                                "-fx-background-color: #eef9ff;" +
                                "-fx-background-radius: 20;" +
                                "-fx-padding: 24;"
                );
                roomBrowserHost.getChildren().setAll(placeholder);
            }
            roomStatusLabel.setText("Appel video ouvert dans une fenetre Chromium MedFlow.");
        } catch (Exception e) {
            roomStatusLabel.setText("Ouverture de l'appel impossible. Le lien navigateur reste disponible.");
        }
    }

    private void refreshResources() {
        if (resourcesFlow == null || evenement == null) {
            return;
        }

        resourcesFlow.getChildren().clear();
        List<Ressource> resources = accessibilityRoomService.getAccessibilityResources(evenement.getId());
        for (Ressource resource : resources) {
            Label chip = new Label(text(resource.getNom_ressource()) + " | " + text(resource.getType_ressource()));
            chip.setStyle(
                    "-fx-background-color: #eef9ff;" +
                            "-fx-background-radius: 999;" +
                            "-fx-padding: 8 14;" +
                            "-fx-text-fill: #0e6077;" +
                            "-fx-font-weight: 700;"
            );
            resourcesFlow.getChildren().add(chip);
        }
    }

    private String handleEmbeddedQuery(String request) {
        if (request == null) {
            return "";
        }
        if (request.startsWith("saveTranscript:")) {
            String transcript = request.substring("saveTranscript:".length()).trim();
            if (transcript.isBlank()) {
                throw new IllegalArgumentException("Transcription vide.");
            }
            try {
                Ressource resource = accessibilityRoomService.saveTranscript(evenement, transcript);
                javafx.application.Platform.runLater(() -> {
                    if (transcriptArea != null) {
                        transcriptArea.setText(transcript);
                    }
                    if (roomStatusLabel != null) {
                        roomStatusLabel.setText("Transcription enregistree: " + text(resource.getChemin_fichier_ressource()));
                    }
                    refreshResources();
                });
                return "OK";
            } catch (Exception e) {
                throw new IllegalStateException("Enregistrement impossible: " + e.getMessage(), e);
            }
        }
        return "";
    }

    private Stage resolveCurrentStage() {
        if (roomBrowserHost != null && roomBrowserHost.getScene() != null) {
            return (Stage) roomBrowserHost.getScene().getWindow();
        }
        if (roomUrlLabel != null && roomUrlLabel.getScene() != null) {
            return (Stage) roomUrlLabel.getScene().getWindow();
        }
        if (transcriptArea != null && transcriptArea.getScene() != null) {
            return (Stage) transcriptArea.getScene().getWindow();
        }
        return null;
    }

    private String dateText(Evenement evenement) {
        String debut = evenement.getDate_debut_event() == null ? "-" : evenement.getDate_debut_event().toString();
        String fin = evenement.getDate_fin_event() == null ? "-" : evenement.getDate_fin_event().toString();
        return debut + " -> " + fin;
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private boolean isCurrentUserEventManager() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return false;
        }

        String role = text(user.getRoleSysteme()).trim().toUpperCase();
        return "ADMIN".equals(role) || "BADMIN".equals(role) || "STAFF".equals(role);
    }

    private boolean isCurrentUserAllowedToJoin() {
        return SessionManager.isLoggedIn() && isOnlineStatus() && !isPastEvent() && isJoinDay();
    }

    private boolean isOnlineStatus() {
        return evenement != null
                && evenement.getStatut_event() != null
                && evenement.getStatut_event().toLowerCase().contains("ligne");
    }

    private boolean isPastEvent() {
        if (evenement == null) {
            return false;
        }

        java.util.Date endDate = evenement.getDate_fin_event() != null ? evenement.getDate_fin_event() : evenement.getDate_debut_event();
        if (endDate == null) {
            return false;
        }

        LocalDate eventDate = endDate instanceof java.sql.Date sqlDate
                ? sqlDate.toLocalDate()
                : endDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return eventDate.isBefore(LocalDate.now());
    }

    private boolean isJoinDay() {
        if (evenement == null) {
            return false;
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = evenement.getDate_debut_event() instanceof java.sql.Date sqlStart
                ? sqlStart.toLocalDate()
                : evenement.getDate_debut_event() == null
                ? null
                : evenement.getDate_debut_event().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = evenement.getDate_fin_event() instanceof java.sql.Date sqlEnd
                ? sqlEnd.toLocalDate()
                : evenement.getDate_fin_event() == null
                ? null
                : evenement.getDate_fin_event().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        if (startDate == null && endDate == null) {
            return false;
        }
        if (startDate == null) {
            startDate = endDate;
        }
        if (endDate == null) {
            endDate = startDate;
        }
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle(title);
        alert.showAndWait();
    }
}
