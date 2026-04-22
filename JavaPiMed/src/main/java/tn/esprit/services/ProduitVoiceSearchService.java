package tn.esprit.services;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ProduitVoiceSearchService {

    private static final double SCORE_MIN_VALIDATION = 0.67;
    private static final double SCORE_GAP_MIN = 0.08;
    private static final double CONFIDENCE_MIN = 0.35;

    private final Object processLock = new Object();
    private volatile Process listeningProcess;

    public String ecouterNomProduit(int timeoutSeconds, Collection<String> nomsProduits) throws Exception {
        int safeTimeout = Math.max(3, timeoutSeconds);
        Process process = null;
        List<String> nomsNettoyes = nettoyerNomsProduits(nomsProduits);
        try {
            String script = buildPowerShellScript(safeTimeout, nomsNettoyes);
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden",
                    "-Command",
                    script
            );
            pb.redirectErrorStream(true);

            process = pb.start();
            synchronized (processLock) {
                listeningProcess = process;
            }

            if (!process.waitFor(safeTimeout + 3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "";
            }

            byte[] output = process.getInputStream().readAllBytes();
            String brut = new String(output, StandardCharsets.UTF_8);
            return trouverMeilleureCorrespondance(brut, nomsNettoyes);
        } finally {
            synchronized (processLock) {
                if (listeningProcess == process) {
                    listeningProcess = null;
                }
            }
        }
    }

    public void arreterEcoute() {
        synchronized (processLock) {
            if (listeningProcess != null && listeningProcess.isAlive()) {
                listeningProcess.destroyForcibly();
            }
        }
    }

    private String buildPowerShellScript(int timeoutSeconds, List<String> nomsProduits) {
        String produitsArray = buildPowerShellArray(nomsProduits);
        return "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; " +
                "Add-Type -AssemblyName System.Speech; " +
                "$timeout = [TimeSpan]::FromSeconds(" + timeoutSeconds + "); " +
                "$engine = $null; $result = $null; " +
                "try { $engine = New-Object System.Speech.Recognition.SpeechRecognitionEngine([System.Globalization.CultureInfo]::GetCultureInfo('fr-FR')) } catch { $engine = New-Object System.Speech.Recognition.SpeechRecognitionEngine } ; " +
                "$engine.InitialSilenceTimeout = [TimeSpan]::FromSeconds(3); " +
                "$engine.BabbleTimeout = [TimeSpan]::FromSeconds(2); " +
                "$engine.EndSilenceTimeout = [TimeSpan]::FromSeconds(1); " +
                "$engine.EndSilenceTimeoutAmbiguous = [TimeSpan]::FromSeconds(1.5); " +
                "$noms = " + produitsArray + "; " +
                "if ($noms.Count -gt 0) { " +
                "  $choices = New-Object System.Speech.Recognition.Choices; " +
                "  foreach ($n in $noms) { if ($n) { [void]$choices.Add($n) } }; " +
                "  $gb = New-Object System.Speech.Recognition.GrammarBuilder; " +
                "  $gb.Append($choices); " +
                "  $gb.Culture = [System.Globalization.CultureInfo]::GetCultureInfo('fr-FR'); " +
                "  $engine.LoadGrammar((New-Object System.Speech.Recognition.Grammar($gb))); " +
                "}; " +
                "$engine.LoadGrammar((New-Object System.Speech.Recognition.DictationGrammar)); " +
                "$engine.SetInputToDefaultAudioDevice(); " +
                "try { $result = $engine.Recognize($timeout) } catch { $result = $null } ; " +
                "if ($result -and $result.Text) { " +
                "  Write-Output ('TEXT=' + $result.Text); " +
                "  Write-Output ('CONF=' + [string]$result.Confidence); " +
                "  foreach ($alt in $result.Alternates) { if ($alt -and $alt.Text) { Write-Output ('ALT=' + $alt.Text + '|' + [string]$alt.Confidence) } } " +
                "}; " +
                "if ($engine) { $engine.Dispose() }";
    }

    private String buildPowerShellArray(List<String> nomsProduits) {
        if (nomsProduits == null || nomsProduits.isEmpty()) {
            return "@()";
        }

        StringBuilder sb = new StringBuilder("@(");
        for (int i = 0; i < nomsProduits.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('\'').append(escapePowerShell(nomsProduits.get(i))).append('\'');
        }
        sb.append(')');
        return sb.toString();
    }

    private String escapePowerShell(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private List<String> nettoyerNomsProduits(Collection<String> nomsProduits) {
        if (nomsProduits == null || nomsProduits.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> uniques = new LinkedHashSet<>();
        for (String nom : nomsProduits) {
            String propre = normaliser(nom);
            if (!propre.isBlank()) {
                uniques.add(propre);
            }
        }
        return new ArrayList<>(uniques);
    }

    private String trouverMeilleureCorrespondance(String sortiePowerShell, List<String> nomsProduits) {
        List<CandidatReconnu> candidats = extraireCandidats(sortiePowerShell);
        if (candidats.isEmpty()) {
            return "";
        }

        if (nomsProduits == null || nomsProduits.isEmpty()) {
            CandidatReconnu meilleur = candidats.get(0);
            return meilleur.confidence >= CONFIDENCE_MIN ? meilleur.texte : "";
        }

        String meilleurNom = "";
        double meilleurScore = 0.0;
        double secondScore = 0.0;
        double confianceAssociee = 0.0;

        for (CandidatReconnu candidat : candidats) {
            String candidatNorm = normaliserPourComparaison(candidat.texte);
            for (String nomProduit : nomsProduits) {
                String nomNorm = normaliserPourComparaison(nomProduit);
                double similarite = similarite(candidatNorm, nomNorm);
                double tokens = tokenOverlap(candidatNorm, nomNorm);
                double prefixBonus = (nomNorm.startsWith(candidatNorm) || candidatNorm.startsWith(nomNorm)) ? 0.05 : 0.0;
                double score = (similarite * 0.60) + (tokens * 0.25) + (candidat.confidence * 0.15) + prefixBonus;
                if (score > meilleurScore) {
                    secondScore = meilleurScore;
                    meilleurScore = score;
                    meilleurNom = nomProduit;
                    confianceAssociee = candidat.confidence;
                } else if (score > secondScore) {
                    secondScore = score;
                }
            }
        }

        if (meilleurNom.isBlank()) {
            return "";
        }

        if (meilleurScore < SCORE_MIN_VALIDATION) {
            return "";
        }

        if ((meilleurScore - secondScore) < SCORE_GAP_MIN) {
            return "";
        }

        if (confianceAssociee < CONFIDENCE_MIN && meilleurScore < 0.80) {
            return "";
        }

        return meilleurNom;
    }

    private double tokenOverlap(String a, String b) {
        if (a.isBlank() || b.isBlank()) {
            return 0.0;
        }

        Set<String> aTokens = new HashSet<>(List.of(a.split(" ")));
        Set<String> bTokens = new HashSet<>(List.of(b.split(" ")));
        aTokens.remove("");
        bTokens.remove("");

        if (aTokens.isEmpty() || bTokens.isEmpty()) {
            return 0.0;
        }

        int communs = 0;
        for (String token : aTokens) {
            if (bTokens.contains(token)) {
                communs++;
            }
        }

        int max = Math.max(aTokens.size(), bTokens.size());
        return max == 0 ? 0.0 : ((double) communs / max);
    }

    private List<CandidatReconnu> extraireCandidats(String sortiePowerShell) {
        String propre = normaliser(sortiePowerShell);
        if (propre.isBlank()) {
            return Collections.emptyList();
        }

        List<CandidatReconnu> candidats = new ArrayList<>();
        String[] lignes = sortiePowerShell.split("\\R");
        String textePrincipal = "";
        double confPrincipale = 0.0;

        for (String brute : lignes) {
            String ligne = brute == null ? "" : brute.trim();
            if (ligne.isEmpty()) {
                continue;
            }

            if (ligne.startsWith("TEXT=")) {
                textePrincipal = normaliser(ligne.substring(5));
            } else if (ligne.startsWith("CONF=")) {
                confPrincipale = parseDoubleSafe(ligne.substring(5));
            } else if (ligne.startsWith("ALT=")) {
                String contenu = ligne.substring(4);
                int sep = contenu.lastIndexOf('|');
                if (sep > 0) {
                    String texteAlt = normaliser(contenu.substring(0, sep));
                    double confAlt = parseDoubleSafe(contenu.substring(sep + 1));
                    if (!texteAlt.isBlank()) {
                        candidats.add(new CandidatReconnu(texteAlt, confAlt));
                    }
                } else {
                    String texteAlt = normaliser(contenu);
                    if (!texteAlt.isBlank()) {
                        candidats.add(new CandidatReconnu(texteAlt, 0.0));
                    }
                }
            }
        }

        if (!textePrincipal.isBlank()) {
            candidats.add(new CandidatReconnu(textePrincipal, confPrincipale));
        }

        if (candidats.isEmpty() && !propre.isBlank()) {
            return List.of(new CandidatReconnu(propre, 0.0));
        }

        candidats.sort(Comparator.comparingDouble((CandidatReconnu c) -> c.confidence).reversed());
        return dedupliquerCandidats(candidats);
    }

    private List<CandidatReconnu> dedupliquerCandidats(List<CandidatReconnu> candidats) {
        LinkedHashSet<String> vus = new LinkedHashSet<>();
        List<CandidatReconnu> uniques = new ArrayList<>();
        for (CandidatReconnu candidat : candidats) {
            String cle = normaliserPourComparaison(candidat.texte);
            if (!cle.isBlank() && vus.add(cle)) {
                uniques.add(candidat);
            }
        }
        return uniques;
    }

    private double parseDoubleSafe(String value) {
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private String normaliserPourComparaison(String texte) {
        if (texte == null) {
            return "";
        }

        String sansAccent = Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return sansAccent
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double similarite(String a, String b) {
        if (a.isBlank() || b.isBlank()) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 1.0;
        }
        if (a.contains(b) || b.contains(a)) {
            return 0.92;
        }

        int distance = levenshtein(a, b);
        int max = Math.max(a.length(), b.length());
        if (max == 0) {
            return 1.0;
        }
        return 1.0 - ((double) distance / max);
    }

    private int levenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int prevDiag = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int old = costs[j];
                int candidate = (a.charAt(i - 1) == b.charAt(j - 1))
                        ? prevDiag
                        : 1 + Math.min(Math.min(costs[j - 1], costs[j]), prevDiag);
                costs[j] = candidate;
                prevDiag = old;
            }
        }
        return costs[b.length()];
    }

    private String normaliser(String texte) {
        if (texte == null) {
            return "";
        }

        return texte
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static final class CandidatReconnu {
        private final String texte;
        private final double confidence;

        private CandidatReconnu(String texte, double confidence) {
            this.texte = texte;
            this.confidence = confidence;
        }
    }
}

