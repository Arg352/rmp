package com.asylum.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asylum.app.R;
import com.asylum.app.api.ApiService;
import com.asylum.app.api.RetrofitClient;
import com.asylum.app.models.User;
import com.asylum.app.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.ViewHolder> {

    private List<User> users;
    private final OnFriendActionCompleteListener listener;

    public interface OnFriendActionCompleteListener {
        void onActionComplete();
    }

    public SearchUserAdapter(List<User> users, OnFriendActionCompleteListener listener) {
        this.users = users;
        this.listener = listener;
    }

    public void updateList(List<User> newList) {
        this.users = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvFullName.setText(user.getFullName());
        holder.tvAvatarLetter.setText(
                user.getFullName().isEmpty() ? "?" : user.getFullName().substring(0, 1).toUpperCase()
        );

        String status = user.getStatus(); 
        
        if ("FRIEND".equals(status)) {
            holder.tvStatus.setText("У вас в друзьях");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.asylum_red));
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_delete); // Иконка удаления
            holder.ivIcon.setAlpha(1.0f);
            holder.ivIcon.setEnabled(true);
        } else if ("SENT".equals(status)) {
            holder.tvStatus.setText("Заявка отправлена");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            holder.ivIcon.setAlpha(1.0f);
            holder.ivIcon.setEnabled(true);
        } else if ("RECEIVED".equals(status)) {
            holder.tvStatus.setText("Хочет в друзья");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.asylum_red));
            holder.ivIcon.setImageResource(android.R.drawable.ic_input_add);
            holder.ivIcon.setAlpha(1.0f);
            holder.ivIcon.setEnabled(true);
        } else {
            holder.tvStatus.setText("@" + user.getUsername());
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
            holder.ivIcon.setImageResource(android.R.drawable.ic_input_add);
            holder.ivIcon.setAlpha(1.0f);
            holder.ivIcon.setEnabled(true);
        }

        holder.ivIcon.setOnClickListener(v -> {
            SessionManager sessionManager = new SessionManager(v.getContext());
            ApiService apiService = RetrofitClient.getInstance().getApiService();
            String token = sessionManager.getBearerToken();

            try {
                int userId = Integer.parseInt(user.getId());
                if (token != null) {
                    apiService.followUser(token, userId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            if (response.isSuccessful()) {
                                String msg = "Выполнено";
                                if ("FRIEND".equals(user.getStatus()) || "SENT".equals(user.getStatus())) {
                                    user.setStatus("NONE");
                                    msg = "Удалено";
                                } else if ("RECEIVED".equals(user.getStatus())) {
                                    user.setStatus("FRIEND");
                                    msg = "Заявка принята";
                                } else {
                                    user.setStatus("SENT");
                                    msg = "Заявка отправлена";
                                }
                                Toast.makeText(v.getContext(), msg, Toast.LENGTH_SHORT).show();
                                notifyItemChanged(holder.getAdapterPosition());
                                if (listener != null) listener.onActionComplete();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            Toast.makeText(v.getContext(), "Ошибка сети", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (NumberFormatException e) {}
        });
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName, tvStatus, tvAvatarLetter;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAvatarLetter = itemView.findViewById(R.id.tvAvatarLetter);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}
