package com.veggo.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.veggo.app.data.local.dao.ProductDao;
import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.mapper.ProductMapper;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

public class ProductRepositoryImpl implements ProductRepository {
    private final ProductDao productDao;

    public ProductRepositoryImpl(ProductDao productDao) {
        this.productDao = productDao;
    }

    @Override
    public LiveData<List<Product>> observeProducts() {
        return Transformations.map(productDao.observeProducts(), entities -> {
            List<Product> products = new ArrayList<>();
            for (ProductEntity entity : entities) {
                products.add(ProductMapper.fromEntity(entity));
            }
            return products;
        });
    }

    @Override
    public void refreshProducts() {
        // SQLite-only mode: Home observes the local Room table directly.
    }
}
