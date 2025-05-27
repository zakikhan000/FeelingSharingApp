package com.example.shadowchatapp2.Notifications;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.shadowchatapp2.AiChatBot.ChatBotActivity;
import com.example.shadowchatapp2.AuthenticationaAndUpdates.LoginActivity;
import com.example.shadowchatapp2.PostManagement.CreatePostActivity;
import com.example.shadowchatapp2.R;
import com.example.shadowchatapp2.UserProfile.SearchActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import AppModules.Fragments.ChatFragment;
import AppModules.Fragments.ExploreFragment;
import AppModules.Fragments.HomeFragment;
import AppModules.Fragments.ProfileFragment;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.OkHttpClient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    FrameLayout fragmentContainer;

    private FloatingActionButton fabCreate;
    private int currentUserUAID;
    ExtendedFloatingActionButton fabChatAI;

    ImageView search_icon;

    private Toolbar toolbar;

    private TextView notificationBadge;

    private OkHttpClient client;

    private BroadcastReceiver notificationBadgeReceiver;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new OkHttpClient();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentContainer = findViewById(R.id.fragment_container);
        search_icon = findViewById(R.id.search_icon);
        fabChatAI = findViewById(R.id.fab_chat_ai);
        fabCreate = findViewById(R.id.fab_create_post);
        toolbar = findViewById(R.id.toolbar);

        notificationBadge = findViewById(R.id.notification_badge);

        ImageView notificationIcon = findViewById(R.id.notification_icon);

        search_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });

        notificationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
                startActivity(intent);
            }
        });

        currentUserUAID = getIntent().getIntExtra("UAID", -1);

        if (currentUserUAID == -1) {
            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
            currentUserUAID = prefs.getInt("uaid", -1);
        }

        if (currentUserUAID == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        loadFragment(new HomeFragment());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new HomeFragment());
                showCreateAndChatButtons();
                showToolbar();
                return true;
            } else if (itemId == R.id.nav_chat) {
                loadFragment(new ChatFragment());
                hideCreateAndChatButtons();
                hideToolbar();
                return true;
            } else if (itemId == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                hideCreateAndChatButtons();
                showToolbar();
                return true;
            }
            else if (itemId == R.id.nav_explore) {
                loadFragment(new ExploreFragment());
                hideCreateAndChatButtons();
                showToolbar();
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            showCreateAndChatButtons();
            showToolbar();
        }

        fabCreate.setOnClickListener(v -> {
            Intent createPostIntent = new Intent(MainActivity.this, CreatePostActivity.class);
            createPostIntent.putExtra("uaid", currentUserUAID);
            startActivity(createPostIntent);
        });

        fabChatAI.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatBotActivity.class);
            startActivity(intent);
        });

        fetchUnreadNotificationCount();
        startNotificationRefresh();

        notificationBadgeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                fetchUnreadNotificationCount();
            }
        };
        registerReceiver(notificationBadgeReceiver, new IntentFilter("com.example.shadowchatapp2.REFRESH_NOTIFICATION_BADGE"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationBadgeReceiver != null) {
            unregisterReceiver(notificationBadgeReceiver);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void showCreateAndChatButtons() {
        fabCreate.setVisibility(View.VISIBLE);
        fabChatAI.setVisibility(View.VISIBLE);
    }

    private void hideCreateAndChatButtons() {
        fabCreate.setVisibility(View.GONE);
        fabChatAI.setVisibility(View.GONE);
    }

    private void showToolbar() {
        toolbar.setVisibility(View.VISIBLE);
    }

    private void hideToolbar() {
        toolbar.setVisibility(View.GONE);
    }

    private void fetchUnreadNotificationCount() {
        String url = "https://shadow-talk-server.vercel.app/api/notifications/unread-count/" + currentUserUAID;
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MainActivity", "Failed to fetch unread count: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (jsonResponse.getBoolean("success")) {
                        int unreadCount = jsonResponse.getInt("unreadCount");
                        runOnUiThread(() -> updateNotificationBadge(unreadCount));
                    }
                } catch (JSONException e) {
                    Log.e("MainActivity", "Error parsing unread count response: " + e.getMessage());
                }
            }
        });
    }

    private void updateNotificationBadge(int count) {
        ImageView notificationIcon = findViewById(R.id.notification_icon);
        if (count > 0) {
            notificationBadge.setVisibility(View.VISIBLE);
            notificationBadge.setText(String.valueOf(count));
            notificationIcon.setAlpha(1.0f); // Full opacity when there are notifications
        } else {
            notificationBadge.setVisibility(View.GONE);
            notificationIcon.setAlpha(0.6f); // Dimmed when no notifications
        }
    }

    private void startNotificationRefresh() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchUnreadNotificationCount();
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(refreshRunnable);
    }
}




