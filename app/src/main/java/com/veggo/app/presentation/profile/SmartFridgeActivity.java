package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.veggo.app.R;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.data.remote.api.FridgeApi;
import com.veggo.app.data.remote.dto.FridgeItemDto;
import com.veggo.app.presentation.common.AssetScreenData;
import com.veggo.app.presentation.dialog.VeggoDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SmartFridgeActivity extends BaseActivity {

    private SwipeRefreshLayout fridgeRefreshLayout;

    private final ActivityResultLauncher<Intent> addIngredientLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadInventory();
                }
            }
    );

    private final ActivityResultLauncher<Intent> quickScanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Intent intent = new Intent(this, AddFridgeIngredientActivity.class);
                FridgeQuickScanHelper.copyScanExtras(intent, result.getData());
                addIngredientLauncher.launch(intent);
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
        findViewById(R.id.smartFridgeHistoryButton).setOnClickListener(v ->
                startActivity(new Intent(this, com.veggo.app.presentation.order.OrderHistoryActivity.class))
        );
        findViewById(R.id.fridgeScanButton).setOnClickListener(v -> openQuickScan());
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

    private void openQuickScan() {
        FridgeQuickScanHelper.showScanOptionsDialog(this, new FridgeQuickScanHelper.OptionsListener() {
            @Override
            public void onReceiptScanSelected() {
                quickScanLauncher.launch(
                        FridgeQuickScanHelper.createFridgeCameraIntent(SmartFridgeActivity.this, true)
                );
            }

            @Override
            public void onIngredientScanSelected() {
                quickScanLauncher.launch(
                        FridgeQuickScanHelper.createFridgeCameraIntent(SmartFridgeActivity.this, false)
                );
            }
        });
    }

    private void loadInventory() {
        new Thread(() -> {
            String customerId = new AppPreferences(this).getCustomerId();
            List<FridgeItemDto> fridgeItems = new ArrayList<>();
            try {
                if (customerId != null && !customerId.isEmpty()) {
                    FridgeApi api = ApiClient.createService(FridgeApi.class);
                    retrofit2.Response<List<FridgeItemDto>> response = api.getFridgeItems(customerId).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        fridgeItems = response.body();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            List<FridgeItemDto> finalItems = fridgeItems;
            runOnUiThread(() -> {
                bindInventory(finalItems);
                PullToRefreshHelper.finish(fridgeRefreshLayout);
            });
        }).start();
    }

    private void updateHeaderCounts(List<FridgeItemDto> items) {
        AssetScreenData.setText(findViewById(android.R.id.content), R.id.fridgeIngredientsCount, items.size() + " nguyên liệu");

        int expiringCount = 0;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        long now = System.currentTimeMillis();

        for (FridgeItemDto item : items) {
            try {
                if (!com.veggo.app.core.notification.FridgeExpiryReminderHelper.isReminderEnabled(item)) {
                    continue;
                }
                if (item.getExpiryDate() != null) {
                    Date d = format.parse(item.getExpiryDate());
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

    private void bindInventory(List<FridgeItemDto> items) {
        updateHeaderCounts(items);

        androidx.recyclerview.widget.RecyclerView list = findViewById(R.id.fridgeInventoryList);
        list.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        FridgeInventoryAdapter adapter = new FridgeInventoryAdapter(items, new FridgeInventoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FridgeItemDto item) {
                Intent intent = new Intent(SmartFridgeActivity.this, FridgeIngredientDetailActivity.class);
                intent.putExtra("ITEM_JSON", new com.google.gson.Gson().toJson(item));
                addIngredientLauncher.launch(intent);
            }

            @Override
            public void onDeleteClick(FridgeItemDto item, int position) {
                VeggoDialog.show(
                        SmartFridgeActivity.this,
                        R.drawable.ic_trash,
                        "Xác nhận xóa nguyên liệu",
                        "Bạn có chắc chắn muốn xóa nguyên liệu này khỏi tủ lạnh?",
                        "Xóa",
                        "Hủy",
                        new VeggoDialog.DialogListener() {
                            @Override
                            public void onConfirm() {
                                new Thread(() -> {
                                    try {
                                        String customerId = new AppPreferences(SmartFridgeActivity.this).getCustomerId();
                                        FridgeApi api = ApiClient.createService(FridgeApi.class);
                                        api.deleteFridgeItem(customerId, item.getId()).execute();
                                        runOnUiThread(() -> {
                                            androidx.recyclerview.widget.RecyclerView list = findViewById(R.id.fridgeInventoryList);
                                            if (list != null && list.getAdapter() instanceof FridgeInventoryAdapter) {
                                                FridgeInventoryAdapter adapt = (FridgeInventoryAdapter) list.getAdapter();
                                                adapt.removeItem(position);
                                                updateHeaderCounts(adapt.getItems());
                                            }
                                            Toast.makeText(SmartFridgeActivity.this, "Đã xóa nguyên liệu", Toast.LENGTH_SHORT).show();
                                        });
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        runOnUiThread(() -> Toast.makeText(SmartFridgeActivity.this, "Lỗi xóa nguyên liệu", Toast.LENGTH_SHORT).show());
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
