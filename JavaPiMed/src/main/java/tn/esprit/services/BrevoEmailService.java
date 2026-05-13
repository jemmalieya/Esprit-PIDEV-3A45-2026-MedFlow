package tn.esprit.services;

import tn.esprit.entities.RendezVous;
import tn.esprit.entities.User;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class BrevoEmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Properties properties = loadProperties();

    public boolean isConfigured() {
        return !getRequiredValue("brevo.api.key", "BREVO_API_KEY").isBlank()
                && !getRequiredValue("brevo.sender.email", "BREVO_SENDER_EMAIL").isBlank();
    }

    public String getMissingConfigurationMessage() {
        StringBuilder missing = new StringBuilder();

        if (getRequiredValue("brevo.api.key", "BREVO_API_KEY").isBlank()) {
            missing.append("- BREVO_API_KEY or brevo.api.key\n");
        }
        if (getRequiredValue("brevo.sender.email", "BREVO_SENDER_EMAIL").isBlank()) {
            missing.append("- BREVO_SENDER_EMAIL or brevo.sender.email\n");
        }

        if (missing.length() == 0) {
            return "";
        }

        return "Brevo is not configured. Missing:\n" + missing;
    }

    public void sendAppointmentConfirmedEmail(User patient, RendezVous rendezVous, User doctor)
            throws IOException, InterruptedException {
        if (patient == null) {
            throw new IllegalArgumentException("Patient introuvable.");
        }
        if (rendezVous == null) {
            throw new IllegalArgumentException("Rendez-vous introuvable.");
        }

        String patientEmail = safe(patient.getEmailUser());
        if (patientEmail.isBlank()) {
            throw new IllegalArgumentException("Le patient n'a pas d'adresse email.");
        }
        if (!isConfigured()) {
            throw new IllegalStateException(getMissingConfigurationMessage());
        }

        String senderEmail = getRequiredValue("brevo.sender.email", "BREVO_SENDER_EMAIL");
        String senderName = getOptionalValue("brevo.sender.name", "BREVO_SENDER_NAME", "MedFlow");
        String apiKey = getRequiredValue("brevo.api.key", "BREVO_API_KEY");

        String patientName = buildDisplayName(patient, "Patient");
        String doctorName = buildDoctorName(doctor);
        String rendezVousDate = rendezVous.getDatetime() == null
                ? "date non disponible"
                : rendezVous.getDatetime().toLocalDateTime().format(DATE_TIME_FORMATTER);
        String rendezVousMode = safe(rendezVous.getMode()).isBlank() ? "Non precise" : rendezVous.getMode();
        String rendezVousMotif = safe(rendezVous.getMotif()).isBlank() ? "Consultation medicale" : rendezVous.getMotif();

        String subject = "Confirmation de votre rendez-vous MedFlow";
        String htmlContent = buildAppointmentConfirmedEmailHtml(
            patientName,
            doctorName,
            rendezVousDate,
            rendezVousMode,
            rendezVousMotif
        );
        sendEmail(senderEmail, senderName, apiKey, patientEmail, patientName, subject, htmlContent);
    }

    public void sendDistancielCallLinkEmail(User patient, RendezVous rendezVous, User doctor, String jitsiUrl)
            throws IOException, InterruptedException {
        if (patient == null) {
            throw new IllegalArgumentException("Patient introuvable.");
        }
        if (rendezVous == null) {
            throw new IllegalArgumentException("Rendez-vous introuvable.");
        }
        if (safe(jitsiUrl).isBlank()) {
            throw new IllegalArgumentException("Lien Jitsi introuvable.");
        }

        String patientEmail = safe(patient.getEmailUser());
        if (patientEmail.isBlank()) {
            throw new IllegalArgumentException("Le patient n'a pas d'adresse email.");
        }
        if (!isConfigured()) {
            throw new IllegalStateException(getMissingConfigurationMessage());
        }

        String senderEmail = getRequiredValue("brevo.sender.email", "BREVO_SENDER_EMAIL");
        String senderName = getOptionalValue("brevo.sender.name", "BREVO_SENDER_NAME", "MedFlow");
        String apiKey = getRequiredValue("brevo.api.key", "BREVO_API_KEY");

        String patientName = buildDisplayName(patient, "Patient");
        String doctorName = buildDoctorName(doctor);
        String rendezVousDate = rendezVous.getDatetime() == null
                ? "date non disponible"
                : rendezVous.getDatetime().toLocalDateTime().format(DATE_TIME_FORMATTER);
        String rendezVousMotif = safe(rendezVous.getMotif()).isBlank() ? "Consultation medicale" : rendezVous.getMotif();

        String subject = "Lien de consultation video MedFlow";
                String htmlContent = buildDistancielCallLinkEmailHtml(
                                patientName,
                                doctorName,
                                rendezVousDate,
                                rendezVousMotif,
                                jitsiUrl
                );

        sendEmail(senderEmail, senderName, apiKey, patientEmail, patientName, subject, htmlContent);
    }

        private String buildAppointmentConfirmedEmailHtml(
                        String patientName,
                        String doctorName,
                        String rendezVousDate,
                        String rendezVousMode,
                        String rendezVousMotif
        ) {
                return """
                                <html>
                                    <body style="margin:0;padding:0;background:#eef6fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                                        <div style="max-width:720px;margin:0 auto;padding:32px 16px;">
                                            <div style="background:linear-gradient(135deg,#0f766e 0%%,#0ea5a4 52%%,#38bdf8 100%%);border-radius:22px;padding:1px;box-shadow:0 18px 40px rgba(15,118,110,0.18);">
                                                <div style="background:#ffffff;border-radius:21px;overflow:hidden;">
                                                    <div style="padding:30px 32px 24px;background:linear-gradient(135deg,#f0fdfa 0%%,#ecfeff 100%%);border-bottom:1px solid #d8f3f5;">
                                                        <div style="display:inline-block;padding:6px 12px;border-radius:999px;background:#d1fae5;color:#047857;font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;">
                                                            Rendez-vous confirmé
                                                        </div>
                                                        <h1 style="margin:18px 0 10px;font-size:30px;line-height:1.15;color:#083344;">Votre rendez-vous est confirmé</h1>
                                                        <p style="margin:0;font-size:15px;line-height:1.7;color:#475569;">Bonjour %s, votre rendez-vous a bien été validé par %s. Vous trouverez ci-dessous un récapitulatif clair et rapide de la réservation.</p>
                                                    </div>

                                                    <div style="padding:28px 32px 18px;">
                                                        <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;">
                                                            <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:16px;padding:16px 18px;">
                                                                <div style="font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:#64748b;margin-bottom:6px;">Date</div>
                                                                <div style="font-size:16px;font-weight:700;color:#0f172a;">%s</div>
                                                            </div>
                                                            <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:16px;padding:16px 18px;">
                                                                <div style="font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:#64748b;margin-bottom:6px;">Mode</div>
                                                                <div style="font-size:16px;font-weight:700;color:#0f172a;">%s</div>
                                                            </div>
                                                            <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:16px;padding:16px 18px;grid-column:1 / -1;">
                                                                <div style="font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:#64748b;margin-bottom:6px;">Motif</div>
                                                                <div style="font-size:16px;line-height:1.6;color:#0f172a;">%s</div>
                                                            </div>
                                                        </div>

                                                        <div style="margin-top:18px;padding:18px 20px;border-radius:16px;background:#eff6ff;border:1px solid #dbeafe;">
                                                            <div style="font-size:13px;font-weight:700;color:#1d4ed8;margin-bottom:4px;">Médecin en charge</div>
                                                            <div style="font-size:15px;line-height:1.6;color:#1e293b;">%s</div>
                                                        </div>

                                                        <div style="margin-top:18px;padding:18px 20px;border-radius:16px;background:#fefce8;border:1px solid #fde68a;">
                                                            <div style="font-size:13px;font-weight:700;color:#a16207;margin-bottom:4px;">À savoir</div>
                                                            <div style="font-size:14px;line-height:1.7;color:#713f12;">Connectez-vous à MedFlow pour consulter les détails de votre rendez-vous et suivre les prochaines étapes si nécessaire.</div>
                                                        </div>
                                                    </div>

                                                    <div style="padding:0 32px 30px;">
                                                        <div style="height:1px;background:#e2e8f0;margin:0 0 22px;"></div>
                                                        <p style="margin:0;font-size:14px;line-height:1.7;color:#475569;">Cordialement,<br><strong style="color:#0f172a;">L'équipe MedFlow</strong></p>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </body>
                                </html>
                                """.formatted(
                                escapeHtml(patientName),
                                escapeHtml(doctorName),
                                escapeHtml(rendezVousDate),
                                escapeHtml(rendezVousMode),
                                escapeHtml(rendezVousMotif),
                                escapeHtml(doctorName)
                );
        }

        private String buildDistancielCallLinkEmailHtml(
                        String patientName,
                        String doctorName,
                        String rendezVousDate,
                        String rendezVousMotif,
                        String jitsiUrl
        ) {
                String safeUrl = escapeHtml(jitsiUrl);
                return """
                                <html>
                                    <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                                        <div style="max-width:760px;margin:0 auto;padding:32px 16px;">
                                            <div style="background:linear-gradient(135deg,#111827 0%%,#1d4ed8 45%%,#0ea5e9 100%%);border-radius:24px;padding:1px;box-shadow:0 18px 40px rgba(17,24,39,0.18);">
                                                <div style="background:#ffffff;border-radius:23px;overflow:hidden;">
                                                    <div style="padding:32px 32px 26px;background:linear-gradient(135deg,#eff6ff 0%%,#e0f2fe 100%%);border-bottom:1px solid #d7e8f8;">
                                                        <div style="display:inline-block;padding:6px 12px;border-radius:999px;background:#dbeafe;color:#1d4ed8;font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;">
                                                            Consultation vidéo
                                                        </div>
                                                        <h1 style="margin:18px 0 10px;font-size:30px;line-height:1.15;color:#0f172a;">Votre salle de consultation est prête</h1>
                                                        <p style="margin:0;font-size:15px;line-height:1.7;color:#334155;">Bonjour %s, %s a lancé votre consultation à distance. Rejoignez la séance quand vous êtes prêt en utilisant le bouton ci-dessous.</p>
                                                    </div>

                                                    <div style="padding:28px 32px 18px;">
                                                        <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;">
                                                            <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:16px;padding:16px 18px;">
                                                                <div style="font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:#64748b;margin-bottom:6px;">Date</div>
                                                                <div style="font-size:16px;font-weight:700;color:#0f172a;">%s</div>
                                                            </div>
                                                            <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:16px;padding:16px 18px;">
                                                                <div style="font-size:12px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:#64748b;margin-bottom:6px;">Motif</div>
                                                                <div style="font-size:16px;font-weight:700;color:#0f172a;">%s</div>
                                                            </div>
                                                        </div>

                                                        <div style="margin-top:18px;padding:18px 20px;border-radius:18px;background:#ecfeff;border:1px solid #a5f3fc;">
                                                            <div style="font-size:13px;font-weight:700;color:#0e7490;margin-bottom:6px;">Médecin</div>
                                                            <div style="font-size:15px;line-height:1.6;color:#0f172a;">%s</div>
                                                        </div>

                                                        <div style="margin-top:20px;text-align:center;">
                                                            <a href="%s" style="display:inline-block;padding:14px 28px;border-radius:999px;background:linear-gradient(135deg,#2563eb 0%%,#0ea5e9 100%%);color:#ffffff;text-decoration:none;font-size:15px;font-weight:700;box-shadow:0 10px 24px rgba(37,99,235,0.28);">Rejoindre la consultation Jitsi</a>
                                                        </div>

                                                        <div style="margin-top:18px;padding:16px 18px;border-radius:16px;background:#f8fafc;border:1px dashed #cbd5e1;">
                                                            <div style="font-size:13px;font-weight:700;color:#475569;margin-bottom:6px;">Lien direct</div>
                                                            <div style="font-size:14px;line-height:1.6;word-break:break-all;"><a href="%s" style="color:#2563eb;text-decoration:none;">%s</a></div>
                                                        </div>
                                                    </div>

                                                    <div style="padding:0 32px 30px;">
                                                        <div style="height:1px;background:#e2e8f0;margin:0 0 22px;"></div>
                                                        <p style="margin:0;font-size:14px;line-height:1.7;color:#475569;">Cordialement,<br><strong style="color:#0f172a;">L'équipe MedFlow</strong></p>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </body>
                                </html>
                                """.formatted(
                                escapeHtml(patientName),
                                escapeHtml(doctorName),
                                escapeHtml(rendezVousDate),
                                escapeHtml(rendezVousMotif),
                                escapeHtml(doctorName),
                                safeUrl,
                                safeUrl,
                                safeUrl
                );
        }

    private void sendEmail(
            String senderEmail,
            String senderName,
            String apiKey,
            String recipientEmail,
            String recipientName,
            String subject,
            String htmlContent
    ) throws IOException, InterruptedException {
        String payload = """
                {
                  "sender": {
                    "email": "%s",
                    "name": "%s"
                  },
                  "to": [
                    {
                      "email": "%s",
                      "name": "%s"
                    }
                  ],
                  "subject": "%s",
                  "htmlContent": "%s"
                }
                """.formatted(
                escapeJson(senderEmail),
                escapeJson(senderName),
                escapeJson(recipientEmail),
                escapeJson(recipientName),
                escapeJson(subject),
                escapeJson(htmlContent)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BREVO_API_URL))
                .header("accept", "application/json")
                .header("api-key", apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("Brevo API error " + statusCode + ": " + response.body());
        }
    }

    private Properties loadProperties() {
        Properties loaded = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("brevo.properties")) {
            if (inputStream != null) {
                loaded.load(inputStream);
            }
        } catch (IOException ignored) {
        }
        return loaded;
    }

    private String getRequiredValue(String propertyKey, String envKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(propertyKey);
        return propertyValue == null ? "" : propertyValue.trim();
    }

    private String getOptionalValue(String propertyKey, String envKey, String defaultValue) {
        String value = getRequiredValue(propertyKey, envKey);
        return value.isBlank() ? defaultValue : value;
    }

    private String buildDisplayName(User user, String fallback) {
        if (user == null) {
            return fallback;
        }

        String fullName = (safe(user.getPrenom()) + " " + safe(user.getNom())).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }

        return safe(user.getEmailUser()).isBlank() ? fallback : user.getEmailUser();
    }

    private String buildDoctorName(User doctor) {
        String fullName = buildDisplayName(doctor, "votre medecin");
        if (fullName.toLowerCase().startsWith("dr")) {
            return fullName;
        }
        return "Dr. " + fullName;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeJson(String value) {
        String safeValue = value == null ? "" : value;
        return safeValue
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String escapeHtml(String value) {
        String safeValue = value == null ? "" : value;
        return safeValue
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
