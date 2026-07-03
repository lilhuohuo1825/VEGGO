package com.veggo.app.presentation.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.data.remote.api.ForecastApi;
import com.veggo.app.data.remote.dto.ForecastResponseDto;
import com.veggo.app.databinding.DialogPriceAlertBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PriceAlertHelper {

    private static final String PREFS_NAME = "veggo_price_alerts";
    private static final String LAST_ALERT_DATE_PREFIX = "last_alert_";
    private static final String GLOBAL_LAST_ALERT_DATE = "global_last_alert_date";

    public interface OnAlertActionListener {
        void onActionClick(String productId);
    }

    /**
     * Kiểm tra và hiển thị Dialog cảnh báo nếu sản phẩm có biến động giá lớn (>= 10% hoặc <= -10%)
     */
    public static void checkAndShowPriceAlert(
            Context context,
            String productId,
            String productName,
            String productImage,
            double productPrice,
            OnAlertActionListener actionListener
    ) {
        if (isGlobalAlertAlreadyShownToday(context) || isAlertAlreadyShownToday(context, productId)) {
            android.util.Log.d("VEGGO_ALERT", "Bỏ qua cảnh báo " + productName + " do đã hiển thị hôm nay.");
            return;
        }

        android.util.Log.d("VEGGO_ALERT", "Đang tải dự báo cho: " + productName + " (ID: " + productId + ")");
        ForecastApi forecastApi = ApiClient.createService(ForecastApi.class);
        forecastApi.getProductForecast(productId).enqueue(new Callback<ForecastResponseDto>() {
            @Override
            public void onResponse(Call<ForecastResponseDto> call, Response<ForecastResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ForecastResponseDto.ForecastData forecast = response.body().getData();
                    
                    if (forecast != null) {
                        double change = forecast.getChangePercent();
                        android.util.Log.d("VEGGO_ALERT", "Dữ liệu dự báo của " + productName + ": biến động = " + change + "%");
                        
                        if (change >= 10.0 || change <= -10.0) {
                            android.util.Log.d("VEGGO_ALERT", "Kích hoạt hiển thị Dialog cảnh báo cho: " + productName);
                            showAlertDialog(context, productId, productName, productImage, productPrice, forecast, actionListener);
                            markAlertAsShownToday(context, productId);
                            markGlobalAlertAsShownToday(context);
                        } else {
                            android.util.Log.d("VEGGO_ALERT", "Không đủ điều kiện hiện Dialog (biến động " + change + "% < 10%)");
                        }
                    } else {
                        android.util.Log.d("VEGGO_ALERT", "Dữ liệu forecast trống (null)");
                    }
                } else {
                    android.util.Log.d("VEGGO_ALERT", "API trả về mã lỗi hoặc body null: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ForecastResponseDto> call, Throwable t) {
                android.util.Log.e("VEGGO_ALERT", "Lỗi kết nối API dự báo cho " + productName + ": " + t.getMessage());
            }
        });
    }

    private static boolean isGlobalAlertAlreadyShownToday(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastShown = prefs.getString(GLOBAL_LAST_ALERT_DATE, "");
        return today.equals(lastShown);
    }

    private static void markGlobalAlertAsShownToday(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        prefs.edit().putString(GLOBAL_LAST_ALERT_DATE, today).apply();
    }

    private static void showAlertDialog(
            Context context,
            String productId,
            String productName,
            String productImage,
            double productPrice,
            ForecastResponseDto.ForecastData forecast,
            OnAlertActionListener actionListener
    ) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        DialogPriceAlertBinding binding = DialogPriceAlertBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());

        // Khi người dùng tắt dialog bằng bất kỳ cách nào (bấm ngoài, nút Để sau, Nhắc tôi, Mua ngay)
        // sẽ ghi nhận đã hiển thị trong ngày để tránh làm phiền (spam)
        dialog.setOnDismissListener(dialogInterface -> {
            markAlertAsShownToday(context, productId);
            markGlobalAlertAsShownToday(context);
            android.util.Log.d("VEGGO_ALERT", "Đã đánh dấu ẩn spam cho sản phẩm ID: " + productId);
        });

        // Gán dữ liệu sản phẩm
        binding.txtProductNameAlert.setText(productName);
        binding.txtProductPriceAlert.setText(String.format(Locale.getDefault(), "Giá hiện tại: %,.0f đ", productPrice));
        binding.txtAlertReason.setText(android.text.Html.fromHtml(forecast.getReason(), android.text.Html.FROM_HTML_MODE_LEGACY));

        // Tải ảnh sản phẩm bằng Glide
        if (productImage != null && !productImage.isEmpty()) {
            Glide.with(context)
                    .load(productImage)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.imgProductAlert);
        }

        // Thiết lập giao diện theo xu hướng TĂNG / GIẢM giá
        if ("down".equals(forecast.getTrend())) {
            // Giảm giá (Tin vui - Màu vàng nhạt, icon ngọn lửa bập bùng)
            binding.txtAlertTitle.setText("Cơ hội mua hời!");
            binding.imgAlertIcon.setImageResource(R.drawable.ic_fire);
            binding.imgAlertIcon.setImageTintList(null); // Sử dụng màu nguyên bản của icon ngọn lửa PNG
            
            // Toàn bộ nền của Dialog (Root) đổi thành vàng nhạt (#FFF9E6) và viền vàng đậm (#FBC02D)
            binding.getRoot().setCardBackgroundColor(ColorStateList.valueOf(android.graphics.Color.parseColor("#FFF9E6")));
            binding.getRoot().setStrokeColor(ColorStateList.valueOf(android.graphics.Color.parseColor("#FBC02D")));
            
            // Nền của section content bên trong giữ màu trắng sạch sẽ để hiển thị chữ rõ nét
            binding.cardReasonContainer.setCardBackgroundColor(ColorStateList.valueOf(android.graphics.Color.parseColor("#FFFFFF")));
            binding.cardReasonContainer.setStrokeColor(ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0")));
            
            // Tạo animation bập bùng (flicker) cho ngọn lửa
            android.view.animation.AnimationSet animationSet = new android.view.animation.AnimationSet(true);
            
            android.view.animation.AlphaAnimation alphaAnim = new android.view.animation.AlphaAnimation(0.6f, 1.0f);
            alphaAnim.setDuration(350);
            alphaAnim.setRepeatMode(android.view.animation.Animation.REVERSE);
            alphaAnim.setRepeatCount(android.view.animation.Animation.INFINITE);
            
            android.view.animation.ScaleAnimation scaleAnim = new android.view.animation.ScaleAnimation(
                    0.88f, 1.12f,
                    0.88f, 1.12f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
            );
            scaleAnim.setDuration(350);
            scaleAnim.setRepeatMode(android.view.animation.Animation.REVERSE);
            scaleAnim.setRepeatCount(android.view.animation.Animation.INFINITE);
            
            animationSet.addAnimation(alphaAnim);
            animationSet.addAnimation(scaleAnim);
            binding.imgAlertIcon.startAnimation(animationSet);
            
            binding.btnAlertAction.setText("Nhắc tôi sau");
            binding.btnAlertAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_main)));
        } else {
            // Tăng giá (Cảnh báo - Màu đỏ)
            binding.txtAlertTitle.setText("Cảnh báo tăng giá!");
            binding.imgAlertIcon.setImageResource(android.R.drawable.stat_sys_warning);
            binding.imgAlertIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.veggo_danger)));
            
            binding.cardReasonContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.veggo_danger_soft));
            binding.cardReasonContainer.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.veggo_danger)));
            
            binding.btnAlertAction.setText("Mua ngay");
            binding.btnAlertAction.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.veggo_green)));
        }

        // Nút Hủy (Để sau)
        binding.btnAlertDismiss.setText("Để sau");
        binding.btnAlertDismiss.setOnClickListener(v -> dialog.dismiss());

        // Nút hành động
        binding.btnAlertAction.setOnClickListener(v -> {
            dialog.dismiss();
            if ("down".equals(forecast.getTrend())) {
                // Trích xuất số ngày giảm giá từ nội dung text lý do
                int days = 2; // Giá trị mặc định
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+) ngày");
                java.util.regex.Matcher matcher = pattern.matcher(forecast.getReason());
                if (matcher.find()) {
                    try {
                        days = Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException ignored) {}
                }

                // Lưu lời nhắc vào SharedPreferences (Local)
                SharedPreferences prefs = context.getSharedPreferences("price_alert_reminders", Context.MODE_PRIVATE);
                String value = productName + "|" + Math.abs(Math.round(forecast.getChangePercent())) + "|" + days + "|" + System.currentTimeMillis();
                prefs.edit().putString("reminder_" + productId, value).apply();

                android.widget.Toast.makeText(context, "Đã tạo nhắc nhở cho " + productName + " sau " + days + " ngày!", android.widget.Toast.LENGTH_SHORT).show();
            } else {
                // Mua ngay -> Gọi callback thực hiện Thanh toán
                if (actionListener != null) {
                    actionListener.onActionClick(productId);
                }
            }
        });

        // Thiết lập kích thước dialog: giới hạn 85% chiều rộng màn hình để không sát lề
        if (dialog.getWindow() != null) {
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int width = (int) (screenWidth * 0.85); // 85% của chiều rộng màn hình
            dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        
        dialog.show();
    }

    private static boolean isAlertAlreadyShownToday(Context context, String productId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastShown = prefs.getString(LAST_ALERT_DATE_PREFIX + productId, "");
        return today.equals(lastShown);
    }

    private static void markAlertAsShownToday(Context context, String productId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        prefs.edit().putString(LAST_ALERT_DATE_PREFIX + productId, today).apply();
    }
}
