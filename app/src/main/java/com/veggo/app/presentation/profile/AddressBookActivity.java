package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.presentation.common.AssetScreenData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddressBookActivity extends BaseActivity {
    private final List<AddressDisplay> boundAddresses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_book);

        findViewById(R.id.addressBookBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.addAddressButton).setOnClickListener(v -> openAddressForm(false));
        findViewById(R.id.addressItemCard).setOnClickListener(v -> openAddressForm(getAddressAt(1)));
        findViewById(R.id.addressEditButton).setOnClickListener(v -> openAddressForm(getAddressAt(1)));
        findViewById(R.id.addressDefaultItemCard).setOnClickListener(v -> openAddressForm(getAddressAt(0)));
        findViewById(R.id.addressDefaultEditButton).setOnClickListener(v -> openAddressForm(getAddressAt(0)));
        loadAddresses();
    }

    private void openAddressForm(boolean prefill) {
        Intent intent = new Intent(this, AddressFormActivity.class);
        intent.putExtra(AddressFormActivity.EXTRA_PREFILL, prefill);
        startActivity(intent);
    }

    private void openAddressForm(AddressDisplay address) {
        if (address == null) {
            openAddressForm(false);
            return;
        }
        Intent intent = new Intent(this, AddressFormActivity.class);
        intent.putExtra(AddressFormActivity.EXTRA_PREFILL, true);
        intent.putExtra(AddressFormActivity.EXTRA_NAME, address.name);
        intent.putExtra(AddressFormActivity.EXTRA_PHONE, address.phone);
        intent.putExtra(AddressFormActivity.EXTRA_EMAIL, address.email);
        intent.putExtra(AddressFormActivity.EXTRA_DETAIL, address.detail);
        intent.putExtra(AddressFormActivity.EXTRA_WARD, address.ward);
        intent.putExtra(AddressFormActivity.EXTRA_DISTRICT, address.district);
        intent.putExtra(AddressFormActivity.EXTRA_CITY, address.city);
        startActivity(intent);
    }

    private AddressDisplay getAddressAt(int index) {
        if (index < 0 || index >= boundAddresses.size()) {
            return null;
        }
        return boundAddresses.get(index);
    }

    private void loadAddresses() {
        new Thread(() -> {
            AssetScreenData.Snapshot snapshot = AssetScreenData.load(this);
            runOnUiThread(() -> bindAddresses(snapshot));
        }).start();
    }

    private void bindAddresses(AssetScreenData.Snapshot snapshot) {
        View root = findViewById(android.R.id.content);
        View secondaryContainer = findViewById(R.id.addressSecondaryContainer);
        View defaultContainer = findViewById(R.id.addressDefaultContainer);

        if (snapshot.user == null) {
            secondaryContainer.setVisibility(View.GONE);
            defaultContainer.setVisibility(View.GONE);
            boundAddresses.clear();
            return;
        }

        List<AddressDisplay> addresses = collectAddresses(snapshot);
        boundAddresses.clear();
        boundAddresses.addAll(addresses);

        secondaryContainer.setVisibility(addresses.size() > 1 ? View.VISIBLE : View.GONE);
        defaultContainer.setVisibility(addresses.isEmpty() ? View.GONE : View.VISIBLE);

        if (addresses.isEmpty()) {
            return;
        }

        AddressDisplay defaultAddress = addresses.get(0);
        bindAddress(root, R.id.addressDefaultItemName, R.id.addressDefaultItemLine,
                R.id.addressDefaultItemPhone, defaultAddress);

        if (addresses.size() > 1) {
            AddressDisplay secondaryAddress = addresses.get(1);
            bindAddress(root, R.id.addressItemName, R.id.addressItemLine,
                    R.id.addressItemPhone, secondaryAddress);
        }
    }

    private List<AddressDisplay> collectAddresses(AssetScreenData.Snapshot snapshot) {
        List<AddressDisplay> addresses = new ArrayList<>();
        String userName = AssetScreenData.hasText(snapshot.user.fullName)
                ? snapshot.user.fullName
                : "Khách hàng " + snapshot.user.customerId;
        AddressParts userParts = parseAddressParts(snapshot.user.address);
        addUniqueAddress(addresses, new AddressDisplay(
                userName,
                AssetScreenData.safe(snapshot.user.address),
                AssetScreenData.safe(snapshot.user.phone),
                AssetScreenData.safe(snapshot.user.email),
                userParts.detail,
                userParts.ward,
                userParts.district,
                userParts.city
        ));

        for (AssetModels.Order order : snapshot.orders) {
            AssetModels.OrderDetail detail = snapshot.detailByOrderId.get(order.orderId);
            if (detail == null || detail.shippingInfo == null) {
                continue;
            }
            addUniqueAddress(addresses, new AddressDisplay(
                    AssetScreenData.safe(detail.shippingInfo.fullName),
                    AssetScreenData.fullAddress(detail.shippingInfo),
                    AssetScreenData.safe(detail.shippingInfo.phone),
                    AssetScreenData.safe(detail.shippingInfo.email),
                    AssetScreenData.safe(detail.shippingInfo.address.detail),
                    AssetScreenData.safe(detail.shippingInfo.address.ward),
                    AssetScreenData.safe(detail.shippingInfo.address.district),
                    AssetScreenData.safe(detail.shippingInfo.address.city)
            ));
        }
        return addresses;
    }

    private void addUniqueAddress(List<AddressDisplay> addresses, AddressDisplay candidate) {
        if (!AssetScreenData.hasText(candidate.address) && !AssetScreenData.hasText(candidate.phone)) {
            return;
        }
        String candidateKey = candidate.key();
        for (AddressDisplay address : addresses) {
            if (address.key().equals(candidateKey)) {
                return;
            }
        }
        addresses.add(candidate);
    }

    private AddressParts parseAddressParts(String fullAddress) {
        String detail = "";
        String ward = "";
        String district = "";
        String city = "";
        if (AssetScreenData.hasText(fullAddress)) {
            String[] parts = fullAddress.split(",");
            if (parts.length > 0) {
                detail = parts[0].trim();
            }
            if (parts.length > 1) {
                ward = parts[1].trim();
            }
            if (parts.length > 2) {
                district = parts[2].trim();
            }
            if (parts.length > 3) {
                city = parts[3].trim();
            }
        }
        return new AddressParts(detail, ward, district, city);
    }

    private void bindAddress(
            View root,
            int nameId,
            int addressId,
            int phoneId,
            AddressDisplay address
    ) {
        AssetScreenData.setText(root, nameId, address.name);
        AssetScreenData.setText(root, addressId, address.address);
        AssetScreenData.setText(root, phoneId, address.phone);
    }

    private static final class AddressDisplay {
        final String name;
        final String address;
        final String phone;
        final String email;
        final String detail;
        final String ward;
        final String district;
        final String city;

        AddressDisplay(String name, String address, String phone, String email, String detail, String ward, String district, String city) {
            this.name = name;
            this.address = address;
            this.phone = phone;
            this.email = email;
            this.detail = detail;
            this.ward = ward;
            this.district = district;
            this.city = city;
        }

        String key() {
            return (address + "|" + phone).trim().toLowerCase(Locale.ROOT);
        }
    }

    private static final class AddressParts {
        final String detail;
        final String ward;
        final String district;
        final String city;

        AddressParts(String detail, String ward, String district, String city) {
            this.detail = detail;
            this.ward = ward;
            this.district = district;
            this.city = city;
        }
    }
}
