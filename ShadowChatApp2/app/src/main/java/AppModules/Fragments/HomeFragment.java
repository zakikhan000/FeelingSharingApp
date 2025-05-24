package AppModules.Fragments;


import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.shadowchatapp2.CommentActivity;
import AppModules.Models.Post;
import com.example.shadowchatapp2.PostManagementActivity;
import com.example.shadowchatapp2.PublicProfileActivity;
import com.example.shadowchatapp2.R;

import java.util.List;

import AppModules.Adapters.PostAdapter;


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
    public void onResume() {
        super.onResume();
        // Optionally refresh data when fragment resumes
        // loadInitialPosts();
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
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return; // Check if fragment is still attached

                hideLoading();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                showEmptyState();
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
