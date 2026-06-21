package com.veggo.app.domain.repository;

import com.veggo.app.data.remote.dto.RecipeDetailDto;
import com.veggo.app.domain.model.Recipe;

import java.util.List;

public interface RecipeRepository {
    interface Callback<T> {
        void onSuccess(T result);
        void onError(Throwable t);
    }

    void getRelatedRecipes(String productName, Callback<List<Recipe>> callback);

    void getRecipeDetail(String instructionId, Callback<RecipeDetailDto> callback);
}
