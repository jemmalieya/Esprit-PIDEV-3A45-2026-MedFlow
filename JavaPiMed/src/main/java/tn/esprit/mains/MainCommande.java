package tn.esprit.mains;

import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.entities.User;
import tn.esprit.services.CommandeService;
import tn.esprit.services.ProduitService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MainCommande {

    static CommandeService commandeService = new CommandeService();
    static ProduitService produitService = new ProduitService();

    public static void main(String[] args) {


        System.out.println("\n========== AJOUTER ==========");

        Produit p1 = produitService.recupererParId(6);
        Produit p2 = produitService.recupererParId(8);
        Produit p3 = produitService.recupererParId(11);

        List<CommandeProduit> lignes = new ArrayList<>();

        if (p1 != null) lignes.add(new CommandeProduit(2, null, p1));
        if (p2 != null) lignes.add(new CommandeProduit(1, null, p2));
        if (p3 != null) lignes.add(new CommandeProduit(3, null, p3));

        User user = new User();
        user.setId(1);

        Commande c = new Commande();
        c.setUser(user);
        c.setDate_creation_commande(LocalDateTime.now());
        c.setStatut_commande("En attente");
        c.setStripe_session_id(null);
        c.setPaid_at(null);
        c.setCommande_produits(lignes);

        commandeService.ajouter(c);


        System.out.println("\n========== LISTE DES COMMANDES ==========");
        for (Commande commande : commandeService.recuperer()) {
            System.out.println(commande);
        }

        System.out.println("\n========== COMMANDE PAR ID ==========");

        int idCommandeAAfficher = 42;
        Commande commandeUnique = commandeService.recupererParId(idCommandeAAfficher);

        if (commandeUnique != null) {
            System.out.println(commandeUnique);
        } else {
            System.out.println("Commande avec ID " + idCommandeAAfficher + " non trouvée");
        }

        System.out.println("\n========== MODIFIER ==========");

        int idAModifier = 2;
        Commande aModifier = commandeService.recupererParId(idAModifier);

        if (aModifier != null) {
            aModifier.setStatut_commande("Finalisée test");

            List<CommandeProduit> nouvellesLignes = new ArrayList<>();

            Produit produitModif1 = produitService.recupererParId(6);
            Produit produitModif2 = produitService.recupererParId(8);

            if (produitModif1 != null) {
                CommandeProduit cp1 = new CommandeProduit();
                cp1.setQuantite_commandee(5);
                cp1.setProduit(produitModif1);
                nouvellesLignes.add(cp1);
            }

            if (produitModif2 != null) {
                CommandeProduit cp2 = new CommandeProduit();
                cp2.setQuantite_commandee(2);
                cp2.setProduit(produitModif2);
                nouvellesLignes.add(cp2);
            }

            aModifier.setCommande_produits(nouvellesLignes);
            commandeService.modifier(aModifier);

        } else {
            System.out.println("Commande avec ID " + idAModifier + " non trouvée");
        }

        /*
        // =========================
        // MODIFIER (sans recupererParId)
        // =========================
        // on construit la commande manuellement sans la charger depuis la base

        User userModif = new User();
        userModif.setId(1);

        Commande aModifier = new Commande();
        aModifier.setId_commande(2);
        // on fixe l'ID de la commande à modifier directement
        aModifier.setUser(userModif);
        aModifier.setDate_creation_commande(LocalDateTime.now());
        aModifier.setStatut_commande("Finalisée");
        aModifier.setStripe_session_id(null);
        aModifier.setPaid_at(null);

        List<CommandeProduit> nouvellesLignes = new ArrayList<>();

        Produit produitModif1 = produitService.recupererParId(6);
        Produit produitModif2 = produitService.recupererParId(8);

        if (produitModif1 != null) {
            CommandeProduit cp1 = new CommandeProduit();
            cp1.setQuantite_commandee(5);
            cp1.setProduit(produitModif1);
            nouvellesLignes.add(cp1);
        }

        if (produitModif2 != null) {
            CommandeProduit cp2 = new CommandeProduit();
            cp2.setQuantite_commandee(2);
            cp2.setProduit(produitModif2);
            nouvellesLignes.add(cp2);
        }

        aModifier.setCommande_produits(nouvellesLignes);
        commandeService.modifier(aModifier);
        */


        System.out.println("\n========== APRES MODIFICATION ==========");
        for (Commande commande : commandeService.recuperer()) {
            System.out.println(commande);
        }


        System.out.println("\n========== SUPPRIMER ==========");

        int idASupprimer = 40;
        Commande aSupprimer = commandeService.recupererParId(idASupprimer);

        if (aSupprimer != null) {
            commandeService.supprimer(aSupprimer);
        } else {
            System.out.println("Commande avec ID " + idASupprimer + " non trouvée");
        }

        /*
        // =========================
        // SUPPRIMER (sans recupererParId)
        // =========================
        // on construit un objet Commande avec juste l'ID, sans charger depuis la base

        Commande aSupprimer = new Commande();
        aSupprimer.setId_commande(38);
        // seul l'ID est nécessaire pour la suppression
        commandeService.supprimer(aSupprimer);
        */

        System.out.println("\n========== APRES SUPPRESSION ==========");
        for (Commande commande : commandeService.recuperer()) {
            System.out.println(commande);
        }
    }
}