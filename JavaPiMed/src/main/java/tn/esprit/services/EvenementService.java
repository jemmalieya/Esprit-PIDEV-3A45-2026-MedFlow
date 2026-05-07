package tn.esprit.services;

import tn.esprit.entities.Evenement;
import tn.esprit.entities.User;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Locale;

public class EvenementService implements IGeneralService<Evenement> {

    Connection cn;

    public EvenementService() {
        cn = MyDataBase.getInstance().getCnx();
        ensureCancellationReasonColumn();
    }

    private void ensureCancellationReasonColumn() {
        if (cn == null) return;

        try {
            DatabaseMetaData metaData = cn.getMetaData();
            try (ResultSet columns = metaData.getColumns(cn.getCatalog(), null, "evenement", "motif_annulation_event")) {
                if (columns.next()) {
                    return;
                }
            }

            try (Statement statement = cn.createStatement()) {
                statement.executeUpdate("""
                        ALTER TABLE evenement
                        ADD COLUMN motif_annulation_event TEXT NULL AFTER statut_event
                        """);
            }
        } catch (SQLException ex) {
            System.out.println("Impossible de verifier/creer motif_annulation_event: " + ex.getMessage());
        }
    }

    public String recupererMotifAnnulation(int eventId) {
        ensureCancellationReasonColumn();
        String sql = "SELECT motif_annulation_event FROM evenement WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("motif_annulation_event");
                }
            }
        } catch (SQLException ex) {
            System.out.println("Impossible de recuperer le motif d'annulation: " + ex.getMessage());
        }
        return "";
    }

    public void modifierMotifAnnulation(int eventId, String motif) {
        ensureCancellationReasonColumn();
        String sql = "UPDATE evenement SET motif_annulation_event = ?, date_mise_a_jour_event = ? WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, motif == null ? "" : motif.trim());
            ps.setDate(2, new java.sql.Date(new java.util.Date().getTime()));
            ps.setInt(3, eventId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("Impossible de modifier le motif d'annulation: " + ex.getMessage());
        }
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
        String sql = "UPDATE evenement SET " +
                "titre_event = ?, slug_event = ?, type_event = ?, description_event = ?, objectif_event = ?, statut_event = ?, " +
                "date_debut_event = ?, date_fin_event = ?, nom_lieu_event = ?, adresse_event = ?, ville_event = ?, " +
                "nb_participants_max_event = ?, inscription_obligatoire_event = ?, date_limite_inscription_event = ?, " +
                "email_contact_event = ?, tel_contact_event = ?, nom_organisateur_event = ?, image_couverture_event = ?, visibilite_event = ?, " +
                "date_mise_a_jour_event = ? " +
                "WHERE id = ?";

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

            ps.setDate(20, new java.sql.Date(e.getDate_mise_a_jour_event().getTime()));

            ps.setInt(21, e.getId());

            ps.executeUpdate();
            System.out.println("Evenement modifié COMPLET");

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
                e.setDemandes_json(rs.getString("demandes_json"));
                e.setTitre_event(rs.getString("titre_event"));
                e.setSlug_event(rs.getString("slug_event"));
                e.setType_event(rs.getString("type_event"));
                e.setDescription_event(rs.getString("description_event"));
                e.setObjectif_event(rs.getString("objectif_event"));
                e.setStatut_event(rs.getString("statut_event"));

                e.setDate_debut_event(rs.getDate("date_debut_event")); // ✅ FIX
                e.setDate_fin_event(rs.getDate("date_fin_event"));     // ✅ FIX

                e.setNom_lieu_event(rs.getString("nom_lieu_event"));
                e.setAdresse_event(rs.getString("adresse_event"));
                e.setVille_event(rs.getString("ville_event"));

                e.setNb_participants_max_event(rs.getInt("nb_participants_max_event"));
                e.setInscription_obligatoire_event(rs.getBoolean("inscription_obligatoire_event"));

                e.setDate_limite_inscription_event(rs.getDate("date_limite_inscription_event")); // ✅

                e.setEmail_contact_event(rs.getString("email_contact_event"));
                e.setTel_contact_event(rs.getString("tel_contact_event"));
                e.setNom_organisateur_event(rs.getString("nom_organisateur_event"));

                e.setImage_couverture_event(rs.getString("image_couverture_event"));
                e.setVisibilite_event(rs.getString("visibilite_event"));

                e.setDate_creation_event(rs.getDate("date_creation_event")); // optional
                e.setDate_mise_a_jour_event(rs.getDate("date_mise_a_jour_event")); // optional

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

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Evenement e = new Evenement();

                e.setId(rs.getInt("id"));
                e.setDemandes_json(rs.getString("demandes_json"));
                e.setTitre_event(rs.getString("titre_event"));
                e.setSlug_event(rs.getString("slug_event"));
                e.setType_event(rs.getString("type_event"));
                e.setDescription_event(rs.getString("description_event"));
                e.setObjectif_event(rs.getString("objectif_event"));
                e.setStatut_event(rs.getString("statut_event"));

                e.setDate_debut_event(rs.getDate("date_debut_event"));
                e.setDate_fin_event(rs.getDate("date_fin_event"));

                e.setNom_lieu_event(rs.getString("nom_lieu_event"));
                e.setAdresse_event(rs.getString("adresse_event"));
                e.setVille_event(rs.getString("ville_event"));

                e.setNb_participants_max_event(rs.getInt("nb_participants_max_event"));
                e.setInscription_obligatoire_event(rs.getBoolean("inscription_obligatoire_event"));

                e.setDate_limite_inscription_event(rs.getDate("date_limite_inscription_event"));

                e.setEmail_contact_event(rs.getString("email_contact_event"));
                e.setTel_contact_event(rs.getString("tel_contact_event"));
                e.setNom_organisateur_event(rs.getString("nom_organisateur_event"));

                e.setImage_couverture_event(rs.getString("image_couverture_event"));
                e.setVisibilite_event(rs.getString("visibilite_event"));

                return e;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public boolean evenementExisteDeja(Date dateDebut) {
        String sql = """
        SELECT COUNT(*)
        FROM evenement
        WHERE date_debut_event = ?
    """;

        try (PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setDate(1, dateDebut);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur unicité événement : " + e.getMessage());
        }

        return false;
    }
    public boolean evenementExisteDejaPourModification(int id, java.sql.Date dateDebut) {
        String sql = """
        SELECT COUNT(*)
        FROM evenement
        WHERE date_debut_event = ?
          AND id <> ?
    """;

        try (PreparedStatement pst = cn.prepareStatement(sql)) {
            pst.setDate(1, dateDebut);
            pst.setInt(2, id);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur unicité événement modif : " + e.getMessage());
        }

        return false;
    }

    public void archiver(Evenement e) {
        String sql = """
                UPDATE evenement
                SET statut_event = ?, visibilite_event = ?, date_mise_a_jour_event = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, "Archive");
            ps.setString(2, "Prive");
            ps.setDate(3, new java.sql.Date(new java.util.Date().getTime()));
            ps.setInt(4, e.getId());
            ps.executeUpdate();

            e.setStatut_event("Archive");
            e.setVisibilite_event("Prive");
            e.setDate_mise_a_jour_event(new java.util.Date());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public List<Evenement> rechercherEvenements(List<Evenement> liste, String keyword) {
        if (liste == null) return new ArrayList<>();

        String motCle = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (motCle.isEmpty()) return new ArrayList<>(liste);

        List<Evenement> resultat = new ArrayList<>();

        for (Evenement e : liste) {
            if (safe(e.getTitre_event()).toLowerCase(Locale.ROOT).contains(motCle)
                    || safe(e.getDescription_event()).toLowerCase(Locale.ROOT).contains(motCle)
                    || safe(e.getType_event()).toLowerCase(Locale.ROOT).contains(motCle)
                    || safe(e.getVille_event()).toLowerCase(Locale.ROOT).contains(motCle)
                    || safe(e.getStatut_event()).toLowerCase(Locale.ROOT).contains(motCle)
                    || safe(e.getNom_organisateur_event()).toLowerCase(Locale.ROOT).contains(motCle)
                    || safe(e.getEmail_contact_event()).toLowerCase(Locale.ROOT).contains(motCle)) {
                resultat.add(e);
            }
        }

        return resultat;
    }

    public List<Evenement> filtrerEvenements(List<Evenement> liste, String type, String statut, String ville) {
        if (liste == null) return new ArrayList<>();

        String typeChoisi = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        String statutChoisi = statut == null ? "" : statut.trim().toLowerCase(Locale.ROOT);
        String villeChoisie = ville == null ? "" : ville.trim().toLowerCase(Locale.ROOT);

        List<Evenement> resultat = new ArrayList<>(liste);

        if (!typeChoisi.isEmpty() && !typeChoisi.equals("tous")) {
            resultat.removeIf(e ->
                    !safe(e.getType_event()).toLowerCase(Locale.ROOT).equals(typeChoisi)
            );
        }

        if (!statutChoisi.isEmpty() && !statutChoisi.equals("tous")) {
            resultat.removeIf(e ->
                    !safe(e.getStatut_event()).toLowerCase(Locale.ROOT).equals(statutChoisi)
            );
        }

        if (!villeChoisie.isEmpty() && !villeChoisie.equals("toutes")) {
            resultat.removeIf(e ->
                    !safe(e.getVille_event()).toLowerCase(Locale.ROOT).equals(villeChoisie)
            );
        }

        return resultat;
    }

    public List<Evenement> trierEvenements(List<Evenement> liste, String tri) {
        if (liste == null) return new ArrayList<>();

        List<Evenement> resultat = new ArrayList<>(liste);
        String triChoisi = tri == null ? "" : tri.trim();

        switch (triChoisi) {
            case "Titre A-Z" ->
                    resultat.sort(Comparator.comparing(e -> safe(e.getTitre_event()).toLowerCase(Locale.ROOT)));

            case "Titre Z-A" ->
                    resultat.sort(Comparator.comparing((Evenement e) -> safe(e.getTitre_event()).toLowerCase(Locale.ROOT)).reversed());

            case "Ville A-Z" ->
                    resultat.sort(Comparator.comparing(e -> safe(e.getVille_event()).toLowerCase(Locale.ROOT)));

            case "Ville Z-A" ->
                    resultat.sort(Comparator.comparing((Evenement e) -> safe(e.getVille_event()).toLowerCase(Locale.ROOT)).reversed());

            case "Statut" ->
                    resultat.sort(Comparator.comparing(e -> safe(e.getStatut_event()).toLowerCase(Locale.ROOT)));

            case "Type" ->
                    resultat.sort(Comparator.comparing(e -> safe(e.getType_event()).toLowerCase(Locale.ROOT)));

            case "Organisateur" ->
                    resultat.sort(Comparator.comparing(e -> safe(e.getNom_organisateur_event()).toLowerCase(Locale.ROOT)));
        }

        return resultat;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
