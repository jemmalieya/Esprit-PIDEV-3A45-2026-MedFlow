package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.tools.MyDataBase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class UserProfileGamificationService {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Connection cnx;
    private final UserService userService;
    private volatile boolean schemaInitialized;

    public UserProfileGamificationService() {
        this(MyDataBase.getInstance().getCnx(), new UserService());
    }

    public UserProfileGamificationService(Connection cnx, UserService userService) {
        this.cnx = cnx;
        this.userService = userService;
    }

    public void ensureSchema() {
        try {
            addUserColumnIfMissing("language", "VARCHAR(10) NOT NULL DEFAULT 'fr'");
            addUserColumnIfMissing("theme", "VARCHAR(10) NOT NULL DEFAULT 'light'");
            addUserColumnIfMissing("email_security_alerts", "BOOLEAN NOT NULL DEFAULT TRUE");
            addUserColumnIfMissing("email_marketing", "BOOLEAN NOT NULL DEFAULT FALSE");
            addUserColumnIfMissing("push_login_alerts", "BOOLEAN NOT NULL DEFAULT TRUE");
            addUserColumnIfMissing("profile_visibility", "VARCHAR(20) NOT NULL DEFAULT 'private'");
            addUserColumnIfMissing("preferences_updated_at", "DATETIME NULL");
            schemaInitialized = true;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de preparer le schema API user: " + e.getMessage(), e);
        }
    }

    private void addUserColumnIfMissing(String column, String ddl) {
        String sql = "ALTER TABLE user ADD COLUMN " + column + " " + ddl;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException ignored) {
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

    public ActivationScoreResponse getActivationScore(int userId) {
        ensureSchemaInitialized();
        User user = requireUser(userId);
        UserPreferences preferences = getOrCreatePreferences(userId);

        Evaluation evaluation = evaluateFor(user, preferences);
        return new ActivationScoreResponse(
                userId,
                evaluation.score(),
                scoreLevel(evaluation.score()),
                100 - evaluation.score(),
                evaluation.breakdown(),
                evaluation.badges(),
                LocalDateTime.now().format(ISO_FORMAT)
        );
    }

    public NextBestActionsResponse getNextBestActions(int userId) {
        ensureSchemaInitialized();
        User user = requireUser(userId);
        UserPreferences preferences = getOrCreatePreferences(userId);

        Evaluation evaluation = evaluateFor(user, preferences);
        List<NextAction> actions = buildNextActions(preferences, evaluation);
        return new NextBestActionsResponse(userId, actions);
    }

    public PreferencesResponse patchPreferences(int userId, PreferencesPatch patch) {
        ensureSchemaInitialized();
        requireUser(userId);
        UserPreferences current = getOrCreatePreferences(userId);

        UserPreferences merged = new UserPreferences(
                userId,
                cleanLanguage(orElse(patch.language(), current.language())),
                cleanTheme(orElse(patch.theme(), current.theme())),
                orElse(patch.emailSecurityAlerts(), current.emailSecurityAlerts()),
                orElse(patch.emailMarketing(), current.emailMarketing()),
                orElse(patch.pushLoginAlerts(), current.pushLoginAlerts()),
                cleanVisibility(orElse(patch.profileVisibility(), current.profileVisibility())),
                LocalDateTime.now().format(ISO_FORMAT)
        );

        upsertPreferences(merged);
        return new PreferencesResponse(true, merged, merged.updatedAt());
    }

    public BadgesResponse getBadges(int userId) {
        ensureSchemaInitialized();
        User user = requireUser(userId);
        UserPreferences preferences = getOrCreatePreferences(userId);
        Evaluation evaluation = evaluateFor(user, preferences);
        return new BadgesResponse(userId, evaluation.badges(), securityLevel(evaluation.badges()));
    }

    static Evaluation evaluateFor(User user, UserPreferences preferences) {
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int score = 0;

        boolean emailVerified = user.isVerified();
        if (emailVerified) score += 20;
        breakdown.put("emailVerified", emailVerified ? 20 : 0);

        boolean phoneSet = isFilled(user.getTelephoneUser());
        if (phoneSet) score += 10;
        breakdown.put("phoneProvided", phoneSet ? 10 : 0);

        boolean profilePicture = isFilled(user.getProfilePicture()) && !"default.png".equalsIgnoreCase(user.getProfilePicture());
        if (profilePicture) score += 10;
        breakdown.put("profilePicture", profilePicture ? 10 : 0);

        boolean profileCompleted = isFilled(user.getNom()) && isFilled(user.getPrenom()) && isFilled(user.getAdresseUser());
        if (profileCompleted) score += 10;
        breakdown.put("profileCompleted", profileCompleted ? 10 : 0);

        boolean strongPassword = isStrongPasswordHashOrValue(user.getPassword());
        if (strongPassword) score += 20;
        breakdown.put("strongPassword", strongPassword ? 20 : 0);

        boolean mfaEnabled = user.isTotpEnabled() && isFilled(user.getTotpSecret());
        if (mfaEnabled) score += 20;
        breakdown.put("mfaEnabled", mfaEnabled ? 20 : 0);

        boolean biometricEnabled = user.isFaceLoginEnabled() && isFilled(user.getFaceReferenceEmbedding());
        if (biometricEnabled) score += 10;
        breakdown.put("faceSecurity", biometricEnabled ? 10 : 0);

        boolean securityAlertsEnabled = preferences.emailSecurityAlerts() && preferences.pushLoginAlerts();
        if (securityAlertsEnabled) score += 10;
        breakdown.put("securityAlerts", securityAlertsEnabled ? 10 : 0);

        List<BadgeStatus> badges = buildBadges(emailVerified, strongPassword, mfaEnabled, biometricEnabled, securityAlertsEnabled);
        return new Evaluation(score, breakdown, badges);
    }

    private static List<BadgeStatus> buildBadges(boolean emailVerified,
                                                 boolean strongPassword,
                                                 boolean mfaEnabled,
                                                 boolean biometricEnabled,
                                                 boolean securityAlertsEnabled) {
        List<BadgeStatus> badges = new ArrayList<>();
        badges.add(new BadgeStatus("verified-account", "Compte verifie", emailVerified, emailVerified ? 1.0 : 0.0, emailVerified ? LocalDateTime.now().format(ISO_FORMAT) : null));
        badges.add(new BadgeStatus("password-guardian", "Mot de passe robuste", strongPassword, strongPassword ? 1.0 : 0.0, strongPassword ? LocalDateTime.now().format(ISO_FORMAT) : null));
        badges.add(new BadgeStatus("mfa-shield", "Bouclier 2FA", mfaEnabled, mfaEnabled ? 1.0 : 0.0, mfaEnabled ? LocalDateTime.now().format(ISO_FORMAT) : null));
        badges.add(new BadgeStatus("biometric-guard", "Protection biometrique", biometricEnabled, biometricEnabled ? 1.0 : 0.0, biometricEnabled ? LocalDateTime.now().format(ISO_FORMAT) : null));
        badges.add(new BadgeStatus("alert-master", "Alertes securite actives", securityAlertsEnabled, securityAlertsEnabled ? 1.0 : 0.0, securityAlertsEnabled ? LocalDateTime.now().format(ISO_FORMAT) : null));

        long unlockedCore = badges.stream().filter(BadgeStatus::unlocked).count();
        boolean master = unlockedCore == 5;
        double progress = unlockedCore / 5.0;
        badges.add(new BadgeStatus("security-master", "Maitre de la securite", master, roundProgress(progress), master ? LocalDateTime.now().format(ISO_FORMAT) : null));
        return badges;
    }

    private List<NextAction> buildNextActions(UserPreferences preferences, Evaluation evaluation) {
        Map<String, Integer> breakdown = evaluation.breakdown();
        List<NextAction> actions = new ArrayList<>();

        if (breakdown.getOrDefault("emailVerified", 0) == 0) {
            actions.add(new NextAction("VERIFY_EMAIL", "Verifier votre e-mail", "Validez votre adresse e-mail pour securiser le compte.", 20, "verified-account", "HIGH"));
        }
        if (breakdown.getOrDefault("strongPassword", 0) == 0) {
            actions.add(new NextAction("SET_STRONG_PASSWORD", "Renforcer le mot de passe", "Utilisez un mot de passe robuste (12+ caracteres avec symboles).", 20, "password-guardian", "HIGH"));
        }
        if (breakdown.getOrDefault("mfaEnabled", 0) == 0) {
            actions.add(new NextAction("ENABLE_MFA", "Activer la double authentification", "Activez la 2FA avec une application OTP.", 20, "mfa-shield", "HIGH"));
        }
        if (breakdown.getOrDefault("faceSecurity", 0) == 0) {
            actions.add(new NextAction("ENABLE_FACE_LOGIN", "Activer la securite biometrie", "Enregistrez votre empreinte faciale pour renforcer la connexion.", 10, "biometric-guard", "MEDIUM"));
        }
        if (!preferences.emailSecurityAlerts() || !preferences.pushLoginAlerts()) {
            actions.add(new NextAction("ENABLE_SECURITY_ALERTS", "Activer toutes les alertes securite", "Recevez les alertes e-mail et push pour chaque connexion sensible.", 10, "alert-master", "MEDIUM"));
        }
        if (breakdown.getOrDefault("profileCompleted", 0) == 0) {
            actions.add(new NextAction("COMPLETE_PROFILE", "Completer votre profil", "Ajoutez nom, prenom et adresse pour un profil complet.", 10, null, "LOW"));
        }
        if (breakdown.getOrDefault("profilePicture", 0) == 0) {
            actions.add(new NextAction("UPLOAD_PROFILE_PICTURE", "Ajouter une photo de profil", "Une photo personnalisee augmente la confiance du compte.", 10, null, "LOW"));
        }
        if (breakdown.getOrDefault("phoneProvided", 0) == 0) {
            actions.add(new NextAction("ADD_PHONE", "Ajouter un numero de telephone", "Ajoutez un numero pour faciliter la recuperation de compte.", 10, null, "LOW"));
        }

        return actions;
    }

    private User requireUser(int userId) {
        User user = userService.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Utilisateur introuvable (id=" + userId + ")");
        }
        return user;
    }

    private UserPreferences getOrCreatePreferences(int userId) {
        String snakeSql = "SELECT id, language, theme, email_security_alerts, email_marketing, push_login_alerts, profile_visibility, preferences_updated_at FROM user WHERE id = ?";
        String camelSql = "SELECT id, language, theme, emailSecurityAlerts AS email_security_alerts, emailMarketing AS email_marketing, pushLoginAlerts AS push_login_alerts, profileVisibility AS profile_visibility, preferencesUpdatedAt AS preferences_updated_at FROM user WHERE id = ?";

        for (String sql : new String[]{snakeSql, camelSql}) {
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Timestamp ts = rs.getTimestamp("preferences_updated_at");
                        UserPreferences preferences = new UserPreferences(
                                rs.getInt("id"),
                                cleanLanguage(safeOrDefault(rs.getString("language"), "fr")),
                                cleanTheme(safeOrDefault(rs.getString("theme"), "light")),
                                rs.getBoolean("email_security_alerts"),
                                rs.getBoolean("email_marketing"),
                                rs.getBoolean("push_login_alerts"),
                                cleanVisibility(safeOrDefault(rs.getString("profile_visibility"), "private")),
                                ts == null ? LocalDateTime.now().format(ISO_FORMAT) : ts.toLocalDateTime().format(ISO_FORMAT)
                        );
                        upsertPreferences(preferences);
                        return preferences;
                    }
                }
            } catch (SQLException ignored) {
            }
        }

        throw new IllegalStateException("Impossible de lire les preferences utilisateur depuis la table user.");
    }

    private void upsertPreferences(UserPreferences preferences) {
        String snakeSql = "UPDATE user SET language = ?, theme = ?, email_security_alerts = ?, email_marketing = ?, push_login_alerts = ?, profile_visibility = ?, preferences_updated_at = ? WHERE id = ?";
        String camelSql = "UPDATE user SET language = ?, theme = ?, emailSecurityAlerts = ?, emailMarketing = ?, pushLoginAlerts = ?, profileVisibility = ?, preferencesUpdatedAt = ? WHERE id = ?";

        if (executePreferencesUpdate(snakeSql, preferences)) {
            return;
        }
        if (executePreferencesUpdate(camelSql, preferences)) {
            return;
        }
        throw new IllegalStateException("Impossible d'enregistrer les preferences utilisateur dans la table user.");
    }

    private boolean executePreferencesUpdate(String sql, UserPreferences preferences) {
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, preferences.language());
            ps.setString(2, preferences.theme());
            ps.setBoolean(3, preferences.emailSecurityAlerts());
            ps.setBoolean(4, preferences.emailMarketing());
            ps.setBoolean(5, preferences.pushLoginAlerts());
            ps.setString(6, preferences.profileVisibility());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.parse(preferences.updatedAt(), ISO_FORMAT)));
            ps.setInt(8, preferences.userId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private static String securityLevel(List<BadgeStatus> badges) {
        long unlocked = badges.stream().filter(BadgeStatus::unlocked).count();
        if (unlocked >= 6) return "elite";
        if (unlocked >= 4) return "advanced";
        if (unlocked >= 2) return "intermediate";
        return "starter";
    }

    private static String scoreLevel(int score) {
        if (score >= 90) return "platinum";
        if (score >= 75) return "gold";
        if (score >= 50) return "silver";
        return "bronze";
    }

    private static boolean isStrongPasswordHashOrValue(String password) {
        if (!isFilled(password)) {
            return false;
        }
        if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
            return true;
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        return password.length() >= 12 && hasUpper && hasLower && hasDigit && hasSpecial;
    }

    private static boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

    private static <T> T orElse(T incoming, T fallback) {
        return incoming == null ? fallback : incoming;
    }

    private static String cleanLanguage(String value) {
        String normalized = safeOrDefault(value, "fr").toLowerCase(Locale.ROOT);
        if (!normalized.equals("fr") && !normalized.equals("en") && !normalized.equals("ar")) {
            return "fr";
        }
        return normalized;
    }

    private static String cleanTheme(String value) {
        String normalized = safeOrDefault(value, "light").toLowerCase(Locale.ROOT);
        if (!normalized.equals("light") && !normalized.equals("dark") && !normalized.equals("system")) {
            return "light";
        }
        return normalized;
    }

    private static String cleanVisibility(String value) {
        String normalized = safeOrDefault(value, "private").toLowerCase(Locale.ROOT);
        if (!normalized.equals("private") && !normalized.equals("friends") && !normalized.equals("public")) {
            return "private";
        }
        return normalized;
    }

    private static String safeOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static double roundProgress(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public record Evaluation(int score, Map<String, Integer> breakdown, List<BadgeStatus> badges) {}

    public record ActivationScoreResponse(
            int userId,
            int activationScore,
            String level,
            int missingPoints,
            Map<String, Integer> breakdown,
            List<BadgeStatus> badges,
            String updatedAt
    ) {}

    public record NextBestActionsResponse(int userId, List<NextAction> actions) {}

    public record NextAction(
            String code,
            String title,
            String description,
            int rewardPoints,
            String badgeUnlock,
            String priority
    ) {}

    public record PreferencesPatch(
            String language,
            String theme,
            Boolean emailSecurityAlerts,
            Boolean emailMarketing,
            Boolean pushLoginAlerts,
            String profileVisibility
    ) {}

    public record UserPreferences(
            int userId,
            String language,
            String theme,
            boolean emailSecurityAlerts,
            boolean emailMarketing,
            boolean pushLoginAlerts,
            String profileVisibility,
            String updatedAt
    ) {}

    public record PreferencesResponse(boolean updated, UserPreferences preferences, String updatedAt) {}

    public record BadgeStatus(String code, String title, boolean unlocked, double progress, String unlockedAt) {
        public BadgeStatus {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(title, "title");
        }
    }

    public record BadgesResponse(int userId, List<BadgeStatus> badges, String securityLevel) {}
}
