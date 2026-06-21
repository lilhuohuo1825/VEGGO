package com.veggo.app.presentation.profile;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.network.ApiHttpException;
import com.veggo.app.data.remote.dto.UserProfileDto;
import com.veggo.app.data.repository.UserRepositoryImpl;
import com.veggo.app.domain.repository.UserRepository;

import java.io.File;

public class PersonalInfoViewModel extends ViewModel {
    private static final String PHONE_REGEX = "^0\\d{9}$";
    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    private final UserRepository userRepository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<UserProfileDto> profile = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> retryableError = new MutableLiveData<>(false);

    public PersonalInfoViewModel() {
        userRepository = new UserRepositoryImpl(ApiClient.createService(
                com.veggo.app.data.remote.api.UserApi.class));
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<UserProfileDto> getProfile() {
        return profile;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getRetryableError() {
        return retryableError;
    }

    public void updateProfile(
            String currentPhone,
            String name,
            String phone,
            String email,
            @Nullable File avatarFile
    ) {
        String validationError = validate(name, phone, email);
        if (validationError != null) {
            retryableError.setValue(false);
            error.setValue(validationError);
            return;
        }

        loading.setValue(true);
        userRepository.updateProfile(
                currentPhone,
                name.trim(),
                phone.trim(),
                email == null ? "" : email.trim(),
                avatarFile,
                new UserRepository.Callback<UserProfileDto>() {
                    @Override
                    public void onSuccess(UserProfileDto result) {
                        loading.setValue(false);
                        retryableError.setValue(false);
                        profile.setValue(result);
                    }

                    @Override
                    public void onError(Throwable t) {
                        loading.setValue(false);
                        if (t instanceof ApiHttpException) {
                            retryableError.setValue(false);
                            error.setValue(t.getMessage());
                            return;
                        }
                        retryableError.setValue(true);
                        error.setValue("Lỗi kết nối. Vui lòng thử lại.");
                    }
                }
        );
    }

    @Nullable
    private String validate(String name, String phone, String email) {
        if (name == null || name.trim().isEmpty()) {
            return "Họ và tên không được để trống";
        }
        if (phone == null || !phone.trim().matches(PHONE_REGEX)) {
            return "Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số";
        }
        if (email != null && !email.trim().isEmpty() && !email.trim().matches(EMAIL_REGEX)) {
            return "Email không hợp lệ";
        }
        return null;
    }
}
