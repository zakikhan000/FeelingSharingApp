package com.example.shadowchatapp2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import AppModules.Adapters.PublicPostAdapter;
import AppModules.Models.PublicPost;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PublicProfileActivity extends AppCompatActivity {
    private static final String TAG = "PublicProfileActivity";
    private static final String BASE_URL = "https://shadow-talk-app2.vercel.app/api";

    private ShapeableImageView profileImage;
    private TextView nameText;
    private TextView anonymousNameText;
    private TextView personalInfoText;
    private RecyclerView postsRecyclerView;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private OkHttpClient client;
    private PublicPostAdapter postAdapter;
    private List<PublicPost> posts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);

        // Initialize views
        profileImage = findViewById(R.id.iv_profile);
        nameText = findViewById(R.id.tv_name);
        anonymousNameText = findViewById(R.id.tv_anonymous_name);
        personalInfoText = findViewById(R.id.tv_personal_info);
        postsRecyclerView = findViewById(R.id.rv_posts);
        progressBar = findViewById(R.id.progress_bar);
        toolbar = findViewById(R.id.toolbar);

        // Set up toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize OkHttpClient
        client = new OkHttpClient();

        // Set up RecyclerView
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize posts list and adapter
        posts = new ArrayList<>();
        postAdapter = new PublicPostAdapter(posts);
        postsRecyclerView.setAdapter(postAdapter);

        // Get UAID from intent
        String uaidString = getIntent().getStringExtra("uaid");

        // Log the received UAID for debugging
        Log.d(TAG, "Received UAID: " + uaidString);

        // Check if UAID is null or empty
        if (uaidString == null || uaidString.trim().isEmpty()) {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            // Try to parse the UAID
            int uaid = Integer.parseInt(uaidString);
            loadProfileData(uaid);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing UAID: " + uaidString, e);
            Toast.makeText(this, "Invalid user ID format", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadProfileData(int uaid) {
        Log.d(TAG, "Attempting to load profile for UAID: " + uaid);
        progressBar.setVisibility(View.VISIBLE);
        String url = BASE_URL + "/view-public-profile/" + uaid;
        Log.d(TAG, "Loading profile from URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PublicProfileActivity.this,
                            "Failed to load profile. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Profile response: " + responseBody);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (jsonResponse.getString("status").equals("success")) {
                            JSONObject profile = jsonResponse.getJSONObject("profile");

                            // Set name (this is the display name based on visibility settings)
                            String name = profile.getString("name");
                            nameText.setText(name);

                            // Set anonymous name (if available)
                            if (profile.has("anonymous_name")) {
                                String anonymousName = profile.getString("anonymous_name");
                                anonymousNameText.setText(anonymousName);
                                anonymousNameText.setVisibility(View.VISIBLE);
                            } else {
                                anonymousNameText.setVisibility(View.GONE);
                            }

                            // Set profile image
                            String profileImageBase64 = profile.optString("profile_image");
                            if (profileImageBase64 != null && !profileImageBase64.isEmpty() && !profileImageBase64.equals("null")) {
                                try {
                                    byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    profileImage.setImageBitmap(decodedByte);
                                    profileImage.setVisibility(View.VISIBLE);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error loading profile image", e);
                                    profileImage.setImageResource(R.drawable.profile_placeholder);
                                }
                            } else {
                                profileImage.setImageResource(R.drawable.profile_placeholder);
                            }

                            // Set personal info if available
                            JSONObject personalInfo = profile.optJSONObject("personal_info");
                            if (personalInfo != null) {
                                StringBuilder infoBuilder = new StringBuilder();
                                // Add name components if available
                                if (personalInfo.has("first_name") && !personalInfo.isNull("first_name")) {
                                    infoBuilder.append("First Name: ").append(personalInfo.getString("first_name")).append("\n");
                                }
                                if (personalInfo.has("middle_name") && !personalInfo.isNull("middle_name")) {
                                    infoBuilder.append("Middle Name: ").append(personalInfo.getString("middle_name")).append("\n");
                                }
                                if (personalInfo.has("last_name") && !personalInfo.isNull("last_name")) {
                                    infoBuilder.append("Last Name: ").append(personalInfo.getString("last_name")).append("\n");
                                }
                                // Add other personal info
                                if (personalInfo.has("age") && !personalInfo.isNull("age")) {
                                    infoBuilder.append("Age: ").append(personalInfo.getInt("age")).append("\n");
                                }
                                if (personalInfo.has("country") && !personalInfo.isNull("country")) {
                                    infoBuilder.append("Country: ").append(personalInfo.getString("country")).append("\n");
                                }
                                if (personalInfo.has("city") && !personalInfo.isNull("city")) {
                                    infoBuilder.append("City: ").append(personalInfo.getString("city")).append("\n");
                                }
                                if (personalInfo.has("postal_code") && !personalInfo.isNull("postal_code")) {
                                    infoBuilder.append("Postal Code: ").append(personalInfo.getString("postal_code"));
                                }
                                personalInfoText.setText(infoBuilder.toString());
                                personalInfoText.setVisibility(View.VISIBLE);
                            } else {
                                personalInfoText.setVisibility(View.GONE);
                            }

                            // Clear existing posts
                            posts.clear();

                            // Set posts
                            JSONArray postsArray = profile.getJSONArray("posts");
                            for (int i = 0; i < postsArray.length(); i++) {
                                JSONObject postObj = postsArray.getJSONObject(i);
                                PublicPost post = new PublicPost();
                                post.setPid(postObj.getInt("PID"));
                                post.setTitle(postObj.getString("Title"));
                                post.setContent(postObj.getString("Content"));
                                post.setCreatedAt(postObj.getString("Created_At"));
                                post.setUaid(uaid);
                                post.setUserName(name);
                                post.setUserProfileImage(profileImageBase64);
                                posts.add(post);
                            }
                            postAdapter.notifyDataSetChanged();

                        } else {
                            String errorMessage = jsonResponse.optString("message", "Error loading profile");
                            Toast.makeText(PublicProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing profile data", e);
                        Toast.makeText(PublicProfileActivity.this,
                                "Error processing profile data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}