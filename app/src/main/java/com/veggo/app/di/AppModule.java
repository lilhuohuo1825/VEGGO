package com.veggo.app.di;

import android.content.Context;

import com.veggo.app.core.database.VeggoDatabase;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.data.remote.api.CartApi;
import com.veggo.app.data.remote.api.ProductApi;
import com.veggo.app.data.repository.CartRepositoryImpl;
import com.veggo.app.data.repository.CategoryRepositoryImpl;
import com.veggo.app.data.repository.ProductRepositoryImpl;
import com.veggo.app.domain.repository.CartRepository;
import com.veggo.app.domain.repository.CategoryRepository;
import com.veggo.app.domain.repository.ProductRepository;

public final class AppModule {
    private AppModule() {
    }

    public static VeggoDatabase provideDatabase(Context context) {
        return VeggoDatabase.getInstance(context);
    }

    public static ProductRepository provideProductRepository(Context context) {
        return new ProductRepositoryImpl(context, provideDatabase(context).productDao(), provideProductApi());
    }

    public static CartRepository provideCartRepository(Context context) {
        return new CartRepositoryImpl(provideCartApi());
    }

    public static CategoryRepository provideCategoryRepository(Context context) {
        return new CategoryRepositoryImpl(provideDatabase(context).assetRecordDao(), new com.google.gson.Gson());
    }

    public static ProductApi provideProductApi() {
        return ApiClient.createService(ProductApi.class);
    }

    public static CartApi provideCartApi() {
        return ApiClient.createService(CartApi.class);
    }
}
