package com.veggo.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.veggo.app.data.local.dao.ProductDao;
import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.mapper.ProductMapper;
import com.veggo.app.data.remote.api.ProductApi;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRepositoryImpl implements ProductRepository {
    private final ProductDao productDao;
    private final ProductApi productApi;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ProductRepositoryImpl(ProductDao productDao, ProductApi productApi) {
        this.productDao = productDao;
        this.productApi = productApi;
    }

    @Override
    public LiveData<List<Product>> observeProducts() {
        return Transformations.map(productDao.observeProducts(), entities -> {
            List<Product> products = new ArrayList<>();
            for (int i = 0; i < entities.size(); i++) {
                products.add(ProductMapper.fromEntity(entities.get(i)));
            }
            return products;
        });
    }

    @Override
    public void refreshProducts() {
        executor.execute(() -> {
            try {
                retrofit2.Response<List<ProductDto>> response = productApi.getProducts().execute();
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                List<ProductEntity> entities = new ArrayList<>();
                for (ProductDto dto : response.body()) {
                    if (dto.getId() != null) {
                        entities.add(ProductMapper.toEntity(dto));
                    }
                }
                productDao.insertAll(entities);
            } catch (Exception ignored) {
                // UI state/error surfacing will be added when feature logic is implemented.
            }
        });
    }
}
