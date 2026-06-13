package com.veggo.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.veggo.app.databinding.ActivityMainBinding;
import com.veggo.app.presentation.category.CategoryFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();

        // Mở trang Category mặc định khi build app
        if (savedInstanceState == null) {
            loadFragment(new CategoryFragment());
            binding.bottomNavigation.setSelectedItemId(R.id.nav_category);
        }
    }

    private void setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_category) {
                loadFragment(new CategoryFragment());
                return true;
            }
            // Handle other items...
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .commit();
    }
}
