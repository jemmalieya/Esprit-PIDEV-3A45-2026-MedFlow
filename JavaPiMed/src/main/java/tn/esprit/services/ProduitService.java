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
            System.out.println(" Produit ajouté avec succès");

        } catch (SQLException ex) {
            System.out.println(" Erreur lors de l'ajout du produit : " + ex.getMessage());
        }
    }

    @Override
    public void supprimer(Produit p) {
        String sql = "DELETE FROM produit WHERE id_produit = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, p.getId_produit());
            ps.executeUpdate();
            System.out.println(" Produit supprimé avec succès");

        } catch (SQLException ex) {
            System.out.println(" Erreur lors de la suppression : " + ex.getMessage());
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
            System.out.println("✏ Produit modifié avec succès");

        } catch (SQLException ex) {
            System.out.println(" Erreur lors de la modification : " + ex.getMessage());
        }
    }

    @Override
    public List<Produit> recuperer() {
        List<Produit> produits = new ArrayList<>();
        String sql = "SELECT * FROM produit";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Produit p = new Produit(
                        rs.getInt("id_produit"),
                        rs.getString("nom_produit"),
                        rs.getString("description_produit"),
                        rs.getDouble("prix_produit"),
                        rs.getInt("quantite_produit"),
                        rs.getString("image_produit"),
                        rs.getString("categorie_produit"),
                        rs.getString("status_produit")
                );

                produits.add(p);
            }

            System.out.println(" Liste des produits récupérée avec succès");

        } catch (SQLException ex) {
            System.out.println(" Erreur lors de la récupération : " + ex.getMessage());
        }

        return produits;
    }


    @Override
    public Produit recupererParId(int id) {
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

        } catch (SQLException ex) {
            System.out.println(" Erreur : " + ex.getMessage());
        }

        System.out.println("Produit non trouvé");
        return null;
    }
    public boolean produitExisteDeja(String nom, String categorie, double prix, String description) {
        String sql = """
        SELECT COUNT(*)
        FROM produit
        WHERE LOWER(TRIM(nom_produit)) = LOWER(TRIM(?))
          AND LOWER(TRIM(categorie_produit)) = LOWER(TRIM(?))
          AND prix_produit = ?
          AND LOWER(TRIM(description_produit)) = LOWER(TRIM(?))
    """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setString(2, categorie);
            ps.setDouble(3, prix);
            ps.setString(4, description);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Erreur test unicité produit : " + e.getMessage());
        }

        return false;
    }
}