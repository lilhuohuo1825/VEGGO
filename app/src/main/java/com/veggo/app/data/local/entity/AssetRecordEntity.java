package com.veggo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "asset_records", primaryKeys = {"collection", "documentId"})
public class AssetRecordEntity {
    @NonNull
    private String collection;
    @NonNull
    private String documentId;
    @NonNull
    private String json;
    private long importedAt;

    public AssetRecordEntity(
            @NonNull String collection,
            @NonNull String documentId,
            @NonNull String json,
            long importedAt
    ) {
        this.collection = collection;
        this.documentId = documentId;
        this.json = json;
        this.importedAt = importedAt;
    }

    @NonNull
    public String getCollection() {
        return collection;
    }

    public void setCollection(@NonNull String collection) {
        this.collection = collection;
    }

    @NonNull
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(@NonNull String documentId) {
        this.documentId = documentId;
    }

    @NonNull
    public String getJson() {
        return json;
    }

    public void setJson(@NonNull String json) {
        this.json = json;
    }

    public long getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(long importedAt) {
        this.importedAt = importedAt;
    }
}
