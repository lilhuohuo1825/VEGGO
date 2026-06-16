package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

import java.util.List;

public class SmartFridgeActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_fridge);
        findViewById(R.id.smartFridgeBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.smartFridgeAddButton).setOnClickListener(v -> openAddIngredient());
        findViewById(R.id.fridgeScanButton).setOnClickListener(v -> openAddIngredient());
        findViewById(R.id.fridgeManualButton).setOnClickListener(v -> openAddIngredient());
        findViewById(R.id.fridgeSuggestionButton).setOnClickListener(v ->
                startActivity(new Intent(this, FridgeSuggestionsActivity.class))
        );
        loadInventory();
    }

    private void openAddIngredient() {
        startActivity(new Intent(this, AddFridgeIngredientActivity.class));
    }

    private void loadInventory() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            List<AssetModels.Inventory> inventories = AssetScreenData.availableInventories(snapshot);
            com.veggo.app.core.database.AssetRepository repository = new com.veggo.app.core.database.AssetRepository(this);
            List<AssetModels.Product> products = repository.getProducts();
            runOnUiThread(() -> bindInventory(snapshot, inventories, products));
        }).start();
    }

    private void bindInventory(AssetScreenData.Snapshot snapshot, List<AssetModels.Inventory> inventories, List<AssetModels.Product> products) {
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.fridgeIngredientsCount, inventories.size() + " nguyên liệu");
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.fridgeExpiringCount, Math.min(3, inventories.size()) + " sắp hết hạn");

        LinearLayout list = findViewById(R.id.fridgeInventoryList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int count = inventories.size();
        for (int i = 0; i < count; i++) {
            AssetModels.Inventory inventory = inventories.get(i);
            View item = inflater.inflate(R.layout.item_fridge_ingredient_urgent, list, false);

            AssetModels.Product matchedProduct = null;
            if (products != null) {
                for (AssetModels.Product p : products) {
                    if (p.sku != null && p.sku.equals(inventory.sku)) {
                        matchedProduct = p;
                        break;
                    }
                }
            }

            String name = (matchedProduct != null && matchedProduct.productName != null) ? matchedProduct.productName : "SKU " + inventory.sku;
            String imageUrl = null;
            if (matchedProduct != null && matchedProduct.image != null && !matchedProduct.image.isEmpty()) {
                imageUrl = matchedProduct.image.get(0);
            }

            AssetScreenData.setText(item, R.id.fridgeIngredientName, name);
            AssetScreenData.setText(item, R.id.fridgeIngredientMeta, "Tồn kho " + inventory.totalStock + " · Nhiều kho");
            AssetScreenData.setText(item, R.id.fridgeIngredientExpiry, "Còn " + (i + 1) + " ngày");

            ImageView image = item.findViewById(R.id.fridgeIngredientImage);
            if (image != null) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    image.setPadding(0, 0, 0, 0);
                    com.bumptech.glide.Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_vegetable)
                            .circleCrop()
                            .into(image);
                } else {
                    int padding = (int) (10 * getResources().getDisplayMetrics().density);
                    image.setPadding(padding, padding, padding, padding);
                    image.setImageResource(R.drawable.ic_vegetable);
                }
            }

            item.setOnClickListener(v -> startActivity(new Intent(this, FridgeSuggestionsActivity.class)));
            list.addView(item);
        }
    }

}
