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
import com.veggo.app.data.remote.dto.FridgeItemDto;
import com.veggo.app.data.remote.dto.FridgeLocationDto;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FridgeIngredientDetailActivity extends BaseActivity {

    private EditText nameInput, quantityInput, unitInput;
    private TextView locationText, purchaseDateText, expiryDateText;
    private LinearLayout fridgeImageList;

    private final List<Uri> selectedImages = new ArrayList<>();
    private final List<String> locationOptions = new ArrayList<>();
    private final List<FridgeLocationDto> fetchedLocations = new ArrayList<>();
    private int selectedLocationIndex = -1;

    private Calendar purchaseCal = Calendar.getInstance();
    private Calendar expiryCal = null;
    private Uri cameraImageUri = null;
    private boolean purchaseDateSet = false;

    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    private FridgeItemDto currentItem;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                            addImage(uri);
                        }
                    } else if (result.getData().getData() != null) {
                        addImage(result.getData().getData());
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                    addImage(cameraImageUri);
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
        setContentView(R.layout.activity_fridge_ingredient_detail);

        bindViews();
        fetchLocations();
        
        String json = getIntent().getStringExtra("ITEM_JSON");
        if (json != null) {
            currentItem = new Gson().fromJson(json, FridgeItemDto.class);
            preFillData();
        } else {
            Toast.makeText(this, "Không có dữ liệu", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        setupListeners();
    }

    private void bindViews() {
        nameInput = findViewById(R.id.fridgeIngredientNameInput);
        quantityInput = findViewById(R.id.fridgeQuantityInput);
        unitInput = findViewById(R.id.fridgeUnitInput);
        locationText = findViewById(R.id.fridgeLocationText);
        purchaseDateText = findViewById(R.id.fridgePurchaseDateText);
        expiryDateText = findViewById(R.id.fridgeExpiryDateText);
        fridgeImageList = findViewById(R.id.fridgeImageList);
    }

    private void fetchLocations() {
        locationOptions.clear();
        fetchedLocations.clear();
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
                            if (currentItem != null && loc.getLocationCode().equals(currentItem.getLocationCode())) {
                                selectedLocationIndex = locationOptions.size() - 1;
                                locationText.setText(loc.getName());
                                locationText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
                            }
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void preFillData() {
        if (currentItem.getName() != null) nameInput.setText(currentItem.getName());
        
        String qtyStr = String.valueOf(currentItem.getQuantity());
        if (qtyStr.endsWith(".0")) qtyStr = qtyStr.substring(0, qtyStr.length() - 2);
        quantityInput.setText(qtyStr);
        
        if (currentItem.getUnit() != null) unitInput.setText(currentItem.getUnit());

        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        try {
            if (currentItem.getPurchaseDate() != null) {
                Date pDate = isoFormat.parse(currentItem.getPurchaseDate());
                if (pDate != null) {
                    purchaseCal.setTime(pDate);
                    purchaseDateSet = true;
                    purchaseDateText.setText(displayFormat.format(pDate));
                    purchaseDateText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
                }
            }
            if (currentItem.getExpiryDate() != null) {
                Date eDate = isoFormat.parse(currentItem.getExpiryDate());
                if (eDate != null) {
                    expiryCal = Calendar.getInstance();
                    expiryCal.setTime(eDate);
                    expiryDateText.setText(displayFormat.format(eDate));
                    expiryDateText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (currentItem.getImages() != null && !currentItem.getImages().isEmpty()) {
            for (String uriString : currentItem.getImages()) {
                selectedImages.add(Uri.parse(uriString));
            }
        } else if (currentItem.getImage() != null && !currentItem.getImage().isEmpty()) {
            selectedImages.add(Uri.parse(currentItem.getImage()));
        }
        refreshImageList();
    }

    private void setupListeners() {
        findViewById(R.id.addFridgeBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.detailFridgeSaveButton).setOnClickListener(v -> saveIngredient());
        findViewById(R.id.detailFridgeDeleteButton).setOnClickListener(v -> deleteIngredient());
        findViewById(R.id.fridgePurchaseDatePicker).setOnClickListener(v -> showPurchaseDatePicker());
        findViewById(R.id.fridgeExpiryDatePicker).setOnClickListener(v -> showExpiryDatePicker());
        findViewById(R.id.fridgeLocationDropdown).setOnClickListener(v -> showLocationDropdown());
        findViewById(R.id.fridgeAddImageBtn).setOnClickListener(v -> showImageSourceDialog());
    }

    private void showPurchaseDatePicker() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            purchaseCal.set(year, month, day);
            purchaseDateSet = true;
            purchaseDateText.setText(displayFormat.format(purchaseCal.getTime()));
            purchaseDateText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
        }, purchaseCal.get(Calendar.YEAR), purchaseCal.get(Calendar.MONTH), purchaseCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showExpiryDatePicker() {
        Calendar initCal = expiryCal != null ? expiryCal : Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            if (expiryCal == null) expiryCal = Calendar.getInstance();
            expiryCal.set(year, month, day);
            expiryDateText.setText(displayFormat.format(expiryCal.getTime()));
            expiryDateText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
        }, initCal.get(Calendar.YEAR), initCal.get(Calendar.MONTH), initCal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showLocationDropdown() {
        List<String> displayOptions = new ArrayList<>(locationOptions);
        displayOptions.add("+ Thêm vị trí mới...");
        String[] items = displayOptions.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Chọn vị trí")
                .setItems(items, (dialog, which) -> {
                    if (which == displayOptions.size() - 1) {
                        showAddLocationDialog();
                    } else {
                        selectedLocationIndex = which;
                        locationText.setText(locationOptions.get(which));
                        locationText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
                    }
                })
                .show();
    }

    private void showAddLocationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_fridge_add_location, null);
        EditText input = dialogView.findViewById(R.id.dialogLocationNameInput);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.dialogLocationCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.dialogLocationConfirm).setOnClickListener(v -> {
            String newLocation = input.getText().toString().trim();
            if (newLocation.isEmpty()) {
                input.setError("Vui lòng nhập tên vị trí");
                return;
            }
            locationOptions.add(newLocation);
            selectedLocationIndex = locationOptions.size() - 1;
            locationText.setText(newLocation);
            locationText.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
            dialog.dismiss();
        });
        dialog.show();
    }

    private static final int MAX_IMAGES = 5;

    private void showImageSourceDialog() {
        if (selectedImages.size() >= MAX_IMAGES) return;
        new AlertDialog.Builder(this)
                .setTitle("Thêm ảnh")
                .setItems(new String[]{"Chọn từ thư viện", "Chụp ảnh bằng camera"}, (dialog, which) -> {
                    if (which == 0) checkGalleryPermAndOpen();
                    else checkCameraPermAndOpen();
                }).show();
    }

    private void checkGalleryPermAndOpen() {
        if (selectedImages.size() >= MAX_IMAGES) return;
        String perm = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) launchGallery();
        else galleryPermLauncher.launch(new String[]{perm});
    }

    private void checkCameraPermAndOpen() {
        if (selectedImages.size() >= MAX_IMAGES) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera();
        else cameraPermLauncher.launch(new String[]{Manifest.permission.CAMERA});
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
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("FRIDGE_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void addImage(Uri uri) {
        if (selectedImages.size() < MAX_IMAGES) {
            selectedImages.add(uri);
            refreshImageList();
        }
    }

    private void removeImage(int index) {
        if (index >= 0 && index < selectedImages.size()) {
            selectedImages.remove(index);
            refreshImageList();
        }
    }

    private void refreshImageList() {
        while (fridgeImageList.getChildCount() > 1) {
            fridgeImageList.removeViewAt(fridgeImageList.getChildCount() - 1);
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < selectedImages.size(); i++) {
            final int index = i;
            View itemView = inflater.inflate(R.layout.item_fridge_image, fridgeImageList, false);
            ImageView thumb = itemView.findViewById(R.id.fridgeImageThumb);
            Glide.with(this).load(selectedImages.get(i)).centerCrop().into(thumb);
            itemView.findViewById(R.id.fridgeImageDeleteBtn).setOnClickListener(v -> removeImage(index));
            fridgeImageList.addView(itemView);
        }
        View addBtn = fridgeImageList.getChildAt(0);
        if (addBtn != null) addBtn.setVisibility(selectedImages.size() >= MAX_IMAGES ? View.GONE : View.VISIBLE);
    }

    private void saveIngredient() {
        String name = nameInput.getText().toString().trim();
        String quantityStr = quantityInput.getText().toString().trim();
        String unitStr = unitInput.getText().toString().trim();

        if (name.isEmpty()) {
            nameInput.setError("Vui lòng nhập tên nguyên liệu");
            return;
        }
        if (expiryCal == null) {
            Toast.makeText(this, "Vui lòng chọn hạn sử dụng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất 1 hình ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        double quantity = 1.0;
        if (!quantityStr.isEmpty()) {
            try { quantity = Double.parseDouble(quantityStr); } catch (NumberFormatException ignored) {}
        }

        final double finalQuantity = quantity;
        
        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String purchaseDateIso = purchaseDateSet ? isoFormat.format(purchaseCal.getTime()) : currentItem.getPurchaseDate();
        String expiryDateIso = isoFormat.format(expiryCal.getTime());

        String customerId = new AppPreferences(this).getCustomerId();
        if (customerId == null || customerId.isEmpty()) return;

        findViewById(R.id.detailFridgeSaveButton).setEnabled(false);

        String selectedLocationName = selectedLocationIndex >= 0 ? locationOptions.get(selectedLocationIndex) : null;
        FridgeApi api = ApiClient.createService(FridgeApi.class);

        new Thread(() -> {
            try {
                String locationCode = currentItem.getLocationCode();
                if (selectedLocationName != null) {
                    boolean isNew = true;
                    for (FridgeLocationDto loc : fetchedLocations) {
                        if (loc.getName().equals(selectedLocationName)) {
                            isNew = false;
                            locationCode = loc.getLocationCode();
                            break;
                        }
                    }
                    if (isNew) {
                        FridgeLocationDto newLocReq = new FridgeLocationDto("LOC_" + System.currentTimeMillis(), selectedLocationName);
                        retrofit2.Response<FridgeLocationDto> locRes = api.addLocation(customerId, newLocReq).execute();
                        if (locRes.isSuccessful() && locRes.body() != null) locationCode = locRes.body().getLocationCode();
                    }
                }

                String primaryImage = selectedImages.isEmpty() ? null : selectedImages.get(0).toString();
                List<String> imageUriStrings = new ArrayList<>();
                for (Uri uri : selectedImages) imageUriStrings.add(uri.toString());

                currentItem.setName(name);
                currentItem.setQuantity(finalQuantity);
                currentItem.setUnit(unitStr);
                currentItem.setPurchaseDate(purchaseDateIso);
                currentItem.setExpiryDate(expiryDateIso);
                currentItem.setLocationCode(locationCode);
                currentItem.setImage(primaryImage);
                currentItem.setImages(imageUriStrings);

                retrofit2.Response<FridgeItemDto> response = api.updateFridgeItem(customerId, currentItem.getId(), currentItem).execute();
                runOnUiThread(() -> {
                    findViewById(R.id.detailFridgeSaveButton).setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Lỗi cập nhật nguyên liệu", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    findViewById(R.id.detailFridgeSaveButton).setEnabled(true);
                    Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void deleteIngredient() {
        new AlertDialog.Builder(this)
            .setTitle("Xóa nguyên liệu")
            .setMessage("Bạn có chắc chắn muốn xóa nguyên liệu này khỏi tủ lạnh?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                String customerId = new AppPreferences(this).getCustomerId();
                if (customerId == null || customerId.isEmpty()) return;

                findViewById(R.id.detailFridgeDeleteButton).setEnabled(false);
                FridgeApi api = ApiClient.createService(FridgeApi.class);

                new Thread(() -> {
                    try {
                        retrofit2.Response<Void> response = api.deleteFridgeItem(customerId, currentItem.getId()).execute();
                        runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                Toast.makeText(this, "Đã xóa nguyên liệu", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                findViewById(R.id.detailFridgeDeleteButton).setEnabled(true);
                                Toast.makeText(this, "Lỗi xóa nguyên liệu", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            findViewById(R.id.detailFridgeDeleteButton).setEnabled(true);
                            Toast.makeText(this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
}
