package tn.esprit.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class MailService {

    private final String username;
    private final String password;
    private final String from;

    public MailService() {
        this.username = System.getenv("MEDFLOW_MAIL_USERNAME");
        this.password = System.getenv("MEDFLOW_MAIL_PASSWORD");
        this.from = System.getenv("MEDFLOW_MAIL_FROM");
    }

    public boolean sendPostModerationEmail(String to, String patientName, String postTitle, boolean approved) {
        if (to == null || to.isBlank()) {
            System.out.println("[MailService] Email patient vide.");
            return false;
        }

        if (username == null || password == null || from == null) {
            System.out.println("[MailService] Variables d'environnement mail manquantes.");
            return false;
        }

        String subject = approved
                ? "Votre post MedFlow a été approuvé"
                : "Votre post MedFlow a été refusé";

        String statusText = approved ? "approuvé" : "refusé";

        String body =
                "Bonjour " + (patientName == null || patientName.isBlank() ? "" : patientName) + ",\n\n" +
                        "Votre post \"" + postTitle + "\" a été " + statusText + " par l’administrateur.\n\n" +
                        (approved
                                ? "Il est maintenant visible dans le blog MedFlow.\n"
                                : "Vous pouvez le modifier puis le renvoyer pour validation.\n") +
                        "\nCordialement,\n" +
                        "L’équipe MedFlow";

        try {
            Properties props = new Properties();

            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);

            System.out.println("[MailService] Email envoyé à " + to);
            return true;

        } catch (Exception e) {
            System.out.println("[MailService] Erreur email : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}