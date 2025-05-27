package AppModules.Fragments;


import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import ApiModules.ApiClient;
import com.example.shadowchatapp2.PostManagement.CommentActivity;
import AppModules.Models.Post;
import com.example.shadowchatapp2.PostManagement.PostManagementActivity;
import com.example.shadowchatapp2.UserProfile.PublicProfileActivity;
import com.example.shadowchatapp2.R;

import java.util.List;

import AppModules.Adapters.PostAdapter;
import okhttp3.OkHttpClient;

public class HomeFragment extends Fragment implements
        PostAdapter.OnPostInteractionListener,
        SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private LinearLayout emptyStateView;
    private Button btnCreateFirstPost;
    private PostAdapter adapter;
    private Context context;
    private static final int MANAGE_POST_REQUEST_CODE = 1001;

    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 4;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int totalPosts = 0;

    private OkHttpClient client;
    private int currentUserUAID;
    private BroadcastReceiver postNotificationReceiver;

    // Define a field to hold the action to perform after loading
    private Runnable postLoadAction = null;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyStateView = view.findViewById(R.id.empty_state);
        btnCreateFirstPost = view.findViewById(R.id.btn_create_first_post);

        // Set up RecyclerView
        adapter = new PostAdapter(context);
        adapter.setOnPostInteractionListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        // Set up scroll listener for pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                // Check if we need to load more data
                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= ITEMS_PER_PAGE) {
                        loadMorePosts();
                    }
                }
            }
        });

        // Set up create post button
        btnCreateFirstPost.setOnClickListener(v -> {
            // Handle create post action
            Toast.makeText(context, "Create post clicked", Toast.LENGTH_SHORT).show();
        });

        // Load initial data
        loadInitialPosts();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = new OkHttpClient();

        // Get current user's UAID
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        currentUserUAID = prefs.getInt("uaid", -1);

        // Initialize post notification receiver
        postNotificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("com.example.shadowchatapp2.POST_NOTIFICATION")) {
                    int postId = intent.getIntExtra("post_id", -1);
                    String type = intent.getStringExtra("type");
                    if (postId != -1) {
                        handlePostNotification(postId, type);
                    }
                }
            }
        };
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onResume() {
        super.onResume();
        // Register receiver
        IntentFilter filter = new IntentFilter("com.example.shadowchatapp2.POST_NOTIFICATION");
        ContextCompat.registerReceiver(requireActivity(), postNotificationReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister receiver
        if (postNotificationReceiver != null) {
            requireActivity().unregisterReceiver(postNotificationReceiver);
        }
    }

    private void handlePostNotification(int postId, String type) {
        // Set the action to perform after loading posts
        postLoadAction = () -> {
            // Scroll to the specific post if it exists
            if (adapter != null) {
                int position = adapter.getPositionForPostId(postId);
                if (position != -1) {
                    recyclerView.smoothScrollToPosition(position);
                    // Optionally highlight the post or show a temporary indicator
                } else {
                    // Post not found in the currently loaded page(s)
                    Toast.makeText(getContext(), "Post not found in feed", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Post ID " + postId + " not found in adapter after refresh.");
                }
            }
        };

        // Refresh the post list to show the updated post
        loadInitialPosts();
    }

    private void loadInitialPosts() {
        showLoading();
        currentPage = 0;
        isLastPage = false;

        ApiClient.getInstance().getPosts(ITEMS_PER_PAGE, 0, new ApiClient.PostsCallback() {
            @Override
            public void onSuccess(List<Post> posts, int totalPostsCount) {
                if (!isAdded()) return; // Check if fragment is still attached

                hideLoading();
                swipeRefreshLayout.setRefreshing(false);
                totalPosts = totalPostsCount;

                if (posts.isEmpty()) {
                    showEmptyState();
                } else {
                    hideEmptyState();
                    adapter.clearAndAddPosts(posts);

                    // Check if this is the last page
                    isLastPage = (currentPage + 1) * ITEMS_PER_PAGE >= totalPosts;
                    currentPage = 1;  // Next page will be 1
                }

                // Execute any pending post-load action
                if (postLoadAction != null) {
                    postLoadAction.run();
                    postLoadAction = null; // Clear the action
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return; // Check if fragment is still attached

                hideLoading();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                showEmptyState();

                // Clear any pending action on error
                postLoadAction = null;
            }
        });
    }

    private void loadMorePosts() {
        isLoading = true;

        ApiClient.getInstance().getPosts(ITEMS_PER_PAGE, currentPage * ITEMS_PER_PAGE,
                new ApiClient.PostsCallback() {
                    @Override
                    public void onSuccess(List<Post> posts, int totalPostsCount) {
                        if (!isAdded()) return; // Check if fragment is still attached

                        isLoading = false;
                        totalPosts = totalPostsCount;

                        if (!posts.isEmpty()) {
                            adapter.addPosts(posts);
                            currentPage++;
                        }

                        // Check if this is the last page
                        isLastPage = (currentPage * ITEMS_PER_PAGE) >= totalPosts;
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isAdded()) return; // Check if fragment is still attached

                        isLoading = false;
                        Toast.makeText(context,
                                "Failed to load more posts: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRefresh() {
        // Reload data when user pulls to refresh
        loadInitialPosts();
    }

    // Post interaction implementation methods
    @Override
    public void onLikeClicked(int position, int postId) {
        Post post = adapter.getPostAtPosition(position);
        if (post == null) return;

        // Get user ID from SharedPreferences
        SharedPreferences prefs = getActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        int userId = prefs.getInt("uaid", -1);
        if (userId == -1) {
            Toast.makeText(context, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.getInstance().toggleLikePost(
                userId,
                postId,
                null, // sessionID removed as per request
                new ApiClient.LikeCallback() {
                    @Override
                    public void onSuccess(boolean isLiked, int newLikeCount) {
                        if (getContext() == null) return; // Fragment not attached
                        adapter.updatePostLikeStatus(position, isLiked, newLikeCount);
                        Log.d(TAG, "Post " + postId + " " + (isLiked ? "liked" : "unliked"));
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (getContext() == null) return; // Fragment not attached
                        adapter.notifyItemChanged(position);
                        Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    @Override
    public void onCommentClick(Post post, int position) {
        String postIdStr = String.valueOf(post.getPid());
        Log.d("HomeFragment", "Opening comments for post_id: " + postIdStr);
        Intent intent = new Intent(getActivity(), CommentActivity.class);
        intent.putExtra("post_id", postIdStr);
        startActivity(intent);
    }

    @Override
    public void onShareClicked(int position, int postId) {
        // Implement share functionality
        Toast.makeText(context, "Share clicked on post #" + postId, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onBookmarkClicked(int position, int postId) {
        // Implement bookmark functionality
        Toast.makeText(context, "Bookmark clicked on post #" + postId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMoreOptionsClicked(int position, int postId) {
        Post post = adapter.getPostAtPosition(position);
        if (post != null) {
            Intent intent = new Intent(getActivity(), PostManagementActivity.class);
            intent.putExtra("post_id", post.getPid());
            intent.putExtra("post_title", post.getTitle());
            intent.putExtra("post_content", post.getContent());
            intent.putExtra("post_date", post.getTimestamp());
            // Pass the base64 image data directly
            if (post.getPostImage() != null && !post.getPostImage().isEmpty()) {
                intent.putExtra("post_image_url", post.getPostImage());
            }
            startActivityForResult(intent, MANAGE_POST_REQUEST_CODE);
        }
    }


    @Override
    public void onPostClicked(int position, int postId) {
        // Open post detail
        Toast.makeText(context, "Post #" + postId + " clicked", Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_POST_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh the posts list after post management
            loadInitialPosts();
        }
    }
    @Override
    public void onProfileClicked(int position, int uaid) {
        // Open PublicProfileActivity with the user's UAID
        Intent intent = new Intent(getActivity(), PublicProfileActivity.class);
        intent.putExtra("uaid", String.valueOf(uaid));
        startActivity(intent);
    }
    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateView.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        emptyStateView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
}
