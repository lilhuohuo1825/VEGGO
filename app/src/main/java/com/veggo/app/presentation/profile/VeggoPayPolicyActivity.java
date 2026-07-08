package com.veggo.app.presentation.profile;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.veggo.app.R;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.data.remote.dto.WalletDto;
import com.veggo.app.data.repository.WalletRepository;
import com.veggo.app.di.AppModule;

public class VeggoPayPolicyActivity extends BaseActivity {
    private CheckBox cbAgree;
    private Button btnActivate;
    private WalletRepository walletRepository;
    private String customerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veggopay_policy);

        cbAgree = findViewById(R.id.cbAgree);
        btnActivate = findViewById(R.id.btnActivate);

        walletRepository = AppModule.provideWalletRepository();
        customerId = new AppPreferences(this).getCustomerId();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        cbAgree.setOnCheckedChangeListener((buttonView, isChecked) -> btnActivate.setEnabled(isChecked));

        btnActivate.setOnClickListener(v -> showCreatePinDialog());
    }

    private void showCreatePinDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_veggopay_password_input);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvPinTitle);
        TextView tvDesc = dialog.findViewById(R.id.tvPinDescription);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        TextView btnConfirm = dialog.findViewById(R.id.btnConfirm);

        tvTitle.setText("Thiết lập mật khẩu ví");
        tvDesc.setText("Nhập 6 số làm mật khẩu thanh toán ví VeggoPay");

        EditText[] pinFields = new EditText[] {
                dialog.findViewById(R.id.edtPin1),
                dialog.findViewById(R.id.edtPin2),
                dialog.findViewById(R.id.edtPin3),
                dialog.findViewById(R.id.edtPin4),
                dialog.findViewById(R.id.edtPin5),
                dialog.findViewById(R.id.edtPin6)
        };

        setupPinAutoShift(pinFields, btnConfirm);
        pinFields[0].requestFocus();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            StringBuilder pinBuilder = new StringBuilder();
            for (EditText edt : pinFields) {
                pinBuilder.append(edt.getText().toString().trim());
            }
            String pinCode = pinBuilder.toString();
            if (pinCode.length() < 6) {
                Toast.makeText(this, "Vui lòng nhập đủ 6 chữ số mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            showProgress("Đang kích hoạt ví...");
            walletRepository.activateWallet(customerId, pinCode, new WalletRepository.ResultCallback<WalletDto>() {
                @Override
                public void onSuccess(WalletDto result) {
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(VeggoPayPolicyActivity.this, "Kích hoạt ví thành công!", Toast.LENGTH_SHORT).show();
                        
                        // Open main wallet activity and finish policy
                        startActivity(new Intent(VeggoPayPolicyActivity.this, VeggoPayActivity.class));
                        finish();
                    });
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(VeggoPayPolicyActivity.this, "Kích hoạt ví thất bại: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        dialog.show();
    }

    private void setupPinAutoShift(final EditText[] pinFields, final View btnConfirm) {
        for (int i = 0; i < pinFields.length; i++) {
            final int index = i;
            pinFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1) {
                        if (index < pinFields.length - 1) {
                            pinFields[index + 1].requestFocus();
                        } else if (btnConfirm != null) {
                            btnConfirm.performClick();
                        }
                    }
                }
            });

            pinFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (pinFields[index].getText().length() == 0 && index > 0) {
                        pinFields[index - 1].requestFocus();
                        pinFields[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private Dialog progressDialog;
    private TextView progressTextView;

    private void showProgress(String message) {
        if (progressDialog == null) {
            progressDialog = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
            android.widget.RelativeLayout rootLayout = new android.widget.RelativeLayout(this);
            rootLayout.setBackgroundColor(android.graphics.Color.parseColor("#99000000"));
            android.widget.LinearLayout container = new android.widget.LinearLayout(this);
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setGravity(android.view.Gravity.CENTER);
            android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this);
            progressTextView = new TextView(this);
            progressTextView.setText(message);
            progressTextView.setTextColor(android.graphics.Color.WHITE);
            progressTextView.setTextSize(16);
            progressTextView.setGravity(android.view.Gravity.CENTER);
            progressTextView.setPadding(30, 32, 30, 0);
            container.addView(progressBar);
            container.addView(progressTextView);
            android.widget.RelativeLayout.LayoutParams layoutParams = new android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
            rootLayout.addView(container, layoutParams);
            progressDialog.setContentView(rootLayout);
            progressDialog.setCancelable(false);
        } else {
            progressTextView.setText(message);
        }
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
