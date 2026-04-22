package tn.esprit.services;

import java.util.concurrent.TimeUnit;

public class ProduitSpeechService {

    private static final Object SPEAK_LOCK = new Object();

    public void parlerAsync(String texte) {
        if (texte == null || texte.trim().isEmpty()) {
            return;
        }

        Thread thread = new Thread(() -> parler(texte), "produit-tts-thread");
        thread.setDaemon(true);
        thread.start();
    }

    public void parler(String texte) {
        if (texte == null) return;

        String message = normaliser(texte);
        if (message.isBlank()) return;

        synchronized (SPEAK_LOCK) {
            try {
                String script = buildPowerShellScript(message);
                ProcessBuilder pb = new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-ExecutionPolicy", "Bypass",
                        "-WindowStyle", "Hidden",
                        "-Command",
                        script
                );
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

                Process process = pb.start();
                if (!process.waitFor(30, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                System.err.println("[ProduitSpeechService] Échec lecture vocale: " + e.getMessage());
            }
        }
    }

    private String buildPowerShellScript(String message) {
        String escaped = message.replace("'", "''");
        return "Add-Type -AssemblyName System.Speech; " +
                "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "try { $s.SelectVoiceByHints([System.Speech.Synthesis.VoiceGender]::Female, [System.Speech.Synthesis.VoiceAge]::Adult, 0, [System.Globalization.CultureInfo]::GetCultureInfo('fr-FR')) } catch {} ; " +
                "$s.Volume = 100; $s.Rate = 0; $s.Speak('" + escaped + "');";
    }

    private String normaliser(String texte) {
        return texte
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

}

