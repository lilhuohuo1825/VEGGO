package com.veggo.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.databinding.ActivityMainBinding;
import com.veggo.app.presentation.cart.CartFragment;

public class MainActivity extends AppCompatActivity {
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

            return false;
        });


    }
}
