package tn.esprit.services;

import tn.esprit.entities.Evenement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventLocationService {

    private static final String DEFAULT_COUNTRY = "Tunisia";
    private static final String EVENT_LOCATIONIQ_API_KEY = System.getenv("EVENT_LOCATIONIQ_API_KEY");
    private static final String EVENT_OPENWEATHER_API_KEY = System.getenv("EVENT_OPENWEATHER_API_KEY");
    private static final Pattern LAT_PATTERN = Pattern.compile("\"lat\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern LON_PATTERN = Pattern.compile("\"lon\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern OPENWEATHER_DESCRIPTION_PATTERN = Pattern.compile("\"description\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern OPENWEATHER_TEMP_PATTERN = Pattern.compile("\"temp\"\\s*:\\s*([-0-9.]+)");
    private static final Pattern OPENWEATHER_HUMIDITY_PATTERN = Pattern.compile("\"humidity\"\\s*:\\s*([-0-9.]+)");
    private static final Pattern OPENWEATHER_WIND_PATTERN = Pattern.compile("\"speed\"\\s*:\\s*([-0-9.]+)");
    private static final Pattern TEMP_PATTERN = Pattern.compile("\"temperature_2m\"\\s*:\\s*([-0-9.]+)");
    private static final Pattern HUMIDITY_PATTERN = Pattern.compile("\"relative_humidity_2m\"\\s*:\\s*([-0-9.]+)");
    private static final Pattern WIND_PATTERN = Pattern.compile("\"wind_speed_10m\"\\s*:\\s*([-0-9.]+)");
    private static final Pattern CODE_PATTERN = Pattern.compile("\"weather_code\"\\s*:\\s*([-0-9.]+)");
    private final Map<String, EventLocation> locationCache = new HashMap<>();

    public EventLocation resolve(Evenement evenement) throws IOException {
        String query = buildQuery(evenement);

        try {
            EventLocation locationIqLocation = resolveWithLocationIq(query);
            if (locationIqLocation != null) {
                return locationIqLocation;
            }
        } catch (IOException ignored) {
            // Continue with the no-key/fallback providers if LocationIQ is unavailable or misconfigured.
        }

        try {
            String geocodeUrl = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + encode(query);
            String response = readUrl(geocodeUrl);

            String lat = extract(LAT_PATTERN, response);
            String lon = extract(LON_PATTERN, response);
            if (!isBlank(lat) && !isBlank(lon)) {
                return new EventLocation(query, lat, lon);
            }
        } catch (IOException ignored) {
            // The city fallback below keeps the detail page usable when the remote geocoder fails.
        }

        EventLocation fallback = fallbackForCity(evenement, query);
        if (fallback != null) {
            return fallback;
        }

        throw new IOException("Adresse introuvable: " + query);
    }

    public WeatherInfo getWeather(EventLocation location) throws IOException {
        if (!isBlank(EVENT_OPENWEATHER_API_KEY)) {
            try {
                return getOpenWeather(location);
            } catch (IOException ignored) {
                // Keep the weather block usable if the keyed provider is temporarily unavailable.
            }
        }

        String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + encode(location.latitude())
                + "&longitude=" + encode(location.longitude())
                + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code"
                + "&timezone=auto";
        String response = readUrl(weatherUrl);

        String temperature = extract(TEMP_PATTERN, response);
        String humidity = extract(HUMIDITY_PATTERN, response);
        String wind = extract(WIND_PATTERN, response);
        String code = extract(CODE_PATTERN, response);

        return new WeatherInfo(
                conditionLabel(code),
                formatNumber(temperature, " C"),
                formatNumber(humidity, "%"),
                formatNumber(wind, " km/h")
        );
    }

    private WeatherInfo getOpenWeather(EventLocation location) throws IOException {
        String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" + encode(location.latitude())
                + "&lon=" + encode(location.longitude())
                + "&units=metric"
                + "&lang=fr"
                + "&appid=" + encode(EVENT_OPENWEATHER_API_KEY);
        String response = readUrl(weatherUrl);

        String condition = extract(OPENWEATHER_DESCRIPTION_PATTERN, response);
        String temperature = extract(OPENWEATHER_TEMP_PATTERN, response);
        String humidity = extract(OPENWEATHER_HUMIDITY_PATTERN, response);
        String windMs = extract(OPENWEATHER_WIND_PATTERN, response);

        String wind = "-";
        if (!isBlank(windMs)) {
            try {
                wind = String.format(Locale.US, "%.0f km/h", Double.parseDouble(windMs) * 3.6);
            } catch (NumberFormatException e) {
                wind = windMs + " m/s";
            }
        }

        return new WeatherInfo(
                isBlank(condition) ? "Meteo disponible" : capitalize(condition),
                formatNumber(temperature, " C"),
                formatNumber(humidity, "%"),
                wind
        );
    }

    public String buildStaticMapUrl(EventLocation location) {
        return buildStaticMapUrl(location, 15);
    }

    public String buildStaticMapUrl(EventLocation location, int zoom) {
        int safeZoom = Math.max(3, Math.min(18, zoom));
        if (!isBlank(EVENT_LOCATIONIQ_API_KEY)) {
            String marker = "icon:large-blue-cutout|" + location.latitude() + "," + location.longitude();
            return "https://maps.locationiq.com/v3/staticmap?key=" + encode(EVENT_LOCATIONIQ_API_KEY)
                    + "&center=" + encode(location.latitude() + "," + location.longitude())
                    + "&zoom=" + safeZoom + "&size=1100x560&format=jpg&maptype=streets"
                    + "&markers=" + encode(marker);
        }

        String marker = location.latitude() + "," + location.longitude() + ",red-pushpin";
        return "https://staticmap.openstreetmap.de/staticmap.php?center="
                + encode(location.latitude() + "," + location.longitude())
                + "&zoom=" + safeZoom + "&size=1100x560&markers=" + encode(marker);
    }

    public String buildInteractiveMapUrl(EventLocation location) {
        return "https://www.openstreetmap.org/?mlat=" + encode(location.latitude())
                + "&mlon=" + encode(location.longitude())
                + "#map=16/" + encode(location.latitude()) + "/" + encode(location.longitude());
    }

    public String buildDirectionsUrl(EventLocation location) {
        return buildDirectionsUrl("", location);
    }

    public String buildDirectionsUrl(String origin, EventLocation location) {
        String base = "https://www.google.com/maps/dir/?api=1&destination="
                + encode(location.latitude() + "," + location.longitude())
                + "&travelmode=driving";
        if (!isBlank(origin)) {
            base += "&origin=" + encode(origin.trim());
        }
        return base;
    }

    public String buildRouteStaticMapUrl(String origin, EventLocation destination) throws IOException {
        if (isBlank(EVENT_LOCATIONIQ_API_KEY)) {
            throw new IOException("Cle LocationIQ manquante.");
        }

        EventLocation start = resolveForRouteOrigin(origin);
        if (start == null) {
            throw new IOException("Adresse de depart introuvable.");
        }

        return buildRouteStaticMapUrl(start, destination);
    }

    public String buildRouteStaticMapUrl(EventLocation start, EventLocation destination) throws IOException {
        if (isBlank(EVENT_LOCATIONIQ_API_KEY)) {
            throw new IOException("Cle LocationIQ manquante.");
        }

        String coordinates = start.longitude() + "," + start.latitude()
                + ";" + destination.longitude() + "," + destination.latitude();
        String routeUrl = "https://us1.locationiq.com/v1/directions/driving/" + coordinates
                + "?key=" + encode(EVENT_LOCATIONIQ_API_KEY)
                + "&overview=full&geometries=polyline&steps=false";
        String response = readUrl(routeUrl);
        String geometry = jsonUnescape(extractJsonString(response, "\"geometry\""));
        if (isBlank(geometry)) {
            throw new IOException("Trajet introuvable entre ces deux points.");
        }

        double centerLat = average(start.latitude(), destination.latitude());
        double centerLon = average(start.longitude(), destination.longitude());
        String path = "color:0x0fa3bf|weight:7|enc:" + geometry;

        return "https://maps.locationiq.com/v3/staticmap?key=" + encode(EVENT_LOCATIONIQ_API_KEY)
                + "&center=" + encode(centerLat + "," + centerLon)
                + "&zoom=12&size=1100x560&format=jpg&maptype=streets"
                + "&markers=" + encode("icon:large-red-cutout|" + start.latitude() + "," + start.longitude())
                + "&markers=" + encode("icon:large-blue-cutout|" + destination.latitude() + "," + destination.longitude())
                + "&path=" + encode(path)
                + "&linecap=round&linejoin=round";
    }

    public String buildDirectLineStaticMapUrl(String origin, EventLocation destination) throws IOException {
        if (isBlank(EVENT_LOCATIONIQ_API_KEY)) {
            throw new IOException("Cle LocationIQ manquante.");
        }

        EventLocation start = resolveForRouteOrigin(origin);
        if (start == null) {
            throw new IOException("Adresse de depart introuvable.");
        }

        return buildDirectLineStaticMapUrl(start, destination);
    }

    public String buildDirectLineStaticMapUrl(EventLocation start, EventLocation destination) {
        double centerLat = average(start.latitude(), destination.latitude());
        double centerLon = average(start.longitude(), destination.longitude());
        String path = "weight:6|color:blue|" + start.latitude() + "," + start.longitude()
                + "|" + destination.latitude() + "," + destination.longitude();

        return "https://maps.locationiq.com/v3/staticmap?key=" + encode(EVENT_LOCATIONIQ_API_KEY)
                + "&center=" + encode(centerLat + "," + centerLon)
                + "&zoom=11&size=1100x560&format=jpg&maptype=streets"
                + "&markers=" + encode("icon:large-red-cutout|" + start.latitude() + "," + start.longitude())
                + "&markers=" + encode("icon:large-blue-cutout|" + destination.latitude() + "," + destination.longitude())
                + "&path=" + encode(path);
    }

    public EventLocation resolveForRouteOrigin(String origin) throws IOException {
        if (isBlank(origin)) {
            throw new IOException("Adresse de depart vide.");
        }

        String key = normalize(origin);
        EventLocation cached = locationCache.get(key);
        if (cached != null) {
            return cached;
        }

        EventLocation fallback = fallbackForCityName(origin, origin);
        if (fallback != null) {
            locationCache.put(key, fallback);
            return fallback;
        }

        EventLocation location = resolveWithLocationIq(origin);
        if (location == null) {
            throw new IOException("Adresse de depart introuvable.");
        }

        locationCache.put(key, location);
        return location;
    }

    private String buildQuery(Evenement evenement) {
        StringBuilder query = new StringBuilder();
        appendPart(query, evenement.getNom_lieu_event());
        appendPart(query, evenement.getAdresse_event());
        appendPart(query, evenement.getVille_event());

        if (query.length() == 0) {
            appendPart(query, evenement.getTitre_event());
        }
        appendPart(query, DEFAULT_COUNTRY);
        return query.toString();
    }

    private EventLocation resolveWithLocationIq(String query) throws IOException {
        if (isBlank(EVENT_LOCATIONIQ_API_KEY)) {
            return null;
        }

        String geocodeUrl = "https://us1.locationiq.com/v1/search?key=" + encode(EVENT_LOCATIONIQ_API_KEY)
                + "&q=" + encode(query)
                + "&countrycodes=tn"
                + "&limit=1"
                + "&format=json";
        String response = readUrl(geocodeUrl);

        String lat = extract(LAT_PATTERN, response);
        String lon = extract(LON_PATTERN, response);
        if (isBlank(lat) || isBlank(lon)) {
            return null;
        }

        return new EventLocation(query, lat, lon);
    }

    private EventLocation fallbackForCity(Evenement evenement, String query) {
        return fallbackForCityName(evenement.getVille_event(), query);
    }

    private EventLocation fallbackForCityName(String cityName, String query) {
        String city = normalize(cityName);
        return switch (city) {
            case "tunis" -> new EventLocation(query, "36.8065", "10.1815");
            case "ariana" -> new EventLocation(query, "36.8625", "10.1956");
            case "ben arous" -> new EventLocation(query, "36.7531", "10.2189");
            case "manouba" -> new EventLocation(query, "36.8080", "10.0972");
            case "nabeul" -> new EventLocation(query, "36.4513", "10.7350");
            case "bizerte" -> new EventLocation(query, "37.2744", "9.8739");
            case "sousse" -> new EventLocation(query, "35.8245", "10.6346");
            case "monastir" -> new EventLocation(query, "35.7643", "10.8113");
            case "mahdia" -> new EventLocation(query, "35.5047", "11.0622");
            case "sfax" -> new EventLocation(query, "34.7406", "10.7603");
            case "kairouan" -> new EventLocation(query, "35.6781", "10.0963");
            case "gabes" -> new EventLocation(query, "33.8881", "10.0975");
            case "gafsa" -> new EventLocation(query, "34.4311", "8.7757");
            case "tozeur" -> new EventLocation(query, "33.9197", "8.1335");
            case "medenine" -> new EventLocation(query, "33.3549", "10.5055");
            default -> null;
        };
    }

    private void appendPart(StringBuilder builder, String value) {
        if (isBlank(value)) return;
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value.trim());
    }

    private String readUrl(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);
        connection.setRequestProperty("User-Agent", "MedFlow-JavaPiMed/1.0");
        connection.setRequestProperty("Accept", "application/json");

        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("Service distant indisponible: HTTP " + status);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }

    private String extract(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractJsonString(String text, String fieldName) {
        if (text == null || fieldName == null) {
            return "";
        }

        int fieldIndex = text.indexOf(fieldName);
        if (fieldIndex < 0) {
            return "";
        }

        int colonIndex = text.indexOf(':', fieldIndex + fieldName.length());
        if (colonIndex < 0) {
            return "";
        }

        int quoteStart = text.indexOf('"', colonIndex + 1);
        if (quoteStart < 0) {
            return "";
        }

        StringBuilder value = new StringBuilder();
        boolean escaping = false;
        for (int i = quoteStart + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaping) {
                value.append('\\').append(c);
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                return value.toString();
            }
            value.append(c);
        }

        return "";
    }

    private String formatNumber(String value, String suffix) {
        if (isBlank(value)) {
            return "-";
        }
        try {
            double number = Double.parseDouble(value);
            return String.format(Locale.US, "%.0f%s", number, suffix);
        } catch (NumberFormatException e) {
            return value + suffix;
        }
    }

    private String conditionLabel(String codeValue) {
        if (isBlank(codeValue)) return "Meteo disponible";

        int code;
        try {
            code = (int) Math.round(Double.parseDouble(codeValue));
        } catch (NumberFormatException e) {
            return "Meteo disponible";
        }

        if (code == 0) return "Ciel clair";
        if (code == 1 || code == 2 || code == 3) return "Partiellement nuageux";
        if (code == 45 || code == 48) return "Brouillard";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "Pluie";
        if (code >= 71 && code <= 77) return "Neige";
        if (code >= 95) return "Orage";
        return "Meteo disponible";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String jsonUnescape(String value) {
        if (value == null) return "";
        return value.replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\/", "/");
    }

    private double average(String first, String second) {
        try {
            return (Double.parseDouble(first) + Double.parseDouble(second)) / 2.0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String capitalize(String value) {
        if (isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT) + trimmed.substring(1);
    }

    public record EventLocation(String query, String latitude, String longitude) {
    }

    public record WeatherInfo(String condition, String temperature, String humidity, String wind) {
    }
}
