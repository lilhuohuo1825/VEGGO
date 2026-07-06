package com.veggo.app.presentation.checkout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veggo.app.core.network.ApiClient;
import com.veggo.app.data.remote.api.CartApi;
import com.veggo.app.data.remote.dto.CartDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PendingCheckoutStore {
    private static final String PREF_NAME = "veggo_pending_checkout";
    private static final String KEY_PENDING = "pending";
    private static final String KEY_BUY_NOW = "buyNow";
    private static final String KEY_SELECTED_LINES = "selectedLines";
    private static final String KEY_VOUCHER_ID = "voucherId";
    private static final String KEY_VOUCHER_TITLE = "voucherTitle";
    private static final String KEY_SHIPPING_VOUCHER_ID = "shippingVoucherId";
    private static final String KEY_SHIPPING_VOUCHER_TITLE = "shippingVoucherTitle";
    private static final String KEY_GUEST_ID = "guestId";
    private static CartDto transferredCheckoutCart;

    private final SharedPreferences prefs;

    public PendingCheckoutStore(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasPending() {
        return prefs.getBoolean(KEY_PENDING, false);
    }

    public void saveCartCheckout(@Nullable ArrayList<String> selectedLines,
                                 @Nullable String productVoucherId,
                                 @Nullable String productVoucherTitle,
                                 @Nullable String shippingVoucherId,
                                 @Nullable String shippingVoucherTitle) {
        Set<String> lines = new HashSet<>();
        if (selectedLines != null) {
            lines.addAll(selectedLines);
        }
        prefs.edit()
                .putBoolean(KEY_PENDING, true)
                .putBoolean(KEY_BUY_NOW, false)
                .putStringSet(KEY_SELECTED_LINES, lines)
                .putString(KEY_VOUCHER_ID, productVoucherId)
                .putString(KEY_VOUCHER_TITLE, productVoucherTitle)
                .putString(KEY_SHIPPING_VOUCHER_ID, shippingVoucherId)
                .putString(KEY_SHIPPING_VOUCHER_TITLE, shippingVoucherTitle)
                .apply();
    }

    public void saveCartCheckout(@Nullable ArrayList<String> selectedLines,
                                 @Nullable String voucherId,
                                 @Nullable String voucherTitle) {
        saveCartCheckout(selectedLines, voucherId, voucherTitle, null, null);
    }

    public void saveBuyNowIntent(@NonNull Intent sourceIntent) {
        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(KEY_PENDING, true)
                .putBoolean(KEY_BUY_NOW, true);
        copyString(editor, sourceIntent, CheckoutActivity.EXTRA_BUY_NOW_PRODUCT_ID);
        copyString(editor, sourceIntent, CheckoutActivity.EXTRA_BUY_NOW_SKU);
        copyString(editor, sourceIntent, CheckoutActivity.EXTRA_BUY_NOW_NAME);
        copyString(editor, sourceIntent, CheckoutActivity.EXTRA_BUY_NOW_IMAGE);
        copyString(editor, sourceIntent, CheckoutActivity.EXTRA_BUY_NOW_WEIGHT);
        copyString(editor, sourceIntent, CheckoutActivity.EXTRA_BUY_NOW_CATEGORY_ID);
        copyString(editor, sourceIntent, CheckoutActivity.EXTRA_BUY_NOW_SUBCATEGORY_ID);
        editor.putLong(CheckoutActivity.EXTRA_BUY_NOW_PRICE,
                sourceIntent.getLongExtra(CheckoutActivity.EXTRA_BUY_NOW_PRICE, 0));
        editor.putLong(CheckoutActivity.EXTRA_BUY_NOW_ORIGINAL_PRICE,
                sourceIntent.getLongExtra(CheckoutActivity.EXTRA_BUY_NOW_ORIGINAL_PRICE, 0));
        editor.putFloat(CheckoutActivity.EXTRA_BUY_NOW_SELECTED_WEIGHT,
                (float) sourceIntent.getDoubleExtra(CheckoutActivity.EXTRA_BUY_NOW_SELECTED_WEIGHT, 1.0));
        editor.putBoolean(CheckoutActivity.EXTRA_BUY_NOW_HAS_WEIGHT_OPTIONS,
                sourceIntent.getBooleanExtra(CheckoutActivity.EXTRA_BUY_NOW_HAS_WEIGHT_OPTIONS, false));
        editor.putInt(CheckoutActivity.EXTRA_BUY_NOW_QUANTITY,
                sourceIntent.getIntExtra(CheckoutActivity.EXTRA_BUY_NOW_QUANTITY, 1));
        editor.putFloat(CheckoutActivity.EXTRA_BUY_NOW_CARBON_POINT,
                (float) sourceIntent.getDoubleExtra(CheckoutActivity.EXTRA_BUY_NOW_CARBON_POINT, 0));
        editor.apply();
    }

    public Intent buildCheckoutIntent(@NonNull Context context) {
        Intent intent = new Intent(context, CheckoutActivity.class);
        if (prefs.getBoolean(KEY_BUY_NOW, false)) {
            intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW, true);
            restoreString(intent, CheckoutActivity.EXTRA_BUY_NOW_PRODUCT_ID);
            restoreString(intent, CheckoutActivity.EXTRA_BUY_NOW_SKU);
            restoreString(intent, CheckoutActivity.EXTRA_BUY_NOW_NAME);
            restoreString(intent, CheckoutActivity.EXTRA_BUY_NOW_IMAGE);
            restoreString(intent, CheckoutActivity.EXTRA_BUY_NOW_WEIGHT);
            restoreString(intent, CheckoutActivity.EXTRA_BUY_NOW_CATEGORY_ID);
            restoreString(intent, CheckoutActivity.EXTRA_BUY_NOW_SUBCATEGORY_ID);
            intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_PRICE,
                    prefs.getLong(CheckoutActivity.EXTRA_BUY_NOW_PRICE, 0));
            intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_ORIGINAL_PRICE,
                    prefs.getLong(CheckoutActivity.EXTRA_BUY_NOW_ORIGINAL_PRICE, 0));
            intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_SELECTED_WEIGHT,
                    (double) prefs.getFloat(CheckoutActivity.EXTRA_BUY_NOW_SELECTED_WEIGHT, 1.0f));
            intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_HAS_WEIGHT_OPTIONS,
                    prefs.getBoolean(CheckoutActivity.EXTRA_BUY_NOW_HAS_WEIGHT_OPTIONS, false));
            intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_QUANTITY,
                    prefs.getInt(CheckoutActivity.EXTRA_BUY_NOW_QUANTITY, 1));
            intent.putExtra(CheckoutActivity.EXTRA_BUY_NOW_CARBON_POINT,
                    (double) prefs.getFloat(CheckoutActivity.EXTRA_BUY_NOW_CARBON_POINT, 0));
            return intent;
        }
        Set<String> lines = prefs.getStringSet(KEY_SELECTED_LINES, new HashSet<>());
        intent.putStringArrayListExtra(CheckoutActivity.EXTRA_SELECTED_CART_LINE_KEYS, new ArrayList<>(lines));
        intent.putExtra(CheckoutActivity.EXTRA_SELECTED_PRODUCT_VOUCHER_ID, prefs.getString(KEY_VOUCHER_ID, null));
        intent.putExtra(CheckoutActivity.EXTRA_SELECTED_PRODUCT_VOUCHER_TITLE, prefs.getString(KEY_VOUCHER_TITLE, null));
        intent.putExtra(CheckoutActivity.EXTRA_SELECTED_SHIPPING_VOUCHER_ID, prefs.getString(KEY_SHIPPING_VOUCHER_ID, null));
        intent.putExtra(CheckoutActivity.EXTRA_SELECTED_SHIPPING_VOUCHER_TITLE, prefs.getString(KEY_SHIPPING_VOUCHER_TITLE, null));
        intent.putExtra(CheckoutActivity.EXTRA_SELECTED_VOUCHER_ID, prefs.getString(KEY_VOUCHER_ID, null));
        intent.putExtra(CheckoutActivity.EXTRA_SELECTED_VOUCHER_TITLE, prefs.getString(KEY_VOUCHER_TITLE, null));
        return intent;
    }

    public String guestId() {
        String id = prefs.getString(KEY_GUEST_ID, null);
        if (id == null || id.trim().isEmpty()) {
            id = "GUEST_" + java.util.UUID.randomUUID().toString().replace("-", "");
            prefs.edit().putString(KEY_GUEST_ID, id).apply();
        }
        return id;
    }

    public void clearPending() {
        String guestId = prefs.getString(KEY_GUEST_ID, null);
        prefs.edit().clear().putString(KEY_GUEST_ID, guestId).apply();
    }

    public void openAfterAuth(@NonNull Activity activity, @NonNull String customerId) {
        Intent checkoutIntent = buildCheckoutIntent(activity);
        boolean buyNow = prefs.getBoolean(KEY_BUY_NOW, false);
        if (buyNow) {
            clearPending();
            activity.startActivity(checkoutIntent);
            return;
        }
        String sourceGuestId = guestId();
        Set<String> selectedLines = new HashSet<>(prefs.getStringSet(KEY_SELECTED_LINES, new HashSet<>()));
        new Thread(() -> {
            CartDto selectedGuestCart = new CartDto();
            ArrayList<CartDto.CartItemDto> selectedItems = new ArrayList<>();
            try {
                CartApi cartApi = ApiClient.createService(CartApi.class);
                retrofit2.Response<CartDto> response = cartApi.getCart(sourceGuestId).execute();
                CartDto guestCart = response.body();
                if (guestCart != null && guestCart.getItems() != null) {
                    for (CartDto.CartItemDto item : guestCart.getItems()) {
                        String lineKey = item.getSku() + "#" + trimTrailingZeros(item.getSelectedWeight() > 0 ? item.getSelectedWeight() : 1.0);
                        if (!selectedLines.isEmpty() && !selectedLines.contains(lineKey)) {
                            continue;
                        }
                        selectedItems.add(item);
                    }
                }
            } catch (Exception ignored) {
                // If loading fails, checkout opens empty and shows a clear message.
            }
            activity.runOnUiThread(() -> {
                selectedGuestCart.setItems(selectedItems);
                transferredCheckoutCart = selectedGuestCart;
                checkoutIntent.putExtra(CheckoutActivity.EXTRA_TRANSFERRED_CHECKOUT, true);
                clearPending();
                activity.startActivity(checkoutIntent);
            });
        }).start();
    }

    @Nullable
    public static CartDto consumeTransferredCart() {
        CartDto cart = transferredCheckoutCart;
        transferredCheckoutCart = null;
        return cart;
    }

    private static void copyString(SharedPreferences.Editor editor, Intent source, String key) {
        editor.putString(key, source.getStringExtra(key));
    }

    private void restoreString(Intent intent, String key) {
        intent.putExtra(key, prefs.getString(key, null));
    }

    private static String trimTrailingZeros(double value) {
        String text = String.format(java.util.Locale.US, "%.3f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
