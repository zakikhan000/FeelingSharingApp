package AppModules.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.shadowchatapp2.AuthenticationaAndUpdates.LoginActivity;
import com.example.shadowchatapp2.R;
import com.example.shadowchatapp2.UserProfile.UpdateProfileActivity;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ProfileFragment extends Fragment {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton menuButton;
    private static final String TAG = "ProfileFragment";
    private static final String BASE_URL = "https://shadow-talk-server.vercel.app/api";
    private static final String USER_SESSION = "user_session";
    private static final String USER_SESSION_ALT = "UserSession";
    private ImageButton editProfileButton;
    private ImageView profileImage;
    private TextView usernameText;
    private TextView emailText;
    private TextView nameText;
    private TextView anonymousNameText;
    private TextView ageText;
    private TextView phoneText;
    private TextView countryText;
    private TextView cityText;
    private TextView postalCodeText;
    private TextView firstNameText;
    private TextView middleNameText;
    private TextView lastNameText;
    private OkHttpClient client;
    private ProgressBar progressBar;

    // Activity section
    private LinearLayout activityContainer;
    private ProgressBar progressBarActivities;
    private TextView noActivityText;
    private RecyclerView rvActivities;
    private ImageButton toggleActivityButton;
    private ActivityAdapter activityAdapter;
    private List<ActivityItem> activityList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        drawerLayout = view.findViewById(R.id.drawer_layout);
        navigationView = view.findViewById(R.id.nav_view);
        menuButton = view.findViewById(R.id.menu_button);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        profileImage = view.findViewById(R.id.profile_image);
        usernameText = view.findViewById(R.id.username_text);
        emailText = view.findViewById(R.id.email_text);
        anonymousNameText = view.findViewById(R.id.anonymous_name_text);
        ageText = view.findViewById(R.id.age_text);
        countryText = view.findViewById(R.id.country_text);
        cityText = view.findViewById(R.id.city_text);
        postalCodeText = view.findViewById(R.id.postal_code_text);
        firstNameText = view.findViewById(R.id.first_name_text);
        middleNameText = view.findViewById(R.id.middle_name_text);
        lastNameText = view.findViewById(R.id.last_name_text);
        progressBar = view.findViewById(R.id.progress);
        // Initialize OkHttpClient
        client = new OkHttpClient();

        // Activity section
        activityContainer = view.findViewById(R.id.activity_container);
        progressBarActivities = view.findViewById(R.id.progress_bar_activities);
        noActivityText = view.findViewById(R.id.no_activity_text);
        rvActivities = view.findViewById(R.id.rv_activities);
        toggleActivityButton = view.findViewById(R.id.toggle_activity_button);
        activityAdapter = new ActivityAdapter(activityList);
        rvActivities.setLayoutManager(new LinearLayoutManager(getContext()));
        rvActivities.setAdapter(activityAdapter);
        toggleActivityButton.setOnClickListener(v -> {
            if (activityContainer.getVisibility() == View.VISIBLE) {
                activityContainer.setVisibility(View.GONE);
            } else {
                activityContainer.setVisibility(View.VISIBLE);
                loadRecentActivities();
            }
        });

        // Set up navigation drawer
        setupNavigationDrawer();

        // Set up click listeners
        setupClickListeners();

        // Fetch profile data
        fetchProfileData();

        return view;
    }

    private void setupNavigationDrawer() {
        menuButton.setOnClickListener(v -> drawerLayout.open());

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_edit_profile) {
                // Handle edit profile
                Toast.makeText(getContext(), "Edit Profile clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_settings) {
                // Handle settings
                Toast.makeText(getContext(), "Settings clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_privacy) {
                // Handle privacy
                Toast.makeText(getContext(), "Privacy clicked", Toast.LENGTH_SHORT).show();
            }
      /*      else if (id == R.id.nav_account) {
                // Handle privacy

                Intent intent = new Intent(getActivity(), UpdateProfileActivity.class);
                intent.putExtra("first_name", firstNameText != null ? firstNameText.getText().toString() : "");
                intent.putExtra("middle_name", middleNameText != null ? middleNameText.getText().toString() : "");
                intent.putExtra("last_name", lastNameText != null ? lastNameText.getText().toString() : "");
                intent.putExtra("anonymous_name", anonymousNameText != null ? anonymousNameText.getText().toString() : "");
                intent.putExtra("age", ageText != null ? ageText.getText().toString() : "");
                intent.putExtra("phone_no", phoneText != null ? phoneText.getText().toString() : "");
                intent.putExtra("country", countryText != null ? countryText.getText().toString() : "");
                intent.putExtra("city", cityText != null ? cityText.getText().toString() : "");
                intent.putExtra("postal_code", postalCodeText != null ? postalCodeText.getText().toString() : "");
                startActivity(intent);         }
      */      else if (id == R.id.nav_delete_profile) {
                // Handle delete profile
                showDeleteProfileConfirmation();
            } else if (id == R.id.nav_logout) {
                // Handle logout
                logout();
            }

            drawerLayout.closeDrawers();
            return true;
        });

        updateNavigationHeader();
    }

    private void setupClickListeners() {
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UpdateProfileActivity.class);
            intent.putExtra("first_name", firstNameText != null ? firstNameText.getText().toString() : "");
            intent.putExtra("middle_name", middleNameText != null ? middleNameText.getText().toString() : "");
            intent.putExtra("last_name", lastNameText != null ? lastNameText.getText().toString() : "");
            intent.putExtra("anonymous_name", anonymousNameText != null ? anonymousNameText.getText().toString() : "");
            intent.putExtra("age", ageText != null ? ageText.getText().toString() : "");
            intent.putExtra("phone_no", phoneText != null ? phoneText.getText().toString() : "");
            intent.putExtra("country", countryText != null ? countryText.getText().toString() : "");
            intent.putExtra("city", cityText != null ? cityText.getText().toString() : "");
            intent.putExtra("postal_code", postalCodeText != null ? postalCodeText.getText().toString() : "");
            startActivity(intent);
        });
    }

    private void showLoading(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                // Also disable/enable UI elements while loading
                editProfileButton.setEnabled(!show);
                menuButton.setEnabled(!show);
                if (show) {
                    progressBar.bringToFront(); // Ensure progress bar is on top
                }
            });
        }
    }

    private void fetchProfileData() {
        showLoading(true);
        SharedPreferences prefs = getActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        int uaid = prefs.getInt("uaid", -1);
        if (uaid == -1) {
            showLoading(false);
            Toast.makeText(getContext(), "User session not found. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = BASE_URL + "/get-user/" + uaid;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                FragmentActivity activity = getActivity();
                if (activity != null && isAdded()) {
                    activity.runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "Failed to fetch profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    FragmentActivity activity = getActivity();
                    if (activity != null && isAdded()) {
                        activity.runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(getContext(), "Failed to fetch profile data: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);

                    if (jsonResponse.getString("status").equals("success")) {
                        JSONObject user = jsonResponse.getJSONObject("user");
                        FragmentActivity activity = getActivity();
                        if (activity != null && isAdded()) {
                            activity.runOnUiThread(() -> {
                                try {
                                    setTextOrHide(usernameText, user.optString("username", null));
                                    // Email visibility
                                    String emailVisibility = user.optString("email_visibility", "visible");
                                    String email = user.optString("email", null);
                                    if (!emailVisibility.equalsIgnoreCase("visible")) {
                                        emailText.setVisibility(View.GONE);
                                    } else {
                                        setTextOrHide(emailText, email);
                                    }
                                    // Username visibility
                                    String usernameVisibility = user.optString("username_visibility", "visible");
                                    String username = user.optString("username", null);
                                    if (!usernameVisibility.equalsIgnoreCase("visible")) {
                                        usernameText.setVisibility(View.GONE);
                                    } else {
                                        setTextOrHide(usernameText, username);
                                    }
                                    // Always show full name (no visibility check)
                                    String firstName = user.optString("first_name", null);
                                    String middleName = user.optString("middle_name", null);
                                    String lastName = user.optString("last_name", null);
                                    StringBuilder fullNameBuilder = new StringBuilder();
                                    if (firstName != null && !firstName.trim().isEmpty() && !firstName.equalsIgnoreCase("null")) {
                                        fullNameBuilder.append(firstName.trim());
                                    }
                                    if (middleName != null && !middleName.trim().isEmpty() && !middleName.equalsIgnoreCase("null")) {
                                        if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                                        fullNameBuilder.append(middleName.trim());
                                    }
                                    if (lastName != null && !lastName.trim().isEmpty() && !lastName.equalsIgnoreCase("null")) {
                                        if (fullNameBuilder.length() > 0) fullNameBuilder.append(" ");
                                        fullNameBuilder.append(lastName.trim());
                                    }
                                    String fullName = fullNameBuilder.length() > 0 ? fullNameBuilder.toString() : null;
                                    setTextOrHide(anonymousNameText, user.optString("anonymous_name", null));
                                    setTextOrHide(ageText, String.valueOf(user.optInt("age", 0)));
                                    setTextOrHide(countryText, user.optString("country", null));
                                    setTextOrHide(cityText, user.optString("city", null));
                                    setTextOrHide(postalCodeText, user.optString("postal_code", null));
                                    setTextOrHide(firstNameText, user.optString("first_name", null));
                                    setTextOrHide(middleNameText, user.optString("middle_name", null));
                                    setTextOrHide(lastNameText, user.optString("last_name", null));

                                    // Profile Image
                                    String profileVisibility = user.optString("profile_visibility", "visible");
                                    String profileImageBase64 = "";
                                    if (profileVisibility.equalsIgnoreCase("visible")) {
                                        profileImageBase64 = user.optString("real_image", "");
                                    } else {
                                        profileImageBase64 = user.optString("hide_image", "");
                                    }

                                    if (!profileImageBase64.isEmpty() && !profileImageBase64.equals("null")) {
                                        byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        profileImage.setImageBitmap(decodedByte);
                                        profileImage.setVisibility(View.VISIBLE);

                                        // Drawer header with Glide (circular)
                                        View headerView = navigationView.getHeaderView(0);
                                        ImageView navProfileImage = headerView.findViewById(R.id.nav_header_image);
                                        Glide.with(getContext())
                                                .load(decodedByte)
                                                .circleCrop()
                                                .into(navProfileImage);
                                    } else {
                                        profileImage.setVisibility(View.GONE);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(getContext(), "Error updating UI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                } finally {
                                    showLoading(false);
                                }
                            });
                        }
                    } else {
                        FragmentActivity activity = getActivity();
                        if (activity != null && isAdded()) {
                            activity.runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(getContext(), jsonResponse.optString("message", "Profile not found"), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    FragmentActivity activity = getActivity();
                    if (activity != null && isAdded()) {
                        activity.runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(getContext(), "Error parsing profile data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    private void setTextOrHide(TextView textView, String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(value);
            textView.setVisibility(View.VISIBLE);
        }
    }

    private void showDeleteProfileConfirmation() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProfile() {
        SharedPreferences prefs = getActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        int uaid = prefs.getInt("uaid", -1);
        if (uaid == -1) {
            Toast.makeText(getContext(), "User session not found. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a loading indicator
        AlertDialog loadingDialog = new AlertDialog.Builder(getContext())
                .setMessage("Deleting profile...")
                .setCancelable(false)
                .show();

        String url = BASE_URL + "/delete-profile/" + uaid;

        // Create PUT request instead of DELETE
        Request request = new Request.Builder()
                .url(url)
                .put(okhttp3.RequestBody.create(null, new byte[0])) // Empty request body for PUT
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                FragmentActivity activity = getActivity();
                if (activity != null && isAdded()) {
                    activity.runOnUiThread(() -> {
                        loadingDialog.dismiss();
                        Toast.makeText(getContext(), "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                FragmentActivity activity = getActivity();
                if (activity == null || !isAdded()) {
                    return;
                }

                final boolean isSuccessful = response.isSuccessful(); // HTTP 200-299
                final String responseBody = response.body() != null ? response.body().string() : "No response";

                activity.runOnUiThread(() -> {
                    loadingDialog.dismiss();

                    // Log the actual response for debugging
                    System.out.println("Delete Profile Response: " + responseBody);
                    System.out.println("Response Code: " + response.code());

                    // First, try to handle it as JSON
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        // Look for any success indicator
                        boolean success = false;
                        if (json.has("success")) {
                            success = json.getBoolean("success");
                        } else if (json.has("status")) {
                            success = json.getString("status").equalsIgnoreCase("success");
                        } else {
                            // Default to HTTP success if no explicit indicator
                            success = isSuccessful;
                        }

                        String message = json.optString("message", success ?
                                "Profile deleted successfully" : "Failed to delete profile");

                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                        if (success) {
                            logout();
                        }
                    }
                    // If it's not valid JSON or any other error occurs, fall back to HTTP status
                    catch (Exception e) {
                        e.printStackTrace();
                        // Just use HTTP status as fallback
                        if (isSuccessful) {
                            Toast.makeText(getContext(), "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                            logout();
                        } else {
                            Toast.makeText(getContext(),
                                    "Server error (HTTP " + response.code() + "): " + responseBody, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void updateNavigationHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView usernameText = headerView.findViewById(R.id.nav_header_username);
        TextView emailText = headerView.findViewById(R.id.nav_header_email);
        ImageView navProfileImage = headerView.findViewById(R.id.nav_header_image);

        SharedPreferences prefs = getActivity().getSharedPreferences(USER_SESSION, Context.MODE_PRIVATE);
        String username = prefs.getString("username", "User");
        String email = prefs.getString("email", "user@example.com");
        String profileImageBase64 = prefs.getString("profile_image_base64", "");

        usernameText.setText(username);
        emailText.setText(email);

        if (!profileImageBase64.isEmpty()) {
            byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            navProfileImage.setImageBitmap(decodedByte);
        }
    }

    private void logout() {
        // Clear UserSession
        SharedPreferences sharedPreferences1 = getActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor1 = sharedPreferences1.edit();
        editor1.clear();
        editor1.apply();

        // Clear user_session
        SharedPreferences sharedPreferences2 = getActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor2 = sharedPreferences2.edit();
        editor2.clear();
        editor2.apply();

        // Navigate to LoginActivity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void loadRecentActivities() {
        progressBarActivities.setVisibility(View.VISIBLE);
        noActivityText.setVisibility(View.GONE);
        rvActivities.setVisibility(View.GONE);
        SharedPreferences prefs = getActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        int uaid = prefs.getInt("uaid", -1);
        if (uaid == -1) {
            progressBarActivities.setVisibility(View.GONE);
            noActivityText.setText("User not found");
            noActivityText.setVisibility(View.VISIBLE);
            return;
        }
        String url = BASE_URL + "/activity/" + uaid;
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    progressBarActivities.setVisibility(View.GONE);
                    noActivityText.setText("Failed to load activities");
                    noActivityText.setVisibility(View.VISIBLE);
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        progressBarActivities.setVisibility(View.GONE);
                        noActivityText.setText("Failed to load activities");
                        noActivityText.setVisibility(View.VISIBLE);
                    });
                    return;
                }
                String responseBody = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseBody);
                    if (json.getBoolean("success")) {
                        List<ActivityItem> newList = new ArrayList<>();
                        for (int i = 0; i < json.getJSONArray("activity").length(); i++) {
                            JSONObject act = json.getJSONArray("activity").getJSONObject(i);
                            newList.add(new ActivityItem(
                                    act.optString("message", ""),
                                    act.optString("reference_id", ""),
                                    act.optString("type", "")
                            ));
                        }
                        if (getActivity() != null) getActivity().runOnUiThread(() -> {
                            progressBarActivities.setVisibility(View.GONE);
                            if (newList.isEmpty()) {
                                noActivityText.setText("No recent activity");
                                noActivityText.setVisibility(View.VISIBLE);
                                rvActivities.setVisibility(View.GONE);
                            } else {
                                activityAdapter.setActivities(newList);
                                rvActivities.setVisibility(View.VISIBLE);
                                noActivityText.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        if (getActivity() != null) getActivity().runOnUiThread(() -> {
                            progressBarActivities.setVisibility(View.GONE);
                            noActivityText.setText("No recent activity");
                            noActivityText.setVisibility(View.VISIBLE);
                            rvActivities.setVisibility(View.GONE);
                        });
                    }
                } catch (Exception e) {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        progressBarActivities.setVisibility(View.GONE);
                        noActivityText.setText("Error loading activity");
                        noActivityText.setVisibility(View.VISIBLE);
                        rvActivities.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private static class ActivityItem {
        String message;
        String referenceId;
        String type;
        ActivityItem(String message, String referenceId, String type) {
            this.message = message;
            this.referenceId = referenceId;
            this.type = type;
        }
        public String getMessage() { return message; }
        public String getReferenceId() { return referenceId; }
        public String getType() { return type; }
    }

    private class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {
        private List<ActivityItem> activities;
        ActivityAdapter(List<ActivityItem> activities) { this.activities = activities; }
        @NonNull
        @Override
        public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ActivityViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
            ActivityItem item = activities.get(position);
            holder.textView.setText(item.getMessage());
        }
        @Override
        public int getItemCount() { return activities.size(); }
        public void setActivities(List<ActivityItem> newActivities) {
            this.activities = newActivities;
            notifyDataSetChanged();
        }
        class ActivityViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ActivityViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}