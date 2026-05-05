package tn.esprit.services;

import tn.esprit.entities.Evenement;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroqEventAiClient {

    private static final String DEFAULT_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
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
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public boolean isConfigured() {
        return !getApiKey().isBlank();
    }

    public String publicStatus() {
        return isConfigured()
                ? "Groq connecte. Le score reste calcule localement et Groq enrichit le resume professionnel."
                : "Mode local. Ajoutez GROQ_EVENT_API_KEY dans .env.local pour activer le resume Groq.";
    }

    public String generateEventExecutiveSummary(
            Evenement event,
            AiEventIntelligenceService.RiskReport risk,
            List<AiEventIntelligenceService.Recommendation> recommendations
    ) throws IOException, InterruptedException {
        String apiKey = getApiKey();
        if (apiKey.isBlank()) {
            throw new IOException("GROQ_EVENT_API_KEY manquant.");
        }

        String systemPrompt = """
                Tu es un assistant IA professionnel pour le back-office MedFlow Evenements.
                Tu dois produire une analyse courte, concrete, en francais, sans inventer de donnees.
                Base-toi uniquement sur les donnees fournies.
                Structure attendue:
                1. Diagnostic
                2. Priorites admin
                3. Recommandations a afficher
                4. Phrase finale professionnelle
                Maximum 170 mots.
                """;

        String userPrompt = """
                Evenement:
                - titre: %s
                - type: %s
                - ville: %s
                - statut: %s
                - visibilite: %s
                - date debut: %s
                - date fin: %s
                - capacite: %d

                Score de risque local: %d/100
                Niveau: %s
                Raisons:
                %s

                Suggestions:
                %s

                Recommandations classees:
                %s
                """.formatted(
                safe(event == null ? "" : event.getTitre_event()),
                safe(event == null ? "" : event.getType_event()),
                safe(event == null ? "" : event.getVille_event()),
                safe(event == null ? "" : event.getStatut_event()),
                safe(event == null ? "" : event.getVisibilite_event()),
                event == null || event.getDate_debut_event() == null ? "-" : event.getDate_debut_event().toString(),
                event == null || event.getDate_fin_event() == null ? "-" : event.getDate_fin_event().toString(),
                event == null ? 0 : event.getNb_participants_max_event(),
                risk.score(),
                safe(risk.level()),
                bulletList(risk.reasons()),
                bulletList(risk.suggestions()),
                recommendationsForPrompt(recommendations)
        );

        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                  ],
                  "temperature": 0.2,
                  "max_tokens": 420
                }
                """.formatted(
                jsonEscape(getModel()),
                jsonEscape(systemPrompt),
                jsonEscape(userPrompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getEndpoint()))
                .timeout(Duration.ofSeconds(25))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "MedFlow-Groq-Event-AI/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur Groq " + response.statusCode() + ": " + limit(response.body(), 500));
        }

        String content = extractAssistantContent(response.body());
        if (content.isBlank()) {
            throw new IOException("Reponse Groq vide ou non lisible.");
        }
        return content;
    }

    public String generateEventAssistantReply(
            Evenement event,
            AiEventIntelligenceService.RiskReport risk,
            List<AiEventIntelligenceService.Recommendation> recommendations,
            String question
    ) throws IOException, InterruptedException {
        String apiKey = getApiKey();
        if (apiKey.isBlank()) {
            throw new IOException("GROQ_EVENT_API_KEY manquant.");
        }

        String systemPrompt = """
                Tu es un assistant evenement MedFlow.
                Tu reponds dans la langue de la question de l'utilisateur: francais, anglais ou arabe.
                Le ton doit etre clair, naturel, professionnel et utile.
                Tu reponds uniquement a partir des donnees fournies sur l'evenement.
                Si une information manque, dis-le clairement sans inventer.
                Tu peux expliquer: date, lieu, inscription, capacite, statut, objectif, risque, recommandations, participation,
                interet du sujet, benefices generaux pour participer, logique de l'evenement, utilite des ressources et accessibilite.
                Si l'utilisateur demande pourquoi participer, explique la valeur generale du theme en restant relie a l'evenement.
                Maximum 170 mots.
                """;

        String userPrompt = """
                Evenement:
                - titre: %s
                - type: %s
                - ville: %s
                - adresse: %s
                - statut: %s
                - visibilite: %s
                - date debut: %s
                - date fin: %s
                - capacite max: %d
                - inscription obligatoire: %s
                - date limite inscription: %s
                - organisateur: %s
                - description: %s
                - objectif: %s

                Risque local:
                - score: %d/100
                - niveau: %s

                Recommandations:
                %s

                Question utilisateur:
                %s
                """.formatted(
                safe(event == null ? "" : event.getTitre_event()),
                safe(event == null ? "" : event.getType_event()),
                safe(event == null ? "" : event.getVille_event()),
                safe(event == null ? "" : event.getAdresse_event()),
                safe(event == null ? "" : event.getStatut_event()),
                safe(event == null ? "" : event.getVisibilite_event()),
                event == null || event.getDate_debut_event() == null ? "-" : event.getDate_debut_event().toString(),
                event == null || event.getDate_fin_event() == null ? "-" : event.getDate_fin_event().toString(),
                event == null ? 0 : event.getNb_participants_max_event(),
                event != null && event.isInscription_obligatoire_event() ? "oui" : "non",
                event == null || event.getDate_limite_inscription_event() == null ? "-" : event.getDate_limite_inscription_event().toString(),
                safe(event == null ? "" : event.getNom_organisateur_event()),
                safe(event == null ? "" : event.getDescription_event()),
                safe(event == null ? "" : event.getObjectif_event()),
                risk == null ? 0 : risk.score(),
                risk == null ? "-" : safe(risk.level()),
                recommendationsForPrompt(recommendations),
                safe(question)
        );

        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                  ],
                  "temperature": 0.2,
                  "max_tokens": 320
                }
                """.formatted(
                jsonEscape(getModel()),
                jsonEscape(systemPrompt),
                jsonEscape(userPrompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getEndpoint()))
                .timeout(Duration.ofSeconds(25))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "MedFlow-Groq-Event-AI/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur Groq " + response.statusCode() + ": " + limit(response.body(), 500));
        }

        String content = extractAssistantContent(response.body());
        if (content.isBlank()) {
            throw new IOException("Reponse Groq vide ou non lisible.");
        }
        return content;
    }

    private String getApiKey() {
        ConfigValue value = readConfig("GROQ_EVENT_API_KEY");
        if (value.value().isBlank()) {
            value = readConfig("GROQ_API_KEY");
        }
        return value.value();
    }

    private String getEndpoint() {
        String endpoint = readConfig("GROQ_EVENT_ENDPOINT").value();
        return endpoint == null || endpoint.isBlank() ? DEFAULT_ENDPOINT : endpoint.trim();
    }

    private String getModel() {
        String model = readConfig("GROQ_EVENT_MODEL").value();
        return model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    private ConfigValue readConfig(String key) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return new ConfigValue(value.trim());
        }

        value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return new ConfigValue(value.trim());
        }

        value = readDotEnvValue(key, ".env.local");
        if (value == null || value.isBlank()) {
            value = readDotEnvValue(key, ".env");
        }
        return new ConfigValue(value == null ? "" : value.trim());
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

    private String recommendationsForPrompt(List<AiEventIntelligenceService.Recommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "- aucune";
        }

        StringBuilder sb = new StringBuilder();
        for (AiEventIntelligenceService.Recommendation rec : recommendations) {
            Evenement event = rec.event();
            sb.append("- ")
                    .append(safe(event.getTitre_event()))
                    .append(" | score ")
                    .append(String.format(Locale.US, "%.1f/10", rec.score()))
                    .append(" | ")
                    .append(String.join(" ", rec.reasons()))
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private String bulletList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "- aucune";
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            sb.append("- ").append(safe(value)).append("\n");
        }
        return sb.toString().trim();
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
        return value.length() <= max ? value : value.substring(0, max) + "...";
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record ConfigValue(String value) {
    }
}
