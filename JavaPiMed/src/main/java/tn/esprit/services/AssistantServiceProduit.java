package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de chat avec l'API Groq (LLaMA 3).
 * Maintient un historique de conversation pour un vrai contexte multi-tours.
 */
public class AssistantServiceProduit {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL        = "llama-3.3-70b-versatile";
    private static final String API_KEY      = System.getenv("GROQ_API_KEY_produit");

    private final List<JSONObject> conversationHistory = new ArrayList<>();

    public AssistantServiceProduit() {
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content",
                "Tu es PharmAssist, un assistant pharmacien virtuel bienveillant et professionnel. " +
                        "Tu travailles pour la Pharmacie Esprit Ariana en Tunisie. " +
                        "Tu réponds en français de façon claire, empathique et accessible. " +
                        "Tu peux : donner des informations générales sur les médicaments (effets secondaires, posologie, contre-indications), " +
                        "conseiller sur des symptômes courants (douleurs, rhume, allergie, fièvre, etc.) et suggérer de consulter un médecin si nécessaire, " +
                        "informer sur les produits de parapharmacie. " +
                        "Tu ne prescris jamais de médicaments. Tu recommandes toujours de consulter un médecin pour tout diagnostic. " +
                        "Réponds de façon concise (3-5 phrases max), chaleureuse et professionnelle."
        );
        conversationHistory.add(system);
    }

    /**
     * Envoie un message et retourne la réponse en maintenant le contexte de conversation.
     */
    public String chat(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return "Veuillez entrer votre question.";

        if (API_KEY == null || API_KEY.isBlank())
            return "⚠ Clé API non configurée. Définissez GROQ_API_KEY_produit.";

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        conversationHistory.add(userMsg);

        try {
            String responseText = callGroqApi();

            JSONObject assistantMsg = new JSONObject();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", responseText);
            conversationHistory.add(assistantMsg);

            // Garder max 21 messages (system + 20 échanges)
            if (conversationHistory.size() > 21) {
                List<JSONObject> trimmed = new ArrayList<>();
                trimmed.add(conversationHistory.get(0));
                trimmed.addAll(conversationHistory.subList(conversationHistory.size() - 20, conversationHistory.size()));
                conversationHistory.clear();
                conversationHistory.addAll(trimmed);
            }

            return responseText;

        } catch (Exception e) {
            conversationHistory.remove(conversationHistory.size() - 1);
            e.printStackTrace();
            return "❌ Erreur de connexion. Vérifiez votre connexion internet.";
        }
    }

    /** Remet à zéro l'historique (garde uniquement le message système). */
    public void resetConversation() {
        JSONObject systemMsg = conversationHistory.get(0);
        conversationHistory.clear();
        conversationHistory.add(systemMsg);
    }

    private String callGroqApi() throws IOException {
        URL url = new URL(GROQ_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("messages", new JSONArray(conversationHistory));
        body.put("max_tokens", 512);
        body.put("temperature", 0.7);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        if (code < 200 || code >= 300)
            throw new IOException("Erreur API Groq (" + code + "): " + sb);

        return new JSONObject(sb.toString())
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
    }
}
