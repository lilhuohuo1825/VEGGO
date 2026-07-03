package com.veggo.app.presentation.community;

import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.PreferencesManager;
import com.veggo.app.data.local.entity.CommunityRecipeIngredientEntity;
import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.CartItemRequestDto;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.repository.CartRepository;
import com.veggo.app.presentation.checkout.PendingCheckoutStore;
import com.veggo.app.presentation.product.ProductDetailActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityIngredientsActivity extends AppCompatActivity {
    public static final String EXTRA_RECIPE_ID = "community_ingredients_recipe_id";

    private CommunityRepository repository;
    private CartRepository cartRepository;
    private LinearLayout list;
    private View bookmarkButton;
    private View addCartButton;
    private String recipeId;
    private SwipeRefreshLayout refreshLayout;
    private final List<IngredientCartItem> ingredientCartItems = new ArrayList<>();
    private boolean isAddingToCart = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_ingredients);

        repository = new CommunityRepository(this);
        cartRepository = AppModule.provideCartRepository(this);
        list = findViewById(R.id.ingredientsList);
        findViewById(R.id.ingredientsBackButton).setOnClickListener(v -> finish());
        bookmarkButton = findViewById(R.id.ingredientsBookmarkButton);
        bookmarkButton.setOnClickListener(v -> toggleBookmark());
        addCartButton = findViewById(R.id.ingredientsAddCartButton);
        addCartButton.setOnClickListener(v -> addSelectedIngredientsToCart());

        recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        refreshLayout = CommunityUi.setupPullToRefresh(this, R.id.ingredientsScroll, this::loadData);
        loadData();
        loadBookmarkState();
    }

    private void loadData() {
        repository.loadRecipeDetail(recipeId, data -> runOnUiThread(() -> {
            bindData(data);
            CommunityUi.finishRefresh(refreshLayout);
        }));
    }

    private void loadBookmarkState() {
        repository.isRecipeSaved(recipeId, saved -> runOnUiThread(() ->
                ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_bookmark_green, saved)));
    }

    private void toggleBookmark() {
        Object tag = bookmarkButton.getTag();
        boolean saved = tag instanceof Boolean && (Boolean) tag;
        if (saved) {
            repository.removeRecipeFromCookbooks(recipeId, done -> runOnUiThread(() -> {
                if (done) {
                    ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_bookmark_green, false);
                }
            }));
            return;
        }
        CommunityUi.showAddToCookbook(this, recipeId, () ->
                ToggleUi.renderSelected(bookmarkButton, R.drawable.ic_bookmark_green, true));
    }

    private void bindData(CommunityRepository.RecipeDetailData data) {
        if (data.recipe == null) {
            finish();
            return;
        }
        ((TextView) findViewById(R.id.ingredientsHeroTitle)).setText(data.recipe.getTitle());
        ((TextView) findViewById(R.id.ingredientsHeroMeta)).setText(
                "\u25CB  " + data.recipe.getTimeMinutes() + " min   \u25CB  "
                        + data.recipe.getIngredientCount() + " nguyên liệu"
        );
        Glide.with(this)
                .load(data.recipe.getImageUrl())
                .transform(new CenterCrop(), new RoundedCorners(dp(8)))
                .into((ImageView) findViewById(R.id.ingredientsHeroImage));

        list.removeAllViews();
        ingredientCartItems.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int index = 0; index < data.ingredients.size(); index++) {
            CommunityRecipeIngredientEntity ingredient = data.ingredients.get(index);
            ProductEntity product = data.productsById.get(ingredient.getProductId());
            IngredientCartItem cartItem = new IngredientCartItem(ingredient, product);
            ingredientCartItems.add(cartItem);

            View row = inflater.inflate(R.layout.item_community_ingredient_check, list, false);
            bindIngredient(row, cartItem);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(54)
            );
            if (index > 0) {
                params.topMargin = dp(8);
            }
            list.addView(row, params);
        }
    }

    private void bindIngredient(View row, IngredientCartItem cartItem) {
        CommunityRecipeIngredientEntity ingredient = cartItem.ingredient;
        ProductEntity product = cartItem.product;
        String name = isBlank(ingredient.getDisplayName())
                ? (product == null ? "Sản phẩm" : product.getName())
                : ingredient.getDisplayName();
        ((TextView) row.findViewById(R.id.ingredientCheckName)).setText(name);
        ((TextView) row.findViewById(R.id.ingredientCheckQuantity)).setText(ingredient.getQuantity());

        ImageView image = row.findViewById(R.id.ingredientCheckImage);
        TextView emoji = row.findViewById(R.id.ingredientCheckEmoji);
        if (!isBlank(ingredient.getIconUrl())) {
            image.setVisibility(View.VISIBLE);
            emoji.setVisibility(View.GONE);
            Glide.with(this).load(ingredient.getIconUrl()).transform(new CenterCrop(), new RoundedCorners(dp(8))).into(image);
        } else if (!isBlank(ingredient.getIconEmoji())) {
            image.setVisibility(View.GONE);
            emoji.setVisibility(View.VISIBLE);
            emoji.setText(ingredient.getIconEmoji());
        } else if (product != null) {
            image.setVisibility(View.VISIBLE);
            emoji.setVisibility(View.GONE);
            Glide.with(this).load(product.getImageUrl()).transform(new CenterCrop(), new RoundedCorners(dp(8))).into(image);
        }

        ImageButton checkbox = row.findViewById(R.id.ingredientCheckButton);
        bindCheckbox(checkbox, cartItem);
        View card = row.findViewById(R.id.ingredientCheckCard);
        card.setOnClickListener(v -> openProductDetail(cartItem));
    }

    private void bindCheckbox(ImageButton checkbox, IngredientCartItem cartItem) {
        checkbox.setTag(cartItem);
        renderCheckbox(checkbox, cartItem.selected);
        checkbox.setOnClickListener(v -> {
            IngredientCartItem item = (IngredientCartItem) v.getTag();
            item.selected = !item.selected;
            renderCheckbox((ImageButton) v, item.selected);
        });
    }

    private void renderCheckbox(ImageButton checkbox, boolean checked) {
        checkbox.setBackgroundResource(checked ? R.drawable.bg_checkbox_checked : R.drawable.bg_checkbox_unchecked);
        checkbox.setImageResource(checked ? R.drawable.ic_check : 0);
        checkbox.setColorFilter(checked ? Color.WHITE : Color.TRANSPARENT);
    }

    private void addSelectedIngredientsToCart() {
        if (isAddingToCart) {
            return;
        }

        List<IngredientCartItem> selectedItems = selectedCartItems();
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn nguyên liệu cần thêm", Toast.LENGTH_SHORT).show();
            return;
        }

        List<IngredientCartItem> purchasableItems = new ArrayList<>();
        for (IngredientCartItem item : selectedItems) {
            if (!isBlank(item.sku()) || !isBlank(item.productId())) {
                purchasableItems.add(item);
            }
        }

        if (purchasableItems.isEmpty()) {
            Toast.makeText(this, "Chưa tìm thấy sản phẩm để thêm vào giỏ", Toast.LENGTH_SHORT).show();
            return;
        }

        String customerId = resolveCustomerId();
        if (isBlank(customerId)) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        isAddingToCart = true;
        addCartButton.setEnabled(false);

        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        int total = purchasableItems.size();
        for (IngredientCartItem item : purchasableItems) {
            CartItemRequestDto request = new CartItemRequestDto(item.sku(), 1, 1.0);
            request.setProductId(item.productId());
            cartRepository.addItem(customerId, request)
                    .enqueue(new Callback<CartDto>() {
                        @Override
                        public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                            if (response.isSuccessful()) {
                                success.incrementAndGet();
                            } else {
                                failed.incrementAndGet();
                            }
                            finishAddToCartIfDone(completed.incrementAndGet(), total, success.get(), failed.get());
                        }

                        @Override
                        public void onFailure(Call<CartDto> call, Throwable t) {
                            failed.incrementAndGet();
                            finishAddToCartIfDone(completed.incrementAndGet(), total, success.get(), failed.get());
                        }
                    });
        }
    }

    private List<IngredientCartItem> selectedCartItems() {
        List<IngredientCartItem> selectedItems = new ArrayList<>();
        for (IngredientCartItem item : ingredientCartItems) {
            if (item.selected) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    private void finishAddToCartIfDone(int completed, int total, int success, int failed) {
        if (completed < total) {
            return;
        }

        isAddingToCart = false;
        addCartButton.setEnabled(true);

        if (success > 0 && failed == 0) {
            Toast.makeText(this, "Đã thêm " + success + " sản phẩm vào giỏ hàng", Toast.LENGTH_SHORT).show();
        } else if (success > 0) {
            Toast.makeText(this, "Đã thêm " + success + " sản phẩm, " + failed + " sản phẩm lỗi", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Không thể thêm sản phẩm vào giỏ hàng", Toast.LENGTH_SHORT).show();
        }
    }

    private String resolveCustomerId() {
        AppPreferences appPreferences = new AppPreferences(this);
        String customerId = appPreferences.getCustomerId();
        if (isBlank(customerId)) {
            customerId = new PreferencesManager(this).getUserId();
        }
        if (isBlank(customerId)) {
            customerId = new PendingCheckoutStore(this).guestId();
        }
        return customerId;
    }

    private void openProductDetail(IngredientCartItem item) {
        String productId = item.product != null ? item.product.getId() : null;
        if (isBlank(productId) && item.ingredient != null) {
            productId = item.ingredient.getProductId();
        }
        if (isBlank(productId)) {
            Toast.makeText(this, "Chưa tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId);
        startActivity(intent);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class IngredientCartItem {
        final CommunityRecipeIngredientEntity ingredient;
        final ProductEntity product;
        boolean selected;

        IngredientCartItem(CommunityRecipeIngredientEntity ingredient, ProductEntity product) {
            this.ingredient = ingredient;
            this.product = product;
        }

        String sku() {
            if (ingredient != null && ingredient.getProductSku() != null && !ingredient.getProductSku().trim().isEmpty()) {
                return ingredient.getProductSku();
            }
            return product == null ? null : product.getSku();
        }

        String productId() {
            if (product != null && product.getId() != null && !product.getId().trim().isEmpty()) {
                return product.getId();
            }
            return ingredient == null ? null : ingredient.getProductId();
        }
    }
}
