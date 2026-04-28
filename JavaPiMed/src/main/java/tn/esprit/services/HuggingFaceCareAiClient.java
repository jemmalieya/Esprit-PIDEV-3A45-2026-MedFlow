package tn.esprit.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HuggingFaceCareAiClient {

    private static final String DEFAULT_ENDPOINT = "https://router.huggingface.co/v1/chat/completions";
    private static final String DEFAULT_MODEL = "Qwen/Qwen3-4B-Thinking-2507:fastest";
    private static final Pattern ASSISTANT_CONTENT_PATTERN = Pattern.compile(
            "\"message\"\\s*:\\s*\\{[^}]*\"role\"\\s*:\\s*\"assistant\"[^}]*\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL
    );
    private static final Pattern ANY_CONTENT_PATTERN = Pattern.compile(
            "\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.DOTALL
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    public boolean isConfigured() {
        return !getToken().isBlank();
    }

    public String getConfigurationStatus() {
        ConfigValue token = readConfig("HUGGINGFACE_EVENT_HUB_TOKEN");
        if (token.value().isBlank()) {
            token = readConfig("HF_EVENT_TOKEN");
        }

        if (token.value().isBlank()) {
            return "Token absent: ajoutez HUGGINGFACE_EVENT_HUB_TOKEN dans Environment variables, VM options ou .env.local.";
        }

        return "Token detecte via " + token.source() + " (" + maskToken(token.value()) + "), modele "
                + getModel() + ", endpoint " + getEndpoint() + ".";
    }

    public boolean isCloudRequired() {
        String value = readConfig("HF_EVENT_REQUIRE_CLOUD").value();
        return "true".equalsIgnoreCase(value)
                || "1".equals(value)
                || "yes".equalsIgnoreCase(value)
                || "oui".equalsIgnoreCase(value);
    }

    public String generateCareReply(String systemPrompt, String userPrompt) throws IOException, InterruptedException {
        String token = getToken();
        if (token.isBlank()) {
            throw new IOException("HUGGINGFACE_EVENT_HUB_TOKEN manquant.");
        }

        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                  ],
                  "temperature": 0.65,
                  "max_tokens": 420
                }
                """.formatted(
                jsonEscape(getModel()),
                jsonEscape(systemPrompt),
                jsonEscape(userPrompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getEndpoint()))
                .timeout(Duration.ofSeconds(35))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "MedFlow-Care-Assistant/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur Hugging Face " + response.statusCode() + ": " + response.body());
        }

        String content = extractAssistantContent(response.body());
        if (content.isBlank()) {
            throw new IOException("Reponse Hugging Face 200 mais contenu assistant introuvable. Body: " + limit(response.body(), 600));
        }
        return content;
    }

    private String getToken() {
        ConfigValue token = readConfig("HUGGINGFACE_EVENT_HUB_TOKEN");
        if (token.value().isBlank()) {
            token = readConfig("HF_EVENT_TOKEN");
        }
        return token.value();
    }

    private String getEndpoint() {
        String endpoint = readConfig("HF_EVENT_CHAT_ENDPOINT").value();
        return endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim();
    }

    private String getModel() {
        String model = readConfig("HUGGINGFACE_EVENT_MODEL").value();
        return model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    private ConfigValue readConfig(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return new ConfigValue(value.trim(), "environment " + key);
        }

        value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return new ConfigValue(value.trim(), "VM option " + key);
        }

        value = readDotEnvValue(key, ".env.local");
        if (value == null || value.isBlank()) {
            value = readDotEnvValue(key, ".env");
        }
        if (value != null && !value.isBlank()) {
            return new ConfigValue(value.trim(), "file " + key);
        }

        return new ConfigValue("", "absent");
    }

    private String readDotEnvValue(String key, String fileName) {
        Path path = Path.of(fileName);
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

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "token masque";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 4);
    }

    private String extractAssistantContent(String json) {
        String body = json == null ? "" : json;
        Matcher matcher = ASSISTANT_CONTENT_PATTERN.matcher(body);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1)).trim();
        }

        matcher = ANY_CONTENT_PATTERN.matcher(body);
        String last = "";
        while (matcher.find()) {
            last = unescapeJson(matcher.group(1));
        }
        return last.trim();
    }

    private String limit(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max) + "...";
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                out.append(c);
                continue;
            }

            char next = value.charAt(++i);
            switch (next) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (i + 4 < value.length()) {
                        String hex = value.substring(i + 1, i + 5);
                        try {
                            out.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException e) {
                            out.append("\\u").append(hex);
                            i += 4;
                        }
                    } else {
                        out.append("\\u");
                    }
                }
                default -> out.append(next);
            }
        }
        return out.toString();
    }

    private record ConfigValue(String value, String source) {
    }
}
