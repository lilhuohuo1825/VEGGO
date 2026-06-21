package com.veggo.app.presentation.profile;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.utils.ImageCompressor;
import com.veggo.app.data.remote.dto.UserProfileDto;
import com.veggo.app.presentation.common.AssetScreenData;

import java.io.File;
import java.io.IOException;

public class PersonalInfoActivity extends BaseActivity {
    private PersonalInfoViewModel viewModel;
    private AppPreferences appPreferences;

    private EditText nameInput;
    private EditText phoneInput;
    private EditText emailInput;
    private TextView saveButton;
    private ImageView avatarView;
    private TextView changeAvatarButton;

    private String sessionPhone;
    private String currentAvatarUrl;
    @Nullable
    private File pendingAvatarFile;

    private ActivityResultLauncher<String> galleryPicker;
    private ActivityResultLauncher<Void> cameraPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        appPreferences = new AppPreferences(this);
        sessionPhone = appPreferences.getCurrentPhone();

        bindViews();
        setupAvatarPickers();
        setupActions();
        setupViewModel();
        loadProfile();
    }

    private void bindViews() {
        nameInput = findViewById(R.id.personalInfoNameInput);
        phoneInput = findViewById(R.id.personalInfoPhoneInput);
        emailInput = findViewById(R.id.personalInfoEmailInput);
        saveButton = findViewById(R.id.personalInfoSaveButton);
        avatarView = findAvatarImageView();
        changeAvatarButton = findChangeAvatarButton();

        com.veggo.app.core.utils.DatePickerHelper.setupDatePicker(
                this,
                findViewById(R.id.personalInfoBirthdayInput),
                findViewById(R.id.personalInfoBirthdayIcon)
        );
    }

    private void setupAvatarPickers() {
        galleryPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), this::onGalleryImageSelected);
        cameraPicker = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), this::onCameraImageSelected);
    }

    private void setupActions() {
        findViewById(R.id.personalInfoBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.personalInfoLogoutButton).setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveProfile());

        if (changeAvatarButton != null) {
            changeAvatarButton.setOnClickListener(v -> showAvatarSourceDialog());
        }
        if (avatarView != null) {
            avatarView.setOnClickListener(v -> showAvatarSourceDialog());
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(PersonalInfoViewModel.class);

        viewModel.getLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            saveButton.setEnabled(!loading);
            saveButton.setAlpha(loading ? 0.6f : 1f);
        });

        viewModel.getProfile().observe(this, this::onProfileUpdated);

        viewModel.getError().observe(this, message -> {
            if (message == null || message.isEmpty()) {
                return;
            }
            if (Boolean.TRUE.equals(viewModel.getRetryableError().getValue())) {
                Snackbar.make(saveButton, message, Snackbar.LENGTH_LONG)
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

        setText(nameInput, name);
        setText(phoneInput, user.phone);
        setText(emailInput, user.email);
        setText(findViewById(R.id.personalInfoBirthdayInput), user.birthDay);
        setText(findViewById(R.id.personalInfoGenderInput), user.gender);
        ((TextView) findViewById(R.id.personalInfoCarbonBadge)).setText(user.carbonPoint + " điểm carbon");

        currentAvatarUrl = AssetScreenData.hasText(user.avatar) ? user.avatar : appPreferences.getAvatarUrl();
        showAvatarPreview(currentAvatarUrl);
    }

    private void saveProfile() {
        if (!AssetScreenData.hasText(sessionPhone)) {
            Toast.makeText(this, "Vui lòng đăng nhập để cập nhật thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.updateProfile(
                sessionPhone,
                nameInput.getText().toString(),
                phoneInput.getText().toString(),
                emailInput.getText().toString(),
                pendingAvatarFile
        );
    }

    private void onProfileUpdated(UserProfileDto profile) {
        appPreferences.saveProfileSession(
                profile.getPhone(),
                profile.getName(),
                profile.getEmail(),
                profile.getAvatarUrl()
        );
        sessionPhone = profile.getPhone();
        currentAvatarUrl = profile.getAvatarUrl();
        pendingAvatarFile = null;

        setText(nameInput, profile.getName());
        setText(phoneInput, profile.getPhone());
        setText(emailInput, profile.getEmail());
        showAvatarPreview(profile.getAvatarUrl());

        Toast.makeText(this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
    }

    private void showAvatarSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đổi ảnh đại diện")
                .setItems(new CharSequence[]{"Chụp ảnh", "Chọn từ thư viện"}, (dialog, which) -> {
                    if (which == 0) {
                        cameraPicker.launch(null);
                    } else {
                        galleryPicker.launch("image/*");
                    }
                })
                .show();
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

    private void setText(EditText view, @Nullable String value) {
        if (view == null || !AssetScreenData.hasText(value)) {
            return;
        }
        view.setText(value);
    }

    private void setText(int viewId, @Nullable String value) {
        if (!AssetScreenData.hasText(value)) {
            return;
        }
        ((EditText) findViewById(viewId)).setText(value);
    }
}
