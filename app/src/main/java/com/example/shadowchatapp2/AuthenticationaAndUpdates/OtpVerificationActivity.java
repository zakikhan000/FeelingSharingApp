package com.example.shadowchatapp2.AuthenticationaAndUpdates;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shadowchatapp2.R;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OtpVerificationActivity extends AppCompatActivity {
    private EditText otpInput;
    private Button verifyButton;
    private Button resendButton;
    private TextView timerText;
    private TextView emailText;
    private CountDownTimer countDownTimer;
    private static final long RESEND_TIMER_DURATION = 60000; // 60 seconds
    private static final String PREF_NAME = "OTPPrefs";
    private static final String KEY_OTP = "stored_otp";
    private static final String KEY_EMAIL = "user_email";
    private final OkHttpClient client = new OkHttpClient();
    private final String OTP_ENDPOINT = "https://shadow-talk-server.vercel.app/api/send-otp";
    // Registration data
    private String username;
    private String password;
    private String confirmPassword;
    private String phoneNumber;
    private String email;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_verification);
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
        confirmPassword = getIntent().getStringExtra("confirmPassword");
        phoneNumber = getIntent().getStringExtra("phone");
        email = getIntent().getStringExtra("email");

        // Initialize views
        otpInput = findViewById(R.id.otp_input);
        verifyButton = findViewById(R.id.verify_button);
        resendButton = findViewById(R.id.resend_button);
        timerText = findViewById(R.id.timer_text);
        emailText = findViewById(R.id.email_text);

        if (email != null) {
            emailText.setText("Enter OTP sent to " + email);
            // Store email in SharedPreferences
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_EMAIL, email)
                    .apply();
        }

        // Send initial OTP
        sendOTP();

        // Start countdown timer
        startResendTimer();

        // Set up verify button click listener
        verifyButton.setOnClickListener(v -> verifyOTP());

        // Set up resend button click listener
        resendButton.setOnClickListener(v -> {
            sendOTP();
            startResendTimer();
        });
    }

    private void sendOTP() {
        // Generate a 4-digit OTP
        String otp = String.format("%04d", (int) (Math.random() * 10000));

        // Store OTP in SharedPreferences
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_OTP, otp)
                .apply();

        // Create JSON request body
        String json = "{" +
                "\"email\": \"" + email + "\"," +
                "\"phone\": \"" + phoneNumber + "\"," +
                "\"otp\": \"" + otp + "\"" +
                "}";

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json
        );

        // Create request
        Request request = new Request.Builder()
                .url(OTP_ENDPOINT)
                .post(requestBody)
                .build();

        // Send request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(OtpVerificationActivity.this,
                            "Failed to send OTP. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (response.isSuccessful() && jsonResponse.getBoolean("success")) {
                            String message = jsonResponse.getString("message");
                            Toast.makeText(OtpVerificationActivity.this,
                                    message,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            String message = jsonResponse.getString("message");
                            Toast.makeText(OtpVerificationActivity.this,
                                    "Failed to send OTP: " + message,
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(OtpVerificationActivity.this,
                                "Error processing response",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void verifyOTP() {
        String enteredOTP = otpInput.getText().toString().trim();
        String storedOTP = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .getString(KEY_OTP, "");

        if (enteredOTP.isEmpty()) {
            otpInput.setError("Please enter OTP");
            return;
        }

        if (enteredOTP.equals(storedOTP)) {
            // OTP is correct
            Toast.makeText(this, "Email verified successfully", Toast.LENGTH_SHORT).show();

            // Store verification status
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_email_verified", true)
                    .apply();

            // Complete registration
            completeRegistration();
        } else {
            Toast.makeText(this, "Invalid OTP. Please try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void completeRegistration() {
        // Create intent to return to registration with verified status
        Intent resultIntent = new Intent();
        resultIntent.putExtra("email_verified", true);
        resultIntent.putExtra("username", username);
        resultIntent.putExtra("password", password);
        resultIntent.putExtra("confirmPassword", confirmPassword);
        resultIntent.putExtra("phone", phoneNumber);
        resultIntent.putExtra("email", email);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void startResendTimer() {
        resendButton.setEnabled(false);
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(RESEND_TIMER_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                timerText.setText("Resend OTP in " + seconds + "s");
            }

            @Override
            public void onFinish() {
                resendButton.setEnabled(true);
                timerText.setText("");
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
