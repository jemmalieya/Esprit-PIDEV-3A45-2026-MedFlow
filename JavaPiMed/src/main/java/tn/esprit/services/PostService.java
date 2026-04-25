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
    public boolean ajouter(Post p) {
        if (existeDeja(p)) {
            System.out.println("Ajout bloqué : post en double.");
            return false;
        }
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

            int rows = ps.executeUpdate();

            if (rows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }

                System.out.println("Ajout effectué : id=" + p.getId());
                return true;
            }

            return false;

        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout du post : " + e.getMessage());
            e.printStackTrace();
            return false;
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
    public boolean modifier(Post p) {
        if (existeDejaPourModification(p)) {
            System.out.println("Modification bloquée : post en double.");
            return false;
        }

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
            if (rows > 0) {
                System.out.println("Modification effectuée pour id=" + p.getId());
                return true;
            } else {
                System.out.println("Aucune modification (id introuvable)");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
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

    // Retourne la liste de tous les posts avec l'utilisateur renseigné (chargement basique par id)
    public List<Post> getAllPosts() {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM post";
        try {
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Post p = new Post();
                p.setId(rs.getInt("id"));
                int userId = rs.getInt("user_id");
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
                // Charger l'utilisateur minimalement (par id)
                tn.esprit.entities.User user = new tn.esprit.entities.User();
                user.setId(userId);
                p.setUser(user);
                posts.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    public boolean existeDeja(Post post) {
        String sql = "SELECT COUNT(*) FROM post " +
                "WHERE titre = ? " +
                "AND contenu = ? " +
                "AND categorie = ? " +
                "AND localisation = ? " +
                "AND img_post = ? " +
                "AND hashtags = ? " +
                "AND humeur = ? " +
                "AND est_anonyme = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, post.getTitre());
            ps.setString(2, post.getContenu());
            ps.setString(3, post.getCategorie());
            ps.setString(4, post.getLocalisation());
            ps.setString(5, post.getImg_post());
            ps.setString(6, post.getHashtags());
            ps.setString(7, post.getHumeur());
            ps.setBoolean(8, post.isEst_anonyme());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
    public boolean existeDejaPourModification(Post post) {
        String sql = "SELECT COUNT(*) FROM post " +
                "WHERE titre = ? " +
                "AND contenu = ? " +
                "AND categorie = ? " +
                "AND localisation = ? " +
                "AND img_post = ? " +
                "AND hashtags = ? " +
                "AND humeur = ? " +
                "AND est_anonyme = ? " +
                "AND id <> ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, post.getTitre());
            ps.setString(2, post.getContenu());
            ps.setString(3, post.getCategorie());
            ps.setString(4, post.getLocalisation());
            ps.setString(5, post.getImg_post());
            ps.setString(6, post.getHashtags());
            ps.setString(7, post.getHumeur());
            ps.setBoolean(8, post.isEst_anonyme());
            ps.setInt(9, post.getId());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<Post> getApprovedPosts() {
        List<Post> posts = new ArrayList<>();

        String sql ="SELECT p.*, u.nom AS user_nom, u.prenom AS user_prenom, u.email_user AS user_email " +
                "FROM post p " +
                "LEFT JOIN user u ON p.user_id = u.id " +
                "WHERE p.is_approved = 1 AND p.moderation_status = 'approved' " +
                "ORDER BY p.date_creation DESC";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                posts.add(mapPostWithUser(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return posts;
    }

    public List<Post> getPendingPostsByUser(int userId) {
        List<Post> posts = new ArrayList<>();

        String sql = "SELECT p.*, u.nom AS user_nom, u.prenom AS user_prenom, u.email_user AS user_email " +
                "FROM post p " +
                "LEFT JOIN user u ON p.user_id = u.id " +
                "WHERE p.user_id = ? AND p.moderation_status = 'pending' " +
                "ORDER BY p.date_creation DESC";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                posts.add(mapPostWithUser(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return posts;
    }

    public List<Post> getPendingPostsForAdmin() {
        List<Post> posts = new ArrayList<>();

        String sql = "SELECT p.*, u.nom AS user_nom, u.prenom AS user_prenom, u.email_user AS user_email " +
                "FROM post p " +
                "LEFT JOIN user u ON p.user_id = u.id " +
                "WHERE p.moderation_status = 'pending' " +
                "ORDER BY p.date_creation DESC";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                posts.add(mapPostWithUser(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return posts;
    }

    public boolean approvePost(int postId) {
        String sql = "UPDATE post SET " +
                "is_approved = 1, " +
                "moderation_status = 'approved', " +
                "moderation_message = 'Votre post a été approuvé par l’administrateur. Il est maintenant visible dans le blog.', " +
                "moderation_seen = 0 " +
                "WHERE id = ?";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, postId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean rejectPost(int postId) {
        String sql = "UPDATE post SET " +
                "is_approved = 0, " +
                "moderation_status = 'rejected', " +
                "moderation_message = 'Votre post a été refusé par l’administrateur. Merci de le modifier puis de le renvoyer pour validation.', " +
                "moderation_seen = 0 " +
                "WHERE id = ?";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, postId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Post mapPostWithUser(ResultSet rs) throws SQLException {
        Post p = new Post();

        p.setId(rs.getInt("id"));
        p.setTitre(rs.getString("titre"));
        p.setContenu(rs.getString("contenu"));
        p.setLocalisation(rs.getString("localisation"));
        p.setImg_post(rs.getString("img_post"));
        p.setHashtags(rs.getString("hashtags"));
        p.setVisibilite(rs.getString("visibilite"));

        Timestamp tsCreation = rs.getTimestamp("date_creation");
        p.setDate_creation(tsCreation != null ? tsCreation.toLocalDateTime() : null);

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

        tn.esprit.entities.User user = new tn.esprit.entities.User();
        user.setId(rs.getInt("user_id"));

        try {
            user.setNom(rs.getString("user_nom"));
        } catch (Exception ignored) {
        }

        try {
            user.setPrenom(rs.getString("user_prenom"));
        } catch (Exception ignored) {
        }
        try {
            user.setEmailUser(rs.getString("user_email"));
        } catch (Exception ignored) {
        }

        p.setUser(user);

        return p;
    }
    public List<Post> getUnseenModerationNotificationsByUser(int userId) {
        List<Post> posts = new ArrayList<>();

        String sql = "SELECT p.*, u.nom AS user_nom, u.prenom AS user_prenom, u.email_user AS user_email " +
                "FROM post p " +
                "LEFT JOIN user u ON p.user_id = u.id " +
                "WHERE p.user_id = ? " +
                "AND p.moderation_seen = 0 " +
                "AND p.moderation_status IN ('approved', 'rejected') " +
                "ORDER BY p.date_modification DESC, p.date_creation DESC";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                posts.add(mapPostWithUser(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return posts;
    }
    public int countUnseenModerationNotificationsByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM post " +
                "WHERE user_id = ? " +
                "AND moderation_seen = 0 " +
                "AND moderation_status IN ('approved', 'rejected')";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
    public boolean markModerationNotificationAsSeen(int postId) {
        String sql = "UPDATE post SET moderation_seen = 1 WHERE id = ?";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, postId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean markAllModerationNotificationsAsSeen(int userId) {
        String sql = "UPDATE post SET moderation_seen = 1 " +
                "WHERE user_id = ? " +
                "AND moderation_status IN ('approved', 'rejected')";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, userId);

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}