package AppModules.Fragments;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import AppModules.Models.ChatUser;
import com.example.shadowchatapp2.AuthenticationaAndUpdates.LoginActivity;
import com.example.shadowchatapp2.UserMessages.MessageActivity;
import com.example.shadowchatapp2.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private static final String BASE_URL = "https://shadowtalk-omega.vercel.app/api";
    private static final int POLL_INTERVAL = 5000; // 1 second

    private EditText searchInput;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView noUsersText;
    private ChatAdapter chatAdapter;
    private List<ChatUser> userList;
    private OkHttpClient client;
    private int currentUserUaid; // Store current user ID here
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private Handler handler;
    private Runnable messagePoller;
    private boolean isPolling = false;
    private boolean isInitialLoad = true;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Get current user UAID from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        currentUserUaid = prefs.getInt("uaid", -1);

        if (currentUserUaid == -1) {
            // If no user is logged in, redirect to login
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
            return view;
        }

        // Initialize views
        searchInput = view.findViewById(R.id.et_search_users);
        recyclerView = view.findViewById(R.id.recycler_users);
        progressBar = view.findViewById(R.id.progress_bar);
        noUsersText = view.findViewById(R.id.tv_no_users);

        // Initialize data
        userList = new ArrayList<>();
        chatAdapter = new ChatAdapter(userList);
        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(chatAdapter);

        // Setup search input
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (!query.isEmpty()) {
                    searchUsers(query);
                } else {
                    fetchAllUsers();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Initialize handler and poller
        handler = new Handler();
        messagePoller = new Runnable() {
            @Override
            public void run() {
                fetchAllUsers();
                handler.postDelayed(this, POLL_INTERVAL);
            }
        };

        // Fetch all users initially
        fetchAllUsers();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reset initial load flag when fragment becomes visible
        isInitialLoad = true;
        // Start polling when fragment becomes visible
        if (!isPolling) {
            startPolling();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop polling when fragment is not visible
        stopPolling();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up handler
        stopPolling();
        handler = null;
    }

    private void startPolling() {
        if (handler == null) {
            handler = new Handler();
        }
        isPolling = true;
        messagePoller = new Runnable() {
            @Override
            public void run() {
                if (isPolling) {
                    fetchAllUsers();
                    handler.postDelayed(this, POLL_INTERVAL);
                }
            }
        };
        handler.post(messagePoller);
        Log.d(TAG, "Started polling for messages");
    }

    private void stopPolling() {
        isPolling = false;
        if (handler != null && messagePoller != null) {
            handler.removeCallbacks(messagePoller);
            Log.d(TAG, "Stopped polling for messages");
        }
    }

    private void fetchAllUsers() {
        // Only show loading on initial load
        if (isInitialLoad) {
            showLoading(true);
        }
        noUsersText.setVisibility(View.GONE);

        String url = "https://shadow-talk-server.vercel.app/api/get-all-user/" + currentUserUaid;
        Log.d(TAG, "Fetching users from URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isInitialLoad) {
                            showLoading(false);
                        }
                        Toast.makeText(getContext(), "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!isPolling) return; // Don't process response if polling is stopped

                String responseBody = response.body().string();
                Log.d(TAG, "Response: " + responseBody);

                if (!response.isSuccessful()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isInitialLoad) {
                                showLoading(false);
                            }
                            Toast.makeText(getContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }

                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.getBoolean("success")) {
                        JSONArray usersArray = jsonResponse.getJSONArray("users");
                        List<ChatUser> users = new ArrayList<>();
                        for (int i = 0; i < usersArray.length(); i++) {
                            JSONObject userObj = usersArray.getJSONObject(i);
                            ChatUser user = new ChatUser();
                            user.setId(String.valueOf(userObj.optInt("uaid")));
                            user.setName(userObj.optString("username", ""));
                            user.setProfileImage(userObj.optString("profileImage", null));
                            user.setLastMessage(userObj.optString("lastMessage", null));
                            user.setLastAction(userObj.optString("lastAction", ""));
                            int unseenCount = userObj.optInt("unseenCount", 0);
                            user.setUnseenCount(unseenCount);
                            Log.d(TAG, "User: " + user.getName() + " Unseen Count: " + unseenCount);
                            user.setLastMessageTime(userObj.optString("lastMessageTime", null));
                            users.add(user);
                        }

                        if (getActivity() != null && isPolling) {
                            getActivity().runOnUiThread(() -> {
                                // Update the userList with new data
                                userList.clear();
                                userList.addAll(users);
                                chatAdapter.updateUsers(users);
                                if (isInitialLoad) {
                                    showLoading(false);
                                    isInitialLoad = false;
                                }
                                updateUI();
                            });
                        }
                    } else {
                        String errorMessage = jsonResponse.optString("message", "No users found");
                        Log.d(TAG, "Error response: " + errorMessage);
                        if (getActivity() != null && isPolling) {
                            getActivity().runOnUiThread(() -> {
                                if (isInitialLoad) {
                                    showLoading(false);
                                    isInitialLoad = false;
                                }
                                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                                userList.clear();
                                chatAdapter.updateUsers(new ArrayList<>());
                                updateUI();
                            });
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing response", e);
                    if (getActivity() != null && isPolling) {
                        getActivity().runOnUiThread(() -> {
                            if (isInitialLoad) {
                                showLoading(false);
                                isInitialLoad = false;
                            }
                            Toast.makeText(getContext(), "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    private void searchUsers(String query) {
        showLoading(true);
        noUsersText.setVisibility(View.GONE);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("search_query", query);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        String url = BASE_URL + "/users/search";
        Log.d(TAG, "Searching users from URL: " + url);
        Log.d(TAG, "Search query: " + query);

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(getContext(), "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response body: " + responseBody);

                if (!response.isSuccessful()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(getContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                    return;
                }

                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    Log.d(TAG, "Parsed JSON response: " + jsonResponse.toString());

                    if (jsonResponse.getBoolean("success")) {
                        JSONArray usersArray = jsonResponse.getJSONArray("users");
                        Log.d(TAG, "Number of users found: " + usersArray.length());

                        Type listType = new TypeToken<List<ChatUser>>(){}.getType();
                        List<ChatUser> users = new Gson().fromJson(usersArray.toString(), listType);
                        Log.d(TAG, "Parsed users: " + users.size());

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                chatAdapter.updateUsers(users);
                                showLoading(false);
                                updateUI();
                            });
                        }
                    } else {
                        String errorMessage = jsonResponse.optString("message", "No users found");
                        Log.d(TAG, "Error response: " + errorMessage);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                                chatAdapter.updateUsers(new ArrayList<>());
                                updateUI();
                            });
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing response", e);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(getContext(), "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }



    private void showLoading(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            });
        }
    }

    private void updateUI() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (userList.isEmpty()) {
                    noUsersText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noUsersText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.UserViewHolder> {
        private List<ChatUser> users;

        public ChatAdapter(List<ChatUser> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            ChatUser user = users.get(position);
            holder.usernameTextView.setText(user.getName());

            // Set last message with proper formatting
            String lastMessage = user.getLastMessage();
            if (lastMessage != null && !lastMessage.isEmpty()) {
                holder.lastMessageTextView.setVisibility(View.VISIBLE);
                holder.lastMessageTextView.setText(lastMessage);

                // Add "New" indicator if message is unseen
                if ("unseen".equals(user.getLastAction())) {
                    holder.lastMessageTextView.setTypeface(null, android.graphics.Typeface.BOLD);
                    holder.lastMessageTextView.setTextColor(getResources().getColor(android.R.color.black));
                } else {
                    holder.lastMessageTextView.setTypeface(null, android.graphics.Typeface.NORMAL);
                    holder.lastMessageTextView.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }
            } else {
                holder.lastMessageTextView.setVisibility(View.GONE);
            }

            // Set last message time
            String lastMessageTime = user.getLastMessageTime();
            if (lastMessageTime != null && !lastMessageTime.isEmpty()) {
                holder.lastMessageTimeTextView.setVisibility(View.VISIBLE);
                try {
                    // Parse the ISO datetime string
                    java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    java.util.Date date = inputFormat.parse(lastMessageTime);

                    // Format for display
                    java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
                    String formattedTime = outputFormat.format(date);
                    holder.lastMessageTimeTextView.setText(formattedTime);
                } catch (Exception e) {
                    holder.lastMessageTimeTextView.setText(lastMessageTime);
                }
            } else {
                holder.lastMessageTimeTextView.setVisibility(View.GONE);
            }

            // Show unseen count badge if there are unseen messages
            int unseenCount = user.getUnseenCount();
            Log.d(TAG, "Binding user: " + user.getName() + " with unseen count: " + unseenCount);

            if (unseenCount > 0) {
                holder.unseenCountTextView.setText(String.valueOf(unseenCount));
                holder.unseenCountTextView.setVisibility(View.VISIBLE);
                // Make the badge more prominent
                holder.unseenCountTextView.setBackgroundResource(R.drawable.badge_background);
                holder.unseenCountTextView.setTextColor(getResources().getColor(android.R.color.white));
                Log.d(TAG, "Showing badge with count: " + unseenCount);
            } else {
                holder.unseenCountTextView.setVisibility(View.GONE);
                Log.d(TAG, "Hiding badge for user: " + user.getName());
            }

            // Load profile image
            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                String base64 = user.getProfileImage();
                // Remove data URL prefix if present
                if (base64.startsWith("data:image")) {
                    base64 = base64.substring(base64.indexOf(",") + 1);
                }
                try {
                    byte[] decodedString = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    com.bumptech.glide.Glide.with(holder.itemView.getContext())
                            .load(bitmap)
                            .circleCrop()
                            .error(R.drawable.default_profile)
                            .into(holder.profileImageView);
                } catch (Exception e) {
                    holder.profileImageView.setImageResource(R.drawable.default_profile);
                }
            } else {
                holder.profileImageView.setImageResource(R.drawable.default_profile);
            }

            // Update the click listener to pass the current user's UAID
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), MessageActivity.class);
                intent.putExtra("uaid", String.valueOf(user.getId()));
                intent.putExtra("user_name", user.getName());
                intent.putExtra("user_avatar", user.getProfileImage());
                intent.putExtra("current_uaid", String.valueOf(getCurrentUserUaid()));
                startActivity(intent);
            });
        }

        private int getCurrentUserUaid() {
            return currentUserUaid;
        }
        @Override
        public int getItemCount() {
            return users.size();
        }

        public void updateUsers(List<ChatUser> newUsers) {
            this.users = newUsers;

            notifyDataSetChanged();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            ImageView profileImageView;
            TextView usernameTextView;
            TextView lastMessageTextView;
            TextView lastMessageTimeTextView;
            TextView unseenCountTextView;

            UserViewHolder(View itemView) {
                super(itemView);
                profileImageView = itemView.findViewById(R.id.iv_profile_pic);
                usernameTextView = itemView.findViewById(R.id.tv_username);
                lastMessageTextView = itemView.findViewById(R.id.tv_last_message);
                lastMessageTimeTextView = itemView.findViewById(R.id.tv_last_message_time);
                unseenCountTextView = itemView.findViewById(R.id.tv_unseen_count);
            }
        }
    }
}
