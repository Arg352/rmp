package com.asylum.app.models;

public class User {
    private String id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String status; // "FRIEND", "SENT", "RECEIVED", "NONE"

    public User(String id, String username, String fullName, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.status = "NONE";
    }

    public User(String id, String username, String fullName, String avatarUrl, String status) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.status = status;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
