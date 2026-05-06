package tn.esprit.controllers;

import com.github.sarxos.webcam.Webcam;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tn.esprit.entities.User;
import tn.esprit.services.FacialRecognitionService;
import tn.esprit.services.UserService;
import tn.esprit.tools.SessionManager;

public class FaceLoginController {
    private static final double FACE_MATCH_THRESHOLD = 92.0;
    private static final int LOGIN_SAMPLE_TARGET = 12;
    private static final int LOGIN_MIN_VALID_SAMPLES = 3;
    private static final long CAMERA_READY_TIMEOUT_MS = 5000;
    @FXML private AnchorPane rootPane;
    @FXML private Label statusLabel;
    @FXML private Label faceStatusLabel;
    @FXML private Label instructionLabel;
    @FXML private Canvas cameraCanvas;
    @FXML private ProgressBar enrollmentProgress;
    @FXML private Button cancelButton;

    private FacialRecognitionService facialRecognitionService;
    private UserService userService;
    private User targetUser;
    private int attemptCount = 0;
    private final int MAX_ATTEMPTS = 3;
    private AnimationTimer animationTimer;
    private Webcam webcam;
    private volatile boolean cameraRunning = false;
    private volatile boolean authenticationStarted = false;
    private volatile BufferedImage latestFrame;

    public FaceLoginController() {
        this.userService = new UserService();
        this.facialRecognitionService = new FacialRecognitionService();
    }

    @FXML
    public void initialize() {
        setupUI();
        instructionLabel.setText("Enter your account email on login screen, then verify your face");
    }

    private void setupUI() {
        statusLabel.setText("🎥 Analyzing face...");
        statusLabel.setTextFill(Color.web("#1e91ad"));

        instructionLabel.setText("Keep your face centered in the frame\nProcessing...");
        instructionLabel.setWrapText(true);
        instructionLabel.setTextFill(Color.web("#235567"));

        faceStatusLabel.setText("Detecting face...");
        faceStatusLabel.setTextFill(Color.web("#f39c12"));

        enrollmentProgress.setProgress(0);

        cancelButton.setOnAction(e -> handleCancel());

        // Start live camera
        if (cameraCanvas != null) {
            startLiveCamera();
        }
    }

    private void startLiveCamera() {
        new Thread(() -> {
            try {
                webcam = Webcam.getDefault();
                if (webcam == null) {
                    Platform.runLater(() -> showError("No camera found"));
                    return;
                }
                webcam.setViewSize(new Dimension(640, 480));
                webcam.open();
                cameraRunning = true;

                double[] breathe = {0};

                animationTimer = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        if (!cameraRunning || webcam == null || !webcam.isOpen()) return;
                        BufferedImage frame = webcam.getImage();
                        if (frame == null) return;
                        latestFrame = frame;

                        GraphicsContext gc = cameraCanvas.getGraphicsContext2D();
                        WritableImage wImg = SwingFXUtils.toFXImage(frame, null);
                        gc.drawImage(wImg, 0, 0, cameraCanvas.getWidth(), cameraCanvas.getHeight());

                        // Animated face detection overlay
                        breathe[0] += 0.04;
                        double pulse = Math.sin(breathe[0]) * 4;
                        double cx = cameraCanvas.getWidth() / 2;
                        double cy = cameraCanvas.getHeight() / 2;
                        double fw = 160 + pulse;
                        double fh = 200 + pulse;

                        // Detection rectangle
                        gc.setStroke(Color.web("#1e91ad"));
                        gc.setLineWidth(2.5);
                        gc.strokeRect(cx - fw / 2, cy - fh / 2, fw, fh);

                        // Corner brackets
                        gc.setStroke(Color.web("#27ae60"));
                        gc.setLineWidth(4);
                        double b = 20;
                        double x = cx - fw / 2;
                        double y = cy - fh / 2;
                        gc.strokeLine(x, y, x + b, y);           gc.strokeLine(x, y, x, y + b);
                        gc.strokeLine(x + fw, y, x + fw - b, y); gc.strokeLine(x + fw, y, x + fw, y + b);
                        gc.strokeLine(x, y + fh, x + b, y + fh); gc.strokeLine(x, y + fh, x, y + fh - b);
                        gc.strokeLine(x + fw, y + fh, x + fw - b, y + fh); gc.strokeLine(x + fw, y + fh, x + fw, y + fh - b);

                        // Scan line
                        double scan = ((breathe[0] * 30) % (fh + 1));
                        gc.setStroke(Color.web("#1e91ad", 0.4));
                        gc.setLineWidth(1.5);
                        gc.strokeLine(x, y + scan, x + fw, y + scan);
                    }
                };
                animationTimer.start();
            } catch (Exception e) {
                Platform.runLater(() -> showError("Camera error: " + e.getMessage()));
            }
        }).start();
    }

    private void startFaceAuthentication() {
        if (authenticationStarted) {
            return;
        }
        authenticationStarted = true;
        new Thread(this::performFaceVerification).start();
    }

    private void performFaceVerification() {
        try {
            if (!waitForCameraReady()) {
                Platform.runLater(() -> showError("Camera not ready. Retry in a few seconds."));
                return;
            }

            List<BufferedImage> samples = new ArrayList<>();
            for (int i = 0; i < LOGIN_SAMPLE_TARGET; i++) {
                BufferedImage frame = getLatestOrDirectFrame();
                if (frame != null) {
                    samples.add(copyFrame(frame));
                }
                Thread.sleep(100);
            }

            String detectedFaceData;
            if (samples.size() >= LOGIN_MIN_VALID_SAMPLES) {
                detectedFaceData = facialRecognitionService.createEmbeddingFromFrames(samples);
            } else {
                detectedFaceData = null; // Désactive le fallback single-frame pour plus de sécurité
            }

            Platform.runLater(() -> {
                if (detectedFaceData == null || detectedFaceData.isBlank()) {
                    showError("Unable to capture a valid face sample. Retry.");
                    return;
                }

                if (targetUser != null && targetUser.getFaceReferenceEmbedding() != null) {
                    double confidence = facialRecognitionService.verifyFace(
                            detectedFaceData,
                            targetUser.getFaceReferenceEmbedding()
                    );
                    System.out.println("[DEBUG] Face login similarity score: " + confidence + "% (user=" + (targetUser.getEmailUser()!=null?targetUser.getEmailUser():targetUser.getId()) + ")");
                    updateVerificationProgress(confidence);
                } else {
                    showError("No enrolled face found for this user");
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> showError("Error: " + e.getMessage()));
        }
    }

    private void updateVerificationProgress(double confidence) {
        enrollmentProgress.setProgress(confidence / 100.0);

        if (confidence > 99.0) {
            // Embedding trop générique, refuse toute authentification
            statusLabel.setText("❌ Erreur sécurité : votre enrôlement facial est trop générique (" + String.format("%.1f%%", confidence) + "). Veuillez ré-enrôler votre visage dans de meilleures conditions ou désactiver la fonctionnalité.");
            statusLabel.setTextFill(Color.web("#e74c3c"));
            faceStatusLabel.setText("Erreur d'enrôlement");
            faceStatusLabel.setTextFill(Color.web("#e74c3c"));
            System.err.println("[SECURITY] Face embedding trop générique, refusé. Score=" + confidence);
            return;
        }

        if (confidence >= FACE_MATCH_THRESHOLD) {
            // Authentication successful
            statusLabel.setText("✅ Face recognized!");
            statusLabel.setTextFill(Color.web("#27ae60"));
            faceStatusLabel.setText("Match: " + String.format("%.1f%%", confidence));
            faceStatusLabel.setTextFill(Color.web("#27ae60"));
            // Delay and then redirect
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> handleLoginSuccess()));
            timeline.play();
        } else {
            // Match failed
            faceStatusLabel.setText("❌ Face not recognized");
            faceStatusLabel.setTextFill(Color.web("#e74c3c"));
            String msg = "Confidence: " + String.format("%.1f%%", confidence)
                    + " (min " + String.format("%.0f%%", FACE_MATCH_THRESHOLD) + ")";
            if (confidence > 80.0) {
                msg += "\n⚠️ Votre enrôlement facial est trop permissif. Veuillez ré-enrôler votre visage dans de bonnes conditions (lumière, face centrée).";
            }
            statusLabel.setText(msg);
            statusLabel.setTextFill(Color.web("#e74c3c"));
            attemptCount++;
            if (attemptCount >= MAX_ATTEMPTS) {
                showError("Max attempts reached. Use password login.");
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> returnToLogin()));
                timeline.play();
            }
        }
    }

    private BufferedImage copyFrame(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private boolean waitForCameraReady() {
        long deadline = System.currentTimeMillis() + CAMERA_READY_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (cameraRunning && webcam != null && webcam.isOpen() && latestFrame != null) {
                return true;
            }
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private BufferedImage getLatestOrDirectFrame() {
        BufferedImage frame = latestFrame;
        if (frame != null) {
            return frame;
        }
        try {
            if (webcam != null && webcam.isOpen()) {
                return webcam.getImage();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void stopCamera() {
        cameraRunning = false;
        if (animationTimer != null) animationTimer.stop();
        if (webcam != null && webcam.isOpen()) webcam.close();
    }

    private void handleLoginSuccess() {
        stopCamera();
        if (targetUser == null) {
            showError("User not found");
            return;
        }

        // Update user session
        targetUser.setFaceLastVerifiedAt(java.time.LocalDateTime.now());
        SessionManager.setCurrentUser(targetUser);

        // Navigate to dashboard
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();

            String fxmlFile = "/EvenementDashboard.fxml";
            String title = "MedFlow - Tableau de bord";

            if ("ADMIN".equals(targetUser.getRoleSysteme())) {
                fxmlFile = "/AdminDashboard.fxml";
                title = "MedFlow - Tableau de bord Admin";
            }

            navigateToStage(stage, fxmlFile, title);
        } catch (Exception e) {
            showError("Navigation error: " + e.getMessage());
        }
    }

    private void returnToLogin() {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            navigateToStage(stage, "/FrontFXML/Login.fxml", "MedFlow - Connexion");
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    private void navigateToStage(Stage stage, String resourcePath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1400, 820);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            showError("Impossible d'ouvrir la page suivante. " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        stopCamera();
        returnToLogin();
    }

    /**
     * Set the user to authenticate
     */
    public void setTargetUser(User user) {
        this.targetUser = user;
        if (user == null) {
            showError("No target account selected for face login");
            return;
        }
        startFaceAuthentication();
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setTextFill(Color.web("#e74c3c"));
        });
    }
}
