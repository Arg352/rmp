package com.asylum.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * DTO для PATCH /users/settings — обновление настроек профиля.
 */
public class UpdateSettingsRequest {
    @SerializedName("username")
    private String username;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("bio")
    private String bio;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("notifyOnMessages")
    private Boolean notifyOnMessages;

    @SerializedName("notifyOnGroups")
    private Boolean notifyOnGroups;

    @SerializedName("notifyOnFollows")
    private Boolean notifyOnFollows;

    @SerializedName("notifyOnLikes")
    private Boolean notifyOnLikes;

    // Builder-style setters
    public UpdateSettingsRequest setUsername(String username) { this.username = username; return this; }
    public UpdateSettingsRequest setDisplayName(String displayName) { this.displayName = displayName; return this; }
    public UpdateSettingsRequest setEmail(String email) { this.email = email; return this; }
    public UpdateSettingsRequest setPassword(String password) { this.password = password; return this; }
    public UpdateSettingsRequest setBio(String bio) { this.bio = bio; return this; }
    public UpdateSettingsRequest setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
    public UpdateSettingsRequest setNotifyOnMessages(Boolean v) { this.notifyOnMessages = v; return this; }
    public UpdateSettingsRequest setNotifyOnGroups(Boolean v) { this.notifyOnGroups = v; return this; }
    public UpdateSettingsRequest setNotifyOnFollows(Boolean v) { this.notifyOnFollows = v; return this; }
    public UpdateSettingsRequest setNotifyOnLikes(Boolean v) { this.notifyOnLikes = v; return this; }
}
