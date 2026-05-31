package com.asylum.app.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asylum.app.PostActivity;
import com.asylum.app.R;
import com.asylum.app.models.Post;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    public interface OnLikeClickListener {
        void onLikeClick(Post post, int position);
    }

    private final List<Post> posts;
    private final OnLikeClickListener likeListener;

    public PostsAdapter(List<Post> posts, OnLikeClickListener likeListener) {
        this.posts = posts;
        this.likeListener = likeListener;
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

        // Автор
        String authorName = post.getAuthorName();
        holder.tvAuthorName.setText(authorName);
        holder.tvPostLetter.setText(authorName.isEmpty() ? "?" : authorName.substring(0, 1).toUpperCase());

        // Время
        String time = post.getCreatedAt();
        if (time != null && time.length() >= 16) {
            holder.tvTimeAgo.setText(time.substring(0, 16).replace("T", " "));
        } else {
            holder.tvTimeAgo.setText(time != null ? time : "");
        }

        // Контент
        holder.tvPostTitle.setText(post.getTitle());
        holder.tvPostContent.setText(post.getText());

        // Лайки
        holder.tvViews.setText(String.valueOf(post.getLikesCount()));
        if (holder.ivLike != null) {
            holder.ivLike.setAlpha(post.isLiked() ? 1.0f : 0.4f);
            holder.ivLike.setOnClickListener(v -> {
                if (likeListener != null) {
                    likeListener.onLikeClick(post, holder.getAdapterPosition());
                }
            });
        }

        // Теги
        if (holder.cgTags != null) {
            holder.cgTags.removeAllViews();
            String tags = post.getTags();
            if (tags != null && !tags.isEmpty()) {
                for (String tag : tags.split("[,\\s]+")) {
                    if (!tag.isEmpty()) {
                        Chip chip = new Chip(holder.itemView.getContext());
                        chip.setText(tag.startsWith("#") ? tag : "#" + tag);
                        chip.setChipBackgroundColorResource(android.R.color.transparent);
                        chip.setChipStrokeWidth(0);
                        chip.setTextSize(10);
                        chip.setClickable(false);
                        holder.cgTags.addView(chip);
                    }
                }
            }
        }

        // Клик по всему посту
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
        }
    }
}
