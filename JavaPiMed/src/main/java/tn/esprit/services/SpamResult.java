package tn.esprit.services;

public class SpamResult {

    private final boolean spam;
    private final String message;
    private final int score;

    public SpamResult(boolean spam, String message, int score) {
        this.spam = spam;
        this.message = message;
        this.score = score;
    }

    public boolean isSpam() {
        return spam;
    }

    public String getMessage() {
        return message;
    }

    public int getScore() {
        return score;
    }

    public static SpamResult ok() {
        return new SpamResult(false, "Commentaire accepté.", 0);
    }

    public static SpamResult blocked(String message, int score) {
        return new SpamResult(true, message, score);
    }
}