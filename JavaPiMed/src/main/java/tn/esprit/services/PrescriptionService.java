package tn.esprit.services;

import tn.esprit.entities.Prescription;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionService implements IGeneralService<Prescription> {
    Connection cn;
    public PrescriptionService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void ajouter(Prescription p) {
        String sql = "INSERT INTO prescription(fiche_medicale_id, nom_medicament, dose, frequence, duree, instructions, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, p.getFiche_medicale_id());
            ps.setString(2, p.getNom_medicament());
            ps.setString(3, p.getDose());
            ps.setString(4, p.getFrequence());
            ps.setInt(5, p.getDuree());
            ps.setString(6, p.getInstructions());
            ps.setTimestamp(7, p.getCreated_at());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void supprimer(Prescription p) {
        String sql = "DELETE FROM prescription WHERE id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void modifier(Prescription p) {
        String sql = "UPDATE prescription SET fiche_medicale_id=?, nom_medicament=?, dose=?, frequence=?, duree=?, instructions=?, created_at=? WHERE id=?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, p.getFiche_medicale_id());
            ps.setString(2, p.getNom_medicament());
            ps.setString(3, p.getDose());
            ps.setString(4, p.getFrequence());
            ps.setInt(5, p.getDuree());
            ps.setString(6, p.getInstructions());
            ps.setTimestamp(7, p.getCreated_at());
            ps.setInt(8, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Prescription> recuperer() {
        List<Prescription> list = new ArrayList<>();
        String sql = "SELECT * FROM prescription";
        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Prescription p = new Prescription(
                        rs.getInt("id"),
                        rs.getInt("fiche_medicale_id"),
                        rs.getString("nom_medicament"),
                        rs.getString("dose"),
                        rs.getString("frequence"),
                        rs.getInt("duree"),
                        rs.getString("instructions"),
                        rs.getTimestamp("created_at")
                );
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
