package com.veggo.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veggo.app.data.local.entity.UserEntity;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    LiveData<UserEntity> observeUserById(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(UserEntity user);

    @Query("DELETE FROM users")
    void clearAll();
}
