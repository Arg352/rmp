package com.asylum.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.asylum.app.api.ApiService;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.api.SocketManager;
import com.asylum.app.models.AuthResponse;
import com.asylum.app.models.RegisterRequest;
import com.asylum.app.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран регистрации нового пользователя.
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLoginLink, tvError;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance().getApiService();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        tilUsername = findViewById(R.id.tilUsername);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> {
            if (validateFields()) {
                performRegister();
            }
        });

        tvLoginLink.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private boolean validateFields() {
        String username = getUsernameText();
        String email = getEmailText();
        String password = getPasswordText();
        String confirmPassword = getConfirmPasswordText();

        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        hideError();

        if (TextUtils.isEmpty(username)) {
            tilUsername.setError("Введите имя пользователя");
            return false;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            return false;
        }
        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_short));
            return false;
        }
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_not_match));
            return false;
        }
        return true;
    }

    private void performRegister() {
        setLoading(true);
        RegisterRequest request = new RegisterRequest(getUsernameText(), getEmailText(), getPasswordText(), getConfirmPasswordText());

        apiService.register(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse auth = response.body();
                    sessionManager.saveToken(auth.getAccessToken());
                    if (auth.getUser() != null) {
                        sessionManager.saveUser(auth.getUser().getId(), auth.getUser().getUsername());
                    }
                    SocketManager.getInstance().connect(auth.getAccessToken());
                    navigateToMain();
                } else {
                    showError(parseErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                showError(getString(R.string.error_network));
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private String getUsernameText() { return etUsername.getText().toString().trim(); }
    private String getEmailText() { return etEmail.getText().toString().trim(); }
    private String getPasswordText() { return etPassword.getText().toString(); }
    private String getConfirmPasswordText() { return etConfirmPassword.getText().toString(); }

    private void setLoading(boolean loading) {
        btnRegister.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setText(loading ? "" : getString(R.string.btn_register));
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() { tvError.setVisibility(View.GONE); }

    private String parseErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                JSONObject obj = new JSONObject(errorJson);
                if (obj.has("message")) return obj.getString("message");
            }
        } catch (Exception ignored) {}
        return "Ошибка регистрации: " + response.code();
    }
}
