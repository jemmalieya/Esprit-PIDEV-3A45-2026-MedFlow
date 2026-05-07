package tn.esprit.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DialogflowAssistantService {

    private static final String DEFAULT_LANGUAGE_CODE = "en-US";
    private static final String DEFAULT_SCOPE = "https://www.googleapis.com/auth/dialogflow";
    private static final Pattern INTENT_PATTERN = Pattern.compile("\"intent\"\\s*:\\s*\\{.*?\"displayName\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
    private static final Pattern FULFILLMENT_PATTERN = Pattern.compile("\"fulfillmentText\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile("\"parameters\"\\s*:\\s*\\{(.*?)\\}\\s*,\\s*\"allRequiredParamsPresent\"", Pattern.DOTALL);
    private static final Pattern STRING_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Properties properties = loadProperties();

    private volatile String cachedAccessToken;
    private volatile long cachedTokenExpiresAtEpochSeconds;

    public DialogflowReply detectIntent(String text, String sessionId) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Message text is required.");
        }

        ServiceAccountCredentials credentials = resolveCredentials();
        String accessToken = getAccessToken(credentials);
        String languageCode = getSetting("dialogflow.language.code", "DIALOGFLOW_LANGUAGE_CODE", DEFAULT_LANGUAGE_CODE);
        String projectId = resolveProjectId(credentials);
        String requestBody = buildDetectIntentPayload(text.trim(), languageCode);
        String endpoint = "https://dialogflow.googleapis.com/v2/projects/" + projectId + "/agent/sessions/" + urlEncode(sessionId) + ":detectIntent";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("authorization", "Bearer " + accessToken)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Dialogflow detectIntent failed with HTTP " + response.statusCode() + ": " + response.body());
            }

            return parseDetectIntentResponse(response.body());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to call Dialogflow detectIntent: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Dialogflow request was interrupted.", ex);
        }
    }

    private DialogflowReply parseDetectIntentResponse(String responseBody) {
        String intentName = unescapeJson(extractFirstGroup(INTENT_PATTERN, responseBody));
        String fulfillmentText = unescapeJson(extractFirstGroup(FULFILLMENT_PATTERN, responseBody));
        Map<String, String> parameters = extractParameters(responseBody);
        return new DialogflowReply(intentName, fulfillmentText, parameters);
    }

    private Map<String, String> extractParameters(String responseBody) {
        Matcher matcher = PARAMETERS_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            return Map.of();
        }

        String block = matcher.group(1);
        Map<String, String> parameters = new LinkedHashMap<>();
        for (String key : List.of("mode", "person", "doctor", "staff", "date-time", "date_time", "datetime", "motif", "reason")) {
            String value = extractParameterValue(block, key);
            if (value != null && !value.isBlank()) {
                parameters.put(key, value);
            }
        }
        return parameters;
    }

    private String extractParameterValue(String parametersBlock, String key) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\"((?:\\\\.|[^\"\\\\])*)\"|\\{.*?\\}|null|true|false|-?\\d+(?:\\.\\d+)?)", Pattern.DOTALL).matcher(parametersBlock);
        if (!matcher.find()) {
            return null;
        }

        String rawValue = matcher.group(1);
        if (rawValue == null || "null".equals(rawValue)) {
            return null;
        }

        if (rawValue.startsWith("\"")) {
            return unescapeJson(rawValue.substring(1, rawValue.length() - 1));
        }

        String nestedValue = extractFirstGroup(Pattern.compile("\"stringValue\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL), rawValue);
        if (nestedValue == null) {
            nestedValue = extractFirstGroup(Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL), rawValue);
        }
        if (nestedValue == null) {
            nestedValue = extractFirstGroup(Pattern.compile("\"value\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL), rawValue);
        }
        return nestedValue == null ? rawValue : unescapeJson(nestedValue);
    }

    private String getAccessToken(ServiceAccountCredentials credentials) {
        long now = Instant.now().getEpochSecond();
        if (cachedAccessToken != null && now < cachedTokenExpiresAtEpochSeconds - 60) {
            return cachedAccessToken;
        }

        String jwt = createSignedJwt(credentials);
        String requestBody = "grant_type=" + urlEncode("urn:ietf:params:oauth:grant-type:jwt-bearer") + "&assertion=" + urlEncode(jwt);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(credentials.tokenUri()))
                    .header("content-type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Failed to obtain Google access token: HTTP " + response.statusCode() + ": " + response.body());
            }

            String token = extractFirstGroup(Pattern.compile("\"access_token\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL), response.body());
            String expiresIn = extractFirstGroup(Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)", Pattern.DOTALL), response.body());
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("Google OAuth did not return an access token.");
            }

            long ttlSeconds = 3600;
            if (expiresIn != null && !expiresIn.isBlank()) {
                try {
                    ttlSeconds = Long.parseLong(expiresIn);
                } catch (NumberFormatException ignored) {
                }
            }

            cachedAccessToken = unescapeJson(token);
            cachedTokenExpiresAtEpochSeconds = now + ttlSeconds;
            return cachedAccessToken;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to request Google access token: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Token request was interrupted.", ex);
        }
    }

    private String createSignedJwt(ServiceAccountCredentials credentials) {
        try {
            long now = Instant.now().getEpochSecond();
            String header = base64UrlEncode("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
            String payload = base64UrlEncode("{" +
                    "\"iss\":\"" + escapeJson(credentials.clientEmail()) + "\"," +
                    "\"scope\":\"" + escapeJson(DEFAULT_SCOPE) + "\"," +
                    "\"aud\":\"" + escapeJson(credentials.tokenUri()) + "\"," +
                    "\"iat\":" + now + "," +
                    "\"exp\":" + (now + 3600) +
                    "}");

            String signingInput = header + "." + payload;
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(credentials.privateKey());
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return signingInput + "." + base64UrlEncode(signature.sign());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create signed Dialogflow JWT: " + ex.getMessage(), ex);
        }
    }

    private ServiceAccountCredentials resolveCredentials() {
        String credentialsPath = getSetting("dialogflow.credentials.path", "DIALOGFLOW_CREDENTIALS_PATH", "");
        if (credentialsPath.isBlank()) {
            credentialsPath = getSetting("google.application.credentials", "GOOGLE_APPLICATION_CREDENTIALS", "");
        }
        if (credentialsPath.isBlank()) {
            throw new IllegalStateException("Dialogflow credentials path is missing. Set dialogflow.credentials.path or GOOGLE_APPLICATION_CREDENTIALS.");
        }

        try {
            Path path = Path.of(credentialsPath);
            String json = Files.readString(path, StandardCharsets.UTF_8);

            String clientEmail = unescapeJson(extractRequiredJsonString(json, "client_email"));
            String privateKeyPem = unescapeJson(extractRequiredJsonString(json, "private_key"));
            String tokenUri = unescapeJson(extractOptionalJsonString(json, "token_uri"));
            String projectId = unescapeJson(extractOptionalJsonString(json, "project_id"));
            if (tokenUri == null || tokenUri.isBlank()) {
                tokenUri = "https://oauth2.googleapis.com/token";
            }

            return new ServiceAccountCredentials(clientEmail, tokenUri, projectId, parsePrivateKey(privateKeyPem));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read Dialogflow credentials file: " + ex.getMessage(), ex);
        }
    }

    private String resolveProjectId(ServiceAccountCredentials credentials) {
        String projectId = getSetting("dialogflow.project.id", "DIALOGFLOW_PROJECT_ID", "");
        if (!projectId.isBlank()) {
            return projectId;
        }

        if (credentials.projectId() != null && !credentials.projectId().isBlank()) {
            return credentials.projectId();
        }

        throw new IllegalStateException("Dialogflow project ID is missing. Set dialogflow.project.id or DIALOGFLOW_PROJECT_ID.");
    }

    private PrivateKey parsePrivateKey(String pem) {
        try {
            String sanitized = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(sanitized);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid Dialogflow service account private key: " + ex.getMessage(), ex);
        }
    }

    private String buildDetectIntentPayload(String text, String languageCode) {
        return """
                {
                  "queryInput": {
                    "text": {
                      "text": "%s",
                      "languageCode": "%s"
                    }
                  }
                }
                """.formatted(escapeJson(text), escapeJson(languageCode));
    }

    private String extractRequiredJsonString(String json, String fieldName) {
        String value = extractOptionalJsonString(json, fieldName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required field '" + fieldName + "' in Dialogflow service account JSON.");
        }
        return value;
    }

    private String extractOptionalJsonString(String json, String fieldName) {
        return extractFirstGroup(Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL), json);
    }

    private String extractFirstGroup(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String base64UrlEncode(String value) {
        return base64UrlEncode(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String escapeJson(String value) {
        String safeValue = value == null ? "" : value;
        return safeValue
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String unescapeJson(String value) {
        if (value == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                switch (next) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        if (i + 4 < value.length()) {
                            String hex = value.substring(i + 1, i + 5);
                            builder.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        }
                    }
                    default -> builder.append(next);
                }
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private String getSetting(String propertyKey, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return defaultValue == null ? "" : defaultValue.trim();
    }

    private Properties loadProperties() {
        Properties loaded = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("dialogflow.properties")) {
            if (inputStream != null) {
                loaded.load(inputStream);
            }
        } catch (IOException ignored) {
        }
        return loaded;
    }

    private record ServiceAccountCredentials(String clientEmail, String tokenUri, String projectId, PrivateKey privateKey) {
    }

    public record DialogflowReply(String intentName, String fulfillmentText, Map<String, String> parameters) {
    }

    // Expose access token for other services (Speech-to-Text) to reuse the same service account
    public String getAccessTokenPublic() {
        ServiceAccountCredentials credentials = resolveCredentials();
        return getAccessToken(credentials);
    }

    public String getDefaultLanguageCode() {
        return getSetting("dialogflow.language.code", "DIALOGFLOW_LANGUAGE_CODE", DEFAULT_LANGUAGE_CODE);
    }
}