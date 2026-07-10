package com.veggo.app.presentation.profile;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.veggo.app.R;
import com.veggo.app.presentation.scan.NavbarScanCameraActivity;

public final class FridgeQuickScanHelper {

    public interface OptionsListener {
        void onReceiptScanSelected();

        void onIngredientScanSelected();
    }

    private FridgeQuickScanHelper() {
    }

    public static void showScanOptionsDialog(
            @NonNull Context context,
            @NonNull OptionsListener listener
    ) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_scan_options);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        dialog.findViewById(R.id.dialogOptionScanReceipt).setOnClickListener(v -> {
            dialog.dismiss();
            listener.onReceiptScanSelected();
        });

        dialog.findViewById(R.id.dialogOptionScanIngredient).setOnClickListener(v -> {
            dialog.dismiss();
            listener.onIngredientScanSelected();
        });

        dialog.show();
    }

    @NonNull
    public static Intent createCameraIntent(
            @NonNull Context context,
            boolean receiptMode,
            boolean deliverResult,
            boolean fromNavbar
    ) {
        Intent intent = new Intent(context, NavbarScanCameraActivity.class);
        intent.putExtra(NavbarScanCameraActivity.EXTRA_SCAN_RECEIPT_MODE, receiptMode);
        intent.putExtra(NavbarScanCameraActivity.EXTRA_DELIVER_RESULT, deliverResult);
        intent.putExtra(NavbarScanCameraActivity.EXTRA_FROM_NAVBAR, fromNavbar);
        return intent;
    }

    @NonNull
    public static Intent createFridgeCameraIntent(
            @NonNull Context context,
            boolean receiptMode
    ) {
        return createCameraIntent(context, receiptMode, true, false);
    }

    @NonNull
    public static Intent createNavbarCameraIntent(
            @NonNull Context context,
            boolean receiptMode
    ) {
        return createCameraIntent(context, receiptMode, false, true);
    }

    @NonNull
    public static Intent createCameraIntent(
            @NonNull Context context,
            boolean receiptMode,
            boolean deliverResult
    ) {
        return createCameraIntent(context, receiptMode, deliverResult, false);
    }

    public static void copyScanExtras(@NonNull Intent target, @NonNull Intent source) {
        String receiptUri = source.getStringExtra("EXTRA_AI_IMAGE_URI");
        String ingredientUri = source.getStringExtra("EXTRA_AI_INGREDIENT_URI");
        if (receiptUri != null) {
            target.putExtra("EXTRA_AI_IMAGE_URI", receiptUri);
        }
        if (ingredientUri != null) {
            target.putExtra("EXTRA_AI_INGREDIENT_URI", ingredientUri);
        }
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
}
