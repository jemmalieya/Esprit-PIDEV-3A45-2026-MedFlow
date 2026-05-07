package tn.esprit.services;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.security.SecureRandom;

public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_SIZE_BYTES = 20;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int VALIDATION_WINDOW = 10;
    private static final Base32 BASE32 = new Base32();

    public String generateSecret() {
        byte[] random = new byte[SECRET_SIZE_BYTES];
        new SecureRandom().nextBytes(random);
        return base32Encode(random);
    }

    public String buildOtpAuthUrl(String issuer, String account, String secret) {
        String safeIssuer = issuer == null || issuer.isBlank() ? "MedFlow" : issuer.trim();
        String safeAccount = account == null || account.isBlank() ? "user" : account.trim();
        String label = urlEncode(safeIssuer + ":" + safeAccount);
        return "otpauth://totp/" + label
                + "?secret=" + urlEncode(secret)
                + "&issuer=" + urlEncode(safeIssuer)
                + "&algorithm=SHA1&digits=" + CODE_DIGITS + "&period=" + TIME_STEP_SECONDS;
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null) {
            return false;
        }
        String normalized = code.replaceAll("\\s+", "").trim();
        if (!normalized.matches("\\d{" + CODE_DIGITS + "}")) {
            return false;
        }

        String normalizedSecret = normalizeSecret(secret);
        if (normalizedSecret.isBlank()) {
            return false;
        }

        long currentCounter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        for (int offset = -VALIDATION_WINDOW; offset <= VALIDATION_WINDOW; offset++) {
            String expected = generateCode(normalizedSecret, currentCounter + offset);
            if (normalized.equals(expected)) {
                return true;
            }
        }
        return false;
    }

    public String getCurrentCode(String secret) {
        return getCodeAtOffset(secret, 0);
    }

    public String getCodeAtOffset(String secret, int stepOffset) {
        if (secret == null || secret.isBlank()) {
            return "";
        }
        String normalizedSecret = normalizeSecret(secret);
        if (normalizedSecret.isBlank()) {
            return "";
        }
        long currentCounter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        return generateCode(normalizedSecret, currentCounter + stepOffset);
    }

    private String normalizeSecret(String secret) {
        return secret.replaceAll("[^A-Za-z2-7]", "").toUpperCase(Locale.ROOT);
    }

    private String generateCode(String base32Secret, long counter) {
        try {
            byte[] secretBytes = base32Decode(base32Secret);
            byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA1"));
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception e) {
            return "";
        }
    }

    private String base32Encode(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        String encoded = BASE32.encodeToString(data);
        return encoded.replace("=", "").toUpperCase(Locale.ROOT);
    }

    private byte[] base32Decode(String input) {
        String clean = input == null ? "" : input.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return BASE32.decode(clean);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

