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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrescriptionSuggestionService {

    private static final String DEFAULT_MODEL_ID = "meta-llama/Llama-2-7b-chat-hf";
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"generated_text\"\\s*:\\s*\"(.*?)\"(?=,|\\})", Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Properties properties = loadProperties();

    public static class PrescriptionSuggestion {
        public String nom_medicament;
        public String dose;
        public String frequence;
        public String instructions;

        public PrescriptionSuggestion(String nom_medicament, String dose, String frequence, String instructions) {
            this.nom_medicament = nom_medicament;
            this.dose = dose;
            this.frequence = frequence;
            this.instructions = instructions;
        }

        @Override
        public String toString() {
            return nom_medicament + " - " + dose + " - " + frequence;
        }
    }

    public List<PrescriptionSuggestion> suggestPrescriptions(String diagnostic) {
        String sanitized = sanitizeText(diagnostic);
        if (sanitized.isBlank()) {
            return new ArrayList<>();
        }

        String apiToken = getRequiredValue("hf.api.token", "HF_API_TOKEN");
        if (apiToken.isBlank()) {
            return suggestPrescriptionsLocally(sanitized);
        }

        String modelId = getOptionalValue("hf.model.id", "HF_MODEL_ID", DEFAULT_MODEL_ID);
        String prompt = buildPrompt(diagnostic);
        String payload = buildPayload(prompt);

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
                return suggestPrescriptionsLocally(sanitized);
            }

            List<PrescriptionSuggestion> suggestions = parseResponseToPrescriptions(response.body());
            if (suggestions.isEmpty()) {
                return suggestPrescriptionsLocally(sanitized);
            }
            return suggestions;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return suggestPrescriptionsLocally(sanitized);
        } catch (IOException ex) {
            return suggestPrescriptionsLocally(sanitized);
        } catch (Exception ex) {
            return suggestPrescriptionsLocally(sanitized);
        }
    }

    private String buildPrompt(String diagnostic) {
        return String.format(
                "Based on the following diagnostic: \"%s\", suggest 3 common medications with dosage and frequency in this format: " +
                "1. [Medicine Name] - [Dose] - [Frequency]. " +
                "Only provide the list, no additional text.",
                diagnostic
        );
    }

    private String buildPayload(String prompt) {
        return """
                {
                  "inputs": "%s",
                  "parameters": {
                    "max_new_tokens": 100,
                    "temperature": 0.7
                  }
                }
                """.formatted(escapeJson(prompt));
    }

    private List<PrescriptionSuggestion> parseResponseToPrescriptions(String responseBody) {
        List<PrescriptionSuggestion> suggestions = new ArrayList<>();
        try {
            Matcher matcher = CONTENT_PATTERN.matcher(responseBody);
            if (matcher.find()) {
                String generatedText = matcher.group(1);
                String[] lines = generatedText.split("\\n");

                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;

                    line = line.replaceAll("^\\d+\\.\\s*", "").trim();

                    String[] parts = line.split("-");
                    if (parts.length >= 2) {
                        String name = parts[0].trim();
                        String dose = parts.length > 1 ? parts[1].trim() : "As directed";
                        String frequency = parts.length > 2 ? parts[2].trim() : "Once daily";
                        String instructions = parts.length > 3 ? parts[3].trim() : "Take with food";

                        if (!name.isEmpty()) {
                            suggestions.add(new PrescriptionSuggestion(name, dose, frequency, instructions));
                            if (suggestions.size() >= 3) break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            return new ArrayList<>();
        }
        return suggestions;
    }

    private List<PrescriptionSuggestion> suggestPrescriptionsLocally(String diagnostic) {
        List<PrescriptionSuggestion> suggestions = new ArrayList<>();
        String normalized = diagnostic.toLowerCase();

        if (containsAny(normalized, "fever", "fievre", "temperature")) {
            suggestions.add(new PrescriptionSuggestion("Paracetamol", "500mg", "Every 6 hours", "Take with food"));
            suggestions.add(new PrescriptionSuggestion("Ibuprofen", "400mg", "Every 8 hours", "Take with water"));
            suggestions.add(new PrescriptionSuggestion("Aspirin", "500mg", "Every 8 hours", "Take after meals"));
        }

        if (containsAny(normalized, "cough", "toux", "cold", "rhume")) {
            suggestions.add(new PrescriptionSuggestion("Codeine", "15mg", "Every 6-8 hours", "Do not exceed 4 doses per day"));
            suggestions.add(new PrescriptionSuggestion("Dextromethorphan", "30mg", "Every 4-6 hours", "Take as needed"));
            suggestions.add(new PrescriptionSuggestion("Guaifenesin", "200mg", "Every 4 hours", "Drink plenty of water"));
        }

        if (containsAny(normalized, "pain", "douleur", "ache", "mal")) {
            suggestions.add(new PrescriptionSuggestion("Acetaminophen", "500mg", "Every 4-6 hours", "Do not exceed 3000mg per day"));
            suggestions.add(new PrescriptionSuggestion("Ibuprofen", "400mg", "Every 6-8 hours", "Take with food"));
            suggestions.add(new PrescriptionSuggestion("Naproxen", "250mg", "Twice daily", "Take with glass of water"));
        }

        if (containsAny(normalized, "bacteria", "infection", "bacterial")) {
            suggestions.add(new PrescriptionSuggestion("Amoxicillin", "500mg", "Three times daily", "Complete the full course"));
            suggestions.add(new PrescriptionSuggestion("Azithromycin", "250mg", "Once daily", "Take on empty stomach"));
            suggestions.add(new PrescriptionSuggestion("Ciprofloxacin", "500mg", "Twice daily", "Take with water"));
        }

        if (containsAny(normalized, "allergy", "allergie", "allergic", "itching", "rash")) {
            suggestions.add(new PrescriptionSuggestion("Loratadine", "10mg", "Once daily", "Take in the morning"));
            suggestions.add(new PrescriptionSuggestion("Cetirizine", "10mg", "Once daily", "Any time of day"));
            suggestions.add(new PrescriptionSuggestion("Diphenhydramine", "25mg", "Every 4-6 hours", "May cause drowsiness"));
        }

        if (containsAny(normalized, "digestion", "digestif", "stomach", "estomac", "gastric")) {
            suggestions.add(new PrescriptionSuggestion("Omeprazole", "20mg", "Once daily", "Take 30 minutes before meal"));
            suggestions.add(new PrescriptionSuggestion("Ranitidine", "150mg", "Twice daily", "Take before meals"));
            suggestions.add(new PrescriptionSuggestion("Metoclopramide", "10mg", "Three times daily", "Take 30 minutes before meals"));
        }

        if (containsAny(normalized, "headache", "migraine", "cephale")) {
            suggestions.add(new PrescriptionSuggestion("Ibuprofen", "400mg", "Every 4-6 hours", "Take with food"));
            suggestions.add(new PrescriptionSuggestion("Sumatriptan", "50mg", "As needed", "Take at first sign of migraine"));
            suggestions.add(new PrescriptionSuggestion("Propranolol", "40mg", "Twice daily", "Take with meals"));
        }

        if (suggestions.isEmpty()) {
            suggestions.add(new PrescriptionSuggestion("Generic Medicine", "As prescribed", "As directed", "Follow doctor instructions"));
        }

        return suggestions.subList(0, Math.min(3, suggestions.size()));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(normalizeForSearch(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeForSearch(String input) {
        String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").toLowerCase();
    }

    private String sanitizeText(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "").trim().toLowerCase();
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
