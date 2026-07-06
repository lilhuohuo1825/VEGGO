package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.presentation.common.AssetScreenData;

import java.util.List;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmartFridgeActivity extends BaseActivity {

    private Uri cameraImageUri = null;
    private SwipeRefreshLayout fridgeRefreshLayout;

    private final ActivityResultLauncher<Intent> addIngredientLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Reload inventory automatically
                    loadInventory();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "tủ lạnh thông minh")) {
            return;
        }
        setContentView(R.layout.activity_smart_fridge);
        findViewById(R.id.smartFridgeBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.smartFridgeHistoryButton).setOnClickListener(v -> {
            startActivity(new Intent(this, com.veggo.app.presentation.order.OrderHistoryActivity.class));
        });
        findViewById(R.id.fridgeScanButton).setOnClickListener(v -> showImageSourceDialog());
        findViewById(R.id.fridgeManualButton).setOnClickListener(v -> openAddIngredient());
        findViewById(R.id.fridgeSuggestionButton).setOnClickListener(v ->
                startActivity(new Intent(this, FridgeSuggestionsActivity.class))
        );
        setupPullToRefresh();
    }

    private void setupPullToRefresh() {
        fridgeRefreshLayout = PullToRefreshHelper.wrap(
                findViewById(R.id.smartFridgeScroll),
                this::loadInventory
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInventory();
    }

    private void openAddIngredient() {
        addIngredientLauncher.launch(new Intent(this, AddFridgeIngredientActivity.class));
    }

    private void launchAddIngredientWithImage(Uri uri) {
        Intent intent = new Intent(this, AddFridgeIngredientActivity.class);
        intent.putExtra("EXTRA_AI_IMAGE_URI", uri.toString());
        addIngredientLauncher.launch(intent);
    }

    private void showImageSourceDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_image_source);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.findViewById(R.id.dialogOptionGallery).setOnClickListener(v -> {
            dialog.dismiss();
            checkGalleryPermAndOpen();
        });

        dialog.findViewById(R.id.dialogOptionCamera).setOnClickListener(v -> {
            dialog.dismiss();
            checkCameraPermAndOpen();
        });

        dialog.show();
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getData() != null) {
                        launchAddIngredientWithImage(result.getData().getData());
                    } else if (result.getData().getClipData() != null && result.getData().getClipData().getItemCount() > 0) {
                        launchAddIngredientWithImage(result.getData().getClipData().getItemAt(0).getUri());
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                    launchAddIngredientWithImage(cameraImageUri);
                }
            }
    );

    private final ActivityResultLauncher<String[]> cameraPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            perms -> {
                if (Boolean.TRUE.equals(perms.get(android.Manifest.permission.CAMERA))) {
                    launchCamera();
                } else {
                    android.widget.Toast.makeText(this, "Cần cấp quyền camera để chụp ảnh", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String[]> galleryPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            perms -> {
                boolean granted = Boolean.TRUE.equals(perms.get(android.Manifest.permission.READ_MEDIA_IMAGES))
                        || Boolean.TRUE.equals(perms.get(android.Manifest.permission.READ_EXTERNAL_STORAGE));
                if (granted) {
                    launchGallery();
                } else {
                    android.widget.Toast.makeText(this, "Cần cấp quyền truy cập thư viện ảnh", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void checkGalleryPermAndOpen() {
        String perm = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
                ? android.Manifest.permission.READ_MEDIA_IMAGES : android.Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) launchGallery();
        else galleryPermLauncher.launch(new String[]{perm});
    }

    private void checkCameraPermAndOpen() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera();
        else cameraPermLauncher.launch(new String[]{android.Manifest.permission.CAMERA});
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void launchCamera() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File photoFile = File.createTempFile("FRIDGE_" + timeStamp + "_", ".jpg", storageDir);
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Không thể mở camera", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void loadInventory() {
        new Thread(() -> {
            String customerId = new com.veggo.app.core.preferences.AppPreferences(this).getCustomerId();
            List<com.veggo.app.data.remote.dto.FridgeItemDto> fridgeItems = new java.util.ArrayList<>();
            try {
                if (customerId != null && !customerId.isEmpty()) {
                    com.veggo.app.data.remote.api.FridgeApi api = com.veggo.app.core.network.ApiClient.createService(com.veggo.app.data.remote.api.FridgeApi.class);
                    retrofit2.Response<List<com.veggo.app.data.remote.dto.FridgeItemDto>> response = api.getFridgeItems(customerId).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        fridgeItems = response.body();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            final List<com.veggo.app.data.remote.dto.FridgeItemDto> finalItems = fridgeItems;
            runOnUiThread(() -> {
                bindInventory(finalItems);
                PullToRefreshHelper.finish(fridgeRefreshLayout);
            });
        }).start();
    }

    private void updateHeaderCounts(List<com.veggo.app.data.remote.dto.FridgeItemDto> items) {
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.fridgeIngredientsCount, items.size() + " nguyên liệu");

        int expiringCount = 0;
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US);
        format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        long now = System.currentTimeMillis();

        for (com.veggo.app.data.remote.dto.FridgeItemDto item : items) {
            try {
                if (!com.veggo.app.core.notification.FridgeExpiryReminderHelper.isReminderEnabled(item)) {
                    continue;
                }
                if (item.getExpiryDate() != null) {
                    java.util.Date d = format.parse(item.getExpiryDate());
                    if (d != null) {
                        long diff = d.getTime() - now;
                        long daysLeft = diff / (1000L * 60 * 60 * 24);
                        if (daysLeft >= 0 && daysLeft <= com.veggo.app.core.notification.FridgeExpiryReminderHelper.REMIND_DAYS_BEFORE) {
                            expiringCount++;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.fridgeExpiringCount, expiringCount + " sắp hết hạn");
    }

    private void bindInventory(List<com.veggo.app.data.remote.dto.FridgeItemDto> items) {
        updateHeaderCounts(items);

        androidx.recyclerview.widget.RecyclerView list = findViewById(R.id.fridgeInventoryList);
        list.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        FridgeInventoryAdapter adapter = new FridgeInventoryAdapter(items, new FridgeInventoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(com.veggo.app.data.remote.dto.FridgeItemDto item) {
                Intent intent = new Intent(SmartFridgeActivity.this, FridgeIngredientDetailActivity.class);
                intent.putExtra("ITEM_JSON", new com.google.gson.Gson().toJson(item));
                addIngredientLauncher.launch(intent);
            }

            @Override
            public void onDeleteClick(com.veggo.app.data.remote.dto.FridgeItemDto item, int position) {
                com.veggo.app.presentation.dialog.VeggoDialog.show(
                    SmartFridgeActivity.this,
                    com.veggo.app.R.drawable.ic_trash,
                    "Xác nhận xóa nguyên liệu",
                    "Bạn có chắc chắn muốn xóa nguyên liệu này khỏi tủ lạnh?",
                    "Xóa",
                    "Hủy",
                    new com.veggo.app.presentation.dialog.VeggoDialog.DialogListener() {
                        @Override
                        public void onConfirm() {
                            new Thread(() -> {
                                try {
                                    String customerId = new com.veggo.app.core.preferences.AppPreferences(SmartFridgeActivity.this).getCustomerId();
                                    com.veggo.app.data.remote.api.FridgeApi api = com.veggo.app.core.network.ApiClient.createService(com.veggo.app.data.remote.api.FridgeApi.class);
                                    api.deleteFridgeItem(customerId, item.getId()).execute();
                                    runOnUiThread(() -> {
                                        androidx.recyclerview.widget.RecyclerView list = findViewById(R.id.fridgeInventoryList);
                                        if (list != null && list.getAdapter() instanceof FridgeInventoryAdapter) {
                                            FridgeInventoryAdapter adapt = (FridgeInventoryAdapter) list.getAdapter();
                                            adapt.removeItem(position);
                                            updateHeaderCounts(adapt.getItems());
                                        }
                                        android.widget.Toast.makeText(SmartFridgeActivity.this, "Đã xóa nguyên liệu", android.widget.Toast.LENGTH_SHORT).show();
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    runOnUiThread(() -> android.widget.Toast.makeText(SmartFridgeActivity.this, "Lỗi xóa nguyên liệu", android.widget.Toast.LENGTH_SHORT).show());
                                }
                            }).start();
                        }
                    }
                );
            }
        });
        list.setAdapter(adapter);


    }

}
