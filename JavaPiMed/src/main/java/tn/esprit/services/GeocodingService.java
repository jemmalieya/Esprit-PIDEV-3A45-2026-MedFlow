package tn.esprit.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeocodingService {

    public static class GeoPoint {
        private final double lat;
        private final double lng;

        public GeoPoint(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }
    }

    public GeoPoint geocoderAdresse(String adresse) throws Exception {
        if (adresse == null || adresse.isBlank()) {
            throw new IllegalArgumentException("Adresse vide.");
        }

        List<String> variantes = construireVariantes(adresse);

        for (String variante : variantes) {
            try {
                GeoPoint point = geocoderUneAdresse(variante);
                if (point != null) {
                    System.out.println("Adresse géocodée avec succès : " + variante);
                    return point;
                }
            } catch (Exception e) {
                System.out.println("Échec géocodage pour : " + variante + " -> " + e.getMessage());
            }
        }

        throw new IllegalStateException("Adresse non géocodée : " + adresse);
    }

    private List<String> construireVariantes(String adresse) {
        List<String> variantes = new ArrayList<>();

        String clean = adresse.trim();

        variantes.add(clean);

        if (!clean.toLowerCase().contains("tunisia")) {
            variantes.add(clean + ", Tunisia");
        }

        if (clean.equalsIgnoreCase("Esprit School of Engineering, Ariana, Tunisia")) {
            variantes.add("Esprit Ariana, Tunisia");
            variantes.add("ESPRIT Ariana, Tunisia");
            variantes.add("Ariana, Tunisia");
        }

        if (clean.equalsIgnoreCase("Mourouj 3, Ben Arous")) {
            variantes.add("Mourouj 3, Ben Arous, Tunisia");
            variantes.add("Ben Arous, Tunisia");
        }

        return variantes;
    }

    private GeoPoint geocoderUneAdresse(String adresse) throws Exception {
        String encoded = URLEncoder.encode(adresse, StandardCharsets.UTF_8);
        String url = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=jsonv2&limit=1";

        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "MedFlow-JavaFX/1.0");
        conn.setRequestProperty("Accept-Language", "fr");

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IllegalStateException("HTTP " + code);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String json = sb.toString();

            if (json == null || json.isBlank() || json.equals("[]")) {
                return null;
            }

            Pattern latPattern = Pattern.compile("\"lat\"\\s*:\\s*\"([^\"]+)\"");
            Pattern lonPattern = Pattern.compile("\"lon\"\\s*:\\s*\"([^\"]+)\"");

            Matcher latMatcher = latPattern.matcher(json);
            Matcher lonMatcher = lonPattern.matcher(json);

            if (latMatcher.find() && lonMatcher.find()) {
                double lat = Double.parseDouble(latMatcher.group(1));
                double lng = Double.parseDouble(lonMatcher.group(1));
                return new GeoPoint(lat, lng);
            }

            return null;
        }
    }
}