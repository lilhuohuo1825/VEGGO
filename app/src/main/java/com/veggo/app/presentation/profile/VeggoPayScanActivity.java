package com.veggo.app.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.R;

public class VeggoPayScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veggopay_scan);

        ImageView btnBack = findViewById(R.id.btnBack);
        View laserBeam = findViewById(R.id.laserBeam);
        View btnFlash = findViewById(R.id.btnFlash);
        View btnGallery = findViewById(R.id.btnGallery);

        btnBack.setOnClickListener(v -> finish());

        // Laser beam animation to make the camera feel premium and alive
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f
        );
        animation.setDuration(3000);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        laserBeam.startAnimation(animation);

        btnFlash.setOnClickListener(v -> Toast.makeText(this, "Bật/tắt đèn Flash camera", Toast.LENGTH_SHORT).show());

        // Simulate choosing a QR code image from gallery
        btnGallery.setOnClickListener(v -> {
            Toast.makeText(this, "Đã đọc mã QR chuyển tiền VeggoPay thành công!", Toast.LENGTH_SHORT).show();
            
            // Navigate directly to transfer activity with pre-filled details
            Intent intent = new Intent(this, VeggoPayTransferActivity.class);
            intent.putExtra("recipientPhone", "0987654321");
            intent.putExtra("amount", "50000");
            intent.putExtra("description", "Thanh toán hóa đơn qua mã QR");
            startActivity(intent);
            finish();
        });
    }
}
