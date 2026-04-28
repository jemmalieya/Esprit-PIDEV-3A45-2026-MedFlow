package tn.esprit.services;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class GroqSpeechToTextServiceReclamation {

    private static final String GROQ_TRANSCRIPTION_URL =
            "https://api.groq.com/openai/v1/audio/transcriptions";

    private static final String MODEL = "whisper-large-v3-turbo";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String transcrire(File audioFile) throws Exception {

        String apiKey = System.getenv("GROQ_RECLAMATION_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Clé Groq manquante : GROQ_RECLAMATION_API_KEY");
        }

        if (audioFile == null || !audioFile.exists()) {
            throw new IllegalArgumentException("Fichier audio introuvable.");
        }

        String boundary = "----MedFlowBoundary" + System.currentTimeMillis();

        byte[] multipartBody = buildMultipartBody(boundary, audioFile);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_TRANSCRIPTION_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                    "Erreur Groq Speech-to-Text HTTP "
                            + response.statusCode()
                            + " : "
                            + response.body()
            );
        }

        JSONObject json = new JSONObject(response.body());

        String texte = json.optString("text", "").trim();

        if (texte.isBlank()) {
            throw new RuntimeException("Aucun texte détecté dans l'audio.");
        }

        return texte;
    }

    private byte[] buildMultipartBody(String boundary, File audioFile) throws IOException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        addTextPart(output, boundary, "model", MODEL);
        addTextPart(output, boundary, "response_format", "json");
        addTextPart(output, boundary, "temperature", "0");

        addTextPart(
                output,
                boundary,
                "prompt",
                "Réclamation patient dans une application médicale MedFlow."
        );

        addFilePart(output, boundary, "file", audioFile);

        write(output, "--" + boundary + "--\r\n");

        return output.toByteArray();
    }

    private void addTextPart(
            ByteArrayOutputStream output,
            String boundary,
            String name,
            String value
    ) throws IOException {

        write(output, "--" + boundary + "\r\n");
        write(output, "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        write(output, value + "\r\n");
    }

    private void addFilePart(
            ByteArrayOutputStream output,
            String boundary,
            String fieldName,
            File file
    ) throws IOException {

        String fileName = file.getName();

        write(output, "--" + boundary + "\r\n");
        write(output, "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n");
        write(output, "Content-Type: audio/wav\r\n\r\n");

        output.write(Files.readAllBytes(file.toPath()));

        write(output, "\r\n");
    }

    private void write(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }
}