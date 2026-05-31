package com.asylum.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asylum.app.adapters.ContactsAdapter;
import com.asylum.app.api.ApiService;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.models.User;
import com.asylum.app.models.UserProfile;
import com.asylum.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private RecyclerView rvResults;
    private ContactsAdapter adapter;
    private SessionManager sessionManager;
    private ApiService apiService;
    private final List<User> resultUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance().getApiService();

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvResults = findViewById(R.id.rvResults);
        rvResults.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ContactsAdapter(resultUsers);
        rvResults.setAdapter(adapter);

        EditText etSearch = findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();
                    if (query.length() >= 2) {
                        searchUsers(query);
                    } else {
                        resultUsers.clear();
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    private void searchUsers(String query) {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.searchUsers(token, query).enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(Call<List<UserProfile>> call, Response<List<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultUsers.clear();
                    for (UserProfile p : response.body()) {
                        String name = p.getDisplayName() != null && !p.getDisplayName().isEmpty()
                                ? p.getDisplayName() : p.getUsername();
                        resultUsers.add(new User(String.valueOf(p.getId()), p.getUsername(), name, p.getAvatarUrl()));
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<UserProfile>> call, Throwable t) {
                Toast.makeText(SearchActivity.this, "Ошибка поиска", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
