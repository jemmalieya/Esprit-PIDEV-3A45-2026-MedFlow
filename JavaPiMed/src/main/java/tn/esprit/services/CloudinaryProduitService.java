package tn.esprit.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.util.Map;

public class CloudinaryProduitService {

    private final Cloudinary cloudinary;

    public CloudinaryProduitService() {
        String cloudName = getConfig("CLOUDINARY_CLOUD_NAME_PRODUIT");
        String apiKey = getConfig("CLOUDINARY_API_KEY_PRODUIT");
        String apiSecret = getConfig("CLOUDINARY_API_SECRET_PRODUIT");

        if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new IllegalStateException(
                    "Variables Cloudinary manquantes. Vérifie : " +
                            "CLOUDINARY_CLOUD_NAME_PRODUIT, " +
                            "CLOUDINARY_API_KEY_PRODUIT, " +
                            "CLOUDINARY_API_SECRET_PRODUIT."
            );
        }

        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName.trim(),
                "api_key", apiKey.trim(),
                "api_secret", apiSecret.trim(),
                "secure", true
        ));
    }

    public String uploadImageProduit(File file) {
        try {
            if (file == null || !file.exists()) {
                throw new IllegalArgumentException("Image introuvable.");
            }

            Map uploadResult = cloudinary.uploader().upload(
                    file,
                    ObjectUtils.asMap(
                            "folder", "medflow/produits",
                            "resource_type", "image"
                    )
            );

            Object secureUrl = uploadResult.get("secure_url");

            if (secureUrl == null) {
                throw new RuntimeException("Cloudinary n'a pas retourné secure_url.");
            }

            return secureUrl.toString();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur upload Cloudinary : " + e.getMessage());
        }
    }

    private String getConfig(String key) {
        String value = System.getenv(key);

        if (isBlank(value)) {
            value = System.getProperty(key);
        }

        if (value == null) return null;

        value = value.trim();

        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1).trim();
        }

        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}