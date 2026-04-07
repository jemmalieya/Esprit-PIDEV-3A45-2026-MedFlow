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
        String sql = "INSERT INTO commande_produit (" +
                "quantite_commandee, commande_id, produit_id" +
                ") VALUES (?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cp.getQuantite_commandee());
            ps.setInt(2, cp.getCommande().getId_commande());  // ID de la commande
            ps.setInt(3, cp.getProduit().getId_produit());  // ID du produit

            ps.executeUpdate();
            System.out.println("✅ CommandeProduit ajoutée");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void supprimer(CommandeProduit cp) {
        String sql = "DELETE FROM commande_produit WHERE id_ligne_commande = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, cp.getId_ligne_commande());
            ps.executeUpdate();
            System.out.println("🗑️ CommandeProduit supprimée avec succès");
        } catch (SQLException ex) {
            ex.printStackTrace();
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
            System.out.println("✏️ CommandeProduit modifiée avec succès");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public List<CommandeProduit> recuperer() {
        List<CommandeProduit> list = new ArrayList<>();
        String sql = "SELECT * FROM commande_produit";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                CommandeProduit cp = new CommandeProduit();

                cp.setId_ligne_commande(rs.getInt("id_ligne_commande"));
                cp.setQuantite_commandee(rs.getInt("quantite_commandee"));
                cp.setCommande(new Commande(rs.getInt("commande_id")));  // Assumer que tu récupères l'objet Commande
                cp.setProduit(new Produit(rs.getInt("produit_id")));  // Assumer que tu récupères l'objet Produit

                list.add(cp);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }
}