package com.example.shadowchatapp2.AuthenticationaAndUpdates;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.shadowchatapp2.AboutUsActivity;
import com.example.shadowchatapp2.HelpcenterActivity;
import com.example.shadowchatapp2.Notifications.MainActivity;
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

public class LoginActivity extends AppCompatActivity {
    private final String BASE_URL = "https://shadow-talk-server.vercel.app/api/login";  // Replace with your actual login endpoint URL
    private OkHttpClient client = new OkHttpClient();

    private EditText etEmail, etPassword;
    private Button btnLogin;

    private TextView tv_signup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);


          etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tv_signup = findViewById(R.id.tv_signup);

        tv_signup.setOnClickListener(v -> {

            Intent intent = new Intent(LoginActivity.this, RegisterationActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();
            //validation

            if (TextUtils.isEmpty(password) || password.length() < 6) {
                etPassword.setError("Password must be at least 6 characters");
                etPassword.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Valid email is required");
                etEmail.requestFocus();
                return;
            }

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });
    }

    private void loginUser(String email, String password) {
        String json = "{" +
                "\"email\": \"" + email + "\"," +
                "\"password\": \"" + password + "\"" +
                "}";

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json
        );

        // Make the request
        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Failed to connect to server", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String res = response.body().string();
                Log.d("LoginResponse", "Response: " + res);

                runOnUiThread(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(res);
                        String responseMessage = jsonResponse.getString("response");

                        if (response.isSuccessful() && responseMessage.equals("Login Successful")) {
                            // Get UAID from response
                            int uaid = jsonResponse.getInt("uaid");
                            String username = jsonResponse.getString("username");

                            // Save to SharedPreferences
                            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("email", email);
                            editor.putInt("uaid", uaid);
                            editor.putString("username", username);
                            editor.apply();

                            // Go to main activity with UAID
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("UAID", uaid);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, responseMessage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}