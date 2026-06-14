package com.veggo.app.presentation.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.veggo.app.MainActivity;
import com.veggo.app.databinding.ActivitySearchBinding;
import com.veggo.app.di.AppModule;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.domain.repository.CategoryRepository;
import com.veggo.app.domain.repository.ProductRepository;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.presentation.category.CategoryViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {
    private ActivitySearchBinding binding;
    private CategorySearchAdapter adapter;
    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;

    private List<AssetModels.Category> allCategories = new ArrayList<>();
    private Map<String, Integer> productCounts = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        categoryRepository = AppModule.provideCategoryRepository(this);
        productRepository = AppModule.provideProductRepository(this);

        setupViews();
        observeData();
    }

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new CategorySearchAdapter();
        binding.rvCategories.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvCategories.setAdapter(adapter);

        adapter.setOnCategoryClickListener(category -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(MainActivity.EXTRA_CATEGORY_ID, category.categoryId);
            startActivity(intent);
            finish();
        });

        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeData() {
        // Observe categories
        categoryRepository.observeCategories().observe(this, categories -> {
            if (categories != null) {
                allCategories = categories;
                updateAdapter();
            }
        });

        // Observe products to compute counts
        productRepository.observeProducts().observe(this, products -> {
            if (products != null) {
                productCounts.clear();
                for (com.veggo.app.domain.model.Product product : products) {
                    String catId = product.getCategoryId();
                    if (catId != null) {
                        int count = productCounts.containsKey(catId) ? productCounts.get(catId) : 0;
                        productCounts.put(catId, count + 1);
                    }
                }
                updateAdapter();
            }
        });
    }

    private void updateAdapter() {
        adapter.setData(allCategories, productCounts);
    }

    private void filterCategories(String query) {
        if (query.isEmpty()) {
            adapter.setData(allCategories, productCounts);
            return;
        }

        List<AssetModels.Category> filtered = new ArrayList<>();
        for (AssetModels.Category cat : allCategories) {
            if (cat.categoryName.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(cat);
            }
        }
        adapter.setData(filtered, productCounts);
    }
}
