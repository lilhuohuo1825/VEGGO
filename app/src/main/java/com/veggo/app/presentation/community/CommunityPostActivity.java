package com.veggo.app.presentation.community;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.veggo.app.R;
import com.veggo.app.data.local.entity.CommunityCategoryEntity;
import com.veggo.app.data.local.entity.CommunityRecipeGalleryEntity;
import com.veggo.app.data.local.entity.CommunityRecipeIngredientEntity;
import com.veggo.app.data.local.entity.ProductEntity;
import com.veggo.app.data.remote.dto.ProductDto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommunityPostActivity extends AppCompatActivity {
    public static final String EXTRA_EDIT_RECIPE_ID = "community_edit_recipe_id";
    private static final int MAX_IMAGES = 6;

    private ActivityResultLauncher<PickVisualMediaRequest> galleryPicker;
    private ActivityResultLauncher<Uri> cameraPicker;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private CommunityRepository repository;
    private LinearLayout imagePreviewRow;
    private LinearLayout ingredientsList;
    private View imagePreviewScroll;
    private TextView draftViewButton;
    private TextView categoryButton;
    private TextView titleError;
    private TextView categoryError;
    private TextView timeError;
    private TextView imageError;
    private TextView ingredientsError;
    private TextView stepsError;
    private TextView nutritionError;
    private TextView formError;
    private EditText titleInput;
    private EditText timeInput;
    private EditText videoInput;
    private EditText caloriesInput;
    private EditText saltInput;
    private EditText sugarInput;
    private LinearLayout stepsList;
    private CommunityRepository.RecipeDraft latestDraft;
    private final List<CommunityRepository.RecipeDraft> savedDrafts = new ArrayList<>();
    private Uri pendingCameraUri;
    private File pendingCameraFile;
    private String editRecipeId;
    private String originalEditSnapshot = "";
    private String selectedCategoryId = "";
    private boolean loadingCategories;
    private boolean openCategoryAfterLoad;
    private boolean loadingProducts;
    private boolean openProductAfterLoad;
    private final List<CommunityCategoryEntity> categories = new ArrayList<>();
    private final List<ProductDto> products = new ArrayList<>();
    private final List<IngredientSelection> selectedIngredients = new ArrayList<>();
    private final List<String> selectedImageUris = new ArrayList<>();
    private final List<EditText> stepInputs = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_post);

        editRecipeId = getIntent().getStringExtra(EXTRA_EDIT_RECIPE_ID);
        repository = new CommunityRepository(this);
        bindViews();

        galleryPicker = registerForActivityResult(
                new ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES),
                this::addSelectedImages
        );
        cameraPicker = registerForActivityResult(new ActivityResultContracts.TakePicture(), this::handleCameraResult);
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (Boolean.TRUE.equals(granted)) {
                        launchCameraCapture();
                    } else {
                        Toast.makeText(this, "Cần quyền camera để chụp ảnh món ăn", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        findViewById(R.id.communityPostBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.communityPostImagePickerButton).setOnClickListener(v -> showImageSourceDialog());
        findViewById(R.id.communityPostAddIngredientButton).setOnClickListener(v -> showProductPicker());
        findViewById(R.id.communityPostAddStepButton).setOnClickListener(v -> addStepInput("", null));
        categoryButton.setOnClickListener(v -> showCategorySheet());
        findViewById(R.id.communityPostDraftButton).setOnClickListener(v -> saveDraft());
        findViewById(R.id.communityPostSubmitButton).setOnClickListener(v -> confirmPublishRecipe());
        draftViewButton.setOnClickListener(v -> showDraftSheet());
        if (!TextUtils.isEmpty(editRecipeId)) {
            ((TextView) findViewById(R.id.communityPostSubmitButton)).setText("Lưu thay đổi");
            draftViewButton.setVisibility(View.GONE);
            findViewById(R.id.communityPostDraftButton).setVisibility(View.GONE);
        }

        loadDraftState();
        setStepTexts(new ArrayList<>());
        setupDraftButtonVisibilityTracking();
        updateDraftButtonState();
        loadCategories();
        loadProducts();
        loadRecipeForEdit();
    }

    private void bindViews() {
        imagePreviewScroll = findViewById(R.id.communityPostImagePreviewScroll);
        imagePreviewRow = findViewById(R.id.communityPostImagePreviewRow);
        ingredientsList = findViewById(R.id.communityPostIngredientsList);
        draftViewButton = findViewById(R.id.communityPostDraftViewButton);
        categoryButton = findViewById(R.id.communityPostCategoryButton);
        titleError = findViewById(R.id.communityPostTitleError);
        categoryError = findViewById(R.id.communityPostCategoryError);
        timeError = findViewById(R.id.communityPostTimeError);
        imageError = findViewById(R.id.communityPostImageError);
        ingredientsError = findViewById(R.id.communityPostIngredientsError);
        stepsError = findViewById(R.id.communityPostStepsError);
        nutritionError = findViewById(R.id.communityPostNutritionError);
        formError = findViewById(R.id.communityPostFormError);
        titleInput = findViewById(R.id.communityPostTitleInput);
        timeInput = findViewById(R.id.communityPostTimeInput);
        videoInput = findViewById(R.id.communityPostVideoInput);
        stepsList = findViewById(R.id.communityPostStepsList);
        caloriesInput = findViewById(R.id.communityPostCaloriesInput);
        saltInput = findViewById(R.id.communityPostSaltInput);
        sugarInput = findViewById(R.id.communityPostSugarInput);
    }

    private void loadDraftState() {
        if (!TextUtils.isEmpty(editRecipeId)) {
            return;
        }
        latestDraft = null;
        savedDrafts.clear();
        updateDraftButtonState();
        repository.loadDraft(response -> runOnUiThread(() -> {
            boolean hasDraft = response != null && response.hasDraft;
            if (response != null && response.drafts != null) {
                savedDrafts.addAll(response.drafts);
            } else if (response != null && response.draft != null) {
                savedDrafts.add(response.draft);
            }
            latestDraft = savedDrafts.isEmpty() ? null : savedDrafts.get(0);
            updateDraftButtonState();
        }));
    }

    private void loadCategories() {
        loadCategories(null);
    }

    private void loadCategories(@Nullable Runnable afterLoad) {
        if (loadingCategories) {
            if (afterLoad != null) {
                openCategoryAfterLoad = true;
            }
            return;
        }
        loadingCategories = true;
        repository.loadCategories(result -> runOnUiThread(() -> {
            loadingCategories = false;
            categories.clear();
            if (result != null) {
                categories.addAll(result);
            }
            updateCategoryLabel();
            if (afterLoad != null) {
                afterLoad.run();
            } else if (openCategoryAfterLoad) {
                openCategoryAfterLoad = false;
                showCategorySheet();
            }
        }));
    }

    private void loadProducts() {
        loadProducts(null);
    }

    private void loadProducts(@Nullable Runnable afterLoad) {
        if (loadingProducts) {
            if (afterLoad != null) {
                openProductAfterLoad = true;
            }
            return;
        }
        loadingProducts = true;
        repository.loadProducts(result -> runOnUiThread(() -> {
            loadingProducts = false;
            products.clear();
            if (result != null) {
                products.addAll(result);
            }
            if (afterLoad != null) {
                afterLoad.run();
            } else if (openProductAfterLoad) {
                openProductAfterLoad = false;
                showProductPicker();
            }
        }));
    }

    private void saveDraft() {
        if (!validateDraftHasContent()) {
            return;
        }
        repository.saveDraft(buildDraft(), success -> runOnUiThread(() -> {
            Toast.makeText(this, success ? "Đã lưu bản nháp" : "Chưa lưu được bản nháp", Toast.LENGTH_SHORT).show();
            if (success) {
                loadDraftState();
            }
        }));
    }

    private void confirmPublishRecipe() {
        if (!validateRecipeFormInline(true)) {
            return;
        }
        if (TextUtils.isEmpty(text(titleInput))) {
            Toast.makeText(this, "Nhập tên món ăn trước nha", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(selectedCategoryId)) {
            Toast.makeText(this, "Chọn danh mục trước nha", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedIngredients.isEmpty()) {
            Toast.makeText(this, "Thêm ít nhất 1 nguyên liệu nha", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateRequiredRecipeFields()) {
            return;
        }
        if (!TextUtils.isEmpty(editRecipeId) && buildFormSnapshot().equals(originalEditSnapshot)) {
            Toast.makeText(this, "Bạn chưa thay đổi nội dung nào", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean editing = !TextUtils.isEmpty(editRecipeId);
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_veggo);
        ImageView icon = dialog.findViewById(R.id.imgIcon);
        icon.setImageResource(editing ? R.drawable.ic_edit : R.drawable.ic_community_publish);
        icon.setColorFilter(getColor(R.color.primary_main));
        ((TextView) dialog.findViewById(R.id.tvTitle)).setText(editing ? "Lưu thay đổi?" : "Đăng công thức?");
        ((TextView) dialog.findViewById(R.id.tvMessage)).setText(editing
                ? "Công thức sẽ được cập nhật và hiển thị cho cộng đồng."
                : "Công thức của bạn sẽ được đăng lên cộng đồng Veggo.");
        ((TextView) dialog.findViewById(R.id.btnConfirm)).setText(editing ? "Lưu" : "Đăng");
        ((TextView) dialog.findViewById(R.id.btnCancel)).setText("Hủy");
        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            publishRecipe();
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.45f);
        }
    }

    private void publishRecipe() {
        if (!validateRecipeFormInline(false)) {
            return;
        }
        if (TextUtils.isEmpty(text(titleInput))) {
            Toast.makeText(this, "Nhập tên món ăn trước nha", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(selectedCategoryId)) {
            Toast.makeText(this, "Chọn danh mục trước nha", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedIngredients.isEmpty()) {
            Toast.makeText(this, "Thêm ít nhất 1 nguyên liệu nha", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateRequiredRecipeFields()) {
            return;
        }

        CommunityRepository.RecipeDraft draft = buildDraft();
        CommunityRepository.Callback<Boolean> callback = success -> runOnUiThread(() -> {
            Toast.makeText(this, success ? (TextUtils.isEmpty(editRecipeId) ? "Đã đăng công thức" : "Đã lưu thay đổi") : "Chưa lưu được công thức", Toast.LENGTH_SHORT).show();
            if (success) {
                finish();
            }
        });
        if (TextUtils.isEmpty(editRecipeId)) {
            repository.publishRecipe(draft, callback);
        } else {
            repository.updateRecipe(editRecipeId, draft, callback);
        }
    }

    private boolean validateRecipeFormInline(boolean requireChanged) {
        clearValidationErrors();
        View firstInvalidView = null;

        if (TextUtils.isEmpty(text(titleInput))) {
            showValidationError(titleError, "Vui lòng nhập tên món ăn");
            firstInvalidView = firstInvalidView == null ? titleInput : firstInvalidView;
        }
        if (TextUtils.isEmpty(selectedCategoryId)) {
            showValidationError(categoryError, "Vui lòng chọn danh mục");
            firstInvalidView = firstInvalidView == null ? categoryButton : firstInvalidView;
        }
        if (TextUtils.isEmpty(text(timeInput)) || parseInt(text(timeInput)) <= 0) {
            showValidationError(timeError, "Vui long nhap thoi gian nau");
            firstInvalidView = firstInvalidView == null ? timeInput : firstInvalidView;
        }
        if (selectedImageUris.isEmpty()) {
            showValidationError(imageError, "Vui lòng thêm ít nhất 1 ảnh món ăn");
            firstInvalidView = firstInvalidView == null ? findViewById(R.id.communityPostImagePickerButton) : firstInvalidView;
        }
        if (selectedIngredients.isEmpty()) {
            showValidationError(ingredientsError, "Vui lòng chọn ít nhất 1 nguyên liệu");
            firstInvalidView = firstInvalidView == null ? findViewById(R.id.communityPostAddIngredientButton) : firstInvalidView;
        } else {
            for (IngredientSelection selection : selectedIngredients) {
                if (TextUtils.isEmpty(selection.quantity)) {
                    showValidationError(ingredientsError, "Vui lòng nhập định lượng cho từng nguyên liệu");
                    firstInvalidView = firstInvalidView == null ? ingredientsList : firstInvalidView;
                    break;
                }
            }
        }
        if (TextUtils.isEmpty(buildStepsText())) {
            showValidationError(stepsError, "Vui lòng nhập các bước nấu");
            firstInvalidView = firstInvalidView == null ? stepsList : firstInvalidView;
        }
        if (TextUtils.isEmpty(text(caloriesInput)) || TextUtils.isEmpty(text(saltInput)) || TextUtils.isEmpty(text(sugarInput))) {
            showValidationError(nutritionError, "Vui lòng nhập đủ calories, muối và đường");
            firstInvalidView = firstInvalidView == null ? caloriesInput : firstInvalidView;
        }

        if (firstInvalidView != null) {
            scrollToView(firstInvalidView);
            return false;
        }

        if (requireChanged && !TextUtils.isEmpty(editRecipeId) && buildFormSnapshot().equals(originalEditSnapshot)) {
            showValidationError(formError, "Bạn chưa thay đổi nội dung nào");
            scrollToView(formError);
            return false;
        }
        return true;
    }

    private void clearValidationErrors() {
        hideValidationError(titleError);
        hideValidationError(categoryError);
        hideValidationError(timeError);
        hideValidationError(imageError);
        hideValidationError(ingredientsError);
        hideValidationError(stepsError);
        hideValidationError(nutritionError);
        hideValidationError(formError);
    }

    private void showValidationError(TextView errorView, String message) {
        if (errorView == null) {
            return;
        }
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    private void hideValidationError(TextView errorView) {
        if (errorView == null) {
            return;
        }
        errorView.setText("");
        errorView.setVisibility(View.GONE);
    }

    private void scrollToView(View target) {
        if (target == null) {
            return;
        }
        target.post(() -> {
            ScrollView scrollView = findViewById(R.id.communityPostScroll);
            scrollView.smoothScrollTo(0, Math.max(0, target.getTop() - dp(18)));
        });
    }

    private boolean validateRequiredRecipeFields() {
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Thêm ít nhất 1 ảnh món ăn nha", Toast.LENGTH_SHORT).show();
            return false;
        }
        for (IngredientSelection selection : selectedIngredients) {
            if (TextUtils.isEmpty(selection.quantity)) {
                Toast.makeText(this, "Nhập định lượng cho từng nguyên liệu nha", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        if (TextUtils.isEmpty(buildStepsText())) {
            Toast.makeText(this, "Nhập các bước nấu trước nha", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(text(timeInput)) || parseInt(text(timeInput)) <= 0) {
            timeInput.requestFocus();
            Toast.makeText(this, "Nhap thoi gian nau truoc nha", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(text(caloriesInput))) {
            caloriesInput.requestFocus();
            Toast.makeText(this, "Nhập calories trước nha", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(text(saltInput))) {
            saltInput.requestFocus();
            Toast.makeText(this, "Nhập lượng muối trước nha", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(text(sugarInput))) {
            sugarInput.requestFocus();
            Toast.makeText(this, "Nhập lượng đường trước nha", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void loadRecipeForEdit() {
        if (TextUtils.isEmpty(editRecipeId)) {
            return;
        }
        repository.loadRecipeDetail(editRecipeId, data -> runOnUiThread(() -> fillRecipeDetail(data)));
    }

    private void fillRecipeDetail(CommunityRepository.RecipeDetailData data) {
        if (data == null || data.recipe == null) {
            return;
        }
        titleInput.setText(data.recipe.getTitle());
        selectedCategoryId = data.recipe.getCategoryId();
        timeInput.setText(data.recipe.getTimeMinutes() > 0 ? String.valueOf(data.recipe.getTimeMinutes()) : "");
        updateCategoryLabel();
        if (data.detail != null) {
            videoInput.setText(data.detail.getVideoUrl());
            setStepTexts(splitSteps(data.detail.getInstructions()));
            caloriesInput.setText(data.detail.getCalories() > 0 ? String.valueOf(data.detail.getCalories()) : "");
            saltInput.setText(data.detail.getSaltLevel());
            sugarInput.setText(data.detail.getSugarLevel());
        }
        selectedImageUris.clear();
        for (CommunityRecipeGalleryEntity gallery : data.galleries) {
            if (!TextUtils.isEmpty(gallery.getImageUrl())) {
                selectedImageUris.add(gallery.getImageUrl());
            }
        }
        if (selectedImageUris.isEmpty() && !TextUtils.isEmpty(data.recipe.getImageUrl())) {
            selectedImageUris.add(data.recipe.getImageUrl());
        }
        selectedIngredients.clear();
        for (CommunityRecipeIngredientEntity ingredient : data.ingredients) {
            ProductEntity product = data.productsById.get(ingredient.getProductId());
            ProductDto dto = new ProductDto();
            dto.setId(ingredient.getProductId());
            dto.setSku(ingredient.getProductSku());
            dto.setName(!TextUtils.isEmpty(ingredient.getDisplayName()) ? ingredient.getDisplayName() : product == null ? "" : product.getName());
            dto.setImageUrl(!TextUtils.isEmpty(ingredient.getIconUrl()) ? ingredient.getIconUrl() : product == null ? "" : product.getImageUrl());
            IngredientSelection selection = new IngredientSelection(dto);
            selection.quantity = ingredient.getQuantity() == null ? "" : ingredient.getQuantity();
            selectedIngredients.add(selection);
        }
        renderImagePreviews();
        renderIngredients();
        originalEditSnapshot = buildFormSnapshot();
    }

    private String buildFormSnapshot() {
        StringBuilder builder = new StringBuilder();
        builder.append(text(titleInput)).append('|');
        builder.append(selectedCategoryId).append('|');
        builder.append(text(timeInput)).append('|');
        builder.append(text(videoInput)).append('|');
        builder.append(buildStepsText()).append('|');
        builder.append(text(caloriesInput)).append('|');
        builder.append(text(saltInput)).append('|');
        builder.append(text(sugarInput)).append('|');
        for (String imageUrl : selectedImageUris) {
            builder.append("img=").append(imageUrl).append('|');
        }
        for (IngredientSelection selection : selectedIngredients) {
            builder.append("ing=")
                    .append(selection.product.getId()).append(',')
                    .append(selection.product.getName()).append(',')
                    .append(selection.quantity)
                    .append('|');
        }
        return builder.toString();
    }

    private CommunityRepository.RecipeDraft buildDraft() {
        CommunityRepository.RecipeDraft draft = new CommunityRepository.RecipeDraft();
        draft.title = text(titleInput);
        draft.categoryId = selectedCategoryId;
        draft.timeMinutes = parseInt(text(timeInput));
        draft.imageUrls = new ArrayList<>(selectedImageUris);
        draft.imageUrl = selectedImageUris.isEmpty() ? "" : selectedImageUris.get(0);
        draft.videoUrl = text(videoInput);
        draft.ingredientItems = new ArrayList<>();
        StringBuilder ingredientText = new StringBuilder();
        for (IngredientSelection selection : selectedIngredients) {
            CommunityRepository.RecipeIngredientDraft item = new CommunityRepository.RecipeIngredientDraft();
            item.productId = selection.product.getId();
            item.productSku = selection.product.getSku();
            item.displayName = selection.product.getName();
            item.imageUrl = selection.product.getImageUrl();
            item.quantity = selection.quantity;
            draft.ingredientItems.add(item);
            if (ingredientText.length() > 0) {
                ingredientText.append("\n");
            }
            ingredientText.append(item.displayName);
            if (!TextUtils.isEmpty(item.quantity)) {
                ingredientText.append(" - ").append(item.quantity);
            }
        }
        draft.ingredientsText = ingredientText.toString();
        draft.steps = buildStepsText();
        draft.calories = parseInt(text(caloriesInput));
        draft.saltLevel = text(saltInput);
        draft.sugarLevel = text(sugarInput);
        return draft;
    }

    private void fillDraft(CommunityRepository.RecipeDraft draft) {
        if (draft == null) {
            return;
        }
        titleInput.setText(draft.title);
        selectedCategoryId = draft.categoryId == null ? "" : draft.categoryId;
        timeInput.setText(draft.timeMinutes > 0 ? String.valueOf(draft.timeMinutes) : "");
        updateCategoryLabel();
        videoInput.setText(draft.videoUrl);
        setStepTexts(splitSteps(draft.steps));
        caloriesInput.setText(draft.calories > 0 ? String.valueOf(draft.calories) : "");
        saltInput.setText(draft.saltLevel);
        sugarInput.setText(draft.sugarLevel);
        selectedImageUris.clear();
        if (draft.imageUrls != null && !draft.imageUrls.isEmpty()) {
            selectedImageUris.addAll(draft.imageUrls);
        } else if (!TextUtils.isEmpty(draft.imageUrl)) {
            selectedImageUris.add(draft.imageUrl);
        }
        selectedIngredients.clear();
        if (draft.ingredientItems != null) {
            for (CommunityRepository.RecipeIngredientDraft item : draft.ingredientItems) {
                ProductDto product = productFromDraft(item);
                IngredientSelection selection = new IngredientSelection(product);
                selection.quantity = item.quantity == null ? "" : item.quantity;
                selectedIngredients.add(selection);
            }
        }
        renderImagePreviews();
        renderIngredients();
    }

    private void showDraftSheet() {
        if (savedDrafts.isEmpty()) {
            Toast.makeText(this, "Chưa có bản nháp", Toast.LENGTH_SHORT).show();
            loadDraftState();
            return;
        }
        Dialog dialog = bottomSheetDialog();
        LinearLayout container = pickerContainer("Bản nháp của bạn", "Chọn một bản nháp để tiếp tục chỉnh sửa");
        LinearLayout list = scrollList(container, dp(420));
        for (CommunityRepository.RecipeDraft draft : savedDrafts) {
            list.addView(draftRow(draft, v -> {
                fillDraft(draft);
                dialog.dismiss();
            }, dialog));
        }
        dialog.setContentView(container);
        showBottomSheet(dialog);
    }

    private View draftRow(CommunityRepository.RecipeDraft draft, View.OnClickListener listener, Dialog sheetDialog) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(10), dp(10));
        row.setBackgroundResource(R.drawable.bg_post_upload);
        row.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(8);
        row.setLayoutParams(params);

        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(textBox, textParams);

        String title = TextUtils.isEmpty(draft.title) ? "Công thức chưa đặt tên" : draft.title;
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(getColor(R.color.neutral_100));
        titleView.setTextSize(14);
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
        textBox.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText(draftSubtitle(draft));
        subtitleView.setTextColor(getColor(R.color.neutral_70));
        subtitleView.setTextSize(11);
        textBox.addView(subtitleView);

        ImageView delete = new ImageView(this);
        delete.setImageResource(R.drawable.ic_trash);
        delete.setColorFilter(getColor(R.color.danger_main));
        delete.setPadding(dp(8), dp(8), dp(8), dp(8));
        delete.setOnClickListener(v -> showDeleteDraftDialog(draft, sheetDialog));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        deleteParams.setMarginStart(dp(8));
        row.addView(delete, deleteParams);
        return row;
    }

    private String draftSubtitle(CommunityRepository.RecipeDraft draft) {
        int imageCount = draft.imageUrls == null ? (TextUtils.isEmpty(draft.imageUrl) ? 0 : 1) : draft.imageUrls.size();
        int ingredientCount = draft.ingredientItems == null ? 0 : draft.ingredientItems.size();
        if (imageCount == 0 && ingredientCount == 0) {
            return "Bản nháp";
        }
        return imageCount + " ảnh · " + ingredientCount + " nguyên liệu";
    }

    private void showDeleteDraftDialog(CommunityRepository.RecipeDraft draft, Dialog sheetDialog) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_veggo);
        ((ImageView) dialog.findViewById(R.id.imgIcon)).setImageResource(R.drawable.ic_trash);
        ((TextView) dialog.findViewById(R.id.tvTitle)).setText("Xóa bản nháp?");
        ((TextView) dialog.findViewById(R.id.tvMessage)).setText("Bản nháp này sẽ bị xóa khỏi danh sách của bạn.");
        ((TextView) dialog.findViewById(R.id.btnConfirm)).setText("Xóa");
        ((TextView) dialog.findViewById(R.id.btnCancel)).setText("Hủy");
        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            deleteDraft(draft, sheetDialog);
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.45f);
        }
    }

    private void deleteDraft(CommunityRepository.RecipeDraft draft, Dialog sheetDialog) {
        if (draft == null || TextUtils.isEmpty(draft.draftId)) {
            Toast.makeText(this, "Không tìm thấy bản nháp để xóa", Toast.LENGTH_SHORT).show();
            return;
        }
        repository.deleteDraft(draft.draftId, deleted -> runOnUiThread(() -> {
            Toast.makeText(this, deleted ? "Đã xóa bản nháp" : "Chưa xóa được bản nháp", Toast.LENGTH_SHORT).show();
            if (!deleted) {
                return;
            }
            savedDrafts.remove(draft);
            latestDraft = savedDrafts.isEmpty() ? null : savedDrafts.get(0);
            updateDraftButtonState();
            if (sheetDialog != null) {
                sheetDialog.dismiss();
            }
            loadDraftState();
        }));
    }

    private void updateDraftButtonState() {
        if (!TextUtils.isEmpty(editRecipeId)) {
            draftViewButton.setVisibility(View.GONE);
            return;
        }
        draftViewButton.setText(savedDrafts.size() > 1 ? "Nháp (" + savedDrafts.size() + ")" : "Xem nháp");
        draftViewButton.setVisibility((savedDrafts.isEmpty() && !hasDraftContent()) ? View.GONE : View.VISIBLE);
    }

    private ProductDto productFromDraft(CommunityRepository.RecipeIngredientDraft item) {
        ProductDto product = new ProductDto();
        product.setId(item.productId);
        product.setSku(item.productSku);
        product.setName(item.displayName);
        product.setImageUrl(item.imageUrl);
        return product;
    }

    private void showCategorySheet() {
        if (categories.isEmpty()) {
            Toast.makeText(this, "Dang tai danh muc", Toast.LENGTH_SHORT).show();
            loadCategories(() -> {
                if (categories.isEmpty()) {
                    Toast.makeText(this, "Chua tai duoc danh muc", Toast.LENGTH_SHORT).show();
                    return;
                }
                showCategorySheet();
            });
            return;
        }
        if (categories.isEmpty()) {
            Toast.makeText(this, "Chưa tải được danh mục", Toast.LENGTH_SHORT).show();
            loadCategories();
            return;
        }
        Dialog dialog = bottomSheetDialog();
        LinearLayout container = pickerContainer("Chọn danh mục", "Chọn nhóm phù hợp cho công thức của bạn");
        LinearLayout list = scrollList(container, dp(420));
        for (CommunityCategoryEntity category : categories) {
            list.addView(categoryRow(category, v -> {
                selectCategory(category);
                dialog.dismiss();
            }));
        }
        dialog.setContentView(container);
        showBottomSheet(dialog);
    }

    private void selectCategory(CommunityCategoryEntity category) {
        if (category == null) {
            return;
        }
        selectedCategoryId = category.getId();
        updateCategoryLabel();
        updateDraftButtonState();
    }

    private void updateCategoryLabel() {
        CommunityCategoryEntity selected = null;
        for (CommunityCategoryEntity category : categories) {
            if (category.getId().equals(selectedCategoryId)) {
                selected = category;
                break;
            }
        }
        if (selected == null) {
            categoryButton.setText("Chọn danh mục");
            categoryButton.setTextColor(getColor(R.color.neutral_60));
            return;
        }
        String icon = selected.getIconEmoji();
        categoryButton.setText((TextUtils.isEmpty(icon) ? "" : icon + " ") + selected.getName());
        categoryButton.setTextColor(getColor(R.color.neutral_100));
    }

    private void showProductPicker() {
        if (products.isEmpty()) {
            Toast.makeText(this, "Chưa tải được sản phẩm", Toast.LENGTH_SHORT).show();
            loadProducts(() -> {
                if (products.isEmpty()) {
                    Toast.makeText(this, "Chua tai duoc san pham", Toast.LENGTH_SHORT).show();
                    return;
                }
                showProductPicker();
            });
            return;
        }
        Dialog dialog = bottomSheetDialog();
        LinearLayout container = pickerContainer("Chọn nguyên liệu", "");
        EditText searchInput = searchInput();
        container.addView(searchInput);
        LinearLayout list = scrollList(container, dp(360));
        Runnable renderAll = () -> renderProductRows(list, dialog, text(searchInput));
        searchInput.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable editable) {
                renderAll.run();
            }
        });
        renderAll.run();
        dialog.setContentView(container);
        showBottomSheet(dialog);
    }

    private void renderProductRows(LinearLayout list, Dialog dialog, String query) {
        list.removeAllViews();
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        for (ProductDto product : products) {
            String name = product.getName() == null ? "" : product.getName();
            if (!TextUtils.isEmpty(lowerQuery) && !name.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                continue;
            }
            list.addView(productRow(product, v -> {
                selectedIngredients.add(new IngredientSelection(product));
                renderIngredients();
                updateDraftButtonState();
                dialog.dismiss();
            }));
        }
    }

    private void renderIngredients() {
        ingredientsList.removeAllViews();
        for (IngredientSelection selection : selectedIngredients) {
            ingredientsList.addView(ingredientRow(selection));
        }
    }

    private View ingredientRow(IngredientSelection selection) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackgroundResource(R.drawable.bg_post_upload);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = dp(6);
        row.setLayoutParams(rowParams);

        ImageView image = new ImageView(this);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        row.addView(image, imageParams);
        Glide.with(this).load(selection.product.getImageUrl()).transform(new CenterCrop(), new RoundedCorners(dp(8))).into(image);

        TextView name = new TextView(this);
        name.setText(selection.product.getName());
        name.setTextColor(getColor(R.color.neutral_100));
        name.setTextSize(12);
        name.setTypeface(name.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameParams.setMarginStart(dp(10));
        row.addView(name, nameParams);

        EditText quantity = new EditText(this);
        quantity.setText(selection.quantity);
        quantity.setHint("100g");
        quantity.setSingleLine(true);
        quantity.setTextSize(12);
        quantity.setBackgroundResource(R.drawable.bg_post_input);
        quantity.setPadding(dp(8), 0, dp(8), 0);
        quantity.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable editable) {
                selection.quantity = editable.toString().trim();
                updateDraftButtonState();
            }
        });
        row.addView(quantity, new LinearLayout.LayoutParams(dp(76), dp(38)));

        TextView remove = new TextView(this);
        remove.setText("×");
        remove.setGravity(Gravity.CENTER);
        remove.setTextColor(getColor(R.color.danger_main));
        remove.setTextSize(20);
        remove.setOnClickListener(v -> {
            selectedIngredients.remove(selection);
            renderIngredients();
            updateDraftButtonState();
        });
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(dp(32), dp(38));
        removeParams.setMarginStart(dp(4));
        row.addView(remove, removeParams);
        return row;
    }

    private View categoryRow(CommunityCategoryEntity category, View.OnClickListener listener) {
        String icon = category.getIconEmoji();
        return optionRow((TextUtils.isEmpty(icon) ? "" : icon + " ") + category.getName(), category.getRecipeCount() + " công thức", listener);
    }

    private View productRow(ProductDto product, View.OnClickListener listener) {
        return optionRow(product.getName(), "", listener);
    }

    private View optionRow(String title, String subtitle, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        row.setBackgroundResource(R.drawable.bg_post_upload);
        row.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(8);
        row.setLayoutParams(params);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(getColor(R.color.neutral_100));
        titleView.setTextSize(14);
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
        row.addView(titleView);

        if (!TextUtils.isEmpty(subtitle)) {
            TextView subtitleView = new TextView(this);
            subtitleView.setText(subtitle);
            subtitleView.setTextColor(getColor(R.color.neutral_70));
            subtitleView.setTextSize(11);
            row.addView(subtitleView);
        }
        return row;
    }

    private LinearLayout pickerContainer(String title, String subtitle) {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(18), dp(22), dp(22));
        content.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(getColor(R.color.neutral_100));
        titleView.setTextSize(18);
        titleView.setTypeface(titleView.getTypeface(), Typeface.BOLD);
        content.addView(titleView);

        if (!TextUtils.isEmpty(subtitle)) {
            TextView subtitleView = new TextView(this);
            subtitleView.setText(subtitle);
            subtitleView.setTextColor(getColor(R.color.neutral_70));
            subtitleView.setTextSize(12);
            content.addView(subtitleView);
        }
        return content;
    }

    private LinearLayout scrollList(LinearLayout container, int height) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
        );
        params.topMargin = dp(10);
        container.addView(scroll, params);
        return list;
    }

    private EditText searchInput() {
        EditText input = new EditText(this);
        input.setHint("Tìm sản phẩm");
        input.setSingleLine(true);
        input.setTextSize(12);
        input.setBackgroundResource(R.drawable.bg_post_input);
        input.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
        );
        params.topMargin = dp(12);
        input.setLayoutParams(params);
        return input;
    }

    private Dialog bottomSheetDialog() {
        return new Dialog(this);
    }

    private void showBottomSheet(Dialog dialog) {
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void addSelectedImages(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) {
            return;
        }
        for (Uri uri : uris) {
            if (uri != null) {
                addImageUri(copyImageToLocalFile(uri), "Chỉ chọn tối đa 6 ảnh");
            }
        }
        renderImagePreviews();
    }

    private void showImageSourceDialog() {
        Dialog dialog = bottomSheetDialog();
        dialog.setContentView(R.layout.bottom_sheet_community_image_source);
        dialog.findViewById(R.id.communityImageSourceGallery).setOnClickListener(v -> {
            if (selectedImageUris.size() >= MAX_IMAGES) {
                Toast.makeText(this, "Đã đủ 6 ảnh", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }
            galleryPicker.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
            dialog.dismiss();
        });
        dialog.findViewById(R.id.communityImageSourceCamera).setOnClickListener(v -> {
            openCamera();
            dialog.dismiss();
        });
        showBottomSheet(dialog);
    }

    private void openCamera() {
        if (selectedImageUris.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Chỉ chụp tối đa 6 ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        launchCameraCapture();
    }

    private void launchCameraCapture() {
        try {
            pendingCameraUri = createImageUri();
            cameraPicker.launch(pendingCameraUri);
        } catch (IOException exception) {
            Toast.makeText(this, "Chưa mở được camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCameraResult(Boolean success) {
        if (Boolean.TRUE.equals(success) && pendingCameraUri != null) {
            addImageUri(pendingCameraFile == null ? pendingCameraUri.toString() : pendingCameraFile.getAbsolutePath(), "Chỉ chụp tối đa 6 ảnh");
            renderImagePreviews();
            askContinueCamera();
        }
        pendingCameraUri = null;
        pendingCameraFile = null;
    }

    private void askContinueCamera() {
        if (selectedImageUris.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Đã đủ 6 ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Chụp ảnh")
                .setMessage("Bạn có muốn chụp thêm ảnh không?")
                .setPositiveButton("Có", (dialog, which) -> openCamera())
                .setNegativeButton("Không", null)
                .show();
    }

    private void addImageUri(String imageUri, String fullMessage) {
        if (selectedImageUris.size() >= MAX_IMAGES) {
            Toast.makeText(this, fullMessage, Toast.LENGTH_SHORT).show();
            return;
        }
        selectedImageUris.add(imageUri);
    }

    private Uri createImageUri() throws IOException {
        File directory = imageDirectory();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        pendingCameraFile = File.createTempFile("recipe_", ".jpg", directory);
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pendingCameraFile);
    }

    private String copyImageToLocalFile(Uri sourceUri) {
        try {
            File target = File.createTempFile("recipe_", ".jpg", imageDirectory());
            try (InputStream input = getContentResolver().openInputStream(sourceUri);
                 FileOutputStream output = new FileOutputStream(target)) {
                if (input == null) {
                    return sourceUri.toString();
                }
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
            return target.getAbsolutePath();
        } catch (IOException exception) {
            return sourceUri.toString();
        }
    }

    private File imageDirectory() {
        File directory = new File(getFilesDir(), "community_images");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    private void renderImagePreviews() {
        imagePreviewRow.removeAllViews();
        imagePreviewScroll.setVisibility(selectedImageUris.isEmpty() ? View.GONE : View.VISIBLE);
        for (String imageUri : selectedImageUris) {
            addPreviewImage(imageUri);
        }
        updateDraftButtonState();
    }

    private void addPreviewImage(String imageUri) {
        if (TextUtils.isEmpty(imageUri)) {
            return;
        }

        FrameLayout frame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(dp(92), dp(92));
        if (imagePreviewRow.getChildCount() > 0) {
            frameParams.setMarginStart(dp(8));
        }
        frame.setLayoutParams(frameParams);

        ImageView image = new ImageView(this);
        image.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(image);
        Glide.with(this)
                .load(imageUri)
                .transform(new CenterCrop(), new RoundedCorners(dp(10)))
                .into(image);

        ImageView delete = new ImageView(this);
        delete.setImageResource(R.drawable.ic_trash);
        delete.setColorFilter(Color.WHITE);
        delete.setBackgroundResource(R.drawable.bg_community_save);
        delete.setPadding(dp(7), dp(7), dp(7), dp(7));
        FrameLayout.LayoutParams deleteParams = new FrameLayout.LayoutParams(dp(32), dp(32), Gravity.TOP | Gravity.END);
        deleteParams.setMargins(0, dp(5), dp(5), 0);
        frame.addView(delete, deleteParams);
        delete.setOnClickListener(v -> showDeleteImageDialog(imageUri));
        frame.setOnLongClickListener(v -> {
            showDeleteImageDialog(imageUri);
            return true;
        });

        imagePreviewRow.addView(frame);
    }

    private void showDeleteImageDialog(String imageUri) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_veggo);
        ((TextView) dialog.findViewById(R.id.tvTitle)).setText("Xóa ảnh này?");
        ((TextView) dialog.findViewById(R.id.tvMessage)).setText("Ảnh sẽ được gỡ khỏi công thức khi bạn lưu thay đổi.");
        ((TextView) dialog.findViewById(R.id.btnConfirm)).setText("Xóa");
        ((TextView) dialog.findViewById(R.id.btnCancel)).setText("Hủy");
        dialog.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
            selectedImageUris.remove(imageUri);
            renderImagePreviews();
            dialog.dismiss();
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.45f);
        }
    }

    private boolean validateDraftHasContent() {
        clearValidationErrors();
        if (hasDraftContent()) {
            return true;
        }
        showValidationError(formError, "Vui lòng nhập ít nhất 1 thông tin để lưu bản nháp");
        scrollToView(formError);
        return false;
    }

    private boolean hasDraftContent() {
        return !TextUtils.isEmpty(text(titleInput))
                || !TextUtils.isEmpty(selectedCategoryId)
                || !TextUtils.isEmpty(text(timeInput))
                || !selectedImageUris.isEmpty()
                || !TextUtils.isEmpty(text(videoInput))
                || !selectedIngredients.isEmpty()
                || !TextUtils.isEmpty(buildStepsText())
                || !TextUtils.isEmpty(text(caloriesInput))
                || !TextUtils.isEmpty(text(saltInput))
                || !TextUtils.isEmpty(text(sugarInput));
    }

    private void setupDraftButtonVisibilityTracking() {
        TextWatcher watcher = new SimpleWatcher() {
            @Override public void afterTextChanged(Editable editable) {
                updateDraftButtonState();
            }
        };
        titleInput.addTextChangedListener(watcher);
        timeInput.addTextChangedListener(watcher);
        videoInput.addTextChangedListener(watcher);
        caloriesInput.addTextChangedListener(watcher);
        saltInput.addTextChangedListener(watcher);
        sugarInput.addTextChangedListener(watcher);
    }

    private void setStepTexts(List<String> steps) {
        stepsList.removeAllViews();
        stepInputs.clear();
        int count = Math.max(3, steps == null ? 0 : steps.size());
        for (int i = 0; i < count; i++) {
            String value = steps != null && i < steps.size() ? steps.get(i) : "";
            addStepInput(value, hintForStep(i + 1));
        }
    }

    private void addStepInput(String value, @Nullable String hint) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        if (stepsList.getChildCount() > 0) {
            rowParams.topMargin = dp(8);
        }
        row.setLayoutParams(rowParams);

        TextView number = new TextView(this);
        number.setGravity(Gravity.CENTER);
        number.setTextColor(getColor(R.color.primary_main));
        number.setTextSize(13);
        number.setTypeface(number.getTypeface(), Typeface.BOLD);
        number.setBackgroundResource(R.drawable.bg_community_circle);
        LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        numberParams.topMargin = dp(3);
        row.addView(number, numberParams);

        EditText input = new EditText(this);
        input.setText(value == null ? "" : value);
        input.setHint(hint == null ? hintForStep(stepInputs.size() + 1) : hint);
        input.setBackgroundResource(R.drawable.bg_post_input);
        input.setGravity(Gravity.TOP);
        input.setMinHeight(dp(52));
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setSingleLine(false);
        input.setTextColor(getColor(R.color.neutral_100));
        input.setHintTextColor(getColor(R.color.neutral_60));
        input.setTextSize(12);
        input.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable editable) {
                updateDraftButtonState();
            }
        });
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputParams.setMarginStart(dp(8));
        row.addView(input, inputParams);

        TextView remove = new TextView(this);
        remove.setText("×");
        remove.setGravity(Gravity.CENTER);
        remove.setTextColor(getColor(R.color.danger_main));
        remove.setTextSize(18);
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(dp(32), dp(40));
        removeParams.setMarginStart(dp(4));
        row.addView(remove, removeParams);
        remove.setVisibility(stepInputs.size() >= 3 ? View.VISIBLE : View.INVISIBLE);
        remove.setOnClickListener(v -> {
            stepsList.removeView(row);
            stepInputs.remove(input);
            renumberStepInputs();
        });

        stepsList.addView(row);
        stepInputs.add(input);
        renumberStepInputs();
    }

    private void renumberStepInputs() {
        for (int i = 0; i < stepsList.getChildCount(); i++) {
            View child = stepsList.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                if (row.getChildCount() > 0 && row.getChildAt(0) instanceof TextView) {
                    ((TextView) row.getChildAt(0)).setText(String.valueOf(i + 1));
                }
                if (row.getChildCount() > 2) {
                    row.getChildAt(2).setVisibility(i >= 3 ? View.VISIBLE : View.INVISIBLE);
                }
            }
        }
    }

    private String buildStepsText() {
        StringBuilder builder = new StringBuilder();
        int stepNumber = 1;
        for (EditText input : stepInputs) {
            String value = text(input);
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("Bước ").append(stepNumber).append(": ").append(value);
            stepNumber++;
        }
        return builder.toString();
    }

    private List<String> splitSteps(String stepsText) {
        List<String> steps = new ArrayList<>();
        if (TextUtils.isEmpty(stepsText)) {
            return steps;
        }
        String[] lines = stepsText.split("\\r?\\n");
        for (String line : lines) {
            String value = line == null ? "" : line.trim();
            if (TextUtils.isEmpty(value)) {
                continue;
            }
            value = value.replaceFirst("(?iu)^\\s*(bước|buoc)\\s*\\d+\\s*[:.\\-]?\\s*", "").trim();
            if (!TextUtils.isEmpty(value)) {
                steps.add(value);
            }
        }
        return steps;
    }

    private String hintForStep(int stepNumber) {
        switch (stepNumber) {
            case 1:
                return "Bước 1: Sơ chế nguyên liệu";
            case 2:
                return "Bước 2: Nấu hoặc chế biến món ăn";
            case 3:
                return "Bước 3: Trình bày và thưởng thức";
            default:
                return "Bước " + stepNumber + ": Nhập hướng dẫn tiếp theo";
        }
    }

    private String text(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private int parseInt(String value) {
        try {
            return TextUtils.isEmpty(value) ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private static class IngredientSelection {
        final ProductDto product;
        String quantity = "";

        IngredientSelection(ProductDto product) {
            this.product = product;
        }
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence text, int start, int before, int count) {}
    }
}
