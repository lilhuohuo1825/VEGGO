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

import com.veggo.app.R;
import com.veggo.app.core.notification.EmulatorSmsSender;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.MainActivity;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.presentation.checkout.PendingCheckoutStore;

public class ForgotPasswordActivity extends BaseActivity {
    public static final String EXTRA_SCREEN_TITLE = "forgot_password_screen_title";

    private static final String TAG = "ForgotPasswordActivity";

    private AuthViewModel authViewModel;
    private LinearLayout layoutForgotForm;
    private LinearLayout layoutForgotVerifyForm;
    private LinearLayout layoutResetForm;
    private EditText edtForgotPhone;
    private TextView tvForgotPhoneError;
    private TextView tvForgotVerifyPhoneDescription;
    private TextView tvForgotOtpAlert;
    private EditText edtNewPassword;
    private EditText edtConfirmNewPassword;
    private TextView tvNewPasswordError;
    private TextView tvConfirmNewPasswordError;
    private ImageView imgEyeNewPassword;
    private ImageView imgEyeConfirmNewPassword;
    private boolean isNewPasswordVisible;
    private boolean isConfirmNewPasswordVisible;
    private String verifiedPhone;
    private String verifiedOtp;
    private String currentOtp;
    private long otpExpiresAt;
    private int otpFailedAttempts;
    private CountDownTimer otpTimer;
    private String newPasswordToAutoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        AuthFormUtils.setupAuthScreen(this);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        applyScreenTitle();
        setupActions();
        observeViewModel();
        showForgotForm();
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
        layoutForgotForm = findViewById(R.id.layoutForgotForm);
        layoutForgotVerifyForm = findViewById(R.id.layoutForgotVerifyForm);
        layoutResetForm = findViewById(R.id.layoutResetForm);
        edtForgotPhone = findViewById(R.id.edtForgotPhone);
        tvForgotPhoneError = findViewById(R.id.tvForgotPhoneError);
        tvForgotVerifyPhoneDescription = findViewById(R.id.tvForgotVerifyPhoneDescription);
        tvForgotOtpAlert = findViewById(R.id.tvForgotOtpAlert);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);
        tvNewPasswordError = findViewById(R.id.tvNewPasswordError);
        tvConfirmNewPasswordError = findViewById(R.id.tvConfirmNewPasswordError);
        imgEyeNewPassword = findViewById(R.id.imgEyeNewPassword);
        imgEyeConfirmNewPassword = findViewById(R.id.imgEyeConfirmNewPassword);

        tvForgotPhoneError.setVisibility(View.GONE);
        tvForgotOtpAlert.setVisibility(View.GONE);
        tvNewPasswordError.setVisibility(View.GONE);
        tvConfirmNewPasswordError.setVisibility(View.GONE);
    }

    private void applyScreenTitle() {
        String title = getIntent().getStringExtra(EXTRA_SCREEN_TITLE);
        if (title == null || title.trim().isEmpty()) {
            return;
        }
        ((TextView) findViewById(R.id.tvForgotTitle)).setText(title);
        ((TextView) findViewById(R.id.tvForgotVerifyTitle)).setText(title);
        ((TextView) findViewById(R.id.tvResetTitle)).setText(title);
    }

    private void setupActions() {
        findViewById(R.id.imgBackForgot).setOnClickListener(v -> finish());
        findViewById(R.id.imgBackVerifyForgot).setOnClickListener(v -> showForgotForm());
        findViewById(R.id.imgBackReset).setOnClickListener(v -> showForgotVerifyForm());
        findViewById(R.id.btnSendOtp).setOnClickListener(v -> handleSendOtp());
        findViewById(R.id.btnVerifyForgot).setOnClickListener(v -> handleVerifyOtp());
        findViewById(R.id.tvResendForgotCode).setOnClickListener(v -> handleSendOtp());
        findViewById(R.id.btnResetPassword).setOnClickListener(v -> handleResetPassword());
        imgEyeNewPassword.setOnClickListener(v -> togglePasswordVisibility(edtNewPassword, imgEyeNewPassword, true));
        imgEyeConfirmNewPassword.setOnClickListener(v -> togglePasswordVisibility(edtConfirmNewPassword, imgEyeConfirmNewPassword, false));
        edtForgotPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validatePhone(edtForgotPhone.getText().toString().trim());
        });
        edtNewPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                validatePassword(s.toString());
                validateConfirmPassword(s.toString(), edtConfirmNewPassword.getText().toString());
            }
        });
        edtConfirmNewPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                validateConfirmPassword(edtNewPassword.getText().toString(), s.toString());
            }
        });
        AuthFormUtils.wireOtpFields(getForgotOtpFields(), this::handleVerifyOtp);
        for (EditText field : getForgotOtpFields()) {
            field.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (getOtpValue(getForgotOtpFields()).length() >= 6) {
                        hideForgotOtpAlert();
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void handleSendOtp() {
        String phone = edtForgotPhone.getText().toString().trim();
        if (!validatePhone(phone)) {
            return;
        }

        verifiedPhone = phone;
        authViewModel.forgotPassword(phone);
    }

    private void handleVerifyOtp() {
        String otp = getOtpValue(getForgotOtpFields());
        if (otp.isEmpty()) {
            showForgotOtpAlert();
            return;
        }
        if (otp.length() < 6) {
            return;
        }
        if (verifiedPhone == null || verifiedPhone.isEmpty()) {
            return;
        }
        verifiedOtp = otp;
        authViewModel.verifyForgotPasswordOtp(verifiedPhone, otp);
    }

    private void showForgotOtpAlert() {
        if (tvForgotOtpAlert == null) {
            return;
        }
        tvForgotOtpAlert.animate().cancel();
        tvForgotOtpAlert.setVisibility(View.VISIBLE);
        tvForgotOtpAlert.setAlpha(0f);
        tvForgotOtpAlert.setTranslationY(18f);
        tvForgotOtpAlert.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180)
                .start();
    }

    private void hideForgotOtpAlert() {
        if (tvForgotOtpAlert == null || tvForgotOtpAlert.getVisibility() != View.VISIBLE) {
            return;
        }
        tvForgotOtpAlert.animate()
                .alpha(0f)
                .translationY(12f)
                .setDuration(160)
                .withEndAction(() -> tvForgotOtpAlert.setVisibility(View.GONE))
                .start();
    }

    private void handleResetPassword() {
        String newPassword = edtNewPassword.getText().toString().trim();
        String confirmPassword = edtConfirmNewPassword.getText().toString().trim();

        boolean isPasswordValid = validatePassword(newPassword);
        boolean isConfirmValid = validateConfirmPassword(newPassword, confirmPassword);
        if (!isPasswordValid || !isConfirmValid) {
            return;
        }

        newPasswordToAutoLogin = newPassword;
        authViewModel.resetPassword(verifiedPhone, verifiedOtp, newPassword);
    }

    private void observeViewModel() {
        authViewModel.getLoading().observe(this, isLoading -> {
            findViewById(R.id.btnSendOtp).setEnabled(!isLoading);
            findViewById(R.id.btnVerifyForgot).setEnabled(!isLoading);
            findViewById(R.id.btnResetPassword).setEnabled(!isLoading);
        });

        authViewModel.getForgotPasswordSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                String otp = authViewModel.getForgotOtp().getValue();
                if (otp == null || otp.trim().isEmpty()) {
                    Toast.makeText(this, "Không nhận được mã xác thực. Vui lòng thử lại.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                sendForgotOtpUi(otp.trim());
            }
        });

        authViewModel.getVerifyForgotOtpSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                showResetForm();
            }
        });

        authViewModel.getResetPasswordSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Đặt lại mật khẩu thành công. Đang tự động đăng nhập...", Toast.LENGTH_SHORT).show();
                if (verifiedPhone != null && newPasswordToAutoLogin != null) {
                    authViewModel.login(verifiedPhone, newPasswordToAutoLogin);
                } else {
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                }
            }
        });

        authViewModel.getUser().observe(this, userDto -> {
            new AppPreferences(this).saveLoginSession(
                    userDto.getPhone(),
                    userDto.getCustomerId(),
                    userDto.getFullName(),
                    userDto.getEmail(),
                    userDto.getAvatarUrl()
            );
            Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

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
        });
    }

    private void sendForgotOtpUi(@NonNull String otp) {
        currentOtp = otp;
        otpExpiresAt = System.currentTimeMillis() + AuthFormUtils.OTP_TTL_MS;
        otpFailedAttempts = 0;
        tvForgotVerifyPhoneDescription.setText("Chúng tôi đã gửi mã xác thực đến số điện thoại "
                + maskPhone(verifiedPhone) + ". Mã có hiệu lực trong 60 giây.");
        clearOtpFields(getForgotOtpFields());
        hideForgotOtpAlert();
        showForgotVerifyForm();
        getForgotOtpFields()[0].requestFocus();
        if (otpTimer != null) {
            otpTimer.cancel();
        }
        otpTimer = new CountDownTimer(AuthFormUtils.OTP_TTL_MS, 1000) {
            @Override public void onTick(long millisUntilFinished) {
                updateForgotOtpDescription(millisUntilFinished);
            }
            @Override public void onFinish() {
                updateForgotOtpDescription(0);
                Toast.makeText(ForgotPasswordActivity.this, "Mã xác thực đã hết hạn", Toast.LENGTH_SHORT).show();
            }
        }.start();
        EmulatorSmsSender.send(
                this,
                "VEGGO: Ma OTP dat lai mat khau cua ban la " + currentOtp + ". Ma co hieu luc trong 60 giay."
        );
    }

    private void updateForgotOtpDescription(long millisUntilFinished) {
        long seconds = Math.max(0, millisUntilFinished / 1000);
        String suffix = seconds > 0
                ? "Mã còn hiệu lực trong " + seconds + " giây."
                : "Mã xác thực đã hết hạn. Vui lòng gửi lại mã.";
        tvForgotVerifyPhoneDescription.setText("Chúng tôi đã gửi mã xác thực đến số điện thoại "
                + maskPhone(verifiedPhone) + ". " + suffix);
    }

    private void togglePasswordVisibility(EditText editText, ImageView imageView, boolean isMainPassword) {
        boolean visible;
        if (isMainPassword) {
            isNewPasswordVisible = !isNewPasswordVisible;
            visible = isNewPasswordVisible;
        } else {
            isConfirmNewPasswordVisible = !isConfirmNewPasswordVisible;
            visible = isConfirmNewPasswordVisible;
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

    private boolean validatePhone(String phone) {
        return AuthFormUtils.showError(tvForgotPhoneError, AuthFormUtils.phoneError(phone));
    }

    private boolean validatePassword(String password) {
        String error = AuthFormUtils.passwordError(password);
        if ("Vui lòng nhập mật khẩu".equals(error)) {
            error = "Vui lòng nhập mật khẩu mới";
        }
        return AuthFormUtils.showError(tvNewPasswordError, error);
    }

    private boolean validateConfirmPassword(String password, String confirmPassword) {
        if (confirmPassword.isEmpty()) {
            tvConfirmNewPasswordError.setText("Vui lòng nhập lại mật khẩu");
            tvConfirmNewPasswordError.setVisibility(View.VISIBLE);
            return false;
        }
        if (!password.equals(confirmPassword)) {
            tvConfirmNewPasswordError.setText("Mật khẩu nhập lại không khớp");
            tvConfirmNewPasswordError.setVisibility(View.VISIBLE);
            return false;
        }
        tvConfirmNewPasswordError.setVisibility(View.GONE);
        return true;
    }

    private void showForgotForm() {
        hideForgotOtpAlert();
        layoutForgotForm.setVisibility(View.VISIBLE);
        layoutForgotVerifyForm.setVisibility(View.GONE);
        layoutResetForm.setVisibility(View.GONE);
    }

    private void showForgotVerifyForm() {
        layoutForgotForm.setVisibility(View.GONE);
        layoutForgotVerifyForm.setVisibility(View.VISIBLE);
        layoutResetForm.setVisibility(View.GONE);
    }

    private void showResetForm() {
        hideForgotOtpAlert();
        layoutForgotForm.setVisibility(View.GONE);
        layoutForgotVerifyForm.setVisibility(View.GONE);
        layoutResetForm.setVisibility(View.VISIBLE);
    }

    @NonNull
    private EditText[] getForgotOtpFields() {
        return new EditText[] {
                findViewById(R.id.edtForgotOtp1),
                findViewById(R.id.edtForgotOtp2),
                findViewById(R.id.edtForgotOtp3),
                findViewById(R.id.edtForgotOtp4),
                findViewById(R.id.edtForgotOtp5),
                findViewById(R.id.edtForgotOtp6)
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
