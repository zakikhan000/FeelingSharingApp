package com.example.shadowchatapp2.PostManagement;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shadowchatapp2.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import AppModules.Adapters.CommentAdapter;
import AppModules.Models.Comment;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CommentActivity extends AppCompatActivity {

    private static final String TAG = "CommentActivity";
    private static final String CACHE_KEY = "comments_cache_";
    private static final int CACHE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final int TIMEOUT_SECONDS = 10;
    private static final int COMMENTS_PER_PAGE = 20;

    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private EditText etComment;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private ProgressBar footerProgressBar;
    private String postId;
    private String userId;
    private OkHttpClient client;
    private static final String BASE_URL = "https://shadow-talk-server.vercel.app/api";
    private boolean isLoading = false;
    private boolean hasMoreComments = true;
    private int currentPage = 1;


    private List<Comment> allComments = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_comment);
        // Initialize views
        // Initialize views
        recyclerView = findViewById(R.id.recycler_view);
        etComment = findViewById(R.id.et_comment);
        btnSend = findViewById(R.id.btn_send);
        progressBar = findViewById(R.id.progress_bar);
        footerProgressBar = findViewById(R.id.footer_progress_bar);

        // Get post ID from intent
        postId = getIntent().getStringExtra("post_id");
        if (postId == null) {
            Toast.makeText(this, "Error: Post ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        userId = String.valueOf(prefs.getInt("uaid", -1));
        if (userId.equals("-1")) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize OkHttpClient with cache and timeout
        client = new OkHttpClient.Builder()
                .cache(new Cache(getCacheDir(), CACHE_SIZE))
                .connectTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new CommentAdapter(this);
        recyclerView.setAdapter(adapter);

        // Add scroll listener for pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && hasMoreComments) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadMoreComments();
                    }
                }
            }
        });

        // Setup click listeners
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> postComment());


        // Load cached comments first
        loadCachedComments();
        loadComments();
        // Then load fresh comments

    }
    private void loadCachedComments() {
        SharedPreferences cache = getSharedPreferences("comments_cache", MODE_PRIVATE);
        String cachedJson = cache.getString(CACHE_KEY + postId, null);

        if (cachedJson != null) {
            try {
                JSONObject jsonResponse = new JSONObject(cachedJson);
                JSONArray commentsArray = jsonResponse.getJSONArray("comments");
                List<Comment> comments = parseComments(commentsArray);
                allComments.clear(); // Prevent duplication
                allComments.addAll(comments);
                adapter.updateComments(allComments);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing cached comments", e);
            }
        }

        // Now load fresh comments from network
        loadComments();
    }


    private void loadComments() {
        if (isLoading) return;
        isLoading = true;
        currentPage = 1;
        hasMoreComments = true;

        progressBar.setVisibility(View.VISIBLE);
        footerProgressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        String url = BASE_URL + "/get-comments/" + postId + "?page=" + currentPage + "&limit=" + COMMENTS_PER_PAGE;
        Log.d(TAG, "Loading comments from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .header("Cache-Control", "public, max-age=60")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    if (allComments.isEmpty()) {
                        Toast.makeText(CommentActivity.this,
                                "Failed to load comments. Please check your connection.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Comments API response: " + responseBody);

                runOnUiThread(() -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                });

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to load comments: HTTP " + response.code() + " - " + responseBody);
                    runOnUiThread(() -> {
                        if (allComments.isEmpty()) {
                            String msg = "Failed to load comments: " + response.code();
                            if (response.code() == 500) {
                                msg += " (Server error)";
                            }
                            Toast.makeText(CommentActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }

                try {
                    // Cache the response
                    SharedPreferences.Editor editor = getSharedPreferences("comments_cache", MODE_PRIVATE).edit();
                    editor.putString(CACHE_KEY + postId, responseBody);
                    editor.apply();

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.getBoolean("success")) {
                        JSONArray commentsArray = jsonResponse.getJSONArray("comments");
                        List<Comment> newComments = parseComments(commentsArray);

                        runOnUiThread(() -> {
                            allComments.clear();
                            allComments.addAll(newComments);
                            adapter.updateComments(allComments);
                        });
                    } else {
                        runOnUiThread(() -> {
                            if (allComments.isEmpty()) {
                                Toast.makeText(CommentActivity.this,
                                        jsonResponse.optString("message", "Failed to load comments"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing response", e);
                    runOnUiThread(() -> {
                        if (allComments.isEmpty()) {
                            Toast.makeText(CommentActivity.this,
                                    "Error loading comments. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void loadMoreComments() {
        if (isLoading || !hasMoreComments) return;
        isLoading = true;
        currentPage++;

        footerProgressBar.setVisibility(View.VISIBLE);

        Request request = new Request.Builder()
                .url(BASE_URL + "/get-comments/" + postId + "?page=" + currentPage + "&limit=" + COMMENTS_PER_PAGE)
                .header("Cache-Control", "public, max-age=60")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    isLoading = false;
                    footerProgressBar.setVisibility(View.GONE);
                    Toast.makeText(CommentActivity.this,
                            "Failed to load more comments",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    isLoading = false;
                    footerProgressBar.setVisibility(View.GONE);
                });


                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(CommentActivity.this,
                            "Failed to load more comments: " + response.code(),
                            Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    Log.d("CommentActivity", "Comments API response: " + responseBody);
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    if (jsonResponse.getBoolean("success")) {
                        JSONArray commentsArray = jsonResponse.getJSONArray("comments");
                        List<Comment> newComments = parseComments(commentsArray);

                        if (newComments.size() < COMMENTS_PER_PAGE) {
                            hasMoreComments = false;
                        }

                        runOnUiThread(() -> {
                            allComments.addAll(newComments);
                            adapter.updateComments(allComments);
                        });
                    }
                } catch (JSONException | IOException e) {
                    Log.e(TAG, "Error parsing response", e);
                    runOnUiThread(() -> Toast.makeText(CommentActivity.this,
                            "Error loading more comments",
                            Toast.LENGTH_SHORT).show());
                }
            }
        });
    }


    private List<Comment> parseComments(JSONArray commentsArray) throws JSONException {
        List<Comment> comments = new ArrayList<>();
        for (int i = 0; i < commentsArray.length(); i++) {
            JSONObject commentJson = commentsArray.getJSONObject(i);
            Comment comment = new Comment();
            comment.setCommentId(commentJson.getInt("comId"));
            comment.setUaid(commentJson.getInt("uaid"));
            comment.setCommentText(commentJson.getString("commentText"));
            comment.setCreatedAt(commentJson.getString("createdAt"));
            String userName = commentJson.optString("user_name", "Anonymous");
            if (userName == null || userName.equals("null") || userName.trim().isEmpty()) {
                userName = "Anonymous";
            }
            comment.setUserName(userName);
            comment.setProfileImage(commentJson.optString("profile_image", null));
            comments.add(comment);
        }
        return comments;
    }
    private void postComment() {
        String commentText = etComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("pid", postId); // postId from intent
            jsonBody.put("uaid", userId); // userId from SharedPreferences
            jsonBody.put("comment_text", commentText);
            // Optionally: jsonBody.put("s_id", ...); // if you have a value for s_id
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating comment", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://shadow-talk-server.vercel.app/api/add-comment")
                .post(body)
                .build();

        progressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    Toast.makeText(CommentActivity.this, "Failed to post comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                });

                String responseBody = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.getBoolean("success")) {
                        runOnUiThread(() -> {
                            etComment.setText("");
                            Toast.makeText(CommentActivity.this, "Comment added!", Toast.LENGTH_SHORT).show();
                            loadComments(); // Refresh comments
                        });
                    } else {
                        String errorMsg = jsonResponse.optString("message", "Failed to add comment");
                        runOnUiThread(() -> Toast.makeText(CommentActivity.this, errorMsg, Toast.LENGTH_SHORT).show());
                    }
                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(CommentActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

}