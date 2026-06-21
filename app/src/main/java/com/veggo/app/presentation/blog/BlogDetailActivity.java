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
import com.veggo.app.data.local.entity.BlogCommentEntity;
import com.veggo.app.data.local.entity.BlogEntity;
import com.veggo.app.databinding.ActivityBlogDetailBinding;

import java.util.List;

public class BlogDetailActivity extends AppCompatActivity {
    public static final String EXTRA_BLOG_ID = "blog_id";

    private ActivityBlogDetailBinding binding;
    private BlogRepository repository;
    private String blogId;
    private BlogEntity currentBlog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlogDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupFixedBackButton();
        repository = new BlogRepository(this);

        blogId = getIntent().getStringExtra(EXTRA_BLOG_ID);
        binding.blogCommentSend.setOnClickListener(v -> submitComment());
        binding.blogDetailCommentIcon.setOnClickListener(v -> binding.blogDetailScroll.post(() ->
                binding.blogDetailScroll.smoothScrollTo(0, binding.blogCommentsTitle.getTop())));

        if (blogId == null) {
            renderMissing();
            return;
        }
        repository.getById(blogId, blog -> runOnUiThread(() -> {
            if (blog == null) {
                renderMissing();
            } else {
                bindBlog(blog);
            }
        }));
    }

    private void setupFixedBackButton() {
        binding.blogDetailHeader.addView(BlogUi.createBackButton(this));
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
        binding.blogDetailLikeIcon.setImageTintList(ColorStateList.valueOf(
                getColor(blog.isLikedByCurrentUser() ? R.color.danger_main : R.color.neutral_70)
        ));
        binding.blogDetailLikeIcon.setOnClickListener(v -> toggleBlogLike());
    }

    private void toggleBlogLike() {
        if (blogId == null || currentBlog == null) {
            return;
        }
        binding.blogDetailLikeIcon.setEnabled(false);
        repository.toggleBlogLike(blogId, updated -> runOnUiThread(() -> {
            binding.blogDetailLikeIcon.setEnabled(true);
            if (updated == null) {
                Toast.makeText(this, "Chưa thả tim được bài viết", Toast.LENGTH_SHORT).show();
                return;
            }
            currentBlog.setLikeCount(updated.getLikeCount());
            currentBlog.setLikedByCurrentUser(updated.isLikedByCurrentUser());
            bindBlogLike(currentBlog);
        }));
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
        likeView.setTextColor(getColor(comment.isLikedByCurrentUser() ? R.color.danger_main : R.color.neutral_60));
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
}
