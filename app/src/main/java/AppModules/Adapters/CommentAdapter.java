package AppModules.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import AppModules.Models.Comment;
import com.example.shadowchatapp2.UserProfile.PublicProfileActivity;
import com.example.shadowchatapp2.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CommentAdapter  extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private Context context;
    private List<Comment> comments;

    public CommentAdapter(Context context) {
        this.context = context;
        this.comments = new ArrayList<>();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);

        holder.tvUsername.setText(comment.getUserName());
        holder.tvComment.setText(comment.getCommentText());
        holder.tvTimestamp.setText(formatTimestamp(comment.getCreatedAt()));

        // Load profile image
        if (comment.getProfileImage() != null && !comment.getProfileImage().isEmpty()) {
            Glide.with(context)
                    .load("data:image/jpeg;base64," + comment.getProfileImage())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(holder.ivProfilePic);
        } else {
            holder.ivProfilePic.setImageResource(R.drawable.default_profile);
        }

        // 🔗 Open PublicProfileActivity on click
        View.OnClickListener profileClickListener = v -> {
            Intent intent = new Intent(context, PublicProfileActivity.class);
            intent.putExtra("uaid", String.valueOf(comment.getUaid()));  // int ko string me convert karke pass kar rahe hain
            context.startActivity(intent);

        };

        holder.tvUsername.setOnClickListener(profileClickListener);
        holder.ivProfilePic.setOnClickListener(profileClickListener);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

public void updateComments(List<Comment> newComments) {
    this.comments = newComments;
    notifyDataSetChanged();
}
    public void addComment(Comment comment) {
        comments.add(0, comment);
        notifyItemInserted(0);
    }

    private String formatTimestamp(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(timestamp);

            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return timestamp;
        }
    }
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivProfilePic;
        TextView tvUsername;
        TextView tvComment;
        TextView tvTimestamp;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePic = itemView.findViewById(R.id.iv_profile_pic);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvComment = itemView.findViewById(R.id.tv_comment);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
        }
    }
}
