package com.veggo.app.presentation.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.veggo.app.R;
import com.veggo.app.core.preferences.PreferencesManager;
import com.veggo.app.domain.model.OnboardingItem;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MaterialButton btnNext;
    private TextView tvSkip;
    private OnboardingPagerAdapter adapter;
    private PreferencesManager preferencesManager;
    private View[] indicators;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        preferencesManager = new PreferencesManager(this);
        if (preferencesManager.isOnboardingCompleted()) {
            goToHome();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        tvSkip = findViewById(R.id.tvSkip);

        // Khởi tạo các indicator
        indicators = new View[]{
                findViewById(R.id.indicator1),
                findViewById(R.id.indicator2),
                findViewById(R.id.indicator3),
                findViewById(R.id.indicator4)
        };

        List<OnboardingItem> onboardingItems = new ArrayList<>();
        onboardingItems.add(new OnboardingItem(R.drawable.onboarding_1, "Rau củ tươi giao tận nhà", "Tươi ngon mỗi ngày, giao nhanh tận nơi để mỗi bữa ăn luôn trọn vị xanh"));
        onboardingItems.add(new OnboardingItem(R.drawable.onboarding_2, "Cá nhân hóa bằng AI", "Thiết lập khẩu vị, dị ứng và sở thích ăn uống để nhận gợi ý thực đơn phù hợp mỗi ngày"));
        onboardingItems.add(new OnboardingItem(R.drawable.onboarding_3, "Tủ lạnh thông minh", "Theo dõi hạn sử dụng nguyên liệu và khám phá món ăn phù hợp từ những gì bạn đang có"));
        onboardingItems.add(new OnboardingItem(R.drawable.onboarding_4, "Cộng đồng sống xanh", "Chia sẻ công thức, tích điểm xanh và lan tỏa lối sống lành mạnh mỗi ngày"));

        adapter = new OnboardingPagerAdapter(onboardingItems);
        viewPager.setAdapter(adapter);

        btnNext.setOnClickListener(v -> {
            int next = viewPager.getCurrentItem() + 1;
            if (next < adapter.getItemCount()) {
                viewPager.setCurrentItem(next);
            } else {
                goToHome();
            }
        });

        tvSkip.setOnClickListener(v -> goToHome());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                
                if (position == adapter.getItemCount() - 1) {
                    btnNext.setText(R.string.onboarding_start);
                } else {
                    btnNext.setText(R.string.onboarding_next);
                }
            }
        });
        
        // Cập nhật trạng thái ban đầu
        updateIndicators(0);
    }

    private void updateIndicators(int position) {
        for (int i = 0; i < indicators.length; i++) {
            if (indicators[i] == null) continue;
            
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) indicators[i].getLayoutParams();
            if (i == position) {
                indicators[i].setBackgroundResource(R.drawable.bg_indicator_selected);
                params.width = (int) (18 * getResources().getDisplayMetrics().density);
            } else {
                indicators[i].setBackgroundResource(R.drawable.bg_indicator_unselected);
                params.width = (int) (6 * getResources().getDisplayMetrics().density);
            }
            indicators[i].setLayoutParams(params);
        }
    }

    private void goToHome() {
        preferencesManager.setOnboardingCompleted(true);
        Intent intent = new Intent(OnboardingActivity.this, com.veggo.app.MainActivity.class);
        startActivity(intent);
        finish();
    }
}
