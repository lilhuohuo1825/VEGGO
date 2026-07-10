package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.gson.JsonElement;
import com.veggo.app.R;
import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetJsonLoader;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.favorite.FavoriteStore;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.veggo.app.core.ui.BadgeUiHelper;
import com.veggo.app.core.ui.CurvedTabIndicatorHelper;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.data.local.entity.BlogEntity;
import com.veggo.app.data.remote.api.BlogApi;
import com.veggo.app.data.remote.api.ProductApi;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.presentation.blog.BlogDetailActivity;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.community.InstructionRecipeDetailActivity;
import com.veggo.app.presentation.dialog.VeggoDialog;
import com.veggo.app.presentation.product.ProductDetailActivity;

import java.util.Locale;
import java.util.List;

public class FavoritesActivity extends BaseActivity {
    private static final int SWIPE_DELETE_WIDTH_DP = 96;
    private static final int SWIPE_THRESHOLD_DP = 56;

    private View tabProducts, tabBlogs, tabDishes;
    private TextView tabProductsText, tabBlogsText, tabDishesText;
    private TextView tabProductsBadge, tabBlogsBadge, tabDishesBadge;
    private View tabProductsIndicator, tabBlogsIndicator, tabDishesIndicator;
    private CurvedTabIndicatorHelper tabIndicator;
    private View sectionProducts, sectionBlogs, sectionDishes;
    private FavoriteStore favoriteStore;
    private boolean isEnrichingFavorites;
    private SwipeRefreshLayout favoritesRefreshLayout;
    private boolean favoritesRefreshPending;

    private enum FavTab {
        PRODUCTS, BLOGS, DISHES
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "yêu thích")) {
            return;
        }
        setContentView(R.layout.activity_favorites);
        favoriteStore = new FavoriteStore(this);
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

        tabIndicator = CurvedTabIndicatorHelper.attach(
                null,
                new CurvedTabIndicatorHelper.TabItem(tabProducts, tabProductsIndicator),
                new CurvedTabIndicatorHelper.TabItem(tabBlogs, tabBlogsIndicator),
                new CurvedTabIndicatorHelper.TabItem(tabDishes, tabDishesIndicator)
        );

        sectionProducts = findViewById(R.id.sectionProducts);
        sectionBlogs = findViewById(R.id.sectionBlogs);
        sectionDishes = findViewById(R.id.sectionDishes);

        // Click listeners for tabs
        tabProducts.setOnClickListener(v -> selectTab(FavTab.PRODUCTS));
        tabBlogs.setOnClickListener(v -> selectTab(FavTab.BLOGS));
        tabDishes.setOnClickListener(v -> selectTab(FavTab.DISHES));

        // Set default tab
        selectTab(FavTab.PRODUCTS);
        setupPullToRefresh();
        loadFavorites();
    }

    private void setupPullToRefresh() {
        favoritesRefreshLayout = PullToRefreshHelper.wrap(
                findViewById(R.id.favoritesScroll),
                () -> {
                    favoritesRefreshPending = true;
                    loadFavorites();
                }
        );
    }

    private void finishFavoritesRefreshIfReady() {
        if (favoritesRefreshPending && !isEnrichingFavorites) {
            favoritesRefreshPending = false;
            PullToRefreshHelper.finish(favoritesRefreshLayout);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (favoriteStore != null) {
            loadFavorites();
        }
    }

    private void selectTab(FavTab tab) {
        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary_main);
        int inactiveColor = androidx.core.content.ContextCompat.getColor(this, R.color.neutral_60);

        // Update Tab Texts, Indicators and Badges
        tabProductsText.setTextColor(tab == FavTab.PRODUCTS ? activeColor : inactiveColor);
        tabProductsText.setTypeface(null, tab == FavTab.PRODUCTS ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        BadgeUiHelper.styleTabBadge(tabProductsBadge, tab == FavTab.PRODUCTS);

        tabBlogsText.setTextColor(tab == FavTab.BLOGS ? activeColor : inactiveColor);
        tabBlogsText.setTypeface(null, tab == FavTab.BLOGS ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        BadgeUiHelper.styleTabBadge(tabBlogsBadge, tab == FavTab.BLOGS);

        tabDishesText.setTextColor(tab == FavTab.DISHES ? activeColor : inactiveColor);
        tabDishesText.setTypeface(null, tab == FavTab.DISHES ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        BadgeUiHelper.styleTabBadge(tabDishesBadge, tab == FavTab.DISHES);

        if (tabIndicator != null) {
            tabIndicator.selectTab(tab.ordinal());
        }

        // Toggle Section Visibility
        sectionProducts.setVisibility(tab == FavTab.PRODUCTS ? View.VISIBLE : View.GONE);
        sectionBlogs.setVisibility(tab == FavTab.BLOGS ? View.VISIBLE : View.GONE);
        sectionDishes.setVisibility(tab == FavTab.DISHES ? View.VISIBLE : View.GONE);
    }

    private void loadFavorites() {
        List<FavoriteStore.FavoriteItem> products = favoriteStore.getByType(FavoriteStore.TYPE_PRODUCT);
        List<FavoriteStore.FavoriteItem> blogs = favoriteStore.getByType(FavoriteStore.TYPE_BLOG);
        List<FavoriteStore.FavoriteItem> recipes = favoriteStore.getByType(FavoriteStore.TYPE_RECIPE);
        bindFavorites(products, blogs, recipes);
        enrichFavoriteMetadata(products, blogs, recipes);
    }

    private void bindFavorites(
            List<FavoriteStore.FavoriteItem> products,
            List<FavoriteStore.FavoriteItem> blogs,
            List<FavoriteStore.FavoriteItem> recipes
    ) {
        tabProductsText.setText("Sản phẩm");
        tabBlogsText.setText("Bài viết");
        tabDishesText.setText("Công thức");

        if (products.size() > 0) {
            tabProductsBadge.setVisibility(View.VISIBLE);
            tabProductsBadge.setText(String.valueOf(products.size()));
        } else {
            tabProductsBadge.setVisibility(View.GONE);
        }

        if (blogs.size() > 0) {
            tabBlogsBadge.setVisibility(View.VISIBLE);
            tabBlogsBadge.setText(String.valueOf(blogs.size()));
        } else {
            tabBlogsBadge.setVisibility(View.GONE);
        }

        if (recipes.size() > 0) {
            tabDishesBadge.setVisibility(View.VISIBLE);
            tabDishesBadge.setText(String.valueOf(recipes.size()));
        } else {
            tabDishesBadge.setVisibility(View.GONE);
        }

        AssetScreenData.setText(findViewById(android.R.id.content), R.id.favoritesProductTotal,
                products.size() + "\n" + getString(R.string.favorites_products_count));
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.favoritesBlogTotal,
                blogs.size() + "\n" + getString(R.string.favorites_blogs_count));
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.favoritesDishTotal,
                recipes.size() + "\n" + getString(R.string.favorites_dishes_count));

        LinearLayout productList = findViewById(R.id.favoritesProductList);
        LinearLayout blogList = findViewById(R.id.favoritesBlogList);
        LinearLayout dishList = findViewById(R.id.favoritesDishList);
        productList.removeAllViews();
        blogList.removeAllViews();
        dishList.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        bindList(inflater, productList, products);
        bindList(inflater, blogList, blogs);
        bindList(inflater, dishList, recipes);
    }

    private void bindList(
            LayoutInflater inflater,
            LinearLayout list,
            List<FavoriteStore.FavoriteItem> favorites
    ) {
        for (FavoriteStore.FavoriteItem favorite : favorites) {
            View item = inflater.inflate(R.layout.item_favorite_card, list, false);
            bindItem(item, favorite);
            View foreground = item.findViewById(R.id.favoriteForeground);
            foreground.setOnClickListener(v -> openFavorite(favorite));
            item.findViewById(R.id.favoriteDeleteAction)
                    .setOnClickListener(v -> showDeleteDialog(favorite));
            bindSwipeToDelete(item, foreground, favorite);
            list.addView(item);
        }
    }

    private void bindItem(View item, FavoriteStore.FavoriteItem favorite) {
        AssetScreenData.setText(item, R.id.favoriteTitle, favorite.title);
        TextView subtitleView = item.findViewById(R.id.favoriteSubtitle);
        if (subtitleView != null) {
            subtitleView.setText(formatFavoriteSubtitle(favorite));
        }
        ImageView image = item.findViewById(R.id.favoriteImage);
        if (image != null && AssetScreenData.hasText(favorite.imageUrl)) {
            Glide.with(this).load(favorite.imageUrl).placeholder(R.drawable.ic_vegetable).into(image);
        }
    }

    private CharSequence formatFavoriteSubtitle(FavoriteStore.FavoriteItem favorite) {
        String subtitle = favorite.subtitle == null ? "" : favorite.subtitle;
        if (FavoriteStore.TYPE_PRODUCT.equals(favorite.type) && subtitle.contains("điểm carbon")) {
            String display = subtitle.replaceFirst("\\s+•\\s+", "\n");
            SpannableString spannable = new SpannableString(display);
            int carbonStart = display.indexOf("điểm carbon");
            if (carbonStart > 0) {
                int lineStart = display.lastIndexOf('\n', carbonStart);
                int start = lineStart >= 0 ? lineStart + 1 : carbonStart;
                spannable.setSpan(
                        new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primary_main)),
                        start,
                        display.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            return spannable;
        }
        if (FavoriteStore.TYPE_RECIPE.equals(favorite.type) && subtitle.contains("nguyên liệu")) {
            return subtitle.replaceFirst("\\s+•\\s+", "\n");
        }
        return subtitle;
    }

    private void openFavorite(FavoriteStore.FavoriteItem favorite) {
        Intent intent;
        if (FavoriteStore.TYPE_PRODUCT.equals(favorite.type)) {
            intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, favorite.id);
        } else if (FavoriteStore.TYPE_BLOG.equals(favorite.type)) {
            intent = new Intent(this, BlogDetailActivity.class);
            intent.putExtra(BlogDetailActivity.EXTRA_BLOG_ID, favorite.id);
        } else {
            intent = new Intent(this, InstructionRecipeDetailActivity.class);
            intent.putExtra(InstructionRecipeDetailActivity.EXTRA_INSTRUCTION_ID, favorite.id);
        }
        startActivity(intent);
    }

    private void bindSwipeToDelete(View root, View foreground, FavoriteStore.FavoriteItem favorite) {
        final float[] downX = new float[1];
        final int revealWidth = dp(SWIPE_DELETE_WIDTH_DP);
        final int threshold = dp(SWIPE_THRESHOLD_DP);
        foreground.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX[0] = event.getRawX();
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float deltaX = event.getRawX() - downX[0];
                    if (deltaX < -threshold) {
                        foreground.animate().translationX(-revealWidth).setDuration(160).start();
                        root.findViewById(R.id.favoriteDeleteAction).setVisibility(View.VISIBLE);
                        return true;
                    }
                    if (deltaX > threshold || foreground.getTranslationX() < 0) {
                        foreground.animate().translationX(0).setDuration(160).start();
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        });
    }

    private void showDeleteDialog(FavoriteStore.FavoriteItem favorite) {
        VeggoDialog.show(
                this,
                R.drawable.ic_trash,
                "Xoá khỏi yêu thích?",
                "Bạn có chắc muốn xoá \"" + favorite.title + "\" khỏi danh sách yêu thích không?",
                "Xoá",
                "Huỷ",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                        favoriteStore.remove(favorite.type, favorite.id);
                        loadFavorites();
                    }
                }
        );
    }

    private void enrichFavoriteMetadata(
            List<FavoriteStore.FavoriteItem> products,
            List<FavoriteStore.FavoriteItem> blogs,
            List<FavoriteStore.FavoriteItem> recipes
    ) {
        if (isEnrichingFavorites) {
            finishFavoritesRefreshIfReady();
            return;
        }
        isEnrichingFavorites = true;
        new Thread(() -> {
            boolean changed = false;
            try {
                for (FavoriteStore.FavoriteItem product : products) {
                    FavoriteStore.FavoriteItem enriched = enrichProduct(product);
                    if (replaceIfChanged(product, enriched)) changed = true;
                }
                for (FavoriteStore.FavoriteItem blog : blogs) {
                    FavoriteStore.FavoriteItem enriched = enrichBlog(blog);
                    if (replaceIfChanged(blog, enriched)) changed = true;
                }
                for (FavoriteStore.FavoriteItem recipe : recipes) {
                    FavoriteStore.FavoriteItem enriched = enrichRecipe(recipe);
                    if (replaceIfChanged(recipe, enriched)) changed = true;
                }
            } finally {
                boolean shouldReload = changed;
                runOnUiThread(() -> {
                    isEnrichingFavorites = false;
                    if (shouldReload) {
                        loadFavorites();
                    }
                    finishFavoritesRefreshIfReady();
                });
            }
        }).start();
    }

    private FavoriteStore.FavoriteItem enrichProduct(FavoriteStore.FavoriteItem favorite) {
        ProductDto dto = loadProductDto(favorite.id);
        if (dto != null) {
            return new FavoriteStore.FavoriteItem(
                    favorite.type,
                    favorite.id,
                    firstNonBlank(dto.getName(), favorite.title),
                    productSubtitle(dto.getPrice(), dto.getCarbonSavingPoint(), favorite.subtitle),
                    firstNonBlank(dto.getImageUrl(), favorite.imageUrl)
            );
        }

        AssetModels.Product assetProduct = findAssetProduct(favorite.id);
        if (assetProduct == null) {
            return favorite;
        }
        return new FavoriteStore.FavoriteItem(
                favorite.type,
                favorite.id,
                firstNonBlank(assetProduct.productName, favorite.title),
                productSubtitle(assetProduct.price, assetProduct.carbonSavingPoint, favorite.subtitle),
                firstNonBlank(firstImage(assetProduct.image), favorite.imageUrl)
        );
    }

    private FavoriteStore.FavoriteItem enrichBlog(FavoriteStore.FavoriteItem favorite) {
        BlogEntity blog = loadBlogEntity(favorite.id);
        if (blog != null) {
            return new FavoriteStore.FavoriteItem(
                    favorite.type,
                    favorite.id,
                    firstNonBlank(blog.getTitle(), favorite.title),
                    firstNonBlank(blog.getAuthor(), favorite.subtitle),
                    firstNonBlank(blog.getImageUrl(), favorite.imageUrl)
            );
        }

        AssetModels.Blog assetBlog = findAssetBlog(favorite.id);
        if (assetBlog == null) {
            return favorite;
        }
        return new FavoriteStore.FavoriteItem(
                favorite.type,
                favorite.id,
                firstNonBlank(assetBlog.title, favorite.title),
                firstNonBlank(assetBlog.author, favorite.subtitle),
                firstNonBlank(assetBlog.img, favorite.imageUrl)
        );
    }

    private FavoriteStore.FavoriteItem enrichRecipe(FavoriteStore.FavoriteItem favorite) {
        RecipeAssetInfo recipe = findAssetRecipe(favorite.id);
        if (recipe == null) {
            return favorite;
        }
        return new FavoriteStore.FavoriteItem(
                favorite.type,
                favorite.id,
                firstNonBlank(recipe.title, favorite.title),
                recipeSubtitle(recipe.cookingTime, recipe.ingredientCount, favorite.subtitle),
                firstNonBlank(recipe.imageUrl, favorite.imageUrl)
        );
    }

    private boolean replaceIfChanged(
            FavoriteStore.FavoriteItem original,
            FavoriteStore.FavoriteItem enriched
    ) {
        if (enriched == null || sameFavorite(original, enriched)) {
            return false;
        }
        favoriteStore.add(enriched);
        return true;
    }

    private boolean sameFavorite(FavoriteStore.FavoriteItem left, FavoriteStore.FavoriteItem right) {
        return safe(left.title).equals(safe(right.title))
                && safe(left.subtitle).equals(safe(right.subtitle))
                && safe(left.imageUrl).equals(safe(right.imageUrl));
    }

    @Nullable
    private ProductDto loadProductDto(String productId) {
        try {
            retrofit2.Response<ProductDto> response = ApiClient.createService(ProductApi.class)
                    .getProductById(productId)
                    .execute();
            return response.isSuccessful() ? response.body() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private BlogEntity loadBlogEntity(String blogId) {
        try {
            String customerId = new AppPreferences(this).getCustomerId();
            retrofit2.Response<BlogEntity> response = ApiClient.createService(BlogApi.class)
                    .getBlog(blogId, customerId)
                    .execute();
            return response.isSuccessful() ? response.body() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private AssetModels.Product findAssetProduct(String productId) {
        try {
            List<AssetModels.Product> products = new AssetJsonLoader(this)
                    .readList(AssetFiles.PRODUCTS, AssetModels.Product.class);
            for (AssetModels.Product product : products) {
                if (productId.equals(product.objectId) || productId.equals(product.sku)) {
                    return product;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    private AssetModels.Blog findAssetBlog(String blogId) {
        try {
            List<AssetModels.Blog> blogs = new AssetJsonLoader(this)
                    .readList(AssetFiles.BLOGS, AssetModels.Blog.class);
            for (AssetModels.Blog blog : blogs) {
                if (blogId.equals(blog.id) || blogId.equals(assetBlogId(blog))) {
                    return blog;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    private RecipeAssetInfo findAssetRecipe(String favoriteId) {
        try {
            AssetJsonLoader loader = new AssetJsonLoader(this);
            List<AssetModels.Instruction> instructions = loader.readList(AssetFiles.INSTRUCTIONS, AssetModels.Instruction.class);
            List<AssetModels.Dish> dishes = loader.readList(AssetFiles.DISHES, AssetModels.Dish.class);

            AssetModels.Instruction matchedInstruction = null;
            for (AssetModels.Instruction instruction : instructions) {
                String objectId = instruction.objectId == null ? "" : instruction.objectId.oid;
                if (favoriteId.equals(instruction.id) || favoriteId.equals(objectId)) {
                    matchedInstruction = instruction;
                    break;
                }
            }
            String dishId = matchedInstruction == null ? favoriteId : matchedInstruction.id;
            AssetModels.Dish matchedDish = null;
            for (AssetModels.Dish dish : dishes) {
                if (dishId.equals(dish.id)) {
                    matchedDish = dish;
                    break;
                }
            }
            if (matchedInstruction == null && matchedDish == null) {
                return null;
            }
            RecipeAssetInfo info = new RecipeAssetInfo();
            info.title = matchedInstruction == null ? "" : matchedInstruction.dishName;
            info.cookingTime = matchedInstruction == null ? "" : matchedInstruction.cookingTime;
            info.imageUrl = matchedInstruction == null ? "" : matchedInstruction.image;
            info.ingredientCount = matchedDish == null ? 0 : countIngredients(matchedDish.ingredients);
            return info;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String productSubtitle(long price, double carbonPoint, String fallback) {
        String subtitle = price > 0 ? CurrencyFormatter.formatVnd(price) : firstNonBlank(fallback, "");
        if (carbonPoint > 0) {
            subtitle += " • " + formatNumber(carbonPoint) + " điểm carbon";
        }
        return subtitle;
    }

    private String recipeSubtitle(String cookingTime, int ingredientCount, String fallback) {
        StringBuilder subtitle = new StringBuilder();
        if (AssetScreenData.hasText(cookingTime)) {
            subtitle.append(cookingTime.trim());
        } else if (AssetScreenData.hasText(fallback)) {
            String firstPart = fallback.split("•")[0].trim();
            if (!firstPart.isEmpty()) subtitle.append(firstPart);
        }
        if (ingredientCount > 0) {
            if (subtitle.length() > 0) subtitle.append(" • ");
            subtitle.append(ingredientCount).append(" nguyên liệu");
        }
        return subtitle.length() == 0 ? fallback : subtitle.toString();
    }

    private int countIngredients(String ingredients) {
        if (!AssetScreenData.hasText(ingredients)) {
            return 0;
        }
        String normalized = ingredients
                .replace("\r", "\n")
                .replace(";", ",")
                .replace("•", ",");
        String[] parts = normalized.split(",|\\n");
        int count = 0;
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) count++;
        }
        return count;
    }

    @Nullable
    private String firstImage(@Nullable List<String> images) {
        return images == null || images.isEmpty() ? null : images.get(0);
    }

    private String assetBlogId(AssetModels.Blog blog) {
        JsonElement objectId = blog.objectId;
        if (objectId != null && objectId.isJsonPrimitive()) {
            return objectId.getAsString();
        }
        return "";
    }

    private String firstNonBlank(String first, String second) {
        return AssetScreenData.hasText(first) ? first : (second == null ? "" : second);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class RecipeAssetInfo {
        String title;
        String cookingTime;
        String imageUrl;
        int ingredientCount;
    }
}
