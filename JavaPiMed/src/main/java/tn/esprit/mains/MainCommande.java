package tn.esprit.mains;

import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.entities.User;
import tn.esprit.services.CommandeService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MainCommande {

    static CommandeService service = new CommandeService();

    public static void main(String[] args) {

        // ══════════════════════════════
        // 1️⃣  AJOUTER
        // ══════════════════════════════
        System.out.println("\n========== AJOUTER ==========");

        Produit p1 = service.recupererProduitParId(6);
        Produit p2 = service.recupererProduitParId(8);

        List<CommandeProduit> lignes = new ArrayList<>();
        if (p1 != null) lignes.add(new CommandeProduit(2, null, p1));
        if (p2 != null) lignes.add(new CommandeProduit(1, null, p2));

        User user = new User();
        user.setId(14);

        Commande c = new Commande();
        c.setUser(user);
        c.setDate_creation_commande(LocalDateTime.now());
        c.setStatut_commande("En attente");
        c.setStripe_session_id(null);
        c.setPaid_at(null);
        c.setMontant_total_cents(500000000);
        c.setCommande_produits(lignes);

        service.ajouter(c);

        // ══════════════════════════════
        // AFFICHAGE
        // ══════════════════════════════
        System.out.println("\n========== LISTE DES COMMANDES ==========");
        for (Commande commande : service.recuperer()) {
            System.out.println(commande);
        }

        // ══════════════════════════════
        // 2️⃣  MODIFIER — entrer l'ID voulu
        // ══════════════════════════════
        System.out.println("\n========== MODIFIER ==========");

        int idAModifier = 31;  // 👈 Change ici l'ID de la commande à modifier

        Commande aModifier = findCommandeById(idAModifier);
        if (aModifier != null) {
            // Nouvelles valeurs
            aModifier.setStatut_commande("well done");
            aModifier.setMontant_total_cents(2000);

            // Nouveaux produits
            List<CommandeProduit> nouvellesLignes = new ArrayList<>();
            Produit p3 = service.recupererProduitParId(6);  // 👈 Change les IDs produits voulus
            if (p3 != null) nouvellesLignes.add(new CommandeProduit(5, null, p3));
            aModifier.setCommande_produits(nouvellesLignes);

            service.modifier(aModifier);
        }

        // AFFICHAGE APRES MODIF
        System.out.println("\n========== APRES MODIFICATION ==========");
        for (Commande commande : service.recuperer()) {
            System.out.println(commande);
        }

        // ══════════════════════════════
        // 3️⃣  SUPPRIMER — entrer l'ID voulu
        // ══════════════════════════════
        System.out.println("\n========== SUPPRIMER ==========");

        int idASupprimer = 29;  // 👈 Change ici l'ID de la commande à supprimer

        Commande aSupprimer = findCommandeById(idASupprimer);
        if (aSupprimer != null) {
            service.supprimer(idASupprimer);
        }

        // AFFICHAGE FINAL
        System.out.println("\n========== APRES SUPPRESSION ==========");
        for (Commande commande : service.recuperer()) {
            System.out.println(commande);
        }
    }

    // ─────────────────────────────────────────
    // Chercher une commande par ID — comme findProduitById dans MainProduit
    // ─────────────────────────────────────────
    public static Commande findCommandeById(int id) {
        for (Commande c : service.recuperer()) {
            if (c.getId_commande() == id) {
                return c;
            }
        }
        System.out.println("❌ Commande avec ID " + id + " non trouvée");
        return null;
    }
}