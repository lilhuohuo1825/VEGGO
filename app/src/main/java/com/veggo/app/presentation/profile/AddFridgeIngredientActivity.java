package com.veggo.app.presentation.profile;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.data.remote.api.FridgeApi;
import com.veggo.app.data.remote.dto.AiRecognitionRequestDto;
import com.veggo.app.data.remote.dto.AiRecognitionItemDto;
import com.veggo.app.data.remote.dto.FridgeBatchRequestDto;
import com.veggo.app.data.remote.dto.FridgeLocationDto;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AddFridgeIngredientActivity extends BaseActivity {

    private LinearLayout blocksContainer;
    private final List<ManualBlockViewHolder> blocks = new ArrayList<>();

    private final List<String> locationOptions = new ArrayList<>();
    private final List<FridgeLocationDto> fetchedLocations = new ArrayList<>();

    private Uri cameraImageUri = null;
    private int activeBlockIndex = -1; // Track which block initiated camera/gallery

    private static final int MODE_BLOCK_IMAGE = 0;
    private static final int MODE_SCAN_RECEIPT = 1;
    private static final int MODE_SCAN_INGREDIENT = 2;
    private int cameraMode = MODE_BLOCK_IMAGE;
    private boolean isFromNavbar = false;

    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    class ManualBlockViewHolder {
        View view;
        EditText nameInput, quantityInput, unitInput;
        TextView locationText, purchaseDateText, expiryDateText;
        LinearLayout fridgeImageList;
        List<Uri> selectedImages = new ArrayList<>();
        Calendar purchaseCal = Calendar.getInstance();
        Calendar expiryCal = null;
        int selectedLocationIndex = -1;
        boolean purchaseDateSet = false;

        public ManualBlockViewHolder(View view) {
            this.view = view;
            nameInput = view.findViewById(R.id.fridgeIngredientNameInput);
            quantityInput = view.findViewById(R.id.fridgeQuantityInput);
            unitInput = view.findViewById(R.id.fridgeUnitInput);
            locationText = view.findViewById(R.id.fridgeLocationText);
            purchaseDateText = view.findViewById(R.id.fridgePurchaseDateText);
            expiryDateText = view.findViewById(R.id.fridgeExpiryDateText);
            fridgeImageList = view.findViewById(R.id.fridgeImageList);
        }
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (cameraMode == MODE_SCAN_RECEIPT) {
                        if (result.getData().getData() != null) {
                            analyzeImageWithAI(result.getData().getData());
                        } else if (result.getData().getClipData() != null && result.getData().getClipData().getItemCount() > 0) {
                            analyzeImageWithAI(result.getData().getClipData().getItemAt(0).getUri());
                        }
                    } else if (activeBlockIndex >= 0 && activeBlockIndex < blocks.size()) {
                        ManualBlockViewHolder block = blocks.get(activeBlockIndex);
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                                addImageToBlock(block, uri);
                            }
                        } else if (result.getData().getData() != null) {
                            addImageToBlock(block, result.getData().getData());
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                    if (cameraMode == MODE_SCAN_RECEIPT) {
                        analyzeImageWithAI(cameraImageUri);
                    } else if (cameraMode == MODE_SCAN_INGREDIENT && activeBlockIndex >= 0 && activeBlockIndex < blocks.size()) {
                        ManualBlockViewHolder block = blocks.get(activeBlockIndex);
                        if (isBlockEmpty(block)) {
                            addImageToBlock(block, cameraImageUri);
                            recognizeIngredientFromImage(cameraImageUri, block);
                        } else {
                            addManualBlock(null);
                            ManualBlockViewHolder newBlock = blocks.get(blocks.size() - 1);
                            addImageToBlock(newBlock, cameraImageUri);
                            recognizeIngredientFromImage(cameraImageUri, newBlock);
                        }
                    } else if (activeBlockIndex >= 0 && activeBlockIndex < blocks.size()) {
                        addImageToBlock(blocks.get(activeBlockIndex), cameraImageUri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<String[]> cameraPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            perms -> {
                if (Boolean.TRUE.equals(perms.get(Manifest.permission.CAMERA))) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Cần cấp quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String[]> galleryPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            perms -> {
                boolean granted = Boolean.TRUE.equals(perms.get(Manifest.permission.READ_MEDIA_IMAGES))
                        || Boolean.TRUE.equals(perms.get(Manifest.permission.READ_EXTERNAL_STORAGE));
                if (granted) {
                    launchGallery();
                } else {
                    Toast.makeText(this, "Cần cấp quyền truy cập thư viện ảnh", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "tủ lạnh thông minh")) {
            return;
        }
        setContentView(R.layout.activity_add_fridge_ingredient);

        blocksContainer = findViewById(R.id.fridgeManualBlocksContainer);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        fetchLocations();
        setupListeners();
        
        // Thêm 1 block rỗng mặc định ban đầu
        addManualBlock(null);
        
        String extraAiImageUri = getIntent().getStringExtra("EXTRA_AI_IMAGE_URI");
        String extraIngredientUri = getIntent().getStringExtra("EXTRA_AI_INGREDIENT_URI");
        isFromNavbar = getIntent().getBooleanExtra("EXTRA_FROM_NAVBAR", false);
        if (extraAiImageUri != null) {
            cameraMode = MODE_SCAN_RECEIPT;
            analyzeImageWithAI(Uri.parse(extraAiImageUri));
        } else if (extraIngredientUri != null && !blocks.isEmpty()) {
            cameraMode = MODE_SCAN_INGREDIENT;
            ManualBlockViewHolder firstBlock = blocks.get(0);
            Uri imgUri = Uri.parse(extraIngredientUri);
            addImageToBlock(firstBlock, imgUri);
            recognizeIngredientFromImage(imgUri, firstBlock);
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            android.view.View v = getCurrentFocus();
            if (v instanceof android.widget.EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void setupListeners() {
        findViewById(R.id.addFridgeBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.addFridgeCancelButton).setOnClickListener(v -> finish());
        findViewById(R.id.addFridgeSaveButton).setOnClickListener(v -> saveIngredients());
        
        findViewById(R.id.fridgeAddAnotherBlockBtn).setOnClickListener(v -> addManualBlock(null));

        // Nút SCAN HÓA ĐƠN
        View scanBtn = findViewById(R.id.addFridgeBackButton); // Temporary fallback, let's find the correct textview
        // It's a TextView without ID in activity_add_fridge_ingredient.xml, wait, let's find it by view traversal or just assume it's the one with text "SCAN HÓA ĐƠN".
        // Let's find it properly. Actually, we should assign an ID. But we can't easily now.
        // Let's iterate to find it.
        findScanButtonAndAttachListener(findViewById(android.R.id.content));
    }
    
    private void findScanButtonAndAttachListener(View view) {
        if (view instanceof TextView && "SCAN HÓA ĐƠN".equals(((TextView) view).getText().toString())) {
            view.setOnClickListener(v -> {
                cameraMode = MODE_SCAN_RECEIPT;
                android.app.Dialog dialog = new android.app.Dialog(this);
                dialog.setContentView(R.layout.dialog_image_source);
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
                }

                dialog.findViewById(R.id.dialogOptionGallery).setOnClickListener(viewOption -> {
                    dialog.dismiss();
                    checkGalleryPermAndOpen();
                });

                dialog.findViewById(R.id.dialogOptionCamera).setOnClickListener(viewOption -> {
                    dialog.dismiss();
                    checkCameraPermAndOpen();
                });

                dialog.show();
            });
            return;
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                findScanButtonAndAttachListener(group.getChildAt(i));
            }
        }
    }

    private void addManualBlock(AiRecognitionItemDto prefillData) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_fridge_manual_block, blocksContainer, false);
        ManualBlockViewHolder block = new ManualBlockViewHolder(view);
        blocks.add(block);
        blocksContainer.addView(view);

        // Thiết lập default purchase date
        block.purchaseDateSet = true;
        block.purchaseDateText.setText(displayFormat.format(block.purchaseCal.getTime()));
        block.purchaseDateText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));

        // Remove block listener
        view.findViewById(R.id.fridgeBlockRemoveBtn).setOnClickListener(v -> {
            blocksContainer.removeView(block.view);
            blocks.remove(block);
            updateTrashIconsVisibility();
        });

        // Date pickers
        view.findViewById(R.id.fridgePurchaseDatePicker).setOnClickListener(v -> showPurchaseDatePicker(block));
        view.findViewById(R.id.fridgeExpiryDatePicker).setOnClickListener(v -> showExpiryDatePicker(block));

        // Location
        view.findViewById(R.id.fridgeLocationDropdown).setOnClickListener(v -> showLocationDropdown(block));

        // Images
        view.findViewById(R.id.fridgeAddImageBtn).setOnClickListener(v -> showImageSourceDialog(block));

        fillBlockWithItem(block, prefillData);
        updateTrashIconsVisibility();
    }

    private boolean isBlockEmpty(ManualBlockViewHolder block) {
        String name = block.nameInput.getText().toString().trim();
        String qty = block.quantityInput.getText().toString().trim();
        String unit = block.unitInput.getText().toString().trim();
        
        boolean hasName = !name.isEmpty();
        boolean hasQty = !qty.isEmpty() && !qty.equals("1") && !qty.equals("1.0");
        boolean hasUnit = !unit.isEmpty();
        boolean hasExpiry = block.expiryCal != null;
        boolean hasImages = !block.selectedImages.isEmpty();

        return !hasName && !hasQty && !hasUnit && !hasExpiry && !hasImages;
    }

    private void fillBlockWithItem(ManualBlockViewHolder block, AiRecognitionItemDto prefillData) {
        if (prefillData == null) return;
        if (prefillData.getName() != null) block.nameInput.setText(prefillData.getName());
        if (prefillData.getQuantity() != null) {
            String qtyStr = String.valueOf(prefillData.getQuantity());
            if (qtyStr.endsWith(".0")) qtyStr = qtyStr.substring(0, qtyStr.length() - 2);
            block.quantityInput.setText(qtyStr);
        }
        if (prefillData.getUnit() != null) block.unitInput.setText(prefillData.getUnit());
        if (prefillData.getPurchaseDate() != null && !prefillData.getPurchaseDate().isEmpty()) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date d = fmt.parse(prefillData.getPurchaseDate());
                if (d != null) {
                    block.purchaseCal.setTime(d);
                    block.purchaseDateText.setText(displayFormat.format(d));
                    block.purchaseDateText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
                }
            } catch (Exception ignored) {}
        }
    }

    private void updateTrashIconsVisibility() {
        boolean showTrash = blocks.size() > 1;
        for (ManualBlockViewHolder b : blocks) {
            View trashBtn = b.view.findViewById(R.id.fridgeBlockRemoveBtn);
            if (trashBtn != null) trashBtn.setVisibility(showTrash ? View.VISIBLE : View.GONE);
        }
    }

    private void fetchLocations() {
        String customerId = new AppPreferences(this).getCustomerId();
        if (customerId == null || customerId.isEmpty()) return;

        new Thread(() -> {
            try {
                FridgeApi api = ApiClient.createService(FridgeApi.class);
                retrofit2.Response<List<FridgeLocationDto>> response = api.getLocations(customerId).execute();
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        fetchedLocations.addAll(response.body());
                        for (FridgeLocationDto loc : fetchedLocations) {
                            locationOptions.add(loc.getName());
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showPurchaseDatePicker(ManualBlockViewHolder block) {
        new DatePickerDialog(this, (view, year, month, day) -> {
            block.purchaseCal.set(year, month, day);
            block.purchaseDateSet = true;
            block.purchaseDateText.setText(displayFormat.format(block.purchaseCal.getTime()));
            block.purchaseDateText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
        }, block.purchaseCal.get(Calendar.YEAR), block.purchaseCal.get(Calendar.MONTH), block.purchaseCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showExpiryDatePicker(ManualBlockViewHolder block) {
        Calendar initCal = block.expiryCal != null ? block.expiryCal : Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            if (block.expiryCal == null) block.expiryCal = Calendar.getInstance();
            block.expiryCal.set(year, month, day);
            block.expiryDateText.setText(displayFormat.format(block.expiryCal.getTime()));
            block.expiryDateText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
        }, initCal.get(Calendar.YEAR), initCal.get(Calendar.MONTH), initCal.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void showLocationDropdown(ManualBlockViewHolder block) {
        List<String> displayOptions = new ArrayList<>(locationOptions);
        displayOptions.add("+ Thêm vị trí mới...");
        String[] items = displayOptions.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Chọn vị trí")
                .setItems(items, (dialog, which) -> {
                    if (which == displayOptions.size() - 1) {
                        showAddLocationDialog(block);
                    } else {
                        block.selectedLocationIndex = which;
                        block.locationText.setText(locationOptions.get(which));
                        block.locationText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
                    }
                })
                .show();
    }

    private void showAddLocationDialog(ManualBlockViewHolder block) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_fridge_add_location, null);
        EditText input = dialogView.findViewById(R.id.dialogLocationNameInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.dialogLocationCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.dialogLocationConfirm).setOnClickListener(v -> {
            String newLocation = input.getText().toString().trim();
            if (newLocation.isEmpty()) {
                input.setError("Vui lòng nhập tên vị trí");
                return;
            }
            locationOptions.add(newLocation);
            block.selectedLocationIndex = locationOptions.size() - 1;
            block.locationText.setText(newLocation);
            block.locationText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
            dialog.dismiss();
        });
        dialog.show();
    }

    private static final int MAX_IMAGES = 5;

    private void showImageSourceDialog(ManualBlockViewHolder block) {
        if (block.selectedImages.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Đã đạt giới hạn " + MAX_IMAGES + " ảnh", Toast.LENGTH_SHORT).show();
            return;
        }
        activeBlockIndex = blocks.indexOf(block);
        
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_image_source);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.findViewById(R.id.dialogOptionGallery).setOnClickListener(v -> {
            cameraMode = MODE_BLOCK_IMAGE;
            dialog.dismiss();
            checkGalleryPermAndOpen();
        });

        dialog.findViewById(R.id.dialogOptionCamera).setOnClickListener(v -> {
            cameraMode = MODE_SCAN_INGREDIENT;
            dialog.dismiss();
            checkCameraPermAndOpen();
        });

        dialog.show();
    }

    private void checkGalleryPermAndOpen() {
        String perm = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            launchGallery();
        } else {
            galleryPermLauncher.launch(new String[]{perm});
        }
    }

    private void checkCameraPermAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể mở camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("FRIDGE_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void addImageToBlock(ManualBlockViewHolder block, Uri uri) {
        if (block.selectedImages.size() >= MAX_IMAGES) return;
        block.selectedImages.add(uri);
        refreshBlockImages(block);
    }

    private void removeImageFromBlock(ManualBlockViewHolder block, int index) {
        if (index >= 0 && index < block.selectedImages.size()) {
            block.selectedImages.remove(index);
            refreshBlockImages(block);
        }
    }

    private void refreshBlockImages(ManualBlockViewHolder block) {
        while (block.fridgeImageList.getChildCount() > 1) {
            block.fridgeImageList.removeViewAt(block.fridgeImageList.getChildCount() - 1);
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < block.selectedImages.size(); i++) {
            final int index = i;
            View itemView = inflater.inflate(R.layout.item_fridge_image, block.fridgeImageList, false);
            ImageView thumb = itemView.findViewById(R.id.fridgeImageThumb);
            Glide.with(this).load(block.selectedImages.get(i)).centerCrop().into(thumb);
            itemView.findViewById(R.id.fridgeImageDeleteBtn).setOnClickListener(v -> removeImageFromBlock(block, index));
            block.fridgeImageList.addView(itemView);
        }
        View addBtn = block.fridgeImageList.getChildAt(0);
        if (addBtn != null) addBtn.setVisibility(block.selectedImages.size() >= MAX_IMAGES ? View.GONE : View.VISIBLE);
    }

    private void analyzeImageWithAI(Uri imageUri) {
        Toast.makeText(this, "AI đang phân tích hóa đơn...", Toast.LENGTH_LONG).show();

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                int maxDim = Math.max(bitmap.getWidth(), bitmap.getHeight());
                if (maxDim > 1000) {
                    float scale = 1000f / maxDim;
                    bitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * scale), (int)(bitmap.getHeight() * scale), true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                AiRecognitionRequestDto request = new AiRecognitionRequestDto(base64Image);
                FridgeApi api = ApiClient.createService(FridgeApi.class);
                retrofit2.Response<List<AiRecognitionItemDto>> response = api.recognizeIngredient(request).execute();

                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        List<AiRecognitionItemDto> items = response.body();
                        if (!items.isEmpty()) {
                            List<com.veggo.app.data.remote.dto.ProductDto> allMatches = new ArrayList<>();
                            for (int i = 0; i < items.size(); i++) {
                                AiRecognitionItemDto item = items.get(i);
                                if (item.getMatchedProducts() != null) {
                                    allMatches.addAll(item.getMatchedProducts());
                                }
                                if (i == 0) {
                                    ManualBlockViewHolder targetBlock = null;
                                    for (ManualBlockViewHolder b : blocks) {
                                        if (isBlockEmpty(b)) {
                                            targetBlock = b;
                                            break;
                                        }
                                    }
                                    if (targetBlock != null) {
                                        fillBlockWithItem(targetBlock, item);
                                    } else {
                                        addManualBlock(item);
                                    }
                                } else {
                                    addManualBlock(item);
                                }
                            }
                            if (isFromNavbar) {
                                if (!allMatches.isEmpty()) {
                                    if (items.size() == 1) {
                                        showSingleMatchedProductDialog(allMatches.get(0));
                                    } else {
                                        showMultiMatchedProductsDialog(allMatches);
                                    }
                                } else {
                                    showNoMatchDialog();
                                }
                            }
                            Toast.makeText(this, "Đã thêm " + items.size() + " nguyên liệu từ hóa đơn!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "AI không tìm thấy nguyên liệu nào trong ảnh", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "AI phân tích thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Lỗi phân tích AI", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void recognizeIngredientFromImage(Uri imageUri, ManualBlockViewHolder block) {
        Toast.makeText(this, "AI đang nhận diện nguyên liệu...", Toast.LENGTH_LONG).show();

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                int maxDim = Math.max(bitmap.getWidth(), bitmap.getHeight());
                if (maxDim > 1000) {
                    float scale = 1000f / maxDim;
                    bitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * scale), (int)(bitmap.getHeight() * scale), true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                AiRecognitionRequestDto request = new AiRecognitionRequestDto(base64Image);
                FridgeApi api = ApiClient.createService(FridgeApi.class);
                retrofit2.Response<List<AiRecognitionItemDto>> response = api.recognizeIngredient(request).execute();

                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        AiRecognitionItemDto item = response.body().get(0);
                        fillBlockWithItem(block, item);
                        if (isFromNavbar) {
                            if (item.getMatchedProducts() != null && item.getMatchedProducts().size() == 1) {
                                showSingleMatchedProductDialog(item.getMatchedProducts().get(0));
                            } else if (item.getMatchedProducts() != null && item.getMatchedProducts().size() > 1) {
                                showMultiMatchedProductsDialog(item.getMatchedProducts());
                            } else {
                                showNoMatchDialog();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Không nhận diện được nguyên liệu", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Lỗi nhận diện AI", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void showSingleMatchedProductDialog(com.veggo.app.data.remote.dto.ProductDto productDto) {
        com.veggo.app.domain.model.Product product = com.veggo.app.data.mapper.ProductMapper.fromDto(productDto);
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        dialog.setContentView(R.layout.dialog_matched_product_single);

        ImageView img = dialog.findViewById(R.id.ivProductImage);
        TextView name = dialog.findViewById(R.id.tvProductName);
        TextView weight = dialog.findViewById(R.id.tvWeight);
        TextView rating = dialog.findViewById(R.id.tvRating);
        TextView sold = dialog.findViewById(R.id.tvSold);
        TextView price = dialog.findViewById(R.id.tvPrice);
        TextView originalPrice = dialog.findViewById(R.id.tvOriginalPrice);
        TextView discountBadge = dialog.findViewById(R.id.tvDiscountBadge);

        if (name != null) name.setText(product.getName());
        if (price != null) price.setText(com.veggo.app.core.utils.CurrencyFormatter.formatVnd(product.getPrice()));
        if (img != null) Glide.with(this).load(product.getImageUrl()).into(img);

        if (weight != null) {
            String w = product.getWeight();
            weight.setText(w != null ? w : "");
        }
        if (rating != null) rating.setText(String.valueOf(product.getRating() == 0 ? 5.0f : product.getRating()));
        if (sold != null) sold.setText(product.getSoldCount() + " lượt mua");

        if (product.hasActiveDiscount()) {
            if (discountBadge != null) {
                discountBadge.setVisibility(View.VISIBLE);
                long discount = product.getOriginalPrice() - product.getPrice();
                int percentage = (int) ((discount * 100.0f) / product.getOriginalPrice());
                discountBadge.setText("-" + percentage + "%");
            }
            if (originalPrice != null) {
                originalPrice.setVisibility(View.VISIBLE);
                originalPrice.setText(com.veggo.app.core.utils.CurrencyFormatter.formatVnd(product.getOriginalPrice()));
                originalPrice.setPaintFlags(originalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            }
        } else {
            if (discountBadge != null) discountBadge.setVisibility(View.GONE);
            if (originalPrice != null) originalPrice.setVisibility(View.GONE);
        }

        View btnViewDetails = dialog.findViewById(R.id.btnViewDetails);
        if (btnViewDetails != null) {
            btnViewDetails.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(this, com.veggo.app.presentation.product.ProductDetailActivity.class);
                intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            });
        }

        View btnAddToCart = dialog.findViewById(R.id.btnAddToCart);
        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> {
                dialog.dismiss();
                String customerId = new com.veggo.app.core.preferences.AppPreferences(this).getCustomerId();
                if (customerId != null) {
                    com.veggo.app.data.remote.api.CartApi cartApi = ApiClient.createService(com.veggo.app.data.remote.api.CartApi.class);
                    new Thread(() -> {
                        try {
                            cartApi.addItem(customerId, new com.veggo.app.data.remote.dto.CartItemRequestDto(product.getSku(), 1)).execute();
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, com.veggo.app.MainActivity.class);
                                intent.putExtra(com.veggo.app.MainActivity.EXTRA_SELECTED_NAV_ITEM, R.id.nav_cart);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            });
        }

        dialog.show();
    }

    private void showMultiMatchedProductsDialog(List<com.veggo.app.data.remote.dto.ProductDto> productDtos) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        dialog.setContentView(R.layout.dialog_matched_products_list);
        
        androidx.recyclerview.widget.RecyclerView rv = dialog.findViewById(R.id.rvMatchedProducts);
        if (rv != null) {
            rv.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
            com.veggo.app.adapter.ProductAdapter adapter = new com.veggo.app.adapter.ProductAdapter();
            
            List<com.veggo.app.domain.model.Product> products = new ArrayList<>();
            for (com.veggo.app.data.remote.dto.ProductDto dto : productDtos) {
                products.add(com.veggo.app.data.mapper.ProductMapper.fromDto(dto));
            }
            
            adapter.setProducts(products);
            adapter.setOnProductClickListener(product -> {
                dialog.dismiss();
                Intent intent = new Intent(this, com.veggo.app.presentation.product.ProductDetailActivity.class);
                intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            });
            
            adapter.setOnAddProductClickListener(product -> {
                String customerId = new com.veggo.app.core.preferences.AppPreferences(this).getCustomerId();
                if (customerId != null) {
                    com.veggo.app.data.remote.api.CartApi cartApi = ApiClient.createService(com.veggo.app.data.remote.api.CartApi.class);
                    new Thread(() -> {
                        try {
                            cartApi.addItem(customerId, new com.veggo.app.data.remote.dto.CartItemRequestDto(product.getSku(), 1)).execute();
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Đã thêm " + product.getName() + " vào giỏ", Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            });
            
            rv.setAdapter(adapter);
        }
        
        dialog.show();
    }

    private void showNoMatchDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Không tìm thấy sản phẩm")
            .setMessage("Không có sản phẩm nào trên VEGGO khớp với nguyên liệu này.")
            .setPositiveButton("Đóng", null)
            .show();
    }

    private void saveIngredients() {
        if (blocks.isEmpty()) {
            Toast.makeText(this, "Không có nguyên liệu nào để lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        String customerId = new AppPreferences(this).getCustomerId();
        if (customerId == null || customerId.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        List<FridgeBatchRequestDto.FridgeItemRequestDto> requestItems = new ArrayList<>();

        for (ManualBlockViewHolder block : blocks) {
            String name = block.nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên nguyên liệu", Toast.LENGTH_SHORT).show();
                block.nameInput.requestFocus();
                return;
            }

            if (block.expiryCal == null) {
                Toast.makeText(this, "Vui lòng chọn hạn sử dụng cho: " + name, Toast.LENGTH_SHORT).show();
                return;
            }

            double quantity = 1.0;
            String quantityStr = block.quantityInput.getText().toString().trim();
            if (!quantityStr.isEmpty()) {
                try { quantity = Double.parseDouble(quantityStr); } catch (Exception ignored) {}
            }
            
            String purchaseDateIso = block.purchaseDateSet ? isoFormat.format(block.purchaseCal.getTime()) : null;
            String expiryDateIso = isoFormat.format(block.expiryCal.getTime());
            
            String selectedLoc = block.selectedLocationIndex >= 0 ? locationOptions.get(block.selectedLocationIndex) : null;
            
            String primaryImage = block.selectedImages.isEmpty() ? null : block.selectedImages.get(0).toString();
            List<String> imageUris = new ArrayList<>();
            for (Uri u : block.selectedImages) imageUris.add(u.toString());

            requestItems.add(new FridgeBatchRequestDto.FridgeItemRequestDto(
                    name, quantity, purchaseDateIso, expiryDateIso,
                    null, null, primaryImage, imageUris,
                    block.unitInput.getText().toString().trim(),
                    "manual", selectedLoc
            ));
        }

        findViewById(R.id.addFridgeSaveButton).setEnabled(false);
        FridgeApi api = ApiClient.createService(FridgeApi.class);

        new Thread(() -> {
            try {
                // Tạo mới các location chưa có
                for (FridgeBatchRequestDto.FridgeItemRequestDto reqItem : requestItems) {
                    if (reqItem.getLocationCode() != null) {
                        String locName = reqItem.getLocationCode(); // Temporarily storing name in code
                        boolean isNew = true;
                        for (FridgeLocationDto loc : fetchedLocations) {
                            if (loc.getName().equals(locName)) {
                                isNew = false;
                                reqItem.setLocationCode(loc.getLocationCode());
                                break;
                            }
                        }
                        if (isNew) {
                            FridgeLocationDto newLoc = new FridgeLocationDto("LOC_" + System.currentTimeMillis(), locName);
                            retrofit2.Response<FridgeLocationDto> locRes = api.addLocation(customerId, newLoc).execute();
                            if (locRes.isSuccessful() && locRes.body() != null) {
                                reqItem.setLocationCode(locRes.body().getLocationCode());
                                fetchedLocations.add(locRes.body());
                            } else {
                                reqItem.setLocationCode(null);
                            }
                        }
                    }
                }

                FridgeBatchRequestDto batchRequest = new FridgeBatchRequestDto(requestItems);
                retrofit2.Response<?> response = api.addBatchItems(customerId, batchRequest).execute();

                runOnUiThread(() -> {
                    findViewById(R.id.addFridgeSaveButton).setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(this, "Đã lưu thành công!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Lỗi lưu nguyên liệu", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    findViewById(R.id.addFridgeSaveButton).setEnabled(true);
                    Toast.makeText(this, "Không thể kết nối server", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
