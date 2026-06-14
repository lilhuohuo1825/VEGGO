package com.veggo.app.presentation.profile;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;
import android.widget.TextView;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class AddressFormActivity extends BaseActivity {
    public static final String EXTRA_PREFILL = "extra_prefill";
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_PHONE = "extra_phone";
    public static final String EXTRA_EMAIL = "extra_email";
    public static final String EXTRA_DETAIL = "extra_detail";
    public static final String EXTRA_WARD = "extra_ward";
    public static final String EXTRA_DISTRICT = "extra_district";
    public static final String EXTRA_CITY = "extra_city";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_form);

        findViewById(R.id.addressFormBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.addressCancelButton).setOnClickListener(v -> finish());
        findViewById(R.id.addressDoneButton).setOnClickListener(v -> finish());
        tintRequiredMarkers();

        if (getIntent().getBooleanExtra(EXTRA_PREFILL, false)) {
            fillAddressDataFromIntent();
        }
    }

    private void fillAddressDataFromIntent() {
        setEditText(R.id.addressNameInput, getIntent().getStringExtra(EXTRA_NAME));
        setEditText(R.id.addressPhoneInput, getIntent().getStringExtra(EXTRA_PHONE));
        setEditText(R.id.addressEmailInput, getIntent().getStringExtra(EXTRA_EMAIL));
        setTextView(R.id.addressDetailInput, getIntent().getStringExtra(EXTRA_DETAIL));
        setTextView(R.id.addressWardInput, getIntent().getStringExtra(EXTRA_WARD));
        setTextView(R.id.addressDistrictInput, getIntent().getStringExtra(EXTRA_DISTRICT));
        setTextView(R.id.addressCityInput, getIntent().getStringExtra(EXTRA_CITY));
    }

    private void setEditText(int viewId, String value) {
        if (value == null) {
            return;
        }
        ((EditText) findViewById(viewId)).setText(value);
    }

    private void setTextView(int viewId, String value) {
        if (value == null) {
            return;
        }
        ((TextView) findViewById(viewId)).setText(value);
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
}
