package com.veggo.app.core.database;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.data.local.entity.AssetRecordEntity;

import java.util.ArrayList;
import java.util.List;

public final class AssetRepository {
    private final Context context;
    private final Gson gson = new Gson();

    public AssetRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public List<AssetModels.Admin> getAdmins() {
        return getAll(AssetFiles.COLLECTION_ADMINS, AssetModels.Admin.class);
    }

    public List<AssetModels.Blog> getBlogs() {
        return getAll(AssetFiles.COLLECTION_BLOGS, AssetModels.Blog.class);
    }

    public List<AssetModels.Category> getCategories() {
        return getAll(AssetFiles.COLLECTION_CATEGORIES, AssetModels.Category.class);
    }

    public List<AssetModels.Certificate> getCertificates() {
        return getAll(AssetFiles.COLLECTION_CERTIFICATES, AssetModels.Certificate.class);
    }

    public List<AssetModels.CommunityPost> getCommunityPosts() {
        return getAll(AssetFiles.COLLECTION_COMMUNITY_POSTS, AssetModels.CommunityPost.class);
    }

    public List<AssetModels.Consultation> getConsultations() {
        return getAll(AssetFiles.COLLECTION_CONSULTATIONS, AssetModels.Consultation.class);
    }

    public List<AssetModels.Dish> getDishes() {
        return getAll(AssetFiles.COLLECTION_DISHES, AssetModels.Dish.class);
    }

    public List<AssetModels.Instruction> getInstructions() {
        return getAll(AssetFiles.COLLECTION_INSTRUCTIONS, AssetModels.Instruction.class);
    }

    public List<AssetModels.Inventory> getInventories() {
        return getAll(AssetFiles.COLLECTION_INVENTORIES, AssetModels.Inventory.class);
    }

    public List<AssetModels.Order> getOrders() {
        return getAll(AssetFiles.COLLECTION_ORDERS, AssetModels.Order.class);
    }

    public List<AssetModels.OrderDetail> getOrderDetails() {
        return getAll(AssetFiles.COLLECTION_ORDER_DETAILS, AssetModels.OrderDetail.class);
    }

    public List<AssetModels.Product> getProducts() {
        return getAll(AssetFiles.COLLECTION_PRODUCTS, AssetModels.Product.class);
    }

    public List<AssetModels.Promotion> getPromotions() {
        return getAll(AssetFiles.COLLECTION_PROMOTIONS, AssetModels.Promotion.class);
    }

    public List<AssetModels.PromotionTarget> getPromotionTargets() {
        return getAll(AssetFiles.COLLECTION_PROMOTION_TARGETS, AssetModels.PromotionTarget.class);
    }

    public List<AssetModels.PromotionUsage> getPromotionUsages() {
        return getAll(AssetFiles.COLLECTION_PROMOTION_USAGES, AssetModels.PromotionUsage.class);
    }

    public List<AssetModels.Reminder> getReminders() {
        return getAll(AssetFiles.COLLECTION_REMINDERS, AssetModels.Reminder.class);
    }

    public List<AssetModels.LocationNode> getLocationTreeRoots() {
        return getAll(AssetFiles.COLLECTION_TREE_COMPLETES, AssetModels.LocationNode.class);
    }

    public List<AssetModels.User> getUsers() {
        return getAll(AssetFiles.COLLECTION_USERS, AssetModels.User.class);
    }

    @NonNull
    public List<UserRawRecord> getAllUsersRaw() {
        List<AssetRecordEntity> records = getRawCollection(AssetFiles.COLLECTION_USERS);
        List<UserRawRecord> rawUsers = new ArrayList<>();
        for (AssetRecordEntity record : records) {
            AssetModels.User user = gson.fromJson(record.getJson(), AssetModels.User.class);
            rawUsers.add(new UserRawRecord(
                    record.getDocumentId(),
                    user != null ? user.phone : null,
                    user != null ? user.password : null,
                    record.getJson(),
                    record.getImportedAt()
            ));
        }
        return rawUsers;
    }

    public List<AssetModels.Warehouse> getWarehouses() {
        return getAll(AssetFiles.COLLECTION_WAREHOUSES, AssetModels.Warehouse.class);
    }

    public <T> List<T> getAll(@NonNull String collection, @NonNull Class<T> modelClass) {
        return AssetDataStore.getAll(context, collection, modelClass);
    }

    public <T> T getById(@NonNull String collection, @NonNull String documentId, @NonNull Class<T> modelClass) {
        return AssetDataStore.getById(context, collection, documentId, modelClass);
    }

    public AssetRecordEntity getRawById(@NonNull String collection, @NonNull String documentId) {
        return AssetDataStore.getById(context, collection, documentId);
    }

    public List<AssetRecordEntity> getRawCollection(@NonNull String collection) {
        return AssetDataStore.getAll(context, collection);
    }

    public <T> void upsert(@NonNull String collection, @NonNull String documentId, @NonNull T model) {
        AssetDataStore.upsert(context, collection, documentId, model);
    }

    public void upsertRaw(@NonNull String collection, @NonNull String documentId, @NonNull String json) {
        AssetDataStore.upsert(context, collection, documentId, json);
    }

    public int deleteById(@NonNull String collection, @NonNull String documentId) {
        return AssetDataStore.deleteById(context, collection, documentId);
    }

    public int deleteCollection(@NonNull String collection) {
        return AssetDataStore.deleteCollection(context, collection);
    }

    public void clearAll() {
        AssetDataStore.clearAll(context);
    }

    public String toJson(@NonNull Object model) {
        return gson.toJson(model);
    }

    public static final class UserRawRecord {
        private final String documentId;
        private final String phone;
        private final String password;
        private final String json;
        private final long importedAt;

        public UserRawRecord(String documentId, String phone, String password, String json, long importedAt) {
            this.documentId = documentId;
            this.phone = phone;
            this.password = password;
            this.json = json;
            this.importedAt = importedAt;
        }

        public String getDocumentId() {
            return documentId;
        }

        public String getPhone() {
            return phone;
        }

        public String getPassword() {
            return password;
        }

        public String getJson() {
            return json;
        }

        public long getImportedAt() {
            return importedAt;
        }
    }
}
