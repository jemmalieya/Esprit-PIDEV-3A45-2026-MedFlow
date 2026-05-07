package tn.esprit.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HuggingFaceCareAiClient {

    private static final String DEFAULT_ENDPOINT = "https://router.huggingface.co/v1/chat/completions";
    private static final String DEFAULT_MODEL = "Qwen/Qwen2.5-7B-Instruct-1M:fastest";
    private static final List<String> DEFAULT_MODEL_CANDIDATES = List.of(
            "Qwen/Qwen2.5-7B-Instruct-1M:fastest",
            "Qwen/Qwen3-4B-Thinking-2507:fastest",
            "openai/gpt-oss-120b:fastest"
    );
    private static final String[] EVENT_TOKEN_KEYS = {"HUGGINGFACE_EVENT_HUB_TOKEN", "HF_EVENT_TOKEN"};
    private static final String[] SHARED_TOKEN_KEYS = {"HF_TOKEN", "HF_API_KEY", "HUGGINGFACE_API_KEY"};
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
        ConfigValue token = getTokenConfig();

        if (token.value().isBlank()) {
            return "Token absent: ajoutez HUGGINGFACE_EVENT_HUB_TOKEN, HF_EVENT_TOKEN, HF_API_KEY ou HUGGINGFACE_API_KEY dans Environment variables, VM options ou .env.local.";
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
            throw new IOException("Token Hugging Face manquant. Definissez HUGGINGFACE_EVENT_HUB_TOKEN, HF_EVENT_TOKEN, HF_API_KEY ou HUGGINGFACE_API_KEY.");
        }

        IOException lastError = null;
        List<String> modelsToTry = getModelsToTry();
        for (String model : modelsToTry) {
            String body = buildChatRequestBody(model, systemPrompt, userPrompt);
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
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String content = extractAssistantContent(response.body());
                if (!content.isBlank()) {
                    return content;
                }
                lastError = new IOException("Modele " + model + ": reponse 200 mais contenu assistant introuvable. Body: "
                        + limit(response.body(), 600));
                continue;
            }

            lastError = new IOException("Modele " + model + ": " + formatHttpError(response.statusCode(), response.body()));
            if (!shouldTryNextModel(response.statusCode(), response.body())) {
                throw lastError;
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new IOException("Aucun modele Hugging Face n'a pu repondre.");
    }

    private String getToken() {
        return getTokenConfig().value();
    }

    private ConfigValue getTokenConfig() {
        ConfigValue token = readConfigCandidates(EVENT_TOKEN_KEYS);
        if (token.value().isBlank()) {
            token = readConfigCandidates(SHARED_TOKEN_KEYS);
        }
        return token;
    }

    private String getEndpoint() {
        String endpoint = readConfig("HF_EVENT_CHAT_ENDPOINT").value();
        return endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim();
    }

    private String getModel() {
        String model = readConfig("HUGGINGFACE_EVENT_MODEL").value();
        if (model == null || model.isBlank()) {
            model = readConfig("HF_EVENT_MODEL").value();
        }
        return model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    private List<String> getModelsToTry() {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        addModelIfPresent(ordered, getModel());

        String configuredCandidates = readConfig("HF_EVENT_MODEL_CANDIDATES").value();
        if (configuredCandidates.isBlank()) {
            configuredCandidates = readConfig("HUGGINGFACE_EVENT_MODEL_CANDIDATES").value();
        }
        if (!configuredCandidates.isBlank()) {
            for (String token : configuredCandidates.split(",")) {
                addModelIfPresent(ordered, token);
            }
        }

        for (String candidate : DEFAULT_MODEL_CANDIDATES) {
            addModelIfPresent(ordered, candidate);
        }
        return new ArrayList<>(ordered);
    }

    private void addModelIfPresent(LinkedHashSet<String> models, String rawModel) {
        if (rawModel == null) {
            return;
        }
        String model = rawModel.trim();
        if (!model.isBlank()) {
            models.add(model);
        }
    }

    private String buildChatRequestBody(String model, String systemPrompt, String userPrompt) {
        return """
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
                jsonEscape(model),
                jsonEscape(systemPrompt),
                jsonEscape(userPrompt)
        );
    }

    private boolean shouldTryNextModel(int statusCode, String body) {
        if (statusCode == 401 || statusCode == 403) {
            return false;
        }
        String lowerBody = body == null ? "" : body.toLowerCase();
        return statusCode == 429
                || statusCode == 500
                || statusCode == 502
                || statusCode == 503
                || statusCode == 504
                || lowerBody.contains("loading")
                || lowerBody.contains("unavailable")
                || lowerBody.contains("provider")
                || lowerBody.contains("overloaded");
    }

    private ConfigValue readConfigCandidates(String... keys) {
        for (String key : keys) {
            ConfigValue value = readConfig(key);
            if (!value.value().isBlank()) {
                return value;
            }
        }
        return new ConfigValue("", "absent");
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

    public static String formatHttpError(int statusCode, String body) {
        String trimmedBody = body == null ? "" : body.trim();
        String prefix = "Erreur Hugging Face " + statusCode + ": ";

        if (trimmedBody.isBlank()) {
            return prefix + "reponse vide du service distant.";
        }

        String lowerBody = trimmedBody.toLowerCase();
        if (lowerBody.contains("<html") || lowerBody.contains("<!doctype html")) {
            return prefix + "page HTML renvoyee par le service distant.";
        }

        if (statusCode == 503 && lowerBody.contains("loading")) {
            String estimatedTime = extractJsonField(trimmedBody, "estimated_time");
            String suffix = estimatedTime.isBlank() ? "" : " Temps estime: " + estimatedTime + "s.";
            return prefix + "modele temporairement indisponible ou en chargement." + suffix;
        }

        return prefix + limitStatic(trimmedBody, 600);
    }

    private static String extractJsonField(String json, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"?([^\",}\\]]+)\"?",
                Pattern.CASE_INSENSITIVE).matcher(json == null ? "" : json);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static String limitStatic(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, max) + "...";
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
