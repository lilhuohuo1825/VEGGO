package com.veggo.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.veggo.app.core.ui.BottomNavController;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.databinding.ActivityMainBinding;
import com.veggo.app.databinding.ComponentBottomNavBinding;
import com.veggo.app.presentation.cart.CartFragment;
import com.veggo.app.presentation.community.CommunityHomeActivity;
import com.veggo.app.presentation.home.HomeFragment;
import com.veggo.app.presentation.order.OrderHistoryFragment;
import com.veggo.app.presentation.profile.ProfileFragment;
import com.veggo.app.presentation.profile.AddFridgeIngredientActivity;
import com.veggo.app.presentation.profile.PostNotificationsActivity;
import com.veggo.app.presentation.profile.ProfileLoggedInFragment;

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
            Intent intent = getIntent();
            if (intent.hasExtra(EXTRA_CATEGORY_ID)) {
                openCategoryDetail(intent.getStringExtra(EXTRA_CATEGORY_ID), intent.getStringExtra(EXTRA_SUBCATEGORY_ID));
            } else {
                openTab(tabFromNavItem(intent.getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_home)));
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasExtra(EXTRA_CATEGORY_ID)) {
            openCategoryDetail(intent.getStringExtra(EXTRA_CATEGORY_ID), intent.getStringExtra(EXTRA_SUBCATEGORY_ID));
        } else {
            openTab(tabFromNavItem(intent.getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_home)));
        }
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
        setBottomNavVisible(true);
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
                fragment = OrderHistoryFragment.newInstance(false);
                break;
            case ACCOUNT:
                fragment = new AppPreferences(this).isLoggedIn()
                        ? new ProfileLoggedInFragment()
                        : new ProfileFragment();
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
        setBottomNavVisible(false);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, new CartFragment())
                .addToBackStack("cart")
                .commit();
    }

    public static final String EXTRA_CATEGORY_ID = "extra_category_id";
    public static final String EXTRA_SUBCATEGORY_ID = "extra_subcategory_id";

    public void openCategoryDetail(String categoryId, String subcategoryId) {
        Fragment fragment = new com.veggo.app.presentation.category.CategoryDetailFragment();
        Bundle args = new Bundle();
        if (categoryId != null) {
            args.putString(com.veggo.app.presentation.category.CategoryDetailFragment.ARG_CATEGORY_ID, categoryId);
        }
        if (subcategoryId != null) {
            args.putString(com.veggo.app.presentation.category.CategoryDetailFragment.ARG_SUBCATEGORY_ID, subcategoryId);
        }
        fragment.setArguments(args);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .addToBackStack("category_detail")
                .commit();
    }

    public void openCategoryScreen() {
        openCategoryScreen(null);
    }

    public void openCategoryScreen(String categoryId) {
        Fragment fragment = new com.veggo.app.presentation.category.CategoryFragment();
        if (categoryId != null) {
            Bundle args = new Bundle();
            args.putString(com.veggo.app.presentation.category.CategoryFragment.ARG_CATEGORY_ID, categoryId);
            fragment.setArguments(args);
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .addToBackStack("category")
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

    public void setBottomNavVisible(boolean isVisible) {
        if (bottomNavBinding == null) {
            return;
        }
        bottomNavBinding.getRoot().setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    private enum Tab {
        HOME,
        CART,
        COMMUNITY,
        ORDERS,
        ACCOUNT
    }
}
