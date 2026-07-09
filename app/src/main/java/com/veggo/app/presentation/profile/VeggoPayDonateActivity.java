package com.veggo.app.presentation.profile;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.veggo.app.R;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.data.repository.WalletRepository;
import com.veggo.app.di.AppModule;

import java.util.Map;

public class VeggoPayDonateActivity extends AppCompatActivity {

    private static final int WATER_CARBON_COST = 5;
    private ImageView imgTree, imgWaterCanAnim;
    private TextView tvStatusBubble, tvTreeName, tvWaterProgress, btnActivateSeed, btnWaterTree;
    private LinearLayout layoutActivate, layoutWater;
    private WalletRepository walletRepository;
    private String customerId;
    private RotateAnimation standardSwayAnim;
    private TextView tvTotalPlanted;
    private TextView tvCarbonPoints;
    private TextView tvStageProgress, tvTotalProgress;
    private LinearLayout layoutStageSegments, layoutTotalSegments;
    private android.widget.RelativeLayout rlGarden;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veggopay_donate);

        imgTree = findViewById(R.id.imgTree);
        imgWaterCanAnim = findViewById(R.id.imgWaterCanAnim);
        tvStatusBubble = findViewById(R.id.tvStatusBubble);
        tvTreeName = findViewById(R.id.tvTreeName);
        tvWaterProgress = findViewById(R.id.tvWaterProgress);
        btnActivateSeed = findViewById(R.id.btnActivateSeed);
        btnWaterTree = findViewById(R.id.btnWaterTree);
        layoutActivate = findViewById(R.id.layoutActivate);
        layoutWater = findViewById(R.id.layoutWater);
        tvTotalPlanted = findViewById(R.id.tvTotalPlanted);
        tvCarbonPoints = findViewById(R.id.tvCarbonPoints);
        tvStageProgress = findViewById(R.id.tvStageProgress);
        tvTotalProgress = findViewById(R.id.tvTotalProgress);
        layoutStageSegments = findViewById(R.id.layoutStageSegments);
        layoutTotalSegments = findViewById(R.id.layoutTotalSegments);
        rlGarden = findViewById(R.id.rlGarden);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        walletRepository = AppModule.provideWalletRepository();
        customerId = new AppPreferences(this).getCustomerId();

        // Standard gentle tree swaying animation (Pivot at the bottom center of the tree!)
        standardSwayAnim = new RotateAnimation(
                -4f, 4f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 1.0f
        );
        standardSwayAnim.setDuration(2000);
        standardSwayAnim.setRepeatCount(Animation.INFINITE);
        standardSwayAnim.setRepeatMode(Animation.REVERSE);
        imgTree.startAnimation(standardSwayAnim);

        // Fetch tree status on startup
        loadTreeStatus();

        btnActivateSeed.setOnClickListener(v -> activateSeedling());
        btnWaterTree.setOnClickListener(v -> performWatering());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTreeStatus();
    }

    private void loadTreeStatus() {
        walletRepository.getTreeStatus(customerId, new WalletRepository.ResultCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> treeData) {
                runOnUiThread(() -> updateTreeUi(treeData));
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> Toast.makeText(VeggoPayDonateActivity.this, "Không thể lấy trạng thái cây", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateTreeUi(Map<String, Object> treeData) {
        if (treeData == null) return;

        // Some APIs may wrap data into { tree: {...}, carbonPoint: n }
        if (treeData.containsKey("tree") && treeData.get("tree") instanceof Map) {
            Map<String, Object> inner = (Map<String, Object>) treeData.get("tree");
            // Merge carbonPoint into inner if present on wrapper
            if (treeData.containsKey("carbonPoint") && !inner.containsKey("carbonPoint")) {
                inner.put("carbonPoint", treeData.get("carbonPoint"));
            }
            treeData = inner;
        }

        String status = (String) treeData.get("status");
        Double waterCountObj = (Double) treeData.get("waterCount");
        int waterCount = waterCountObj != null ? waterCountObj.intValue() : 0;
        String name = (String) treeData.get("treeName");

        if (name != null) {
            tvTreeName.setText(name);
        }

        int plantedCount = 0;
        if (treeData.containsKey("plantedCount")) {
            Double plantedCountObj = (Double) treeData.get("plantedCount");
            plantedCount = plantedCountObj != null ? plantedCountObj.intValue() : 0;
        }
        tvTotalPlanted.setText("Đã trồng: " + plantedCount + " cây");
        populateMiniForest(plantedCount);

        if (tvCarbonPoints != null) {
            int carbonPoint = 0;
            Object carbonRaw = treeData.get("carbonPoint");
            if (carbonRaw instanceof Double) {
                carbonPoint = ((Double) carbonRaw).intValue();
            } else if (carbonRaw instanceof Integer) {
                carbonPoint = (Integer) carbonRaw;
            } else if (carbonRaw instanceof Long) {
                carbonPoint = ((Long) carbonRaw).intValue();
            }
            tvCarbonPoints.setText("Carbon: " + carbonPoint + " điểm");
        }

        if ("none".equals(status) || "mature".equals(status) || waterCount >= 24) {
            layoutActivate.setVisibility(View.VISIBLE);
            layoutWater.setVisibility(View.GONE);
            if ("mature".equals(status) || waterCount >= 24) {
                imgTree.setImageResource(R.drawable.ic_tree_stage_5);
                tvStatusBubble.setText("Cây của bạn đã trưởng thành hoàn toàn! Hãy trồng thêm một cây mới nhé!");
                btnActivateSeed.setText("Kích hoạt cây mới (10.000đ)");
            } else {
                imgTree.setImageResource(R.drawable.ic_tree_stage_1);
                tvStatusBubble.setText("Xin chào! Hãy kích hoạt hạt giống để gieo mầm xanh");
                btnActivateSeed.setText("Kích hoạt hạt giống (10.000đ)");
            }
        } else {
            layoutActivate.setVisibility(View.GONE);
            layoutWater.setVisibility(View.VISIBLE);
            tvWaterProgress.setText("Tiến trình: " + waterCount + "/24 lần tưới");

            int currentStageWater = waterCount % 6;
            tvStageProgress.setText("Tiến trình cấp hiện tại (" + currentStageWater + "/6 lần tưới):");
            tvTotalProgress.setText("Tiến trình tổng (" + waterCount + "/24 lần tưới):");

            updateSegments(layoutStageSegments, currentStageWater, 6);
            updateSegments(layoutTotalSegments, waterCount, 24);

            if (waterCount >= 0 && waterCount <= 5) {
                imgTree.setImageResource(R.drawable.ic_tree_stage_1);
                tvStatusBubble.setText("Hạt giống đang nảy mầm! Hãy tưới thêm nước nhé (" + WATER_CARBON_COST + " điểm Carbon/lần)");
            } else if (waterCount >= 6 && waterCount <= 11) {
                imgTree.setImageResource(R.drawable.ic_tree_stage_2);
                tvStatusBubble.setText("Cây con đang vươn lên đón nắng! (" + WATER_CARBON_COST + " điểm Carbon/lần)");
            } else if (waterCount >= 12 && waterCount <= 17) {
                imgTree.setImageResource(R.drawable.ic_tree_stage_3);
                tvStatusBubble.setText("Cây của bạn đang phát triển rất tốt! (" + WATER_CARBON_COST + " điểm Carbon/lần)");
            } else if (waterCount >= 18 && waterCount <= 23) {
                imgTree.setImageResource(R.drawable.ic_tree_stage_4);
                tvStatusBubble.setText("Cây sắp trưởng thành rồi đấy! (" + WATER_CARBON_COST + " điểm Carbon/lần)");
            } else {
                imgTree.setImageResource(R.drawable.ic_tree_stage_5);
                tvStatusBubble.setText("Chúc mừng cây đã trưởng thành cổ thụ! (" + WATER_CARBON_COST + " điểm Carbon/lần)");
            }
        }
    }

    private void activateSeedling() {
        walletRepository.activateSeed(customerId, new WalletRepository.ResultCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                runOnUiThread(() -> {
                    Toast.makeText(VeggoPayDonateActivity.this, "Đã kích hoạt hạt giống thành công! -10.000 ₫", Toast.LENGTH_LONG).show();
                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                    if (data != null) {
                        Map<String, Object> treeData = (Map<String, Object>) data.get("tree");
                        updateTreeUi(treeData);
                    }

                    // Happy swaying animation for 3 seconds
                    triggerHappySway();
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> Toast.makeText(VeggoPayDonateActivity.this, error.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void performWatering() {
        // Show watering can simulation
        imgWaterCanAnim.setVisibility(View.VISIBLE);
        RotateAnimation waterCanTilt = new RotateAnimation(0f, -30f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        waterCanTilt.setDuration(1000);
        imgWaterCanAnim.startAnimation(waterCanTilt);

        imgWaterCanAnim.postDelayed(() -> {
            imgWaterCanAnim.setVisibility(View.GONE);
            walletRepository.waterTree(customerId, new WalletRepository.ResultCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> treeData) {
                    runOnUiThread(() -> {
                        updateTreeUi(treeData);
                        triggerHappySway();
                        Toast.makeText(VeggoPayDonateActivity.this, "Cây được tiếp thêm nước ngọt lành!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Throwable error) {
                    runOnUiThread(() -> Toast.makeText(VeggoPayDonateActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        }, 1000);
    }

    private void triggerHappySway() {
        RotateAnimation happySway = new RotateAnimation(
                -12f, 12f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 1.0f
        );
        happySway.setDuration(400);
        happySway.setRepeatCount(6);
        happySway.setRepeatMode(Animation.REVERSE);
        imgTree.startAnimation(happySway);

        // Resume standard gentle sway after happy animation finishes
        imgTree.postDelayed(() -> imgTree.startAnimation(standardSwayAnim), 2400);
    }

    private void updateSegments(LinearLayout layout, int current, int max) {
        if (layout == null) return;
        layout.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < max; i++) {
            View segment = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (8 * density), 1.0f);
            if (i > 0) {
                lp.leftMargin = (int) (2 * density);
            }
            segment.setLayoutParams(lp);

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setCornerRadius(4 * density);
            if (i < current) {
                gd.setColor(android.graphics.Color.parseColor("#388E3C")); // active green
            } else {
                gd.setColor(android.graphics.Color.parseColor("#E0E0E0")); // inactive gray
            }
            segment.setBackground(gd);
            layout.addView(segment);
        }
    }

    private void populateMiniForest(int count) {
        if (rlGarden == null) return;
        
        // Clear previously added mini-trees
        for (int i = rlGarden.getChildCount() - 1; i >= 0; i--) {
            View child = rlGarden.getChildAt(i);
            if (child.getTag() != null && child.getTag().equals("mini_tree")) {
                rlGarden.removeViewAt(i);
            }
        }
    }
}
