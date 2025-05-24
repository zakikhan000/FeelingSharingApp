package com.example.shadowchatapp2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import AppModules.Adapters.MessageAdapter;
import AppModules.Models.Message;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChatBotActivity extends AppCompatActivity {
    private static final String GEMINI_API_KEY = "AIzaSyD4GlI1BKX7KRhHEbfgrRCgLcFZowmE7LM";
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private EditText messageInput;


    private ImageButton sendButton;
    private TextView typingIndicator;
    private Handler handler = new Handler(Looper.getMainLooper());
    private OkHttpClient client;
    private List<Message> messageHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat_bot);


        ImageView backButton = findViewById(R.id.iv_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to MainActivity
                finish();
            }
        });
        // Initialize views
        // Initialize OkHttpClient with timeout
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        messageHistory = new ArrayList<>();

        // Initialize views
        recyclerView = findViewById(R.id.rv_messages);
        messageInput = findViewById(R.id.et_message);
        sendButton = findViewById(R.id.btn_send);
        typingIndicator = findViewById(R.id.tv_typing);

        // Set up the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter();
        recyclerView.setAdapter(messageAdapter);

        // Add welcome message from bot
        welcomeMessage();

        // Set up send button click listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageInput.getText().toString().trim();
                if (!messageText.isEmpty()) {
                    sendMessage(messageText);
                }
            }
        });
    }

    private void sendMessage(String userMessage) {
        // Add user message to UI
        Message userMsg = new Message(userMessage, Message.TYPE_USER);
        messageAdapter.addMessage(userMsg);
        messageHistory.add(userMsg);
        scrollToBottom();

        // Clear input
        messageInput.setText("");

        // Show typing indicator
        showTypingIndicator(true);

        try {
            // Prepare the request body
            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", userMessage);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 1024);
            requestBody.put("generationConfig", generationConfig);


            // Add safety settings
            JSONArray safetySettings = new JSONArray();
            JSONObject safetySetting = new JSONObject();
            safetySetting.put("category", "HARM_CATEGORY_HARASSMENT");
            safetySetting.put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
            safetySettings.put(safetySetting);
            requestBody.put("safetySettings", safetySettings);

            String url = GEMINI_API_URL + "?key=" + GEMINI_API_KEY;
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    handler.post(() -> {
                        showTypingIndicator(false);
                        Toast.makeText(ChatBotActivity.this,
                                "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            JSONArray candidates = jsonResponse.getJSONArray("candidates");
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject content = candidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            JSONObject part = parts.getJSONObject(0);
                            String botResponse = part.getString("text");

                            handler.post(() -> {
                                showTypingIndicator(false);
                                Message botMsg = new Message(botResponse, Message.TYPE_BOT);
                                messageAdapter.addMessage(botMsg);
                                messageHistory.add(botMsg);
                                scrollToBottom();
                            });
                        } catch (Exception e) {
                            handler.post(() -> {
                                showTypingIndicator(false);
                                Toast.makeText(ChatBotActivity.this,
                                        "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        handler.post(() -> {
                            showTypingIndicator(false);
                            Toast.makeText(ChatBotActivity.this,
                                    "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            showTypingIndicator(false);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void welcomeMessage() {
        handler.postDelayed(() -> {
            Message welcomeMsg = new Message(
                    "Hello! I'm your shadowTalk AI assistant. How can I help you today?",
                    Message.TYPE_BOT
            );
            messageAdapter.addMessage(welcomeMsg);
            messageHistory.add(welcomeMsg);
            scrollToBottom();
        }, 500);
    }

    private void showTypingIndicator(boolean show) {
        typingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void scrollToBottom() {
        if (messageAdapter.getItemCount() > 0) {
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
        }
    }
}

