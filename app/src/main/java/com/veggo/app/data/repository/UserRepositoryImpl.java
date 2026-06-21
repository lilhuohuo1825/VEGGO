package com.veggo.app.data.repository;

import androidx.annotation.Nullable;

import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.data.remote.api.UserApi;
import com.veggo.app.data.remote.dto.UserProfileDto;
import com.veggo.app.domain.repository.UserRepository;

import org.json.JSONObject;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

public class UserRepositoryImpl implements UserRepository {
    private static final MediaType TEXT_PLAIN = MediaType.parse("text/plain");
    private static final MediaType IMAGE_JPEG = MediaType.parse("image/jpeg");

    private final UserApi userApi;

    public UserRepositoryImpl(UserApi userApi) {
        this.userApi = userApi;
    }

    @Override
    public void updateProfile(
            String currentPhone,
            String name,
            String phone,
            String email,
            @Nullable File avatarFile,
            Callback<UserProfileDto> callback
    ) {
        RequestBody nameBody = RequestBody.create(name, TEXT_PLAIN);
        RequestBody phoneBody = RequestBody.create(phone, TEXT_PLAIN);
        RequestBody emailBody = RequestBody.create(email == null ? "" : email, TEXT_PLAIN);

        MultipartBody.Part avatarPart = null;
        if (avatarFile != null) {
            RequestBody fileBody = RequestBody.create(avatarFile, IMAGE_JPEG);
            avatarPart = MultipartBody.Part.createFormData("avatar", avatarFile.getName(), fileBody);
        }

        userApi.updateProfile(currentPhone, nameBody, phoneBody, emailBody, avatarPart)
                .enqueue(new retrofit2.Callback<UserProfileDto>() {
                    @Override
                    public void onResponse(Call<UserProfileDto> call, Response<UserProfileDto> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                            return;
                        }
                        callback.onError(new ApiHttpException(response.code(), parseErrorMessage(response)));
                    }

                    @Override
                    public void onFailure(Call<UserProfileDto> call, Throwable t) {
                        callback.onError(t);
                    }
                });
    }

    private static String parseErrorMessage(Response<?> response) {
        String fallback = "Không thể cập nhật thông tin cá nhân";
        if (response.code() == 413) {
            return "Ảnh đại diện quá lớn (tối đa 5MB)";
        }
        if (response.errorBody() == null) {
            return fallback;
        }
        try {
            String raw = response.errorBody().string();
            JSONObject json = new JSONObject(raw);
            if (json.has("message")) {
                return json.getString("message");
            }
            return raw;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
