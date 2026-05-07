package tn.esprit.services;

import tn.esprit.entities.Evenement;
import tn.esprit.entities.User;
import tn.esprit.services.ParticipationDemandeService.ParticipationDemande;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AiEventIntelligenceService {

    public RiskReport analyzeRisk(Evenement event, List<ParticipationDemande> demandes, List<Evenement> allEvents) {
        int score = 5;
        List<String> reasons = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (event == null) {
            return new RiskReport(0, "Indisponible", List.of("Evenement introuvable."), List.of());
        }

        String status = normalize(event.getStatut_event());
        String visibility = normalize(event.getVisibilite_event());
        LocalDate start = toLocalDate(event.getDate_debut_event());
        LocalDate end = toLocalDate(event.getDate_fin_event());
        LocalDate limit = toLocalDate(event.getDate_limite_inscription_event());
        LocalDate today = LocalDate.now();

        if (status.contains("annul") || status.contains("cancel")) {
            score += 60;
            reasons.add("Statut: evenement annule, impact critique sur l'experience participant.");
            suggestions.add("Afficher clairement le motif, proposer une compensation et des alternatives proches.");
        } else if (status.contains("brouillon") || status.contains("draft")) {
            score += 16;
            reasons.add("Statut: evenement encore en brouillon, niveau de preparation incomplet.");
            suggestions.add("Finaliser le contenu, les dates, le lieu et publier uniquement apres verification.");
        } else if (!status.contains("publi")) {
            score += 10;
            reasons.add("Statut: publication non claire ou non stabilisee.");
            suggestions.add("Clarifier le statut avant de le recommander aux utilisateurs.");
        }

        if (!visibility.contains("public")) {
            score += 8;
            reasons.add("Visibilite: evenement non public, exposition utilisateur reduite.");
            suggestions.add("Basculer en public si l'evenement doit recevoir des demandes.");
        }

        if (start == null || end == null) {
            score += 24;
            reasons.add("Planification: dates de debut ou de fin incompletes.");
            suggestions.add("Renseigner une date de debut et une date de fin fiables.");
        } else {
            if (end.isBefore(start)) {
                score += 30;
                reasons.add("Planification: date de fin anterieure a la date de debut.");
                suggestions.add("Corriger la plage de dates avant publication.");
            }
            if (end.isBefore(today)) {
                score += 18;
                reasons.add("Temporalite: evenement deja passe.");
                suggestions.add("Archiver l'evenement ou le dupliquer avec de nouvelles dates.");
            } else {
                long daysLeft = ChronoUnit.DAYS.between(today, start);
                if (daysLeft <= 2) {
                    score += 16;
                    reasons.add("Temporalite: evenement a moins de 48h, marge operationnelle tres faible.");
                    suggestions.add("Prioriser la validation des participants et la communication logistique.");
                } else if (daysLeft <= 7) {
                    score += 8;
                    reasons.add("Temporalite: evenement proche, execution logistique a securiser.");
                }
            }
        }

        if (limit != null && limit.isBefore(today) && start != null && !start.isBefore(today)) {
            score += 10;
            reasons.add("Inscriptions: date limite depassee alors que l'evenement est encore a venir.");
            suggestions.add("Fermer clairement les inscriptions ou prolonger la date limite.");
        }

        int pending = countByStatus(demandes, ParticipationDemande.STATUS_PENDING);
        int accepted = countByStatus(demandes, ParticipationDemande.STATUS_ACCEPTED);
        int refused = countByStatus(demandes, ParticipationDemande.STATUS_REFUSED);
        int capacity = Math.max(0, event.getNb_participants_max_event());
        double occupancy = capacity > 0 ? accepted / (double) capacity : 0.0;

        if (pending > 0) {
            score += Math.min(18, pending * 3);
            reasons.add("Traitement: " + pending + " demande(s) encore en attente de decision.");
            suggestions.add("Traiter les demandes en attente pour eviter les retards de confirmation.");
        }
        if (capacity > 0 && accepted > capacity) {
            score += 26;
            reasons.add("Capacite: nombre d'acceptations superieur a la capacite declaree.");
            suggestions.add("Verifier la capacite du lieu ou limiter les nouvelles validations.");
        } else if (capacity > 0 && occupancy >= 0.90) {
            score += 10;
            reasons.add("Capacite: taux de remplissage superieur a 90%.");
            suggestions.add("Preparer une liste d'attente ou augmenter la capacite si possible.");
        } else if (capacity > 0 && occupancy < 0.20 && start != null && !start.isBefore(today) && ChronoUnit.DAYS.between(today, start) <= 10) {
            score += 9;
            reasons.add("Capacite: remplissage faible a l'approche de l'evenement.");
            suggestions.add("Renforcer la communication ou reevaluer le format et la capacite prevue.");
        }
        if (refused > accepted && refused >= 3) {
            score += 8;
            reasons.add("Selection: volume de refus eleve par rapport aux acceptations.");
            suggestions.add("Analyser les motifs de refus pour ameliorer les criteres d'inscription.");
        }

        long sameDay = countSameDayEvents(event, allEvents);
        if (sameDay > 0) {
            score += (int) Math.min(14, sameDay * 4);
            reasons.add("Planning: " + sameDay + " autre(s) evenement(s) programmes le meme jour.");
            suggestions.add("Verifier les conflits de planning et la disponibilite du staff.");
        }

        if (safe(event.getVille_event()).isBlank() || safe(event.getAdresse_event()).isBlank()) {
            score += 10;
            reasons.add("Lieu: informations de ville ou d'adresse incompletes.");
            suggestions.add("Completer la ville et l'adresse pour ameliorer carte, meteo et recommandations.");
        }

        if (reasons.isEmpty()) {
            reasons.add("Evaluation: aucun signal critique detecte sur les donnees disponibles.");
            suggestions.add("Maintenir le suivi des demandes et verifier la logistique avant l'evenement.");
        }

        int bounded = Math.max(0, Math.min(100, score));
        return new RiskReport(bounded, riskLevel(bounded), reasons, distinctLimit(suggestions, 5));
    }

    public List<Recommendation> recommendForEvent(Evenement current, User user, List<Evenement> allEvents, int limit) {
        List<Recommendation> recommendations = new ArrayList<>();
        if (current == null || allEvents == null) {
            return recommendations;
        }

        LocalDate currentDate = toLocalDate(current.getDate_debut_event());
        Set<String> currentKeywords = extractKeywords(current.getDescription_event() + " " + current.getObjectif_event());

        for (Evenement candidate : allEvents) {
            if (candidate == null || candidate.getId() == current.getId() || !isRecommendable(candidate)) {
                continue;
            }

            double score = 0.8;
            List<String> reasons = new ArrayList<>();
            List<ParticipationDemande> candidateDemandes = new ParticipationDemandeService().getDemandes(candidate);
            int accepted = countByStatus(candidateDemandes, ParticipationDemande.STATUS_ACCEPTED);
            int waiting = countByStatus(candidateDemandes, ParticipationDemande.STATUS_WAITING);
            int capacity = Math.max(0, candidate.getNb_participants_max_event());
            LocalDate candidateDate = toLocalDate(candidate.getDate_debut_event());
            Set<String> candidateKeywords = extractKeywords(candidate.getDescription_event() + " " + candidate.getObjectif_event());

            if (same(current.getType_event(), candidate.getType_event())) {
                score += 2.8;
                reasons.add("Type: meme categorie d'evenement, donc besoin et experience proches.");
            }
            if (same(current.getVille_event(), candidate.getVille_event())) {
                score += 2.1;
                reasons.add("Localisation: meme ville, donc acces et deplacement simplifies.");
            }

            if (candidateDate != null && !candidateDate.isBefore(LocalDate.now())) {
                score += 1.0;
                reasons.add("Disponibilite: date future encore ouverte a l'inscription.");
            }
            if (currentDate != null && candidateDate != null) {
                long gap = Math.abs(ChronoUnit.DAYS.between(currentDate, candidateDate));
                if (gap <= 7) {
                    score += 1.4;
                    reasons.add("Calendrier: evenement programme a moins de 7 jours de l'evenement consulte.");
                } else if (gap <= 21) {
                    score += 0.9;
                    reasons.add("Calendrier: proximite de date utile pour une suite logique.");
                } else if (gap <= 45) {
                    score += 0.6;
                }
            }

            int commonKeywords = countIntersection(currentKeywords, candidateKeywords);
            if (commonKeywords >= 3) {
                score += 2.0;
                reasons.add("Contenu: objectifs et themes fortement similaires.");
            } else if (commonKeywords >= 1) {
                score += 0.9;
                reasons.add("Contenu: quelques themes communs avec l'evenement consulte.");
            }

            if (accepted > 0) {
                score += Math.min(1.2, accepted / 6.0);
                reasons.add("Attractivite: " + accepted + " participant(s) deja accepte(s), signe d'interet reel.");
            }

            if (capacity > 0) {
                double occupancy = accepted / (double) capacity;
                if (occupancy >= 0.35 && occupancy <= 0.85) {
                    score += 0.9;
                    reasons.add("Capacite: taux d'occupation equilibre sans saturation.");
                } else if (occupancy > 0.95) {
                    score -= 0.8;
                    reasons.add("Capacite: evenement presque sature, disponibilite limitee.");
                }
            }

            if (waiting > 0) {
                score -= Math.min(0.9, waiting * 0.2);
                reasons.add("Demande: liste d'attente deja active, accessibilite plus faible.");
            }

            if (same(current.getNom_organisateur_event(), candidate.getNom_organisateur_event())) {
                score += 0.6;
                reasons.add("Organisation: meme organisateur, donc continuite editoriale.");
            }

            if (user != null && same(user.getPrenom(), candidate.getNom_organisateur_event())) {
                score += 0.3;
            }

            if (reasons.isEmpty()) {
                reasons.add("Alternative publique disponible.");
            }

            recommendations.add(new Recommendation(candidate, Math.min(10.0, score), distinctLimit(reasons, 5)));
        }

        return recommendations.stream()
                .sorted(Comparator.comparingDouble(Recommendation::score).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    public String buildRecommendationSummary(Evenement current, List<Recommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "Aucune recommandation fiable trouvee pour cet evenement avec les donnees actuelles.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Analyse IA locale pour \"").append(safe(current == null ? "" : current.getTitre_event())).append("\".\n\n");
        int index = 1;
        for (Recommendation rec : recommendations) {
            Evenement event = rec.event();
            builder.append(index++)
                    .append(". ")
                    .append(safe(event.getTitre_event()))
                    .append(" - score ")
                    .append(String.format(Locale.US, "%.1f/10", rec.score()))
                    .append("\n   Criteres:\n   - ")
                    .append(String.join("\n   - ", rec.reasons()))
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private Set<String> extractKeywords(String rawText) {
        Set<String> keywords = new HashSet<>();
        String normalized = normalize(rawText);
        if (normalized.isBlank()) {
            return keywords;
        }

        for (String token : normalized.split("[^a-z0-9]+")) {
            if (token.length() >= 4) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    private int countIntersection(Set<String> first, Set<String> second) {
        if (first == null || second == null || first.isEmpty() || second.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (String value : first) {
            if (second.contains(value)) {
                count++;
            }
        }
        return count;
    }

    private boolean isRecommendable(Evenement event) {
        String status = normalize(event.getStatut_event());
        String visibility = normalize(event.getVisibilite_event());
        LocalDate date = toLocalDate(event.getDate_debut_event());
        return visibility.contains("public")
                && status.contains("publi")
                && (date == null || !date.isBefore(LocalDate.now()));
    }

    private long countSameDayEvents(Evenement event, List<Evenement> allEvents) {
        LocalDate date = toLocalDate(event.getDate_debut_event());
        if (date == null || allEvents == null) {
            return 0;
        }
        return allEvents.stream()
                .filter(other -> other != null && other.getId() != event.getId())
                .filter(other -> date.equals(toLocalDate(other.getDate_debut_event())))
                .count();
    }

    private int countByStatus(List<ParticipationDemande> demandes, String status) {
        if (demandes == null) {
            return 0;
        }
        int count = 0;
        for (ParticipationDemande demande : demandes) {
            if (demande != null && status.equals(demande.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private String riskLevel(int score) {
        if (score >= 75) return "Critique";
        if (score >= 55) return "Sous tension";
        if (score >= 30) return "Sous surveillance";
        return "Stable";
    }

    private List<String> distinctLimit(List<String> values, int limit) {
        List<String> out = new ArrayList<>();
        if (values == null) {
            return out;
        }
        for (String value : values) {
            String clean = safe(value);
            if (!clean.isBlank() && !out.contains(clean)) {
                out.add(clean);
            }
            if (out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    private boolean same(String a, String b) {
        String left = normalize(a);
        String right = normalize(b);
        return !left.isBlank() && left.equals(right);
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String normalize(String value) {
        String clean = safe(value).toLowerCase(Locale.ROOT);
        return Normalizer.normalize(clean, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record RiskReport(int score, String level, List<String> reasons, List<String> suggestions) {
    }

    public record Recommendation(Evenement event, double score, List<String> reasons) {
    }
}
