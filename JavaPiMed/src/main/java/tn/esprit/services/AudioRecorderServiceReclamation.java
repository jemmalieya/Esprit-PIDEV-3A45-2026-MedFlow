package tn.esprit.services;

import javax.sound.sampled.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class AudioRecorderServiceReclamation {

    public File enregistrerWavPendant(int secondes) throws Exception {

        AudioFormat format = new AudioFormat(
                16000.0f,
                16,
                1,
                true,
                false
        );

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new IllegalStateException("Microphone non détecté ou non supporté.");
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        Path tempPath = Files.createTempFile("medflow-reclamation-voice-", ".wav");
        File wavFile = tempPath.toFile();

        AudioInputStream audioStream = new AudioInputStream(line);

        Thread stopThread = new Thread(() -> {
            try {
                Thread.sleep(secondes * 1000L);
            } catch (InterruptedException ignored) {
            } finally {
                line.stop();
                line.close();
            }
        });

        stopThread.start();

        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavFile);

        return wavFile;
    }
}