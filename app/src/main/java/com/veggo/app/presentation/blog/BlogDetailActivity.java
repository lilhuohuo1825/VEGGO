package com.veggo.app.presentation.blog;

import android.os.Bundle;
import android.content.res.ColorStateList;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.veggo.app.R;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.core.favorite.FavoriteStore;
import com.veggo.app.data.local.entity.BlogCommentEntity;
import com.veggo.app.data.local.entity.BlogEntity;
import com.veggo.app.databinding.ActivityBlogDetailBinding;

import java.util.List;

public class BlogDetailActivity extends AppCompatActivity {
    public static final String EXTRA_BLOG_ID = "blog_id";

    private ActivityBlogDetailBinding binding;
    private BlogRepository repository;
    private FavoriteStore favoriteStore;
    private String blogId;
    private BlogEntity currentBlog;
    private int preFocusScrollY = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlogDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupFixedBackButton();
        repository = new BlogRepository(this);
        favoriteStore = new FavoriteStore(this);

        blogId = getIntent().getStringExtra(EXTRA_BLOG_ID);
        binding.blogCommentSend.setOnClickListener(v -> submitComment());
        binding.blogDetailCommentIcon.setOnClickListener(v -> binding.blogDetailScroll.post(() -> {
            if (binding != null && binding.blogCommentsTitle != null) {
                int relativeTop = getRelativeTop(binding.blogCommentsTitle, binding.blogDetailScroll);
                binding.blogDetailScroll.smoothScrollTo(0, relativeTop);
            }
        }));
        binding.blogDetailCommentIcon.setImageTintList(ColorStateList.valueOf(getColor(R.color.primary_main)));
        
        binding.blogCommentInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                preFocusScrollY = binding.blogDetailScroll.getScrollY();
                binding.blogDetailContainer.setPadding(0, 0, 0, dp(200));
                scrollToCommentInput();
            } else {
                binding.blogDetailContainer.setPadding(0, 0, 0, dp(16));
                if (preFocusScrollY != -1) {
                    binding.blogDetailScroll.smoothScrollTo(0, preFocusScrollY);
                    preFocusScrollY = -1;
                }
            }
        });
        binding.blogCommentInput.setOnClickListener(v -> {
            binding.blogDetailContainer.setPadding(0, 0, 0, dp(200));
            scrollToCommentInput();
        });

        PullToRefreshHelper.bind(binding.blogDetailRefresh, binding.blogDetailScroll, this::loadBlog);

        if (blogId == null) {
            renderMissing();
            return;
        }
        loadBlog();
    }

    private void scrollToCommentInput() {
        if (binding == null) return;
        binding.blogDetailScroll.postDelayed(() -> {
            if (binding != null && binding.blogCommentInputRow != null) {
                int relativeTop = getRelativeTop(binding.blogCommentInputRow, binding.blogDetailScroll);
                binding.blogDetailScroll.smoothScrollTo(0, Math.max(relativeTop - dp(200), 0));
            }
        }, 250);
    }

    private int getRelativeTop(View view, View ancestor) {
        if (view == null || view == ancestor) {
            return 0;
        }
        android.view.ViewParent parent = view.getParent();
        if (parent instanceof View) {
            return view.getTop() + getRelativeTop((View) parent, ancestor);
        }
        return view.getTop();
    }

    private void loadBlog() {
        if (blogId == null) {
            PullToRefreshHelper.finish(binding.blogDetailRefresh);
            return;
        }
        repository.getById(blogId, blog -> runOnUiThread(() -> {
            PullToRefreshHelper.finish(binding.blogDetailRefresh);
            if (blog == null) {
                renderMissing();
            } else {
                bindBlog(blog);
            }
        }));
    }

    private void setupFixedBackButton() {
        android.widget.ImageButton backButton = BlogUi.createBackButton(this);
        backButton.setBackgroundResource(R.drawable.bg_community_circle);
        int paddingPx = dp(10);
        backButton.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        binding.blogDetailHeader.addView(backButton);
        binding.blogDetailHeader.bringToFront();
    }

    private void bindBlog(BlogEntity blog) {
        currentBlog = blog;
        Glide.with(this)
                .load(blog.getImageUrl())
                .transform(new CenterCrop(), new RoundedCorners(dp(12)))
                .into(binding.blogDetailHeroImage);

        String author = BlogText.clean(blog.getAuthor());
        binding.blogDetailAuthorAvatar.setText(initials(author));
        binding.blogDetailAuthor.setText(author);
        binding.blogDetailCategory.setText(BlogText.clean(blog.getCategoryTag()));
        binding.blogDetailTitle.setText(BlogText.clean(blog.getTitle()));
        binding.blogDetailDate.setText(BlogText.dateTime(blog.getPublishedAt()));
        binding.blogDetailBody.setText(Html.fromHtml(BlogText.clean(blog.getContent()), Html.FROM_HTML_MODE_LEGACY));
        bindBlogLike(blog);
        loadComments();
    }

    private void bindBlogLike(BlogEntity blog) {
        boolean liked = blog.isLikedByCurrentUser();
        binding.blogDetailLikeIcon.setImageResource(liked
                ? R.drawable.ic_profile_menu_heart_filled
                : R.drawable.ic_heart_outline_green);
        binding.blogDetailLikeIcon.setImageTintList(ColorStateList.valueOf(getColor(R.color.primary_main)));
        binding.blogDetailLikeIcon.setAlpha(liked ? 1f : 0.92f);
        binding.blogDetailLikeIcon.setOnClickListener(v -> toggleBlogLike());
    }

    private void toggleBlogLike() {
        if (blogId == null || currentBlog == null) {
            return;
        }
        binding.blogDetailLikeIcon.setEnabled(false);
        boolean selected = !currentBlog.isLikedByCurrentUser();
        syncFavorite(selected);
        currentBlog.setLikedByCurrentUser(selected);
        bindBlogLike(currentBlog);
        repository.setBlogLike(blogId, selected, updated -> runOnUiThread(() -> {
            binding.blogDetailLikeIcon.setEnabled(true);
            if (updated == null) {
                Toast.makeText(this, selected ? "Đã lưu bài viết" : "Đã xoá khỏi yêu thích", Toast.LENGTH_SHORT).show();
                return;
            }
            currentBlog.setLikeCount(updated.getLikeCount());
            currentBlog.setLikedByCurrentUser(updated.isLikedByCurrentUser());
            syncFavorite(updated.isLikedByCurrentUser());
            bindBlogLike(currentBlog);
        }));
    }

    private void syncFavorite(boolean selected) {
        if (currentBlog == null || favoriteStore == null) {
            return;
        }
        if (selected) {
            favoriteStore.add(new FavoriteStore.FavoriteItem(
                    FavoriteStore.TYPE_BLOG,
                    currentBlog.getId(),
                    BlogText.clean(currentBlog.getTitle()),
                    BlogText.clean(currentBlog.getAuthor()),
                    currentBlog.getImageUrl()
            ));
        } else {
            favoriteStore.remove(FavoriteStore.TYPE_BLOG, currentBlog.getId());
        }
    }

    private void loadComments() {
        if (blogId == null) {
            return;
        }
        repository.loadComments(blogId, comments -> runOnUiThread(() -> bindComments(comments)));
    }

    private void bindComments(List<BlogCommentEntity> comments) {
        binding.blogCommentsContainer.removeAllViews();
        if (comments.isEmpty()) {
            addEmptyCommentHint();
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (BlogCommentEntity comment : comments) {
            View item = inflater.inflate(R.layout.item_blog_comment, binding.blogCommentsContainer, false);
            ((TextView) item.findViewById(R.id.blogCommentAvatar)).setText(initials(comment.getUserName()));
            ((TextView) item.findViewById(R.id.blogCommentName)).setText(displayName(comment));
            ((TextView) item.findViewById(R.id.blogCommentContent)).setText(comment.getContent());
            ((TextView) item.findViewById(R.id.blogCommentMeta)).setText(commentMeta(comment));
            bindCommentLike(item.findViewById(R.id.blogCommentLike), comment);
            binding.blogCommentsContainer.addView(item);
        }
    }

    private void addEmptyCommentHint() {
        View item = LayoutInflater.from(this).inflate(R.layout.item_blog_comment, binding.blogCommentsContainer, false);
        ((TextView) item.findViewById(R.id.blogCommentAvatar)).setText("V");
        ((TextView) item.findViewById(R.id.blogCommentName)).setText("Veggo");
        ((TextView) item.findViewById(R.id.blogCommentContent)).setText("Bạn có thể là người đầu tiên để lại bình luận cho bài viết này.");
        ((TextView) item.findViewById(R.id.blogCommentMeta)).setText("Gợi ý");
        item.findViewById(R.id.blogCommentLike).setVisibility(View.GONE);
        binding.blogCommentsContainer.addView(item);
    }

    private void submitComment() {
        String content = binding.blogCommentInput.getText().toString().trim();
        if (content.isEmpty() || blogId == null) {
            return;
        }
        binding.blogCommentSend.setEnabled(false);
        repository.createComment(blogId, content, comment -> runOnUiThread(() -> {
            binding.blogCommentSend.setEnabled(true);
            if (comment == null) {
                Toast.makeText(this, "Chưa gửi được bình luận", Toast.LENGTH_SHORT).show();
                return;
            }
            binding.blogCommentInput.setText("");
            Toast.makeText(this, "Đã thêm bình luận", Toast.LENGTH_SHORT).show();
            loadComments();
        }));
    }

    private void bindCommentLike(TextView likeView, BlogCommentEntity comment) {
        likeView.setText("♥ " + comment.getLikeCount());
        likeView.setTextColor(getColor(R.color.primary_main));
        likeView.setAlpha(comment.isLikedByCurrentUser() ? 1f : 0.65f);
        likeView.setOnClickListener(v -> {
            likeView.setEnabled(false);
            repository.toggleCommentLike(comment.getId(), updated -> runOnUiThread(() -> {
                likeView.setEnabled(true);
                if (updated == null) {
                    Toast.makeText(this, "Chưa thả tim được bình luận", Toast.LENGTH_SHORT).show();
                    return;
                }
                comment.setLikeCount(updated.getLikeCount());
                comment.setLikedByCurrentUser(updated.isLikedByCurrentUser());
                bindCommentLike(likeView, comment);
            }));
        });
    }

    private String displayName(BlogCommentEntity comment) {
        String name = BlogText.clean(comment.getUserName());
        if (name.isEmpty()) {
            return "Customer " + BlogText.clean(comment.getCustomerId());
        }
        return name;
    }

    private String commentMeta(BlogCommentEntity comment) {
        String time = BlogText.relativeTime(comment.getCreatedAt());
        return time.isEmpty() ? "" : time;
    }


    private void renderMissing() {
        binding.blogDetailContainer.removeAllViews();
        TextView missing = new TextView(this);
        missing.setText("Không tìm thấy bài viết.");
        missing.setTextColor(getColor(R.color.neutral_10));
        missing.setTextSize(14);
        missing.setPadding(dp(28), dp(80), dp(28), dp(28));
        binding.blogDetailContainer.addView(missing);
    }

    private String initials(String name) {
        String clean = BlogText.clean(name);
        if (clean.trim().isEmpty()) {
            return "V";
        }
        return clean.trim().substring(0, 1).toUpperCase();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof android.widget.EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                android.graphics.Rect sendRect = new android.graphics.Rect();
                if (binding != null && binding.blogCommentSend != null) {
                    binding.blogCommentSend.getGlobalVisibleRect(sendRect);
                }
                
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY()) 
                        && !sendRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    android.view.inputmethod.InputMethodManager imm = 
                            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
