package com.veggo.app.presentation.blog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.R;
import com.veggo.app.data.local.entity.BlogEntity;
import com.veggo.app.databinding.ActivityBlogHomeBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlogHomeActivity extends AppCompatActivity {
    private static final String ALL_CATEGORY = "T\u1ea5t c\u1ea3";

    private ActivityBlogHomeBinding binding;
    private BlogRepository repository;
    private List<BlogEntity> allBlogs = new ArrayList<>();
    private List<BlogEntity> randomPostBlogs = new ArrayList<>();
    private String selectedCategory = ALL_CATEGORY;
    private boolean randomPopupShown;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlogHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new BlogRepository(this);
        binding.blogHomeRefresh.setColorSchemeResources(R.color.primary_main, R.color.primary_hover);
        binding.blogHomeRefresh.setOnChildScrollUpCallback((parent, child) -> binding.blogHomeScroll.canScrollVertically(-1));
        binding.blogHomeRefresh.setOnRefreshListener(() -> loadBlogs(true));
        renderLoading();
        loadBlogs(false);
    }

    private void loadBlogs(boolean fromRefresh) {
        repository.getAll(blogs -> runOnUiThread(() -> {
            binding.blogHomeRefresh.setRefreshing(false);
            allBlogs = new ArrayList<>(blogs);
            randomizePostBlogs();
            render();
            if (!fromRefresh) {
                showRandomPopupOnce();
            }
        }));
    }

    private void randomizePostBlogs() {
        randomPostBlogs = new ArrayList<>(allBlogs);
        if (!randomPostBlogs.isEmpty()) {
            randomPostBlogs.remove(0);
        }
        Collections.shuffle(randomPostBlogs);
    }

    private void showRandomPopupOnce() {
        if (randomPopupShown || allBlogs.isEmpty()) {
            return;
        }
        randomPopupShown = true;
        BlogUi.showRandomPostPopup(this, allBlogs);
    }

    private void render() {
        binding.blogHomeHeader.removeAllViews();
        binding.blogHomeContainer.removeAllViews();
        if (allBlogs.isEmpty()) {
            renderEmpty();
            return;
        }

        BlogUi.addTopActions(this, binding.blogHomeHeader, "Blog", allBlogs);
        BlogUi.addSectionHeader(
                this,
                binding.blogHomeContainer,
                "M\u1edbi nh\u1ea5t",
                "Xem th\u00eam",
                v -> startActivity(new Intent(this, BlogNewPostsActivity.class))
        );
        BlogUi.addFeaturedCard(this, binding.blogHomeContainer, allBlogs.get(0), repository, this::render);

        BlogUi.addSectionHeader(
                this,
                binding.blogHomeContainer,
                "B\u00e0i \u0111\u0103ng",
                "Xem th\u00eam",
                v -> startActivity(new Intent(this, BlogAllPostsActivity.class))
        );
        BlogUi.addCategoryTabs(this, binding.blogHomeContainer, allBlogs, selectedCategory, category -> {
            selectedCategory = category;
            render();
        });

        List<BlogEntity> preview = filterByCategory(randomPostBlogs);
        int limit = Math.min(preview.size(), 5);
        for (int index = 0; index < limit; index++) {
            BlogUi.addPostItem(this, binding.blogHomeContainer, preview.get(index), repository, this::render);
        }
    }

    private List<BlogEntity> filterByCategory(List<BlogEntity> blogs) {
        if (selectedCategory == null || selectedCategory.equals(ALL_CATEGORY)) {
            return blogs;
        }
        List<BlogEntity> filtered = new ArrayList<>();
        for (BlogEntity blog : blogs) {
            if (selectedCategory.equals(BlogText.clean(blog.getCategoryTag()))) {
                filtered.add(blog);
            }
        }
        return filtered;
    }

    private void renderLoading() {
        binding.blogHomeContainer.removeAllViews();
        TextView loading = new TextView(this);
        loading.setText("\u0110ang t\u1ea3i b\u00e0i vi\u1ebft...");
        loading.setTextColor(getColor(R.color.neutral_70));
        loading.setTextSize(14);
        binding.blogHomeContainer.addView(loading);
    }

    private void renderEmpty() {
        TextView empty = new TextView(this);
        empty.setText("Ch\u01b0a c\u00f3 b\u00e0i vi\u1ebft.");
        empty.setTextColor(getColor(R.color.neutral_70));
        empty.setTextSize(14);
        binding.blogHomeContainer.addView(empty);
    }
}
