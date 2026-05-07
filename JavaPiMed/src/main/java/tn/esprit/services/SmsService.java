package tn.esprit.services;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import tn.esprit.entities.Commande;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.User;

public class SmsService {

    private final String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
    private final String authToken = System.getenv("TWILIO_AUTH_TOKEN");
    private final String twilioPhoneNumber = System.getenv("TWILIO_PHONE_NUMBER");
    private final boolean twilioConfigured;

    public SmsService() {
        twilioConfigured = isNotBlank(accountSid) && isNotBlank(authToken) && isNotBlank(twilioPhoneNumber);

        if (twilioConfigured) {
            Twilio.init(accountSid, authToken);
        }
    }

    public boolean envoyerSmsConfirmationCommande(User user, Commande commande) {
        try {
            if (user == null || commande == null) {
                System.out.println("User ou commande null.");
                return false;
            }

            if (!twilioConfigured) {
                System.out.println("Configuration Twilio manquante (SID/TOKEN/PHONE). SMS non envoye.");
                return false;
            }

            if (!accountSid.startsWith("AC")) {
                System.out.println("TWILIO_ACCOUNT_SID invalide. SMS non envoye.");
                return false;
            }

            String numero = normaliserNumero(user.getTelephoneUser());
            if (numero.isBlank()) {
                System.out.println("Numéro téléphone utilisateur introuvable.");
                return false;
            }

            String prenom = user.getPrenom() != null ? user.getPrenom() : "";
            String nom = user.getNom() != null ? user.getNom() : "";
            String nomComplet = (prenom + " " + nom).trim();
            if (nomComplet.isBlank()) {
                nomComplet = "Client";
            }

            int nbArticles = 0;
            StringBuilder detailsProduits = new StringBuilder();

            if (commande.getCommande_produits() != null) {
                for (CommandeProduit cp : commande.getCommande_produits()) {
                    if (cp != null && cp.getProduit() != null) {
                        nbArticles += cp.getQuantite_commandee();

                        detailsProduits
                                .append("- ")
                                .append(cp.getProduit().getNom_produit())
                                .append(" x")
                                .append(cp.getQuantite_commandee())
                                .append("\n");
                    }
                }
            }

            double total = commande.getMontant_total_cents() / 100.0;

            String messageTexte =
                    "Bonjour " + nomComplet + ",\n" +
                            "Votre commande #" + commande.getId_commande() + " a été payée avec succès.\n" +
                            "Articles: " + nbArticles + "\n" +
                            "Total: " + String.format("%.2f DT", total) + "\n" +
                            "Détails:\n" + detailsProduits +
                            "Patientez pour votre SMS de confirmation final.\n" +
                            "Merci pour votre achat.";

            Message.creator(
                    new com.twilio.type.PhoneNumber(numero),
                    new com.twilio.type.PhoneNumber(twilioPhoneNumber),
                    messageTexte
            ).create();

            System.out.println("SMS envoyé avec succès à " + numero);
            return true;

        } catch (ApiException e) {
            String errorMessage = e.getMessage() == null ? "" : e.getMessage();
            if (e.getStatusCode() == 401 || errorMessage.toLowerCase().contains("authenticate")) {
                System.out.println("Echec authentification Twilio. Verifiez TWILIO_ACCOUNT_SID et TWILIO_AUTH_TOKEN.");
            } else {
                System.out.println("Erreur Twilio API: " + errorMessage);
            }
            return false;

        } catch (Exception e) {
            System.out.println("Erreur envoi SMS: " + e.getMessage());
            return false;
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String normaliserNumero(String numero) {
        if (numero == null) return "";

        numero = numero.trim().replace(" ", "");

        if (numero.isBlank()) return "";

        // déjà au format international
        if (numero.startsWith("+")) {
            return numero;
        }

        // Tunisie : 0XXXXXXXX -> +216XXXXXXXX
        if (numero.startsWith("0") && numero.length() >= 8) {
            return "+216" + numero.substring(1);
        }

        // si juste 8 chiffres tunisien
        if (numero.matches("\\d{8}")) {
            return "+216" + numero;
        }

        // fallback
        return "+216" + numero;
    }
}