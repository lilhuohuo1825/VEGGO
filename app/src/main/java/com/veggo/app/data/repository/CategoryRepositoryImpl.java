package com.veggo.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.google.gson.Gson;
import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.data.local.dao.AssetRecordDao;
import com.veggo.app.data.local.entity.AssetRecordEntity;
import com.veggo.app.data.remote.api.CategoryApi;
import com.veggo.app.data.remote.dto.CategoryDto;
import com.veggo.app.domain.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class CategoryRepositoryImpl implements CategoryRepository {
    private final AssetRecordDao assetRecordDao;
    private final CategoryApi categoryApi;
    private final Gson gson;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CategoryRepositoryImpl(AssetRecordDao assetRecordDao, CategoryApi categoryApi, Gson gson) {
        this.assetRecordDao = assetRecordDao;
        this.categoryApi = categoryApi;
        this.gson = gson;
    }

    @Override
    public LiveData<List<AssetModels.Category>> observeCategories() {
        return Transformations.map(assetRecordDao.observeByCollection(AssetFiles.COLLECTION_CATEGORIES), records -> {
            List<AssetModels.Category> categories = new ArrayList<>();
            for (AssetRecordEntity record : records) {
                categories.add(gson.fromJson(record.getJson(), AssetModels.Category.class));
            }
            return categories;
        });
    }

    @Override
    public void refreshCategories() {
        executor.execute(() -> {
            try {
                Response<List<CategoryDto>> response = categoryApi.getCategories().execute();
                if (response.isSuccessful() && response.body() != null) {
                    List<AssetRecordEntity> entities = new ArrayList<>();
                    for (CategoryDto dto : response.body()) {
                        AssetRecordEntity entity = new AssetRecordEntity(
                                AssetFiles.COLLECTION_CATEGORIES,
                                dto.getCategoryID(),
                                gson.toJson(dto),
                                System.currentTimeMillis()
                        );
                        entities.add(entity);
                    }
                    assetRecordDao.insertAll(entities);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
