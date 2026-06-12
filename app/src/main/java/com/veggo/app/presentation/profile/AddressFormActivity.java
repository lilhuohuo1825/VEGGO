package com.veggo.app.presentation.profile;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class AddressFormActivity extends BaseActivity {
    public static final String EXTRA_PREFILL = "extra_prefill";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_form);

        findViewById(R.id.addressFormBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.addressCancelButton).setOnClickListener(v -> finish());
        findViewById(R.id.addressDoneButton).setOnClickListener(v -> finish());

        if (getIntent().getBooleanExtra(EXTRA_PREFILL, false)) {
            fillAddressData();
        }
    }

    private void fillAddressData() {
        ((EditText) findViewById(R.id.addressNameInput)).setText(R.string.address_sample_name);
        ((EditText) findViewById(R.id.addressPhoneInput)).setText(R.string.address_sample_phone);
        ((EditText) findViewById(R.id.addressEmailInput)).setText(R.string.address_sample_email);
        ((TextView) findViewById(R.id.addressCityInput)).setText("TP.HCM");
        ((TextView) findViewById(R.id.addressDistrictInput)).setText("Vũng Tàu");
        ((TextView) findViewById(R.id.addressWardInput)).setText("Phường Vũng Tàu");
        ((EditText) findViewById(R.id.addressDetailInput)).setText("12345 Lê Hồng Phong");
    }
}
