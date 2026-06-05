package com.veggo.app.core.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.veggo.app.data.local.dao.AssetRecordDao;
import com.veggo.app.data.local.dao.CartDao;
import com.veggo.app.data.local.dao.ProductDao;
import com.veggo.app.data.local.dao.SearchHistoryDao;
import com.veggo.app.data.local.dao.UserDao;
import com.veggo.app.data.local.entity.AssetRecordEntity;
import com.veggo.app.data.local.entity.CartItemEntity;
import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.local.entity.SearchHistoryEntity;
import com.veggo.app.data.local.entity.UserEntity;

@Database(
        entities = {
                ProductEntity.class,
                CartItemEntity.class,
                SearchHistoryEntity.class,
                UserEntity.class,
                AssetRecordEntity.class
        },
        version = DatabaseManager.DATABASE_VERSION,
        exportSchema = true
)
public abstract class VeggoDatabase extends RoomDatabase {
    private static volatile VeggoDatabase instance;

    public abstract ProductDao productDao();
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
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return instance;
    }
}
