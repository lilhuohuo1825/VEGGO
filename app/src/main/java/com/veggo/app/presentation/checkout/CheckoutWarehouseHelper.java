package com.veggo.app.presentation.checkout;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.veggo.app.R;
import com.veggo.app.adapter.WarehouseOptionAdapter;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.utils.WarehouseDistanceUtils;
import com.veggo.app.data.remote.api.WarehouseApi;
import com.veggo.app.data.remote.dto.WarehouseDto;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class CheckoutWarehouseHelper {
    public interface WarehouseUi {
        void bindWarehouse(
                @NonNull WarehouseDistanceUtils.RankedWarehouse warehouse,
                @NonNull String distanceText
        );
    }

    private final List<WarehouseDistanceUtils.RankedWarehouse> rankedWarehouses = new ArrayList<>();
    private int selectedWarehouseIndex;
    private boolean warehousesLoaded;

    @Nullable
    public WarehouseDistanceUtils.RankedWarehouse getSelectedWarehouse() {
        if (rankedWarehouses.isEmpty() || selectedWarehouseIndex < 0 || selectedWarehouseIndex >= rankedWarehouses.size()) {
            return null;
        }
        return rankedWarehouses.get(selectedWarehouseIndex);
    }

    @Nullable
    public String getSelectedWarehouseCode() {
        WarehouseDistanceUtils.RankedWarehouse selected = getSelectedWarehouse();
        return selected == null || selected.warehouse == null ? null : selected.warehouse.getCode();
    }

    public int getSelectedWarehouseEtaMinutes() {
        WarehouseDistanceUtils.RankedWarehouse selected = getSelectedWarehouse();
        return selected == null ? 45 : selected.etaMinutes;
    }

    public void refreshWarehouses(
            @Nullable String deliveryAddress,
            @NonNull Runnable onComplete
    ) {
        WarehouseApi warehouseApi = ApiClient.createService(WarehouseApi.class);
        warehouseApi.getWarehouses().enqueue(new Callback<List<WarehouseDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<WarehouseDto>> call, @NonNull Response<List<WarehouseDto>> response) {
                List<WarehouseDto> warehouses = response.isSuccessful() && response.body() != null
                        ? response.body()
                        : new ArrayList<>();
                applyRankedWarehouses(WarehouseDistanceUtils.rankByDeliveryAddress(warehouses, deliveryAddress));
                warehousesLoaded = true;
                onComplete.run();
            }

            @Override
            public void onFailure(@NonNull Call<List<WarehouseDto>> call, @NonNull Throwable t) {
                if (!warehousesLoaded) {
                    applyRankedWarehouses(new ArrayList<>());
                }
                onComplete.run();
            }
        });
    }

    private void applyRankedWarehouses(List<WarehouseDistanceUtils.RankedWarehouse> next) {
        rankedWarehouses.clear();
        rankedWarehouses.addAll(next);
        selectedWarehouseIndex = 0;
    }

    public void updateDeliveryAddress(@Nullable String deliveryAddress, @NonNull WarehouseUi ui) {
        if (rankedWarehouses.isEmpty()) {
            refreshWarehouses(deliveryAddress, () -> bindSelectedWarehouse(ui));
            return;
        }
        WarehouseDistanceUtils.RankedWarehouse current = getSelectedWarehouse();
        String currentCode = current == null || current.warehouse == null ? null : current.warehouse.getCode();
        List<WarehouseDto> source = new ArrayList<>();
        for (WarehouseDistanceUtils.RankedWarehouse ranked : rankedWarehouses) {
            source.add(ranked.warehouse);
        }
        applyRankedWarehouses(WarehouseDistanceUtils.rankByDeliveryAddress(source, deliveryAddress));
        if (currentCode != null) {
            for (int i = 0; i < rankedWarehouses.size(); i++) {
                WarehouseDto warehouse = rankedWarehouses.get(i).warehouse;
                if (warehouse != null && currentCode.equals(warehouse.getCode())) {
                    selectedWarehouseIndex = i;
                    break;
                }
            }
        }
        bindSelectedWarehouse(ui);
    }

    public void bindSelectedWarehouse(@NonNull WarehouseUi ui) {
        WarehouseDistanceUtils.RankedWarehouse selected = getSelectedWarehouse();
        if (selected == null) {
            return;
        }
        ui.bindWarehouse(
                selected,
                WarehouseDistanceUtils.formatDistanceEta(selected.distanceKm, selected.etaMinutes)
        );
    }

    public void showSelectionDialog(
            @NonNull Context context,
            @NonNull WarehouseUi ui,
            @Nullable Runnable onConfirmed
    ) {
        if (rankedWarehouses.isEmpty()) {
            Toast.makeText(context, "Không tải được danh sách kho hàng.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delivery_origin, null, false);
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvWarehouseOptions);
        TextView btnConfirm = dialogView.findViewById(R.id.btnConfirmWarehouse);
        TextView btnClose = dialogView.findViewById(R.id.btnCloseDeliveryOrigin);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        WarehouseOptionAdapter adapter = new WarehouseOptionAdapter(
                rankedWarehouses,
                selectedWarehouseIndex,
                new WarehouseOptionAdapter.OnWarehouseActionListener() {
                    @Override
                    public void onWarehouseSelected(WarehouseDistanceUtils.RankedWarehouse warehouse) {
                        selectedWarehouseIndex = rankedWarehouses.indexOf(warehouse);
                    }

                    @Override
                    public void onViewMap(WarehouseDistanceUtils.RankedWarehouse warehouse) {
                        openWarehouseInGoogleMaps(context, warehouse);
                    }
                }
        );
        recyclerView.setAdapter(adapter);

        BottomSheetDialog dialog = new BottomSheetDialog(context);
        dialog.setContentView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet
            );
            if (bottomSheet == null) {
                return;
            }
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            int expandedHeight = (int) (screenHeight * 0.65f);
            bottomSheet.getLayoutParams().height = expandedHeight;
            bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded);
            bottomSheet.setPadding(0, 0, 0, 0);
            bottomSheet.requestLayout();

            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setPeekHeight(expandedHeight, true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
            behavior.setDraggable(true);
        });
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            selectedWarehouseIndex = adapter.getSelectedPosition();
            bindSelectedWarehouse(ui);
            dialog.dismiss();
            if (onConfirmed != null) {
                onConfirmed.run();
            }
        });
        dialog.show();
    }

    public static void openWarehouseInGoogleMaps(
            @NonNull Context context,
            @NonNull WarehouseDistanceUtils.RankedWarehouse warehouse
    ) {
        if (warehouse.warehouse == null || warehouse.warehouse.getLocation() == null) {
            Toast.makeText(context, "Kho chưa có tọa độ bản đồ.", Toast.LENGTH_SHORT).show();
            return;
        }
        double lat = warehouse.warehouse.getLocation().lat;
        double lng = warehouse.warehouse.getLocation().lng;
        String label = warehouse.warehouse.getName() == null ? "VEGGO Warehouse" : warehouse.warehouse.getName();
        Uri geoUri = Uri.parse(String.format(
                java.util.Locale.US,
                "geo:%f,%f?q=%f,%f(%s)",
                lat,
                lng,
                lat,
                lng,
                Uri.encode(label)
        ));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, geoUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        try {
            context.startActivity(mapIntent);
        } catch (ActivityNotFoundException ignored) {
            Uri webUri = Uri.parse(String.format(
                    java.util.Locale.US,
                    "https://www.google.com/maps/search/?api=1&query=%f,%f",
                    lat,
                    lng
            ));
            context.startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }
}
