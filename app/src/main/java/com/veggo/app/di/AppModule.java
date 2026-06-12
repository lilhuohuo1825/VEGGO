package com.veggo.app.di;

import android.content.Context;

import com.veggo.app.core.database.VeggoDatabase;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.data.local.dao.CartDao;
import com.veggo.app.data.local.dao.ProductDao;
import com.veggo.app.data.local.dao.SearchHistoryDao;
import com.veggo.app.data.local.dao.UserDao;
import com.veggo.app.data.remote.api.ProductApi;
import com.veggo.app.data.repository.ProductRepositoryImpl;
import com.veggo.app.domain.repository.ProductRepository;

public final class AppModule {
    private AppModule() {
    }

    public static VeggoDatabase provideDatabase(Context context) {
        return VeggoDatabase.getInstance(context);
    }

    public static ProductDao provideProductDao(Context context) {
        return provideDatabase(context).productDao();
    }

    public static CartDao provideCartDao(Context context) {
        return provideDatabase(context).cartDao();
    }

    public static SearchHistoryDao provideSearchHistoryDao(Context context) {
        return provideDatabase(context).searchHistoryDao();
    }

    public static UserDao provideUserDao(Context context) {
        return provideDatabase(context).userDao();
    }

    public static ProductRepository provideProductRepository(Context context) {
        return new ProductRepositoryImpl(provideProductDao(context));
    }

    public static ProductApi provideProductApi() {
        return ApiClient.createService(ProductApi.class);
    }
}
