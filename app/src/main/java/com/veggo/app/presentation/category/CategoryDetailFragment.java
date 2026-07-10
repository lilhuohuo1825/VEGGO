package com.veggo.app.presentation.category;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.dto.FlashSaleResponseDto;
import com.veggo.app.di.AppModule;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.repository.CategoryRepository;
import com.veggo.app.domain.repository.ProductRepository;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.speech.SearchVoiceInputController;
import com.veggo.app.presentation.product.AddToCartBottomSheetHelper;
import com.veggo.app.presentation.product.ProductDetailActivity;
import com.veggo.app.presentation.profile.FridgeQuickScanHelper;
import com.veggo.app.presentation.profile.TastePreferenceStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.Normalizer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryDetailFragment extends Fragment {
    public static final String ARG_CATEGORY_ID = "arg_category_id";
    public static final String ARG_SUBCATEGORY_ID = "arg_subcategory_id";
    private static final long FILTER_DEBOUNCE_MS = 200L;

    private FragmentCategoryDetailBinding binding;
    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;

    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;
    private TastePreferenceStore tasteStore;

    private List<AssetModels.Category> allCategories = new ArrayList<>();
    private List<Product> catalogProducts = new ArrayList<>();
    private List<Product> currentProducts = new ArrayList<>();
    private final Map<String, long[]> flashSalePricingByKey = new HashMap<>();
    
    // Filter states
    private String selectedSort = ""; // "popular", "discount", "low_high", "high_low", "organic"
    private int selectedMinPrice = 0;
    private int selectedMaxPrice = 1000000; // Default 1M
    
    private String selectedCategoryId = "";
    private String selectedSubcategoryId = ""; // empty means "Tất cả"
    private String selectedBrand = ""; // empty means "Tất cả"

    private boolean isInitialLoad = true;
    private boolean isBuildingTabs = false;
    private SearchVoiceInputController voiceInputController;
    private SwipeRefreshLayout categoryDetailRefreshLayout;
    private final Handler filterHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService filterExecutor = Executors.newSingleThreadExecutor();
    private Runnable pendingFilterRunnable;
    private int filterGeneration = 0;

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
        setupPullToRefresh();
        observeData();
        loadActiveFlashSales();
        productRepository.observeCatalogProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                catalogProducts = products;
                updateCurrentProductsFromCatalog();
                applyFiltersAndSort();
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
        productAdapter.setTastePreferenceStore(tasteStore);
        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvProducts.setAdapter(productAdapter);
        productAdapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
            startActivity(intent);
        });
        productAdapter.setOnAddProductClickListener(product ->
                com.veggo.app.presentation.product.AddToCartBottomSheetHelper.show(
                        requireContext(),
                        product,
                        null
                )
        );

        // Local search + voice input on category product list
        binding.layoutSearch.edtSearch.setFocusable(true);
        binding.layoutSearch.edtSearch.setFocusableInTouchMode(true);
        binding.layoutSearch.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleApplyFiltersAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        voiceInputController = SearchVoiceInputController.attach(
                this,
                binding.layoutSearch.getRoot(),
                binding.layoutSearch.edtSearch,
                this::scheduleApplyFiltersAndSort
        );
        binding.layoutSearch.btnCamera.setOnClickListener(v ->
                startActivity(FridgeQuickScanHelper.createSearchSuggestionCameraIntent(requireContext()))
        );
        binding.categoryDetailBackButton.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed()
        );

        binding.tabSubcategories.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (isBuildingTabs) return;
                if (tab.getTag() != null) {
                    selectedSubcategoryId = (String) tab.getTag();
                } else {
                    selectedSubcategoryId = ""; // "Tất cả"
                }
                applyFiltersAndSort();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.ivFilter.setOnClickListener(v -> showFilterPopup());
    }

    private void setupPullToRefresh() {
        categoryDetailRefreshLayout = PullToRefreshHelper.wrap(binding.rvProducts, this::refreshPageData);
    }

    private void refreshPageData() {
        categoryRepository.refreshCategories();
        productRepository.refreshProducts();
        loadActiveFlashSales();
        loadProducts();
        tasteStore.syncFromMongo(() -> {
            if (binding != null) {
                applyFiltersAndSort();
                PullToRefreshHelper.finish(categoryDetailRefreshLayout);
            }
        });
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
        updateCurrentProductsFromCatalog();
        applyFiltersAndSort();
    }

    private void updateCurrentProductsFromCatalog() {
        if (selectedCategoryId == null || selectedCategoryId.isEmpty()) {
            currentProducts = catalogProducts != null
                    ? catalogProducts
                    : Collections.emptyList();
            return;
        }

        List<Product> filtered = new ArrayList<>();
        List<Product> source = catalogProducts != null ? catalogProducts : Collections.emptyList();
        for (Product product : source) {
            if (product != null && selectedCategoryId.equals(product.getCategoryId())) {
                filtered.add(product);
            }
        }
        currentProducts = filtered;
    }

    private void scheduleApplyFiltersAndSort() {
        if (pendingFilterRunnable != null) {
            filterHandler.removeCallbacks(pendingFilterRunnable);
        }
        pendingFilterRunnable = this::applyFiltersAndSort;
        filterHandler.postDelayed(pendingFilterRunnable, FILTER_DEBOUNCE_MS);
    }


    private void loadActiveFlashSales() {
        PromotionApi promotionApi = ApiClient.createService(PromotionApi.class);
        promotionApi.getFlashSales().enqueue(new Callback<FlashSaleResponseDto>() {
            @Override
            public void onResponse(Call<FlashSaleResponseDto> call, Response<FlashSaleResponseDto> response) {
                FlashSaleResponseDto body = response.body();
                if (!response.isSuccessful() || body == null || !body.isSuccess() || body.getData() == null) {
                    return;
                }

                flashSalePricingByKey.clear();
                for (FlashSaleResponseDto.FlashSaleItemDto item : body.getData()) {
                    if (item == null) {
                        continue;
                    }
                    long[] pricing = new long[] { item.getPrice(), item.getOriginalPrice() };
                    if (item.getSku() != null && !item.getSku().trim().isEmpty()) {
                        flashSalePricingByKey.put(item.getSku().trim(), pricing);
                    }
                    if (item.getId() != null && !item.getId().trim().isEmpty()) {
                        flashSalePricingByKey.put(item.getId().trim(), pricing);
                    }
                }

                if (binding != null) {
                    applyFiltersAndSort();
                }
            }

            @Override
            public void onFailure(Call<FlashSaleResponseDto> call, Throwable t) {
                // Keep catalog-only discount filtering when flash sale API is unavailable.
            }
        });
    }

    private Product applyFlashSalePricing(Product product) {
        if (product == null) {
            return null;
        }

        long[] pricing = null;
        if (product.getSku() != null && !product.getSku().trim().isEmpty()) {
            pricing = flashSalePricingByKey.get(product.getSku().trim());
        }
        if (pricing == null && product.getId() != null && !product.getId().trim().isEmpty()) {
            pricing = flashSalePricingByKey.get(product.getId().trim());
        }
        if (pricing == null) {
            return product;
        }

        return product.withPricing(pricing[0], pricing[1]);
    }

    private boolean isDiscountedProduct(Product product) {
        Product displayProduct = applyFlashSalePricing(product);
        return displayProduct != null && displayProduct.hasActiveDiscount();
    }

    private boolean matchesSelectedSubcategory(Product product) {
        return matchesSelectedSubcategory(product, selectedSubcategoryId);
    }

    private boolean matchesSelectedSubcategory(Product product, String subcategoryId) {
        if (product == null) {
            return false;
        }
        if (subcategoryId == null || subcategoryId.isEmpty()) {
            return true;
        }
        if (subcategoryId.equals(product.getSubcategoryId())) {
            return true;
        }
        return matchesSubcategoryFallback(product, subcategoryId);
    }

    private boolean matchesSubcategoryFallback(Product product, String subcategoryId) {
        if (product == null || subcategoryId == null || subcategoryId.isEmpty()) {
            return false;
        }
        String name = normalizeProductName(product.getName());
        switch (subcategoryId) {
            case "SUB00101":
                return name.contains("ca cao") || name.contains("cacao");
            case "SUB00102":
                return name.contains("ca phe") || name.contains("coffee");
            default:
                return false;
        }
    }

    private static String normalizeProductName(String value) {
        String raw = value == null ? "" : value.toLowerCase(Locale.ROOT);
        String stripped = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return stripped.replace('đ', 'd');
    }

    private void applyFiltersAndSort() {
        if (currentProducts == null || binding == null) {
            return;
        }

        final int generation = ++filterGeneration;
        final List<Product> sourceProducts = new ArrayList<>(currentProducts);
        final String query = binding.layoutSearch.edtSearch.getText().toString().toLowerCase(Locale.ROOT);
        final String sort = selectedSort;
        final int minPrice = selectedMinPrice;
        final int maxPrice = selectedMaxPrice;
        final String brand = selectedBrand;
        final String subcategoryId = selectedSubcategoryId;

        filterExecutor.execute(() -> {
            List<Product> filtered = new ArrayList<>();

            for (Product product : sourceProducts) {
                Product displayProduct = applyFlashSalePricing(product);
                if (displayProduct == null) {
                    continue;
                }
                boolean matchesSearch = query.isEmpty()
                        || product.getName().toLowerCase(Locale.ROOT).contains(query);
                boolean matchesPrice = displayProduct.getPrice() >= minPrice
                        && displayProduct.getPrice() <= maxPrice;
                boolean matchesBrand = brand.isEmpty()
                        || brand.equalsIgnoreCase(resolveBrandLabel(product));
                boolean matchesSubcategory = matchesSelectedSubcategory(product, subcategoryId);

                if (matchesSearch && matchesPrice && matchesBrand && matchesSubcategory) {
                    if ("discount".equals(sort)) {
                        if (isDiscountedProduct(product)) {
                            filtered.add(displayProduct);
                        }
                    } else if ("organic".equals(sort)) {
                        if (product.getName().toLowerCase(Locale.ROOT).contains("organic")
                                || (product.getDescription() != null
                                && product.getDescription().toLowerCase(Locale.ROOT).contains("organic"))) {
                            filtered.add(displayProduct);
                        }
                    } else {
                        filtered.add(displayProduct);
                    }
                }
            }

            Collections.sort(filtered, (p1, p2) -> {
                switch (sort) {
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

            if (binding == null || generation != filterGeneration) {
                return;
            }
            binding.getRoot().post(() -> {
                if (binding == null || generation != filterGeneration) {
                    return;
                }
                productAdapter.submitList(filtered);
                updateEmptyState(filtered.isEmpty());
            });
        });
    }

    private void filterProductsByName(String query) {
        applyFiltersAndSort();
    }

    private void updateEmptyState(boolean showEmpty) {
        if (binding == null) {
            return;
        }
        binding.rvProducts.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
        binding.categoryEmptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (voiceInputController != null) {
            voiceInputController.onResume();
        }
    }

    @Override
    public void onPause() {
        if (voiceInputController != null) {
            voiceInputController.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (pendingFilterRunnable != null) {
            filterHandler.removeCallbacks(pendingFilterRunnable);
            pendingFilterRunnable = null;
        }
        if (voiceInputController != null) {
            voiceInputController.release();
            voiceInputController = null;
        }
        super.onDestroyView();
        binding = null;
    }
}
