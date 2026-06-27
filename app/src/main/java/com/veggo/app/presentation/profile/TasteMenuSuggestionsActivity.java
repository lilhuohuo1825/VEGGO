package com.veggo.app.presentation.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.database.AssetRepository;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

import java.util.List;

public class TasteMenuSuggestionsActivity extends BaseActivity {
    private TastePreferenceStore tasteStore;
    private List<AssetModels.Instruction> lastInstructions;
    private List<AssetModels.Product> lastProducts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "khẩu vị của tôi")) {
            return;
        }
        setContentView(R.layout.activity_taste_menu_suggestions);
        tasteStore = new TastePreferenceStore(this);
        findViewById(R.id.tasteMenuBackButton).setOnClickListener(v -> finish());
        loadSuggestions();
        tasteStore.syncFromMongo(() -> {
            if (lastInstructions != null || lastProducts != null) {
                bindSuggestions(lastInstructions, lastProducts);
            }
        });
    }

    private void loadSuggestions() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            AssetRepository repository = new AssetRepository(this);
            List<AssetModels.Product> products = repository.getProducts();
            runOnUiThread(() -> {
                lastInstructions = snapshot.instructions;
                lastProducts = products;
                bindSuggestions(lastInstructions, lastProducts);
            });
        }).start();
    }

    private void bindSuggestions(List<AssetModels.Instruction> instructions, List<AssetModels.Product> products) {
        bindAlternatives(products);
        bindRecipes(instructions);
    }

    private void bindRecipes(List<AssetModels.Instruction> instructions) {
        LinearLayout list = findViewById(R.id.tasteRecipeList);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        List<AssetModels.Instruction> safeRecipes = tasteStore.safeInstructions(instructions, 8);
        for (AssetModels.Instruction instruction : safeRecipes) {
            View item = inflater.inflate(R.layout.item_taste_menu_card, list, false);
            AssetScreenData.setText(item, R.id.tasteMenuCardTitle, instruction.dishName);
            AssetScreenData.setText(item, R.id.tasteMenuCardBody, "Nguyên liệu phù hợp: " + AssetScreenData.safe(instruction.ingredient));
            AssetScreenData.setText(item, R.id.tasteMenuCardAction, "Xem công thức");
            AssetScreenData.setText(item, R.id.tasteMenuCardChip, AssetScreenData.safe(instruction.cookingTime));
            ImageView image = item.findViewById(R.id.tasteMenuCardImage);
            if (image != null && AssetScreenData.hasText(instruction.image)) {
                image.setPadding(0, 0, 0, 0);
                Glide.with(this).load(instruction.image).placeholder(R.drawable.ic_fork_knife).into(image);
            }
            list.addView(item);
        }
    }

    private void bindAlternatives(List<AssetModels.Product> products) {
        LinearLayout container = findViewById(R.id.tasteAlternativeContainer);
        container.setOrientation(LinearLayout.VERTICAL);
        container.removeAllViews();
        List<AssetModels.Product> alternatives = tasteStore.alternativesForUserTags(products, 5);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (AssetModels.Product product : alternatives) {
            View card = inflater.inflate(R.layout.item_taste_menu_card, container, false);
            AssetScreenData.setText(card, R.id.tasteMenuCardTitle, product.productName);
            AssetScreenData.setText(card, R.id.tasteMenuCardBody, "Sản phẩm thay thế cho nguyên liệu cần tránh");
            AssetScreenData.setText(card, R.id.tasteMenuCardAction, "Xem sản phẩm");
            AssetScreenData.setText(card, R.id.tasteMenuCardChip, "Thay thế");
            ImageView image = card.findViewById(R.id.tasteMenuCardImage);
            if (image != null && product.image != null && !product.image.isEmpty()) {
                image.setPadding(0, 0, 0, 0);
                Glide.with(this).load(product.image.get(0)).placeholder(R.drawable.ic_vegetable).circleCrop().into(image);
            }
            container.addView(card);
        }
    }
}
