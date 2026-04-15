package tn.esprit.services;

import tn.esprit.entities.Evenement;
import tn.esprit.entities.User;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements IGeneralService<Evenement> {

    Connection cn;

    public EvenementService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void ajouter(Evenement e) throws SQLException {

        String sql = "INSERT INTO evenement (" +
                "titre_event, slug_event, type_event, description_event, objectif_event, statut_event, " +
                "date_debut_event, date_fin_event, nom_lieu_event, adresse_event, ville_event, " +
                "nb_participants_max_event, inscription_obligatoire_event, date_limite_inscription_event, " +
                "email_contact_event, tel_contact_event, nom_organisateur_event, image_couverture_event, visibilite_event, " +
                "date_creation_event, date_mise_a_jour_event" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, e.getTitre_event());
            ps.setString(2, e.getSlug_event());
            ps.setString(3, e.getType_event());
            ps.setString(4, e.getDescription_event());
            ps.setString(5, e.getObjectif_event());
            ps.setString(6, e.getStatut_event());
            ps.setDate(7, new java.sql.Date(e.getDate_debut_event().getTime()));
            ps.setDate(8, new java.sql.Date(e.getDate_fin_event().getTime()));
            ps.setString(9, e.getNom_lieu_event());
            ps.setString(10, e.getAdresse_event());
            ps.setString(11, e.getVille_event());
            ps.setInt(12, e.getNb_participants_max_event());
            ps.setBoolean(13, e.isInscription_obligatoire_event());

            if (e.getDate_limite_inscription_event() != null)
                ps.setDate(14, new java.sql.Date(e.getDate_limite_inscription_event().getTime()));
            else
                ps.setNull(14, Types.DATE);

            ps.setString(15, e.getEmail_contact_event());
            ps.setString(16, e.getTel_contact_event());
            ps.setString(17, e.getNom_organisateur_event());
            ps.setString(18, e.getImage_couverture_event());
            ps.setString(19, e.getVisibilite_event());
            ps.setDate(20, new java.sql.Date(e.getDate_creation_event().getTime()));
            ps.setDate(21, new java.sql.Date(e.getDate_mise_a_jour_event().getTime()));

            ps.executeUpdate();
            System.out.println("Evenement ajouté");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void supprimer(Evenement e) {
        String sql = "DELETE FROM evenement WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, e.getId());
            ps.executeUpdate();
            System.out.println("Evenement supprimé avec succès");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void modifier(Evenement e) {
        String sql = "UPDATE evenement SET titre_event = ?, slug_event = ?, type_event = ?, description_event = ?, ville_event = ?, nb_participants_max_event = ?, email_contact_event = ?, nom_organisateur_event = ?, statut_event = ? WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, e.getTitre_event());
            ps.setString(2, e.getSlug_event());
            ps.setString(3, e.getType_event());
            ps.setString(4, e.getDescription_event());
            ps.setString(5, e.getVille_event());
            ps.setInt(6, e.getNb_participants_max_event());
            ps.setString(7, e.getEmail_contact_event());
            ps.setString(8, e.getNom_organisateur_event());
            ps.setString(9, e.getStatut_event());
            ps.setInt(10, e.getId());

            ps.executeUpdate();
            System.out.println("Evenement modifié avec succès");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public List<Evenement> recuperer() throws SQLException {
        List<Evenement> list = new ArrayList<>();
        String sql = "SELECT * FROM evenement";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Evenement e = new Evenement();

                e.setId(rs.getInt("id"));
                e.setTitre_event(rs.getString("titre_event"));
                e.setSlug_event(rs.getString("slug_event"));
                e.setType_event(rs.getString("type_event"));
                e.setDescription_event(rs.getString("description_event"));
                e.setVille_event(rs.getString("ville_event"));
                e.setNb_participants_max_event(rs.getInt("nb_participants_max_event"));
                e.setEmail_contact_event(rs.getString("email_contact_event"));
                e.setNom_organisateur_event(rs.getString("nom_organisateur_event"));
                e.setStatut_event(rs.getString("statut_event"));
                e.setImage_couverture_event(rs.getString("image_couverture_event"));
                e.setVisibilite_event(rs.getString("visibilite_event"));

                list.add(e);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }

    @Override
    public Evenement recupererParId(int id) {
        String sql = "SELECT * FROM evenement WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Evenement e = new Evenement();

                    e.setId(rs.getInt("id"));
                    e.setTitre_event(rs.getString("titre_event"));
                    e.setSlug_event(rs.getString("slug_event"));
                    e.setType_event(rs.getString("type_event"));
                    e.setDescription_event(rs.getString("description_event"));
                    e.setVille_event(rs.getString("ville_event"));
                    e.setNb_participants_max_event(rs.getInt("nb_participants_max_event"));
                    e.setEmail_contact_event(rs.getString("email_contact_event"));
                    e.setNom_organisateur_event(rs.getString("nom_organisateur_event"));
                    e.setStatut_event(rs.getString("statut_event"));

                    return e;
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
