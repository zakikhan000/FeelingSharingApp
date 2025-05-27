package com.example.shadowchatapp2.AuthenticationaAndUpdates;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;

import com.example.shadowchatapp2.AboutUsActivity;
import com.example.shadowchatapp2.HelpcenterActivity;
import com.example.shadowchatapp2.Notifications.MainActivity;
import com.example.shadowchatapp2.R;
import com.example.shadowchatapp2.UserProfile.UserProfileActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterationActivity extends AppCompatActivity {


    private final String BASE_URL = "https://shadow-talk-server.vercel.app/api/register";
    private TextView tv_login;
    private OkHttpClient client = new OkHttpClient();
    private EditText etEmail, etUsername, etPassword, etConfirmPassword, etPhone;
    private Button signUp;
    private SharedPreferences sharedPreferences;
    private static final int REQUEST_OTP_VERIFICATION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registeration);

        ImageButton help = findViewById(R.id.btn_Helpcenter);
        ImageButton Aboutus = findViewById(R.id.btnAboutus);

        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), HelpcenterActivity.class);
                startActivity(i);
            }
        });
        Aboutus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i =new Intent(getApplicationContext(), AboutUsActivity.class);
                startActivity(i);
            }
        });

        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);

        // If user is already logged in, navigate to MainActivity
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(RegisterationActivity.this, MainActivity.class));
            finish();
            return;
        }

        etEmail = findViewById(R.id.et_email);
        etUsername = findViewById(R.id.et_username);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etPassword.setOnLongClickListener(v -> {
            togglePasswordVisibility(etPassword);
            return true;
        });

        etConfirmPassword.setOnLongClickListener(v -> {
            togglePasswordVisibility(etConfirmPassword);
            return true;
        });


        signUp = findViewById(R.id.btnSign);
        tv_login = findViewById(R.id.tv_login);

        tv_login.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterationActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmail.getText().toString();
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();
                String confirmPassword = etConfirmPassword.getText().toString();
                String phone = etPhone.getText().toString();
                //validation

                if (TextUtils.isEmpty(username)) {
                    etUsername.setError("Username is required");
                    etUsername.requestFocus();
                    return;
                }

                if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etEmail.setError("Valid email is required");
                    etEmail.requestFocus();
                    return;
                }

                if (!phone.matches("^\\+92[0-9]{10}$")) {
                    etPhone.setError("Phone must be in +92XXXXXXXXXX format");
                    etPhone.requestFocus();
                    return;
                }


                if (TextUtils.isEmpty(password) || password.length() < 6) {
                    etPassword.setError("Password must be at least 6 characters");
                    etPassword.requestFocus();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    etConfirmPassword.setError("Passwords do not match");
                    etConfirmPassword.requestFocus();
                    return;
                }

                // Start OTP verification
                startEmailVerification(email, username, password, confirmPassword, phone);
            }
        });
    }

    private void togglePasswordVisibility(EditText editText) {
        if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        editText.setSelection(editText.getText().length()); // move cursor to end
    }
    public void saveData(String email, String username, String password, String confirmPassword, String phoneNo) {
        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phoneNo.isEmpty()) {
            Toast.makeText(RegisterationActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(RegisterationActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        String json = "{" +
                "\"email\": \"" + email + "\"," +
                "\"username\": \"" + username + "\"," +
                "\"password\": \"" + password + "\"," +
                "\"confirm_password\": \"" + confirmPassword + "\"," +
                "\"phoneNo\": \"" + phoneNo + "\"" +
                "}";

        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json
        );

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(RegisterationActivity.this, "Server connection failed", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String res = response.body().string();
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonObject = new JSONObject(res);

                            if (jsonObject.has("uaid")) {
                                int userId = jsonObject.getInt("uaid");

                                // Save user ID and login state
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("user_id", userId);
                                editor.putString("uaid", String.valueOf(userId));
                                editor.putString("username", username);
                                editor.putBoolean("isLoggedIn", true);
                                editor.apply();

                                Toast.makeText(RegisterationActivity.this, "Registration successful!", Toast.LENGTH_LONG).show();

                                etEmail.setText("");
                                etUsername.setText("");
                                etPassword.setText("");
                                etConfirmPassword.setText("");
                                etPhone.setText("");

                                Intent intent = new Intent(RegisterationActivity.this, UserProfileActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(RegisterationActivity.this, "Missing user ID in response", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(RegisterationActivity.this, "Failed to parse response", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(RegisterationActivity.this, "Registration failed: " + res, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void startEmailVerification(String email, String username, String password, String confirmPassword, String phone) {
        Intent intent = new Intent(this, OtpVerificationActivity.class);
        intent.putExtra("email", email);
        intent.putExtra("username", username);
        intent.putExtra("password", password);
        intent.putExtra("confirmPassword", confirmPassword);
        intent.putExtra("phone", phone);
        startActivityForResult(intent, REQUEST_OTP_VERIFICATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OTP_VERIFICATION && resultCode == RESULT_OK && data != null) {
            boolean emailVerified = data.getBooleanExtra("email_verified", false);
            if (emailVerified) {
                // Email is verified, proceed with registration
                String username = data.getStringExtra("username");
                String password = data.getStringExtra("password");
                String confirmPassword = data.getStringExtra("confirmPassword");
                String phone = data.getStringExtra("phone");
                String email = data.getStringExtra("email");

                // Call saveData with verified email
                saveData(email, username, password, confirmPassword, phone);
            }
        }
    }
}