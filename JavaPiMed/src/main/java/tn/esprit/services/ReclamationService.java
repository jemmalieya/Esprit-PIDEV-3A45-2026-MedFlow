package tn.esprit.services;

import tn.esprit.entities.Reclamation;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import tn.esprit.entities.User;

public class ReclamationService implements IGeneralService<Reclamation> {

    Connection cn;

    public ReclamationService() {
        cn = MyDataBase.getInstance().getCnx();
    }
    @Override
    public void ajouter(Reclamation r) throws SQLException {
        int score = calculerUrgenceScore(r);
        String prioriteAuto = determinerPrioriteAutomatique(score);

        r.setUrgence_score(score);
        r.setPriorite(prioriteAuto);

        String sql = "insert into reclamation(user_id, reference_reclamation, contenu, description, type, statut_reclamation, priorite, date_creation_r, notification_envoyee,urgence_score) values(?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cn.prepareStatement(sql);

        ps.setInt(1, r.getUser().getId());
        ps.setString(2, r.getReference_reclamation());
        ps.setString(3, r.getContenu());
        ps.setString(4, r.getDescription());
        ps.setString(5, r.getType());
        ps.setString(6, r.getStatut_reclamation());
        ps.setString(7, r.getPriorite());
        ps.setTimestamp(8, Timestamp.valueOf(r.getDate_creation_r()));
        ps.setBoolean(9, r.isNotification_envoyee());
        ps.setInt(10, r.getUrgence_score());

        int rows = ps.executeUpdate();
        System.out.println("Inserted rows: " + rows);
    }

    @Override
    public void supprimer(Reclamation r) {
        String sql = "delete from reclamation where id_reclamation = ?";
        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, r.getId_reclamation());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void modifier(Reclamation r) {
        int score = calculerUrgenceScore(r);
        String prioriteAuto = determinerPrioriteAutomatique(score);

        r.setUrgence_score(score);
        r.setPriorite(prioriteAuto);

        String sql = "update reclamation set reference_reclamation=?, contenu=?, description=?, type=?, statut_reclamation=?, priorite=?,urgence_score=? where id_reclamation=?";
        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setString(1, r.getReference_reclamation());
            ps.setString(2, r.getContenu());
            ps.setString(3, r.getDescription());
            ps.setString(4, r.getType());
            ps.setString(5, r.getStatut_reclamation());
            ps.setString(6, r.getPriorite());
            ps.setInt(8, r.getId_reclamation());
            ps.setInt(7, r.getUrgence_score());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Reclamation> recuperer() throws SQLException {
        String sql = "select * from reclamation";
        Statement st = cn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        List<Reclamation> reclamations = new ArrayList<>();

        while (rs.next()) {
            Reclamation rec = new Reclamation();
            rec.setId_reclamation(rs.getInt("id_reclamation"));

            User user = new User();
            user.setId(rs.getInt("user_id"));
            rec.setUser(user);

            rec.setReference_reclamation(rs.getString("reference_reclamation"));
            rec.setContenu(rs.getString("contenu"));
            rec.setDescription(rs.getString("description"));
            rec.setType(rs.getString("type"));
            rec.setStatut_reclamation(rs.getString("statut_reclamation"));
            rec.setPriorite(rs.getString("priorite"));
            rec.setNotification_envoyee(rs.getBoolean("notification_envoyee"));

            Timestamp tsCreation = rs.getTimestamp("date_creation_r");
            rec.setDate_creation_r(tsCreation != null ? tsCreation.toLocalDateTime() : null);

            reclamations.add(rec);
        }

        return reclamations;
    }

    @Override
    public Reclamation recupererParId(int id) {

        String sql = "SELECT * FROM reclamation WHERE id_reclamation = ?";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Reclamation r = new Reclamation();

                r.setId_reclamation(rs.getInt("id_reclamation"));

                User user = new User();
                user.setId(rs.getInt("user_id"));
                r.setUser(user);

                r.setReference_reclamation(rs.getString("reference_reclamation"));
                r.setContenu(rs.getString("contenu"));
                r.setDescription(rs.getString("description"));
                r.setType(rs.getString("type"));
                r.setStatut_reclamation(rs.getString("statut_reclamation"));
                r.setPriorite(rs.getString("priorite"));

                Timestamp tsCreation = rs.getTimestamp("date_creation_r");
                r.setDate_creation_r(tsCreation != null ? tsCreation.toLocalDateTime() : null);
                r.setNotification_envoyee(rs.getBoolean("notification_envoyee"));

                return r;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    public List<Reclamation> getAll() {

        List<Reclamation> list = new ArrayList<>();

        String sql = "SELECT * FROM reclamation";

        try {
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                Reclamation r = new Reclamation();

                r.setId_reclamation(rs.getInt("id_reclamation"));

                User user = new User();
                user.setId(rs.getInt("user_id"));
                r.setUser(user);

                r.setContenu(rs.getString("contenu"));
                r.setDescription(rs.getString("description"));
                r.setType(rs.getString("type"));
                r.setStatut_reclamation(rs.getString("statut_reclamation"));
                r.setPriorite(rs.getString("priorite"));
                r.setReference_reclamation(rs.getString("reference_reclamation"));

                Timestamp tsCreation = rs.getTimestamp("date_creation_r");
                r.setDate_creation_r(tsCreation != null ? tsCreation.toLocalDateTime() : null);

                list.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public List<Reclamation> getByUserId(int userId) {

        List<Reclamation> list = new ArrayList<>();

        String sql = "SELECT * FROM reclamation WHERE user_id = ?";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Reclamation r = new Reclamation();

                r.setId_reclamation(rs.getInt("id_reclamation"));

                User user = new User();
                user.setId(rs.getInt("user_id"));
                r.setUser(user);

                r.setContenu(rs.getString("contenu"));
                r.setDescription(rs.getString("description"));
                r.setType(rs.getString("type"));
                r.setStatut_reclamation(rs.getString("statut_reclamation"));
                r.setPriorite(rs.getString("priorite"));
                r.setReference_reclamation(rs.getString("reference_reclamation"));

                Timestamp tsCreation = rs.getTimestamp("date_creation_r");
                r.setDate_creation_r(tsCreation != null ? tsCreation.toLocalDateTime() : null);

                list.add(r);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void updateNotificationEnvoyee(int idReclamation, boolean notificationEnvoyee) {
        String sql = "UPDATE reclamation SET notification_envoyee = ? WHERE id_reclamation = ?";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setBoolean(1, notificationEnvoyee);
            ps.setInt(2, idReclamation);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateStatut(int idReclamation, String statut) {
        String sql = "UPDATE reclamation SET statut_reclamation = ? WHERE id_reclamation = ?";

        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setString(1, statut);
            ps.setInt(2, idReclamation);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int calculerUrgenceScore(Reclamation r) {
        String texte = ((r.getContenu() == null ? "" : r.getContenu()) + " "
                + (r.getDescription() == null ? "" : r.getDescription()))
                .toLowerCase();

        int score = 0;

        // mots très graves
        if (texte.contains("urgent")) score += 25;
        if (texte.contains("urgence")) score += 25;
        if (texte.contains("grave")) score += 20;
        if (texte.contains("critique")) score += 25;
        if (texte.contains("danger")) score += 25;
        if (texte.contains("douleur")) score += 15;
        if (texte.contains("saignement")) score += 20;
        if (texte.contains("erreur")) score += 10;
        if (texte.contains("médicament")) score += 15;
        if (texte.contains("medicament")) score += 15;
        if (texte.contains("prescription")) score += 10;
        if (texte.contains("allergie")) score += 20;
        if (texte.contains("effet secondaire")) score += 20;
        if (texte.contains("hospitalisation")) score += 25;
        if (texte.contains("bloqué")) score += 10;
        if (texte.contains("bloque")) score += 10;
        if (texte.contains("impossible")) score += 10;
        if (texte.contains("panne")) score += 10;
        if (texte.contains("bug")) score += 10;
        if (texte.contains("arnaque")) score += 15;
        if (texte.contains("paiement")) score += 10;
        if (texte.contains("remboursement")) score += 10;

        if (score > 100) {
            score = 100;
        }

        return score;
    }

    private String determinerPrioriteAutomatique(int score) {
        if (score >= 60) {
            return "Élevée";
        } else if (score >= 30) {
            return "Moyenne";
        } else {
            return "Faible";
        }
    }
}