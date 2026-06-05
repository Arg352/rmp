package com.asylum.app;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.asylum.app.api.ApiService;
import com.asylum.app.api.MediaUploadResponse;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.models.CreatePostRequest;
import com.asylum.app.models.Post;
import com.asylum.app.models.UserProfile;
import com.asylum.app.utils.SessionManager;
import com.bumptech.glide.Glide;

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

public class CreatePostActivity extends AppCompatActivity {

    private EditText etTitle, etTags, etContent;
    private TextView tvHeaderDisplayName, tvHeaderUsername, tvVisibility;
    private CheckBox cbAnonymous;
    private ImageView btnEditName;
    private LinearLayout btnVisibility, btnAttachFile, attachmentsContainer;
    private View btnCreatePost, progressLayout;

    private String currentDisplayName = "";
    private String visibilityMode = "PUBLIC"; 
    private final List<Uri> selectedImageUris = new ArrayList<>();
    private String profileUsername = "";

    private SessionManager sessionManager;
    private ApiService apiService;

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
        setContentView(R.layout.activity_create_post);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance().getApiService();

        initViews();
        loadUserProfile();
        setupListeners();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etTags = findViewById(R.id.etTags);
        etContent = findViewById(R.id.etContent);
        tvHeaderDisplayName = findViewById(R.id.tvHeaderDisplayName);
        tvHeaderUsername = findViewById(R.id.tvHeaderUsername);
        tvVisibility = findViewById(R.id.tvVisibility);
        attachmentsContainer = findViewById(R.id.attachmentsContainer);
        cbAnonymous = findViewById(R.id.cbAnonymous);
        btnEditName = findViewById(R.id.btnEditName);
        btnVisibility = findViewById(R.id.btnVisibility);
        btnAttachFile = findViewById(R.id.btnAttachFile);
        btnCreatePost = findViewById(R.id.btnCreatePost);
        progressLayout = findViewById(R.id.progressLayout);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadUserProfile() {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.getMyProfile(token).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfile profile = response.body();
                    profileUsername = profile.getUsername();
                    currentDisplayName = profile.getDisplayName() != null && !profile.getDisplayName().isEmpty()
                            ? profile.getDisplayName() : profile.getUsername();
                    tvHeaderDisplayName.setText(currentDisplayName);
                    tvHeaderUsername.setText("@" + profileUsername);
                    
                    ImageView ivAvatar = findViewById(R.id.ivUserAvatar);
                    if (ivAvatar != null && profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
                        String url = profile.getAvatarUrl();
                        if (url.startsWith("/")) url = RetrofitClient.BASE_URL + url.substring(1);
                        Glide.with(CreatePostActivity.this).load(url).into(ivAvatar);
                    }
                }
            }
            @Override public void onFailure(Call<UserProfile> call, Throwable t) {}
        });
    }

    private void setupListeners() {
        cbAnonymous.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnEditName.setVisibility(View.VISIBLE);
                tvHeaderUsername.setVisibility(View.GONE);
                currentDisplayName = "Анонимус";
                tvHeaderDisplayName.setText(currentDisplayName);
            } else {
                btnEditName.setVisibility(View.GONE);
                tvHeaderUsername.setVisibility(View.VISIBLE);
                tvHeaderDisplayName.setText(currentDisplayName);
            }
        });

        btnEditName.setOnClickListener(v -> showEditNameDialog());
        btnAttachFile.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnVisibility.setOnClickListener(v -> showVisibilityMenu());

        // Валидация тегов при вводе
        etTags.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String original = s.toString();
                if (original.isEmpty()) return;
                
                String[] words = original.split(" ");
                StringBuilder sb = new StringBuilder();
                boolean changed = false;
                
                for (int i = 0; i < words.length; i++) {
                    String word = words[i];
                    if (!word.isEmpty()) {
                        if (!word.startsWith("#")) {
                            word = "#" + word;
                            changed = true;
                        }
                        sb.append(word);
                        if (i < words.length - 1 || original.endsWith(" ")) {
                            sb.append(" ");
                        }
                    }
                }
                
                if (changed) {
                    etTags.removeTextChangedListener(this);
                    etTags.setText(sb.toString());
                    etTags.setSelection(etTags.getText().length());
                    etTags.addTextChangedListener(this);
                }
            }
        });

        btnCreatePost.setOnClickListener(v -> {
            if (validateFields()) submitPost();
        });
    }

    private void addAttachment(Uri uri) {
        selectedImageUris.add(uri);
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_attachment_preview, attachmentsContainer, false);
        ImageView ivPreview = itemView.findViewById(R.id.ivPreview);
        ImageView btnRemove = itemView.findViewById(R.id.btnRemove);
        Glide.with(this).load(uri).into(ivPreview);
        btnRemove.setOnClickListener(v -> {
            selectedImageUris.remove(uri);
            attachmentsContainer.removeView(itemView);
        });
        attachmentsContainer.addView(itemView);
    }

    private void submitPost() {
        setLoading(true);
        if (selectedImageUris.isEmpty()) {
            createPost(new ArrayList<>());
        } else {
            uploadImagesAndCreatePost();
        }
    }

    private void uploadImagesAndCreatePost() {
        String token = sessionManager.getBearerToken();
        if (token == null) { setLoading(false); return; }

        List<MultipartBody.Part> parts = new ArrayList<>();
        for (Uri uri : selectedImageUris) {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) continue;
                byte[] bytes = getBytes(is);
                String mimeType = getContentResolver().getType(uri);
                if (mimeType == null) mimeType = "image/jpeg";

                RequestBody rb = RequestBody.create(MediaType.parse(mimeType), bytes);
                parts.add(MultipartBody.Part.createFormData("images", "post_" + System.currentTimeMillis() + ".jpg", rb));
            } catch (IOException e) {
                Toast.makeText(this, "Ошибка чтения файла", Toast.LENGTH_SHORT).show();
            }
        }

        if (parts.isEmpty()) { createPost(new ArrayList<>()); return; }

        apiService.uploadImages(token, parts).enqueue(new Callback<MediaUploadResponse>() {
            @Override
            public void onResponse(Call<MediaUploadResponse> call, Response<MediaUploadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    createPost(response.body().getUrls());
                } else {
                    setLoading(false);
                    Toast.makeText(CreatePostActivity.this, "Ошибка загрузки изображений (500)", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<MediaUploadResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CreatePostActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) byteBuffer.write(buffer, 0, len);
        return byteBuffer.toByteArray();
    }

    private void createPost(List<String> imageUrls) {
        String token = sessionManager.getBearerToken();
        String title = etTitle.getText().toString().trim();
        String text = etContent.getText().toString().trim();
        String tags = etTags.getText().toString().trim();
        boolean isAnon = cbAnonymous.isChecked();

        CreatePostRequest request = new CreatePostRequest(title, text, tags, isAnon, visibilityMode, imageUrls);
        apiService.createPost(token, request).enqueue(new Callback<Post>() {
            @Override
            public void onResponse(Call<Post> call, Response<Post> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(CreatePostActivity.this, "Запись создана!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreatePostActivity.this, "Ошибка создания: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Post> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CreatePostActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditNameDialog() {
        EditText input = new EditText(this);
        input.setText(currentDisplayName);
        new AlertDialog.Builder(this).setTitle("Имя автора").setView(input)
                .setPositiveButton("ОК", (d, w) -> {
                    currentDisplayName = input.getText().toString();
                    tvHeaderDisplayName.setText(currentDisplayName);
                }).show();
    }

    private void showVisibilityMenu() {
        PopupMenu popup = new PopupMenu(this, btnVisibility);
        popup.getMenu().add(0, 0, 0, "Все пользователи");
        popup.getMenu().add(0, 1, 1, "Только подписчики");
        popup.getMenu().add(0, 2, 2, "Приватно");
        popup.setOnMenuItemClickListener(item -> {
            visibilityMode = item.getItemId() == 0 ? "PUBLIC" : (item.getItemId() == 1 ? "FOLLOWERS" : "PRIVATE");
            tvVisibility.setText(item.getTitle());
            return true;
        });
        popup.show();
    }

    private boolean validateFields() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Введите заголовок", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void setLoading(boolean loading) {
        btnCreatePost.setEnabled(!loading);
        if (progressLayout != null) progressLayout.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
