package com.asylum.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asylum.app.R;
import com.asylum.app.models.ImageAttachment;
import com.asylum.app.models.Message;
import com.bumptech.glide.Glide;

import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_INCOMING = 1;
    private static final int TYPE_OUTGOING = 2;

    private final List<Message> messages;
    private final int myUserId;

    public MessagesAdapter(List<Message> messages, int myUserId) {
        this.messages = messages;
        this.myUserId = myUserId;
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isOutgoing(myUserId) ? TYPE_OUTGOING : TYPE_INCOMING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_OUTGOING) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_outgoing, parent, false);
            return new MessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_incoming, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        MessageViewHolder vh = (MessageViewHolder) holder;
        
        if (message.getText() == null || message.getText().isEmpty()) {
            vh.tvText.setVisibility(View.GONE);
        } else {
            vh.tvText.setVisibility(View.VISIBLE);
            vh.tvText.setText(message.getText());
        }
        
        vh.tvTime.setText(message.getFormattedTime());

        // Отображение вложений
        vh.imagesContainer.removeAllViews();
        List<ImageAttachment> attachments = message.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            vh.imagesContainer.setVisibility(View.VISIBLE);
            for (ImageAttachment attach : attachments) {
                ImageView iv = new ImageView(vh.itemView.getContext());
                int height = (int) (200 * vh.itemView.getContext().getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, height);
                params.bottomMargin = (int) (4 * vh.itemView.getContext().getResources().getDisplayMetrics().density);
                iv.setLayoutParams(params);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                
                Glide.with(vh.itemView.getContext())
                        .load(attach.getUrl())
                        .into(iv);
                
                vh.imagesContainer.addView(iv);
            }
        } else {
            vh.imagesContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvTime;
        LinearLayout imagesContainer;

        MessageViewHolder(View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
            imagesContainer = itemView.findViewById(R.id.imagesContainer);
        }
    }
}
