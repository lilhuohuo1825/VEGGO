package com.veggo.app.presentation.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

import java.util.List;

public class FridgeSuggestionsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fridge_suggestions);
        findViewById(R.id.fridgeSuggestionsBackButton).setOnClickListener(v -> finish());
        loadSuggestions();
    }

    private void loadSuggestions() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            List<AssetModels.Inventory> inventories = AssetScreenData.availableInventories(snapshot);
            List<AssetModels.Instruction> instructions = AssetScreenData.recipeSuggestions(snapshot);
            runOnUiThread(() -> bindSuggestions(snapshot, inventories, instructions));
        }).start();
    }

    private void bindSuggestions(
            AssetScreenData.Snapshot snapshot,
            List<AssetModels.Inventory> inventories,
            List<AssetModels.Instruction> instructions
    ) {
        bindUseFirst(snapshot, inventories);
        LinearLayout list = findViewById(R.id.fridgeRecipeList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int count = Math.min(5, instructions.size());
        for (int i = 0; i < count; i++) {
            AssetModels.Instruction instruction = instructions.get(i);
            View item = inflater.inflate(R.layout.item_fridge_recipe, list, false);
            AssetScreenData.setText(item, R.id.fridgeRecipeTitle, instruction.dishName);
            AssetScreenData.setText(item, R.id.fridgeRecipeBody, "Nguyên liệu chính: " + AssetScreenData.safe(instruction.ingredient));
            AssetScreenData.setText(item, R.id.fridgeRecipeTime, AssetScreenData.safe(instruction.cookingTime));
            list.addView(item);
        }
    }

    private void bindUseFirst(AssetScreenData.Snapshot snapshot, List<AssetModels.Inventory> inventories) {
        if (inventories.size() > 0) {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.fridgeUseFirstOne, "SKU " + inventories.get(0).sku + " · 1 ngày");
        }
        if (inventories.size() > 1) {
            AssetScreenData.setText(findViewById(android.R.id.content), R.id.fridgeUseFirstTwo, "SKU " + inventories.get(1).sku + " · 2 ngày");
        }
    }
}
