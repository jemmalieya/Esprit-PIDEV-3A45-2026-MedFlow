package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GroqTranslationServiceReclamation {

    private static final String GROQ_CHAT_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public VoiceTranslationResult traduireVersFrancais(String texteOriginal) throws Exception {

        String apiKey = System.getenv("GROQ_RECLAMATION_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Clé Groq manquante : GROQ_RECLAMATION_API_KEY");
        }

        if (texteOriginal == null || texteOriginal.isBlank()) {
            return new VoiceTranslationResult("", "", "unknown");
        }

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("temperature", 0.1);

        body.put("response_format", new JSONObject()
                .put("type", "json_object"));

        JSONArray messages = new JSONArray();

        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", """
                        Tu es un service de traduction pour une application médicale appelée MedFlow.

                        Ta mission :
                        - détecter la langue du texte original
                        - traduire le texte en français clair
                        - garder le sens médical ou administratif exact
                        - répondre uniquement en JSON valide

                        Format obligatoire :
                        {
                          "langue_source": "fr/en/ar/unknown",
                          "texte_francais": "traduction en français"
                        }
                        """));

        messages.put(new JSONObject()
                .put("role", "user")
                .put("content", texteOriginal));

        body.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_CHAT_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                    "Erreur Groq Translation HTTP "
                            + response.statusCode()
                            + " : "
                            + response.body()
            );
        }

        JSONObject root = new JSONObject(response.body());

        String content = root
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        JSONObject json = new JSONObject(content);

        String langueSource = json.optString("langue_source", "unknown");
        String texteFrancais = json.optString("texte_francais", texteOriginal);

        return new VoiceTranslationResult(
                texteOriginal,
                texteFrancais,
                langueSource
        );
    }
}