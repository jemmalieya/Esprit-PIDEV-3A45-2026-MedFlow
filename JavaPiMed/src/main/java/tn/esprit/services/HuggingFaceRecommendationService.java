package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.entities.Produit;
import tn.esprit.tools.MyDataBase;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class HuggingFaceRecommendationService {

    private static final String DEFAULT_MODEL_ID = "HuggingFaceTB/SmolLM2-1.7B-Instruct";
    private static final List<String> DEFAULT_MODEL_CANDIDATES = List.of(
            "meta-llama/Llama-3.1-8B-Instruct",
            "mistralai/Mistral-7B-Instruct-v0.3",
            "Qwen/Qwen2.5-7B-Instruct",
            "HuggingFaceTB/SmolLM2-1.7B-Instruct"
    );
    private static final int MAX_503_RETRY = 1;

    // Variables d'environnement:
    // - HF_API_KEY (ou HUGGINGFACE_API_KEY)
    // - HF_MODEL_ID (optionnel)
    // - HF_API_URL (optionnel, endpoint complet)
    // - HF_ENABLED (optionnel, défaut auto: actif si la clé est présente)
    private static final String HF_API_KEY = firstNonBlank(System.getenv("HF_API_KEY"), System.getenv("HUGGINGFACE_API_KEY"));
    private static final String HF_MODEL_ID = normalizeModelId(firstNonBlank(System.getenv("HF_MODEL_ID"), DEFAULT_MODEL_ID));
    private static final List<String> HF_MODEL_CANDIDATES = resolveModelCandidates();
    private static final String HF_API_URL = System.getenv("HF_API_URL");
    private static final String HF_ROUTER_PROVIDER_TEMPLATE = "https://router.huggingface.co/hf-inference/models/%s";
    private static final String HF_ROUTER_CHAT_COMPLETIONS = "https://router.huggingface.co/v1/chat/completions";
    private static final String HF_API_INFERENCE_CHAT_COMPLETIONS = "https://api-inference.huggingface.co/v1/chat/completions";
    private static final String HF_ROUTER_MODELS_URL = "https://router.huggingface.co/v1/models";
    private static final int MAX_DISCOVERED_MODELS = 8;
    private static final boolean HF_TRY_PROVIDER_ENDPOINT = Boolean.parseBoolean(firstNonBlank(System.getenv("HF_TRY_PROVIDER_ENDPOINT"), "false"));
    private static final boolean HF_ENABLED = computeAiEnabled();

    private static volatile boolean HF_DISABLED = false;
    private static volatile String HF_DISABLED_REASON = null;
    private static volatile boolean HF_OFFLINE_LOGGED = false;
    private static volatile List<String> HF_DISCOVERED_MODELS = null;
    private static volatile boolean HF_ROUTER_PERMISSION_BLOCKED = false;
    private final Connection cn;

    public HuggingFaceRecommendationService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POINT D'ENTRÉE PRINCIPAL
    // Retourne une liste de recommandations textuelles pour l'UI
    // ─────────────────────────────────────────────────────────────────────────

    public List<String> genererRecommandations(int userId) {
        try {
            // 1. Récupérer l'historique des commandes du user
            List<String> produitsAchetes = getProduitsAchetesParUser(userId);

            // 2. Récupérer les best sellers globaux
            List<String> bestSellers = getBestSellers(5);

            // 3. Récupérer les produits disponibles en stock
            List<String> produitsDisponibles = getProduitsDisponibles(20);

            // 4. Si le user n'a pas d'historique → retourner best sellers uniquement
            if (produitsAchetes.isEmpty()) {
                System.out.println("[HuggingFace] Pas d'historique → retour best sellers");
                return genererRecommandationsBestSellers(bestSellers, produitsDisponibles);
            }

            // 5. Appel IA avec contexte complet
            return appellerHuggingFace(produitsAchetes, bestSellers, produitsDisponibles);

        } catch (Exception e) {
            System.err.println("[HuggingFace] Erreur genererRecommandations : " + e.getMessage());
            return getFallbackRecommandations();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPEL API HUGGING FACE
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> appellerHuggingFace(
            List<String> produitsAchetes,
            List<String> bestSellers,
            List<String> produitsDisponibles) {

        if (!HF_ENABLED) {
            if (!HF_OFFLINE_LOGGED) {
                HF_OFFLINE_LOGGED = true;
                System.out.println("[HuggingFace] IA désactivée (HF_ENABLED=false). Retour fallback local.");
            }
            return genererRecommandationsBestSellers(bestSellers, produitsDisponibles);
        }

        if (HF_DISABLED) {
            if (HF_DISABLED_REASON != null) {
                System.out.println("[HuggingFace] IA désactivée pour cette session: " + HF_DISABLED_REASON);
            }
            return genererRecommandationsBestSellers(bestSellers, produitsDisponibles);
        }

        if (HF_API_KEY == null || HF_API_KEY.isBlank()) {
            System.err.println("[HuggingFace] Token manquant. Definir HF_API_KEY ou HUGGINGFACE_API_KEY.");
            return genererRecommandationsBestSellers(bestSellers, produitsDisponibles);
        }

        try {
            String prompt = construirePrompt(produitsAchetes, bestSellers, produitsDisponibles);

            List<HfAttempt> attempts = buildAttempts();
            for (HfAttempt attempt : attempts) {
                System.out.println("[HuggingFace] Envoi prompt... model=" + attempt.modelId + " endpoint=" + attempt.endpoint);

                String payload = buildPayload(prompt, attempt);
                HfResponse response = postJson(attempt.endpoint, payload);

                int retry = 0;
                while (response.code == 503 && retry < MAX_503_RETRY) {
                    retry++;
                    System.out.println("[HuggingFace] Modele en chargement, attente 10s...");
                    Thread.sleep(10000);
                    response = postJson(attempt.endpoint, payload);
                }

                if (response.code == 200) {
                    String reponse = response.body == null ? "" : response.body;
                    System.out.println("[HuggingFace] Réponse reçue : " + reponse.substring(0, Math.min(200, reponse.length())));
                    List<String> parsed = parserReponseIA(reponse, produitsDisponibles, produitsAchetes);
                    if (parsed != null && parsed.size() >= 2) {
                        return parsed;
                    }
                    System.out.println("[HuggingFace] Réponse inutilisable, tentative suivante...");
                    continue;
                }

                handleHfFailure(attempt.endpoint, response);
                if (HF_DISABLED) {
                    break;
                }
            }

            return genererRecommandationsBestSellers(bestSellers, produitsDisponibles);
        } catch (Exception e) {
            System.err.println("[HuggingFace] Erreur appel API : " + e.getMessage());
            return genererRecommandationsBestSellers(bestSellers, produitsDisponibles);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRUCTION DU PROMPT
    // ─────────────────────────────────────────────────────────────────────────

    private String construirePrompt(
            List<String> produitsAchetes,
            List<String> bestSellers,
            List<String> produitsDisponibles) {

        StringBuilder sb = new StringBuilder();
        sb.append("<s>[INST] Tu es un assistant pharmacien expert en recommandations médicales.\n\n");
        sb.append("Voici les produits achetés récemment par ce client :\n");

        for (String p : produitsAchetes) {
            sb.append("- ").append(p).append("\n");
        }

        sb.append("\nVoici les produits best sellers de notre pharmacie :\n");
        for (String p : bestSellers) {
            sb.append("- ").append(p).append("\n");
        }

        sb.append("\nVoici les produits disponibles en stock :\n");
        for (String p : produitsDisponibles) {
            sb.append("- ").append(p).append("\n");
        }

        sb.append("\nGénère exactement 4 recommandations personnalisées pour ce client.");
        sb.append(" Chaque recommandation doit être sur une ligne séparée.");
        sb.append(" Format : '💊 [Nom produit] — [raison courte en français]'.");
        sb.append(" Recommande uniquement des produits de la liste disponible.");
        sb.append(" Ne répète pas les produits déjà achetés.");
        sb.append(" [/INST]");

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSER LA RÉPONSE IA
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> parserReponseIA(String jsonReponse, List<String> produitsDisponibles, List<String> produitsAchetes) {
        List<String> recommandations = new ArrayList<>();

        try {
            String texte = extractGeneratedText(jsonReponse).trim();
            if (texte.isEmpty()) {
                return genererRecommandationsBestSellers(getBestSellers(5), produitsDisponibles);
            }

            System.out.println("[HuggingFace] Texte généré : " + texte);

            Set<String> achetesNorm = new HashSet<>();
            for (String p : produitsAchetes) {
                achetesNorm.add(normalizeForCompare(p));
            }

            Map<String, String> stockByNorm = new LinkedHashMap<>();
            for (String p : produitsDisponibles) {
                stockByNorm.put(normalizeForCompare(p), p);
            }

            Set<String> dejaAjoutesNorm = new HashSet<>();

            // Parser ligne par ligne
            String[] lignes = texte.split("\n");
            for (String ligne : lignes) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;
                if (isPromptConstraintLine(ligne)) continue;

                String cleaned = ligne
                        .replaceAll("^[-•*]\\s*", "")
                        .replaceAll("^[0-9]+\\.\\s*", "")
                        .trim();
                if (cleaned.isEmpty()) continue;

                String produitDetecte = detectProduitFromLine(cleaned, stockByNorm);
                if (produitDetecte == null) {
                    continue;
                }

                String produitNorm = normalizeForCompare(produitDetecte);
                if (achetesNorm.contains(produitNorm)) {
                    continue;
                }
                if (dejaAjoutesNorm.contains(produitNorm)) {
                    continue;
                }

                String raison = extractRaison(cleaned);
                recommandations.add("💊 " + produitDetecte + " — " + raison);
                dejaAjoutesNorm.add(produitNorm);
                if (recommandations.size() >= 4) break;
            }

            // Completer proprement si l'IA n'a pas respecte le format.
            if (recommandations.size() < 4) {
                for (String produit : produitsDisponibles) {
                    String produitNorm = normalizeForCompare(produit);
                    if (achetesNorm.contains(produitNorm) || dejaAjoutesNorm.contains(produitNorm)) {
                        continue;
                    }
                    recommandations.add("💊 " + produit + " — Recommandé selon votre profil");
                    dejaAjoutesNorm.add(produitNorm);
                    if (recommandations.size() >= 4) break;
                }
            }

            if (recommandations.isEmpty()) {
                System.out.println("[HuggingFace] Parse vide → fallback best sellers");
                return genererRecommandationsBestSellers(getBestSellers(5), produitsDisponibles);
            }

            return recommandations;

        } catch (Exception e) {
            System.err.println("[HuggingFace] Erreur parsing : " + e.getMessage());
            return getFallbackRecommandations();
        }
    }

    private static boolean isPromptConstraintLine(String line) {
        String low = line.toLowerCase(Locale.ROOT);
        return low.contains("constraints")
                || low.contains("exactly 4")
                || low.contains("each on a separate line")
                || low.contains("format:")
                || low.contains("recommend only")
                || low.contains("do not repeat")
                || low.startsWith("1.")
                || low.startsWith("2.")
                || low.startsWith("3.")
                || low.startsWith("4.")
                || low.startsWith("5.");
    }

    private static String detectProduitFromLine(String line, Map<String, String> stockByNorm) {
        String lineNorm = normalizeForCompare(line);
        for (Map.Entry<String, String> entry : stockByNorm.entrySet()) {
            if (lineNorm.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String extractRaison(String line) {
        String cleaned = line.replaceFirst("^[💊🌿💉🩺]\\s*", "").trim();
        String[] separators = {" — ", " - ", " : "};
        for (String sep : separators) {
            int idx = cleaned.indexOf(sep);
            if (idx >= 0 && idx + sep.length() < cleaned.length()) {
                String reason = cleaned.substring(idx + sep.length()).trim();
                if (!reason.isEmpty()) {
                    return reason;
                }
            }
        }
        return "Suggestion personnalisée";
    }

    private static String normalizeForCompare(String value) {
        if (value == null) {
            return "";
        }
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BEST SELLERS — fallback si pas d'historique ou erreur IA
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> genererRecommandationsBestSellers(
            List<String> bestSellers,
            List<String> produitsDisponibles) {

        List<String> recommandations = new ArrayList<>();

        // Ajouter les best sellers
        for (int i = 0; i < Math.min(2, bestSellers.size()); i++) {
            recommandations.add("🔥 " + bestSellers.get(i) + " — Best seller de la semaine");
        }

        // Compléter avec des produits disponibles
        for (String p : produitsDisponibles) {
            if (recommandations.size() >= 4) break;
            boolean dejaPresent = recommandations.stream()
                    .anyMatch(r -> r.contains(p.split(" ")[0]));
            if (!dejaPresent) {
                recommandations.add("✨ " + p + " — Recommandé pour vous");
            }
        }

        return recommandations.isEmpty() ? getFallbackRecommandations() : recommandations;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REQUÊTES SQL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Récupère les noms des produits achetés par le user (10 dernières commandes)
     */
    public List<String> getProduitsAchetesParUser(int userId) {
        List<String> liste = new ArrayList<>();
        String sql = """
                SELECT DISTINCT p.nom_produit
                FROM commande_produit cp
                JOIN produit p   ON cp.produit_id  = p.id_produit
                JOIN commande c  ON cp.commande_id = c.id_commande
                WHERE c.user_id = ?
                ORDER BY c.date_creation_commande DESC
                LIMIT 10
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(rs.getString("nom_produit"));
        } catch (SQLException e) {
            System.err.println("[HuggingFace] Erreur SQL getProduitsAchetes : " + e.getMessage());
        }
        System.out.println("[HuggingFace] Produits achetés par user " + userId + " : " + liste);
        return liste;
    }

    /**
     * Récupère les best sellers (produits les plus commandés globalement)
     */
    public List<String> getBestSellers(int limit) {
        List<String> liste = new ArrayList<>();
        String sql = """
                SELECT p.nom_produit, SUM(cp.quantite_commandee) AS total_vendu
                FROM commande_produit cp
                JOIN produit p ON cp.produit_id = p.id_produit
                GROUP BY p.id_produit, p.nom_produit
                ORDER BY total_vendu DESC
                LIMIT ?
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(rs.getString("nom_produit"));
        } catch (SQLException e) {
            System.err.println("[HuggingFace] Erreur SQL getBestSellers : " + e.getMessage());
        }
        System.out.println("[HuggingFace] Best sellers : " + liste);
        return liste;
    }

    /**
     * Récupère les produits disponibles en stock (pour le prompt)
     */
    public List<Produit> getProduitsBestSellersComplets(int limit) {
        List<Produit> liste = new ArrayList<>();
        String sql = """
                SELECT p.*, SUM(cp.quantite_commandee) AS total_vendu
                FROM commande_produit cp
                JOIN produit p ON cp.produit_id = p.id_produit
                WHERE p.status_produit != 'Rupture'
                GROUP BY p.id_produit
                ORDER BY total_vendu DESC
                LIMIT ?
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Produit p = new Produit(
                        rs.getInt("id_produit"),
                        rs.getString("nom_produit"),
                        rs.getString("description_produit"),
                        rs.getDouble("prix_produit"),
                        rs.getInt("quantite_produit"),
                        rs.getString("image_produit"),
                        rs.getString("categorie_produit"),
                        rs.getString("status_produit")
                );
                liste.add(p);
            }
        } catch (SQLException e) {
            System.err.println("[HuggingFace] Erreur SQL getProduitsBestSellers : " + e.getMessage());
        }
        return liste;
    }

    private List<String> getProduitsDisponibles(int limit) {
        List<String> liste = new ArrayList<>();
        String sql = """
                SELECT nom_produit FROM produit
                WHERE status_produit != 'Rupture'
                AND quantite_produit > 0
                ORDER BY RAND()
                LIMIT ?
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) liste.add(rs.getString("nom_produit"));
        } catch (SQLException e) {
            System.err.println("[HuggingFace] Erreur SQL getProduitsDisponibles : " + e.getMessage());
        }
        return liste;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FALLBACK si tout échoue
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> getFallbackRecommandations() {
        return List.of(
                "💊 Consultez nos médicaments disponibles",
                "🔥 Découvrez nos best sellers de la semaine",
                "✨ Nouveaux produits en stock",
                "🩺 Produits recommandés par nos pharmaciens"
        );
    }

    private static List<HfAttempt> buildAttempts() {
        List<HfAttempt> attempts = new ArrayList<>();

        if (HF_API_URL != null && !HF_API_URL.isBlank()) {
            String custom = HF_API_URL.trim();
            String modelForCustom = HF_MODEL_CANDIDATES.isEmpty() ? HF_MODEL_ID : HF_MODEL_CANDIDATES.get(0);
            if (custom.contains("{model}")) {
                custom = custom.replace("{model}", modelForCustom);
            }
            boolean isChatEndpoint = custom.contains("/v1/chat/completions");
            attempts.add(new HfAttempt(custom, modelForCustom, isChatEndpoint));
            return attempts;
        }

        List<String> modelsToTry = resolveModelsToTry();
        for (String model : modelsToTry) {
            // Priorite a chat/completions, plus stable que hf-inference pour ce compte.
            if (!HF_ROUTER_PERMISSION_BLOCKED) {
                attempts.add(new HfAttempt(HF_ROUTER_CHAT_COMPLETIONS, model, true));
            }
            attempts.add(new HfAttempt(HF_API_INFERENCE_CHAT_COMPLETIONS, model, true));
            if (HF_TRY_PROVIDER_ENDPOINT) {
                attempts.add(new HfAttempt(String.format(HF_ROUTER_PROVIDER_TEMPLATE, model), model, false));
            }
        }

        return attempts;
    }

    private static List<String> resolveModelsToTry() {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();

        List<String> discovered = discoverRouterModels();
        for (String model : discovered) {
            ordered.add(model);
            if (ordered.size() >= MAX_DISCOVERED_MODELS) {
                break;
            }
        }

        for (String model : HF_MODEL_CANDIDATES) {
            if (ordered.size() >= MAX_DISCOVERED_MODELS) {
                break;
            }
            ordered.add(model);
        }

        return new ArrayList<>(ordered);
    }

    private static List<String> discoverRouterModels() {
        if (HF_DISCOVERED_MODELS != null) {
            return HF_DISCOVERED_MODELS;
        }

        if (HF_API_KEY == null || HF_API_KEY.isBlank()) {
            HF_DISCOVERED_MODELS = Collections.emptyList();
            return HF_DISCOVERED_MODELS;
        }

        try {
            HfResponse response = getJson(HF_ROUTER_MODELS_URL);
            if (response.code != 200) {
                System.out.println("[HuggingFace] Impossible de lister les modeles router (HTTP " + response.code + ")");
                HF_DISCOVERED_MODELS = Collections.emptyList();
                return HF_DISCOVERED_MODELS;
            }

            List<String> models = parseModelIds(response.body);
            if (!models.isEmpty()) {
                System.out.println("[HuggingFace] Modeles disponibles detectes: " + models.subList(0, Math.min(5, models.size())));
            } else {
                System.out.println("[HuggingFace] Aucun modele detecte via /v1/models, fallback sur la liste locale.");
            }
            HF_DISCOVERED_MODELS = models;
            return HF_DISCOVERED_MODELS;
        } catch (Exception e) {
            System.out.println("[HuggingFace] Echec discovery modeles: " + e.getMessage());
            HF_DISCOVERED_MODELS = Collections.emptyList();
            return HF_DISCOVERED_MODELS;
        }
    }

    private static List<String> parseModelIds(String body) {
        if (body == null || body.isBlank()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> models = new LinkedHashSet<>();
        String trimmed = body.trim();

        if (trimmed.startsWith("{")) {
            JSONObject root = new JSONObject(trimmed);
            JSONArray data = root.optJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject item = data.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    addModelIfValid(models, item.optString("id", null));
                    addModelIfValid(models, item.optString("model", null));
                }
            }
        } else if (trimmed.startsWith("[")) {
            JSONArray array = new JSONArray(trimmed);
            for (int i = 0; i < array.length(); i++) {
                Object item = array.get(i);
                if (item instanceof JSONObject obj) {
                    addModelIfValid(models, obj.optString("id", null));
                    addModelIfValid(models, obj.optString("model", null));
                } else if (item instanceof String text) {
                    addModelIfValid(models, text);
                }
            }
        }

        return new ArrayList<>(models);
    }

    private static void addModelIfValid(Set<String> models, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String normalized = normalizeModelId(raw);
        if (normalized == null || normalized.isBlank()) {
            return;
        }
        if (isNonTextGenerationPlaceholder(normalized)) {
            return;
        }
        if (!normalized.contains("/")) {
            return;
        }
        models.add(normalized);
    }

    private static String buildPayload(String prompt, HfAttempt attempt) {
        if (attempt.chatCompletions) {
            JSONObject body = new JSONObject();
            body.put("model", attempt.modelId);
            body.put("max_tokens", 300);
            body.put("temperature", 0.7);

            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);
            body.put("messages", messages);

            return body.toString();
        }

        JSONObject body = new JSONObject();
        body.put("inputs", prompt);

        JSONObject parameters = new JSONObject();
        parameters.put("max_new_tokens", 300);
        parameters.put("temperature", 0.7);
        parameters.put("return_full_text", false);
        body.put("parameters", parameters);

        return body.toString();
    }

    private static void handleHfFailure(String endpoint, HfResponse response) {
        String body = response.body == null ? "" : response.body;
        String lowerBody = body.toLowerCase(Locale.ROOT);

        System.err.println("[HuggingFace] Erreur HTTP " + response.code + " endpoint=" + endpoint);
        if (response.requestId != null && !response.requestId.isBlank()) {
            System.err.println("[HuggingFace] request-id=" + response.requestId);
        }
        if (!body.isBlank()) {
            System.err.println("[HuggingFace] body=" + body);
        }

        if (response.code == 401 || response.code == 403) {
            if (lowerBody.contains("insufficient permissions")
                    || lowerBody.contains("invalid token")
                    || lowerBody.contains("authentication")
                    || lowerBody.contains("unauthorized")) {
                if (endpoint.contains("router.huggingface.co")) {
                    HF_ROUTER_PERMISSION_BLOCKED = true;
                    System.out.println("[HuggingFace] Permissions Router insuffisantes, bascule vers api-inference.");
                } else {
                    HF_DISABLED = true;
                    HF_DISABLED_REASON = "token invalide ou permissions insuffisantes";
                    System.out.println("[HuggingFace] IA désactivée pour cette session: " + HF_DISABLED_REASON);
                }
            }
        }

        if (response.code == 400 || response.code == 404) {
            if (lowerBody.contains("not supported")
                    || lowerBody.contains("cannot post")
                    || lowerBody.contains("provider hf-inference")
                    || lowerBody.contains("model_not_supported")) {
                System.out.println("[HuggingFace] Endpoint/modele incompatible, tentative suivante...");
            }
        }
    }

    private static HfResponse postJson(String endpoint, String payload) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + HF_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String requestId = conn.getHeaderField("x-request-id");
        String body = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        System.out.println("[HuggingFace] HTTP " + code + " endpoint=" + endpoint);
        return new HfResponse(code, body, requestId);
    }

    private static HfResponse getJson(String endpoint) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoOutput(false);
        conn.setRequestProperty("Authorization", "Bearer " + HF_API_KEY);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        int code = conn.getResponseCode();
        String requestId = conn.getHeaderField("x-request-id");
        String body = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        return new HfResponse(code, body, requestId);
    }

    private static String readAll(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static String extractGeneratedText(String jsonReponse) {
        if (jsonReponse == null || jsonReponse.isBlank()) {
            return "";
        }

        String trimmed = jsonReponse.trim();
        if (trimmed.startsWith("[")) {
            JSONArray array = new JSONArray(trimmed);
            if (array.isEmpty()) {
                return "";
            }
            return array.getJSONObject(0).optString("generated_text", "");
        }

        JSONObject object = new JSONObject(trimmed);
        if (object.has("generated_text")) {
            return object.optString("generated_text", "");
        }

        JSONArray choices = object.optJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject first = choices.getJSONObject(0);
            JSONObject message = first.optJSONObject("message");
            if (message != null) {
                return message.optString("content", "");
            }
            return first.optString("text", "");
        }

        if (object.has("error")) {
            throw new IllegalStateException("API HuggingFace: " + object.optString("error"));
        }

        return "";
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    private static boolean computeAiEnabled() {
        String explicitFlag = System.getenv("HF_ENABLED");
        if (explicitFlag != null && !explicitFlag.isBlank()) {
            return Boolean.parseBoolean(explicitFlag);
        }
        return HF_API_KEY != null && !HF_API_KEY.isBlank();
    }

    private static String normalizeModelId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DEFAULT_MODEL_ID;
        }

        String value = rawValue.trim();
        final String modelsPrefix = "https://api-inference.huggingface.co/models/";
        final String pipelinePrefix = "https://api-inference.huggingface.co/pipeline/text-generation/";
        final String routerPrefix = "https://router.huggingface.co/hf-inference/models/";

        if (value.startsWith(modelsPrefix)) {
            value = value.substring(modelsPrefix.length());
        } else if (value.startsWith(pipelinePrefix)) {
            value = value.substring(pipelinePrefix.length());
        } else if (value.startsWith(routerPrefix)) {
            value = value.substring(routerPrefix.length());
        }

        while (value.startsWith("/")) {
            value = value.substring(1);
        }

        if (isNonTextGenerationPlaceholder(value)) {
            System.out.println("[HuggingFace] Modèle non adapté a la generation texte detecte: " + value + " -> " + DEFAULT_MODEL_ID);
            return DEFAULT_MODEL_ID;
        }

        return value.isBlank() ? DEFAULT_MODEL_ID : value;
    }

    private static List<String> resolveModelCandidates() {
        String rawList = System.getenv("HF_MODEL_CANDIDATES");
        List<String> candidates = new ArrayList<>();

        if (rawList != null && !rawList.isBlank()) {
            for (String token : rawList.split(",")) {
                String normalized = normalizeModelId(token);
                if (normalized != null && !normalized.isBlank() && !candidates.contains(normalized)) {
                    candidates.add(normalized);
                }
            }
        }

        String normalizedPrimary = normalizeModelId(HF_MODEL_ID);
        if (normalizedPrimary != null && !normalizedPrimary.isBlank() && !candidates.contains(normalizedPrimary)) {
            candidates.add(0, normalizedPrimary);
        }

        for (String fallback : DEFAULT_MODEL_CANDIDATES) {
            String normalized = normalizeModelId(fallback);
            if (normalized != null && !normalized.isBlank() && !candidates.contains(normalized)) {
                candidates.add(normalized);
            }
        }

        return candidates;
    }

    private static boolean isNonTextGenerationPlaceholder(String modelId) {
        if (modelId == null) {
            return true;
        }
        String lower = modelId.toLowerCase(Locale.ROOT);
        return lower.equals("gpt2")
                || lower.contains("all-minilm")
                || lower.contains("sentence-transformers")
                || lower.contains("embedding")
                || lower.contains("bert")
                || lower.contains("clip");
    }

    private static class HfResponse {
        private final int code;
        private final String body;
        private final String requestId;

        private HfResponse(int code, String body, String requestId) {
            this.code = code;
            this.body = body;
            this.requestId = requestId;
        }
    }

    private static class HfAttempt {
        private final String endpoint;
        private final String modelId;
        private final boolean chatCompletions;

        private HfAttempt(String endpoint, String modelId, boolean chatCompletions) {
            this.endpoint = endpoint;
            this.modelId = modelId;
            this.chatCompletions = chatCompletions;
        }
    }
}
