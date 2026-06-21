package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.database.AssetRepository;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

import java.util.List;

public class TasteAlertActivity extends BaseActivity {
    private TastePreferenceStore tasteStore;
    private List<AssetModels.Product> lastProducts;
    private List<AssetModels.Instruction> lastInstructions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taste_alert);
        tasteStore = new TastePreferenceStore(this);
        findViewById(R.id.tasteAlertBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.tasteViewMenuButton).setOnClickListener(v ->
                startActivity(new Intent(this, TasteMenuSuggestionsActivity.class)));
        loadData();
        tasteStore.syncFromMongo(this::renderCurrentState);
    }

    private void loadData() {
        new Thread(() -> {
            AssetRepository repository = new AssetRepository(this);
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            lastProducts = repository.getProducts();
            lastInstructions = snapshot.instructions;
            runOnUiThread(this::renderCurrentState);
        }).start();
    }

    private void renderCurrentState() {
        List<TastePreferenceStore.TasteTag> alertTags = tasteStore.enabledTags();
        List<String> notes = tasteStore.replacementNotes(lastProducts, lastInstructions, 5);
        bindIngredients(alertTags);
        bindReplacements(notes);
        tasteStore.saveGeneratedTasteData(alertTags, notes);
    }

    private void bindIngredients(List<TastePreferenceStore.TasteTag> tags) {
        LinearLayout list = findViewById(R.id.tasteAlertIngredientList);
        list.removeAllViews();
        if (tags == null || tags.isEmpty()) {
            list.addView(emptyText("Chưa có nguyên liệu cần lưu ý."));
            return;
        }
        for (TastePreferenceStore.TasteTag tag : tags) {
            list.addView(ingredientCard(tag));
        }
    }

    private void bindReplacements(List<String> notes) {
        LinearLayout list = findViewById(R.id.tasteAlertReplacementList);
        list.removeAllViews();
        if (notes == null || notes.isEmpty()) {
            list.addView(emptyText("Veggo chưa tìm được gợi ý phù hợp từ dữ liệu hiện có."));
            return;
        }
        for (String note : notes) {
            TextView item = new TextView(this);
            item.setText("• " + note);
            item.setTextColor(getColor(R.color.neutral_100));
            item.setTextSize(14);
            item.setLineSpacing(dp(2), 1f);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, dp(6), 0, 0);
            item.setLayoutParams(params);
            list.addView(item);
        }
    }

    private LinearLayout ingredientCard(TastePreferenceStore.TasteTag tag) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_taste_card);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, dp(10), 0, 0);
        card.setLayoutParams(cardParams);

        ImageView icon = new ImageView(this);
        icon.setBackgroundResource(R.drawable.bg_taste_thumb);
        icon.setImageResource(TastePreferenceStore.ACTION_WARN.equals(tag.action) ? R.drawable.ic_fruit : R.drawable.ic_grain_dark);
        icon.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.addView(icon, new LinearLayout.LayoutParams(dp(54), dp(54)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bodyParams.setMarginStart(dp(14));
        card.addView(body, bodyParams);

        TextView title = new TextView(this);
        title.setText(capitalize(tag.label));
        title.setTextColor(getColor(R.color.neutral_100));
        title.setTextSize(15);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        body.addView(title);

        TextView desc = new TextView(this);
        desc.setText(descriptionFor(tag));
        desc.setTextColor(getColor(R.color.neutral_80));
        desc.setTextSize(13);
        desc.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, dp(4), 0, 0);
        body.addView(desc, descParams);

        TextView chip = new TextView(this);
        chip.setText(statusText(tag));
        chip.setGravity(Gravity.CENTER);
        chip.setTextSize(13);
        chip.setTypeface(chip.getTypeface(), Typeface.BOLD);
        chip.setBackgroundResource(TastePreferenceStore.ACTION_WARN.equals(tag.action) ? R.drawable.bg_taste_chip_yellow : R.drawable.bg_taste_chip_red);
        chip.setTextColor(getColor(TastePreferenceStore.ACTION_WARN.equals(tag.action) ? R.color.secondary_hover : R.color.danger_main));
        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(dp(124), dp(34));
        chipParams.setMargins(0, dp(8), 0, 0);
        body.addView(chip, chipParams);
        return card;
    }

    private String descriptionFor(TastePreferenceStore.TasteTag tag) {
        if (TastePreferenceStore.ACTION_WARN.equals(tag.action)) {
            return "Sản phẩm liên quan sẽ được gắn cảnh báo trước khi đặt món.";
        }
        return "Sản phẩm liên quan sẽ được chặn khỏi catalog hoặc đánh dấu theo lựa chọn của bạn.";
    }

    private String statusText(TastePreferenceStore.TasteTag tag) {
        if (TastePreferenceStore.ACTION_WARN.equals(tag.action)) return "Cần kiểm tra";
        return "Không phù hợp";
    }

    private TextView emptyText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(getColor(R.color.neutral_70));
        view.setTextSize(14);
        view.setPadding(0, dp(12), 0, 0);
        return view;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
