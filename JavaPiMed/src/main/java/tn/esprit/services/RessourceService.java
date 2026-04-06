package tn.esprit.services;

import tn.esprit.entities.Ressource;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RessourceService {

    // Establishing a connection to the database
    Connection cn;

    // Constructor to initialize the database connection
    public RessourceService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    // Add a new resource (Ressource)
    public void ajouter(Ressource r) {
        // SQL query to insert a resource into the ressource table
        String sql = "INSERT INTO ressource (nom_ressource, categorie_ressource, type_ressource, chemin_fichier_ressource, mime_type_ressource, taille_kb_ressource, url_externe_ressource, quantite_disponible_ressource, unite_ressource, fournisseur_ressource, cout_estime_ressource, est_publique_ressource, notes_ressource, date_creation_ressource, date_mise_a_jour_ressource) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            // Setting the values of the resource
            ps.setString(1, r.getNom_ressource());
            ps.setString(2, r.getCategorie_ressource());
            ps.setString(3, r.getType_ressource());
            ps.setString(4, r.getChemin_fichier_ressource());
            ps.setString(5, r.getMime_type_ressource());
            ps.setInt(6, r.getTaille_kb_ressource());
            ps.setString(7, r.getUrl_externe_ressource());
            ps.setInt(8, r.getQuantite_disponible_ressource());
            ps.setString(9, r.getUnite_ressource());
            ps.setString(10, r.getFournisseur_ressource());
            ps.setDouble(11, r.getCout_estime_ressource());
            ps.setBoolean(12, r.isEst_publique_ressource());
            ps.setString(13, r.getNotes_ressource());
            ps.setDate(14, new java.sql.Date(r.getDate_creation_ressource().getTime()));
            ps.setDate(15, new java.sql.Date(r.getDate_mise_a_jour_ressource().getTime()));

            // Execute the insert query
            ps.executeUpdate();
            System.out.println("✅ Ressource ajoutée");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Modify an existing resource (Ressource)
    public void modifier(Ressource r) {
        String sql = "UPDATE ressource SET nom_ressource = ?, categorie_ressource = ?, type_ressource = ?, chemin_fichier_ressource = ?, mime_type_ressource = ?, taille_kb_ressource = ?, url_externe_ressource = ?, quantite_disponible_ressource = ?, unite_ressource = ?, fournisseur_ressource = ?, cout_estime_ressource = ?, est_publique_ressource = ?, notes_ressource = ?, date_mise_a_jour_ressource = ? WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, r.getNom_ressource());
            ps.setString(2, r.getCategorie_ressource());
            ps.setString(3, r.getType_ressource());
            ps.setString(4, r.getChemin_fichier_ressource());
            ps.setString(5, r.getMime_type_ressource());
            ps.setInt(6, r.getTaille_kb_ressource());
            ps.setString(7, r.getUrl_externe_ressource());
            ps.setInt(8, r.getQuantite_disponible_ressource());
            ps.setString(9, r.getUnite_ressource());
            ps.setString(10, r.getFournisseur_ressource());
            ps.setDouble(11, r.getCout_estime_ressource());
            ps.setBoolean(12, r.isEst_publique_ressource());
            ps.setString(13, r.getNotes_ressource());
            ps.setDate(14, new java.sql.Date(r.getDate_mise_a_jour_ressource().getTime()));
            ps.setInt(15, r.getId());

            ps.executeUpdate();
            System.out.println("✏️ Ressource modifiée");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Delete an existing resource (Ressource)
    public void supprimer(Ressource r) {
        String sql = "DELETE FROM ressource WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, r.getId());
            ps.executeUpdate();
            System.out.println("🗑️ Ressource supprimée");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // Retrieve all resources (Ressources)
    public List<Ressource> recuperer() {
        List<Ressource> list = new ArrayList<>();
        String sql = "SELECT * FROM ressource";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Ressource r = new Ressource();
                r.setId(rs.getInt("id"));
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
}