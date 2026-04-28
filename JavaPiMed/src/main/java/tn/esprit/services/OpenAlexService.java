package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.entities.OpenAlexReference;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class OpenAlexService {

    private final HttpClient httpClient;

    public OpenAlexService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public List<OpenAlexReference> searchReferences(String query, int limit) throws IOException, InterruptedException {
        if (query == null || query.trim().isBlank()) {
            return new ArrayList<>();
        }

        String cleanQuery = prepareQuery(query);
        String encodedQuery = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8);

        String apiKey = System.getenv("OPENALEX_API_KEY");
        System.out.println("OPENALEX_API_KEY existe ? " + (apiKey != null && !apiKey.isBlank()));

        StringBuilder url = new StringBuilder();
        url.append("https://api.openalex.org/works");
        url.append("?search=").append(encodedQuery);
        url.append("&per-page=").append(limit);
        url.append("&select=id,doi,display_name,publication_year,cited_by_count,authorships,primary_location");

        if (apiKey != null && !apiKey.isBlank()) {
            url.append("&api_key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(25))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur OpenAlex : HTTP " + response.statusCode());
        }

        List<OpenAlexReference> references = parseReferences(response.body());

        if (references.isEmpty()) {
            System.out.println("Aucun résultat avec la requête principale. Nouvelle tentative avec mental health.");
            return searchReferencesFallback("mental health anxiety stress", limit);
        }

        return references;
    }
    private List<OpenAlexReference> searchReferencesFallback(String query, int limit)
            throws IOException, InterruptedException {

        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String apiKey = System.getenv("OPENALEX_API_KEY");

        StringBuilder url = new StringBuilder();
        url.append("https://api.openalex.org/works");
        url.append("?search=").append(encodedQuery);
        url.append("&per-page=").append(limit);
        url.append("&select=id,doi,display_name,publication_year,cited_by_count,authorships,primary_location");

        if (apiKey != null && !apiKey.isBlank()) {
            url.append("&api_key=").append(URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(25))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur OpenAlex fallback : HTTP " + response.statusCode());
        }

        return parseReferences(response.body());
    }

    private String prepareQuery(String text) {
        String result = text
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String englishQuery = buildEnglishScientificQuery(result);

        if (!englishQuery.isBlank()) {
            System.out.println("Requête OpenAlex envoyée : " + englishQuery);
            return englishQuery;
        }

        if (result.length() > 220) {
            result = result.substring(0, 220);
        }

        System.out.println("Requête OpenAlex envoyée : " + result);
        return result;
    }
    private String buildEnglishScientificQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }

        String lower = query.toLowerCase();
        StringBuilder keywords = new StringBuilder();

        if (lower.contains("anxiété") || lower.contains("anxiete")) {
            keywords.append(" anxiety");
        }

        if (lower.contains("stress")) {
            keywords.append(" stress psychological stress");
        }

        if (lower.contains("peur")) {
            keywords.append(" fear");
        }

        if (lower.contains("sombre") || lower.contains("obscur") || lower.contains("noir")) {
            keywords.append(" darkness dark environment");
        }

        if (lower.contains("inconnu") || lower.contains("inconnus")) {
            keywords.append(" uncertainty unfamiliar environment");
        }

        if (lower.contains("santé mentale") || lower.contains("sante mentale")) {
            keywords.append(" mental health");
        }

        if (lower.contains("sommeil")) {
            keywords.append(" sleep quality");
        }

        if (lower.contains("dépression") || lower.contains("depression")) {
            keywords.append(" depression mental health");
        }

        if (lower.contains("diabète") || lower.contains("diabete")) {
            keywords.append(" diabetes");
        }

        if (lower.contains("glycémie") || lower.contains("glycemie")) {
            keywords.append(" blood glucose glycemic control");
        }

        if (lower.contains("alimentation") || lower.contains("nutrition")) {
            keywords.append(" nutrition diet dietary habits");
        }

        if (lower.contains("obésité") || lower.contains("obesite")) {
            keywords.append(" obesity overweight");
        }

        if (lower.contains("hypertension") || lower.contains("tension")) {
            keywords.append(" hypertension blood pressure");
        }

        if (lower.contains("coeur") || lower.contains("cardiaque")) {
            keywords.append(" cardiovascular disease heart");
        }

        if (lower.contains("asthme")) {
            keywords.append(" asthma respiratory disease");
        }

        if (lower.contains("cancer")) {
            keywords.append(" cancer screening prevention");
        }

        if (lower.contains("sein")) {
            keywords.append(" breast cancer");
        }

        if (lower.contains("prévention") || lower.contains("prevention")) {
            keywords.append(" prevention public health");
        }

        if (lower.contains("douleur")) {
            keywords.append(" pain symptoms");
        }

        if (lower.contains("fièvre") || lower.contains("fievre")) {
            keywords.append(" fever symptoms");
        }

        if (lower.contains("fatigue")) {
            keywords.append(" fatigue");
        }

        if (lower.contains("sport") || lower.contains("activité physique") || lower.contains("activite physique")) {
            keywords.append(" physical activity exercise");
        }

        if (lower.contains("enfant") || lower.contains("enfants")) {
            keywords.append(" children pediatric");
        }

        if (keywords.toString().isBlank()) {
            return "";
        }

        return keywords.toString().trim();
    }

    private List<OpenAlexReference> parseReferences(String json) {
        List<OpenAlexReference> references = new ArrayList<>();

        JSONObject root = new JSONObject(json);
        JSONArray results = root.optJSONArray("results");

        if (results == null) {
            return references;
        }

        for (int i = 0; i < results.length(); i++) {
            JSONObject item = results.getJSONObject(i);

            String title = item.optString("display_name", "Sans titre");
            int year = item.optInt("publication_year", 0);
            int citationCount = item.optInt("cited_by_count", 0);

            String doi = item.optString("doi", "");
            String openAlexUrl = item.optString("id", "");

            String url = !doi.isBlank() ? doi : openAlexUrl;

            String source = extractSource(item);
            String authors = extractAuthors(item);

            references.add(new OpenAlexReference(
                    title,
                    year,
                    source,
                    authors,
                    doi,
                    url,
                    citationCount
            ));
        }

        return references;
    }

    private String extractSource(JSONObject item) {
        try {
            JSONObject primaryLocation = item.optJSONObject("primary_location");

            if (primaryLocation == null) {
                return "Source inconnue";
            }

            JSONObject source = primaryLocation.optJSONObject("source");

            if (source == null) {
                return "Source inconnue";
            }

            return source.optString("display_name", "Source inconnue");

        } catch (Exception e) {
            return "Source inconnue";
        }
    }

    private String extractAuthors(JSONObject item) {
        try {
            JSONArray authorships = item.optJSONArray("authorships");

            if (authorships == null || authorships.length() == 0) {
                return "Auteurs inconnus";
            }

            List<String> names = new ArrayList<>();

            int max = Math.min(authorships.length(), 3);

            for (int i = 0; i < max; i++) {
                JSONObject authorship = authorships.getJSONObject(i);
                JSONObject author = authorship.optJSONObject("author");

                if (author != null) {
                    String name = author.optString("display_name", "");

                    if (!name.isBlank()) {
                        names.add(name);
                    }
                }
            }

            if (names.isEmpty()) {
                return "Auteurs inconnus";
            }

            if (authorships.length() > 3) {
                return String.join(", ", names) + " et al.";
            }

            return String.join(", ", names);

        } catch (Exception e) {
            return "Auteurs inconnus";
        }
    }
    private String enrichFrenchMedicalQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }

        String lower = query.toLowerCase();
        StringBuilder englishKeywords = new StringBuilder();

        // Santé mentale
        if (lower.contains("anxiété") || lower.contains("anxiete")) {
            englishKeywords.append(" anxiety mental health");
        }

        if (lower.contains("stress")) {
            englishKeywords.append(" stress psychological stress");
        }

        if (lower.contains("peur")) {
            englishKeywords.append(" fear");
        }

        if (lower.contains("sombre") || lower.contains("obscur") || lower.contains("noir")) {
            englishKeywords.append(" dark environments darkness");
        }

        if (lower.contains("inconnu") || lower.contains("inconnus")) {
            englishKeywords.append(" unknown environments uncertainty");
        }

        if (lower.contains("sommeil")) {
            englishKeywords.append(" sleep sleep quality");
        }

        if (lower.contains("dépression") || lower.contains("depression")) {
            englishKeywords.append(" depression mental health");
        }

        // Diabète / nutrition
        if (lower.contains("diabète") || lower.contains("diabete")) {
            englishKeywords.append(" diabetes glycemic control blood glucose");
        }

        if (lower.contains("glycémie") || lower.contains("glycemie")) {
            englishKeywords.append(" blood glucose glycemic control");
        }

        if (lower.contains("alimentation") || lower.contains("nutrition")) {
            englishKeywords.append(" nutrition diet dietary habits");
        }

        if (lower.contains("obésité") || lower.contains("obesite")) {
            englishKeywords.append(" obesity overweight prevention");
        }

        // Cardiologie
        if (lower.contains("hypertension") || lower.contains("tension")) {
            englishKeywords.append(" hypertension blood pressure");
        }

        if (lower.contains("coeur") || lower.contains("cardiaque")) {
            englishKeywords.append(" heart cardiovascular disease");
        }

        // Respiration
        if (lower.contains("asthme")) {
            englishKeywords.append(" asthma respiratory disease");
        }

        if (lower.contains("respiration") || lower.contains("respiratoire")) {
            englishKeywords.append(" respiratory health breathing");
        }

        // Cancer / prévention
        if (lower.contains("cancer")) {
            englishKeywords.append(" cancer screening prevention");
        }

        if (lower.contains("sein")) {
            englishKeywords.append(" breast cancer");
        }

        if (lower.contains("prévention") || lower.contains("prevention")) {
            englishKeywords.append(" prevention public health");
        }

        // Douleur / symptômes
        if (lower.contains("douleur")) {
            englishKeywords.append(" pain symptoms");
        }

        if (lower.contains("fièvre") || lower.contains("fievre")) {
            englishKeywords.append(" fever symptoms");
        }

        if (lower.contains("fatigue")) {
            englishKeywords.append(" fatigue tiredness");
        }

        // Activité physique
        if (lower.contains("sport") || lower.contains("activité physique") || lower.contains("activite physique")) {
            englishKeywords.append(" physical activity exercise");
        }

        // Enfants / personnes âgées
        if (lower.contains("enfant") || lower.contains("enfants")) {
            englishKeywords.append(" children pediatric");
        }

        if (lower.contains("personnes âgées") || lower.contains("personnes agees") || lower.contains("senior")) {
            englishKeywords.append(" elderly older adults");
        }

        if (englishKeywords.toString().isBlank()) {
            return query;
        }

        return query + " " + englishKeywords;
    }
}