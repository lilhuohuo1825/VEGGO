package com.veggo.app.presentation.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.notification.EmulatorSmsSender;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.data.remote.api.UserApi;
import com.veggo.app.data.remote.dto.UserDto;
import com.veggo.app.presentation.checkout.PendingCheckoutStore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseActivity {
    private static final String TAG = "RegisterActivity";

    private AuthViewModel authViewModel;
    private LinearLayout layoutRegisterForm;
    private LinearLayout layoutVerifyForm;
    private EditText edtPhone;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private TextView tvPhoneError;
    private TextView tvPasswordError;
    private TextView tvConfirmPasswordError;
    private TextView tvVerifyPhoneDescription;
    private ImageView imgEyePassword;
    private ImageView imgEyeConfirmPassword;
    private ImageView cbPolicy;
    private boolean isPolicyAccepted = true;
    private boolean isPasswordVisible;
    private boolean isConfirmPasswordVisible;
    private String pendingPhone;
    private String pendingPassword;
    private String currentOtp;
    private long otpExpiresAt;
    private int otpFailedAttempts;
    private CountDownTimer otpTimer;
    private String checkedPhone = "";
    private boolean checkedPhoneExists;
    private boolean checkingPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        AuthFormUtils.setupAuthScreen(this);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        setupActions();
        observeViewModel();
        showRegisterForm();
    }

    @Override
    protected void onDestroy() {
        if (otpTimer != null) {
            otpTimer.cancel();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EmulatorSmsSender.handlePermissionResult(this, requestCode, grantResults);
    }

    private void initViews() {
        layoutRegisterForm = findViewById(R.id.layoutRegisterForm);
        layoutVerifyForm = findViewById(R.id.layoutVerifyForm);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        tvPhoneError = findViewById(R.id.tvPhoneError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError);
        tvVerifyPhoneDescription = findViewById(R.id.tvVerifyPhoneDescription);
        imgEyePassword = findViewById(R.id.imgEyePassword);
        imgEyeConfirmPassword = findViewById(R.id.imgEyeConfirmPassword);
        cbPolicy = findViewById(R.id.cbPolicy);

        tvPhoneError.setVisibility(View.GONE);
        tvPasswordError.setVisibility(View.GONE);
        tvConfirmPasswordError.setVisibility(View.GONE);
        updatePolicyIcon();
    }

    private void setupActions() {
        findViewById(R.id.imgBack).setOnClickListener(v -> finish());
        findViewById(R.id.imgBackVerify).setOnClickListener(v -> showRegisterForm());
        imgEyePassword.setOnClickListener(v -> togglePasswordVisibility(edtPassword, imgEyePassword, true));
        imgEyeConfirmPassword.setOnClickListener(v -> togglePasswordVisibility(edtConfirmPassword, imgEyeConfirmPassword, false));
        cbPolicy.setOnClickListener(v -> {
            isPolicyAccepted = !isPolicyAccepted;
            updatePolicyIcon();
        });
        findViewById(R.id.tvPolicy).setOnClickListener(v -> {
            isPolicyAccepted = !isPolicyAccepted;
            updatePolicyIcon();
        });
        findViewById(R.id.btnRegister).setOnClickListener(v -> handleRegister());
        findViewById(R.id.btnVerify).setOnClickListener(v -> handleVerifyRegister());
        findViewById(R.id.tvResendCode).setOnClickListener(v -> sendRegisterOtp());
        edtPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                checkRegisterPhoneAvailability(edtPhone.getText().toString().trim(), false);
            }
        });
        edtPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                validatePassword(s.toString());
                validateConfirmPassword(s.toString(), edtConfirmPassword.getText().toString());
            }
        });
        edtConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                validateConfirmPassword(edtPassword.getText().toString(), s.toString());
            }
        });
        AuthFormUtils.wireOtpFields(getRegisterOtpFields(), this::handleVerifyRegister);
    }

    private void handleRegister() {
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        boolean isPhoneValid = validatePhone(phone);
        boolean isPasswordValid = validatePassword(password);
        boolean isConfirmValid = validateConfirmPassword(password, confirmPassword);
        boolean isPolicyValid = validatePolicy();

        if (!isPhoneValid || !isPasswordValid || !isConfirmValid || !isPolicyValid) {
            return;
        }
        if (checkingPhone) {
            Toast.makeText(this, "Đang kiểm tra số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!phone.equals(checkedPhone)) {
            checkRegisterPhoneAvailability(phone, true);
            return;
        }
        if (checkedPhoneExists) {
            AuthFormUtils.showError(tvPhoneError, "Số điện thoại này đã được đăng ký");
            return;
        }

        pendingPhone = phone;
        pendingPassword = password;
        sendRegisterOtp();
    }

    private void checkRegisterPhoneAvailability(String phone, boolean continueAfterAvailable) {
        if (!validatePhone(phone)) {
            return;
        }
        checkingPhone = true;
        ApiClient.createService(UserApi.class).getUserByPhone(phone).enqueue(new Callback<UserDto>() {
            @Override
            public void onResponse(Call<UserDto> call, Response<UserDto> response) {
                checkingPhone = false;
                checkedPhone = phone;
                checkedPhoneExists = response.isSuccessful() && response.body() != null;
                if (checkedPhoneExists) {
                    AuthFormUtils.showError(tvPhoneError, "Số điện thoại này đã được đăng ký");
                    return;
                }
                AuthFormUtils.showError(tvPhoneError, "");
                if (continueAfterAvailable) {
                    handleRegister();
                }
            }

            @Override
            public void onFailure(Call<UserDto> call, Throwable t) {
                checkingPhone = false;
                checkedPhone = phone;
                checkedPhoneExists = false;
                AuthFormUtils.showError(tvPhoneError, "");
                if (continueAfterAvailable) {
                    handleRegister();
                }
            }
        });
    }

    private void sendRegisterOtp() {
        currentOtp = AuthFormUtils.randomOtp();
        otpExpiresAt = System.currentTimeMillis() + AuthFormUtils.OTP_TTL_MS;
        otpFailedAttempts = 0;
        tvVerifyPhoneDescription.setText("Chúng tôi đã gửi mã xác thực đến số điện thoại "
                + maskPhone(pendingPhone) + ". Mã có hiệu lực trong 60 giây.");
        clearOtpFields(getRegisterOtpFields());
        showVerifyForm();
        getRegisterOtpFields()[0].requestFocus();
        if (otpTimer != null) {
            otpTimer.cancel();
        }
        otpTimer = new CountDownTimer(AuthFormUtils.OTP_TTL_MS, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                updateRegisterOtpDescription(millisUntilFinished);
            }
            @Override public void onFinish() {
                updateRegisterOtpDescription(0);
                Toast.makeText(RegisterActivity.this, "Mã xác thực đã hết hạn", Toast.LENGTH_SHORT).show();
            }
        }.start();
        EmulatorSmsSender.send(
                this,
                "VEGGO: Ma OTP dang ky cua ban la " + currentOtp + ". Ma co hieu luc trong 60 giay."
        );
    }

    private void updateRegisterOtpDescription(long millisUntilFinished) {
        long seconds = Math.max(0, millisUntilFinished / 1000);
        String suffix = seconds > 0
                ? "Mã còn hiệu lực trong " + seconds + " giây."
                : "Mã xác thực đã hết hạn. Vui lòng gửi lại mã.";
        tvVerifyPhoneDescription.setText("Chúng tôi đã gửi mã xác thực đến số điện thoại "
                + maskPhone(pendingPhone) + ". " + suffix);
    }

    private void handleVerifyRegister() {
        String otp = getOtpValue(getRegisterOtpFields());
        if (otp.length() < 6) {
            return;
        }
        if (System.currentTimeMillis() > otpExpiresAt) {
            Toast.makeText(this, "Mã xác thực đã hết hạn. Vui lòng gửi lại mã", Toast.LENGTH_SHORT).show();
            return;
        }
        if (otpFailedAttempts >= AuthFormUtils.MAX_OTP_ATTEMPTS) {
            Toast.makeText(this, "Bạn đã nhập sai quá số lần cho phép. Vui lòng gửi lại mã", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!currentOtp.equals(otp)) {
            otpFailedAttempts++;
            Toast.makeText(this,
                    "Mã xác thực không đúng (" + otpFailedAttempts + "/" + AuthFormUtils.MAX_OTP_ATTEMPTS + ")",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.register(pendingPhone, pendingPassword, "Người dùng mới", "");
    }

    private void observeViewModel() {
        authViewModel.getLoading().observe(this, isLoading -> {
            findViewById(R.id.btnVerify).setEnabled(!isLoading);
            findViewById(R.id.btnRegister).setEnabled(!isLoading);
        });

        authViewModel.getUser().observe(this, userDto -> {
            new AppPreferences(this).saveLoginSession(
                    userDto.getPhone(),
                    userDto.getCustomerId(),
                    userDto.getFullName(),
                    userDto.getEmail()
            );

            Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();

            PendingCheckoutStore pendingCheckoutStore = new PendingCheckoutStore(this);
            if (pendingCheckoutStore.hasPending()) {
                pendingCheckoutStore.openAfterAuth(this, userDto.getCustomerId());
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, R.id.nav_profile);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
            finish();
        });

        authViewModel.getError().observe(this, errorMessage -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            if (errorMessage != null && (errorMessage.contains("đăng ký") || errorMessage.contains("already exists"))) {
                showRegisterForm();
                tvPhoneError.setText(errorMessage);
                tvPhoneError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void togglePasswordVisibility(EditText editText, ImageView imageView, boolean isMainPassword) {
        boolean visible;
        if (isMainPassword) {
            isPasswordVisible = !isPasswordVisible;
            visible = isPasswordVisible;
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            visible = isConfirmPasswordVisible;
        }

        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            imageView.setImageResource(R.drawable.ic_eye_show_dark);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imageView.setImageResource(R.drawable.ic_eye_hide_dark);
        }
        editText.setSelection(editText.getText().length());
    }

    private void updatePolicyIcon() {
        cbPolicy.setImageResource(isPolicyAccepted ? R.drawable.ic_checkbox_checked : R.drawable.ic_checkbox_uncheck);
    }

    private boolean validatePhone(String phone) {
        return AuthFormUtils.showError(tvPhoneError, AuthFormUtils.phoneError(phone));
    }

    private boolean validatePassword(String password) {
        return AuthFormUtils.showError(tvPasswordError, AuthFormUtils.passwordError(password));
    }

    private boolean validateConfirmPassword(String password, String confirmPassword) {
        if (confirmPassword.isEmpty()) {
            tvConfirmPasswordError.setText("Vui lòng nhập lại mật khẩu");
            tvConfirmPasswordError.setVisibility(View.VISIBLE);
            return false;
        }
        if (!password.equals(confirmPassword)) {
            tvConfirmPasswordError.setText("Mật khẩu nhập lại không khớp");
            tvConfirmPasswordError.setVisibility(View.VISIBLE);
            return false;
        }
        tvConfirmPasswordError.setVisibility(View.GONE);
        return true;
    }

    private boolean validatePolicy() {
        if (isPolicyAccepted) {
            return true;
        }
        Toast.makeText(this, "Vui lòng đồng ý với Chính sách & Quy định", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void showRegisterForm() {
        layoutRegisterForm.setVisibility(View.VISIBLE);
        layoutVerifyForm.setVisibility(View.GONE);
    }

    private void showVerifyForm() {
        layoutRegisterForm.setVisibility(View.GONE);
        layoutVerifyForm.setVisibility(View.VISIBLE);
    }

    @NonNull
    private EditText[] getRegisterOtpFields() {
        return new EditText[] {
                findViewById(R.id.edtRegisterOtp1),
                findViewById(R.id.edtRegisterOtp2),
                findViewById(R.id.edtRegisterOtp3),
                findViewById(R.id.edtRegisterOtp4),
                findViewById(R.id.edtRegisterOtp5),
                findViewById(R.id.edtRegisterOtp6)
        };
    }

    private void clearOtpFields(@NonNull EditText[] fields) {
        for (EditText field : fields) {
            field.setText("");
        }
    }

    @NonNull
    private String getOtpValue(@NonNull EditText[] fields) {
        StringBuilder builder = new StringBuilder();
        for (EditText field : fields) {
            builder.append(field.getText().toString().trim());
        }
        return builder.toString();
    }

    @NonNull
    private String maskPhone(@NonNull String phone) {
        if (phone.length() < 4) {
            return phone;
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
