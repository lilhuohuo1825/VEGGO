package com.veggo.app.presentation.product;

import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.veggo.app.R;
import com.veggo.app.adapter.ReviewAdapter;
import com.veggo.app.adapter.ReviewPhotoAdapter;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.model.Review;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewDetailActivity extends BaseActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    private ProductViewModel viewModel;
    private ReviewAdapter adapter;
    private ReviewPhotoAdapter photoAdapter;
    
    private TextView tvAverageRating;
    private RatingBar rbAverageRating;
    private TextView tvTotalRatings;
    private TextView tvPhotoCount;
    
    private List<Review> allReviews = new ArrayList<>();
    private TextView currentFilterView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_detail);

        initViews();
        setupViewModel();
        
        String productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId == null || productId.isEmpty()) {
            // Fallback để test nếu không có ID truyền sang
            productId = "68e36b50c0042663fb020b01";
        }
        viewModel.setProductId(productId);
        
        observeViewModel();
    }

    private void initViews() {
        tvAverageRating = findViewById(R.id.tvAverageRating);
        rbAverageRating = findViewById(R.id.rbAverageRating);
        tvTotalRatings = findViewById(R.id.tvTotalRatings);
        tvPhotoCount = findViewById(R.id.tvPhotoCount);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        
        RecyclerView rvReviewsDetail = findViewById(R.id.rvReviews);
        adapter = new ReviewAdapter();
        rvReviewsDetail.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvReviewsDetail.setAdapter(adapter);

        RecyclerView rvPhotos = findViewById(R.id.rvPhotos);
        photoAdapter = new ReviewPhotoAdapter();
        rvPhotos.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        rvPhotos.setAdapter(photoAdapter);
        
        setupFilters();
        
        findViewById(R.id.btnCartToolbar).setOnClickListener(v -> {
            // Navigate to Cart
        });
    }

    private void setupFilters() {
        TextView filterAll = findViewById(R.id.filterAll);
        TextView filter5Star = findViewById(R.id.filter5Star);
        TextView filter4Star = findViewById(R.id.filter4Star);
        TextView filterWithImages = findViewById(R.id.filterWithImages);

        currentFilterView = filterAll;

        filterAll.setOnClickListener(v -> applyFilter(filterAll, null));
        filter5Star.setOnClickListener(v -> applyFilter(filter5Star, 5));
        filter4Star.setOnClickListener(v -> applyFilter(filter4Star, 4));
        filterWithImages.setOnClickListener(v -> applyFilterWithImages(filterWithImages));
    }

    private void applyFilter(TextView view, Integer rating) {
        updateFilterUI(view);
        if (rating == null) {
            adapter.setReviews(allReviews);
        } else {
            List<Review> filtered = new ArrayList<>();
            for (Review r : allReviews) {
                if (Math.round(r.getRating()) == rating) {
                    filtered.add(r);
                }
            }
            adapter.setReviews(filtered);
        }
    }

    private void applyFilterWithImages(TextView view) {
        updateFilterUI(view);
        List<Review> filtered = new ArrayList<>();
        for (Review r : allReviews) {
            if (r.getImageUrls() != null) {
                for (String url : r.getImageUrls()) {
                    if (url != null && !url.trim().isEmpty()) {
                        filtered.add(r);
                        break;
                    }
                }
            }
        }
        adapter.setReviews(filtered);
    }

    private void updateFilterUI(TextView selectedView) {
        if (currentFilterView != null) {
            currentFilterView.setBackgroundResource(R.drawable.bg_search_bar);
            currentFilterView.setTextColor(getResources().getColor(R.color.neutral_100));
        }
        selectedView.setBackgroundResource(R.drawable.bg_button_primary);
        selectedView.setTextColor(getResources().getColor(R.color.white));
        currentFilterView = selectedView;
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(AppModule.provideProductRepository(this));
        viewModel = new ViewModelProvider(this, factory).get(ProductViewModel.class);
    }

    private void observeViewModel() {
        viewModel.getProductReviews().observe(this, reviews -> {
            if (reviews != null) {
                this.allReviews = reviews;
                updateFilterLabels(reviews);
                if (currentFilterView != null && currentFilterView.getId() == R.id.filterAll) {
                    adapter.setReviews(reviews);
                }
                updateRatingSummary(reviews);
                updatePhotoGallery(reviews);
            }
        });
    }

    private void updateFilterLabels(List<Review> reviews) {
        int count5 = 0, count4 = 0, countWithImages = 0;
        for (Review r : reviews) {
            int star = Math.round(r.getRating());
            if (star >= 5) count5++;
            if (star == 4) count4++;
            
            // Check for valid images
            if (r.getImageUrls() != null) {
                for (String url : r.getImageUrls()) {
                    if (url != null && !url.trim().isEmpty()) {
                        countWithImages++;
                        break; 
                    }
                }
            }
        }

        ((TextView) findViewById(R.id.filterAll)).setText(getString(R.string.filter_all_format, reviews.size()));
        ((TextView) findViewById(R.id.filter5Star)).setText(getString(R.string.filter_5_star_format, count5));
        ((TextView) findViewById(R.id.filter4Star)).setText(getString(R.string.filter_4_star_format, count4));
        ((TextView) findViewById(R.id.filterWithImages)).setText(getString(R.string.filter_with_images_format, countWithImages));
    }

    private void updatePhotoGallery(List<Review> reviews) {
        List<String> allPhotos = new ArrayList<>();
        for (Review r : reviews) {
            if (r.getImageUrls() != null) {
                for (String url : r.getImageUrls()) {
                    if (url != null && !url.trim().isEmpty()) {
                        allPhotos.add(url);
                    }
                }
            }
        }
        
        View galleryContainer = findViewById(R.id.llPhotoGalleryContainer);
        if (allPhotos.isEmpty()) {
            if (galleryContainer != null) galleryContainer.setVisibility(View.GONE);
        } else {
            if (galleryContainer != null) galleryContainer.setVisibility(View.VISIBLE);
            photoAdapter.setPhotos(allPhotos);
            if (tvPhotoCount != null) {
                tvPhotoCount.setText(getString(R.string.customer_photos_format, allPhotos.size()));
            }
        }
    }

    private void updateRatingSummary(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) return;

        int count5 = 0, count4 = 0, count3 = 0, count2 = 0, count1 = 0;
        float total = 0;
        for (Review r : reviews) {
            total += r.getRating();
            int star = Math.round(r.getRating());
            if (star >= 5) count5++;
            else if (star == 4) count4++;
            else if (star == 3) count3++;
            else if (star == 2) count2++;
            else if (star <= 1) count1++;
        }
        float avg = total / reviews.size();

        tvAverageRating.setText(String.format(Locale.getDefault(), "%.1f", avg));
        rbAverageRating.setRating(avg);
        tvTotalRatings.setText(getString(R.string.reviews_count_format, reviews.size()));

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
}

