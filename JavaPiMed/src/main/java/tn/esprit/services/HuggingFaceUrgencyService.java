package tn.esprit.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HuggingFaceUrgencyService {

    private static final String DEFAULT_MODEL_ID = "MoritzLaurer/mDeBERTa-v3-base-mnli-xnli";
    private static final String DEFAULT_URGENCY = "mid";
    private static final Pattern LABELS_PATTERN = Pattern.compile("\"labels\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern SCORES_PATTERN = Pattern.compile("\"scores\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern STRING_PATTERN = Pattern.compile("\"(.*?)\"");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Properties properties = loadProperties();

    public String classifyUrgencyLevel(String motif) {
        String sanitizedMotif = sanitizeForSearch(motif);
        if (sanitizedMotif.isBlank()) {
            return DEFAULT_URGENCY;
        }

        String apiToken = getRequiredValue("hf.api.token", "HF_API_TOKEN");
        if (apiToken.isBlank()) {
            return estimateUrgencyLocally(sanitizedMotif);
        }

        String modelId = getOptionalValue("hf.model.id", "HF_MODEL_ID", DEFAULT_MODEL_ID);
        String payload = buildPayload(motif);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api-inference.huggingface.co/models/" + modelId))
                    .header("accept", "application/json")
                    .header("authorization", "Bearer " + apiToken)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return estimateUrgencyLocally(sanitizedMotif);
            }

            String parsedLabel = extractBestLabel(response.body());
            if (parsedLabel == null || parsedLabel.isBlank()) {
                return estimateUrgencyLocally(sanitizedMotif);
            }

            return normalizeUrgencyLabel(parsedLabel);
        } catch (IOException ex) {
            return estimateUrgencyLocally(sanitizedMotif);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return estimateUrgencyLocally(sanitizedMotif);
        } catch (Exception ex) {
            return estimateUrgencyLocally(sanitizedMotif);
        }
    }

    private String buildPayload(String motif) {
        return """
                {
                  "inputs": "%s",
                  "parameters": {
                    "candidate_labels": ["low", "mid", "high"],
                    "multi_label": false
                  }
                }
                """.formatted(escapeJson(motif));
    }

    private String extractBestLabel(String responseBody) {
        Matcher labelsMatcher = LABELS_PATTERN.matcher(responseBody);
        Matcher scoresMatcher = SCORES_PATTERN.matcher(responseBody);
        if (!labelsMatcher.find() || !scoresMatcher.find()) {
            return null;
        }

        List<String> labels = extractStrings(labelsMatcher.group(1));
        List<Double> scores = extractNumbers(scoresMatcher.group(1));
        if (labels.isEmpty() || labels.size() != scores.size()) {
            return null;
        }

        int bestIndex = 0;
        double bestScore = scores.get(0);
        for (int i = 1; i < scores.size(); i++) {
            if (scores.get(i) > bestScore) {
                bestScore = scores.get(i);
                bestIndex = i;
            }
        }

        return labels.get(bestIndex);
    }

    private List<String> extractStrings(String source) {
        List<String> values = new ArrayList<>();
        Matcher matcher = STRING_PATTERN.matcher(source);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    private List<Double> extractNumbers(String source) {
        List<Double> values = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(source);
        while (matcher.find()) {
            try {
                values.add(Double.parseDouble(matcher.group()));
            } catch (NumberFormatException ignored) {
            }
        }
        return values;
    }

    private String normalizeUrgencyLabel(String label) {
        String normalized = sanitizeForSearch(label);
        if (normalized.contains("high") || normalized.contains("urgent") || normalized.contains("elev") || normalized.contains("haute")) {
            return "high";
        }
        if (normalized.contains("low") || normalized.contains("faible") || normalized.contains("bas")) {
            return "low";
        }
        if (normalized.contains("mid") || normalized.contains("medium") || normalized.contains("moyen")) {
            return "mid";
        }
        return DEFAULT_URGENCY;
    }

    private String estimateUrgencyLocally(String motif) {
        if (containsAny(motif,
                "urgence",
                "urgent",
                "severe",
                "sudden",
                "douleur intense",
                "chest pain",
                "breathing",
                "essouffl",
                "saign",
                "bleeding",
                "fracture",
                "trauma",
                "loss of consciousness",
                "fievre",
                "fièvre")) {
            return "high";
        }

        if (containsAny(motif,
                "follow up",
                "suivi",
                "controle",
                "check-up",
                "routine",
                "ordonnance",
                "vaccin",
                "renouvel",
                "review")) {
            return "low";
        }

        return DEFAULT_URGENCY;
    }

    private boolean containsAny(String text, String... keywords) {
        String searchable = sanitizeForSearch(text);
        for (String keyword : keywords) {
            if (searchable.contains(sanitizeForSearch(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String sanitizeForSearch(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase().trim();
    }

    private Properties loadProperties() {
        Properties loaded = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("hf.properties")) {
            if (inputStream != null) {
                loaded.load(inputStream);
            }
        } catch (IOException ignored) {
        }
        return loaded;
    }

    private String getRequiredValue(String propertyKey, String envKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(propertyKey);
        return propertyValue == null ? "" : propertyValue.trim();
    }

    private String getOptionalValue(String propertyKey, String envKey, String defaultValue) {
        String value = getRequiredValue(propertyKey, envKey);
        return value.isBlank() ? defaultValue : value;
    }

    private String escapeJson(String value) {
        String safeValue = value == null ? "" : value;
        return safeValue
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}