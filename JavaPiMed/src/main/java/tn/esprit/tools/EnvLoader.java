package tn.esprit.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Utilitaire pour charger les variables d'environnement depuis le fichier .env.local
 * Appeler EnvLoader.load() au démarrage de l'application
 */
public class EnvLoader {

    /**
     * Charge les variables d'environnement depuis .env.local ou .env
     * Remplace les variable d'environnement système si elles existent dans le fichier
     */
    public static void load() {
        String[] possiblePaths = {
                ".env.local",
                ".env"
        };

        for (String path : possiblePaths) {
            try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();

                    // Ignorer les commentaires et lignes vides
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    // Parser les variables KEY=VALUE
                    int eqIndex = line.indexOf('=');
                    if (eqIndex > 0) {
                        String key = line.substring(0, eqIndex).trim();
                        String value = line.substring(eqIndex + 1).trim();

                        // Retirer les guillemets si présents
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }

                        publishPropertyAliases(key, value);

                        System.out.println("[EnvLoader] Charged: " + key);
                    }
                }
                System.out.println("[EnvLoader] Loaded environment from: " + path);
                return;
            } catch (IOException ignored) {
                // Fichier non trouvé, continuation
            }
        }
    }

    private static void publishPropertyAliases(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }

        String trimmedKey = key.trim();
        String normalized = trimmedKey.toLowerCase(Locale.ROOT);

        System.setProperty(trimmedKey, value);
        System.setProperty(normalized, value);
        System.setProperty(normalized.replace('_', '.'), value);

        switch (trimmedKey) {
            case "BREVO_API_KEY" -> System.setProperty("brevo.api.key", value);
            case "BREVO_SENDER_EMAIL" -> System.setProperty("brevo.sender.email", value);
            case "BREVO_SENDER_NAME" -> System.setProperty("brevo.sender.name", value);
            case "BREVO_API_URL" -> System.setProperty("brevo.api.url", value);
            case "MEDFLOW_MAIL_USER" -> System.setProperty("mail.user", value);
            case "RECAPTCHA_SITE_KEY" -> System.setProperty("recaptcha.siteKey", value);
            case "RECAPTCHA_SECRET_KEY" -> System.setProperty("recaptcha.secretKey", value);
            default -> {
            }
        }
    }
}


