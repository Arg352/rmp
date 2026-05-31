package com.asylum.app.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.appcompat.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.asylum.app.CreatePostActivity;
import com.asylum.app.R;
import com.asylum.app.adapters.PostsAdapter;
import com.asylum.app.api.ApiService;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.models.Post;
import com.asylum.app.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostsFragment extends Fragment {

    public static final String ARG_FILTER_MODE = "filter_mode";
    public static final String FILTER_ALL = "Все пользователи";
    public static final String FILTER_FRIENDS = "Друзья";
    public static final String FILTER_ME = "Вы";

    private TextView tvFilterUser, tvSort;
    private View btnFilterUser, btnSort;
    private ImageView ivSortIcon;
    private EditText etSearch;
    private RecyclerView rvPosts;
    private PostsAdapter adapter;
    private List<Post> allPosts = new ArrayList<>();
    private FloatingActionButton fabAddPost;
    private SwipeRefreshLayout swipeRefresh;

    private ApiService apiService;
    private SessionManager sessionManager;

    private String currentFilter = FILTER_ALL;
    private String currentSort = "По времени";
    private boolean isSortDescending = true; 
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_posts, container, false);

        sessionManager = new SessionManager(requireContext());
        apiService = RetrofitClient.getInstance().getApiService();

        if (getArguments() != null && getArguments().containsKey(ARG_FILTER_MODE)) {
            currentFilter = getArguments().getString(ARG_FILTER_MODE);
        }

        initViews(view);
        setupMenus();
        setupSearch();
        loadFeed();

        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreatePostActivity.class);
            startActivity(intent);
        });

        return view;
    }

    /** Метод для внешнего обновления фильтра (например, из MainActivity) */
    public void setFilterMode(String filter) {
        this.currentFilter = filter;
        if (tvFilterUser != null) {
            tvFilterUser.setText(currentFilter);
        }
        applyFiltersAndSort();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFeed();
    }

    private void initViews(View view) {
        rvPosts = view.findViewById(R.id.rvPosts);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new PostsAdapter(new ArrayList<>(), this::onLikeClicked);
        rvPosts.setAdapter(adapter);

        tvFilterUser = view.findViewById(R.id.tvFilterUser);
        if (tvFilterUser != null) {
            tvFilterUser.setText(currentFilter);
        }
        tvSort = view.findViewById(R.id.tvSort);
        btnFilterUser = view.findViewById(R.id.btnFilterUser);
        btnSort = view.findViewById(R.id.btnSort);
        ivSortIcon = view.findViewById(R.id.ivSortIcon);
        etSearch = view.findViewById(R.id.etSearch);
        fabAddPost = view.findViewById(R.id.fabAddPost);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(this::loadFeed);
        }

        if (ivSortIcon != null) {
            ivSortIcon.setScaleY(isSortDescending ? 1.0f : -1.0f);
        }
    }

    @SuppressLint("RestrictedApi")
    private void setupMenus() {
        if (btnFilterUser != null) {
            btnFilterUser.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), v);
                popup.getMenu().add(0, 0, 0, FILTER_ALL).setIcon(R.drawable.ic_profile);
                popup.getMenu().add(0, 1, 1, FILTER_FRIENDS).setIcon(R.drawable.ic_profile_filled);
                popup.getMenu().add(0, 2, 2, FILTER_ME).setIcon(R.drawable.ic_profile_filled);
                
                popup.setForceShowIcon(true);

                popup.setOnMenuItemClickListener(item -> {
                    setFilterMode(item.getTitle().toString());
                    return true;
                });
                popup.show();
            });
        }

        if (btnSort != null) {
            btnSort.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(requireContext(), v);
                popup.getMenu().add(0, 0, 0, "По времени").setIcon(R.drawable.ic_sort_custom);
                popup.getMenu().add(0, 1, 1, "По кол-ву лайков").setIcon(R.drawable.ic_fire);
                
                popup.setForceShowIcon(true);

                popup.setOnMenuItemClickListener(item -> {
                    currentSort = item.getTitle().toString();
                    tvSort.setText(currentSort);
                    applyFiltersAndSort();
                    return true;
                });
                popup.show();
            });
        }

        if (ivSortIcon != null) {
            ivSortIcon.setOnClickListener(v -> {
                isSortDescending = !isSortDescending;
                ivSortIcon.animate().scaleY(isSortDescending ? 1.0f : -1.0f).setDuration(200).start();
                applyFiltersAndSort();
            });
        }
    }

    private void setupSearch() {
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    searchQuery = s.toString().toLowerCase().trim();
                    applyFiltersAndSort();
                }
            });
        }
    }

    private void loadFeed() {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.getFeed(token).enqueue(new Callback<List<Post>>() {
            @Override
            public void onResponse(@NonNull Call<List<Post>> call,
                                   @NonNull Response<List<Post>> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    allPosts = response.body();
                    applyFiltersAndSort();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Post>> call, @NonNull Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void applyFiltersAndSort() {
        List<Post> filteredPosts = new ArrayList<>();
        int myUserId = sessionManager.getUserId();

        for (Post post : allPosts) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    (post.getTitle() != null && post.getTitle().toLowerCase().contains(searchQuery)) ||
                    (post.getText() != null && post.getText().toLowerCase().contains(searchQuery)) ||
                    (post.getTags() != null && post.getTags().toLowerCase().contains(searchQuery)) ||
                    (post.getAuthorName() != null && post.getAuthorName().toLowerCase().contains(searchQuery));

            if (!matchesSearch) continue;

            boolean matchesFilter = false;
            if (currentFilter.equals(FILTER_ALL)) {
                matchesFilter = true;
            } else if (currentFilter.equals(FILTER_FRIENDS)) {
                matchesFilter = "FRIENDS".equalsIgnoreCase(post.getVisibility());
            } else if (currentFilter.equals(FILTER_ME)) {
                matchesFilter = (post.getUser() != null && post.getUser().getId() == myUserId);
            }

            if (matchesFilter) {
                filteredPosts.add(post);
            }
        }

        Collections.sort(filteredPosts, (p1, p2) -> {
            int result = 0;
            if (currentSort.equals("По времени")) {
                if (p1.getCreatedAt() == null || p2.getCreatedAt() == null) result = 0;
                else result = p1.getCreatedAt().compareTo(p2.getCreatedAt());
            } else if (currentSort.equals("По кол-ву лайков")) {
                result = Integer.compare(p1.getLikesCount(), p2.getLikesCount());
            }
            return isSortDescending ? -result : result;
        });

        if (adapter != null) {
            adapter.updatePosts(filteredPosts);
        }
    }

    private void onLikeClicked(Post post, int position) {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        post.toggleLike();
        if (adapter != null) adapter.notifyItemChanged(position);

        apiService.toggleLike(token, post.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {}
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                post.toggleLike();
                if (adapter != null) adapter.notifyItemChanged(position);
            }
        });
    }
}
