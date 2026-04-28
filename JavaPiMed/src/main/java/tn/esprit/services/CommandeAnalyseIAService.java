package tn.esprit.services;

import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.entities.User;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandeAnalyseIAService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    public static class AnalyseResult {
        public boolean success;

        public String decision;
        public String score;
        public String priorite;
        public String ordonnance;
        public String doublon;
        public String interaction;
        public String verification;

        public String resume;
        public String risques;
        public String action;
        public String confiance;
        public String error;

        public static AnalyseResult ok(
                String decision,
                String score,
                String priorite,
                String ordonnance,
                String doublon,
                String interaction,
                String verification,
                String resume,
                String risques,
                String action,
                String confiance
        ) {
            AnalyseResult r = new AnalyseResult();
            r.success = true;

            r.decision = decision;
            r.score = score;
            r.priorite = priorite;
            r.ordonnance = ordonnance;
            r.doublon = doublon;
            r.interaction = interaction;
            r.verification = verification;

            r.resume = resume;
            r.risques = risques;
            r.action = action;
            r.confiance = confiance;

            return r;
        }

        public static AnalyseResult fail(String error) {
            AnalyseResult r = new AnalyseResult();
            r.success = false;
            r.error = error;

            r.decision = "Analyse impossible";
            r.score = "—";
            r.priorite = "À vérifier";
            r.ordonnance = "Vérification manuelle nécessaire";
            r.doublon = "Non vérifié";
            r.interaction = "Non vérifiée";
            r.verification = "Vérifier la commande manuellement.";

            r.resume = "Analyse indisponible.";
            r.risques = "Impossible de récupérer l’analyse IA.";
            r.action = "Vérifier manuellement la commande.";
            r.confiance = "Faible";

            return r;
        }
    }

    public AnalyseResult analyserCommande(Commande commande) {
        if (commande == null) {
            return AnalyseResult.fail("Commande introuvable.");
        }

        String apiKey = getGroqApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            return AnalyseResult.fail(
                    "Clé Groq manquante. Ajoute GROQ_API_KEY_produit dans les variables d'environnement IntelliJ."
            );
        }

        try {
            String prompt = construirePromptCommande(commande);

            String systemPrompt = """
                    Tu es un assistant IA spécialisé pour un pharmacien dans une application JavaFX de pharmacie.

                    Ton rôle :
                    - analyser une commande de médicaments
                    - détecter les médicaments sensibles
                    - détecter les doublons de produits ou les quantités suspectes
                    - signaler si une ordonnance doit être vérifiée
                    - signaler les interactions possibles entre médicaments
                    - proposer une décision claire au pharmacien

                    Règles importantes :
                    - Ne donne jamais un diagnostic médical.
                    - Ne donne pas de conseils médicaux au patient.
                    - Tu aides seulement le pharmacien à vérifier la commande.
                    - Si tu n'es pas sûr, écris clairement : "à vérifier par le pharmacien".
                    - Utilise un français simple, professionnel et court.
                    - Ne fais pas une réponse générale inutile.
                    - Donne une vraie analyse métier.

                    Réponds exactement avec ce format, sans ajouter d'autre texte :

                    DECISION: ...
                    SCORE: ...
                    PRIORITE: ...
                    ORDONNANCE: ...
                    DOUBLON: ...
                    INTERACTION: ...
                    VERIFICATION: ...
                    RESUME: ...
                    RISQUES: ...
                    ACTION: ...
                    CONFIANCE: ...
                    """;

            String body = """
                    {
                      "model": "%s",
                      "messages": [
                        {
                          "role": "system",
                          "content": "%s"
                        },
                        {
                          "role": "user",
                          "content": "%s"
                        }
                      ],
                      "temperature": 0.1,
                      "max_tokens": 900
                    }
                    """.formatted(
                    MODEL,
                    jsonEscape(systemPrompt),
                    jsonEscape(prompt)
            );

            HttpURLConnection conn = (HttpURLConnection) new URL(GROQ_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();

            InputStream stream = code >= 200 && code < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String response = readAll(stream);

            if (code < 200 || code >= 300) {
                return AnalyseResult.fail("Erreur Groq HTTP " + code + " : " + response);
            }

            String content = extractContentFromGroqResponse(response);

            if (content == null || content.isBlank()) {
                return AnalyseResult.fail("Réponse Groq vide ou format invalide.");
            }

            String decision = extractSection(content, "DECISION", "SCORE");
            String score = extractSection(content, "SCORE", "PRIORITE");
            String priorite = extractSection(content, "PRIORITE", "ORDONNANCE");
            String ordonnance = extractSection(content, "ORDONNANCE", "DOUBLON");
            String doublon = extractSection(content, "DOUBLON", "INTERACTION");
            String interaction = extractSection(content, "INTERACTION", "VERIFICATION");
            String verification = extractSection(content, "VERIFICATION", "RESUME");
            String resume = extractSection(content, "RESUME", "RISQUES");
            String risques = extractSection(content, "RISQUES", "ACTION");
            String action = extractSection(content, "ACTION", "CONFIANCE");
            String confiance = extractSection(content, "CONFIANCE", null);

            if (decision.isBlank()) decision = "Commande à vérifier avant validation";
            if (score.isBlank()) score = "7/10";
            if (priorite.isBlank()) priorite = "Normale";
            if (ordonnance.isBlank()) ordonnance = "Vérifier si un produit nécessite une ordonnance.";
            if (doublon.isBlank()) doublon = "Aucun doublon évident détecté.";
            if (interaction.isBlank()) interaction = "Interaction à vérifier par le pharmacien.";
            if (verification.isBlank()) verification = "Contrôler les produits sensibles, les quantités et l’ordonnance.";
            if (resume.isBlank()) resume = content;
            if (risques.isBlank()) risques = "Aucun risque évident détecté, mais une vérification pharmacien reste nécessaire.";
            if (action.isBlank()) action = "Valider seulement après vérification pharmacien.";
            if (confiance.isBlank()) confiance = "Moyenne";

            return AnalyseResult.ok(
                    decision,
                    score,
                    priorite,
                    ordonnance,
                    doublon,
                    interaction,
                    verification,
                    resume,
                    risques,
                    action,
                    confiance
            );

        } catch (Exception e) {
            e.printStackTrace();
            return AnalyseResult.fail(e.getMessage());
        }
    }

    private String construirePromptCommande(Commande commande) {
        StringBuilder sb = new StringBuilder();

        sb.append("Analyse cette commande de pharmacie pour aider le pharmacien.\n\n");

        sb.append("=== COMMANDE ===\n");
        sb.append("ID : ").append(commande.getId_commande()).append("\n");
        sb.append("Statut : ").append(safe(commande.getStatut_commande())).append("\n");
        sb.append("Date : ").append(commande.getDate_creation_commande()).append("\n");
        sb.append("Montant total : ")
                .append(String.format("%.2f Dt", commande.getMontant_total_cents() / 100.0))
                .append("\n\n");

        User user = commande.getUser();

        sb.append("=== CLIENT ===\n");

        if (user != null) {
            String nom = safe(user.getPrenom()) + " " + safe(user.getNom());
            sb.append("Nom : ").append(nom.trim().isBlank() ? "Client inconnu" : nom.trim()).append("\n");
            sb.append("Email : ").append(safe(user.getEmailUser())).append("\n");
            sb.append("Adresse : ").append(safe(user.getAdresseUser())).append("\n");
        } else {
            sb.append("Client : introuvable\n");
        }

        sb.append("\n=== PRODUITS COMMANDÉS ===\n");

        List<CommandeProduit> produits = commande.getCommande_produits();

        if (produits == null || produits.isEmpty()) {
            sb.append("Aucun produit trouvé.\n");
        } else {
            for (CommandeProduit cp : produits) {
                Produit p = cp.getProduit();

                if (p == null) {
                    sb.append("- Produit introuvable\n");
                    continue;
                }

                sb.append("- Nom : ").append(safe(p.getNom_produit())).append("\n");
                sb.append("  Catégorie : ").append(safe(p.getCategorie_produit())).append("\n");
                sb.append("  Quantité : ").append(cp.getQuantite_commandee()).append("\n");
                sb.append("  Prix unitaire : ").append(String.format("%.2f Dt", p.getPrix_produit())).append("\n");
                sb.append("  Sous-total : ")
                        .append(String.format("%.2f Dt", p.getPrix_produit() * cp.getQuantite_commandee()))
                        .append("\n\n");
            }
        }

        sb.append("""
                
                Fais une analyse utile pour le pharmacien.
                Cherche surtout :
                1. Médicaments sensibles ou à ordonnance.
                2. Doublons de produits.
                3. Quantités suspectes.
                4. Interactions possibles.
                5. Décision claire : valider, vérifier, bloquer temporairement.
                6. Score sécurité sur 10.
                """);

        return sb.toString();
    }

    private String getGroqApiKey() {
        String key = System.getenv("GROQ_API_KEY_produit");

        if (key == null || key.isBlank()) {
            key = System.getProperty("GROQ_API_KEY_produit");
        }

        if (key == null || key.isBlank()) {
            key = System.getenv("GROQ_API_KEY");
        }

        if (key == null || key.isBlank()) {
            key = System.getProperty("GROQ_API_KEY");
        }

        if (key == null) return null;

        key = key.trim();

        if ((key.startsWith("\"") && key.endsWith("\"")) ||
                (key.startsWith("'") && key.endsWith("'"))) {
            key = key.substring(1, key.length() - 1).trim();
        }

        return key;
    }

    private String extractContentFromGroqResponse(String json) {
        Pattern pattern = Pattern.compile(
                "\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
                Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return jsonUnescape(matcher.group(1));
        }

        return null;
    }

    private String extractSection(String text, String start, String end) {
        if (text == null) return "";

        String regex;

        if (end == null) {
            regex = "(?is)" + Pattern.quote(start) + "\\s*:\\s*(.*)$";
        } else {
            regex = "(?is)" + Pattern.quote(start) + "\\s*:\\s*(.*?)\\s*" + Pattern.quote(end) + "\\s*:";
        }

        Matcher matcher = Pattern.compile(regex).matcher(text);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "";
    }

    private String readAll(InputStream inputStream) throws Exception {
        if (inputStream == null) return "";

        StringBuilder sb = new StringBuilder();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )) {
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        return sb.toString();
    }

    private String jsonEscape(String s) {
        if (s == null) return "";

        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String jsonUnescape(String s) {
        if (s == null) return "";

        return s
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}