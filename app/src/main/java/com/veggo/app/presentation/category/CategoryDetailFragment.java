package com.veggo.app.presentation.category;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.veggo.app.R;
import com.veggo.app.adapter.ProductAdapter;
import com.veggo.app.databinding.FragmentCategoryDetailBinding;
import com.veggo.app.di.AppModule;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.repository.CategoryRepository;
import com.veggo.app.domain.repository.ProductRepository;
import com.veggo.app.presentation.product.ProductDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class CategoryDetailFragment extends Fragment {
    public static final String ARG_CATEGORY_ID = "arg_category_id";
    public static final String ARG_SUBCATEGORY_ID = "arg_subcategory_id";

    private FragmentCategoryDetailBinding binding;
    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;

    private CategoryHorizontalAdapter categoryAdapter;
    private ProductAdapter productAdapter;

    private List<AssetModels.Category> allCategories = new ArrayList<>();
    private List<Product> currentProducts = new ArrayList<>();
    
    private String selectedCategoryId = "";
    private String selectedSubcategoryId = ""; // empty means "Tất cả"

    private boolean isInitialLoad = true;
    private boolean isBuildingTabs = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoryDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        categoryRepository = AppModule.provideCategoryRepository(requireContext());
        productRepository = AppModule.provideProductRepository(requireContext());

        if (getArguments() != null) {
            selectedCategoryId = getArguments().getString(ARG_CATEGORY_ID, "");
            selectedSubcategoryId = getArguments().getString(ARG_SUBCATEGORY_ID, "");
        }

        setupViews();
        observeData();
    }

    private void setupViews() {
        // Horizontal Categories
        categoryAdapter = new CategoryHorizontalAdapter();
        binding.rvHorizontalCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvHorizontalCategories.setAdapter(categoryAdapter);
        categoryAdapter.setOnCategoryClickListener(category -> {
            selectedCategoryId = category.categoryId;
            selectedSubcategoryId = ""; // Reset subcategory when switching category
            categoryAdapter.setSelectedCategoryId(selectedCategoryId);
            setupTabs(category);
            loadProducts();
        });

        // Product Grid
        productAdapter = new ProductAdapter();
        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvProducts.setAdapter(productAdapter);
        productAdapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
            startActivity(intent);
        });

        // Search text filter
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProductsByName(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.tabSubcategories.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (isBuildingTabs) return;
                if (tab.getTag() != null) {
                    selectedSubcategoryId = (String) tab.getTag();
                } else {
                    selectedSubcategoryId = ""; // "Tất cả"
                }
                loadProducts();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void observeData() {
        categoryRepository.observeCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null && !categories.isEmpty()) {
                allCategories = categories;
                categoryAdapter.setData(categories);

                // If no initial category or category is invalid, fallback to first category
                if (selectedCategoryId.isEmpty()) {
                    selectedCategoryId = categories.get(0).categoryId;
                }

                categoryAdapter.setSelectedCategoryId(selectedCategoryId);

                // Find currently selected category
                AssetModels.Category currentCat = null;
                for (AssetModels.Category cat : categories) {
                    if (cat.categoryId.equals(selectedCategoryId)) {
                        currentCat = cat;
                        break;
                    }
                }

                if (currentCat != null) {
                    setupTabs(currentCat);
                }
                loadProducts();
            }
        });
    }

    private void setupTabs(AssetModels.Category category) {
        isBuildingTabs = true;
        binding.tabSubcategories.removeAllTabs();

        // Add "Tất cả" tab
        TabLayout.Tab allTab = binding.tabSubcategories.newTab().setText("Tất cả").setTag("");
        binding.tabSubcategories.addTab(allTab);

        TabLayout.Tab targetTab = null;
        if (category.subcategories != null) {
            for (AssetModels.Subcategory sub : category.subcategories) {
                TabLayout.Tab tab = binding.tabSubcategories.newTab()
                        .setText(sub.subcategoryName)
                        .setTag(sub.subcategoryId);
                binding.tabSubcategories.addTab(tab);

                // If this matches the target subcategory, keep a reference to select it
                if (sub.subcategoryId.equals(selectedSubcategoryId)) {
                    targetTab = tab;
                }
            }
        }

        if (targetTab != null) {
            targetTab.select();
            selectedSubcategoryId = (String) targetTab.getTag();
        } else {
            allTab.select();
            selectedSubcategoryId = "";
        }

        isBuildingTabs = false;
        isInitialLoad = false;
    }

    private void loadProducts() {
        if (selectedCategoryId.isEmpty()) return;

        if (selectedSubcategoryId.isEmpty()) {
            // Observe products by Category
            productRepository.observeProductsByCategory(selectedCategoryId).observe(getViewLifecycleOwner(), products -> {
                if (products != null) {
                    currentProducts = products;
                    productAdapter.submitList(products);
                }
            });
        } else {
            // Observe products by Subcategory
            productRepository.observeProductsBySubcategory(selectedSubcategoryId).observe(getViewLifecycleOwner(), products -> {
                if (products != null) {
                    currentProducts = products;
                    productAdapter.submitList(products);
                }
            });
        }
    }

    private void filterProductsByName(String query) {
        if (query.isEmpty()) {
            productAdapter.submitList(currentProducts);
            return;
        }

        List<Product> filtered = new ArrayList<>();
        for (Product product : currentProducts) {
            if (product.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(product);
            }
        }
        productAdapter.submitList(filtered);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
