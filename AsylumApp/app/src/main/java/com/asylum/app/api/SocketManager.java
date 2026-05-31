package com.asylum.app.api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.net.URISyntaxException;

/**
 * Singleton-менеджер WebSocket (Socket.IO) подключения к Asylum бэкенду.
 *
 * Использование:
 *   SocketManager.getInstance().connect(token);
 *   SocketManager.getInstance().sendMessage(receiverId, text);
 *   SocketManager.getInstance().onNewMessage(listener);
 *   SocketManager.getInstance().disconnect();
 */
public class SocketManager {
    private static final String TAG = "SocketManager";

    // Для эмулятора: 10.0.2.2 = localhost хост-машины
    private static final String SERVER_URL = "http://10.0.2.2:3000";

    private static SocketManager instance;
    private Socket socket;

    private SocketManager() {}

    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    /**
     * Подключиться к серверу с JWT-токеном.
     * Вызывать после успешного логина.
     */
    public void connect(String token) {
        if (socket != null && socket.connected()) return;

        try {
            IO.Options options = new IO.Options();
            options.auth = java.util.Collections.singletonMap("token", token);
            options.reconnection = true;
            options.reconnectionAttempts = 5;
            options.reconnectionDelay = 1000;

            // Используем перегрузку IO.socket(String, Options), которая выбрасывает URISyntaxException.
            // URI.create() не выбрасывает проверяемое исключение URISyntaxException.
            socket = IO.socket(SERVER_URL, options);
            socket.connect();

            socket.on(Socket.EVENT_CONNECT, args ->
                    Log.d(TAG, "Socket connected"));
            socket.on(Socket.EVENT_DISCONNECT, args ->
                    Log.d(TAG, "Socket disconnected"));
            socket.on(Socket.EVENT_CONNECT_ERROR, args ->
                    Log.e(TAG, "Socket connect error: " + args[0]));

        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid server URL", e);
        }
    }

    /** Отключиться от сервера */
    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
        }
    }

    /** Проверить состояние подключения */
    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    /**
     * Отправить личное сообщение.
     * Событие: "sendMessage" { receiverId, text }
     */
    public void sendMessage(int receiverId, String text) {
        if (!isConnected()) {
            Log.w(TAG, "Socket not connected, cannot send message");
            return;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put("receiverId", receiverId);
            payload.put("text", text);
            socket.emit("sendMessage", payload);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build sendMessage payload", e);
        }
    }

    /**
     * Отправить сообщение в группу.
     * Событие: "sendGroupMessage" { groupId, text }
     */
    public void sendGroupMessage(int groupId, String text) {
        if (!isConnected()) {
            Log.w(TAG, "Socket not connected, cannot send group message");
            return;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put("groupId", groupId);
            payload.put("text", text);
            socket.emit("sendGroupMessage", payload);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build sendGroupMessage payload", e);
        }
    }

    /**
     * Подписаться на входящие личные сообщения.
     * Событие: "newMessage" — вызывается при получении сообщения.
     */
    public void onNewMessage(NewMessageListener listener) {
        if (socket == null) return;
        socket.off("newMessage"); // убираем старый листенер
        socket.on("newMessage", args -> {
            if (args.length > 0 && args[0] instanceof JSONObject) {
                listener.onNewMessage((JSONObject) args[0]);
            }
        });
    }

    /**
     * Подписаться на входящие групповые сообщения.
     */
    public void onNewGroupMessage(NewMessageListener listener) {
        if (socket == null) return;
        socket.off("newGroupMessage");
        socket.on("newGroupMessage", args -> {
            if (args.length > 0 && args[0] instanceof JSONObject) {
                listener.onNewMessage((JSONObject) args[0]);
            }
        });
    }

    /** Снять все листенеры с определённого события */
    public void off(String event) {
        if (socket != null) socket.off(event);
    }

    public interface NewMessageListener {
        void onNewMessage(JSONObject messageJson);
    }
}
