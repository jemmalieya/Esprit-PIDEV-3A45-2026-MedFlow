package tn.esprit.services;

import tn.esprit.entities.RendezVous;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RendezVousService implements IGeneralService<RendezVous> {
    Connection cn;
    public RendezVousService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void ajouter(RendezVous r) {
        String sql = "INSERT INTO rendez_vous(datetime, statut, mode, motif, created_at, idPatient, idStaff, urgency_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setTimestamp(1, r.getDatetime());
            ps.setString(2, r.getStatut());
            ps.setString(3, r.getMode());
            ps.setString(4, r.getMotif());
            ps.setTimestamp(5, r.getCreated_at());
            ps.setInt(6, r.getIdPatient());
            ps.setInt(7, r.getIdStaff());
            ps.setString(8, r.getUrgency_level());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void supprimer(RendezVous r) {
        String sql = "DELETE FROM rendez_vous WHERE id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void modifier(RendezVous r) {
        String sql = "UPDATE rendez_vous SET datetime=?, statut=?, mode=?, motif=?, created_at=?, idPatient=?, idStaff=?, urgency_level=? WHERE id=?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setTimestamp(1, r.getDatetime());
            ps.setString(2, r.getStatut());
            ps.setString(3, r.getMode());
            ps.setString(4, r.getMotif());
            ps.setTimestamp(5, r.getCreated_at());
            ps.setInt(6, r.getIdPatient());
            ps.setInt(7, r.getIdStaff());
            ps.setString(8, r.getUrgency_level());
            ps.setInt(9, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<RendezVous> recuperer() {
        List<RendezVous> list = new ArrayList<>();
        String sql = "SELECT * FROM rendez_vous";
        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                RendezVous r = new RendezVous(
                        rs.getInt("id"),
                        rs.getTimestamp("datetime"),
                        rs.getString("statut"),
                        rs.getString("mode"),
                        rs.getString("motif"),
                        rs.getTimestamp("created_at"),
                        rs.getInt("idPatient"),
                        rs.getInt("idStaff"),
                        rs.getString("urgency_level")
                );
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
