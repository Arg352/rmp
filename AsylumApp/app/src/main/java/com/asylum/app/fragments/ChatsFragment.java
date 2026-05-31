package com.asylum.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.asylum.app.R;
import com.asylum.app.SelectContactActivity;
import com.asylum.app.adapters.ChatsAdapter;
import com.asylum.app.api.ApiService;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.models.Chat;
import com.asylum.app.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatsFragment extends Fragment {

    private RecyclerView rvChats;
    private SwipeRefreshLayout swipeRefresh;
    private ChatsAdapter adapter;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        sessionManager = new SessionManager(requireContext());
        apiService = RetrofitClient.getInstance().getApiService();

        rvChats = view.findViewById(R.id.rvChats);
        rvChats.setLayoutManager(new LinearLayoutManager(getContext()));

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadChats);
        }

        FloatingActionButton fabNewChat = view.findViewById(R.id.fabNewChat);
        fabNewChat.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SelectContactActivity.class);
            startActivity(intent);
        });

        loadChats();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadChats();
    }

    private void loadChats() {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.getChats(token).enqueue(new Callback<List<Chat>>() {
            @Override
            public void onResponse(@NonNull Call<List<Chat>> call,
                                   @NonNull Response<List<Chat>> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Chat> chats = response.body();
                    adapter = new ChatsAdapter(chats, sessionManager.getUserId());
                    rvChats.setAdapter(adapter);
                } else {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Ошибка загрузки чатов", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Chat>> call, @NonNull Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (getContext() != null)
                    Toast.makeText(getContext(), "Нет подключения к серверу", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
