package com.veggo.app.domain.repository;

import androidx.annotation.Nullable;

import com.veggo.app.data.remote.dto.UserProfileDto;

import java.io.File;

public interface UserRepository {
    interface Callback<T> {
        void onSuccess(T result);

        void onError(Throwable t);
    }

    void updateProfile(
            String currentPhone,
            String name,
            String phone,
            String email,
            String birthday,
            String gender,
            @Nullable File avatarFile,
            Callback<UserProfileDto> callback
    );
}
