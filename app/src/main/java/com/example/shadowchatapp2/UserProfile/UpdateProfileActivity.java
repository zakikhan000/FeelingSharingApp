package com.example.shadowchatapp2.UserProfile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.shadowchatapp2.R;
import com.google.android.material.button.MaterialButtonToggleGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
public class UpdateProfileActivity extends AppCompatActivity {
    private EditText etFirstName, etMiddleName, etLastName, etAge, etCountry, etCity, etAnonymousName, etPostalCode;

    private Button btnSave, btnSelectRealImage, btnSelectHideImage;
    private ImageView ivRealImage, ivHideImage;
    private String realImageBase64 = null, hideImageBase64 = null;
    private static final int PICK_REAL_IMAGE = 1;
    private static final int PICK_HIDE_IMAGE = 2;
    private static final String BASE_URL = "https://shadow-talk-server.vercel.app/api/update-complete-profile";
    private static final String USER_SESSION = "user_session";
    private static final String USER_SESSION_ALT = "UserSession";

    private MaterialButtonToggleGroup toggleUsernameVisibility;
    private MaterialButtonToggleGroup toggleProfileVisibility;
    private MaterialButtonToggleGroup toggleImageVisibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_profile);

        etFirstName = findViewById(R.id.et_first_name);
        etMiddleName = findViewById(R.id.et_middle_name);
        etLastName = findViewById(R.id.et_last_name);
        etAge = findViewById(R.id.et_age);
        etCountry = findViewById(R.id.et_country);
        etCity = findViewById(R.id.et_city);
        etAnonymousName = findViewById(R.id.et_anonymous_name);
        etPostalCode = findViewById(R.id.et_postal_code);
        btnSave = findViewById(R.id.btn_save);
        btnSelectRealImage = findViewById(R.id.btn_select_real_image);
        btnSelectHideImage = findViewById(R.id.btn_select_hide_image);
        ivRealImage = findViewById(R.id.iv_real_image);
        ivHideImage = findViewById(R.id.iv_hide_image);

        toggleUsernameVisibility = findViewById(R.id.toggle_username_visibility);
        toggleProfileVisibility = findViewById(R.id.toggle_profile_visibility);
        toggleImageVisibility = findViewById(R.id.toggle_image_visibility);

        btnSelectRealImage.setOnClickListener(v -> pickImage(PICK_REAL_IMAGE));
        btnSelectHideImage.setOnClickListener(v -> pickImage(PICK_HIDE_IMAGE));
        btnSave.setOnClickListener(v -> updateProfile());


        // Receive data from intent
        Intent intent = getIntent();
        if (intent != null) {
            String firstName = intent.getStringExtra("first_name");
            String middleName = intent.getStringExtra("middle_name");
            String lastName = intent.getStringExtra("last_name");
            String anonymousName = intent.getStringExtra("anonymous_name");
            String age = intent.getStringExtra("age");
            String country = intent.getStringExtra("country");
            String city = intent.getStringExtra("city");
            String postalCode = intent.getStringExtra("postal_code");
            String usernameVisibility = intent.getStringExtra("username_visibility");
            String profileVisibility = intent.getStringExtra("profile_visibility");
            String imageVisibility = intent.getStringExtra("image_visibility");

            if (firstName != null) etFirstName.setText(firstName);
            if (middleName != null) etMiddleName.setText(middleName);
            if (lastName != null) etLastName.setText(lastName);
            if (anonymousName != null) etAnonymousName.setText(anonymousName);
            if (age != null) etAge.setText(age);
            if (country != null) etCountry.setText(country);
            if (city != null) etCity.setText(city);
            if (postalCode != null) etPostalCode.setText(postalCode);



            // Set visibility states
            if (usernameVisibility != null) {
                toggleUsernameVisibility.check(usernameVisibility.equals("public") ? R.id.btn_username_public : R.id.btn_username_private);
            }
            if (profileVisibility != null) {
                toggleProfileVisibility.check(profileVisibility.equals("public") ? R.id.btn_profile_public : R.id.btn_profile_private);
            }
            if (imageVisibility != null) {
                toggleImageVisibility.check(imageVisibility.equals("public") ? R.id.btn_image_public : R.id.btn_image_private);
            }
        }
    }

    private void pickImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }
    private String getVisibilityValue(MaterialButtonToggleGroup toggleGroup, int publicButtonId, int privateButtonId) {
        if (toggleGroup == null) {
            return "hidden"; // Default to hidden if toggle group is not initialized
        }

        int checkedId = toggleGroup.getCheckedButtonId();
        if (checkedId == -1) {
            return "hidden"; // Default to hidden if no button is selected
        }

        return checkedId == publicButtonId ? "visible" : "hidden";
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                if (requestCode == PICK_REAL_IMAGE) {
                    ivRealImage.setImageBitmap(bitmap);
                    realImageBase64 = bitmapToBase64(bitmap);
                } else if (requestCode == PICK_HIDE_IMAGE) {
                    ivHideImage.setImageBitmap(bitmap);
                    hideImageBase64 = bitmapToBase64(bitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private String getUaidFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(USER_SESSION, Context.MODE_PRIVATE);
        int uaid = prefs.getInt("uaid", -1);
        if (uaid == -1) {
            SharedPreferences prefsAlt = getSharedPreferences(USER_SESSION_ALT, Context.MODE_PRIVATE);
            String uaidStr = prefsAlt.getString("uaid", null);
            if (uaidStr != null) {
                return uaidStr;
            }
        }
        return String.valueOf(uaid);
    }

    private void updateProfile() {
        String firstName = etFirstName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String country = etCountry.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String anonymousName = etAnonymousName.getText().toString().trim();
        String postalCode = etPostalCode.getText().toString().trim();
        String uaid = getUaidFromPrefs();
        String usernameVisibility = getVisibilityValue(toggleUsernameVisibility, R.id.btn_username_public, R.id.btn_username_private);
        String profileVisibility = getVisibilityValue(toggleProfileVisibility, R.id.btn_profile_public, R.id.btn_profile_private);
        String imageVisibility = getVisibilityValue(toggleImageVisibility, R.id.btn_image_public, R.id.btn_image_private);
        JSONObject json = new JSONObject();
        try {
            json.put("first_name", firstName);
            json.put("middle_name", middleName);
            json.put("last_name", lastName);
            json.put("age", age);
            json.put("country", country);
            json.put("city", city);
            json.put("anonymous_name", anonymousName);
            json.put("postal_code", postalCode);
            json.put("uaid", uaid);
            // Add visibility fields
            json.put("uservisible", usernameVisibility);
            json.put("emailvisible", profileVisibility);
            json.put("imagevisible", imageVisibility);

            if (realImageBase64 != null) json.put("real_image", realImageBase64);
            if (hideImageBase64 != null) json.put("hide_image", hideImageBase64);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json.toString()
        );

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(body)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(UpdateProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string();
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(UpdateProfileActivity.this, "Profile updated!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(UpdateProfileActivity.this, "Update failed: " + res, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
