package tn.esprit.services;

import tn.esprit.entities.Reclamation;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService implements IGeneralService<Reclamation> {

    Connection cn;

    public ReclamationService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void ajouter(Reclamation r) throws SQLException {
        String sql = "insert into reclamation(reference_reclamation, contenu, description, type, statut_reclamation, priorite) values(?,?,?,?,?,?)";
        PreparedStatement ps = cn.prepareStatement(sql);
        ps.setString(1, r.getReference_reclamation());
        ps.setString(2, r.getContenu());
        ps.setString(3, r.getDescription());
        ps.setString(4, r.getType());
        ps.setString(5, r.getStatut_reclamation());
        ps.setString(6, r.getPriorite());
        ps.executeUpdate();
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
        String sql = "update reclamation set reference_reclamation=?, contenu=?, description=?, type=?, statut_reclamation=?, priorite=? where id_reclamation=?";
        try {
            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setString(1, r.getReference_reclamation());
            ps.setString(2, r.getContenu());
            ps.setString(3, r.getDescription());
            ps.setString(4, r.getType());
            ps.setString(5, r.getStatut_reclamation());
            ps.setString(6, r.getPriorite());
            ps.setInt(7, r.getId_reclamation());
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
            rec.setReference_reclamation(rs.getString("reference_reclamation"));
            rec.setContenu(rs.getString("contenu"));
            rec.setDescription(rs.getString("description"));
            rec.setType(rs.getString("type"));
            rec.setStatut_reclamation(rs.getString("statut_reclamation"));
            rec.setPriorite(rs.getString("priorite"));

            reclamations.add(rec);
        }

        return reclamations;
    }

    @Override
    public Reclamation recupererParId(int id) {
        return null;
    }
}