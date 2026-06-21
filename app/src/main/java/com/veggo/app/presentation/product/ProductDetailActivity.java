package com.veggo.app.presentation.product;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.veggo.app.R;
import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.repository.ConsultationRepository;

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
    private View tvDiscountBadge;
    private TextView tvDescription;
    private TextView tvShowMoreDesc;
    private View llRelatedRecipes;
    private View llSustainability;
    private TextView tvEmissionFactor;
    private TextView tvCarbonSavingPoint;

    // Rating summary views
    private TextView tvAverageRatingSummary;
    private RatingBar rbAverageRatingSummary;
    private TextView tvTotalRatingsSummary;

    // Consultation
    private com.veggo.app.adapter.ConsultationAdapter consultationAdapter;
    private androidx.recyclerview.widget.RecyclerView rvConsultations;
    private android.widget.EditText edtQuestion;
    private android.widget.ImageView btnSendQuestion;
    private android.widget.ProgressBar progressSendQuestion;
    // Related products
    private com.veggo.app.adapter.RelatedProductAdapter relatedAdapter;
    private androidx.recyclerview.widget.RecyclerView rvRelatedProducts;
    private com.veggo.app.adapter.RecipeAdapter recipeAdapter;

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
        }
    }

    private void observeViewModel() {
        viewModel.getProduct().observe(this, product -> {
            if (product != null) {
                bindProductData(product);
                viewModel.triggerReviewFetch(product);
                viewModel.triggerConsultationFetch(product);
                viewModel.triggerRelatedRecipesFetch(product);
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

        viewModel.isSubmittingQuestion().observe(this, this::setConsultationSubmittingUi);
    }

    private void setConsultationSubmittingUi(boolean submitting) {
        if (btnSendQuestion != null) {
            btnSendQuestion.setEnabled(!submitting);
            btnSendQuestion.setAlpha(submitting ? 0.4f : 1f);
            btnSendQuestion.setVisibility(submitting ? View.INVISIBLE : View.VISIBLE);
        }
        if (edtQuestion != null) {
            edtQuestion.setEnabled(!submitting);
        }
        if (progressSendQuestion != null) {
            progressSendQuestion.setVisibility(submitting ? View.VISIBLE : View.GONE);
        }
    }

    private void submitConsultationQuestion() {
        if (edtQuestion == null) return;

        String questionText = edtQuestion.getText().toString().trim();
        if (questionText.isEmpty()) {
            android.widget.Toast.makeText(this, R.string.consultation_submit_empty,
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        Product product = viewModel.getProduct().getValue();
        if (product == null || product.getSku() == null || product.getSku().isEmpty()) {
            android.widget.Toast.makeText(this, R.string.consultation_submit_no_sku,
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.submitQuestion(product.getSku(), questionText, null, product.getName(),
                new ConsultationRepository.Callback<java.util.List<com.veggo.app.domain.model.Consultation>>() {
                    @Override
                    public void onSuccess(java.util.List<com.veggo.app.domain.model.Consultation> result) {
                        runOnUiThread(() -> {
                            android.widget.Toast.makeText(ProductDetailActivity.this,
                                    R.string.consultation_submit_success,
                                    android.widget.Toast.LENGTH_SHORT).show();
                            edtQuestion.setText("");
                        });
                    }

                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> showConsultationSubmitError(t, questionText));
                    }
                });
    }

    private void showConsultationSubmitError(Throwable t, String questionText) {
        String message;
        if (t instanceof ApiHttpException) {
            int code = ((ApiHttpException) t).getCode();
            if (code == 404) {
                message = getString(R.string.consultation_submit_not_found);
            } else if (code == 400) {
                message = getString(R.string.consultation_submit_empty);
            } else {
                message = getString(R.string.consultation_submit_network_error);
            }
        } else if (t instanceof IllegalArgumentException) {
            message = getString(R.string.consultation_submit_empty);
        } else {
            message = getString(R.string.consultation_submit_network_error);
        }

        View anchor = edtQuestion != null ? (View) edtQuestion.getParent() : findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(anchor, message, Snackbar.LENGTH_LONG);
        if (t instanceof ApiHttpException && ((ApiHttpException) t).getCode() >= 500
                || !(t instanceof ApiHttpException)) {
            snackbar.setAction(R.string.consultation_submit_retry, v -> {
                edtQuestion.setText(questionText);
                submitConsultationQuestion();
            });
        }
        snackbar.show();
    }

    private void updateRatingSummary(java.util.List<com.veggo.app.domain.model.Review> reviews) {
        View ratingSummary = findViewById(R.id.llRatingSummary);
        if (reviews == null || reviews.isEmpty()) {
            if (ratingSummary != null) ratingSummary.setVisibility(View.GONE);
            // Even if reviews are empty, we might want to show 0.0 (0) if the product exists
            Product currentProduct = viewModel.getProduct().getValue();
            if (currentProduct != null && tvRating != null) {
                tvRating.setText(String.format(new Locale("vi", "VN"), "%.1f (0)", 0.0));
            }
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
        
        int realTotalReviews = reviews.size();
        float calculatedAvg = total / realTotalReviews;
        
        Locale vnLocale = new Locale("vi", "VN");
        
        tvAverageRatingSummary.setText(String.format(vnLocale, "%.1f", calculatedAvg));
        rbAverageRatingSummary.setRating(calculatedAvg);
        tvTotalRatingsSummary.setText(getString(R.string.reviews_count_format, realTotalReviews));
        
        // Cập nhật cả phần rating ở phía trên tiêu đề sản phẩm để đồng bộ với số lượng đánh giá thực tế
        if (tvRating != null) {
            tvRating.setText(String.format(vnLocale, "%.1f (%d)", calculatedAvg, realTotalReviews));
        }

        // Update ProgressBars and Counts
        updateStarRow(R.id.progress5Star, R.id.tvCount5Star, count5, realTotalReviews);
        updateStarRow(R.id.progress4Star, R.id.tvCount4Star, count4, realTotalReviews);
        updateStarRow(R.id.progress3Star, R.id.tvCount3Star, count3, realTotalReviews);
        updateStarRow(R.id.progress2Star, R.id.tvCount2Star, count2, realTotalReviews);
        updateStarRow(R.id.progress1Star, R.id.tvCount1Star, count1, realTotalReviews);
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
        if (recipeAdapter == null) {
            recipeAdapter = new com.veggo.app.adapter.RecipeAdapter();
            recipeAdapter.setOnRecipeClickListener(recipe -> {
                android.content.Intent intent = new android.content.Intent(
                        this, com.veggo.app.presentation.community.CommunityRecipeDetailActivity.class);
                intent.putExtra(
                        com.veggo.app.presentation.community.CommunityRecipeDetailActivity.EXTRA_INSTRUCTION_ID,
                        recipe.getId());
                startActivity(intent);
            });
            rvRecipes.setAdapter(recipeAdapter);
        }
        recipeAdapter.setRecipes(recipes);
    }



    private void initViews() {
        ivProductImage = findViewById(R.id.ivProductImage);
        tvProductName = findViewById(R.id.tvProductName);
        tvWeight = findViewById(R.id.tvWeight);
        tvRating = findViewById(R.id.tvRating);
        tvSold = findViewById(R.id.tvSold);
        tvPrice = findViewById(R.id.tvPrice);
        tvOriginalPrice = findViewById(R.id.tvOriginalPrice);
        tvDiscountBadge = findViewById(R.id.tvDiscountBadge);
        tvDescription = findViewById(R.id.tvDescription);
        tvShowMoreDesc = findViewById(R.id.tvShowMoreDesc);
        llRelatedRecipes = findViewById(R.id.llRelatedRecipes);

        tvAverageRatingSummary = findViewById(R.id.tvAverageRatingSummary);
        rbAverageRatingSummary = findViewById(R.id.rbAverageRatingSummary);
        tvTotalRatingsSummary = findViewById(R.id.tvTotalRatingsSummary);

        rvConsultations = findViewById(R.id.rvConsultations);
        consultationAdapter = new com.veggo.app.adapter.ConsultationAdapter();
        rvConsultations.setAdapter(consultationAdapter);

        edtQuestion = findViewById(R.id.edtQuestion);
        btnSendQuestion = findViewById(R.id.btnSendQuestion);
        progressSendQuestion = findViewById(R.id.progressSendQuestion);
        if (btnSendQuestion != null) {
            btnSendQuestion.setOnClickListener(v -> submitConsultationQuestion());
        }

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
            startActivity(new android.content.Intent(this, com.veggo.app.presentation.search.SearchActivity.class));
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

        // Bind data
        Glide.with(this).load(product.getImageUrl()).into(ivThumb);
        tvName.setText(product.getName());
        tvPrice.setText(CurrencyFormatter.formatVnd(product.getPrice()));
        tvTotal.setText(CurrencyFormatter.formatVnd(product.getPrice()));

        final int[] quantity = {1};
        
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
            // Logic thêm vào giỏ hàng thực tế
            android.widget.Toast.makeText(this, "Đã thêm " + quantity[0] + " " + product.getName() + " vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(
                AppModule.provideProductRepository(this),
                AppModule.provideReviewRepository(this),
                AppModule.provideConsultationRepository(this),
                AppModule.provideRecipeRepository()
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
        
        if (product.hasActiveDiscount()) {
            tvOriginalPrice.setVisibility(View.VISIBLE);
            tvOriginalPrice.setText(CurrencyFormatter.formatVnd(product.getOriginalPrice()));
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            if (tvDiscountBadge != null) {
                tvDiscountBadge.setVisibility(View.VISIBLE);
                if (tvDiscountBadge instanceof TextView) {
                    ((TextView) tvDiscountBadge).setText(calculateDiscountPercentage(product));
                }
            }
        } else {
            tvOriginalPrice.setVisibility(View.GONE);
            if (tvDiscountBadge != null) tvDiscountBadge.setVisibility(View.GONE);
        }
        
        tvDescription.setText(product.getDescription());
    }

    private String calculateDiscountPercentage(Product product) {
        if (product.getOriginalPrice() <= 0) return "";
        long discount = product.getOriginalPrice() - product.getPrice();
        int percentage = (int) ((discount * 100.0f) / product.getOriginalPrice());
        return "-" + percentage + "%";
    }
}
