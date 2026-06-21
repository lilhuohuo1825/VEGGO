package com.veggo.app.data.repository;

import com.veggo.app.data.remote.api.RecipeApi;
import com.veggo.app.data.remote.dto.RecipeDetailDto;
import com.veggo.app.data.remote.dto.RelatedRecipeDto;
import com.veggo.app.domain.model.Recipe;
import com.veggo.app.domain.repository.RecipeRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class RecipeRepositoryImpl implements RecipeRepository {
    private final RecipeApi recipeApi;

    public RecipeRepositoryImpl(RecipeApi recipeApi) {
        this.recipeApi = recipeApi;
    }

    @Override
    public void getRelatedRecipes(String productName, Callback<List<Recipe>> callback) {
        if (productName == null || productName.trim().isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        recipeApi.getRelatedRecipes(productName.trim()).enqueue(new retrofit2.Callback<List<RelatedRecipeDto>>() {
            @Override
            public void onResponse(Call<List<RelatedRecipeDto>> call, Response<List<RelatedRecipeDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(mapRelatedRecipes(response.body()));
                } else {
                    callback.onError(new Exception("Failed to fetch related recipes"));
                }
            }

            @Override
            public void onFailure(Call<List<RelatedRecipeDto>> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    @Override
    public void getRecipeDetail(String instructionId, Callback<RecipeDetailDto> callback) {
        recipeApi.getRecipeDetail(instructionId).enqueue(new retrofit2.Callback<RecipeDetailDto>() {
            @Override
            public void onResponse(Call<RecipeDetailDto> call, Response<RecipeDetailDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(new Exception("Failed to fetch recipe detail"));
                }
            }

            @Override
            public void onFailure(Call<RecipeDetailDto> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    private List<Recipe> mapRelatedRecipes(List<RelatedRecipeDto> dtos) {
        List<Recipe> recipes = new ArrayList<>();
        if (dtos == null) {
            return recipes;
        }
        for (RelatedRecipeDto dto : dtos) {
            if (dto.getInstructionId() == null || dto.getTitle() == null) {
                continue;
            }
            String cookingTime = dto.getCookingTime() != null ? dto.getCookingTime().trim() : "";
            recipes.add(new Recipe(
                    dto.getInstructionId(),
                    dto.getTitle(),
                    dto.getImage(),
                    cookingTime,
                    "",
                    0f,
                    0
            ));
        }
        return recipes;
    }
}
