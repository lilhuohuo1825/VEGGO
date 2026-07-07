package com.veggo.app.presentation.profile;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.utils.CurrencyFormatter;

import java.text.NumberFormat;
import java.util.Locale;

public class VeggoPayMyQrActivity extends AppCompatActivity {

    private FrameLayout cardContainer;
    private ImageView imgCardTheme, imgBarcode;
    private TextView tvUserName, tvUserPhone, tvAmountHint, btnAddAmount;
    private TextView tvPaymentNote, tvPaymentTimer;
    private View layoutThemeSelection;

    // Tabs
    private FrameLayout tabReceive, tabPay;
    private TextView tvTabReceive, tvTabPay;
    private View indicatorReceive, indicatorPay;

    private String rawPhone = "";
    private CountDownTimer payTimer;
    private boolean isReceiveTab = true;
    private int selectedThemeColor = Color.WHITE;
    private boolean isCustomThemeImage = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veggopay_my_qr);

        cardContainer = findViewById(R.id.cardContainer);
        imgCardTheme = findViewById(R.id.imgCardTheme);
        imgBarcode = findViewById(R.id.imgBarcode);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        tvAmountHint = findViewById(R.id.tvAmountHint);
        btnAddAmount = findViewById(R.id.btnAddAmount);
        
        tvPaymentNote = findViewById(R.id.tvPaymentNote);
        tvPaymentTimer = findViewById(R.id.tvPaymentTimer);
        layoutThemeSelection = findViewById(R.id.layoutThemeSelection);

        // Tab Views
        tabReceive = findViewById(R.id.tabReceive);
        tabPay = findViewById(R.id.tabPay);
        tvTabReceive = findViewById(R.id.tvTabReceive);
        tvTabPay = findViewById(R.id.tvTabPay);
        indicatorReceive = findViewById(R.id.indicatorReceive);
        indicatorPay = findViewById(R.id.indicatorPay);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Get user details from preferences
        AppPreferences prefs = new AppPreferences(this);
        String name = prefs.getFullName();
        rawPhone = prefs.getCurrentPhone();

        if (name != null && !name.trim().isEmpty()) {
            tvUserName.setText(name.toUpperCase());
        } else {
            tvUserName.setText("KHÁCH HÀNG VEGGO");
        }

        if (rawPhone != null && rawPhone.length() >= 7) {
            String masked = rawPhone.substring(0, 3) + "****" + rawPhone.substring(rawPhone.length() - 3);
            tvUserPhone.setText("SĐT: " + masked);
        } else {
            tvUserPhone.setText("SĐT: Liên kết ví");
        }

        // Tab selection events
        tabReceive.setOnClickListener(v -> selectReceiveTab());
        tabPay.setOnClickListener(v -> selectPayTab());

        // Add amount listener
        btnAddAmount.setOnClickListener(v -> showAddAmountDialog());

        // Setup theme selectors
        findViewById(R.id.btnThemeDefault).setOnClickListener(v -> {
            isCustomThemeImage = false;
            imgCardTheme.setVisibility(View.GONE);
            selectedThemeColor = Color.WHITE;
            cardContainer.setBackgroundColor(selectedThemeColor);
        });

        findViewById(R.id.btnThemePink).setOnClickListener(v -> {
            isCustomThemeImage = false;
            imgCardTheme.setVisibility(View.GONE);
            selectedThemeColor = Color.parseColor("#FFE4E1");
            cardContainer.setBackgroundColor(selectedThemeColor);
        });

        findViewById(R.id.btnThemeYellow).setOnClickListener(v -> {
            isCustomThemeImage = false;
            imgCardTheme.setVisibility(View.GONE);
            selectedThemeColor = Color.parseColor("#FFF8DC");
            cardContainer.setBackgroundColor(selectedThemeColor);
        });

        findViewById(R.id.btnThemeBlue).setOnClickListener(v -> {
            isCustomThemeImage = false;
            imgCardTheme.setVisibility(View.GONE);
            selectedThemeColor = Color.parseColor("#E0FFFF");
            cardContainer.setBackgroundColor(selectedThemeColor);
        });

        // "+ Thêm ảnh" mock background selection
        findViewById(R.id.btnThemeAdd).setOnClickListener(v -> {
            isCustomThemeImage = true;
            imgCardTheme.setVisibility(View.VISIBLE);
            imgCardTheme.setImageResource(R.drawable.bg_button_gradient);
            Toast.makeText(this, "Đã áp dụng hình nền tùy chỉnh!", Toast.LENGTH_SHORT).show();
        });

        // Down / Share actions
        findViewById(R.id.btnDownload).setOnClickListener(v -> 
            Toast.makeText(this, "Đã lưu mã QR thành công vào thư viện ảnh!", Toast.LENGTH_LONG).show());

        findViewById(R.id.btnShare).setOnClickListener(v -> 
            Toast.makeText(this, "Đang chuẩn bị chia sẻ mã QR ví của bạn...", Toast.LENGTH_SHORT).show());

        // Start default tab state
        selectReceiveTab();
    }

    private void selectReceiveTab() {
        isReceiveTab = true;
        
        // Stop timer
        if (payTimer != null) {
            payTimer.cancel();
        }

        // Tab state updates
        tvTabReceive.setTextColor(Color.parseColor("#388E3C"));
        indicatorReceive.setVisibility(View.VISIBLE);
        tvTabPay.setTextColor(Color.parseColor("#707070"));
        indicatorPay.setVisibility(View.INVISIBLE);

        // Subviews visibility
        imgBarcode.setVisibility(View.GONE);
        tvPaymentNote.setVisibility(View.GONE);
        tvPaymentTimer.setVisibility(View.GONE);

        btnAddAmount.setVisibility(View.VISIBLE);
        if (tvAmountHint.getText().length() > 0) {
            tvAmountHint.setVisibility(View.VISIBLE);
        }
        layoutThemeSelection.setVisibility(View.VISIBLE);

        // Restore card color
        if (isCustomThemeImage) {
            imgCardTheme.setVisibility(View.VISIBLE);
        } else {
            imgCardTheme.setVisibility(View.GONE);
            cardContainer.setBackgroundColor(selectedThemeColor);
        }
    }

    private void selectPayTab() {
        isReceiveTab = false;

        // Tab state updates
        tvTabReceive.setTextColor(Color.parseColor("#707070"));
        indicatorReceive.setVisibility(View.INVISIBLE);
        tvTabPay.setTextColor(Color.parseColor("#388E3C"));
        indicatorPay.setVisibility(View.VISIBLE);

        // Subviews visibility
        imgBarcode.setVisibility(View.VISIBLE);
        tvPaymentNote.setVisibility(View.VISIBLE);
        tvPaymentTimer.setVisibility(View.VISIBLE);

        btnAddAmount.setVisibility(View.GONE);
        tvAmountHint.setVisibility(View.GONE);
        layoutThemeSelection.setVisibility(View.GONE);

        // Payment QR must look standard (clean white card background for scanner readability)
        imgCardTheme.setVisibility(View.GONE);
        cardContainer.setBackgroundColor(Color.WHITE);

        // Start payment code refresh countdown timer
        startPaymentTimer();
    }

    private void startPaymentTimer() {
        if (payTimer != null) {
            payTimer.cancel();
        }

        payTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvPaymentTimer.setText("Mã sẽ tự động làm mới sau " + (millisUntilFinished / 1000) + " giây");
            }

            @Override
            public void onFinish() {
                tvPaymentTimer.setText("Đang cập nhật mã mới...");
                new Handler().postDelayed(() -> {
                    if (!isDestroyed() && !isReceiveTab) {
                        startPaymentTimer();
                    }
                }, 1000);
            }
        }.start();
    }

    private void showAddAmountDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_veggopay_password_input); // repurpose dialog container
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvPinTitle);
        TextView tvDesc = dialog.findViewById(R.id.tvPinDescription);
        ViewGroup fieldsContainer = dialog.findViewById(R.id.layoutPinFields);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        TextView btnConfirm = dialog.findViewById(R.id.btnConfirm);

        tvTitle.setText("Thêm số tiền nhận");
        tvDesc.setText("Nhập số tiền bạn muốn người khác chuyển khoản");

        // Dynamically replace 6 OTP fields with a single EditText for money input
        fieldsContainer.removeAllViews();
        EditText edtAmountInput = new EditText(this);
        edtAmountInput.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        edtAmountInput.setHint("Nhập số tiền (đ)");
        edtAmountInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        edtAmountInput.setGravity(android.view.Gravity.CENTER);
        edtAmountInput.setTextSize(20);
        edtAmountInput.setBackgroundResource(R.drawable.bg_input);
        edtAmountInput.setPadding(20, 20, 20, 20);
        fieldsContainer.addView(edtAmountInput);

        // Format money input automatically
        edtAmountInput.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    edtAmountInput.removeTextChangedListener(this);
                    String cleanString = s.toString().replaceAll("[.,\\s]", "");
                    if (!cleanString.isEmpty()) {
                        try {
                            double parsed = Double.parseDouble(cleanString);
                            String formatted = NumberFormat.getNumberInstance(Locale.GERMANY).format(parsed);
                            current = formatted;
                            edtAmountInput.setText(formatted);
                            edtAmountInput.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            // Ignored
                        }
                    } else {
                        current = "";
                        edtAmountInput.setText("");
                    }
                    edtAmountInput.addTextChangedListener(this);
                }
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String text = edtAmountInput.getText().toString().trim().replaceAll("[.,\\s]", "");
            if (!text.isEmpty()) {
                long amount = Long.parseLong(text);
                tvAmountHint.setText("Số tiền yêu cầu: " + CurrencyFormatter.formatVnd(amount));
                tvAmountHint.setVisibility(View.VISIBLE);
                btnAddAmount.setText("Thay đổi số tiền");
            } else {
                tvAmountHint.setText("");
                tvAmountHint.setVisibility(View.GONE);
                btnAddAmount.setText("+ Thêm số tiền");
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        if (payTimer != null) {
            payTimer.cancel();
        }
        super.onDestroy();
    }
}
