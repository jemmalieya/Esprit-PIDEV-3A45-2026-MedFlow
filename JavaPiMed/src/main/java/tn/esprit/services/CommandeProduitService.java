package tn.esprit.services;

import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandeProduitService implements IGeneralService<CommandeProduit> {

    Connection cn;

    public CommandeProduitService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void ajouter(CommandeProduit cp) {
        String sql = "INSERT INTO commande_produit (quantite_commandee, commande_id, produit_id) VALUES (?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cp.getQuantite_commandee());
            ps.setInt(2, cp.getCommande().getId_commande());
            ps.setInt(3, cp.getProduit().getId_produit());

            ps.executeUpdate();
            System.out.println("✅ Ligne de commande ajoutée avec succès");

        } catch (SQLException ex) {
            System.out.println("❌ Erreur lors de l'ajout de la ligne de commande : " + ex.getMessage());
        }
    }

    @Override
    public void supprimer(CommandeProduit cp) {
        String sql = "DELETE FROM commande_produit WHERE id_ligne_commande = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cp.getId_ligne_commande());

            ps.executeUpdate();
            System.out.println("🗑 Ligne de commande supprimée avec succès");

        } catch (SQLException ex) {
            System.out.println(" Erreur lors de la suppression de la ligne de commande : " + ex.getMessage());
        }
    }

    @Override
    public void modifier(CommandeProduit cp) {
        String sql = "UPDATE commande_produit SET quantite_commandee = ?, commande_id = ?, produit_id = ? WHERE id_ligne_commande = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cp.getQuantite_commandee());
            ps.setInt(2, cp.getCommande().getId_commande());
            ps.setInt(3, cp.getProduit().getId_produit());
            ps.setInt(4, cp.getId_ligne_commande());

            ps.executeUpdate();
            System.out.println("✏ Ligne de commande modifiée avec succès");

        } catch (SQLException ex) {
            System.out.println("❌ Erreur lors de la modification de la ligne de commande : " + ex.getMessage());
        }
    }

    @Override
    public List<CommandeProduit> recuperer() {
        List<CommandeProduit> liste = new ArrayList<>();
        String sql = "SELECT * FROM commande_produit";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                CommandeProduit cp = new CommandeProduit();

                cp.setId_ligne_commande(rs.getInt("id_ligne_commande"));
                cp.setQuantite_commandee(rs.getInt("quantite_commandee"));

                Commande commande = new Commande();
                commande.setId_commande(rs.getInt("commande_id"));
                cp.setCommande(commande);

                Produit produit = new Produit();
                produit.setId_produit(rs.getInt("produit_id"));
                cp.setProduit(produit);

                liste.add(cp);
            }

            System.out.println("📦 Liste des lignes de commande récupérée avec succès");

        } catch (SQLException ex) {
            System.out.println("❌ Erreur lors de la récupération des lignes de commande : " + ex.getMessage());
        }

        return liste;
    }

    @Override
    public CommandeProduit recupererParId(int id) {
        String sql = "SELECT * FROM commande_produit WHERE id_ligne_commande = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    CommandeProduit cp = new CommandeProduit();

                    cp.setId_ligne_commande(rs.getInt("id_ligne_commande"));
                    cp.setQuantite_commandee(rs.getInt("quantite_commandee"));

                    Commande commande = new Commande();
                    commande.setId_commande(rs.getInt("commande_id"));
                    cp.setCommande(commande);

                    Produit produit = new Produit();
                    produit.setId_produit(rs.getInt("produit_id"));
                    cp.setProduit(produit);

                    return cp;
                }
            }

        } catch (SQLException ex) {
            System.out.println("❌ Erreur lors de la récupération par ID : " + ex.getMessage());
        }

        System.out.println("❌ Ligne de commande avec ID " + id + " non trouvée");
        return null;
    }
}