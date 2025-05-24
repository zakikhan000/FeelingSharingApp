package com.example.shadowchatapp2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.ArrayAdapter;

import com.example.shadowchatapp2.Authentication_and_Updates.RegisterationActivity;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import androidx.activity.result.ActivityResultLauncher;

import org.json.JSONObject;

public class UserProfileActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> realImagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;


    private final String PROFILE_URL = "https://shadow-talk-server.vercel.app/api/create-complete-profile";

    private OkHttpClient client = new OkHttpClient();

    private EditText etFirstName, etMiddleName, etLastName, etAge, etCity, etAnonymous, etPostal;
    private AutoCompleteTextView actvCountry;
    private Button btnSubmitProfile, btnSkip;

    private int UAID; // Remove hardcoded value
    private Bitmap realImageBitmap; // Get this from image picker or camera
    private Bitmap hideImageBitmap;

    private MaterialButtonToggleGroup toggleEmailVisibility, toggleUsernameVisibility, toggleProfileVisibility;
    private Button btnEmailPublic, btnEmailPrivate, btnUsernamePublic, btnUsernamePrivate, btnProfilePublic, btnProfilePrivate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
        // Get UAID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        UAID = prefs.getInt("user_id", -1);

        if (UAID == -1) {
            Toast.makeText(this, "User ID not found. Please register again", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, RegisterationActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        etFirstName = findViewById(R.id.et_first_name);
        etMiddleName = findViewById(R.id.et_middle_name);
        etLastName = findViewById(R.id.et_last_name);
        etAge = findViewById(R.id.et_age);
        etCity = findViewById(R.id.et_city);
        etAnonymous = findViewById(R.id.et_anonymous_name);
        etPostal = findViewById(R.id.et_postal_code);
        actvCountry = findViewById(R.id.et_country);
        btnSubmitProfile = findViewById(R.id.btn_save_profile);
        btnSkip = findViewById(R.id.btn_skip);

        toggleEmailVisibility = findViewById(R.id.toggle_email_visibility);
        toggleUsernameVisibility = findViewById(R.id.toggle_username_visibility);
        toggleProfileVisibility = findViewById(R.id.toggle_profile_visibility);

        btnEmailPublic = findViewById(R.id.btn_email_public);
        btnEmailPrivate = findViewById(R.id.btn_email_private);
        btnUsernamePublic = findViewById(R.id.btn_username_public);
        btnUsernamePrivate = findViewById(R.id.btn_username_private);
        btnProfilePublic = findViewById(R.id.btn_profile_public);
        btnProfilePrivate = findViewById(R.id.btn_profile_private);

        setupCountryDropdown();


        btnSubmitProfile.setOnClickListener(v -> validateAndSaveProfile());
        btnSkip.setOnClickListener(v -> {
            Intent intent = new Intent(UserProfileActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }


    private void setupCountryDropdown() {
        // Get all country names
        String[] countries = Locale.getISOCountries();
        String[] countryNames = new String[countries.length];

        for (int i = 0; i < countries.length; i++) {
            Locale locale = new Locale("", countries[i]);
            countryNames[i] = locale.getDisplayCountry();
        }

        // Create adapter for the AutoCompleteTextView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                countryNames
        );

        actvCountry.setAdapter(adapter);
        actvCountry.setThreshold(1); // Start showing suggestions after 1 character
    }

    private void validateAndSaveProfile() {
        // Validate only required fields
        if (isEmpty(etFirstName) ||
                isEmpty(etLastName) ||
                isEmpty(etAnonymous)) {

            Toast.makeText(this, "First name, last name, and anonymous name are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate age if provided
        if (!isEmpty(etAge)) {
            try {
                int age = Integer.parseInt(etAge.getText().toString());
                if (age <= 0 || age > 120) {
                    Toast.makeText(this, "Please enter a valid age between 1 and 120", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Age must be a number", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // If all validations pass, save profile
        saveProfile();
    }

    private boolean isEmpty(EditText editText) {
        return editText.getText().toString().trim().isEmpty();
    }

    private boolean isEmpty(AutoCompleteTextView autoCompleteTextView) {
        return autoCompleteTextView.getText().toString().trim().isEmpty();
    }

    private String getVisibilityValue(MaterialButtonToggleGroup group, int publicBtnId, int privateBtnId) {
        int checkedId = group.getCheckedButtonId();
        if (checkedId == publicBtnId) {
            return "visible";
        } else if (checkedId == privateBtnId) {
            return "hidden";
        }
        return "visible"; // default
    }

    private void saveProfile() {
        // Check if UAID is valid
        if (UAID == -1) {
            Toast.makeText(this, "User ID not found. Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        // Required fields
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String anonymousName = etAnonymous.getText().toString().trim();

        // Optional fields - will be null if empty
        String middleName = isEmpty(etMiddleName) ? null : etMiddleName.getText().toString().trim();
        String age = isEmpty(etAge) ? null : etAge.getText().toString().trim();
        String country = isEmpty(actvCountry) ? null : actvCountry.getText().toString().trim();
        String city = isEmpty(etCity) ? null : etCity.getText().toString().trim();
        String postalCode = isEmpty(etPostal) ? null : etPostal.getText().toString().trim();
        String emailVisibility = getVisibilityValue(toggleEmailVisibility, R.id.btn_email_public, R.id.btn_email_private);
        String usernameVisibility = getVisibilityValue(toggleUsernameVisibility, R.id.btn_username_public, R.id.btn_username_private);

        // Create JSON object with profile data
        try {
            JSONObject json = new JSONObject();
            json.put("uaid", UAID);
            json.put("first_name", firstName);
            json.put("middle_name", middleName);
            json.put("last_name", lastName);
            json.put("age", age);
            json.put("country", country);
            json.put("city", city);
            json.put("anonymous_name", anonymousName);
            json.put("postal_code", postalCode);
            json.put("emailvisible", emailVisibility);
            json.put("uservisible", usernameVisibility);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(PROFILE_URL)
                    .post(body)
                    .build();

            // Show loading state
            setLoadingState(true);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        Toast.makeText(UserProfileActivity.this, "Profile creation failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String res = response.body() != null ? response.body().string() : null;
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        if (response.isSuccessful() && res != null) {
                            try {
                                JSONObject responseJson = new JSONObject(res);
                                String status = responseJson.getString("status");
                                String message = responseJson.getString("message");

                                if (status.equals("success")) {
                                    Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                                    navigateToNextScreen();
                                } else {
                                    Toast.makeText(UserProfileActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(UserProfileActivity.this, "Error parsing response", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(UserProfileActivity.this, "Server error: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Something went wrong while preparing profile data", Toast.LENGTH_SHORT).show();
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private String getUserId() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);
        return userId != -1 ? String.valueOf(userId) : null;
    }

    private void setLoadingState(boolean isLoading) {
        btnSubmitProfile.setEnabled(!isLoading);
        btnSkip.setEnabled(!isLoading);
    }

    private void navigateToNextScreen() {
        Intent intent = new Intent(UserProfileActivity.this, ProfileImageActivity.class);
        startActivity(intent);
        finish();
    }
}