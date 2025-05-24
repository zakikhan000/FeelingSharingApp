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

import com.example.shadowchatapp2.R;
import AppModules.Models.User;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {
    private static final String TAG = "UserSearchAdapter";
    private final List<User> users;
    private OnUserClickListener listener;

    public UserSearchAdapter(List<User> users) {
        this.users = users;
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);

        // Set username to empty since it's not provided by the API
        holder.usernameText.setText("");
        holder.nameText.setText(user.getName());

        // Load profile image
        String profileImageBase64 = user.getProfileImage();
        if (profileImageBase64 != null && !profileImageBase64.isEmpty() && !profileImageBase64.equals("null")) {
            try {
                byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                if (decodedByte != null) {
                    holder.profileImage.setImageBitmap(decodedByte);
                    holder.profileImage.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Successfully loaded profile image for user: " + user.getName());
                } else {
                    Log.e(TAG, "Failed to decode bitmap for user: " + user.getName());
                    holder.profileImage.setImageResource(R.drawable.profile_placeholder);
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid base64 string for user: " + user.getName(), e);
                holder.profileImage.setImageResource(R.drawable.profile_placeholder);
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile image for user: " + user.getName(), e);
                holder.profileImage.setImageResource(R.drawable.profile_placeholder);
            }
        } else {
            Log.d(TAG, "No profile image available for user: " + user.getName());
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onViewRecycled(@NonNull UserViewHolder holder) {
        super.onViewRecycled(holder);
        // Clear the image when the view is recycled
        holder.profileImage.setImageBitmap(null);
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView profileImage;
        final TextView usernameText;
        final TextView nameText;

        UserViewHolder(View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.iv_profile);
            usernameText = itemView.findViewById(R.id.tv_username);
            nameText = itemView.findViewById(R.id.tv_name);
        }
    }
}