package tn.esprit.services;

import tn.esprit.entities.Prescription;
import tn.esprit.entities.Produit;
import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.*;
public class ProduitPrescriptionService {
    private final Connection cn;

    public ProduitPrescriptionService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Récupère toutes les prescriptions du patient connecté
    // via : prescription → fiche_medicale → rendez_vous → idPatient
    // ─────────────────────────────────────────────────────────────────────────

    public List<Prescription> getPrescriptionsParPatient(int idPatient) {
        List<Prescription> liste = new ArrayList<>();

        String sql = """
                SELECT p.id, p.fiche_medicale_id, p.nom_medicament,
                       p.dose, p.frequence, p.duree, p.instructions, p.created_at
                FROM prescription p
                JOIN fiche_medicale fm ON p.fiche_medicale_id = fm.id
                JOIN rendez_vous rv    ON fm.rendez_vous_id   = rv.id
                WHERE rv.idPatient = ?
                ORDER BY p.created_at DESC
                """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idPatient);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Prescription pr = new Prescription();
                pr.setId(rs.getInt("id"));
                pr.setFiche_medicale_id(rs.getInt("fiche_medicale_id"));
                pr.setNom_medicament(rs.getString("nom_medicament"));
                pr.setDose(rs.getString("dose"));
                pr.setFrequence(rs.getString("frequence"));
                pr.setDuree(rs.getInt("duree"));
                pr.setInstructions(rs.getString("instructions"));
                pr.setCreated_at(rs.getTimestamp("created_at"));
                liste.add(pr);
            }

            System.out.println("[ProduitPrescription] "
                    + liste.size() + " prescription(s) trouvée(s) pour patient id=" + idPatient);

        } catch (SQLException e) {
            System.out.println("[ProduitPrescription] Erreur SQL getPrescriptions : " + e.getMessage());
        }

        return liste;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cherche un produit en stock par nom (correspondance floue LIKE)
    // ─────────────────────────────────────────────────────────────────────────

    public Produit trouverProduitParNom(String nomMedicament) {
        if (nomMedicament == null || nomMedicament.isBlank()) return null;

        String sql = "SELECT * FROM produit WHERE LOWER(nom_produit) LIKE ? LIMIT 1";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, "%" + nomMedicament.toLowerCase(Locale.ROOT) + "%");
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
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
                System.out.println("[ProduitPrescription] ✓ Produit trouvé : " + p.getNom_produit());
                return p;
            }

        } catch (SQLException e) {
            System.out.println("[ProduitPrescription] Erreur SQL trouverProduit : " + e.getMessage());
        }

        System.out.println("[ProduitPrescription] ✗ Aucun produit trouvé pour : " + nomMedicament);
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Correspondance floue entre deux noms de médicaments
    // ─────────────────────────────────────────────────────────────────────────

    public boolean correspondanceFloue(String nom1, String nom2) {
        if (nom1 == null || nom2 == null) return false;
        String a = nom1.toLowerCase(Locale.ROOT).trim();
        String b = nom2.toLowerCase(Locale.ROOT).trim();
        if (a.equals(b)) return true;
        if (a.contains(b) || b.contains(a)) return true;
        if (a.length() >= 6 && b.length() >= 6 && a.substring(0, 6).equals(b.substring(0, 6))) return true;
        return false;
    }
}
