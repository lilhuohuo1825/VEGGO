package com.veggo.app.presentation.order;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import androidx.lifecycle.LiveData;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.adapter.ProductAdapter;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.repository.ProductRepository;
import com.veggo.app.presentation.category.CategoryAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecurringProductPickerActivity extends BaseActivity {
    public static final String EXTRA_SELECTED_PRODUCTS_JSON = "extra_selected_products_json";

    private final ProductAdapter productAdapter = new ProductAdapter();
    private final CategoryAdapter categoryAdapter = new CategoryAdapter(CategoryAdapter.TYPE_HORIZONTAL);
    private final List<AssetModels.Category> allCategories = new ArrayList<>();
    private final List<Product> currentProducts = new ArrayList<>();
    private final List<RecurringOrderStore.RecurringProductItem> selectedItems = new ArrayList<>();
    private ProductRepository productRepository;
    private LiveData<List<Product>> currentProductSource;
    private TabLayout subcategoryTabs;
    private TextView selectedCountText;
    private TextView doneButton;
    private String selectedCategoryId = "";
    private String selectedSubcategoryId = "";
    private String query = "";
    private boolean buildingTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recurring_product_picker);
        productRepository = AppModule.provideProductRepository(this);
        subcategoryTabs = findViewById(R.id.recurringProductSubcategories);
        findViewById(R.id.recurringProductBackButton).setOnClickListener(v -> finish());
        setupCategories();
        setupProducts();
        setupSearch();
        selectedCountText = findViewById(R.id.recurringProductSelectedCount);
        doneButton = findViewById(R.id.recurringProductDoneButton);
        doneButton.setOnClickListener(v -> finishWithSelectedProducts());
        updateDoneBar();
        loadData();
    }

    private void setupCategories() {
        RecyclerView recyclerView = findViewById(R.id.recurringProductCategories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(categoryAdapter);
        categoryAdapter.setOnCategoryClickListener(category -> {
            selectedCategoryId = category.categoryId;
            selectedSubcategoryId = "";
            categoryAdapter.setSelectedCategoryId(selectedCategoryId);
            setupSubcategoryTabs(category);
            loadProductsForSelection();
        });
    }

    private void setupProducts() {
        RecyclerView grid = findViewById(R.id.recurringProductGrid);
        grid.setLayoutManager(new GridLayoutManager(this, 2));
        grid.setAdapter(productAdapter);
        productAdapter.setOnProductClickListener(null);
        productAdapter.setOnAddProductClickListener(this::showAddProductDialog);
    }

    private void setupSearch() {
        ViewHolderSearch search = new ViewHolderSearch(findViewById(R.id.layoutSearch));
        search.input.setFocusable(true);
        search.input.setHint("Tìm kiếm");
        search.input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s == null ? "" : s.toString().trim().toLowerCase(Locale.US);
                applyProductFilter();
            }
        });
    }

    private void loadData() {
        AppModule.provideCategoryRepository(this).observeCategories().observe(this, categories -> {
            allCategories.clear();
            if (categories != null) {
                allCategories.addAll(categories);
            }
            if (!allCategories.isEmpty() && selectedCategoryId.isEmpty()) {
                selectedCategoryId = allCategories.get(0).categoryId;
            }
            categoryAdapter.setCategories(allCategories);
            categoryAdapter.setSelectedCategoryId(selectedCategoryId);
            AssetModels.Category selected = findSelectedCategory();
            if (selected != null) {
                setupSubcategoryTabs(selected);
            }
            loadProductsForSelection();
        });
        AppModule.provideCategoryRepository(this).refreshCategories();

        productRepository.refreshProducts();
    }

    private void setupSubcategoryTabs(AssetModels.Category category) {
        buildingTabs = true;
        subcategoryTabs.removeAllTabs();

        TabLayout.Tab allTab = subcategoryTabs.newTab().setText("Tất cả").setTag("");
        subcategoryTabs.addTab(allTab, selectedSubcategoryId.isEmpty());
        if (category.subcategories != null) {
            for (AssetModels.Subcategory subcategory : category.subcategories) {
                TabLayout.Tab tab = subcategoryTabs.newTab()
                        .setText(subcategory.subcategoryName)
                        .setTag(subcategory.subcategoryId);
                subcategoryTabs.addTab(tab, subcategory.subcategoryId.equals(selectedSubcategoryId));
            }
        }
        subcategoryTabs.clearOnTabSelectedListeners();
        subcategoryTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (buildingTabs) return;
                Object tag = tab.getTag();
                selectedSubcategoryId = tag == null ? "" : String.valueOf(tag);
                loadProductsForSelection();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        buildingTabs = false;
    }

    private AssetModels.Category findSelectedCategory() {
        for (AssetModels.Category category : allCategories) {
            if (category.categoryId.equals(selectedCategoryId)) {
                return category;
            }
        }
        return allCategories.isEmpty() ? null : allCategories.get(0);
    }

    private void loadProductsForSelection() {
        if (productRepository == null || selectedCategoryId.isEmpty()) {
            return;
        }
        if (currentProductSource != null) {
            currentProductSource.removeObservers(this);
        }
        currentProductSource = selectedSubcategoryId.isEmpty()
                ? productRepository.observeProductsByCategory(selectedCategoryId)
                : productRepository.observeProductsBySubcategory(selectedSubcategoryId);
        currentProductSource.observe(this, products -> {
            currentProducts.clear();
            if (products != null) {
                currentProducts.addAll(products);
            }
            applyProductFilter();
        });
    }

    private void applyProductFilter() {
        List<Product> filtered = new ArrayList<>();
        for (Product product : currentProducts) {
            boolean matchesQuery = query.isEmpty()
                    || safe(product.getName()).toLowerCase(Locale.US).contains(query);
            if (matchesQuery) {
                filtered.add(product);
            }
        }
        productAdapter.setProducts(filtered);
    }

    private void showAddProductDialog(Product product) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_add_to_cart_bottom_sheet, null, false);
        dialog.setContentView(view);

        ImageView thumb = view.findViewById(R.id.ivProductThumb);
        TextView name = view.findViewById(R.id.tvProductNamePopup);
        TextView price = view.findViewById(R.id.tvPricePopup);
        TextView originalPrice = view.findViewById(R.id.tvOriginalPricePopup);
        TextView priceUnit = view.findViewById(R.id.tvPriceUnitPopup);
        TextView quantityText = view.findViewById(R.id.tvQuantityPopup);
        TextView total = view.findViewById(R.id.tvTotalPopup);
        TextView decrease = view.findViewById(R.id.tvDecrease);
        TextView increase = view.findViewById(R.id.tvIncrease);
        TextView confirm = view.findViewById(R.id.btnConfirmAddToCart);
        View close = view.findViewById(R.id.btnClose);
        TextView weightTitle = view.findViewById(R.id.tvWeightTitle);
        ChipGroup weightGroup = view.findViewById(R.id.cgWeight);

        Glide.with(this).load(product.getImageUrl()).placeholder(R.drawable.ic_leaf).into(thumb);
        name.setText(product.getName());
        price.setText(CurrencyFormatter.formatVnd(product.getPrice()));
        bindOriginalPrice(originalPrice, product.getOriginalPrice(), product.getPrice());
        priceUnit.setVisibility(hasWeightOptions(product) ? View.VISIBLE : View.GONE);
        confirm.setText("Thêm");

        final double[] selectedWeight = {resolveDefaultWeight(product)};
        final int[] quantity = {1};
        bindWeightOptions(weightTitle, weightGroup, product, selectedWeight,
                () -> updateDialogTotal(total, product, quantity[0], selectedWeight[0]));
        updateDialogTotal(total, product, quantity[0], selectedWeight[0]);

        decrease.setOnClickListener(v -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                quantityText.setText(String.valueOf(quantity[0]));
                updateDialogTotal(total, product, quantity[0], selectedWeight[0]);
            }
        });
        increase.setOnClickListener(v -> {
            quantity[0]++;
            quantityText.setText(String.valueOf(quantity[0]));
            updateDialogTotal(total, product, quantity[0], selectedWeight[0]);
        });
        close.setOnClickListener(v -> dialog.dismiss());
        confirm.setOnClickListener(v -> {
            addSelectedProduct(product, quantity[0], selectedWeight[0]);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void addSelectedProduct(Product product, int quantity, double selectedWeight) {
        long unitPrice = variantPrice(product.getPrice(), selectedWeight, hasWeightOptions(product));
        double carbonPoints = calculateCarbonPoints(product, quantity, selectedWeight, hasWeightOptions(product));
        RecurringOrderStore.RecurringProductItem item = new RecurringOrderStore.RecurringProductItem();
        item.productId = product.getId();
        item.sku = product.getSku();
        item.name = product.getName();
        item.imageUrl = product.getImageUrl();
        item.unit = hasWeightOptions(product) ? formatWeight(selectedWeight) : safe(product.getWeight());
        item.quantity = quantity;
        item.selectedWeight = selectedWeight;
        item.unitPrice = unitPrice;
        item.carbonPoints = carbonPoints;
        selectedItems.add(item);
        updateDoneBar();
    }

    private void finishWithSelectedProducts() {
        if (selectedItems.isEmpty()) {
            finish();
            return;
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_SELECTED_PRODUCTS_JSON, new Gson().toJson(selectedItems));
        setResult(RESULT_OK, data);
        finish();
    }

    private void updateDoneBar() {
        int count = 0;
        for (RecurringOrderStore.RecurringProductItem item : selectedItems) {
            count += Math.max(1, item.quantity);
        }
        selectedCountText.setText(count == 0 ? "Chưa chọn sản phẩm" : count + " sản phẩm đã chọn");
        doneButton.setEnabled(count > 0);
        doneButton.setAlpha(count > 0 ? 1f : 0.5f);
    }

    private void updateDialogTotal(TextView total, Product product, int quantity, double selectedWeight) {
        long totalValue = variantPrice(product.getPrice(), selectedWeight, hasWeightOptions(product)) * quantity;
        total.setText(CurrencyFormatter.formatVnd(totalValue));
    }

    private void bindOriginalPrice(TextView originalPriceView, long originalPrice, long salePrice) {
        if (originalPriceView == null) return;
        if (originalPrice > salePrice && salePrice > 0) {
            originalPriceView.setVisibility(View.VISIBLE);
            originalPriceView.setText(CurrencyFormatter.formatVnd(originalPrice));
            originalPriceView.setPaintFlags(originalPriceView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            originalPriceView.setVisibility(View.GONE);
        }
    }

    private void bindWeightOptions(TextView title, ChipGroup group, Product product, double[] selectedWeight, Runnable onChanged) {
        group.removeAllViews();
        List<Double> options = product.getWeightOptions();
        if (options == null || options.isEmpty()) {
            title.setVisibility(View.GONE);
            group.setVisibility(View.GONE);
            selectedWeight[0] = resolveDefaultWeight(product);
            return;
        }
        title.setVisibility(View.VISIBLE);
        group.setVisibility(View.VISIBLE);
        for (int i = 0; i < options.size(); i++) {
            double option = options.get(i);
            Chip chip = new Chip(this);
            chip.setMinWidth(dp(100));
            chip.setHeight(dp(44));
            chip.setCheckable(true);
            chip.setText(formatWeight(option));
            chip.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            chip.setChipBackgroundColorResource(R.color.chip_choice_background_color);
            chip.setChipStrokeColorResource(R.color.chip_choice_stroke_color);
            chip.setChipStrokeWidth(1f);
            chip.setChipCornerRadius(dp(12));
            chip.setTextColor(ContextCompat.getColor(this, R.color.chip_choice_text_color));
            chip.setChecked(i == 0);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedWeight[0] = option;
                    onChanged.run();
                }
            });
            group.addView(chip);
            if (i == 0) selectedWeight[0] = option;
        }
    }

    private boolean hasWeightOptions(Product product) {
        return product.getWeightOptions() != null && !product.getWeightOptions().isEmpty();
    }

    private double calculateCarbonPoints(Product product, int quantity, double selectedWeight, boolean hasWeightOptions) {
        if (product == null || quantity <= 0) {
            return 0;
        }
        double basePoint = product.getCarbonSavingPoint();
        if (basePoint <= 0) {
            return 0;
        }
        return hasWeightOptions ? basePoint * selectedWeight * quantity : basePoint * quantity;
    }

    private double resolveDefaultWeight(Product product) {
        if (hasWeightOptions(product)) {
            return product.getWeightOptions().get(0);
        }
        return parseWeightInKilograms(product.getWeight());
    }

    private long variantPrice(long price, double weight, boolean hasWeightOptions) {
        return Math.round(price * (hasWeightOptions ? weight : 1.0));
    }

    private double parseWeightInKilograms(String value) {
        String text = safe(value).toLowerCase(Locale.US);
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+(?:[\\.,]\\d+)?)\\s*(kg|g|gr|gram)")
                .matcher(text);
        if (!matcher.find()) return 1.0;
        try {
            double amount = Double.parseDouble(matcher.group(1).replace(',', '.'));
            return matcher.group(2).startsWith("g") ? amount / 1000.0 : amount;
        } catch (NumberFormatException exception) {
            return 1.0;
        }
    }

    private String formatWeight(double weight) {
        if (weight >= 1) {
            return String.format(Locale.US, weight == Math.round(weight) ? "%.0fkg" : "%.2fkg", weight);
        }
        return String.format(Locale.US, "%.0fg", weight * 1000);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class ViewHolderSearch {
        final EditText input;

        ViewHolderSearch(android.view.View root) {
            input = root.findViewById(R.id.edtSearch);
        }
    }
}
