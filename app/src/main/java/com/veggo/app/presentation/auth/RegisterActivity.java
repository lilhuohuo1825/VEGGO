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

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;

public class RegisterActivity extends BaseActivity {
    private static final String TAG = "RegisterActivity";
    private static final String PHONE_REGEX = "^0\\d{9}$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Z]).{8,}$";
    private static final String DEMO_OTP = "123456";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        setupActions();
        observeViewModel();
        showRegisterForm();
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
        findViewById(R.id.tvResendCode).setOnClickListener(v ->
                Toast.makeText(this, "Mã xác thực: " + DEMO_OTP, Toast.LENGTH_SHORT).show()
        );
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

        pendingPhone = phone;
        pendingPassword = password;
        tvVerifyPhoneDescription.setText("Chúng tôi đã gửi mã xác thực đến số điện thoại " + maskPhone(phone));
        clearOtpFields(getRegisterOtpFields());
        showVerifyForm();
        Toast.makeText(this, "Mã xác thực: " + DEMO_OTP, Toast.LENGTH_SHORT).show();
    }

    private void handleVerifyRegister() {
        if (!DEMO_OTP.equals(getOtpValue(getRegisterOtpFields()))) {
            Toast.makeText(this, "Mã xác thực không đúng", Toast.LENGTH_SHORT).show();
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

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, R.id.nav_profile);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
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
        if (phone.isEmpty()) {
            tvPhoneError.setText("Vui lòng nhập số điện thoại");
            tvPhoneError.setVisibility(View.VISIBLE);
            return false;
        }
        if (!phone.matches(PHONE_REGEX)) {
            tvPhoneError.setText("Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số");
            tvPhoneError.setVisibility(View.VISIBLE);
            return false;
        }
        tvPhoneError.setVisibility(View.GONE);
        return true;
    }

    private boolean validatePassword(String password) {
        if (password.isEmpty()) {
            tvPasswordError.setText("Vui lòng nhập mật khẩu");
            tvPasswordError.setVisibility(View.VISIBLE);
            return false;
        }
        if (!password.matches(PASSWORD_REGEX)) {
            tvPasswordError.setText("Mật khẩu phải có ít nhất 8 ký tự và chứa ít nhất 1 chữ in hoa");
            tvPasswordError.setVisibility(View.VISIBLE);
            return false;
        }
        tvPasswordError.setVisibility(View.GONE);
        return true;
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
