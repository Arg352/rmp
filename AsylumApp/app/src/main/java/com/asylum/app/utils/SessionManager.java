package com.asylum.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Хранит JWT токен и базовые данные пользователя в SharedPreferences
 */
public class SessionManager {
    private static final String PREF_NAME = "asylum_session";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_DARK_MODE = "dark_mode";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /** Сохранить токен после входа/регистрации */
    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    /** Получить сохранённый токен */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /** Получить токен в формате "Bearer <token>" для заголовка Authorization */
    public String getBearerToken() {
        String token = getToken();
        if (token == null) return null;
        return "Bearer " + token;
    }

    /** Сохранить данные пользователя */
    public void saveUser(int userId, String username) {
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    /** Получить username */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    /** Получить ID пользователя */
    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    /** Проверить, залогинен ли пользователь */
    public boolean isLoggedIn() {
        return getToken() != null;
    }

    /** Выйти из аккаунта (очистить все данные) */
    public void logout() {
        editor.clear();
        editor.apply();
    }

    /** Сохранить настройку тёмной темы */
    public void setDarkMode(boolean enabled) {
        editor.putBoolean(KEY_DARK_MODE, enabled);
        editor.apply();
    }

    /** Получить текущую настройку тёмной темы */
    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }
}
