package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.entities.PostAiSuggestion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GeminiPostAssistantService {

    private final String apiKey;
    private final HttpClient httpClient;

    public GeminiPostAssistantService() {
        this.apiKey = System.getenv("GEMINI_API_KEY");
        this.httpClient = HttpClient.newHttpClient();
        
        if (this.apiKey == null) {
            System.err.println("[Gemini] ✗ GEMINI_API_KEY non trouvée dans les variables d'environnement");
            System.err.println("[Gemini] Vérifiez que JetBrains a bien lancé l'app avec les env vars du run config");
        } else {
            System.out.println("[Gemini] ✓ GEMINI_API_KEY chargée avec succès");
        }
    }

    public PostAiSuggestion improvePost(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Le contenu est vide.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Variable d'environnement GEMINI_API_KEY manquante.");
        }

        try {
            String prompt = buildPrompt(rawContent);

            JSONObject body = new JSONObject();

            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();

            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);

            parts.put(part);
            content.put("parts", parts);
            contents.put(content);

            body.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.2);
            generationConfig.put("maxOutputTokens", 2000);
            generationConfig.put("responseMimeType", "application/json");
            body.put("generationConfig", generationConfig);

            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.out.println("[Gemini] HTTP " + response.statusCode());
                System.out.println("[Gemini] Body: " + response.body());
                throw new RuntimeException("Erreur Gemini HTTP " + response.statusCode());
            }

            String aiText = extractText(response.body());
            return parseSuggestion(aiText);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Impossible de générer la suggestion IA : " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String rawContent) {
        return """
            Tu es un assistant IA intégré dans une application médicale appelée MedFlow.

            Objectif :
            Aider un utilisateur à transformer une idée en article informatif clair, utile et responsable pour un blog santé.

            Règles :
            - Ne donne jamais de diagnostic.
            - Ne recommande jamais un médicament précis.
            - Ne donne jamais de posologie.
            - Le texte doit être informatif, éducatif et simple.
            - Utilise des phrases courtes.
            - Ne mets pas de retour à la ligne dans les valeurs JSON.
            - La catégorie doit être exactement une de ces valeurs :
              Actualité, Conseils, Service, Santé, Urgence

            Réponds uniquement avec un JSON valide.
            Pas de markdown.
            Pas de ```json.
            Pas d’explication avant ou après.

            Format obligatoire :
            {
              "titre": "titre proposé",
              "categorie": "Conseils",
              "hashtags": "#sante #prevention #bienetre",
              "contenuReformule": "article reformulé en un seul paragraphe",
              "resume": "résumé court",
              "ton": "Informatif et rassurant",
              "conseilResponsable": "Ce contenu est informatif et ne remplace pas l’avis d’un professionnel de santé."
            }

            Contenu du patient :
            """ + rawContent;
    }

    private String extractText(String responseBody) {
        JSONObject json = new JSONObject(responseBody);

        return json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");
    }

    private PostAiSuggestion parseSuggestion(String aiText) {
        String clean = aiText == null ? "" : aiText.trim();

        clean = clean.replace("```json", "")
                .replace("```", "")
                .trim();

        int firstBrace = clean.indexOf("{");
        int lastBrace = clean.lastIndexOf("}");

        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            clean = clean.substring(firstBrace, lastBrace + 1);
        }

        try {
            JSONObject json = new JSONObject(clean);

            PostAiSuggestion suggestion = new PostAiSuggestion();
            suggestion.setTitre(json.optString("titre", ""));
            suggestion.setCategorie(json.optString("categorie", "Conseils"));
            suggestion.setHashtags(json.optString("hashtags", ""));
            suggestion.setContenuReformule(json.optString("contenuReformule", ""));
            suggestion.setResume(json.optString("resume", ""));
            suggestion.setTon(json.optString("ton", "Informatif"));
            suggestion.setConseilResponsable(json.optString(
                    "conseilResponsable",
                    "Ce contenu est informatif et ne remplace pas l’avis d’un professionnel de santé."
            ));

            return suggestion;

        } catch (Exception e) {
            System.out.println("[Gemini] Réponse IA non parsable :");
            System.out.println(clean);
            throw new RuntimeException("La réponse IA n'est pas un JSON valide. Réessayez avec un contenu plus simple.");
        }
    }
}