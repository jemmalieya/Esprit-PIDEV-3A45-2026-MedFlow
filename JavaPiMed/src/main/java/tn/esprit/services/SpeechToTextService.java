package tn.esprit.services;

import org.vosk.Model;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * SpeechToTextService using Vosk for offline speech recognition.
 * No API costs, works entirely offline.
 */
public class SpeechToTextService {
    static {
        try {
            LibVosk.setLogLevel(LogLevel.WARNINGS);
        } catch (Throwable ignored) {
        }
    }

    private static final float SAMPLE_RATE = 16000.0f;
    private static final double SILENCE_RMS_THRESHOLD = 250.0;
    private static final Pattern VOSK_TEXT_PATTERN = Pattern.compile("\\\"text\\\"\\s*:\\s*\\\"((?:\\\\\\\"|[^\\\"])*)\\\"");
    private static final Pattern VOSK_PARTIAL_PATTERN = Pattern.compile("\\\"partial\\\"\\s*:\\s*\\\"((?:\\\\\\\"|[^\\\"])*)\\\"");

    private TargetDataLine line;
    private ByteArrayOutputStream out;
    private Thread recordingThread;
    private volatile boolean recordingActive;
    private final Properties properties = loadProperties();

    public synchronized void startRecording() throws LineUnavailableException {
        if (line != null && line.isOpen()) {
            return;
        }

        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        recordingActive = true;

        out = new ByteArrayOutputStream();
        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            try {
                while (!Thread.currentThread().isInterrupted() && recordingActive) {
                    int read = line.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        out.write(buffer, 0, read);
                    }
                }
            } catch (Exception ignored) {
            }
        }, "stt-recording-thread");
        recordingThread.setDaemon(true);
        recordingThread.start();
    }

    public synchronized String stopRecordingAndTranscribe() throws Exception {
        if (line == null) {
            throw new IllegalStateException("Recording is not active. Please start voice capture first.");
        }

        try {
            recordingActive = false;
            line.stop();
            line.flush();
            line.close();
        } finally {
            line = null;
        }

        if (recordingThread != null) {
            recordingThread.interrupt();
            try {
                recordingThread.join(600);
            } catch (InterruptedException ignored) {
            }
            recordingThread = null;
        }

        byte[] audioBytes = out == null ? new byte[0] : out.toByteArray();
        if (out != null) {
            try {
                out.close();
            } catch (Exception ignored) {
            }
            out = null;
        }

        if (audioBytes.length == 0) {
            throw new IllegalStateException("No audio was captured from microphone. Check your input device and permissions.");
        }

        // 3200 bytes ~= 100ms at 16kHz/16-bit/mono. Ignore ultra-short clips.
        if (audioBytes.length < 3200) {
            throw new IllegalStateException("Audio clip is too short. Hold recording for at least 1-2 seconds while speaking.");
        }

        return transcribeWithVosk(audioBytes);
    }

    private String transcribeWithVosk(byte[] pcm16bytes) throws Exception {
        Path modelPath = resolveModelPath();
        if (modelPath == null) {
            throw new IllegalStateException("Vosk model folder not found. Set VOSK_MODEL_PATH or vosk.model.path to a valid directory.");
        }

        double rms = computeRms(pcm16bytes);
        if (rms < SILENCE_RMS_THRESHOLD) {
            throw new IllegalStateException("Microphone input is near silence. Check microphone permissions/input device and speak louder.");
        }

        String transcript = recognizeWithModel(modelPath, pcm16bytes);
        if (transcript != null && !transcript.isBlank()) {
            return transcript;
        }

        byte[] boosted = boostPcm16(pcm16bytes);
        transcript = recognizeWithModel(modelPath, boosted);
        if (transcript != null && !transcript.isBlank()) {
            return transcript;
        }

        throw new IllegalStateException("Speech was captured but not recognized. Try an English Vosk model that matches your speech, or switch to a matching language model.");
    }

    private String recognizeWithModel(Path modelPath, byte[] pcm16bytes) throws Exception {
        try (Model model = new Model(modelPath.toString()); Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {
            ByteArrayInputStream stream = new ByteArrayInputStream(pcm16bytes);
            byte[] buffer = new byte[4096];
            int bytesRead;
            StringBuilder transcriptBuilder = new StringBuilder();
            String latestPartial = null;

            while ((bytesRead = stream.read(buffer)) >= 0) {
                if (bytesRead == 0) {
                    continue;
                }

                boolean utteranceCompleted = recognizer.acceptWaveForm(buffer, bytesRead);
                if (utteranceCompleted) {
                    appendToken(transcriptBuilder, extractTextField(recognizer.getResult()));
                } else {
                    String partial = extractPartialField(recognizer.getPartialResult());
                    if (partial != null && !partial.isBlank()) {
                        latestPartial = partial;
                    }
                }
            }

            appendToken(transcriptBuilder, extractTextField(recognizer.getFinalResult()));
            if (transcriptBuilder.toString().isBlank()) {
                appendToken(transcriptBuilder, latestPartial);
            }

            String normalized = transcriptBuilder.toString().trim().replaceAll("\\s+", " ");
            return normalized.isBlank() ? null : normalized;
        } catch (Exception ex) {
            throw new Exception("Offline speech recognition failed: " + ex.getMessage(), ex);
        }
    }

    private Path resolveModelPath() {
        List<Path> candidates = new ArrayList<>();

        String env = System.getenv("VOSK_MODEL_PATH");
        if (env != null && !env.isBlank()) {
            candidates.add(Paths.get(env.trim()));
        }

        String propertyValue = properties.getProperty("vosk.model.path");
        if (propertyValue != null && !propertyValue.isBlank()) {
            candidates.add(Paths.get(propertyValue.trim()));
        }

        candidates.add(Paths.get("models/vosk-model-small-en-us-0.15"));
        candidates.add(Paths.get("JavaPiMed/models/vosk-model-small-en-us-0.15"));
        candidates.add(Paths.get("src/main/resources/models/vosk-model-small-en-us-0.15"));
        candidates.add(Paths.get("JavaPiMed/src/main/resources/models/vosk-model-small-en-us-0.15"));

        for (Path candidate : candidates) {
            if (candidate == null) {
                continue;
            }

            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.exists(normalized) && Files.isDirectory(normalized)) {
                return normalized;
            }
        }

        for (Path modelsRoot : List.of(
                Paths.get("models"),
                Paths.get("JavaPiMed/models"),
                Paths.get("src/main/resources/models"),
                Paths.get("JavaPiMed/src/main/resources/models")
        )) {
            Path discovered = discoverFirstVoskModel(modelsRoot);
            if (discovered != null) {
                return discovered;
            }
        }

        return null;
    }

    private Path discoverFirstVoskModel(Path modelsRoot) {
        if (modelsRoot == null) {
            return null;
        }

        Path normalizedRoot = modelsRoot.toAbsolutePath().normalize();
        if (!Files.exists(normalizedRoot) || !Files.isDirectory(normalizedRoot)) {
            return null;
        }

        try (Stream<Path> children = Files.list(normalizedRoot)) {
            return children
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .filter(path -> path.getFileName().toString().toLowerCase().startsWith("vosk-model"))
                    .filter(path -> Files.exists(path.resolve("am")) || Files.exists(path.resolve("conf")))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private double computeRms(byte[] pcm16bytes) {
        if (pcm16bytes == null || pcm16bytes.length < 2) {
            return 0.0;
        }

        long sumSquares = 0L;
        int samples = 0;
        for (int i = 0; i + 1 < pcm16bytes.length; i += 2) {
            int low = pcm16bytes[i] & 0xFF;
            int high = pcm16bytes[i + 1];
            short sample = (short) ((high << 8) | low);
            int value = sample;
            sumSquares += (long) value * value;
            samples++;
        }

        if (samples == 0) {
            return 0.0;
        }
        return Math.sqrt((double) sumSquares / samples);
    }

    private byte[] boostPcm16(byte[] pcm16bytes) {
        if (pcm16bytes == null || pcm16bytes.length < 2) {
            return pcm16bytes;
        }

        int peak = 1;
        for (int i = 0; i + 1 < pcm16bytes.length; i += 2) {
            int low = pcm16bytes[i] & 0xFF;
            int high = pcm16bytes[i + 1];
            short sample = (short) ((high << 8) | low);
            int abs = Math.abs(sample);
            if (abs > peak) {
                peak = abs;
            }
        }

        double gain = Math.min(6.0, 12000.0 / peak);
        if (gain <= 1.05) {
            return pcm16bytes;
        }

        byte[] boosted = new byte[pcm16bytes.length];
        for (int i = 0; i + 1 < pcm16bytes.length; i += 2) {
            int low = pcm16bytes[i] & 0xFF;
            int high = pcm16bytes[i + 1];
            short sample = (short) ((high << 8) | low);

            int amplified = (int) Math.round(sample * gain);
            if (amplified > Short.MAX_VALUE) {
                amplified = Short.MAX_VALUE;
            } else if (amplified < Short.MIN_VALUE) {
                amplified = Short.MIN_VALUE;
            }

            boosted[i] = (byte) (amplified & 0xFF);
            boosted[i + 1] = (byte) ((amplified >> 8) & 0xFF);
        }
        return boosted;
    }

    private String extractTextField(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Matcher matcher = VOSK_TEXT_PATTERN.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replace("\\\"", "\"");
    }

    private String extractPartialField(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Matcher matcher = VOSK_PARTIAL_PATTERN.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replace("\\\"", "\"");
    }

    private void appendToken(StringBuilder builder, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(token.trim());
    }

    private Properties loadProperties() {
        Properties loaded = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("dialogflow.properties")) {
            if (inputStream != null) {
                loaded.load(inputStream);
            }
        } catch (IOException ignored) {
        }
        return loaded;
    }
}
