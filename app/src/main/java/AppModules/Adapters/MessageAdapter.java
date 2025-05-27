package AppModules.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import AppModules.Models.Message;
import com.example.shadowchatapp2.R;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Message> messageList;

    public MessageAdapter() {
        this.messageList = new ArrayList<>();
    }

    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == Message.TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_item, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.bot_message_item, parent, false);
            return new BotMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (holder instanceof UserMessageViewHolder) {
            UserMessageViewHolder userHolder = (UserMessageViewHolder) holder;
            userHolder.messageText.setText(message.getText());
            userHolder.timeText.setText(message.getFormattedTime());
        } else if (holder instanceof BotMessageViewHolder) {
            BotMessageViewHolder botHolder = (BotMessageViewHolder) holder;
            botHolder.messageText.setText(message.getText());
            botHolder.timeText.setText(message.getFormattedTime());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;

        UserMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tv_message);
            timeText = itemView.findViewById(R.id.tv_time);
        }
    }

    // ViewHolder for bot messages
    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        ImageView botAvatar;

        BotMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tv_message);
            timeText = itemView.findViewById(R.id.tv_time);
            botAvatar = itemView.findViewById(R.id.iv_bot_avatar);
        }
    }
}

