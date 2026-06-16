package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.gson.JsonElement;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.database.AssetRepository;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.blog.BlogDetailActivity;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.product.ProductDetailActivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritesActivity extends BaseActivity {
    private static final int MAX_SECTION_ITEMS = 6;

    private View tabProducts, tabBlogs, tabDishes;
    private TextView tabProductsText, tabBlogsText, tabDishesText;
    private TextView tabProductsBadge, tabBlogsBadge, tabDishesBadge;
    private View tabProductsIndicator, tabBlogsIndicator, tabDishesIndicator;
    private View sectionProducts, sectionBlogs, sectionDishes;

    private enum FavTab {
        PRODUCTS, BLOGS, DISHES
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        findViewById(R.id.favoritesBackButton).setOnClickListener(v -> finish());

        // Initialize Tab Views
        tabProducts = findViewById(R.id.favTabProducts);
        tabBlogs = findViewById(R.id.favTabBlogs);
        tabDishes = findViewById(R.id.favTabDishes);

        tabProductsText = findViewById(R.id.favTabProductsText);
        tabBlogsText = findViewById(R.id.favTabBlogsText);
        tabDishesText = findViewById(R.id.favTabDishesText);

        tabProductsBadge = findViewById(R.id.favTabProductsBadge);
        tabBlogsBadge = findViewById(R.id.favTabBlogsBadge);
        tabDishesBadge = findViewById(R.id.favTabDishesBadge);

        tabProductsIndicator = findViewById(R.id.favTabProductsIndicator);
        tabBlogsIndicator = findViewById(R.id.favTabBlogsIndicator);
        tabDishesIndicator = findViewById(R.id.favTabDishesIndicator);

        sectionProducts = findViewById(R.id.sectionProducts);
        sectionBlogs = findViewById(R.id.sectionBlogs);
        sectionDishes = findViewById(R.id.sectionDishes);

        // Click listeners for tabs
        tabProducts.setOnClickListener(v -> selectTab(FavTab.PRODUCTS));
        tabBlogs.setOnClickListener(v -> selectTab(FavTab.BLOGS));
        tabDishes.setOnClickListener(v -> selectTab(FavTab.DISHES));

        // Set default tab
        selectTab(FavTab.PRODUCTS);

        loadFavorites();
    }

    private void selectTab(FavTab tab) {
        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary_main);
        int inactiveColor = androidx.core.content.ContextCompat.getColor(this, R.color.neutral_60);

        // Update Tab Texts, Indicators and Badges
        tabProductsText.setTextColor(tab == FavTab.PRODUCTS ? activeColor : inactiveColor);
        tabProductsText.setTypeface(null, tab == FavTab.PRODUCTS ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabProductsIndicator.setVisibility(tab == FavTab.PRODUCTS ? View.VISIBLE : View.INVISIBLE);
        tabProductsBadge.setBackgroundResource(tab == FavTab.PRODUCTS ? R.drawable.bg_notification_badge_alert : R.drawable.bg_notification_badge_dark);

        tabBlogsText.setTextColor(tab == FavTab.BLOGS ? activeColor : inactiveColor);
        tabBlogsText.setTypeface(null, tab == FavTab.BLOGS ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabBlogsIndicator.setVisibility(tab == FavTab.BLOGS ? View.VISIBLE : View.INVISIBLE);
        tabBlogsBadge.setBackgroundResource(tab == FavTab.BLOGS ? R.drawable.bg_notification_badge_alert : R.drawable.bg_notification_badge_dark);

        tabDishesText.setTextColor(tab == FavTab.DISHES ? activeColor : inactiveColor);
        tabDishesText.setTypeface(null, tab == FavTab.DISHES ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabDishesIndicator.setVisibility(tab == FavTab.DISHES ? View.VISIBLE : View.INVISIBLE);
        tabDishesBadge.setBackgroundResource(tab == FavTab.DISHES ? R.drawable.bg_notification_badge_alert : R.drawable.bg_notification_badge_dark);

        // Toggle Section Visibility
        sectionProducts.setVisibility(tab == FavTab.PRODUCTS ? View.VISIBLE : View.GONE);
        sectionBlogs.setVisibility(tab == FavTab.BLOGS ? View.VISIBLE : View.GONE);
        sectionDishes.setVisibility(tab == FavTab.DISHES ? View.VISIBLE : View.GONE);
    }

    private void loadFavorites() {
        new Thread(() -> {
            AssetRepository repository = new AssetRepository(this);
            FavoriteSnapshot snapshot = new FavoriteSnapshot(
                    favoriteProducts(repository.getProducts()),
                    favoriteBlogs(repository.getBlogs()),
                    favoriteDishes(repository.getDishes(), repository.getInstructions())
            );
            runOnUiThread(() -> bindFavorites(snapshot));
        }).start();
    }

    private List<AssetModels.Product> favoriteProducts(List<AssetModels.Product> products) {
        List<AssetModels.Product> favorites = new ArrayList<>(products);
        favorites.sort((left, right) -> Integer.compare(right.liked, left.liked));
        return limit(favorites);
    }

    private List<AssetModels.Blog> favoriteBlogs(List<AssetModels.Blog> blogs) {
        List<AssetModels.Blog> favorites = new ArrayList<>(blogs);
        favorites.sort((left, right) -> AssetScreenData.safe(right.pubDate == null ? null : right.pubDate.date)
                .compareTo(AssetScreenData.safe(left.pubDate == null ? null : left.pubDate.date)));
        return limit(favorites);
    }

    private List<FavoriteDish> favoriteDishes(List<AssetModels.Dish> dishes, List<AssetModels.Instruction> instructions) {
        Map<String, AssetModels.Instruction> instructionById = new HashMap<>();
        for (AssetModels.Instruction instruction : instructions) {
            instructionById.put(instruction.id, instruction);
        }

        List<FavoriteDish> favorites = new ArrayList<>();
        for (AssetModels.Dish dish : dishes) {
            AssetModels.Instruction instruction = instructionById.get(dish.id);
            if (instruction != null && AssetScreenData.hasText(instruction.dishName)) {
                favorites.add(new FavoriteDish(dish, instruction));
            }
        }
        favorites.sort(Comparator.comparing(item -> item.instruction.dishName));
        return limit(favorites);
    }

    private void bindFavorites(FavoriteSnapshot snapshot) {
        tabProductsText.setText("Sản phẩm");
        tabBlogsText.setText("Bài viết");
        tabDishesText.setText("Công thức");

        tabProductsBadge.setText(String.valueOf(snapshot.products.size()));
        tabBlogsBadge.setText(String.valueOf(snapshot.blogs.size()));
        tabDishesBadge.setText(String.valueOf(snapshot.dishes.size()));

        AssetScreenData.setText(findViewById(android.R.id.content), R.id.favoritesProductTotal,
                snapshot.products.size() + "\n" + getString(R.string.favorites_products_count));
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.favoritesBlogTotal,
                snapshot.blogs.size() + "\n" + getString(R.string.favorites_blogs_count));
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.favoritesDishTotal,
                snapshot.dishes.size() + "\n" + getString(R.string.favorites_dishes_count));

        LinearLayout productList = findViewById(R.id.favoritesProductList);
        LinearLayout blogList = findViewById(R.id.favoritesBlogList);
        LinearLayout dishList = findViewById(R.id.favoritesDishList);
        productList.removeAllViews();
        blogList.removeAllViews();
        dishList.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (AssetModels.Product product : snapshot.products) {
            View item = inflater.inflate(R.layout.item_favorite_card, productList, false);
            bindItem(item, getString(R.string.favorites_products_count), product.productName,
                    AssetScreenData.money(product.price) + " • " + product.liked + " lượt thích",
                    firstImage(product.image));
            item.setOnClickListener(v -> {
                Intent intent = new Intent(this, ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.objectId);
                startActivity(intent);
            });
            productList.addView(item);
        }

        for (AssetModels.Blog blog : snapshot.blogs) {
            View item = inflater.inflate(R.layout.item_favorite_card, blogList, false);
            bindItem(item, getString(R.string.favorites_blogs_count), blog.title,
                    blog.author + " • " + AssetScreenData.date(blog.pubDate),
                    blog.img);
            String blogId = blogId(blog);
            if (AssetScreenData.hasText(blogId)) {
                item.setOnClickListener(v -> {
                    Intent intent = new Intent(this, BlogDetailActivity.class);
                    intent.putExtra(BlogDetailActivity.EXTRA_BLOG_ID, blogId);
                    startActivity(intent);
                });
            }
            blogList.addView(item);
        }

        for (FavoriteDish favoriteDish : snapshot.dishes) {
            AssetModels.Instruction instruction = favoriteDish.instruction;
            AssetModels.Dish dish = favoriteDish.dish;
            View item = inflater.inflate(R.layout.item_favorite_card, dishList, false);
            bindItem(item, getString(R.string.favorites_dishes_count), instruction.dishName,
                    instruction.cookingTime + " • " + instruction.difficulty + " • " + firstSentence(dish.description),
                    instruction.image);
            dishList.addView(item);
        }
    }

    private void bindItem(View item, String type, String title, String subtitle, @Nullable String imageUrl) {
        AssetScreenData.setText(item, R.id.favoriteType, type);
        AssetScreenData.setText(item, R.id.favoriteTitle, title);
        AssetScreenData.setText(item, R.id.favoriteSubtitle, subtitle);
        ImageView image = item.findViewById(R.id.favoriteImage);
        if (image != null && AssetScreenData.hasText(imageUrl)) {
            Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_vegetable).into(image);
        }
    }

    @Nullable
    private String firstImage(@Nullable List<String> images) {
        return images == null || images.isEmpty() ? null : images.get(0);
    }

    @Nullable
    private String blogId(AssetModels.Blog blog) {
        JsonElement objectId = blog.objectId;
        if (objectId != null && objectId.isJsonPrimitive()) {
            return objectId.getAsString();
        }
        return blog.id;
    }

    private String firstSentence(@Nullable String value) {
        if (!AssetScreenData.hasText(value)) {
            return "";
        }
        int end = value.indexOf('.');
        String sentence = end > 0 ? value.substring(0, end + 1) : value;
        return sentence.length() > 80 ? sentence.substring(0, 77) + "..." : sentence;
    }

    private <T> List<T> limit(List<T> source) {
        if (source.size() <= MAX_SECTION_ITEMS) {
            return source;
        }
        return new ArrayList<>(source.subList(0, MAX_SECTION_ITEMS));
    }

    private static final class FavoriteSnapshot {
        final List<AssetModels.Product> products;
        final List<AssetModels.Blog> blogs;
        final List<FavoriteDish> dishes;

        FavoriteSnapshot(
                List<AssetModels.Product> products,
                List<AssetModels.Blog> blogs,
                List<FavoriteDish> dishes
        ) {
            this.products = products;
            this.blogs = blogs;
            this.dishes = dishes;
        }
    }

    private static final class FavoriteDish {
        final AssetModels.Dish dish;
        final AssetModels.Instruction instruction;

        FavoriteDish(AssetModels.Dish dish, AssetModels.Instruction instruction) {
            this.dish = dish;
            this.instruction = instruction;
        }
    }
}
