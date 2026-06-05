package com.veggo.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veggo.app.data.local.entity.AssetRecordEntity;
import com.veggo.app.data.local.model.AssetCollectionCount;

import java.util.List;

@Dao
public interface AssetRecordDao {
    @Query("SELECT collection, COUNT(*) AS count FROM asset_records GROUP BY collection ORDER BY collection ASC")
    LiveData<List<AssetCollectionCount>> observeCollectionCounts();

    @Query("SELECT * FROM asset_records WHERE collection = :collection ORDER BY documentId ASC")
    LiveData<List<AssetRecordEntity>> observeByCollection(String collection);

    @Query("SELECT * FROM asset_records WHERE collection = :collection AND documentId = :documentId LIMIT 1")
    AssetRecordEntity getById(String collection, String documentId);

    @Query("SELECT * FROM asset_records WHERE collection = :collection ORDER BY documentId ASC")
    List<AssetRecordEntity> getByCollection(String collection);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AssetRecordEntity> records);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(AssetRecordEntity record);

    @Query("DELETE FROM asset_records WHERE collection = :collection AND documentId = :documentId")
    int deleteById(String collection, String documentId);

    @Query("DELETE FROM asset_records WHERE collection = :collection")
    int deleteByCollection(String collection);

    @Query("DELETE FROM asset_records")
    void clearAll();
}
