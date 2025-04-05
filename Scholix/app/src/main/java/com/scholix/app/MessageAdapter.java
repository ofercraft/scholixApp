package com.scholix.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private final List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.title.setText(message.title);
        holder.sender.setText(message.sender);

        Glide.with(holder.itemView.getContext())
                .load(message.iconUrl)
                .placeholder(R.drawable.round_notifications_24)
                .error(R.drawable.round_notifications_24)
                .circleCrop() // Automatically make images rounded
                .into(holder.icon);
    }




    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView title, sender;
        ImageView icon;

        MessageViewHolder(View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.message_title);
            title = itemView.findViewById(R.id.message_description);
            icon = itemView.findViewById(R.id.message_icon);
        }
    }
}
