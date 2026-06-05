package com.asylum.app;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.asylum.app.api.ApiService;
import com.asylum.app.api.MediaUploadResponse;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.models.UpdateSettingsRequest;
import com.asylum.app.models.UserProfile;
import com.asylum.app.utils.SessionManager;
import com.bumptech.glide.Glide;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvLoginValue, tvEmailValue, tvDisplayNameValue;
    private View rlDisplayName, rlEmail, rlPassword;
    private CircleImageView ivAvatar;
    private TextView btnChangeAvatar;
    private SwitchCompat switchMessages, switchGroups, switchFollows, switchLikes, switchDarkMode;
    private SessionManager sessionManager;
    private ApiService apiService;
    private UserProfile currentProfile;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadAvatar(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance().getApiService();

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ivAvatar = findViewById(R.id.ivAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        tvLoginValue = findViewById(R.id.tvLoginValue);
        tvEmailValue = findViewById(R.id.tvEmailValue);
        tvDisplayNameValue = findViewById(R.id.tvDisplayNameValue);

        rlDisplayName = findViewById(R.id.rlDisplayName);
        rlEmail = findViewById(R.id.rlEmail);
        rlPassword = findViewById(R.id.rlPassword);

        switchMessages = findViewById(R.id.switchMessages);
        switchGroups = findViewById(R.id.switchGroups);
        switchFollows = findViewById(R.id.switchFollows);
        switchLikes = findViewById(R.id.switchLikes);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        // Инициализация переключателя тёмной темы
        if (switchDarkMode != null) {
            switchDarkMode.setChecked(sessionManager.isDarkMode());
            switchDarkMode.setOnCheckedChangeListener((btn, isChecked) -> {
                sessionManager.setDarkMode(isChecked);
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                                  : AppCompatDelegate.MODE_NIGHT_NO);
                recreate(); // пересоздать активити чтобы применить тему
            });
        }

        loadProfile();
        setupClickListeners();
    }

    private void loadProfile() {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.getMyProfile(token).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentProfile = response.body();
                    displayProfile(currentProfile);
                    setupSwitchListeners();
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                Toast.makeText(SettingsActivity.this, "Ошибка загрузки настроек", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayProfile(UserProfile profile) {
        if (tvLoginValue != null) tvLoginValue.setText("@" + profile.getUsername());
        if (tvEmailValue != null) tvEmailValue.setText(profile.getEmail() != null ? profile.getEmail() : "—");
        if (tvDisplayNameValue != null) tvDisplayNameValue.setText(profile.getDisplayName() != null ? profile.getDisplayName() : "—");

        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
            Glide.with(this).load(profile.getAvatarUrl()).into(ivAvatar);
        } else {
            ivAvatar.setImageResource(R.color.asylum_red);
        }

        if (switchMessages != null) switchMessages.setChecked(profile.isNotifyOnMessages());
        if (switchGroups != null) switchGroups.setChecked(profile.isNotifyOnGroups());
        if (switchFollows != null) switchFollows.setChecked(profile.isNotifyOnFollows());
        if (switchLikes != null) switchLikes.setChecked(profile.isNotifyOnLikes());
    }

    private void setupClickListeners() {
        if (rlDisplayName != null) rlDisplayName.setOnClickListener(v -> showEditDisplayNameDialog());
        if (rlEmail != null) rlEmail.setOnClickListener(v -> showEditEmailDialog());
        if (rlPassword != null) rlPassword.setOnClickListener(v -> showEditPasswordDialog());
        if (btnChangeAvatar != null) btnChangeAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        if (ivAvatar != null) ivAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
    }

    private void setupSwitchListeners() {
        android.widget.CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> saveSwitchSettings();
        if (switchMessages != null) switchMessages.setOnCheckedChangeListener(listener);
        if (switchGroups != null) switchGroups.setOnCheckedChangeListener(listener);
        if (switchFollows != null) switchFollows.setOnCheckedChangeListener(listener);
        if (switchLikes != null) switchLikes.setOnCheckedChangeListener(listener);
    }

    private void uploadAvatar(Uri uri) {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        try {
            InputStream is = getContentResolver().openInputStream(uri);
            byte[] bytes = is.readAllBytes();
            is.close();
            RequestBody requestBody = RequestBody.create(bytes, MediaType.parse("image/*"));
            MultipartBody.Part body = MultipartBody.Part.createFormData("images", "avatar.jpg", requestBody);
            List<MultipartBody.Part> list = new ArrayList<>();
            list.add(body);

            apiService.uploadImages(token, list).enqueue(new Callback<MediaUploadResponse>() {
                @Override
                public void onResponse(Call<MediaUploadResponse> call, Response<MediaUploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().getUrls().isEmpty()) {
                        updateSettings(new UpdateSettingsRequest().setAvatarUrl(response.body().getUrls().get(0)));
                    }
                }
                @Override public void onFailure(Call<MediaUploadResponse> call, Throwable t) {}
            });
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при чтении файла", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditDisplayNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Изменить имя");

        final EditText input = new EditText(this);
        input.setHint("Новое имя");
        input.setText(tvDisplayNameValue.getText().toString().equals("—") ? "" : tvDisplayNameValue.getText().toString());
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            updateSettings(new UpdateSettingsRequest().setDisplayName(newName));
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showEditEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Изменить почту");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etNewEmail = new EditText(this);
        etNewEmail.setHint("Новая почта");
        etNewEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(etNewEmail);

        final EditText etPassword = new EditText(this);
        etPassword.setHint("Текущий пароль");
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etPassword);

        builder.setView(layout);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newEmail = etNewEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Пароль обязателен", Toast.LENGTH_SHORT).show();
                return;
            }
            updateSettings(new UpdateSettingsRequest().setEmail(newEmail).setPassword(password));
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showEditPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Изменить пароль");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText etNewPass = new EditText(this);
        etNewPass.setHint("Новый пароль");
        etNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPass);

        final EditText etOldPass = new EditText(this);
        etOldPass.setHint("Текущий пароль");
        etOldPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etOldPass);

        builder.setView(layout);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newPass = etNewPass.getText().toString();
            String oldPass = etOldPass.getText().toString();
            if (oldPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }
            updateSettings(new UpdateSettingsRequest().setPassword(newPass));
        });
        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveSwitchSettings() {
        UpdateSettingsRequest request = new UpdateSettingsRequest()
                .setNotifyOnMessages(switchMessages.isChecked())
                .setNotifyOnGroups(switchGroups.isChecked())
                .setNotifyOnFollows(switchFollows.isChecked())
                .setNotifyOnLikes(switchLikes.isChecked());
        updateSettings(request);
    }

    private void updateSettings(UpdateSettingsRequest request) {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.updateSettings(token, request).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentProfile = response.body();
                    displayProfile(currentProfile);
                    Toast.makeText(SettingsActivity.this, "Сохранено", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                Toast.makeText(SettingsActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
