package tn.esprit.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Windows text-to-speech service using System.Speech through PowerShell.
 */
public class TextToSpeechService {
    private final ExecutorService speakerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai-tts-thread");
        t.setDaemon(true);
        return t;
    });

    public void speakAsync(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        speakerExecutor.submit(() -> {
            try {
                speakWindows(text);
            } catch (Exception ignored) {
            }
        });
    }

    private void speakWindows(String text) throws Exception {
        String cleanedText = text
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("'", "''")
                .trim();

        String command = "Add-Type -AssemblyName System.Speech; "
                + "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
            + "$englishVoice = $speak.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture -and $_.VoiceInfo.Culture.Name -like 'en*' } | Select-Object -First 1; "
            + "if ($null -ne $englishVoice) { $speak.SelectVoice($englishVoice.VoiceInfo.Name) } ; "
                + "$speak.Rate = 0; "
                + "$speak.Volume = 100; "
                + "$speak.Speak('" + cleanedText + "');";

        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-Command",
                command
        );
        pb.inheritIO();

        Process process = pb.start();
        process.waitFor();
    }
}
