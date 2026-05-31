package com.asylum.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.asylum.app.MainActivity;
import com.asylum.app.R;
import com.asylum.app.SettingsActivity;
import com.asylum.app.adapters.SearchUserAdapter;
import com.asylum.app.api.ApiService;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.models.User;
import com.asylum.app.models.UserProfile;
import com.asylum.app.utils.SessionManager;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvFullName, tvUsername, tvAvatarLetter, tvFriendsCount;
    private CircleImageView ivAvatar;

    private SessionManager sessionManager;
    private ApiService apiService;
    private UserProfile currentProfile;

    private List<UserProfile> cachedFollowing = new ArrayList<>();
    private List<UserProfile> cachedFollowers = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = new SessionManager(requireContext());
        apiService = RetrofitClient.getInstance().getApiService();

        tvFullName = view.findViewById(R.id.tvFullName);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvAvatarLetter = view.findViewById(R.id.tvAvatarLetter);
        tvFriendsCount = view.findViewById(R.id.tvFriendsCount);
        ivAvatar = view.findViewById(R.id.ivAvatar);

        ImageView btnLogout = view.findViewById(R.id.btnLogout);
        View btnFriends = view.findViewById(R.id.btnFriends);
        View btnMyPosts = view.findViewById(R.id.btnMyPosts);
        View btnSettingsBottom = view.findViewById(R.id.btnSettingsBottom);

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).logout();
                }
            });
        }

        if (btnSettingsBottom != null) {
            btnSettingsBottom.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            });
        }

        if (btnFriends != null) {
            btnFriends.setOnClickListener(v -> showFriendsDialog());
        }

        if (btnMyPosts != null) {
            btnMyPosts.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    Bundle args = new Bundle();
                    args.putString(PostsFragment.ARG_FILTER_MODE, PostsFragment.FILTER_ME);
                    ((MainActivity) getActivity()).navigateToTab(R.id.navigation_posts, args);
                }
            });
        }

        loadProfile();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentProfile != null) {
            loadProfile();
        }
    }

    private void loadProfile() {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.getMyProfile(token).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(@NonNull Call<UserProfile> call,
                                   @NonNull Response<UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentProfile = response.body();
                    sessionManager.saveUser(currentProfile.getId(), currentProfile.getUsername());
                    displayProfile(currentProfile);
                    loadFriendsCount(token);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfile> call, @NonNull Throwable t) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Не удалось загрузить профиль", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFriendsCount(String token) {
        apiService.getFollowing(token).enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserProfile>> call,
                                   @NonNull Response<List<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null && tvFriendsCount != null) {
                    int count = response.body().size();
                    tvFriendsCount.setText(count + " друзей");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserProfile>> call, @NonNull Throwable t) {}
        });
    }

    private void displayProfile(UserProfile profile) {
        if (getContext() == null) return;

        String displayName = (profile.getDisplayName() != null && !profile.getDisplayName().isEmpty())
                ? profile.getDisplayName()
                : profile.getUsername();

        if (tvFullName != null) tvFullName.setText(displayName);
        if (tvUsername != null) tvUsername.setText("@" + profile.getUsername());

        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
            if (ivAvatar != null) {
                ivAvatar.setVisibility(View.VISIBLE);
                if (tvAvatarLetter != null) tvAvatarLetter.setVisibility(View.INVISIBLE);
                Glide.with(this).load(profile.getAvatarUrl()).into(ivAvatar);
            }
        } else {
            if (tvAvatarLetter != null) {
                tvAvatarLetter.setVisibility(View.VISIBLE);
                tvAvatarLetter.setText(displayName.substring(0, 1).toUpperCase());
            }
        }
    }

    private void showFriendsDialog() {
        if (getContext() == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_add_friend, null);
        dialog.setContentView(view);

        EditText etSearch = view.findViewById(R.id.etSearch);
        RecyclerView rvSuggestions = view.findViewById(R.id.rvSuggestions);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(getContext()));

        // Мы будем использовать финальную ссылку на адаптер внутри listener
        final SearchUserAdapter[] adapterContainer = new SearchUserAdapter[1];
        
        adapterContainer[0] = new SearchUserAdapter(new ArrayList<>(), () -> {
            loadFriendsCount(sessionManager.getBearerToken());
            // Обновляем данные и список
            loadFriendsAndRequests(adapterContainer[0]);
        });
        
        rvSuggestions.setAdapter(adapterContainer[0]);
        loadFriendsAndRequests(adapterContainer[0]);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    loadFriendsAndRequests(adapterContainer[0]);
                    return;
                }
                if (query.length() < 2) return;

                String token = sessionManager.getBearerToken();
                if (token == null) return;

                apiService.searchUsers(token, query).enqueue(new Callback<List<UserProfile>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<UserProfile>> call,
                                           @NonNull Response<List<UserProfile>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            adapterContainer[0].updateList(mergeWithCache(response.body()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<UserProfile>> call, @NonNull Throwable t) {}
                });
            }
        });

        dialog.show();
    }

    private void loadFriendsAndRequests(SearchUserAdapter adapter) {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.getFollowing(token).enqueue(new Callback<List<UserProfile>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserProfile>> call, @NonNull Response<List<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cachedFollowing = response.body();
                    
                    apiService.getFollowers(token).enqueue(new Callback<List<UserProfile>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<UserProfile>> call, @NonNull Response<List<UserProfile>> response2) {
                            if (response2.isSuccessful() && response2.body() != null) {
                                cachedFollowers = response2.body();
                                List<User> merged = mergeFriends(cachedFollowing, cachedFollowers);
                                if (adapter != null) {
                                    adapter.updateList(merged);
                                }
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<List<UserProfile>> call, @NonNull Throwable t) {}
                    });
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<UserProfile>> call, @NonNull Throwable t) {}
        });
    }

    private List<User> mergeFriends(List<UserProfile> following, List<UserProfile> followers) {
        Map<Integer, User> userMap = new HashMap<>();

        for (UserProfile p : following) {
            String name = (p.getDisplayName() != null && !p.getDisplayName().isEmpty()) ? p.getDisplayName() : p.getUsername();
            User u = new User(String.valueOf(p.getId()), p.getUsername(), name, p.getAvatarUrl(), "SENT");
            userMap.put(p.getId(), u);
        }

        for (UserProfile p : followers) {
            if (userMap.containsKey(p.getId())) {
                userMap.get(p.getId()).setStatus("FRIEND");
            } else {
                String name = (p.getDisplayName() != null && !p.getDisplayName().isEmpty()) ? p.getDisplayName() : p.getUsername();
                User u = new User(String.valueOf(p.getId()), p.getUsername(), name, p.getAvatarUrl(), "RECEIVED");
                userMap.put(p.getId(), u);
            }
        }

        List<User> result = new ArrayList<>(userMap.values());
        Collections.sort(result, (u1, u2) -> Integer.compare(getStatusPriority(u1.getStatus()), getStatusPriority(u2.getStatus())));
        return result;
    }

    private List<User> mergeWithCache(List<UserProfile> searchResults) {
        Map<Integer, String> statusMap = new HashMap<>();
        for (UserProfile p : cachedFollowing) statusMap.put(p.getId(), "SENT");
        for (UserProfile p : cachedFollowers) {
            if (statusMap.containsKey(p.getId())) statusMap.put(p.getId(), "FRIEND");
            else statusMap.put(p.getId(), "RECEIVED");
        }

        List<User> result = new ArrayList<>();
        for (UserProfile p : searchResults) {
            String name = (p.getDisplayName() != null && !p.getDisplayName().isEmpty()) ? p.getDisplayName() : p.getUsername();
            String status = statusMap.getOrDefault(p.getId(), "NONE");
            result.add(new User(String.valueOf(p.getId()), p.getUsername(), name, p.getAvatarUrl(), status));
        }
        return result;
    }

    private int getStatusPriority(String status) {
        switch (status) {
            case "RECEIVED": return 1;
            case "FRIEND": return 2;
            case "SENT": return 3;
            default: return 4;
        }
    }
}
