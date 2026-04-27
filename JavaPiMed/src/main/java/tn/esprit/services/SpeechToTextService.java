package tn.esprit.services;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpeechToTextService {
    private TargetDataLine line;
    private ByteArrayOutputStream out;
    private Thread recordingThread;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final DialogflowAssistantService dfService = new DialogflowAssistantService();

    public synchronized void startRecording() throws LineUnavailableException {
        if (line != null && line.isOpen()) {
            return; // already recording
        }

        AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        out = new ByteArrayOutputStream();
        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (!Thread.currentThread().isInterrupted() && line.isOpen() && line.isRunning()) {
                int read = line.read(buffer, 0, buffer.length);
                if (read > 0) {
                    out.write(buffer, 0, read);
                }
            }
        }, "stt-recording-thread");
        recordingThread.setDaemon(true);
        recordingThread.start();
    }

    public synchronized String stopRecordingAndTranscribe() throws IOException, InterruptedException {
        if (line == null) {
            return null;
        }

        try {
            line.stop();
            line.close();
        } finally {
            line = null;
        }

        if (recordingThread != null) {
            recordingThread.interrupt();
            try {
                recordingThread.join(200);
            } catch (InterruptedException ignored) {
            }
            recordingThread = null;
        }

        byte[] audioBytes = out == null ? new byte[0] : out.toByteArray();
        if (out != null) {
            try {
                out.close();
            } catch (IOException ignored) {
            }
            out = null;
        }

        if (audioBytes.length == 0) {
            return null;
        }

        return transcribeLinear16(audioBytes, dfService.getDefaultLanguageCode());
    }

    private String transcribeLinear16(byte[] pcm16bytes, String languageCode) throws IOException, InterruptedException {
        String accessToken = dfService.getAccessTokenPublic();
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("No Google access token available for Speech-to-Text.");
        }

        String base64 = Base64.getEncoder().encodeToString(pcm16bytes);

        String json = "{\n" +
                "  \"config\": {\n" +
                "    \"encoding\": \"LINEAR16\",\n" +
                "    \"sampleRateHertz\": 16000,\n" +
                "    \"languageCode\": \"" + escapeJson(languageCode) + "\"\n" +
                "  },\n" +
                "  \"audio\": {\n" +
                "    \"content\": \"" + base64 + "\"\n" +
                "  }\n" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://speech.googleapis.com/v1/speech:recognize"))
                .timeout(Duration.ofSeconds(30))
                .header("authorization", "Bearer " + accessToken)
                .header("content-type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Speech-to-Text failed with HTTP " + response.statusCode() + ": " + response.body());
        }

        // extract first transcript
        Pattern p = Pattern.compile("\"transcript\"\\s*:\\s*\"((?:\\\\.|[^\\\\])*)\"", Pattern.DOTALL);
        Matcher m = p.matcher(response.body());
        if (m.find()) {
            return unescapeJson(m.group(1));
        }

        return null;
    }

    private String unescapeJson(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\\\n", "\n").replaceAll("\\\\\"", "\"");
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
