package com.asylum.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asylum.app.R;
import com.asylum.app.models.Message;

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

    /** Добавить новое сообщение в конец (для WebSocket) */
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
        vh.tvText.setText(message.getText());
        vh.tvTime.setText(message.getFormattedTime());
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvTime;

        MessageViewHolder(View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
        }
    }
}
