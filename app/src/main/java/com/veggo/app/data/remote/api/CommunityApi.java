package com.veggo.app.data.remote.api;

import com.veggo.app.data.local.entity.CommunityCategoryEntity;
import com.veggo.app.data.local.entity.CommunityChefEntity;
import com.veggo.app.data.local.entity.CommunityCookbookEntity;
import com.veggo.app.data.local.entity.CommunityFollowEntity;
import com.veggo.app.data.local.entity.CommunityRecipeCommentEntity;
import com.veggo.app.data.local.entity.CommunityRecipeEntity;
import com.veggo.app.presentation.community.CommunityRepository;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface CommunityApi {
    @GET("community/home")
    Call<CommunityRepository.CommunityData> getHome();

    @GET("community/categories")
    Call<List<CommunityCategoryEntity>> getCategories();

    @GET("community/chefs")
    Call<List<CommunityChefEntity>> getChefs();

    @GET("community/users/{customerId}")
    Call<CommunityChefEntity> getUser(@Path("customerId") String customerId);

    @GET("community/recipes")
    Call<List<CommunityRecipeEntity>> getRecipes(
            @Query("categoryId") String categoryId,
            @Query("chefId") String chefId,
            @Query("limit") Integer limit
    );

    @GET("community/recipes/drafts/{customerId}")
    Call<CommunityRepository.DraftResponse> getRecipeDraft(@Path("customerId") String customerId);

    @POST("community/recipes/drafts")
    Call<CommunityRepository.RecipeDraft> saveRecipeDraft(@Body CommunityRepository.RecipeDraft request);

    @DELETE("community/recipes/drafts/{draftId}")
    Call<CommunityRepository.OkResponse> deleteRecipeDraft(
            @Path("draftId") String draftId,
            @Query("customerId") String customerId
    );

    @POST("community/recipes/publish")
    Call<CommunityRecipeEntity> publishRecipe(@Body CommunityRepository.RecipeDraft request);

    @Multipart
    @POST("community/uploads/images")
    Call<CommunityRepository.ImageUploadResponse> uploadImages(@Part List<MultipartBody.Part> images);

    @PUT("community/recipes/{recipeId}")
    Call<CommunityRecipeEntity> updateRecipe(
            @Path("recipeId") String recipeId,
            @Body CommunityRepository.RecipeDraft request
    );

    @DELETE("community/recipes/{recipeId}")
    Call<CommunityRepository.OkResponse> deleteRecipe(
            @Path("recipeId") String recipeId,
            @Query("customerId") String customerId
    );

    @GET("community/recipes/{recipeId}/detail")
    Call<CommunityRepository.RecipeDetailData> getRecipeDetail(
            @Path("recipeId") String recipeId,
            @Query("viewerId") String viewerId
    );

    @GET("community/recipes/{recipeId}/saved")
    Call<CommunityRepository.SavedStatus> getRecipeSavedStatus(
            @Path("recipeId") String recipeId,
            @Query("customerId") String customerId
    );

    @GET("community/cookbooks")
    Call<List<CommunityCookbookEntity>> getCookbooks(@Query("customerId") String customerId);

    @GET("community/cookbooks/{cookbookId}")
    Call<CommunityRepository.CookbookData> getCookbook(@Path("cookbookId") String cookbookId);

    @POST("community/cookbooks")
    Call<CommunityCookbookEntity> createCookbook(@Body CommunityRepository.CreateCookbookRequest request);

    @POST("community/cookbooks/{cookbookId}/recipes")
    Call<CommunityRepository.OkResponse> addRecipeToCookbook(
            @Path("cookbookId") String cookbookId,
            @Body CommunityRepository.AddRecipeToCookbookRequest request
    );

    @DELETE("community/cookbooks/recipes/{recipeId}")
    Call<CommunityRepository.OkResponse> removeRecipeFromCookbooks(
            @Path("recipeId") String recipeId,
            @Query("customerId") String customerId
    );

    @GET("community/follows")
    Call<List<CommunityFollowEntity>> getFollows(
            @Query("chefId") String chefId,
            @Query("relationType") String relationType
    );

    @GET("community/follows/counts")
    Call<CommunityRepository.FollowCounts> getFollowCounts(
            @Query("chefId") String chefId,
            @Query("viewerId") String viewerId
    );

    @POST("community/follows/toggle")
    Call<CommunityRepository.FollowCounts> toggleFollow(@Body CommunityRepository.ToggleFollowRequest request);

    @POST("community/comments")
    Call<CommunityRecipeCommentEntity> createComment(@Body CommunityRepository.CreateCommentRequest request);

    @POST("community/comments/{commentId}/like")
    Call<CommunityRecipeCommentEntity> toggleCommentLike(
            @Path("commentId") String commentId,
            @Body CommunityRepository.ToggleCommentLikeRequest request
    );
}
