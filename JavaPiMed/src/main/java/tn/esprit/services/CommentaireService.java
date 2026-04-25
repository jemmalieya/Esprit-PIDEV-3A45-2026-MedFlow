package tn.esprit.services;

import tn.esprit.entities.Commentaire;
import tn.esprit.entities.Post;
import tn.esprit.entities.User;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import tn.esprit.services.CommentModerationService.ModerationResult;

public class CommentaireService {

    private Connection cn;
    private final CommentModerationService commentModerationService = new CommentModerationService();

    public CommentaireService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    // =========================
    // AJOUT
    // =========================
    public boolean ajouter(Commentaire c) {
        if (c == null) {
            System.out.println("Ajout commentaire refusé : commentaire null.");
            return false;
        }

        if (c.getPost() == null || c.getPost().getId() <= 0) {
            System.out.println("Ajout commentaire refusé : post invalide.");
            return false;
        }

        if (c.getUser() == null || c.getUser().getId() <= 0) {
            System.out.println("Ajout commentaire refusé : utilisateur invalide.");
            return false;
        }

        if (c.getContenu() == null || c.getContenu().trim().isEmpty()) {
            System.out.println("Ajout commentaire refusé : contenu vide.");
            return false;
        }

        ModerationResult moderation = commentModerationService.analyzeComment(c.getContenu());

        c.setModeration_score(moderation.getScore());
        c.setModeration_label(moderation.getLabel());
        c.setModerated_at(LocalDateTime.now());

        if (moderation.isBlocked()) {
            c.setStatus("blocked");

            System.out.println(
                    "Commentaire bloqué par Perspective API : label=" +
                            moderation.getLabel() +
                            ", score=" +
                            moderation.getScore()
            );

            return false;
        }

        c.setStatus("approved");

        if (c.getDate_creation() == null) {
            c.setDate_creation(LocalDateTime.now());
        }

        if (c.getParametres_confidentialite() == null || c.getParametres_confidentialite().isBlank()) {
            c.setParametres_confidentialite("Public");
        }

        String sql = "INSERT INTO commentaire(post_id, user_id, contenu, date_creation, est_anonyme, parametres_confidentialite, status, moderation_score, moderation_label, moderated_at) VALUES(?,?,?,?,?,?,?,?,?,?)";

        try {
            PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, c.getPost().getId());
            ps.setInt(2, c.getUser().getId());
            ps.setString(3, c.getContenu());
            ps.setTimestamp(4, Timestamp.valueOf(c.getDate_creation()));
            ps.setBoolean(5, c.isEst_anonyme());
            ps.setString(6, c.getParametres_confidentialite());
            ps.setString(7, c.getStatus());

            if (c.getModeration_score() != null) {
                ps.setDouble(8, c.getModeration_score());
            } else {
                ps.setNull(8, Types.DOUBLE);
            }

            ps.setString(9, c.getModeration_label());

            if (c.getModerated_at() != null) {
                ps.setTimestamp(10, Timestamp.valueOf(c.getModerated_at()));
            } else {
                ps.setNull(10, Types.TIMESTAMP);
            }

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if (rs.next()) {
                c.setId(rs.getInt(1));
            }

            System.out.println("Ajout commentaire effectué : id=" + c.getId());
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // =========================
    // RECUPERER TOUS
    // =========================
    public List<Commentaire> recuperer() {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT c.*, u.nom AS user_nom, u.prenom AS user_prenom " +
                "FROM commentaire c " +
                "LEFT JOIN user u ON c.user_id = u.id " +
                "ORDER BY c.date_creation DESC";

        try {
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                list.add(mapCommentaire(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================
    // RECUPERER PAR POST
    // =========================
    public List<Commentaire> recupererParPost(int postId) {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT c.*, u.nom AS user_nom, u.prenom AS user_prenom " +
                "FROM commentaire c " +
                "LEFT JOIN user u ON c.user_id = u.id " +
                "WHERE c.post_id = ? " +
                "ORDER BY c.date_creation DESC";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapCommentaire(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================
    // MODIFIER
    // =========================
    public void modifier(Commentaire c) {
        String sql = "UPDATE commentaire SET contenu=?, est_anonyme=?, parametres_confidentialite=?, status=?, moderation_score=?, moderation_label=?, moderated_at=? WHERE id=?";
        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setString(1, c.getContenu());
            ps.setBoolean(2, c.isEst_anonyme());
            ps.setString(3, c.getParametres_confidentialite());
            ps.setString(4, c.getStatus());

            if (c.getModeration_score() != null) ps.setDouble(5, c.getModeration_score());
            else ps.setNull(5, Types.DOUBLE);

            ps.setString(6, c.getModeration_label());

            if (c.getModerated_at() != null) ps.setTimestamp(7, Timestamp.valueOf(c.getModerated_at()));
            else ps.setNull(7, Types.TIMESTAMP);

            ps.setInt(8, c.getId());

            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Modification commentaire effectuée pour id=" + c.getId());
            else System.out.println("Aucune modification commentaire (id introuvable)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // SUPPRIMER
    // =========================
    public void supprimer(Commentaire c) {
        String sql = "DELETE FROM commentaire WHERE id=?";
        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, c.getId());
            int rows = ps.executeUpdate();

            if (rows > 0) System.out.println("Suppression commentaire effectuée pour id=" + c.getId());
            else System.out.println("Aucune suppression commentaire (id introuvable)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // FIND BY ID
    // =========================
    public Commentaire findById(int id) {
        String sql = "SELECT c.*, u.nom AS user_nom, u.prenom AS user_prenom " +
                "FROM commentaire c " +
                "LEFT JOIN user u ON c.user_id = u.id " +
                "WHERE c.id = ?";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapCommentaire(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Commentaire avec id=" + id + " non trouvé");
        return null;
    }

    // =========================
    // MAPPING
    // =========================
    private Commentaire mapCommentaire(ResultSet rs) throws SQLException {
        Commentaire c = new Commentaire();
        c.setId(rs.getInt("id"));

        Post post = new Post();
        post.setId(rs.getInt("post_id"));
        c.setPost(post);

        User user = new User();
        user.setId(rs.getInt("user_id"));
        try {
            user.setNom(rs.getString("user_nom"));
        } catch (Exception ignored) {
        }
        try {
            user.setPrenom(rs.getString("user_prenom"));
        } catch (Exception ignored) {
        }
        c.setUser(user);

        c.setContenu(rs.getString("contenu"));
        c.setDate_creation(rs.getTimestamp("date_creation").toLocalDateTime());
        c.setEst_anonyme(rs.getBoolean("est_anonyme"));
        c.setParametres_confidentialite(rs.getString("parametres_confidentialite"));
        c.setStatus(rs.getString("status"));
        c.setModeration_score(rs.getObject("moderation_score", Double.class));
        c.setModeration_label(rs.getString("moderation_label"));

        Timestamp ts = rs.getTimestamp("moderated_at");
        c.setModerated_at(ts != null ? ts.toLocalDateTime() : null);

        return c;
    }

    public boolean commentaireAccepteParModeration(Commentaire commentaire) {
        try {
            String contenu = commentaire.getContenu();

            if (contenu == null || contenu.trim().isEmpty()) {
                return false;
            }

            ModerationResult moderation = commentModerationService.analyzeComment(contenu);

            commentaire.setModeration_score(moderation.getScore());
            commentaire.setModeration_label(moderation.getLabel());
            commentaire.setModerated_at(LocalDateTime.now());

            if (moderation.isBlocked()) {
                commentaire.setStatus("blocked");

                System.out.println(
                        "Modification commentaire bloquée par Perspective API : label=" +
                                moderation.getLabel() +
                                ", score=" +
                                moderation.getScore()
                );

                return false;
            }

            commentaire.setStatus("approved");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}