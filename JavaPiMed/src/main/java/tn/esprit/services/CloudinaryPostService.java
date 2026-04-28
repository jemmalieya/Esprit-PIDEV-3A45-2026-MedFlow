package tn.esprit.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.util.Map;

public class CloudinaryPostService {

    private final Cloudinary cloudinary;

    public CloudinaryPostService() {
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME_POST");
        String apiKey = System.getenv("CLOUDINARY_API_KEY_POST");
        String apiSecret = System.getenv("CLOUDINARY_API_SECRET_POST");

        if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new IllegalStateException(
                    "Variables Cloudinary manquantes : CLOUDINARY_CLOUD_NAME_POST, CLOUDINARY_API_KEY_POST, CLOUDINARY_API_SECRET_POST"
            );
        }

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    public String uploadPostImageFromUrl(String imageUrl) throws Exception {
        if (isBlank(imageUrl)) {
            throw new IllegalArgumentException("URL image obligatoire.");
        }

        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            throw new IllegalArgumentException("URL invalide. Elle doit commencer par http:// ou https://");
        }

        Map uploadResult = cloudinary.uploader().upload(
                imageUrl,
                ObjectUtils.asMap(
                        "folder", "medflow/posts",
                        "resource_type", "image",
                        "unique_filename", true,
                        "overwrite", false
                )
        );

        Object secureUrl = uploadResult.get("secure_url");

        if (secureUrl == null) {
            throw new RuntimeException("Cloudinary n'a pas retourné secure_url.");
        }

        return secureUrl.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}