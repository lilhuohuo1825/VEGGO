package com.veggo.app.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.data.repository.AuthRepositoryImpl;
import com.veggo.app.domain.repository.AuthRepository;
import com.veggo.app.domain.usecase.auth.FacebookLoginUseCase;
import com.veggo.app.domain.usecase.auth.GoogleLoginUseCase;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository;
    private final GoogleLoginUseCase googleLoginUseCase;
    private final FacebookLoginUseCase facebookLoginUseCase;

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>(false);
    public LiveData<Boolean> getLoading() { return _loading; }

    private final MutableLiveData<UserDto> _user = new MutableLiveData<>();
    public LiveData<UserDto> getUser() { return _user; }

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> getError() { return _error; }

    private final MutableLiveData<Boolean> _forgotPasswordSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getForgotPasswordSuccess() { return _forgotPasswordSuccess; }

    private final MutableLiveData<String> _forgotOtp = new MutableLiveData<>();
    public LiveData<String> getForgotOtp() { return _forgotOtp; }

    private final MutableLiveData<Boolean> _resetPasswordSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getResetPasswordSuccess() { return _resetPasswordSuccess; }

    private final MutableLiveData<Boolean> _verifyForgotOtpSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getVerifyForgotOtpSuccess() { return _verifyForgotOtpSuccess; }

    public AuthViewModel() {
        this.authRepository = new AuthRepositoryImpl();
        this.googleLoginUseCase = new GoogleLoginUseCase(authRepository);
        this.facebookLoginUseCase = new FacebookLoginUseCase(authRepository);
    }

    public void resetLoading() {
        _loading.setValue(false);
    }

    public void googleLogin(String idToken) {
        _loading.setValue(true);
        googleLoginUseCase.execute(idToken, new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                _loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    _user.setValue(response.body());
                } else {
                    handleError(response);
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                _loading.setValue(false);
                _error.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    public void facebookLogin(String idToken) {
        facebookLogin(idToken, null);
    }

    public void facebookLogin(String idToken, String avatarUrl) {
        _loading.setValue(true);
        facebookLoginUseCase.execute(idToken, avatarUrl, new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                _loading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    _user.setValue(response.body());
                } else {
                    handleError(response);
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                _loading.setValue(false);
                _error.setValue("Lỗi kết nối: " + t.getMessage());
            }
        });
    }

    private void handleError(Response<UserDto> response) {
        String errorMsg = "Đăng nhập thất bại";
        if (response.errorBody() != null) {
            try {
                String errorJson = response.errorBody().string();
                org.json.JSONObject jsonObj = new org.json.JSONObject(errorJson);
                if (jsonObj.has("message")) {
                    errorMsg = jsonObj.getString("message");
                }
            } catch (Exception e) {
                // ignore
            }
        }
        _error.setValue(errorMsg);
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
                    handleError(response);
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
                            try {
                                org.json.JSONObject jsonObj = new org.json.JSONObject(errorJson);
                                if (jsonObj.has("message")) {
                                    errorMsg = jsonObj.getString("message");
                                }
                            } catch (Exception e) {
                                if (errorJson.contains("already exists") || errorJson.contains("đăng ký")) {
                                    errorMsg = "Số điện thoại đã được đăng ký";
                                }
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
                    if (response.body() != null) {
                        _forgotOtp.setValue(response.body().get("otp"));
                    }
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

    private void handleMapError(Response<Map<String, String>> response, String fallback) {
        String errorMsg = fallback;
        if (response.errorBody() != null) {
            try {
                String errorJson = response.errorBody().string();
                org.json.JSONObject jsonObj = new org.json.JSONObject(errorJson);
                if (jsonObj.has("message")) {
                    errorMsg = jsonObj.getString("message");
                }
            } catch (Exception ignored) {
            }
        }
        _error.setValue(errorMsg);
    }

    public void verifyForgotPasswordOtp(String phone, String otp) {
        _loading.setValue(true);
        authRepository.verifyForgotPasswordOtp(phone, otp, new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                _loading.setValue(false);
                if (response.isSuccessful()) {
                    _verifyForgotOtpSuccess.setValue(true);
                } else {
                    handleMapError(response, "Mã xác thực không đúng hoặc đã hết hạn");
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
                    handleMapError(response, "Mã xác thực không đúng hoặc đã hết hạn");
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
