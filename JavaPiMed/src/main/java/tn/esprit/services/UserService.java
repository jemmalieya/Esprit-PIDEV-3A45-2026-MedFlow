package tn.esprit.services;

import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.entities.Reclamation;
import tn.esprit.entities.User;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IGeneralService<User> {


    private final Connection cnx;
    private String lastFaceEnrollmentError;

    public UserService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void ajouter(User user) {
        String dbError = ajouterAvecRetour(user);
        if (dbError == null) {
            System.out.println("Utilisateur ajoute avec succes");
        } else {
            System.out.println("Erreur lors de l'ajout de l'utilisateur : " + dbError);
        }
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM user WHERE email_user = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur vérification unicité email : " + e.getMessage());
        }
        return false;
    }

    public boolean existsByCin(String cin) {
        String sql = "SELECT COUNT(*) FROM user WHERE cin = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, cin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur vérification unicité CIN : " + e.getMessage());
        }
        return false;
    }

    public String ajouterAvecRetour(User user) {
        if (existsByEmail(user.getEmailUser())) {
            return "Un compte avec l'adresse e-mail '" + user.getEmailUser() + "' existe déjà.";
        }
        if (existsByCin(user.getCin())) {
            return "Un compte avec le CIN '" + user.getCin() + "' existe déjà.";
        }

        String extendedSnakeCase = "INSERT INTO user(cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme, type_staff, staff_request_status, staff_request_type, staff_request_message, staff_requested_at, staff_request_proof_path, staff_documents, staff_request_reason) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String extendedCamelCase = "INSERT INTO user(cin, profilePicture, nom, prenom, dateNaissance, telephoneUser, emailUser, adresseUser, password, isVerified, statutCompte, roleSysteme, typeStaff, staffRequestStatus, staffRequestType, staffRequestMessage, staffRequestedAt, staffRequestProofPath, staffDocuments, staffRequestReason) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        String legacySql = "INSERT INTO user(cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";

        try {
            insertUser(user, extendedSnakeCase, true);
            return null;
        } catch (SQLException ignored) {
            try {
                insertUser(user, extendedCamelCase, true);
                return null;
            } catch (SQLException ignoredToo) {
                try {
                    insertUser(user, legacySql, false);
                    return null;
                } catch (SQLException e) {
                    return e.getMessage();
                }
            }
        }
    }

    private void insertUser(User user, String sql, boolean includeStaffColumns) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getCin());
            ps.setString(2, user.getProfilePicture());
            ps.setString(3, user.getNom());
            ps.setString(4, user.getPrenom());

            if (user.getDateNaissance() != null) {
                ps.setDate(5, Date.valueOf(user.getDateNaissance()));
            } else {
                ps.setNull(5, Types.DATE);
            }

            ps.setString(6, user.getTelephoneUser());
            ps.setString(7, user.getEmailUser());
            ps.setString(8, user.getAdresseUser());
            ps.setString(9, user.getPassword());
            ps.setBoolean(10, user.isVerified());
            ps.setString(11, user.getStatutCompte());
            ps.setString(12, user.getRoleSysteme());

            if (includeStaffColumns) {
                ps.setString(13, user.getTypeStaff());
                ps.setString(14, user.getStaffRequestStatus());
                ps.setString(15, user.getStaffRequestType());
                ps.setString(16, user.getStaffRequestMessage());
                if (user.getStaffRequestedAt() != null) {
                    ps.setTimestamp(17, Timestamp.valueOf(user.getStaffRequestedAt()));
                } else {
                    ps.setNull(17, Types.TIMESTAMP);
                }
                ps.setString(18, user.getStaffRequestProofPath());
                ps.setString(19, user.getStaffDocuments());
                ps.setString(20, user.getStaffRequestReason());
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
        }
    }

    @Override
    public void supprimer(User user) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            ps.executeUpdate();
            System.out.println("Utilisateur supprimé avec succès");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression de l'utilisateur : " + e.getMessage());
        }
    }

    @Override
    public void modifier(User user) {
        String sqlWithFace = "UPDATE user SET cin = ?, profile_picture = ?, nom = ?, prenom = ?, date_naissance = ?, " +
                "telephone_user = ?, email_user = ?, adresse_user = ?, password = ?, statut_compte = ?, role_systeme = ?, " +
                "face_login_enabled = ?, face_enrolled_at = ?, face_last_verified_at = ?, face_failed_attempts = ?, face_locked_until = ?, face_reference_embedding = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sqlWithFace)) {
            ps.setString(1, user.getCin());
            ps.setString(2, user.getProfilePicture());
            ps.setString(3, user.getNom());
            ps.setString(4, user.getPrenom());

            if (user.getDateNaissance() != null) {
                ps.setDate(5, Date.valueOf(user.getDateNaissance()));
            } else {
                ps.setNull(5, Types.DATE);
            }

            ps.setString(6, user.getTelephoneUser());
            ps.setString(7, user.getEmailUser());
            ps.setString(8, user.getAdresseUser());
            ps.setString(9, user.getPassword());
            ps.setString(10, user.getStatutCompte());
            ps.setString(11, user.getRoleSysteme());
            ps.setBoolean(12, user.isFaceLoginEnabled());
            if (user.getFaceEnrolledAt() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(user.getFaceEnrolledAt()));
            } else {
                ps.setNull(13, Types.TIMESTAMP);
            }
            if (user.getFaceLastVerifiedAt() != null) {
                ps.setTimestamp(14, Timestamp.valueOf(user.getFaceLastVerifiedAt()));
            } else {
                ps.setNull(14, Types.TIMESTAMP);
            }
            ps.setInt(15, user.getFaceFailedAttempts());
            if (user.getFaceLockedUntil() != null) {
                ps.setTimestamp(16, Timestamp.valueOf(user.getFaceLockedUntil()));
            } else {
                ps.setNull(16, Types.TIMESTAMP);
            }
            ps.setString(17, user.getFaceReferenceEmbedding());
            ps.setInt(18, user.getId());

            ps.executeUpdate();
            System.out.println("Utilisateur modifié avec succès");
        } catch (SQLException e) {
            String legacySql = "UPDATE user SET cin = ?, profile_picture = ?, nom = ?, prenom = ?, date_naissance = ?, " +
                    "telephone_user = ?, email_user = ?, adresse_user = ?, password = ?, statut_compte = ?, role_systeme = ? " +
                    "WHERE id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(legacySql)) {
                ps.setString(1, user.getCin());
                ps.setString(2, user.getProfilePicture());
                ps.setString(3, user.getNom());
                ps.setString(4, user.getPrenom());
                if (user.getDateNaissance() != null) {
                    ps.setDate(5, Date.valueOf(user.getDateNaissance()));
                } else {
                    ps.setNull(5, Types.DATE);
                }
                ps.setString(6, user.getTelephoneUser());
                ps.setString(7, user.getEmailUser());
                ps.setString(8, user.getAdresseUser());
                ps.setString(9, user.getPassword());
                ps.setString(10, user.getStatutCompte());
                ps.setString(11, user.getRoleSysteme());
                ps.setInt(12, user.getId());
                ps.executeUpdate();
                System.out.println("Utilisateur modifié (mode legacy, colonnes face_* absentes)");
            } catch (SQLException legacyEx) {
                System.out.println("Erreur lors de la modification de l'utilisateur : " + legacyEx.getMessage());
            }
        }
    }

    @Override
    public List<User> recuperer() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme FROM user";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                users.add(user);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des utilisateurs : " + e.getMessage());
        }
        return users;
    }

    @Override
    public User recupererParId(int id) {
        return null;
    }


    public User findById(int id) {
        String sql = "SELECT id, cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    enrichFaceDataIfAvailable(user);
                    return user;
                }
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la recherche de l'utilisateur : " + e.getMessage());
        }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT id, cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme, type_staff FROM user WHERE LOWER(email_user) = LOWER(?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = mapResultSetToUser(rs);
                user.setTypeStaff(readOptionalString(rs, "type_staff", "typeStaff"));
                enrichFaceDataIfAvailable(user);
                return user;
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la recherche par email : " + e.getMessage());
            return null;
        }
    }

    public User authenticate(String email, String plainPassword) {
        String sql = "SELECT id, cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme, type_staff FROM user WHERE LOWER(email_user) = LOWER(?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String hashedPassword = rs.getString("password");
                if (hashedPassword == null || plainPassword == null) {
                    return null;
                }
                boolean passwordMatches;
                String checkHash = hashedPassword;
                if (checkHash.startsWith("$2b$") || checkHash.startsWith("$2y$")) {
                    checkHash = "$2a$" + checkHash.substring(4);
                }
                try {
                    passwordMatches = BCrypt.checkpw(plainPassword, checkHash);
                } catch (IllegalArgumentException e) {
                    passwordMatches = hashedPassword.equals(plainPassword);
                }
                if (!passwordMatches) {
                    return null;
                }

                User user = mapResultSetToUser(rs);
                user.setTypeStaff(readOptionalString(rs, "type_staff", "typeStaff"));
                enrichFaceDataIfAvailable(user);
                return user;
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'authentification : " + e.getMessage());
            return null;
        }
    }

    public boolean updateStatutCompte(int userId, String statutCompte) {
        String sql = "UPDATE user SET statut_compte = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, statutCompte);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Erreur lors de la mise a jour du statut compte : " + e.getMessage());
            return false;
        }
    }

    public boolean updateFaceEnrollment(int userId, String embedding) {
        if (userId <= 0) {
            lastFaceEnrollmentError = "ID utilisateur invalide pendant l'enregistrement du visage.";
            return false;
        }
        if (embedding == null || embedding.isBlank()) {
            lastFaceEnrollmentError = "Donnees biométriques vides.";
            return false;
        }

        String sql = "UPDATE user SET face_login_enabled = ?, face_enrolled_at = ?, face_reference_embedding = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBoolean(1, true);
            ps.setTimestamp(2, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(3, buildFaceEmbeddingPayload(embedding));
            ps.setInt(4, userId);
            int updated = ps.executeUpdate();
            if (updated <= 0) {
                lastFaceEnrollmentError = "Aucune ligne mise a jour pour l'utilisateur id=" + userId + ".";
                return false;
            }
            lastFaceEnrollmentError = null;
            return true;
        } catch (SQLException e) {
            lastFaceEnrollmentError = "Erreur SQL enrollment facial: " + e.getMessage();
            System.out.println(lastFaceEnrollmentError);
            return false;
        }
    }

    private String buildFaceEmbeddingPayload(String embedding) {
        String escaped = embedding.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"embedding\":\"" + escaped + "\"}";
    }

    public String getLastFaceEnrollmentError() {
        return lastFaceEnrollmentError;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setCin(rs.getString("cin"));
        user.setProfilePicture(rs.getString("profile_picture"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));

        Date dateNaissance = rs.getDate("date_naissance");
        if (dateNaissance != null) {
            user.setDateNaissance(dateNaissance.toLocalDate());
        }

        user.setTelephoneUser(rs.getString("telephone_user"));
        user.setEmailUser(rs.getString("email_user"));
        user.setAdresseUser(rs.getString("adresse_user"));
        user.setPassword(rs.getString("password"));
        user.setVerified(rs.getBoolean("is_verified"));
        user.setStatutCompte(rs.getString("statut_compte"));
        user.setRoleSysteme(rs.getString("role_systeme"));
        user.setTypeStaff(readOptionalString(rs, "type_staff", "typeStaff"));
        user.setStaffRequestStatus(readOptionalString(rs, "staff_request_status", "staffRequestStatus"));
        user.setStaffRequestType(readOptionalString(rs, "staff_request_type", "staffRequestType"));
        user.setStaffRequestMessage(readOptionalString(rs, "staff_request_message", "staffRequestMessage"));
        user.setStaffRequestedAt(readOptionalTimestamp(rs, "staff_requested_at", "staffRequestedAt"));
        user.setStaffRequestProofPath(readOptionalString(rs, "staff_request_proof_path", "staffRequestProofPath"));
        user.setStaffDocuments(readOptionalString(rs, "staff_documents", "staffDocuments"));
        user.setStaffRequestReason(readOptionalString(rs, "staff_request_reason", "staffRequestReason"));
        return user;
    }

    private String readOptionalString(ResultSet rs, String... columnNames) {
        for (String columnName : columnNames) {
            try {
                return rs.getString(columnName);
            } catch (SQLException ignored) {
            }
        }
        return null;
    }

    private LocalDateTime readOptionalTimestamp(ResultSet rs, String... columnNames) {
        for (String columnName : columnNames) {
            try {
                Timestamp timestamp = rs.getTimestamp(columnName);
                if (timestamp != null) {
                    return timestamp.toLocalDateTime();
                }
            } catch (SQLException ignored) {
            }
        }
        return null;
    }

    private void enrichFaceDataIfAvailable(User user) {
        if (user == null) {
            return;
        }

        String sql = "SELECT face_login_enabled, face_enrolled_at, face_last_verified_at, face_failed_attempts, face_locked_until, face_reference_embedding FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                user.setFaceLoginEnabled(rs.getBoolean("face_login_enabled"));
                Timestamp enrolledAt = rs.getTimestamp("face_enrolled_at");
                if (enrolledAt != null) {
                    user.setFaceEnrolledAt(enrolledAt.toLocalDateTime());
                }
                Timestamp lastVerified = rs.getTimestamp("face_last_verified_at");
                if (lastVerified != null) {
                    user.setFaceLastVerifiedAt(lastVerified.toLocalDateTime());
                }
                user.setFaceFailedAttempts(rs.getInt("face_failed_attempts"));
                Timestamp lockedUntil = rs.getTimestamp("face_locked_until");
                if (lockedUntil != null) {
                    user.setFaceLockedUntil(lockedUntil.toLocalDateTime());
                }
                user.setFaceReferenceEmbedding(normalizeFaceEmbedding(rs.getString("face_reference_embedding")));
            }
        } catch (SQLException ignored) {
        }
    }

    private String normalizeFaceEmbedding(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return storedValue;
        }

        String marker = "\"embedding\":\"";
        int idx = storedValue.indexOf(marker);
        if (idx >= 0) {
            int start = idx + marker.length();
            int end = storedValue.indexOf('"', start);
            if (end > start) {
                String raw = storedValue.substring(start, end);
                return raw.replace("\\\"", "\"").replace("\\\\", "\\");
            }
        }

        if (storedValue.length() >= 2 && storedValue.startsWith("\"") && storedValue.endsWith("\"")) {
            String raw = storedValue.substring(1, storedValue.length() - 1);
            return raw.replace("\\\"", "\"").replace("\\\\", "\\");
        }

        return storedValue;
    }

    public List<User> findPendingStaffRequests() {
        List<User> requests = new ArrayList<>();

        String snakeQuery = "SELECT id, cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme, type_staff, staff_request_status, staff_request_type, staff_request_message, staff_requested_at, staff_request_reason, staff_documents, staff_request_proof_path FROM user WHERE role_systeme = 'STAFF' AND (COALESCE(staff_request_status, 'PENDING') = 'PENDING' OR statut_compte = 'EN_ATTENTE_VALIDATION' OR is_verified = 0) ORDER BY staff_requested_at DESC, id DESC";
        String camelQuery = "SELECT id, cin, profilePicture AS profile_picture, nom, prenom, dateNaissance AS date_naissance, telephoneUser AS telephone_user, emailUser AS email_user, adresseUser AS adresse_user, password, isVerified AS is_verified, statutCompte AS statut_compte, roleSysteme AS role_systeme, typeStaff AS type_staff, staffRequestStatus AS staff_request_status, staffRequestType AS staff_request_type, staffRequestMessage AS staff_request_message, staffRequestedAt AS staff_requested_at, staffRequestReason AS staff_request_reason, staffDocuments AS staff_documents, staffRequestProofPath AS staff_request_proof_path FROM user WHERE roleSysteme = 'STAFF' AND (COALESCE(staffRequestStatus, 'PENDING') = 'PENDING' OR statutCompte = 'EN_ATTENTE_VALIDATION' OR isVerified = 0) ORDER BY staffRequestedAt DESC, id DESC";
        String legacyQuery = "SELECT id, cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme FROM user WHERE role_systeme = 'STAFF' AND (statut_compte = 'EN_ATTENTE_VALIDATION' OR is_verified = 0) ORDER BY id DESC";

        if (loadStaffRequestsWithQuery(requests, snakeQuery)) {
            return requests;
        }
        if (loadStaffRequestsWithQuery(requests, camelQuery)) {
            return requests;
        }
        loadStaffRequestsWithQuery(requests, legacyQuery);
        return requests;
    }

    private boolean loadStaffRequestsWithQuery(List<User> target, String sql) {
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            target.clear();
            while (rs.next()) {
                target.add(mapResultSetToUser(rs));
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean reviewStaffRequest(int userId, boolean approve, Integer reviewedBy) {
        String requestStatus = approve ? "APPROVED" : "REJECTED";
        String accountStatus = approve ? "ACTIF" : "REFUSE";

        String snakeUpdate = "UPDATE user SET is_verified = ?, statut_compte = ?, staff_request_status = ?, staff_reviewed_at = ?, staff_reviewed_by = ? WHERE id = ? AND role_systeme = 'STAFF'";
        String camelUpdate = "UPDATE user SET isVerified = ?, statutCompte = ?, staffRequestStatus = ?, staffReviewedAt = ?, staffReviewedBy = ? WHERE id = ? AND roleSysteme = 'STAFF'";
        String snakeLegacy = "UPDATE user SET is_verified = ?, statut_compte = ? WHERE id = ? AND role_systeme = 'STAFF'";
        String camelLegacy = "UPDATE user SET isVerified = ?, statutCompte = ? WHERE id = ? AND roleSysteme = 'STAFF'";

        if (executeReviewUpdate(snakeUpdate, userId, approve, accountStatus, requestStatus, reviewedBy, true)) {
            return true;
        }
        if (executeReviewUpdate(camelUpdate, userId, approve, accountStatus, requestStatus, reviewedBy, true)) {
            return true;
        }
        if (executeReviewUpdate(snakeLegacy, userId, approve, accountStatus, requestStatus, reviewedBy, false)) {
            return true;
        }
        return executeReviewUpdate(camelLegacy, userId, approve, accountStatus, requestStatus, reviewedBy, false);
    }

    private boolean executeReviewUpdate(
            String sql,
            int userId,
            boolean approve,
            String accountStatus,
            String requestStatus,
            Integer reviewedBy,
            boolean includeReviewColumns
    ) {
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setBoolean(1, approve);
            ps.setString(2, accountStatus);

            if (includeReviewColumns) {
                ps.setString(3, requestStatus);
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                if (reviewedBy == null) {
                    ps.setNull(5, Types.INTEGER);
                } else {
                    ps.setInt(5, reviewedBy);
                }
                ps.setInt(6, userId);
            } else {
                ps.setInt(3, userId);
            }

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Password reset
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Sauvegarde un token de réinitialisation du mot de passe pour l'utilisateur donné.
     *
     * @param userId    id de l'utilisateur
     * @param token     code / token à persister
     * @param expiresAt date d'expiration
     * @return true si la mise à jour a réussi
     */
    public boolean saveResetToken(int userId, String token, LocalDateTime expiresAt) {
        String snakeSql = "UPDATE user SET reset_token = ?, reset_token_expires_at = ? WHERE id = ?";
        String camelSql = "UPDATE user SET resetToken = ?, resetTokenExpiresAt = ? WHERE id = ?";
        for (String sql : new String[]{snakeSql, camelSql}) {
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, token);
                ps.setTimestamp(2, Timestamp.valueOf(expiresAt));
                ps.setInt(3, userId);
                if (ps.executeUpdate() > 0) return true;
            } catch (SQLException ignored) {
            }
        }
        return false;
    }

    /**
     * Recherche un utilisateur par son token de réinitialisation (non expiré).
     *
     * @param token le token à chercher
     * @return l'utilisateur si trouvé et token valide, null sinon
     */
    public User findByResetToken(String token) {
        if (token == null || token.isBlank()) return null;
        String snakeSql = "SELECT id, cin, profile_picture, nom, prenom, date_naissance, telephone_user, email_user, adresse_user, password, is_verified, statut_compte, role_systeme FROM user WHERE reset_token = ? AND reset_token_expires_at > NOW()";
        String camelSql = "SELECT id, cin, profilePicture AS profile_picture, nom, prenom, dateNaissance AS date_naissance, telephoneUser AS telephone_user, emailUser AS email_user, adresseUser AS adresse_user, password, isVerified AS is_verified, statutCompte AS statut_compte, roleSysteme AS role_systeme FROM user WHERE resetToken = ? AND resetTokenExpiresAt > NOW()";
        for (String sql : new String[]{snakeSql, camelSql}) {
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, token);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapResultSetToUser(rs);
                }
            } catch (SQLException ignored) {
            }
        }
        return null;
    }

    /**
     * Met à jour le mot de passe et efface le reset token.
     *
     * @param userId         id de l'utilisateur
     * @param hashedPassword mot de passe hashé (BCrypt)
     * @return true si la mise à jour a réussi
     */
    public boolean updatePasswordAndClearToken(int userId, String hashedPassword) {
        String snakeSql = "UPDATE user SET password = ?, reset_token = NULL, reset_token_expires_at = NULL WHERE id = ?";
        String camelSql = "UPDATE user SET password = ?, resetToken = NULL, resetTokenExpiresAt = NULL WHERE id = ?";
        for (String sql : new String[]{snakeSql, camelSql}) {
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setString(1, hashedPassword);
                ps.setInt(2, userId);
                if (ps.executeUpdate() > 0) return true;
            } catch (SQLException ignored) {
            }
        }
        return false;
    }

    public List<User> getStaffByRoleAndType(String roleSysteme, String typeStaff) {
        List<User> users = new ArrayList<>();

        String querySnakeCase = "SELECT id, nom, prenom, role_systeme, type_staff FROM user WHERE role_systeme = ? AND type_staff = ? ORDER BY nom, prenom";
        String queryCamelCase = "SELECT id, nom, prenom, roleSysteme, typeStaff FROM user WHERE roleSysteme = ? AND typeStaff = ? ORDER BY nom, prenom";

        boolean loaded = loadUsersWithQuery(users, querySnakeCase, roleSysteme, typeStaff, "role_systeme", "type_staff");
        if (!loaded) {
            loadUsersWithQuery(users, queryCamelCase, roleSysteme, typeStaff, "roleSysteme", "typeStaff");
        }

        return users;
    }

    private boolean loadUsersWithQuery(
            List<User> target,
            String sql,
            String roleSysteme,
            String typeStaff,
            String roleColumn,
            String typeColumn
    ) {
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, roleSysteme);
            ps.setString(2, typeStaff);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setNom(rs.getString("nom"));
                    user.setPrenom(rs.getString("prenom"));
                    user.setRoleSysteme(rs.getString(roleColumn));
                    user.setTypeStaff(rs.getString(typeColumn));
                    target.add(user);
                }
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

}
