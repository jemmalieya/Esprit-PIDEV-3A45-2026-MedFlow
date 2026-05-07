package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.tools.MyDataBase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserSecurityRiskService {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Connection cnx;
    private final UserService userService;
    private volatile boolean schemaInitialized;

    public UserSecurityRiskService() {
        this(MyDataBase.getInstance().getCnx(), new UserService());
    }

    public UserSecurityRiskService(Connection cnx, UserService userService) {
        this.cnx = cnx;
        this.userService = userService;
    }

    public void ensureSchema() {
        try {
            // Core security columns
            addUserColumnIfMissing("suspicious_activity", "BOOLEAN NOT NULL DEFAULT FALSE");
            addUserColumnIfMissing("suspicious_reason", "VARCHAR(255) NULL");
            addUserColumnIfMissing("suspicious_score", "INT NOT NULL DEFAULT 0");
            addUserColumnIfMissing("last_login_ip", "VARCHAR(64) NULL");
            addUserColumnIfMissing("last_login_country", "VARCHAR(120) NULL");
            addUserColumnIfMissing("last_login_at", "DATETIME NULL");
            addUserColumnIfMissing("last_login_city", "VARCHAR(120) NULL");
            addUserColumnIfMissing("last_login_latitude", "DECIMAL(9,6) NULL");
            addUserColumnIfMissing("last_login_longitude", "DECIMAL(9,6) NULL");
            addUserColumnIfMissing("last_device_fingerprint", "VARCHAR(180) NULL");
            addUserColumnIfMissing("last_risk_level", "VARCHAR(12) NULL");
            addUserColumnIfMissing("last_risk_reasons", "VARCHAR(500) NULL");

            // Alert preferences in same table user
            addUserColumnIfMissing("email_security_alerts", "BOOLEAN NOT NULL DEFAULT TRUE");
            addUserColumnIfMissing("push_login_alerts", "BOOLEAN NOT NULL DEFAULT TRUE");

            // Single latest notification stored in user table
            addUserColumnIfMissing("last_security_notification_title", "VARCHAR(140) NULL");
            addUserColumnIfMissing("last_security_notification_message", "VARCHAR(500) NULL");
            addUserColumnIfMissing("last_security_notification_level", "VARCHAR(12) NULL");
            addUserColumnIfMissing("last_security_notification_at", "DATETIME NULL");
            addUserColumnIfMissing("last_security_notification_read", "BOOLEAN NOT NULL DEFAULT FALSE");

            // Brevo per-user credentials
            addUserColumnIfMissing("brevo_api_key", "VARCHAR(255) NULL");
            addUserColumnIfMissing("brevo_sender_email", "VARCHAR(190) NULL");
            addUserColumnIfMissing("brevo_sender_name", "VARCHAR(120) NULL");

            schemaInitialized = true;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de preparer le schema security risk: " + e.getMessage(), e);
        }
    }

    private void addUserColumnIfMissing(String column, String ddl) {
        String sql = "ALTER TABLE user ADD COLUMN " + column + " " + ddl;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException ignored) {
            // Ignore if column already exists.
        }
    }

    private void ensureSchemaInitialized() {
        if (schemaInitialized) {
            return;
        }
        synchronized (this) {
            if (!schemaInitialized) {
                ensureSchema();
            }
        }
    }

    public SecurityContextResponse getSecurityContext(int userId) {
        ensureSchemaInitialized();
        ensureUserExists(userId);

        UserSecuritySnapshot snapshot = readUserSecuritySnapshot(userId);

        return new SecurityContextResponse(
                userId,
                snapshot.lastLoginIp(),
                snapshot.lastLoginCountry(),
                toIso(snapshot.lastLoginAt()),
                snapshot.suspiciousActivity(),
                snapshot.suspiciousScore(),
                isFilled(snapshot.lastRiskLevel()) ? snapshot.lastRiskLevel() : scoreToLevel(snapshot.suspiciousScore()),
                snapshot.suspiciousReason(),
                snapshot.lastLoginCity(),
                snapshot.lastDeviceFingerprint(),
                LocalDateTime.now().format(ISO_FORMAT)
        );
    }

    public RiskEvaluateResponse evaluateRisk(int userId, RiskEvaluationRequest request) {
        ensureSchemaInitialized();
        ensureUserExists(userId);

        LocalDateTime occurredAt = parseOrNow(request.occurredAt());
        UserSecuritySnapshot previous = readUserSecuritySnapshot(userId);
        SecurityEvent latest = toLatestEvent(previous);

        RiskComputation computation = computeRisk(
                previous,
                latest,
                request.ipAddress(),
                request.country(),
                request.latitude(),
                request.longitude(),
                safeInt(request.failedAttempts()),
                request.deviceFingerprint(),
                occurredAt
        );

        updateUserSecurityState(userId, request, computation, occurredAt);
        triggerAlertsIfNeeded(userId, request, previous, latest, computation);

        return new RiskEvaluateResponse(
                userId,
                computation.riskScore(),
                computation.riskLevel(),
                computation.suspicious(),
                computation.reasons(),
                toIso(occurredAt)
        );
    }

    public SuspiciousActivityResponse patchSuspiciousActivity(int userId, SuspiciousActivityPatch patch) {
        ensureSchemaInitialized();
        ensureUserExists(userId);

        boolean flag = patch.flag() != null && patch.flag();
        String reason = trimToNull(patch.reason());
        int score = flag ? Math.max(safeInt(patch.manualScore()), 70) : 0;

        updateSuspiciousStateOnly(userId, flag, score, reason);
        persistManualReviewEvent(userId, flag, score, reason, trimToNull(patch.reviewedBy()));

        SecurityContextResponse context = getSecurityContext(userId);
        return new SuspiciousActivityResponse(true, context);
    }

    public LocationHistoryResponse getLocationHistory(int userId, int limit) {
        ensureSchemaInitialized();
        ensureUserExists(userId);

        List<SecurityEvent> events = new ArrayList<>();
        if (limit <= 0) {
            return new LocationHistoryResponse(userId, 0, events);
        }

        UserSecuritySnapshot snapshot = readUserSecuritySnapshot(userId);
        SecurityEvent latest = toLatestEvent(snapshot);
        if (latest != null) {
            events.add(latest);
        }

        return new LocationHistoryResponse(userId, events.size(), events);
    }

    public SecurityNotificationsResponse getSecurityNotifications(int userId, int limit) {
        ensureSchemaInitialized();
        ensureUserExists(userId);

        List<SecurityNotification> notifications = new ArrayList<>();
        if (limit <= 0) {
            return new SecurityNotificationsResponse(userId, 0, notifications);
        }

        UserSecuritySnapshot snapshot = readUserSecuritySnapshot(userId);
        if (snapshot.lastSecurityNotificationAt() != null && isFilled(snapshot.lastSecurityNotificationTitle())) {
            notifications.add(new SecurityNotification(
                    1L,
                    snapshot.lastSecurityNotificationTitle(),
                    snapshot.lastSecurityNotificationMessage(),
                    snapshot.lastSecurityNotificationLevel(),
                    snapshot.lastSecurityNotificationRead(),
                    toIso(snapshot.lastSecurityNotificationAt())
            ));
        }

        return new SecurityNotificationsResponse(userId, notifications.size(), notifications);
    }

    static RiskComputation computeRisk(
            UserSecuritySnapshot previous,
            SecurityEvent latest,
            String ipAddress,
            String country,
            Double latitude,
            Double longitude,
            int failedAttempts,
            String deviceFingerprint,
            LocalDateTime occurredAt
    ) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        String prevCountry = previous == null ? null : previous.lastLoginCountry();
        String prevIp = previous == null ? null : previous.lastLoginIp();

        if (failedAttempts >= 5) {
            score += 45;
            reasons.add("Plusieurs echecs de connexion detectes");
        } else if (failedAttempts >= 3) {
            score += 30;
            reasons.add("Echecs de connexion repetes");
        }

        if (isFilled(country) && isFilled(prevCountry) && !country.equalsIgnoreCase(prevCountry)) {
            score += 30;
            reasons.add("Nouveau pays de connexion");
        }

        if (isFilled(ipAddress) && isFilled(prevIp) && !ipAddress.equals(prevIp)) {
            score += 10;
            reasons.add("Adresse IP differente");
        }

        if (latest != null && latitude != null && longitude != null
                && latest.latitude() != null && latest.longitude() != null
                && latest.createdAt() != null) {
            long hours = Math.abs(Duration.between(latest.createdAt(), occurredAt).toHours());
            double distanceKm = haversineKm(latest.latitude(), latest.longitude(), latitude, longitude);
            if (hours <= 6 && distanceKm > 700) {
                score += 35;
                reasons.add("Deplacement geographique improbable");
            }
        }

        if (isFilled(deviceFingerprint) && latest != null && isFilled(latest.deviceFingerprint())
                && !deviceFingerprint.equalsIgnoreCase(latest.deviceFingerprint())) {
            score += 15;
            reasons.add("Nouvel appareil detecte");
        }

        score = Math.max(0, Math.min(score, 100));
        String level = scoreToLevel(score);
        boolean suspicious = score >= 60;
        return new RiskComputation(score, level, suspicious, reasons);
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(earthRadiusKm * c).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static String scoreToLevel(int score) {
        if (score >= 80) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    private UserSecuritySnapshot readUserSecuritySnapshot(int userId) {
        String snakeSql = "SELECT last_login_ip, last_login_country, last_login_at, last_login_city, last_login_latitude, last_login_longitude, last_device_fingerprint, last_risk_level, last_risk_reasons, suspicious_activity, suspicious_reason, suspicious_score, email_security_alerts, push_login_alerts, last_security_notification_title, last_security_notification_message, last_security_notification_level, last_security_notification_at, last_security_notification_read FROM user WHERE id = ?";
        String camelSql = "SELECT lastLoginIp AS last_login_ip, lastLoginCountry AS last_login_country, lastLoginAt AS last_login_at, lastLoginCity AS last_login_city, lastLoginLatitude AS last_login_latitude, lastLoginLongitude AS last_login_longitude, lastDeviceFingerprint AS last_device_fingerprint, lastRiskLevel AS last_risk_level, lastRiskReasons AS last_risk_reasons, suspiciousActivity AS suspicious_activity, suspiciousReason AS suspicious_reason, suspiciousScore AS suspicious_score, emailSecurityAlerts AS email_security_alerts, pushLoginAlerts AS push_login_alerts, lastSecurityNotificationTitle AS last_security_notification_title, lastSecurityNotificationMessage AS last_security_notification_message, lastSecurityNotificationLevel AS last_security_notification_level, lastSecurityNotificationAt AS last_security_notification_at, lastSecurityNotificationRead AS last_security_notification_read FROM user WHERE id = ?";

        for (String sql : new String[]{snakeSql, camelSql}) {
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new UserSecuritySnapshot(
                                rs.getString("last_login_ip"),
                                rs.getString("last_login_country"),
                                toLocalDateTime(rs.getTimestamp("last_login_at")),
                                rs.getString("last_login_city"),
                                readNullableDouble(rs, "last_login_latitude"),
                                readNullableDouble(rs, "last_login_longitude"),
                                rs.getString("last_device_fingerprint"),
                                rs.getString("last_risk_level"),
                                rs.getString("last_risk_reasons"),
                                rs.getBoolean("suspicious_activity"),
                                trimToNull(rs.getString("suspicious_reason")),
                                rs.getInt("suspicious_score"),
                                rs.getBoolean("email_security_alerts"),
                                rs.getBoolean("push_login_alerts"),
                                rs.getString("last_security_notification_title"),
                                rs.getString("last_security_notification_message"),
                                rs.getString("last_security_notification_level"),
                                toLocalDateTime(rs.getTimestamp("last_security_notification_at")),
                                rs.getBoolean("last_security_notification_read")
                        );
                    }
                }
            } catch (SQLException ignored) {
            }
        }

        return new UserSecuritySnapshot(null, null, null, null, null, null, null, null, null, false, null, 0, true, true, null, null, null, null, false);
    }

    private Double readNullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private void persistManualReviewEvent(int userId, boolean flag, int score, String reason, String reviewedBy) {
        String finalReason = reason == null ? (flag ? "Marque manuellement comme suspect" : "Etat suspect retire") : reason;
        if (isFilled(reviewedBy)) {
            finalReason = finalReason + " (reviewedBy=" + reviewedBy + ")";
        }
        persistSecurityNotification(userId, flag ? "Revue manuelle: suspect" : "Revue manuelle: normal", finalReason, scoreToLevel(score));
    }

    private static final int LOCK_THRESHOLD = 75; // score >= 75 → verrouillage automatique

    private void triggerAlertsIfNeeded(
            int userId,
            RiskEvaluationRequest request,
            UserSecuritySnapshot previous,
            SecurityEvent latest,
            RiskComputation computation
    ) {
        boolean loginChanged = hasLoginChanged(previous, latest, request);
        boolean shouldAlert = computation.suspicious() || loginChanged;
        if (!shouldAlert) {
            return;
        }

        String reasonsText = joinReasons(computation.reasons());
        if (reasonsText == null || reasonsText.isBlank()) {
            reasonsText = "Activite de connexion inhabituelle detectee";
        }

        if (isPushLoginAlertsEnabled(userId)) {
            String title = computation.suspicious() ? "Connexion suspecte detectee" : "Changement de connexion detecte";
            String message = "Risque " + computation.riskLevel() + " (score " + computation.riskScore() + ") - " + reasonsText;
            persistSecurityNotification(userId, title, message, computation.riskLevel());
        }

        if (isEmailSecurityAlertsEnabled(userId)) {
            User user = userService.findById(userId);
            if (user != null) {
                // Verrouillage automatique si score critique
                if (computation.riskScore() >= LOCK_THRESHOLD) {
                    lockAccountDueToSuspiciousActivity(userId, user, reasonsText, computation.riskScore());
                } else {
                    EmailService.sendSecurityAlertEmail(
                            user,
                            computation.suspicious() ? "Connexion suspecte" : "Changement de connexion",
                            reasonsText,
                            computation.riskScore(),
                            computation.riskLevel()
                    );
                }
            }
        }
    }

    private boolean hasLoginChanged(UserSecuritySnapshot previous, SecurityEvent latest, RiskEvaluationRequest request) {
        String previousIp = previous == null ? null : previous.lastLoginIp();
        String previousCountry = previous == null ? null : previous.lastLoginCountry();
        String previousDevice = latest == null ? null : latest.deviceFingerprint();

        boolean ipChanged = isFilled(request.ipAddress()) && isFilled(previousIp) && !request.ipAddress().equals(previousIp);
        boolean countryChanged = isFilled(request.country()) && isFilled(previousCountry) && !request.country().equalsIgnoreCase(previousCountry);
        boolean deviceChanged = isFilled(request.deviceFingerprint()) && isFilled(previousDevice) && !request.deviceFingerprint().equalsIgnoreCase(previousDevice);

        return ipChanged || countryChanged || deviceChanged;
    }

    private boolean isEmailSecurityAlertsEnabled(int userId) {
        return readUserSecuritySnapshot(userId).emailSecurityAlertsEnabled();
    }

    private boolean isPushLoginAlertsEnabled(int userId) {
        return readUserSecuritySnapshot(userId).pushLoginAlertsEnabled();
    }

    private void persistSecurityNotification(int userId, String title, String message, String level) {
        String snakeSql = "UPDATE user SET last_security_notification_title = ?, last_security_notification_message = ?, last_security_notification_level = ?, last_security_notification_at = ?, last_security_notification_read = FALSE WHERE id = ?";
        String camelSql = "UPDATE user SET lastSecurityNotificationTitle = ?, lastSecurityNotificationMessage = ?, lastSecurityNotificationLevel = ?, lastSecurityNotificationAt = ?, lastSecurityNotificationRead = FALSE WHERE id = ?";

        if (tryPersistNotification(snakeSql, userId, title, message, level)) {
            return;
        }
        tryPersistNotification(camelSql, userId, title, message, level);
    }

    private boolean tryPersistNotification(String sql, int userId, String title, String message, String level) {
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, trimToNull(title));
            ps.setString(2, trimToNull(message));
            ps.setString(3, trimToNull(level));
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(5, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.DECIMAL);
        } else {
            ps.setBigDecimal(index, BigDecimal.valueOf(value));
        }
    }

    private void updateUserSecurityState(
            int userId,
            RiskEvaluationRequest request,
            RiskComputation computation,
            LocalDateTime occurredAt
    ) {
        String reason = joinReasons(computation.reasons());

        String snakeSqlExtended = "UPDATE user SET last_login_ip = ?, last_login_country = ?, last_login_at = ?, last_login_city = ?, last_login_latitude = ?, last_login_longitude = ?, last_device_fingerprint = ?, last_risk_level = ?, last_risk_reasons = ?, suspicious_activity = ?, suspicious_reason = ?, suspicious_score = ? WHERE id = ?";
        String camelSqlExtended = "UPDATE user SET lastLoginIp = ?, lastLoginCountry = ?, lastLoginAt = ?, lastLoginCity = ?, lastLoginLatitude = ?, lastLoginLongitude = ?, lastDeviceFingerprint = ?, lastRiskLevel = ?, lastRiskReasons = ?, suspiciousActivity = ?, suspiciousReason = ?, suspiciousScore = ? WHERE id = ?";

        if (tryUpdateUserStateExtended(snakeSqlExtended, userId, request, occurredAt, computation.suspicious(), reason, computation.riskScore(), computation.riskLevel())) {
            return;
        }
        if (tryUpdateUserStateExtended(camelSqlExtended, userId, request, occurredAt, computation.suspicious(), reason, computation.riskScore(), computation.riskLevel())) {
            return;
        }

        String snakeSql = "UPDATE user SET last_login_ip = ?, last_login_country = ?, last_login_at = ?, suspicious_activity = ?, suspicious_reason = ?, suspicious_score = ? WHERE id = ?";
        String camelSql = "UPDATE user SET lastLoginIp = ?, lastLoginCountry = ?, lastLoginAt = ?, suspiciousActivity = ?, suspiciousReason = ?, suspiciousScore = ? WHERE id = ?";

        if (tryUpdateUserState(snakeSql, userId, request, occurredAt, computation.suspicious(), reason, computation.riskScore())) {
            return;
        }
        tryUpdateUserState(camelSql, userId, request, occurredAt, computation.suspicious(), reason, computation.riskScore());
    }

    private void updateSuspiciousStateOnly(int userId, boolean suspicious, int score, String reason) {
        String snakeSql = "UPDATE user SET suspicious_activity = ?, suspicious_reason = ?, suspicious_score = ? WHERE id = ?";
        String camelSql = "UPDATE user SET suspiciousActivity = ?, suspiciousReason = ?, suspiciousScore = ? WHERE id = ?";

        if (tryUpdateSuspiciousOnly(snakeSql, userId, suspicious, reason, score)) {
            return;
        }
        tryUpdateSuspiciousOnly(camelSql, userId, suspicious, reason, score);
    }

    private boolean tryUpdateUserState(
            String sql,
            int userId,
            RiskEvaluationRequest request,
            LocalDateTime occurredAt,
            boolean suspicious,
            String reason,
            int score
    ) {
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, trimToNull(request.ipAddress()));
            ps.setString(2, trimToNull(request.country()));
            ps.setTimestamp(3, Timestamp.valueOf(occurredAt));
            ps.setBoolean(4, suspicious);
            ps.setString(5, trimToNull(reason));
            ps.setInt(6, score);
            ps.setInt(7, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean tryUpdateUserStateExtended(
            String sql,
            int userId,
            RiskEvaluationRequest request,
            LocalDateTime occurredAt,
            boolean suspicious,
            String reason,
            int score,
            String riskLevel
    ) {
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, trimToNull(request.ipAddress()));
            ps.setString(2, trimToNull(request.country()));
            ps.setTimestamp(3, Timestamp.valueOf(occurredAt));
            ps.setString(4, trimToNull(request.city()));
            setNullableDouble(ps, 5, request.latitude());
            setNullableDouble(ps, 6, request.longitude());
            ps.setString(7, trimToNull(request.deviceFingerprint()));
            ps.setString(8, trimToNull(riskLevel));
            ps.setString(9, trimToNull(reason));
            ps.setBoolean(10, suspicious);
            ps.setString(11, trimToNull(reason));
            ps.setInt(12, score);
            ps.setInt(13, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean tryUpdateSuspiciousOnly(String sql, int userId, boolean suspicious, String reason, int score) {
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBoolean(1, suspicious);
            ps.setString(2, trimToNull(reason));
            ps.setInt(3, score);
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private SecurityEvent toLatestEvent(UserSecuritySnapshot snapshot) {
        if (snapshot == null || snapshot.lastLoginAt() == null) {
            return null;
        }
        return new SecurityEvent(
                1L,
                "LATEST_LOGIN",
                snapshot.lastLoginIp(),
                snapshot.lastLoginCountry(),
                snapshot.lastLoginCity(),
                snapshot.lastLoginLatitude(),
                snapshot.lastLoginLongitude(),
                snapshot.lastDeviceFingerprint(),
                snapshot.suspiciousActivity(),
                snapshot.suspiciousScore(),
                isFilled(snapshot.lastRiskLevel()) ? snapshot.lastRiskLevel() : scoreToLevel(snapshot.suspiciousScore()),
                snapshot.lastRiskReasons(),
                snapshot.lastLoginAt()
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Account Lock / Unlock
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Verrouille le compte, génère un token 8 chiffres, et envoie l'email.
     * Appelé automatiquement quand riskScore >= LOCK_THRESHOLD.
     */
    private void lockAccountDueToSuspiciousActivity(int userId, User user, String reason, int score) {
        // 1. Générer un token numérique à 8 chiffres
        String unlockToken = generateUnlockCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        // 2. Verrouiller le compte et stocker le token
        String sql = "UPDATE user SET statut_compte = 'BLOQUE', reset_token = ?, reset_token_expires_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, unlockToken);
            ps.setTimestamp(2, Timestamp.valueOf(expiresAt));
            ps.setInt(3, userId);
            ps.executeUpdate();
            System.out.println("[Security] Compte verrouillé userId=" + userId + " score=" + score);
        } catch (SQLException e) {
            System.err.println("[Security] Impossible de verrouiller le compte userId=" + userId + ": " + e.getMessage());
            return;
        }

        // 3. Persister la notification in-app
        persistSecurityNotification(userId,
                "Compte verrouillé – activité suspecte",
                "Score de risque : " + score + ". " + reason + ". Un code de déverrouillage a été envoyé par email.",
                "CRITICAL");

        // 4. Envoyer l'email avec le code de déverrouillage
        user.setStatutCompte("BLOQUE");
        EmailService.sendAccountLockedEmail(user, unlockToken);
    }

    /**
     * Déverrouille le compte si le token est valide et non expiré.
     *
     * @param token code à 8 chiffres reçu par email
     * @return true si déverrouillé avec succès, false sinon
     */
    public boolean unlockAccountByToken(String token) {
        if (token == null || token.isBlank()) return false;

        String selectSql = "SELECT id, reset_token_expires_at FROM user WHERE reset_token = ? AND statut_compte = 'BLOQUE'";
        try (PreparedStatement ps = cnx.prepareStatement(selectSql)) {
            ps.setString(1, token.trim());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.err.println("[Security] Token de déverrouillage invalide ou compte non verrouillé.");
                return false;
            }
            int userId = rs.getInt("id");
            Timestamp expiresAt = rs.getTimestamp("reset_token_expires_at");
            if (expiresAt == null || expiresAt.toLocalDateTime().isBefore(LocalDateTime.now())) {
                System.err.println("[Security] Token expiré pour userId=" + userId);
                return false;
            }

            // Token valide → déverrouiller
            String unlockSql = "UPDATE user SET statut_compte = 'ACTIF', reset_token = NULL, reset_token_expires_at = NULL WHERE id = ?";
            try (PreparedStatement upd = cnx.prepareStatement(unlockSql)) {
                upd.setInt(1, userId);
                upd.executeUpdate();
            }
            persistSecurityNotification(userId, "Compte déverrouillé", "Compte réactivé via le code de déverrouillage.", "LOW");
            System.out.println("[Security] Compte déverrouillé avec succès userId=" + userId);
            return true;

        } catch (SQLException e) {
            System.err.println("[Security] Erreur déverrouillage: " + e.getMessage());
            return false;
        }
    }

    /**
     * Vérifie si un compte est verrouillé (pour bloquer la connexion).
     */
    public boolean isAccountLocked(int userId) {
        String sql = "SELECT statut_compte FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "BLOQUE".equalsIgnoreCase(rs.getString("statut_compte"));
            }
        } catch (SQLException ignored) {}
        return false;
    }

    private static String generateUnlockCode() {
        SecureRandom rng = new SecureRandom();
        int code = 10_000_000 + rng.nextInt(90_000_000); // 8 chiffres
        return String.valueOf(code);
    }

    private void ensureUserExists(int userId) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur introuvable (id=" + userId + ")");
        }
    }

    private static boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static LocalDateTime parseOrNow(String input) {
        if (input == null || input.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(input, ISO_FORMAT);
        } catch (Exception ignored) {
            return LocalDateTime.now();
        }
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static String toIso(LocalDateTime value) {
        return value == null ? null : value.format(ISO_FORMAT);
    }

    private static String joinReasons(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return null;
        }
        return String.join("; ", reasons);
    }

    public record UserSecuritySnapshot(
            String lastLoginIp,
            String lastLoginCountry,
            LocalDateTime lastLoginAt,
            String lastLoginCity,
            Double lastLoginLatitude,
            Double lastLoginLongitude,
            String lastDeviceFingerprint,
            String lastRiskLevel,
            String lastRiskReasons,
            boolean suspiciousActivity,
            String suspiciousReason,
            int suspiciousScore,
            boolean emailSecurityAlertsEnabled,
            boolean pushLoginAlertsEnabled,
            String lastSecurityNotificationTitle,
            String lastSecurityNotificationMessage,
            String lastSecurityNotificationLevel,
            LocalDateTime lastSecurityNotificationAt,
            boolean lastSecurityNotificationRead
    ) {}

    public record RiskComputation(int riskScore, String riskLevel, boolean suspicious, List<String> reasons) {}

    public record RiskEvaluationRequest(
            String ipAddress,
            String country,
            String city,
            Double latitude,
            Double longitude,
            Integer failedAttempts,
            String deviceFingerprint,
            String source,
            String occurredAt
    ) {}

    public record RiskEvaluateResponse(
            int userId,
            int riskScore,
            String riskLevel,
            boolean suspicious,
            List<String> reasons,
            String evaluatedAt
    ) {}

    public record SecurityContextResponse(
            int userId,
            String lastLoginIp,
            String lastLoginCountry,
            String lastLoginAt,
            boolean suspiciousActivity,
            int riskScore,
            String riskLevel,
            String suspiciousReason,
            String lastCity,
            String lastDeviceFingerprint,
            String updatedAt
    ) {}

    public record SuspiciousActivityPatch(Boolean flag, String reason, Integer manualScore, String reviewedBy) {}

    public record SuspiciousActivityResponse(boolean updated, SecurityContextResponse securityContext) {}

    public record SecurityEvent(
            long id,
            String eventType,
            String ipAddress,
            String country,
            String city,
            Double latitude,
            Double longitude,
            String deviceFingerprint,
            boolean suspicious,
            int riskScore,
            String riskLevel,
            String reason,
            LocalDateTime createdAt
    ) {}

    public record SecurityNotification(
            long id,
            String title,
            String message,
            String level,
            boolean read,
            String createdAt
    ) {}

    public record SecurityNotificationsResponse(int userId, int count, List<SecurityNotification> notifications) {}

    public record LocationHistoryResponse(int userId, int count, List<SecurityEvent> events) {}
}

