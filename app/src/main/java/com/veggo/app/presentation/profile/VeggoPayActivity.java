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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.veggo.app.R;
import com.veggo.app.adapter.BankSpinnerAdapter;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.di.AppModule;
import com.veggo.app.data.remote.dto.WalletDto;
import com.veggo.app.data.repository.WalletRepository;

import java.util.List;

public class VeggoPayActivity extends BaseActivity {
    private TextView tvBalance;
    private TextView tvLinkedBanksHeader;
    private LinearLayout llLinkedBanks;
    private View nsvLinkedBanks;
    private WalletRepository walletRepository;
    private String customerId;
    private List<WalletDto.LinkedBankDto> linkedBanksList;

    // Show/Hide balance variables
    private boolean isBalanceHidden = false;
    private double currentBalance = 0;
    private ImageView btnToggleBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (LoginRequiredActivity.redirectIfGuest(this, "ví VeggoPay")) {
            return;
        }
        setContentView(R.layout.activity_veggopay);

        tvBalance = findViewById(R.id.tvBalance);
        tvLinkedBanksHeader = findViewById(R.id.tvLinkedBanksHeader);
        llLinkedBanks = findViewById(R.id.llLinkedBanks);
        nsvLinkedBanks = findViewById(R.id.nsvLinkedBanks);
        btnToggleBalance = findViewById(R.id.btnToggleBalance);

        walletRepository = AppModule.provideWalletRepository();
        customerId = new AppPreferences(this).getCustomerId();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDeposit).setOnClickListener(v -> showDepositDialog());
        findViewById(R.id.btnLinkBank).setOnClickListener(v -> showLinkBankDialog());
        TextView tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        findViewById(R.id.btnHistory).setOnClickListener(v -> {
            if (tvNotificationBadge != null) {
                tvNotificationBadge.setVisibility(View.GONE);
            }
            startActivity(new Intent(this, VeggoPayHistoryActivity.class));
        });

        findViewById(R.id.btnScanQr).setOnClickListener(v -> startActivity(new Intent(this, VeggoPayScanActivity.class)));
        findViewById(R.id.btnTransfer).setOnClickListener(v -> startActivity(new Intent(this, VeggoPayTransferActivity.class)));
        findViewById(R.id.btnDonate).setOnClickListener(v -> startActivity(new Intent(this, VeggoPayDonateActivity.class)));
        findViewById(R.id.btnReceiveQr).setOnClickListener(v -> startActivity(new Intent(this, VeggoPayMyQrActivity.class)));

        if (btnToggleBalance != null) {
            btnToggleBalance.setOnClickListener(v -> {
                isBalanceHidden = !isBalanceHidden;
                updateBalanceDisplay();
            });
        }

        checkWalletStatus();
    }

    private void checkWalletStatus() {
        if (TextUtils.isEmpty(customerId)) return;
        showProgress("Đang kiểm tra trạng thái ví...");
        walletRepository.getWalletInfo(customerId, new WalletRepository.ResultCallback<WalletDto>() {
            @Override
            public void onSuccess(WalletDto wallet) {
                runOnUiThread(() -> {
                    hideProgress();
                    // Redirect to policy screen if wallet is inactive
                    if (!"active".equals(wallet.getStatus())) {
                        startActivity(new Intent(VeggoPayActivity.this, VeggoPayPolicyActivity.class));
                        finish();
                    } else {
                        // Wallet is active, ask for password before entering
                        showEntrancePasswordDialog();
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> {
                    hideProgress();
                    Toast.makeText(VeggoPayActivity.this, "Lỗi ví: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void showEntrancePasswordDialog() {
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

        tvTitle.setText("Mở khóa VeggoPay");
        tvDesc.setText("Nhập mật khẩu 6 số của ví để mở khóa truy cập");

        EditText[] pinFields = new EditText[] {
                dialog.findViewById(R.id.edtPin1),
                dialog.findViewById(R.id.edtPin2),
                dialog.findViewById(R.id.edtPin3),
                dialog.findViewById(R.id.edtPin4),
                dialog.findViewById(R.id.edtPin5),
                dialog.findViewById(R.id.edtPin6)
        };

        setupPinAutoShift(pinFields, btnConfirm);

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); // Exit activity if canceled
        });

        btnConfirm.setOnClickListener(v -> {
            StringBuilder pinBuilder = new StringBuilder();
            for (EditText edt : pinFields) {
                pinBuilder.append(edt.getText().toString().trim());
            }
            String pinCode = pinBuilder.toString();
            if (pinCode.length() < 6) {
                Toast.makeText(this, "Vui lòng nhập đủ 6 số", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            showProgress("Đang xác thực mật khẩu...");
            walletRepository.verifyPassword(customerId, pinCode, new WalletRepository.ResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean success) {
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(VeggoPayActivity.this, "Truy cập ví VeggoPay thành công!", Toast.LENGTH_SHORT).show();
                        loadWalletInfo(); // Proceed loading wallet details
                    });
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(VeggoPayActivity.this, "Mật khẩu ví không đúng. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                        // Re-prompt password input
                        showEntrancePasswordDialog();
                    });
                }
            });
        });

        dialog.setCancelable(false);
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

    private void updateBalanceDisplay() {
        if (tvBalance == null) return;
        if (isBalanceHidden) {
            tvBalance.setText("•••••• ₫");
            if (btnToggleBalance != null) {
                btnToggleBalance.setImageResource(R.drawable.ic_eye_hide_dark);
            }
        } else {
            tvBalance.setText(CurrencyFormatter.formatVnd((long) currentBalance));
            if (btnToggleBalance != null) {
                btnToggleBalance.setImageResource(R.drawable.ic_eye_show_dark);
            }
        }
    }

    private void loadWalletInfo() {
        if (TextUtils.isEmpty(customerId)) return;
        walletRepository.getWalletInfo(customerId, new WalletRepository.ResultCallback<WalletDto>() {
            @Override
            public void onSuccess(WalletDto wallet) {
                runOnUiThread(() -> {
                    currentBalance = wallet.getBalance();
                    updateBalanceDisplay();
                    
                    // Display linked banks list with logos
                    List<WalletDto.LinkedBankDto> banks = wallet.getLinkedBanks();
                    linkedBanksList = banks;
                    if (banks != null && !banks.isEmpty()) {
                        tvLinkedBanksHeader.setVisibility(View.VISIBLE);
                        if (nsvLinkedBanks != null) {
                            nsvLinkedBanks.setVisibility(View.VISIBLE);
                            ViewGroup.LayoutParams params = nsvLinkedBanks.getLayoutParams();
                            if (banks.size() > 3) {
                                float density = getResources().getDisplayMetrics().density;
                                params.height = (int) (160 * density);
                            } else {
                                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            }
                            nsvLinkedBanks.setLayoutParams(params);
                        }
                        llLinkedBanks.removeAllViews();
                        
                        for (WalletDto.LinkedBankDto bank : banks) {
                            View itemView = getLayoutInflater().inflate(R.layout.item_linked_bank, llLinkedBanks, false);
                            ImageView imgLogo = itemView.findViewById(R.id.imgBankLogo);
                            TextView tvInfo = itemView.findViewById(R.id.tvBankInfo);
                            TextView tvDefaultBadge = itemView.findViewById(R.id.tvDefaultBadge);
                            
                            String infoText = bank.getBankCode() + " - " + bank.getAccountNumber();
                            tvInfo.setText(infoText);
                            if (bank.isDefault()) {
                                tvDefaultBadge.setVisibility(View.VISIBLE);
                            } else {
                                tvDefaultBadge.setVisibility(View.GONE);
                            }
                            
                            // Bind logo based on bank code
                            int logoRes = R.drawable.ic_card; // fallback generic card icon
                            String codeLower = bank.getBankCode().toLowerCase();
                            if (codeLower.contains("vietcombank") || codeLower.equals("vcb")) {
                                logoRes = R.drawable.logo_vcb;
                            } else if (codeLower.contains("techcombank") || codeLower.equals("tcb")) {
                                logoRes = R.drawable.logo_tcb;
                            } else if (codeLower.contains("mb") || codeLower.contains("mbbank")) {
                                logoRes = R.drawable.logo_mb;
                            } else if (codeLower.contains("bidv")) {
                                logoRes = R.drawable.logo_bidv;
                            } else if (codeLower.contains("vietin") || codeLower.equals("ctg")) {
                                logoRes = R.drawable.logo_ctg;
                            }
                            imgLogo.setImageResource(logoRes);
                             
                             // Set click listener to change default bank
                             itemView.setOnClickListener(v -> {
                                 if (bank.isDefault()) {
                                     Toast.makeText(VeggoPayActivity.this, "Tài khoản này đang là mặc định", Toast.LENGTH_SHORT).show();
                                     return;
                                 }
                                 com.veggo.app.presentation.dialog.VeggoDialog.show(
                                         VeggoPayActivity.this,
                                         R.drawable.ic_card,
                                         "Đặt tài khoản mặc định",
                                         "Bạn có muốn chọn tài khoản " + bank.getBankCode() + " làm mặc định không?",
                                         "Đồng ý",
                                         "Hủy",
                                         new com.veggo.app.presentation.dialog.VeggoDialog.DialogListener() {
                                             @Override
                                             public void onConfirm() {
                                                 showProgress("Đang cập nhật tài khoản mặc định...");
                                                 walletRepository.setDefaultBank(customerId, bank.getBankCode(), bank.getAccountNumber(), new WalletRepository.ResultCallback<WalletDto>() {
                                                     @Override
                                                     public void onSuccess(WalletDto result) {
                                                         runOnUiThread(() -> {
                                                             hideProgress();
                                                             Toast.makeText(VeggoPayActivity.this, "Cập nhật tài khoản mặc định thành công!", Toast.LENGTH_SHORT).show();
                                                             loadWalletInfo();
                                                         });
                                                     }
                                                     @Override
                                                     public void onError(Throwable error) {
                                                         runOnUiThread(() -> {
                                                             hideProgress();
                                                             Toast.makeText(VeggoPayActivity.this, "Thao tác thất bại: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                                         });
                                                     }
                                                 });
                                             }
                                         }
                                 );
                             });
                             llLinkedBanks.addView(itemView);
                        }
                    } else {
                        tvLinkedBanksHeader.setVisibility(View.GONE);
                        if (nsvLinkedBanks != null) {
                            nsvLinkedBanks.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> Toast.makeText(VeggoPayActivity.this, "Lỗi tải thông tin ví", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showDepositDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_wallet_deposit);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText edtAmount = dialog.findViewById(R.id.edtAmount);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);

        // Funding source bank selection pre-select logic
        if (linkedBanksList == null || linkedBanksList.isEmpty()) {
            Toast.makeText(this, "Vui lòng liên kết tài khoản ngân hàng trước khi nạp tiền!", Toast.LENGTH_LONG).show();
            return;
        }

        final WalletDto.LinkedBankDto[] selectedBankArr = new WalletDto.LinkedBankDto[1];
        for (WalletDto.LinkedBankDto b : linkedBanksList) {
            if (b.isDefault()) {
                selectedBankArr[0] = b;
                break;
            }
        }
        if (selectedBankArr[0] == null) {
            selectedBankArr[0] = linkedBanksList.get(0);
        }

        View layoutSelectBank = dialog.findViewById(R.id.layoutSelectBank);
        ImageView imgSelectedBankLogo = dialog.findViewById(R.id.imgSelectedBankLogo);
        TextView tvSelectedBankInfo = dialog.findViewById(R.id.tvSelectedBankInfo);

        autoUpdateSelectedBankDisplay(selectedBankArr[0], imgSelectedBankLogo, tvSelectedBankInfo);

        layoutSelectBank.setOnClickListener(v -> {
            String[] bankOptions = new String[linkedBanksList.size()];
            for (int i = 0; i < linkedBanksList.size(); i++) {
                WalletDto.LinkedBankDto bankItem = linkedBanksList.get(i);
                bankOptions[i] = bankItem.getBankCode() + " - " + bankItem.getAccountNumber() + (bankItem.isDefault() ? " (Mặc định)" : "");
            }

            new android.app.AlertDialog.Builder(this)
                .setTitle("Chọn tài khoản ngân hàng nguồn")
                .setItems(bankOptions, (dialogInterface, index) -> {
                    selectedBankArr[0] = linkedBanksList.get(index);
                    autoUpdateSelectedBankDisplay(selectedBankArr[0], imgSelectedBankLogo, tvSelectedBankInfo);
                })
                .show();
        });

        // Dynamic VNĐ input formatting Watcher
        edtAmount.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    edtAmount.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[.,]", "");
                    if (!cleanString.isEmpty()) {
                        try {
                            double parsed = Double.parseDouble(cleanString);
                            java.text.DecimalFormat formatter = (java.text.DecimalFormat) java.text.NumberFormat.getInstance(java.util.Locale.US);
                            formatter.applyPattern("#,###");
                            String formatted = formatter.format(parsed).replace(",", ".");
                            current = formatted;
                            edtAmount.setText(formatted);
                            edtAmount.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    } else {
                        current = "";
                        edtAmount.setText("");
                    }

                    edtAmount.addTextChangedListener(this);
                }
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String rawAmount = edtAmount.getText().toString().trim().replaceAll("[.,]", "");
            if (rawAmount.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount;
            try {
                amount = Double.parseDouble(rawAmount);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            if (amount < 10000) {
                Toast.makeText(this, "Số tiền nạp tối thiểu là 10,000 ₫", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            
            // Require 6-digit wallet password for Deposit request
            showDepositPasswordConfirmDialog(amount, selectedBankArr[0].getBankCode());
        });

        dialog.show();
    }

    private void autoUpdateSelectedBankDisplay(WalletDto.LinkedBankDto bank, ImageView imgLogo, TextView tvInfo) {
        if (bank == null) return;
        tvInfo.setText(bank.getBankCode() + " - " + bank.getAccountNumber());
        int logoRes = R.drawable.ic_card;
        String codeLower = bank.getBankCode().toLowerCase();
        if (codeLower.contains("vietcombank") || codeLower.equals("vcb")) {
            logoRes = R.drawable.logo_vcb;
        } else if (codeLower.contains("techcombank") || codeLower.equals("tcb")) {
            logoRes = R.drawable.logo_tcb;
        } else if (codeLower.contains("mb") || codeLower.contains("mbbank")) {
            logoRes = R.drawable.logo_mb;
        } else if (codeLower.contains("bidv")) {
            logoRes = R.drawable.logo_bidv;
        } else if (codeLower.contains("vietin") || codeLower.equals("ctg")) {
            logoRes = R.drawable.logo_ctg;
        }
        imgLogo.setImageResource(logoRes);
    }

    private void showDepositPasswordConfirmDialog(final double amount, final String bankCode) {
        Dialog pinDialog = new Dialog(this);
        pinDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        pinDialog.setContentView(R.layout.dialog_veggopay_password_input);
        if (pinDialog.getWindow() != null) {
            pinDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            pinDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = pinDialog.findViewById(R.id.tvPinTitle);
        TextView tvDesc = pinDialog.findViewById(R.id.tvPinDescription);
        TextView btnCancel = pinDialog.findViewById(R.id.btnCancel);
        TextView btnConfirm = pinDialog.findViewById(R.id.btnConfirm);

        tvTitle.setText("Xác thực giao dịch");
        tvDesc.setText("Nhập mật khẩu ví 6 số để xác nhận nạp " + CurrencyFormatter.formatVnd((long) amount));

        EditText[] pinFields = new EditText[] {
                pinDialog.findViewById(R.id.edtPin1),
                pinDialog.findViewById(R.id.edtPin2),
                pinDialog.findViewById(R.id.edtPin3),
                pinDialog.findViewById(R.id.edtPin4),
                pinDialog.findViewById(R.id.edtPin5),
                pinDialog.findViewById(R.id.edtPin6)
        };

        setupPinAutoShift(pinFields, btnConfirm);

        btnCancel.setOnClickListener(v -> pinDialog.dismiss());
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

            pinDialog.dismiss();
            showProgress("Đang xử lý nạp tiền...");
            walletRepository.deposit(customerId, amount, bankCode, pinCode, new WalletRepository.ResultCallback<WalletDto>() {
                @Override
                public void onSuccess(WalletDto result) {
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(VeggoPayActivity.this, "Nạp tiền thành công!", Toast.LENGTH_SHORT).show();
                        loadWalletInfo();
                    });
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(VeggoPayActivity.this, "Nạp tiền thất bại: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        pinDialog.show();
    }

    private void showLinkBankDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_wallet_link_bank);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Spinner spinnerBank = dialog.findViewById(R.id.spinnerBank);
        EditText edtAccountNum = dialog.findViewById(R.id.edtAccountNumber);
        EditText edtHolder = dialog.findViewById(R.id.edtAccountHolder);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirm);

        // Bind custom Spinner Adapter with logo images
        String[] banks = {"Vietcombank", "Techcombank", "MB Bank", "BIDV", "VietinBank"};
        BankSpinnerAdapter bankAdapter = new BankSpinnerAdapter(this, banks);
        spinnerBank.setAdapter(bankAdapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String bankCode = spinnerBank.getSelectedItem().toString();
            String accountNum = edtAccountNum.getText().toString().trim();
            String holder = edtHolder.getText().toString().trim();

            if (accountNum.isEmpty() || holder.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            showProgress("Đang liên kết ngân hàng...");
            walletRepository.linkBank(customerId, bankCode, accountNum, holder, new WalletRepository.ResultCallback<WalletDto>() {
                @Override
                public void onSuccess(WalletDto result) {
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(VeggoPayActivity.this, "Liên kết thành công!", Toast.LENGTH_SHORT).show();
                        loadWalletInfo();
                    });
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(VeggoPayActivity.this, "Liên kết thất bại: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        dialog.show();
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
