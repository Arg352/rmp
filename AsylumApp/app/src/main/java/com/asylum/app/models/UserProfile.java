package com.asylum.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Модель профиля пользователя, возвращаемая GET /users/me
 */
public class UserProfile {
    @SerializedName("id")
    private int id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("bio")
    private String bio;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("lastActiveAt")
    private String lastActiveAt;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("notifyOnMessages")
    private boolean notifyOnMessages;

    @SerializedName("notifyOnGroups")
    private boolean notifyOnGroups;

    @SerializedName("notifyOnFollows")
    private boolean notifyOnFollows;

    @SerializedName("notifyOnLikes")
    private boolean notifyOnLikes;

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getBio() { return bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getLastActiveAt() { return lastActiveAt; }
    public String getCreatedAt() { return createdAt; }
    public boolean isNotifyOnMessages() { return notifyOnMessages; }
    public boolean isNotifyOnGroups() { return notifyOnGroups; }
    public boolean isNotifyOnFollows() { return notifyOnFollows; }
    public boolean isNotifyOnLikes() { return notifyOnLikes; }
}
