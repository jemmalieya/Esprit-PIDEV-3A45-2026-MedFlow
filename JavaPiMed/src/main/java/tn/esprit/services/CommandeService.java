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
                total += (int) (cp.getProduit().getPrix_produit() * cp.getQuantite_commandee());
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
             ResultSet rs = st.executeQuery("SELECT * FROM commande")) {

            while (rs.next()) {

                Commande c = new Commande();
                c.setId_commande(rs.getInt("id_commande"));

                User u = new User();
                u.setId(rs.getInt("user_id"));
                c.setUser(u);

                c.setDate_creation_commande(rs.getTimestamp("date_creation_commande").toLocalDateTime());
                c.setStatut_commande(rs.getString("statut_commande"));
                c.setMontant_total_cents(rs.getInt("montant_total_cents"));

                List<CommandeProduit> lignes = new ArrayList<>();
                try (PreparedStatement psL = cn.prepareStatement("SELECT * FROM commande_produit WHERE commande_id=?")) {
                    psL.setInt(1, c.getId_commande());
                    ResultSet rsL = psL.executeQuery();
                    while (rsL.next()) {
                        CommandeProduit cp = new CommandeProduit();
                        cp.setId_ligne_commande(rsL.getInt("id_ligne_commande"));
                        cp.setQuantite_commandee(rsL.getInt("quantite_commandee"));

                        int produitId = rsL.getInt("produit_id");
                        Produit p = recupererProduitParId(produitId);
                        cp.setProduit(p);

                        lignes.add(cp);
                    }
                }
                c.setCommande_produits(lignes);

                liste.add(c);
            }

        } catch (SQLException ex) {
            System.out.println("Erreur récupération : " + ex.getMessage());
        }

        return liste;
    }

    @Override
    public Commande recupererParId(int id) {
        try (PreparedStatement ps = cn.prepareStatement("SELECT * FROM commande WHERE id_commande=?")) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Commande c = new Commande();
                c.setId_commande(id);

                User u = new User();
                u.setId(rs.getInt("user_id"));
                c.setUser(u);

                c.setDate_creation_commande(rs.getTimestamp("date_creation_commande").toLocalDateTime());
                c.setStatut_commande(rs.getString("statut_commande"));
                c.setMontant_total_cents(rs.getInt("montant_total_cents"));

                List<CommandeProduit> lignes = new ArrayList<>();
                try (PreparedStatement psL = cn.prepareStatement("SELECT * FROM commande_produit WHERE commande_id=?")) {
                    psL.setInt(1, id);
                    ResultSet rsL = psL.executeQuery();
                    while (rsL.next()) {
                        CommandeProduit cp = new CommandeProduit();
                        cp.setId_ligne_commande(rsL.getInt("id_ligne_commande"));
                        cp.setQuantite_commandee(rsL.getInt("quantite_commandee"));

                        int produitId = rsL.getInt("produit_id");
                        Produit p = recupererProduitParId(produitId);
                        cp.setProduit(p);

                        lignes.add(cp);
                    }
                }
                c.setCommande_produits(lignes);

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

    private boolean isProduitValid(int id) {
        try (PreparedStatement ps = cn.prepareStatement("SELECT id_produit FROM produit WHERE id_produit=?")) {
            ps.setInt(1, id);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }
}