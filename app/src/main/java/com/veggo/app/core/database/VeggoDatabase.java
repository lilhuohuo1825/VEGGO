package com.veggo.app.core.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.veggo.app.data.local.dao.AssetRecordDao;
import com.veggo.app.data.local.dao.BlogDao;
import com.veggo.app.data.local.dao.CartDao;
import com.veggo.app.data.local.dao.CommunityDao;
import com.veggo.app.data.local.dao.ProductDao;
import com.veggo.app.data.local.dao.SearchHistoryDao;
import com.veggo.app.data.local.dao.UserDao;
import com.veggo.app.data.local.entity.AssetRecordEntity;
import com.veggo.app.data.local.entity.BlogEntity;
import com.veggo.app.data.local.entity.CartItemEntity;
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
import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.local.entity.RecipeEntity;
import com.veggo.app.data.local.entity.ReviewEntity;
import com.veggo.app.data.local.entity.SearchHistoryEntity;
import com.veggo.app.data.local.entity.UserEntity;

@Database(
        entities = {
                ProductEntity.class,
                BlogEntity.class,
                CommunityCategoryEntity.class,
                CommunityChefEntity.class,
                CommunityCookbookEntity.class,
                CommunityCookbookRecipeEntity.class,
                CommunityRecipeEntity.class,
                CommunityFollowEntity.class,
                CommunityRecipeDetailEntity.class,
                CommunityRecipeIngredientEntity.class,
                CommunityRecipeGalleryEntity.class,
                CommunityRecipeCommentEntity.class,
                CartItemEntity.class,
                SearchHistoryEntity.class,
                UserEntity.class,
                AssetRecordEntity.class,
                RecipeEntity.class,
                ReviewEntity.class
        },
        version = DatabaseManager.DATABASE_VERSION,
        exportSchema = true
)
public abstract class VeggoDatabase extends RoomDatabase {
    private static volatile VeggoDatabase instance;

    public abstract ProductDao productDao();
    public abstract BlogDao blogDao();
    public abstract CommunityDao communityDao();
    public abstract CartDao cartDao();
    public abstract SearchHistoryDao searchHistoryDao();
    public abstract UserDao userDao();
    public abstract AssetRecordDao assetRecordDao();

    public static VeggoDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (VeggoDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            VeggoDatabase.class,
                            DatabaseManager.DATABASE_NAME
                    )
                    .addCallback(new Callback() {
                        @Override
                        public void onCreate(@androidx.annotation.NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            db.execSQL(DatabaseManager.Sql.CREATE_ASSET_RECORDS_TABLE);
                            db.execSQL(DatabaseManager.Sql.CREATE_PRODUCTS_TABLE);
                            db.execSQL(DatabaseManager.Sql.CREATE_CART_ITEMS_TABLE);
                            db.execSQL(DatabaseManager.Sql.CREATE_SEARCH_HISTORY_TABLE);
                            db.execSQL(DatabaseManager.Sql.CREATE_USERS_TABLE);
                            db.execSQL(DatabaseManager.Sql.CREATE_RECIPES_TABLE);
                            db.execSQL(DatabaseManager.Sql.CREATE_REVIEWS_TABLE);
                            // Reset seed version so seeder runs after fresh DB creation
                            context.getApplicationContext()
                                    .getSharedPreferences("veggo_asset_database_seed", android.content.Context.MODE_PRIVATE)
                                    .edit().putInt("seeded_asset_version", 0).apply();
                        }

                        @Override
                        public void onDestructiveMigration(@androidx.annotation.NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                            super.onDestructiveMigration(db);
                            // DB was wiped by fallbackToDestructiveMigration – reset the seed flag
                            // so AssetDatabaseSeeder re-seeds the fresh database on next startup.
                            context.getApplicationContext()
                                    .getSharedPreferences("veggo_asset_database_seed", android.content.Context.MODE_PRIVATE)
                                    .edit().putInt("seeded_asset_version", 0).apply();
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return instance;
    }
}
