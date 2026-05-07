package tn.esprit.services;

import tn.esprit.entities.Evenement;
import tn.esprit.entities.User;
import tn.esprit.services.ParticipationDemandeService.ParticipationDemande;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AiCancellationCareService {

    public boolean isCancelled(Evenement event) {
        String status = normalize(event == null ? "" : event.getStatut_event());
        return status.contains("annul") || status.contains("cancel");
    }

    public List<Evenement> recommendAlternatives(Evenement cancelledEvent, List<Evenement> allEvents, int limit) {
        List<ScoredEvent> scored = new ArrayList<>();
        if (cancelledEvent == null || allEvents == null) {
            return List.of();
        }

        for (Evenement candidate : allEvents) {
            if (candidate == null || candidate.getId() == cancelledEvent.getId() || isCancelled(candidate)) {
                continue;
            }

            String visibility = normalize(candidate.getVisibilite_event());
            String status = normalize(candidate.getStatut_event());
            if (!visibility.contains("public") || (!status.contains("publi") && !status.contains("publie"))) {
                continue;
            }

            int score = 0;
            if (same(cancelledEvent.getType_event(), candidate.getType_event())) score += 45;
            if (same(cancelledEvent.getVille_event(), candidate.getVille_event())) score += 35;
            if (isFutureOrToday(candidate.getDate_debut_event())) score += 15;
            score += Math.max(0, 5 - Math.abs(daysBetween(cancelledEvent.getDate_debut_event(), candidate.getDate_debut_event())));

            scored.add(new ScoredEvent(candidate, score));
        }

        return scored.stream()
                .sorted(Comparator.comparingInt(ScoredEvent::score).reversed())
                .limit(Math.max(1, limit))
                .map(ScoredEvent::event)
                .toList();
    }

    public String buildOpeningMessage(Evenement event, ParticipationDemande demande, User user, List<Evenement> alternatives) {
        return buildOpeningMessage(event, demande, user, alternatives, "");
    }

    public String buildOpeningMessage(Evenement event, ParticipationDemande demande, User user, List<Evenement> alternatives, String cancellationReason) {
        String firstName = safe(user == null ? "" : user.getPrenom());
        if (firstName.isBlank()) {
            firstName = safe(demande == null ? "" : demande.getPrenom());
        }
        if (firstName.isBlank()) {
            firstName = "Participant";
        }

        return "Bonjour " + firstName + ". Je suis MedFlow Care Assistant.\n\n"
                + "L'evenement \"" + safe(event.getTitre_event()) + "\" a ete annule. "
                + "Je ne vais pas seulement afficher une alerte: je peux vous aider a comprendre la situation, "
                + "vous proposer une compensation et trouver une alternative proche.\n\n"
                + buildCancellationReasonText(cancellationReason)
                + buildCompensationText(event, user)
                + "\n\n" + buildAlternativesText(alternatives);
    }

    public String replyToEmotion(String emotion, Evenement event, User user, List<Evenement> alternatives) {
        String normalized = normalize(emotion);

        if (normalized.contains("colere") || normalized.contains("angry") || normalized.contains("enerve")) {
            return "Je comprends votre frustration. Une annulation peut casser votre planning.\n\n"
                    + "Je vous propose une compensation prioritaire: " + buildCompensationCode(event, user)
                    + ". Ce code vous donne une priorite sur un prochain evenement similaire.\n\n"
                    + buildAlternativesText(alternatives);
        }

        if (normalized.contains("decu") || normalized.contains("triste") || normalized.contains("disappointed")) {
            return "Je comprends, c'est decevant quand vous aviez prevu de participer.\n\n"
                    + "Pour compenser, MedFlow vous reserve un acces prioritaire: "
                    + buildCompensationCode(event, user)
                    + ". Je peux aussi vous orienter vers un autre evenement proche.\n\n"
                    + buildAlternativesText(alternatives);
        }

        if (normalized.contains("calme") || normalized.contains("neutral") || normalized.contains("ok")) {
            return "Merci pour votre comprehension. Je vais aller directement a l'essentiel.\n\n"
                    + buildCompensationText(event, user)
                    + "\n\n" + buildAlternativesText(alternatives);
        }

        return "Merci pour votre retour. Je vais adapter l'aide selon ce que vous ressentez.\n\n"
                + buildCompensationText(event, user)
                + "\n\n" + buildAlternativesText(alternatives);
    }

    public String replyToMessage(String message, Evenement event, User user, List<Evenement> alternatives) {
        return replyToMessage(message, event, user, alternatives, "");
    }

    public String replyToMessage(String message, Evenement event, User user, List<Evenement> alternatives, String cancellationReason) {
        String normalized = normalize(message);

        if (isEmotionMessage(normalized)) {
            return replyToEmotion(message, event, user, alternatives);
        }

        if (isCancellationReasonQuestion(normalized)) {
            String reason = safe(cancellationReason);
            if (!reason.isBlank()) {
                return "L'evenement \"" + safe(event == null ? "" : event.getTitre_event())
                        + "\" a ete annule pour cette raison: " + reason + "\n\n"
                        + buildCompensationText(event, user);
            }

            return "Je n'ai pas de motif administratif detaille dans le dossier local, mais l'evenement \""
                    + safe(event == null ? "" : event.getTitre_event())
                    + "\" est marque comme annule. Mon role ici est de vous donner une suite utile: "
                    + "compensation, alternatives et accompagnement.\n\n"
                    + buildCompensationText(event, user);
        }

        if (normalized.contains("compensation") || normalized.contains("rembourse") || normalized.contains("dedommagement")
                || normalized.contains("voucher") || normalized.contains("code")) {
            return buildCompensationText(event, user)
                    + "\n\nVous pouvez presenter ce code au staff MedFlow lors d'un prochain evenement.";
        }

        if (normalized.contains("alternative") || normalized.contains("autre") || normalized.contains("prochain")
                || normalized.contains("similaire")) {
            return buildAlternativesText(alternatives);
        }

        if (isGeneralHelpQuestion(normalized)) {
            return buildHelpScopeText(event, user, alternatives);
        }

        if (isResourceQuestion(normalized)) {
            return "Oui. Je peux aussi vous aider pour les ressources liees aux evenements: documents, liens utiles, fiches PDF, "
                    + "transcriptions accessibilite, supports medicals et ressources publiques. Pour cette annulation, vous pouvez demander "
                    + "un document de remplacement, une ressource explicative, ou une alternative avec ressources similaires.\n\n"
                    + buildAlternativesText(alternatives);
        }

        if (isEventManagementQuestion(normalized)) {
            return "Je peux vous aider sur la gestion evenement: statut, publication, annulation, participations, tickets, compensation, "
                    + "alternatives, meteo, lieu, capacite, demandes en attente et ressources associees. Pour cet evenement annule, "
                    + "la meilleure suite est de gerer le participant avec un Care Credit et de proposer un evenement proche.\n\n"
                    + buildCompensationText(event, user);
        }

        return buildSmartFallback(message, event, user, alternatives);
    }

    public String buildCloudSystemPrompt() {
        return """
                You are MedFlow Care Assistant, an intelligent assistant for the MedFlow event and resource modules.
                Reply in the same language as the participant: French, English, Arabic, or Tunisian Arabic.
                Output only the final participant-facing answer. Do not reveal chain-of-thought, hidden reasoning,
                analysis, planning, or phrases like "let me check" / "the user is asking".
                You can answer questions about cancelled events, event management, participation requests, tickets,
                compensation, recommendations, accessibility resources, PDFs, public/private resources, location, weather,
                and user-facing next steps.
                Be practical, warm, and concise. Never mention SMS or email. Never invent exact administrative facts.
                If the user asks why the event was cancelled and no reason is provided, say that the local file has no detailed reason.
                When relevant, offer: emotional support, the Care Credit compensation code, and alternative events.
                Keep the answer under 140 words unless the user asks for details.
                """;
    }

    public String buildCloudUserPrompt(String userMessage, Evenement event, User user, List<Evenement> alternatives) {
        return buildCloudUserPrompt(userMessage, event, user, alternatives, "");
    }

    public String buildCloudUserPrompt(String userMessage, Evenement event, User user, List<Evenement> alternatives, String cancellationReason) {
        return """
                Participant message:
                %s

                Cancelled event:
                - title: %s
                - type: %s
                - city: %s
                - date: %s -> %s
                - cancellation reason: %s
                - compensation code: %s

                Alternative events:
                %s

                You may also answer general questions related to MedFlow event management and resources.
                """.formatted(
                safe(userMessage),
                safe(event == null ? "" : event.getTitre_event()),
                safe(event == null ? "" : event.getType_event()),
                safe(event == null ? "" : event.getVille_event()),
                dateText(event == null ? null : event.getDate_debut_event()),
                dateText(event == null ? null : event.getDate_fin_event()),
                safe(cancellationReason),
                buildCompensationCode(event, user),
                alternativesForPrompt(alternatives)
        );
    }

    public String buildCompensationCode(Evenement event, User user) {
        int eventId = event == null ? 0 : event.getId();
        int userId = user == null ? 0 : user.getId();
        int seed = Math.abs((eventId + "-" + userId).hashCode());
        String suffix = ("00000" + Integer.toHexString(seed).toUpperCase(Locale.ROOT));
        return "CARE-" + eventId + "-" + suffix.substring(suffix.length() - 5);
    }

    private String buildCompensationText(Evenement event, User user) {
        return "Compensation proposee: MedFlow Care Credit " + buildCompensationCode(event, user)
                + ". Avantage: priorite d'inscription pour le prochain evenement du meme type ou dans la meme ville.";
    }

    private String buildCancellationReasonText(String cancellationReason) {
        String reason = safe(cancellationReason);
        if (reason.isBlank()) {
            return "";
        }
        return "Motif d'annulation: " + reason + "\n\n";
    }

    private String buildAlternativesText(List<Evenement> alternatives) {
        if (alternatives == null || alternatives.isEmpty()) {
            return "Je n'ai pas trouve d'alternative publique proche pour le moment. Gardez votre Care Credit: il reste utile pour le prochain evenement.";
        }

        StringBuilder sb = new StringBuilder("Alternatives recommandees:\n");
        int index = 1;
        for (Evenement alternative : alternatives) {
            sb.append(index++)
                    .append(". ")
                    .append(safe(alternative.getTitre_event()))
                    .append(" | ")
                    .append(safe(alternative.getVille_event()))
                    .append(" | ")
                    .append(dateText(alternative.getDate_debut_event()))
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private String alternativesForPrompt(List<Evenement> alternatives) {
        if (alternatives == null || alternatives.isEmpty()) {
            return "- none found";
        }

        StringBuilder sb = new StringBuilder();
        for (Evenement alternative : alternatives) {
            sb.append("- ")
                    .append(safe(alternative.getTitre_event()))
                    .append(" | type: ")
                    .append(safe(alternative.getType_event()))
                    .append(" | city: ")
                    .append(safe(alternative.getVille_event()))
                    .append(" | date: ")
                    .append(dateText(alternative.getDate_debut_event()))
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private boolean isGeneralHelpQuestion(String normalized) {
        return normalized.contains("can i ask")
                || normalized.contains("can you help")
                || normalized.contains("what can you do")
                || normalized.contains("help me")
                || normalized.contains("anything")
                || normalized.contains("aide")
                || normalized.contains("tu peux")
                || normalized.contains("شنوة تنجم")
                || normalized.contains("تعاونني");
    }

    private boolean isCancellationReasonQuestion(String normalized) {
        return normalized.contains("why")
                || normalized.contains("pourquoi")
                || normalized.contains("raison")
                || normalized.contains("motif")
                || normalized.contains("cause")
                || normalized.contains("annule")
                || normalized.contains("annul")
                || normalized.contains("cancel");
    }

    private boolean isEmotionMessage(String normalized) {
        return normalized.contains("decu")
                || normalized.contains("triste")
                || normalized.contains("disappointed")
                || normalized.contains("colere")
                || normalized.contains("angry")
                || normalized.contains("enerve")
                || normalized.contains("calme")
                || normalized.contains("neutral");
    }

    private boolean isResourceQuestion(String normalized) {
        return normalized.contains("resource")
                || normalized.contains("ressource")
                || normalized.contains("document")
                || normalized.contains("pdf")
                || normalized.contains("support")
                || normalized.contains("transcription")
                || normalized.contains("accessibilite")
                || normalized.contains("accessibility");
    }

    private boolean isEventManagementQuestion(String normalized) {
        return normalized.contains("gestion")
                || normalized.contains("manage")
                || normalized.contains("management")
                || normalized.contains("status")
                || normalized.contains("statut")
                || normalized.contains("participation")
                || normalized.contains("ticket")
                || normalized.contains("capacity")
                || normalized.contains("capacite")
                || normalized.contains("demande");
    }

    private String buildHelpScopeText(Evenement event, User user, List<Evenement> alternatives) {
        return "Oui, vous pouvez me poser des questions sur ce qui concerne MedFlow Evenements et Ressources.\n\n"
                + "Je peux aider avec: annulation, compensation, alternatives, participations, tickets, statut d'evenement, "
                + "capacite, demandes, ressources publiques, PDF, documents, accessibilite et prochaine action pour le participant.\n\n"
                + "Pour cet evenement annule, votre code est " + buildCompensationCode(event, user) + ".\n\n"
                + buildAlternativesText(alternatives);
    }

    private String buildSmartFallback(String message, Evenement event, User user, List<Evenement> alternatives) {
        String trimmed = safe(message);
        return "Je peux vous aider, mais je vais rester dans le contexte MedFlow: evenements, annulations, participations, "
                + "compensation et ressources.\n\n"
                + "Votre question: \"" + trimmed + "\"\n\n"
                + "Si vous voulez, demandez par exemple: \"donne-moi une alternative\", \"explique la compensation\", "
                + "\"quelles ressources sont disponibles ?\" ou \"comment gerer cette annulation ?\".\n\n"
                + buildCompensationText(event, user);
    }

    private boolean same(String a, String b) {
        String left = normalize(a);
        String right = normalize(b);
        return !left.isBlank() && left.equals(right);
    }

    private boolean isFutureOrToday(Date date) {
        if (date == null) return false;
        LocalDate localDate = new java.util.Date(date.getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return !localDate.isBefore(LocalDate.now());
    }

    private int daysBetween(Date a, Date b) {
        if (a == null || b == null) return 5;
        long millis = Math.abs(a.getTime() - b.getTime());
        return (int) (millis / 86_400_000L);
    }

    private String dateText(Date date) {
        return date == null ? "-" : date.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        String clean = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(clean, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private record ScoredEvent(Evenement event, int score) {
    }
}
