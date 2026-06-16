package com.veggo.app.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.data.repository.AuthRepositoryImpl;
import com.veggo.app.domain.repository.AuthRepository;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> getLoading() { return _loading; }

    private final MutableLiveData<UserDto> _user = new MutableLiveData<>();
    public LiveData<UserDto> getUser() { return _user; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    private final MutableLiveData<Boolean> _forgotPasswordSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getForgotPasswordSuccess() { return _forgotPasswordSuccess; }

    private final MutableLiveData<Boolean> _resetPasswordSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getResetPasswordSuccess() { return _resetPasswordSuccess; }

    public AuthViewModel() {
        this.authRepository = new AuthRepositoryImpl();
    }

    public void login(String phone, String password) {
        _loading.setValue(true);
        authRepository.login(phone, password, new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                _loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    _user.setValue(response.body());
                } else {
                    _error.setValue("Số điện thoại hoặc mật khẩu không đúng");
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                _loading.setValue(false);
                _error.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void register(String phone, String password, String fullName, String email) {
        _loading.setValue(true);
        authRepository.register(phone, password, fullName, email, new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                _loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    _user.setValue(response.body());
                } else {
                    try {
                        String errorMsg = "Đăng ký thất bại";
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            if (errorJson.contains("already exists")) {
                                errorMsg = "Số điện thoại đã được đăng ký";
                            }
                        }
                        _error.setValue(errorMsg);
                    } catch (Exception e) {
                        _error.setValue("Đăng ký thất bại");
                    }
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                _loading.setValue(false);
                _error.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void forgotPassword(String phone) {
        _loading.setValue(true);
        authRepository.forgotPassword(phone, new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                _loading.setValue(false);
                if (response.isSuccessful()) {
                    _forgotPasswordSuccess.setValue(true);
                } else {
                    _error.setValue("Số điện thoại chưa được đăng ký");
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                _loading.setValue(false);
                _error.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void resetPassword(String phone, String otp, String newPassword) {
        _loading.setValue(true);
        authRepository.resetPassword(phone, otp, newPassword, new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                _loading.setValue(false);
                if (response.isSuccessful()) {
                    _resetPasswordSuccess.setValue(true);
                } else {
                    _error.setValue("Mã xác thực không đúng hoặc đã hết hạn");
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                _loading.setValue(false);
                _error.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }
}
