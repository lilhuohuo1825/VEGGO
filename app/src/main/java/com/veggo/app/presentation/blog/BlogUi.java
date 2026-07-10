package com.veggo.app.presentation.blog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.veggo.app.R;
import com.veggo.app.core.favorite.FavoriteStore;
import com.veggo.app.data.local.entity.BlogEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class BlogUi {
    public interface CategoryClickListener {
        void onCategoryClick(String category);
    }

    public interface BlogLikeChangedListener {
        void onBlogLikeChanged();
    }

    public static final class CategoryTabsHolder {
        public final HorizontalScrollView scrollView;
        public final List<TextView> tabViews;
        public final com.veggo.app.core.ui.CurvedTabIndicatorHelper indicatorHelper;
        public final List<String> categories;

        public CategoryTabsHolder(
                HorizontalScrollView scrollView,
                List<TextView> tabViews,
                com.veggo.app.core.ui.CurvedTabIndicatorHelper indicatorHelper,
                List<String> categories
        ) {
            this.scrollView = scrollView;
            this.tabViews = tabViews;
            this.indicatorHelper = indicatorHelper;
            this.categories = categories;
        }
    }

    private BlogUi() {
    }

    public static void addTopActions(Activity activity, LinearLayout parent) {
        addTopActions(activity, parent, null, new ArrayList<>());
    }

    public static void addTopActions(Activity activity, LinearLayout parent, String title) {
        addTopActions(activity, parent, title, new ArrayList<>());
    }

    public static void addTopActions(Activity activity, LinearLayout parent, String title, List<BlogEntity> searchableBlogs) {
        View row = LayoutInflater.from(activity).inflate(R.layout.layout_blog_top_actions, parent, false);
        ImageButton back = row.findViewById(R.id.blogBackButton);
        ImageButton search = row.findViewById(R.id.blogSearchButton);
        TextView titleView = row.findViewById(R.id.blogHeaderTitle);

        titleView.setText(title == null ? "" : title);
        back.setOnClickListener(v -> activity.finish());
        search.setOnClickListener(v -> activity.startActivity(new Intent(activity, BlogSearchActivity.class)));
        parent.addView(row);
    }

    public static ImageButton createBackButton(Activity activity) {
        View row = LayoutInflater.from(activity).inflate(R.layout.layout_blog_top_actions, new LinearLayout(activity), false);
        ImageButton button = row.findViewById(R.id.blogBackButton);
        ((ViewGroup) row).removeView(button);
        button.setOnClickListener(v -> activity.finish());
        return button;
    }

    public static void addSectionHeader(
            Activity activity,
            LinearLayout parent,
            String title,
            String action,
            View.OnClickListener listener
    ) {
        View row = LayoutInflater.from(activity).inflate(R.layout.layout_blog_section_header, parent, false);
        ((TextView) row.findViewById(R.id.blogSectionTitle)).setText(title);
        TextView actionView = row.findViewById(R.id.blogSectionAction);
        if (action == null) {
            actionView.setVisibility(View.GONE);
        } else {
            actionView.setText(action);
            actionView.setOnClickListener(listener);
        }
        parent.addView(row);
    }

    public static CategoryTabsHolder addCategoryTabs(
            Activity activity,
            LinearLayout parent,
            List<BlogEntity> blogs,
            String selectedCategory,
            CategoryClickListener listener
    ) {
        HorizontalScrollView scrollView = new HorizontalScrollView(activity);
        scrollView.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(row);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        scrollParams.topMargin = dp(activity, 16);
        parent.addView(scrollView, scrollParams);

        Set<String> categories = new LinkedHashSet<>();
        categories.add("Tất cả");
        for (BlogEntity blog : blogs) {
            String category = BlogText.clean(blog.getCategoryTag());
            if (!category.isEmpty()) {
                categories.add(category);
            }
        }

        List<String> categoriesList = new ArrayList<>(categories);
        List<TextView> tabViews = new ArrayList<>();
        List<com.veggo.app.core.ui.CurvedTabIndicatorHelper.TabItem> tabItems = new ArrayList<>();
        int selectedIndex = 0;
        int index = 0;
        for (String category : categoriesList) {
            boolean isSelected = category.equals(selectedCategory);
            if (isSelected) {
                selectedIndex = index;
            }
            TextView tabView = addTab(activity, row, category, isSelected, listener);
            tabViews.add(tabView);
            tabItems.add(new com.veggo.app.core.ui.CurvedTabIndicatorHelper.TabItem(tabView));
            index++;
        }

        com.veggo.app.core.ui.CurvedTabIndicatorHelper indicatorHelper = null;
        if (!tabItems.isEmpty()) {
            indicatorHelper = com.veggo.app.core.ui.CurvedTabIndicatorHelper.attach(
                    scrollView,
                    tabItems.toArray(new com.veggo.app.core.ui.CurvedTabIndicatorHelper.TabItem[0])
            );
            indicatorHelper.selectTab(selectedIndex, false);
        }

        return new CategoryTabsHolder(scrollView, tabViews, indicatorHelper, categoriesList);
    }

    public static void addFeaturedCard(Activity activity, LinearLayout parent, BlogEntity blog) {
        addFeaturedCard(activity, parent, blog, null, null);
    }

    public static void addFeaturedCard(
            Activity activity,
            LinearLayout parent,
            BlogEntity blog,
            BlogRepository repository,
            BlogLikeChangedListener listener
    ) {
        View item = LayoutInflater.from(activity).inflate(R.layout.item_blog_featured, parent, false);
        item.setOnClickListener(v -> openDetail(activity, blog));
        bindImage(activity, item.findViewById(R.id.blogFeaturedImage), blog.getImageUrl(), 8);
        ((TextView) item.findViewById(R.id.blogFeaturedCategory)).setText(BlogText.clean(blog.getCategoryTag()));
        ((TextView) item.findViewById(R.id.blogFeaturedTitle)).setText(BlogText.clean(blog.getTitle()));
        bindMeta(item, blog);
        bindBlogLikeButton(activity, item.findViewById(R.id.blogFeaturedLikeButton), blog, repository, listener);
        parent.addView(item);
    }

    public static void addPostItem(Activity activity, LinearLayout parent, BlogEntity blog) {
        addPostItem(activity, parent, blog, null, null);
    }

    public static void addPostItem(
            Activity activity,
            LinearLayout parent,
            BlogEntity blog,
            BlogRepository repository,
            BlogLikeChangedListener listener
    ) {
        View item = LayoutInflater.from(activity).inflate(R.layout.item_blog_post, parent, false);
        item.setOnClickListener(v -> openDetail(activity, blog));
        bindImage(activity, item.findViewById(R.id.blogPostImage), blog.getImageUrl(), 8);
        ((TextView) item.findViewById(R.id.blogPostCategory)).setText(BlogText.clean(blog.getCategoryTag()));
        ((TextView) item.findViewById(R.id.blogPostTitle)).setText(BlogText.clean(blog.getTitle()));
        bindMeta(item, blog);
        bindBlogLikeButton(activity, item.findViewById(R.id.blogPostLikeButton), blog, repository, listener);
        parent.addView(item);
    }

    public static void showRandomPostPopup(Activity activity, List<BlogEntity> blogs) {
        if (blogs == null || blogs.isEmpty() || activity.isFinishing()) {
            return;
        }
        BlogEntity blog = blogs.get(new Random().nextInt(blogs.size()));
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_blog_random, null, false);
        bindImage(activity, view.findViewById(R.id.blogRandomImage), blog.getImageUrl(), 12);
        ((TextView) view.findViewById(R.id.blogRandomTitle)).setText(BlogText.clean(blog.getTitle()));
        ((TextView) view.findViewById(R.id.blogRandomExcerpt)).setText(BlogText.clean(blog.getExcerpt()));
        AlertDialog dialog = new AlertDialog.Builder(activity).setView(view).create();
        view.setOnClickListener(v -> {
            dialog.dismiss();
            openDetail(activity, blog);
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = Math.min(
                    activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 36),
                    dp(activity, 380)
            );
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
    }

    private static TextView addTab(Activity activity, LinearLayout row, String title, boolean selected, CategoryClickListener listener) {
        TextView tab = (TextView) LayoutInflater.from(activity).inflate(R.layout.item_blog_category_tab, row, false);
        tab.setText(title);
        tab.setTextColor(ContextCompat.getColor(activity, selected ? R.color.primary_main : R.color.neutral_70));
        android.graphics.Typeface tf = androidx.core.content.res.ResourcesCompat.getFont(activity, selected ? R.font.inter_semibold : R.font.inter_regular);
        tab.setTypeface(tf);
        tab.setOnClickListener(v -> listener.onCategoryClick(title));
        row.addView(tab);
        return tab;
    }

    private static void bindMeta(View root, BlogEntity blog) {
        TextView author = root.findViewById(R.id.blogMetaAuthor);
        if (author != null) {
            author.setText(BlogText.clean(blog.getAuthor()));
        }
        TextView time = root.findViewById(R.id.blogMetaTime);
        if (time != null) {
            String published = BlogText.date(blog.getPublishedAt());
            time.setText(published.isEmpty() ? "" : "  • " + published);
        }
    }

    private static void bindBlogLikeButton(
            Activity activity,
            ImageButton button,
            BlogEntity blog,
            BlogRepository repository,
            BlogLikeChangedListener listener
    ) {
        if (button == null) {
            return;
        }
        FavoriteStore favoriteStore = new FavoriteStore(activity);
        renderBlogLikeButton(button, blog.isLikedByCurrentUser());
        button.setOnClickListener(v -> {
            button.setEnabled(false);
            boolean selected = !blog.isLikedByCurrentUser();
            syncFavorite(favoriteStore, blog, selected);
            blog.setLikedByCurrentUser(selected);
            renderBlogLikeButton(button, selected);
            if (listener != null) {
                listener.onBlogLikeChanged();
            }
            if (repository == null) {
                button.setEnabled(true);
                return;
            }
            repository.setBlogLike(blog.getId(), selected, updated -> activity.runOnUiThread(() -> {
                button.setEnabled(true);
                if (updated == null) {
                    Toast.makeText(activity, selected ? "Đã lưu bài viết" : "Đã xoá khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    return;
                }
                blog.setLikeCount(updated.getLikeCount());
                blog.setLikedByCurrentUser(updated.isLikedByCurrentUser());
                syncFavorite(favoriteStore, blog, updated.isLikedByCurrentUser());
                renderBlogLikeButton(button, updated.isLikedByCurrentUser());
                if (listener != null) {
                    listener.onBlogLikeChanged();
                }
            }));
        });
    }

    private static void renderBlogLikeButton(ImageButton button, boolean selected) {
        button.setImageResource(selected
                ? R.drawable.ic_profile_menu_heart_filled
                : R.drawable.ic_heart_outline_green);
        button.setAlpha(selected ? 1f : 0.92f);
    }

    private static void syncFavorite(FavoriteStore favoriteStore, BlogEntity blog, boolean selected) {
        if (selected) {
            favoriteStore.add(new FavoriteStore.FavoriteItem(
                    FavoriteStore.TYPE_BLOG,
                    blog.getId(),
                    BlogText.clean(blog.getTitle()),
                    BlogText.clean(blog.getAuthor()),
                    blog.getImageUrl()
            ));
        } else {
            favoriteStore.remove(FavoriteStore.TYPE_BLOG, blog.getId());
        }
    }

    private static void bindImage(Activity activity, ImageView image, String imageUrl, int radiusDp) {
        Glide.with(activity)
                .load(imageUrl)
                .transform(new CenterCrop(), new RoundedCorners(dp(activity, radiusDp)))
                .into(image);
    }

    private static void openDetail(Activity activity, BlogEntity blog) {
        Intent intent = new Intent(activity, BlogDetailActivity.class);
        intent.putExtra(BlogDetailActivity.EXTRA_BLOG_ID, blog.getId());
        activity.startActivity(intent);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
