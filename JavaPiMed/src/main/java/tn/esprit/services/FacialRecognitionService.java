package tn.esprit.services;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

public class FacialRecognitionService {
    private static final int EMBED_W = 32;
    private static final int EMBED_H = 32;
    private boolean isInitialized = false;

    public FacialRecognitionService() {
        initialize();
    }

    private void initialize() {
        try {
            isInitialized = true;
            System.out.println("Facial Recognition Service initialized (local mode)");
        } catch (Exception e) {
            System.err.println("Face Recognition initialization failed: " + e.getMessage());
            isInitialized = false;
        }
    }

    public int detectFaces() {
        if (!isInitialized) return 0;
        return 1;
    }

    public String captureFaceForEnrollment(int userId) {
        return null;
    }

    public double verifyFace(String detectedFaceData, String storedFaceData) {
        if (!isInitialized || detectedFaceData == null || storedFaceData == null) {
            return 0.0;
        }

        try {
            double[] detected = parseEmbedding(detectedFaceData);
            double[] stored = parseEmbedding(storedFaceData);
            if (detected == null || stored == null || detected.length != stored.length) {
                return 0.0;
            }
            double cosine = cosineSimilarity(detected, stored);
            double normalizedScore = Math.max(0.0, Math.min(100.0, ((cosine + 1.0) / 2.0) * 100.0));
            return normalizedScore;
        } catch (Exception e) {
            System.err.println("Face verification error: " + e.getMessage());
            return 0.0;
        }
    }

    public String createEmbeddingFromImage(BufferedImage frame) {
        if (!isInitialized || frame == null) {
            return null;
        }

        try {
            double[] vector = buildEmbeddingVector(frame);
            if (vector == null) {
                return null;
            }
            return serializeEmbedding(vector);
        } catch (Exception e) {
            return null;
        }
    }

    public String createEmbeddingFromFrames(List<BufferedImage> frames) {
        if (frames == null || frames.isEmpty()) {
            return null;
        }

        List<double[]> vectors = new ArrayList<>();
        for (BufferedImage frame : frames) {
            double[] vec = buildEmbeddingVector(frame);
            if (vec != null) {
                vectors.add(vec);
            }
        }

        if (vectors.isEmpty()) {
            return null;
        }

        int len = vectors.get(0).length;
        double[] avg = new double[len];
        for (double[] v : vectors) {
            for (int i = 0; i < len; i++) {
                avg[i] += v[i];
            }
        }
        for (int i = 0; i < len; i++) {
            avg[i] /= vectors.size();
        }
        normalizeInPlace(avg);
        return serializeEmbedding(avg);
    }

    private double[] buildEmbeddingVector(BufferedImage src) {
        if (src == null) {
            return null;
        }

        int crop = Math.min(src.getWidth(), src.getHeight());
        if (crop <= 0) {
            return null;
        }

        int x = (src.getWidth() - crop) / 2;
        int y = (src.getHeight() - crop) / 2;
        BufferedImage center = src.getSubimage(x, y, crop, crop);

        BufferedImage resized = new BufferedImage(EMBED_W, EMBED_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(center, 0, 0, EMBED_W, EMBED_H, null);
        g2d.dispose();

        double[] vector = new double[EMBED_W * EMBED_H];
        int idx = 0;
        for (int j = 0; j < EMBED_H; j++) {
            for (int i = 0; i < EMBED_W; i++) {
                int rgb = resized.getRGB(i, j);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                vector[idx++] = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
            }
        }

        normalizeInPlace(vector);
        return vector;
    }

    private void normalizeInPlace(double[] vec) {
        double norm = 0.0;
        for (double v : vec) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm < 1e-9) {
            return;
        }
        for (int i = 0; i < vec.length; i++) {
            vec[i] /= norm;
        }
    }

    private String serializeEmbedding(double[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8);
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.US, "%.6f", vector[i]));
        }
        return Base64.getEncoder().encodeToString(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public double[] parseEmbedding(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(raw), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            decoded = raw;
        }

        String[] parts = decoded.split(",");
        if (parts.length < 64) {
            return null;
        }

        double[] vec = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vec[i] = Double.parseDouble(parts[i].trim());
        }
        normalizeInPlace(vec);
        return vec;
    }

    public double cosineSimilarity(double[] a, double[] b) {
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void release() {
        // Cleanup (none needed for mock)
    }
}
