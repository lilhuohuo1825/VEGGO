package com.veggo.app.presentation.category;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
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
import com.veggo.app.presentation.profile.TastePreferenceStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CategoryDetailFragment extends Fragment {
    public static final String ARG_CATEGORY_ID = "arg_category_id";
    public static final String ARG_SUBCATEGORY_ID = "arg_subcategory_id";

    private FragmentCategoryDetailBinding binding;
    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;

    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;
    private TastePreferenceStore tasteStore;

    private List<AssetModels.Category> allCategories = new ArrayList<>();
    private List<Product> catalogProducts = new ArrayList<>();
    private List<Product> currentProducts = new ArrayList<>();
    
    // Filter states
    private String selectedSort = ""; // "popular", "discount", "low_high", "high_low", "organic"
    private int selectedMinPrice = 0;
    private int selectedMaxPrice = 1000000; // Default 1M
    
    private String selectedCategoryId = "";
    private String selectedSubcategoryId = ""; // empty means "Tất cả"
    private String selectedBrand = ""; // empty means "Tất cả"

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
        tasteStore = new TastePreferenceStore(requireContext());

        categoryRepository.refreshCategories();

        if (getArguments() != null) {
            selectedCategoryId = getArguments().getString(ARG_CATEGORY_ID, "");
            selectedSubcategoryId = getArguments().getString(ARG_SUBCATEGORY_ID, "");
        }

        setupViews();
        observeData();
        productRepository.observeCatalogProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                catalogProducts = products;
            }
        });
        tasteStore.syncFromMongo(() -> {
            if (binding != null) {
                applyFiltersAndSort();
            }
        });
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
        productAdapter.setOnAddProductClickListener(product -> {
            Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
            intent.putExtra(ProductDetailActivity.EXTRA_OPEN_ADD_TO_CART, true);
            startActivity(intent);
        });

        // Navigate to SearchActivity when search bar is clicked
        View.OnClickListener openSearchClick = v -> {
            Intent intent = new Intent(requireContext(), com.veggo.app.presentation.search.SearchActivity.class);
            startActivity(intent);
        };
        binding.layoutSearch.getRoot().setOnClickListener(openSearchClick);
        binding.layoutSearch.edtSearch.setFocusable(false);
        binding.layoutSearch.edtSearch.setOnClickListener(openSearchClick);

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
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        PopupFilterBinding filterBinding = PopupFilterBinding.inflate(getLayoutInflater());
        dialog.setContentView(filterBinding.getRoot());
        dialog.setCanceledOnTouchOutside(true);

        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) return;

            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int sheetHeight = (int) (screenHeight * 0.92f);

            bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded);

            ViewGroup.LayoutParams sheetParams = bottomSheet.getLayoutParams();
            if (sheetParams == null) {
                sheetParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        sheetHeight
                );
            } else {
                sheetParams.height = sheetHeight;
            }
            bottomSheet.setLayoutParams(sheetParams);
            bottomSheet.requestLayout();

            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setPeekHeight(sheetHeight, true);
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

            filterBinding.scrollFilterContent.setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                            behavior.setDraggable(scrollY == 0)
            );

            int baseBottomPadding = getResources().getDimensionPixelSize(R.dimen.spacing_lg);
            ViewCompat.setOnApplyWindowInsetsListener(filterBinding.layoutFilterFooter, (v, insets) -> {
                int navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                v.setPadding(
                        v.getPaddingLeft(),
                        v.getPaddingTop(),
                        v.getPaddingRight(),
                        baseBottomPadding + navBarInset
                );
                return insets;
            });
            ViewCompat.requestApplyInsets(bottomSheet);
        });

        // Sync UI with current state
        if ("popular".equals(selectedSort)) {
            filterBinding.rbPopular.setChecked(true);
        } else if ("discount".equals(selectedSort)) {
            filterBinding.rbDiscount.setChecked(true);
        } else if ("low_high".equals(selectedSort)) {
            filterBinding.rbLowHigh.setChecked(true);
        } else if ("high_low".equals(selectedSort)) {
            filterBinding.rbHighLow.setChecked(true);
        } else if ("organic".equals(selectedSort)) {
            filterBinding.rbOrganic.setChecked(true);
        }

        filterBinding.rangePrice.setValues((float) selectedMinPrice, (float) selectedMaxPrice);
        filterBinding.txtPrice.setText(String.format("%,dđ - %,dđ", selectedMinPrice, selectedMaxPrice));

        populateCategories(filterBinding);
        populateBrands(filterBinding, selectedCategoryId, selectedSubcategoryId);

        filterBinding.rangePrice.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int min = values.get(0).intValue();
            int max = values.get(1).intValue();
            filterBinding.txtPrice.setText(String.format("%,dđ - %,dđ", min, max));
        });

        filterBinding.btnBack.setOnClickListener(v -> dialog.dismiss());
        filterBinding.btnApply.setOnClickListener(v -> {
            int checkedSortId = filterBinding.rgSort.getCheckedRadioButtonId();
            if (checkedSortId == R.id.rbPopular) selectedSort = "popular";
            else if (checkedSortId == R.id.rbDiscount) selectedSort = "discount";
            else if (checkedSortId == R.id.rbLowHigh) selectedSort = "low_high";
            else if (checkedSortId == R.id.rbHighLow) selectedSort = "high_low";
            else if (checkedSortId == R.id.rbOrganic) selectedSort = "organic";
            else selectedSort = "";

            int checkedCatId = filterBinding.cgCategories.getCheckedChipId();
            if (checkedCatId != View.NO_ID) {
                com.google.android.material.chip.Chip catChip = filterBinding.getRoot().findViewById(checkedCatId);
                selectedCategoryId = catChip.getTag() != null ? (String) catChip.getTag() : "";
            } else {
                selectedCategoryId = "";
            }

            int checkedSubId = filterBinding.cgSubcategories.getCheckedChipId();
            if (checkedSubId != View.NO_ID) {
                com.google.android.material.chip.Chip subChip = filterBinding.getRoot().findViewById(checkedSubId);
                selectedSubcategoryId = (String) subChip.getTag();
            } else {
                selectedSubcategoryId = ""; // All
            }

            int checkedBrandId = filterBinding.cgBrands.getCheckedChipId();
            if (checkedBrandId != View.NO_ID) {
                com.google.android.material.chip.Chip brandChip = filterBinding.getRoot().findViewById(checkedBrandId);
                selectedBrand = (String) brandChip.getTag();
            } else {
                selectedBrand = "";
            }

            List<Float> values = filterBinding.rangePrice.getValues();
            selectedMinPrice = values.get(0).intValue();
            selectedMaxPrice = values.get(1).intValue();

            // Find current category to update tabs
            if (selectedCategoryId.isEmpty()) {
                setupAllCategoryTabs();
            } else {
                for (AssetModels.Category cat : allCategories) {
                    if (cat.categoryId != null && cat.categoryId.equals(selectedCategoryId)) {
                        setupTabs(cat);
                        break;
                    }
                }
            }

            categoryAdapter.setSelectedCategoryId(selectedCategoryId);
            loadProducts();
            applyFiltersAndSort();
            dialog.dismiss();
        });

        filterBinding.txtClear.setOnClickListener(v -> {
            filterBinding.rgSort.clearCheck();
            filterBinding.rangePrice.setValues(0f, 1000000f);
            filterBinding.txtPrice.setText("0đ - 1,000,000đ");
            selectedSort = "";
            selectedMinPrice = 0;
            selectedMaxPrice = 1000000;
            selectedCategoryId = "";
            selectedSubcategoryId = "";
            selectedBrand = "";
            filterBinding.cgCategories.removeAllViews();
            filterBinding.cgSubcategories.removeAllViews();
            populateCategories(filterBinding);
            populateBrands(filterBinding, "", "");
        });

        dialog.show();
    }

    private void populateCategories(PopupFilterBinding filterBinding) {
        filterBinding.cgCategories.removeAllViews();

        com.google.android.material.chip.Chip allCategoryChip = createFilterChip(
                filterBinding.cgCategories, "Tất cả", "");
        if (selectedCategoryId == null || selectedCategoryId.isEmpty()) {
            allCategoryChip.setChecked(true);
        }
        allCategoryChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                updateSubcategories(filterBinding, null, "");
                populateBrands(filterBinding, "", "");
            }
        });

        for (AssetModels.Category category : allCategories) {
            if (category.categoryId == null || category.categoryId.trim().isEmpty()) {
                continue;
            }
            com.google.android.material.chip.Chip chip = createFilterChip(
                    filterBinding.cgCategories, category.categoryName, category.categoryId);
            if (category.categoryId.equals(selectedCategoryId)) {
                chip.setChecked(true);
            }
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    updateSubcategories(filterBinding, category, "");
                    populateBrands(filterBinding, category.categoryId, "");
                }
            });
        }

        if (selectedCategoryId != null && !selectedCategoryId.isEmpty()) {
            for (AssetModels.Category cat : allCategories) {
                if (selectedCategoryId.equals(cat.categoryId)) {
                    updateSubcategories(filterBinding, cat, selectedSubcategoryId);
                    break;
                }
            }
        } else {
            updateSubcategories(filterBinding, null, selectedSubcategoryId);
        }
    }

    private com.google.android.material.chip.Chip createFilterChip(
            com.google.android.material.chip.ChipGroup group, String label, String tag) {
        com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip)
                getLayoutInflater().inflate(R.layout.item_filter_chip, group, false);
        chip.setText(label);
        chip.setTag(tag);
        group.addView(chip);
        return chip;
    }

    private String getSelectedCategoryId(PopupFilterBinding filterBinding) {
        int checkedId = filterBinding.cgCategories.getCheckedChipId();
        if (checkedId == View.NO_ID) return "";
        com.google.android.material.chip.Chip chip = filterBinding.getRoot().findViewById(checkedId);
        return chip != null && chip.getTag() != null ? (String) chip.getTag() : "";
    }

    private String getSelectedSubcategoryId(PopupFilterBinding filterBinding) {
        int checkedId = filterBinding.cgSubcategories.getCheckedChipId();
        if (checkedId == View.NO_ID) return "";
        com.google.android.material.chip.Chip chip = filterBinding.getRoot().findViewById(checkedId);
        return chip != null && chip.getTag() != null ? (String) chip.getTag() : "";
    }

    private void updateSubcategories(
            PopupFilterBinding filterBinding,
            @Nullable AssetModels.Category category,
            @Nullable String activeSubcategoryId) {
        filterBinding.cgSubcategories.removeAllViews();
        String subcategoryId = activeSubcategoryId != null ? activeSubcategoryId : selectedSubcategoryId;

        com.google.android.material.chip.Chip allChip = createFilterChip(
                filterBinding.cgSubcategories, "Tất cả", "");
        if (subcategoryId == null || subcategoryId.isEmpty()) {
            allChip.setChecked(true);
        }
        allChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                populateBrands(filterBinding, getSelectedCategoryId(filterBinding), "");
            }
        });

        if (category == null || category.subcategories == null) {
            return;
        }

        for (AssetModels.Subcategory sub : category.subcategories) {
            if (sub.subcategoryId == null || sub.subcategoryId.trim().isEmpty()) {
                continue;
            }
            com.google.android.material.chip.Chip chip = createFilterChip(
                    filterBinding.cgSubcategories, sub.subcategoryName, sub.subcategoryId);
            if (sub.subcategoryId.equals(subcategoryId)) {
                chip.setChecked(true);
            }
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    populateBrands(
                            filterBinding,
                            getSelectedCategoryId(filterBinding),
                            sub.subcategoryId
                    );
                }
            });
        }
    }

    private void populateBrands(PopupFilterBinding filterBinding, String categoryId, String subcategoryId) {
        filterBinding.cgBrands.removeAllViews();

        com.google.android.material.chip.Chip allChip = createFilterChip(
                filterBinding.cgBrands, "Tất cả", "");

        Set<String> brands = new LinkedHashSet<>();
        for (Product product : getProductsForBrandFilter(categoryId, subcategoryId)) {
            String brand = resolveBrandLabel(product);
            if (brand != null) {
                brands.add(brand);
            }
        }

        List<String> sortedBrands = new ArrayList<>(brands);
        Collections.sort(sortedBrands, String.CASE_INSENSITIVE_ORDER);

        boolean hasSelectedBrand = false;
        if (!selectedBrand.isEmpty()) {
            for (String brand : sortedBrands) {
                if (brand.equalsIgnoreCase(selectedBrand)) {
                    hasSelectedBrand = true;
                    break;
                }
            }
        }
        if (!hasSelectedBrand) {
            selectedBrand = "";
        }
        if (selectedBrand.isEmpty()) {
            allChip.setChecked(true);
        }

        for (String brand : sortedBrands) {
            com.google.android.material.chip.Chip chip = createFilterChip(
                    filterBinding.cgBrands, brand, brand);
            if (brand.equalsIgnoreCase(selectedBrand)) {
                chip.setChecked(true);
            }
        }
    }

    private List<Product> getProductsForBrandFilter(String categoryId, String subcategoryId) {
        List<Product> source = resolveBrandSourceProducts(categoryId, subcategoryId);
        if (source.isEmpty()) {
            return Collections.emptyList();
        }

        if ((categoryId == null || categoryId.isEmpty())
                && (subcategoryId == null || subcategoryId.isEmpty())) {
            return source;
        }

        List<Product> filtered = new ArrayList<>();
        for (Product product : source) {
            if (product == null) continue;
            if (categoryId != null && !categoryId.isEmpty()) {
                String productCategoryId = product.getCategoryId();
                if (productCategoryId == null || !categoryId.equals(productCategoryId)) {
                    continue;
                }
            }
            if (subcategoryId != null && !subcategoryId.isEmpty()) {
                String productSubcategoryId = product.getSubcategoryId();
                if (productSubcategoryId == null || !subcategoryId.equals(productSubcategoryId)) {
                    continue;
                }
            }
            filtered.add(product);
        }
        return filtered;
    }

    private List<Product> resolveBrandSourceProducts(String categoryId, String subcategoryId) {
        if (!catalogProducts.isEmpty()) {
            return catalogProducts;
        }

        if (subcategoryId != null && !subcategoryId.isEmpty()) {
            List<Product> subcategoryProducts = productRepository
                    .observeProductsBySubcategory(subcategoryId)
                    .getValue();
            if (subcategoryProducts != null && !subcategoryProducts.isEmpty()) {
                return subcategoryProducts;
            }
        }

        if (categoryId != null && !categoryId.isEmpty()) {
            List<Product> categoryProducts = productRepository
                    .observeProductsByCategory(categoryId)
                    .getValue();
            if (categoryProducts != null && !categoryProducts.isEmpty()) {
                return categoryProducts;
            }
        }

        return currentProducts != null ? currentProducts : Collections.emptyList();
    }

    private String resolveBrandLabel(Product product) {
        if (product.getBrand() != null && !product.getBrand().trim().isEmpty()) {
            return product.getBrand().trim();
        }
        if (product.getProducer() != null && !product.getProducer().trim().isEmpty()) {
            return product.getProducer().trim();
        }
        return null;
    }

    private void setupAllCategoryTabs() {
        isBuildingTabs = true;
        binding.tabSubcategories.removeAllTabs();
        binding.tabSubcategories.addTab(binding.tabSubcategories.newTab().setText("Tất cả").setTag(""));
        selectedSubcategoryId = "";
        isBuildingTabs = false;
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
        if (selectedCategoryId == null || selectedCategoryId.isEmpty()) {
            productRepository.observeProducts().removeObservers(getViewLifecycleOwner());
            productRepository.observeProducts().observe(getViewLifecycleOwner(), products -> {
                if (products != null) {
                    currentProducts = products;
                    applyFiltersAndSort();
                }
            });
            return;
        }

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
        String query = binding.layoutSearch.edtSearch.getText().toString().toLowerCase();

        for (Product product : currentProducts) {
            boolean matchesSearch = query.isEmpty() || product.getName().toLowerCase().contains(query);
            boolean matchesPrice = product.getPrice() >= selectedMinPrice && product.getPrice() <= selectedMaxPrice;
            boolean matchesBrand = selectedBrand.isEmpty()
                    || selectedBrand.equalsIgnoreCase(resolveBrandLabel(product));

            if (matchesSearch && matchesPrice && matchesBrand && !tasteStore.shouldHide(product)) {
                if ("discount".equals(selectedSort)) {
                    if (product.hasActiveDiscount()) {
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
