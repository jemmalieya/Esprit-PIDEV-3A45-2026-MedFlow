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
import java.util.Objects;
import java.util.function.BiConsumer;

public class StripeCallbackServer {

    private static final Object LOCK = new Object();
    private static HttpServer server;
    private static volatile BiConsumer<String, Map<String, String>> callbackHandler;
    private static final int DEFAULT_PORT = 4242;

    public void start(BiConsumer<String, Map<String, String>> callback) throws IOException {
        Objects.requireNonNull(callback, "callback");

        synchronized (LOCK) {
            callbackHandler = callback;
            if (server != null) {
                if (server.getAddress() != null) {
                    return;
                }
                server = null;
            }

            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", DEFAULT_PORT), 0);
            } catch (IOException ex) {
                throw new IOException("Impossible de démarrer le serveur callback Stripe sur le port " + DEFAULT_PORT
                        + ". Il est probablement déjà utilisé par une autre instance de l'application.", ex);
            }

            server.createContext("/stripe/success", exchange -> {
                Map<String, String> params = parseQuery(exchange.getRequestURI());
                sendHtml(exchange, "<h2>Merci pour votre achat !</h2><p>Redirection vers le détail de votre commande...</p>");
                BiConsumer<String, Map<String, String>> handler = callbackHandler;
                if (handler != null) {
                    Platform.runLater(() -> handler.accept("success", params));
                }
            });

            server.createContext("/stripe/cancel", exchange -> {
                Map<String, String> params = parseQuery(exchange.getRequestURI());
                sendHtml(exchange, "<h2>Paiement annulé.</h2>");
                BiConsumer<String, Map<String, String>> handler = callbackHandler;
                if (handler != null) {
                    Platform.runLater(() -> handler.accept("cancel", params));
                }
            });

            server.start();
        }
    }

    public void stop() {
        synchronized (LOCK) {
            if (server != null) {
                server.stop(0);
                server = null;
            }
            callbackHandler = null;
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