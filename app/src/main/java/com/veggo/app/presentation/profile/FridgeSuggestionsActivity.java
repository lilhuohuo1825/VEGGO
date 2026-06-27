package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.database.AssetRepository;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.data.remote.api.FridgeApi;
import com.veggo.app.data.remote.api.RecipeApi;
import com.veggo.app.data.remote.dto.FridgeItemDto;
import com.veggo.app.data.remote.dto.RelatedRecipeDto;
import com.veggo.app.presentation.community.InstructionRecipeDetailActivity;
import com.veggo.app.presentation.common.AssetScreenData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class FridgeSuggestionsActivity extends BaseActivity {
    private static final int USE_FIRST_LIMIT = 2;
    private static final int RECIPE_LIMIT = 5;
    private static final long UNKNOWN_EXPIRY = Long.MAX_VALUE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "tủ lạnh thông minh")) {
            return;
        }
        setContentView(R.layout.activity_fridge_suggestions);
        findViewById(R.id.fridgeSuggestionsBackButton).setOnClickListener(v -> finish());
        loadSuggestions();
    }

    private void loadSuggestions() {
        new Thread(() -> {
            AssetRepository repository = new AssetRepository(this);
            List<AssetModels.Product> products = repository.getProducts();
            List<FridgeItemDto> fridgeItems = loadFridgeItems();
            List<ExpiringIngredient> expiringIngredients = buildExpiringIngredients(fridgeItems, products);
            List<RecipeSuggestion> recipeSuggestions = loadRelatedRecipeSuggestions(expiringIngredients);
            runOnUiThread(() -> bindSuggestions(expiringIngredients, recipeSuggestions));
        }).start();
    }

    private List<FridgeItemDto> loadFridgeItems() {
        try {
            String customerId = new AppPreferences(this).getCustomerId();
            if (customerId == null || customerId.trim().isEmpty()) {
                return new ArrayList<>();
            }
            FridgeApi api = ApiClient.createService(FridgeApi.class);
            retrofit2.Response<List<FridgeItemDto>> response = api.getFridgeItems(customerId).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    private List<ExpiringIngredient> buildExpiringIngredients(List<FridgeItemDto> fridgeItems, List<AssetModels.Product> products) {
        Map<String, AssetModels.Product> productBySku = new HashMap<>();
        if (products != null) {
            for (AssetModels.Product product : products) {
                if (hasText(product.sku)) {
                    productBySku.put(product.sku, product);
                }
            }
        }

        List<ExpiringIngredient> ingredients = new ArrayList<>();
        if (fridgeItems != null) {
            for (FridgeItemDto item : fridgeItems) {
                AssetModels.Product product = item.getSku() == null ? null : productBySku.get(item.getSku());
                String name = firstText(item.getName(), product == null ? null : product.productName, item.getSku());
                if (!hasText(name)) {
                    continue;
                }
                String imageUrl = firstText(item.getImage(), firstImage(item.getImages()), product == null ? null : firstImage(product.image));
                long expiryMillis = parseExpiryMillis(item.getExpiryDate());
                ingredients.add(new ExpiringIngredient(name, item.getSku(), imageUrl, expiryMillis));
            }
        }

        ingredients.sort(Comparator
                .comparingLong((ExpiringIngredient ingredient) -> ingredient.expiryMillis)
                .thenComparing(ingredient -> ingredient.name));
        return ingredients;
    }

    private List<RecipeSuggestion> loadRelatedRecipeSuggestions(List<ExpiringIngredient> expiringIngredients) {
        List<RecipeSuggestion> suggestions = new ArrayList<>();
        Set<String> usedInstructionIds = new HashSet<>();
        if (expiringIngredients == null || expiringIngredients.isEmpty()) {
            return suggestions;
        }

        RecipeApi recipeApi = ApiClient.createService(RecipeApi.class);
        for (ExpiringIngredient ingredient : expiringIngredients) {
            if (!hasText(ingredient.name)) {
                continue;
            }
            try {
                retrofit2.Response<List<RelatedRecipeDto>> response = recipeApi.getRelatedRecipes(ingredient.name).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    continue;
                }
                for (RelatedRecipeDto recipe : response.body()) {
                    if (!hasText(recipe.getInstructionId()) || !hasText(recipe.getTitle()) || usedInstructionIds.contains(recipe.getInstructionId())) {
                        continue;
                    }
                    suggestions.add(new RecipeSuggestion(
                            recipe.getInstructionId(),
                            recipe.getTitle(),
                            recipe.getImage(),
                            recipe.getCookingTime(),
                            ingredient.name
                    ));
                    usedInstructionIds.add(recipe.getInstructionId());
                    if (suggestions.size() >= RECIPE_LIMIT) {
                        return suggestions;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return suggestions;
    }

    private void bindSuggestions(List<ExpiringIngredient> expiringIngredients, List<RecipeSuggestion> suggestions) {
        bindUseFirst(expiringIngredients);
        LinearLayout list = findViewById(R.id.fridgeRecipeList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        int count = Math.min(RECIPE_LIMIT, suggestions.size());
        if (count == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Chưa có món phù hợp với sản phẩm sắp hết hạn.");
            emptyText.setTextColor(getResources().getColor(R.color.neutral_60));
            emptyText.setTextSize(14);
            emptyText.setPadding(0, dp(12), 0, dp(12));
            list.addView(emptyText);
            return;
        }
        for (int i = 0; i < count; i++) {
            RecipeSuggestion suggestion = suggestions.get(i);
            View item = inflater.inflate(R.layout.item_fridge_recipe, list, false);
            AssetScreenData.setText(item, R.id.fridgeRecipeTitle, suggestion.title);
            AssetScreenData.setText(item, R.id.fridgeRecipeBody, "Nguyên liệu chính: " + AssetScreenData.safe(suggestion.matchedIngredient));
            AssetScreenData.setText(item, R.id.fridgeRecipeTime, AssetScreenData.safe(suggestion.cookingTime));

            android.widget.ImageView image = item.findViewById(R.id.fridgeRecipeImage);
            if (image != null) {
                if (hasText(suggestion.imageUrl)) {
                    image.setPadding(0, 0, 0, 0);
                    com.bumptech.glide.Glide.with(this)
                            .load(suggestion.imageUrl)
                            .placeholder(R.drawable.ic_fork_knife)
                            .into(image);
                } else {
                    int padding = (int) (18 * getResources().getDisplayMetrics().density);
                    image.setPadding(padding, padding, padding, padding);
                    image.setImageResource(R.drawable.ic_fork_knife);
                }
            }

            item.setOnClickListener(v -> openRecipeDetail(suggestion.instructionId));
            list.addView(item);
        }
    }

    private void bindUseFirst(List<ExpiringIngredient> expiringIngredients) {
        LinearLayout container = findViewById(R.id.fridgeUseFirstContainer);
        if (container == null) return;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int count = Math.min(USE_FIRST_LIMIT, expiringIngredients.size());
        for (int i = 0; i < count; i++) {
            ExpiringIngredient ingredient = expiringIngredients.get(i);
            View card = inflater.inflate(R.layout.item_fridge_use_first, container, false);

            AssetScreenData.setText(card, R.id.useFirstTitle, ingredient.name);
            AssetScreenData.setText(card, R.id.useFirstDays, formatDaysLeft(ingredient.expiryMillis));

            TextView daysTv = card.findViewById(R.id.useFirstDays);
            if (daysTv != null) {
                if (i == 0) {
                    daysTv.setTextColor(getResources().getColor(R.color.danger_main));
                } else {
                    daysTv.setTextColor(getResources().getColor(R.color.secondary_hover));
                }
            }

            android.widget.ImageView image = card.findViewById(R.id.useFirstImage);
            if (image != null) {
                if (hasText(ingredient.imageUrl)) {
                    image.setPadding(0, 0, 0, 0);
                    com.bumptech.glide.Glide.with(this)
                            .load(ingredient.imageUrl)
                            .placeholder(R.drawable.ic_vegetable)
                            .circleCrop()
                            .into(image);
                } else {
                    int padding = (int) (8 * getResources().getDisplayMetrics().density);
                    image.setPadding(padding, padding, padding, padding);
                    image.setImageResource(R.drawable.ic_vegetable);
                }
            }

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) card.getLayoutParams();
            if (i > 0) {
                lp.setMarginStart((int) (8 * getResources().getDisplayMetrics().density));
            }
            card.setLayoutParams(lp);

            container.addView(card);
        }
    }

    private void openRecipeDetail(String instructionId) {
        if (!hasText(instructionId)) {
            return;
        }
        Intent intent = new Intent(this, InstructionRecipeDetailActivity.class);
        intent.putExtra(InstructionRecipeDetailActivity.EXTRA_INSTRUCTION_ID, instructionId);
        startActivity(intent);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private long parseExpiryMillis(String value) {
        if (!hasText(value)) {
            return UNKNOWN_EXPIRY;
        }
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                if (pattern.contains("'Z'")) {
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                java.util.Date date = format.parse(value);
                if (date != null) {
                    return date.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return UNKNOWN_EXPIRY;
    }

    private String formatDaysLeft(long expiryMillis) {
        if (expiryMillis == UNKNOWN_EXPIRY) {
            return "Sắp hết hạn";
        }
        long diff = expiryMillis - System.currentTimeMillis();
        if (diff <= 0) {
            return "Còn hôm nay";
        }
        long days = (long) Math.ceil(diff / (1000d * 60d * 60d * 24d));
        return "Còn " + Math.max(1, days) + " ngày";
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String firstImage(List<String> images) {
        return images == null || images.isEmpty() ? null : images.get(0);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static final class ExpiringIngredient {
        final String name;
        final String sku;
        final String imageUrl;
        final long expiryMillis;

        ExpiringIngredient(String name, String sku, String imageUrl, long expiryMillis) {
            this.name = name;
            this.sku = sku;
            this.imageUrl = imageUrl;
            this.expiryMillis = expiryMillis;
        }
    }

    private static final class RecipeSuggestion {
        final String instructionId;
        final String title;
        final String imageUrl;
        final String cookingTime;
        final String matchedIngredient;

        RecipeSuggestion(String instructionId, String title, String imageUrl, String cookingTime, String matchedIngredient) {
            this.instructionId = instructionId;
            this.title = title;
            this.imageUrl = imageUrl;
            this.cookingTime = cookingTime;
            this.matchedIngredient = matchedIngredient;
        }
    }
}
