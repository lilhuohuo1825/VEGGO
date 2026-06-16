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
            com.veggo.app.core.database.AssetRepository repository = new com.veggo.app.core.database.AssetRepository(this);
            List<AssetModels.Product> products = repository.getProducts();
            runOnUiThread(() -> bindSuggestions(snapshot, inventories, instructions, products));
        }).start();
    }

    private void bindSuggestions(
            AssetScreenData.Snapshot snapshot,
            List<AssetModels.Inventory> inventories,
            List<AssetModels.Instruction> instructions,
            List<AssetModels.Product> products
    ) {
        bindUseFirst(snapshot, inventories, products);
        LinearLayout list = findViewById(R.id.fridgeRecipeList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        
        java.util.List<AssetModels.Instruction> filteredInstructions = new java.util.ArrayList<>();
        if (instructions != null) {
            for (AssetModels.Instruction instruction : instructions) {
                if (instruction.image != null && !instruction.image.isEmpty()) {
                    filteredInstructions.add(instruction);
                }
            }
        }

        int count = Math.min(5, filteredInstructions.size());
        for (int i = 0; i < count; i++) {
            AssetModels.Instruction instruction = filteredInstructions.get(i);
            View item = inflater.inflate(R.layout.item_fridge_recipe, list, false);
            AssetScreenData.setText(item, R.id.fridgeRecipeTitle, instruction.dishName);
            AssetScreenData.setText(item, R.id.fridgeRecipeBody, "Nguyên liệu chính: " + AssetScreenData.safe(instruction.ingredient));
            AssetScreenData.setText(item, R.id.fridgeRecipeTime, AssetScreenData.safe(instruction.cookingTime));

            android.widget.ImageView image = item.findViewById(R.id.fridgeRecipeImage);
            if (image != null) {
                if (instruction.image != null && !instruction.image.isEmpty()) {
                    image.setPadding(0, 0, 0, 0);
                    com.bumptech.glide.Glide.with(this)
                            .load(instruction.image)
                            .placeholder(R.drawable.ic_fork_knife)
                            .into(image);
                } else {
                    int padding = (int) (18 * getResources().getDisplayMetrics().density);
                    image.setPadding(padding, padding, padding, padding);
                    image.setImageResource(R.drawable.ic_fork_knife);
                }
            }

            list.addView(item);
        }
    }

    private void bindUseFirst(AssetScreenData.Snapshot snapshot, List<AssetModels.Inventory> inventories, List<AssetModels.Product> products) {
        LinearLayout container = findViewById(R.id.fridgeUseFirstContainer);
        if (container == null) return;
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int count = Math.min(2, inventories.size());
        for (int i = 0; i < count; i++) {
            AssetModels.Inventory inventory = inventories.get(i);
            View card = inflater.inflate(R.layout.item_fridge_use_first, container, false);

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

            AssetScreenData.setText(card, R.id.useFirstTitle, name);
            AssetScreenData.setText(card, R.id.useFirstDays, "Còn " + (i + 1) + " ngày");

            android.widget.TextView daysTv = card.findViewById(R.id.useFirstDays);
            if (daysTv != null) {
                if (i == 0) {
                    daysTv.setTextColor(getResources().getColor(R.color.danger_main));
                } else {
                    daysTv.setTextColor(getResources().getColor(R.color.secondary_hover));
                }
            }

            android.widget.ImageView image = card.findViewById(R.id.useFirstImage);
            if (image != null) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    image.setPadding(0, 0, 0, 0);
                    com.bumptech.glide.Glide.with(this)
                            .load(imageUrl)
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
}
