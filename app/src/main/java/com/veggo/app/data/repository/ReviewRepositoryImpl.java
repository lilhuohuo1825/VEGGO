package com.veggo.app.data.repository;

import com.veggo.app.data.local.dao.ProductDao;
import com.veggo.app.data.local.entity.ReviewEntity;
import com.veggo.app.data.mapper.ReviewMapper;
import com.veggo.app.data.remote.api.ReviewApi;
import com.veggo.app.data.remote.dto.ProductReviewsDto;
import com.veggo.app.domain.model.Review;
import com.veggo.app.domain.repository.ReviewRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class ReviewRepositoryImpl implements ReviewRepository {
    private final ProductDao productDao;
    private final ReviewApi reviewApi;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ReviewRepositoryImpl(ProductDao productDao, ReviewApi reviewApi) {
        this.productDao = productDao;
        this.reviewApi = reviewApi;
    }

    @Override
    public void getReviewsBySku(String sku, Callback<List<Review>> callback) {
        reviewApi.getReviewsBySku(sku).enqueue(new retrofit2.Callback<ProductReviewsDto>() {
            @Override
            public void onResponse(Call<ProductReviewsDto> call, Response<ProductReviewsDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Review> reviews = new ArrayList<>();
                    List<ReviewEntity> entities = new ArrayList<>();

                    ProductReviewsDto dto = response.body();
                    if (dto.getReviews() != null) {
                        for (com.veggo.app.data.remote.dto.ReviewDto reviewDto : dto.getReviews()) {
                            Review review = ReviewMapper.fromDto(reviewDto);
                            reviews.add(review);

                            // Optionally map to entity to save in local DB
                            // Note: we need a productId to link to the products table if using foreign keys
                            // For now, just return the list
                        }
                    }
                    callback.onSuccess(reviews);

                    // Update local DB in background if needed
                    if (!entities.isEmpty()) {
                        executor.execute(() -> productDao.insertReviews(entities));
                    }
                } else {
                    callback.onError(new Exception("Failed to fetch reviews"));
                }
            }

            @Override
            public void onFailure(Call<ProductReviewsDto> call, Throwable t) {
                callback.onError(t);
            }
        });
    }
}
