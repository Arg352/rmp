package com.asylum.app.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Ответ POST /media/upload — список URL загруженных изображений.
 */
public class MediaUploadResponse {
    @SerializedName("urls")
    private List<String> urls;

    public List<String> getUrls() { return urls; }
}
