package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.veggo.app.R;
import com.veggo.app.core.address.VietnamAddressTree;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.model.Address;
import com.veggo.app.presentation.dialog.VeggoDialog;

import java.util.List;

public class AddressFormActivity extends BaseActivity {
    public static final String EXTRA_EDIT_MODE = "extra_edit_mode";
    public static final String EXTRA_ADDRESS_ID = "extra_address_id";
    public static final String EXTRA_PREFILL = "extra_prefill";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_PHONE = "extra_phone";
    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_DETAIL = "extra_detail";
    public static final String EXTRA_WARD = "extra_ward";
    public static final String EXTRA_DISTRICT = "extra_district";
    public static final String EXTRA_CITY = "extra_city";
    public static final String EXTRA_IS_DEFAULT = "extra_is_default";

    private AddressFormViewModel viewModel;
    private AppPreferences appPreferences;
    private String userId;
    @Nullable
    private String addressId;
    private boolean isEditMode;
    private boolean isDefaultSelected;

    private TextView provinceView;
    private TextView districtView;
    private TextView wardView;
    private TextView defaultCheckbox;
    private TextView cancelButton;
    private TextView doneButton;

    @Nullable
    private VietnamAddressTree addressTree;
    @Nullable
    private String selectedProvince;
    @Nullable
    private String selectedDistrict;
    @Nullable
    private String selectedWard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "sổ địa chỉ")) {
            return;
        }
        setContentView(R.layout.activity_address_form);

        appPreferences = new AppPreferences(this);
        userId = appPreferences.getCustomerId();
        isEditMode = getIntent().getBooleanExtra(EXTRA_EDIT_MODE, false)
                || getIntent().getBooleanExtra(EXTRA_PREFILL, false);
        addressId = getIntent().getStringExtra(EXTRA_ADDRESS_ID);
        isDefaultSelected = getIntent().getBooleanExtra(EXTRA_IS_DEFAULT, false);

        bindViews();
        setupActions();
        setupViewModel();
        tintRequiredMarkers();
        updateDefaultCheckboxUi();

        if (isEditMode) {
            fillAddressDataFromIntent();
        } else {
            prefillPersonalDataFromSession();
        }

        viewModel.loadAddressTree();
    }

    private void bindViews() {
        provinceView = findViewById(R.id.addressCityInput);
        districtView = findViewById(R.id.addressDistrictInput);
        wardView = findViewById(R.id.addressWardInput);
        defaultCheckbox = findViewById(R.id.addressDefaultCheckbox);
        cancelButton = findViewById(R.id.addressCancelButton);
        doneButton = findViewById(R.id.addressDoneButton);
        configureBottomActions();
    }

    private void setupActions() {
        findViewById(R.id.addressFormBackButton).setOnClickListener(v -> finish());
        cancelButton.setOnClickListener(v -> {
            if (isExistingAddressEdit()) {
                confirmDeleteAddress();
            } else {
                finish();
            }
        });
        doneButton.setOnClickListener(v -> submitForm());

        provinceView.setOnClickListener(v -> showProvincePicker());
        districtView.setOnClickListener(v -> showDistrictPicker());
        wardView.setOnClickListener(v -> showWardPicker());
        defaultCheckbox.setOnClickListener(v -> {
            isDefaultSelected = !isDefaultSelected;
            updateDefaultCheckboxUi();
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(
                this,
                new AddressFormViewModelFactory(AppModule.provideAddressRepository(this))
        ).get(AddressFormViewModel.class);

        viewModel.getLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            doneButton.setEnabled(!loading);
            doneButton.setAlpha(loading ? 0.6f : 1f);
        });

        viewModel.getTreeLoading().observe(this, isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            setLocationInputsEnabled(!loading && addressTree != null && !addressTree.isEmpty());
        });

        viewModel.getAddressTree().observe(this, tree -> {
            addressTree = tree;
            boolean enabled = tree != null && !tree.isEmpty();
            setLocationInputsEnabled(enabled);
            if (!enabled) {
                Toast.makeText(this, "Không thể tải dữ liệu Tỉnh/Huyện/Xã", Toast.LENGTH_SHORT).show();
                return;
            }
            preselectSavedLocations();
        });

        viewModel.getSavedAddress().observe(this, this::onAddressSaved);
        viewModel.getDeletedAddress().observe(this, deleted -> {
            if (!Boolean.TRUE.equals(deleted)) {
                return;
            }
            Toast.makeText(this, "Đã xóa địa chỉ", Toast.LENGTH_SHORT).show();
            AddressSelectionStore.remove(addressId);
            setResult(RESULT_OK);
            finish();
        });

        viewModel.getError().observe(this, message -> {
            if (message == null || message.isEmpty()) {
                return;
            }
            if (Boolean.TRUE.equals(viewModel.getRetryableError().getValue())) {
                Snackbar.make(doneButton, message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.consultation_submit_retry, v -> {
                            if (addressTree == null || addressTree.isEmpty()) {
                                viewModel.loadAddressTree();
                            } else {
                                submitForm();
                            }
                        })
                        .show();
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void prefillPersonalDataFromSession() {
        String name = appPreferences.getFullName();
        if (name != null && !name.isEmpty()) {
            setEditText(R.id.addressNameInput, name);
        }
        String phone = appPreferences.getCurrentPhone();
        if (phone != null && !phone.isEmpty()) {
            setEditText(R.id.addressPhoneInput, phone);
        }
        String email = appPreferences.getEmail();
        if (email != null && !email.isEmpty()) {
            setEditText(R.id.addressEmailInput, email);
        }
    }

    private void fillAddressDataFromIntent() {
        Address selectedAddress = AddressSelectionStore.get(addressId);
        if (selectedAddress != null) {
            fillAddressData(selectedAddress);
            return;
        }

        setEditText(R.id.addressNameInput, getIntent().getStringExtra(EXTRA_NAME));
        setEditText(R.id.addressPhoneInput, getIntent().getStringExtra(EXTRA_PHONE));
        setEditText(R.id.addressEmailInput, getIntent().getStringExtra(EXTRA_EMAIL));
        setEditText(R.id.addressDetailInput, getIntent().getStringExtra(EXTRA_DETAIL));

        selectedProvince = getIntent().getStringExtra(EXTRA_CITY);
        selectedDistrict = getIntent().getStringExtra(EXTRA_DISTRICT);
        selectedWard = getIntent().getStringExtra(EXTRA_WARD);
        updateLocationViews();
    }

    private void fillAddressData(Address address) {
        setEditText(R.id.addressNameInput, address.getName());
        setEditText(R.id.addressPhoneInput, address.getPhone());
        setEditText(R.id.addressEmailInput, address.getEmail());
        setEditText(R.id.addressDetailInput, address.getDetail());
        selectedProvince = address.getProvince();
        selectedDistrict = address.getDistrict();
        selectedWard = address.getWard();
        isDefaultSelected = address.isDefault();
        updateDefaultCheckboxUi();
        updateLocationViews();
    }

    private void preselectSavedLocations() {
        if (addressTree == null) {
            return;
        }
        if (selectedProvince != null) {
            selectedProvince = addressTree.findMatchingProvince(selectedProvince);
        }
        if (selectedProvince != null && selectedDistrict != null) {
            selectedDistrict = addressTree.findMatchingDistrict(selectedProvince, selectedDistrict);
        }
        if (selectedProvince != null && selectedDistrict != null && selectedWard != null) {
            selectedWard = addressTree.findMatchingWard(selectedProvince, selectedDistrict, selectedWard);
        }
        updateLocationViews();
    }

    private void showProvincePicker() {
        if (addressTree == null || addressTree.isEmpty()) {
            Toast.makeText(this, "Dữ liệu Tỉnh/Huyện/Xã chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }
        showPicker(getString(R.string.address_city), addressTree.getProvinces(), selectedProvince, value -> {
            if (value.equals(selectedProvince)) {
                return;
            }
            selectedProvince = value;
            selectedDistrict = null;
            selectedWard = null;
            updateLocationViews();
        });
    }

    private void showDistrictPicker() {
        if (addressTree == null || selectedProvince == null) {
            Toast.makeText(this, "Vui lòng chọn Tỉnh/Thành phố trước", Toast.LENGTH_SHORT).show();
            return;
        }
        showPicker(getString(R.string.address_district), addressTree.getDistricts(selectedProvince), selectedDistrict, value -> {
            if (value.equals(selectedDistrict)) {
                return;
            }
            selectedDistrict = value;
            selectedWard = null;
            updateLocationViews();
        });
    }

    private void showWardPicker() {
        if (addressTree == null || selectedProvince == null || selectedDistrict == null) {
            Toast.makeText(this, "Vui lòng chọn Quận/Huyện trước", Toast.LENGTH_SHORT).show();
            return;
        }
        showPicker(
                getString(R.string.address_ward),
                addressTree.getWards(selectedProvince, selectedDistrict),
                selectedWard,
                value -> {
                    selectedWard = value;
                    updateLocationViews();
                }
        );
    }

    private void showPicker(String title, List<String> options, @Nullable String currentValue, PickerCallback callback) {
        if (options.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu để chọn", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = options.toArray(new String[0]);
        int checkedItem = 0;
        if (currentValue != null) {
            for (int index = 0; index < options.size(); index++) {
                if (options.get(index).equals(currentValue)) {
                    checkedItem = index;
                    break;
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                    callback.onSelected(items[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.address_cancel, null)
                .show();
    }

    private void updateLocationViews() {
        setLocationText(provinceView, selectedProvince, R.string.address_city_hint);
        setLocationText(districtView, selectedDistrict, R.string.address_district_hint);
        setLocationText(wardView, selectedWard, R.string.address_ward_hint);
    }

    private void setLocationText(TextView view, @Nullable String value, int hintRes) {
        if (value == null || value.isEmpty()) {
            view.setText(hintRes);
            view.setTextColor(getColor(R.color.neutral_60));
            return;
        }
        view.setText(value);
        view.setTextColor(getColor(R.color.neutral_100));
    }

    private void setLocationInputsEnabled(boolean enabled) {
        provinceView.setEnabled(enabled);
        districtView.setEnabled(enabled);
        wardView.setEnabled(enabled);
        provinceView.setAlpha(enabled ? 1f : 0.5f);
        districtView.setAlpha(enabled ? 1f : 0.5f);
        wardView.setAlpha(enabled ? 1f : 0.5f);
    }

    private void updateDefaultCheckboxUi() {
        defaultCheckbox.setBackgroundResource(isDefaultSelected
                ? R.drawable.bg_address_checkbox_checked
                : R.drawable.bg_address_checkbox);
    }

    private void submitForm() {
        viewModel.saveAddress(
                addressId,
                userId,
                textOf(R.id.addressNameInput),
                textOf(R.id.addressPhoneInput),
                textOf(R.id.addressEmailInput),
                selectedProvince == null ? "" : selectedProvince,
                selectedDistrict == null ? "" : selectedDistrict,
                selectedWard == null ? "" : selectedWard,
                textOf(R.id.addressDetailInput),
                isDefaultSelected
        );
    }

    private void configureBottomActions() {
        if (!isExistingAddressEdit()) {
            return;
        }
        cancelButton.setText(R.string.address_delete);
        cancelButton.setBackgroundResource(R.drawable.bg_button_outline_red);
        cancelButton.setTextColor(getColor(R.color.danger_main));
        doneButton.setText("Lưu");
    }

    private boolean isExistingAddressEdit() {
        return isEditMode && addressId != null && !addressId.trim().isEmpty();
    }

    private void confirmDeleteAddress() {
        VeggoDialog.show(
                this,
                R.drawable.ic_trash,
                "Xoá địa chỉ?",
                getString(R.string.address_delete_confirm),
                getString(R.string.address_delete),
                getString(R.string.address_cancel),
                new VeggoDialog.DialogListener() {
                    @Override
                    public void onConfirm() {
                        viewModel.deleteAddress(addressId);
                    }
                }
        );
    }

    private void onAddressSaved(Address address) {
        String message = isEditMode
                ? getString(R.string.address_update_success)
                : getString(R.string.address_add_success);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        AddressSelectionStore.remove(addressId);
        setResult(RESULT_OK, buildSavedAddressResult(address));
        finish();
    }

    private Intent buildSavedAddressResult(Address address) {
        Intent data = new Intent();
        if (address == null) {
            data.putExtra(EXTRA_NAME, textOf(R.id.addressNameInput));
            data.putExtra(EXTRA_PHONE, textOf(R.id.addressPhoneInput));
            data.putExtra(EXTRA_EMAIL, textOf(R.id.addressEmailInput));
            data.putExtra(EXTRA_DETAIL, textOf(R.id.addressDetailInput));
            data.putExtra(EXTRA_WARD, selectedWard == null ? "" : selectedWard);
            data.putExtra(EXTRA_DISTRICT, selectedDistrict == null ? "" : selectedDistrict);
            data.putExtra(EXTRA_CITY, selectedProvince == null ? "" : selectedProvince);
            data.putExtra(EXTRA_IS_DEFAULT, isDefaultSelected);
            return data;
        }

        data.putExtra(EXTRA_NAME, address.getName());
        data.putExtra(EXTRA_PHONE, address.getPhone());
        data.putExtra(EXTRA_EMAIL, address.getEmail());
        data.putExtra(EXTRA_DETAIL, address.getDetail());
        data.putExtra(EXTRA_WARD, address.getWard());
        data.putExtra(EXTRA_DISTRICT, address.getDistrict());
        data.putExtra(EXTRA_CITY, address.getProvince());
        data.putExtra(EXTRA_IS_DEFAULT, address.isDefault());
        return data;
    }

    private void setEditText(int viewId, @Nullable String value) {
        if (value == null) {
            return;
        }
        ((EditText) findViewById(viewId)).setText(value);
    }

    private void tintRequiredMarkers() {
        int[] labelIds = {
                R.id.addressNameLabel,
                R.id.addressPhoneLabel,
                R.id.addressCityLabel,
                R.id.addressDistrictLabel,
                R.id.addressWardLabel,
                R.id.addressDetailLabel
        };
        for (int labelId : labelIds) {
            TextView label = findViewById(labelId);
            if (label != null) {
                tintRequiredMarker(label);
            }
        }
    }

    private void tintRequiredMarker(TextView label) {
        String text = label.getText().toString();
        int markerIndex = text.indexOf('*');
        if (markerIndex < 0) {
            return;
        }
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(
                new ForegroundColorSpan(getColor(R.color.danger_main)),
                markerIndex,
                markerIndex + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        label.setText(spannable);
    }

    private String textOf(int viewId) {
        TextView view = findViewById(viewId);
        return view == null ? "" : view.getText().toString().trim();
    }

    private interface PickerCallback {
        void onSelected(String value);
    }
}
