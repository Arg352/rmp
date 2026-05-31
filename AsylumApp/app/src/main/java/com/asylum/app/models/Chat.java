package com.asylum.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Модель чата — соответствует элементу массива GET /chats.
 * Содержит собеседника и последнее сообщение.
 */
public class Chat {
    /** Пользователь-собеседник */
    @SerializedName("user")
    private ApiUser user;

    /** Последнее сообщение в чате */
    @SerializedName("lastMessage")
    private LastMessage lastMessage;

    /** Количество непрочитанных сообщений */
    @SerializedName("unreadCount")
    private int unreadCount;

    /** Статус уведомлений (заглушен или нет) */
    private boolean isMuted;

    public ApiUser getUser() { return user; }
    public LastMessage getLastMessage() { return lastMessage; }
    public int getUnreadCount() { return unreadCount; }
    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { isMuted = muted; }

    // Вспомогательные методы для адаптеров
    public String getParticipantName() {
        return user != null ? user.getDisplayOrUsername() : "Неизвестный";
    }

    public String getAvatarLetter() {
        String name = getParticipantName();
        return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
    }

    public String getLastMessageText() {
        return lastMessage != null ? lastMessage.getText() : "";
    }

    public String getLastMessageTime() {
        if (lastMessage == null) return "";
        String createdAt = lastMessage.getCreatedAt();
        if (createdAt == null) return "";
        try {
            if (createdAt.length() >= 16) {
                return createdAt.substring(11, 16); 
            }
        } catch (Exception ignored) {}
        return createdAt;
    }

    public static class LastMessage {
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

        public int getId() { return id; }
        public String getText() { return text; }
        public String getCreatedAt() { return createdAt; }
        public boolean isRead() { return isRead; }
        public int getSenderId() { return senderId; }
        public int getReceiverId() { return receiverId; }
    }
}
