package tn.esprit.services;

import tn.esprit.entities.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

/**
 * Service d'envoi des emails MedFlow.
 *
 * Configuration (via variables d'environnement OU proprietes systeme) :
 *   BREVO_API_KEY      / brevo.api.key
 *   BREVO_SENDER_EMAIL / brevo.sender.email
 *   BREVO_SENDER_NAME  / brevo.sender.name (defaut: MedFlow)
 *   BREVO_API_URL      / brevo.api.url     (defaut: https://api.brevo.com/v3/smtp/email)
 *
 * Compatibilite: MEDFLOW_MAIL_USER / mail.user est aussi accepte pour l'email expediteur.
 */
public class EmailService {

    private static final String BREVO_API_URL;
    private static final String BREVO_API_KEY;
    private static final String SENDER_EMAIL;
    private static final String SENDER_NAME;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    static {
        BREVO_API_URL = getSetting("BREVO_API_URL", "brevo.api.url", "https://api.brevo.com/v3/smtp/email");
        BREVO_API_KEY = getSetting("BREVO_API_KEY", "brevo.api.key", null);
        String configuredSender = getSetting("BREVO_SENDER_EMAIL", "brevo.sender.email", null);
        if (isBlank(configuredSender)) {
            configuredSender = getSetting("MEDFLOW_MAIL_USER", "mail.user", null);
        }
        SENDER_EMAIL = configuredSender;
        SENDER_NAME = getSetting("BREVO_SENDER_NAME", "brevo.sender.name", "MedFlow");
    }

    private static String getSetting(String envKey, String propKey, String defaultValue) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env;
        String prop = System.getProperty(propKey);
        if (prop != null && !prop.isBlank()) return prop;
        return defaultValue;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Public API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Envoie un email de confirmation d'approbation de la demande staff.
     */
    public static void sendStaffApprovalEmail(User user) {
        if (user == null || isBlank(user.getEmailUser())) return;

        String name = firstName(user);
        String subject = "✅ MedFlow – Demande staff approuvée";
        String html = buildHtml(
                "Félicitations, " + name + " !",
                "Votre demande d'accès staff a été <strong style='color:#059669;'>approuvée</strong> par l'équipe MedFlow.",
                new String[]{
                        "Votre compte est maintenant <strong>actif</strong>.",
                        "Vous pouvez désormais vous connecter avec votre email et mot de passe.",
                        "Type de poste : <strong>" + safe(user.getTypeStaff()) + "</strong>"
                },
                "#059669", "Connexion approuvée ✓"
        );
        sendAsync(user.getEmailUser(), subject, html);
    }

    /**
     * Envoie un email de confirmation d'approbation staff avec piece jointe.
     */
    public static void sendStaffApprovalEmail(User user, byte[] pdfBytes, String pdfName) {
        if (user == null || isBlank(user.getEmailUser())) return;

        String name = firstName(user);
        String subject = "✅ MedFlow – Demande staff approuvée";
        String html = buildHtml(
                "Félicitations, " + name + " !",
                "Votre demande d'accès staff a été <strong style='color:#059669;'>approuvée</strong> par l'équipe MedFlow.",
                new String[]{
                        "Votre compte est maintenant <strong>actif</strong>.",
                        "Vous pouvez désormais vous connecter avec votre email et mot de passe.",
                        "Type de poste : <strong>" + safe(user.getTypeStaff()) + "</strong>",
                        "Une fiche de décision est jointe à cet email."
                },
                "#059669", "Connexion approuvée ✓"
        );

        if (pdfBytes == null || pdfBytes.length == 0) {
            sendAsync(user.getEmailUser(), subject, html);
        } else {
            String fileName = isBlank(pdfName) ? "decision_staff.pdf" : pdfName;
            sendAsyncWithAttachment(user.getEmailUser(), subject, html, fileName, pdfBytes);
        }
    }

    /**
     * Envoie un email de decision staff avec motif.
     */
    public static void sendStaffDecisionEmail(User user, boolean approved, String reason) {
        if (user == null || isBlank(user.getEmailUser())) return;

        String name = firstName(user);
        String subject = approved
                ? "MedFlow - Demande staff approuvee"
                : "MedFlow - Demande staff refusee";
        String statusText = approved
                ? "approuvee"
                : "refusee";
        String accent = approved ? "#059669" : "#dc2626";

        String html = buildHtml(
                "Bonjour, " + name,
                "Votre demande d'acces staff a ete "
                        + "<strong style='color:" + accent + ";'>" + statusText + "</strong>.",
                new String[]{
                        "Type de poste : <strong>" + safe(user.getTypeStaff()) + "</strong>",
                        "Motif de decision : <strong>" + escapeHtml(safe(reason)) + "</strong>",
                        "Pour toute question, contactez l'administrateur."
                },
                accent,
                approved ? "Decision approuvee" : "Decision refusee"
        );
        sendAsync(user.getEmailUser(), subject, html);
    }

    /**
     * Envoie un email de bannissement avec motif et piece jointe (fiche de decision).
     */
    public static void sendAccountBanEmail(User user, String reason, byte[] pdfBytes, String pdfName) {
        if (user == null || isBlank(user.getEmailUser())) return;

        String subject = "MedFlow - Notification de suspension de compte";
        String html = buildHtml(
                "Notification de suspension",
                "Votre compte a ete suspendu par l'administration MedFlow.",
                new String[]{
                        "Motif de la decision : <strong>" + escapeHtml(safe(reason)) + "</strong>",
                        "Une fiche de decision est jointe a cet email.",
                        "Si vous souhaitez contester la decision, contactez le support."
                },
                "#dc2626",
                "Compte suspendu"
        );

        if (pdfBytes == null || pdfBytes.length == 0) {
            sendAsync(user.getEmailUser(), subject, html);
            return;
        }
        sendAsyncWithAttachment(user.getEmailUser(), subject, html, pdfName, pdfBytes);
    }

    /**
     * Envoie un email de notification de refus de la demande staff (sans piece jointe).
     */
    public static void sendStaffRejectionEmail(User user) {
        if (user == null || isBlank(user.getEmailUser())) return;
        sendStaffRejectionEmail(user, null, null);
    }

    /**
     * Envoie un email de notification de refus de la demande staff avec piece jointe PDF.
     */
    public static void sendStaffRejectionEmail(User user, byte[] pdfBytes, String pdfName) {
        if (user == null || isBlank(user.getEmailUser())) return;

        String name = firstName(user);
        String subject = "❌ MedFlow – Demande staff refusée";
        String html = buildHtml(
                "Bonjour, " + name,
                "Votre demande d'accès staff a été <strong style='color:#dc2626;'>refusée</strong> par l'équipe MedFlow.",
                new String[]{
                        "Votre demande n'a pas pu être validée à ce stade.",
                        "Vous pouvez contacter l'administrateur pour plus d'informations.",
                        "Vous avez la possibilité de soumettre une nouvelle demande après vérification de vos documents.",
                        "Une fiche de décision est jointe à cet email."
                },
                "#dc2626", "Demande refusée"
        );
        if (pdfBytes == null || pdfBytes.length == 0) {
            sendAsync(user.getEmailUser(), subject, html);
        } else {
            String fileName = isBlank(pdfName) ? "decision_refus.pdf" : pdfName;
            sendAsyncWithAttachment(user.getEmailUser(), subject, html, fileName, pdfBytes);
        }
    }

    /**
     * Envoie un email avec le code de réinitialisation du mot de passe.
     *
     * @param toEmail email destinataire
     * @param code    code de réinitialisation à 6 chiffres
     */
    public static void sendPasswordResetEmail(String toEmail, String code) {
        if (isBlank(toEmail)) return;

        String subject = "🔑 MedFlow – Code de réinitialisation du mot de passe";
        String html = buildHtml(
                "Réinitialisation du mot de passe",
                "Vous avez demandé un code de réinitialisation pour votre compte MedFlow.",
                new String[]{
                        "Votre code de réinitialisation est : <span style='font-size:28px;font-weight:800;letter-spacing:6px;color:#2563eb;padding:4px 12px;background:#eff6ff;border-radius:8px;'>" + code + "</span>",
                        "Ce code est <strong>valide pendant 15 minutes</strong>.",
                        "Si vous n'avez pas effectué cette demande, ignorez cet email."
                },
                "#2563eb", "Code : " + code
        );
        sendAsync(toEmail, subject, html);
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
                System.err.println("[EmailService] Erreur envoi email à " + to + " : " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.setName("email-sender");
        thread.start();
    }

    private static void sendAsyncWithAttachment(String to, String subject, String html, String fileName, byte[] content) {
        Thread thread = new Thread(() -> {
            try {
                sendWithAttachment(to, subject, html, fileName, content);
            } catch (Exception e) {
                System.err.println("[EmailService] Erreur envoi email avec piece jointe a " + to + " : " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.setName("email-sender-attachment");
        thread.start();
    }

    private static void send(String to, String subject, String html) throws Exception {
        if (isBlank(BREVO_API_KEY) || isBlank(SENDER_EMAIL)) {
            System.err.println("[EmailService] Brevo non configure. Definissez BREVO_API_KEY et BREVO_SENDER_EMAIL.");
            System.out.println("[EmailService] Email simule -> A: " + to + " | Objet: " + subject);
            return;
        }

        String payload = "{"
                + "\"sender\":{\"name\":\"" + escapeJson(SENDER_NAME) + "\",\"email\":\"" + escapeJson(SENDER_EMAIL) + "\"},"
                + "\"to\":[{\"email\":\"" + escapeJson(to) + "\"}],"
                + "\"subject\":\"" + escapeJson(subject) + "\","
                + "\"htmlContent\":\"" + escapeJson(html) + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BREVO_API_URL))
                .header("accept", "application/json")
                .header("api-key", BREVO_API_KEY)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Brevo refuse l'envoi (" + response.statusCode() + "): " + truncate(response.body(), 280));
        }

        System.out.println("[EmailService] Email envoye via Brevo a : " + to);
    }

    private static void sendWithAttachment(String to, String subject, String html, String fileName, byte[] content) throws Exception {
        if (isBlank(BREVO_API_KEY) || isBlank(SENDER_EMAIL)) {
            System.err.println("[EmailService] Brevo non configure. Definissez BREVO_API_KEY et BREVO_SENDER_EMAIL.");
            System.out.println("[EmailService] Email simule -> A: " + to + " | Objet: " + subject + " | PJ: " + fileName);
            return;
        }

        String b64 = Base64.getEncoder().encodeToString(content);
        String payload = "{"
                + "\"sender\":{\"name\":\"" + escapeJson(SENDER_NAME) + "\",\"email\":\"" + escapeJson(SENDER_EMAIL) + "\"},"
                + "\"to\":[{\"email\":\"" + escapeJson(to) + "\"}],"
                + "\"subject\":\"" + escapeJson(subject) + "\","
                + "\"htmlContent\":\"" + escapeJson(html) + "\","
                + "\"attachment\":[{\"content\":\"" + b64 + "\",\"name\":\"" + escapeJson(fileName) + "\"}]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BREVO_API_URL))
                .header("accept", "application/json")
                .header("api-key", BREVO_API_KEY)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Brevo refuse l'envoi (" + response.statusCode() + "): " + truncate(response.body(), 280));
        }
        System.out.println("[EmailService] Email envoye via Brevo a : " + to + " (avec PJ)");
    }

    /**
     * Envoie un email de confirmation d'approbation staff.
     */
    public void sendStaffApprovalEmail(String to, String nomStaff, String roleStaff) throws Exception {
        String subject = "✅ Votre demande d'accès MedFlow a été approuvée";
        String html = buildHtml(
                "Bienvenue dans l'équipe MedFlow! 🎉",
                "Votre demande d'accès a été examinée et <strong>approuvée</strong> par nos administrateurs.",
                new String[]{
                        "Rôle: <strong>" + escapeHtml(roleStaff) + "</strong>",
                        "Votre compte est maintenant <strong>actif</strong>",
                        "Vous pouvez vous connecter immédiatement avec vos identifiants",
                        "En cas de questions, contactez notre support"
                },
                "#10b981",
                "ACCÈS ACCORDÉ"
        );
        sendViaBrevo(to, subject, html);
    }

    /**
     * Envoie un email de notification de refus staff.
     */
    public void sendStaffRejectionEmail(String to, String nomStaff, String reason) throws Exception {
        String subject = "⚠️ Votre demande d'accès MedFlow a été refusée";
        String html = buildHtml(
                "Votre demande d'accès a été refusée",
                "Après examen de votre dossier, nous regrettons de vous informer que votre demande n'a pas pu être approuvée à ce stade.",
                new String[]{
                        "Motif du refus: <strong>" + escapeHtml(reason) + "</strong>",
                        "Vous pouvez soumettre une nouvelle demande après amélioration de votre dossier",
                        "N'hésitez pas à nous contacter pour plus d'informations",
                        "L'équipe MedFlow"
                },
                "#ef4444",
                "DEMANDE REFUSÉE"
        );
        sendViaBrevo(to, subject, html);
    }

    // Pont de compatibilite pour les appels historiques.
    private void sendViaBrevo(String to, String subject, String html) throws Exception {
        send(to, subject, html);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  HTML template
    // ──────────────────────────────────────────────────────────────────────────

    private static String buildHtml(String headline, String intro, String[] points, String accentColor, String badgeText) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='margin:0;padding:0;background:#f0f4fb;font-family:Segoe UI,Helvetica,Arial,sans-serif;'>");
        sb.append("<table width='100%' cellpadding='0' cellspacing='0'><tr><td align='center' style='padding:40px 16px;'>");
        sb.append("<table width='580' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:20px;overflow:hidden;box-shadow:0 4px 24px rgba(15,45,107,0.10);'>");

        // Header
        sb.append("<tr><td style='background:linear-gradient(135deg,#0f2d6b 0%,#1a4da8 55%,#1298b7 100%);padding:32px 40px;text-align:center;'>");
        sb.append("<div style='font-size:28px;font-weight:900;color:#ffffff;letter-spacing:2px;'>✦ MEDFLOW</div>");
        sb.append("<div style='font-size:13px;color:rgba(255,255,255,0.75);margin-top:4px;'>Plateforme de Santé Numérique</div>");
        sb.append("</td></tr>");

        // Badge
        sb.append("<tr><td style='padding:28px 40px 8px;text-align:center;'>");
        sb.append("<span style='display:inline-block;background:" + accentColor + ";color:#fff;font-size:13px;font-weight:700;padding:6px 20px;border-radius:999px;'>" + escapeHtml(badgeText) + "</span>");
        sb.append("</td></tr>");

        // Headline
        sb.append("<tr><td style='padding:16px 40px 4px;'>");
        sb.append("<h1 style='margin:0;font-size:24px;color:#0f2d6b;font-weight:800;'>" + escapeHtml(headline) + "</h1>");
        sb.append("</td></tr>");

        // Intro
        sb.append("<tr><td style='padding:8px 40px 16px;'>");
        sb.append("<p style='margin:0;font-size:15px;color:#374151;line-height:1.6;'>" + intro + "</p>");
        sb.append("</td></tr>");

        // Points
        if (points != null && points.length > 0) {
            sb.append("<tr><td style='padding:0 40px 24px;'><ul style='padding-left:20px;margin:8px 0;'>");
            for (String point : points) {
                sb.append("<li style='font-size:14px;color:#4b5563;margin-bottom:8px;line-height:1.5;'>" + point + "</li>");
            }
            sb.append("</ul></td></tr>");
        }

        // Footer
        sb.append("<tr><td style='background:#f9fbff;padding:20px 40px;border-top:1px solid #e5e7eb;text-align:center;'>");
        sb.append("<p style='margin:0;font-size:12px;color:#9ca3af;'>Cet email a été envoyé automatiquement par MedFlow. Ne pas répondre à ce message.</p>");
        sb.append("</td></tr>");

        sb.append("</table></td></tr></table></body></html>");
        return sb.toString();
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