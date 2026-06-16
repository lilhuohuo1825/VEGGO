package com.veggo.app.core.database;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteStatement;

import com.google.gson.Gson;
import com.veggo.app.assets.AssetFiles;
import com.veggo.app.data.local.entity.AssetRecordEntity;

import java.util.ArrayList;
import java.util.List;

public final class AssetDataStore {
    private static final String TABLE_PREFIX = "asset_";
    private static final Gson GSON = new Gson();

    private AssetDataStore() {
    }

    public static void createAllTables(@NonNull Context context) {
        SupportSQLiteDatabase database = DatabaseManager.getWritableDatabase(context);
        database.beginTransaction();
        try {
            for (String collection : AssetFiles.COLLECTIONS) {
                database.execSQL(createTableSql(collection));
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public static void replaceAll(@NonNull Context context, @NonNull List<AssetRecordEntity> records) {
        SupportSQLiteDatabase database = DatabaseManager.getWritableDatabase(context);
        database.beginTransaction();
        try {
            for (String collection : AssetFiles.COLLECTIONS) {
                database.execSQL(createTableSql(collection));
                database.execSQL("DELETE FROM " + tableName(collection));
            }

            String currentTable = null;
            SupportSQLiteStatement statement = null;
            for (AssetRecordEntity record : records) {
                String table = tableName(record.getCollection());
                if (!table.equals(currentTable)) {
                    if (statement != null) {
                        closeQuietly(statement);
                    }
                    statement = database.compileStatement(
                            "INSERT OR REPLACE INTO "
                                    + table
                                    + " (documentId, json, importedAt) VALUES (?, ?, ?)"
                    );
                    currentTable = table;
                }
                bindAndInsert(statement, record);
            }
            if (statement != null) {
                closeQuietly(statement);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public static List<AssetRecordEntity> getAll(@NonNull Context context, @NonNull String collection) {
        createTable(context, collection);
        List<AssetRecordEntity> records = new ArrayList<>();
        SupportSQLiteDatabase database = DatabaseManager.getWritableDatabase(context);
        Cursor cursor = database.query("SELECT documentId, json, importedAt FROM "
                + tableName(collection)
                + " ORDER BY documentId ASC");
        try {
            if (cursor instanceof android.database.AbstractWindowedCursor) {
                android.database.CursorWindow window = new android.database.CursorWindow("large_window", 32 * 1024 * 1024); // 32MB window size
                ((android.database.AbstractWindowedCursor) cursor).setWindow(window);
            }
            while (cursor.moveToNext()) {
                records.add(new AssetRecordEntity(
                        collection,
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getLong(2)
                ));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return records;
    }

    public static <T> List<T> getAll(@NonNull Context context, @NonNull String collection, @NonNull Class<T> modelClass) {
        List<AssetRecordEntity> records = getAll(context, collection);
        List<T> models = new ArrayList<>();
        for (AssetRecordEntity record : records) {
            models.add(GSON.fromJson(record.getJson(), modelClass));
        }
        return models;
    }

    public static AssetRecordEntity getById(
            @NonNull Context context,
            @NonNull String collection,
            @NonNull String documentId
    ) {
        createTable(context, collection);
        SupportSQLiteDatabase database = DatabaseManager.getWritableDatabase(context);
        try (Cursor cursor = database.query(
                "SELECT documentId, json, importedAt FROM "
                        + tableName(collection)
                        + " WHERE documentId = ? LIMIT 1",
                new Object[]{documentId}
        )) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return new AssetRecordEntity(collection, cursor.getString(0), cursor.getString(1), cursor.getLong(2));
        }
    }

    public static <T> T getById(
            @NonNull Context context,
            @NonNull String collection,
            @NonNull String documentId,
            @NonNull Class<T> modelClass
    ) {
        AssetRecordEntity record = getById(context, collection, documentId);
        if (record == null) {
            return null;
        }
        return GSON.fromJson(record.getJson(), modelClass);
    }

    public static void upsert(
            @NonNull Context context,
            @NonNull String collection,
            @NonNull String documentId,
            @NonNull String json
    ) {
        createTable(context, collection);
        long importedAt = System.currentTimeMillis();
        VeggoDatabase roomDatabase = VeggoDatabase.getInstance(context);
        roomDatabase.runInTransaction(() -> {
            roomDatabase.assetRecordDao().upsert(new AssetRecordEntity(collection, documentId, json, importedAt));
            SupportSQLiteStatement statement = roomDatabase.getOpenHelper().getWritableDatabase().compileStatement(
                    "INSERT OR REPLACE INTO "
                            + tableName(collection)
                            + " (documentId, json, importedAt) VALUES (?, ?, ?)"
            );
            try {
                statement.bindString(1, documentId);
                statement.bindString(2, json);
                statement.bindLong(3, importedAt);
                statement.executeInsert();
            } finally {
                closeQuietly(statement);
            }
        });
    }

    public static <T> void upsert(
            @NonNull Context context,
            @NonNull String collection,
            @NonNull String documentId,
            @NonNull T model
    ) {
        upsert(context, collection, documentId, GSON.toJson(model));
    }

    public static int deleteById(
            @NonNull Context context,
            @NonNull String collection,
            @NonNull String documentId
    ) {
        createTable(context, collection);
        VeggoDatabase roomDatabase = VeggoDatabase.getInstance(context);
        final int[] deleted = {0};
        roomDatabase.runInTransaction(() -> {
            deleted[0] = roomDatabase.assetRecordDao().deleteById(collection, documentId);
            SupportSQLiteStatement statement = roomDatabase.getOpenHelper().getWritableDatabase().compileStatement(
                    "DELETE FROM " + tableName(collection) + " WHERE documentId = ?"
            );
            try {
                statement.bindString(1, documentId);
                statement.executeUpdateDelete();
            } finally {
                closeQuietly(statement);
            }
        });
        return deleted[0];
    }

    public static int deleteCollection(@NonNull Context context, @NonNull String collection) {
        createTable(context, collection);
        VeggoDatabase roomDatabase = VeggoDatabase.getInstance(context);
        final int[] deleted = {0};
        roomDatabase.runInTransaction(() -> {
            deleted[0] = roomDatabase.assetRecordDao().deleteByCollection(collection);
            roomDatabase.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM " + tableName(collection));
        });
        return deleted[0];
    }

    public static void clearAll(@NonNull Context context) {
        VeggoDatabase roomDatabase = VeggoDatabase.getInstance(context);
        roomDatabase.runInTransaction(() -> {
            roomDatabase.assetRecordDao().clearAll();
            SupportSQLiteDatabase database = roomDatabase.getOpenHelper().getWritableDatabase();
            for (String collection : AssetFiles.COLLECTIONS) {
                database.execSQL(createTableSql(collection));
                database.execSQL("DELETE FROM " + tableName(collection));
            }
        });
    }

    private static void createTable(@NonNull Context context, @NonNull String collection) {
        DatabaseManager.getWritableDatabase(context).execSQL(createTableSql(collection));
    }

    private static void bindAndInsert(SupportSQLiteStatement statement, AssetRecordEntity record) {
        statement.clearBindings();
        statement.bindString(1, record.getDocumentId());
        statement.bindString(2, record.getJson());
        statement.bindLong(3, record.getImportedAt());
        statement.executeInsert();
    }

    private static String createTableSql(String collection) {
        return "CREATE TABLE IF NOT EXISTS "
                + tableName(collection)
                + " (documentId TEXT NOT NULL PRIMARY KEY, json TEXT NOT NULL, importedAt INTEGER NOT NULL)";
    }

    private static String tableName(String collection) {
        return TABLE_PREFIX + collection.replace('-', '_');
    }

    private static void closeQuietly(SupportSQLiteStatement statement) {
        try {
            statement.close();
        } catch (Exception ignored) {
        }
    }
}
