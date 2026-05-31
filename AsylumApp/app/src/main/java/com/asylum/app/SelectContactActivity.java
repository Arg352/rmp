package com.asylum.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asylum.app.adapters.ContactsAdapter;
import com.asylum.app.api.ApiService;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.models.ApiUser;
import com.asylum.app.models.Chat;
import com.asylum.app.models.User;
import com.asylum.app.models.UserProfile;
import com.asylum.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectContactActivity extends AppCompatActivity {

    private RecyclerView rvContacts;
    private ContactsAdapter adapter;
    private final List<User> contacts = new ArrayList<>();
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contact);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance().getApiService();

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        boolean isSelectionMode = getIntent().getBooleanExtra("MODE_SELECT", false);

        rvContacts = findViewById(R.id.rvContacts);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ContactsAdapter(contacts, isSelectionMode);
        rvContacts.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
                @Override public void afterTextChanged(Editable s) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();
                    if (!query.isEmpty()) {
                        searchUsers(query);
                    } else {
                        loadInitialData();
                    }
                }
            });
        }

        loadInitialData();
    }

    private void loadInitialData() {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.getChats(token).enqueue(new Callback<List<Chat>>() {
            @Override
            public void onResponse(@NonNull Call<List<Chat>> call, @NonNull Response<List<Chat>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    contacts.clear();
                    for (Chat chat : response.body()) {
                        ApiUser u = chat.getUser();
                        if (u != null) {
                            String name = u.getDisplayOrUsername();
                            contacts.add(new User(String.valueOf(u.getId()), u.getUsername(), name, u.getAvatarUrl()));
                        }
                    }
                    if (contacts.isEmpty()) {
                        loadFollowing();
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    loadFollowing();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Chat>> call, @NonNull Throwable t) {
                loadFollowing();
            }
        });
    }

    private void loadFollowing() {
        String token = sessionManager.getBearerToken();
        if (token == null) return;
        apiService.getFollowing(token).enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserProfile>> call, @NonNull Response<List<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    contacts.clear();
                    for (UserProfile p : response.body()) {
                        String name = (p.getDisplayName() != null && !p.getDisplayName().isEmpty())
                                ? p.getDisplayName() : p.getUsername();
                        contacts.add(new User(String.valueOf(p.getId()), p.getUsername(), name, p.getAvatarUrl()));
                    }
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<UserProfile>> call, @NonNull Throwable t) {
                Toast.makeText(SelectContactActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchUsers(String query) {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.searchUsers(token, query).enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserProfile>> call, @NonNull Response<List<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    contacts.clear();
                    for (UserProfile p : response.body()) {
                        String name = (p.getDisplayName() != null && !p.getDisplayName().isEmpty())
                                ? p.getDisplayName() : p.getUsername();
                        contacts.add(new User(String.valueOf(p.getId()), p.getUsername(), name, p.getAvatarUrl()));
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserProfile>> call, @NonNull Throwable t) {
                Toast.makeText(SelectContactActivity.this, "Ошибка поиска", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
