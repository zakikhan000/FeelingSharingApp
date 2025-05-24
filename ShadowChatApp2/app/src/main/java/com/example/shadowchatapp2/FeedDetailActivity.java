package com.example.shadowchatapp2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

public class FeedDetailActivity extends AppCompatActivity {

    ImageView image;
    TextView title, description;
    Button readMore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_detail);
        image = findViewById(R.id.feed_image);
        title = findViewById(R.id.feed_title);
        description = findViewById(R.id.feed_desc);
        readMore = findViewById(R.id.read_more_btn);

        Intent i = getIntent();
        title.setText(i.getStringExtra("title"));
        description.setText(i.getStringExtra("description"));

        Glide.with(this).load(i.getStringExtra("imageUrl")).into(image);

        readMore.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(i.getStringExtra("link")));
            startActivity(browserIntent);
        });
    }
}

