package com.asylum.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.asylum.app.api.ApiService;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.models.Post;
import com.asylum.app.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Экран отдельного поста.
 */
public class PostActivity extends AppCompatActivity {

    public static Post currentPost;

    private SessionManager sessionManager;
    private ApiService apiService;

    private final ActivityResultLauncher<Intent> selectContactLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    int userId = result.getData().getIntExtra("SELECTED_USER_ID", -1);
                    if (userId != -1) {
                        sendPostToChat(userId);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getInstance().getApiService();

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                Intent intent = new Intent(this, SelectContactActivity.class);
                intent.putExtra("MODE_SELECT", true);
                selectContactLauncher.launch(intent);
            });
        }

        if (currentPost != null) {
            displayPost(currentPost);
            setupDeleteButton(currentPost);
        }
    }

    private void setupDeleteButton(Post post) {
        ImageView btnDelete = findViewById(R.id.btnDeletePost);
        if (btnDelete == null) return;

        int myUserId = sessionManager.getUserId();
        // Показываем кнопку удаления только автору поста
        if (post.getUser() != null && post.getUser().getId() == myUserId) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> confirmDelete(post));
        } else {
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void confirmDelete(Post post) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить запись?")
                .setMessage("Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (d, w) -> deletePost(post))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deletePost(Post post) {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        apiService.deletePost(token, post.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() || response.code() == 204) {
                    Toast.makeText(PostActivity.this, "Запись удалена", Toast.LENGTH_SHORT).show();
                    // Возвращаемся и сигнализируем ленте обновить список
                    Intent result = new Intent();
                    result.putExtra("POST_DELETED", true);
                    result.putExtra("POST_ID", post.getId());
                    setResult(RESULT_OK, result);
                    finish();
                } else {
                    Toast.makeText(PostActivity.this, "Ошибка удаления: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(PostActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayPost(Post post) {
        TextView tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        if (tvToolbarTitle != null) tvToolbarTitle.setText(post.getTitle());

        TextView tvLikesCount = findViewById(R.id.tvLikesCount);
        if (tvLikesCount != null) tvLikesCount.setText(String.valueOf(post.getLikesCount()));

        TextView tvAuthorName = findViewById(R.id.tvAuthorName);
        TextView tvAuthorHandle = findViewById(R.id.tvAuthorHandle);
        TextView tvPostDate = findViewById(R.id.tvPostDate);
        if (tvAuthorName != null) tvAuthorName.setText(post.getAuthorName());
        if (tvAuthorHandle != null) {
            tvAuthorHandle.setText(post.getUser() != null ? "@" + post.getUser().getUsername() : "");
        }
        if (tvPostDate != null) {
            String createdAt = post.getCreatedAt();
            if (createdAt != null && createdAt.length() >= 16) {
                tvPostDate.setText(createdAt.substring(0, 16).replace("T", " "));
            }
        }

        TextView tvPostTitle = findViewById(R.id.tvPostTitle);
        if (tvPostTitle != null) tvPostTitle.setText(post.getTitle());

        TextView tvPostContent = findViewById(R.id.tvPostContent);
        if (tvPostContent != null) tvPostContent.setText(post.getText());

        ChipGroup cgTags = findViewById(R.id.cgTags);
        if (cgTags != null) {
            cgTags.removeAllViews();
            String tagsString = post.getTags();
            if (tagsString != null && !tagsString.isEmpty()) {
                for (String tag : tagsString.split("[,\\s]+")) {
                    if (!tag.isEmpty()) {
                        String cleanTag = tag.startsWith("#") ? tag.substring(1) : tag;
                        Chip chip = new Chip(this);
                        chip.setText("#" + cleanTag);
                        chip.setChipBackgroundColorResource(android.R.color.transparent);
                        chip.setChipStrokeColorResource(R.color.asylum_red);
                        chip.setChipStrokeWidth(2f);
                        chip.setTextColor(getResources().getColor(R.color.asylum_red));
                        chip.setTextSize(12);
                        chip.setClickable(true);
                        chip.setOnClickListener(v -> {
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.putExtra("FILTER_TAG", cleanTag);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                        });
                        cgTags.addView(chip);
                    }
                }
            }
        }

        FloatingActionButton fabUpvote = findViewById(R.id.fabUpvote);
        if (fabUpvote != null) {
            updateLikeButton(fabUpvote, post.isLiked());
            fabUpvote.setOnClickListener(v -> toggleLike(post, fabUpvote, tvLikesCount));
        }

        if (!post.hasImages()) {
            View imgCard1 = findViewById(R.id.cardImage1);
            View imgCard2 = findViewById(R.id.cardImage2);
            if (imgCard1 != null) imgCard1.setVisibility(View.GONE);
            if (imgCard2 != null) imgCard2.setVisibility(View.GONE);
        }
    }

    private void sendPostToChat(int userId) {
        if (currentPost == null) return;
        String shareText = "Посмотри этот пост: " + currentPost.getTitle()
                + "\n\n" + currentPost.getText()
                + "\n\nСсылка: asylum://post/" + currentPost.getId();

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("CHAT_USER_ID", userId);
        intent.putExtra("PREFILL_MESSAGE", shareText);
        startActivity(intent);
        Toast.makeText(this, "Пост готов к отправке в чате", Toast.LENGTH_SHORT).show();
    }

    private void toggleLike(Post post, FloatingActionButton fab, TextView tvCount) {
        String token = sessionManager.getBearerToken();
        if (token == null) return;

        post.toggleLike();
        updateLikeButton(fab, post.isLiked());
        if (tvCount != null) tvCount.setText(String.valueOf(post.getLikesCount()));

        apiService.toggleLike(token, post.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) rollbackLike(post, fab, tvCount);
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                rollbackLike(post, fab, tvCount);
            }
        });
    }

    private void rollbackLike(Post post, FloatingActionButton fab, TextView tvCount) {
        post.toggleLike();
        updateLikeButton(fab, post.isLiked());
        if (tvCount != null) tvCount.setText(String.valueOf(post.getLikesCount()));
        Toast.makeText(this, "Ошибка сети", Toast.LENGTH_SHORT).show();
    }

    private void updateLikeButton(FloatingActionButton fab, boolean liked) {
        if (liked) {
            fab.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            fab.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.asylum_red)));
        } else {
            fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.asylum_red)));
            fab.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        }
    }
}
