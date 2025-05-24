package com.example.shadowchatapp2;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import AppModules.Adapters.UserMessageAdapter;
import AppModules.Models.UserMessage;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MessageActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final String BASE_URL = "https://shadowtalk-omega.vercel.app/api";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int POLL_INTERVAL = 2000; // 3 seconds

    private ImageView ivBack, ivUserAvatar;
    private TextView tvUsername;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private UserMessageAdapter messageAdapter;
    private List<UserMessage> messageList;
    private OkHttpClient client;
    private String currentUserId;
    private String selectedUserId;
    private String selectedUserName;
    private String selectedUserAvatar;
    private Socket mSocket;
    private Handler handler;
    private Runnable messagePoller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_message);
        selectedUserId = getIntent().getStringExtra("uaid");
        selectedUserName = getIntent().getStringExtra("user_name");
        selectedUserAvatar = getIntent().getStringExtra("user_avatar");
        currentUserId = getIntent().getStringExtra("current_uaid");

        // Log the values to verify they're correct
        Log.d(TAG, "Selected User ID: " + selectedUserId);
        Log.d(TAG, "Current User ID: " + currentUserId);

        // Initialize views
        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        // Initialize handler and poller
        handler = new Handler();
        messagePoller = new Runnable() {
            @Override
            public void run() {
                loadMessages();
                handler.postDelayed(this, POLL_INTERVAL);
            }
        };
        // Load initial messages
        loadMessages();
        // Mark messages as seen
        markMessagesAsSeen();
        // --- SOCKET SETUP ---
        try {
            mSocket = IO.socket("https://shadow-talk-server.vercel.app"); // Use your socket server URL
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        mSocket.connect();
        mSocket.emit("join", currentUserId); // Join your own room
        mSocket.on("newMessage", onNewMessage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start polling when activity is visible
        handler.post(messagePoller);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop polling when activity is not visible
        handler.removeCallbacks(messagePoller);
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.iv_back);
        ivUserAvatar = findViewById(R.id.iv_user_avatar);
        tvUsername = findViewById(R.id.tv_username);
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        // Set user info
        tvUsername.setText(selectedUserName);
        if (selectedUserAvatar != null && !selectedUserAvatar.isEmpty()) {
            String base64 = selectedUserAvatar;
            // Remove data URL prefix if present
            if (base64.startsWith("data:image")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }
            try {
                byte[] decodedString = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                com.bumptech.glide.Glide.with(this)
                        .load(bitmap)
                        .circleCrop()
                        .error(R.drawable.default_profile)
                        .into(ivUserAvatar);
            } catch (Exception e) {
                ivUserAvatar.setImageResource(R.drawable.default_profile);
            }
        } else {
            ivUserAvatar.setImageResource(R.drawable.default_profile);
        }

        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String messageText = etMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(messageText)) {
                sendMessage(messageText);
                etMessage.setText("");
            }
        });
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new UserMessageAdapter(messageList, currentUserId);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messageAdapter);
    }

    private void sendMessage(String messageText) {
        try {
            Log.d(TAG, "Preparing to send message. Sender: " + currentUserId + ", Receiver: " + selectedUserId + ", Message: " + messageText);

            // Create JSON object for the message
            JSONObject data = new JSONObject();
            // Convert string IDs to integers
            int senderId = Integer.parseInt(currentUserId);
            int receiverId = Integer.parseInt(selectedUserId);

            data.put("uaid", senderId);
            data.put("s_id", receiverId);
            data.put("message_text", messageText);
            data.put("sentiment_score", 0.0);
            data.put("cb_id", JSONObject.NULL);  // Use JSONObject.NULL instead of null

            String requestBody = data.toString();
            Log.d(TAG, "Request data: " + requestBody);

            // First save to database through API
            String url = "https://shadowtalk-uebk.vercel.app/api/add-message";

            // Create OkHttpClient with longer timeouts
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            RequestBody body = RequestBody.create(
                    requestBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Log.d(TAG, "Sending request to: " + url);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network failure: " + e.getMessage());
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(MessageActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "API Response Code: " + response.code());
                    Log.d(TAG, "API Response Body: " + responseBody);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (jsonResponse.getBoolean("success")) {
                            Log.d(TAG, "Message saved successfully to database");
                            // Only emit through socket and update UI after successful database save
                            runOnUiThread(() -> {
                                try {
                                    // Socket emit with original format
                                    JSONObject socketData = new JSONObject();
                                    socketData.put("sender", currentUserId);
                                    socketData.put("receiver", selectedUserId);
                                    socketData.put("message", messageText);

                                    Log.d(TAG, "Emitting socket message: " + socketData.toString());
                                    mSocket.emit("sendMessage", socketData);

                                    // Update UI
                                    String datetime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                            .format(new Date());
                                    UserMessage userMessage = new UserMessage(currentUserId, selectedUserId, messageText, datetime);
                                    messageList.add(userMessage);
                                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                                    rvMessages.scrollToPosition(messageList.size() - 1);

                                    Toast.makeText(MessageActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
                                } catch (JSONException e) {
                                    Log.e(TAG, "Error creating socket data: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        } else {
                            String errorMessage = jsonResponse.optString("message", "Unknown error");
                            Log.e(TAG, "API returned error: " + errorMessage);
                            runOnUiThread(() -> {
                                Toast.makeText(MessageActivity.this, "Failed to send message: " + errorMessage, Toast.LENGTH_LONG).show();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage());
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(MessageActivity.this, "Error sending message: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });

        } catch (NumberFormatException e) {
            Log.e(TAG, "Error converting user IDs to numbers: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Invalid user ID format", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error in sendMessage: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error sending message: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(() -> {
                org.json.JSONObject data = (org.json.JSONObject) args[0];
                try {
                    String sender = data.getString("sender");
                    String receiver = data.getString("receiver");
                    String message = data.getString("message");
                    String datetime = data.getString("datetime");
                    // Only add to chat if this message is for this conversation
                    if ((sender.equals(currentUserId) && receiver.equals(selectedUserId)) ||
                            (sender.equals(selectedUserId) && receiver.equals(currentUserId))) {
                        UserMessage userMessage = new UserMessage(sender, receiver, message, datetime);
                        messageList.add(userMessage);
                        messageAdapter.notifyItemInserted(messageList.size() - 1);
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                }
            });
        }
    };

    private void loadMessages() {
        String url = "https://shadow-talk-server.vercel.app/api/get-messages"
                + "?uaid=" + currentUserId
                + "&sid=" + selectedUserId
                + "&limit=50&offset=0";
        Log.d(TAG, "Loading messages from URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MessageActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Response: " + responseBody);

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(MessageActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.getBoolean("success")) {
                        JSONArray messagesArray = jsonResponse.getJSONArray("messages");
                        List<UserMessage> messages = new ArrayList<>();

                        for (int i = 0; i < messagesArray.length(); i++) {
                            JSONObject messageObj = messagesArray.getJSONObject(i);
                            UserMessage message = new UserMessage(
                                    messageObj.getString("sender"),
                                    messageObj.getString("receiver"),
                                    messageObj.getString("message"),
                                    messageObj.getString("datetime")
                            );
                            messages.add(message);
                        }

                        runOnUiThread(() -> {
                            messageList.clear();
                            messageList.addAll(messages);
                            messageAdapter.notifyDataSetChanged();
                            if (!messageList.isEmpty()) {
                                rvMessages.scrollToPosition(messageList.size() - 1);
                            }
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing response", e);
                    runOnUiThread(() -> {
                        Toast.makeText(MessageActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void markMessagesAsSeen() {
        String url = "https://shadow-talk-server.vercel.app/api/receive-message";
        JSONObject json = new JSONObject();
        try {
            json.put("uaid", selectedUserId); // The sender of the messages you are viewing
            json.put("sid", currentUserId);   // The current user (receiver)
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to mark messages as seen: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "Messages marked as seen");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MessageActivity", "onDestroy called");
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off("newMessage", onNewMessage);
        }
        // Clean up handler
        if (handler != null) {
            handler.removeCallbacks(messagePoller);
            handler = null;
        }
    }
}