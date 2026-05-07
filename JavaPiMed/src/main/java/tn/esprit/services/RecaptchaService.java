package tn.esprit.services;

import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class RecaptchaService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    // Google's official public TEST keys — safe to ship, bypass domain validation
    private static final String TEST_SITE_KEY   = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI";
    private static final String TEST_SECRET_KEY = "6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe";

    // Port for the embedded HTTP server serving the reCAPTCHA widget HTML
    private static final int LOCAL_PORT = 8787;
    private static HttpServer localServer;

    /**
     * Starts a tiny embedded HTTP server on 127.0.0.1:8787 that serves
     * the recaptcha-widget.html file. This allows Google reCAPTCHA to see
     * a valid http:// origin instead of file://.
     * Call once at app startup (LoginController.initialize).
     */
    public String startLocalServer() {
        if (localServer != null) {
            return "http://127.0.0.1:" + LOCAL_PORT + "/recaptcha";
        }
        try {
            localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", LOCAL_PORT), 0);
            localServer.createContext("/recaptcha", exchange -> {
                InputStream htmlStream = RecaptchaService.class.getResourceAsStream("/web/recaptcha-widget.html");
                if (htmlStream == null) {
                    String error = "<html><body>recaptcha-widget.html not found</body></html>";
                    exchange.sendResponseHeaders(404, error.length());
                    exchange.getResponseBody().write(error.getBytes(StandardCharsets.UTF_8));
                    exchange.getResponseBody().close();
                    return;
                }

                byte[] rawHtml = htmlStream.readAllBytes();
                htmlStream.close();
                byte[] body = rawHtml;
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            localServer.setExecutor(null);
            localServer.start();
            return "http://127.0.0.1:" + LOCAL_PORT + "/recaptcha";
        } catch (IOException e) {
            // Port may already be in use — return null so caller falls back to file://
            return null;
        }
    }

    /** Stop the embedded server (call on app exit). */
    public static void stopLocalServer() {
        if (localServer != null) {
            localServer.stop(0);
            localServer = null;
        }
    }

    public String getSiteKey() {
        String configured = readSetting("RECAPTCHA_SITE_KEY", "recaptcha.siteKey");
        return (configured != null && !configured.isBlank()) ? configured : TEST_SITE_KEY;
    }

    public String getSecretKey() {
        String configured = readSetting("RECAPTCHA_SECRET_KEY", "recaptcha.secretKey");
        return (configured != null && !configured.isBlank()) ? configured : TEST_SECRET_KEY;
    }

    /** Always true — falls back to Google test keys if no env var is set. */
    public boolean isConfigured() {
        return true;
    }

    public VerificationResult verifyToken(String token) {
        String secret = getSecretKey();
        if (secret == null || secret.isBlank()) {
            return VerificationResult.failure("Secret reCAPTCHA non configure.");
        }
        if (token == null || token.isBlank()) {
            return VerificationResult.failure("Token reCAPTCHA absent.");
        }

        try {
            String body = "secret=" + urlEncode(secret)
                    + "&response=" + urlEncode(token);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERIFY_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return VerificationResult.failure("Erreur HTTP reCAPTCHA: " + response.statusCode());
            }

            JSONObject json = new JSONObject(response.body());
            boolean success = json.optBoolean("success", false);
            if (success) {
                return VerificationResult.success();
            }

            if (json.has("error-codes") && json.getJSONArray("error-codes").length() > 0) {
                return VerificationResult.failure("Erreur reCAPTCHA: " + json.getJSONArray("error-codes").join(", "));
            }
            return VerificationResult.failure("Verification reCAPTCHA refusee.");
        } catch (Exception e) {
            return VerificationResult.failure("Verification reCAPTCHA impossible: " + e.getMessage());
        }
    }

    private String readSetting(String envKey, String propKey) {
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String prop = firstNonBlank(
                System.getProperty(propKey),
                System.getProperty(envKey),
                System.getProperty(envKey.toLowerCase()),
                System.getProperty(envKey.toLowerCase().replace('_', '.'))
        );
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class VerificationResult {
        private final boolean success;
        private final String message;

        private VerificationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static VerificationResult success() {
            return new VerificationResult(true, "");
        }

        public static VerificationResult failure(String message) {
            return new VerificationResult(false, message == null ? "" : message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}




