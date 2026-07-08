package com.veggo.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.veggo.app.core.ui.BottomNavController;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.data.remote.dto.OrderNotificationDto;
import com.veggo.app.data.repository.OrderNotificationRepository;
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
import com.veggo.app.presentation.support.SupportChatActivity;
import com.veggo.app.core.preferences.PreferencesManager;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_NAV_ITEM = "extra_selected_nav_item";
    public static final String EXTRA_SCROLL_HOME_PRODUCTS = "extra_scroll_home_products";

    private ActivityMainBinding binding;
    private ComponentBottomNavBinding bottomNavBinding;
    private Tab currentTab;
    private boolean supportBubbleInitialized = false;
    private android.net.Uri cameraImageUri = null;
    private boolean isScanReceiptMode = true;
    private View currentNotificationAlert;
    private final OrderNotificationRepository orderNotificationRepository = new OrderNotificationRepository();
    private final android.os.Handler notificationHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable notificationPollRunnable = new Runnable() {
        @Override
        public void run() {
            refreshGlobalNotificationAlert(true);
            notificationHandler.postDelayed(this, 30000);
        }
    };

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
                    intent.putExtra("EXTRA_FROM_NAVBAR", true);
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
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setBottomNavVisible(true);
            }
            updateSupportChatBubbleVisibility();
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
                openTab(tabFromNavItem(intent.getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_home)),
                        fromExternal,
                        intent.getBooleanExtra(EXTRA_SCROLL_HOME_PRODUCTS, false));
                if (intent.getBooleanExtra("EXTRA_OPEN_SCAN", false)) {
                    openScanScreen();
                }
            }
        } else if (savedInstanceState.containsKey("current_tab")) {
            currentTab = Tab.valueOf(savedInstanceState.getString("current_tab", Tab.HOME.name()));
            setSelectedTab(currentTab);
        } else {
            syncCurrentTabFromVisibleFragment();
        }

        setupSupportChatBubble();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentTab != null) {
            outState.putString("current_tab", currentTab.name());
        }
    }

    private void syncCurrentTabFromVisibleFragment() {
        Fragment visible = getVisibleMainFragment();
        if (visible instanceof OrderHistoryFragment) {
            currentTab = Tab.ORDERS;
        } else if (visible instanceof ProfileLoggedInFragment || visible instanceof ProfileFragment) {
            currentTab = Tab.ACCOUNT;
        } else {
            currentTab = Tab.HOME;
        }
        setSelectedTab(currentTab);
    }

    private void setupSupportChatBubble() {
        if (binding == null || binding.supportChatBubble == null) {
            return;
        }
        final View bubble = binding.supportChatBubble;

        if (!supportBubbleInitialized) {
            supportBubbleInitialized = true;
            android.content.SharedPreferences prefs = getSharedPreferences("support_chat_bubble", MODE_PRIVATE);
            float savedX = prefs.getFloat("x", -1f);
            float savedY = prefs.getFloat("y", -1f);
            bubble.post(() -> {
                if (savedX >= 0f) bubble.setX(savedX);
                if (savedY >= 0f) bubble.setY(savedY);
                clampSupportBubbleWithinScreen(bubble);
                avoidOverlapWithHomeChatbotBubble(bubble);
            });

            bubble.setOnClickListener(v -> {
                if (!new AppPreferences(this).isLoggedIn()) {
                    android.widget.Toast.makeText(this, "Vui lòng đăng nhập để chat hỗ trợ", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                startActivity(new Intent(this, SupportChatActivity.class));
            });

            attachSupportBubbleDragBehavior(bubble, prefs);
        }

        updateSupportChatBubbleVisibility();
    }

    private boolean shouldShowSupportChatBubble() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            return false;
        }
        return resolveActiveTab() == Tab.HOME;
    }

    private Tab resolveActiveTab() {
        if (currentTab != null) {
            return currentTab;
        }
        Fragment visible = getVisibleMainFragment();
        if (visible instanceof OrderHistoryFragment) {
            return Tab.ORDERS;
        }
        if (visible instanceof ProfileLoggedInFragment || visible instanceof ProfileFragment) {
            return Tab.ACCOUNT;
        }
        return Tab.HOME;
    }

    private void updateSupportChatBubbleVisibility() {
        if (binding == null || binding.supportChatBubble == null) {
            return;
        }
        final View bubble = binding.supportChatBubble;
        boolean show = shouldShowSupportChatBubble();
        bubble.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            return;
        }
        Runnable positionBubble = () -> {
            clampSupportBubbleWithinScreen(bubble);
            avoidOverlapWithHomeChatbotBubble(bubble);
        };
        ViewGroup parent = (ViewGroup) bubble.getParent();
        if (parent != null && parent.getWidth() > 0 && parent.getHeight() > 0) {
            positionBubble.run();
        } else {
            bubble.post(positionBubble);
        }
    }

    private void attachSupportBubbleDragBehavior(View bubble, android.content.SharedPreferences prefs) {
        final float density = getResources().getDisplayMetrics().density;
        final float touchSlop = 8f * density;
        final float edgeMargin = 16f * density;
        final float bottomSafeMargin = 96f * density;
        final int[] parentLocation = new int[2];
        final ViewGroup parent = (ViewGroup) bubble.getParent();

        bubble.setOnTouchListener(new View.OnTouchListener() {
            boolean dragging = false;
            float offsetX, offsetY;
            float downRawX, downRawY;

            @Override
            public boolean onTouch(View view, android.view.MotionEvent event) {
                if (parent == null) return false;
                parent.getLocationOnScreen(parentLocation);
                float localX = event.getRawX() - parentLocation[0];
                float localY = event.getRawY() - parentLocation[1];

                switch (event.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        dragging = false;
                        offsetX = localX - view.getX();
                        offsetY = localY - view.getY();
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        view.setPressed(true);
                        return true;

                    case android.view.MotionEvent.ACTION_MOVE:
                        float deltaX = Math.abs(event.getRawX() - downRawX);
                        float deltaY = Math.abs(event.getRawY() - downRawY);
                        if (!dragging && (deltaX > touchSlop || deltaY > touchSlop)) {
                            dragging = true;
                            view.setPressed(false);
                        }
                        if (dragging) {
                            float targetX = clampX(view, parent, localX - offsetX, edgeMargin);
                            float targetY = clampY(view, parent, localY - offsetY, edgeMargin, bottomSafeMargin);
                            view.setX(targetX);
                            view.setY(targetY);
                        }
                        return true;

                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        view.setPressed(false);
                        if (dragging) {
                            snapToEdge(view, parent, edgeMargin, prefs);
                            avoidOverlapWithHomeChatbotBubble(view);
                            prefs.edit().putFloat("x", view.getX()).putFloat("y", view.getY()).apply();
                            dragging = false;
                            return true;
                        }
                        view.performClick();
                        return true;

                    default:
                        return false;
                }
            }
        });
    }

    private float clampX(View bubble, ViewGroup parent, float x, float edgeMargin) {
        float minX = edgeMargin;
        float maxX = parent.getWidth() - bubble.getWidth() - edgeMargin;
        if (maxX < minX) maxX = minX;
        return Math.max(minX, Math.min(x, maxX));
    }

    private float clampY(View bubble, ViewGroup parent, float y, float topMargin, float bottomSafeMargin) {
        float minY = 12f * getResources().getDisplayMetrics().density;
        float maxY = parent.getHeight() - bubble.getHeight() - bottomSafeMargin;
        if (maxY < minY) maxY = minY;
        return Math.max(minY, Math.min(y, maxY));
    }

    private void snapToEdge(View bubble, ViewGroup parent, float edgeMargin, android.content.SharedPreferences prefs) {
        float centerX = bubble.getX() + bubble.getWidth() / 2f;
        float targetX = centerX < parent.getWidth() / 2f
                ? edgeMargin
                : parent.getWidth() - bubble.getWidth() - edgeMargin;
        bubble.animate()
                .x(targetX)
                .setDuration(180L)
                .withEndAction(() -> prefs.edit().putFloat("x", bubble.getX()).putFloat("y", bubble.getY()).apply())
                .start();
    }

    private void clampSupportBubbleWithinScreen(View bubble) {
        ViewGroup parent = (ViewGroup) bubble.getParent();
        if (parent == null) return;
        final float density = getResources().getDisplayMetrics().density;
        final float edgeMargin = 16f * density;
        final float bottomSafeMargin = 168f * density;
        bubble.setX(clampX(bubble, parent, bubble.getX(), edgeMargin));
        bubble.setY(clampY(bubble, parent, bubble.getY(), edgeMargin, bottomSafeMargin));
    }

    private void avoidOverlapWithHomeChatbotBubble(View supportBubble) {
        try {
            Fragment fragment = getVisibleMainFragment();
            if (!(fragment instanceof HomeFragment)) {
                return;
            }
            if (fragment.getView() == null) return;
            View chatbot = fragment.getView().findViewById(R.id.btnChatbot);
            if (chatbot == null || chatbot.getVisibility() != View.VISIBLE || supportBubble.getVisibility() != View.VISIBLE) {
                return;
            }

            int[] supportLoc = new int[2];
            int[] chatbotLoc = new int[2];
            supportBubble.getLocationOnScreen(supportLoc);
            chatbot.getLocationOnScreen(chatbotLoc);

            android.graphics.RectF supportRect = new android.graphics.RectF(
                    supportLoc[0],
                    supportLoc[1],
                    supportLoc[0] + supportBubble.getWidth(),
                    supportLoc[1] + supportBubble.getHeight()
            );
            android.graphics.RectF chatbotRect = new android.graphics.RectF(
                    chatbotLoc[0],
                    chatbotLoc[1],
                    chatbotLoc[0] + chatbot.getWidth(),
                    chatbotLoc[1] + chatbot.getHeight()
            );

            final float gap = 12f * getResources().getDisplayMetrics().density;
            if (android.graphics.RectF.intersects(supportRect, chatbotRect)) {
                // Prefer moving support bubble upward to avoid covering chatbot.
                float newY = supportBubble.getY() - (chatbot.getHeight() + gap);
                supportBubble.setY(newY);
                clampSupportBubbleWithinScreen(supportBubble);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationHandler.removeCallbacks(notificationPollRunnable);
        refreshGlobalNotificationAlert(true);
        notificationHandler.postDelayed(notificationPollRunnable, 30000);

        // Re-check login/token state after returning to MainActivity
        setupSupportChatBubble();
    }

    @Override
    protected void onPause() {
        super.onPause();
        notificationHandler.removeCallbacks(notificationPollRunnable);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasExtra(EXTRA_CATEGORY_ID)) {
            openCategoryDetail(intent.getStringExtra(EXTRA_CATEGORY_ID), intent.getStringExtra(EXTRA_SUBCATEGORY_ID));
        } else {
            boolean fromExternal = intent.getBooleanExtra("from_external", false);
            openTab(tabFromNavItem(intent.getIntExtra(EXTRA_SELECTED_NAV_ITEM, R.id.nav_home)),
                    fromExternal,
                    intent.getBooleanExtra(EXTRA_SCROLL_HOME_PRODUCTS, false));
            if (intent.getBooleanExtra("EXTRA_OPEN_SCAN", false)) {
                openScanScreen();
            }
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
        openTab(tab, fromExternal, false);
    }

    private void openTab(Tab tab, boolean fromExternal, boolean scrollHomeProducts) {
        setBottomNavVisible(true);

        boolean isAlreadyInHome = currentTab == Tab.HOME
                && getVisibleMainFragment() instanceof HomeFragment;

        if (tab == Tab.HOME && isAlreadyInHome) {
            Fragment currentFragment = getVisibleMainFragment();
            if (!fromExternal) {
                ((HomeFragment) currentFragment).onHomeButtonPressed();
            }
            if (scrollHomeProducts) {
                ((HomeFragment) currentFragment).scrollToProductsSection();
            }
            updateSupportChatBubbleVisibility();
            return;
        }

        if (tab == Tab.HOME && currentTab == Tab.HOME) {
            Fragment currentFragment = getVisibleMainFragment();
            if (currentFragment != null && !(currentFragment instanceof HomeFragment)) {
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
        }

        if (tab == Tab.COMMUNITY) {
            openCommunityScreen();
            updateSupportChatBubbleVisibility();
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
                    if (scrollHomeProducts) {
                        Bundle args = new Bundle();
                        args.putBoolean(EXTRA_SCROLL_HOME_PRODUCTS, true);
                        targetFragment.setArguments(args);
                    }
                    ft.add(R.id.mainFragmentContainer, targetFragment, tag);
                } else {
                    ft.show(targetFragment);
                    if (scrollHomeProducts && targetFragment instanceof HomeFragment) {
                        fm.executePendingTransactions();
                        ((HomeFragment) targetFragment).scrollToProductsSection();
                    }
                }
                break;
        }

        ft.runOnCommit(this::updateSupportChatBubbleVisibility);
        ft.commit();
        fm.executePendingTransactions();
        updateSupportChatBubbleVisibility();
        if (scrollHomeProducts && targetFragment instanceof HomeFragment) {
            fm.executePendingTransactions();
            ((HomeFragment) targetFragment).scrollToProductsSection();
        }
    }

    private Fragment getVisibleMainFragment() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null && fragment.getId() == R.id.mainFragmentContainer && fragment.isVisible()) {
                return fragment;
            }
        }
        return null;
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
        CartFragment cartFragment = new CartFragment();
        java.util.ArrayList<String> selectedSkus = getIntent().getStringArrayListExtra(CartFragment.ARG_SELECTED_SKUS);
        if (selectedSkus != null && !selectedSkus.isEmpty()) {
            Bundle args = new Bundle();
            args.putStringArrayList(CartFragment.ARG_SELECTED_SKUS, selectedSkus);
            cartFragment.setArguments(args);
            getIntent().removeExtra(CartFragment.ARG_SELECTED_SKUS);
        }
        ft.add(R.id.mainFragmentContainer, cartFragment)
                .addToBackStack("cart")
                .commit();
        updateSupportChatBubbleVisibility();
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
        updateSupportChatBubbleVisibility();
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
        updateSupportChatBubbleVisibility();
    }

    public void openNotificationsScreen() {
        startActivity(new Intent(this, PostNotificationsActivity.class));
    }

    public void showTopNotificationAlert(String title, String body) {
        if (binding == null) {
            return;
        }
        if (currentNotificationAlert != null) {
            binding.main.removeView(currentNotificationAlert);
            currentNotificationAlert = null;
        }

        android.widget.LinearLayout card = new android.widget.LinearLayout(this);
        card.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackgroundResource(R.drawable.bg_post_notification_featured_card);
        card.setElevation(dp(8));
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> openNotificationsScreen());

        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageResource(R.drawable.ic_notify);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.primary_hover));
        android.widget.LinearLayout.LayoutParams iconParams =
                new android.widget.LinearLayout.LayoutParams(dp(26), dp(26));
        card.addView(icon, iconParams);

        android.widget.LinearLayout textColumn = new android.widget.LinearLayout(this);
        textColumn.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.LinearLayout.LayoutParams textParams =
                new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMarginStart(dp(12));

        android.widget.TextView titleView = new android.widget.TextView(this);
        titleView.setText(title == null || title.trim().isEmpty() ? "Thông báo mới" : title);
        titleView.setTextColor(ContextCompat.getColor(this, R.color.neutral_100));
        titleView.setTextSize(14);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        titleView.setMaxLines(1);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textColumn.addView(titleView);

        android.widget.TextView bodyView = new android.widget.TextView(this);
        bodyView.setText(body == null ? "" : body);
        bodyView.setTextColor(ContextCompat.getColor(this, R.color.neutral_70));
        bodyView.setTextSize(13);
        bodyView.setMaxLines(2);
        bodyView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textColumn.addView(bodyView);
        card.addView(textColumn, textParams);

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        0,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                );
        params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        params.setMargins(dp(16), getStatusBarHeight() + dp(8), dp(16), 0);
        binding.main.addView(card, params);
        currentNotificationAlert = card;

        card.post(() -> {
            card.setTranslationY(-card.getHeight() - dp(24));
            card.animate().translationY(0).setDuration(260).start();
            card.postDelayed(() -> {
                if (currentNotificationAlert == card) {
                    card.animate()
                            .translationY(-card.getHeight() - dp(24))
                            .alpha(0f)
                            .setDuration(220)
                            .withEndAction(() -> {
                                if (currentNotificationAlert == card) {
                                    binding.main.removeView(card);
                                    currentNotificationAlert = null;
                                }
                            })
                            .start();
                }
            }, 4500);
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int getStatusBarHeight() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return dp(24);
    }

    private void refreshGlobalNotificationAlert(boolean allowAlert) {
        String customerId = new AppPreferences(this).getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            updateHomeNotificationBadge(0);
            return;
        }
        orderNotificationRepository.getNotifications(customerId, notifications -> runOnUiThread(() -> {
            updateHomeNotificationBadge(OrderNotificationRepository.countUnread(notifications));
            if (allowAlert) {
                maybeShowNewNotificationAlert(notifications);
            }
        }));
    }

    private void maybeShowNewNotificationAlert(java.util.List<OrderNotificationDto> notifications) {
        OrderNotificationDto latestUnread = OrderNotificationRepository.latestUnread(notifications);
        if (latestUnread == null) {
            return;
        }

        String latestId = latestUnread.getId();
        if (latestId.trim().isEmpty()) {
            latestId = latestUnread.getCreatedAtText();
        }
        if (latestId.trim().isEmpty()) {
            return;
        }

        android.content.SharedPreferences prefs =
                getSharedPreferences("order_notification_state", android.content.Context.MODE_PRIVATE);
        String lastSeenId = prefs.getString("last_seen_order_notification_id", "");
        if (lastSeenId == null || lastSeenId.isEmpty()) {
            prefs.edit().putString("last_seen_order_notification_id", latestId).apply();
            return;
        }
        if (latestId.equals(lastSeenId)) {
            return;
        }

        prefs.edit().putString("last_seen_order_notification_id", latestId).apply();
        String title = firstNonBlank(latestUnread.getTitle(), "Thông báo mới");
        String body = firstNonBlank(latestUnread.getBody(), "");
        showTopNotificationAlert(title, body);
    }

    private void updateHomeNotificationBadge(int count) {
        Fragment fragment = getVisibleMainFragment();
        if (fragment instanceof HomeFragment) {
            ((HomeFragment) fragment).updateNotificationBadgesFromMain(count);
        }
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
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
