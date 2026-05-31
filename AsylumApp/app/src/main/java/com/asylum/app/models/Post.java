package com.asylum.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Модель поста — соответствует ответу GET /posts/feed и POST /posts.
 */
public class Post {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("text")
    private String text;

    @SerializedName("tags")
    private String tags;

    @SerializedName("isAnonymous")
    private boolean isAnonymous;

    @SerializedName("visibility")
    private String visibility;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("user")
    private ApiUser user;

    @SerializedName("images")
    private List<ImageAttachment> images;

    @SerializedName("likesCount")
    private int likesCount;

    @SerializedName("isLiked")
    private boolean isLiked;

    // Геттеры
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public String getTags() { return tags; }
    public boolean isAnonymous() { return isAnonymous; }
    public String getVisibility() { return visibility; }
    public String getCreatedAt() { return createdAt; }
    public ApiUser getUser() { return user; }
    public List<ImageAttachment> getImages() { return images; }
    public int getLikesCount() { return likesCount; }
    public boolean isLiked() { return isLiked; }

    // Вспомогательные методы для адаптеров
    public String getAuthorName() {
        if (user == null) return "Анонимус";
        return user.getDisplayOrUsername();
    }

    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }

    /** Локальное обновление состояния лайка (без запроса к сети) */
    public void toggleLike() {
        if (isLiked) {
            isLiked = false;
            likesCount--;
        } else {
            isLiked = true;
            likesCount++;
        }
    }
}
