package com.asylum.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Модель вложения (изображения) в посте или сообщении.
 */
public class ImageAttachment {
    @SerializedName("id")
    private int id;

    @SerializedName("url")
    private String url;

    public ImageAttachment() {
    }

    public ImageAttachment(String url) {
        this.url = url;
    }

    public int getId() { return id; }
    public String getUrl() { return url; }
}
