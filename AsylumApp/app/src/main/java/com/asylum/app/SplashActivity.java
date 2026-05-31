package com.asylum.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.asylum.app.api.SocketManager;
import com.asylum.app.utils.SessionManager;

/**
 * Splash Screen — показывается при запуске.
 * Если пользователь уже залогинен (есть токен) — переходим на MainActivity.
 * Иначе — на LoginActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SessionManager session = new SessionManager(this);

            Class<?> nextActivity;
            if (session.isLoggedIn()) {
                nextActivity = MainActivity.class;
                // Подключаем WebSocket при автологине
                SocketManager.getInstance().connect(session.getToken());
            } else {
                nextActivity = LoginActivity.class;
            }

            Intent intent = new Intent(SplashActivity.this, nextActivity);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
