package com.asylum.app.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asylum.app.PostActivity;
import com.asylum.app.R;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.models.ImageAttachment;
import com.asylum.app.models.Post;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    public interface OnPostInteractionListener {
        void onLikeClick(Post post, int position);
        void onTagClick(String tag);
    }

    private final List<Post> posts;
    private final OnPostInteractionListener listener;

    public PostsAdapter(List<Post> posts, OnPostInteractionListener listener) {
        this.posts = posts;
        this.listener = listener;
    }

    public void updatePosts(List<Post> newPosts) {
        this.posts.clear();
        this.posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);

        String authorName = post.getAuthorName();
        holder.tvAuthorName.setText(authorName);
        holder.tvPostLetter.setText(authorName.isEmpty() ? "?" : authorName.substring(0, 1).toUpperCase());

        // Загрузка аватарки пользователя
        String avatarUrl = (post.getUser() != null) ? post.getUser().getAvatarUrl() : null;
        if (avatarUrl != null && !avatarUrl.isEmpty() && holder.ivUserAvatar != null) {
            holder.ivUserAvatar.setVisibility(android.view.View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .into(holder.ivUserAvatar);
        } else if (holder.ivUserAvatar != null) {
            holder.ivUserAvatar.setVisibility(android.view.View.GONE);
        }

        String time = post.getCreatedAt();
        if (time != null && time.length() >= 16) {
            holder.tvTimeAgo.setText(time.substring(0, 16).replace("T", " "));
        } else {
            holder.tvTimeAgo.setText(time != null ? time : "");
        }

        holder.tvPostTitle.setText(post.getTitle());
        holder.tvPostContent.setText(post.getText());
        holder.tvViews.setText(String.valueOf(post.getLikesCount()));

        if (holder.ivLike != null) {
            holder.ivLike.setAlpha(post.isLiked() ? 1.0f : 0.4f);
            holder.ivLike.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(post, holder.getAdapterPosition());
                }
            });
        }

        // Отображение изображений поста
        holder.imagesContainer.removeAllViews();
        if (post.hasImages()) {
            holder.imagesContainer.setVisibility(View.VISIBLE);
            for (ImageAttachment image : post.getImages()) {
                ImageView iv = new ImageView(holder.itemView.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        (int) (200 * holder.itemView.getContext().getResources().getDisplayMetrics().density));
                params.setMargins(0, 0, 0, (int) (8 * holder.itemView.getContext().getResources().getDisplayMetrics().density));
                iv.setLayoutParams(params);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                
                String url = image.getUrl();
                if (url != null && url.startsWith("/")) {
                    url = RetrofitClient.BASE_URL + url.substring(1);
                }
                
                Glide.with(holder.itemView.getContext()).load(url).into(iv);
                holder.imagesContainer.addView(iv);
            }
        } else {
            holder.imagesContainer.setVisibility(View.GONE);
        }

        if (holder.cgTags != null) {
            holder.cgTags.removeAllViews();
            String tagsString = post.getTags();
            if (tagsString != null && !tagsString.isEmpty()) {
                for (String tag : tagsString.split("[,\\s]+")) {
                    if (!tag.isEmpty()) {
                        String cleanTag = tag.startsWith("#") ? tag.substring(1) : tag;
                        Chip chip = new Chip(holder.itemView.getContext());
                        chip.setText("#" + cleanTag);
                        chip.setChipBackgroundColorResource(android.R.color.transparent);
                        chip.setChipStrokeColorResource(R.color.asylum_red);
                        chip.setChipStrokeWidth(2f);
                        chip.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.asylum_red));
                        chip.setTextSize(12);
                        chip.setClickable(true);
                        chip.setOnClickListener(v -> {
                            if (listener != null) listener.onTagClick(cleanTag);
                        });
                        holder.cgTags.addView(chip);
                    }
                }
            }
        }

        holder.itemView.setOnClickListener(v -> {
            PostActivity.currentPost = post;
            Intent intent = new Intent(v.getContext(), PostActivity.class);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthorName, tvPostLetter, tvTimeAgo, tvPostTitle, tvPostContent, tvViews;
        ChipGroup cgTags;
        ImageView ivLike;
        ImageView ivUserAvatar;
        LinearLayout imagesContainer;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvPostLetter = itemView.findViewById(R.id.tvPostLetter);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
            tvPostTitle = itemView.findViewById(R.id.tvPostTitle);
            tvPostContent = itemView.findViewById(R.id.tvPostContent);
            tvViews = itemView.findViewById(R.id.tvViews);
            cgTags = itemView.findViewById(R.id.cgTags);
            ivLike = itemView.findViewById(R.id.ivLike);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            imagesContainer = itemView.findViewById(R.id.imagesContainer);
        }
    }
}
