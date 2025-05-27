package com.example.shadowchatapp2.UserProfile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.shadowchatapp2.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import AppModules.Adapters.UserSearchAdapter;
import AppModules.Models.User;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchUsersActivity";
    private static final String BASE_URL = "https://shadow-talk-app2.vercel.app/api";
    private static final String USER_SESSION = "UserSession";
    private static final String USER_PROFILE = "user_session";

    private TextInputEditText searchInput;
    private RecyclerView usersRecyclerView;
    private ProgressBar progressBar;
    private TextView noResultsText;
    private UserSearchAdapter userAdapter;
    private List<User> users;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private OkHttpClient client;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        searchInput = findViewById(R.id.et_search);
        usersRecyclerView = findViewById(R.id.rv_users);
        progressBar = findViewById(R.id.progress_bar);
        noResultsText = findViewById(R.id.tv_no_results);
        ImageButton backButton = findViewById(R.id.btn_back);

        // Initialize SharedPreferences and Gson
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gson = new Gson();
        client = new OkHttpClient();

        // Initialize RecyclerView
        users = new ArrayList<>();
        userAdapter = new UserSearchAdapter(users);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(userAdapter);

        // Set up click listeners
        backButton.setOnClickListener(v -> finish());

        userAdapter.setOnUserClickListener(user -> {
            Intent intent = new Intent(SearchActivity.this, PublicProfileActivity.class);
            intent.putExtra("uaid", user.getId());
            startActivity(intent);
        });


        // Set up search input listener
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    searchUsers(s.toString());
                } else {
                    clearSearchResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchUsers(String query) {
        progressBar.setVisibility(View.VISIBLE);
        noResultsText.setVisibility(View.GONE);
        usersRecyclerView.setVisibility(View.GONE);

        // Build the URL with the query parameter
        String url = BASE_URL + "/search-public-profiles?query=" + Uri.encode(query);
        Log.d(TAG, "Searching with URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SearchActivity.this,
                            "Failed to search users. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Capture the full response body for logging
                String responseBody = response.body().string();

                // Log the FULL raw response
                Log.d(TAG, "FULL Search API Response (RAW): " + responseBody);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        // Parse the JSON response
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        // Log the parsed JSON for verification
                        Log.d(TAG, "Parsed JSON Response: " + jsonResponse.toString(2));

                        // Check response status
                        if (jsonResponse.getString("status").equals("success")) {
                            JSONArray usersArray = jsonResponse.getJSONArray("users");

                            // Log users array details
                            Log.d(TAG, "Users Array Length: " + usersArray.length());

                            users.clear();

                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject userObj = usersArray.getJSONObject(i);

                                // Log each user object for debugging
                                Log.d(TAG, "User Object " + i + ": " + userObj.toString());

                                User user = new User();

                                // Use 'id' from API
                                String userId = userObj.optString("id", "");
                                String name = userObj.optString("name", "");
                                String profileImage = userObj.optString("profile_image", "");

                                // Log the extracted values
                                Log.d(TAG, "Extracted User Details:");
                                Log.d(TAG, "  - UAID: " + userId);
                                Log.d(TAG, "  - Name: " + name);
                                Log.d(TAG, "  - Profile Image: " + (profileImage != null && !profileImage.isEmpty() ? "Present" : "Not Present"));

                                user.setId(userId);
                                user.setName(name);

                                // Handle profile image
                                if (profileImage != null && !profileImage.isEmpty()) {
                                    // Remove any existing data:image prefix if present
                                    if (profileImage.startsWith("data:image")) {
                                        profileImage = profileImage.substring(profileImage.indexOf(",") + 1);
                                    }
                                    user.setProfileImage(profileImage);
                                }

                                users.add(user);
                            }

                            updateUI();
                        } else {
                            // Log error status
                            String errorMessage = jsonResponse.optString("message", "Error searching users");
                            Log.e(TAG, "Search API Error: " + errorMessage);
                            Toast.makeText(SearchActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        // Log parsing error with full details
                        Log.e(TAG, "Error parsing search results", e);
                        Log.e(TAG, "Problematic Response Body: " + responseBody);

                        Toast.makeText(SearchActivity.this,
                                "Error processing search results: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void updateUI() {
        if (users.isEmpty()) {
            noResultsText.setVisibility(View.VISIBLE);
            usersRecyclerView.setVisibility(View.GONE);
        } else {
            noResultsText.setVisibility(View.GONE);
            usersRecyclerView.setVisibility(View.VISIBLE);
            userAdapter.notifyDataSetChanged();
            Log.d(TAG, "Updated UI with " + users.size() + " users");
        }
    }

    private void clearSearchResults() {
        users.clear();
        userAdapter.notifyDataSetChanged();
        noResultsText.setVisibility(View.GONE);
        usersRecyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }
}