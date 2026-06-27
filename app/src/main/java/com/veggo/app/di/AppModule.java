package com.veggo.app.di;

import android.content.Context;

import com.google.gson.Gson;
import com.veggo.app.core.database.VeggoDatabase;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.data.remote.api.AddressApi;
import com.veggo.app.data.remote.api.CartApi;
import com.veggo.app.data.remote.api.CategoryApi;
import com.veggo.app.data.remote.api.ConsultationApi;
import com.veggo.app.data.remote.api.ProductApi;
import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.api.RecipeApi;
import com.veggo.app.data.remote.api.ReviewApi;
import com.veggo.app.data.remote.api.UserApi;
import com.veggo.app.data.repository.AddressRepositoryImpl;
import com.veggo.app.data.repository.CartRepositoryImpl;
import com.veggo.app.data.repository.CategoryRepositoryImpl;
import com.veggo.app.data.repository.ConsultationRepositoryImpl;
import com.veggo.app.data.repository.ProductRepositoryImpl;
import com.veggo.app.data.repository.RecipeRepositoryImpl;
import com.veggo.app.data.repository.ReviewRepositoryImpl;
import com.veggo.app.data.repository.UserRepositoryImpl;
import com.veggo.app.domain.repository.AddressRepository;
import com.veggo.app.domain.repository.CartRepository;
import com.veggo.app.domain.repository.CategoryRepository;
import com.veggo.app.domain.repository.ConsultationRepository;
import com.veggo.app.domain.repository.ProductRepository;
import com.veggo.app.domain.repository.RecipeRepository;
import com.veggo.app.domain.repository.ReviewRepository;
import com.veggo.app.domain.repository.UserRepository;

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
        return new CategoryRepositoryImpl(
                provideDatabase(context).assetRecordDao(),
                provideCategoryApi(),
                new Gson()
        );
    }

    public static ReviewRepository provideReviewRepository(Context context) {
        return new ReviewRepositoryImpl(
                provideDatabase(context).productDao(),
                provideReviewApi()
        );
    }

    public static ConsultationRepository provideConsultationRepository(Context context) {
        return new ConsultationRepositoryImpl(provideConsultationApi());
    }

    public static UserRepository provideUserRepository(Context context) {
        return new UserRepositoryImpl(provideUserApi());
    }

    public static AddressRepository provideAddressRepository(Context context) {
        return new AddressRepositoryImpl(provideAddressApi());
    }

    public static RecipeRepository provideRecipeRepository() {
        return new RecipeRepositoryImpl(provideRecipeApi());
    }

    public static CategoryApi provideCategoryApi() {
        return ApiClient.createService(CategoryApi.class);
    }

    public static ProductApi provideProductApi() {
        return ApiClient.createService(ProductApi.class);
    }

    public static CartApi provideCartApi() {
        return ApiClient.createService(CartApi.class);
    }

    public static ReviewApi provideReviewApi() {
        return ApiClient.createService(ReviewApi.class);
    }

    public static ConsultationApi provideConsultationApi() {
        return ApiClient.createService(ConsultationApi.class);
    }

    public static UserApi provideUserApi() {
        return ApiClient.createService(UserApi.class);
    }

    public static AddressApi provideAddressApi() {
        return ApiClient.createService(AddressApi.class);
    }

    public static RecipeApi provideRecipeApi() {
        return ApiClient.createService(RecipeApi.class);
    }

    public static PromotionApi providePromotionApi() {
        return ApiClient.createService(PromotionApi.class);
    }
}
