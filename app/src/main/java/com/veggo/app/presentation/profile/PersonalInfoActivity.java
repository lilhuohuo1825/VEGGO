package com.veggo.app.presentation.profile;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.utils.ImageCompressor;
import com.veggo.app.data.remote.dto.UserProfileDto;
import com.veggo.app.MainActivity;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.dialog.VeggoDialog;

import java.io.File;
import java.io.IOException;

public class PersonalInfoActivity extends BaseActivity {
    private static final String PHONE_REGEX = "^0\\d{9}$";
    private static final String DEMO_OTP = "123456";

    private PersonalInfoViewModel viewModel;
    private AppPreferences appPreferences;

    private EditText nameInput;
    private EditText phoneInput;
    private EditText emailInput;
    private EditText birthdayInput;
    private TextView actionButton;
    private TextView logoutButton;
    private ImageView avatarView;
    private TextView changeAvatarButton;
    private TextView changePhoneButton;
    private CheckBox maleCheckBox;
    private CheckBox femaleCheckBox;
    private View genderEditContainer;
    private TextView genderDisplay;
    private View birthdayEditContainer;
    private TextView birthdayDisplay;
    private ImageView birthdayIcon;
    private View layoutGenderMale;
    private View layoutGenderFemale;

    private TextView tvNameError;
    private TextView tvPhoneError;
    private TextView tvEmailError;
    private TextView tvBirthdayError;

    private String sessionPhone;
    private String currentAvatarUrl;
    @Nullable
    private File pendingAvatarFile;

    private ActivityResultLauncher<String> galleryPicker;
    private ActivityResultLauncher<Void> cameraPicker;

    private boolean isEditMode = false;
    @Nullable
    private ProfileSnapshot savedSnapshot;

    private static final class ProfileSnapshot {
        private String name;
        private String phone;
        private String email;
        private String birthday;
        private String gender;
        @Nullable
        private String avatarUrl;
        @Nullable
        private File pendingAvatarFile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        appPreferences = new AppPreferences(this);
        sessionPhone = appPreferences.getCurrentPhone();

        bindViews();
        setupAvatarPickers();
        setupActions();
        setupValidationListeners();
        setupViewModel();
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isEditMode) {
            loadProfile();
        }
    }

    private void bindViews() {
        nameInput = findViewById(R.id.personalInfoNameInput);
        phoneInput = findViewById(R.id.personalInfoPhoneInput);
        emailInput = findViewById(R.id.personalInfoEmailInput);
        birthdayInput = findViewById(R.id.personalInfoBirthdayInput);
        actionButton = findViewById(R.id.personalInfoSaveButton);
        logoutButton = findViewById(R.id.personalInfoLogoutButton);
        avatarView = findViewById(R.id.personalInfoAvatarImage);
        changeAvatarButton = findViewById(R.id.personalInfoChangeAvatarButton);
        changePhoneButton = findViewById(R.id.personalInfoChangePhoneButton);
        maleCheckBox = findViewById(R.id.personalInfoGenderMaleCheck);
        femaleCheckBox = findViewById(R.id.personalInfoGenderFemaleCheck);
        genderEditContainer = findViewById(R.id.personalInfoGenderEditContainer);
        genderDisplay = findViewById(R.id.personalInfoGenderDisplay);
        birthdayEditContainer = findViewById(R.id.personalInfoBirthdayEditContainer);
        birthdayDisplay = findViewById(R.id.personalInfoBirthdayDisplay);
        birthdayIcon = findViewById(R.id.personalInfoBirthdayIcon);
        layoutGenderMale = findViewById(R.id.layoutGenderMale);
        layoutGenderFemale = findViewById(R.id.layoutGenderFemale);

        tvNameError = findViewById(R.id.personalInfoNameError);
        tvPhoneError = findViewById(R.id.personalInfoPhoneError);
        tvEmailError = findViewById(R.id.personalInfoEmailError);
        tvBirthdayError = findViewById(R.id.personalInfoBirthdayError);

        phoneInput.setEnabled(false);
        phoneInput.setFocusable(false);

        com.veggo.app.core.utils.DatePickerHelper.setupDatePicker(
                this,
                birthdayInput,
                birthdayIcon
        );

        setEditMode(false);
    }

    private void setupAvatarPickers() {
        galleryPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onGalleryImageSelected);
        cameraPicker = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), this::onCameraImageSelected);
    }

    private void setupActions() {
        findViewById(R.id.personalInfoBackButton).setOnClickListener(v -> finish());
        logoutButton.setOnClickListener(v -> {
            if (isEditMode) {
                cancelEdit();
            } else {
                showLogoutDialog();
            }
        });
        changePhoneButton.setOnClickListener(v -> showChangePhoneDialog());
        findViewById(R.id.personalInfoChangePasswordButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.veggo.app.presentation.auth.ForgotPasswordActivity.class);
            intent.putExtra(com.veggo.app.presentation.auth.ForgotPasswordActivity.EXTRA_SCREEN_TITLE, "Thay đổi mật khẩu");
            startActivity(intent);
        });
        actionButton.setOnClickListener(v -> {
            if (isEditMode) {
                saveProfile();
            } else {
                enterEditMode();
            }
        });

        if (changeAvatarButton != null) {
            changeAvatarButton.setOnClickListener(v -> showAvatarSourceDialog());
        }
        if (avatarView != null) {
            avatarView.setOnClickListener(v -> showAvatarSourceDialog());
        }
        maleCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                femaleCheckBox.setChecked(false);
            }
        });
        femaleCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                maleCheckBox.setChecked(false);
            }
        });
        layoutGenderMale.setOnClickListener(v -> {
            if (isEditMode && !maleCheckBox.isChecked()) {
                maleCheckBox.setChecked(true);
            }
        });
        layoutGenderFemale.setOnClickListener(v -> {
            if (isEditMode && !femaleCheckBox.isChecked()) {
                femaleCheckBox.setChecked(true);
            }
        });
    }

    private void showLogoutDialog() {
        VeggoDialog.show(
                this,
                R.drawable.ic_profile_logout,
                "Xác nhận đăng xuất",
                "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản này không?",
                "Đăng xuất",
                "Hủy",
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                        logout();
                    }
                }
        );
    }

    private void logout() {
        appPreferences.logout();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SELECTED_NAV_ITEM, R.id.nav_profile);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(PersonalInfoViewModel.class);

        viewModel.getLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            actionButton.setEnabled(!loading);
            actionButton.setAlpha(loading ? 0.6f : 1f);
            logoutButton.setEnabled(!loading);
            logoutButton.setAlpha(loading ? 0.6f : 1f);
        });

        viewModel.getProfile().observe(this, this::onProfileUpdated);

        viewModel.getError().observe(this, message -> {
            if (message == null || message.isEmpty()) {
                return;
            }
            if (Boolean.TRUE.equals(viewModel.getRetryableError().getValue())) {
                Snackbar.make(actionButton, message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.consultation_submit_retry, v -> saveProfile())
                        .show();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfile() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            runOnUiThread(() -> bindProfile(snapshot.user));
        }).start();
    }

    private void bindProfile(@Nullable AssetModels.User user) {
        if (user == null) {
            return;
        }

        String name = user.fullName;
        if (!AssetScreenData.hasText(name)) {
            name = "Khách hàng " + user.customerId;
        }

        setFieldText(nameInput, name);
        setFieldText(phoneInput, user.phone);
        setFieldText(emailInput, user.email);
        setFieldText(birthdayInput, user.birthDay);
        bindGender(user.gender);
        updateGenderDisplay();
        updateBirthdayDisplay();
        ((TextView) findViewById(R.id.personalInfoCarbonBadge)).setText(user.carbonPoint + " điểm carbon");

        currentAvatarUrl = AssetScreenData.hasText(user.avatarUrl) ? user.avatarUrl : appPreferences.getAvatarUrl();
        showAvatarPreview(currentAvatarUrl);
        savedSnapshot = captureSnapshot();
        setEditMode(false);
    }

    private void bindGender(@Nullable String gender) {
        if (!AssetScreenData.hasText(gender)) {
            maleCheckBox.setChecked(false);
            femaleCheckBox.setChecked(false);
            return;
        }
        String value = gender.trim();
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        maleCheckBox.setChecked("nam".equals(normalized) || "male".equals(normalized));
        femaleCheckBox.setChecked(
                "nữ".equals(normalized)
                        || "nu".equals(normalized)
                        || "female".equals(normalized)
                        || "Nữ".equals(value)
                        || value.startsWith("N\u1eef")
        );
    }

    private void enterEditMode() {
        savedSnapshot = captureSnapshot();
        setEditMode(true);
    }

    private void cancelEdit() {
        if (savedSnapshot != null) {
            restoreSnapshot(savedSnapshot);
        }
        setEditMode(false);
    }

    private void setEditMode(boolean editMode) {
        isEditMode = editMode;
        applyFieldMode(editMode);
        applyBottomBarMode(editMode);
    }

    private void applyFieldMode(boolean editMode) {
        applyEditableField(nameInput, editMode);
        applyEditableField(emailInput, editMode);
        applyPhoneFieldMode(editMode);

        if (birthdayEditContainer != null) {
            birthdayEditContainer.setVisibility(editMode ? View.VISIBLE : View.GONE);
        }
        if (birthdayDisplay != null) {
            birthdayDisplay.setVisibility(editMode ? View.GONE : View.VISIBLE);
            if (!editMode) {
                updateBirthdayDisplay();
            }
        }
        if (!editMode) {
            applyEditableField(birthdayInput, false);
        } else {
            applyEditableField(birthdayInput, true);
        }

        if (birthdayIcon != null) {
            birthdayIcon.setVisibility(editMode ? View.VISIBLE : View.GONE);
        }
        if (changePhoneButton != null) {
            changePhoneButton.setVisibility(editMode ? View.VISIBLE : View.GONE);
        }
        if (genderEditContainer != null) {
            genderEditContainer.setVisibility(editMode ? View.VISIBLE : View.GONE);
        }
        if (genderDisplay != null) {
            genderDisplay.setVisibility(editMode ? View.GONE : View.VISIBLE);
            if (!editMode) {
                updateGenderDisplay();
            }
        }
        if (genderEditContainer != null) {
            layoutGenderMale.setEnabled(editMode);
            layoutGenderFemale.setEnabled(editMode);
            maleCheckBox.setEnabled(editMode);
            femaleCheckBox.setEnabled(editMode);
        }
    }

    private void applyPhoneFieldMode(boolean editMode) {
        if (editMode) {
            phoneInput.setBackgroundResource(R.drawable.bg_input);
            phoneInput.setTextColor(ContextCompat.getColor(this, R.color.neutral_60));
        } else {
            phoneInput.setBackground(null);
            phoneInput.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
        }
        phoneInput.setEnabled(false);
        phoneInput.setFocusable(false);
        phoneInput.setClickable(false);
    }

    private void applyEditableField(@NonNull EditText field, boolean editMode) {
        if (editMode) {
            field.setBackgroundResource(R.drawable.bg_input);
            field.setEnabled(true);
            field.setFocusable(true);
            field.setFocusableInTouchMode(true);
            field.setClickable(true);
            field.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
        } else {
            field.setBackground(null);
            field.setEnabled(false);
            field.setFocusable(false);
            field.setFocusableInTouchMode(false);
            field.setClickable(false);
            field.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
            field.setHint("");
        }
    }

    private void applyBottomBarMode(boolean editMode) {
        if (editMode) {
            logoutButton.setText("Huỷ");
            logoutButton.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
            logoutButton.setBackgroundResource(R.drawable.bg_cancel_outline_button);
            actionButton.setText("Lưu");
            actionButton.setBackgroundResource(R.drawable.bg_button_primary);
            actionButton.setTextColor(ContextCompat.getColor(this, R.color.neutral_10));
        } else {
            logoutButton.setText("Đăng xuất");
            logoutButton.setTextColor(ContextCompat.getColor(this, R.color.danger_main));
            logoutButton.setBackgroundResource(R.drawable.bg_logout_outline_button);
            actionButton.setText("Chỉnh sửa");
            actionButton.setBackgroundResource(R.drawable.bg_button_primary);
            actionButton.setTextColor(ContextCompat.getColor(this, R.color.neutral_10));
        }
    }

    private void updateGenderDisplay() {
        if (genderDisplay == null) {
            return;
        }
        String display = getGenderValue();
        genderDisplay.setText(AssetScreenData.hasText(display) ? display : "—");
    }

    private void updateBirthdayDisplay() {
        if (birthdayDisplay == null) {
            return;
        }
        String birthday = birthdayInput.getText() == null ? "" : birthdayInput.getText().toString().trim();
        birthdayDisplay.setText(AssetScreenData.hasText(birthday) ? birthday : "—");
    }

    @NonNull
    private ProfileSnapshot captureSnapshot() {
        ProfileSnapshot snapshot = new ProfileSnapshot();
        snapshot.name = nameInput.getText().toString();
        snapshot.phone = phoneInput.getText().toString();
        snapshot.email = emailInput.getText().toString();
        snapshot.birthday = birthdayInput.getText().toString();
        snapshot.gender = getGenderValue();
        snapshot.avatarUrl = currentAvatarUrl;
        snapshot.pendingAvatarFile = pendingAvatarFile;
        return snapshot;
    }

    private void restoreSnapshot(@NonNull ProfileSnapshot snapshot) {
        setFieldText(nameInput, snapshot.name);
        setFieldText(phoneInput, snapshot.phone);
        setFieldText(emailInput, snapshot.email);
        setFieldText(birthdayInput, snapshot.birthday);
        bindGender(snapshot.gender);
        updateGenderDisplay();
        updateBirthdayDisplay();
        pendingAvatarFile = snapshot.pendingAvatarFile;
        currentAvatarUrl = snapshot.avatarUrl;
        showAvatarPreview(currentAvatarUrl);
        clearFieldErrors();
    }

    private void clearFieldErrors() {
        tvNameError.setVisibility(View.GONE);
        tvPhoneError.setVisibility(View.GONE);
        tvEmailError.setVisibility(View.GONE);
        tvBirthdayError.setVisibility(View.GONE);
    }

    @Nullable
    private String getGenderValue() {
        if (maleCheckBox.isChecked()) {
            return "Nam";
        }
        if (femaleCheckBox.isChecked()) {
            return "Nữ";
        }
        return "";
    }

    private void saveProfile() {
        if (!AssetScreenData.hasText(sessionPhone)) {
            Toast.makeText(this, "Vui lòng đăng nhập để cập nhật thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isNameValid = validateName(nameInput.getText().toString());
        boolean isPhoneValid = validatePhone(phoneInput.getText().toString());
        boolean isEmailValid = validateEmail(emailInput.getText().toString());
        boolean isBirthdayValid = validateBirthday(birthdayInput.getText().toString());

        if (!isNameValid || !isPhoneValid || !isEmailValid || !isBirthdayValid) {
            Toast.makeText(this, "Vui lòng sửa các thông tin không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.updateProfile(
                sessionPhone,
                nameInput.getText().toString(),
                phoneInput.getText().toString(),
                emailInput.getText().toString(),
                birthdayInput.getText().toString(),
                getGenderValue(),
                pendingAvatarFile
        );
    }

    private void onProfileUpdated(UserProfileDto profile) {
        sessionPhone = profile.getPhone();
        currentAvatarUrl = profile.getAvatarUrl();
        pendingAvatarFile = null;

        String savedBirthday = AssetScreenData.hasText(profile.getBirthday())
                ? profile.getBirthday()
                : birthdayInput.getText().toString().trim();
        String savedGender = AssetScreenData.hasText(profile.getGender())
                ? profile.getGender()
                : getGenderValue();

        setFieldText(nameInput, profile.getName());
        setFieldText(phoneInput, profile.getPhone());
        setFieldText(emailInput, profile.getEmail());
        setFieldText(birthdayInput, savedBirthday);
        bindGender(savedGender);
        updateGenderDisplay();
        updateBirthdayDisplay();
        showAvatarPreview(profile.getAvatarUrl());
        savedSnapshot = captureSnapshot();
        setEditMode(false);

        appPreferences.saveProfileSession(
                profile.getPhone(),
                profile.getName(),
                profile.getEmail(),
                profile.getAvatarUrl(),
                savedBirthday,
                savedGender
        );

        Toast.makeText(this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
    }

    private void showChangePhoneDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_change_phone_otp);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        LinearLayout inputSection = dialog.findViewById(R.id.changePhoneInputSection);
        LinearLayout otpSection = dialog.findViewById(R.id.changePhoneOtpSection);
        EditText phoneEditText = dialog.findViewById(R.id.changePhoneInput);
        TextView phoneError = dialog.findViewById(R.id.changePhoneError);
        TextView otpDescription = dialog.findViewById(R.id.changePhoneOtpDescription);
        TextView primaryButton = dialog.findViewById(R.id.changePhonePrimaryButton);
        final String[] pendingPhone = {""};

        dialog.findViewById(R.id.changePhoneCancelButton).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.changePhoneResendButton).setOnClickListener(v ->
                Toast.makeText(this, "Mã xác thực: " + DEMO_OTP, Toast.LENGTH_SHORT).show());

        primaryButton.setOnClickListener(v -> {
            if (otpSection.getVisibility() != View.VISIBLE) {
                String newPhone = phoneEditText.getText().toString().trim();
                if (!validateChangePhone(newPhone, phoneError)) {
                    return;
                }
                pendingPhone[0] = newPhone;
                inputSection.setVisibility(View.GONE);
                otpSection.setVisibility(View.VISIBLE);
                otpDescription.setText("Chúng tôi đã gửi mã xác thực đến số điện thoại " + maskPhone(newPhone));
                primaryButton.setText("Xác thực");
                clearOtpFields(getChangePhoneOtpFields(dialog));
                Toast.makeText(this, "Mã xác thực: " + DEMO_OTP, Toast.LENGTH_SHORT).show();
                return;
            }

            String otp = getOtpValue(getChangePhoneOtpFields(dialog));
            if (!DEMO_OTP.equals(otp)) {
                Toast.makeText(this, "Mã xác thực không đúng", Toast.LENGTH_SHORT).show();
                return;
            }
            setFieldText(phoneInput, pendingPhone[0]);
            Toast.makeText(this, "Đã xác thực số điện thoại mới. Bấm Lưu để cập nhật.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private boolean validateChangePhone(String phone, TextView errorView) {
        if (!AssetScreenData.hasText(phone)) {
            errorView.setText("Vui lòng nhập số điện thoại mới");
            errorView.setVisibility(View.VISIBLE);
            return false;
        }
        if (!phone.matches(PHONE_REGEX)) {
            errorView.setText("Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số");
            errorView.setVisibility(View.VISIBLE);
            return false;
        }
        if (phone.equals(phoneInput.getText().toString().trim())) {
            errorView.setText("Số điện thoại mới phải khác số hiện tại");
            errorView.setVisibility(View.VISIBLE);
            return false;
        }
        errorView.setVisibility(View.GONE);
        return true;
    }

    private EditText[] getChangePhoneOtpFields(Dialog dialog) {
        return new EditText[] {
                dialog.findViewById(R.id.changePhoneOtp1),
                dialog.findViewById(R.id.changePhoneOtp2),
                dialog.findViewById(R.id.changePhoneOtp3),
                dialog.findViewById(R.id.changePhoneOtp4),
                dialog.findViewById(R.id.changePhoneOtp5),
                dialog.findViewById(R.id.changePhoneOtp6)
        };
    }

    private void clearOtpFields(EditText[] fields) {
        for (EditText field : fields) {
            field.setText("");
        }
    }

    private String getOtpValue(EditText[] fields) {
        StringBuilder builder = new StringBuilder();
        for (EditText field : fields) {
            builder.append(field.getText().toString().trim());
        }
        return builder.toString();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }

    private void showAvatarSourceDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_image_source);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.findViewById(R.id.dialogOptionGallery).setOnClickListener(v -> {
            dialog.dismiss();
            galleryPicker.launch("image/*");
        });

        dialog.findViewById(R.id.dialogOptionCamera).setOnClickListener(v -> {
            dialog.dismiss();
            cameraPicker.launch(null);
        });

        dialog.show();
    }

    private void onGalleryImageSelected(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            pendingAvatarFile = ImageCompressor.compressToJpeg(this, uri);
            showAvatarPreview(uri);
        } catch (IOException exception) {
            Toast.makeText(this, "Không thể xử lý ảnh đã chọn", Toast.LENGTH_SHORT).show();
        }
    }

    private void onCameraImageSelected(@Nullable Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        try {
            pendingAvatarFile = ImageCompressor.compressToJpeg(this, bitmap);
            if (avatarView != null) {
                avatarView.setImageBitmap(bitmap);
            }
        } catch (IOException exception) {
            Toast.makeText(this, "Không thể xử lý ảnh đã chụp", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAvatarPreview(@Nullable String avatarUrl) {
        if (avatarView == null || !AssetScreenData.hasText(avatarUrl)) {
            return;
        }
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_profile_avatar)
                .error(R.drawable.ic_profile_avatar)
                .into(avatarView);
    }

    private void showAvatarPreview(@Nullable Uri uri) {
        if (avatarView == null || uri == null) {
            return;
        }
        avatarView.setImageURI(uri);
    }

    @Nullable
    private ImageView findAvatarImageView() {
        View root = findViewById(android.R.id.content);
        if (!(root instanceof ViewGroup)) {
            return null;
        }
        ViewGroup content = (ViewGroup) root;
        if (content.getChildCount() == 0) {
            return null;
        }
        View screenRoot = content.getChildAt(0);
        if (!(screenRoot instanceof ViewGroup)) {
            return null;
        }
        ViewGroup frame = (ViewGroup) screenRoot;
        for (int i = 0; i < frame.getChildCount(); i++) {
            View child = frame.getChildAt(i);
            if (child instanceof ScrollView) {
                View scrollContent = ((ScrollView) child).getChildAt(0);
                if (scrollContent instanceof ViewGroup) {
                    ViewGroup vertical = (ViewGroup) scrollContent;
                    if (vertical.getChildCount() > 1 && vertical.getChildAt(1) instanceof ViewGroup) {
                        ViewGroup avatarCard = (ViewGroup) vertical.getChildAt(1);
                        if (avatarCard.getChildAt(0) instanceof ViewGroup) {
                            ViewGroup avatarFrame = (ViewGroup) avatarCard.getChildAt(0);
                            if (avatarFrame.getChildAt(0) instanceof ImageView) {
                                return (ImageView) avatarFrame.getChildAt(0);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private TextView findChangeAvatarButton() {
        View root = findViewById(android.R.id.content);
        if (!(root instanceof ViewGroup)) {
            return null;
        }
        ViewGroup content = (ViewGroup) root;
        if (content.getChildCount() == 0) {
            return null;
        }
        View screenRoot = content.getChildAt(0);
        if (!(screenRoot instanceof ViewGroup)) {
            return null;
        }
        ViewGroup frame = (ViewGroup) screenRoot;
        for (int i = 0; i < frame.getChildCount(); i++) {
            View child = frame.getChildAt(i);
            if (child instanceof ScrollView) {
                View scrollContent = ((ScrollView) child).getChildAt(0);
                if (scrollContent instanceof ViewGroup) {
                    ViewGroup vertical = (ViewGroup) scrollContent;
                    if (vertical.getChildCount() > 1 && vertical.getChildAt(1) instanceof ViewGroup) {
                        ViewGroup avatarCard = (ViewGroup) vertical.getChildAt(1);
                        if (avatarCard.getChildAt(1) instanceof TextView) {
                            return (TextView) avatarCard.getChildAt(1);
                        }
                    }
                }
            }
        }
        return null;
    }

    private void setFieldText(@Nullable EditText view, @Nullable String value) {
        if (view == null) {
            return;
        }
        view.setText(value == null ? "" : value);
    }

    private void setupValidationListeners() {
        nameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateName(nameInput.getText().toString());
            }
        });
        nameInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                validateName(s.toString());
            }
        });

        phoneInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                validatePhone(s.toString());
            }
        });

        emailInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail(emailInput.getText().toString());
            }
        });
        emailInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                validateEmail(s.toString());
            }
        });

        birthdayInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                validateBirthday(s.toString());
            }
        });
    }

    private boolean validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            tvNameError.setText("Họ và tên không được để trống");
            tvNameError.setVisibility(View.VISIBLE);
            return false;
        }
        if (!name.trim().replaceAll("\\s+", " ").matches("^[\\p{L}\\s'.-]{2,}$")) {
            tvNameError.setText("Họ và tên không hợp lệ (không chứa số hoặc ký tự lạ, tối thiểu 2 ký tự)");
            tvNameError.setVisibility(View.VISIBLE);
            return false;
        }
        tvNameError.setVisibility(View.GONE);
        return true;
    }

    private boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            tvPhoneError.setText("Số điện thoại không được để trống");
            tvPhoneError.setVisibility(View.VISIBLE);
            return false;
        }
        if (!phone.trim().matches(PHONE_REGEX)) {
            tvPhoneError.setText("Số điện thoại phải bắt đầu bằng 0 và gồm đúng 10 chữ số");
            tvPhoneError.setVisibility(View.VISIBLE);
            return false;
        }
        tvPhoneError.setVisibility(View.GONE);
        return true;
    }

    private boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            tvEmailError.setVisibility(View.GONE);
            return true;
        }
        if (!email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            tvEmailError.setText("Email không đúng định dạng (Ví dụ: user@example.com)");
            tvEmailError.setVisibility(View.VISIBLE);
            return false;
        }
        tvEmailError.setVisibility(View.GONE);
        return true;
    }

    private boolean validateBirthday(String birthday) {
        if (birthday == null || birthday.trim().isEmpty()) {
            tvBirthdayError.setVisibility(View.GONE);
            return true;
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        sdf.setLenient(false);
        try {
            java.util.Date date = sdf.parse(birthday.trim());
            if (date.after(new java.util.Date())) {
                tvBirthdayError.setText("Ngày sinh không thể ở tương lai");
                tvBirthdayError.setVisibility(View.VISIBLE);
                return false;
            }
        } catch (java.text.ParseException e) {
            tvBirthdayError.setText("Ngày sinh không đúng định dạng (dd/MM/yyyy)");
            tvBirthdayError.setVisibility(View.VISIBLE);
            return false;
        }
        tvBirthdayError.setVisibility(View.GONE);
        return true;
    }
}
