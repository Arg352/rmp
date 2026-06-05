package com.asylum.app.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asylum.app.ChatActivity;
import com.asylum.app.R;
import com.asylum.app.models.Chat;
import com.bumptech.glide.Glide;

import java.util.List;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private final List<Chat> chats;
    private final int myUserId;

    public ChatsAdapter(List<Chat> chats, int myUserId) {
        this.chats = chats;
        this.myUserId = myUserId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chats.get(position);

        holder.tvParticipantName.setText(chat.getParticipantName());
        holder.tvLastMessage.setText(chat.getLastMessageText());
        holder.tvTime.setText(chat.getLastMessageTime());
        holder.tvAvatarLetter.setText(chat.getAvatarLetter());

        // Загрузка аватарки собеседника
        String avatarUrl = (chat.getUser() != null) ? chat.getUser().getAvatarUrl() : null;
        if (avatarUrl != null && !avatarUrl.isEmpty() && holder.ivUserAvatar != null) {
            holder.ivUserAvatar.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .into(holder.ivUserAvatar);
        } else if (holder.ivUserAvatar != null) {
            holder.ivUserAvatar.setVisibility(View.GONE);
        }

        // Иконка заглушенного чата справа от ника
        if (holder.ivMuteStatus != null) {
            holder.ivMuteStatus.setVisibility(chat.isMuted() ? View.VISIBLE : View.GONE);
        }

        int unread = chat.getUnreadCount();
        if (unread > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(unread));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChatActivity.class);
            if (chat.getUser() != null) {
                intent.putExtra("CHAT_USER_ID", chat.getUser().getId());
                intent.putExtra("CHAT_NAME", chat.getParticipantName());
                intent.putExtra("CHAT_USERNAME", "@" + chat.getUser().getUsername());
                intent.putExtra("IS_MUTED", chat.isMuted());
            }
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvParticipantName, tvLastMessage, tvTime, tvAvatarLetter, tvUnreadCount;
        ImageView ivMuteStatus;
        ImageView ivUserAvatar;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvParticipantName = itemView.findViewById(R.id.tvParticipantName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvAvatarLetter = itemView.findViewById(R.id.tvAvatarLetter);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            ivMuteStatus = itemView.findViewById(R.id.ivMuteStatus);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
        }
    }
}
