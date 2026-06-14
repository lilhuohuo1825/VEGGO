package com.veggo.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veggo.app.data.local.entity.BlogEntity;

import java.util.List;

@Dao
public interface BlogDao {
    @Query("SELECT COUNT(*) FROM blogs")
    int count();

    @Query("SELECT * FROM blogs ORDER BY publishedAtMillis DESC, id DESC")
    List<BlogEntity> getAll();

    @Query("SELECT * FROM blogs ORDER BY publishedAtMillis DESC, id DESC LIMIT :limit")
    List<BlogEntity> getLatest(int limit);

    @Query("SELECT * FROM blogs WHERE id = :blogId LIMIT 1")
    BlogEntity getById(String blogId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BlogEntity> blogs);

    @Query("DELETE FROM blogs")
    void clearAll();
}
