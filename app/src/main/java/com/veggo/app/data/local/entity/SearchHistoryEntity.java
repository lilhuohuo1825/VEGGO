package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "search_history")
public class SearchHistoryEntity {
    @PrimaryKey
    @NonNull
    private String keyword;

    public SearchHistoryEntity(@NonNull String keyword) {
        this.keyword = keyword;
    }

    @NonNull public String getKeyword() { return keyword; }
    public void setKeyword(@NonNull String keyword) { this.keyword = keyword; }
}
