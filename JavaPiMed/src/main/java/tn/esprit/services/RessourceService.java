package tn.esprit.services;

import tn.esprit.entities.Ressource;
import tn.esprit.entities.Evenement;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RessourceService {

    Connection cn;

    public RessourceService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    public void ajouter(Ressource r) {

        String sql = "INSERT INTO ressource (evenement_id, nom_ressource, categorie_ressource, type_ressource, chemin_fichier_ressource, mime_type_ressource, taille_kb_ressource, url_externe_ressource, quantite_disponible_ressource, unite_ressource, fournisseur_ressource, cout_estime_ressource, est_publique_ressource, notes_ressource, date_creation_ressource, date_mise_a_jour_ressource) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {

            // Set Evenement ID (or handle this via the Evenement object)
            ps.setInt(1, r.getEvenement().getId());  // This fetches the event ID from the Evenement object.
            ps.setString(2, r.getNom_ressource());
            ps.setString(3, r.getCategorie_ressource());
            ps.setString(4, r.getType_ressource());
            ps.setString(5, r.getChemin_fichier_ressource());
            ps.setString(6, r.getMime_type_ressource());
            ps.setInt(7, r.getTaille_kb_ressource());
            ps.setString(8, r.getUrl_externe_ressource());
            ps.setInt(9, r.getQuantite_disponible_ressource());
            ps.setString(10, r.getUnite_ressource());
            ps.setString(11, r.getFournisseur_ressource());
            ps.setDouble(12, r.getCout_estime_ressource());
            ps.setBoolean(13, r.isEst_publique_ressource());
            ps.setString(14, r.getNotes_ressource());
            ps.setDate(15, new java.sql.Date(r.getDate_creation_ressource().getTime()));
            ps.setDate(16, new java.sql.Date(r.getDate_mise_a_jour_ressource().getTime()));

            ps.executeUpdate();
            System.out.println("Ressource ajoutée");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void modifier(Ressource r) {
        String sql = "UPDATE ressource SET evenement_id = ?, nom_ressource = ?, categorie_ressource = ?, type_ressource = ?, chemin_fichier_ressource = ?, mime_type_ressource = ?, taille_kb_ressource = ?, url_externe_ressource = ?, quantite_disponible_ressource = ?, unite_ressource = ?, fournisseur_ressource = ?, cout_estime_ressource = ?, est_publique_ressource = ?, notes_ressource = ?, date_mise_a_jour_ressource = ? WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, r.getEvenement().getId());
            ps.setString(2, r.getNom_ressource());
            ps.setString(3, r.getCategorie_ressource());
            ps.setString(4, r.getType_ressource());
            ps.setString(5, r.getChemin_fichier_ressource());
            ps.setString(6, r.getMime_type_ressource());
            ps.setInt(7, r.getTaille_kb_ressource());
            ps.setString(8, r.getUrl_externe_ressource());
            ps.setInt(9, r.getQuantite_disponible_ressource());
            ps.setString(10, r.getUnite_ressource());
            ps.setString(11, r.getFournisseur_ressource());
            ps.setDouble(12, r.getCout_estime_ressource());
            ps.setBoolean(13, r.isEst_publique_ressource());
            ps.setString(14, r.getNotes_ressource());
            ps.setDate(15, new java.sql.Date(r.getDate_mise_a_jour_ressource().getTime()));
            ps.setInt(16, r.getId());

            ps.executeUpdate();
            System.out.println("Ressource modifiée");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void supprimer(Ressource r) {
        String sql = "DELETE FROM ressource WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, r.getId());
            ps.executeUpdate();
            System.out.println("Ressource supprimée");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void archiver(Ressource r) {
        String sql = """
                UPDATE ressource
                SET est_publique_ressource = ?, date_mise_a_jour_ressource = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setBoolean(1, false);
            ps.setDate(2, new java.sql.Date(new java.util.Date().getTime()));
            ps.setInt(3, r.getId());
            ps.executeUpdate();

            r.setEst_publique_ressource(false);
            r.setDate_mise_a_jour_ressource(new java.util.Date());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public List<Ressource> recuperer() {
        List<Ressource> list = new ArrayList<>();
        String sql = "SELECT * FROM ressource";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Ressource r = new Ressource();
                r.setId(rs.getInt("id"));
                // We now fetch the Evenement object directly from the database.
                Evenement evenement = new Evenement();  // Assuming Evenement ID is stored, we retrieve the event
                evenement.setId(rs.getInt("evenement_id"));  // Set the ID of the Evenement
                r.setEvenement(evenement);  // Link the Evenement object to the Ressource

                r.setNom_ressource(rs.getString("nom_ressource"));
                r.setCategorie_ressource(rs.getString("categorie_ressource"));
                r.setType_ressource(rs.getString("type_ressource"));
                r.setChemin_fichier_ressource(rs.getString("chemin_fichier_ressource"));
                r.setMime_type_ressource(rs.getString("mime_type_ressource"));
                r.setTaille_kb_ressource(rs.getInt("taille_kb_ressource"));
                r.setUrl_externe_ressource(rs.getString("url_externe_ressource"));
                r.setQuantite_disponible_ressource(rs.getInt("quantite_disponible_ressource"));
                r.setUnite_ressource(rs.getString("unite_ressource"));
                r.setFournisseur_ressource(rs.getString("fournisseur_ressource"));
                r.setCout_estime_ressource(rs.getDouble("cout_estime_ressource"));
                r.setEst_publique_ressource(rs.getBoolean("est_publique_ressource"));
                r.setNotes_ressource(rs.getString("notes_ressource"));
                r.setDate_creation_ressource(rs.getDate("date_creation_ressource"));
                r.setDate_mise_a_jour_ressource(rs.getDate("date_mise_a_jour_ressource"));

                list.add(r);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }
    public boolean ressourceExisteDeja(int evenementId, String nom, String type) {
        String sql = """
            SELECT COUNT(*)
            FROM ressource
            WHERE evenement_id = ?
              AND LOWER(TRIM(nom_ressource)) = LOWER(TRIM(?))
              AND LOWER(TRIM(type_ressource)) = LOWER(TRIM(?))
        """;

        try (PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setInt(1, evenementId);
            pst.setString(2, nom);
            pst.setString(3, type);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur unicité ressource : " + e.getMessage());
        }

        return false;
    }

    public boolean ressourceExisteDejaPourModification(int id, int evenementId, String nom, String type) {
        String sql = """
            SELECT COUNT(*)
            FROM ressource
            WHERE evenement_id = ?
              AND LOWER(TRIM(nom_ressource)) = LOWER(TRIM(?))
              AND LOWER(TRIM(type_ressource)) = LOWER(TRIM(?))
              AND id <> ?
        """;

        try (PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setInt(1, evenementId);
            pst.setString(2, nom);
            pst.setString(3, type);
            pst.setInt(4, id);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur unicité ressource modif : " + e.getMessage());
        }

        return false;
    }
}
