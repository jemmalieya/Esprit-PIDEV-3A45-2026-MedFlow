package tn.esprit.services;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CommentModerationService {

    private static final double TOXICITY_THRESHOLD = 0.75;

    private final String apiKey;

    public CommentModerationService() {
        this.apiKey = System.getenv("PERSPECTIVE_API_KEY");
    }

    public ModerationResult analyzeComment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ModerationResult(false, 0.0, "clean");
        }

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("PERSPECTIVE_API_KEY manquante.");
            return new ModerationResult(false, 0.0, "not_checked");
        }

        try {
            String endpoint = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=" + apiKey;

            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            String safeText = escapeJson(text.trim());

            String jsonBody = """
                    {
                      "comment": {
                        "text": "%s"
                      },
                      "languages": ["fr", "en"],
                      "requestedAttributes": {
                        "TOXICITY": {},
                        "SEVERE_TOXICITY": {},
                        "INSULT": {},
                        "PROFANITY": {},
                        "THREAT": {}
                      }
                    }
                    """.formatted(safeText);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            String response = readResponse(connection);

            double toxicity = extractScore(response, "TOXICITY");
            double severeToxicity = extractScore(response, "SEVERE_TOXICITY");
            double insult = extractScore(response, "INSULT");
            double profanity = extractScore(response, "PROFANITY");
            double threat = extractScore(response, "THREAT");

            double maxScore = Math.max(
                    toxicity,
                    Math.max(
                            severeToxicity,
                            Math.max(insult, Math.max(profanity, threat))
                    )
            );

            boolean blocked = maxScore >= TOXICITY_THRESHOLD;

            String label;
            if (threat >= TOXICITY_THRESHOLD) {
                label = "threat";
            } else if (profanity >= TOXICITY_THRESHOLD) {
                label = "profanity";
            } else if (insult >= TOXICITY_THRESHOLD) {
                label = "insult";
            } else if (severeToxicity >= TOXICITY_THRESHOLD) {
                label = "severe_toxicity";
            } else if (toxicity >= TOXICITY_THRESHOLD) {
                label = "toxicity";
            } else {
                label = "clean";
            }

            return new ModerationResult(blocked, maxScore, label);

        } catch (Exception e) {
            e.printStackTrace();

            /*
             * Ici on ne bloque pas si l'API tombe.
             * Pour une application stricte, tu peux mettre true.
             */
            return new ModerationResult(false, 0.0, "api_error");
        }
    }

    private double extractScore(String json, String attributeName) {
        try {
            String search = "\"" + attributeName + "\"";
            int attrIndex = json.indexOf(search);

            if (attrIndex == -1) {
                return 0.0;
            }

            String summarySearch = "\"summaryScore\"";
            int summaryIndex = json.indexOf(summarySearch, attrIndex);

            if (summaryIndex == -1) {
                return 0.0;
            }

            String valueSearch = "\"value\":";
            int valueIndex = json.indexOf(valueSearch, summaryIndex);

            if (valueIndex == -1) {
                return 0.0;
            }

            valueIndex += valueSearch.length();

            int end = json.indexOf(",", valueIndex);
            if (end == -1) {
                end = json.indexOf("}", valueIndex);
            }

            String value = json.substring(valueIndex, end).trim();

            return Double.parseDouble(value);

        } catch (Exception e) {
            return 0.0;
        }
    }

    private String readResponse(HttpURLConnection connection) throws Exception {
        StringBuilder response = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static class ModerationResult {
        private final boolean blocked;
        private final double score;
        private final String label;

        public ModerationResult(boolean blocked, double score, String label) {
            this.blocked = blocked;
            this.score = score;
            this.label = label;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public double getScore() {
            return score;
        }

        public String getLabel() {
            return label;
        }
    }
}