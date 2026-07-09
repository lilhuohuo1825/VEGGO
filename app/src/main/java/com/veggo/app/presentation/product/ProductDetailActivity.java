package com.veggo.app.presentation.product;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.R;
import com.veggo.app.core.favorite.FavoriteStore;
import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.core.utils.CartCountUtils;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.core.utils.ProductImageUtils;
import com.veggo.app.core.ui.BadgeUiHelper;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.repository.ConsultationRepository;
import com.veggo.app.presentation.auth.LoginActivity;
import com.veggo.app.presentation.checkout.CheckoutActivity;
import com.veggo.app.presentation.checkout.CheckoutGuestActivity;
import com.veggo.app.presentation.checkout.PendingCheckoutStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends BaseActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    public static final String EXTRA_OPEN_ADD_TO_CART = "extra_open_add_to_cart";
    public static final String EXTRA_FLASH_SALE_PRICE = "extra_flash_sale_price";
    public static final String EXTRA_FLASH_SALE_ORIGINAL_PRICE = "extra_flash_sale_original_price";
    public static final String EXTRA_FLASH_SALE_DISCOUNT_LABEL = "extra_flash_sale_discount_label";
    public static final String EXTRA_SCROLL_TO_CONSULTATION = "extra_scroll_to_consultation";

    private ProductViewModel viewModel;
    private Product currentProduct;
    private long flashSalePrice;
    private long flashSaleOriginalPrice;
    private String flashSaleDiscountLabel;
    
    private ImageView ivProductImage;
    private TextView tvProductName;
    private TextView tvWeight;
    private TextView tvRating;
    private TextView tvSold;
    private TextView tvPrice;
    private TextView tvOriginalPrice;
    private View tvDiscountBadge;
    private TableLayout tblDescription;
    private TextView tvShowMoreDesc;
    private View llRelatedRecipes;
    private View llSustainability;
    private TextView tvEmissionFactor;
    private TextView tvCarbonSavingPoint;
    private View btnCartContainer;
    private TextView tvProductCartBadge;
    private ImageView ivFavorite;
    private FavoriteStore favoriteStore;

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
    private final List<TableRow> descriptionRows = new ArrayList<>();
    private boolean descriptionExpanded = false;
    private boolean openAddToCartPending;
    private boolean scrollToConsultationPending;
    private int consultationScrollAttempts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        initViews();
        setupViewModel();
        favoriteStore = new FavoriteStore(this);
        observeViewModel();
        refreshCartBadge(false);
        openAddToCartPending = getIntent().getBooleanExtra(EXTRA_OPEN_ADD_TO_CART, false);
        scrollToConsultationPending = getIntent().getBooleanExtra(EXTRA_SCROLL_TO_CONSULTATION, false);
        consultationScrollAttempts = 0;
        flashSalePrice = getIntent().getLongExtra(EXTRA_FLASH_SALE_PRICE, 0);
        flashSaleOriginalPrice = getIntent().getLongExtra(EXTRA_FLASH_SALE_ORIGINAL_PRICE, 0);
        flashSaleDiscountLabel = getIntent().getStringExtra(EXTRA_FLASH_SALE_DISCOUNT_LABEL);
        
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
                this.currentProduct = product;
                bindProductData(product);
                viewModel.triggerReviewFetch(product);
                viewModel.triggerConsultationFetch(product);
                viewModel.triggerRelatedRecipesFetch(product);
                if (openAddToCartPending) {
                    openAddToCartPending = false;
                    showAddToCartPopup(product, false);
                }
                maybeScrollToConsultationSection();
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
                maybeScrollToConsultationSection();
            }
        });

        viewModel.getConsultations().observe(this, questions -> {
            AppPreferences prefs = new AppPreferences(this);
            consultationAdapter.setCurrentCustomerId(prefs.getCustomerId());
            java.util.List<com.veggo.app.domain.model.Consultation> display =
                    ConsultationUiHelper.filterForUser(questions, prefs.getCustomerId());
            java.util.List<com.veggo.app.domain.model.Consultation> preview =
                    ConsultationUiHelper.limit(display, ConsultationUiHelper.PREVIEW_LIMIT);
            consultationAdapter.setQuestions(preview);

            android.widget.TextView tvEmpty = findViewById(R.id.tvConsultationEmpty);
            android.widget.TextView tvShowMore = findViewById(R.id.tvShowMoreConsultation);
            if (tvEmpty != null) {
                tvEmpty.setVisibility(display.isEmpty() ? View.VISIBLE : View.GONE);
            }
            if (rvConsultations != null) {
                rvConsultations.setVisibility(display.isEmpty() ? View.GONE : View.VISIBLE);
            }
            if (tvShowMore != null) {
                tvShowMore.setVisibility(display.isEmpty() ? View.GONE : View.VISIBLE);
            }
            if (scrollToConsultationPending) {
                View section = findViewById(R.id.consultationSectionContainer);
                if (section != null) {
                    section.getViewTreeObserver().addOnGlobalLayoutListener(
                            new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    section.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    maybeScrollToConsultationSection();
                                }
                            });
                } else {
                    maybeScrollToConsultationSection();
                }
            }
        });

        viewModel.isSubmittingQuestion().observe(this, this::setConsultationSubmittingUi);
        viewModel.getAddToCartSuccess().observe(this, success -> {
            if (success) {
                android.widget.Toast.makeText(this, R.string.add_to_cart_success, android.widget.Toast.LENGTH_SHORT).show();
                refreshCartBadge(true);
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

        AppPreferences appPreferences = new AppPreferences(this);
        if (!appPreferences.isLoggedIn()
                || appPreferences.getCustomerId() == null
                || appPreferences.getCustomerId().isEmpty()) {
            android.widget.Toast.makeText(this, R.string.consultation_login_required,
                    android.widget.Toast.LENGTH_SHORT).show();
            startActivity(new android.content.Intent(this, LoginActivity.class));
            return;
        }

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

        viewModel.submitQuestion(
                product.getSku(),
                questionText,
                appPreferences.getCustomerId(),
                appPreferences.getFullName(),
                product.getName(),
                appPreferences.getAvatarUrl(),
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

    private void setupConsultationAdapterListener() {
        consultationAdapter.setActionListener(new com.veggo.app.adapter.ConsultationAdapter.ActionListener() {
            @Override
            public void onToggleLike(com.veggo.app.domain.model.Consultation question) {
                toggleConsultationLike(question);
            }

            @Override
            public void onSubmitReply(com.veggo.app.domain.model.Consultation question, String content) {
                submitConsultationReply(question, content);
            }

            @Override
            public void onLoginRequired() {
                android.widget.Toast.makeText(ProductDetailActivity.this,
                        R.string.consultation_login_required,
                        android.widget.Toast.LENGTH_SHORT).show();
                startActivity(new android.content.Intent(ProductDetailActivity.this, LoginActivity.class));
            }
        });
    }

    private void toggleConsultationLike(com.veggo.app.domain.model.Consultation question) {
        if (question == null || question.getId() == null || question.getId().isEmpty()) {
            return;
        }
        Product product = viewModel.getProduct().getValue();
        if (product == null || product.getSku() == null || product.getSku().isEmpty()) {
            return;
        }
        AppPreferences prefs = new AppPreferences(this);
        final boolean wasLiked = question.isLikedBy(prefs.getCustomerId());
        viewModel.toggleQuestionLike(
                product.getSku(),
                question.getId(),
                prefs.getCustomerId(),
                prefs.getFullName(),
                new ConsultationRepository.Callback<java.util.List<com.veggo.app.domain.model.Consultation>>() {
                    @Override
                    public void onSuccess(java.util.List<com.veggo.app.domain.model.Consultation> result) {
                        boolean likedNow = !wasLiked;
                        if (result != null) {
                            for (com.veggo.app.domain.model.Consultation item : result) {
                                if (item != null && question.getId().equals(item.getId())) {
                                    likedNow = item.isLikedBy(prefs.getCustomerId());
                                    break;
                                }
                            }
                        }
                        final boolean finalLikedNow = likedNow;
                        runOnUiThread(() -> android.widget.Toast.makeText(
                                ProductDetailActivity.this,
                                finalLikedNow ? R.string.consultation_like_success : R.string.consultation_unlike_success,
                                android.widget.Toast.LENGTH_SHORT
                        ).show());
                    }

                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> android.widget.Toast.makeText(
                                ProductDetailActivity.this,
                                R.string.consultation_submit_network_error,
                                android.widget.Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void submitConsultationReply(com.veggo.app.domain.model.Consultation question, String content) {
        if (question == null || question.getId() == null || question.getId().isEmpty()) {
            return;
        }
        Product product = viewModel.getProduct().getValue();
        if (product == null || product.getSku() == null || product.getSku().isEmpty()) {
            return;
        }
        AppPreferences prefs = new AppPreferences(this);
        viewModel.submitReply(
                product.getSku(),
                question.getId(),
                content,
                prefs.getCustomerId(),
                prefs.getFullName(),
                prefs.getAvatarUrl(),
                new ConsultationRepository.Callback<java.util.List<com.veggo.app.domain.model.Consultation>>() {
                    @Override
                    public void onSuccess(java.util.List<com.veggo.app.domain.model.Consultation> result) {
                        runOnUiThread(() -> android.widget.Toast.makeText(
                                ProductDetailActivity.this,
                                R.string.consultation_reply_success,
                                android.widget.Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(Throwable t) {
                        runOnUiThread(() -> {
                            String message = getString(R.string.consultation_submit_network_error);
                            if (t instanceof com.veggo.app.core.network.ApiHttpException
                                    && ((com.veggo.app.core.network.ApiHttpException) t).getCode() == 400) {
                                message = getString(R.string.consultation_reply_self_error);
                            }
                            android.widget.Toast.makeText(
                                    ProductDetailActivity.this,
                                    message,
                                    android.widget.Toast.LENGTH_SHORT).show();
                        });
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
            } else if (code == 401) {
                message = getString(R.string.consultation_login_required);
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
        if ((t instanceof ApiHttpException && ((ApiHttpException) t).getCode() >= 500)
                || !(t instanceof ApiHttpException)) {
            snackbar.setAction(R.string.consultation_submit_retry, v -> {
                edtQuestion.setText(questionText);
                submitConsultationQuestion();
            });
        }
        snackbar.show();
    }

    private void maybeScrollToConsultationSection() {
        if (!scrollToConsultationPending) {
            return;
        }
        View section = findViewById(R.id.consultationSectionContainer);
        androidx.core.widget.NestedScrollView scrollView = findViewById(R.id.productDetailScroll);
        if (section == null || scrollView == null) {
            return;
        }

        section.post(() -> {
            if (!scrollToConsultationPending) {
                return;
            }

            if (section.getHeight() <= 0 && consultationScrollAttempts < 8) {
                consultationScrollAttempts++;
                section.postDelayed(this::maybeScrollToConsultationSection, 150);
                return;
            }

            int scrollY = computeScrollOffset(section, scrollView);
            scrollToConsultationPending = false;
            consultationScrollAttempts = 0;
            int topPadding = (int) (12 * getResources().getDisplayMetrics().density);
            scrollView.smoothScrollTo(0, Math.max(0, scrollY - topPadding));
        });
    }

    private int computeScrollOffset(View target, androidx.core.widget.NestedScrollView scrollView) {
        int offset = 0;
        View current = target;
        while (current != null && current != scrollView) {
            offset += current.getTop();
            if (!(current.getParent() instanceof View)) {
                break;
            }
            current = (View) current.getParent();
        }
        return offset;
    }

    private void updateRatingSummary(java.util.List<com.veggo.app.domain.model.Review> reviews) {
        View ratingSummary = findViewById(R.id.llRatingSummary);
        if (reviews == null || reviews.isEmpty()) {
            Product product = viewModel != null ? viewModel.getProduct().getValue() : null;
            float rating = product != null ? product.getRating() : 0f;
            int reviewCount = product != null ? product.getReviewCount() : 0;
            Locale vnLocale = new Locale("vi", "VN");
            if (ratingSummary != null) {
                ratingSummary.setVisibility(reviewCount > 0 ? View.VISIBLE : View.GONE);
            }
            if (tvRating != null) {
                tvRating.setText(String.format(vnLocale, "%.1f (%d)", rating, reviewCount));
            }
            if (tvAverageRatingSummary != null) {
                tvAverageRatingSummary.setText(String.format(vnLocale, "%.1f", rating));
            }
            if (rbAverageRatingSummary != null) {
                rbAverageRatingSummary.setRating(rating);
            }
            if (tvTotalRatingsSummary != null) {
                tvTotalRatingsSummary.setText(getString(R.string.reviews_count_format, reviewCount));
            }
            updateStarRow(R.id.progress5Star, R.id.tvCount5Star, 0, 0);
            updateStarRow(R.id.progress4Star, R.id.tvCount4Star, 0, 0);
            updateStarRow(R.id.progress3Star, R.id.tvCount3Star, 0, 0);
            updateStarRow(R.id.progress2Star, R.id.tvCount2Star, 0, 0);
            updateStarRow(R.id.progress1Star, R.id.tvCount1Star, 0, 0);
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
            setupReviewAdapterListener(adapter);
            rvReviews.setAdapter(adapter);
        }
        AppPreferences prefs = new AppPreferences(this);
        adapter.setCurrentCustomerId(prefs.getCustomerId());
        if (reviews != null && !reviews.isEmpty()) {
            int previewCount = Math.min(2, reviews.size());
            adapter.setReviews(new java.util.ArrayList<>(reviews.subList(0, previewCount)));
            rvReviews.setVisibility(View.VISIBLE);
        } else {
            adapter.setReviews(new java.util.ArrayList<>());
            rvReviews.setVisibility(View.GONE);
        }
    }

    private void setupReviewAdapterListener(com.veggo.app.adapter.ReviewAdapter adapter) {
        adapter.setActionListener(new com.veggo.app.adapter.ReviewAdapter.ActionListener() {
            @Override
            public void onToggleLike(com.veggo.app.domain.model.Review review) {
                toggleReviewLike(review);
            }

            @Override
            public void onLoginRequired() {
                android.widget.Toast.makeText(ProductDetailActivity.this,
                        R.string.consultation_like_login_required,
                        android.widget.Toast.LENGTH_SHORT).show();
                startActivity(new android.content.Intent(ProductDetailActivity.this, LoginActivity.class));
            }
        });
    }

    private void toggleReviewLike(com.veggo.app.domain.model.Review review) {
        if (review == null || review.getId() == null || review.getId().isEmpty()) {
            return;
        }
        Product product = viewModel.getProduct().getValue();
        if (product == null || product.getSku() == null || product.getSku().isEmpty()) {
            return;
        }
        AppPreferences prefs = new AppPreferences(this);
        viewModel.toggleReviewLike(product.getSku(), review.getId(), prefs.getCustomerId());
    }

    private void updateRecipes(java.util.List<com.veggo.app.domain.model.Recipe> recipes) {
        androidx.recyclerview.widget.RecyclerView rvRecipes = findViewById(R.id.rvRecipes);
        if (recipeAdapter == null) {
            recipeAdapter = new com.veggo.app.adapter.RecipeAdapter();
            recipeAdapter.setOnRecipeClickListener(recipe -> {
                android.content.Intent intent = new android.content.Intent(
                        this, com.veggo.app.presentation.community.InstructionRecipeDetailActivity.class);
                intent.putExtra(
                        com.veggo.app.presentation.community.InstructionRecipeDetailActivity.EXTRA_INSTRUCTION_ID,
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
        tblDescription = findViewById(R.id.tblDescription);
        tvShowMoreDesc = findViewById(R.id.tvShowMoreDesc);
        llRelatedRecipes = findViewById(R.id.llRelatedRecipes);

        tvAverageRatingSummary = findViewById(R.id.tvAverageRatingSummary);
        rbAverageRatingSummary = findViewById(R.id.rbAverageRatingSummary);
        tvTotalRatingsSummary = findViewById(R.id.tvTotalRatingsSummary);

        rvConsultations = findViewById(R.id.rvConsultations);
        consultationAdapter = new com.veggo.app.adapter.ConsultationAdapter();
        setupConsultationAdapterListener();
        rvConsultations.setAdapter(consultationAdapter);

        edtQuestion = findViewById(R.id.edtQuestion);
        btnSendQuestion = findViewById(R.id.btnSendQuestion);
        progressSendQuestion = findViewById(R.id.progressSendQuestion);
        btnCartContainer = findViewById(R.id.btnCartContainer);
        tvProductCartBadge = findViewById(R.id.tvProductCartBadge);
        if (btnSendQuestion != null) {
            btnSendQuestion.setOnClickListener(v -> submitConsultationQuestion());
        }

        // Related products
        rvRelatedProducts = findViewById(R.id.rvRelatedProducts);
        relatedAdapter = new com.veggo.app.adapter.RelatedProductAdapter();
        rvRelatedProducts.setAdapter(relatedAdapter);
        // default add action
        relatedAdapter.setOnAddClickListener(product -> {
            android.content.Intent intent = new android.content.Intent(this, ProductDetailActivity.class);
            intent.putExtra(EXTRA_PRODUCT_ID, product.getId());
            intent.putExtra(EXTRA_OPEN_ADD_TO_CART, true);
            startActivity(intent);
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
            // Chuyển về MainActivity và chọn tab Giỏ hàng
            android.content.Intent intent = new android.content.Intent(this, com.veggo.app.MainActivity.class);
            intent.putExtra(com.veggo.app.MainActivity.EXTRA_SELECTED_NAV_ITEM, R.id.nav_cart);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        if (ivProductImage != null) {
            ivProductImage.setOnClickListener(v -> showProductImagePreview());
        }

        View shareButton = findViewById(R.id.ivShare);
        if (shareButton != null) {
            shareButton.setOnClickListener(v -> shareCurrentProduct());
        }
        ivFavorite = findViewById(R.id.ivFavorite);
        if (ivFavorite != null) {
            ivFavorite.setOnClickListener(v -> toggleCurrentProductFavorite());
        }
        
        // "Show more" for description
        tvShowMoreDesc.setOnClickListener(v -> {
            descriptionExpanded = !descriptionExpanded;
            applyDescriptionExpansion();
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
            if (this.currentProduct != null) {
                showAddToCartPopup(this.currentProduct, false);
            } else {
                android.widget.Toast.makeText(this, "Đang tải thông tin sản phẩm...", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        // Buy now
        findViewById(R.id.btnBuyNow).setOnClickListener(v -> {
            if (this.currentProduct != null) {
                showAddToCartPopup(this.currentProduct, true);
            } else {
                android.widget.Toast.makeText(this, "Đang tải thông tin sản phẩm...", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shareCurrentProduct() {
        Product product = viewModel != null ? viewModel.getProduct().getValue() : null;
        if (product == null) {
            return;
        }
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, product.getName());
        startActivity(android.content.Intent.createChooser(shareIntent, "Chia sẻ sản phẩm"));
    }

    private void toggleCurrentProductFavorite() {
        Product product = viewModel != null ? viewModel.getProduct().getValue() : null;
        if (product == null || product.getId() == null) {
            return;
        }
        boolean selected = favoriteStore.toggle(new FavoriteStore.FavoriteItem(
                FavoriteStore.TYPE_PRODUCT,
                product.getId(),
                product.getName(),
                favoriteProductSubtitle(product),
                product.getImageUrl()
        ));
        renderProductFavorite(selected);
        android.widget.Toast.makeText(this,
                selected ? "Đã thêm vào yêu thích" : "Đã xoá khỏi yêu thích",
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private void renderProductFavorite(boolean selected) {
        if (ivFavorite == null) {
            return;
        }
        ivFavorite.setImageResource(selected
                ? R.drawable.ic_profile_menu_heart_filled
                : R.drawable.ic_heart_outline_green);
    }

    private String favoriteProductSubtitle(Product product) {
        String subtitle = CurrencyFormatter.formatVnd(product.getPrice());
        double carbonPoint = product.getCarbonSavingPoint();
        if (carbonPoint > 0) {
            subtitle += " • " + formatCarbonPoint(carbonPoint) + " điểm carbon";
        }
        return subtitle;
    }

    private String formatCarbonPoint(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private void showAddToCartPopup(Product product, boolean buyNow) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.layout_add_to_cart_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        ImageView ivThumb = view.findViewById(R.id.ivProductThumb);
        TextView tvName = view.findViewById(R.id.tvProductNamePopup);
        TextView tvPrice = view.findViewById(R.id.tvPricePopup);
        TextView tvOriginalPrice = view.findViewById(R.id.tvOriginalPricePopup);
        TextView tvPriceUnit = view.findViewById(R.id.tvPriceUnitPopup);
        TextView tvQuantity = view.findViewById(R.id.tvQuantityPopup);
        TextView tvTotal = view.findViewById(R.id.tvTotalPopup);
        View btnDecrease = view.findViewById(R.id.tvDecrease);
        View btnIncrease = view.findViewById(R.id.tvIncrease);
        View btnConfirm = view.findViewById(R.id.btnConfirmAddToCart);
        View btnClose = view.findViewById(R.id.btnClose);
        TextView tvWeightTitle = view.findViewById(R.id.tvWeightTitle);
        ChipGroup weightGroup = view.findViewById(R.id.cgWeight);
        TextView btnConfirmText = view.findViewById(R.id.btnConfirmAddToCart);

        // Bind data
        loadProductImage(ivThumb, product.getImageUrl());
        tvName.setText(product.getName());
        long displayPrice = effectivePrice(product);
        long displayOriginalPrice = effectiveOriginalPrice(product);
        tvPrice.setText(CurrencyFormatter.formatVnd(displayPrice));
        bindPopupOriginalPrice(tvOriginalPrice, displayOriginalPrice, displayPrice);
        if (btnConfirmText != null) {
            btnConfirmText.setText(buyNow ? "Mua ngay" : "Thêm vào giỏ");
        }

        final int[] quantity = {1};
        final double[] selectedWeight = {1.0};
        boolean hasWeightOptions = hasWeightOptions(product);
        if (tvPriceUnit != null) {
            tvPriceUnit.setVisibility(hasWeightOptions ? View.VISIBLE : View.GONE);
            tvPriceUnit.setText("/ kg");
        }
        bindWeightOptions(tvWeightTitle, weightGroup, product.getWeightOptions(), selectedWeight, () ->
                updateAddToCartTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions));
        updateAddToCartTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions);
        
        btnDecrease.setOnClickListener(v -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                tvQuantity.setText(String.valueOf(quantity[0]));
                updateAddToCartTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions);
            }
        });

        btnIncrease.setOnClickListener(v -> {
            quantity[0]++;
            tvQuantity.setText(String.valueOf(quantity[0]));
            updateAddToCartTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions);
        });

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String sku = product.getSku();
            if (sku == null || sku.trim().isEmpty()) {
                android.widget.Toast.makeText(this, "Không tìm thấy SKU sản phẩm", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            if (buyNow) {
                openCheckoutForBuyNow(product, sku, quantity[0], selectedWeight[0], hasWeightOptions);
            } else {
                String customerId = resolveCustomerId();
                viewModel.addToCart(customerId, sku, quantity[0], selectedWeight[0]);
            }
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void bindPopupOriginalPrice(TextView originalPriceView, long originalPrice, long salePrice) {
        if (originalPriceView == null) {
            return;
        }
        if (originalPrice > salePrice && salePrice > 0) {
            originalPriceView.setVisibility(View.VISIBLE);
            originalPriceView.setText(CurrencyFormatter.formatVnd(originalPrice));
            originalPriceView.setPaintFlags(originalPriceView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            originalPriceView.setVisibility(View.GONE);
        }
    }

    private void openCheckoutForBuyNow(Product product, String sku, int quantity, double selectedWeight, boolean hasWeightOptions) {
        android.content.Intent intent = buildBuyNowIntent(CheckoutActivity.class, product, sku, quantity, selectedWeight, hasWeightOptions);
        if (new AppPreferences(this).isLoggedIn()) {
            startActivity(intent);
            return;
        }
        showBuyNowRoleDialog(intent, product, sku, quantity, selectedWeight, hasWeightOptions);
    }

    private android.content.Intent buildBuyNowIntent(Class<?> target, Product product, String sku, int quantity,
                                                     double selectedWeight, boolean hasWeightOptions) {
        android.content.Intent intent = new android.content.Intent(this, target);
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW, true);
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_PRODUCT_ID, product.getId());
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_SKU, sku);
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_NAME, product.getName());
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_PRICE, effectivePrice(product));
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_ORIGINAL_PRICE, effectiveOriginalPrice(product));
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_IMAGE, product.getImageUrl());
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_WEIGHT, product.getWeight());
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_SELECTED_WEIGHT, selectedWeight);
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_HAS_WEIGHT_OPTIONS, hasWeightOptions);
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_QUANTITY, quantity);
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_CATEGORY_ID, product.getCategoryId());
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_SUBCATEGORY_ID, product.getSubcategoryId());
        intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_CARBON_POINT, product.getCarbonSavingPoint());
        return intent;
    }

    private void showBuyNowRoleDialog(android.content.Intent accountIntent, Product product, String sku, int quantity,
                                      double selectedWeight, boolean hasWeightOptions) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_checkout_type, null, false);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialogView.findViewById(R.id.btnOpenCheckout).setOnClickListener(v -> {
            new PendingCheckoutStore(this).saveBuyNowIntent(accountIntent);
            dialog.dismiss();
            startActivity(new android.content.Intent(this, LoginActivity.class));
        });
        dialogView.findViewById(R.id.btnOpenCheckoutGuest).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(buildBuyNowIntent(CheckoutGuestActivity.class, product, sku, quantity, selectedWeight, hasWeightOptions));
        });
        dialog.show();
    }

    private void refreshCartBadge(boolean animate) {
        String customerId = resolveCustomerId();
        AppModule.provideCartRepository(this).getCart(customerId).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int count = CartCountUtils.countLineItems(response.body());
                    runOnUiThread(() -> {
                        updateProductCartBadge(count);
                        if (animate) {
                            playCartBounce();
                        }
                    });
                } else if (animate) {
                    runOnUiThread(ProductDetailActivity.this::playCartBounce);
                }
            }

            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                if (animate) {
                    runOnUiThread(ProductDetailActivity.this::playCartBounce);
                }
            }
        });
    }

    private void updateProductCartBadge(int count) {
        BadgeUiHelper.applyAlertBadge(tvProductCartBadge, count);
    }

    private void playCartBounce() {
        View target = btnCartContainer != null ? btnCartContainer : findViewById(R.id.btnCart);
        if (target == null) {
            return;
        }
        target.animate().cancel();
        target.setScaleX(1f);
        target.setScaleY(1f);
        target.animate()
                .scaleX(1.22f)
                .scaleY(1.22f)
                .setDuration(120)
                .withEndAction(() -> target.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(160)
                        .start())
                .start();
    }

    private void showProductImagePreview() {
        Product product = viewModel != null && viewModel.getProduct() != null
                ? viewModel.getProduct().getValue()
                : null;
        String imageUrl = product != null ? product.getImageUrl() : null;
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        ImageView previewImage = new ImageView(this);
        previewImage.setAdjustViewBounds(true);
        previewImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        previewImage.setOnTouchListener(new ImageZoomTouchListener(previewImage));
        root.addView(previewImage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        FrameLayout closeButton = new FrameLayout(this);
        closeButton.setBackgroundResource(R.drawable.bg_community_circle);
        closeButton.setClickable(true);
        closeButton.setFocusable(true);
        closeButton.setForeground(resolveBorderlessSelectable());
        closeButton.setClipChildren(false);
        closeButton.setClipToPadding(false);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        androidx.appcompat.widget.AppCompatImageView closeIcon =
                new androidx.appcompat.widget.AppCompatImageView(this);
        closeIcon.setImageResource(R.drawable.ic_close);
        closeIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary_main));
        closeIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams closeIconParams = new FrameLayout.LayoutParams(dp(20), dp(20));
        closeIconParams.gravity = android.view.Gravity.CENTER;
        closeButton.addView(closeIcon, closeIconParams);

        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(dp(42), dp(42));
        closeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        closeParams.setMargins(0, dp(16), dp(14), 0);
        root.addView(closeButton, closeParams);

        dialog.setContentView(root);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }

        Glide.with(this).load(imageUrl).into(previewImage);
    }

    private android.graphics.drawable.Drawable resolveBorderlessSelectable() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, typedValue, true);
        return ContextCompat.getDrawable(this, typedValue.resourceId);
    }

    private class ImageZoomTouchListener implements View.OnTouchListener {
        private final ImageView target;
        private final ScaleGestureDetector scaleDetector;
        private float scale = 1f;
        private float lastX;
        private float lastY;

        ImageZoomTouchListener(ImageView target) {
            this.target = target;
            this.scaleDetector = new ScaleGestureDetector(
                    ProductDetailActivity.this,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override
                        public boolean onScale(ScaleGestureDetector detector) {
                            scale = Math.max(1f, Math.min(scale * detector.getScaleFactor(), 4f));
                            target.setScaleX(scale);
                            target.setScaleY(scale);
                            target.setTranslationX(clampTranslation(target.getTranslationX(), target.getWidth(), scale));
                            target.setTranslationY(clampTranslation(target.getTranslationY(), target.getHeight(), scale));
                            return true;
                        }
                    }
            );
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            scaleDetector.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!scaleDetector.isInProgress() && scale > 1f) {
                        float dx = event.getX() - lastX;
                        float dy = event.getY() - lastY;
                        target.setTranslationX(clampTranslation(target.getTranslationX() + dx, target.getWidth(), scale));
                        target.setTranslationY(clampTranslation(target.getTranslationY() + dy, target.getHeight(), scale));
                    }
                    lastX = event.getX();
                    lastY = event.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (scale <= 1.02f) {
                        resetImage();
                    }
                    return true;
                default:
                    return true;
            }
        }

        private float clampTranslation(float value, int size, float currentScale) {
            float max = (size * (currentScale - 1f)) / 2f;
            return Math.max(-max, Math.min(max, value));
        }

        private void resetImage() {
            scale = 1f;
            target.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationX(0f)
                    .translationY(0f)
                    .setDuration(160)
                    .start();
        }
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(
                AppModule.provideProductRepository(this),
                AppModule.provideReviewRepository(this),
                AppModule.provideConsultationRepository(this),
                AppModule.provideRecipeRepository(),
                AppModule.provideCartRepository(this)
        );
        viewModel = new ViewModelProvider(this, factory).get(ProductViewModel.class);
    }

    private void loadProductImage(ImageView target, String imageUrl) {
        ProductImageUtils.loadInto(this, target, imageUrl, R.drawable.ic_vegetable, R.drawable.ic_vegetable);
    }

    private void bindProductData(Product product) {
        loadProductImage(ivProductImage, product.getImageUrl());
        tvProductName.setText(product.getName());
        tvWeight.setText(product.getWeight());
        
        Locale vnLocale = new Locale("vi", "VN");
        tvRating.setText(String.format(vnLocale, "%.1f (%d)", product.getRating(), product.getReviewCount()));
        
        tvSold.setText(getString(R.string.sold_count_format, product.getSoldCount()));
        long displayPrice = effectivePrice(product);
        long displayOriginalPrice = effectiveOriginalPrice(product);
        tvPrice.setText(CurrencyFormatter.formatVnd(displayPrice));
        
        if (displayOriginalPrice > displayPrice && displayPrice > 0) {
            tvOriginalPrice.setVisibility(View.VISIBLE);
            tvOriginalPrice.setText(CurrencyFormatter.formatVnd(displayOriginalPrice));
            tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            if (tvDiscountBadge != null) {
                tvDiscountBadge.setVisibility(View.VISIBLE);
                if (tvDiscountBadge instanceof TextView) {
                    ((TextView) tvDiscountBadge).setText(effectiveDiscountLabel(product, displayPrice, displayOriginalPrice));
                }
            }
        } else {
            tvOriginalPrice.setVisibility(View.GONE);
            if (tvDiscountBadge != null) tvDiscountBadge.setVisibility(View.GONE);
        }
        
        bindDescriptionTable(product);
        renderProductFavorite(favoriteStore != null
                && favoriteStore.isFavorite(FavoriteStore.TYPE_PRODUCT, product.getId()));
        
        loadPriceAlertBanner(product.getId());
    }

    private void loadPriceAlertBanner(String productId) {
        if (productId == null || productId.isEmpty()) return;

        com.veggo.app.data.remote.api.ForecastApi forecastApi = com.veggo.app.core.network.ApiClient.createService(com.veggo.app.data.remote.api.ForecastApi.class);
        forecastApi.getProductForecast(productId).enqueue(new retrofit2.Callback<com.veggo.app.data.remote.dto.ForecastResponseDto>() {
            @Override
            public void onResponse(retrofit2.Call<com.veggo.app.data.remote.dto.ForecastResponseDto> call, retrofit2.Response<com.veggo.app.data.remote.dto.ForecastResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    com.veggo.app.data.remote.dto.ForecastResponseDto.ForecastData forecast = response.body().getData();
                    if (forecast != null) {
                        double change = forecast.getChangePercent();
                        if (change >= 10.0 || change <= -10.0) {
                            com.google.android.material.card.MaterialCardView banner = findViewById(R.id.cardProductDetailAlert);
                            TextView txtDetailAlertText = findViewById(R.id.txtDetailAlertText);
                            ImageView imgDetailAlertIcon = findViewById(R.id.imgDetailAlertIcon);

                            if (banner != null) {
                                banner.setVisibility(View.VISIBLE);
                                 if (txtDetailAlertText != null) {
                                     txtDetailAlertText.setText(android.text.Html.fromHtml(forecast.getReason(), android.text.Html.FROM_HTML_MODE_LEGACY));
                                 }
                                if (imgDetailAlertIcon != null) {
                                    if ("down".equals(forecast.getTrend())) {
                                        imgDetailAlertIcon.setImageResource(R.drawable.ic_fire);
                                        imgDetailAlertIcon.setImageTintList(null); // Sử dụng màu nguyên bản của icon ngọn lửa PNG
                                        
                                        banner.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                                                android.graphics.Color.parseColor("#FFF9E6")
                                        ));
                                        banner.setStrokeColor(android.content.res.ColorStateList.valueOf(
                                                android.graphics.Color.parseColor("#FBC02D")
                                        ));

                                        // Tạo animation bập bùng (flicker) cho ngọn lửa ở banner trang chi tiết
                                        android.view.animation.AnimationSet animationSet = new android.view.animation.AnimationSet(true);
                                        
                                        android.view.animation.AlphaAnimation alphaAnim = new android.view.animation.AlphaAnimation(0.6f, 1.0f);
                                        alphaAnim.setDuration(350);
                                        alphaAnim.setRepeatMode(android.view.animation.Animation.REVERSE);
                                        alphaAnim.setRepeatCount(android.view.animation.Animation.INFINITE);
                                        
                                        android.view.animation.ScaleAnimation scaleAnim = new android.view.animation.ScaleAnimation(
                                                0.88f, 1.12f,
                                                0.88f, 1.12f,
                                                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                                                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
                                        );
                                        scaleAnim.setDuration(350);
                                        scaleAnim.setRepeatMode(android.view.animation.Animation.REVERSE);
                                        scaleAnim.setRepeatCount(android.view.animation.Animation.INFINITE);
                                        
                                        animationSet.addAnimation(alphaAnim);
                                        animationSet.addAnimation(scaleAnim);
                                        imgDetailAlertIcon.startAnimation(animationSet);
                                    } else {
                                        imgDetailAlertIcon.setImageResource(android.R.drawable.stat_sys_warning);
                                        imgDetailAlertIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                                                androidx.core.content.ContextCompat.getColor(ProductDetailActivity.this, R.color.veggo_danger)
                                        ));
                                        banner.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(
                                                androidx.core.content.ContextCompat.getColor(ProductDetailActivity.this, R.color.veggo_danger_soft)
                                        ));
                                        banner.setStrokeColor(android.content.res.ColorStateList.valueOf(
                                                androidx.core.content.ContextCompat.getColor(ProductDetailActivity.this, R.color.veggo_danger)
                                        ));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<com.veggo.app.data.remote.dto.ForecastResponseDto> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void bindDescriptionTable(Product product) {
        if (tblDescription == null) {
            return;
        }

        tblDescription.removeAllViews();
        descriptionRows.clear();
        descriptionExpanded = false;

        Map<String, String> rows = buildDescriptionRows(product);
        for (Map.Entry<String, String> row : rows.entrySet()) {
            TableRow tableRow = createDescriptionRow(row.getKey(), row.getValue());
            tblDescription.addView(tableRow);
            descriptionRows.add(tableRow);
        }

        applyDescriptionExpansion();
    }

    private Map<String, String> buildDescriptionRows(Product product) {
        LinkedHashMap<String, String> rows = new LinkedHashMap<>();
        Map<String, String> parsed = parseDescriptionSections(product.getDescription());

        putIfPresent(rows, "Thành phần", firstNonBlank(product.getIngredients(), parsed.get("Thành phần")));
        putIfPresent(rows, "Cách dùng", firstNonBlank(product.getUsage(), parsed.get("Cách dùng"), product.getDescription()));
        putIfPresent(rows, "Cách bảo quản", firstNonBlank(product.getStorage(), parsed.get("Cách bảo quản"), parsed.get("Bảo quản")));
        putIfPresent(rows, "Lưu ý khi sử dụng", firstNonBlank(product.getSafetyWarning(), parsed.get("Lưu ý khi sử dụng"), parsed.get("Lưu ý")));
        putIfPresent(rows, "Hãng", firstNonBlank(parsed.get("Hãng"), product.getFatContent()));
        putIfPresent(rows, "Nơi sản xuất", firstNonBlank(parsed.get("Nơi sản xuất"), parsed.get("Nhà sản xuất"), product.getOrigin()));
        putIfPresent(rows, "Nhà sản xuất", product.getProducer());
        putIfPresent(rows, "Đơn vị chịu trách nhiệm", product.getResponsibleOrg());
        putIfPresent(rows, "Ngày sản xuất", product.getManufactureDate());
        putIfPresent(rows, "Hạn sử dụng", product.getExpiryDate());

        if (rows.isEmpty()) {
            putIfPresent(rows, "Mô tả", product.getDescription());
        }
        if (rows.isEmpty()) {
            rows.put("Mô tả", "Chưa có mô tả chi tiết cho sản phẩm này.");
        }
        return rows;
    }

    private Map<String, String> parseDescriptionSections(String description) {
        LinkedHashMap<String, String> rows = new LinkedHashMap<>();
        if (description == null || description.trim().isEmpty()) {
            return rows;
        }

        String currentLabel = null;
        StringBuilder currentValue = new StringBuilder();
        String[] lines = description.split("\\r?\\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                if (currentLabel != null) {
                    rows.put(currentLabel, currentValue.toString().trim());
                }
                currentLabel = normalizeDescriptionLabel(line.substring(0, colonIndex));
                currentValue = new StringBuilder(line.substring(colonIndex + 1).trim());
            } else if (currentLabel != null) {
                if (currentValue.length() > 0) {
                    currentValue.append('\n');
                }
                currentValue.append(line);
            }
        }

        if (currentLabel != null) {
            rows.put(currentLabel, currentValue.toString().trim());
        } else {
            rows.put("Mô tả", description.trim());
        }
        return rows;
    }

    private String normalizeDescriptionLabel(String label) {
        String cleaned = label != null ? label.trim() : "";
        if ("Bảo quản".equalsIgnoreCase(cleaned)) {
            return "Cách bảo quản";
        }
        if ("Nhà sản xuất".equalsIgnoreCase(cleaned)) {
            return "Nơi sản xuất";
        }
        return cleaned;
    }

    private TableRow createDescriptionRow(String label, String value) {
        TableRow row = new TableRow(this);
        row.setPadding(0, dp(4), 0, dp(4));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
        labelView.setTextSize(14);
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labelView.setPadding(0, 0, dp(12), 0);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(ContextCompat.getColor(this, R.color.neutral_70));
        valueView.setTextSize(14);
        valueView.setLineSpacing(0, 1.08f);

        TableRow.LayoutParams labelParams = new TableRow.LayoutParams(dp(118), TableRow.LayoutParams.WRAP_CONTENT);
        TableRow.LayoutParams valueParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(labelView, labelParams);
        row.addView(valueView, valueParams);
        return row;
    }

    private void applyDescriptionExpansion() {
        int collapsedRows = 4;
        for (int i = 0; i < descriptionRows.size(); i++) {
            descriptionRows.get(i).setVisibility(descriptionExpanded || i < collapsedRows ? View.VISIBLE : View.GONE);
        }
        if (tvShowMoreDesc != null) {
            boolean showToggle = descriptionRows.size() > collapsedRows;
            tvShowMoreDesc.setVisibility(showToggle ? View.VISIBLE : View.GONE);
            tvShowMoreDesc.setText(descriptionExpanded ? R.string.show_less : R.string.show_more);
        }
    }

    private void putIfPresent(Map<String, String> rows, String label, String value) {
        String cleaned = firstNonBlank(value);
        if (cleaned != null) {
            rows.put(label, cleaned);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String calculateDiscountPercentage(Product product) {
        if (product.getOriginalPrice() <= 0) return "";
        long discount = product.getOriginalPrice() - product.getPrice();
        int percentage = (int) ((discount * 100.0f) / product.getOriginalPrice());
        return "-" + percentage + "%";
    }

    private long effectivePrice(Product product) {
        if (flashSalePrice > 0) {
            return flashSalePrice;
        }
        return product.getPrice();
    }

    private long effectiveOriginalPrice(Product product) {
        if (flashSaleOriginalPrice > 0) {
            return flashSaleOriginalPrice;
        }
        return product.getOriginalPrice();
    }

    private String effectiveDiscountLabel(Product product, long salePrice, long originalPrice) {
        if (flashSaleDiscountLabel != null && !flashSaleDiscountLabel.trim().isEmpty()) {
            return flashSaleDiscountLabel;
        }
        if (originalPrice <= 0 || salePrice <= 0 || originalPrice <= salePrice) {
            return "";
        }
        long discount = originalPrice - salePrice;
        int percentage = (int) ((discount * 100.0f) / originalPrice);
        return "-" + percentage + "%";
    }

    private String resolveCustomerId() {
        AppPreferences appPreferences = new AppPreferences(this);
        String customerId = appPreferences.getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            customerId = new com.veggo.app.core.preferences.PreferencesManager(this).getUserId();
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            customerId = new PendingCheckoutStore(this).guestId();
        }
        return customerId;
    }

    private boolean hasWeightOptions(Product product) {
        if (product == null || product.getWeightOptions() == null) {
            return false;
        }
        for (Double option : product.getWeightOptions()) {
            if (option != null && option > 0) {
                return true;
            }
        }
        return false;
    }

    private void updateAddToCartTotal(TextView totalView, long unitPrice, int quantity, double selectedWeight, boolean hasWeightOptions) {
        if (totalView == null) {
            return;
        }
        double multiplier = hasWeightOptions ? selectedWeight : 1.0;
        long total = Math.round(unitPrice * multiplier * quantity);
        totalView.setText(CurrencyFormatter.formatVnd(total));
    }

    private void bindWeightOptions(TextView titleView, ChipGroup chipGroup, java.util.List<Double> weightOptions,
                                   double[] selectedWeight, Runnable onSelectionChanged) {
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
            selectedWeight[0] = 1.0;
            chipGroup.setVisibility(View.GONE);
            if (titleView != null) {
                titleView.setVisibility(View.GONE);
            }
            return;
        }

        chipGroup.setVisibility(View.VISIBLE);
        if (titleView != null) {
            titleView.setVisibility(View.VISIBLE);
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
                    if (onSelectionChanged != null) {
                        onSelectionChanged.run();
                    }
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
