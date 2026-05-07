package tn.esprit.services;

import tn.esprit.entities.AdminActionLog;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminActionLogService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Path LOG_PATH = Paths.get(System.getProperty("user.home"), ".medflow", "admin_action_log.csv");

    public AdminActionLogService() {
        ensureLogFile();
    }

    public synchronized void logAction(String adminName, String action, String targetType, int targetId, String details) {
        ensureLogFile();
        String safeAdmin = sanitize(adminName);
        String safeAction = sanitize(action);
        String safeTargetType = sanitize(targetType);
        String safeDetails = sanitize(details);

        String line = String.join(",",
                encodeCsv(LocalDateTime.now().format(FORMATTER)),
                encodeCsv(safeAdmin),
                encodeCsv(safeAction),
                encodeCsv(safeTargetType),
                encodeCsv(String.valueOf(targetId)),
                encodeCsv(safeDetails)
        );

        try (BufferedWriter writer = Files.newBufferedWriter(
                LOG_PATH,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[AdminActionLogService] Echec ecriture log: " + e.getMessage());
        }
    }

    public synchronized List<AdminActionLog> readRecent(int limit) {
        ensureLogFile();
        List<AdminActionLog> logs = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(LOG_PATH, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                List<String> parts = decodeCsvLine(line);
                if (parts.size() < 6) {
                    continue;
                }

                AdminActionLog log = new AdminActionLog();
                try {
                    log.setTimestamp(LocalDateTime.parse(parts.get(0), FORMATTER));
                } catch (Exception ignored) {
                    log.setTimestamp(LocalDateTime.now());
                }
                log.setAdminName(parts.get(1));
                log.setAction(parts.get(2));
                log.setTargetType(parts.get(3));
                try {
                    log.setTargetId(Integer.parseInt(parts.get(4)));
                } catch (NumberFormatException ignored) {
                    log.setTargetId(0);
                }
                log.setDetails(parts.get(5));
                logs.add(log);
            }
        } catch (IOException e) {
            System.err.println("[AdminActionLogService] Echec lecture logs: " + e.getMessage());
        }

        Collections.reverse(logs);
        if (limit > 0 && logs.size() > limit) {
            return new ArrayList<>(logs.subList(0, limit));
        }
        return logs;
    }

    private void ensureLogFile() {
        try {
            Path parent = LOG_PATH.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(LOG_PATH)) {
                Files.createFile(LOG_PATH);
            }
        } catch (IOException e) {
            System.err.println("[AdminActionLogService] Impossible de preparer le fichier de log: " + e.getMessage());
        }
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private String encodeCsv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private List<String> decodeCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());

        return values;
    }
}

