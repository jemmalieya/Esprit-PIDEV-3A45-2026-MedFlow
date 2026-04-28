package tn.esprit.services;
import tn.esprit.entities.Evenement;
import tn.esprit.services.ParticipationDemandeService.ParticipationDemande;
import tn.esprit.tools.MyDataBase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventNotificationService {
    public static final String TYPE_EVENT_ONLINE = "EVENT_ONLINE";

    private final Connection cnx;

    public EventNotificationService() {
        this.cnx = MyDataBase.getInstance().getCnx();
        ensureNotificationTable();
    }

    private void ensureNotificationTable() {
        if (cnx == null) return;

        try {
            DatabaseMetaData metaData = cnx.getMetaData();
            try (ResultSet tables = metaData.getTables(cnx.getCatalog(), null, "event_notification", null)) {
                if (tables.next()) {
                    return;
                }
            }

            try (Statement statement = cnx.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE event_notification (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            user_id INT NOT NULL,
                            event_id INT NOT NULL,
                            title VARCHAR(255) NOT NULL,
                            message TEXT NOT NULL,
                            notification_type VARCHAR(80) NOT NULL,
                            is_read BOOLEAN NOT NULL DEFAULT FALSE,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
            }
        } catch (SQLException ex) {
            System.out.println("Impossible de verifier/creer event_notification: " + ex.getMessage());
        }
    }

    public boolean isOnlineStatus(String status) {
        return safe(status).toLowerCase(Locale.ROOT).contains("ligne");
    }

    public boolean shouldNotifyOnline(String previousStatus, String currentStatus) {
        return !isOnlineStatus(previousStatus) && isOnlineStatus(currentStatus);
    }

    public int createOnlineNotifications(Evenement evenement, List<ParticipationDemande> demandes) {
        if (evenement == null || demandes == null || demandes.isEmpty()) {
            return 0;
        }

        int created = 0;
        for (ParticipationDemande demande : demandes) {
            if (demande == null
                    || demande.getUserId() <= 0
                    || !ParticipationDemande.STATUS_ACCEPTED.equals(safe(demande.getStatus()))) {
                continue;
            }

            if (hasExistingNotification(demande.getUserId(), evenement.getId(), TYPE_EVENT_ONLINE)) {
                continue;
            }

            String title = "Evenement en ligne";
            String message = buildOnlineMessage(evenement);
            if (insertNotification(demande.getUserId(), evenement.getId(), title, message, TYPE_EVENT_ONLINE)) {
                created++;
            }
        }
        return created;
    }

    public List<NotificationItem> getNotificationsForUser(int userId) {
        List<NotificationItem> notifications = new ArrayList<>();
        if (userId <= 0) return notifications;

        String sql = """
                SELECT id, user_id, event_id, title, message, notification_type, is_read, created_at
                FROM event_notification
                WHERE user_id = ?
                ORDER BY created_at DESC, id DESC
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    notifications.add(new NotificationItem(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getInt("event_id"),
                            rs.getString("title"),
                            rs.getString("message"),
                            rs.getString("notification_type"),
                            rs.getBoolean("is_read"),
                            createdAt == null ? null : createdAt.toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException ex) {
            System.out.println("Impossible de recuperer les notifications evenement: " + ex.getMessage());
        }

        return notifications;
    }

    public int countUnreadForUser(int userId) {
        if (userId <= 0) return 0;
        String sql = "SELECT COUNT(*) FROM event_notification WHERE user_id = ? AND is_read = FALSE";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            System.out.println("Impossible de compter les notifications non lues: " + ex.getMessage());
        }

        return 0;
    }

    public void markAllAsRead(int userId) {
        if (userId <= 0) return;

        String sql = "UPDATE event_notification SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("Impossible de marquer les notifications comme lues: " + ex.getMessage());
        }
    }

    private String buildOnlineMessage(Evenement evenement) {
        String title = safe(evenement.getTitre_event()).isBlank() ? "Votre evenement" : evenement.getTitre_event();
        String location = safe(evenement.getNom_lieu_event()).isBlank() ? "" : " Salle: " + evenement.getNom_lieu_event() + ".";
        return "Votre evenement " + title + " est maintenant en ligne." + location + " Ouvrez MedFlow pour consulter les details.";
    }

    private boolean hasExistingNotification(int userId, int eventId, String type) {
        String sql = """
                SELECT id
                FROM event_notification
                WHERE user_id = ? AND event_id = ? AND notification_type = ?
                LIMIT 1
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            System.out.println("Impossible de verifier la notification existante: " + ex.getMessage());
            return false;
        }
    }

    private boolean insertNotification(int userId, int eventId, String title, String message, String type) {
        String sql = """
                INSERT INTO event_notification (user_id, event_id, title, message, notification_type, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, eventId);
            ps.setString(3, title);
            ps.setString(4, message);
            ps.setString(5, type);
            ps.setBoolean(6, false);
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.out.println("Impossible d'ajouter la notification evenement: " + ex.getMessage());
            return false;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record NotificationItem(
            int id,
            int userId,
            int eventId,
            String title,
            String message,
            String notificationType,
            boolean read,
            LocalDateTime createdAt
    ) {
    }
}
