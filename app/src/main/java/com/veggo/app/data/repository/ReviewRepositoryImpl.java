package com.veggo.app.data.repository;

import com.veggo.app.data.local.dao.ProductDao;
import com.veggo.app.data.local.entity.ReviewEntity;
import com.veggo.app.data.mapper.ReviewMapper;
import com.veggo.app.data.remote.api.ReviewApi;
import com.veggo.app.data.remote.dto.ProductReviewsDto;
import com.veggo.app.data.remote.request.ReviewLikeRequest;
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
    public void toggleReviewLike(String sku, String reviewId, String customerId, Callback<List<Review>> callback) {
        reviewApi.toggleReviewLike(sku, reviewId, new ReviewLikeRequest(customerId))
                .enqueue(new retrofit2.Callback<ProductReviewsDto>() {
                    @Override
                    public void onResponse(Call<ProductReviewsDto> call, Response<ProductReviewsDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(mapReviews(response.body()));
                        } else {
                            callback.onError(new Exception("Failed to toggle review like"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ProductReviewsDto> call, Throwable t) {
                        callback.onError(t);
                    }
                });
    }

    private List<Review> mapReviews(ProductReviewsDto dto) {
        List<Review> reviews = new ArrayList<>();
        if (dto != null && dto.getReviews() != null) {
            for (com.veggo.app.data.remote.dto.ReviewDto reviewDto : dto.getReviews()) {
                reviews.add(ReviewMapper.fromDto(reviewDto));
            }
        }
        return reviews;
    }

    @Override
    public void getReviewsBySku(String sku, Callback<List<Review>> callback) {
        reviewApi.getReviewsBySku(sku).enqueue(new retrofit2.Callback<ProductReviewsDto>() {
            @Override
            public void onResponse(Call<ProductReviewsDto> call, Response<ProductReviewsDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProductReviewsDto dto = response.body();
                    List<Review> reviews = mapReviews(dto);
                    callback.onSuccess(reviews);

                    if (dto.getReviews() != null && !dto.getReviews().isEmpty()) {
                        executor.execute(() -> {
                            String productId = productDao.getProductIdBySku(sku);
                            if (productId == null || productId.isEmpty()) {
                                return;
                            }
                            List<ReviewEntity> entities = new ArrayList<>();
                            int index = 0;
                            for (com.veggo.app.data.remote.dto.ReviewDto reviewDto : dto.getReviews()) {
                                Review review = ReviewMapper.fromDto(reviewDto);
                                String reviewId = reviewDto.getId() != null && !reviewDto.getId().isEmpty()
                                        ? reviewDto.getId()
                                        : "remote_review_" + sku + "_" + index;
                                entities.add(ReviewMapper.toEntity(review, productId, reviewId));
                                index++;
                            }
                            productDao.insertReviews(entities);
                            productDao.updateProductReviewStats(productId);
                        });
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
