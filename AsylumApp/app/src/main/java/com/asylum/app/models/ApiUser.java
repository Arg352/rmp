package com.asylum.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Краткая информация о пользователе — встраивается в посты, чаты, сообщения.
 * Соответствует полям, которые бэкенд возвращает в select { id, username, displayName, avatarUrl }.
 */
public class ApiUser {
    @SerializedName("id")
    private int id;

    @SerializedName("username")
    private String username;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("email")
    private String email;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("lastActiveAt")
    private String lastActiveAt;

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getLastActiveAt() { return lastActiveAt; }

    /** Возвращает displayName если есть, иначе username */
    public String getDisplayOrUsername() {
        return (displayName != null && !displayName.isEmpty()) ? displayName : username;
    }
}
