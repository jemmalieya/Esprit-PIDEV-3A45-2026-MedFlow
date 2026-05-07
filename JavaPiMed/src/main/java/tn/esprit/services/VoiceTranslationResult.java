package tn.esprit.services;

public class VoiceTranslationResult {

    private final String texteOriginal;
    private final String texteFrancais;
    private final String langueSource;

    public VoiceTranslationResult(String texteOriginal, String texteFrancais, String langueSource) {
        this.texteOriginal = texteOriginal;
        this.texteFrancais = texteFrancais;
        this.langueSource = langueSource;
    }

    public String getTexteOriginal() {
        return texteOriginal;
    }

    public String getTexteFrancais() {
        return texteFrancais;
    }

    public String getLangueSource() {
        return langueSource;
    }
}