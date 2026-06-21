package com.veggo.app.presentation.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.veggo.app.MainActivity;
import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;

public class LoginActivity extends BaseActivity {
    private static final String PHONE_REGEX = "^0\\d{9}$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Z]).{8,}$";

    private AuthViewModel authViewModel;
    private EditText edtPhone;
    private EditText edtPassword;
    private TextView tvPhoneError;
    private TextView tvPasswordError;
    private ImageView imgEye;
    private View btnLogin;
    private View progressBar;
    private boolean isPasswordVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        setupActions();
        observeViewModel();
    }

    private void initViews() {
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        tvPhoneError = findViewById(R.id.tvPhoneError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        imgEye = findViewById(R.id.imgEye);
        btnLogin = findViewById(R.id.btnLogin);
//        progressBar = findViewById(R.id.progressBar); // Đảm bảo layout có progressBar hoặc xử lý ẩn/hiện btnLogin

        tvPhoneError.setVisibility(View.GONE);
        tvPasswordError.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void setupActions() {
        ImageView imgBack = findViewById(R.id.imgBack);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvRegister = findViewById(R.id.tvRegister);

        imgBack.setOnClickListener(v -> finish());
        imgEye.setOnClickListener(v -> togglePasswordVisibility());
        btnLogin.setOnClickListener(v -> handleLogin());
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void observeViewModel() {
        authViewModel.getLoading().observe(this, isLoading -> {
            btnLogin.setEnabled(!isLoading);
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
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

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, R.id.nav_profile);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        authViewModel.getError().observe(this, errorMessage -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            imgEye.setImageResource(R.drawable.ic_eye_show_dark);
        } else {
            edtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imgEye.setImageResource(R.drawable.ic_eye_hide_dark);
        }
        edtPassword.setSelection(edtPassword.getText().length());
    }

    private void handleLogin() {
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        boolean isPhoneValid = validatePhone(phone);
        boolean isPasswordValid = validatePassword(password);
        if (!isPhoneValid || !isPasswordValid) {
            return;
        }

        authViewModel.login(phone, password);
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
}
