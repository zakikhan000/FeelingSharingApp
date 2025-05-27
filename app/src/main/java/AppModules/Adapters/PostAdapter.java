package AppModules.Adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import AppModules.Models.Post;
import com.example.shadowchatapp2.R;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
    private static final String TAG = "PostAdapter";
    private final Context context;
    private final List<Post> posts;
    private OnPostInteractionListener listener;

    // Interface for post interactions
    public interface OnPostInteractionListener {
        void onLikeClicked(int position, int postId);
        void onCommentClick(Post post, int position);
        void onShareClicked(int position, int postId);
        void onBookmarkClicked(int position, int postId);
        void onMoreOptionsClicked(int position, int postId);
        void onPostClicked(int position, int postId);
        void onProfileClicked(int position, int uaid);
    }

    public PostAdapter(Context context) {
        this.context = context;
        this.posts = new ArrayList<>();
    }

    public void setOnPostInteractionListener(OnPostInteractionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);

        // Set user info
        holder.tvUsername.setText(post.getUsername());
        holder.tvPid.setText(String.valueOf(post.getPid()));
        holder.tvUaid.setText(String.valueOf(post.getUaid()));

        // Hide location if not available
        if (post.getLocation() != null && !post.getLocation().isEmpty()) {
            holder.layoutLocation.setVisibility(View.VISIBLE);
            holder.tvLocation.setText(post.getLocation());
        } else {
            holder.layoutLocation.setVisibility(View.GONE);
        }

        // Set post content
        holder.tvTitle.setText(post.getTitle());
        holder.tvCaption.setText(post.getContent());

        // Set like count
        holder.tvLikesCount.setText(post.getLikeCount() + " likes");

        // Set timestamp
        holder.tvTimestamp.setText(post.getTimestamp());

        // Load profile image with Glide
        if (post.getProfileImage() != null && !post.getProfileImage().isEmpty()) {
            Glide.with(context)
                    .load(post.getProfileImage())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.ivProfilePic);
        } else {
            holder.ivProfilePic.setImageResource(R.drawable.default_profile);
        }

        // Load post image with Glide if available
        if (post.getPostImage() != null && !post.getPostImage().isEmpty()) {
            holder.ivPostImage.setVisibility(View.VISIBLE);
            Log.d("PostAdapter", "Loading image from base64: " + post.getPostImage().substring(0, Math.min(50, post.getPostImage().length())) + "...");

            try {
                // Handle base64 image
                String base64Image = post.getPostImage();
                // Remove data URL prefix if present
                if (base64Image.startsWith("data:image")) {
                    base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                }

                byte[] imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                Glide.with(context)
                        .load(imageBytes)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {

                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                Log.e("PostAdapter", "Error loading base64 image: " + e.getMessage());
                                holder.ivPostImage.setVisibility(View.GONE);
                                return false;}


                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                Log.d("PostAdapter", "Base64 image loaded successfully");
                                return false;
                            }
                        })
                        .into(holder.ivPostImage);
            } catch (Exception e) {
                Log.e("PostAdapter", "Error decoding base64 image: " + e.getMessage());
                holder.ivPostImage.setVisibility(View.GONE);
            }
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        // Add profile click listener
        holder.ivProfilePic.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProfileClicked(position, post.getUaid());
            }
        });

        // Show state of like button if the post is liked
        updateLikeButtonState(holder, post.isLiked());

        // Hide PID and UAID TextViews
        holder.tvPid.setVisibility(View.GONE);
        holder.tvUaid.setVisibility(View.GONE);

        // Hide more options button for posts not owned by the logged-in user
        SharedPreferences prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        int loggedInUaid = prefs.getInt("uaid", -1);
        if (post.getUaid() != loggedInUaid) {
            holder.btnMoreOptions.setVisibility(View.GONE);
        } else {
            holder.btnMoreOptions.setVisibility(View.VISIBLE);
        }
    }

    // Method to update like button visual state
    private void updateLikeButtonState(PostViewHolder holder, boolean isLiked) {
        if (isLiked) {
            holder.btnLike.setImageResource(R.drawable.like); // Replace with your filled like icon
        } else {
            holder.btnLike.setImageResource(R.drawable.like); // Replace with your outline like icon
        }
    }

    // Method to update a post's like status and count
    public void updatePostLikeStatus(int position, boolean isLiked, int newLikeCount) {
        if (position >= 0 && position < posts.size()) {
            Post post = posts.get(position);
            post.setLiked(isLiked);

            // Update like count if provided, otherwise increment/decrement
            if (newLikeCount >= 0) {
                post.setLikeCount(newLikeCount);
            } else {
                // Increment or decrement based on like action
                int currentLikes = post.getLikeCount();
                post.setLikeCount(isLiked ? currentLikes + 1 : Math.max(0, currentLikes - 1));
            }

            // Notify adapter of changes to update UI
            notifyItemChanged(position);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void addPosts(List<Post> newPosts) {
        int startPos = posts.size();
        posts.addAll(newPosts);
        notifyItemRangeInserted(startPos, newPosts.size());
    }

    public void clearAndAddPosts(List<Post> newPosts) {
        posts.clear();
        posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    public Post getPostAtPosition(int position) {
        if (position >= 0 && position < posts.size()) {
            return posts.get(position);
        }
        return null;
    }

    public int getPositionForPostId(int postId) {
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getPid() == postId) {
                return i;
            }
        }
        return -1;
    }

    class PostViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView ivProfilePic, ivPostImage;
        TextView tvUsername, tvPid, tvUaid, tvLocation, tvTitle, tvCaption;
        TextView tvLikesCount, tvViewComments, tvTimestamp;
        ImageButton btnLike, btnComment, btnShare, btnBookmark, btnMoreOptions;
        LinearLayout layoutLocation;
        ProgressBar likeButtonProgress;
        FrameLayout likeButtonContainer;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            ivProfilePic = itemView.findViewById(R.id.iv_profile_pic);
            ivPostImage = itemView.findViewById(R.id.iv_post_image);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvPid = itemView.findViewById(R.id.tv_pid);
            tvUaid = itemView.findViewById(R.id.tv_uaid);
            tvLocation = itemView.findViewById(R.id.tv_location);
            layoutLocation = itemView.findViewById(R.id.layout_location);
            tvTitle = itemView.findViewById(R.id.post_title);
            tvCaption = itemView.findViewById(R.id.tv_caption);
            tvLikesCount = itemView.findViewById(R.id.tv_likes_count);
            tvViewComments = itemView.findViewById(R.id.tv_view_comments);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnComment = itemView.findViewById(R.id.btn_comment);
            btnShare = itemView.findViewById(R.id.btn_share);
            btnBookmark = itemView.findViewById(R.id.btn_bookmark);
            btnMoreOptions = itemView.findViewById(R.id.btn_more_options);
            likeButtonProgress = itemView.findViewById(R.id.like_button_progress);
            likeButtonContainer = (FrameLayout) btnLike.getParent();

            // Set click listeners
            btnLike.setOnClickListener(this);
            btnComment.setOnClickListener(this);
            btnShare.setOnClickListener(this);
            btnBookmark.setOnClickListener(this);
            btnMoreOptions.setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION || listener == null) return;

            int postId = posts.get(position).getPid();

            if (v.getId() == R.id.btn_like) {
                // Show progress indicator while like is processing
                showLikeProgress(true);
                listener.onLikeClicked(position, postId);
            } else if (v.getId() == R.id.btn_comment) {
                listener.onCommentClick(posts.get(position), position);
            } else if (v.getId() == R.id.btn_share) {
                listener.onShareClicked(position, postId);
            } else if (v.getId() == R.id.btn_bookmark) {
                listener.onBookmarkClicked(position, postId);
            } else if (v.getId() == R.id.btn_more_options) {
                listener.onMoreOptionsClicked(position, postId);
            } else {
                listener.onPostClicked(position, postId);
            }
        }

        // Show or hide progress indicator for like button
        void showLikeProgress(boolean show) {
            btnLike.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
            likeButtonProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}