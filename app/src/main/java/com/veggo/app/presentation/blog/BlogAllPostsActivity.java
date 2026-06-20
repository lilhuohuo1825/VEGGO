package com.veggo.app.presentation.blog;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.data.local.entity.BlogEntity;
import com.veggo.app.databinding.ActivityBlogAllpostsBinding;

import java.util.ArrayList;
import java.util.List;

public class BlogAllPostsActivity extends AppCompatActivity {
    private static final String ALL_CATEGORY = "T\u1ea5t c\u1ea3";

    private ActivityBlogAllpostsBinding binding;
    private BlogRepository repository;
    private List<BlogEntity> allBlogs = new ArrayList<>();
    private String selectedCategory = ALL_CATEGORY;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlogAllpostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new BlogRepository(this);
        repository.getAll(blogs -> runOnUiThread(() -> {
            allBlogs = new ArrayList<>(blogs);
            render();
        }));
    }

    private void render() {
        binding.blogAllPostsHeader.removeAllViews();
        binding.blogAllPostsContainer.removeAllViews();
        BlogUi.addTopActions(this, binding.blogAllPostsHeader, "B\u00e0i \u0111\u0103ng", allBlogs);
        BlogUi.addCategoryTabs(this, binding.blogAllPostsHeader, allBlogs, selectedCategory, category -> {
            selectedCategory = category;
            render();
        });
        for (BlogEntity blog : filteredBlogs()) {
            BlogUi.addFeaturedCard(this, binding.blogAllPostsContainer, blog);
        }
    }

    private List<BlogEntity> filteredBlogs() {
        if (selectedCategory == null || selectedCategory.equals(ALL_CATEGORY)) {
            return allBlogs;
        }
        List<BlogEntity> filtered = new ArrayList<>();
        for (BlogEntity blog : allBlogs) {
            if (selectedCategory.equals(BlogText.clean(blog.getCategoryTag()))) {
                filtered.add(blog);
            }
        }
        return filtered;
    }
}
