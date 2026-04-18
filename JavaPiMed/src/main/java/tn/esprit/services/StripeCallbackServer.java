package tn.esprit.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class StripeCallbackServer {

    private HttpServer server;

    public void start(BiConsumer<String, Map<String, String>> callback) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 4242), 0);

        server.createContext("/stripe/success", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI());
            sendHtml(exchange, "<h2>Paiement reçu. Vous pouvez revenir à l'application.</h2>");
            Platform.runLater(() -> callback.accept("success", params));
        });

        server.createContext("/stripe/cancel", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI());
            sendHtml(exchange, "<h2>Paiement annulé.</h2>");
            Platform.runLater(() -> callback.accept("cancel", params));
        });

        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> map = new HashMap<>();
        String query = uri.getQuery();
        if (query == null || query.isBlank()) return map;

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                map.put(parts[0], parts[1]);
            }
        }
        return map;
    }

    private void sendHtml(HttpExchange exchange, String html) throws IOException {
        byte[] response = ("<html><body>" + html + "</body></html>").getBytes();
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}