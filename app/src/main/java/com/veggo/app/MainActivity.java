package com.veggo.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.veggo.app.core.ui.BadgeUiHelper;
import com.veggo.app.core.ui.BottomNavController;
import com.veggo.app.core.realtime.RealtimeEvents;
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
import com.veggo.app.presentation.profile.FridgeQuickScanHelper;
import com.veggo.app.core.utils.KeyboardUtils;
import com.veggo.app.presentation.profile.LoginRequiredActivity;
import com.veggo.app.presentation.profile.PostNotificationsActivity;
import com.veggo.app.presentation.profile.ProfileLoggedInFragment;
import com.veggo.app.presentation.support.SupportChatActivity;
import com.veggo.app.core.preferences.PreferencesManager;
import com.veggo.app.data.remote.api.SupportApi;
import com.veggo.app.data.remote.dto.SupportConversationDto;
import com.veggo.app.data.remote.dto.SupportConversationsResponseDto;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.network.SupportSocketManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_NAV_ITEM = "extra_selected_nav_item";
    public static final String EXTRA_SCROLL_HOME_PRODUCTS = "extra_scroll_home_products";

    private ActivityMainBinding binding;
    private ComponentBottomNavBinding bottomNavBinding;
    private Tab currentTab;
    private boolean supportBubbleInitialized = false;
    private SupportApi supportApi;
    private SupportSocketManager supportSocketManager;
    private boolean supportSocketConnecting = false;
    private boolean supportSocketActive = false;
    private int lastSupportUnreadCount = 0;
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
    private final Runnable supportChatPollRunnable = new Runnable() {
        @Override
        public void run() {
            if (shouldShowSupportChatBubble() && new AppPreferences(MainActivity.this).isLoggedIn()) {
                fetchSupportChatUnreadCount();
            }
            notificationHandler.postDelayed(this, 15000);
        }
    };

    private final androidx.activity.result.ActivityResultLauncher<String> notificationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Đã xử lý kết quả xin quyền thông báo, không cần làm gì thêm
            });

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
                restoreBaseTabFragment();
            }
            updateSupportChatBubbleVisibility();
            
            // Cập nhật lại số lượng giỏ hàng trên trang chủ sau khi quay lại từ giỏ hàng (pop fragment)
            Fragment visible = getVisibleMainFragment();
            if (visible instanceof HomeFragment) {
                ((HomeFragment) visible).refreshCartBadge();
            }
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
            if (intent.hasExtra(EXTRA_CATEGORY_ID) || intent.hasExtra(EXTRA_SUBCATEGORY_ID)) {
                openCategoryFromIntent(intent);
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
                View chatbot = findHomeChatbotBubble();
                boolean hasSavedPosition = savedX >= 0f && savedY >= 0f;
                if (hasSavedPosition) {
                    bubble.setX(savedX);
                    bubble.setY(savedY);
                } else if (chatbot != null) {
                    placeSupportBubbleAboveChatbot(bubble, chatbot);
                }
                repositionSupportBubbleIfNeeded(bubble);
                clampSupportBubbleWithinScreen(bubble);
            });

            bubble.setOnClickListener(v -> {
                if (!new AppPreferences(this).isLoggedIn()) {
                    android.widget.Toast.makeText(this, "Vui lòng đăng nhập để chat hỗ trợ", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                updateSupportChatUnreadBadge(0);
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
            updateSupportChatSocketListener();
            return;
        }
        fetchSupportChatUnreadCount();
        updateSupportChatSocketListener();
        Runnable positionBubble = () -> {
            repositionSupportBubbleIfNeeded(bubble);
            clampSupportBubbleWithinScreen(bubble);
        };
        ViewGroup parent = (ViewGroup) bubble.getParent();
        if (parent != null && parent.getWidth() > 0 && parent.getHeight() > 0) {
            positionBubble.run();
            bubble.post(positionBubble);
        } else {
            bubble.post(positionBubble);
        }
    }

    private void fetchSupportChatUnreadCount() {
        String customerId = new AppPreferences(this).getCustomerId();
        if (android.text.TextUtils.isEmpty(customerId)) {
            updateSupportChatUnreadBadge(0);
            return;
        }

        if (supportApi == null) {
            supportApi = ApiClient.createService(SupportApi.class);
        }

        supportApi.getConversations(customerId).enqueue(new Callback<SupportConversationsResponseDto>() {
            @Override
            public void onResponse(Call<SupportConversationsResponseDto> call, Response<SupportConversationsResponseDto> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SupportConversationDto> data = response.body().getData();
                    if (data != null && !data.isEmpty() && data.get(0) != null) {
                        int unreadCount = data.get(0).getUnreadCountUser();
                        runOnUiThread(() -> updateSupportChatUnreadBadge(unreadCount));
                    } else {
                        runOnUiThread(() -> updateSupportChatUnreadBadge(0));
                    }
                } else {
                    runOnUiThread(() -> updateSupportChatUnreadBadge(0));
                }
            }

            @Override
            public void onFailure(Call<SupportConversationsResponseDto> call, Throwable t) {
                runOnUiThread(() -> updateSupportChatUnreadBadge(0));
            }
        });
    }

    private void updateSupportChatUnreadBadge(int count) {
        if (binding == null) return;
        android.widget.TextView tvBadge = binding.tvSupportChatUnreadBadge;
        if (tvBadge == null) return;
        boolean shouldAnimate = count > lastSupportUnreadCount && count > 0;
        lastSupportUnreadCount = count;
        BadgeUiHelper.applyAlertBadge(tvBadge, count);
        if (count > 0 && shouldAnimate) {
            playSupportBubbleAttentionAnimation();
        }
    }

    private void playSupportBubbleAttentionAnimation() {
        if (binding == null || binding.supportChatBubble == null) return;
        final View bubble = binding.supportChatBubble;
        final float bounce = -10f * getResources().getDisplayMetrics().density;
        bubble.animate().cancel();
        bubble.setTranslationY(0f);
        bubble.animate()
                .translationY(bounce)
                .setDuration(110)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> bubble.animate()
                        .translationY(0f)
                        .setDuration(110)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .withEndAction(() -> bubble.animate()
                                .translationY(bounce * 0.55f)
                                .setDuration(90)
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                                .withEndAction(() -> bubble.animate()
                                        .translationY(0f)
                                        .setDuration(90)
                                        .setInterpolator(new AccelerateDecelerateInterpolator())
                                        .start())
                                .start())
                        .start())
                .start();
    }

    private void updateSupportChatSocketListener() {
        if (!new AppPreferences(this).isLoggedIn()) {
            disconnectSupportChatSocket();
            return;
        }
        connectSupportChatSocketIfNeeded();
    }

    private void connectSupportChatSocketIfNeeded() {
        if (supportSocketConnecting || supportSocketActive) return;
        String token = new PreferencesManager(this).getAccessToken();
        if (TextUtils.isEmpty(token)) return;

        if (supportSocketManager == null) {
            supportSocketManager = new SupportSocketManager();
        }
        supportSocketConnecting = true;
        supportSocketManager.connect(token.trim(), new SupportSocketManager.Listener() {
            @Override
            public void onConnected() {
                supportSocketConnecting = false;
                supportSocketActive = true;
            }

            @Override
            public void onDisconnected() {
                supportSocketConnecting = false;
                supportSocketActive = false;
            }

            @Override
            public void onNewMessage(org.json.JSONObject messageJson) {
                // Badge updates are pushed via support:unread while user is on home.
            }

            @Override
            public void onTypingUpdate(org.json.JSONObject typingJson) {
                // no-op on home screen
            }

            @Override
            public void onUnreadUpdate(int unreadCountUser) {
                runOnUiThread(() -> updateSupportChatUnreadBadge(unreadCountUser));
            }

            @Override
            public void onUserNotification(JSONObject notificationJson) {
                runOnUiThread(() -> {
                    refreshGlobalNotificationAlert(false);
                    broadcastRealtime(RealtimeEvents.ACTION_NOTIFICATION_CHANGED,
                            realtimeType(notificationJson, "user:notification"),
                            notificationJson);
                });
            }

            @Override
            public void onOrderUpdated(JSONObject orderJson) {
                runOnUiThread(() -> {
                    refreshGlobalNotificationAlert(false);
                    String type = realtimeType(orderJson, "order:changed");
                    broadcastRealtime(RealtimeEvents.ACTION_ORDER_CHANGED, type, orderJson);
                    broadcastRealtime(RealtimeEvents.ACTION_NOTIFICATION_CHANGED, type, orderJson);
                });
            }

            @Override
            public void onPromotionChanged(JSONObject promotionJson) {
                runOnUiThread(() ->
                        broadcastRealtime(RealtimeEvents.ACTION_PROMOTION_CHANGED,
                                realtimeType(promotionJson, "promotion:changed"),
                                promotionJson));
            }

            @Override
            public void onError(String message) {
                supportSocketConnecting = false;
                supportSocketActive = false;
            }
        });
    }

    private String realtimeType(JSONObject payload, String fallback) {
        if (payload == null) {
            return fallback;
        }
        String type = payload.optString("type", "");
        return TextUtils.isEmpty(type) ? fallback : type;
    }

    private void broadcastRealtime(String action, String type, JSONObject payload) {
        Intent intent = new Intent(action);
        intent.setPackage(getPackageName());
        intent.putExtra(RealtimeEvents.EXTRA_TYPE, type);
        intent.putExtra(RealtimeEvents.EXTRA_PAYLOAD, payload != null ? payload.toString() : "{}");
        sendBroadcast(intent);
    }

    private void disconnectSupportChatSocket() {
        supportSocketConnecting = false;
        supportSocketActive = false;
        if (supportSocketManager != null) {
            supportSocketManager.disconnect();
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
                            repositionSupportBubbleIfNeeded(view);
                            clampSupportBubbleWithinScreen(view);
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
        final float minY = 12f * density;
        final float gap = 12f * density;

        bubble.setX(clampX(bubble, parent, bubble.getX(), edgeMargin));

        float maxY = parent.getHeight() - bubble.getHeight() - 96f * density;
        View chatbot = findHomeChatbotBubble();
        if (chatbot != null) {
            int[] parentLoc = new int[2];
            int[] chatbotLoc = new int[2];
            parent.getLocationOnScreen(parentLoc);
            chatbot.getLocationOnScreen(chatbotLoc);
            float chatbotTopInParent = chatbotLoc[1] - parentLoc[1];
            maxY = Math.min(maxY, chatbotTopInParent - bubble.getHeight() - gap);
        } else {
            maxY = parent.getHeight() - bubble.getHeight() - 168f * density;
        }
        if (maxY < minY) {
            maxY = minY;
        }
        bubble.setY(Math.max(minY, Math.min(bubble.getY(), maxY)));
    }

    @androidx.annotation.Nullable
    private View findHomeChatbotBubble() {
        Fragment fragment = getVisibleMainFragment();
        if (!(fragment instanceof HomeFragment) || fragment.getView() == null) {
            return null;
        }
        View chatbot = fragment.getView().findViewById(R.id.btnChatbot);
        if (chatbot == null || chatbot.getVisibility() != View.VISIBLE) {
            return null;
        }
        return chatbot;
    }

    public void repositionSupportChatBubbleIfNeeded() {
        if (binding == null || binding.supportChatBubble == null) {
            return;
        }
        View bubble = binding.supportChatBubble;
        if (bubble.getVisibility() != View.VISIBLE) {
            return;
        }
        bubble.post(() -> {
            repositionSupportBubbleIfNeeded(bubble);
            clampSupportBubbleWithinScreen(bubble);
        });
    }

    private void placeSupportBubbleAboveChatbot(View supportBubble, View chatbot) {
        ViewGroup parent = (ViewGroup) supportBubble.getParent();
        if (parent == null) {
            return;
        }
        final float density = getResources().getDisplayMetrics().density;
        final float edgeMargin = 16f * density;
        final float gap = 12f * density;

        int[] parentLoc = new int[2];
        int[] chatbotLoc = new int[2];
        parent.getLocationOnScreen(parentLoc);
        chatbot.getLocationOnScreen(chatbotLoc);

        float chatbotTopInParent = chatbotLoc[1] - parentLoc[1];
        float targetX = parent.getWidth() - supportBubble.getWidth() - edgeMargin;
        float targetY = chatbotTopInParent - supportBubble.getHeight() - gap;

        supportBubble.setX(clampX(supportBubble, parent, targetX, edgeMargin));
        supportBubble.setY(targetY);
    }

    private void repositionSupportBubbleIfNeeded(View supportBubble) {
        View chatbot = findHomeChatbotBubble();
        if (chatbot == null || supportBubble.getVisibility() != View.VISIBLE) {
            return;
        }
        ViewGroup parent = (ViewGroup) supportBubble.getParent();
        if (parent == null) {
            return;
        }

        final float density = getResources().getDisplayMetrics().density;
        final float gap = 12f * density;
        final float edgeMargin = 16f * density;

        int[] parentLoc = new int[2];
        int[] supportLoc = new int[2];
        int[] chatbotLoc = new int[2];
        parent.getLocationOnScreen(parentLoc);
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
        supportRect.inset(-gap, -gap);
        if (!android.graphics.RectF.intersects(supportRect, chatbotRect)) {
            return;
        }

        float chatbotTopInParent = chatbotLoc[1] - parentLoc[1];
        float targetY = chatbotTopInParent - supportBubble.getHeight() - gap;
        float targetX = parent.getWidth() - supportBubble.getWidth() - edgeMargin;
        supportBubble.setX(clampX(supportBubble, parent, targetX, edgeMargin));
        supportBubble.setY(targetY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        notificationHandler.removeCallbacks(notificationPollRunnable);
        notificationHandler.removeCallbacks(supportChatPollRunnable);
        refreshGlobalNotificationAlert(true);
        notificationHandler.postDelayed(notificationPollRunnable, 30000);
        notificationHandler.postDelayed(supportChatPollRunnable, 15000);

        // Re-check login/token state after returning to MainActivity
        setupSupportChatBubble();
    }

    @Override
    protected void onPause() {
        super.onPause();
        notificationHandler.removeCallbacks(notificationPollRunnable);
        notificationHandler.removeCallbacks(supportChatPollRunnable);
    }

    @Override
    protected void onDestroy() {
        disconnectSupportChatSocket();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasExtra(EXTRA_CATEGORY_ID) || intent.hasExtra(EXTRA_SUBCATEGORY_ID)) {
            openCategoryFromIntent(intent);
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

    private void openCategoryFromIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID);
        String subcategoryId = intent.getStringExtra(EXTRA_SUBCATEGORY_ID);
        if (categoryId == null && subcategoryId == null) {
            return;
        }
        ensureHomeTabLoaded();
        getSupportFragmentManager().executePendingTransactions();
        openCategoryDetail(categoryId, subcategoryId);
        intent.removeExtra(EXTRA_CATEGORY_ID);
        intent.removeExtra(EXTRA_SUBCATEGORY_ID);
    }

    private void ensureHomeTabLoaded() {
        Fragment homeFragment = getSupportFragmentManager().findFragmentByTag("HOME_FRAGMENT");
        if (homeFragment == null || !(homeFragment instanceof HomeFragment) || currentTab != Tab.HOME) {
            openTab(Tab.HOME, true);
        }
    }

    private void restoreBaseTabFragment() {
        FragmentManager fm = getSupportFragmentManager();
        Tab tab = currentTab != null ? currentTab : Tab.HOME;
        String tag = tab == Tab.HOME ? "HOME_FRAGMENT" : tab.name();
        Fragment baseFragment = fm.findFragmentByTag(tag);
        if (baseFragment == null) {
            if (tab == Tab.HOME) {
                openTab(Tab.HOME, true);
            }
            return;
        }

        androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction();
        for (Fragment fragment : fm.getFragments()) {
            if (fragment != null
                    && fragment.getId() == R.id.mainFragmentContainer
                    && fragment.isVisible()
                    && fragment != baseFragment) {
                ft.hide(fragment);
            }
        }
        if (baseFragment.isHidden()) {
            ft.show(baseFragment);
        }
        ft.commit();
    }

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
        if (!new AppPreferences(this).isLoggedIn()) {
            LoginRequiredActivity.open(this, "quét sản phẩm");
            return;
        }

        FridgeQuickScanHelper.showScanOptionsDialog(this, new FridgeQuickScanHelper.OptionsListener() {
            @Override
            public void onReceiptScanSelected() {
                startActivity(FridgeQuickScanHelper.createNavbarCameraIntent(MainActivity.this, true));
            }

            @Override
            public void onIngredientScanSelected() {
                startActivity(FridgeQuickScanHelper.createNavbarCameraIntent(MainActivity.this, false));
            }
        });
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        KeyboardUtils.handleActivityTouchToHideKeyboard(this, event);
        return super.dispatchTouchEvent(event);
    }
}
