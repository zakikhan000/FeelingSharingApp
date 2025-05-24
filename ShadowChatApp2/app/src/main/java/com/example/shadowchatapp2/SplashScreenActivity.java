package com.example.shadowchatapp2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shadowchatapp2.Authentication_and_Updates.LoginActivity;

public class SplashScreenActivity extends AppCompatActivity {
    private static final long SPLASH_DISPLAY_TIME = 3000;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);
        ImageView logoImageView = findViewById(R.id.splash_logo);
        TextView appNameTextView = findViewById(R.id.splash_app_name);
        TextView taglineTextView = findViewById(R.id.splash_tagline);

        // Create fade-in animation
        Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1000);

        // Apply animations to UI elements
        logoImageView.startAnimation(fadeIn);

        // Delayed animation for text elements
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            appNameTextView.startAnimation(fadeIn);
            taglineTextView.startAnimation(fadeIn);
        }, 500);

        // Check user session and navigate to appropriate activity after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check both SharedPreferences used in your app
            SharedPreferences userSession = getSharedPreferences("UserSession", MODE_PRIVATE);
            SharedPreferences userSession2 = getSharedPreferences("user_session", MODE_PRIVATE);

            // User is logged in if either SharedPreferences has valid user data
            boolean isLoggedInFromRegister = userSession.getBoolean("isLoggedIn", false);
            boolean isLoggedInFromLogin = userSession2.getString("email", null) != null;

            Intent intent;
            if (isLoggedInFromRegister || isLoggedInFromLogin) {
                // User is logged in, go to MainActivity
                intent = new Intent(SplashScreenActivity.this, MainActivity.class);

                // If login has UAID, pass it along
                if (isLoggedInFromLogin) {
                    int uaid = userSession2.getInt("uaid", -1);
                    if (uaid != -1) {
                        intent.putExtra("UAID", uaid);
                    }
                }
            } else {
                // User is not logged in, go to LoginActivity (change to RegisterationActivity if you prefer)
                intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish();  // Close SplashActivity
        }, SPLASH_DISPLAY_TIME);
    }
}