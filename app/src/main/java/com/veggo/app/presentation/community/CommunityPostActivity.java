package com.veggo.app.presentation.community;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.R;

public class CommunityPostActivity extends AppCompatActivity {
    private ActivityResultLauncher<String> imagePicker;
    private ImageView uploadPreview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_post);

        uploadPreview = findViewById(R.id.communityPostUploadPreview);
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), this::showSelectedImage);

        findViewById(R.id.communityPostBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.communityPostDraftButton).setOnClickListener(v ->
                Toast.makeText(this, "Da luu nhap", Toast.LENGTH_SHORT).show());
        findViewById(R.id.communityPostSubmitButton).setOnClickListener(v ->
                Toast.makeText(this, "Da dang bai", Toast.LENGTH_SHORT).show());
        findViewById(R.id.communityPostUploadBox).setOnClickListener(v -> imagePicker.launch("image/*"));
    }

    private void showSelectedImage(Uri uri) {
        if (uri == null) {
            return;
        }
        uploadPreview.setImageURI(uri);
        uploadPreview.setVisibility(View.VISIBLE);
        findViewById(R.id.communityPostUploadPlus).setVisibility(View.GONE);
        findViewById(R.id.communityPostUploadHint).setVisibility(View.GONE);
    }
}
