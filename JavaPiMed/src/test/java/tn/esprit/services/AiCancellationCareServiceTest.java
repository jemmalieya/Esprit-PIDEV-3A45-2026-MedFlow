package tn.esprit.services;

import org.junit.jupiter.api.Test;
import tn.esprit.entities.Evenement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCancellationCareServiceTest {

    private final AiCancellationCareService service = new AiCancellationCareService();

    @Test
    void replyToMessageExplainsMissingCancellationReasonForEnglishWhyQuestion() {
        Evenement event = new Evenement();
        event.setId(75);
        event.setTitre_event("azizcha");
        event.setStatut_event("Annule");

        String reply = service.replyToMessage(
                "why was the event azizcha cancelled?",
                event,
                null,
                List.of()
        );

        assertTrue(reply.contains("motif administratif detaille"));
        assertTrue(reply.contains("azizcha"));
        assertTrue(reply.contains("CARE-75-"));
        assertFalse(reply.contains("Alternatives recommandees"));
    }

    @Test
    void replyToMessageUsesEmotionCareForDisappointedMessage() {
        Evenement event = new Evenement();
        event.setId(75);
        event.setTitre_event("azizcha");
        event.setStatut_event("Annule");

        String reply = service.replyToMessage(
                "Je suis decu / disappointed",
                event,
                null,
                List.of()
        );

        assertTrue(reply.contains("c'est decevant"));
        assertTrue(reply.contains("CARE-75-"));
    }

    @Test
    void replyToMessageUsesStoredCancellationReasonWhenAvailable() {
        Evenement event = new Evenement();
        event.setId(75);
        event.setTitre_event("azizcha");
        event.setStatut_event("Annule");

        String reply = service.replyToMessage(
                "why was it cancelled?",
                event,
                null,
                List.of(),
                "Le medecin intervenant est indisponible et la salle doit etre replanifiee."
        );

        assertTrue(reply.contains("Le medecin intervenant est indisponible"));
        assertFalse(reply.contains("Je n'ai pas de motif administratif detaille"));
    }
}
