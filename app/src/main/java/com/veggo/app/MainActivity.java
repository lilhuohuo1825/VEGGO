package com.veggo.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.veggo.app.databinding.ActivityMainBinding;
import com.veggo.app.databinding.ComponentBottomNavBinding;
import com.veggo.app.presentation.community.CommunityFragment;
import com.veggo.app.presentation.home.HomeFragment;
import com.veggo.app.presentation.order.OrderHistoryFragment;
import com.veggo.app.presentation.profile.ProfileFragment;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_NAV_ITEM = "extra_selected_nav_item";

    private ActivityMainBinding binding;
    private ComponentBottomNavBinding bottomNavBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });

        bottomNavBinding = binding.bottomNavHost;

        bottomNavBinding.navHomeButton.setOnClickListener(view -> openTab(Tab.HOME));
        bottomNavBinding.navCommunityButton.setOnClickListener(view -> openTab(Tab.COMMUNITY));
        bottomNavBinding.navOrdersButton.setOnClickListener(view -> openTab(Tab.ORDERS));
        bottomNavBinding.navAccountButton.setOnClickListener(view -> openTab(Tab.ACCOUNT));
        bottomNavBinding.bottomNavCard.setOnClickListener(view -> {
        });
        bottomNavBinding.navScanButton.setOnClickListener(view -> {
        });

        if (savedInstanceState == null) {
            openTab(tabFromNavItem(getIntent().getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_profile)));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openTab(tabFromNavItem(intent.getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_profile)));
    }

    private Tab tabFromNavItem(int itemId) {
        if (itemId == R.id.nav_profile) {
            return Tab.ACCOUNT;
        }
        if (itemId == R.id.nav_orders) {
            return Tab.ORDERS;
        }
        if (itemId == R.id.nav_category) {
            return Tab.COMMUNITY;
        }
        return Tab.HOME;
    }

    private void openTab(Tab tab) {
        setSelectedTab(tab);

        Fragment fragment;
        switch (tab) {
            case COMMUNITY:
                fragment = new CommunityFragment();
                break;
            case ORDERS:
                fragment = new OrderHistoryFragment();
                break;
            case ACCOUNT:
                fragment = new ProfileFragment();
                break;
            case HOME:
            default:
                fragment = new HomeFragment();
                break;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .commit();
    }

    private void setSelectedTab(Tab tab) {
        int selectedColor = ContextCompat.getColor(this, R.color.primary_main);
        int unselectedColor = ContextCompat.getColor(this, R.color.neutral_80);

        applyTabState(bottomNavBinding.navHomeButton, bottomNavBinding.navHomeIcon, tab == Tab.HOME, selectedColor, unselectedColor, R.drawable.ic_home, R.drawable.ic_home);
        applyTabState(bottomNavBinding.navCommunityButton, bottomNavBinding.navCommunityIcon, tab == Tab.COMMUNITY, selectedColor, unselectedColor, R.drawable.ic_community, R.drawable.ic_community_green);
        applyTabState(bottomNavBinding.navOrdersButton, bottomNavBinding.navOrdersIcon, tab == Tab.ORDERS, selectedColor, unselectedColor, R.drawable.ic_order, R.drawable.ic_order_green);
        applyTabState(bottomNavBinding.navAccountButton, bottomNavBinding.navAccountIcon, tab == Tab.ACCOUNT, selectedColor, unselectedColor, R.drawable.ic_user, R.drawable.ic_user_green);
    }

    private void applyTabState(android.view.View container, AppCompatImageView icon, boolean selected, int selectedColor, int unselectedColor, int unselectedIconRes, int selectedIconRes) {
        int color = selected ? selectedColor : unselectedColor;
        icon.setImageResource(selected ? selectedIconRes : unselectedIconRes);
        icon.setImageTintList(ColorStateList.valueOf(color));
        container.setSelected(selected);
    }

    private enum Tab {
        HOME,
        COMMUNITY,
        ORDERS,
        ACCOUNT
    }
}
