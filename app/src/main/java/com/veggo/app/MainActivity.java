package com.veggo.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.veggo.app.core.ui.BottomNavController;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.databinding.ActivityMainBinding;
import com.veggo.app.databinding.ComponentBottomNavBinding;
import com.veggo.app.presentation.cart.CartFragment;
import com.veggo.app.presentation.community.CommunityHomeActivity;
import com.veggo.app.presentation.home.HomeFragment;
import com.veggo.app.presentation.order.OrderHistoryFragment;
import com.veggo.app.presentation.profile.ProfileFragment;
import com.veggo.app.presentation.profile.AddFridgeIngredientActivity;
import com.veggo.app.presentation.profile.PostNotificationsActivity;
import com.veggo.app.presentation.profile.ProfileLoggedInFragment;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_NAV_ITEM = "extra_selected_nav_item";

    private ActivityMainBinding binding;
    private ComponentBottomNavBinding bottomNavBinding;
    private Tab currentTab;
    private android.net.Uri cameraImageUri = null;
    private boolean isScanReceiptMode = true;

    private final androidx.activity.result.ActivityResultLauncher<String> notificationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Đã xử lý kết quả xin quyền thông báo, không cần làm gì thêm
            });

    private final androidx.activity.result.ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                    Intent intent = new Intent(this, AddFridgeIngredientActivity.class);
                    if (isScanReceiptMode) {
                        intent.putExtra("EXTRA_AI_IMAGE_URI", cameraImageUri.toString());
                    } else {
                        intent.putExtra("EXTRA_AI_INGREDIENT_URI", cameraImageUri.toString());
                    }
                    startActivity(intent);
                }
            }
    );

    private final androidx.activity.result.ActivityResultLauncher<String[]> cameraPermLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
            perms -> {
                if (Boolean.TRUE.equals(perms.get(android.Manifest.permission.CAMERA))) {
                    launchCamera();
                } else {
                    android.widget.Toast.makeText(this, "Cần cấp quyền camera để chụp ảnh", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bottomNavBinding = binding.bottomNavHost;
        BottomNavController.applySystemBarColors(this);

        bottomNavBinding.navHomeButton.setOnClickListener(view -> openTab(Tab.HOME));
        bottomNavBinding.navCommunityButton.setOnClickListener(view -> openCommunityScreen());
        bottomNavBinding.navOrdersButton.setOnClickListener(view -> openTab(Tab.ORDERS));
        bottomNavBinding.navAccountButton.setOnClickListener(view -> openTab(Tab.ACCOUNT));
        bottomNavBinding.bottomNavCard.setOnClickListener(view -> {
        });
        bottomNavBinding.navScanButton.setOnClickListener(view -> {
            openScanScreen();
        });

        // Seed database from JSON assets if needed
        com.veggo.app.core.database.AssetDatabaseSeeder.seedIfNeeded(this);

        // Xin quyền POST_NOTIFICATIONS trên Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            if (intent.hasExtra(EXTRA_CATEGORY_ID)) {
                openCategoryDetail(intent.getStringExtra(EXTRA_CATEGORY_ID), intent.getStringExtra(EXTRA_SUBCATEGORY_ID));
            } else {
                boolean fromExternal = intent.getBooleanExtra("from_external", false);
                openTab(tabFromNavItem(intent.getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_home)), fromExternal);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasExtra(EXTRA_CATEGORY_ID)) {
            openCategoryDetail(intent.getStringExtra(EXTRA_CATEGORY_ID), intent.getStringExtra(EXTRA_SUBCATEGORY_ID));
        } else {
            boolean fromExternal = intent.getBooleanExtra("from_external", false);
            openTab(tabFromNavItem(intent.getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_home)), fromExternal);
        }
    }

    private Tab tabFromNavItem(int itemId) {
        if (itemId == R.id.nav_home) {
            return Tab.HOME;
        }
        if (itemId == R.id.nav_profile) {
            return Tab.ACCOUNT;
        }
        if (itemId == R.id.nav_orders) {
            return Tab.ORDERS;
        }
        if (itemId == R.id.nav_cart) {
            return Tab.CART;
        }
        if (itemId == R.id.nav_category) {
            return Tab.COMMUNITY;
        }
        return Tab.HOME;
    }

    private void openTab(Tab tab) {
        openTab(tab, false);
    }

    private void openTab(Tab tab, boolean fromExternal) {
        setBottomNavVisible(true);

        boolean isAlreadyInHome = (currentTab == Tab.HOME);
        
        if (tab == Tab.HOME && isAlreadyInHome) {
            if (!fromExternal) {
                Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("HOME_FRAGMENT");
                if (currentFragment instanceof HomeFragment) {
                    ((HomeFragment) currentFragment).onHomeButtonPressed();
                }
            }
            return;
        }

        if (tab == Tab.COMMUNITY) {
            openCommunityScreen();
            return;
        }

        setSelectedTab(tab);
        this.currentTab = tab;

        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();
        
        for (Fragment f : fm.getFragments()) {
            if (f != null && f.getId() == R.id.mainFragmentContainer && !f.isHidden()) {
                ft.hide(f);
            }
        }

        String tag = tab.name();
        Fragment targetFragment = fm.findFragmentByTag(tag);

        switch (tab) {
            case CART:
                openCartScreen();
                return;
            case ORDERS:
                if (targetFragment == null) {
                    targetFragment = OrderHistoryFragment.newInstance(false);
                    ft.add(R.id.mainFragmentContainer, targetFragment, tag);
                } else {
                    ft.show(targetFragment);
                }
                break;
            case ACCOUNT:
                boolean isLoggedIn = new AppPreferences(this).isLoggedIn();
                if (targetFragment != null) {
                    boolean isCorrectClass = (isLoggedIn && targetFragment instanceof ProfileLoggedInFragment) || 
                                             (!isLoggedIn && targetFragment instanceof ProfileFragment);
                    if (!isCorrectClass) {
                        ft.remove(targetFragment);
                        targetFragment = null;
                    }
                }
                if (targetFragment == null) {
                    targetFragment = isLoggedIn ? new ProfileLoggedInFragment() : new ProfileFragment();
                    ft.add(R.id.mainFragmentContainer, targetFragment, tag);
                } else {
                    ft.show(targetFragment);
                }
                break;
            case HOME:
            default:
                tag = "HOME_FRAGMENT";
                targetFragment = fm.findFragmentByTag(tag);
                if (targetFragment == null) {
                    targetFragment = new HomeFragment();
                    ft.add(R.id.mainFragmentContainer, targetFragment, tag);
                } else {
                    ft.show(targetFragment);
                }
                break;
        }

        ft.commit();
    }

    public void openCartScreen() {
        // Không set về Tab.HOME ở đây nếu muốn hiển thị Giỏ hàng chuyên biệt
        setBottomNavVisible(false);
        FragmentManager fm = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();
        for (Fragment f : fm.getFragments()) {
            if (f != null && f.getId() == R.id.mainFragmentContainer && !f.isHidden()) {
                ft.hide(f);
            }
        }
        ft.add(R.id.mainFragmentContainer, new CartFragment())
                .addToBackStack("cart")
                .commit();
    }

    public static final String EXTRA_CATEGORY_ID = "extra_category_id";
    public static final String EXTRA_SUBCATEGORY_ID = "extra_subcategory_id";

    public void openCategoryDetail(String categoryId, String subcategoryId) {
        Fragment fragment = new com.veggo.app.presentation.category.CategoryDetailFragment();
        Bundle args = new Bundle();
        if (categoryId != null) {
            args.putString(com.veggo.app.presentation.category.CategoryDetailFragment.ARG_CATEGORY_ID, categoryId);
        }
        if (subcategoryId != null) {
            args.putString(com.veggo.app.presentation.category.CategoryDetailFragment.ARG_SUBCATEGORY_ID, subcategoryId);
        }
        fragment.setArguments(args);
        
        FragmentManager fm = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();
        for (Fragment f : fm.getFragments()) {
            if (f != null && f.getId() == R.id.mainFragmentContainer && !f.isHidden()) {
                ft.hide(f);
            }
        }
        ft.add(R.id.mainFragmentContainer, fragment)
                .addToBackStack("category_detail")
                .commit();
    }

    public void openCategoryScreen() {
        openCategoryScreen(null);
    }

    public void openCategoryScreen(String categoryId) {
        Fragment fragment = new com.veggo.app.presentation.category.CategoryFragment();
        if (categoryId != null) {
            Bundle args = new Bundle();
            args.putString(com.veggo.app.presentation.category.CategoryFragment.ARG_CATEGORY_ID, categoryId);
            fragment.setArguments(args);
        }
        FragmentManager fm = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();
        for (Fragment f : fm.getFragments()) {
            if (f != null && f.getId() == R.id.mainFragmentContainer && !f.isHidden()) {
                ft.hide(f);
            }
        }
        ft.add(R.id.mainFragmentContainer, fragment)
                .addToBackStack("category")
                .commit();
    }

    public void openNotificationsScreen() {
        startActivity(new Intent(this, PostNotificationsActivity.class));
    }

    public void openScanScreen() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_scan_options);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.findViewById(R.id.dialogOptionScanReceipt).setOnClickListener(v -> {
            dialog.dismiss();
            isScanReceiptMode = true;
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                cameraPermLauncher.launch(new String[]{android.Manifest.permission.CAMERA});
            }
        });

        dialog.findViewById(R.id.dialogOptionScanIngredient).setOnClickListener(v -> {
            dialog.dismiss();
            isScanReceiptMode = false;
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                cameraPermLauncher.launch(new String[]{android.Manifest.permission.CAMERA});
            }
        });

        dialog.show();
    }

    private void launchCamera() {
        try {
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
            java.io.File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
            java.io.File photoFile = java.io.File.createTempFile("FRIDGE_" + timeStamp + "_", ".jpg", storageDir);
            cameraImageUri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Không thể mở camera", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    public void openCommunityScreen() {
        getSupportFragmentManager()
                .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        startActivity(new Intent(this, CommunityHomeActivity.class));
        overridePendingTransition(0, 0);
    }

    private void setSelectedTab(Tab tab) {
        int selectedColor = ContextCompat.getColor(this, R.color.primary_main);
        int unselectedColor = ContextCompat.getColor(this, R.color.neutral_80);

        applyTabState(bottomNavBinding.navHomeButton, bottomNavBinding.navHomeIcon, tab == Tab.HOME, selectedColor, unselectedColor, R.drawable.ic_home, R.drawable.ic_home);
        applyTabState(bottomNavBinding.navCommunityButton, bottomNavBinding.navCommunityIcon, tab == Tab.COMMUNITY, selectedColor, unselectedColor, R.drawable.ic_community, R.drawable.ic_community_green);
        applyTabState(bottomNavBinding.navOrdersButton, bottomNavBinding.navOrdersIcon, tab == Tab.ORDERS, selectedColor, unselectedColor, R.drawable.ic_order, R.drawable.ic_order_green);
        applyTabState(bottomNavBinding.navAccountButton, bottomNavBinding.navAccountIcon, tab == Tab.ACCOUNT, selectedColor, unselectedColor, R.drawable.ic_user, R.drawable.ic_user_green);
    }

    private void applyTabState(android.view.View container, AppCompatImageView icon, boolean selected, int selectedColor, int unselectedColor, int unselectedIconRes, int selectedIconRes) {
        int color = selected ? selectedColor : unselectedColor;
        icon.setImageResource(selected ? selectedIconRes : unselectedIconRes);
        icon.setImageTintList(ColorStateList.valueOf(color));
        container.setSelected(selected);
    }

    public void setBottomNavVisible(boolean isVisible) {
        if (bottomNavBinding == null) {
            return;
        }
        bottomNavBinding.getRoot().setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    private enum Tab {
        HOME,
        CART,
        COMMUNITY,
        ORDERS,
        ACCOUNT
    }
}
