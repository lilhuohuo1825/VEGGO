package com.veggo.app.presentation.profile;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.data.remote.dto.WalletDto;
import com.veggo.app.data.repository.WalletRepository;
import com.veggo.app.di.AppModule;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class VeggoPayTransferActivity extends AppCompatActivity {

    private EditText edtRecipientPhone, edtAmount, edtNote;
    private ScrollView scrollFormView;
    private View layoutFormContainer;
    private LinearLayout layoutReceiptView;
    private TextView tvSourceBalance, tvRecipientStatus;
    private TextView tvReceiptAmount, tvReceiptRecipient, tvReceiptNote, tvReceiptTxId;
    
    private WalletRepository walletRepository;
    private String senderCustomerId;
    private String verifiedRecipientCustomerId = "";
    private String verifiedRecipientName = "";
    
    // OTP State Variables
    private String currentGeneratedOtp = "";
    private int incorrectOtpAttempts = 0;
    private CountDownTimer otpTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veggopay_transfer);

        edtRecipientPhone = findViewById(R.id.edtRecipientPhone);
        edtAmount = findViewById(R.id.edtAmount);
        edtNote = findViewById(R.id.edtNote);
        scrollFormView = findViewById(R.id.scrollFormView);
        layoutFormContainer = findViewById(R.id.layoutFormContainer);
        layoutReceiptView = findViewById(R.id.layoutReceiptView);
        
        tvSourceBalance = findViewById(R.id.tvSourceBalance);
        tvRecipientStatus = findViewById(R.id.tvRecipientStatus);
        
        tvReceiptAmount = findViewById(R.id.tvReceiptAmount);
        tvReceiptRecipient = findViewById(R.id.tvReceiptRecipient);
        tvReceiptNote = findViewById(R.id.tvReceiptNote);
        tvReceiptTxId = findViewById(R.id.tvReceiptTxId);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.btnReceiptDone).setOnClickListener(v -> finish());

        walletRepository = AppModule.provideWalletRepository();
        senderCustomerId = new AppPreferences(this).getCustomerId();

        // Check verification on focus change
        edtRecipientPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                verifyRecipient();
            }
        });

        // Handle auto-format for amount input
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
                    String cleanString = s.toString().replaceAll("[.,\\s]", "");
                    if (!cleanString.isEmpty()) {
                        try {
                            double parsed = Double.parseDouble(cleanString);
                            String formatted = NumberFormat.getNumberInstance(Locale.GERMANY).format(parsed);
                            current = formatted;
                            edtAmount.setText(formatted);
                            edtAmount.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            // Ignored
                        }
                    } else {
                        current = "";
                        edtAmount.setText("");
                    }
                    edtAmount.addTextChangedListener(this);
                }
            }
        });

        // Prepopulate from intents (e.g. Scan QR)
        if (getIntent() != null) {
            String fillPhone = getIntent().getStringExtra("recipientPhone");
            String fillAmount = getIntent().getStringExtra("amount");
            String fillDesc = getIntent().getStringExtra("description");

            if (fillPhone != null) {
                edtRecipientPhone.setText(fillPhone);
                verifyRecipient();
            }
            if (fillAmount != null) {
                try {
                    double parsed = Double.parseDouble(fillAmount);
                    edtAmount.setText(NumberFormat.getNumberInstance(Locale.GERMANY).format(parsed));
                } catch (Exception ignored) {}
            }
            if (fillDesc != null) edtNote.setText(fillDesc);
        }

        findViewById(R.id.btnSubmitTransfer).setOnClickListener(v -> attemptTransfer());

        loadSourceBalance();
    }

    private void loadSourceBalance() {
        if (TextUtils.isEmpty(senderCustomerId)) return;
        walletRepository.getWalletInfo(senderCustomerId, new WalletRepository.ResultCallback<WalletDto>() {
            @Override
            public void onSuccess(WalletDto wallet) {
                runOnUiThread(() -> tvSourceBalance.setText(CurrencyFormatter.formatVnd((long) wallet.getBalance())));
            }

            @Override
            public void onError(Throwable error) {
                // Fallback
            }
        });
    }

    private void verifyRecipient() {
        String phone = edtRecipientPhone.getText().toString().trim();
        if (phone.isEmpty() || phone.length() < 9) {
            return;
        }

        walletRepository.findRecipient(phone, new WalletRepository.ResultCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                runOnUiThread(() -> {
                    verifiedRecipientCustomerId = (String) result.get("customerId");
                    verifiedRecipientName = (String) result.get("name");
                    tvRecipientStatus.setText("Người thụ hưởng: " + verifiedRecipientName);
                    tvRecipientStatus.setTextColor(Color.parseColor("#388E3C")); // green
                    tvRecipientStatus.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> {
                    verifiedRecipientCustomerId = "";
                    verifiedRecipientName = "";
                    tvRecipientStatus.setText("Lỗi: " + error.getMessage());
                    tvRecipientStatus.setTextColor(Color.parseColor("#D32F2F")); // red
                    tvRecipientStatus.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void attemptTransfer() {
        String phone = edtRecipientPhone.getText().toString().trim();
        String amountText = edtAmount.getText().toString().trim().replaceAll("[.,\\s]", "");
        String note = edtNote.getText().toString().trim();

        if (phone.isEmpty() || phone.length() < 9) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại người nhận hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amountText.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền cần chuyển", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountText);
        if (amount <= 0) {
            Toast.makeText(this, "Số tiền phải lớn hơn 0 ₫", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(verifiedRecipientCustomerId)) {
            Toast.makeText(this, "Vui lòng kiểm tra lại thông tin người thụ hưởng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate and send simulated OTP
        generateAndSendOtp();
        
        // Show OTP dialog
        showOtpVerificationDialog(phone, amount, note);
    }

    private void generateAndSendOtp() {
        Random random = new Random();
        currentGeneratedOtp = String.format("%06d", random.nextInt(900000) + 100000);
        incorrectOtpAttempts = 0;
        sendOtpSystemNotification(currentGeneratedOtp);
    }

    private void sendOtpSystemNotification(String otp) {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "veggopay_otp_channel";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "VeggoPay OTP", android.app.NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle("Mã OTP VeggoPay")
                .setContentText("Mã OTP chuyển tiền của bạn là: " + otp + ". Hiệu lực trong 60 giây.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(2002, builder.build());
    }

    private void showOtpVerificationDialog(String phone, double amount, String note) {
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

        tvTitle.setText("Xác thực mã OTP");
        tvDesc.setText("Nhập mã OTP gồm 6 chữ số đã được gửi qua thông báo hệ thống.");

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

        // Add countdown timer text dynamically below the pin fields
        LinearLayout rootContainer = dialog.findViewById(R.id.layoutPinContainer);
        TextView tvTimer = new TextView(this);
        tvTimer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvTimer.setGravity(android.view.Gravity.CENTER);
        tvTimer.setPadding(0, 16, 0, 8);
        tvTimer.setTextColor(Color.parseColor("#757575"));
        tvTimer.setTextSize(13);
        tvTimer.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        
        // Link to resend code
        TextView tvResend = new TextView(this);
        tvResend.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvResend.setGravity(android.view.Gravity.CENTER);
        tvResend.setPadding(0, 8, 0, 16);
        tvResend.setTextColor(Color.parseColor("#388E3C"));
        tvResend.setText("Gửi lại mã");
        tvResend.setTextSize(13);
        tvResend.setPaintFlags(tvResend.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        tvResend.setClickable(true);

        rootContainer.addView(tvTimer);
        rootContainer.addView(tvResend);

        // Resend callback
        tvResend.setOnClickListener(v -> {
            generateAndSendOtp();
            Toast.makeText(this, "Đã gửi lại mã OTP mới", Toast.LENGTH_SHORT).show();
            startCountdownTimer(tvTimer, dialog);
        });

        startCountdownTimer(tvTimer, dialog);

        btnCancel.setOnClickListener(v -> {
            if (otpTimer != null) otpTimer.cancel();
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            StringBuilder otpBuilder = new StringBuilder();
            for (EditText et : pinFields) {
                otpBuilder.append(et.getText().toString().trim());
            }

            String enteredOtp = otpBuilder.toString();
            if (enteredOtp.length() < 6) {
                Toast.makeText(this, "Vui lòng nhập đủ 6 số OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!enteredOtp.equals(currentGeneratedOtp)) {
                incorrectOtpAttempts++;
                Toast.makeText(this, "Mã OTP không chính xác. Còn lại " + (3 - incorrectOtpAttempts) + " lần thử.", Toast.LENGTH_SHORT).show();
                if (incorrectOtpAttempts >= 3) {
                    if (otpTimer != null) otpTimer.cancel();
                    dialog.dismiss();
                    Toast.makeText(this, "Bạn đã nhập sai quá 3 lần. Giao dịch bị hủy.", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // OTP verified successfully
            if (otpTimer != null) otpTimer.cancel();
            dialog.dismiss();
            
            // Perform money transfer using dummy OTP_VERIFIED flag
            performApiTransfer(phone, amount, note);
        });

        dialog.show();
    }

    private void startCountdownTimer(TextView tvTimer, Dialog dialog) {
        if (otpTimer != null) {
            otpTimer.cancel();
        }

        otpTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Hiệu lực còn lại: " + (millisUntilFinished / 1000) + " giây");
            }

            @Override
            public void onFinish() {
                tvTimer.setText("Mã OTP đã hết hiệu lực");
                Toast.makeText(VeggoPayTransferActivity.this, "Mã OTP hết hiệu lực. Vui lòng bấm gửi lại mã.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void performApiTransfer(String phone, double amount, String note) {
        walletRepository.transferMoney(senderCustomerId, phone, amount, note, "OTP_VERIFIED", new WalletRepository.ResultCallback<WalletDto>() {
            @Override
            public void onSuccess(WalletDto result) {
                runOnUiThread(() -> {
                    // Update Receipt views
                    tvReceiptAmount.setText(CurrencyFormatter.formatVnd((long) amount));
                    tvReceiptRecipient.setText(verifiedRecipientName + " (" + phone + ")");
                    tvReceiptNote.setText(note.isEmpty() ? "Chuyển tiền VeggoPay" : note);
                    
                    // Generate mock transaction id
                    long txRandom = (long) (Math.random() * 90000000L) + 10000000L;
                    tvReceiptTxId.setText("VP" + txRandom);

                    // Show receipt, hide input form
                    layoutFormContainer.setVisibility(View.GONE);
                    layoutReceiptView.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> Toast.makeText(VeggoPayTransferActivity.this, error.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void setupPinAutoShift(EditText[] editTexts, final View btnConfirm) {
        for (int i = 0; i < editTexts.length; i++) {
            final int index = i;
            editTexts[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
                        if (index < editTexts.length - 1) {
                            editTexts[index + 1].requestFocus();
                        } else if (btnConfirm != null) {
                            btnConfirm.performClick();
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            editTexts[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (editTexts[index].getText().length() == 0 && index > 0) {
                        editTexts[index - 1].requestFocus();
                        editTexts[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }
    
    @Override
    protected void onDestroy() {
        if (otpTimer != null) {
            otpTimer.cancel();
        }
        super.onDestroy();
    }
}
