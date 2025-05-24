package com.example.shadowchatapp2;

import android.content.SharedPreferences;
import android.os.Bundle;
import okhttp3.Call;
import okhttp3.Callback;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.IOException;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

public class ProfileImageActivity extends AppCompatActivity {
    private static final int PICK_REAL_IMAGE = 100;
    private static final int TAKE_REAL_PICTURE = 101;
    private static final int PICK_HIDDEN_IMAGE = 102;
    private static final int TAKE_HIDDEN_PICTURE = 103;

    // Real profile elements
    private ShapeableImageView imgRealProfilePreview;
    private TextView txtRealSelectionStatus;
    private ImageView imgRealSelectionIndicator;
    private MaterialButton btnRealCamera;
    private MaterialButton btnRealGallery;

    // Hidden profile elements
    private ShapeableImageView imgHiddenProfilePreview;
    private TextView txtHiddenSelectionStatus;
    private ImageView imgHiddenSelectionIndicator;
    private MaterialButton btnHiddenCamera;
    private MaterialButton btnHiddenGallery;

    // Navigation buttons
    private MaterialButton btnContinue;
    private MaterialButton btnSkip;
    // Image data
    private Bitmap realImageBitmap;
    private Bitmap hiddenImageBitmap;
    private OkHttpClient client = new OkHttpClient();

    private final String PROFILE_URL = "https://shadow-talk-server.vercel.app/api/upload-profile-image";
    private String getUserId() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String uaid = prefs.getString("uaid", null);  // Default to null if not found

        // Check if the UAID exists
        if (uaid == null || uaid.isEmpty()) {
            // Show a message if UAID is missing
            Toast.makeText(this, "User ID not found. Please login again", Toast.LENGTH_SHORT).show();
        }
        return uaid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_image);
        setupToolbar();
        initializeViews();
        setupClickListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeViews() {
        // Real profile elements
        imgRealProfilePreview = findViewById(R.id.img_real_profile_preview);
        txtRealSelectionStatus = findViewById(R.id.txt_real_selection_status);
        imgRealSelectionIndicator = findViewById(R.id.img_real_selection_indicator);
        btnRealCamera = findViewById(R.id.btn_real_camera);
        btnRealGallery = findViewById(R.id.btn_real_gallery);

        // Hidden profile elements
        imgHiddenProfilePreview = findViewById(R.id.img_hidden_profile_preview);
        txtHiddenSelectionStatus = findViewById(R.id.txt_hidden_selection_status);
        imgHiddenSelectionIndicator = findViewById(R.id.img_hidden_selection_indicator);
        btnHiddenCamera = findViewById(R.id.btn_hidden_camera);
        btnHiddenGallery = findViewById(R.id.btn_hidden_gallery);

        // Navigation buttons
        btnContinue = findViewById(R.id.btn_continue);
        btnSkip = findViewById(R.id.btn_skip);
    }

    private void setupClickListeners() {
        // Real profile image options
        btnRealCamera.setOnClickListener(v -> openCamera(TAKE_REAL_PICTURE));
        btnRealGallery.setOnClickListener(v -> openGallery(PICK_REAL_IMAGE));

        // Hidden profile image options
        btnHiddenCamera.setOnClickListener(v -> openCamera(TAKE_HIDDEN_PICTURE));
        btnHiddenGallery.setOnClickListener(v -> openGallery(PICK_HIDDEN_IMAGE));

        // Navigation buttons
        btnContinue.setOnClickListener(v -> uploadProfileImages());
        btnSkip.setOnClickListener(v -> navigateToNextScreen());
    }

    private void openCamera(int requestCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, requestCode);
        }
    }

    private void openGallery(int requestCode) {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK);
        pickPhotoIntent.setType("image/*");
        startActivityForResult(pickPhotoIntent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            try {
                Bitmap selectedBitmap = null;

                // Gallery image selection
                if (requestCode == PICK_REAL_IMAGE || requestCode == PICK_HIDDEN_IMAGE) {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                }
                // Camera image capture
                else if (requestCode == TAKE_REAL_PICTURE || requestCode == TAKE_HIDDEN_PICTURE) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        selectedBitmap = (Bitmap) extras.get("data");
                    }
                }

                if (selectedBitmap != null) {
                    // Update the appropriate image preview based on request code
                    if (requestCode == PICK_REAL_IMAGE || requestCode == TAKE_REAL_PICTURE) {
                        realImageBitmap = selectedBitmap;
                        imgRealProfilePreview.setImageBitmap(realImageBitmap);
                        txtRealSelectionStatus.setText("Real photo selected");
                        imgRealSelectionIndicator.setVisibility(View.GONE);
                    } else if (requestCode == PICK_HIDDEN_IMAGE || requestCode == TAKE_HIDDEN_PICTURE) {
                        hiddenImageBitmap = selectedBitmap;
                        imgHiddenProfilePreview.setImageBitmap(hiddenImageBitmap);
                        txtHiddenSelectionStatus.setText("Hidden photo selected");
                        imgHiddenSelectionIndicator.setVisibility(View.GONE);
                    }

                    // Enable continue button if at least one image is selected
                    updateContinueButtonState();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateContinueButtonState() {
        // Enable the continue button only if both images are selected
        btnContinue.setEnabled(true);
    }
    private void uploadProfileImages() {
        // Get UAID from SharedPreferences or another source
        String uaid = getUserId();
        if (uaid == null || uaid.isEmpty()) {
            Toast.makeText(this, "User ID not found. Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log to verify continue button is working
        Log.d("ProfileImageActivity", "Continue button clicked, uploading images...");

        // Default base64 image (1x1 transparent PNG)
        String defaultBase64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGMAAQAABQABDQottAAAAABJRU5ErkJggg==";

        // Create JSON object with image data
        try {
            JSONObject json = new JSONObject();
            json.put("uaid", uaid);
            json.put("real_image", realImageBitmap != null ? bitmapToBase64(realImageBitmap) : defaultBase64Image);
            json.put("hide_image", hiddenImageBitmap != null ? bitmapToBase64(hiddenImageBitmap) : defaultBase64Image);
            json.put("profile_visibility", "hidden"); // Default visibility

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
                        Toast.makeText(ProfileImageActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String res = response.body() != null ? response.body().string() : null;
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        if (response.isSuccessful() && res != null) {
                            // Log the raw response for debugging
                            Log.d("ProfileImageActivity", "Raw response: " + res);
                            try {
                                JSONObject responseJson = new JSONObject(res);
                                boolean success = responseJson.getBoolean("success");
                                String message = responseJson.getString("message");

                                if (success) {
                                    if (realImageBitmap != null) {
                                        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putString("profile_image", bitmapToBase64(realImageBitmap));
                                        editor.apply();
                                    }
                                    Toast.makeText(ProfileImageActivity.this, message, Toast.LENGTH_SHORT).show();
                                    navigateToNextScreen();
                                } else {
                                    Toast.makeText(ProfileImageActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                // Log the exception details for debugging
                                Log.e("ProfileImageActivity", "Error parsing response: " + e.getMessage(), e);
                                Toast.makeText(ProfileImageActivity.this, "Error parsing response", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(ProfileImageActivity.this, "Server error: " + response.code(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Something went wrong while preparing images", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoadingState(boolean isLoading) {
        // Disable UI interaction during loading
        btnContinue.setEnabled(!isLoading);
        btnSkip.setEnabled(!isLoading);
        btnRealCamera.setEnabled(!isLoading);
        btnRealGallery.setEnabled(!isLoading);
        btnHiddenCamera.setEnabled(!isLoading);
        btnHiddenGallery.setEnabled(!isLoading);

        // Add progress indication if needed
        if (isLoading) {
            btnContinue.setText("UPLOADING...");
        } else {
            btnContinue.setText("CONTINUE");
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos); // Reduced quality for smaller payload
        byte[] byteArray = baos.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void navigateToNextScreen() {
        // Navigate to the main activity or the next screen in your flow
        startActivity(new Intent(ProfileImageActivity.this, MainActivity.class));
        finish(); // Close this activity to prevent returning to it with back button
    }
}