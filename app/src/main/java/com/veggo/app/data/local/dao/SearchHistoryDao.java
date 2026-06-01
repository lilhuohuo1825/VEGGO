package com.veggo.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veggo.app.data.local.entity.SearchHistoryEntity;

import java.util.List;

@Dao
public interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY keyword ASC")
    LiveData<List<SearchHistoryEntity>> observeSearchHistory();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SearchHistoryEntity item);

    @Query("DELETE FROM search_history WHERE keyword = :keyword")
    void deleteByKeyword(String keyword);

    @Query("DELETE FROM search_history")
    void clearAll();
}
