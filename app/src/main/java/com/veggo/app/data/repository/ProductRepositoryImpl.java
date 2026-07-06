package com.veggo.app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;

import com.veggo.app.data.local.dao.ProductDao;
import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.local.entity.RecipeEntity;
import com.veggo.app.data.local.entity.ReviewEntity;
import com.veggo.app.data.local.projection.ProductItemProjection;
import com.veggo.app.data.mapper.ProductMapper;
import com.veggo.app.data.mapper.RecipeMapper;
import com.veggo.app.data.mapper.ReviewMapper;
import com.veggo.app.data.remote.api.ProductApi;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.core.utils.ProductDisplayValidator;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.model.Recipe;
import com.veggo.app.domain.model.Review;
import com.veggo.app.domain.repository.ProductRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductRepositoryImpl implements ProductRepository {
    private static final int RELATED_PRODUCT_CANDIDATE_LIMIT = 30;
    private static final int RELATED_PRODUCT_DISPLAY_LIMIT = 10;

    private final ProductDao productDao;
    private final ProductApi productApi;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean catalogRefreshInFlight = false;

    private final Context context;

    public ProductRepositoryImpl(Context context, ProductDao productDao, ProductApi productApi) {
        this.context = context.getApplicationContext();
        this.productDao = productDao;
        this.productApi = productApi;
    }

    @Override
    public LiveData<List<Product>> observeProducts() {
        return observeProducts(0); // 0 means no limit if we want to keep existing behavior, but better to use the overload
    }

    @Override
    public LiveData<List<Product>> observeProducts(int limit) {
        LiveData<List<ProductItemProjection>> source = (limit > 0) ? productDao.observeProducts(limit) : productDao.observeProducts();
        return Transformations.map(source, projections -> {
            List<Product> products = new ArrayList<>();
            for (int i = 0; i < projections.size(); i++) {
                ProductItemProjection projection = projections.get(i);
                if (!ProductDisplayValidator.isDisplayable(projection)) {
                    continue;
                }
                products.add(ProductMapper.fromProjection(projection));
            }
            return products;
        });
    }

    @Override
    public LiveData<Product> observeProductById(String productId) {
        MediatorLiveData<Product> result = new MediatorLiveData<>();
        LiveData<ProductEntity> localSource = productDao.observeProductById(productId);
        final boolean[] remoteRequested = {false};

        result.addSource(localSource, entity -> {
            if (!remoteRequested[0]) {
                remoteRequested[0] = true;
                fetchRemoteProduct(productId, result);
            }

            if (entity != null) {
                result.setValue(ProductMapper.fromEntity(entity));
                return;
            }

            result.setValue(null);
        });

        return result;
    }

    private void fetchRemoteProduct(String productId, MediatorLiveData<Product> result) {
        if (productApi == null || productId == null || productId.trim().isEmpty()) {
            return;
        }

        productApi.getProductById(productId).enqueue(new Callback<ProductDto>() {
            @Override
            public void onResponse(Call<ProductDto> call, Response<ProductDto> response) {
                ProductDto dto = response.body();
                if (!response.isSuccessful() || dto == null) {
                    return;
                }

                Product product = ProductMapper.fromDto(dto);
                if (!ProductDisplayValidator.isDisplayable(product)) {
                    return;
                }
                result.postValue(product);
                executor.execute(() -> productDao.upsert(ProductMapper.toEntity(dto)));
            }

            @Override
            public void onFailure(Call<ProductDto> call, Throwable t) {
                // Keep the local null state; callers already render an empty/loading detail state.
            }
        });
    }

    @Override
    public LiveData<List<Product>> observeRelatedProducts(String currentProductId, String categoryId, String subcategoryId) {
        refreshCatalogFromApi();
        String excludeId = currentProductId != null ? currentProductId : "";
        String catId = categoryId != null ? categoryId : "";
        String subcatId = subcategoryId != null ? subcategoryId : "";

        LiveData<List<ProductEntity>> source = !subcatId.trim().isEmpty()
                ? productDao.observeRelatedMerged(catId, subcatId, excludeId, RELATED_PRODUCT_CANDIDATE_LIMIT)
                : productDao.observeRelatedByCategory(catId, excludeId, RELATED_PRODUCT_CANDIDATE_LIMIT);
        return Transformations.map(source, entities -> limitDisplayableProducts(toProductList(entities), RELATED_PRODUCT_DISPLAY_LIMIT));
    }

    private List<Product> limitDisplayableProducts(List<Product> products, int maxCount) {
        List<Product> displayable = new ArrayList<>();
        if (products == null) {
            return displayable;
        }
        for (Product product : products) {
            if (!ProductDisplayValidator.isDisplayable(product)) {
                continue;
            }
            displayable.add(product);
            if (displayable.size() >= maxCount) {
                break;
            }
        }
        return displayable;
    }

    private void refreshCatalogFromApi() {
        if (catalogRefreshInFlight || productApi == null) {
            return;
        }
        catalogRefreshInFlight = true;
        productApi.getProducts("true").enqueue(new Callback<List<ProductDto>>() {
            @Override
            public void onResponse(Call<List<ProductDto>> call, Response<List<ProductDto>> response) {
                catalogRefreshInFlight = false;
                List<ProductDto> dtos = response.body();
                if (!response.isSuccessful() || dtos == null) {
                    return;
                }
                executor.execute(() -> {
                    int syncedCount = 0;
                    for (ProductDto dto : dtos) {
                        ProductEntity entity = buildSyncedEntity(dto);
                        if (entity != null) {
                            productDao.upsert(entity);
                            syncedCount++;
                        }
                    }
                    if (syncedCount == 0) {
                        android.util.Log.w("ProductRepository", "Catalog sync skipped: no displayable products with images");
                    }
                });
            }

            @Override
            public void onFailure(Call<List<ProductDto>> call, Throwable t) {
                catalogRefreshInFlight = false;
            }
        });
    }

    private ProductEntity buildSyncedEntity(ProductDto dto) {
        if (dto == null) {
            return null;
        }

        ProductEntity incoming = ProductMapper.toEntity(dto);
        preserveExistingImageIfNeeded(incoming);

        if (!ProductDisplayValidator.isDisplayable(incoming)) {
            return null;
        }
        return incoming;
    }

    private void preserveExistingImageIfNeeded(ProductEntity incoming) {
        if (incoming == null || ProductDisplayValidator.hasValidImage(incoming.getImageUrl())) {
            return;
        }

        ProductEntity existing = productDao.getProductById(incoming.getId());
        if (existing != null && ProductDisplayValidator.hasValidImage(existing.getImageUrl())) {
            incoming.setImageUrl(existing.getImageUrl());
        }
    }

    private List<Product> toProductList(List<com.veggo.app.data.local.entity.ProductEntity> entities) {
        List<Product> products = new ArrayList<>();
        if (entities != null) {
            for (com.veggo.app.data.local.entity.ProductEntity entity : entities) {
                if (!ProductDisplayValidator.isDisplayable(entity)) {
                    continue;
                }
                products.add(ProductMapper.fromEntity(entity));
            }
        }
        return products;
    }

    @Override
    public LiveData<List<Recipe>> getRelatedRecipes(String productId) {
        return Transformations.map(productDao.observeRelatedRecipes(productId), entities -> {
            List<Recipe> recipes = new ArrayList<>();
            if (entities != null) {
                for (RecipeEntity entity : entities) {
                    recipes.add(RecipeMapper.fromEntity(entity));
                }
            }
            return recipes;
        });
    }

    @Override
    public LiveData<List<Review>> getProductReviews(String productId) {
        return Transformations.map(productDao.observeProductReviews(productId), entities -> {
            List<Review> reviews = new ArrayList<>();
            if (entities != null) {
                for (ReviewEntity entity : entities) {
                    reviews.add(ReviewMapper.fromEntity(entity));
                }
            }
            return reviews;
        });
    }

    @Override
    public LiveData<List<Product>> searchProducts(String query) {
        return Transformations.map(productDao.searchProducts(query), projections -> {
            List<Product> products = new ArrayList<>();
            if (projections != null) {
                for (ProductItemProjection projection : projections) {
                    if (!ProductDisplayValidator.isDisplayable(projection)) {
                        continue;
                    }
                    products.add(ProductMapper.fromProjection(projection));
                }
            }
            return products;
        });
    }

    @Override
    public LiveData<List<Product>> observeCatalogProducts() {
        return Transformations.map(productDao.observeAllProductEntities(), this::toProductList);
    }

    @Override
    public LiveData<List<Product>> observeProductsByCategory(String categoryId) {
        refreshCatalogFromApi();
        return Transformations.map(productDao.observeProductsByCategory(categoryId), this::toProductList);
    }

    @Override
    public LiveData<List<Product>> observeProductsBySubcategory(String subcategoryId) {
        refreshCatalogFromApi();
        return Transformations.map(productDao.observeProductsBySubcategory(subcategoryId), this::toProductList);
    }

    @Override
    public void refreshProducts() {
        refreshCatalogFromApi();
        com.veggo.app.core.network.FirebaseSyncManager.getInstance(context).syncProducts();
    }

    public void saveProduct(ProductEntity entity) {
        executor.execute(() -> productDao.upsert(entity));
    }

    public void saveRecipes(List<RecipeEntity> recipes) {
        executor.execute(() -> productDao.insertRecipes(recipes));
    }

    public void saveReviews(List<ReviewEntity> reviews) {
        executor.execute(() -> {
            productDao.insertReviews(reviews);
            Set<String> productIds = new HashSet<>();
            for (ReviewEntity review : reviews) {
                if (review.getProductId() != null && !review.getProductId().isEmpty()) {
                    productIds.add(review.getProductId());
                }
            }
            for (String productId : productIds) {
                productDao.updateProductReviewStats(productId);
            }
        });
    }
}
