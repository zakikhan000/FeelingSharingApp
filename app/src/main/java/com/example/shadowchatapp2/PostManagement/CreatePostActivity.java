package com.example.shadowchatapp2.PostManagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.shadowchatapp2.R;
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreatePostActivity extends AppCompatActivity {
    private static final String BASE_URL = "https://shadow-talk-server.vercel.app/api/create-post";
    private OkHttpClient client = new OkHttpClient();

    private EditText titleInput, contentInput;
    private MaterialButton addImageButton, addLocationButton, postButton;
    private ImageButton backButton;
    private ImageView postImageView, userProfileImage;
    private TextView usernameText;
    private Uri selectedImageUri;
    private String selectedLocation;
    private int uaid;
    private static final int PICK_IMAGE_REQUEST = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        uaid = getIntent().getIntExtra("uaid", -1);
        if (uaid == -1) {
            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
            uaid = prefs.getInt("uaid", -1);
            if (uaid == -1) {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        titleInput = findViewById(R.id.et_post_title);
        contentInput = findViewById(R.id.et_post_content);
        addImageButton = findViewById(R.id.btn_add_photo);
        addLocationButton = findViewById(R.id.btn_add_video);
        backButton = findViewById(R.id.btn_back);
        postButton = findViewById(R.id.btn_post);
        postImageView = findViewById(R.id.iv_post_image);
        userProfileImage = findViewById(R.id.iv_user_profile);
        usernameText = findViewById(R.id.tv_username);

        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String username = prefs.getString("username", "User");
        String userImageUrl = prefs.getString("profile_image_url", null);

        usernameText.setText(username);
        if (userImageUrl != null && !userImageUrl.isEmpty()) {
            Glide.with(this).load(userImageUrl).into(userProfileImage);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        addImageButton.setOnClickListener(v -> openImagePicker());
        addLocationButton.setOnClickListener(v -> {
            selectedLocation = "New York, USA";
            Toast.makeText(this, "Location set to: " + selectedLocation, Toast.LENGTH_SHORT).show();
        });
        postButton.setOnClickListener(v -> createPost());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            postImageView.setVisibility(View.VISIBLE);
            Glide.with(this).load(selectedImageUri).into(postImageView);
        }
    }

    private void createPost() {
        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();

        // Validate required fields
        if (content.isEmpty()) {
            Toast.makeText(this, "Content is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        postButton.setEnabled(false);
        postButton.setText("Posting...");

        try {
            // Create JSON request body
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("uaid", uaid);
            if (!title.isEmpty()) {
                jsonBody.put("title", title);
            }
            jsonBody.put("content", content);
            jsonBody.put("s_id", 1); // Default sentiment ID
            if (selectedImageUri != null) {
                jsonBody.put("image", encodeImageToBase64(selectedImageUri));
            }

            // Create request
            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonBody.toString()
            );

            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .post(requestBody)
                    .build();

            // Make API call
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(CreatePostActivity.this,
                                "Network error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        resetPostButton();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            if (response.isSuccessful() && jsonResponse.getBoolean("success")) {
                                // Success case
                                String postId = jsonResponse.has("post_id") ?
                                        String.valueOf(jsonResponse.getInt("post_id")) : null;

                                Toast.makeText(CreatePostActivity.this,
                                        jsonResponse.getString("message"),
                                        Toast.LENGTH_SHORT).show();

                                // Return result to previous activity
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("new_post_id", postId);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            } else {
                                // Error case - Handle both API errors and HTTP errors
                                String errorMessage;
                                if (response.code() == 400) {
                                    // Handle profanity check error
                                    errorMessage = jsonResponse.has("message") ?
                                            jsonResponse.getString("message") :
                                            "Post contains inappropriate language";
                                } else {
                                    errorMessage = jsonResponse.has("message") ?
                                            jsonResponse.getString("message") :
                                            "Failed to create post";
                                }

                                // Show error in a more visible way
                                Toast.makeText(CreatePostActivity.this,
                                        errorMessage,
                                        Toast.LENGTH_LONG).show();

                                // If it's a profanity error, highlight the content field
                                if (response.code() == 400 && errorMessage.contains("inappropriate")) {
                                    contentInput.setError(errorMessage);
                                    contentInput.requestFocus();
                                }
                            }
                        } catch (JSONException e) {
                            Toast.makeText(CreatePostActivity.this,
                                    "Error parsing response: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        } finally {
                            resetPostButton();
                        }
                    });
                }
            });
        } catch (Exception e) {
            Toast.makeText(this,
                    "Error creating post: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            resetPostButton();
        }
    }

    private String encodeImageToBase64(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void resetPostButton() {
        postButton.setEnabled(true);
        postButton.setText("Post");
    }
}
