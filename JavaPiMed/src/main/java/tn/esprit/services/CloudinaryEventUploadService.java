package tn.esprit.services;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloudinaryEventUploadService {

    private static final Pattern SECURE_URL_PATTERN = Pattern.compile("\"secure_url\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern PUBLIC_ID_PATTERN = Pattern.compile("\"public_id\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public boolean isConfigured() {
        return isEventConfigured();
    }

    public boolean isEventConfigured() {
        return !config("CLOUDINARY_EVENT_CLOUD_NAME").isBlank()
                && !config("CLOUDINARY_EVENT_API_KEY").isBlank()
                && !config("CLOUDINARY_EVENT_API_SECRET").isBlank();
    }

    public UploadResult uploadEventImage(File file) throws IOException, InterruptedException {
        return uploadFile(
                file,
                "medflow/events",
                config("CLOUDINARY_EVENT_CLOUD_NAME"),
                config("CLOUDINARY_EVENT_API_KEY"),
                config("CLOUDINARY_EVENT_API_SECRET")
        );
    }

    public UploadResult uploadResourceFile(File file) throws IOException, InterruptedException {
        return uploadFile(
                file,
                "medflow/resources",
                config("CLOUDINARY_EVENT_CLOUD_NAME"),
                config("CLOUDINARY_EVENT_API_KEY"),
                config("CLOUDINARY_EVENT_API_SECRET")
        );
    }

    private UploadResult uploadFile(File file, String folder, String cloudName, String apiKey, String apiSecret) throws IOException, InterruptedException {
        if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            throw new IOException("Cloudinary non configure. Ajoutez CLOUDINARY_EVENT_CLOUD_NAME, CLOUDINARY_EVENT_API_KEY et CLOUDINARY_EVENT_API_SECRET.");
        }
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IOException("Fichier introuvable.");
        }

        long timestamp = Instant.now().getEpochSecond();
        String signature = sha1("folder=" + folder + "&timestamp=" + timestamp + apiSecret);
        String boundary = "MedFlowBoundary" + System.currentTimeMillis();

        byte[] body = buildMultipartBody(boundary, file, folder, timestamp, signature, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur Cloudinary " + response.statusCode() + ": " + limit(response.body(), 500));
        }

        String secureUrl = extract(response.body(), SECURE_URL_PATTERN);
        String publicId = extract(response.body(), PUBLIC_ID_PATTERN);
        if (secureUrl.isBlank()) {
            throw new IOException("Upload Cloudinary reussi mais URL introuvable.");
        }
        return new UploadResult(secureUrl, publicId);
    }

    private byte[] buildMultipartBody(String boundary, File file, String folder, long timestamp, String signature, String apiKey) throws IOException {
        String mime = Files.probeContentType(file.toPath());
        if (mime == null || mime.isBlank()) {
            mime = "application/octet-stream";
        }

        byte[] fileBytes = Files.readAllBytes(file.toPath());
        StringBuilder head = new StringBuilder();
        addField(head, boundary, "api_key", apiKey);
        addField(head, boundary, "timestamp", String.valueOf(timestamp));
        addField(head, boundary, "folder", folder);
        addField(head, boundary, "signature", signature);
        head.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(file.getName().replace("\"", ""))
                .append("\"\r\n")
                .append("Content-Type: ").append(mime).append("\r\n\r\n");

        byte[] headBytes = head.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] tailBytes = ("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] body = new byte[headBytes.length + fileBytes.length + tailBytes.length];
        System.arraycopy(headBytes, 0, body, 0, headBytes.length);
        System.arraycopy(fileBytes, 0, body, headBytes.length, fileBytes.length);
        System.arraycopy(tailBytes, 0, body, headBytes.length + fileBytes.length, tailBytes.length);
        return body;
    }

    private void addField(StringBuilder builder, String boundary, String name, String value) {
        builder.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
                .append(value == null ? "" : value).append("\r\n");
    }

    private String extract(String json, Pattern pattern) {
        Matcher matcher = pattern.matcher(json == null ? "" : json);
        return matcher.find() ? unescapeJson(matcher.group(1)).trim() : "";
    }

    private String sha1(String value) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IOException("Impossible de signer la requete Cloudinary.", e);
        }
    }

    private String unescapeJson(String value) {
        return value == null ? "" : value.replace("\\/", "/").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) return value == null ? "" : value;
        return value.substring(0, max) + "...";
    }

    private String config(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) return value.trim();
        value = System.getProperty(key);
        if (value != null && !value.isBlank()) return value.trim();
        value = readDotEnvValue(key, ".env.local");
        if (value == null || value.isBlank()) {
            value = readDotEnvValue(key, ".env");
        }
        return value == null ? "" : value.trim();
    }

    private String readDotEnvValue(String key, String fileName) {
        java.nio.file.Path path = java.nio.file.Path.of(fileName);
        if (!Files.exists(path)) {
            return "";
        }

        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.startsWith(key + "=")) {
                    continue;
                }
                String value = trimmed.substring((key + "=").length()).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        } catch (IOException ignored) {
            return "";
        }
        return "";
    }

    public record UploadResult(String secureUrl, String publicId) {
    }
}
