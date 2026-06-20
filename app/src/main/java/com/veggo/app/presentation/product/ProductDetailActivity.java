package com.veggo.app.presentation.product;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.model.Product;

import java.util.Locale;

public class ProductDetailActivity extends BaseActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private ProductViewModel viewModel;
    
    private ImageView ivProductImage;
    private TextView tvProductName;
    private TextView tvWeight;
    private TextView tvRating;
    private TextView tvSold;
    private TextView tvPrice;
    private TextView tvOriginalPrice;
    private TextView tvDescription;
    private TextView tvShowMoreDesc;
    private View llRelatedRecipes;

    // Rating summary views
    private TextView tvAverageRatingSummary;
    private RatingBar rbAverageRatingSummary;
    private TextView tvTotalRatingsSummary;

    // Consultation
    private com.veggo.app.adapter.ConsultationAdapter consultationAdapter;
    private androidx.recyclerview.widget.RecyclerView rvConsultations;
    // Related products
    private com.veggo.app.adapter.RelatedProductAdapter relatedAdapter;
    private androidx.recyclerview.widget.RecyclerView rvRelatedProducts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        initViews();
        setupViewModel();
        observeViewModel();
        
        // Uncomment dòng dưới nếu bạn muốn nạp lại dữ liệu JSON vào Database (chỉ cần chạy 1 lần)
        // com.veggo.app.core.database.AssetDatabaseSeeder.reseed(this);
        
        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId != null) {
            viewModel.setProductId(productId);
        } else {
            // Sử dụng ID thực tế từ products.json để kiểm tra hiển thị
            // Sản phẩm 1: 68e36b50c0042663fb020b01
            // Sản phẩm 2 (Rating thấp để test biểu đồ): 68e36b55c0042663fb020b12
            viewModel.setProductId("68e36b50c0042663fb020b01");
        }
    }

    private void observeViewModel() {
        viewModel.getProduct().observe(this, product -> {
            if (product != null) {
                bindProductData(product);
            }
        });

        viewModel.getRelatedProducts().observe(this, products -> {
            androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvRelatedProducts);
            if (products != null && !products.isEmpty()) {
                if (rv != null) rv.setVisibility(android.view.View.VISIBLE);
                if (relatedAdapter != null) relatedAdapter.setProducts(products);
            } else {
                if (rv != null) rv.setVisibility(android.view.View.GONE);
            }
        });

        viewModel.getRelatedRecipes().observe(this, recipes -> {
            if (recipes != null && !recipes.isEmpty()) {
                llRelatedRecipes.setVisibility(View.VISIBLE);
                updateRecipes(recipes);
            } else {
                llRelatedRecipes.setVisibility(View.GONE);
            }
        });

        viewModel.getProductReviews().observe(this, reviews -> {
            if (reviews != null) {
                updateReviews(reviews);
                updateRatingSummary(reviews);
            }
        });

        viewModel.getConsultations().observe(this, questions -> {
            if (questions != null) {
                consultationAdapter.setQuestions(questions);
            }
        });

        viewModel.getAddToCartSuccess().observe(this, success -> {
            if (success) {
                android.widget.Toast.makeText(this, R.string.add_to_cart_success, android.widget.Toast.LENGTH_SHORT).show();
                viewModel.resetAddToCartStatus();
            }
        });

        viewModel.getCartError().observe(this, error -> {
            if (error != null) {
                android.widget.Toast.makeText(this, error, android.widget.Toast.LENGTH_SHORT).show();
                viewModel.resetAddToCartStatus();
            }
        });
    }

    private void updateRatingSummary(java.util.List<com.veggo.app.domain.model.Review> reviews) {
        View ratingSummary = findViewById(R.id.llRatingSummary);
        if (reviews == null || reviews.isEmpty()) {
            if (ratingSummary != null) ratingSummary.setVisibility(View.GONE);
            return;
        }
        if (ratingSummary != null) ratingSummary.setVisibility(View.VISIBLE);
        
        int count5 = 0, count4 = 0, count3 = 0, count2 = 0, count1 = 0;
        float total = 0;
        for (com.veggo.app.domain.model.Review r : reviews) {
            total += r.getRating();
            int star = Math.round(r.getRating());
            if (star >= 5) count5++;
            else if (star == 4) count4++;
            else if (star == 3) count3++;
            else if (star == 2) count2++;
            else if (star <= 1) count1++;
        }
        float avg = total / reviews.size();
        Locale vnLocale = new Locale("vi", "VN");
        
        tvAverageRatingSummary.setText(String.format(vnLocale, "%.1f", avg));
        rbAverageRatingSummary.setRating(avg);
        tvTotalRatingsSummary.setText(getString(R.string.reviews_count_format, reviews.size()));
        
        // Cập nhật cả phần rating ở phía trên tiêu đề sản phẩm để đồng bộ với số lượng đánh giá thực tế
        if (tvRating != null) {
            tvRating.setText(String.format(vnLocale, "%.1f (%d)", avg, reviews.size()));
        }

        // Update ProgressBars and Counts
        int totalReviews = reviews.size();
        updateStarRow(R.id.progress5Star, R.id.tvCount5Star, count5, totalReviews);
        updateStarRow(R.id.progress4Star, R.id.tvCount4Star, count4, totalReviews);
        updateStarRow(R.id.progress3Star, R.id.tvCount3Star, count3, totalReviews);
        updateStarRow(R.id.progress2Star, R.id.tvCount2Star, count2, totalReviews);
        updateStarRow(R.id.progress1Star, R.id.tvCount1Star, count1, totalReviews);
    }

    private void updateStarRow(int progressId, int textId, int count, int total) {
        android.widget.ProgressBar pb = findViewById(progressId);
        TextView tv = findViewById(textId);
        if (pb != null) {
            pb.setProgress(total > 0 ? (count * 100 / total) : 0);
        }
        if (tv != null) {
            tv.setText(String.valueOf(count));
        }
    }

    private void loadMockConsultations() {
        // This should ideally come from ViewModel -> Repository -> consultations.json
        // For this demo, we'll initialize the adapter
        if (consultationAdapter == null) {
            consultationAdapter = new com.veggo.app.adapter.ConsultationAdapter();
            rvConsultations.setAdapter(consultationAdapter);
        }
    }

    private void updateReviews(java.util.List<com.veggo.app.domain.model.Review> reviews) {
        androidx.recyclerview.widget.RecyclerView rvReviews = findViewById(R.id.rvReviews);
        com.veggo.app.adapter.ReviewAdapter adapter = (com.veggo.app.adapter.ReviewAdapter) rvReviews.getAdapter();
        if (adapter == null) {
            adapter = new com.veggo.app.adapter.ReviewAdapter();
            rvReviews.setAdapter(adapter);
        }
        // Chỉ hiển thị tối đa 1 review trên trang chi tiết sản phẩm
        if (reviews != null && !reviews.isEmpty()) {
            adapter.setReviews(reviews.subList(0, 1));
            rvReviews.setVisibility(View.VISIBLE);
        } else {
            rvReviews.setVisibility(View.GONE);
        }
    }

    private void updateRecipes(java.util.List<com.veggo.app.domain.model.Recipe> recipes) {
        androidx.recyclerview.widget.RecyclerView rvRecipes = findViewById(R.id.rvRecipes);
        com.veggo.app.adapter.RecipeAdapter adapter = (com.veggo.app.adapter.RecipeAdapter) rvRecipes.getAdapter();
        if (adapter == null) {
            adapter = new com.veggo.app.adapter.RecipeAdapter();
            rvRecipes.setAdapter(adapter);
        }
        adapter.setRecipes(recipes);
    }

    private void showMockData() {
        Product mockProduct = new Product(
                "mock_id_123",
                "Australia beef tenderloin",
                "SKU12345",
                40000,
                50000,
                "https://images.unsplash.com/photo-1558030006-45c25be991f1?q=80&w=1000",
                "450-500g / pack",
                4.5f,
                375,
                1300,
                "In terms of quality look for well-marbled tenderloin, where fat is interspersed within the muscle. This marbling enhances the flavor and juiciness when cooked. A high-quality tenderloin should have a vibrant red color and a firm texture.",
                "Australia",
                "Fresh",
                "Non Fatty"
        );
        // Lưu vào SQLite để test
        viewModel.addProduct(mockProduct);
    }

    private void initViews() {
        ivProductImage = findViewById(R.id.ivProductImage);
        tvProductName = findViewById(R.id.tvProductName);
        tvWeight = findViewById(R.id.tvWeight);
        tvRating = findViewById(R.id.tvRating);
        tvSold = findViewById(R.id.tvSold);
        tvPrice = findViewById(R.id.tvPrice);
        tvOriginalPrice = findViewById(R.id.tvOriginalPrice);
        tvDescription = findViewById(R.id.tvDescription);
        tvShowMoreDesc = findViewById(R.id.tvShowMoreDesc);
        llRelatedRecipes = findViewById(R.id.llRelatedRecipes);
        
        tvAverageRatingSummary = findViewById(R.id.tvAverageRatingSummary);
        rbAverageRatingSummary = findViewById(R.id.rbAverageRatingSummary);
        tvTotalRatingsSummary = findViewById(R.id.tvTotalRatingsSummary);

        rvConsultations = findViewById(R.id.rvConsultations);
        consultationAdapter = new com.veggo.app.adapter.ConsultationAdapter();
        rvConsultations.setAdapter(consultationAdapter);

        // Related products
        rvRelatedProducts = findViewById(R.id.rvRelatedProducts);
        relatedAdapter = new com.veggo.app.adapter.RelatedProductAdapter();
        rvRelatedProducts.setAdapter(relatedAdapter);
        // default add action
        relatedAdapter.setOnAddClickListener(product -> {
            android.widget.Toast.makeText(this, "Đã thêm " + product.getName() + " vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show();
        });

        relatedAdapter.setOnProductClickListener(product -> {
            android.content.Intent intent = new android.content.Intent(this, ProductDetailActivity.class);
            intent.putExtra(EXTRA_PRODUCT_ID, product.getId());
            startActivity(intent);
        });
        
        // Navigation buttons on Image
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            // Handle search
        });
        findViewById(R.id.btnCart).setOnClickListener(v -> {
            // Chuyển về MainActivity và chọn tab Giỏ hàng
            android.content.Intent intent = new android.content.Intent(this, com.veggo.app.MainActivity.class);
            intent.putExtra(com.veggo.app.MainActivity.EXTRA_SELECTED_NAV_ITEM, R.id.nav_cart);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
        
        // "Show more" for description
        tvShowMoreDesc.setOnClickListener(v -> {
            if (tvDescription.getMaxLines() == 3) {
                tvDescription.setMaxLines(Integer.MAX_VALUE);
                tvShowMoreDesc.setText(R.string.show_less);
            } else {
                tvDescription.setMaxLines(3);
                tvShowMoreDesc.setText(R.string.show_more);
            }
        });

        // "Show more" for reviews
        View tvShowMoreReviews = findViewById(R.id.tvShowMoreReviews);
        View llRatingSummary = findViewById(R.id.llRatingSummary);
        
        View.OnClickListener goToReviews = v -> {
            android.content.Intent intent = new android.content.Intent(this, ReviewDetailActivity.class);
            String pId = viewModel.getProductId();
            intent.putExtra(ReviewDetailActivity.EXTRA_PRODUCT_ID, pId);
            startActivity(intent);
        };

        if (tvShowMoreReviews != null) {
            tvShowMoreReviews.setOnClickListener(goToReviews);
        }
        if (llRatingSummary != null) {
            llRatingSummary.setOnClickListener(goToReviews);
        }

        // "Show more" for consultations
        findViewById(R.id.tvShowMoreConsultation).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, ConsultationDetailActivity.class);
            intent.putExtra(ConsultationDetailActivity.EXTRA_PRODUCT_ID, viewModel.getProductId());
            startActivity(intent);
        });

        // Add to cart
        findViewById(R.id.btnAddToCart).setOnClickListener(v -> {
            Product currentProduct = viewModel.getProduct().getValue();
            if (currentProduct != null) {
                showAddToCartPopup(currentProduct);
            }
        });

        // Buy now
        findViewById(R.id.btnBuyNow).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Chuyển đến màn hình thanh toán", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void showAddToCartPopup(Product product) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_add_to_cart_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        ImageView ivThumb = view.findViewById(R.id.ivProductThumb);
        TextView tvName = view.findViewById(R.id.tvProductNamePopup);
        TextView tvPrice = view.findViewById(R.id.tvPricePopup);
        TextView tvQuantity = view.findViewById(R.id.tvQuantityPopup);
        TextView tvTotal = view.findViewById(R.id.tvTotalPopup);
        View btnDecrease = view.findViewById(R.id.tvDecrease);
        View btnIncrease = view.findViewById(R.id.tvIncrease);
        View btnConfirm = view.findViewById(R.id.btnConfirmAddToCart);
        View btnClose = view.findViewById(R.id.btnClose);
        ChipGroup weightGroup = view.findViewById(R.id.cgWeight);

        // Bind data
        Glide.with(this).load(product.getImageUrl()).into(ivThumb);
        tvName.setText(product.getName());
        tvPrice.setText(CurrencyFormatter.formatVnd(product.getPrice()));
        tvTotal.setText(CurrencyFormatter.formatVnd(product.getPrice()));

        final int[] quantity = {1};
        final double[] selectedWeight = {1.0};
        bindWeightOptions(weightGroup, product.getWeightOptions(), selectedWeight);
        
        btnDecrease.setOnClickListener(v -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                tvQuantity.setText(String.valueOf(quantity[0]));
                tvTotal.setText(CurrencyFormatter.formatVnd(product.getPrice() * quantity[0]));
            }
        });

        btnIncrease.setOnClickListener(v -> {
            quantity[0]++;
            tvQuantity.setText(String.valueOf(quantity[0]));
            tvTotal.setText(CurrencyFormatter.formatVnd(product.getPrice() * quantity[0]));
        });

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String sku = product.getSku();
            if (sku == null || sku.trim().isEmpty()) {
                android.widget.Toast.makeText(this, "Không tìm thấy SKU sản phẩm", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            String customerId = resolveCustomerId();
            viewModel.addToCart(customerId, sku, quantity[0], selectedWeight[0]);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(
                AppModule.provideProductRepository(this),
                null,
                AppModule.provideCartRepository(this)
        );
        viewModel = new ViewModelProvider(this, factory).get(ProductViewModel.class);
    }

    private void bindProductData(Product product) {
        Glide.with(this).load(product.getImageUrl()).into(ivProductImage);
        tvProductName.setText(product.getName());
        tvWeight.setText(product.getWeight());
        
        // Rating format: 4,5 (875) - Sử dụng Locale VN để hiển thị dấu phẩy thập phân
        Locale vnLocale = new Locale("vi", "VN");
        tvRating.setText(String.format(vnLocale, "%.1f (%d)", product.getRating(), product.getReviewCount()));
        
        tvSold.setText(getString(R.string.sold_count_format, product.getSoldCount()));
        tvPrice.setText(CurrencyFormatter.formatVnd(product.getPrice()));
        
        if (product.getOriginalPrice() > 0) {
            tvOriginalPrice.setVisibility(View.VISIBLE);
            tvOriginalPrice.setText(CurrencyFormatter.formatVnd(product.getOriginalPrice()));
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tvOriginalPrice.setVisibility(View.GONE);
        }
        
        tvDescription.setText(product.getDescription());
    }

    private String resolveCustomerId() {
        AppPreferences appPreferences = new AppPreferences(this);
        String customerId = appPreferences.getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            customerId = new com.veggo.app.core.preferences.PreferencesManager(this).getUserId();
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            customerId = "CUS900025";
        }
        return customerId;
    }

    private void bindWeightOptions(ChipGroup chipGroup, java.util.List<Double> weightOptions, double[] selectedWeight) {
        if (chipGroup == null) {
            return;
        }

        chipGroup.removeAllViews();

        java.util.List<Double> resolvedOptions = new java.util.ArrayList<>();
        if (weightOptions != null) {
            for (Double option : weightOptions) {
                if (option != null && option > 0) {
                    resolvedOptions.add(option);
                }
            }
        }

        if (resolvedOptions.isEmpty()) {
            resolvedOptions.add(1.0);
        }

        for (int i = 0; i < resolvedOptions.size(); i++) {
            double option = resolvedOptions.get(i);
            Chip chip = new Chip(this);
            chip.setCheckable(true);
            chip.setText(formatWeightOption(option));
            chip.setChipBackgroundColorResource(R.color.chip_choice_background_color);
            chip.setChipStrokeColorResource(R.color.chip_choice_stroke_color);
            chip.setChipStrokeWidth(1f);
            chip.setTextColor(ContextCompat.getColor(this, R.color.chip_choice_text_color));
            chip.setChecked(i == 0);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedWeight[0] = option;
                }
            });
            chipGroup.addView(chip);

            if (i == 0) {
                selectedWeight[0] = option;
            }
        }
    }

    private String formatWeightOption(double weight) {
        if (weight >= 1.0) {
            if (Math.abs(weight - Math.round(weight)) < 0.0001) {
                return String.format(Locale.US, "%.0fkg", weight);
            }
            return String.format(Locale.US, "%skg", trimTrailingZeros(weight));
        }

        int grams = (int) Math.round(weight * 1000d);
        return grams + "g";
    }

    private String trimTrailingZeros(double value) {
        String text = String.format(Locale.US, "%.3f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
