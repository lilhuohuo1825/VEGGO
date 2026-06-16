package com.veggo.app.presentation.auth;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.veggo.app.assets.AssetFiles;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.database.AssetRepository;
import com.veggo.app.core.database.AssetDatabaseSeeder;
import com.veggo.app.presentation.auth.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class UserAssetRepository {
    private static final String TAG = "UserAssetRepository";
    private final Context context;
    private final Gson gson;
    private final AssetRepository assetRepository;

    public UserAssetRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        AssetDatabaseSeeder.seedIfNeededBlocking(this.context);
        this.gson = new Gson();
        this.assetRepository = new AssetRepository(this.context);
    }

    @NonNull
    public List<User> getUsers() {
        List<UserRecord> sqliteUsers = getUserRecords();
        List<User> mappedUsers = new ArrayList<>();
        for (UserRecord sqliteUser : sqliteUsers) {
            mappedUsers.add(mapToUser(sqliteUser.user));
        }
        return mappedUsers;
    }

    @Nullable
    public User findUserByPhone(@Nullable String phone) {
        UserRecord userRecord = findBestUserRecordByPhone(phone);
        return userRecord != null ? mapToUser(userRecord.user) : null;
    }

    @NonNull
    public List<AssetRepository.UserRawRecord> getAllUsersRaw() {
        return assetRepository.getAllUsersRaw();
    }

    public int countUsersByPhone(@Nullable String phone) {
        String normalizedPhone = normalizePhone(phone);
        if (normalizedPhone == null) {
            return 0;
        }
        int count = 0;
        for (UserRecord record : getUserRecords()) {
            if (normalizedPhone.equals(normalizePhone(record.user.phone))) {
                count++;
            }
        }
        return count;
    }

    public void logAllUsersRaw(@NonNull String reason) {
        List<AssetRepository.UserRawRecord> records = getAllUsersRaw();
        Log.d(TAG, "asset_users dump [" + reason + "] count=" + records.size());
        for (AssetRepository.UserRawRecord record : records) {
            Log.d(
                    TAG,
                    String.format(
                            Locale.US,
                            "asset_users row documentId=%s, Phone=%s, Password=%s, importedAt=%d",
                            record.getDocumentId(),
                            safeValue(record.getPhone()),
                            safeValue(record.getPassword()),
                            record.getImportedAt()
                    )
            );
        }
    }

    @NonNull
    private User mapToUser(@NonNull AssetModels.User source) {
        return gson.fromJson(gson.toJson(source), User.class);
    }

    @Nullable
    private UserRecord findBestUserRecordByPhone(@Nullable String phone) {
        String normalizedPhone = normalizePhone(phone);
        if (normalizedPhone == null) {
            return null;
        }

        List<UserRecord> matches = new ArrayList<>();
        for (UserRecord record : getUserRecords()) {
            if (normalizedPhone.equals(normalizePhone(record.user.phone))) {
                matches.add(record);
            }
        }

        if (matches.isEmpty()) {
            return null;
        }

        Collections.sort(matches, Comparator.comparingLong(UserRecord::getImportedAt).reversed());
        if (matches.size() > 1) {
            Log.w(TAG, "Found " + matches.size() + " records with the same phone " + normalizedPhone
                    + ". Using newest importedAt record: " + matches.get(0).documentId);
        }
        return matches.get(0);
    }

    @NonNull
    private List<UserRecord> getUserRecords() {
        List<UserRecord> userRecords = new ArrayList<>();
        for (AssetRepository.UserRawRecord rawRecord : assetRepository.getAllUsersRaw()) {
            AssetModels.User user = gson.fromJson(rawRecord.getJson(), AssetModels.User.class);
            if (user == null) {
                user = new AssetModels.User();
                user.phone = rawRecord.getPhone();
                user.password = rawRecord.getPassword();
            }
            userRecords.add(new UserRecord(
                    rawRecord.getDocumentId(),
                    user,
                    rawRecord.getImportedAt()
            ));
        }
        return userRecords;
    }

    @Nullable
    private String normalizePhone(@Nullable String phone) {
        if (phone == null) {
            return null;
        }
        String trimmedPhone = phone.trim();
        return trimmedPhone.isEmpty() ? null : trimmedPhone;
    }

    @NonNull
    private String safeValue(@Nullable String value) {
        return value == null ? "null" : value.trim();
    }

    private static final class UserRecord {
        private final String documentId;
        private final AssetModels.User user;
        private final long importedAt;

        private UserRecord(@NonNull String documentId, @NonNull AssetModels.User user, long importedAt) {
            this.documentId = documentId;
            this.user = user;
            this.importedAt = importedAt;
        }

        private long getImportedAt() {
            return importedAt;
        }
    }
}
