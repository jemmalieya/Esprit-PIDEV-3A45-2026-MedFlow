package tn.esprit.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import tn.esprit.config.StripeConfig;
import tn.esprit.entities.CommandeProduit;
import tn.esprit.entities.Produit;
import tn.esprit.entities.User;

import java.util.ArrayList;
import java.util.List;

public class StripeCheckoutService {

    public StripeCheckoutService() {
        Stripe.apiKey = StripeConfig.SECRET_KEY;
    }

    public Session creerSessionCheckout(
            User user,
            List<CommandeProduit> lignes,
            int localCommandeId
    ) throws StripeException {

        List<SessionCreateParams.LineItem> stripeItems = new ArrayList<>();

        for (CommandeProduit cp : lignes) {
            Produit p = cp.getProduit();

            SessionCreateParams.LineItem item =
                    SessionCreateParams.LineItem.builder()
                            .setQuantity((long) cp.getQuantite_commandee())
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("eur") // ou usd, ou autre supportée
                                            .setUnitAmount((long) Math.round(p.getPrix_produit() * 100))
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(p.getNom_produit())
                                                            .build()
                                            )
                                            .build()
                            )
                            .build();

            stripeItems.add(item);
        }

        String successUrl = StripeConfig.BASE_URL
                + "/stripe/success?session_id={CHECKOUT_SESSION_ID}&commande_id=" + localCommandeId;

        String cancelUrl = StripeConfig.BASE_URL
                + "/stripe/cancel?commande_id=" + localCommandeId;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(user.getEmailUser())
                .setClientReferenceId(String.valueOf(localCommandeId))
                .putMetadata("commande_id", String.valueOf(localCommandeId))
                .addAllLineItem(stripeItems)
                .build();

        return Session.create(params);
    }

    public Session recupererSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId, SessionRetrieveParams.builder().build(), null);
    }

    public boolean paiementConfirme(String sessionId) throws StripeException {
        Session session = recupererSession(sessionId);
        return "paid".equalsIgnoreCase(session.getPaymentStatus());
    }
}