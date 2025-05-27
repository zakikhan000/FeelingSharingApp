package com.example.shadowchatapp2.PostManagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;


import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.shadowchatapp2.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ApiModules.ApiClient;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostManagementActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String BASE_URL = "https://shadow-talk-app2.vercel.app/api";
    private static final String TAG = "PostManagementActivity";

    private TextInputEditText postContentEditText;
    private TextInputEditText postTitleEditText;
    private TextView postDateTextView;
    private ImageView postImageView;
    private MaterialButton changeImageButton;
    private MaterialButton updateButton;
    private MaterialButton deleteButton;
    private ProgressBar progressBar;
    private String postId;
    private String currentImageUrl;
    private Uri selectedImageUri;
    private OkHttpClient client;
    private boolean isFinishing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_management);
        postTitleEditText = findViewById(R.id.et_post_title);
        postContentEditText = findViewById(R.id.et_post_content);
        postDateTextView = findViewById(R.id.tv_post_date);
        postImageView = findViewById(R.id.iv_post_image);
        changeImageButton = findViewById(R.id.btn_change_image);
        updateButton = findViewById(R.id.btn_update);
        deleteButton = findViewById(R.id.btn_delete);
        progressBar = findViewById(R.id.progress_bar);

        client = new OkHttpClient();

        // Get post data from intent
        int postIdInt = getIntent().getIntExtra("post_id", -1);
        if (postIdInt == -1) {
            Toast.makeText(this, "Invalid post ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        postId = String.valueOf(postIdInt);

        String postContent = getIntent().getStringExtra("post_content");
        String postTitle = getIntent().getStringExtra("post_title");
        long postDate = getIntent().getLongExtra("post_date", 0);
        currentImageUrl = getIntent().getStringExtra("post_image_url");

        // Set post data
        postContentEditText.setText(postContent);
        postTitleEditText.setText(postTitle);
        postDateTextView.setText(formatDate(postDate));

        // Load post image if exists
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            if (currentImageUrl.startsWith("data:image")) {
                // Handle base64 image data
                Glide.with(this)
                        .load(currentImageUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .centerCrop()
                        .into(postImageView);
            } else {
                // Handle URL image
                Glide.with(this)
                        .load(currentImageUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .centerCrop()
                        .into(postImageView);
            }
            postImageView.setVisibility(View.VISIBLE);
        } else {
            postImageView.setVisibility(View.GONE);
        }

        // Set up click listeners
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        changeImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        updateButton.setOnClickListener(v -> updatePost());
        deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                Glide.with(this)
                        .load(selectedImageUri)
                        .centerCrop()
                        .into(postImageView);
                postImageView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updatePost() {
        String newContent = postContentEditText.getText().toString().trim();
        String newTitle = postTitleEditText.getText().toString().trim();
        if (newContent.isEmpty()) {
            Toast.makeText(this, "Post content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        try {
            // Get user ID from SharedPreferences
            int uaid = -1;

            // Try user_session first
            SharedPreferences prefs1 = getSharedPreferences("user_session", MODE_PRIVATE);
            uaid = prefs1.getInt("uaid", -1);

            // If not found, try UserSession
            if (uaid == -1) {
                SharedPreferences prefs2 = getSharedPreferences("UserSession", MODE_PRIVATE);
                String uaidStr = prefs2.getString("uaid", null);
                if (uaidStr != null) {
                    try {
                        uaid = Integer.parseInt(uaidStr);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing uaid from UserSession", e);
                    }
                }
            }

            if (uaid == -1) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                showLoading(false);
                return;
            }

            // Create JSON request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("uaid", uaid);
            requestBody.put("content", newContent);
            requestBody.put("pid", Integer.parseInt(postId));
            requestBody.put("title", newTitle.isEmpty() ? null : newTitle);

            // Handle image data
            if (selectedImageUri != null) {
                // Convert new image to Base64
                String base64Image = convertImageToBase64(selectedImageUri);
                requestBody.put("image", base64Image);
            } else if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                // Keep existing image
                requestBody.put("image", currentImageUrl);
            }

            // Log request data for debugging
            Log.d(TAG, "Update Post Request - UAID: " + uaid);
            Log.d(TAG, "Update Post Request - PostID: " + postId);
            Log.d(TAG, "Update Post Request - Title: " + newTitle);
            Log.d(TAG, "Update Post Request - Content: " + newContent);
            Log.d(TAG, "Update Post Request - Has Image: " + (selectedImageUri != null || (currentImageUrl != null && !currentImageUrl.isEmpty())));

            // Create request
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(ApiClient.BASE_URL + "/api/update-post/" + postId)
                    .put(body)
                    .build();

            // Make API call
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Error updating post", e);
                        Toast.makeText(PostManagementActivity.this,
                                "Network error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Update Post Response - Code: " + response.code());
                    Log.d(TAG, "Update Post Response - Body: " + responseBody);

                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            if (response.isSuccessful() && jsonResponse.getBoolean("success")) {
                                // Update successful
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("post_id", postId);
                                resultIntent.putExtra("post_content", newContent);
                                resultIntent.putExtra("post_title", newTitle);
                                if (selectedImageUri != null || currentImageUrl != null) {
                                    resultIntent.putExtra("post_image_url",
                                            selectedImageUri != null ?
                                                    convertImageToBase64(selectedImageUri) :
                                                    currentImageUrl);
                                }
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            } else {
                                // Handle API error
                                String errorMessage = jsonResponse.getString("message");
                                Log.e(TAG, "Update Post Error: " + errorMessage);
                                Toast.makeText(PostManagementActivity.this,
                                        "Error: " + errorMessage,
                                        Toast.LENGTH_SHORT).show();
                                showLoading(false);
                            }
                        } catch (JSONException | IOException e) {
                            Log.e(TAG, "Error parsing response", e);
                            Toast.makeText(PostManagementActivity.this,
                                    "Error parsing server response",
                                    Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error updating post", e);
            Toast.makeText(this, "Error updating post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showLoading(false);
        }
    }

    private void deletePost() {
        // Prevent multiple attempts
        if (isFinishing) {
            return;
        }

        showLoading(true);

        try {
            // Create request
            Request request = new Request.Builder()
                    .url(ApiClient.BASE_URL + "/api/delete-post/" + postId)
                    .delete()
                    .build();

            // Log request for debugging
            Log.d(TAG, "Delete Post Request - PostID: " + postId);

            // Make API call
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Error deleting post", e);
                        Toast.makeText(PostManagementActivity.this,
                                "Network error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Delete Post Response - Code: " + response.code());
                    Log.d(TAG, "Delete Post Response - Body: " + responseBody);

                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            if (response.isSuccessful() && jsonResponse.getBoolean("success")) {
                                // Delete successful
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("post_id", postId);
                                resultIntent.putExtra("deleted", true);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            } else {
                                // Handle API error
                                String errorMessage = jsonResponse.getString("message");
                                Log.e(TAG, "Delete Post Error: " + errorMessage);
                                Toast.makeText(PostManagementActivity.this,
                                        "Error: " + errorMessage,
                                        Toast.LENGTH_SHORT).show();
                                showLoading(false);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing response", e);
                            Toast.makeText(PostManagementActivity.this,
                                    "Error parsing server response",
                                    Toast.LENGTH_SHORT).show();
                            showLoading(false);
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error deleting post", e);
            Toast.makeText(this, "Error deleting post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            showLoading(false);
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        updateButton.setEnabled(!show);
        deleteButton.setEnabled(!show);
    }

    private String convertImageToBase64(Uri imageUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        if (inputStream == null) {
            throw new IOException("Could not open image stream");
        }

        // Read the image data
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        inputStream.close();

        // Convert to Base64 and add data URI prefix
        String base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
        return "data:image/jpeg;base64," + base64;
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePost())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Override onDestroy to log when the activity is being destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PostManagementActivity: onDestroy called");
    }
}
