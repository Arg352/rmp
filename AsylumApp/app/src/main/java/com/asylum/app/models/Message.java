package com.asylum.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Модель сообщения — соответствует ответу GET /chats/{userId}
 * и событию WebSocket newMessage.
 */
public class Message {
    @SerializedName("id")
    private int id;

    @SerializedName("text")
    private String text;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("isRead")
    private boolean isRead;

    @SerializedName("senderId")
    private int senderId;

    @SerializedName("receiverId")
    private int receiverId;

    @SerializedName("attachments")
    private List<ImageAttachment> attachments;

    // Конструктор для локального создания (WebSocket optimistic update)
    public Message(int senderId, String text) {
        this.senderId = senderId;
        this.text = text;
        this.createdAt = "";
    }

    public int getId() { return id; }
    public String getText() { return text; }
    public String getCreatedAt() { return createdAt; }
    public boolean isRead() { return isRead; }
    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public List<ImageAttachment> getAttachments() { return attachments; }

    public void setAttachments(List<ImageAttachment> attachments) {
        this.attachments = attachments;
    }

    /** Определяем "исходящее" ли сообщение по senderId */
    public boolean isOutgoing(int myUserId) {
        return senderId == myUserId;
    }

    /** Форматированное время */
    public String getFormattedTime() {
        if (createdAt == null || createdAt.isEmpty()) return "";
        try {
            if (createdAt.length() >= 16) return createdAt.substring(11, 16);
        } catch (Exception ignored) {}
        return createdAt;
    }

    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }
}
