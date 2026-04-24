package tn.esprit.services;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import tn.esprit.entities.User;

import java.util.Properties;

/**
 * Service d'envoi des emails MedFlow.
 *
 * Configuration (via variables d'environnement OU propriétés système) :
 *   MEDFLOW_MAIL_USER  / mail.user     → ex: votre.compte@gmail.com
 *   MEDFLOW_MAIL_PASS  / mail.password → mot de passe d'application Gmail
 *   MEDFLOW_MAIL_HOST  / mail.smtp.host  (défaut : smtp.gmail.com)
 *   MEDFLOW_MAIL_PORT  / mail.smtp.port  (défaut : 587)
 */
public class EmailService {

    private static final String SMTP_HOST;
    private static final String SMTP_PORT;
    private static final String SMTP_USER;
    private static final String SMTP_PASS;

    static {
        SMTP_HOST = getSetting("MEDFLOW_MAIL_HOST", "mail.smtp.host", "smtp.gmail.com");
        SMTP_PORT = getSetting("MEDFLOW_MAIL_PORT", "mail.smtp.port", "587");
        SMTP_USER = getSetting("MEDFLOW_MAIL_USER", "mail.user", null);
        SMTP_PASS = getSetting("MEDFLOW_MAIL_PASS", "mail.password", null);
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
     * Envoie un email de notification de refus de la demande staff.
     */
    public static void sendStaffRejectionEmail(User user) {
        if (user == null || isBlank(user.getEmailUser())) return;

        String name = firstName(user);
        String subject = "❌ MedFlow – Demande staff refusée";
        String html = buildHtml(
                "Bonjour, " + name,
                "Votre demande d'accès staff a été <strong style='color:#dc2626;'>refusée</strong> par l'équipe MedFlow.",
                new String[]{
                        "Votre demande n'a pas pu être validée à ce stade.",
                        "Vous pouvez contacter l'administrateur pour plus d'informations.",
                        "Vous avez la possibilité de soumettre une nouvelle demande après vérification de vos documents."
                },
                "#dc2626", "Demande refusée"
        );
        sendAsync(user.getEmailUser(), subject, html);
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

    private static void send(String to, String subject, String html) throws MessagingException {
        if (isBlank(SMTP_USER) || isBlank(SMTP_PASS)) {
            System.err.println("[EmailService] SMTP non configuré. Définissez MEDFLOW_MAIL_USER et MEDFLOW_MAIL_PASS.");
            System.out.println("[EmailService] Email simulé → À: " + to + " | Objet: " + subject);
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(SMTP_USER, "MedFlow", "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(SMTP_USER));
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, "UTF-8");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=UTF-8");

        MimeMultipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(htmlPart);
        message.setContent(multipart);

        Transport.send(message);
        System.out.println("[EmailService] Email envoyé à : " + to);
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
}



