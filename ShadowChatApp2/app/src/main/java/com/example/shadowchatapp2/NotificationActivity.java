package com.example.shadowchatapp2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import AppModules.Adapters.NotificationAdapter;
import AppModules.Models.Notification;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ProgressBar progressBar;
    private OkHttpClient client;
    private int currentUserUAID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Initialize UI components
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Notifications");

        recyclerView = findViewById(R.id.notification_recycler_view);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize OkHttpClient
        client = new OkHttpClient();

        // Get current user's UAID
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        currentUserUAID = prefs.getInt("uaid", -1);

        if (currentUserUAID == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(new ArrayList<>(), new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onNotificationClick(Notification notification) {
                handleNotificationClick(notification);
            }
        });
        recyclerView.setAdapter(adapter);

        // Load notifications
        loadNotifications();

        // Mark all notifications as read after 10 seconds
        new Handler(Looper.getMainLooper()).postDelayed(this::markAllNotificationsAsRead, 5000);
    }

    private void handleNotificationClick(Notification notification) {
        // Mark the clicked notification as read
        markNotificationAsRead(notification.getNotificationId());

        Intent intent;
        switch (notification.getType()) {
            case "message":
                // Open chat with the sender
                intent = new Intent(this, MessageActivity.class);
                intent.putExtra("sender_uaid", notification.getSenderUaid());
                startActivity(intent);
                break;
            case "comment":
                // Open post with the comment
                intent = new Intent(this, CommentActivity.class);
                intent.putExtra("post_id", notification.getReferenceId());
                startActivity(intent);
                break;
            case "like":
                // Open the liked post
                intent = new Intent(this, MessageActivity.class);
                intent.putExtra("post_id", notification.getReferenceId());
                startActivity(intent);
                break;
            default:
                Toast.makeText(this, "Unknown notification type", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        String url = "https://shadow-talk-server.vercel.app/api/notifications/" + currentUserUAID;

        Log.d("NotificationActivity", "Fetching notifications from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NotificationActivity", "Network error: " + e.getMessage());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(NotificationActivity.this,
                            "Failed to load notifications", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("NotificationActivity", "Raw API Response: " + responseBody);

                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    Log.d("NotificationActivity", "Success status: " + jsonResponse.getBoolean("success"));

                    if (jsonResponse.getBoolean("success")) {
                        List<Notification> allNotifications = new ArrayList<>();

                        // Log unread notifications
                        JSONArray unreadArray = jsonResponse.getJSONArray("unread");
                        Log.d("NotificationActivity", "Unread notifications count: " + unreadArray.length());
                        for (int i = 0; i < unreadArray.length(); i++) {
                            JSONObject notificationJson = unreadArray.getJSONObject(i);
                            Log.d("NotificationActivity", "Unread notification " + i + ": " + notificationJson.toString());
                            Notification notification = parseNotification(notificationJson);
                            allNotifications.add(notification);
                        }

                        // Log read notifications
                        JSONArray readArray = jsonResponse.getJSONArray("read");
                        Log.d("NotificationActivity", "Read notifications count: " + readArray.length());
                        for (int i = 0; i < readArray.length(); i++) {
                            JSONObject notificationJson = readArray.getJSONObject(i);
                            Log.d("NotificationActivity", "Read notification " + i + ": " + notificationJson.toString());
                            Notification notification = parseNotification(notificationJson);
                            allNotifications.add(notification);
                        }

                        Log.d("NotificationActivity", "Total notifications loaded: " + allNotifications.size());

                        runOnUiThread(() -> {
                            adapter.updateNotifications(allNotifications);
                            progressBar.setVisibility(View.GONE);
                        });
                    } else {
                        String errorMessage = jsonResponse.optString("message", "Unknown error");
                        Log.e("NotificationActivity", "API Error: " + errorMessage);
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(NotificationActivity.this,
                                    "Failed to load notifications: " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (JSONException e) {
                    Log.e("NotificationActivity", "JSON parsing error: " + e.getMessage());
                    Log.e("NotificationActivity", "Error response: " + responseBody);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(NotificationActivity.this,
                                "Error parsing notifications", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private Notification parseNotification(JSONObject json) throws JSONException {
        String referenceIdStr = json.isNull("ReferenceID") ? null : json.getString("ReferenceID");
        int referenceId = referenceIdStr != null ? Integer.parseInt(referenceIdStr) : -1;

        return new Notification(
                json.getInt("NotificationID"),
                json.getInt("UAID"),
                json.getInt("Sender_UAID"),
                json.getString("Type"),
                referenceId,
                json.getString("Message"),
                json.getInt("IsRead") == 1,
                json.getString("Created_At")
        );
    }

    private void markNotificationAsRead(int notificationId) {
        String url = "https://shadow-talk-server.vercel.app/api/notifications/mark-read/" + currentUserUAID;
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("notificationId", notificationId);
        } catch (JSONException e) {
            Log.e("NotificationActivity", "Error creating JSON body: " + e.getMessage());
            return;
        }
        String jsonString = jsonBody.toString();
        Log.d("NotificationActivity", "Request body: " + jsonString);
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonString);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NotificationActivity", "Failed to mark notification as read: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("NotificationActivity", "Response: " + responseBody);
                Log.d("NotificationActivity", "Notification marked as read");
                // Optionally reload notifications to update the UI
                runOnUiThread(() -> loadNotifications());
            }
        });
    }

    private void markAllNotificationsAsRead() {
        String url = "https://shadow-talk-server.vercel.app/api/notifications/mark-read/" + currentUserUAID;
        Request request = new Request.Builder()
                .url(url)
                .put(RequestBody.create(null, new byte[0])) // Empty POST
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("NotificationActivity", "Failed to mark notifications as read: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("NotificationActivity", "All notifications marked as read");
                runOnUiThread(() -> loadNotifications());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}