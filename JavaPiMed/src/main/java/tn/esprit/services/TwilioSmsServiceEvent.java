package tn.esprit.services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class TwilioSmsServiceEvent {
    private static final String ACCOUNT_SID_ENV = "TWILIO_SMS_EVENT_ACCOUNT_SID";
    private static final String AUTH_TOKEN_ENV = "TWILIO_SMS_EVENT_AUTH_TOKEN";
    private static final String FROM_ENV = "TWILIO_SMS_EVENT_FROM";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public SmsResult sendEvenementSms(String recipient, String content) {
        String accountSid = System.getenv(ACCOUNT_SID_ENV);
        String authToken = System.getenv(AUTH_TOKEN_ENV);
        String from = System.getenv(FROM_ENV);

        if (isBlank(accountSid) || isBlank(authToken) || isBlank(from)) {
            return SmsResult.notConfigured();
        }

        String endpoint = "https://api.twilio.com/2010-04-01/Accounts/"
                + urlEncode(accountSid)
                + "/Messages.json";

        String formBody = "From=" + urlEncode(from)
                + "&To=" + urlEncode(recipient)
                + "&Body=" + urlEncode(content);

        String credentials = accountSid + ":" + authToken;
        String authorization = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", authorization)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            return new SmsResult(success, response.statusCode(), response.body(), false);
        } catch (IOException e) {
            return new SmsResult(false, 0, e.getMessage(), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SmsResult(false, 0, "Envoi SMS interrompu.", false);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record SmsResult(boolean success, int statusCode, String responseBody, boolean missingConfiguration) {
        public static SmsResult notConfigured() {
            return new SmsResult(false, 0,
                    "TWILIO_SMS_EVENT_ACCOUNT_SID, TWILIO_SMS_EVENT_AUTH_TOKEN ou TWILIO_SMS_EVENT_FROM n'est pas configure.",
                    true);
        }
    }
}
