package com.veggo.app.presentation.profile;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class TasteSetupActivity extends BaseActivity {
    private TastePreferenceStore tasteStore;
    private LinearLayout dynamicTags;
    private TextView veganChip;
    private TextView pureVeganChip;
    private TextView savoryChip;
    private SwitchMaterial hideBehaviorSwitch;
    private SwitchMaterial badgeBehaviorSwitch;
    private boolean bindingBehaviorSwitches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "khẩu vị của tôi")) {
            return;
        }
        setContentView(R.layout.activity_taste_setup);
        tasteStore = new TastePreferenceStore(this);
        dynamicTags = findViewById(R.id.tasteSetupDynamicTags);
        veganChip = findViewById(R.id.tasteDietVeganChip);
        pureVeganChip = findViewById(R.id.tasteDietPureVeganChip);
        savoryChip = findViewById(R.id.tasteDietSavoryChip);
        hideBehaviorSwitch = findViewById(R.id.tasteSetupHideBehaviorSwitch);
        badgeBehaviorSwitch = findViewById(R.id.tasteSetupBadgeBehaviorSwitch);

        findViewById(R.id.tasteSetupBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.tasteSetupCancelButton).setOnClickListener(v -> finish());
        findViewById(R.id.tasteSetupSaveButton).setOnClickListener(v -> {
            Toast.makeText(this, "Đã lưu hồ sơ khẩu vị", Toast.LENGTH_SHORT).show();
            finish();
        });
        findViewById(R.id.tasteQuickAddChip).setOnClickListener(v -> showTagDialog(null));

        setupDietChips();
        setupBehaviorControls();
        renderTags();
        tasteStore.syncFromMongo(() -> {
            renderDietChips();
            syncBehaviorSwitches();
            renderTags();
        });
    }

    private void setupBehaviorControls() {
        syncBehaviorSwitches();
        hideBehaviorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingBehaviorSwitches) return;
            tasteStore.setHideCatalog(isChecked);
            syncBehaviorSwitches();
        });
        badgeBehaviorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bindingBehaviorSwitches) return;
            tasteStore.setBadgeCatalog(isChecked);
            syncBehaviorSwitches();
        });
        findViewById(R.id.tasteSetupHideBehaviorCard).setOnClickListener(v -> hideBehaviorSwitch.setChecked(true));
        findViewById(R.id.tasteSetupBadgeBehaviorCard).setOnClickListener(v -> badgeBehaviorSwitch.setChecked(true));
    }

    private void syncBehaviorSwitches() {
        bindingBehaviorSwitches = true;
        hideBehaviorSwitch.setChecked(tasteStore.hideCatalog());
        badgeBehaviorSwitch.setChecked(tasteStore.badgeCatalog());
        tintSwitch(hideBehaviorSwitch, tasteStore.hideCatalog());
        tintSwitch(badgeBehaviorSwitch, tasteStore.badgeCatalog());
        bindingBehaviorSwitches = false;
    }

    private void tintSwitch(SwitchMaterial view, boolean enabled) {
        view.setTrackTintList(ColorStateList.valueOf(getColor(enabled ? R.color.primary_bg : R.color.neutral_30)));
        view.setThumbTintList(ColorStateList.valueOf(getColor(enabled ? R.color.primary_main : R.color.neutral_60)));
    }

    private void setupDietChips() {
        veganChip.setOnClickListener(v -> selectDiet("vegan"));
        pureVeganChip.setOnClickListener(v -> selectDiet("pure_vegan"));
        savoryChip.setOnClickListener(v -> selectDiet("savory"));
        renderDietChips();
    }

    private void selectDiet(String mode) {
        tasteStore.setDietMode(mode);
        renderDietChips();
    }

    private void renderDietChips() {
        styleDietChip(veganChip, "vegan".equals(tasteStore.dietMode()));
        styleDietChip(pureVeganChip, "pure_vegan".equals(tasteStore.dietMode()));
        styleDietChip(savoryChip, "savory".equals(tasteStore.dietMode()));
    }

    private void styleDietChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_taste_chip_green : R.drawable.bg_taste_chip_green_outline);
        chip.setTextColor(getColor(selected ? R.color.background_main : R.color.primary_main));
        chip.setPadding(dp(14), 0, dp(14), 0);
    }

    private void renderTags() {
        dynamicTags.removeAllViews();
        for (TastePreferenceStore.TasteTag tag : tasteStore.tags()) {
            TextView chip = tagChip(tag);
            chip.setOnClickListener(v -> showTagDialog(tag));
            dynamicTags.addView(chip);
        }
    }

    private void showTagDialog(TastePreferenceStore.TasteTag tag) {
        TasteTagDialog.show(this, tasteStore, tag, this::renderTags);
    }

    private TextView tagChip(TastePreferenceStore.TasteTag tag) {
        TextView view = new TextView(this);
        String state = tag.enabled ? "Đang bật" : "Đang tắt";
        view.setText(state + " · " + tag.action + ": " + tag.label);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTextSize(14);
        view.setTypeface(view.getTypeface(), Typeface.BOLD);
        view.setPadding(dp(14), 0, dp(14), 0);
        styleTagChip(view, tag.action, tag.enabled);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
        );
        params.setMargins(0, 0, 0, dp(8));
        view.setLayoutParams(params);
        return view;
    }

    private void styleTagChip(TextView view, String action, boolean enabled) {
        if (!enabled) {
            view.setBackgroundResource(R.drawable.bg_taste_chip_green_outline);
            view.setTextColor(getColor(R.color.neutral_70));
        } else if (TastePreferenceStore.ACTION_WARN.equals(action)) {
            view.setBackgroundResource(R.drawable.bg_taste_chip_yellow);
            view.setTextColor(getColor(R.color.secondary_hover));
        } else {
            view.setBackgroundResource(R.drawable.bg_taste_chip_red);
            view.setTextColor(getColor(R.color.danger_main));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
