package tn.esprit.services;

import tn.esprit.entities.ReponseReclamation;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReponseService {

    private Connection cn;

    public ReponseService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    // =========================
    // AJOUT
    // =========================
    public void ajouter(ReponseReclamation r) {
        String sql = "INSERT INTO reponse_reclamation(id_reclamation, message, type_reponse, date_creation_rep, date_modification_rep, is_read) VALUES(?,?,?,?,?,?)";
        try {
            PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, r.getId_reclamation());
            ps.setString(2, r.getMessage());
            ps.setString(3, r.getType_reponse());
            ps.setTimestamp(4, Timestamp.valueOf(r.getDate_creation_rep()));
            ps.setTimestamp(5, Timestamp.valueOf(r.getDate_modification_rep()));
            ps.setBoolean(6, r.isIs_read());

            ps.executeUpdate();

            // récupérer l'id généré
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                r.setId_reponse(rs.getInt(1));
            }

            System.out.println("Ajout effectué : " + r.getId_reponse());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // RECUPERER
    // =========================
    public List<ReponseReclamation> recuperer() {
        List<ReponseReclamation> list = new ArrayList<>();
        String sql = "SELECT * FROM reponse_reclamation";
        try {
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                ReponseReclamation r = new ReponseReclamation();
                r.setId_reponse(rs.getInt("id_reponse"));
                r.setId_reclamation(rs.getInt("id_reclamation"));
                r.setMessage(rs.getString("message"));
                r.setType_reponse(rs.getString("type_reponse"));
                r.setDate_creation_rep(rs.getTimestamp("date_creation_rep").toLocalDateTime());
                r.setDate_modification_rep(rs.getTimestamp("date_modification_rep").toLocalDateTime());
                r.setIs_read(rs.getBoolean("is_read"));

                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // =========================
    // MODIFIER
    // =========================
    public void modifier(ReponseReclamation r) {
        String sql = "UPDATE reponse_reclamation SET message=?, type_reponse=?, date_modification_rep=?, is_read=? WHERE id_reponse=?";
        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setString(1, r.getMessage());
            ps.setString(2, r.getType_reponse());
            ps.setTimestamp(3, Timestamp.valueOf(r.getDate_modification_rep()));
            ps.setBoolean(4, r.isIs_read());
            ps.setInt(5, r.getId_reponse());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Modification effectuée pour id_reponse=" + r.getId_reponse());
            } else {
                System.out.println("Aucune modification (id introuvable)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // SUPPRIMER
    // =========================
    public void supprimer(ReponseReclamation r) {
        String sql = "DELETE FROM reponse_reclamation WHERE id_reponse=?";
        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, r.getId_reponse());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Suppression effectuée pour id_reponse=" + r.getId_reponse());
            } else {
                System.out.println("Aucune suppression (id introuvable)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // FIND BY ID
    // =========================
    public ReponseReclamation findById(int id) {
        for (ReponseReclamation r : recuperer()) {
            if (r.getId_reponse() == id) {
                return r;
            }
        }
        System.out.println("❌ Réponse avec id_reponse=" + id + " non trouvée");
        return null;
    }
}