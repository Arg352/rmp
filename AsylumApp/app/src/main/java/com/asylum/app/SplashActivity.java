package com.asylum.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.asylum.app.api.SocketManager;
import com.asylum.app.utils.SessionManager;

/**
 * Splash Screen — показывается при запуске.
 * Применяет сохранённую тему, затем переходит на MainActivity или LoginActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем сохранённую тему ДО setContentView
        SessionManager session = new SessionManager(this);
        AppCompatDelegate.setDefaultNightMode(
                session.isDarkMode()
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Class<?> nextActivity;
            if (session.isLoggedIn()) {
                nextActivity = MainActivity.class;
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
