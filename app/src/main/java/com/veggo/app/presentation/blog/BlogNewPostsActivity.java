package com.veggo.app.presentation.blog;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.data.local.entity.BlogEntity;
import com.veggo.app.databinding.ActivityBlogNewpostsBinding;

import java.util.List;

public class BlogNewPostsActivity extends AppCompatActivity {
    private ActivityBlogNewpostsBinding binding;
    private BlogRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlogNewpostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new BlogRepository(this);
        repository.getLatest(6, blogs -> runOnUiThread(() -> render(blogs)));
    }

    private void render(List<BlogEntity> blogs) {
        binding.blogNewPostsHeader.removeAllViews();
        binding.blogNewPostsContainer.removeAllViews();
        BlogUi.addTopActions(this, binding.blogNewPostsHeader, "B\u00e0i \u0111\u0103ng m\u1edbi nh\u1ea5t", blogs);
        for (BlogEntity blog : blogs) {
            BlogUi.addFeaturedCard(this, binding.blogNewPostsContainer, blog, repository, () -> render(blogs));
        }
    }
}
