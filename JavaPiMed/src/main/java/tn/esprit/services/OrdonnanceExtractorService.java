package tn.esprit.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class OrdonnanceExtractorService {

    private static final String OCR_API_KEY = "K83256614988957";
    private static final String OCR_API_URL = "https://api.ocr.space/parse/image";
    private static final int OCR_MAX_DIMENSION = 1600;
    private static final int LOCAL_OCR_MAX_PAGES = 4;

    // ─────────────────────────────────────────────────────────────────────────
    // POINT D'ENTRÉE : retourne le texte brut OCR
    // ─────────────────────────────────────────────────────────────────────────

    public String extraireTexteOcr(File imageFile) {
        if (imageFile == null || !imageFile.exists()) {
            System.err.println("[Ordonnance] ✗ Fichier introuvable");
            return "";
        }

        System.out.println("[Ordonnance] Analyse du fichier : " + imageFile.getName());

        String texteOcr = lancerOcrSpace(imageFile);
        if (texteOcr.isBlank()) {
            System.out.println("[Ordonnance] OCR.space indisponible ou vide, tentative OCR locale...");
            texteOcr = lancerOcrLocal(imageFile);
        }

        if (texteOcr.isBlank()) {
            System.out.println("[Ordonnance] ✗ OCR n'a retourné aucun texte");
            return "";
        }

        System.out.println("[Ordonnance] ✓ Texte OCR (" + texteOcr.length() + " chars) : " + texteOcr);
        return texteOcr;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OCR.SPACE API
    // ─────────────────────────────────────────────────────────────────────────

    private String lancerOcrSpace(File imageFile) {
        HttpURLConnection conn = null;
        File uploadFile = null;
        try {
            File source = imageFile;
            if (!isPdf(source)) {
                uploadFile = prepareForOcrSpace(imageFile);
                source = uploadFile != null ? uploadFile : imageFile;
            }

            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            URL url = new URL(OCR_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("apikey", OCR_API_KEY);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            byte[] fileBytes = Files.readAllBytes(source.toPath());
            String mimeType = detecterMimeType(source.getName());

            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                ecrireChampTexte(out, boundary, "language", "eng");
                ecrireChampTexte(out, boundary, "isOverlayRequired", "false");
                ecrireChampTexte(out, boundary, "detectOrientation", "true");
                ecrireChampTexte(out, boundary, "scale", "true");
                ecrireChampTexte(out, boundary, "OCREngine", "2");
                ecrireChampFichier(out, boundary, source.getName(), mimeType, fileBytes);
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
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String jsonResponse = sb.toString();
            System.out.println("[Ordonnance] Réponse OCR.space : "
                    + jsonResponse.substring(0, Math.min(500, jsonResponse.length())));

            return parserReponseOcr(jsonResponse);
        } catch (Exception e) {
            System.err.println("[Ordonnance] ✗ OCR.space indisponible : " + e.getMessage());
            return "";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (uploadFile != null) {
                try {
                    Files.deleteIfExists(uploadFile.toPath());
                } catch (IOException ignored) {
                }
            }
        }
    }

    private File prepareForOcrSpace(File imageFile) {
        try {
            BufferedImage original = ImageIO.read(imageFile);
            if (original == null) {
                return null;
            }

            BufferedImage reduced = redimensionnerSiNecessaire(original);
            File tmp = File.createTempFile("medflow_ordonnance_prepared_", ".png");
            ImageIO.write(reduced, "png", tmp);
            return tmp;
        } catch (Exception e) {
            System.err.println("[Ordonnance] Préparation OCR.space impossible : " + e.getMessage());
            return null;
        }
    }

    private void ecrireChampTexte(DataOutputStream out, String boundary,
                                  String nom, String valeur) throws IOException {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + nom + "\"\r\n\r\n");
        out.writeBytes(valeur + "\r\n");
    }

    private void ecrireChampFichier(DataOutputStream out, String boundary,
                                    String nomFichier,
                                    String mimeType, byte[] data) throws IOException {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + nomFichier + "\"\r\n");
        out.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");
        out.write(data);
        out.writeBytes("\r\n");
    }

    private String detecterMimeType(String nomFichier) {
        String lower = nomFichier.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return "image/tiff";
        if (lower.endsWith(".gif")) return "image/gif";
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
            if (parsedResults == null || parsedResults.isEmpty()) {
                System.err.println("[Ordonnance] ✗ Pas de ParsedResults dans la réponse");
                return "";
            }

            StringBuilder texteTotal = new StringBuilder();
            for (int i = 0; i < parsedResults.length(); i++) {
                JSONObject result = parsedResults.getJSONObject(i);
                String texte = result.optString("ParsedText", "");
                if (!texte.isBlank()) {
                    texteTotal.append(texte).append(' ');
                }
            }

            return texteTotal.toString().trim();
        } catch (Exception e) {
            System.err.println("[Ordonnance] ✗ Erreur parsing JSON OCR : " + e.getMessage());
            return "";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OCR LOCAL (Tesseract)
    // ─────────────────────────────────────────────────────────────────────────

    private String lancerOcrLocal(File inputFile) {
        try {
            String tesseractExe = resolveTesseractExecutable();
            if (tesseractExe == null || tesseractExe.isBlank()) {
                System.err.println("[Ordonnance] ✗ Tesseract introuvable. Définir TESSERACT_CMD ou installer Tesseract-OCR.");
                return "";
            }

            String name = inputFile.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".pdf")) {
                return ocrPdfLocal(inputFile, tesseractExe);
            }
            return ocrImageLocale(inputFile, tesseractExe);
        } catch (Exception e) {
            System.err.println("[Ordonnance] ✗ OCR local impossible : " + e.getMessage());
            return "";
        }
    }

    private String ocrImageLocale(File imageFile, String tesseractExe) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image == null) {
                return "";
            }

            BufferedImage reduced = redimensionnerSiNecessaire(image);
            return lancerTesseractSurImage(reduced, tesseractExe);
        } catch (Exception e) {
            System.err.println("[Ordonnance] ✗ OCR image local impossible : " + e.getMessage());
            return "";
        }
    }

    private String ocrPdfLocal(File pdfFile, String tesseractExe) {
        List<String> morceaux = new ArrayList<>();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = Math.min(document.getNumberOfPages(), LOCAL_OCR_MAX_PAGES);

            for (int i = 0; i < pages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 160);
                BufferedImage reduced = redimensionnerSiNecessaire(image);
                String texte = lancerTesseractSurImage(reduced, tesseractExe);
                if (!texte.isBlank()) {
                    morceaux.add(texte);
                }
            }
        } catch (Exception e) {
            System.err.println("[Ordonnance] ✗ OCR PDF local impossible : " + e.getMessage());
        }

        return morceaux.isEmpty() ? "" : String.join("\n\n", morceaux);
    }

    private BufferedImage redimensionnerSiNecessaire(BufferedImage image) {
        if (image == null) {
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int largest = Math.max(width, height);

        if (largest <= OCR_MAX_DIMENSION) {
            return image;
        }

        double ratio = (double) OCR_MAX_DIMENSION / largest;
        int newW = Math.max(1, (int) Math.round(width * ratio));
        int newH = Math.max(1, (int) Math.round(height * ratio));

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, newW, newH, null);
        g2d.dispose();
        return resized;
    }

    private String lancerTesseractSurImage(BufferedImage image, String tesseractExe) throws Exception {
        if (image == null) {
            return "";
        }

        File tempImage = File.createTempFile("medflow_ordonnance_", ".png");
        File outBase = File.createTempFile("medflow_ordo_tess_", "");
        String outPathNoExt = outBase.getAbsolutePath();
        File outTxt = new File(outPathNoExt + ".txt");

        try {
            ImageIO.write(image, "png", tempImage);
            List<String> command = new ArrayList<>();
            command.add(tesseractExe);
            command.add(tempImage.getAbsolutePath());
            command.add(outPathNoExt);
            command.add("-l");
            command.add(resolveOcrLanguage());

            String tessdataDir = resolveTessdataDir();
            if (!tessdataDir.isBlank()) {
                command.add("--tessdata-dir");
                command.add(tessdataDir);
            }

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();

            if (exitCode != 0 || !outTxt.exists()) {
                return "";
            }

            String text = Files.readString(outTxt.toPath(), StandardCharsets.UTF_8);
            return text.trim();
        } catch (IOException e) {
            System.err.println("[Ordonnance] ✗ Tesseract non lancé : " + e.getMessage());
            return "";
        } finally {
            try {
                Files.deleteIfExists(tempImage.toPath());
            } catch (IOException ignored) {
            }
            try {
                Files.deleteIfExists(outTxt.toPath());
            } catch (IOException ignored) {
            }
            try {
                Files.deleteIfExists(outBase.toPath());
            } catch (IOException ignored) {
            }
        }
    }

    private String resolveOcrLanguage() {
        String lang = System.getenv("OCR_LANG");
        if (lang == null || lang.isBlank()) {
            return "eng";
        }
        return lang.trim();
    }

    private String resolveTessdataDir() {
        String env = System.getenv("TESSDATA_PREFIX");
        if (env != null && !env.isBlank()) {
            String trimmed = env.trim();
            File f = new File(trimmed);
            if (f.exists()) {
                if (trimmed.toLowerCase(Locale.ROOT).endsWith("tessdata")) {
                    return f.getAbsolutePath();
                }
                File nested = new File(f, "tessdata");
                if (nested.exists()) {
                    return nested.getAbsolutePath();
                }
                return f.getAbsolutePath();
            }
        }
        return "";
    }

    private String resolveTesseractExecutable() {
        // 1) variables d'environnement explicites
        String[] envKeys = {"TESSERACT_CMD", "TESSERACT_PATH", "TESSERACT_EXE"};
        for (String key : envKeys) {
            String candidate = normalizeExecutablePath(System.getenv(key));
            if (isUsableExecutable(candidate)) {
                return candidate;
            }
        }

        // 2) chemins Windows usuels
        String[] windowsCandidates = {
                "C:\\Program Files\\Tesseract-OCR\\tesseract.exe",
                "C:\\Program Files (x86)\\Tesseract-OCR\\tesseract.exe",
                "C:\\Program Files\\Tesseract-OCR\\tesseract",
                "C:\\Program Files (x86)\\Tesseract-OCR\\tesseract"
        };
        for (String candidate : windowsCandidates) {
            if (isUsableExecutable(candidate)) {
                return candidate;
            }
        }

        // 3) PATH
        String fromPath = searchTesseractInPath();
        if (isUsableExecutable(fromPath)) {
            return fromPath;
        }

        return "";
    }

    private String searchTesseractInPath() {
        try {
            Process process = new ProcessBuilder("where", "tesseract")
                    .redirectErrorStream(true)
                    .start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        sb.append(line.trim()).append('\n');
                    }
                }
            }
            process.waitFor();
            if (sb.isEmpty()) {
                return "";
            }
            return sb.toString().split("\\R")[0].trim();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isPdf(File file) {
        if (file == null) {
            return false;
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".pdf");
    }

    private String normalizeExecutablePath(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().replace('"', ' ').trim();
    }

    private boolean isUsableExecutable(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        File f = new File(path.trim());
        return f.exists() && f.isFile();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXTRACTION CIBLÉE : uniquement les médicaments attendus dans le texte OCR
    // ─────────────────────────────────────────────────────────────────────────

    public List<String> extraireMedicamentsCibles(String texteOcr, List<String> nomsMedicamentsCibles) {
        if (texteOcr == null || texteOcr.isBlank()) return Collections.emptyList();
        if (nomsMedicamentsCibles == null || nomsMedicamentsCibles.isEmpty()) return Collections.emptyList();

        String texteNorm = texteOcr.toLowerCase(Locale.ROOT);
        List<String> trouves = new ArrayList<>();

        for (String med : nomsMedicamentsCibles) {
            String medLower = med.toLowerCase(Locale.ROOT);
            boolean trouve = false;

            if (texteNorm.contains(medLower)) {
                trouve = true;
            } else if (medLower.length() >= 6 && texteNorm.contains(medLower.substring(0, 6))) {
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
