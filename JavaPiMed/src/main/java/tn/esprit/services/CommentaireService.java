package tn.esprit.services;

import tn.esprit.entities.Commentaire;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentaireService {

    private Connection cn;

    public CommentaireService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    // =========================
    // AJOUT
    // =========================
    public void ajouter(Commentaire c) {
        String sql = "INSERT INTO commentaire(post_id, user_id, contenu, date_creation, est_anonyme, parametres_confidentialite, status, moderation_score, moderation_label, moderated_at) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, c.getPost_id());
            ps.setInt(2, c.getUser_id());
            ps.setString(3, c.getContenu());
            ps.setTimestamp(4, Timestamp.valueOf(c.getDate_creation()));
            ps.setBoolean(5, c.isEst_anonyme());
            ps.setString(6, c.getParametres_confidentialite());
            ps.setString(7, c.getStatus());
            if (c.getModeration_score() != null) ps.setDouble(8, c.getModeration_score());
            else ps.setNull(8, Types.DOUBLE);
            ps.setString(9, c.getModeration_label());
            if (c.getModerated_at() != null) ps.setTimestamp(10, Timestamp.valueOf(c.getModerated_at()));
            else ps.setNull(10, Types.TIMESTAMP);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) c.setId(rs.getInt(1));

            System.out.println("Ajout effectué : id=" + c.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // RECUPERER
    // =========================
    public List<Commentaire> recuperer() {
        List<Commentaire> list = new ArrayList<>();
        String sql = "SELECT * FROM commentaire";
        try {
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Commentaire c = new Commentaire();
                c.setId(rs.getInt("id"));
                c.setPost_id(rs.getInt("post_id"));
                c.setUser_id(rs.getInt("user_id"));
                c.setContenu(rs.getString("contenu"));
                c.setDate_creation(rs.getTimestamp("date_creation").toLocalDateTime());
                c.setEst_anonyme(rs.getBoolean("est_anonyme"));
                c.setParametres_confidentialite(rs.getString("parametres_confidentialite"));
                c.setStatus(rs.getString("status"));
                c.setModeration_score(rs.getObject("moderation_score", Double.class));
                c.setModeration_label(rs.getString("moderation_label"));
                Timestamp ts = rs.getTimestamp("moderated_at");
                c.setModerated_at(ts != null ? ts.toLocalDateTime() : null);

                list.add(c);
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
            if (rows > 0) System.out.println("Modification effectuée pour id=" + c.getId());
            else System.out.println("Aucune modification (id introuvable)");
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
            if (rows > 0) System.out.println("Suppression effectuée pour id=" + c.getId());
            else System.out.println("Aucune suppression (id introuvable)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // FIND BY ID
    // =========================
    public Commentaire findById(int id) {
        for (Commentaire c : recuperer()) {
            if (c.getId() == id) return c;
        }
        System.out.println("❌ Commentaire avec id=" + id + " non trouvé");
        return null;
    }
}