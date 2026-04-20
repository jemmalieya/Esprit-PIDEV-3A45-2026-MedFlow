package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class OrdonnanceExtractorService {

    private static final String OCR_API_KEY = "K83256614988957";
    private static final String OCR_API_URL = "https://api.ocr.space/parse/image";

    // ─────────────────────────────────────────────────────────────────────────
    // POINT D'ENTRÉE : retourne le texte brut OCR (plus de détection générique)
    // ─────────────────────────────────────────────────────────────────────────

    public String extraireTexteOcr(File imageFile) throws Exception {
        System.out.println("[Ordonnance] Analyse du fichier : " + imageFile.getName());
        String texteOcr = lancerOcrSpace(imageFile);

        if (texteOcr == null || texteOcr.isBlank()) {
            System.out.println("[Ordonnance] ✗ OCR n'a retourné aucun texte");
            return "";
        }

        System.out.println("[Ordonnance] ✓ Texte OCR (" + texteOcr.length() + " chars) : " + texteOcr);
        return texteOcr;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OCR.SPACE API
    // ─────────────────────────────────────────────────────────────────────────

    private String lancerOcrSpace(File imageFile) throws Exception {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();

        URL url = new URL(OCR_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("apikey", OCR_API_KEY);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        byte[] fileBytes = Files.readAllBytes(imageFile.toPath());
        String mimeType = detecterMimeType(imageFile.getName());

        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
            ecrireChampTexte(out, boundary, "language", "eng");
            ecrireChampTexte(out, boundary, "isOverlayRequired", "false");
            ecrireChampTexte(out, boundary, "detectOrientation", "true");
            ecrireChampTexte(out, boundary, "scale", "true");
            ecrireChampTexte(out, boundary, "OCREngine", "2");
            ecrireChampFichier(out, boundary, "file", imageFile.getName(), mimeType, fileBytes);
            out.writeBytes("--" + boundary + "--\r\n");
            out.flush();
        }

        int code = conn.getResponseCode();
        System.out.println("[Ordonnance] OCR.space HTTP " + code);

        if (code != 200) {
            System.err.println("[Ordonnance] ✗ Erreur HTTP " + code);
            return "";
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        String jsonResponse = sb.toString();
        System.out.println("[Ordonnance] Réponse OCR.space : "
                + jsonResponse.substring(0, Math.min(500, jsonResponse.length())));

        return parserReponseOcr(jsonResponse);
    }

    private void ecrireChampTexte(DataOutputStream out, String boundary,
                                  String nom, String valeur) throws IOException {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + nom + "\"\r\n\r\n");
        out.writeBytes(valeur + "\r\n");
    }

    private void ecrireChampFichier(DataOutputStream out, String boundary,
                                    String nom, String nomFichier,
                                    String mimeType, byte[] data) throws IOException {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + nom
                + "\"; filename=\"" + nomFichier + "\"\r\n");
        out.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");
        out.write(data);
        out.writeBytes("\r\n");
    }

    private String detecterMimeType(String nomFichier) {
        String lower = nomFichier.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".bmp"))  return "image/bmp";
        if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return "image/tiff";
        if (lower.endsWith(".gif"))  return "image/gif";
        return "image/jpeg";
    }

    private String parserReponseOcr(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);

            boolean isErrored = json.optBoolean("IsErroredOnProcessing", false);
            if (isErrored) {
                String errMsg = json.optString("ErrorMessage", "Erreur inconnue");
                System.err.println("[Ordonnance] ✗ OCR.space erreur : " + errMsg);
                return "";
            }

            JSONArray parsedResults = json.optJSONArray("ParsedResults");
            if (parsedResults == null || parsedResults.length() == 0) {
                System.err.println("[Ordonnance] ✗ Pas de ParsedResults dans la réponse");
                return "";
            }

            StringBuilder texteTotal = new StringBuilder();
            for (int i = 0; i < parsedResults.length(); i++) {
                JSONObject result = parsedResults.getJSONObject(i);
                String texte = result.optString("ParsedText", "");
                if (!texte.isBlank()) {
                    texteTotal.append(texte).append(" ");
                }
            }

            return texteTotal.toString().trim();

        } catch (Exception e) {
            System.err.println("[Ordonnance] ✗ Erreur parsing JSON OCR : " + e.getMessage());
            return "";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXTRACTION CIBLÉE : uniquement les médicaments du panier dans le texte
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compare le texte OCR uniquement aux noms des médicaments attendus (panier).
     * Élimine tout faux positif (mots Java, mots communs, etc.)
     */
    public List<String> extraireMedicamentsCibles(String texteOcr, List<String> nomsMedicamentsCibles) {
        if (texteOcr == null || texteOcr.isBlank()) return Collections.emptyList();
        if (nomsMedicamentsCibles == null || nomsMedicamentsCibles.isEmpty()) return Collections.emptyList();

        String texteNorm = texteOcr.toLowerCase(Locale.ROOT);
        List<String> trouves = new ArrayList<>();

        for (String med : nomsMedicamentsCibles) {
            String medLower = med.toLowerCase(Locale.ROOT);

            boolean trouve = false;

            // 1. Correspondance exacte
            if (texteNorm.contains(medLower)) {
                trouve = true;
            }
            // 2. Correspondance sur les 6 premiers caractères (ex: "aspirin" ~ "aspirine")
            else if (medLower.length() >= 6 && texteNorm.contains(medLower.substring(0, 6))) {
                trouve = true;
            }

            if (trouve) {
                trouves.add(medLower);
                System.out.println("[Ordonnance] ✓ Trouvé dans ordonnance : " + medLower);
            } else {
                System.out.println("[Ordonnance] ✗ Absent de l'ordonnance : " + medLower);
            }
        }

        return trouves;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFICATION COUVERTURE ORDONNANCE
    // ─────────────────────────────────────────────────────────────────────────

    public boolean ordonnanceCouvreInteractions(
            List<String> medicamentsOrdonnance,
            List<DrugInteractionResult> interactions) {

        if (medicamentsOrdonnance == null || medicamentsOrdonnance.isEmpty()) return false;
        if (interactions == null || interactions.isEmpty()) return true;

        Set<String> medOrdoLower = new HashSet<>();
        for (String m : medicamentsOrdonnance) {
            medOrdoLower.add(m.toLowerCase(Locale.ROOT));
        }

        System.out.println("[Ordonnance] Médicaments validés depuis ordonnance : " + medOrdoLower);

        for (DrugInteractionResult interaction : interactions) {
            String a = interaction.getMedicamentA().toLowerCase(Locale.ROOT);
            String b = interaction.getMedicamentB().toLowerCase(Locale.ROOT);

            boolean aPresent = correspondanceFloue(a, medOrdoLower);
            boolean bPresent = correspondanceFloue(b, medOrdoLower);

            System.out.println("[Ordonnance] " + a + " présent=" + aPresent
                    + " | " + b + " présent=" + bPresent);

            if (!aPresent || !bPresent) {
                System.out.println("[Ordonnance] ✗ Non couvert : " + (!aPresent ? a : b));
                return false;
            }
        }

        System.out.println("[Ordonnance] ✓ Tous les médicaments couverts");
        return true;
    }

    private boolean correspondanceFloue(String nomMed, Set<String> liste) {
        for (String item : liste) {
            if (item.equals(nomMed)) return true;
            if (item.contains(nomMed) || nomMed.contains(item)) return true;
            if (nomMed.length() >= 6 && item.length() >= 6
                    && nomMed.substring(0, 6).equals(item.substring(0, 6))) return true;
        }
        return false;
    }
}