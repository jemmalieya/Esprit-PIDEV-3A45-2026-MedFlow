package tn.esprit.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import tn.esprit.entities.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyse locale des demandes staff (sans LLM):
 * - Extraction de texte CV via OCR Tesseract (et extraction native quand possible)
 * - Verdict via regles metier offline
 */
public class StaffRequestAIAnalysisService {

    private static final String OCR_LANG_DEFAULT = "fra+eng";

    private final String ocrLang;
    private final String tessdataPath;

    public StaffRequestAIAnalysisService() {
        this.ocrLang = getSetting("OCR_LANG", OCR_LANG_DEFAULT);
        this.tessdataPath = getSetting("TESSDATA_PREFIX", "");
    }

    /**
     * Point d'entree principal: OCR + regles locales uniquement.
     */
    public StaffRequestAnalysisResult analyzeStaffRequest(User user) {
        String cvContent = extractCVContent(user.getStaffRequestProofPath());
        if (cvContent == null || cvContent.isBlank()) {
            cvContent = "[CV inaccessible - analyse basee sur metadonnees]";
        }

        StaffRequestAnalysisResult result = buildOfflineHeuristicResult(user, cvContent);
        result.source = "OCR_LOCAL_RULES";
        return result;
    }

    private String getSetting(String key, String defaultValue) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        String prop = System.getProperty(key);
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        return defaultValue;
    }

    /**
     * Extrait le contenu textuel du CV.
     * Strategie: texte natif (TXT/PDF/DOCX) puis OCR Tesseract pour PDF/images.
     */
    private String extractCVContent(String cvPath) {
        if (cvPath == null || cvPath.isBlank()) {
            return null;
        }

        try {
            File cvFile = new File(cvPath);
            if (!cvFile.exists()) {
                return null;
            }

            String fileName = cvFile.getName().toLowerCase();
            if (fileName.endsWith(".txt")) {
                return Files.readString(cvFile.toPath());
            }
            if (fileName.endsWith(".pdf")) {
                return extractFromPdfWithOcrFallback(cvFile);
            }
            if (fileName.endsWith(".docx")) {
                return extractFromDocx(cvFile);
            }
            if (fileName.endsWith(".doc")) {
                return "[DOC detecte: conversion vers DOCX recommandee]";
            }
            if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
                    || fileName.endsWith(".bmp") || fileName.endsWith(".tif") || fileName.endsWith(".tiff")) {
                return ocrImage(cvFile);
            }
            return null;
        } catch (Exception e) {
            System.err.println("[OCR] Erreur extraction CV: " + e.getMessage());
            return null;
        }
    }

    private String extractFromPdfWithOcrFallback(File pdfFile) {
        String nativeText = extractPdfText(pdfFile);
        if (nativeText != null && !nativeText.isBlank()) {
            return trimForPrompt(nativeText, 7000);
        }

        String ocrText = ocrPdf(pdfFile);
        if (ocrText != null && !ocrText.isBlank()) {
            return trimForPrompt(ocrText, 7000);
        }

        return "[PDF detecte mais extraction texte/OCR indisponible]";
    }

    private String extractPdfText(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text == null ? null : text.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String ocrPdf(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<String> pageTexts = new ArrayList<>();
            int pages = Math.min(document.getNumberOfPages(), 6);

            for (int i = 0; i < pages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 180);
                String text = runTesseractOnImage(image);
                if (text != null && !text.isBlank()) {
                    pageTexts.add(text);
                }
            }
            return pageTexts.isEmpty() ? null : String.join("\n\n", pageTexts);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractFromDocx(File docxFile) {
        try (FileInputStream fis = new FileInputStream(docxFile);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            if (text == null || text.isBlank()) {
                return "[DOCX detecte mais texte vide]";
            }
            return trimForPrompt(text, 7000);
        } catch (Exception e) {
            return "[DOCX detecte mais extraction echouee]";
        }
    }

    private String ocrImage(File imageFile) {
        try {
            String text = runTesseractCli(imageFile);
            return text == null || text.isBlank()
                    ? "[Image detectee mais OCR vide]"
                    : trimForPrompt(text, 7000);
        } catch (Exception e) {
            return "[Image detectee mais OCR indisponible]";
        }
    }

    private String runTesseractOnImage(BufferedImage image) {
        File tempImage = null;
        try {
            tempImage = File.createTempFile("medflow_ocr_", ".png");
            ImageIO.write(image, "png", tempImage);
            return runTesseractCli(tempImage);
        } catch (Exception e) {
            return null;
        } finally {
            if (tempImage != null && tempImage.exists()) {
                tempImage.delete();
            }
        }
    }

    private String runTesseractCli(File inputFile) throws Exception {
        File outBase = File.createTempFile("medflow_tess_", "");
        String outPathNoExt = outBase.getAbsolutePath();
        File outTxt = new File(outPathNoExt + ".txt");

        List<String> command = new ArrayList<>();
        command.add("tesseract");
        command.add(inputFile.getAbsolutePath());
        command.add(outPathNoExt);
        command.add("-l");
        command.add(ocrLang);

        if (!tessdataPath.isBlank()) {
            command.add("--tessdata-dir");
            command.add(tessdataPath);
        }

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();

        try {
            if (exitCode != 0 || !outTxt.exists()) {
                return null;
            }
            String text = Files.readString(outTxt.toPath());
            return text == null ? null : text.trim();
        } finally {
            if (outTxt.exists()) {
                outTxt.delete();
            }
            if (outBase.exists()) {
                outBase.delete();
            }
        }
    }

    /**
     * Regles metier locales pour generer un verdict.
     */
    private StaffRequestAnalysisResult buildOfflineHeuristicResult(User user, String cvContent) {
        int score = 0;
        JsonArray strengths = new JsonArray();
        JsonArray concerns = new JsonArray();
        JsonObject critical = defaultCriticalInfo();

        boolean hasCv = user.getStaffRequestProofPath() != null && !user.getStaffRequestProofPath().isBlank();
        if (hasCv) {
            score += 30;
            strengths.add("CV fourni");
            critical.addProperty("qualifications", "CV present dans le dossier.");
        } else {
            concerns.add("CV manquant");
            critical.addProperty("qualifications", "CV manquant.");
        }

        int docCount = countDocuments(user.getStaffDocuments());
        if (docCount >= 3) {
            score += 25;
            strengths.add("Dossier documentaire complet (>=3 pieces)");
            critical.addProperty("credentials", "Pieces justificatives minimales presentes.");
        } else {
            concerns.add("Pieces justificatives insuffisantes (minimum CV + identite + diplome)");
            critical.addProperty("credentials", "Pieces justificatives insuffisantes.");
        }

        if (user.getTypeStaff() != null && !user.getTypeStaff().isBlank()) {
            score += 15;
            strengths.add("Type staff renseigne");
        } else {
            concerns.add("Type staff non renseigne");
        }

        if (isLikelyValidEmail(user.getEmailUser())) {
            score += 10;
        } else {
            concerns.add("Email invalide ou absent");
        }

        int experienceYears = extractExperienceYears(user.getStaffRequestReason());
        if (experienceYears >= 5) {
            score += 15;
            strengths.add("Experience significative (" + experienceYears + " ans)");
            critical.addProperty("experience_match", "Experience solide declaree.");
        } else if (experienceYears >= 1) {
            score += 8;
            strengths.add("Experience presente (" + experienceYears + " ans)");
            critical.addProperty("experience_match", "Experience presente mais a confirmer.");
        } else {
            concerns.add("Experience non precisee ou trop faible");
            critical.addProperty("experience_match", "Experience non verifiable via dossier.");
        }

        if (containsAuthorization(user.getStaffRequestReason())) {
            score += 5;
            strengths.add("Numero d'autorisation detecte");
        } else {
            concerns.add("Numero d'autorisation absent");
        }

        String verdict;
        if (!hasCv || docCount < 3) {
            verdict = "REJECT";
            critical.addProperty("red_flags", "Absence de documents obligatoires.");
        } else if (score >= 80) {
            verdict = "APPROVE";
            critical.addProperty("red_flags", "Aucun signal bloquant detecte par les regles offline.");
        } else if (score >= 60) {
            verdict = "PENDING_REVIEW";
            critical.addProperty("red_flags", "Verification manuelle recommandee.");
        } else {
            verdict = "REJECT";
            critical.addProperty("red_flags", "Score dossier trop faible selon les regles offline.");
        }

        String recommendation = switch (verdict) {
            case "APPROVE" -> "Dossier coherent selon les regles locales. Validation admin finale recommandee.";
            case "PENDING_REVIEW" -> "Verifier manuellement l'experience et les accreditations avant decision.";
            default -> "Refuser ou demander un dossier complet (CV + identite + diplome + details metier).";
        };

        JsonObject full = new JsonObject();
        full.addProperty("verdict", verdict);
        full.addProperty("confidence_score", Math.max(35, Math.min(90, score)));
        full.add("critical_info", critical);
        full.add("strengths", strengths);
        full.add("concerns", concerns);
        full.addProperty("recommendation", recommendation);
        JsonArray questions = new JsonArray();
        if ("PENDING_REVIEW".equals(verdict)) {
            questions.add("Pouvez-vous confirmer les accreditations du candidat ?");
            questions.add("Le poste demande est-il aligne avec son experience reelle ?");
        }
        full.add("questions_for_admin", questions);
        full.addProperty("cv_preview", trimForPrompt(cvContent, 400));

        return new StaffRequestAnalysisResult(
                verdict,
                recommendation,
                critical.toString(),
                full.toString(),
                "OCR_LOCAL_RULES"
        );
    }

    private int countDocuments(String staffDocumentsJson) {
        if (staffDocumentsJson == null || staffDocumentsJson.isBlank()) {
            return 0;
        }
        try {
            JsonElement element = JsonParser.parseString(staffDocumentsJson);
            return element.isJsonArray() ? element.getAsJsonArray().size() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int extractExperienceYears(String reason) {
        if (reason == null || reason.isBlank()) {
            return 0;
        }
        Matcher matcher = Pattern.compile("experience\\s*=\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE).matcher(reason);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private boolean containsAuthorization(String reason) {
        if (reason == null) {
            return false;
        }
        return reason.toLowerCase().contains("autorisation=") || reason.toLowerCase().contains("autorization=");
    }

    private boolean isLikelyValidEmail(String email) {
        return email != null && email.matches("^[^@]+@[^@]+\\.[^@]+$");
    }

    private JsonObject defaultCriticalInfo() {
        JsonObject critical = new JsonObject();
        critical.addProperty("qualifications", "A confirmer");
        critical.addProperty("experience_match", "A confirmer");
        critical.addProperty("credentials", "A confirmer");
        critical.addProperty("red_flags", "A confirmer");
        return critical;
    }

    private String trimForPrompt(String value, int maxLen) {
        String safe = value == null ? "" : value;
        if (safe.length() <= maxLen) {
            return safe;
        }
        return safe.substring(0, maxLen) + "...";
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Deep analysis (public API for detail view)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Analyse approfondie : extraction CV, comparaison profil vs CV, score, points cles.
     */
    public DeepStaffAnalysisResult performDeepAnalysis(User user) {
        List<String> allDocPaths = extractAllDocumentPaths(user);

        // Extract text from main CV and other documents
        StringBuilder combinedText = new StringBuilder();
        String mainCvText = extractCVContent(user.getStaffRequestProofPath());
        if (mainCvText != null && !mainCvText.isBlank()) {
            combinedText.append(mainCvText);
        }
        for (String path : allDocPaths) {
            if (!path.equals(user.getStaffRequestProofPath())) {
                String t = extractCVContent(path);
                if (t != null && !t.isBlank()) {
                    combinedText.append("\n\n---\n").append(t);
                }
            }
        }

        String fullText = combinedText.toString().isBlank()
                ? "[Aucun texte extrait des documents fournis]"
                : combinedText.toString();

        // Base heuristic analysis (reuse existing logic)
        StaffRequestAnalysisResult baseResult = buildOfflineHeuristicResult(user, fullText);

        // Parse strengths / concerns from JSON
        List<String> strengths = new ArrayList<>();
        List<String> concerns  = new ArrayList<>();
        List<String> adminQs   = new ArrayList<>();
        int confidenceScore    = 50;
        try {
            com.google.gson.JsonObject full = com.google.gson.JsonParser.parseString(baseResult.fullAnalysisJson).getAsJsonObject();
            full.getAsJsonArray("strengths").forEach(e -> strengths.add(e.getAsString()));
            full.getAsJsonArray("concerns").forEach(e -> concerns.add(e.getAsString()));
            if (full.has("questions_for_admin")) {
                full.getAsJsonArray("questions_for_admin").forEach(e -> adminQs.add(e.getAsString()));
            }
            if (full.has("confidence_score")) {
                confidenceScore = full.get("confidence_score").getAsInt();
            }
        } catch (Exception ignored) { }

        DeepStaffAnalysisResult result = new DeepStaffAnalysisResult();
        result.verdict           = baseResult.verdict;
        result.confidenceScore   = confidenceScore;
        result.recommendation    = baseResult.recommendation;
        result.cvTextPreview     = trimForPrompt(fullText, 900);
        result.cvKeyPoints       = extractCVKeyPoints(fullText, user);
        result.comparisonTable   = buildComparisonTable(user, fullText);
        result.strengths         = strengths;
        result.concerns          = concerns;
        result.adminQuestions    = adminQs;
        result.documentPaths     = allDocPaths;
        result.analysisSource    = "OCR_LOCAL_RULES";
        return result;
    }

    public List<String> extractAllDocumentPaths(User user) {
        List<String> paths = new ArrayList<>();
        addDocPath(paths, user.getStaffRequestProofPath());
        if (user.getStaffDocuments() != null && !user.getStaffDocuments().isBlank()) {
            String normalized = user.getStaffDocuments()
                    .replace("[", "").replace("]", "").replace("\"", "").replace("\r", "\n");
            for (String token : normalized.split("\\s*;\\s*|\\s*,\\s*|\\n+")) {
                addDocPath(paths, token.trim());
            }
        }
        return paths;
    }

    private void addDocPath(List<String> paths, String path) {
        if (path != null && !path.trim().isEmpty() && !paths.contains(path.trim())) {
            paths.add(path.trim());
        }
    }

    private List<String> extractCVKeyPoints(String cvText, User user) {
        List<String> points = new ArrayList<>();
        if (cvText == null || cvText.isBlank() || cvText.startsWith("[")) {
            points.add("⚠️ Texte du CV inaccessible ou vide — vérification manuelle requise");
            return points;
        }
        String lower = cvText.toLowerCase();
        String[] lines = cvText.split("[\r\n]+");

        // Medical degrees
        String[] degrees = {"doctorat", "docteur", "md ", "m.d", "master", "licence", "baccalauréat",
                "baccalaureat", "diplôme", "diplome", "certificat", "dcem", "pcem", "d.e.s"};
        for (String line : lines) {
            String ll = line.toLowerCase();
            for (String d : degrees) {
                if (ll.contains(d) && line.trim().length() > 6) {
                    points.add("🎓 " + line.trim().replaceAll("\\s+", " "));
                    break;
                }
            }
            if (points.size() >= 2) break;
        }

        // Medical specialty
        String[] specialties = {"médecin", "medecin", "infirmier", "infirmière", "pharmacien",
                "chirurg", "cardio", "généraliste", "generaliste", "urgentiste",
                "anesthé", "pédiat", "gynéco", "dermato", "psychiatr", "neurologue",
                "radiologue", "oncologue", "rhumato"};
        for (String line : lines) {
            String ll = line.toLowerCase();
            for (String sp : specialties) {
                if (ll.contains(sp) && line.trim().length() > 4) {
                    points.add("🏥 " + line.trim().replaceAll("\\s+", " "));
                    break;
                }
            }
            if (points.stream().filter(p -> p.startsWith("🏥")).count() >= 1) break;
        }

        // Experience years from CV text
        Pattern expPat = Pattern.compile("(\\d{1,2})\\s*(ans?|ann[ée]es?)\\s*(d[''']?exp[ée]rience|d[''']exp[ée]riences?)", Pattern.CASE_INSENSITIVE);
        Matcher expm = expPat.matcher(cvText);
        if (expm.find()) {
            points.add("⏱️ Expérience mentionnée : " + expm.group(0).trim());
        }

        // Institution
        String[] insts = {"université", "universite", "university", "faculté", "faculte",
                "hôpital", "hopital", "hospital", "clinique", "chu ", "polyclinique", "établissement"};
        for (String line : lines) {
            String ll = line.toLowerCase();
            for (String ins : insts) {
                if (ll.contains(ins) && line.trim().length() > 6) {
                    points.add("🏛️ " + line.trim().replaceAll("\\s+", " "));
                    break;
                }
            }
            if (points.stream().filter(p -> p.startsWith("🏛️")).count() >= 1) break;
        }

        // Email in CV
        Matcher emailm = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}").matcher(cvText);
        if (emailm.find()) {
            points.add("📧 Email trouvé dans le CV : " + emailm.group(0));
        }

        // Skills section
        if (lower.contains("compétence") || lower.contains("competence") || lower.contains("skill") || lower.contains("aptitude")) {
            points.add("✔️ Section compétences détectée");
        }

        // Certifications
        if (lower.contains("certif") || lower.contains("accrédité") || lower.contains("accréditation") || lower.contains("habilit")) {
            points.add("🏅 Certification / habilitation mentionnée");
        }

        if (points.isEmpty()) {
            points.add("ℹ️ Aucun point clé reconnu automatiquement");
            points.add("ℹ️ Consultation manuelle du document recommandée");
        }
        return points;
    }

    private List<ComparisonRow> buildComparisonTable(User user, String cvText) {
        List<ComparisonRow> rows = new ArrayList<>();
        String lower = cvText == null ? "" : cvText.toLowerCase();
        String safeText = cvText == null ? "" : cvText;

        // --- Nom & Prénom ---
        String userName = (safe(user.getNom()) + " " + safe(user.getPrenom())).trim();
        String nameStatus;
        String cvName;
        if (lower.contains(user.getNom() == null ? "" : user.getNom().toLowerCase())
                || lower.contains(user.getPrenom() == null ? "" : user.getPrenom().toLowerCase())) {
            cvName = "Nom correspondant détecté";
            nameStatus = "MATCH";
        } else {
            cvName = "Non détecté";
            nameStatus = "UNKNOWN";
        }
        rows.add(new ComparisonRow("Nom & Prénom", userName, cvName, nameStatus));

        // --- Type staff / Spécialité ---
        String typeStaff = safe(user.getTypeStaff());
        String cvSpecialty = extractSpecialtyFromCvLower(lower);
        String specialtyStatus = (!cvSpecialty.equals("Non détecté") && !typeStaff.isBlank()
                && (lower.contains(typeStaff.toLowerCase()) || cvSpecialty.toLowerCase().contains(typeStaff.toLowerCase())))
                ? "MATCH" : (!cvSpecialty.equals("Non détecté") ? "PARTIAL" : "UNKNOWN");
        rows.add(new ComparisonRow("Type / Spécialité", typeStaff, cvSpecialty, specialtyStatus));

        // --- Email ---
        String userEmail = safe(user.getEmailUser());
        Matcher emailm = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}").matcher(safeText);
        if (emailm.find()) {
            String cvEmail = emailm.group(0);
            rows.add(new ComparisonRow("Email", userEmail, cvEmail, cvEmail.equalsIgnoreCase(userEmail) ? "MATCH" : "PARTIAL"));
        } else {
            rows.add(new ComparisonRow("Email", userEmail, "Non trouvé", "UNKNOWN"));
        }

        // --- Expérience ---
        String reasonExp = extractExpYearsStr(safe(user.getStaffRequestReason()) + " " + safe(user.getStaffRequestMessage()));
        String cvExp     = extractExpYearsStr(safeText);
        String expStatus = (reasonExp.equals("Non précisé") && cvExp.equals("Non précisé")) ? "UNKNOWN"
                : (reasonExp.equals(cvExp) ? "MATCH" : "PARTIAL");
        rows.add(new ComparisonRow("Expérience déclarée", reasonExp, cvExp, expStatus));

        // --- CIN ---
        String userCin = safe(user.getCin());
        if (!userCin.isBlank()) {
            boolean cinInCv = lower.contains(userCin.toLowerCase());
            rows.add(new ComparisonRow("CIN", userCin, cinInCv ? "✓ Trouvé dans document" : "Non détecté",
                    cinInCv ? "MATCH" : "UNKNOWN"));
        }

        // --- Diplôme attendu selon type ---
        String expectedDegree = getExpectedDegreeKeyword(typeStaff);
        if (!expectedDegree.isEmpty()) {
            boolean found = lower.contains(expectedDegree.toLowerCase());
            rows.add(new ComparisonRow("Diplôme requis (" + typeStaff + ")",
                    expectedDegree, found ? "✓ Présent" : "Non détecté",
                    found ? "MATCH" : "MISMATCH"));
        }

        return rows;
    }

    private String extractSpecialtyFromCvLower(String lower) {
        String[] sp = {"médecin", "medecin", "infirmier", "pharmacien", "chirurgien", "généraliste",
                "urgentiste", "anesthésiste", "pédiatre", "gynécologue", "dermatologue",
                "psychiatre", "neurologue", "cardiologue", "radiologue"};
        for (String s : sp) {
            if (lower.contains(s)) return capitalize(s);
        }
        return "Non détecté";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String extractExpYearsStr(String text) {
        if (text == null || text.isBlank()) return "Non précisé";
        Matcher m = Pattern.compile("(\\d{1,2})\\s*(ans?|ann[ée]es?|year)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return m.group(1) + " ans";
        return "Non précisé";
    }

    private String getExpectedDegreeKeyword(String typeStaff) {
        if (typeStaff == null || typeStaff.isBlank()) return "";
        String t = typeStaff.toLowerCase();
        if (t.contains("médecin") || t.contains("medecin") || t.contains("doctor")) return "diplôme de médecine";
        if (t.contains("infirm")) return "diplôme d'État infirmier";
        if (t.contains("pharmac")) return "diplôme de pharmacie";
        if (t.contains("chirur")) return "chirurgie";
        return "";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Result inner classes
    // ──────────────────────────────────────────────────────────────────────────

    public static class ComparisonRow {
        public final String field;
        public final String userInput;
        public final String cvExtracted;
        public final String status; // MATCH | MISMATCH | PARTIAL | UNKNOWN

        public ComparisonRow(String field, String userInput, String cvExtracted, String status) {
            this.field = field;
            this.userInput = userInput == null ? "" : userInput;
            this.cvExtracted = cvExtracted == null ? "" : cvExtracted;
            this.status = status;
        }
    }

    public static class DeepStaffAnalysisResult {
        public String verdict;
        public int confidenceScore;
        public String recommendation;
        public String cvTextPreview;
        public List<String> cvKeyPoints    = new ArrayList<>();
        public List<String> strengths      = new ArrayList<>();
        public List<String> concerns       = new ArrayList<>();
        public List<ComparisonRow> comparisonTable = new ArrayList<>();
        public List<String> adminQuestions = new ArrayList<>();
        public List<String> documentPaths  = new ArrayList<>();
        public String analysisSource       = "OCR_LOCAL_RULES";
    }

    /**
     * Resultat structure de l'analyse staff.
     */
    public static class StaffRequestAnalysisResult {
        public String verdict; // APPROVE, PENDING_REVIEW, REJECT
        public String recommendation;
        public String criticalInfoJson;
        public String fullAnalysisJson;
        public String source; // OCR_LOCAL_RULES

        public StaffRequestAnalysisResult(String verdict, String recommendation, String criticalInfoJson, String fullAnalysisJson, String source) {
            this.verdict = verdict;
            this.recommendation = recommendation;
            this.criticalInfoJson = criticalInfoJson;
            this.fullAnalysisJson = fullAnalysisJson;
            this.source = source;
        }

        @Override
        public String toString() {
            return "StaffRequestAnalysisResult{" +
                    "verdict='" + verdict + '\'' +
                    ", recommendation='" + recommendation + '\'' +
                    ", source='" + source + '\'' +
                    '}';
        }
    }
}

