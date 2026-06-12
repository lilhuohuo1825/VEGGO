package com.veggo.app.presentation.checkout;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;

public class QrPaymentActivity extends BaseActivity {
    private View layoutQrPayment;
    private View layoutOrderSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_payment);

        layoutQrPayment = findViewById(R.id.layoutQrPayment);
        layoutOrderSuccess = findViewById(R.id.layoutOrderSuccess);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView btnConfirmPaid = findViewById(R.id.btnConfirmPaid);
        TextView btnContinueShopping = findViewById(R.id.btnContinueShopping);
        TextView btnTrackOrder = findViewById(R.id.btnTrackOrder);

        btnBack.setOnClickListener(v -> finish());
        btnConfirmPaid.setOnClickListener(v -> showOrderSuccess());
        btnContinueShopping.setOnClickListener(v -> finish());
        btnTrackOrder.setOnClickListener(v -> finish());
    }

    private void showOrderSuccess() {
        layoutQrPayment.setVisibility(View.GONE);
        layoutOrderSuccess.setVisibility(View.VISIBLE);
    }
}
