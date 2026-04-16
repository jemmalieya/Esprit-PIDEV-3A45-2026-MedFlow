package tn.esprit.services;

import tn.esprit.entities.Post;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostService {

    private Connection cn;

    public PostService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    // =========================
    // AJOUT
    // =========================
    public void ajouter(Post p) {
        String sql = "INSERT INTO post(user_id, titre, contenu, localisation, img_post, hashtags, visibilite, date_creation, est_anonyme, categorie, humeur, nbr_reactions, nbr_commentaires, is_approved, moderation_status, moderation_message, moderation_seen) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, p.getUser_id());
            ps.setString(2, p.getTitre());
            ps.setString(3, p.getContenu());
            ps.setString(4, p.getLocalisation());
            ps.setString(5, p.getImg_post());
            ps.setString(6, p.getHashtags());
            ps.setString(7, p.getVisibilite());
            ps.setTimestamp(8, Timestamp.valueOf(p.getDate_creation()));
            ps.setBoolean(9, p.isEst_anonyme());
            ps.setString(10, p.getCategorie());
            ps.setString(11, p.getHumeur());
            ps.setInt(12, p.getNbr_reactions());
            ps.setInt(13, p.getNbr_commentaires());
            ps.setBoolean(14, p.isIs_approved());
            ps.setString(15, p.getModeration_status());
            ps.setString(16, p.getModeration_message());
            ps.setBoolean(17, p.isModeration_seen());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                p.setId(rs.getInt(1));
            }

            System.out.println("Ajout effectué : id=" + p.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // RECUPERER
    // =========================
    public List<Post> recuperer() {
        List<Post> list = new ArrayList<>();
        String sql = "SELECT * FROM post";
        try {
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Post p = new Post();
                p.setId(rs.getInt("id"));
                p.setUser_id(rs.getInt("user_id"));
                p.setTitre(rs.getString("titre"));
                p.setContenu(rs.getString("contenu"));
                p.setLocalisation(rs.getString("localisation"));
                p.setImg_post(rs.getString("img_post"));
                p.setHashtags(rs.getString("hashtags"));
                p.setVisibilite(rs.getString("visibilite"));
                p.setDate_creation(rs.getTimestamp("date_creation").toLocalDateTime());
                Timestamp tsModif = rs.getTimestamp("date_modification");
                p.setDate_modification(tsModif != null ? tsModif.toLocalDateTime() : null);
                p.setEst_anonyme(rs.getBoolean("est_anonyme"));
                p.setCategorie(rs.getString("categorie"));
                p.setHumeur(rs.getString("humeur"));
                p.setNbr_reactions(rs.getInt("nbr_reactions"));
                p.setNbr_commentaires(rs.getInt("nbr_commentaires"));
                p.setIs_approved(rs.getBoolean("is_approved"));
                p.setModeration_status(rs.getString("moderation_status"));
                p.setModeration_message(rs.getString("moderation_message"));
                p.setModeration_seen(rs.getBoolean("moderation_seen"));

                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =========================
    // MODIFIER
    // =========================
    public void modifier(Post p) {
        String sql = "UPDATE post SET titre=?, contenu=?, localisation=?, img_post=?, hashtags=?, visibilite=?, date_modification=?, est_anonyme=?, categorie=?, humeur=?, nbr_reactions=?, nbr_commentaires=?, is_approved=?, moderation_status=?, moderation_message=?, moderation_seen=? WHERE id=?";
        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setString(1, p.getTitre());
            ps.setString(2, p.getContenu());
            ps.setString(3, p.getLocalisation());
            ps.setString(4, p.getImg_post());
            ps.setString(5, p.getHashtags());
            ps.setString(6, p.getVisibilite());
            ps.setTimestamp(7, Timestamp.valueOf(p.getDate_modification()));
            ps.setBoolean(8, p.isEst_anonyme());
            ps.setString(9, p.getCategorie());
            ps.setString(10, p.getHumeur());
            ps.setInt(11, p.getNbr_reactions());
            ps.setInt(12, p.getNbr_commentaires());
            ps.setBoolean(13, p.isIs_approved());
            ps.setString(14, p.getModeration_status());
            ps.setString(15, p.getModeration_message());
            ps.setBoolean(16, p.isModeration_seen());
            ps.setInt(17, p.getId());

            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Modification effectuée pour id=" + p.getId());
            else System.out.println("Aucune modification (id introuvable)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // SUPPRIMER
    // =========================
    public void supprimer(Post p) {
        String sql = "DELETE FROM post WHERE id=?";
        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, p.getId());
            int rows = ps.executeUpdate();
            if (rows > 0) System.out.println("Suppression effectuée pour id=" + p.getId());
            else System.out.println("Aucune suppression (id introuvable)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // FIND BY ID
    // =========================
    public Post findById(int id) {
        for (Post p : recuperer()) {
            if (p.getId() == id) return p;
        }
        System.out.println("❌ Post avec id=" + id + " non trouvé");
        return null;
    }
}