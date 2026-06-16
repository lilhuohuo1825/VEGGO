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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.veggo.app.R;
import com.veggo.app.adapter.ProductAdapter;
import com.veggo.app.databinding.FragmentCategoryDetailBinding;
import com.veggo.app.databinding.PopupFilterBinding;
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

    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;

    private List<AssetModels.Category> allCategories = new ArrayList<>();
    private List<Product> currentProducts = new ArrayList<>();
    
    // Filter states
    private String selectedSort = ""; // "popular", "discount", "low_high", "high_low", "organic"
    private int selectedMinPrice = 0;
    private int selectedMaxPrice = 1000000; // Default 1M
    
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
        categoryAdapter = new CategoryAdapter(CategoryAdapter.TYPE_HORIZONTAL);
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

        // Navigate to SearchActivity when search bar is clicked
        View.OnClickListener openSearchClick = v -> {
            Intent intent = new Intent(requireContext(), com.veggo.app.presentation.search.SearchActivity.class);
            startActivity(intent);
        };
        binding.layoutSearch.setOnClickListener(openSearchClick);
        binding.etSearch.setOnClickListener(openSearchClick);

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

        binding.ivFilter.setOnClickListener(v -> showFilterPopup());
    }

    private void showFilterPopup() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        PopupFilterBinding filterBinding = PopupFilterBinding.inflate(getLayoutInflater());
        dialog.setContentView(filterBinding.getRoot());

        // Sync UI with current state
        filterBinding.chkPopular.setChecked("popular".equals(selectedSort));
        filterBinding.chkDiscount.setChecked("discount".equals(selectedSort));
        filterBinding.chkLowHigh.setChecked("low_high".equals(selectedSort));
        filterBinding.chkHighLow.setChecked("high_low".equals(selectedSort));
        filterBinding.chkOrganic.setChecked("organic".equals(selectedSort));

        filterBinding.rangePrice.setValues((float) selectedMinPrice, (float) selectedMaxPrice);
        filterBinding.txtPrice.setText(String.format("%,dđ - %,dđ", selectedMinPrice, selectedMaxPrice));

        // Populate Categories
        for (AssetModels.Category category : allCategories) {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) getLayoutInflater().inflate(com.veggo.app.R.layout.item_filter_chip, filterBinding.cgCategories, false);
            chip.setText(category.categoryName);
            chip.setTag(category.categoryId);
            if (category.categoryId.equals(selectedCategoryId)) {
                chip.setChecked(true);
            }
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    updateSubcategories(filterBinding, category);
                }
            });
            filterBinding.cgCategories.addView(chip);
        }

        // Initial Subcategories if category selected
        for (AssetModels.Category cat : allCategories) {
            if (cat.categoryId.equals(selectedCategoryId)) {
                updateSubcategories(filterBinding, cat);
                break;
            }
        }

        // Handle mutual exclusivity for sort checkboxes
        View.OnClickListener sortClickListener = v -> {
            filterBinding.chkPopular.setChecked(v == filterBinding.chkPopular);
            filterBinding.chkDiscount.setChecked(v == filterBinding.chkDiscount);
            filterBinding.chkLowHigh.setChecked(v == filterBinding.chkLowHigh);
            filterBinding.chkHighLow.setChecked(v == filterBinding.chkHighLow);
            filterBinding.chkOrganic.setChecked(v == filterBinding.chkOrganic);
        };

        filterBinding.chkPopular.setOnClickListener(sortClickListener);
        filterBinding.chkDiscount.setOnClickListener(sortClickListener);
        filterBinding.chkLowHigh.setOnClickListener(sortClickListener);
        filterBinding.chkHighLow.setOnClickListener(sortClickListener);
        filterBinding.chkOrganic.setOnClickListener(sortClickListener);

        filterBinding.rangePrice.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int min = values.get(0).intValue();
            int max = values.get(1).intValue();
            filterBinding.txtPrice.setText(String.format("%,dđ - %,dđ", min, max));
        });

        filterBinding.btnBack.setOnClickListener(v -> dialog.dismiss());
        filterBinding.btnApply.setOnClickListener(v -> {
            // Save state
            if (filterBinding.chkPopular.isChecked()) selectedSort = "popular";
            else if (filterBinding.chkDiscount.isChecked()) selectedSort = "discount";
            else if (filterBinding.chkLowHigh.isChecked()) selectedSort = "low_high";
            else if (filterBinding.chkHighLow.isChecked()) selectedSort = "high_low";
            else if (filterBinding.chkOrganic.isChecked()) selectedSort = "organic";
            else selectedSort = "";

            // Category & Subcategory selection
            int checkedCatId = filterBinding.cgCategories.getCheckedChipId();
            if (checkedCatId != View.NO_ID) {
                com.google.android.material.chip.Chip catChip = filterBinding.getRoot().findViewById(checkedCatId);
                selectedCategoryId = (String) catChip.getTag();
            }

            int checkedSubId = filterBinding.cgSubcategories.getCheckedChipId();
            if (checkedSubId != View.NO_ID) {
                com.google.android.material.chip.Chip subChip = filterBinding.getRoot().findViewById(checkedSubId);
                selectedSubcategoryId = (String) subChip.getTag();
            } else {
                selectedSubcategoryId = ""; // All
            }

            List<Float> values = filterBinding.rangePrice.getValues();
            selectedMinPrice = values.get(0).intValue();
            selectedMaxPrice = values.get(1).intValue();

            // Find current category to update tabs
            for (AssetModels.Category cat : allCategories) {
                if (cat.categoryId.equals(selectedCategoryId)) {
                    setupTabs(cat);
                    break;
                }
            }

            applyFiltersAndSort();
            dialog.dismiss();
        });

        filterBinding.txtClear.setOnClickListener(v -> {
            filterBinding.chkPopular.setChecked(false);
            filterBinding.chkDiscount.setChecked(false);
            filterBinding.chkLowHigh.setChecked(false);
            filterBinding.chkHighLow.setChecked(false);
            filterBinding.chkOrganic.setChecked(false);
            filterBinding.rangePrice.setValues(0f, 1000000f);
            filterBinding.txtPrice.setText("0đ - 1,000,000đ");
            filterBinding.cgCategories.clearCheck();
            filterBinding.cgSubcategories.removeAllViews();
        });

        dialog.show();
    }

    private void updateSubcategories(PopupFilterBinding filterBinding, AssetModels.Category category) {
        filterBinding.cgSubcategories.removeAllViews();
        
        // Add "All" option
        com.google.android.material.chip.Chip allChip = (com.google.android.material.chip.Chip) getLayoutInflater().inflate(com.veggo.app.R.layout.item_filter_chip, filterBinding.cgSubcategories, false);
        allChip.setText("Tất cả");
        allChip.setTag("");
        if (selectedSubcategoryId.isEmpty()) {
            allChip.setChecked(true);
        }
        filterBinding.cgSubcategories.addView(allChip);

        if (category.subcategories != null) {
            for (AssetModels.Subcategory sub : category.subcategories) {
                com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) getLayoutInflater().inflate(com.veggo.app.R.layout.item_filter_chip, filterBinding.cgSubcategories, false);
                chip.setText(sub.subcategoryName);
                chip.setTag(sub.subcategoryId);
                if (sub.subcategoryId.equals(selectedSubcategoryId)) {
                    chip.setChecked(true);
                }
                filterBinding.cgSubcategories.addView(chip);
            }
        }
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
        if (selectedCategoryId == null || selectedCategoryId.isEmpty()) return;

        if (selectedSubcategoryId == null || selectedSubcategoryId.isEmpty()) {
            // Observe products by Category
            productRepository.observeProductsByCategory(selectedCategoryId).removeObservers(getViewLifecycleOwner());
            productRepository.observeProductsByCategory(selectedCategoryId).observe(getViewLifecycleOwner(), products -> {
                if (products != null) {
                    currentProducts = products;
                    applyFiltersAndSort();
                }
            });
        } else {
            // Observe products by Subcategory
            productRepository.observeProductsBySubcategory(selectedSubcategoryId).removeObservers(getViewLifecycleOwner());
            productRepository.observeProductsBySubcategory(selectedSubcategoryId).observe(getViewLifecycleOwner(), products -> {
                if (products != null) {
                    currentProducts = products;
                    applyFiltersAndSort();
                }
            });
        }
    }

    private void applyFiltersAndSort() {
        if (currentProducts == null) return;

        List<Product> filtered = new ArrayList<>();
        
        // 1. Filter by Price and Search Query
        String query = binding.etSearch.getText().toString().toLowerCase();
        
        for (Product product : currentProducts) {
            boolean matchesSearch = query.isEmpty() || product.getName().toLowerCase().contains(query);
            boolean matchesPrice = product.getPrice() >= selectedMinPrice && product.getPrice() <= selectedMaxPrice;
            
            if (matchesSearch && matchesPrice) {
                if ("discount".equals(selectedSort)) {
                    if (product.getPrice() < product.getOriginalPrice()) {
                        filtered.add(product);
                    }
                } else if ("organic".equals(selectedSort)) {
                    if (product.getName().toLowerCase().contains("organic") || 
                        (product.getDescription() != null && product.getDescription().toLowerCase().contains("organic"))) {
                        filtered.add(product);
                    }
                } else {
                    filtered.add(product);
                }
            }
        }

        // 2. Sort
        java.util.Collections.sort(filtered, (p1, p2) -> {
            switch (selectedSort) {
                case "low_high":
                    return Long.compare(p1.getPrice(), p2.getPrice());
                case "high_low":
                    return Long.compare(p2.getPrice(), p1.getPrice());
                case "popular":
                    return Integer.compare(p2.getSoldCount(), p1.getSoldCount());
                default:
                    return 0;
            }
        });

        productAdapter.submitList(filtered);
    }

    private void filterProductsByName(String query) {
        applyFiltersAndSort();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
