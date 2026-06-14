package com.veggo.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.google.gson.Gson;
import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.data.local.dao.AssetRecordDao;
import com.veggo.app.data.local.entity.AssetRecordEntity;
import com.veggo.app.domain.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.List;

public class CategoryRepositoryImpl implements CategoryRepository {
    private final AssetRecordDao assetRecordDao;
    private final Gson gson;

    public CategoryRepositoryImpl(AssetRecordDao assetRecordDao, Gson gson) {
        this.assetRecordDao = assetRecordDao;
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
}
