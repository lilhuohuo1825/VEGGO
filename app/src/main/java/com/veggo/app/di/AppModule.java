package com.veggo.app.di;

import android.content.Context;

import com.veggo.app.core.database.VeggoDatabase;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.data.remote.api.ProductApi;
import com.veggo.app.data.repository.ProductRepositoryImpl;
import com.veggo.app.domain.repository.ProductRepository;

public final class AppModule {
    private AppModule() {
    }

    public static VeggoDatabase provideDatabase(Context context) {
        return VeggoDatabase.getInstance(context);
    }

    public static ProductRepository provideProductRepository(Context context) {
        return new ProductRepositoryImpl(provideDatabase(context).productDao(), provideProductApi());
    }

    public static ProductApi provideProductApi() {
        return ApiClient.createService(ProductApi.class);
    }
}
