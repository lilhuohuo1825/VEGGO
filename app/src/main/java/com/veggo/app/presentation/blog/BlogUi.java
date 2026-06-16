package com.veggo.app.presentation.blog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.card.MaterialCardView;
import com.veggo.app.R;
import com.veggo.app.data.local.entity.BlogEntity;

import java.util.List;

public final class BlogUi {
    public interface CategoryClickListener {
        void onCategoryClick(String category);
    }

    private BlogUi() {
    }

    public static void addTopActions(Activity activity, LinearLayout parent) {
        addTopActions(activity, parent, null);
    }

    public static void addTopActions(Activity activity, LinearLayout parent, String title) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 48)
        ));

        row.addView(backButton(activity));
        if (title == null) {
            View spacer = new View(activity);
            row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        } else {
            TextView titleView = text(activity, title, 22, R.color.neutral_100, true);
            titleView.setGravity(Gravity.CENTER);
            row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        row.addView(circleButton(activity, R.drawable.ic_search, null));
    }

    public static ImageButton createBackButton(Activity activity) {
        return backButton(activity);
    }

    public static void addSectionHeader(
            Activity activity,
            LinearLayout parent,
            String title,
            String action,
            View.OnClickListener listener
    ) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = dp(activity, 14);
        parent.addView(row, rowParams);

        TextView titleView = text(activity, title, 20, R.color.neutral_100, true);
        row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if (action != null) {
            TextView actionView = text(activity, action, 14, R.color.primary_main, false);
            actionView.setGravity(Gravity.END);
            actionView.setOnClickListener(listener);
            row.addView(actionView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        }
    }

    public static void addCenteredTitle(Activity activity, LinearLayout parent, String title) {
        TextView view = text(activity, title, 20, R.color.neutral_100, true);
        view.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(activity, 18);
        parent.addView(view, params);
    }

    public static void addCategoryTabs(Activity activity, LinearLayout parent, List<BlogEntity> blogs) {
        addCategoryTabs(activity, parent, blogs, null, null);
    }

    public static void addCategoryTabs(
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

        String allTitle = "T\u1ea5t c\u1ea3";
        addTab(activity, row, allTitle, selectedCategory == null || selectedCategory.equals(allTitle), listener);
        for (BlogEntity blog : blogs) {
            String category = BlogText.clean(blog.getCategoryTag());
            boolean exists = false;
            for (int i = 0; i < row.getChildCount(); i++) {
                if (((TextView) row.getChildAt(i)).getText().toString().equals(category)) {
                    exists = true;
                    break;
                }
            }
            if (!exists && !category.isEmpty()) {
                addTab(activity, row, category, category.equals(selectedCategory), listener);
            }
        }
    }

    public static void addFeaturedCard(Activity activity, LinearLayout parent, BlogEntity blog) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setClickable(true);
        card.setOnClickListener(v -> openDetail(activity, blog));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(activity, 14);
        parent.addView(card, params);

        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(image, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 198)
        ));
        Glide.with(activity)
                .load(blog.getImageUrl())
                .transform(new CenterCrop(), new RoundedCorners(dp(activity, 8)))
                .into(image);

        addMetaBlock(activity, card, blog, false);
    }

    public static void addPostItem(Activity activity, LinearLayout parent, BlogEntity blog) {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.TOP);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClickable(true);
        row.setOnClickListener(v -> openDetail(activity, blog));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = dp(activity, 14);
        parent.addView(row, rowParams);

        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        row.addView(image, new LinearLayout.LayoutParams(dp(activity, 92), dp(activity, 92)));
        Glide.with(activity)
                .load(blog.getImageUrl())
                .transform(new CenterCrop(), new RoundedCorners(dp(activity, 8)))
                .into(image);

        LinearLayout info = new LinearLayout(activity);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        infoParams.leftMargin = dp(activity, 8);
        row.addView(info, infoParams);

        TextView category = text(activity, BlogText.clean(blog.getCategoryTag()), 12, R.color.neutral_70, false);
        info.addView(category);

        TextView title = text(activity, BlogText.clean(blog.getTitle()), 14, R.color.neutral_100, false);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dp(activity, 5);
        info.addView(title, titleParams);

        LinearLayout meta = metaRow(activity, blog);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        metaParams.topMargin = dp(activity, 7);
        info.addView(meta, metaParams);

        TextView more = text(activity, "...", 18, R.color.neutral_70, false);
        more.setGravity(Gravity.TOP);
        row.addView(more, new LinearLayout.LayoutParams(dp(activity, 26), ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public static void renderDetail(Activity activity, LinearLayout parent, BlogEntity blog) {
        FrameLayout hero = new FrameLayout(activity);
        parent.addView(hero, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 340)
        ));

        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        hero.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        Glide.with(activity)
                .load(blog.getImageUrl())
                .transform(new CenterCrop(), new RoundedCorners(dp(activity, 12)))
                .into(image);

        MaterialCardView card = new MaterialCardView(activity);
        card.setCardBackgroundColor(ContextCompat.getColor(activity, R.color.neutral_10));
        card.setCardElevation(0f);
        card.setRadius(dp(activity, 14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.leftMargin = dp(activity, 4);
        cardParams.rightMargin = dp(activity, 4);
        cardParams.topMargin = dp(activity, -6);
        parent.addView(card, cardParams);

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 28), dp(activity, 30), dp(activity, 28), dp(activity, 28));
        card.addView(content);

        LinearLayout authorRow = new LinearLayout(activity);
        authorRow.setGravity(Gravity.CENTER_VERTICAL);
        authorRow.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(authorRow);

        TextView avatar = text(activity, initials(blog.getAuthor()), 12, R.color.neutral_10, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(oval(activity, R.color.primary_main));
        authorRow.addView(avatar, new LinearLayout.LayoutParams(dp(activity, 38), dp(activity, 38)));

        TextView author = text(activity, BlogText.clean(blog.getAuthor()), 14, R.color.neutral_100, true);
        LinearLayout.LayoutParams authorParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        authorParams.leftMargin = dp(activity, 12);
        authorRow.addView(author, authorParams);

        authorRow.addView(icon(activity, R.drawable.ic_heart_full, R.color.danger_main));
        authorRow.addView(icon(activity, R.drawable.ic_message, R.color.neutral_70));
        authorRow.addView(icon(activity, R.drawable.ic_share_grey, R.color.neutral_70));

        TextView category = text(activity, BlogText.clean(blog.getCategoryTag()), 12, R.color.neutral_70, false);
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        categoryParams.topMargin = dp(activity, 18);
        content.addView(category, categoryParams);

        TextView title = text(activity, BlogText.clean(blog.getTitle()), 20, R.color.neutral_100, true);
        title.setLineSpacing(dp(activity, 2), 1f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dp(activity, 10);
        content.addView(title, titleParams);

        TextView date = text(activity, BlogText.dateTime(blog.getPublishedAt()), 12, R.color.neutral_60, false);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dateParams.topMargin = dp(activity, 8);
        content.addView(date, dateParams);

        View divider = new View(activity);
        divider.setBackgroundColor(ContextCompat.getColor(activity, R.color.neutral_30));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(activity, 1)
        );
        dividerParams.topMargin = dp(activity, 14);
        content.addView(divider, dividerParams);

        TextView body = text(activity, "", 16, R.color.neutral_90, false);
        body.setLineSpacing(dp(activity, 4), 1f);
        body.setText(Html.fromHtml(BlogText.clean(blog.getContent()), Html.FROM_HTML_MODE_LEGACY));
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        bodyParams.topMargin = dp(activity, 16);
        content.addView(body, bodyParams);
    }

    private static void addMetaBlock(Activity activity, LinearLayout parent, BlogEntity blog, boolean compact) {
        TextView category = text(activity, BlogText.clean(blog.getCategoryTag()), 12, R.color.neutral_70, false);
        LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        categoryParams.topMargin = dp(activity, 12);
        parent.addView(category, categoryParams);

        TextView title = text(activity, BlogText.clean(blog.getTitle()), compact ? 14 : 15, R.color.neutral_100, false);
        title.setLineSpacing(dp(activity, 3), 1f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = dp(activity, 7);
        parent.addView(title, titleParams);

        LinearLayout meta = metaRow(activity, blog);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        metaParams.topMargin = dp(activity, 8);
        parent.addView(meta, metaParams);
    }

    private static LinearLayout metaRow(Activity activity, BlogEntity blog) {
        LinearLayout meta = new LinearLayout(activity);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.setOrientation(LinearLayout.HORIZONTAL);

        TextView logo = text(activity, "NEWS", 7, R.color.neutral_10, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(ovalColor(0xFFD71920));
        meta.addView(logo, new LinearLayout.LayoutParams(dp(activity, 24), dp(activity, 24)));

        TextView author = text(activity, BlogText.clean(blog.getAuthor()), 12, R.color.neutral_80, true);
        LinearLayout.LayoutParams authorParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        authorParams.leftMargin = dp(activity, 5);
        meta.addView(author, authorParams);

        TextView time = text(activity, "  ◷ 14m ago", 12, R.color.neutral_70, false);
        meta.addView(time);
        return meta;
    }

    private static TextView text(Activity activity, String value, int sp, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(ContextCompat.getColor(activity, color));
        view.setTextSize(sp);
        view.setIncludeFontPadding(true);
        view.setFontFeatureSettings("kern");
        if (bold) {
            try {
                view.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(activity, R.font.inter_bold));
            } catch (Exception e) {
                view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            }
        } else {
            try {
                view.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(activity, R.font.inter));
            } catch (Exception e) {
                view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            }
        }
        return view;
    }

    private static ImageView icon(Activity activity, int drawable, int tint) {
        ImageView image = new ImageView(activity);
        image.setImageResource(drawable);
        image.setColorFilter(ContextCompat.getColor(activity, tint));
        image.setPadding(dp(activity, 8), dp(activity, 8), dp(activity, 8), dp(activity, 8));
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        image.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 40), dp(activity, 40)));
        return image;
    }

    private static ImageButton circleButton(Activity activity, int drawable, View.OnClickListener listener) {
        ImageButton button = new ImageButton(activity);
        button.setImageResource(drawable);
        button.setBackground(oval(activity, R.color.primary_bg));
        button.setColorFilter(ContextCompat.getColor(activity, R.color.primary_main));
        button.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setAdjustViewBounds(false);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 44), dp(activity, 44)));
        if (listener != null) {
            button.setOnClickListener(listener);
        }
        return button;
    }

    private static ImageButton backButton(Activity activity) {
        ImageButton button = new ImageButton(activity);
        button.setImageResource(R.drawable.ic_left);
        button.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.transparent));
        button.setColorFilter(ContextCompat.getColor(activity, R.color.primary_hover));
        button.setPadding(dp(activity, 6), dp(activity, 6), dp(activity, 6), dp(activity, 6));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setAdjustViewBounds(false);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 40), dp(activity, 40)));
        button.setOnClickListener(v -> activity.finish());
        return button;
    }

    private static void addTab(Activity activity, LinearLayout row, String title, boolean selected) {
        addTab(activity, row, title, selected, null);
    }

    private static void addTab(
            Activity activity,
            LinearLayout row,
            String title,
            boolean selected,
            CategoryClickListener listener
    ) {
        TextView tab = text(activity, title, 14, selected ? R.color.primary_main : R.color.neutral_70, false);
        tab.setPadding(0, 0, dp(activity, 16), dp(activity, 8));
        tab.setSingleLine(true);
        if (listener != null) {
            tab.setOnClickListener(v -> listener.onCategoryClick(title));
        }
        row.addView(tab);
    }

    private static GradientDrawable oval(Activity activity, int color) {
        return ovalColor(ContextCompat.getColor(activity, color));
    }

    private static GradientDrawable ovalColor(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private static String initials(String name) {
        String clean = BlogText.clean(name);
        if (clean.trim().isEmpty()) {
            return "V";
        }
        return clean.trim().substring(0, 1).toUpperCase();
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
