package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class TastePreferencesActivity extends BaseActivity {
    private TastePreferenceStore tasteStore;
    private LinearLayout tagRow;
    private LinearLayout ingredientList;
    private TextView activeSummary;
    private TextView blockSummary;
    private SwitchMaterial hideSwitch;
    private SwitchMaterial badgeSwitch;
    private boolean bindingSwitches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "khẩu vị của tôi")) {
            return;
        }
        setContentView(R.layout.activity_taste_preferences);
        tasteStore = new TastePreferenceStore(this);
        tagRow = findViewById(R.id.tasteTagRow);
        ingredientList = findViewById(R.id.tasteIngredientList);
        activeSummary = findViewById(R.id.tasteActiveSummary);
        blockSummary = findViewById(R.id.tasteBlockCountSummary);
        hideSwitch = findViewById(R.id.tasteHideCatalogSwitch);
        badgeSwitch = findViewById(R.id.tasteSuggestMenuSwitch);

        findViewById(R.id.tasteBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.tasteEditButton).setOnClickListener(v -> openSetup());
        findViewById(R.id.tasteAlertBehaviorCard).setOnClickListener(v -> hideSwitch.setChecked(true));
        findViewById(R.id.tasteBadgeBehaviorCard).setOnClickListener(v -> badgeSwitch.setChecked(true));
        findViewById(R.id.tasteViewMenuMainButton).setOnClickListener(v -> openMenu());

        syncSwitches();
        hideSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingSwitches) return;
            tasteStore.setHideCatalog(isChecked);
            syncSwitches();
        });
        badgeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingSwitches) return;
            tasteStore.setBadgeCatalog(isChecked);
            syncSwitches();
        });
        renderTasteState();
        tasteStore.syncFromMongo(this::renderTasteState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tasteStore != null) {
            renderTasteState();
        }
    }

    private void renderTasteState() {
        activeSummary.setText("Đang bật " + tasteStore.enabledTagCount());
        blockSummary.setText(tasteStore.enabledBlockingCount() + " tag chặn");
        syncSwitches();
        bindTagChips();
        bindIngredientCards();
    }

    private void bindTagChips() {
        tagRow.removeAllViews();
        for (TastePreferenceStore.TasteTag tag : tasteStore.tags()) {
            if (tag.enabled) {
                TextView item = chip(tag.action + ": " + tag.label, tag.action);
                item.setOnClickListener(v -> openAlert());
                tagRow.addView(item);
            }
        }
        TextView add = chip("+ Thêm tag", "add");
        add.setOnClickListener(v -> openTagDialog());
        tagRow.addView(add);
    }

    private void bindIngredientCards() {
        ingredientList.removeAllViews();
        for (TastePreferenceStore.TasteTag tag : tasteStore.enabledTags()) {
            ingredientList.addView(ingredientCard(tag));
        }
        if (ingredientList.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Chưa có nguyên liệu cần lưu ý.");
            empty.setTextColor(getColor(R.color.neutral_70));
            empty.setTextSize(14);
            empty.setPadding(0, dp(12), 0, 0);
            ingredientList.addView(empty);
        }
    }

    private LinearLayout ingredientCard(TastePreferenceStore.TasteTag tag) {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_taste_card);
        card.setOnClickListener(v -> openAlert());
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

        TextView status = new TextView(this);
        boolean warn = TastePreferenceStore.ACTION_WARN.equals(tag.action);
        status.setText(warn ? "Cần kiểm tra" : "Không phù hợp");
        status.setGravity(Gravity.CENTER);
        status.setTextSize(13);
        status.setTypeface(status.getTypeface(), Typeface.BOLD);
        status.setBackgroundResource(warn ? R.drawable.bg_taste_chip_yellow : R.drawable.bg_taste_chip_red);
        status.setTextColor(getColor(warn ? R.color.secondary_hover : R.color.danger_main));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(dp(124), dp(34));
        statusParams.setMargins(0, dp(8), 0, 0);
        body.addView(status, statusParams);
        return card;
    }

    private String descriptionFor(TastePreferenceStore.TasteTag tag) {
        if (TastePreferenceStore.ACTION_WARN.equals(tag.action)) {
            return "Hiển thị cảnh báo trước khi đặt món hoặc thêm vào giỏ.";
        }
        if (TastePreferenceStore.ACTION_ALLERGY.equals(tag.action)) {
            return "Dị ứng nặng, tự động chặn khỏi catalog và gợi ý thay thế.";
        }
        return "Không muốn ăn, tự động chặn khỏi catalog và gợi ý thay thế.";
    }

    private void syncSwitches() {
        bindingSwitches = true;
        hideSwitch.setChecked(tasteStore.hideCatalog());
        badgeSwitch.setChecked(tasteStore.badgeCatalog());
        tintSwitch(hideSwitch, tasteStore.hideCatalog());
        tintSwitch(badgeSwitch, tasteStore.badgeCatalog());
        bindingSwitches = false;
    }

    private void tintSwitch(SwitchMaterial view, boolean enabled) {
        view.setTrackTintList(ColorStateList.valueOf(getColor(enabled ? R.color.primary_bg : R.color.neutral_30)));
        view.setThumbTintList(ColorStateList.valueOf(getColor(enabled ? R.color.primary_main : R.color.neutral_60)));
    }

    private TextView chip(String text, String action) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setGravity(Gravity.CENTER);
        view.setTextSize(13);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        view.setPadding(dp(12), 0, dp(12), 0);
        if (TastePreferenceStore.ACTION_ALLERGY.equals(action) || TastePreferenceStore.ACTION_BLOCK.equals(action)) {
            view.setBackgroundResource(R.drawable.bg_taste_chip_red);
            view.setTextColor(getColor(R.color.danger_main));
        } else if (TastePreferenceStore.ACTION_WARN.equals(action)) {
            view.setBackgroundResource(R.drawable.bg_taste_chip_yellow);
            view.setTextColor(getColor(R.color.secondary_hover));
        } else {
            view.setBackgroundResource(R.drawable.bg_taste_chip_green_outline);
            view.setTextColor(getColor(R.color.primary_main));
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(36)
        );
        params.setMarginEnd(dp(8));
        view.setLayoutParams(params);
        return view;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void openSetup() {
        startActivity(new Intent(this, TasteSetupActivity.class));
    }

    private void openTagDialog() {
        TasteTagDialog.show(this, tasteStore, null, this::renderTasteState);
    }

    private void openAlert() {
        startActivity(new Intent(this, TasteAlertActivity.class));
    }

    private void openMenu() {
        startActivity(new Intent(this, TasteMenuSuggestionsActivity.class));
    }
}
