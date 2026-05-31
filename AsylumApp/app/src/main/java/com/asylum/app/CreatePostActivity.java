package com.asylum.app;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePostActivity extends AppCompatActivity {

    private EditText etTitle, etTags, etContent;
    private TextView tvHeaderDisplayName, tvHeaderUsername, tvVisibility, tvAttachedCount;
    private CheckBox cbAnonymous;
    private ImageView btnEditName;
    private LinearLayout btnVisibility, btnAttachFile;
    private View btnCreatePost, progressLayout;

    private String currentDisplayName = "";
    private String visibilityMode = "PUBLIC"; // PUBLIC, FOLLOWERS, PRIVATE
    private final List<Uri> selectedImageUris = new ArrayList<>();
    private String profileUsername = "";

    private SessionManager sessionManager;
    private ApiService apiService;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null && selectedImageUris.size() < 5) {
                    selectedImageUris.add(uri);
                    updateAttachedCount();
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
        tvAttachedCount = findViewById(R.id.tvLinkInfo);
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
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                String saved = sessionManager.getUsername();
                if (saved != null) {
                    currentDisplayName = saved;
                    profileUsername = saved;
                    tvHeaderDisplayName.setText(currentDisplayName);
                    tvHeaderUsername.setText("@" + profileUsername);
                }
            }
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

        btnCreatePost.setOnClickListener(v -> {
            if (validateFields()) {
                submitPost();
            }
        });
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
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) continue;
                byte[] bytes = inputStream.readAllBytes();
                inputStream.close();

                String mimeType = getContentResolver().getType(uri);
                if (mimeType == null) mimeType = "image/jpeg";

                RequestBody requestBody = RequestBody.create(bytes, MediaType.parse(mimeType));
                MultipartBody.Part part = MultipartBody.Part.createFormData(
                        "images",
                        "img_" + System.currentTimeMillis() + ".jpg",
                        requestBody
                );
                parts.add(part);
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка чтения файла", Toast.LENGTH_SHORT).show();
            }
        }

        if (parts.isEmpty()) {
            createPost(new ArrayList<>());
            return;
        }

        apiService.uploadImages(token, parts).enqueue(new Callback<MediaUploadResponse>() {
            @Override
            public void onResponse(Call<MediaUploadResponse> call, Response<MediaUploadResponse> response) {
                List<String> urls = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    urls = response.body().getUrls();
                }
                createPost(urls);
            }

            @Override
            public void onFailure(Call<MediaUploadResponse> call, Throwable t) {
                Toast.makeText(CreatePostActivity.this, "Ошибка загрузки изображений. Создаём пост без них.", Toast.LENGTH_SHORT).show();
                createPost(new ArrayList<>());
            }
        });
    }

    private void createPost(List<String> imageUrls) {
        String token = sessionManager.getBearerToken();
        if (token == null) { setLoading(false); return; }

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

            @Override
            public void onFailure(Call<Post> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CreatePostActivity.this, "Нет подключения к серверу", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditNameDialog() {
        EditText input = new EditText(this);
        input.setText(currentDisplayName);
        new AlertDialog.Builder(this)
                .setTitle("Изменить имя автора")
                .setView(input)
                .setPositiveButton("ОК", (dialog, which) -> {
                    currentDisplayName = input.getText().toString();
                    tvHeaderDisplayName.setText(currentDisplayName);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showVisibilityMenu() {
        PopupMenu popup = new PopupMenu(this, btnVisibility);
        popup.getMenu().add(0, 0, 0, "Все пользователи");
        popup.getMenu().add(0, 1, 1, "Только подписчики");
        popup.getMenu().add(0, 2, 2, "Приватно");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 0: visibilityMode = "PUBLIC"; break;
                case 1: visibilityMode = "FOLLOWERS"; break;
                case 2: visibilityMode = "PRIVATE"; break;
            }
            tvVisibility.setText(item.getTitle());
            return true;
        });
        popup.show();
    }

    private void updateAttachedCount() {
        if (tvAttachedCount != null) {
            if (selectedImageUris.isEmpty()) {
                tvAttachedCount.setVisibility(View.GONE);
            } else {
                tvAttachedCount.setVisibility(View.VISIBLE);
                tvAttachedCount.setText("Прикреплено изображений: " + selectedImageUris.size());
            }
        }
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
        if (progressLayout != null) {
            progressLayout.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }
}
