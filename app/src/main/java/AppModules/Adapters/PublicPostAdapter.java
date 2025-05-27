package AppModules.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import AppModules.Models.PublicPost;
import com.example.shadowchatapp2.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PublicPostAdapter extends RecyclerView.Adapter<PublicPostAdapter.PublicPostViewHolder> {
    private static final String TAG = "PublicPostAdapter";
    private final List<PublicPost> posts;

    public PublicPostAdapter(List<PublicPost> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public PublicPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.public_item, parent, false);
        return new PublicPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PublicPostViewHolder holder, int position) {
        PublicPost post = posts.get(position);

        // Set post title
        holder.titleText.setText(post.getTitle());

        // Set post content
        holder.contentText.setText(post.getContent());

        // Set post date
        holder.dateText.setText(formatDate(post.getCreatedAt()));

        // Set user profile image if available
        if (post.getUserProfileImage() != null && !post.getUserProfileImage().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(post.getUserProfileImage(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.profileImage.setImageBitmap(decodedByte);
                holder.profileImage.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile image", e);
                holder.profileImage.setImageResource(R.drawable.profile_placeholder);
            }
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }

        // Set user name if available
        if (post.getUserName() != null && !post.getUserName().isEmpty()) {
            holder.userNameText.setText(post.getUserName());
            holder.userNameText.setVisibility(View.VISIBLE);
        } else {
            holder.userNameText.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "";
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return date != null ? outputFormat.format(date) : "";
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateStr, e);
            return dateStr;
        }
    }

    static class PublicPostViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView profileImage;
        final TextView userNameText;
        final TextView titleText;
        final TextView contentText;
        final TextView dateText;

        PublicPostViewHolder(View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.iv_profile);
            userNameText = itemView.findViewById(R.id.tv_username);
            titleText = itemView.findViewById(R.id.tv_title);
            contentText = itemView.findViewById(R.id.tv_content);
            dateText = itemView.findViewById(R.id.tv_date);
        }
    }
}