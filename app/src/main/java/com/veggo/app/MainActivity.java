package com.veggo.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.veggo.app.databinding.ActivityMainBinding;
import com.veggo.app.databinding.ComponentBottomNavBinding;
import com.veggo.app.core.ui.BottomNavController;
import com.veggo.app.presentation.cart.CartFragment;
import com.veggo.app.presentation.community.CommunityHomeActivity;
import com.veggo.app.presentation.home.HomeFragment;
import com.veggo.app.presentation.order.OrderHistoryFragment;
import com.veggo.app.presentation.profile.ProfileFragment;
import com.veggo.app.presentation.profile.AddFridgeIngredientActivity;
import com.veggo.app.presentation.profile.PostNotificationsActivity;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_NAV_ITEM = "extra_selected_nav_item";

    private ActivityMainBinding binding;
    private ComponentBottomNavBinding bottomNavBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bottomNavBinding = binding.bottomNavHost;
        BottomNavController.applySystemBarColors(this);

        bottomNavBinding.navHomeButton.setOnClickListener(view -> openTab(Tab.HOME));
        bottomNavBinding.navCommunityButton.setOnClickListener(view -> openCommunityScreen());
        bottomNavBinding.navOrdersButton.setOnClickListener(view -> openTab(Tab.ORDERS));
        bottomNavBinding.navAccountButton.setOnClickListener(view -> openTab(Tab.ACCOUNT));
        bottomNavBinding.bottomNavCard.setOnClickListener(view -> {
        });
        bottomNavBinding.navScanButton.setOnClickListener(view -> {
            openScanScreen();
        });

        if (savedInstanceState == null) {
            openTab(tabFromNavItem(getIntent().getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_home)));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openTab(tabFromNavItem(intent.getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_home)));
    }

    private Tab tabFromNavItem(int itemId) {
        if (itemId == R.id.nav_home) {
            return Tab.HOME;
        }
        if (itemId == R.id.nav_profile) {
            return Tab.ACCOUNT;
        }
        if (itemId == R.id.nav_orders) {
            return Tab.ORDERS;
        }
        if (itemId == R.id.nav_cart) {
            return Tab.CART;
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
                openCommunityScreen();
                return;
            case CART:
                openCartScreen();
                return;
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
                .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .commit();
    }

    public void openCartScreen() {
        setSelectedTab(Tab.HOME);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, new CartFragment())
                .addToBackStack("cart")
                .commit();
    }

    public void openNotificationsScreen() {
        startActivity(new Intent(this, PostNotificationsActivity.class));
    }

    public void openScanScreen() {
        startActivity(new Intent(this, AddFridgeIngredientActivity.class));
    }

    public void openCommunityScreen() {
        getSupportFragmentManager()
                .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        setSelectedTab(Tab.COMMUNITY);
        startActivity(new Intent(this, CommunityHomeActivity.class));
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
        CART,
        COMMUNITY,
        ORDERS,
        ACCOUNT
    }
}
