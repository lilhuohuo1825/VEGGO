package com.veggo.app;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.order.OrderHistoryFragment;
import com.veggo.app.presentation.home.HomeFragment;
import com.veggo.app.presentation.category.CategoryFragment;
import com.veggo.app.presentation.cart.CartFragment;
import com.veggo.app.presentation.profile.ProfileFragment;
import com.veggo.app.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity {
    public static final String EXTRA_SELECTED_NAV_ITEM = "extra_selected_nav_item";
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                showFragment(new HomeFragment());
                return true;
            }
            if (itemId == R.id.nav_category) {
                showFragment(new CategoryFragment());
                return true;
            }
            if (itemId == R.id.nav_cart) {
                showFragment(new CartFragment());
                return true;
            }
            if (itemId == R.id.nav_orders) {
                showFragment(new OrderHistoryFragment());
                return true;
            }
            if (itemId == R.id.nav_profile) {
                showFragment(new ProfileFragment());
                return true;
            }
            return true;
        });
        if (savedInstanceState == null) {
            int selectedItem = getIntent().getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_profile);
            binding.bottomNavigation.setSelectedItemId(selectedItem);
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .commit();
    }
}
