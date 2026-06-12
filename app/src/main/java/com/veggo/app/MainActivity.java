package com.veggo.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.databinding.ActivityMainBinding;
import com.veggo.app.presentation.cart.CartFragment;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_NAV_ITEM = "selected_nav_item";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_cart) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.mainFragmentContainer, new CartFragment())
                        .commit();
                return true;
            }

            return true;
        });

        binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        binding.bottomNavigation.setSelectedItemId(intent.getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_profile));
    }
}
