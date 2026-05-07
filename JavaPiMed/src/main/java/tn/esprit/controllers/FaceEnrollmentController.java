package tn.esprit.controllers;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import tn.esprit.services.FacialRecognitionService;

public class FaceEnrollmentController {
    @FXML private AnchorPane rootPane;
    @FXML private Label statusLabel;
    @FXML private Label instructionLabel;
    @FXML private Canvas cameraCanvas;
    @FXML private ProgressBar enrollmentProgress;
    @FXML private Button startButton;
    @FXML private Button skipButton;
    @FXML private Button confirmButton;

    private FacialRecognitionService facialRecognitionService;
    private String capturedEmbedding = null;
    private OnEnrollmentComplete enrollmentCallback;
    private AnimationTimer animationTimer;
    private Webcam webcam;
    private volatile boolean cameraRunning = false;
    private volatile BufferedImage latestFrame;
    private static final int SAMPLE_TARGET = 12;
    private static final int MIN_VALID_SAMPLES = 8;
    private static final double ENROLLMENT_SIMILARITY_MIN = 95.0;
    private static final long CAMERA_READY_TIMEOUT_MS = 5000;

    public interface OnEnrollmentComplete {
        void onSuccess(String faceEmbedding);
        void onSkip();
    }

    public FaceEnrollmentController() {
        this.facialRecognitionService = new FacialRecognitionService();
    }

    @FXML
    public void initialize() {
        setupUI();
    }

    private void setupUI() {
        statusLabel.setText("Ready for enrollment");
        statusLabel.setTextFill(Color.web("#1e91ad"));

        instructionLabel.setText("Click 'Start Enrollment' to begin\nPosition your face centered in the frame");
        instructionLabel.setWrapText(true);

        enrollmentProgress.setProgress(0);

        startButton.setOnAction(e -> handleStartEnrollment());
        skipButton.setOnAction(e -> handleSkip());
        confirmButton.setOnAction(e -> handleConfirm());
        confirmButton.setDisable(true);

        // Start real camera feed
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
                        if (!cameraRunning || !webcam.isOpen()) return;
                        BufferedImage frame = webcam.getImage();
                        if (frame == null) return;
                        latestFrame = frame;
                        GraphicsContext gc = cameraCanvas.getGraphicsContext2D();
                        WritableImage wImg = SwingFXUtils.toFXImage(frame, null);
                        gc.drawImage(wImg, 0, 0, cameraCanvas.getWidth(), cameraCanvas.getHeight());
                        // Corner brackets overlay
                        breathe[0] += 0.04;
                        double pulse = Math.sin(breathe[0]) * 4;
                        double cx = cameraCanvas.getWidth() / 2;
                        double cy = cameraCanvas.getHeight() / 2;
                        double fw = 140 + pulse, fh = 180 + pulse;
                        double bx = cx - fw / 2, by = cy - fh / 2;
                        double b = 20;
                        gc.setStroke(Color.web("#27ae60"));
                        gc.setLineWidth(4);
                        gc.strokeLine(bx, by, bx + b, by);
                        gc.strokeLine(bx, by, bx, by + b);
                        gc.strokeLine(bx + fw, by, bx + fw - b, by);
                        gc.strokeLine(bx + fw, by, bx + fw, by + b);
                        gc.strokeLine(bx, by + fh, bx + b, by + fh);
                        gc.strokeLine(bx, by + fh, bx, by + fh - b);
                        gc.strokeLine(bx + fw, by + fh, bx + fw - b, by + fh);
                        gc.strokeLine(bx + fw, by + fh, bx + fw, by + fh - b);
                    }
                };
                animationTimer.start();
            } catch (Exception e) {
                Platform.runLater(() -> showError("Camera error: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleStartEnrollment() {
        startButton.setDisable(true);
        skipButton.setDisable(true);

        statusLabel.setText("Recording face...");
        statusLabel.setTextFill(Color.web("#1e91ad"));
        instructionLabel.setText("Hold still while we capture your face");

        // Simulate face recording (mock)
        simulateEnrollment();
    }

    private void simulateEnrollment() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    statusLabel.setText("Preparing camera...");
                    enrollmentProgress.setProgress(0.05);
                });

                if (!waitForCameraReady()) {
                    Platform.runLater(() -> {
                        showError("Camera not ready. Please retry in a few seconds.");
                        startButton.setDisable(false);
                        skipButton.setDisable(false);
                    });
                    return;
                }

                List<BufferedImage> samples = new ArrayList<>();
                for (int i = 0; i < SAMPLE_TARGET; i++) {
                    BufferedImage frame = getLatestOrDirectFrame();
                    if (frame != null) {
                        samples.add(copyFrame(frame));
                    }
                    final double progress = 0.1 + ((i + 1) / (double) SAMPLE_TARGET) * 0.75;
                    Platform.runLater(() -> enrollmentProgress.setProgress(progress));
                    Thread.sleep(120);
                }

                if (samples.size() < MIN_VALID_SAMPLES) {
                    Platform.runLater(() -> {
                        showError("Pas assez d'images valides. Gardez votre visage centré et bien éclairé.");
                        startButton.setDisable(false);
                        skipButton.setDisable(false);
                    });
                    return;
                }

                // Calculer la similarité moyenne entre toutes les frames
                List<double[]> vectors = new ArrayList<>();
                for (BufferedImage f : samples) {
                    double[] v = facialRecognitionService.parseEmbedding(facialRecognitionService.createEmbeddingFromImage(f));
                    if (v != null) vectors.add(v);
                }
                double simSum = 0.0;
                int simCount = 0;
                for (int i = 0; i < vectors.size(); i++) {
                    for (int j = i + 1; j < vectors.size(); j++) {
                        double cos = facialRecognitionService.cosineSimilarity(vectors.get(i), vectors.get(j));
                        double norm = Math.max(0.0, Math.min(100.0, ((cos + 1.0) / 2.0) * 100.0));
                        simSum += norm;
                        simCount++;
                    }
                }
                double simAvg = simCount > 0 ? (simSum / simCount) : 0.0;
                if (simAvg < ENROLLMENT_SIMILARITY_MIN) {
                    Platform.runLater(() -> {
                        showError("Enrôlement instable (" + String.format("%.1f", simAvg) + "% de similarité). Gardez la même expression, évitez les mouvements et la lumière changeante.");
                        startButton.setDisable(false);
                        skipButton.setDisable(false);
                    });
                    return;
                }

                capturedEmbedding = facialRecognitionService.createEmbeddingFromFrames(samples);
                Platform.runLater(() -> {
                    if (capturedEmbedding == null || capturedEmbedding.isBlank()) {
                        showError("No usable face frame captured. Retry in better lighting.");
                        startButton.setDisable(false);
                        skipButton.setDisable(false);
                        return;
                    }
                    statusLabel.setText("✅ Face enrollment successful!");
                    statusLabel.setTextFill(Color.web("#27ae60"));
                    instructionLabel.setText("Your face has been captured successfully.");
                    enrollmentProgress.setProgress(1.0);
                    confirmButton.setDisable(false);
                    startButton.setDisable(false);
                    skipButton.setDisable(false);
                    startButton.setText("Retry");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error: " + e.getMessage()));
            }
        }).start();
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

    private BufferedImage copyFrame(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    @FXML
    private void handleConfirm() {
        if (capturedEmbedding != null && enrollmentCallback != null) {
            enrollmentCallback.onSuccess(capturedEmbedding);
            closeWindow();
        }
    }

    @FXML
    private void handleSkip() {
        if (enrollmentCallback != null) {
            enrollmentCallback.onSkip();
        }
        closeWindow();
    }

    private void stopCamera() {
        cameraRunning = false;
        if (animationTimer != null) animationTimer.stop();
        if (webcam != null && webcam.isOpen()) webcam.close();
    }

    private void closeWindow() {
        stopCamera();
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            System.err.println("Error closing window: " + e.getMessage());
        }
    }

    public void setOnEnrollmentComplete(OnEnrollmentComplete callback) {
        this.enrollmentCallback = callback;
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setTextFill(Color.web("#e74c3c"));
        });
    }
}
