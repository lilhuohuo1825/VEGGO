package com.veggo.app.presentation.profile;

import android.os.Bundle;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class CreatePostActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);
        findViewById(R.id.createPostBackButton).setOnClickListener(v -> finish());
    }
}
