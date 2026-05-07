package tn.esprit.services;

import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Service d'envoi des emails PRODUIT/COMMANDE MedFlow.
 * Utilise les clés de configuration PRODUIT (BREVO_API_KEY_PRODUIT, etc).
 *
 * Configuration (via variables d'environnement OU proprietes systeme) :
 *   BREVO_API_KEY_PRODUIT      / brevo.api.key.produit
 *   BREVO_SENDER_EMAIL_PRODUIT / brevo.sender.email.produit
 *   BREVO_SENDER_NAME_PRODUIT  / brevo.sender.name.produit (defaut: MedFlow)
 *   BREVO_API_URL              / brevo.api.url (defaut: https://api.brevo.com/v3/smtp/email)
 */
public final class EmailServiceProduit {

    private static final String BREVO_API_URL;
    private static final String BREVO_API_KEY;
    private static final String SENDER_EMAIL;
    private static final String SENDER_NAME;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    static {
        BREVO_API_URL = getSetting("BREVO_API_URL", "brevo.api.url", "https://api.brevo.com/v3/smtp/email");
        BREVO_API_KEY = getSetting("BREVO_API_KEY_PRODUIT", "brevo.api.key.produit", null);
        SENDER_EMAIL = getSetting("BREVO_SENDER_EMAIL_PRODUIT", "brevo.sender.email.produit", null);
        SENDER_NAME = getSetting("BREVO_SENDER_NAME_PRODUIT", "brevo.sender.name.produit", "MedFlow");
    }

    private EmailServiceProduit() {
    }

    private static String getSetting(String envKey, String propKey, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env;
        String prop = System.getProperty(propKey);
        if (prop != null && !prop.isBlank()) return prop;
        return defaultValue;
    }

    /**
     * Envoie un email au client quand la commande passe en livraison.
     */
    public static void sendCommandeLivraisonStartedEmail(Commande commande, Integer etaMinutes) {
        if (commande == null || commande.getUser() == null || isBlank(commande.getUser().getEmailUser())) return;

        User user = commande.getUser();
        String to = user.getEmailUser();
        String clientName = firstName(user);
        String etaText = (etaMinutes != null && etaMinutes > 0) ? (etaMinutes + " min") : "30-45 min";
        String total = String.format("%.2f DT", commande.getMontant_total_cents() / 100.0);

        int nbArticles = 0;
        StringBuilder details = new StringBuilder();
        if (commande.getCommande_produits() != null) {
            for (CommandeProduit cp : commande.getCommande_produits()) {
                if (cp != null && cp.getProduit() != null) {
                    nbArticles += Math.max(0, cp.getQuantite_commandee());
                    details.append("<li>")
                            .append(escapeHtml(safe(cp.getProduit().getNom_produit())))
                            .append(" x")
                            .append(cp.getQuantite_commandee())
                            .append("</li>");
                }
            }
        }

        if (details.isEmpty()) {
            details.append("<li>Details des produits indisponibles</li>");
        }

        String address = safe(user.getAdresseUser());
        if (isBlank(address)) {
            address = "Adresse non disponible";
        }

        String subject = "Merci ! Votre commande #" + commande.getId_commande() + " est en livraison";
        String html = """
                <html>
                  <body style="font-family:Segoe UI,Arial,sans-serif;color:#1f2937;">
                    <h2>Bonjour %s,</h2>
                    <p>Merci pour votre achat. Votre commande <strong>#%d</strong> est sortie en livraison.</p>
                    <p><strong>Temps estime :</strong> %s</p>
                    <p><strong>Adresse de livraison :</strong> %s</p>
                    <p><strong>Nombre d'articles :</strong> %d</p>
                    <p><strong>Total :</strong> %s</p>
                    <p><strong>Details :</strong></p>
                    <ul>%s</ul>
                    <p>Cordialement,<br/>L'equipe MedFlow</p>
                  </body>
                </html>
                """.formatted(
                escapeHtml(clientName),
                commande.getId_commande(),
                escapeHtml(etaText),
                escapeHtml(address),
                nbArticles,
                escapeHtml(total),
                details.toString()
        );

        sendAsync(to, subject, html);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Internal sending logic
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Envoie l'email dans un thread séparé pour ne pas bloquer l'UI.
     */
    private static void sendAsync(String to, String subject, String html) {
        Thread thread = new Thread(() -> {
            try {
                send(to, subject, html);
            } catch (Exception e) {
                System.err.println("[EmailServiceProduit] Erreur envoi email à " + to + " : " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.setName("email-sender-produit");
        thread.start();
    }

    private static void send(String to, String subject, String html) throws Exception {
        if (isBlank(BREVO_API_KEY) || isBlank(SENDER_EMAIL)) {
            System.err.println("[EmailServiceProduit] ❌ Brevo PRODUIT non configure.");
            System.err.println("  → BREVO_API_KEY_PRODUIT: " + (isBlank(BREVO_API_KEY) ? "NON DEFINI" : "✓"));
            System.err.println("  → BREVO_SENDER_EMAIL_PRODUIT: " + (isBlank(SENDER_EMAIL) ? "NON DEFINI" : "✓"));
            System.err.println("  Definissez BREVO_API_KEY_PRODUIT et BREVO_SENDER_EMAIL_PRODUIT.");
            System.out.println("[EmailServiceProduit] Email simule (PRODUIT) -> A: " + to + " | Objet: " + subject);
            return;
        }

        String payload = "{"
                + "\"sender\":{\"name\":\"" + escapeJson(SENDER_NAME) + "\",\"email\":\"" + escapeJson(SENDER_EMAIL) + "\"},"
                + "\"to\":[{\"email\":\"" + escapeJson(to) + "\"}],"
                + "\"subject\":\"" + escapeJson(subject) + "\","
                + "\"htmlContent\":\"" + escapeJson(html) + "\""
                + "}";

        System.out.println("[EmailServiceProduit] 📧 Configuration Brevo PRODUIT:");
        System.out.println("  → Destinataire: " + to);
        System.out.println("  → Expediteur: " + SENDER_EMAIL + " (" + SENDER_NAME + ")");
        System.out.println("  → Sujet: " + subject);
        System.out.println("  → URL API: " + BREVO_API_URL);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BREVO_API_URL))
                .header("accept", "application/json")
                .header("api-key", BREVO_API_KEY)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("[EmailServiceProduit] Status code Brevo: " + response.statusCode());
        System.out.println("[EmailServiceProduit] Reponse Brevo: " + truncate(response.body(), 500));
        
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("❌ Brevo refuse l'envoi (" + response.statusCode() + "): " + truncate(response.body(), 280));
        }

        System.out.println("[EmailServiceProduit] ✅ Email envoye avec succes via Brevo (PRODUIT) a : " + to);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Utilities
    // ──────────────────────────────────────────────────────────────────────────

    private static String firstName(User user) {
        String prenom = safe(user.getPrenom());
        String nom = safe(user.getNom());
        if (!prenom.isBlank()) return prenom;
        if (!nom.isBlank()) return nom;
        return "Utilisateur";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() <= maxLen ? value : value.substring(0, maxLen) + "...";
    }
}

