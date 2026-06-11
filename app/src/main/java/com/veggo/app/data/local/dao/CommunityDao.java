package com.veggo.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veggo.app.data.local.entity.CommunityCategoryEntity;
import com.veggo.app.data.local.entity.CommunityChefEntity;
import com.veggo.app.data.local.entity.CommunityCookbookEntity;
import com.veggo.app.data.local.entity.CommunityCookbookRecipeEntity;
import com.veggo.app.data.local.entity.CommunityFollowEntity;
import com.veggo.app.data.local.entity.CommunityRecipeCommentEntity;
import com.veggo.app.data.local.entity.CommunityRecipeDetailEntity;
import com.veggo.app.data.local.entity.CommunityRecipeEntity;
import com.veggo.app.data.local.entity.CommunityRecipeGalleryEntity;
import com.veggo.app.data.local.entity.CommunityRecipeIngredientEntity;

import java.util.List;

@Dao
public interface CommunityDao {
    @Query("SELECT COUNT(*) FROM community_categories")
    int countCategories();

    @Query("SELECT * FROM community_categories")
    List<CommunityCategoryEntity> getCategories();

    @Query("SELECT * FROM community_chefs")
    List<CommunityChefEntity> getChefs();

    @Query("SELECT * FROM community_chefs LIMIT :limit")
    List<CommunityChefEntity> getChefs(int limit);

    @Query("SELECT * FROM community_chefs WHERE id = :chefId LIMIT 1")
    CommunityChefEntity getChef(String chefId);

    @Query("SELECT * FROM community_recipes")
    List<CommunityRecipeEntity> getRecipes();

    @Query("SELECT * FROM community_recipes WHERE categoryId = :categoryId")
    List<CommunityRecipeEntity> getRecipesByCategory(String categoryId);

    @Query("SELECT * FROM community_recipes WHERE chefId = :chefId")
    List<CommunityRecipeEntity> getRecipesByChef(String chefId);

    @Query("SELECT * FROM community_recipes LIMIT :limit")
    List<CommunityRecipeEntity> getRecipes(int limit);

    @Query("SELECT * FROM community_cookbooks WHERE accountId = :accountId")
    List<CommunityCookbookEntity> getCookbooks(String accountId);

    @Query("SELECT * FROM community_cookbooks WHERE id = :cookbookId LIMIT 1")
    CommunityCookbookEntity getCookbook(String cookbookId);

    @Query("SELECT r.* FROM community_recipes r INNER JOIN community_cookbook_recipes cr ON r.id = cr.recipeId WHERE cr.cookbookId = :cookbookId ORDER BY cr.sortOrder ASC")
    List<CommunityRecipeEntity> getRecipesByCookbook(String cookbookId);

    @Query("SELECT COUNT(*) FROM community_cookbook_recipes WHERE cookbookId = :cookbookId AND recipeId = :recipeId")
    int countCookbookRecipe(String cookbookId, String recipeId);

    @Query("UPDATE community_cookbooks SET recipeCount = recipeCount + 1 WHERE id = :cookbookId")
    void incrementCookbookRecipeCount(String cookbookId);

    @Query("SELECT * FROM community_recipes WHERE id = :recipeId LIMIT 1")
    CommunityRecipeEntity getRecipe(String recipeId);

    @Query("SELECT * FROM community_follows WHERE chefId = :chefId AND relationType = :relationType")
    List<CommunityFollowEntity> getFollows(String chefId, String relationType);

    @Query("SELECT COUNT(*) FROM community_follows WHERE chefId = :chefId AND relationType = :relationType")
    int countFollows(String chefId, String relationType);

    @Query("SELECT * FROM community_recipe_details WHERE recipeId = :recipeId LIMIT 1")
    CommunityRecipeDetailEntity getRecipeDetail(String recipeId);

    @Query("SELECT * FROM community_recipe_ingredients WHERE recipeId = :recipeId ORDER BY sortOrder ASC")
    List<CommunityRecipeIngredientEntity> getRecipeIngredients(String recipeId);

    @Query("SELECT * FROM community_recipe_galleries WHERE recipeId = :recipeId ORDER BY sortOrder ASC")
    List<CommunityRecipeGalleryEntity> getRecipeGallery(String recipeId);

    @Query("SELECT * FROM community_recipe_comments WHERE recipeId = :recipeId ORDER BY sortOrder ASC")
    List<CommunityRecipeCommentEntity> getRecipeComments(String recipeId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCategories(List<CommunityCategoryEntity> categories);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChefs(List<CommunityChefEntity> chefs);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecipes(List<CommunityRecipeEntity> recipes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCookbooks(List<CommunityCookbookEntity> cookbooks);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCookbookRecipes(List<CommunityCookbookRecipeEntity> cookbookRecipes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCookbook(CommunityCookbookEntity cookbook);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCookbookRecipe(CommunityCookbookRecipeEntity cookbookRecipe);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFollows(List<CommunityFollowEntity> follows);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecipeDetails(List<CommunityRecipeDetailEntity> details);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecipeIngredients(List<CommunityRecipeIngredientEntity> ingredients);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecipeGalleries(List<CommunityRecipeGalleryEntity> galleries);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecipeComments(List<CommunityRecipeCommentEntity> comments);
}
