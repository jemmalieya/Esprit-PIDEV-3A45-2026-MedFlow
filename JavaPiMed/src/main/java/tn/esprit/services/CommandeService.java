package tn.esprit.services;

import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.entities.User;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommandeService implements IGeneralService<Commande> {

    private final Connection cn;

    public CommandeService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    public int calculerMontantTotal(List<CommandeProduit> lignes) {
        int total = 0;
        for (CommandeProduit cp : lignes) {
            if (cp.getProduit() != null) {
                total += (int) Math.round(cp.getProduit().getPrix_produit() * 100) * cp.getQuantite_commandee();
            }
        }
        return total;
    }

    public boolean validerCommandeDepuisPanier(User user, List<CommandeProduit> lignes) {
        if (user == null || !isUserValid(user)) {
            System.out.println("Utilisateur invalide. Commande annulée.");
            return false;
        }

        if (lignes == null || lignes.isEmpty()) {
            System.out.println("Aucune ligne de commande.");
            return false;
        }

        try {
            cn.setAutoCommit(false);

            for (CommandeProduit cp : lignes) {
                if (cp.getProduit() == null) {
                    throw new SQLException("Produit null dans une ligne.");
                }

                Produit produitBDD = recupererProduitParId(cp.getProduit().getId_produit());
                if (produitBDD == null) {
                    throw new SQLException("Produit introuvable : " + cp.getProduit().getId_produit());
                }

                if (produitBDD.getQuantite_produit() < cp.getQuantite_commandee()) {
                    throw new SQLException("Stock insuffisant pour le produit : " + produitBDD.getNom_produit());
                }
            }

            Commande commande = new Commande();
            commande.setUser(user);
            commande.setDate_creation_commande(LocalDateTime.now());
            commande.setStatut_commande("confirmée");
            commande.setStripe_session_id(null);
            commande.setPaid_at(null);
            commande.setCommande_produits(lignes);
            commande.setMontant_total_cents(calculerMontantTotal(lignes));

            String sqlCommande = "INSERT INTO commande (user_id, date_creation_commande, statut_commande, stripe_session_id, paid_at, montant_total_cents) VALUES (?, ?, ?, ?, ?, ?)";
            int idCommande;

            try (PreparedStatement ps = cn.prepareStatement(sqlCommande, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, user.getId());
                ps.setTimestamp(2, Timestamp.valueOf(commande.getDate_creation_commande()));
                ps.setString(3, commande.getStatut_commande());
                ps.setNull(4, Types.VARCHAR);
                ps.setNull(5, Types.TIMESTAMP);
                ps.setInt(6, commande.getMontant_total_cents());

                int rows = ps.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("Insertion commande échouée.");
                }

                ResultSet rs = ps.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("ID commande non récupéré.");
                }

                idCommande = rs.getInt(1);
            }

            String sqlLigne = "INSERT INTO commande_produit (quantite_commandee, commande_id, produit_id) VALUES (?, ?, ?)";
            try (PreparedStatement psLigne = cn.prepareStatement(sqlLigne)) {
                for (CommandeProduit cp : lignes) {
                    psLigne.setInt(1, cp.getQuantite_commandee());
                    psLigne.setInt(2, idCommande);
                    psLigne.setInt(3, cp.getProduit().getId_produit());
                    psLigne.executeUpdate();
                }
            }

            String sqlUpdateStock = "UPDATE produit SET quantite_produit = ?, status_produit = ? WHERE id_produit = ?";
            try (PreparedStatement psStock = cn.prepareStatement(sqlUpdateStock)) {
                for (CommandeProduit cp : lignes) {
                    Produit produitBDD = recupererProduitParId(cp.getProduit().getId_produit());
                    int nouveauStock = produitBDD.getQuantite_produit() - cp.getQuantite_commandee();
                    String nouveauStatut = (nouveauStock <= 0) ? "Rupture" : "Disponible";

                    psStock.setInt(1, nouveauStock);
                    psStock.setString(2, nouveauStatut);
                    psStock.setInt(3, cp.getProduit().getId_produit());
                    psStock.executeUpdate();
                }
            }

            cn.commit();
            cn.setAutoCommit(true);

            System.out.println("Commande validée avec succès.");
            return true;

        } catch (SQLException ex) {
            try {
                cn.rollback();
                cn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Erreur rollback : " + e.getMessage());
            }
            System.out.println("Erreur validation commande : " + ex.getMessage());
            return false;
        }
    }

    private Produit recupererProduitParId(int id) {
        String sql = "SELECT * FROM produit WHERE id_produit = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Produit(
                        rs.getInt("id_produit"),
                        rs.getString("nom_produit"),
                        rs.getString("description_produit"),
                        rs.getDouble("prix_produit"),
                        rs.getInt("quantite_produit"),
                        rs.getString("image_produit"),
                        rs.getString("categorie_produit"),
                        rs.getString("status_produit")
                );
            }
        } catch (SQLException e) {
            System.out.println("Erreur récupération produit : " + e.getMessage());
        }
        return null;
    }

    @Override
    public void ajouter(Commande c) {
        if (c.getUser() == null || !isUserValid(c.getUser())) {
            System.out.println("Utilisateur invalide. Commande annulée.");
            return;
        }

        c.setMontant_total_cents(calculerMontantTotal(c.getCommande_produits()));

        String sql = "INSERT INTO commande (user_id, date_creation_commande, statut_commande, stripe_session_id, paid_at, montant_total_cents) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, c.getUser().getId());
            ps.setTimestamp(2, Timestamp.valueOf(c.getDate_creation_commande()));
            ps.setString(3, c.getStatut_commande());
            ps.setString(4, c.getStripe_session_id());

            if (c.getPaid_at() != null)
                ps.setTimestamp(5, Timestamp.valueOf(c.getPaid_at()));
            else
                ps.setNull(5, Types.TIMESTAMP);

            ps.setInt(6, c.getMontant_total_cents());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                System.out.println("Erreur lors de l'ajout.");
                return;
            }

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                c.setId_commande(rs.getInt(1));

                String sqlLigne = "INSERT INTO commande_produit (quantite_commandee, commande_id, produit_id) VALUES (?, ?, ?)";
                try (PreparedStatement psL = cn.prepareStatement(sqlLigne)) {
                    for (CommandeProduit cp : c.getCommande_produits()) {
                        if (cp.getProduit() == null || !isProduitValid(cp.getProduit().getId_produit())) continue;
                        psL.setInt(1, cp.getQuantite_commandee());
                        psL.setInt(2, c.getId_commande());
                        psL.setInt(3, cp.getProduit().getId_produit());
                        psL.executeUpdate();
                    }
                }

                System.out.println("Commande ajoutée. ID = " + c.getId_commande());
            }

        } catch (SQLException ex) {
            System.out.println("Erreur ajout : " + ex.getMessage());
        }
    }

    @Override
    public void modifier(Commande c) {
        c.setMontant_total_cents(calculerMontantTotal(c.getCommande_produits()));

        String sql = "UPDATE commande SET user_id=?, date_creation_commande=?, statut_commande=?, stripe_session_id=?, paid_at=?, montant_total_cents=? WHERE id_commande=?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, c.getUser().getId());
            ps.setTimestamp(2, Timestamp.valueOf(c.getDate_creation_commande()));
            ps.setString(3, c.getStatut_commande());
            ps.setString(4, c.getStripe_session_id());

            if (c.getPaid_at() != null)
                ps.setTimestamp(5, Timestamp.valueOf(c.getPaid_at()));
            else
                ps.setNull(5, Types.TIMESTAMP);

            ps.setInt(6, c.getMontant_total_cents());
            ps.setInt(7, c.getId_commande());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                System.out.println("Commande introuvable.");
                return;
            }

            try (PreparedStatement psDel = cn.prepareStatement("DELETE FROM commande_produit WHERE commande_id=?")) {
                psDel.setInt(1, c.getId_commande());
                psDel.executeUpdate();
            }

            String sqlLigne = "INSERT INTO commande_produit (quantite_commandee, commande_id, produit_id) VALUES (?, ?, ?)";
            try (PreparedStatement psL = cn.prepareStatement(sqlLigne)) {
                for (CommandeProduit cp : c.getCommande_produits()) {
                    if (cp.getProduit() == null || !isProduitValid(cp.getProduit().getId_produit())) continue;
                    psL.setInt(1, cp.getQuantite_commandee());
                    psL.setInt(2, c.getId_commande());
                    psL.setInt(3, cp.getProduit().getId_produit());
                    psL.executeUpdate();
                }
            }

            System.out.println("Commande modifiée avec succès.");

        } catch (SQLException ex) {
            System.out.println("Erreur modification : " + ex.getMessage());
        }
    }

    @Override
    public void supprimer(Commande c) {
        try {
            try (PreparedStatement psDel = cn.prepareStatement("DELETE FROM commande_produit WHERE commande_id=?")) {
                psDel.setInt(1, c.getId_commande());
                psDel.executeUpdate();
            }

            try (PreparedStatement ps = cn.prepareStatement("DELETE FROM commande WHERE id_commande=?")) {
                ps.setInt(1, c.getId_commande());
                int rows = ps.executeUpdate();
                System.out.println(rows > 0 ? "Commande supprimée." : "Commande introuvable.");
            }

        } catch (SQLException ex) {
            System.out.println("Erreur suppression : " + ex.getMessage());
        }
    }

    @Override
    public List<Commande> recuperer() {
        List<Commande> liste = new ArrayList<>();

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery("""
SELECT c.*, u.nom, u.prenom, u.email_user, u.telephone_user, u.adresse_user, u.cin
FROM commande c
LEFT JOIN user u ON c.user_id = u.id
""");) {

            while (rs.next()) {
                liste.add(buildCommandeFromResultSet(rs));
            }

        } catch (SQLException ex) {
            System.out.println("Erreur récupération : " + ex.getMessage());
        }

        return liste;
    }

    public List<Commande> recupererParUserId(int userId) {
        List<Commande> liste = new ArrayList<>();
        if (userId <= 0) {
            return liste;
        }

        String sql = """
        SELECT c.*, u.nom, u.prenom, u.email_user, u.telephone_user, u.adresse_user, u.cin
        FROM commande c
        LEFT JOIN user u ON c.user_id = u.id
        WHERE c.user_id = ?
        ORDER BY c.date_creation_commande DESC
        """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(buildCommandeFromResultSet(rs));
                }
            }
        } catch (SQLException ex) {
            System.out.println("Erreur récupération par utilisateur : " + ex.getMessage());
        }

        return liste;
    }

    @Override
    public Commande recupererParId(int id) {
        String sql = """
            SELECT c.*, u.nom, u.prenom, u.email_user, u.telephone_user, u.adresse_user, u.cin
            FROM commande c
            LEFT JOIN user u ON c.user_id = u.id
            WHERE c.id_commande = ?
            """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Commande c = new Commande();
                c.setId_commande(rs.getInt("id_commande"));

                User u = new User();
                u.setId(rs.getInt("user_id"));
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));
                u.setEmailUser(rs.getString("email_user"));
                u.setTelephoneUser(rs.getString("telephone_user"));
                u.setAdresseUser(rs.getString("adresse_user"));
                u.setCin(rs.getString("cin"));
                c.setUser(u);

                Timestamp dateCreation = rs.getTimestamp("date_creation_commande");
                if (dateCreation != null) {
                    c.setDate_creation_commande(dateCreation.toLocalDateTime());
                }

                c.setStatut_commande(rs.getString("statut_commande"));
                c.setStripe_session_id(rs.getString("stripe_session_id"));

                Timestamp paidAt = rs.getTimestamp("paid_at");
                if (paidAt != null) {
                    c.setPaid_at(paidAt.toLocalDateTime());
                }

                c.setMontant_total_cents(rs.getInt("montant_total_cents"));
                c.setCommande_produits(chargerLignesCommande(c.getId_commande()));

                return c;
            }

        } catch (SQLException ex) {
            System.out.println("Erreur récupération ID : " + ex.getMessage());
        }

        return null;
    }

    private boolean isUserValid(User user) {
        try (PreparedStatement ps = cn.prepareStatement("SELECT id FROM user WHERE id=?")) {
            ps.setInt(1, user.getId());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    private Commande buildCommandeFromResultSet(ResultSet rs) throws SQLException {
        Commande c = new Commande();
        c.setId_commande(rs.getInt("id_commande"));

        User u = new User();
        u.setId(rs.getInt("user_id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmailUser(rs.getString("email_user"));
        u.setTelephoneUser(rs.getString("telephone_user"));
        u.setAdresseUser(rs.getString("adresse_user"));
        u.setCin(rs.getString("cin"));
        c.setUser(u);

        Timestamp date = rs.getTimestamp("date_creation_commande");
        if (date != null) {
            c.setDate_creation_commande(date.toLocalDateTime());
        }

        c.setStatut_commande(rs.getString("statut_commande"));
        c.setMontant_total_cents(rs.getInt("montant_total_cents"));
        c.setCommande_produits(chargerLignesCommande(c.getId_commande()));
        return c;
    }

    private List<CommandeProduit> chargerLignesCommande(int commandeId) throws SQLException {
        List<CommandeProduit> lignes = new ArrayList<>();

        try (PreparedStatement psL = cn.prepareStatement("SELECT * FROM commande_produit WHERE commande_id=?")) {
            psL.setInt(1, commandeId);
            try (ResultSet rsL = psL.executeQuery()) {
                while (rsL.next()) {
                    CommandeProduit cp = new CommandeProduit();
                    cp.setId_ligne_commande(rsL.getInt("id_ligne_commande"));
                    cp.setQuantite_commandee(rsL.getInt("quantite_commandee"));

                    int produitId = rsL.getInt("produit_id");
                    cp.setProduit(recupererProduitParId(produitId));
                    lignes.add(cp);
                }
            }
        }

        return lignes;
    }

    private boolean isProduitValid(int id) {
        try (PreparedStatement ps = cn.prepareStatement("SELECT id_produit FROM produit WHERE id_produit=?")) {
            ps.setInt(1, id);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    private void verifierStockDisponible(List<CommandeProduit> lignes) throws SQLException {
        for (CommandeProduit cp : lignes) {
            if (cp.getProduit() == null) {
                throw new SQLException("Produit null dans une ligne.");
            }

            Produit produitBDD = recupererProduitParId(cp.getProduit().getId_produit());
            if (produitBDD == null) {
                throw new SQLException("Produit introuvable : " + cp.getProduit().getId_produit());
            }

            if (produitBDD.getQuantite_produit() < cp.getQuantite_commandee()) {
                throw new SQLException("Stock insuffisant pour le produit : " + produitBDD.getNom_produit());
            }
        }
    }
    public Commande creerCommandeEnAttentePaiement(User user, List<CommandeProduit> lignes) {
        if (user == null || !isUserValid(user)) {
            System.out.println("Utilisateur invalide. Commande annulée.");
            return null;
        }

        if (lignes == null || lignes.isEmpty()) {
            System.out.println("Aucune ligne de commande.");
            return null;
        }

        try {
            cn.setAutoCommit(false);

            for (CommandeProduit cp : lignes) {
                if (cp.getProduit() == null) {
                    throw new SQLException("Produit null dans une ligne.");
                }

                Produit produitBDD = recupererProduitParId(cp.getProduit().getId_produit());
                if (produitBDD == null) {
                    throw new SQLException("Produit introuvable : " + cp.getProduit().getId_produit());
                }

                if (produitBDD.getQuantite_produit() < cp.getQuantite_commandee()) {
                    throw new SQLException("Stock insuffisant pour le produit : " + produitBDD.getNom_produit());
                }
            }

            Commande commande = new Commande();
            commande.setUser(user);
            commande.setDate_creation_commande(LocalDateTime.now());
            commande.setStatut_commande("En attente paiement");
            commande.setStripe_session_id(null);
            commande.setPaid_at(null);
            commande.setCommande_produits(lignes);
            commande.setMontant_total_cents(calculerMontantTotal(lignes));

            String sqlCommande = "INSERT INTO commande (user_id, date_creation_commande, statut_commande, stripe_session_id, paid_at, montant_total_cents) VALUES (?, ?, ?, ?, ?, ?)";
            int idCommande;

            try (PreparedStatement ps = cn.prepareStatement(sqlCommande, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, user.getId());
                ps.setTimestamp(2, Timestamp.valueOf(commande.getDate_creation_commande()));
                ps.setString(3, commande.getStatut_commande());
                ps.setNull(4, Types.VARCHAR);
                ps.setNull(5, Types.TIMESTAMP);
                ps.setInt(6, commande.getMontant_total_cents());

                int rows = ps.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("Insertion commande échouée.");
                }

                ResultSet rs = ps.getGeneratedKeys();
                if (!rs.next()) {
                    throw new SQLException("ID commande non récupéré.");
                }

                idCommande = rs.getInt(1);
                commande.setId_commande(idCommande);
            }

            String sqlLigne = "INSERT INTO commande_produit (quantite_commandee, commande_id, produit_id) VALUES (?, ?, ?)";
            try (PreparedStatement psLigne = cn.prepareStatement(sqlLigne)) {
                for (CommandeProduit cp : lignes) {
                    psLigne.setInt(1, cp.getQuantite_commandee());
                    psLigne.setInt(2, idCommande);
                    psLigne.setInt(3, cp.getProduit().getId_produit());
                    psLigne.executeUpdate();
                }
            }

            cn.commit();
            cn.setAutoCommit(true);

            System.out.println("Commande créée en attente paiement. ID = " + idCommande);
            return commande;

        } catch (SQLException ex) {
            try {
                cn.rollback();
                cn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Erreur rollback : " + e.getMessage());
            }
            System.out.println("Erreur création commande en attente : " + ex.getMessage());
            return null;
        }
    }

    public boolean enregistrerStripeSessionId(int commandeId, String stripeSessionId) {
        String sql = "UPDATE commande SET stripe_session_id = ? WHERE id_commande = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, stripeSessionId);
            ps.setInt(2, commandeId);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException ex) {
            System.out.println("Erreur enregistrement stripe_session_id : " + ex.getMessage());
            return false;
        }
    }
    public boolean marquerCommandeCommePayee(int commandeId, String stripeSessionId) {
        try {
            cn.setAutoCommit(false);

            Commande commande = recupererParId(commandeId);
            if (commande == null) {
                throw new SQLException("Commande introuvable.");
            }

            for (CommandeProduit cp : commande.getCommande_produits()) {
                if (cp.getProduit() == null) {
                    throw new SQLException("Produit null dans une ligne.");
                }

                Produit produitBDD = recupererProduitParId(cp.getProduit().getId_produit());
                if (produitBDD == null) {
                    throw new SQLException("Produit introuvable : " + cp.getProduit().getId_produit());
                }

                if (produitBDD.getQuantite_produit() < cp.getQuantite_commandee()) {
                    throw new SQLException("Stock insuffisant pour le produit : " + produitBDD.getNom_produit());
                }
            }

            String sqlCommande = "UPDATE commande SET statut_commande = ?, stripe_session_id = ?, paid_at = ? WHERE id_commande = ?";
            try (PreparedStatement ps = cn.prepareStatement(sqlCommande)) {
                ps.setString(1, "En cours");
                ps.setString(2, stripeSessionId);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(4, commandeId);
                ps.executeUpdate();
            }

            String sqlUpdateStock = "UPDATE produit SET quantite_produit = ?, status_produit = ? WHERE id_produit = ?";
            try (PreparedStatement psStock = cn.prepareStatement(sqlUpdateStock)) {
                for (CommandeProduit cp : commande.getCommande_produits()) {
                    Produit produitBDD = recupererProduitParId(cp.getProduit().getId_produit());

                    int nouveauStock = produitBDD.getQuantite_produit() - cp.getQuantite_commandee();
                    String nouveauStatut = (nouveauStock <= 0) ? "Rupture" : "Disponible";

                    psStock.setInt(1, nouveauStock);
                    psStock.setString(2, nouveauStatut);
                    psStock.setInt(3, cp.getProduit().getId_produit());
                    psStock.executeUpdate();
                }
            }

            cn.commit();
            cn.setAutoCommit(true);

            System.out.println("Commande marquée en cours avec succès.");
            return true;

        } catch (SQLException ex) {
            try {
                cn.rollback();
                cn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Erreur rollback : " + e.getMessage());
            }
            System.out.println("Erreur marquage commande payée : " + ex.getMessage());
            return false;
        }
    }


}