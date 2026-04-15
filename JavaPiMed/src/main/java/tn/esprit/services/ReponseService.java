package tn.esprit.services;

import tn.esprit.entities.ReponseReclamation;
import tn.esprit.entities.Reclamation;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
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

        String sql = "INSERT INTO reponse_reclamation " +
                "(id_reclamation, message, type_reponse, date_creation_rep, date_modification_rep, is_read) " +
                "VALUES (?,?,?,?,?,?)";

        try {
            PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            // ✅ CORRECTION ICI
            ps.setInt(1, r.getReclamation().getId_reclamation());
            ps.setString(2, r.getMessage());
            ps.setString(3, r.getType_reponse());
            ps.setTimestamp(4, Timestamp.valueOf(r.getDate_creation_rep()));
            ps.setTimestamp(5, Timestamp.valueOf(r.getDate_modification_rep()));
            ps.setBoolean(6, r.isIs_read());

            ps.executeUpdate();

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
    // RECUPERER TOUT
    // =========================
    public List<ReponseReclamation> recuperer() {

        List<ReponseReclamation> list = new ArrayList<>();
        String sql = "SELECT * FROM reponse_reclamation";

        try {
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                Reclamation reclamation = new Reclamation();
                reclamation.setId_reclamation(rs.getInt("id_reclamation"));

                ReponseReclamation r = new ReponseReclamation();

                r.setId_reponse(rs.getInt("id_reponse"));
                r.setReclamation(reclamation); // ✅ FIX
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
    // FIND BY RECLAMATION ID
    // =========================
    public List<ReponseReclamation> getByReclamationId(int idReclamation) {

        List<ReponseReclamation> list = new ArrayList<>();

        String sql = "SELECT * FROM reponse_reclamation WHERE id_reclamation = ?";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, idReclamation);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Reclamation reclamation = new Reclamation();
                reclamation.setId_reclamation(idReclamation);

                ReponseReclamation r = new ReponseReclamation();

                r.setId_reponse(rs.getInt("id_reponse"));
                r.setReclamation(reclamation); // ✅ FIX
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

            ps.executeUpdate();

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
            ps.executeUpdate();

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
        return null;
    }
}