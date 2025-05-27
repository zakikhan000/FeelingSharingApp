package com.example.shadowchatapp2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class HelpcenterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_helpcenter);
        // Inside your Activity (e.g., HelpcenterActivity.java)
        // Inside your HelpcenterActivity class
        Button supportSubmit = findViewById(R.id.support_submit);
        EditText supportSubject = findViewById(R.id.support_subject);
        EditText supportMessage = findViewById(R.id.support_message);

        supportSubmit.setOnClickListener(v -> {
            String subject = supportSubject.getText().toString();
            String message = supportMessage.getText().toString();

            if (subject.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create explicit email intent
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");  // MIME type for email
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"khanzaki006006@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, message);

            try {
                // Try to launch Gmail specifically first
                Intent gmailIntent = new Intent(Intent.ACTION_SEND);
                gmailIntent.setClassName("com.google.android.gm", "com.google.android.gm.ComposeActivityGmail");
                gmailIntent.setType("message/rfc822");
                gmailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"khanzaki006006@gmail.com"});
                gmailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                gmailIntent.putExtra(Intent.EXTRA_TEXT, message);

                // Check if Gmail is installed
                PackageManager pm = getPackageManager();
                if (gmailIntent.resolveActivity(pm) != null) {
                    startActivity(gmailIntent);
                } else {
                    // Fallback to generic email intent
                    startActivity(Intent.createChooser(emailIntent, "Send email via..."));
                }
            } catch (Exception e) {
                // Final fallback
                startActivity(Intent.createChooser(emailIntent, "Send email via..."));
            }
        });
    }
}