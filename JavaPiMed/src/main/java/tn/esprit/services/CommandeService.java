package tn.esprit.services;

import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.entities.User;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeService {

    private Connection cn;

    public CommandeService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    // ─────────────────────────────────────────
    // AJOUTER
    // ─────────────────────────────────────────
    public void ajouter(Commande c) {
        if (!isUserValid(c.getUser())) {
            System.out.println("❌ Utilisateur invalide. Commande annulée.");
            return;
        }

        String sql = "INSERT INTO commande " +
                "(user_id, date_creation_commande, statut_commande, stripe_session_id, paid_at, montant_total_cents) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

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

            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int commandeId = keys.getInt(1);
                c.setId_commande(commandeId);

                // Insérer les lignes commande_produit
                if (c.getCommande_produits() != null && !c.getCommande_produits().isEmpty()) {
                    String sqlLine = "INSERT INTO commande_produit (quantite_commandee, commande_id, produit_id) VALUES (?, ?, ?)";
                    try (PreparedStatement psLine = cn.prepareStatement(sqlLine)) {
                        for (CommandeProduit cp : c.getCommande_produits()) {
                            psLine.setInt(1, cp.getQuantite_commandee());
                            psLine.setInt(2, commandeId);
                            psLine.setInt(3, cp.getProduit().getId_produit());
                            psLine.executeUpdate();
                            System.out.println("   ✅ Produit ID " + cp.getProduit().getId_produit()
                                    + " ajouté (qté: " + cp.getQuantite_commandee() + ")");
                        }
                    }
                }
            }
            System.out.println("✅ Commande ajoutée avec succès (ID: " + c.getId_commande() + ")");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ─────────────────────────────────────────
    // MODIFIER
    // ─────────────────────────────────────────
    public void modifier(Commande c) {
        // 1. Mettre à jour la commande principale
        String sql = "UPDATE commande SET " +
                "user_id = ?, date_creation_commande = ?, statut_commande = ?, " +
                "stripe_session_id = ?, paid_at = ?, montant_total_cents = ? " +
                "WHERE id_commande = ?";

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
                System.out.println("❌ Aucune commande trouvée avec l'ID " + c.getId_commande());
                return;
            }

            // 2. Supprimer les anciennes lignes commande_produit
            String deleteLignes = "DELETE FROM commande_produit WHERE commande_id = ?";
            try (PreparedStatement psDel = cn.prepareStatement(deleteLignes)) {
                psDel.setInt(1, c.getId_commande());
                psDel.executeUpdate();
            }

            // 3. Réinsérer les nouvelles lignes
            if (c.getCommande_produits() != null && !c.getCommande_produits().isEmpty()) {
                String sqlLine = "INSERT INTO commande_produit (quantite_commandee, commande_id, produit_id) VALUES (?, ?, ?)";
                try (PreparedStatement psLine = cn.prepareStatement(sqlLine)) {
                    for (CommandeProduit cp : c.getCommande_produits()) {
                        psLine.setInt(1, cp.getQuantite_commandee());
                        psLine.setInt(2, c.getId_commande());
                        psLine.setInt(3, cp.getProduit().getId_produit());
                        psLine.executeUpdate();
                        System.out.println("   ✏️ Produit ID " + cp.getProduit().getId_produit()
                                + " mis à jour (qté: " + cp.getQuantite_commandee() + ")");
                    }
                }
            }

            System.out.println("✏️ Commande ID " + c.getId_commande() + " modifiée avec succès");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ─────────────────────────────────────────
    // SUPPRIMER
    // ─────────────────────────────────────────
    public void supprimer(int commandeId) {
        // 1. Supprimer d'abord les lignes liées (FK)
        String deleteLines = "DELETE FROM commande_produit WHERE commande_id = ?";
        try (PreparedStatement ps = cn.prepareStatement(deleteLines)) {
            ps.setInt(1, commandeId);
            int rows = ps.executeUpdate();
            System.out.println("   🗑️ " + rows + " ligne(s) commande_produit supprimée(s)");
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }

        // 2. Supprimer la commande
        String deleteCommande = "DELETE FROM commande WHERE id_commande = ?";
        try (PreparedStatement ps = cn.prepareStatement(deleteCommande)) {
            ps.setInt(1, commandeId);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("🗑️ Commande ID " + commandeId + " supprimée avec succès");
            else
                System.out.println("❌ Aucune commande trouvée avec l'ID " + commandeId);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ─────────────────────────────────────────
    // RECUPERER TOUTES LES COMMANDES
    // ─────────────────────────────────────────
    public List<Commande> recuperer() {
        List<Commande> commandesList = new ArrayList<>();
        String sql = "SELECT * FROM commande";

        try (Statement st = cn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Commande commande = new Commande();
                commande.setId_commande(rs.getInt("id_commande"));

                User user = new User();
                user.setId(rs.getInt("user_id"));
                commande.setUser(user);

                commande.setDate_creation_commande(rs.getTimestamp("date_creation_commande").toLocalDateTime());
                commande.setStatut_commande(rs.getString("statut_commande"));
                commande.setStripe_session_id(rs.getString("stripe_session_id"));

                Timestamp paidAt = rs.getTimestamp("paid_at");
                commande.setPaid_at(paidAt != null ? paidAt.toLocalDateTime() : null);

                commande.setMontant_total_cents(rs.getInt("montant_total_cents"));
                commandesList.add(commande);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return commandesList;
    }

    // ─────────────────────────────────────────
    // RECUPERER PAR ID
    // ─────────────────────────────────────────
    public Commande recupererParId(int id) {
        String sql = "SELECT * FROM commande WHERE id_commande = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Commande c = new Commande();
                c.setId_commande(rs.getInt("id_commande"));
                User user = new User();
                user.setId(rs.getInt("user_id"));
                c.setUser(user);
                c.setDate_creation_commande(rs.getTimestamp("date_creation_commande").toLocalDateTime());
                c.setStatut_commande(rs.getString("statut_commande"));
                c.setStripe_session_id(rs.getString("stripe_session_id"));
                Timestamp paidAt = rs.getTimestamp("paid_at");
                c.setPaid_at(paidAt != null ? paidAt.toLocalDateTime() : null);
                c.setMontant_total_cents(rs.getInt("montant_total_cents"));
                return c;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // ─────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────
    private boolean isUserValid(User user) {
        String sql = "SELECT id FROM user WHERE id = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            return ps.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public Produit recupererProduitParId(int produitId) {
        String sql = "SELECT * FROM produit WHERE id_produit = ?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, produitId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Produit p = new Produit();
                p.setId_produit(rs.getInt("id_produit"));
                p.setNom_produit(rs.getString("nom_produit"));
                p.setDescription_produit(rs.getString("description_produit"));
                p.setPrix_produit(rs.getDouble("prix_produit"));
                p.setQuantite_produit(rs.getInt("quantite_produit"));
                p.setImage_produit(rs.getString("image_produit"));
                p.setCategorie_produit(rs.getString("categorie_produit"));
                p.setStatus_produit(rs.getString("status_produit"));
                return p;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}