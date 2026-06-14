package com.veggo.app.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
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
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.model.Recipe;
import com.veggo.app.domain.model.Review;
import com.veggo.app.domain.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRepositoryImpl implements ProductRepository {
    private final ProductDao productDao;
    private final ProductApi productApi;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
                products.add(ProductMapper.fromProjection(projections.get(i)));
            }
            return products;
        });
    }

    @Override
    public LiveData<Product> observeProductById(String productId) {
        return Transformations.map(productDao.observeProductById(productId), entity -> {
            if (entity == null) return null;
            return ProductMapper.fromEntity(entity);
        });
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
    public LiveData<List<com.veggo.app.assets.AssetModels.Question>> getConsultations(String productId) {
        androidx.lifecycle.MutableLiveData<List<com.veggo.app.assets.AssetModels.Question>> data = new androidx.lifecycle.MutableLiveData<>();
        
        executor.execute(() -> {
            try {
                ProductEntity product = productDao.getProductById(productId);
                String sku = (product != null) ? product.getSku() : null;
                
                if (sku == null) {
                    data.postValue(new ArrayList<>());
                    return;
                }

                com.google.gson.Gson gson = new com.google.gson.Gson();
                
                // Thử lấy từ AssetRecord (SQLite) theo documentId = sku
                com.veggo.app.data.local.dao.AssetRecordDao assetDao = com.veggo.app.core.database.VeggoDatabase.getInstance(context).assetRecordDao();
                com.veggo.app.data.local.entity.AssetRecordEntity record = assetDao.getById(com.veggo.app.assets.AssetFiles.COLLECTION_CONSULTATIONS, sku);
                
                if (record != null) {
                    com.veggo.app.assets.AssetModels.Consultation c = gson.fromJson(record.getJson(), com.veggo.app.assets.AssetModels.Consultation.class);
                    if (c != null && c.questions != null) {
                        data.postValue(c.questions);
                        return;
                    }
                }
                
                // Nếu không thấy theo ID (có thể schema cũ lưu cả mảng trong 1 record), thử lấy tất cả nhưng tối ưu hơn
                // Hoặc nạp trực tiếp từ assets (cho trường hợp chưa seed)
                com.veggo.app.assets.AssetJsonLoader loader = new com.veggo.app.assets.AssetJsonLoader(context);
                List<com.veggo.app.assets.AssetModels.Consultation> consultations = loader.readList(com.veggo.app.assets.AssetFiles.CONSULTATIONS, com.veggo.app.assets.AssetModels.Consultation.class);
                
                if (consultations != null) {
                    for (com.veggo.app.assets.AssetModels.Consultation c : consultations) {
                        if (sku.equals(c.sku)) {
                            data.postValue(c.questions);
                            return;
                        }
                    }
                }

                data.postValue(new ArrayList<>());
            } catch (Exception e) {
                e.printStackTrace();
                data.postValue(new ArrayList<>());
            }
        });

        return data;
    }

    @Override
    public LiveData<List<Product>> observeProductsByCategory(String categoryId) {
        return Transformations.map(productDao.observeProductsByCategory(categoryId), entities -> {
            List<Product> products = new ArrayList<>();
            if (entities != null) {
                for (ProductEntity entity : entities) {
                    products.add(ProductMapper.fromEntity(entity));
                }
            }
            return products;
        });
    }

    @Override
    public LiveData<List<Product>> observeProductsBySubcategory(String subcategoryId) {
        return Transformations.map(productDao.observeProductsBySubcategory(subcategoryId), entities -> {
            List<Product> products = new ArrayList<>();
            if (entities != null) {
                for (ProductEntity entity : entities) {
                    products.add(ProductMapper.fromEntity(entity));
                }
            }
            return products;
        });
    }

    @Override
    public void refreshProducts() {
        com.veggo.app.core.network.FirebaseSyncManager.getInstance(context).syncProducts();
    }

    public void saveProduct(ProductEntity entity) {
        executor.execute(() -> productDao.upsert(entity));
    }

    public void saveRecipes(List<RecipeEntity> recipes) {
        executor.execute(() -> productDao.insertRecipes(recipes));
    }

    public void saveReviews(List<ReviewEntity> reviews) {
        executor.execute(() -> productDao.insertReviews(reviews));
    }
}
