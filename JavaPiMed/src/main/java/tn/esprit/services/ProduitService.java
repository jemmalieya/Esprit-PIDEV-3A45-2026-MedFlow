package tn.esprit.services;

import tn.esprit.entities.Produit;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitService implements IGeneralService<Produit> {

    Connection cn;

    public ProduitService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void ajouter(Produit p) {
        String sql = "INSERT INTO produit (" +
                "nom_produit, description_produit, prix_produit, quantite_produit, image_produit, categorie_produit, status_produit" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, p.getNom_produit());
            ps.setString(2, p.getDescription_produit());
            ps.setDouble(3, p.getPrix_produit());
            ps.setInt(4, p.getQuantite_produit());
            ps.setString(5, p.getImage_produit());
            ps.setString(6, p.getCategorie_produit());
            ps.setString(7, p.getStatus_produit());

            ps.executeUpdate();
            System.out.println("✅ Produit ajouté");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void supprimer(Produit p) {
        String sql = "DELETE FROM produit WHERE id_produit = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, p.getId_produit());
            ps.executeUpdate();
            System.out.println("🗑️ Produit supprimé avec succès");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void modifier(Produit p) {
        String sql = "UPDATE produit SET nom_produit = ?, description_produit = ?, prix_produit = ?, quantite_produit = ?, image_produit = ?, categorie_produit = ?, status_produit = ? WHERE id_produit = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, p.getNom_produit());
            ps.setString(2, p.getDescription_produit());
            ps.setDouble(3, p.getPrix_produit());
            ps.setInt(4, p.getQuantite_produit());
            ps.setString(5, p.getImage_produit());
            ps.setString(6, p.getCategorie_produit());
            ps.setString(7, p.getStatus_produit());
            ps.setInt(8, p.getId_produit());

            ps.executeUpdate();
            System.out.println("✏️ Produit modifié avec succès");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public List<Produit> recuperer() {
        List<Produit> list = new ArrayList<>();
        String sql = "SELECT * FROM produit";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Produit p = new Produit();

                p.setId_produit(rs.getInt("id_produit"));
                p.setNom_produit(rs.getString("nom_produit"));
                p.setDescription_produit(rs.getString("description_produit"));
                p.setPrix_produit(rs.getDouble("prix_produit"));
                p.setQuantite_produit(rs.getInt("quantite_produit"));
                p.setImage_produit(rs.getString("image_produit"));
                p.setCategorie_produit(rs.getString("categorie_produit"));
                p.setStatus_produit(rs.getString("status_produit"));

                list.add(p);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return list;
    }
}