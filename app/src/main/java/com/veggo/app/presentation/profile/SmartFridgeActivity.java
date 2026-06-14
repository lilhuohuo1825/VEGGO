package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
            runOnUiThread(() -> bindInventory(snapshot, inventories));
        }).start();
    }

    private void bindInventory(AssetScreenData.Snapshot snapshot, List<AssetModels.Inventory> inventories) {
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.fridgeIngredientsCount, inventories.size() + " nguyên liệu");
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.fridgeExpiringCount, Math.min(3, inventories.size()) + " sắp hết hạn");

        LinearLayout list = findViewById(R.id.fridgeInventoryList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int count = Math.min(5, inventories.size());
        for (int i = 0; i < count; i++) {
            AssetModels.Inventory inventory = inventories.get(i);
            View item = inflater.inflate(R.layout.item_fridge_ingredient_urgent, list, false);
            AssetScreenData.setText(item, R.id.fridgeIngredientName, "SKU " + inventory.sku);
            AssetScreenData.setText(item, R.id.fridgeIngredientMeta, "Tồn kho " + inventory.totalStock + " · Nhiều kho");
            AssetScreenData.setText(item, R.id.fridgeIngredientExpiry, "Còn " + (i + 1) + " ngày");
            item.setOnClickListener(v -> startActivity(new Intent(this, FridgeSuggestionsActivity.class)));
            list.addView(item);
        }
    }

}
