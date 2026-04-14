package tn.esprit.services;

import tn.esprit.entities.FicheMedicale;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FicheMedicaleService implements IGeneralService<FicheMedicale> {
    Connection cn;
    public FicheMedicaleService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void ajouter(FicheMedicale f) {
        String sql = "INSERT INTO fiche_medicale(rendez_vous_id, diagnostic, observations, resultats_examens, start_time, end_time, duree_minutes, created_at, signature) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, f.getRendez_vous_id());
            ps.setString(2, f.getDiagnostic());
            ps.setString(3, f.getObservations());
            ps.setString(4, f.getResultats_examens());
            ps.setTimestamp(5, f.getStart_time());
            ps.setTimestamp(6, f.getEnd_time());
            ps.setObject(7, f.getDuree_minutes());
            ps.setTimestamp(8, f.getCreated_at());
            ps.setString(9, f.getSignature());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void supprimer(FicheMedicale f) {
        String sql = "DELETE FROM fiche_medicale WHERE id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, f.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void modifier(FicheMedicale f) {
        String sql = "UPDATE fiche_medicale SET rendez_vous_id=?, diagnostic=?, observations=?, resultats_examens=?, start_time=?, end_time=?, duree_minutes=?, created_at=?, signature=? WHERE id=?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setObject(1, f.getRendez_vous_id());
            ps.setString(2, f.getDiagnostic());
            ps.setString(3, f.getObservations());
            ps.setString(4, f.getResultats_examens());
            ps.setTimestamp(5, f.getStart_time());
            ps.setTimestamp(6, f.getEnd_time());
            ps.setObject(7, f.getDuree_minutes());
            ps.setTimestamp(8, f.getCreated_at());
            ps.setString(9, f.getSignature());
            ps.setInt(10, f.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<FicheMedicale> recuperer() {
        List<FicheMedicale> list = new ArrayList<>();
        String sql = "SELECT * FROM fiche_medicale";
        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                FicheMedicale f = new FicheMedicale(
                        rs.getInt("id"),
                        (Integer)rs.getObject("rendez_vous_id"),
                        rs.getString("diagnostic"),
                        rs.getString("observations"),
                        rs.getString("resultats_examens"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"),
                        (Integer)rs.getObject("duree_minutes"),
                        rs.getTimestamp("created_at"),
                        rs.getString("signature")
                );
                list.add(f);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public FicheMedicale recupererParId(int id) {
        return null;
    }
}
