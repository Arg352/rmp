package com.asylum.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO для POST /posts — создание нового поста.
 */
public class CreatePostRequest {
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

    @SerializedName("images")
    private List<String> images;

    @SerializedName("allowedUserIds")
    private List<Integer> allowedUserIds = new ArrayList<>();

    public CreatePostRequest(String title, String text, String tags,
                              boolean isAnonymous, String visibility,
                              List<String> images) {
        this.title = title;
        this.text = text;
        this.tags = tags;
        this.isAnonymous = isAnonymous;
        this.visibility = visibility;
        this.images = images;
    }

    public String getTitle() { return title; }
    public String getText() { return text; }
    public String getTags() { return tags; }
    public boolean isAnonymous() { return isAnonymous; }
    public String getVisibility() { return visibility; }
    public List<String> getImages() { return images; }
    public List<Integer> getAllowedUserIds() { return allowedUserIds; }
    
    public void setAllowedUserIds(List<Integer> allowedUserIds) {
        this.allowedUserIds = allowedUserIds;
    }
}
