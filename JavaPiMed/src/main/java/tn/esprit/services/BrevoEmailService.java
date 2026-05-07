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
        String htmlContent = """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #1f2937;">
                    <h2>Rendez-vous confirme</h2>
                    <p>Bonjour %s,</p>
                    <p>Votre rendez-vous a ete confirme par %s.</p>
                    <p><strong>Date :</strong> %s</p>
                    <p><strong>Mode :</strong> %s</p>
                    <p><strong>Motif :</strong> %s</p>
                    <p>Merci de vous connecter a MedFlow pour consulter les details si besoin.</p>
                    <p>Cordialement,<br/>L'equipe MedFlow</p>
                  </body>
                </html>
                """.formatted(
                escapeHtml(patientName),
                escapeHtml(doctorName),
                escapeHtml(rendezVousDate),
                escapeHtml(rendezVousMode),
                escapeHtml(rendezVousMotif)
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
        String htmlContent = """
                <html>
                  <body style="font-family: Arial, sans-serif; color: #1f2937;">
                    <h2>Votre consultation video est prete</h2>
                    <p>Bonjour %s,</p>
                    <p>%s a demarre votre consultation a distance.</p>
                    <p><strong>Date :</strong> %s</p>
                    <p><strong>Motif :</strong> %s</p>
                    <p>Pour rejoindre l'appel Jitsi, cliquez sur ce lien :</p>
                    <p><a href="%s">%s</a></p>
                    <p>Cordialement,<br/>L'equipe MedFlow</p>
                  </body>
                </html>
                """.formatted(
                escapeHtml(patientName),
                escapeHtml(doctorName),
                escapeHtml(rendezVousDate),
                escapeHtml(rendezVousMotif),
                escapeHtml(jitsiUrl),
                escapeHtml(jitsiUrl)
        );

        sendEmail(senderEmail, senderName, apiKey, patientEmail, patientName, subject, htmlContent);
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
