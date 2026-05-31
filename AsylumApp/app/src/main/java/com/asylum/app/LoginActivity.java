package com.asylum.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.asylum.app.api.ApiService;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.api.SocketManager;
import com.asylum.app.models.AuthResponse;
import com.asylum.app.models.LoginRequest;
import com.asylum.app.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilLogin, tilPassword;
    private TextInputEditText etLogin, etPassword;
    private Button btnLogin, btnGuestLogin;
    private TextView tvRegisterLink, tvError;
    private ProgressBar progressBar;

    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance().getApiService();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        tilLogin = findViewById(R.id.tilLogin);
        tilPassword = findViewById(R.id.tilPassword);
        etLogin = findViewById(R.id.etLogin);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGuestLogin = findViewById(R.id.btnGuestLogin);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvError = findViewById(R.id.tvError);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            if (validateFields()) {
                performLogin();
            }
        });

        // Кнопка быстрого входа с тестовыми данными
        if (btnGuestLogin != null) {
            btnGuestLogin.setText("Заполнить тест (test / test123)");
            btnGuestLogin.setOnClickListener(v -> {
                etLogin.setText("test");
                etPassword.setText("test123");
                performLogin();
            });
        }

        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private boolean validateFields() {
        String login = getLoginText();
        String password = getPasswordText();

        if (TextUtils.isEmpty(login)) {
            showError("Введите логин");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            showError("Введите пароль");
            return false;
        }
        hideError();
        return true;
    }

    private void performLogin() {
        String login = getLoginText();
        String password = getPasswordText();

        setLoading(true);
        LoginRequest request = new LoginRequest(login, password);

        apiService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getAccessToken();
                    sessionManager.saveToken(token);
                    // Подключаем WebSocket сразу после логина
                    SocketManager.getInstance().connect(token);
                    navigateToMain();
                } else {
                    showError(parseErrorMessage(response));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                showError("Ошибка сети или сервер не запущен");
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private String getLoginText() { return etLogin.getText() != null ? etLogin.getText().toString().trim() : ""; }
    private String getPasswordText() { return etPassword.getText() != null ? etPassword.getText().toString() : ""; }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setText(loading ? "" : "Войти");
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() { tvError.setVisibility(View.GONE); }

    private String parseErrorMessage(Response<?> response) {
        if (response.code() == 500) return "Внутренняя ошибка сервера (500)";
        if (response.code() == 401) return "Неверный логин или пароль";
        return "Ошибка: " + response.code();
    }
}
