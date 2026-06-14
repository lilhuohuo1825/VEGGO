package com.veggo.app.presentation.product;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
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
        
        tvAverageRatingSummary.setText(String.format(Locale.getDefault(), "%.1f", avg));
        rbAverageRatingSummary.setRating(avg);
        tvTotalRatingsSummary.setText(getString(R.string.reviews_count_format, reviews.size()));

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
        
        // Navigation buttons on Image
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            // Handle search
        });
        findViewById(R.id.btnCart).setOnClickListener(v -> {
            // Navigate to Cart
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
        if (tvShowMoreReviews != null) {
            tvShowMoreReviews.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(this, ReviewDetailActivity.class);
                String pId = viewModel.getProductId();
                intent.putExtra(ReviewDetailActivity.EXTRA_PRODUCT_ID, pId);
                startActivity(intent);
            });
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
                // Add to cart logic here
                android.widget.Toast.makeText(this, "Đã thêm " + currentProduct.getName() + " vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Buy now
        findViewById(R.id.btnBuyNow).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Chuyển đến màn hình thanh toán", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(AppModule.provideProductRepository(this));
        viewModel = new ViewModelProvider(this, factory).get(ProductViewModel.class);
    }

    private void bindProductData(Product product) {
        Glide.with(this).load(product.getImageUrl()).into(ivProductImage);
        tvProductName.setText(product.getName());
        tvWeight.setText(product.getWeight());
        
        // Rating format: 4,5 (875)
        tvRating.setText(String.format(Locale.getDefault(), "%.1f (%d)", product.getRating(), product.getReviewCount()));
        
        tvSold.setText(getString(R.string.sold_count_format, product.getSoldCount()));
        tvPrice.setText(getString(R.string.price_format, (double) product.getPrice()));
        
        if (product.getOriginalPrice() > 0) {
            tvOriginalPrice.setVisibility(View.VISIBLE);
            tvOriginalPrice.setText(getString(R.string.price_format, (double) product.getOriginalPrice()));
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tvOriginalPrice.setVisibility(View.GONE);
        }
        
        tvDescription.setText(product.getDescription());
    }
}
