package com.veggo.app.domain.repository;

import androidx.lifecycle.LiveData;
import com.veggo.app.assets.AssetModels;
import java.util.List;

public interface CategoryRepository {
    LiveData<List<AssetModels.Category>> observeCategories();
}
