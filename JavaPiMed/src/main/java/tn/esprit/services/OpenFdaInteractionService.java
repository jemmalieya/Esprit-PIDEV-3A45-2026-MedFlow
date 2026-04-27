package tn.esprit.services;

import tn.esprit.entities.Produit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenFdaInteractionService {

    private static final String BASE_URL = "https://api.fda.gov/drug/label.json";

    private static final List<String> DANGER_KEYWORDS = Arrays.asList(
            "contraindicated", "serious", "avoid", "not recommended",
            "severe", "major", "fatal", "danger", "life-threatening",
            "increased risk", "bleeding", "hemorrhage", "toxicity",
            "do not use", "may increase", "potentiate", "interaction",
            "caution", "monitor", "inhibit", "enhance", "additive"
    );

    private static final Map<String, List<String>> SYNONYMES = new HashMap<>();
    static {
        SYNONYMES.put("warfarin",    Arrays.asList("warfarin", "coumadin", "anticoagulant", "coumarin", "coumarine"));
        SYNONYMES.put("aspirin",     Arrays.asList("aspirin", "acetylsalicylic", "salicylate", "asa", "salicylic", "salicylates"));
        SYNONYMES.put("tramadol",    Arrays.asList("tramadol", "ultram", "opioid", "opiate", "narcotic", "opioids", "opioid analgesic"));
        SYNONYMES.put("ibuprofen",   Arrays.asList("ibuprofen", "advil", "motrin", "nsaid", "nonsteroidal", "nsaids"));
        SYNONYMES.put("paracetamol", Arrays.asList("paracetamol", "acetaminophen", "tylenol", "apap"));
        SYNONYMES.put("metformin",   Arrays.asList("metformin", "glucophage"));
        SYNONYMES.put("amoxicillin", Arrays.asList("amoxicillin", "amoxil", "penicillin"));
        SYNONYMES.put("omeprazole",  Arrays.asList("omeprazole", "prilosec", "proton pump", "ppi"));
        SYNONYMES.put("lisinopril",  Arrays.asList("lisinopril", "zestril", "ace inhibitor", "ace-inhibitor"));
        SYNONYMES.put("metoprolol",  Arrays.asList("metoprolol", "lopressor", "beta blocker", "beta-blocker"));
        SYNONYMES.put("sertraline",  Arrays.asList("sertraline", "zoloft", "ssri", "antidepressant", "serotonin"));
        SYNONYMES.put("diazepam",    Arrays.asList("diazepam", "valium", "benzodiazepine", "benzodiazepines"));
        SYNONYMES.put("codeine",     Arrays.asList("codeine", "opioid", "opiate", "narcotic"));
        SYNONYMES.put("clopidogrel", Arrays.asList("clopidogrel", "plavix", "antiplatelet", "platelet"));
        SYNONYMES.put("naproxen",    Arrays.asList("naproxen", "aleve", "naprosyn", "nsaid", "nonsteroidal"));
        SYNONYMES.put("fluoxetine",  Arrays.asList("fluoxetine", "prozac", "ssri", "serotonin"));
        SYNONYMES.put("heparin",     Arrays.asList("heparin", "anticoagulant", "blood thinner"));
    }

    private boolean derniereAnalyseIndisponible = false;
    private String dernierMessageIndisponibilite = "";
    private volatile boolean apiTemporairementIndisponible = false;

    public boolean isDerniereAnalyseIndisponible() { return derniereAnalyseIndisponible; }
    public String getDernierMessageIndisponibilite() { return dernierMessageIndisponibilite; }

    // ─────────────────────────────────────────────────────────────────────────
    // POINT D'ENTRÉE PRINCIPAL
    // ─────────────────────────────────────────────────────────────────────────

    public List<DrugInteractionResult> verifierInteractions(List<Produit> produits) {
        List<DrugInteractionResult> resultats = new ArrayList<>();
        derniereAnalyseIndisponible = false;
        dernierMessageIndisponibilite = "";
        apiTemporairementIndisponible = false;

        if (produits == null || produits.size() < 2) return resultats;

        Set<String> nomsUniques = new LinkedHashSet<>();
        for (Produit p : produits) {
            if (p.getNom_produit() != null && !p.getNom_produit().isBlank()) {
                nomsUniques.add(p.getNom_produit().trim());
            }
        }

        List<String> noms = new ArrayList<>(nomsUniques);
        Map<String, String> interactionTextCache = new HashMap<>();
        System.out.println("[OpenFDA] ══════════════════════════════════════");
        System.out.println("[OpenFDA] Vérification pour : " + noms);
        System.out.println("[OpenFDA] ══════════════════════════════════════");

        outer:
        for (int i = 0; i < noms.size(); i++) {
            for (int j = i + 1; j < noms.size(); j++) {
                String a = noms.get(i);
                String b = noms.get(j);
                try {
                    DrugInteractionResult r = verifierPaire(a, b, interactionTextCache);
                    if (r != null) {
                        System.out.println("[OpenFDA] ⚠⚠⚠ INTERACTION TROUVÉE : " + a + " ↔ " + b);
                        resultats.add(r);
                    } else {
                        System.out.println("[OpenFDA] ✓ Pas d'interaction détectée : " + a + " ↔ " + b);
                    }
                } catch (Exception e) {
                    System.err.println("[OpenFDA] ✗ Erreur pour " + a + "/" + b + " : " + e.getMessage());
                    // Continuer au lieu de s'arrêter complètement
                    System.err.println("[OpenFDA] → Continuant les vérifications...");
                }
            }
        }

        System.out.println("[OpenFDA] Résultat final : " + resultats.size() + " interaction(s) détectée(s)");
        System.out.println("[OpenFDA] ══════════════════════════════════════");
        return resultats;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFICATION D'UNE PAIRE
    // ─────────────────────────────────────────────────────────────────────────

    private DrugInteractionResult verifierPaire(String nomA, String nomB, Map<String, String> interactionTextCache) throws Exception {
        System.out.println("[OpenFDA] ── Paire : " + nomA + " ↔ " + nomB);

        String texteA = fetchDrugInteractionText(nomA, interactionTextCache);
        if (texteA != null && !texteA.isBlank()) {
            System.out.println("[OpenFDA] Texte '" + nomA + "' ("
                    + texteA.length() + " chars) : "
                    + texteA.substring(0, Math.min(150, texteA.length())) + "...");
            String detail = chercherMentionDangereuse(nomA, nomB, texteA);
            if (detail != null) return new DrugInteractionResult(nomA, nomB, detail, true);
        } else {
            System.out.println("[OpenFDA] Texte '" + nomA + "' : (vide)");
        }

        String texteB = fetchDrugInteractionText(nomB, interactionTextCache);
        if (texteB != null && !texteB.isBlank()) {
            System.out.println("[OpenFDA] Texte '" + nomB + "' ("
                    + texteB.length() + " chars) : "
                    + texteB.substring(0, Math.min(150, texteB.length())) + "...");
            String detail = chercherMentionDangereuse(nomB, nomA, texteB);
            if (detail != null) return new DrugInteractionResult(nomB, nomA, detail, true);
        } else {
            System.out.println("[OpenFDA] Texte '" + nomB + "' : (vide)");
        }

        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RÉCUPÉRATION DU TEXTE FDA (5 STRATÉGIES)
    // ─────────────────────────────────────────────────────────────────────────

    private String fetchDrugInteractionText(String nomMedicament, Map<String, String> interactionTextCache) throws Exception {
        String nom = nomMedicament.trim().toLowerCase(Locale.ROOT);
        if (interactionTextCache.containsKey(nom)) {
            return interactionTextCache.get(nom);
        }
        if (apiTemporairementIndisponible) {
            return "";
        }

        String r = fetchFromUrl(buildUrl("openfda.generic_name", nom, true));
        if (r != null && !r.isBlank()) {
            interactionTextCache.put(nom, r);
            return r;
        }

        r = fetchFromUrl(buildUrl("openfda.brand_name", nom, true));
        if (r != null && !r.isBlank()) {
            interactionTextCache.put(nom, r);
            return r;
        }

        r = fetchFromUrl(buildUrl("openfda.substance_name", nom, true));
        if (r != null && !r.isBlank()) {
            interactionTextCache.put(nom, r);
            return r;
        }

        String encoded = URLEncoder.encode(nom, StandardCharsets.UTF_8).replace("+", "%20");
        String urlFullText = BASE_URL + "?search=drug_interactions:%22" + encoded + "%22&limit=1";
        r = fetchFromUrl(urlFullText);
        if (r != null && !r.isBlank()) {
            interactionTextCache.put(nom, r);
            return r;
        }

        r = fetchFromUrl(buildUrl("openfda.generic_name", nom, false));
        if (r != null && !r.isBlank()) {
            interactionTextCache.put(nom, r);
            return r;
        }

        interactionTextCache.put(nom, "");
        return "";
    }

    private String buildUrl(String champ, String valeur, boolean filtrerAvecDrugInteractions) {
        try {
            String encoded = URLEncoder.encode(valeur, StandardCharsets.UTF_8).replace("+", "%20");
            String base = BASE_URL + "?search=" + champ + ":%22" + encoded + "%22";
            if (filtrerAvecDrugInteractions) {
                base += "+AND+drug_interactions:*";
            }
            return base + "&limit=1";
        } catch (Exception e) {
            return BASE_URL + "?search=" + champ + ":%22" + valeur + "%22&limit=1";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPEL HTTP
    // ─────────────────────────────────────────────────────────────────────────

    private String fetchFromUrl(String urlStr) throws Exception {
        if (apiTemporairementIndisponible) {
            return "";
        }

        System.out.println("[OpenFDA] GET " + urlStr);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "MedicamentInteractionChecker/1.0");
            conn.setConnectTimeout(15000);  // 15 secondes pour la connexion
            conn.setReadTimeout(20000);     // 20 secondes pour la lecture

            int code = conn.getResponseCode();
            System.out.println("[OpenFDA] HTTP " + code);

            if (code == 404) return "";
            if (code == 429) {
                System.err.println("[OpenFDA] Rate limit, pause 1.5s...");
                Thread.sleep(1500);
                return "";
            }
            if (code != 200) return "";

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
            }

            String json = sb.toString();
            String result = extraireTexteInteractions(json);

            if (result == null || result.isBlank()) {
                System.out.println("[OpenFDA] → Extraction vide");
            } else {
                System.out.println("[OpenFDA] → Extrait " + result.length() + " chars");
            }

            return result == null ? "" : result;
        } catch (SocketTimeoutException e) {
            System.err.println("[OpenFDA] ⏱ TIMEOUT - L'API FDA a pris trop de temps à répondre");
            System.err.println("[OpenFDA] ⏱ Pause avant prochaine tentative...");
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            // Ne pas marquer comme indisponible, juste continuer
            return "";
        } catch (Exception e) {
            System.err.println("[OpenFDA] Erreur réseau : " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return "";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXTRACTION — LECTURE CARACTÈRE PAR CARACTÈRE (100% FIABLE)
    // ─────────────────────────────────────────────────────────────────────────

    private String extraireTexteInteractions(String json) {
        if (json == null || json.isBlank()) return "";

        // Trouver le champ drug_interactions
        int champIdx = json.indexOf("\"drug_interactions\"");
        if (champIdx < 0) {
            System.out.println("[OpenFDA] ✗ Champ drug_interactions absent");
            return "";
        }

        // Avancer après "drug_interactions"
        int i = champIdx + "\"drug_interactions\"".length();

        // Passer les espaces et le ':'
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == '\t'
                || json.charAt(i) == '\n' || json.charAt(i) == '\r' || json.charAt(i) == ':')) {
            i++;
        }

        if (i >= json.length()) return "";

        char next = json.charAt(i);

        if (next == '[') {
            // Tableau : avancer jusqu'au premier '"'
            i++; // passer '['
            // Passer espaces/newlines
            while (i < json.length() && json.charAt(i) != '"') {
                char c = json.charAt(i);
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                    // Caractère inattendu avant le " (tableau vide ou null)
                    if (c == ']') {
                        System.out.println("[OpenFDA] ✗ Tableau drug_interactions vide");
                        return "";
                    }
                }
                i++;
            }
            if (i >= json.length()) return "";
            i++; // passer le '"' ouvrant

        } else if (next == '"') {
            i++; // passer le '"' ouvrant directement
        } else {
            System.out.println("[OpenFDA] ✗ Format inattendu après drug_interactions : '" + next + "'");
            return "";
        }

        // Lire la chaîne jusqu'au '"' fermant non échappé
        StringBuilder texte = new StringBuilder();
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char suivant = json.charAt(i + 1);
                switch (suivant) {
                    case 'n':  texte.append(' '); break;
                    case 'r':  texte.append(' '); break;
                    case 't':  texte.append(' '); break;
                    case '"':  texte.append('"'); break;
                    case '\\': texte.append('\\'); break;
                    default:   texte.append(suivant); break;
                }
                i += 2;
            } else if (c == '"') {
                break; // fin de la chaîne
            } else {
                texte.append(c);
                i++;
            }
        }

        String result = texte.toString().replaceAll("\\s{2,}", " ").trim();
        System.out.println("[OpenFDA] ✓ Extraction char-by-char : " + result.length() + " chars");
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECHERCHE DE MENTIONS DANGEREUSES
    // ─────────────────────────────────────────────────────────────────────────

    private String chercherMentionDangereuse(String medicamentSource, String medicamentCible,
                                             String texteSource) {
        String lowerTexte = texteSource.toLowerCase(Locale.ROOT);
        List<String> variantes = getVariantes(medicamentCible.toLowerCase(Locale.ROOT));

        System.out.println("[OpenFDA] Recherche '" + medicamentCible
                + "' (variantes: " + variantes + ") dans texte '" + medicamentSource + "'");

        for (String variante : variantes) {
            int idx = lowerTexte.indexOf(variante);
            while (idx >= 0) {
                int debut = Math.max(0, idx - 350);
                int fin = Math.min(lowerTexte.length(), idx + variante.length() + 350);
                String contexte = lowerTexte.substring(debut, fin);

                for (String motDanger : DANGER_KEYWORDS) {
                    if (contexte.contains(motDanger)) {
                        String extrait = texteSource.substring(debut, fin).trim();
                        if (extrait.length() > 500) extrait = extrait.substring(0, 500) + "...";
                        System.out.println("[OpenFDA] ✓ Match : variante='"
                                + variante + "', danger='" + motDanger + "'");
                        return "⚠ Interaction " + medicamentSource + " + " + medicamentCible
                                + " :\n" + extrait;
                    }
                }
                idx = lowerTexte.indexOf(variante, idx + 1);
            }
        }

        System.out.println("[OpenFDA] ✗ Aucune mention dangereuse de '"
                + medicamentCible + "' dans '" + medicamentSource + "'");
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> getVariantes(String nomLower) {
        List<String> variantes = new ArrayList<>();
        variantes.add(nomLower);

        for (Map.Entry<String, List<String>> entry : SYNONYMES.entrySet()) {
            if (nomLower.contains(entry.getKey()) || entry.getKey().contains(nomLower)) {
                variantes.addAll(entry.getValue());
            }
        }

        return new ArrayList<>(new LinkedHashSet<>(variantes));
    }
}