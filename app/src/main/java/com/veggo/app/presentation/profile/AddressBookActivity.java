package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.model.Address;

import java.util.ArrayList;
import java.util.List;

public class AddressBookActivity extends BaseActivity {
    private AddressBookViewModel viewModel;
    private AppPreferences appPreferences;
    private String userId;

    private View rootView;
    private LinearLayout addressListContainer;
    private View defaultContainer;
    private View secondaryContainer;
    private View addAddressButton;
    @Nullable
    private TextView emptyStateView;

    private final List<Address> boundAddresses = new ArrayList<>();

    private final ActivityResultLauncher<Intent> addressFormLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadAddresses();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_book);

        appPreferences = new AppPreferences(this);
        userId = appPreferences.getCustomerId();

        bindViews();
        setupViewModel();
        setupActions();
        loadAddresses();
    }

    private void bindViews() {
        rootView = findViewById(android.R.id.content);
        defaultContainer = findViewById(R.id.addressDefaultContainer);
        secondaryContainer = findViewById(R.id.addressSecondaryContainer);
        addAddressButton = findViewById(R.id.addAddressButton);
        if (secondaryContainer != null && secondaryContainer.getParent() instanceof LinearLayout) {
            addressListContainer = (LinearLayout) secondaryContainer.getParent();
        }
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(
                this,
                new AddressBookViewModelFactory(AppModule.provideAddressRepository(this))
        ).get(AddressBookViewModel.class);

        viewModel.getLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            addAddressButton.setEnabled(!loading);
            addAddressButton.setAlpha(loading ? 0.6f : 1f);
        });

        viewModel.getAddresses().observe(this, this::bindAddresses);

        viewModel.getError().observe(this, message -> {
            if (message == null || message.isEmpty()) {
                return;
            }
            if (Boolean.TRUE.equals(viewModel.getRetryableError().getValue())) {
                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.consultation_submit_retry, v -> loadAddresses())
                        .show();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
            viewModel.clearMessages();
        });

        viewModel.getSuccessMessage().observe(this, message -> {
            if (message == null || message.isEmpty()) {
                return;
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            viewModel.clearMessages();
        });
    }

    private void setupActions() {
        findViewById(R.id.addressBookBackButton).setOnClickListener(v -> finish());
        addAddressButton.setOnClickListener(v -> openAddressForm(null));
    }

    private void loadAddresses() {
        viewModel.loadAddresses(userId);
    }

    private void bindAddresses(List<Address> addresses) {
        boundAddresses.clear();
        if (addresses != null) {
            boundAddresses.addAll(addresses);
        }

        clearDynamicAddressViews();
        bindStaticAddressSlots();
        bindExtraAddressViews();

        boolean isEmpty = boundAddresses.isEmpty();
        showEmptyState(isEmpty);
        defaultContainer.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        secondaryContainer.setVisibility(boundAddresses.size() > 1 ? View.VISIBLE : View.GONE);
    }

    private void bindStaticAddressSlots() {
        if (boundAddresses.isEmpty()) {
            return;
        }

        Address defaultAddress = findDefaultAddress();
        bindDefaultAddress(defaultAddress);

        Address secondaryAddress = findSecondaryAddress(defaultAddress);
        if (secondaryAddress != null) {
            bindSecondaryAddress(secondaryAddress);
        }
    }

    private void bindExtraAddressViews() {
        if (boundAddresses.size() <= 2 || addressListContainer == null) {
            return;
        }

        Address defaultAddress = findDefaultAddress();
        LayoutInflater inflater = LayoutInflater.from(this);
        int insertIndex = addressListContainer.indexOfChild(secondaryContainer);
        Address secondaryAddress = findSecondaryAddress(defaultAddress);

        for (Address address : boundAddresses) {
            if (address.getId().equals(defaultAddress.getId())
                    || (secondaryAddress != null && address.getId().equals(secondaryAddress.getId()))) {
                continue;
            }
            View itemView = inflater.inflate(R.layout.item_address, addressListContainer, false);
            bindRegularAddressItem(itemView, address);
            addressListContainer.addView(itemView, insertIndex);
            insertIndex++;
        }
    }

    private void clearDynamicAddressViews() {
        if (addressListContainer == null) {
            return;
        }
        for (int index = addressListContainer.getChildCount() - 1; index >= 0; index--) {
            View child = addressListContainer.getChildAt(index);
            if (child != defaultContainer && child != secondaryContainer && child != emptyStateView) {
                addressListContainer.removeViewAt(index);
            }
        }
    }

    private Address findDefaultAddress() {
        for (Address address : boundAddresses) {
            if (address.isDefault()) {
                return address;
            }
        }
        return boundAddresses.get(0);
    }

    @Nullable
    private Address findSecondaryAddress(Address defaultAddress) {
        for (Address address : boundAddresses) {
            if (!address.getId().equals(defaultAddress.getId())) {
                return address;
            }
        }
        return null;
    }

    private void bindDefaultAddress(Address address) {
        bindAddressCard(
                defaultContainer,
                R.id.addressDefaultItemName,
                R.id.addressDefaultItemLine,
                R.id.addressDefaultItemPhone,
                address
        );
        View card = defaultContainer.findViewById(R.id.addressDefaultItemCard);
        View editButton = defaultContainer.findViewById(R.id.addressDefaultEditButton);
        View deleteButton = defaultContainer.findViewById(R.id.addressDefaultDeleteButton);

        if (card != null) {
            card.setOnClickListener(v -> openAddressForm(address));
        }
        if (editButton != null) {
            editButton.setOnClickListener(v -> openAddressForm(address));
        }
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> confirmDelete(address));
        }
    }

    private void bindSecondaryAddress(Address address) {
        bindAddressCard(
                secondaryContainer,
                R.id.addressItemName,
                R.id.addressItemLine,
                R.id.addressItemPhone,
                address
        );
        bindRegularAddressItem(secondaryContainer, address);
    }

    private void bindRegularAddressItem(View container, Address address) {
        View card = container.findViewById(R.id.addressItemCard);
        View editButton = container.findViewById(R.id.addressEditButton);
        View deleteButton = container.findViewById(R.id.addressDeleteButton);
        View defaultButton = container.findViewById(R.id.addressDefaultButton);

        if (card != null) {
            card.setOnClickListener(v -> openAddressForm(address));
        }
        if (editButton != null) {
            editButton.setOnClickListener(v -> openAddressForm(address));
        }
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> confirmDelete(address));
        }
        if (defaultButton != null) {
            defaultButton.setVisibility(address.isDefault() ? View.GONE : View.VISIBLE);
            defaultButton.setOnClickListener(v -> viewModel.setDefaultAddress(address.getId(), userId));
        }
    }

    private void bindAddressCard(
            View container,
            int nameId,
            int addressId,
            int phoneId,
            Address address
    ) {
        setText(container, nameId, address.getName());
        setText(container, phoneId, address.getPhone());
        setText(container, addressId, address.getFullAddressLine());
    }

    private void setText(View root, int viewId, String value) {
        TextView view = root.findViewById(viewId);
        if (view != null) {
            view.setText(value);
        }
    }

    private void showEmptyState(boolean show) {
        if (addressListContainer == null) {
            return;
        }
        if (show) {
            if (emptyStateView == null) {
                emptyStateView = new TextView(this);
                emptyStateView.setText(R.string.address_empty);
                emptyStateView.setTextColor(getColor(R.color.neutral_70));
                emptyStateView.setTextSize(16f);
                emptyStateView.setPadding(
                        getResources().getDimensionPixelSize(R.dimen.spacing_md),
                        getResources().getDimensionPixelSize(R.dimen.spacing_lg),
                        getResources().getDimensionPixelSize(R.dimen.spacing_md),
                        getResources().getDimensionPixelSize(R.dimen.spacing_md)
                );
            }
            if (emptyStateView.getParent() == null) {
                addressListContainer.addView(emptyStateView, 0);
            }
            emptyStateView.setVisibility(View.VISIBLE);
            return;
        }
        if (emptyStateView != null) {
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private void openAddressForm(@Nullable Address address) {
        Intent intent = new Intent(this, AddressFormActivity.class);
        if (address != null) {
            intent.putExtra(AddressFormActivity.EXTRA_ADDRESS_ID, address.getId());
            intent.putExtra(AddressFormActivity.EXTRA_NAME, address.getName());
            intent.putExtra(AddressFormActivity.EXTRA_PHONE, address.getPhone());
            intent.putExtra(AddressFormActivity.EXTRA_EMAIL, address.getEmail());
            intent.putExtra(AddressFormActivity.EXTRA_DETAIL, address.getDetail());
            intent.putExtra(AddressFormActivity.EXTRA_WARD, address.getWard());
            intent.putExtra(AddressFormActivity.EXTRA_DISTRICT, address.getDistrict());
            intent.putExtra(AddressFormActivity.EXTRA_CITY, address.getProvince());
            intent.putExtra(AddressFormActivity.EXTRA_IS_DEFAULT, address.isDefault());
            intent.putExtra(AddressFormActivity.EXTRA_EDIT_MODE, true);
        }
        addressFormLauncher.launch(intent);
    }

    private void confirmDelete(Address address) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.address_delete_confirm)
                .setNegativeButton(R.string.address_cancel, null)
                .setPositiveButton(R.string.address_delete, (dialog, which) ->
                        viewModel.deleteAddress(address.getId(), userId))
                .show();
    }
}
