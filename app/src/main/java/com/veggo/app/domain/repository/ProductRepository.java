package com.veggo.app.domain.repository;

import androidx.lifecycle.LiveData;

import com.veggo.app.domain.model.Product;

import java.util.List;

public interface ProductRepository {
    LiveData<List<Product>> observeProducts();
    void refreshProducts();
}
