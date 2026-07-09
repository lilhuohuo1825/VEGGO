package com.veggo.app.core.utils;

import androidx.annotation.Nullable;

import com.veggo.app.assets.AssetModels;
import com.veggo.app.data.remote.dto.FridgeItemDto;

import java.util.List;

public final class FridgeOrderHelper {
    private FridgeOrderHelper() {
    }

    public static boolean supportsFridgeActions(@Nullable String orderStatus) {
        if (orderStatus == null) {
            return false;
        }
        switch (orderStatus) {
            case "unreview":
            case "reviewed":
            case "completed":
            case "rejected":
                return true;
            default:
                return false;
        }
    }

    public static int countAddableItems(
            @Nullable String orderId,
            @Nullable AssetModels.OrderDetail detail,
            @Nullable List<FridgeItemDto> fridgeItems
    ) {
        if (!hasText(orderId) || detail == null || detail.items == null || detail.items.isEmpty()) {
            return 0;
        }
        int addable = 0;
        for (AssetModels.OrderDetailItem item : detail.items) {
            if (!isOrderItemInFridge(orderId, item, fridgeItems)) {
                addable++;
            }
        }
        return addable;
    }

    public static boolean hasAddableFridgeItems(
            @Nullable String orderId,
            @Nullable AssetModels.OrderDetail detail,
            @Nullable List<FridgeItemDto> fridgeItems
    ) {
        return countAddableItems(orderId, detail, fridgeItems) > 0;
    }

    public static boolean isOrderItemInFridge(
            @Nullable String orderId,
            @Nullable AssetModels.OrderDetailItem item,
            @Nullable List<FridgeItemDto> fridgeItems
    ) {
        if (!hasText(orderId) || item == null || fridgeItems == null || fridgeItems.isEmpty()) {
            return false;
        }
        for (FridgeItemDto fridgeItem : fridgeItems) {
            if (!sameText(orderId, fridgeItem.getOrderId())) {
                continue;
            }
            if (sameText(item.sku, fridgeItem.getSku())) {
                return true;
            }
            if ((!hasText(item.sku) || !hasText(fridgeItem.getSku()))
                    && sameText(item.productName, fridgeItem.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasText(@Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static boolean sameText(@Nullable String left, @Nullable String right) {
        if (!hasText(left) || !hasText(right)) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }
}
