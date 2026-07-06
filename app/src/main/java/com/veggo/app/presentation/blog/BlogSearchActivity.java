package com.veggo.app.presentation.blog;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.veggo.app.R;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.data.local.entity.BlogEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlogSearchActivity extends AppCompatActivity {
    private BlogRepository repository;
    private LinearLayout container;
    private EditText searchInput;
    private final List<BlogEntity> blogs = new ArrayList<>();
    private SwipeRefreshLayout refreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blog_search);

        repository = new BlogRepository(this);
        container = findViewById(R.id.blogSearchContainer);
        searchInput = findViewById(R.id.blogSearchInput);
        findViewById(R.id.blogSearchBackButton).setOnClickListener(v -> finish());
        refreshLayout = PullToRefreshHelper.wrap(
                findViewById(R.id.blogSearchScroll),
                this::reloadBlogs
        );
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                render();
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        reloadBlogs();
    }

    private void reloadBlogs() {
        repository.getAll(result -> runOnUiThread(() -> {
            blogs.clear();
            blogs.addAll(result);
            render();
            PullToRefreshHelper.finish(refreshLayout);
        }));
    }

    private void render() {
        container.removeAllViews();
        List<BlogEntity> filtered = filteredBlogs();
        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Không tìm thấy bài viết phù hợp.");
            empty.setTextColor(getColor(R.color.neutral_70));
            empty.setTextSize(14);
            container.addView(empty);
            return;
        }
        for (BlogEntity blog : filtered) {
            BlogUi.addPostItem(this, container, blog, repository, this::render);
        }
    }

    private List<BlogEntity> filteredBlogs() {
        List<String> tokens = tokens(searchInput.getText().toString());
        if (tokens.isEmpty()) {
            return blogs;
        }
        List<BlogEntity> filtered = new ArrayList<>();
        for (BlogEntity blog : blogs) {
            if (matches(blog, tokens)) {
                filtered.add(blog);
            }
        }
        return filtered;
    }

    private boolean matches(BlogEntity blog, List<String> tokens) {
        String haystack = normalize(
                BlogText.clean(blog.getTitle()) + " " +
                BlogText.clean(blog.getExcerpt()) + " " +
                BlogText.clean(blog.getCategoryTag()) + " " +
                BlogText.clean(blog.getAuthor()) + " " +
                BlogText.clean(blog.getContent())
        );
        for (String token : tokens) {
            if (!haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private List<String> tokens(String query) {
        List<String> result = new ArrayList<>();
        for (String part : normalize(query).split("\\s+")) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
