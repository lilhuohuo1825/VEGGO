package com.veggo.app.presentation.blog;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.R;
import com.veggo.app.databinding.ActivityBlogDetailBinding;

public class BlogDetailActivity extends AppCompatActivity {
    public static final String EXTRA_BLOG_ID = "blog_id";

    private ActivityBlogDetailBinding binding;
    private BlogRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlogDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupFixedBackButton();
        repository = new BlogRepository(this);

        String blogId = getIntent().getStringExtra(EXTRA_BLOG_ID);
        if (blogId == null) {
            renderMissing();
            return;
        }
        repository.getById(blogId, blog -> runOnUiThread(() -> {
            binding.blogDetailContainer.removeAllViews();
            if (blog == null) {
                renderMissing();
            } else {
                BlogUi.renderDetail(this, binding.blogDetailContainer, blog);
            }
        }));
    }

    private void setupFixedBackButton() {
        binding.blogDetailHeader.addView(BlogUi.createBackButton(this));
        binding.blogDetailHeader.bringToFront();
    }

    private void renderMissing() {
        binding.blogDetailContainer.removeAllViews();
        TextView missing = new TextView(this);
        missing.setText("Không tìm thấy bài viết.");
        missing.setTextColor(getColor(R.color.neutral_10));
        missing.setTextSize(14);
        missing.setPadding(28, 80, 28, 28);
        binding.blogDetailContainer.addView(missing);
    }
}
