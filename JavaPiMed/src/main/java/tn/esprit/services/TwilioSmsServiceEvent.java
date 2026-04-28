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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwilioSmsServiceEvent {
    private static final String ACCOUNT_SID_ENV = "TWILIO_SMS_EVENT_ACCOUNT_SID";
    private static final String AUTH_TOKEN_ENV = "TWILIO_SMS_EVENT_AUTH_TOKEN";
    private static final String FROM_ENV = "TWILIO_SMS_EVENT_FROM";
    private static final Pattern SID_PATTERN = Pattern.compile("\"sid\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern STATUS_PATTERN = Pattern.compile("\"status\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\"error_code\"\\s*:\\s*(null|\\d+)");
    private static final Pattern ERROR_MESSAGE_PATTERN = Pattern.compile("\"error_message\"\\s*:\\s*(null|\"((?:\\\\.|[^\"])*)\")");

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
            return new SmsResult(
                    success,
                    response.statusCode(),
                    response.body(),
                    false,
                    extract(response.body(), SID_PATTERN),
                    extract(response.body(), STATUS_PATTERN),
                    extract(response.body(), ERROR_CODE_PATTERN),
                    extractErrorMessage(response.body())
            );
        } catch (IOException e) {
            return new SmsResult(false, 0, e.getMessage(), false, "", "", "", "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SmsResult(false, 0, "Envoi SMS interrompu.", false, "", "", "", "");
        }
    }

    public String diagnosticSummary() {
        String from = System.getenv(FROM_ENV);
        return "From=" + (isBlank(from) ? "non configure" : from);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String extract(String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body == null ? "" : body);
        if (!matcher.find()) return "";
        String value = matcher.group(1);
        return "null".equals(value) ? "" : value;
    }

    private String extractErrorMessage(String body) {
        Matcher matcher = ERROR_MESSAGE_PATTERN.matcher(body == null ? "" : body);
        if (!matcher.find()) return "";
        String value = matcher.group(2);
        return value == null ? "" : value.replace("\\\"", "\"");
    }

    public record SmsResult(
            boolean success,
            int statusCode,
            String responseBody,
            boolean missingConfiguration,
            String messageSid,
            String twilioStatus,
            String errorCode,
            String errorMessage
    ) {
        public static SmsResult notConfigured() {
            return new SmsResult(false, 0,
                    "TWILIO_SMS_EVENT_ACCOUNT_SID, TWILIO_SMS_EVENT_AUTH_TOKEN ou TWILIO_SMS_EVENT_FROM n'est pas configure.",
                    true, "", "", "", "");
        }

        public String deliveryDiagnostic() {
            StringBuilder sb = new StringBuilder();
            if (messageSid != null && !messageSid.isBlank()) {
                sb.append(" SID: ").append(messageSid).append(".");
            }
            if (twilioStatus != null && !twilioStatus.isBlank()) {
                sb.append(" Statut Twilio: ").append(twilioStatus).append(".");
            }
            if (errorCode != null && !errorCode.isBlank()) {
                sb.append(" Code erreur: ").append(errorCode).append(".");
            }
            if (errorMessage != null && !errorMessage.isBlank()) {
                sb.append(" Message erreur: ").append(errorMessage).append(".");
            }
            return sb.toString().trim();
        }
    }
}
