package com.veggo.app.presentation.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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
import com.veggo.app.core.ui.BaseActivity;

public class ForgotPasswordActivity extends BaseActivity {
    private static final String TAG = "ForgotPasswordActivity";
    private static final String PHONE_REGEX = "^0\\d{9}$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Z]).{8,}$";
    private static final String DEMO_OTP = "123456";

    private AuthViewModel authViewModel;
    private LinearLayout layoutForgotForm;
    private LinearLayout layoutForgotVerifyForm;
    private LinearLayout layoutResetForm;
    private EditText edtForgotPhone;
    private TextView tvForgotPhoneError;
    private TextView tvForgotVerifyPhoneDescription;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        setupActions();
        observeViewModel();
        showForgotForm();
    }

    private void initViews() {
        layoutForgotForm = findViewById(R.id.layoutForgotForm);
        layoutForgotVerifyForm = findViewById(R.id.layoutForgotVerifyForm);
        layoutResetForm = findViewById(R.id.layoutResetForm);
        edtForgotPhone = findViewById(R.id.edtForgotPhone);
        tvForgotPhoneError = findViewById(R.id.tvForgotPhoneError);
        tvForgotVerifyPhoneDescription = findViewById(R.id.tvForgotVerifyPhoneDescription);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = findViewById(R.id.edtConfirmNewPassword);
        tvNewPasswordError = findViewById(R.id.tvNewPasswordError);
        tvConfirmNewPasswordError = findViewById(R.id.tvConfirmNewPasswordError);
        imgEyeNewPassword = findViewById(R.id.imgEyeNewPassword);
        imgEyeConfirmNewPassword = findViewById(R.id.imgEyeConfirmNewPassword);

        tvForgotPhoneError.setVisibility(View.GONE);
        tvNewPasswordError.setVisibility(View.GONE);
        tvConfirmNewPasswordError.setVisibility(View.GONE);
    }

    private void setupActions() {
        findViewById(R.id.imgBackForgot).setOnClickListener(v -> finish());
        findViewById(R.id.imgBackVerifyForgot).setOnClickListener(v -> showForgotForm());
        findViewById(R.id.imgBackReset).setOnClickListener(v -> showForgotVerifyForm());
        findViewById(R.id.btnSendOtp).setOnClickListener(v -> handleSendOtp());
        findViewById(R.id.btnVerifyForgot).setOnClickListener(v -> handleVerifyOtp());
        findViewById(R.id.tvResendForgotCode).setOnClickListener(v ->
                Toast.makeText(this, "Mã xác thực: " + DEMO_OTP, Toast.LENGTH_SHORT).show()
        );
        findViewById(R.id.btnResetPassword).setOnClickListener(v -> handleResetPassword());
        imgEyeNewPassword.setOnClickListener(v -> togglePasswordVisibility(edtNewPassword, imgEyeNewPassword, true));
        imgEyeConfirmNewPassword.setOnClickListener(v -> togglePasswordVisibility(edtConfirmNewPassword, imgEyeConfirmNewPassword, false));
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
        if (!DEMO_OTP.equals(otp)) {
            Toast.makeText(this, "Mã xác thực không đúng", Toast.LENGTH_SHORT).show();
            return;
        }
        verifiedOtp = otp;
        showResetForm();
    }

    private void handleResetPassword() {
        String newPassword = edtNewPassword.getText().toString().trim();
        String confirmPassword = edtConfirmNewPassword.getText().toString().trim();

        boolean isPasswordValid = validatePassword(newPassword);
        boolean isConfirmValid = validateConfirmPassword(newPassword, confirmPassword);
        if (!isPasswordValid || !isConfirmValid) {
            return;
        }

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
                tvForgotVerifyPhoneDescription.setText("Chúng tôi đã gửi mã xác thực đến số điện thoại " + maskPhone(verifiedPhone));
                clearOtpFields(getForgotOtpFields());
                showForgotVerifyForm();
                Toast.makeText(this, "Mã xác thực: " + DEMO_OTP, Toast.LENGTH_SHORT).show();
            }
        });

        authViewModel.getResetPasswordSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "Đặt lại mật khẩu thành công", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
        });

        authViewModel.getError().observe(this, errorMessage -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        });
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
        if (phone.isEmpty()) {
            tvForgotPhoneError.setText("Vui lòng nhập số điện thoại");
            tvForgotPhoneError.setVisibility(View.VISIBLE);
            return false;
        }
        if (!phone.matches(PHONE_REGEX)) {
            tvForgotPhoneError.setText("Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số");
            tvForgotPhoneError.setVisibility(View.VISIBLE);
            return false;
        }
        tvForgotPhoneError.setVisibility(View.GONE);
        return true;
    }

    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            tvNewPasswordError.setText("Vui lòng nhập mật khẩu mới");
            tvNewPasswordError.setVisibility(View.VISIBLE);
            return false;
        }
        if (!password.matches(PASSWORD_REGEX)) {
            tvNewPasswordError.setText("Mật khẩu phải có ít nhất 8 ký tự và chứa ít nhất 1 chữ in hoa");
            tvNewPasswordError.setVisibility(View.VISIBLE);
            return false;
        }
        tvNewPasswordError.setVisibility(View.GONE);
        return true;
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
