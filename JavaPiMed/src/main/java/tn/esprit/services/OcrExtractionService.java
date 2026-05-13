package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for OCR extraction from PDF files using OCR.space API
 * Free API endpoint: https://api.ocr.space/parse
 */
public class OcrExtractionService {
    
    private static final String OCR_SPACE_URL = "https://api.ocr.space/parse";
    private static final String API_KEY = "K87899142C"; // Free API key (you can replace with paid key)
    private static final Gson gson = new Gson();
    private static String lastError = "";
    
    /**
     * Extract text from a PDF file using OCR.space API
     * @param pdfFile the PDF file to extract text from
     * @return extracted text content, or null if extraction failed
     */
    public static String extractTextFromPdf(File pdfFile) {
        lastError = "";
        if (pdfFile == null || !pdfFile.exists() || !pdfFile.getName().toLowerCase().endsWith(".pdf")) {
            lastError = "Fichier PDF invalide: " + (pdfFile != null ? pdfFile.getAbsolutePath() : "null");
            System.err.println(lastError);
            return null;
        }

        String embeddedText = extractEmbeddedPdfText(pdfFile);
        if (embeddedText != null && !embeddedText.trim().isEmpty()) {
            System.out.println("[OCR] Extracted " + embeddedText.length() + " characters with PDFBox");
            return embeddedText;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost uploadFile = new HttpPost(OCR_SPACE_URL);
            
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", pdfFile);
            builder.addTextBody("apikey", API_KEY);
            builder.addTextBody("language", "fre"); // French language
            builder.addTextBody("isOverlayRequired", "false");
            builder.addTextBody("OCREngine", "2");
            builder.addTextBody("scale", "true");
            
            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);
            
            try (CloseableHttpResponse response = httpClient.execute(uploadFile)) {
                int statusCode = response.getStatusLine().getStatusCode();
            
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                
                    // Parse JSON response
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                    if (jsonResponse.has("IsErroredOnProcessing") && jsonResponse.get("IsErroredOnProcessing").getAsBoolean()) {
                        lastError = "Erreur OCR.space: " + readErrorMessage(jsonResponse);
                        System.err.println(lastError);
                        return null;
                    }
                
                    String extractedText = readParsedText(jsonResponse);
                    if (extractedText != null && !extractedText.trim().isEmpty()) {
                        System.out.println("[OCR] Successfully extracted " + extractedText.length() + " characters");
                        return extractedText;
                    }

                    lastError = "OCR.space n'a retourne aucun texte pour ce PDF.";
                } else {
                    lastError = "OCR API request failed with status code: " + statusCode;
                    System.err.println(lastError);
                }
            }
        } catch (Exception e) {
            lastError = "Erreur extraction OCR: " + e.getMessage();
            System.err.println(lastError);
            e.printStackTrace();
        }
        
        return null;
    }

    public static String getLastError() {
        return lastError == null || lastError.isBlank() ? "Aucun texte detecte dans le PDF." : lastError;
    }

    private static String extractEmbeddedPdfText(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (Exception e) {
            lastError = "Lecture PDF locale impossible: " + e.getMessage();
            return null;
        }
    }

    private static String readParsedText(JsonObject jsonResponse) {
        if (jsonResponse.has("ParsedText") && !jsonResponse.get("ParsedText").isJsonNull()) {
            return jsonResponse.get("ParsedText").getAsString();
        }

        if (!jsonResponse.has("ParsedResults") || !jsonResponse.get("ParsedResults").isJsonArray()) {
            return "";
        }

        StringBuilder text = new StringBuilder();
        JsonArray parsedResults = jsonResponse.getAsJsonArray("ParsedResults");
        for (JsonElement element : parsedResults) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject result = element.getAsJsonObject();
            if (result.has("ParsedText") && !result.get("ParsedText").isJsonNull()) {
                text.append(result.get("ParsedText").getAsString()).append('\n');
            }
        }
        return text.toString();
    }

    private static String readErrorMessage(JsonObject jsonResponse) {
        if (!jsonResponse.has("ErrorMessage") || jsonResponse.get("ErrorMessage").isJsonNull()) {
            return "erreur inconnue";
        }

        JsonElement error = jsonResponse.get("ErrorMessage");
        if (error.isJsonArray()) {
            List<String> messages = new ArrayList<>();
            for (JsonElement element : error.getAsJsonArray()) {
                messages.add(element.getAsString());
            }
            return String.join(" ", messages);
        }
        return error.getAsString();
    }
    
    /**
     * Parse OCR extracted text into a JSON object based on document type
     * @param extractedText raw OCR text
     * @param documentType "FICHE_MEDICALE" or "PRESCRIPTION"
     * @return JsonObject with parsed fields
     */
    public static JsonObject parseOcrTextByType(String extractedText, String documentType) {
        if (extractedText == null || extractedText.isEmpty()) {
            return new JsonObject();
        }

        String cleanedText = cleanOcrText(extractedText);
        
        JsonObject result = new JsonObject();

        String normalizedDocumentType = normalizeDocumentType(documentType);
        String detectedDocumentType = detectDocumentTypeFromText(cleanedText);

        if (detectedDocumentType.equals("fiche medicale") || normalizedDocumentType.equals("fiche medicale") || normalizedDocumentType.equals("fiche_medicale")) {
            parseFicheMedicaleSmart(cleanedText, result);
        } else if (detectedDocumentType.equals("prescription") || normalizedDocumentType.equals("prescription")) {
            parsePrescriptionSmart(cleanedText, result);
        }
        
        return result;
    }

    private static String normalizeDocumentType(String documentType) {
        return documentType == null ? "" : normalize(documentType).replace('-', ' ');
    }

    private static String detectDocumentTypeFromText(String text) {
        String[] lines = normalizeLines(text);
        for (int i = 0; i < Math.min(lines.length, 12); i++) {
            String normalized = normalize(lines[i]);
            if (containsAny(normalized, "fiche medicale", "fiche medical", "fiche medicale", "fiche mediale")) {
                return "fiche medicale";
            }
            if (containsAny(normalized, "prescription")) {
                return "prescription";
            }
        }

        if (normalize(text).contains("fiche medicale") || normalize(text).contains("fiche medical")) {
            return "fiche medicale";
        }
        if (normalize(text).contains("prescription")) {
            return "prescription";
        }
        return "";
    }

    private static String cleanOcrText(String text) {
        if (text == null) {
            return "";
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        normalized = normalized.replace('\u00a0', ' ');
        normalized = normalized.replaceAll("[ \t]+\n", "\n");
        normalized = normalized.replaceAll("\n{3,}", "\n\n");
        return normalized.trim();
    }

    private static boolean containsAny(String text, String... patterns) {
        if (text == null || patterns == null) {
            return false;
        }

        for (String pattern : patterns) {
            if (pattern != null && !pattern.isBlank() && text.contains(normalize(pattern))) {
                return true;
            }
        }
        return false;
    }
    
    private static void parseFicheMedicale(String text, JsonObject result) {
        // Extract key fields from medical file text
        // This is a basic pattern matching approach; adjust regex patterns as needed
        
        String diagnostic = extractField(text, "diagnostic", "diagnostic|diagnosis");
        result.addProperty("diagnostic", diagnostic);
        
        String observations = extractField(text, "observations", "observations|notes|remarks");
        result.addProperty("observations", observations);
        
        String resultatsExamens = extractField(text, "resultats", "resultats|résultats|examen|tests");
        result.addProperty("resultatsExamens", resultatsExamens);
        
        String duree = extractField(text, "duree", "duré|durée|duration");
        result.addProperty("dureeMinutes", duree.isEmpty() ? "0" : duree);
    }
    
    private static void parsePrescription(String text, JsonObject result) {
        // Extract medication information from prescription text
        
        String nomMedicament = extractField(text, "medicament", "medicament|médicament|drug|medicine");
        result.addProperty("nomMedicament", nomMedicament);
        
        String dose = extractField(text, "dose", "dose");
        result.addProperty("dose", dose);
        
        String frequence = extractField(text, "frequence", "frequence|fréquence|frequency");
        result.addProperty("frequence", frequence);
        
        String duree = extractField(text, "duree", "duré|durée|duration");
        result.addProperty("duree", duree);
        
        String instructions = extractField(text, "instructions", "instructions|instructions|mode");
        result.addProperty("instructions", instructions);
    }
    
    private static void parseFicheMedicaleSmart(String text, JsonObject result) {
        result.addProperty("diagnostic", extractSectionByHeadings(text,
                new String[]{"diagnostic", "diagnostic associe", "diagnostic associe", "diagnostic associe", "diagnosis"},
                new String[]{"observations", "observation"}));

        result.addProperty("observations", extractSectionByHeadings(text,
                new String[]{"observations", "observation"},
                new String[]{"resultats des examens", "resultats examens", "resultats d'examens", "resultats", "motif de consultation", "signature du docteur", "signature"}));

        result.addProperty("resultatsExamens", extractSectionByHeadings(text,
                new String[]{"resultats des examens", "resultats examens", "resultats d'examens", "resultats", "examens", "examen", "tests"},
                new String[]{"motif de consultation", "signature du docteur", "signature", "prescription"}));

        String duree = extractDurationFromText(text);
        result.addProperty("dureeMinutes", duree.isEmpty() ? "0" : duree);
    }

    private static void parsePrescriptionSmart(String text, JsonObject result) {
        PrescriptionTableRow row = extractPrescriptionTableRow(text);
        if (!hasPrescriptionCoreData(row)) {
            row = extractPrescriptionTableRowAlternate(text);
        }
        result.addProperty("nomMedicament", row.nomMedicament);
        result.addProperty("dose", row.dose);
        result.addProperty("frequence", row.frequence);
        result.addProperty("duree", row.duree);

        result.addProperty("instructions", extractPrescriptionInstructionSection(text));

        result.addProperty("diagnosticAssocie", extractPrescriptionDiagnosticSection(text));
    }

    private static boolean hasPrescriptionCoreData(PrescriptionTableRow row) {
        if (row == null) {
            return false;
        }
        return !cleanValue(row.nomMedicament).isEmpty()
                || !cleanValue(row.dose).isEmpty()
                || !cleanValue(row.frequence).isEmpty()
                || !cleanValue(row.duree).isEmpty();
    }

    private static String extractSectionByHeadings(String text, String[] startAliases, String[] endAliases) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String[] lines = normalizeLines(text);
        int startIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (isHeadingLine(lines[i], startAliases)) {
                startIndex = i;
                break;
            }
        }

        if (startIndex == -1) {
            return "";
        }

        List<String> collected = new ArrayList<>();
        String inlineValue = extractInlineValueAfterHeading(lines[startIndex], startAliases);
        if (!inlineValue.isEmpty()) {
            collected.add(inlineValue);
        }

        for (int i = startIndex + 1; i < lines.length; i++) {
            String candidate = cleanValue(lines[i]);
            if (candidate.isEmpty()) {
                if (!collected.isEmpty()) {
                    collected.add("");
                }
                continue;
            }

            if (isHeadingLine(candidate, endAliases) || isHeadingLine(candidate, startAliases) || looksLikePrescriptionTableHeader(candidate)) {
                break;
            }

            collected.add(candidate);
        }

        return trimCollectedLines(collected);
    }

    private static String extractPrescriptionInstructionSection(String text) {
        String section = extractSectionByHeadings(text,
                new String[]{"instructions", "mode d'emploi", "mode emploi"},
                new String[]{"diagnostic associe", "diagnostic associé", "signature du docteur", "signature"});
        if (!section.isBlank()) {
            return section;
        }

        Pattern pattern = Pattern.compile("(?is)(?:^|\\n)\\s*(?:instructions|mode d'emploi|mode emploi)\\s*[:\\-]?\\s*(.*?)(?=\\n\\s*(?:diagnostic associe|diagnostic associé|signature du docteur|signature)\\s*[:\\-]?|$)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return cleanupExtractedBlock(matcher.group(1));
        }

        return "";
    }

    private static String extractPrescriptionDiagnosticSection(String text) {
        String section = extractSectionByHeadings(text,
                new String[]{"diagnostic associe", "diagnostic associé"},
                new String[]{"signature du docteur", "signature"});
        if (!section.isBlank()) {
            return section;
        }

        Pattern pattern = Pattern.compile("(?is)(?:^|\\n)\\s*(?:diagnostic associe|diagnostic associé)\\s*[:\\-]?\\s*(.*?)(?=\\n\\s*(?:signature du docteur|signature)\\s*[:\\-]?|$)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return cleanupExtractedBlock(matcher.group(1));
        }

        return "";
    }

    private static String extractDurationFromText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String[] lines = normalizeLines(cleanOcrText(text));
        Pattern numberPattern = Pattern.compile("(?i)\\b(\\d+)\\b");

        for (int i = 0; i < lines.length; i++) {
            String currentLine = cleanValue(lines[i]);
            if (currentLine.isEmpty()) {
                continue;
            }

            if (!containsDurationHeading(currentLine)) {
                continue;
            }

            String sameLineValue = extractNumberFromTextAfterHeading(currentLine, numberPattern);
            if (!sameLineValue.isEmpty()) {
                return sameLineValue;
            }

            for (int offset = 1; offset <= 3 && i + offset < lines.length; offset++) {
                String lookAheadLine = cleanValue(lines[i + offset]);
                if (lookAheadLine.isEmpty()) {
                    continue;
                }

                if (isHeadingLine(lookAheadLine, new String[]{"diagnostic", "observations", "resultats des examens", "resultats examens", "resultats d'examens", "motif de consultation", "signature", "prescription"})) {
                    break;
                }

                String lookAheadValue = extractFirstNumber(lookAheadLine, numberPattern);
                if (!lookAheadValue.isEmpty()) {
                    return lookAheadValue;
                }
            }
        }

        Matcher fallbackMatcher = Pattern.compile("(?i)(?:duration|durée|duree).*?(\\d+)").matcher(cleanOcrText(text));
        if (fallbackMatcher.find()) {
            return fallbackMatcher.group(1);
        }

        return "";
    }

    private static boolean containsDurationHeading(String line) {
        String normalized = normalize(line);
        return normalized.contains("duration") || normalized.contains("duree");
    }

    private static String extractNumberFromTextAfterHeading(String line, Pattern numberPattern) {
        Matcher matcher = numberPattern.matcher(line);
        while (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static String extractFirstNumber(String line, Pattern numberPattern) {
        Matcher matcher = numberPattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static String extractLeadingNumber(String value) {
        if (value == null) {
            return "";
        }

        Matcher matcher = Pattern.compile("(\\d+)").matcher(value);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return cleanValue(value);
    }

    private static boolean isHeadingLine(String line, String[] aliases) {
        String normalized = normalize(line);
        for (String alias : aliases) {
            String aliasNormalized = normalize(alias);
            if (aliasNormalized.isEmpty()) {
                continue;
            }
            if (normalized.equals(aliasNormalized)
                    || normalized.startsWith(aliasNormalized + ":")
                    || normalized.startsWith(aliasNormalized + " ")
                    || normalized.contains(aliasNormalized + ":")
                    || normalized.contains(aliasNormalized + " ")) {
                return true;
            }
        }
        return false;
    }

    private static String extractInlineValueAfterHeading(String line, String[] aliases) {
        String normalizedLine = normalize(line);
        for (String alias : aliases) {
            String aliasNormalized = normalize(alias);
            int index = normalizedLine.indexOf(aliasNormalized);
            if (index >= 0) {
                String original = line.substring(Math.min(line.length(), index + alias.length()));
                original = original.replaceFirst("^[\\s:.-]+", "");
                if (!normalize(original).equals(aliasNormalized)) {
                    return cleanValue(original);
                }
            }
        }
        return "";
    }

    private static String trimCollectedLines(List<String> lines) {
        int start = 0;
        int end = lines.size();
        while (start < end && lines.get(start).isBlank()) {
            start++;
        }
        while (end > start && lines.get(end - 1).isBlank()) {
            end--;
        }
        return String.join("\n", lines.subList(start, end)).trim();
    }

    private static boolean looksLikePrescriptionTableHeader(String line) {
        String normalized = normalize(line);
        return normalized.contains("medicament") && normalized.contains("dose") && normalized.contains("frequence") && normalized.contains("duree");
    }

    private static PrescriptionTableRow extractPrescriptionTableRow(String text) {
        PrescriptionTableRow row = new PrescriptionTableRow();
        String[] lines = normalizeLines(text);

        String tableCandidate = findPrescriptionRowCandidate(lines);
        if (!tableCandidate.isEmpty()) {
            row = parsePrescriptionRow(tableCandidate);
            if (!row.nomMedicament.isEmpty() || !row.dose.isEmpty() || !row.frequence.isEmpty() || !row.duree.isEmpty()) {
                return row;
            }
        }

        // Fallback: try to parse from the whole cleaned text if the row was not isolated.
        row = parsePrescriptionRow(text);
        return row;
    }

    private static PrescriptionTableRow extractPrescriptionTableRowAlternate(String text) {
        PrescriptionTableRow row = new PrescriptionTableRow();
        if (text == null || text.isBlank()) {
            return row;
        }

        String[] lines = normalizeLines(text);
        String tableCandidate = findPrescriptionRowCandidateAlternate(lines);
        if (!tableCandidate.isEmpty()) {
            row = parsePrescriptionRowAlternate(tableCandidate);
            if (hasPrescriptionCoreData(row)) {
                return row;
            }
        }

        // Last-resort fallback for exports where table cells are flattened into one line.
        return parsePrescriptionRowAlternate(cleanOcrText(text));
    }

    private static String findPrescriptionRowCandidateAlternate(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String headerLine = cleanValue(lines[i]);
            if (headerLine.isEmpty()) {
                continue;
            }

            if (!looksLikePrescriptionHeader(normalize(headerLine))) {
                continue;
            }

            for (int j = i + 1; j < lines.length && j <= i + 8; j++) {
                String candidate = cleanValue(lines[j]);
                if (candidate.isEmpty()) {
                    continue;
                }

                String normalizedCandidate = normalize(candidate);
                if (looksLikeSectionHeading(normalizedCandidate)) {
                    break;
                }

                if (looksLikePrescriptionHeader(normalizedCandidate)) {
                    continue;
                }

                if (candidate.split("\\s+").length >= 4) {
                    return candidate;
                }
            }
        }

        return "";
    }

    private static PrescriptionTableRow parsePrescriptionRowAlternate(String rowText) {
        PrescriptionTableRow row = new PrescriptionTableRow();
        if (rowText == null || rowText.isBlank()) {
            return row;
        }

        String candidate = cleanValue(rowText).replaceAll("\\s+", " ");

        String[] wideColumns = candidate.split("\\s{2,}|\\t+");
        if (wideColumns.length >= 4) {
            row.nomMedicament = cleanValue(wideColumns[0]);
            row.dose = cleanValue(wideColumns[1]);
            row.frequence = cleanValue(wideColumns[2]);
            row.duree = extractLeadingNumber(wideColumns[3]);
            return row;
        }

        String[] tokens = candidate.split("\\s+");
        if (tokens.length >= 4) {
            row.duree = extractLeadingNumber(tokens[tokens.length - 1]);
            row.frequence = cleanValue(tokens[tokens.length - 2]);
            row.dose = cleanValue(tokens[tokens.length - 3]);
            row.nomMedicament = cleanValue(joinTokens(tokens, 0, tokens.length - 3));
        }

        return row;
    }

    private static String extractBetweenSections(String text, String startPatterns, String endPatterns) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String startRegex = buildHeadingRegex(startPatterns);
        String endRegex = buildHeadingRegex(endPatterns);
        Pattern pattern = Pattern.compile("(?is)(?:^|\\n)\\s*" + startRegex + "\\s*[:\\-]?(.*?)(?=\\n\\s*(?:" + endRegex + ")\\s*[:\\-]?|$)");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return "";
        }

        return cleanupExtractedBlock(matcher.group(1));
    }

    private static String extractSectionBlock(String text, String startPatterns) {
        return extractBetweenSections(text,
                startPatterns,
                "diagnostic|observations|resultats|résultats|examens|signature|instructions|prescription|medicament|médicament|motif de consultation");
    }

    private static String extractDurationValue(String text) {
        String duration = extractBetweenSections(text,
                "duration|durée|duree",
                "diagnostic|observations|resultats|résultats|examens|signature|prescription|medicament|médicament|motif de consultation");
        if (!duration.isEmpty()) {
            String compact = duration.replaceAll("\\s+", " ").trim();
            Matcher matcher = Pattern.compile("(?i)\\b(\\d+\\s*(?:min|mins?|minutes?|j|jours?|day|days?))\\b").matcher(compact);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }

            matcher = Pattern.compile("(?i)\\b(\\d+)\\b").matcher(compact);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            return compact;
        }

        Matcher matcher = Pattern.compile("(?im)^\\s*(?:duration|durée|duree)\\s*[:\\-]?\\s*(\\d+\\s*(?:min|mins?|minutes?|j|jours?|day|days?))\\s*$").matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "";
    }

    private static String findPrescriptionRowCandidate(String[] lines) {
        Pattern rowPattern = Pattern.compile(
                "(?i)^\\s*(.+?)\\s+(\\d+\\s*(?:mg|g|ml|mcg|µg|ui|iu))\\s+(.+?)\\s+(\\d+\\s*(?:j|jours?|day|days?))\\s*$");
        for (String line : lines) {
            String candidate = cleanValue(line);
            if (candidate.isEmpty()) {
                continue;
            }
            if (looksLikePrescriptionHeader(normalize(candidate))) {
                continue;
            }
            if (rowPattern.matcher(candidate).matches()) {
                return candidate;
            }
        }

        return "";
    }

    private static PrescriptionTableRow parsePrescriptionRow(String rowText) {
        PrescriptionTableRow row = new PrescriptionTableRow();
        if (rowText == null || rowText.isBlank()) {
            return row;
        }

        String candidate = cleanValue(rowText).replaceAll("\\s+", " ");
        Pattern rowPattern = Pattern.compile(
                "(?i)^\\s*(.+?)\\s+(\\d+\\s*(?:mg|g|ml|mcg|µg|ui|iu))\\s+(.+?)\\s+(\\d+\\s*(?:j|jours?|day|days?))\\s*$");
        Matcher matcher = rowPattern.matcher(candidate);
        if (matcher.find()) {
            row.nomMedicament = cleanValue(matcher.group(1));
            row.dose = cleanValue(matcher.group(2));
            row.frequence = cleanValue(matcher.group(3));
            row.duree = extractLeadingNumber(matcher.group(4));
            return row;
        }

        String[] columns = candidate.split("\\s{2,}|\\t+");
        if (columns.length >= 4) {
            row.nomMedicament = cleanValue(columns[0]);
            row.dose = cleanValue(columns[1]);
            row.frequence = cleanValue(columns[2]);
            row.duree = extractLeadingNumber(columns[3]);
            return row;
        }

        row.nomMedicament = extractFirstValueAfterLabel(candidate, "medicament|médicament|drug|medicine");
        row.dose = extractFirstValueAfterLabel(candidate, "dose");
        row.frequence = extractFirstValueAfterLabel(candidate, "frequence|fréquence|frequency");
        row.duree = extractLeadingNumber(extractDurationValue(candidate));
        return row;
    }

    private static String buildHeadingRegex(String headings) {
        String[] parts = headings.split("\\|");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append("|");
            }
            builder.append("(?:").append(Pattern.quote(normalizeHeadingVariant(parts[i]))).append(")");
        }
        return builder.toString();
    }

    private static String normalizeHeadingVariant(String heading) {
        return normalize(heading)
                .replace('’', '\'')
                .replace('`', '\'')
                .replace("d'examens", "d'examens")
                .trim();
    }

    private static String cleanupExtractedBlock(String block) {
        if (block == null) {
            return "";
        }

        String cleaned = block.replaceAll("^[\\s:.-]+", "");
        cleaned = cleaned.replaceAll("[\\s:.-]+$", "");
        cleaned = cleaned.replaceAll("(?m)^[ \\t]+", "");
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        return cleaned.trim();
    }

    private static String[] normalizeLines(String text) {
        return text.replace("\r", "").split("\n");
    }

    private static String nextContentLine(String[] lines, int startIndex) {
        for (int i = startIndex; i < lines.length; i++) {
            String candidate = lines[i].trim();
            if (!candidate.isEmpty() && !looksLikeHeader(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e')
                .replace('à', 'a')
                .replace('ç', 'c')
                .trim();
    }

    private static String findMatchingPattern(String normalizedLine, String[] patterns) {
        for (String pattern : patterns) {
            String normalizedPattern = normalize(pattern);
            if (!normalizedPattern.isEmpty() && normalizedLine.contains(normalizedPattern)) {
                return pattern;
            }
        }
        return null;
    }

    private static String extractInlineValue(String line, String pattern) {
        int colonIndex = line.indexOf(':');
        if (colonIndex >= 0 && colonIndex < line.length() - 1) {
            return cleanValue(line.substring(colonIndex + 1));
        }

        String normalizedLine = normalize(line);
        String normalizedPattern = normalize(pattern);
        if (normalizedLine.equals(normalizedPattern)) {
            return "";
        }

        return cleanValue(line.replaceFirst("(?i)" + Pattern.quote(pattern), ""));
    }

    private static String extractFirstValueAfterLabel(String text, String patterns) {
        String block = extractSectionBlock(text, patterns);
        if (block.isEmpty()) {
            return "";
        }

        String[] lines = normalizeLines(block);
        for (String line : lines) {
            String value = cleanValue(line);
            if (!value.isEmpty() && !looksLikeHeader(value)) {
                return value;
            }
        }

        return cleanValue(block);
    }

    private static boolean looksLikeTableHeader(String normalizedLine) {
        return normalizedLine.contains("medicament")
                && normalizedLine.contains("dose")
                && normalizedLine.contains("frequence")
                && normalizedLine.contains("duree");
    }

    private static boolean looksLikePrescriptionHeader(String normalizedLine) {
        return normalizedLine.contains("medicament")
                && normalizedLine.contains("dose")
                && normalizedLine.contains("frequence")
                && normalizedLine.contains("duree");
    }

    private static boolean looksLikeSectionHeading(String normalizedLine) {
        return normalizedLine.equals("diagnostic")
                || normalizedLine.equals("observations")
                || normalizedLine.equals("resultats")
                || normalizedLine.equals("resultats des examens")
                || normalizedLine.equals("resultats d'examens")
                || normalizedLine.equals("examens")
                || normalizedLine.equals("instructions")
                || normalizedLine.equals("mode d'emploi")
                || normalizedLine.equals("mode emploi")
                || normalizedLine.equals("prescription")
                || normalizedLine.equals("fiche medicale");
    }

    private static boolean looksLikeHeader(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty()
                || normalized.equals("-")
                || normalized.equals("medicament")
                || normalized.equals("dose")
                || normalized.equals("frequence")
                || normalized.equals("duree")
                || looksLikeTableHeader(normalized);
    }

    private static String cleanValue(String value) {
        return value == null ? "" : value.replace('\u00a0', ' ').trim();
    }

    private static String joinTokens(String[] tokens, int startInclusive, int endExclusive) {
        StringBuilder builder = new StringBuilder();
        for (int i = startInclusive; i < endExclusive; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(tokens[i]);
        }
        return builder.toString();
    }

    private static class PrescriptionTableRow {
        private String nomMedicament = "";
        private String dose = "";
        private String frequence = "";
        private String duree = "";
    }

    private static String extractField(String text, String fieldName, String patterns) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Simple pattern matching: look for lines containing the pattern keywords
        String[] lines = text.split("\n");
        String[] patternArray = patterns.split("\\|");
        
        for (String line : lines) {
            String lowerLine = line.toLowerCase();
            for (String pattern : patternArray) {
                if (lowerLine.contains(pattern.toLowerCase())) {
                    // Extract content after colon or from the next line
                    int colonIndex = line.indexOf(':');
                    if (colonIndex != -1 && colonIndex < line.length() - 1) {
                        return line.substring(colonIndex + 1).trim();
                    }
                    // If no colon, return the whole line minus the pattern
                    return line.replaceAll("(?i)" + pattern, "").trim();
                }
            }
        }
        
        return "";
    }
}
