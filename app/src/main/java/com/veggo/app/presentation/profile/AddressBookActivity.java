package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

public class AddressBookActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_book);

        findViewById(R.id.addressBookBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.addAddressButton).setOnClickListener(v -> openAddressForm(false));
        findViewById(R.id.addressItemCard).setOnClickListener(v -> openAddressForm(true));
        findViewById(R.id.addressEditButton).setOnClickListener(v -> openAddressForm(true));
        findViewById(R.id.addressDefaultItemCard).setOnClickListener(v -> openAddressForm(true));
        findViewById(R.id.addressDefaultEditButton).setOnClickListener(v -> openAddressForm(true));
        loadAddresses();
    }

    private void openAddressForm(boolean prefill) {
        Intent intent = new Intent(this, AddressFormActivity.class);
        intent.putExtra(AddressFormActivity.EXTRA_PREFILL, prefill);
        startActivity(intent);
    }

    private void loadAddresses() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            runOnUiThread(() -> bindAddresses(snapshot));
        }).start();
    }

    private void bindAddresses(AssetScreenData.Snapshot snapshot) {
        if (snapshot.user == null) {
            return;
        }
        String name = AssetScreenData.hasText(snapshot.user.fullName)
                ? snapshot.user.fullName
                : "Khách hàng " + snapshot.user.customerId;
        View root = findViewById(android.R.id.content);
        AssetScreenData.setText(root, R.id.addressDefaultItemName, name);
        AssetScreenData.setText(root, R.id.addressDefaultItemLine, AssetScreenData.safe(snapshot.user.address));
        AssetScreenData.setText(root, R.id.addressDefaultItemPhone, snapshot.user.phone);
        AssetScreenData.setText(root, R.id.addressDefaultItemEmail,
                AssetScreenData.hasText(snapshot.user.email) ? snapshot.user.email : "Chưa cập nhật email");

        AssetModels.OrderDetail recentDetail = null;
        for (AssetModels.Order order : snapshot.orders) {
            recentDetail = snapshot.detailByOrderId.get(order.orderId);
            if (recentDetail != null && recentDetail.shippingInfo != null) {
                break;
            }
        }
        if (recentDetail != null && recentDetail.shippingInfo != null) {
            AssetScreenData.setText(root, R.id.addressItemName, recentDetail.shippingInfo.fullName);
            AssetScreenData.setText(root, R.id.addressItemLine, AssetScreenData.fullAddress(recentDetail.shippingInfo));
            AssetScreenData.setText(root, R.id.addressItemPhone, recentDetail.shippingInfo.phone);
            AssetScreenData.setText(root, R.id.addressItemEmail,
                    AssetScreenData.hasText(recentDetail.shippingInfo.email)
                            ? recentDetail.shippingInfo.email
                            : "Chưa cập nhật email");
        }
    }
}
