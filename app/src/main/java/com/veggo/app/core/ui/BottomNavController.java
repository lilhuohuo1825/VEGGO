package com.veggo.app.core.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.databinding.ComponentBottomNavBinding;
import com.veggo.app.presentation.community.CommunityHomeActivity;
import com.veggo.app.presentation.profile.AddFridgeIngredientActivity;

public final class BottomNavController {
    private BottomNavController() {
    }

    public static void setup(Activity activity, ComponentBottomNavBinding binding, int selectedItemId) {
        applySystemBarColors(activity);
        applyState(activity, binding, selectedItemId);

        binding.navHomeButton.setOnClickListener(v -> openMainTab(activity, selectedItemId, R.id.nav_home));
        binding.navCommunityButton.setOnClickListener(v -> openCommunity(activity, selectedItemId));
        binding.navOrdersButton.setOnClickListener(v -> openMainTab(activity, selectedItemId, R.id.nav_orders));
        binding.navAccountButton.setOnClickListener(v -> openMainTab(activity, selectedItemId, R.id.nav_profile));
        binding.navScanButton.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, AddFridgeIngredientActivity.class))
        );
        binding.bottomNavCard.setOnClickListener(v -> {
        });
    }

    public static void applySystemBarColors(Activity activity) {
        activity.getWindow().setNavigationBarColor(ContextCompat.getColor(activity, R.color.neutral_10));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = activity.getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            );
        }
    }

    private static void openMainTab(Activity activity, int selectedItemId, int targetItemId) {
        if (selectedItemId == targetItemId && activity instanceof MainActivity) {
            return;
        }
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, targetItemId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    private static void openCommunity(Activity activity, int selectedItemId) {
        if (selectedItemId == R.id.nav_category && activity instanceof CommunityHomeActivity) {
            return;
        }
        activity.startActivity(new Intent(activity, CommunityHomeActivity.class));
    }

    private static void applyState(Activity activity, ComponentBottomNavBinding binding, int selectedItemId) {
        int selectedColor = ContextCompat.getColor(activity, R.color.primary_main);
        int unselectedColor = ContextCompat.getColor(activity, R.color.neutral_80);

        applyTabState(binding.navHomeButton, binding.navHomeIcon, selectedItemId == R.id.nav_home, selectedColor, unselectedColor, R.drawable.ic_home, R.drawable.ic_home);
        applyTabState(binding.navCommunityButton, binding.navCommunityIcon, selectedItemId == R.id.nav_category, selectedColor, unselectedColor, R.drawable.ic_community, R.drawable.ic_community_green);
        applyTabState(binding.navOrdersButton, binding.navOrdersIcon, selectedItemId == R.id.nav_orders, selectedColor, unselectedColor, R.drawable.ic_order, R.drawable.ic_order_green);
        applyTabState(binding.navAccountButton, binding.navAccountIcon, selectedItemId == R.id.nav_profile, selectedColor, unselectedColor, R.drawable.ic_user, R.drawable.ic_user_green);
    }

    private static void applyTabState(android.view.View container, AppCompatImageView icon, boolean selected, int selectedColor, int unselectedColor, int unselectedIconRes, int selectedIconRes) {
        icon.setImageResource(selected ? selectedIconRes : unselectedIconRes);
        icon.setImageTintList(ColorStateList.valueOf(selected ? selectedColor : unselectedColor));
        container.setSelected(selected);
    }
}
