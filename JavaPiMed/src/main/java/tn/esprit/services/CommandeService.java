package tn.esprit.services;

import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.entities.User;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeService implements IGeneralService<Commande> {

    private Connection cn;

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


    @Override
    public void ajouter(Commande c) {

        if (c.getUser() == null || !isUserValid(c.getUser()))  {
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
            if (rows == 0) { System.out.println("Erreur lors de l'ajout."); return; }

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
            if (rows == 0) { System.out.println("Commande introuvable."); return; }

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

                        // produit comme objet — même pattern que ProduitService
                        int produitId = rsL.getInt("produit_id");
                        try (PreparedStatement psP = cn.prepareStatement("SELECT * FROM produit WHERE id_produit=?")) {
                            psP.setInt(1, produitId);
                            ResultSet rsP = psP.executeQuery();
                            if (rsP.next()) {
                                Produit p = new Produit(
                                        rsP.getInt("id_produit"),
                                        rsP.getString("nom_produit"),
                                        rsP.getString("description_produit"),
                                        rsP.getDouble("prix_produit"),
                                        rsP.getInt("quantite_produit"),
                                        rsP.getString("image_produit"),
                                        rsP.getString("categorie_produit"),
                                        rsP.getString("status_produit")
                                );
                                cp.setProduit(p);
                            }
                        }
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

                // ----- charger lignes inline -----
                List<CommandeProduit> lignes = new ArrayList<>();
                try (PreparedStatement psL = cn.prepareStatement("SELECT * FROM commande_produit WHERE commande_id=?")) {
                    psL.setInt(1, id);
                    ResultSet rsL = psL.executeQuery();
                    while (rsL.next()) {
                        CommandeProduit cp = new CommandeProduit();
                        cp.setId_ligne_commande(rsL.getInt("id_ligne_commande"));
                        cp.setQuantite_commandee(rsL.getInt("quantite_commandee"));

                        // produit comme objet — même pattern que ProduitService
                        int produitId = rsL.getInt("produit_id");
                        try (PreparedStatement psP = cn.prepareStatement("SELECT * FROM produit WHERE id_produit=?")) {
                            psP.setInt(1, produitId);
                            ResultSet rsP = psP.executeQuery();
                            if (rsP.next()) {
                                Produit p = new Produit(
                                        rsP.getInt("id_produit"),
                                        rsP.getString("nom_produit"),
                                        rsP.getString("description_produit"),
                                        rsP.getDouble("prix_produit"),
                                        rsP.getInt("quantite_produit"),
                                        rsP.getString("image_produit"),
                                        rsP.getString("categorie_produit"),
                                        rsP.getString("status_produit")
                                );
                                cp.setProduit(p);
                            }
                        }
                        lignes.add(cp);
                    }
                }
                c.setCommande_produits(lignes);
                // ----------------------------------

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
        } catch (SQLException e) { return false; }
    }

    private boolean isProduitValid(int id) {
        try (PreparedStatement ps = cn.prepareStatement("SELECT id_produit FROM produit WHERE id_produit=?")) {
            ps.setInt(1, id);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }
}