package com.asylum.app;

import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asylum.app.adapters.MessagesAdapter;
import com.asylum.app.api.ApiService;
import com.asylum.app.api.MediaUploadResponse;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.api.SocketManager;
import com.asylum.app.models.Message;
import com.asylum.app.utils.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private MessagesAdapter adapter;
    private List<Message> messages = new ArrayList<>();
    private LinearLayoutManager layoutManager;

    private SessionManager sessionManager;
    private ApiService apiService;
    private SocketManager socketManager;

    private int otherUserId;
    private int myUserId;
    private boolean isMuted = false;

    private final List<Uri> selectedImageUris = new ArrayList<>();

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUris.add(uri);
                    Toast.makeText(this, "Изображение прикреплено (" + selectedImageUris.size() + ")", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance().getApiService();
        socketManager = SocketManager.getInstance();
        myUserId = sessionManager.getUserId();

        otherUserId = getIntent().getIntExtra("CHAT_USER_ID", -1);
        String chatName = getIntent().getStringExtra("CHAT_NAME");
        isMuted = getIntent().getBooleanExtra("IS_MUTED", false);
        String prefillMessage = getIntent().getStringExtra("PREFILL_MESSAGE");

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView tvChatTitle = findViewById(R.id.tvChatTitle);
        TextView tvAvatarLetter = findViewById(R.id.tvAvatarLetter);
        ImageView btnMute = findViewById(R.id.btnMute);

        String chatUsername = getIntent().getStringExtra("CHAT_USERNAME");

        if (chatName != null) {
            tvChatTitle.setText(chatName);
            tvAvatarLetter.setText(chatName.substring(0, 1).toUpperCase());
        }

        TextView tvUserHandle = findViewById(R.id.tvUserHandle);
        if (tvUserHandle != null && chatUsername != null) {
            tvUserHandle.setText(chatUsername);
        }

        TextView tvStatus = findViewById(R.id.tvStatus);
        if (tvStatus != null) {
            tvStatus.setText("в сети");
        }

        updateMuteUI(btnMute);
        btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            updateMuteUI(btnMute);
        });

        rvMessages = findViewById(R.id.rvMessages);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);

        adapter = new MessagesAdapter(messages, myUserId);
        rvMessages.setAdapter(adapter);

        etMessage = findViewById(R.id.etMessage);
        if (prefillMessage != null) {
            etMessage.setText(prefillMessage);
        }

        ImageView btnSend = findViewById(R.id.btnSend);
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> sendMessage());
        }

        ImageView btnAttachView = findViewById(R.id.btnAttach);
        if (btnAttachView != null) {
            btnAttachView.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        }

        loadChatHistory();
        setupWebSocket();
    }

    private void updateMuteUI(ImageView btnMute) {
        if (btnMute == null) return;
        btnMute.setImageResource(isMuted ? R.drawable.ic_mute : R.drawable.ic_volume_up);
        int color = getResources().getColor(isMuted ? R.color.asylum_red : R.color.text_secondary);
        btnMute.setColorFilter(color);
    }

    private void loadChatHistory() {
        if (otherUserId == -1) return;
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.getChatHistory(token, otherUserId).enqueue(new Callback<List<Message>>() {
            @Override
            public void onResponse(Call<List<Message>> call, Response<List<Message>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messages.clear();
                    messages.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    scrollToBottom();
                }
            }

            @Override
            public void onFailure(Call<List<Message>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Не удалось загрузить историю", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupWebSocket() {
        String token = sessionManager.getToken();
        if (token == null) return;

        socketManager.connect(token);
        socketManager.onNewMessage(messageJson -> runOnUiThread(() -> {
            try {
                int senderId = messageJson.getInt("senderId");
                int receiverId = messageJson.getInt("receiverId");

                if (senderId == myUserId) return;

                boolean isThisChat = (senderId == otherUserId && receiverId == myUserId);

                if (isThisChat) {
                    Message msg = new Message(senderId, messageJson.getString("text"));
                    adapter.addMessage(msg);
                    scrollToBottom();
                }
            } catch (JSONException e) {
            }
        }));
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty() && selectedImageUris.isEmpty()) return;

        if (selectedImageUris.isEmpty()) {
            sendViaWebSocket(text, null);
        } else {
            uploadImagesAndSend(text);
        }
    }

    private void uploadImagesAndSend(String text) {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        List<MultipartBody.Part> parts = new ArrayList<>();
        for (Uri uri : selectedImageUris) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) continue;
                byte[] bytes = inputStream.readAllBytes();
                inputStream.close();

                RequestBody requestBody = RequestBody.create(bytes, MediaType.parse("image/*"));
                MultipartBody.Part part = MultipartBody.Part.createFormData("images",
                        "image_" + System.currentTimeMillis() + ".jpg", requestBody);
                parts.add(part);
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка чтения изображения", Toast.LENGTH_SHORT).show();
            }
        }

        if (parts.isEmpty()) {
            sendViaWebSocket(text, null);
            return;
        }

        apiService.uploadImages(token, parts).enqueue(new Callback<MediaUploadResponse>() {
            @Override
            public void onResponse(Call<MediaUploadResponse> call, Response<MediaUploadResponse> response) {
                List<String> urls = null;
                if (response.isSuccessful() && response.body() != null) {
                    urls = response.body().getUrls();
                }
                sendViaWebSocket(text, urls);
                selectedImageUris.clear();
            }

            @Override
            public void onFailure(Call<MediaUploadResponse> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Ошибка загрузки изображений", Toast.LENGTH_SHORT).show();
                sendViaWebSocket(text, null);
                selectedImageUris.clear();
            }
        });
    }

    private void sendViaWebSocket(String text, List<String> imageUrls) {
        if (otherUserId == -1) {
            Toast.makeText(this, "Нет ID собеседника", Toast.LENGTH_SHORT).show();
            return;
        }

        Message optimisticMsg = new Message(myUserId, text);
        adapter.addMessage(optimisticMsg);
        scrollToBottom();
        etMessage.setText("");

        if (socketManager.isConnected()) {
            socketManager.sendMessage(otherUserId, text);
        } else {
            Toast.makeText(this, "WebSocket не подключён", Toast.LENGTH_SHORT).show();
        }
    }

    private void scrollToBottom() {
        if (messages.size() > 0) {
            rvMessages.smoothScrollToPosition(messages.size() - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socketManager.off("newMessage");
    }
}
