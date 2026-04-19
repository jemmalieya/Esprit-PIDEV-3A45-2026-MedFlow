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
                "(id_reclamation, message, type_reponse, date_creation_rep, date_modification_rep, auteur, role_emetteur, lu_par_admin, lu_par_patient) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";

        try {
            PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, r.getReclamation().getId_reclamation());
            ps.setString(2, r.getMessage());
            ps.setString(3, r.getType_reponse());
            ps.setTimestamp(4, Timestamp.valueOf(r.getDate_creation_rep()));
            ps.setTimestamp(5, Timestamp.valueOf(r.getDate_modification_rep()));
            ps.setString(6, r.getAuteur());
            ps.setString(7, r.getRole_emetteur());
            ps.setBoolean(8, r.isLu_par_admin());
            ps.setBoolean(9, r.isLu_par_patient());

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                r.setId_reponse(rs.getInt(1));
            }

            recalculerStatutReclamation(r.getReclamation().getId_reclamation());

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
                r.setReclamation(reclamation);
                r.setMessage(rs.getString("message"));
                r.setType_reponse(rs.getString("type_reponse"));

                Timestamp tsCreation = rs.getTimestamp("date_creation_rep");
                if (tsCreation != null) {
                    r.setDate_creation_rep(tsCreation.toLocalDateTime());
                }

                Timestamp tsModification = rs.getTimestamp("date_modification_rep");
                if (tsModification != null) {
                    r.setDate_modification_rep(tsModification.toLocalDateTime());
                }

                r.setAuteur(rs.getString("auteur"));
                r.setRole_emetteur(rs.getString("role_emetteur"));
                r.setLu_par_admin(rs.getBoolean("lu_par_admin"));
                r.setLu_par_patient(rs.getBoolean("lu_par_patient"));

                // ancien champ si encore présent en base
                try {
                    r.setIs_read(rs.getBoolean("is_read"));
                } catch (SQLException ignored) {
                }

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
        String sql = "SELECT * FROM reponse_reclamation WHERE id_reclamation = ? ORDER BY date_creation_rep ASC";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, idReclamation);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reclamation reclamation = new Reclamation();
                reclamation.setId_reclamation(idReclamation);

                ReponseReclamation r = new ReponseReclamation();
                r.setId_reponse(rs.getInt("id_reponse"));
                r.setReclamation(reclamation);
                r.setMessage(rs.getString("message"));
                r.setType_reponse(rs.getString("type_reponse"));

                Timestamp tsCreation = rs.getTimestamp("date_creation_rep");
                if (tsCreation != null) {
                    r.setDate_creation_rep(tsCreation.toLocalDateTime());
                }

                Timestamp tsModification = rs.getTimestamp("date_modification_rep");
                if (tsModification != null) {
                    r.setDate_modification_rep(tsModification.toLocalDateTime());
                }

                r.setAuteur(rs.getString("auteur"));
                r.setRole_emetteur(rs.getString("role_emetteur"));
                r.setLu_par_admin(rs.getBoolean("lu_par_admin"));
                r.setLu_par_patient(rs.getBoolean("lu_par_patient"));

                try {
                    r.setIs_read(rs.getBoolean("is_read"));
                } catch (SQLException ignored) {
                }

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

        String sql = "UPDATE reponse_reclamation SET message=?, type_reponse=?, date_modification_rep=? WHERE id_reponse=?";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setString(1, r.getMessage());
            ps.setString(2, r.getType_reponse());
            ps.setTimestamp(3, Timestamp.valueOf(r.getDate_modification_rep()));
            ps.setInt(4, r.getId_reponse());

            ps.executeUpdate();
            recalculerStatutReclamation(r.getReclamation().getId_reclamation());

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

            recalculerStatutReclamation(r.getReclamation().getId_reclamation());

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

    // =========================
    // RECALCUL STATUT RECLAMATION
    // =========================
    private void recalculerStatutReclamation(int idReclamation) {
        String sqlCount = "SELECT COUNT(*) FROM reponse_reclamation WHERE id_reclamation = ?";

        try {
            PreparedStatement ps = cn.prepareStatement(sqlCount);
            ps.setInt(1, idReclamation);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                String newStatut = (count > 0) ? "Répondu" : "En attente";

                String sqlUpdate = "UPDATE reclamation SET statut_reclamation = ? WHERE id_reclamation = ?";
                PreparedStatement ps2 = cn.prepareStatement(sqlUpdate);
                ps2.setString(1, newStatut);
                ps2.setInt(2, idReclamation);
                ps2.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // DOUBLON
    // =========================
    public boolean existeDoublon(ReponseReclamation r) {

        String sql = """
            SELECT COUNT(*)
            FROM reponse_reclamation
            WHERE id_reclamation = ?
              AND LOWER(TRIM(message)) = LOWER(TRIM(?))
              AND type_reponse = ?
        """;

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, r.getReclamation().getId_reclamation());
            ps.setString(2, r.getMessage());
            ps.setString(3, r.getType_reponse());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public ReponseReclamation getDoublon(ReponseReclamation r) {

        String sql = """
            SELECT *
            FROM reponse_reclamation
            WHERE id_reclamation = ?
              AND LOWER(TRIM(message)) = LOWER(TRIM(?))
              AND type_reponse = ?
            LIMIT 1
        """;

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, r.getReclamation().getId_reclamation());
            ps.setString(2, r.getMessage());
            ps.setString(3, r.getType_reponse());

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                ReponseReclamation rep = new ReponseReclamation();
                rep.setId_reponse(rs.getInt("id_reponse"));
                rep.setMessage(rs.getString("message"));
                rep.setType_reponse(rs.getString("type_reponse"));
                rep.setAuteur(rs.getString("auteur"));
                rep.setRole_emetteur(rs.getString("role_emetteur"));
                rep.setLu_par_admin(rs.getBoolean("lu_par_admin"));
                rep.setLu_par_patient(rs.getBoolean("lu_par_patient"));
                return rep;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    // =========================
    // LECTURE / NON LUS
    // =========================
    public void markAdminMessagesAsReadByPatient(int reclamationId) {
        String sql = """
            UPDATE reponse_reclamation
            SET lu_par_patient = true
            WHERE id_reclamation = ?
              AND role_emetteur = 'ADMIN'
              AND lu_par_patient = false
        """;

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, reclamationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void markPatientMessagesAsReadByAdmin(int reclamationId) {
        String sql = """
            UPDATE reponse_reclamation
            SET lu_par_admin = true
            WHERE id_reclamation = ?
              AND role_emetteur = 'PATIENT'
              AND lu_par_admin = false
        """;

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, reclamationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasUnreadAdminResponseForPatient(int reclamationId) {
        String sql = """
            SELECT COUNT(*)
            FROM reponse_reclamation
            WHERE id_reclamation = ?
              AND role_emetteur = 'ADMIN'
              AND lu_par_patient = false
        """;

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean hasUnreadPatientResponseForAdmin(int reclamationId) {
        String sql = """
            SELECT COUNT(*)
            FROM reponse_reclamation
            WHERE id_reclamation = ?
              AND role_emetteur = 'PATIENT'
              AND lu_par_admin = false
        """;

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean hasAdminResponseReadByPatient(int reclamationId) {
        String sql = """
            SELECT COUNT(*)
            FROM reponse_reclamation
            WHERE id_reclamation = ?
              AND role_emetteur = 'ADMIN'
              AND lu_par_patient = true
        """;

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, reclamationId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // =========================
    // REPONSE FINALE
    // =========================
    public boolean hasFinalResponseForReclamation(int reclamationId) {
        String sql = """
            SELECT COUNT(*)
            FROM reponse_reclamation
            WHERE id_reclamation = ?
              AND LOWER(TRIM(type_reponse)) = LOWER(TRIM(?))
        """;

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, reclamationId);
            ps.setString(2, "Réponse finale");

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}