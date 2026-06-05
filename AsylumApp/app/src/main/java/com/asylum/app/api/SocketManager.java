package com.asylum.app.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Singleton-менеджер WebSocket (Socket.IO) подключения к Asylum бэкенду.
 */
public class SocketManager {
    private static final String TAG = "SocketManager";
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

    public void connect(String token) {
        if (socket != null && socket.connected()) return;

        try {
            IO.Options options = new IO.Options();
            options.auth = java.util.Collections.singletonMap("token", token);
            options.reconnection = true;

            socket = IO.socket(SERVER_URL, options);
            socket.connect();

            socket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "Socket connected"));
            socket.on(Socket.EVENT_DISCONNECT, args -> Log.d(TAG, "Socket disconnected"));
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> Log.e(TAG, "Socket connect error: " + args[0]));

        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid server URL", e);
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    /**
     * Отправить личное сообщение (с поддержкой изображений).
     */
    public void sendMessage(int receiverId, String text, List<String> imageUrls) {
        if (!isConnected()) {
            Log.w(TAG, "Socket not connected, cannot send message");
            return;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put("receiverId", receiverId);
            payload.put("text", text);
            if (imageUrls != null && !imageUrls.isEmpty()) {
                payload.put("imageUrls", new JSONArray(imageUrls));
            }
            socket.emit("sendMessage", payload);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build sendMessage payload", e);
        }
    }

    /** Устаревшая версия для совместимости */
    public void sendMessage(int receiverId, String text) {
        sendMessage(receiverId, text, null);
    }

    public void onNewMessage(NewMessageListener listener) {
        if (socket == null) return;
        socket.off("newMessage");
        socket.on("newMessage", args -> {
            if (args.length > 0 && args[0] instanceof JSONObject) {
                listener.onNewMessage((JSONObject) args[0]);
            }
        });
    }

    public void off(String event) {
        if (socket != null) socket.off(event);
    }

    public interface NewMessageListener {
        void onNewMessage(JSONObject messageJson);
    }
}
