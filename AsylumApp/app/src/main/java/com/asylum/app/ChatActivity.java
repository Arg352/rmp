package com.asylum.app;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private LinearLayout attachmentsContainer;
    private View attachmentPreviewArea;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && selectedImageUris.size() < 5) {
                    addAttachment(uri);
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

        if (chatName != null) {
            tvChatTitle.setText(chatName);
            tvAvatarLetter.setText(chatName.substring(0, 1).toUpperCase());
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

        attachmentPreviewArea = findViewById(R.id.attachmentPreviewArea);
        attachmentsContainer = findViewById(R.id.attachmentsContainer);

        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
        findViewById(R.id.btnAttach).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        loadChatHistory();
        setupWebSocket();
    }

    private void addAttachment(Uri uri) {
        selectedImageUris.add(uri);
        if (attachmentPreviewArea != null) {
            attachmentPreviewArea.setVisibility(View.VISIBLE);
        }

        View itemView = LayoutInflater.from(this).inflate(R.layout.item_attachment_preview, attachmentsContainer, false);
        ImageView ivPreview = itemView.findViewById(R.id.ivPreview);
        ImageView btnRemove = itemView.findViewById(R.id.btnRemove);

        int size = (int) (60 * getResources().getDisplayMetrics().density);
        itemView.getLayoutParams().width = size;
        itemView.getLayoutParams().height = size;

        Glide.with(this).load(uri).into(ivPreview);

        btnRemove.setOnClickListener(v -> {
            selectedImageUris.remove(uri);
            attachmentsContainer.removeView(itemView);
            if (selectedImageUris.isEmpty() && attachmentPreviewArea != null) {
                attachmentPreviewArea.setVisibility(View.GONE);
            }
        });

        attachmentsContainer.addView(itemView);
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
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                if (inputStream == null) continue;
                byte[] bytes = getBytes(inputStream);
                
                String mimeType = getContentResolver().getType(uri);
                if (mimeType == null) mimeType = "image/jpeg";

                // В OkHttp 4.x Java порядок: create(bytes, MediaType)
                RequestBody requestBody = RequestBody.create(bytes, MediaType.parse(mimeType));
                MultipartBody.Part part = MultipartBody.Part.createFormData("images",
                        "chat_" + System.currentTimeMillis() + ".jpg", requestBody);
                parts.add(part);
            } catch (IOException e) {
                Toast.makeText(this, "Ошибка чтения изображения", Toast.LENGTH_SHORT).show();
            }
        }

        apiService.uploadImages(token, parts).enqueue(new Callback<MediaUploadResponse>() {
            @Override
            public void onResponse(Call<MediaUploadResponse> call, Response<MediaUploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> urls = response.body().getUrls();
                    sendViaWebSocket(text, urls);
                    clearAttachments();
                } else {
                    Toast.makeText(ChatActivity.this, "Ошибка загрузки (500): проверьте сервер", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MediaUploadResponse> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void clearAttachments() {
        selectedImageUris.clear();
        attachmentsContainer.removeAllViews();
        if (attachmentPreviewArea != null) {
            attachmentPreviewArea.setVisibility(View.GONE);
        }
    }

    private void sendViaWebSocket(String text, List<String> imageUrls) {
        if (otherUserId == -1) return;

        Message optimisticMsg = new Message(myUserId, text);
        if (imageUrls != null) {
            List<com.asylum.app.models.ImageAttachment> attachments = new ArrayList<>();
            for (String url : imageUrls) {
                attachments.add(new com.asylum.app.models.ImageAttachment(url));
            }
            optimisticMsg.setAttachments(attachments);
        }
        adapter.addMessage(optimisticMsg);
        scrollToBottom();
        etMessage.setText("");

        if (socketManager.isConnected()) {
            socketManager.sendMessage(otherUserId, text, imageUrls);
        }
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
            public void onFailure(Call<List<Message>> call, Throwable t) {}
        });
    }

    private void setupWebSocket() {
        String token = sessionManager.getToken();
        if (token == null) return;

        socketManager.connect(token);
        socketManager.onNewMessage(messageJson -> runOnUiThread(() -> {
            try {
                int senderId = messageJson.getInt("senderId");
                if (senderId == otherUserId) {
                    Message msg = new Message(senderId, messageJson.getString("text"));
                    if (messageJson.has("imageUrls")) {
                        JSONArray urlsArr = messageJson.getJSONArray("imageUrls");
                        List<com.asylum.app.models.ImageAttachment> attachments = new ArrayList<>();
                        for (int i = 0; i < urlsArr.length(); i++) {
                            attachments.add(new com.asylum.app.models.ImageAttachment(urlsArr.getString(i)));
                        }
                        msg.setAttachments(attachments);
                    }
                    adapter.addMessage(msg);
                    scrollToBottom();
                }
            } catch (JSONException ignored) {}
        }));
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
