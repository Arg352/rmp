package com.asylum.app.models;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("login")
    private String login;

    @SerializedName("password")
    private String password;

    public LoginRequest(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() { return login; }
    public String getPassword() { return password; }
}
