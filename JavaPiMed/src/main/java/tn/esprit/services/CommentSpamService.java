package tn.esprit.services;

import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommentSpamService {

    private final Connection cn;

    private static final Pattern URL_PATTERN =
            Pattern.compile("(https?://\\S+|www\\.\\S+)", Pattern.CASE_INSENSITIVE);

    public CommentSpamService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    public SpamResult checkBeforeAdd(int userId, int postId, String contenu) {
        return check(userId, postId, 0, contenu, true);
    }

    public SpamResult checkBeforeUpdate(int userId, int postId, int commentId, String contenu) {
        return check(userId, postId, commentId, contenu, false);
    }

    private SpamResult check(int userId, int postId, int commentIdToExclude, String contenu, boolean checkFrequency) {
        if (contenu == null || contenu.trim().isEmpty()) {
            return SpamResult.blocked("Veuillez écrire un commentaire avant d’envoyer.", 10);
        }

        String original = contenu.trim();
        String normalized = normalize(original);

        int words = countWords(normalized);
        int urls = countUrls(original);

        if (words < 3) {
            return SpamResult.blocked("Votre commentaire est trop court. Merci d’écrire au moins 3 mots.", 5);
        }

        if (words > 120) {
            return SpamResult.blocked("Votre commentaire est trop long. Merci de ne pas dépasser 120 mots.", 5);
        }

        if (urls >= 2) {
            return SpamResult.blocked("Votre commentaire contient trop de liens. Un seul lien est autorisé.", 6);
        }

        if (urls == 1 && words < 6) {
            return SpamResult.blocked("Veuillez ajouter une explication claire avec votre lien.", 5);
        }

        if (hasTooManyUppercase(original)) {
            return SpamResult.blocked("Merci d’éviter d’écrire tout le commentaire en majuscules.", 5);
        }

        if (hasRepeatedCharacters(normalized)) {
            return SpamResult.blocked("Veuillez éviter les lettres répétées comme “merciiiiii” ou “nullllll”.", 5);
        }

        if (hasRepeatedWords(normalized)) {
            return SpamResult.blocked("Votre commentaire contient des mots répétés plusieurs fois. Merci de le reformuler.", 5);
        }

        try {
            if (checkFrequency && countRecentComments(userId) >= 3) {
                return SpamResult.blocked("Vous envoyez trop de commentaires rapidement. Veuillez attendre 1 minute avant de réessayer.", 8);
            }

            if (isExactDuplicate(userId, postId, normalized, commentIdToExclude)) {
                return SpamResult.blocked("Ce commentaire a déjà été publié. Veuillez écrire un commentaire différent.", 8);
            }

            if (isVerySimilarToRecentComment(userId, postId, normalized, commentIdToExclude)) {
                return SpamResult.blocked("Votre commentaire ressemble trop à un commentaire récent. Merci de le reformuler.", 7);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return SpamResult.blocked("Erreur lors de la vérification anti-spam.", 10);
        }

        return SpamResult.ok();
    }

    private int countRecentComments(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM commentaire " +
                "WHERE user_id = ? " +
                "AND date_creation >= DATE_SUB(NOW(), INTERVAL 1 MINUTE)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    private boolean isExactDuplicate(int userId, int postId, String normalizedContent, int commentIdToExclude) throws SQLException {
        String sql = "SELECT id, contenu FROM commentaire " +
                "WHERE user_id = ? " +
                "AND post_id = ? " +
                "AND date_creation >= DATE_SUB(NOW(), INTERVAL 1 DAY)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, postId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int oldId = rs.getInt("id");

                    if (commentIdToExclude > 0 && oldId == commentIdToExclude) {
                        continue;
                    }

                    String oldContent = normalize(rs.getString("contenu"));

                    if (oldContent.equals(normalizedContent)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isVerySimilarToRecentComment(int userId, int postId, String normalizedContent, int commentIdToExclude) throws SQLException {
        String sql = "SELECT id, contenu FROM commentaire " +
                "WHERE user_id = ? " +
                "AND post_id = ? " +
                "AND date_creation >= DATE_SUB(NOW(), INTERVAL 10 MINUTE) " +
                "ORDER BY date_creation DESC " +
                "LIMIT 5";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, postId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int oldId = rs.getInt("id");

                    if (commentIdToExclude > 0 && oldId == commentIdToExclude) {
                        continue;
                    }

                    String oldContent = normalize(rs.getString("contenu"));
                    double similarity = similarity(normalizedContent, oldContent);

                    if (similarity >= 0.85) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        String result = text.toLowerCase().trim();

        result = Normalizer.normalize(result, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        result = result.replaceAll("[^a-z0-9\\s]", " ");
        result = result.replaceAll("\\s+", " ");

        return result.trim();
    }

    private int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        return text.trim().split("\\s+").length;
    }

    private int countUrls(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        int count = 0;

        while (matcher.find()) {
            count++;
        }

        return count;
    }

    private boolean hasRepeatedCharacters(String text) {
        return text.matches(".*(.)\\1{5,}.*");
    }

    private boolean hasRepeatedWords(String text) {
        String[] words = text.split("\\s+");

        int repeatCount = 1;

        for (int i = 1; i < words.length; i++) {
            if (words[i].equals(words[i - 1])) {
                repeatCount++;

                if (repeatCount >= 4) {
                    return true;
                }
            } else {
                repeatCount = 1;
            }
        }

        return false;
    }

    private boolean hasTooManyUppercase(String text) {
        int letters = 0;
        int uppercase = 0;

        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                letters++;

                if (Character.isUpperCase(c)) {
                    uppercase++;
                }
            }
        }

        if (letters < 15) {
            return false;
        }

        double ratio = (double) uppercase / letters;
        return ratio >= 0.70;
    }

    private double similarity(String a, String b) {
        if (a == null || b == null) {
            return 0.0;
        }

        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }

        int distance = levenshtein(a, b);
        int maxLength = Math.max(a.length(), b.length());

        if (maxLength == 0) {
            return 1.0;
        }

        return 1.0 - ((double) distance / maxLength);
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(
                                dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1
                        ),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }
}