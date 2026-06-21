package com.veggo.app.domain.repository;

import com.veggo.app.domain.model.Review;
import java.util.List;

public interface ReviewRepository {
    interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable t);
    }

    void getReviewsBySku(String sku, Callback<List<Review>> callback);
}
