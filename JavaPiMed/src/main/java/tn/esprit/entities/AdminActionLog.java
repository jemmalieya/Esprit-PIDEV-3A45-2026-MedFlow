package tn.esprit.entities;

import java.time.LocalDateTime;

public class AdminActionLog {
    private LocalDateTime timestamp;
    private String adminName;
    private String action;
    private String targetType;
    private int targetId;
    private String details;

    public AdminActionLog() {
    }

    public AdminActionLog(LocalDateTime timestamp, String adminName, String action, String targetType, int targetId, String details) {
        this.timestamp = timestamp;
        this.adminName = adminName;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

