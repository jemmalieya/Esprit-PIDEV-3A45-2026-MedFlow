package tn.esprit.services;

import tn.esprit.tools.MyDataBase;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReactionService {

    private final Connection cn;

    public ReactionService() {
        cn = MyDataBase.getInstance().getCnx();
    }

    public int countReactionsByPost(int postId) {
        String sql = "SELECT COUNT(*) FROM reaction WHERE post_id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, postId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public String getUserReactionType(int postId, int userId) {
        String sql = "SELECT type FROM reaction WHERE post_id = ? AND user_id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("type");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean toggleReaction(int postId, int userId, String type) {
        String oldType = getUserReactionType(postId, userId);

        if (oldType == null) {
            return addReaction(postId, userId, type);
        }

        if (oldType.equalsIgnoreCase(type)) {
            return deleteReaction(postId, userId);
        }

        return updateReaction(postId, userId, type);
    }

    private boolean addReaction(int postId, int userId, String type) {
        String sql = "INSERT INTO reaction(post_id, user_id, type, created_at) VALUES (?, ?, ?, NOW())";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ps.setString(3, type);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean updateReaction(int postId, int userId, String type) {
        String sql = "UPDATE reaction SET type = ?, created_at = NOW() WHERE post_id = ? AND user_id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setInt(2, postId);
            ps.setInt(3, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean deleteReaction(int postId, int userId) {
        String sql = "DELETE FROM reaction WHERE post_id = ? AND user_id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, Integer> countReactionTypesByPost(int postId) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("like", 0);
        stats.put("love", 0);
        stats.put("haha", 0);
        stats.put("wow", 0);
        stats.put("sad", 0);
        stats.put("angry", 0);

        String sql = "SELECT type, COUNT(*) AS total FROM reaction WHERE post_id = ? GROUP BY type";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, postId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stats.put(rs.getString("type"), rs.getInt("total"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return stats;
    }
}