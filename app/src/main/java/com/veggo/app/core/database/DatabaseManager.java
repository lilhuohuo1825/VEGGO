package com.veggo.app.core.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.veggo.app.assets.AssetFiles;

public final class DatabaseManager {
    public static final String DATABASE_NAME = "veggo.db";
    public static final int DATABASE_VERSION = 35;
    public static final int ASSET_SEED_VERSION = 36;

    private DatabaseManager() {
    }

    public static VeggoDatabase getDatabase(@NonNull Context context) {
        return VeggoDatabase.getInstance(context);
    }

    public static SupportSQLiteDatabase getWritableDatabase(@NonNull Context context) {
        return getDatabase(context).getOpenHelper().getWritableDatabase();
    }

    public static void execute(@NonNull Context context, @NonNull String sql) {
        getWritableDatabase(context).execSQL(sql);
    }

    public static void clearLocalData(@NonNull Context context) {
        SupportSQLiteDatabase database = getWritableDatabase(context);
        database.execSQL(Sql.CREATE_ASSET_RECORDS_TABLE);
        database.beginTransaction();
        try {
            database.execSQL(Sql.DELETE_ALL_ASSET_RECORDS);
            database.execSQL(Sql.DELETE_ALL_CART_ITEMS);
            database.execSQL(Sql.DELETE_ALL_SEARCH_HISTORY);
            database.execSQL(Sql.DELETE_ALL_PRODUCTS);
            database.execSQL(Sql.DELETE_ALL_USERS);
            database.execSQL(Sql.DELETE_ALL_RECIPES);
            database.execSQL(Sql.DELETE_ALL_REVIEWS);
            for (String collection : AssetFiles.COLLECTIONS) {
                database.execSQL(Sql.createAssetCollectionTable(collection));
                database.execSQL("DELETE FROM " + Sql.assetCollectionTable(collection));
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public static final class Tables {
        public static final String PRODUCTS = "products";
        public static final String CART_ITEMS = "cart_items";
        public static final String SEARCH_HISTORY = "search_history";
        public static final String USERS = "users";
        public static final String ASSET_RECORDS = "asset_records";
        public static final String RECIPES = "recipes";
        public static final String REVIEWS = "reviews";

        private Tables() {
        }
    }

    public static final class ProductColumns {
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String PRICE = "price";
        public static final String ORIGINAL_PRICE = "originalPrice";
        public static final String IMAGE_URL = "imageUrl";
        public static final String WEIGHT = "weight";
        public static final String RATING = "rating";
        public static final String REVIEW_COUNT = "reviewCount";
        public static final String SOLD_COUNT = "soldCount";
        public static final String DESCRIPTION = "description";
        public static final String ORIGIN = "origin";
        public static final String CONDITION = "condition";
        public static final String FAT_CONTENT = "fatContent";
        public static final String SKU = "sku";
        public static final String CATEGORY_ID = "categoryId";
        public static final String SUBCATEGORY_ID = "subcategoryId";

        private ProductColumns() {
        }
    }

    public static final class CartItemColumns {
        public static final String PRODUCT_ID = "productId";
        public static final String QUANTITY = "quantity";

        private CartItemColumns() {
        }
    }

    public static final class SearchHistoryColumns {
        public static final String KEYWORD = "keyword";

        private SearchHistoryColumns() {
        }
    }

    public static final class UserColumns {
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String EMAIL = "email";

        private UserColumns() {
        }
    }

    public static final class AssetRecordColumns {
        public static final String COLLECTION = "collection";
        public static final String DOCUMENT_ID = "documentId";
        public static final String JSON = "json";
        public static final String IMPORTED_AT = "importedAt";

        private AssetRecordColumns() {
        }
    }

    public static final class Sql {
        public static final String CREATE_ASSET_RECORDS_TABLE =
                "CREATE TABLE IF NOT EXISTS " + Tables.ASSET_RECORDS + " ("
                        + AssetRecordColumns.COLLECTION + " TEXT NOT NULL, "
                        + AssetRecordColumns.DOCUMENT_ID + " TEXT NOT NULL, "
                        + AssetRecordColumns.JSON + " TEXT NOT NULL, "
                        + AssetRecordColumns.IMPORTED_AT + " INTEGER NOT NULL, "
                        + "PRIMARY KEY("
                        + AssetRecordColumns.COLLECTION + ", "
                        + AssetRecordColumns.DOCUMENT_ID
                        + ")"
                        + ")";

        public static final String CREATE_PRODUCTS_TABLE =
                "CREATE TABLE IF NOT EXISTS " + Tables.PRODUCTS + " ("
                        + ProductColumns.ID + " TEXT NOT NULL PRIMARY KEY, "
                        + ProductColumns.NAME + " TEXT, "
                        + ProductColumns.PRICE + " INTEGER NOT NULL, "
                        + ProductColumns.ORIGINAL_PRICE + " INTEGER NOT NULL, "
                        + ProductColumns.IMAGE_URL + " TEXT, "
                        + ProductColumns.WEIGHT + " TEXT, "
                        + ProductColumns.RATING + " REAL NOT NULL, "
                        + ProductColumns.REVIEW_COUNT + " INTEGER NOT NULL, "
                        + ProductColumns.SOLD_COUNT + " INTEGER NOT NULL, "
                        + ProductColumns.DESCRIPTION + " TEXT, "
                        + ProductColumns.ORIGIN + " TEXT, "
                        + ProductColumns.CONDITION + " TEXT, "
                        + ProductColumns.FAT_CONTENT + " TEXT, "
                        + ProductColumns.SKU + " TEXT, "
                        + ProductColumns.CATEGORY_ID + " TEXT, "
                        + ProductColumns.SUBCATEGORY_ID + " TEXT"
                        + ")";

        public static final String CREATE_CART_ITEMS_TABLE =
                "CREATE TABLE IF NOT EXISTS " + Tables.CART_ITEMS + " ("
                        + CartItemColumns.PRODUCT_ID + " TEXT NOT NULL PRIMARY KEY, "
                        + CartItemColumns.QUANTITY + " INTEGER NOT NULL"
                        + ")";

        public static final String CREATE_SEARCH_HISTORY_TABLE =
                "CREATE TABLE IF NOT EXISTS " + Tables.SEARCH_HISTORY + " ("
                        + SearchHistoryColumns.KEYWORD + " TEXT NOT NULL PRIMARY KEY"
                        + ")";

        public static final String CREATE_USERS_TABLE =
                "CREATE TABLE IF NOT EXISTS " + Tables.USERS + " ("
                        + UserColumns.ID + " TEXT NOT NULL PRIMARY KEY, "
                        + UserColumns.NAME + " TEXT, "
                        + UserColumns.EMAIL + " TEXT"
                        + ")";

        public static final String CREATE_RECIPES_TABLE =
                "CREATE TABLE IF NOT EXISTS " + Tables.RECIPES + " ("
                        + "id TEXT NOT NULL PRIMARY KEY, "
                        + "name TEXT, "
                        + "imageUrl TEXT, "
                        + "cookingTime TEXT, "
                        + "price TEXT, "
                        + "rating REAL NOT NULL, "
                        + "reviewCount INTEGER NOT NULL, "
                        + "isBookmarked INTEGER NOT NULL, "
                        + "productId TEXT"
                        + ")";

        public static final String CREATE_REVIEWS_TABLE =
                "CREATE TABLE IF NOT EXISTS " + Tables.REVIEWS + " ("
                        + "id TEXT NOT NULL PRIMARY KEY, "
                        + "productId TEXT, "
                        + "reviewerName TEXT, "
                        + "reviewTime TEXT, "
                        + "rating REAL NOT NULL, "
                        + "content TEXT, "
                        + "avatarUrl TEXT, "
                        + "imageUrls TEXT"
                        + ")";

        public static final String DROP_PRODUCTS_TABLE =
                "DROP TABLE IF EXISTS " + Tables.PRODUCTS;
        public static final String DROP_CART_ITEMS_TABLE =
                "DROP TABLE IF EXISTS " + Tables.CART_ITEMS;
        public static final String DROP_SEARCH_HISTORY_TABLE =
                "DROP TABLE IF EXISTS " + Tables.SEARCH_HISTORY;
        public static final String DROP_USERS_TABLE =
                "DROP TABLE IF EXISTS " + Tables.USERS;
        public static final String DROP_ASSET_RECORDS_TABLE =
                "DROP TABLE IF EXISTS " + Tables.ASSET_RECORDS;

        public static final String SELECT_ALL_PRODUCTS =
                "SELECT * FROM " + Tables.PRODUCTS;
        public static final String SELECT_PRODUCT_BY_ID =
                "SELECT * FROM " + Tables.PRODUCTS
                        + " WHERE " + ProductColumns.ID + " = ? LIMIT 1";
        public static final String SELECT_ALL_CART_ITEMS =
                "SELECT * FROM " + Tables.CART_ITEMS;
        public static final String SELECT_SEARCH_HISTORY =
                "SELECT * FROM " + Tables.SEARCH_HISTORY
                        + " ORDER BY " + SearchHistoryColumns.KEYWORD + " ASC";
        public static final String SELECT_USER_BY_ID =
                "SELECT * FROM " + Tables.USERS
                        + " WHERE " + UserColumns.ID + " = ? LIMIT 1";
        public static final String SELECT_ASSET_RECORDS_BY_COLLECTION =
                "SELECT * FROM " + Tables.ASSET_RECORDS
                        + " WHERE " + AssetRecordColumns.COLLECTION + " = ?";

        public static final String DELETE_ALL_ASSET_RECORDS =
                "DELETE FROM " + Tables.ASSET_RECORDS;
        public static final String DELETE_ALL_PRODUCTS =
                "DELETE FROM " + Tables.PRODUCTS;
        public static final String DELETE_ALL_CART_ITEMS =
                "DELETE FROM " + Tables.CART_ITEMS;
        public static final String DELETE_CART_ITEM_BY_PRODUCT_ID =
                "DELETE FROM " + Tables.CART_ITEMS
                        + " WHERE " + CartItemColumns.PRODUCT_ID + " = ?";
        public static final String DELETE_ALL_SEARCH_HISTORY =
                "DELETE FROM " + Tables.SEARCH_HISTORY;
        public static final String DELETE_SEARCH_HISTORY_BY_KEYWORD =
                "DELETE FROM " + Tables.SEARCH_HISTORY
                        + " WHERE " + SearchHistoryColumns.KEYWORD + " = ?";
        public static final String DELETE_ALL_USERS =
                "DELETE FROM " + Tables.USERS;
        public static final String DELETE_ALL_RECIPES =
                "DELETE FROM " + Tables.RECIPES;
        public static final String DELETE_ALL_REVIEWS =
                "DELETE FROM " + Tables.REVIEWS;
        public static final String UPSERT_ASSET_RECORD =
                "INSERT OR REPLACE INTO " + Tables.ASSET_RECORDS + " ("
                        + AssetRecordColumns.COLLECTION + ", "
                        + AssetRecordColumns.DOCUMENT_ID + ", "
                        + AssetRecordColumns.JSON + ", "
                        + AssetRecordColumns.IMPORTED_AT
                        + ") VALUES (?, ?, ?, ?)";

        public static String assetCollectionTable(String collection) {
            return "asset_" + collection.replace('-', '_');
        }

        public static String createAssetCollectionTable(String collection) {
            return "CREATE TABLE IF NOT EXISTS "
                    + assetCollectionTable(collection)
                    + " (documentId TEXT NOT NULL PRIMARY KEY, json TEXT NOT NULL, importedAt INTEGER NOT NULL)";
        }

        private Sql() {
        }
    }
}
