package com.veggo.app.presentation.blog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.R;
import com.veggo.app.core.ui.PullToRefreshHelper;
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
    private BlogUi.CategoryTabsHolder categoryTabsHolder;
    private boolean randomPopupShown;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlogHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new BlogRepository(this);
        PullToRefreshHelper.bind(binding.blogHomeRefresh, binding.blogHomeScroll, () -> loadBlogs(true));
        renderLoading();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBlogs(false);
    }

    private void loadBlogs(boolean fromRefresh) {
        repository.getAll(blogs -> runOnUiThread(() -> {
            PullToRefreshHelper.finish(binding.blogHomeRefresh);
            allBlogs = new ArrayList<>(blogs);
            if (fromRefresh) {
                randomPostBlogs.clear();
            }
            if (randomPostBlogs.isEmpty()) {
                randomizePostBlogs();
            } else {
                for (BlogEntity oldBlog : randomPostBlogs) {
                    for (BlogEntity newBlog : blogs) {
                        if (oldBlog.getId().equals(newBlog.getId())) {
                            oldBlog.setLikedByCurrentUser(newBlog.isLikedByCurrentUser());
                            oldBlog.setLikeCount(newBlog.getLikeCount());
                            break;
                        }
                    }
                }
            }
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
        categoryTabsHolder = BlogUi.addCategoryTabs(this, binding.blogHomeContainer, allBlogs, selectedCategory, category -> {
            onCategorySelected(category);
        });

        List<BlogEntity> preview = filterByCategory(randomPostBlogs);
        int limit = Math.min(preview.size(), 5);
        for (int index = 0; index < limit; index++) {
            BlogUi.addPostItem(this, binding.blogHomeContainer, preview.get(index), repository, this::render);
        }
    }

    private void onCategorySelected(String category) {
        selectedCategory = category;
        if (categoryTabsHolder != null) {
            int selectedIndex = categoryTabsHolder.categories.indexOf(category);
            if (selectedIndex >= 0) {
                // 1. Cập nhật kiểu chữ của các tab (selected: semibold, unselected: regular)
                for (int i = 0; i < categoryTabsHolder.tabViews.size(); i++) {
                    TextView tab = categoryTabsHolder.tabViews.get(i);
                    boolean isSelected = (i == selectedIndex);
                    tab.setTextColor(androidx.core.content.ContextCompat.getColor(this, isSelected ? R.color.primary_main : R.color.neutral_70));
                    android.graphics.Typeface tf = androidx.core.content.res.ResourcesCompat.getFont(this, isSelected ? R.font.inter_semibold : R.font.inter_regular);
                    tab.setTypeface(tf);
                }
                
                // 2. Chuyển chỉ báo xanh mượt mà
                if (categoryTabsHolder.indicatorHelper != null) {
                    categoryTabsHolder.indicatorHelper.selectTab(selectedIndex, true);
                }
                
                // 3. Cuộn tab được chọn vào giữa màn hình (đảm bảo nhìn thấy được)
                TextView selectedTabView = categoryTabsHolder.tabViews.get(selectedIndex);
                categoryTabsHolder.scrollView.post(() -> {
                    int scrollX = selectedTabView.getLeft() - (categoryTabsHolder.scrollView.getWidth() - selectedTabView.getWidth()) / 2;
                    categoryTabsHolder.scrollView.smoothScrollTo(Math.max(scrollX, 0), 0);
                });
            }
        }

        // 4. Xóa và vẽ lại danh sách bài viết dưới các tab
        while (binding.blogHomeContainer.getChildCount() > 4) {
            binding.blogHomeContainer.removeViewAt(4);
        }
        
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
