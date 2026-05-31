package com.asylum.app.adapters;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asylum.app.ChatActivity;
import com.asylum.app.R;
import com.asylum.app.models.User;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private final List<User> users;
    private final boolean selectionMode;

    public ContactsAdapter(List<User> users) {
        this(users, false);
    }

    public ContactsAdapter(List<User> users, boolean selectionMode) {
        this.users = users;
        this.selectionMode = selectionMode;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvFullName.setText(user.getFullName());
        holder.tvAvatarLetter.setText(
                user.getFullName().isEmpty() ? "?" : user.getFullName().substring(0, 1).toUpperCase()
        );
        holder.tvStatus.setText("@" + user.getUsername());

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                Intent data = new Intent();
                try {
                    data.putExtra("SELECTED_USER_ID", Integer.parseInt(user.getId()));
                    data.putExtra("SELECTED_USER_NAME", user.getFullName());
                } catch (NumberFormatException e) {
                    data.putExtra("SELECTED_USER_ID", -1);
                }
                if (v.getContext() instanceof Activity) {
                    ((Activity) v.getContext()).setResult(Activity.RESULT_OK, data);
                    ((Activity) v.getContext()).finish();
                }
            } else {
                Intent intent = new Intent(v.getContext(), ChatActivity.class);
                try {
                    intent.putExtra("CHAT_USER_ID", Integer.parseInt(user.getId()));
                } catch (NumberFormatException e) {
                    intent.putExtra("CHAT_USER_ID", -1);
                }
                intent.putExtra("CHAT_NAME", user.getFullName());
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName, tvStatus, tvAvatarLetter;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAvatarLetter = itemView.findViewById(R.id.tvAvatarLetter);
        }
    }
}
