package com.veggo.app.presentation.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import com.veggo.app.R;
import com.veggo.app.MainActivity;
import com.veggo.app.adapter.BannerAdapter;
import com.veggo.app.adapter.CategoryAdapter;
import com.veggo.app.adapter.FlashSaleAdapter;
import com.veggo.app.adapter.ProductAdapter;
import com.veggo.app.adapter.RecipeAdapter;
import com.veggo.app.adapter.UtilityAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.core.utils.CartCountUtils;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.PreferencesManager;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.repository.OrderNotificationRepository;
import com.veggo.app.databinding.FragmentHomeBinding;
import com.veggo.app.databinding.LayoutHomeStickyTabsBinding;
import com.veggo.app.di.AppModule;
import com.veggo.app.presentation.about.AboutUsActivity;
import com.veggo.app.presentation.checkout.PendingCheckoutStore;
import com.veggo.app.presentation.product.AddToCartBottomSheetHelper;
import com.veggo.app.presentation.profile.LoginRequiredActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String PREF_CHATBOT_FAB = "home_chatbot_fab_position";
    private static final String KEY_CHATBOT_FAB_X = "chatbot_fab_x";
    private static final String KEY_CHATBOT_FAB_Y = "chatbot_fab_y";

    // ─── Interface để MainActivity gọi khi nhấn icon Home ────────────────
    public interface HomeButtonHandler {
        /**
         * Gọi khi người dùng nhấn icon Home trong bottom nav khi đang ở màn Home.
         * @return true nếu fragment đã tự xử lý (scroll về top hoặc reload)
         */
        boolean onHomeButtonPressed();
    }

    // ─── Static: giữ scroll position qua các lần replace fragment ────────
    /** Lưu scrollY khi rời khỏi Home để có thể phục hồi khi quay lại. */
    private static int savedScrollY = 0;

    private FragmentHomeBinding binding;
    /** Binding riêng cho sticky tabs overlay */
    private LayoutHomeStickyTabsBinding stickyTabsBinding;
    private HomeViewModel homeViewModel;

    private ProductAdapter productAdapter;
    private UtilityAdapter utilityAdapter;
    private BannerAdapter bannerAdapter;
    private FlashSaleAdapter flashSaleAdapter;
    private RecipeAdapter recipeAdapter;
    private CategoryAdapter categoryAdapter;

    private float chatbotDragOffsetX;
    private float chatbotDragOffsetY;
    private float chatbotDownRawX;
    private float chatbotDownRawY;
    private boolean chatbotDragging;

    /** Thứ tự tương ứng với VALID_TABS trong ViewModel */
    private static final String[] TAB_KEYS = {
            HomeViewModel.TAB_POPULAR,
            HomeViewModel.TAB_TRENDING,
            HomeViewModel.TAB_NEWEST,
            "PRICE_PLACEHOLDER",
            HomeViewModel.TAB_TOP_RATED
    };

    /** Top Y của HorizontalScrollView chứa tabs (tính trong ScrollView content) */
    private int tabBarTop = Integer.MAX_VALUE;
    /** Height của sticky header (64dp) */
    private int stickyHeaderHeight;

    /** HorizontalScrollView chứa tabs (inline) */
    private android.widget.HorizontalScrollView inlineTabsHsv;
    /** HorizontalScrollView chứa tabs (sticky) */
    private android.widget.HorizontalScrollView stickyTabsHsv;

    private View[] bannerIndicators;
    private SwipeRefreshLayout homeRefreshLayout;
    private boolean homeRefreshPending;

    private static final int BANNER_LOOP_ANCHOR = Integer.MAX_VALUE / 2;

    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final OrderNotificationRepository orderNotificationRepository = new OrderNotificationRepository();
    private final Runnable bannerRunnable = () -> {
        if (binding == null || binding.vpBanners == null
                || bannerAdapter == null || bannerAdapter.getRealCount() <= 0) {
            return;
        }
        int count = bannerAdapter.getRealCount();
        int current = binding.vpBanners.getCurrentItem();
        int nextIndex = (Math.floorMod(current, count) + 1) % count;
        binding.vpBanners.setCurrentItem(resolveBannerLoopPosition(nextIndex), true);
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        stickyTabsBinding = LayoutHomeStickyTabsBinding.bind(binding.stickyTabs.getRoot());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        stickyHeaderHeight = (int) (64 * getResources().getDisplayMetrics().density);

        setupHeader();
        setupRecyclerViews();
        setupBannerIndicators();
        setupProductTabs();
        setupStickyBehavior();
        setupHeaderActions();
        observeViewModel();
        setupPullToRefresh();

        homeViewModel.loadHomeData(requireContext());
        if (getArguments() != null
                && getArguments().getBoolean(MainActivity.EXTRA_SCROLL_HOME_PRODUCTS, false)) {
            binding.homeScrollView.postDelayed(this::scrollToProductsSection, 250);
            getArguments().putBoolean(MainActivity.EXTRA_SCROLL_HOME_PRODUCTS, false);
        }

        // Phục hồi scroll position đã lưu (khi quay lại từ tab khác)
        if (savedScrollY > 0) {
            binding.homeScrollView.post(() -> {
                if (binding != null) {
                    binding.homeScrollView.scrollTo(0, savedScrollY);
                }
            });
        }
    }

    /**
     * Xử lý nhấn icon Home từ bottom nav khi đang ở màn Home:
     *  - Nếu đã ở đầu trang (scrollY == 0) → reload dữ liệu
     *  - Nếu đang ở giữa trang (scrollY > 0) → scroll về đầu
     *
     * @return true nếu đã xử lý
     */
    public boolean onHomeButtonPressed() {
        if (binding == null) return false;
        int currentScrollY = binding.homeScrollView.getScrollY();
        if (currentScrollY == 0) {
            homeRefreshPending = true;
            homeViewModel.loadHomeData(requireContext());
        } else {
            // Đang ở giữa trang → scroll smooth về top
            binding.homeScrollView.smoothScrollTo(0, 0);
        }
        return true;
    }

    private int resolveBannerLoopPosition(int realIndex) {
        int count = bannerAdapter != null ? bannerAdapter.getRealCount() : 0;
        if (count <= 0) {
            return 0;
        }
        int normalizedIndex = Math.floorMod(realIndex, count);
        int aligned = BANNER_LOOP_ANCHOR - (BANNER_LOOP_ANCHOR % count);
        return aligned + normalizedIndex;
    }

    private void anchorBannerPositionIfNeeded() {
        if (binding == null || bannerAdapter == null || bannerAdapter.getRealCount() <= 0) {
            return;
        }
        int count = bannerAdapter.getRealCount();
        int pos = binding.vpBanners.getCurrentItem();
        int aligned = BANNER_LOOP_ANCHOR - (BANNER_LOOP_ANCHOR % count);
        int realIndex = Math.floorMod(pos, count);
        if (pos < aligned - count * 20 || pos > aligned + count * 20) {
            binding.vpBanners.setCurrentItem(aligned + realIndex, false);
        }
    }

    private void setupPullToRefresh() {
        homeRefreshLayout = PullToRefreshHelper.wrap(binding.homeScrollView, () -> {
            homeRefreshPending = true;
            homeViewModel.loadHomeData(requireContext());
        });
    }

    public void scrollToProductsSection() {
        if (binding == null) return;
        binding.homeScrollView.post(() -> {
            if (binding == null) return;
            int anchorTop = getViewTopInScrollContent(binding.productAnchor);
            int targetScrollY = Math.max(0, anchorTop - stickyHeaderHeight);
            binding.homeScrollView.smoothScrollTo(0, targetScrollY);
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────

    private void setupHeader() {
        View.OnClickListener aboutUsClick = v -> startActivity(
                new Intent(requireContext(), AboutUsActivity.class));
        binding.imgLogo.setOnClickListener(aboutUsClick);
        binding.stickyHeader.imgStickyLogo.setOnClickListener(aboutUsClick);

        View.OnClickListener openCategory = v -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).openCategoryScreen();
        };
        binding.btnMenuCategory.setOnClickListener(openCategory);
        binding.stickyHeader.btnStickyMenuCategory.setOnClickListener(openCategory);

        View.OnClickListener openSearchClick = v -> {
            Intent intent = new Intent(requireContext(), com.veggo.app.presentation.search.SearchActivity.class);
            startActivity(intent);
        };
        View.OnClickListener openSearchWithVoice = v -> {
            Intent intent = new Intent(requireContext(), com.veggo.app.presentation.search.SearchActivity.class);
            intent.putExtra(com.veggo.app.presentation.search.SearchActivity.EXTRA_START_VOICE, true);
            startActivity(intent);
        };
        binding.layoutSearch.setOnClickListener(openSearchClick);
        binding.stickyHeader.layoutStickySearch.setOnClickListener(openSearchClick);
        if (binding.stickyHeader.layoutStickySearch.findViewById(R.id.edtSearch) != null) {
            binding.stickyHeader.layoutStickySearch.findViewById(R.id.edtSearch).setFocusable(false);
            binding.stickyHeader.layoutStickySearch.findViewById(R.id.edtSearch).setOnClickListener(openSearchClick);
        }

        View homeMic = binding.layoutSearch.findViewById(R.id.btnHomeMic);
        if (homeMic != null) {
            homeMic.setOnClickListener(openSearchWithVoice);
        }
        View stickyMic = binding.stickyHeader.getRoot().findViewById(R.id.btnStickyMic);
        if (stickyMic != null) {
            stickyMic.setOnClickListener(openSearchWithVoice);
        }

        binding.btnChatbot.setOnClickListener(v -> startActivity(
                new Intent(requireContext(), com.veggo.app.presentation.chatbot.ChatbotActivity.class)));
        setupDraggableChatbot();
    }

    private void setupDraggableChatbot() {
        View fab = binding.btnChatbot;
        fab.post(() -> {
            if (binding == null || fab.getParent() == null) {
                return;
            }
            restoreChatbotPosition(fab);
            attachChatbotDragBehavior(fab);
        });
    }

    private void attachChatbotDragBehavior(View fab) {
        final float touchSlop = 8f * getResources().getDisplayMetrics().density;
        final float edgeMargin = 16f * getResources().getDisplayMetrics().density;
        final float bottomSafeMargin = 96f * getResources().getDisplayMetrics().density;
        final int[] parentLocation = new int[2];

        fab.setOnTouchListener((view, event) -> {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent == null) {
                return false;
            }

            parent.getLocationOnScreen(parentLocation);
            float localX = event.getRawX() - parentLocation[0];
            float localY = event.getRawY() - parentLocation[1];

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    chatbotDragging = false;
                    chatbotDragOffsetX = localX - view.getX();
                    chatbotDragOffsetY = localY - view.getY();
                    chatbotDownRawX = event.getRawX();
                    chatbotDownRawY = event.getRawY();
                    view.setPressed(true);
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = Math.abs(event.getRawX() - chatbotDownRawX);
                    float deltaY = Math.abs(event.getRawY() - chatbotDownRawY);
                    if (!chatbotDragging && (deltaX > touchSlop || deltaY > touchSlop)) {
                        chatbotDragging = true;
                        view.setPressed(false);
                    }
                    if (chatbotDragging) {
                        view.setX(clampChatbotX(view, parent, localX - chatbotDragOffsetX, edgeMargin));
                        view.setY(clampChatbotY(view, parent, localY - chatbotDragOffsetY, bottomSafeMargin));
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.setPressed(false);
                    if (chatbotDragging) {
                        snapChatbotToEdge(view, parent, edgeMargin);
                        chatbotDragging = false;
                        return true;
                    }
                    view.performClick();
                    return true;

                default:
                    return false;
            }
        });
    }

    private float clampChatbotX(View fab, ViewGroup parent, float x, float edgeMargin) {
        float minX = edgeMargin;
        float maxX = parent.getWidth() - fab.getWidth() - edgeMargin;
        if (maxX < minX) {
            maxX = minX;
        }
        return Math.max(minX, Math.min(x, maxX));
    }

    private float clampChatbotY(View fab, ViewGroup parent, float y, float bottomSafeMargin) {
        float minY = edgeMarginFromTop();
        float maxY = parent.getHeight() - fab.getHeight() - bottomSafeMargin;
        if (maxY < minY) {
            maxY = minY;
        }
        return Math.max(minY, Math.min(y, maxY));
    }

    private float edgeMarginFromTop() {
        return 12f * getResources().getDisplayMetrics().density;
    }

    private void snapChatbotToEdge(View fab, ViewGroup parent, float edgeMargin) {
        float centerX = fab.getX() + fab.getWidth() / 2f;
        float targetX = centerX < parent.getWidth() / 2f
                ? edgeMargin
                : parent.getWidth() - fab.getWidth() - edgeMargin;
        fab.animate()
                .x(targetX)
                .setDuration(180L)
                .withEndAction(() -> saveChatbotPosition(fab))
                .start();
    }

    private void restoreChatbotPosition(View fab) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_CHATBOT_FAB, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_CHATBOT_FAB_X) || !prefs.contains(KEY_CHATBOT_FAB_Y)) {
            return;
        }
        ViewGroup parent = (ViewGroup) fab.getParent();
        if (parent == null) {
            return;
        }
        float edgeMargin = 16f * getResources().getDisplayMetrics().density;
        float bottomSafeMargin = 96f * getResources().getDisplayMetrics().density;
        fab.setX(clampChatbotX(fab, parent, prefs.getFloat(KEY_CHATBOT_FAB_X, fab.getX()), edgeMargin));
        fab.setY(clampChatbotY(fab, parent, prefs.getFloat(KEY_CHATBOT_FAB_Y, fab.getY()), bottomSafeMargin));
    }

    private void saveChatbotPosition(View fab) {
        requireContext()
                .getSharedPreferences(PREF_CHATBOT_FAB, Context.MODE_PRIVATE)
                .edit()
                .putFloat(KEY_CHATBOT_FAB_X, fab.getX())
                .putFloat(KEY_CHATBOT_FAB_Y, fab.getY())
                .apply();
    }

    private void setupRecyclerViews() {
        // Banners
        bannerAdapter = new BannerAdapter();
        binding.vpBanners.setOffscreenPageLimit(1);
        binding.vpBanners.setAdapter(bannerAdapter);
        bannerAdapter.setOnBannerClickListener(banner -> {
            Intent intent = new Intent(requireContext(), com.veggo.app.presentation.promotion.PromotionDetailActivity.class);
            intent.putExtra(com.veggo.app.presentation.promotion.PromotionDetailActivity.EXTRA_PROMOTION_ID, banner.getId());
            startActivity(intent);
        });
        binding.vpBanners.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (bannerAdapter.getRealCount() > 0) {
                    updateBannerIndicators(Math.floorMod(position, bannerAdapter.getRealCount()));
                }
                bannerHandler.removeCallbacks(bannerRunnable);
                bannerHandler.postDelayed(bannerRunnable, 10000);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    anchorBannerPositionIfNeeded();
                }
            }
        });

        // Utilities
        utilityAdapter = new UtilityAdapter();
        binding.rvUtilities.setAdapter(utilityAdapter);
        utilityAdapter.setOnUtilityClickListener(utility -> {
            Intent intent = null;
            switch (utility.getId()) {
                case "1":
                    if (!new AppPreferences(requireContext()).isLoggedIn()) {
                        LoginRequiredActivity.open(requireContext(), "tủ lạnh thông minh");
                        return;
                    }
                    intent = new Intent(requireContext(), com.veggo.app.presentation.profile.SmartFridgeActivity.class);
                    break;
                case "2": intent = new Intent(requireContext(), com.veggo.app.presentation.chatbot.ChatbotActivity.class); break;
                case "3":
                    if (!new AppPreferences(requireContext()).isLoggedIn()) {
                        LoginRequiredActivity.open(requireContext(), "yêu thích");
                        return;
                    }
                    intent = new Intent(requireContext(), com.veggo.app.presentation.profile.FavoritesActivity.class);
                    break;
                case "4":
                    if (!new AppPreferences(requireContext()).isLoggedIn()) {
                        LoginRequiredActivity.open(requireContext(), "điểm carbon");
                        return;
                    }
                    intent = new Intent(requireContext(), com.veggo.app.presentation.profile.CarbonPointsActivity.class);
                    break;
                case "5": intent = new Intent(requireContext(), com.veggo.app.presentation.blog.BlogHomeActivity.class); break;
                case "6":
                    if (!new AppPreferences(requireContext()).isLoggedIn()) {
                        LoginRequiredActivity.open(requireContext(), "ví VeggoPay");
                        return;
                    }
                    intent = new Intent(requireContext(), com.veggo.app.presentation.profile.VeggoPayActivity.class);
                    break;
            }
            if (intent != null) startActivity(intent);
        });

        // Categories
        categoryAdapter = new CategoryAdapter();
        binding.rvCategories.setAdapter(categoryAdapter);
        categoryAdapter.setOnCategoryClickListener(category -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).openCategoryDetail(category.getId(), null);
        });
        binding.btnViewMoreCategories.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity)
                ((MainActivity) getActivity()).openCategoryDetail(null, null);
        });

        // Flash Sale
        flashSaleAdapter = new FlashSaleAdapter();
        binding.rvFlashSale.setAdapter(flashSaleAdapter);
        flashSaleAdapter.setOnFlashSaleClickListener(flashSale -> openFlashSaleProduct(flashSale, false));
        flashSaleAdapter.setOnFlashSaleAddClickListener(this::openFlashSaleAddToCart);

        // Recipes
        recipeAdapter = new RecipeAdapter();
        binding.rvRecipes.setAdapter(recipeAdapter);
        recipeAdapter.setOnRecipeClickListener(recipe -> {
            Intent intent = new Intent(requireContext(),
                    com.veggo.app.presentation.community.InstructionRecipeDetailActivity.class);
            intent.putExtra(
                    com.veggo.app.presentation.community.InstructionRecipeDetailActivity.EXTRA_INSTRUCTION_ID,
                    recipe.getId());
            startActivity(intent);
        });

        // Products Grid
        productAdapter = new ProductAdapter();
        binding.rvProducts.setAdapter(productAdapter);
        productAdapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(requireContext(), com.veggo.app.presentation.product.ProductDetailActivity.class);
            intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
            startActivity(intent);
        });
        productAdapter.setOnAddProductClickListener(product ->
                com.veggo.app.presentation.product.AddToCartBottomSheetHelper.show(
                        requireContext(),
                        product,
                        this::refreshCartBadge
                )
        );

        binding.btnLoadMore.setOnClickListener(v -> homeViewModel.loadMoreProducts());
    }

    private void openFlashSaleProduct(com.veggo.app.domain.model.FlashSale flashSale, boolean openAddToCart) {
        Intent intent = new Intent(requireContext(), com.veggo.app.presentation.product.ProductDetailActivity.class);
        intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_PRODUCT_ID, flashSale.getId());
        intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_FLASH_SALE_PRICE, flashSale.getPrice());
        intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_FLASH_SALE_ORIGINAL_PRICE, flashSale.getOriginalPrice());
        intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_FLASH_SALE_DISCOUNT_LABEL, flashSale.getDiscount());
        intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_OPEN_ADD_TO_CART, openAddToCart);
        startActivity(intent);
    }

    private void openFlashSaleAddToCart(com.veggo.app.domain.model.FlashSale flashSale) {
        if (flashSale == null) {
            return;
        }

        com.veggo.app.domain.model.Product matchedProduct = null;
        for (com.veggo.app.domain.model.Product product : productAdapter.getProducts()) {
            if (flashSale.getId().equals(product.getId())) {
                matchedProduct = product;
                break;
            }
        }

        AddToCartBottomSheetHelper.Options options = AddToCartBottomSheetHelper.Options.forFlashSale(
                flashSale.getPrice(),
                flashSale.getOriginalPrice()
        );

        if (matchedProduct != null) {
            com.veggo.app.domain.model.Product pricedProduct = matchedProduct.withPricing(
                    flashSale.getPrice(),
                    flashSale.getOriginalPrice() > 0 ? flashSale.getOriginalPrice() : matchedProduct.getOriginalPrice()
            );
            AddToCartBottomSheetHelper.show(requireContext(), pricedProduct, options, this::refreshCartBadge);
            return;
        }

        AddToCartBottomSheetHelper.showForProductId(
                requireContext(),
                flashSale.getId(),
                options,
                this::refreshCartBadge
        );
    }

    private void setupBannerIndicators() {
        bannerIndicators = new View[]{
                binding.indicator1, binding.indicator2,
                binding.indicator3, binding.indicator4, binding.indicator5
        };
        updateBannerIndicators(0);
    }

    private void updateBannerIndicators(int position) {
        if (bannerIndicators == null) return;
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < bannerIndicators.length; i++) {
            if (bannerIndicators[i] == null) continue;
            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) bannerIndicators[i].getLayoutParams();
            if (i == position) {
                bannerIndicators[i].setBackgroundResource(R.drawable.bg_indicator_selected);
                p.width = (int) (14 * density); p.height = (int) (4 * density);
            } else {
                bannerIndicators[i].setBackgroundResource(R.drawable.bg_indicator_unselected);
                p.width = (int) (4 * density); p.height = (int) (4 * density);
            }
            bannerIndicators[i].setLayoutParams(p);
        }
    }

    /**
     * Gắn click listener cho inline tabs VÀ sticky tabs — đều gọi ViewModel.selectTab()
     */
    private void setupProductTabs() {
        // Tham chiếu tới HSV để scroll
        inlineTabsHsv = (android.widget.HorizontalScrollView) binding.tabContainer.getParent();
        stickyTabsHsv = (android.widget.HorizontalScrollView) stickyTabsBinding.getRoot().findViewById(R.id.stickyTabsHsv);
        if (stickyTabsHsv == null) {
            // Fallback nếu trong layout_home_sticky_tabs chưa có ID cho HSV
            stickyTabsHsv = (android.widget.HorizontalScrollView) stickyTabsBinding.getRoot().getChildAt(0);
        }

        // Đồng bộ cuộn ngang giữa inline tabs và sticky tabs
        if (inlineTabsHsv != null && stickyTabsHsv != null) {
            inlineTabsHsv.setOnScrollChangeListener((v, scrollX, scrollY, oldX, oldY) -> {
                if (stickyTabsHsv.getScrollX() != scrollX) {
                    stickyTabsHsv.scrollTo(scrollX, 0);
                }
            });
            stickyTabsHsv.setOnScrollChangeListener((v, scrollX, scrollY, oldX, oldY) -> {
                if (inlineTabsHsv.getScrollX() != scrollX) {
                    inlineTabsHsv.scrollTo(scrollX, 0);
                }
            });
        }

        // Inline tabs (trong ScrollView)
        binding.tabPopular.setOnClickListener(v -> homeViewModel.selectTab(HomeViewModel.TAB_POPULAR));
        binding.tabTrending.setOnClickListener(v -> homeViewModel.selectTab(HomeViewModel.TAB_TRENDING));
        binding.tabNewest.setOnClickListener(v -> homeViewModel.selectTab(HomeViewModel.TAB_NEWEST));
        binding.tabTopRated.setOnClickListener(v -> homeViewModel.selectTab(HomeViewModel.TAB_TOP_RATED));

        // Giá: toggle cao→thấp / thấp→cao
        binding.tabBestPriceContainer.setOnClickListener(v -> togglePriceTab());
        binding.tabBestPrice.setOnClickListener(v -> togglePriceTab());
        binding.homePriceArrowGroup.setOnClickListener(v -> togglePriceTab());

        // Sticky tabs (overlay)
        stickyTabsBinding.stickyTabPopular.setOnClickListener(v -> homeViewModel.selectTab(HomeViewModel.TAB_POPULAR));
        stickyTabsBinding.stickyTabTrending.setOnClickListener(v -> homeViewModel.selectTab(HomeViewModel.TAB_TRENDING));
        stickyTabsBinding.stickyTabNewest.setOnClickListener(v -> homeViewModel.selectTab(HomeViewModel.TAB_NEWEST));
        stickyTabsBinding.stickyTabTopRated.setOnClickListener(v -> homeViewModel.selectTab(HomeViewModel.TAB_TOP_RATED));
        
        stickyTabsBinding.stickyTabBestPriceContainer.setOnClickListener(v -> togglePriceTab());
        stickyTabsBinding.stickyTabBestPrice.setOnClickListener(v -> togglePriceTab());
        stickyTabsBinding.stickyPriceArrowGroup.setOnClickListener(v -> togglePriceTab());
    }

    /**
     * Toggle tab Giá:
     *  - Nếu đang là price_desc (giá cao → thấp): chuyển sang price_asc (giá thấp → cao)
     *  - Ngược lại: chuyển sang price_desc (giá cao → thấp)
     */
    private void togglePriceTab() {
        String currentTab = homeViewModel.getCurrentTab().getValue();
        if (HomeViewModel.TAB_PRICE_DESC.equals(currentTab)) {
            homeViewModel.selectTab(HomeViewModel.TAB_PRICE_ASC);
        } else {
            homeViewModel.selectTab(HomeViewModel.TAB_PRICE_DESC);
        }
    }

    /**
     * Xử lý scroll để:
     * 1. Show/hide stickyHeader (header ở top)
     * 2. Sau khi tab bar cuộn qua ngưỡng → show stickyTabs bên dưới stickyHeader
     *    Khi tab bar quay lại vùng hiển thị → ẩn stickyTabs
     */
    private void setupStickyBehavior() {
        // Lấy top của tabContainer (HorizontalScrollView chứa tabs) sau khi layout xong
        binding.tabContainer.post(() -> {
            // getTop() tính trong LinearLayout cha (content của ScrollView)
            // cần cộng thêm top của HorizontalScrollView cha
            View hsv = (View) binding.tabContainer.getParent(); // HorizontalScrollView
            // Lấy vị trí Y tuyệt đối trong ScrollView content
            tabBarTop = getViewTopInScrollContent(hsv);
        });

        binding.homeScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldX, oldY) -> {
            // ── 1. Sticky main header ──────────────────────────────────
            if (scrollY >= stickyHeaderHeight) {
                binding.stickyHeader.getRoot().setVisibility(View.VISIBLE);
            } else {
                binding.stickyHeader.getRoot().setVisibility(View.GONE);
            }

            // ── 2. Sticky tabs ─────────────────────────────────────────
            // Ngưỡng: khi top của tab bar cuộn lên quá vị trí (stickyHeader + stickyTabs)
            // => tabBarTop - scrollY < stickyHeaderHeight
            // Tức là: tab bar gần chạm mép dưới sticky header
            if (tabBarTop != Integer.MAX_VALUE) {
                boolean shouldStick = scrollY > (tabBarTop - stickyHeaderHeight);
                binding.stickyTabs.getRoot().setVisibility(shouldStick ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * Tính vị trí top tuyệt đối của view trong content của ScrollView
     * bằng cách đi ngược lên cây view cho đến khi gặp ScrollView.
     */
    private int getViewTopInScrollContent(View view) {
        int top = 0;
        View current = view;
        while (current != null && current != binding.homeScrollView) {
            top += (int) current.getY();
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else break;
        }
        return top;
    }

    private void setupHeaderActions() {
        View.OnClickListener openCart = v -> ((MainActivity) requireActivity()).openCartScreen();
        View.OnClickListener openNotifications = v -> ((MainActivity) requireActivity()).openNotificationsScreen();

        View cartBtn = binding.getRoot().findViewById(R.id.homeCartButton);
        View notifyBtn = binding.getRoot().findViewById(R.id.homeNotifyButton);
        if (cartBtn != null) cartBtn.setOnClickListener(openCart);
        if (notifyBtn != null) notifyBtn.setOnClickListener(openNotifications);

        View stickyCart = binding.stickyHeader.getRoot().findViewById(R.id.homeStickyCartButton);
        View stickyNotify = binding.stickyHeader.getRoot().findViewById(R.id.homeStickyNotifyButton);
        if (stickyCart != null) stickyCart.setOnClickListener(openCart);
        if (stickyNotify != null) stickyNotify.setOnClickListener(openNotifications);
    }

    private void refreshCartBadge() {
        if (!isAdded() || binding == null) {
            return;
        }
        AppModule.provideCartRepository(requireContext()).getCart(resolveCustomerId()).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                if (!isAdded() || binding == null) {
                    return;
                }
                int count = response.isSuccessful() && response.body() != null
                        ? CartCountUtils.countLineItems(response.body())
                        : 0;
                requireActivity().runOnUiThread(() -> updateCartBadges(count));
            }

            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> updateCartBadges(0));
                }
            }
        });
    }

    private String resolveCustomerId() {
        AppPreferences appPreferences = new AppPreferences(requireContext());
        String customerId = appPreferences.getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            customerId = new PreferencesManager(requireContext()).getUserId();
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            customerId = new PendingCheckoutStore(requireContext()).guestId();
        }
        return customerId;
    }

    private void updateCartBadges(int count) {
        setCartBadge(binding.getRoot().findViewById(R.id.homeCartBadge), count);
        setCartBadge(binding.stickyHeader.getRoot().findViewById(R.id.homeStickyCartBadge), count);
    }

    private void setCartBadge(TextView badge, int count) {
        if (badge == null) {
            return;
        }
        badge.setText(count > 99 ? "99+" : String.valueOf(count));
        badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    private void refreshNotificationState(boolean allowAlert) {
        if (!isAdded() || binding == null) {
            return;
        }
        String customerId = resolveCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            updateNotificationBadgesFromMain(0);
            return;
        }
        orderNotificationRepository.getNotifications(customerId, notifications -> {
            if (!isAdded() || binding == null) {
                return;
            }
            requireActivity().runOnUiThread(() ->
                    updateNotificationBadgesFromMain(OrderNotificationRepository.countUnread(notifications)));
        });
    }

    public void updateNotificationBadgesFromMain(int count) {
        if (binding == null) {
            return;
        }
        setCartBadge(binding.getRoot().findViewById(R.id.homeNotificationBadge), count);
        setCartBadge(binding.stickyHeader.getRoot().findViewById(R.id.homeStickyNotificationBadge), count);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Observe ViewModel
    // ─────────────────────────────────────────────────────────────────────

    private void observeViewModel() {
        homeViewModel.getBanners().observe(getViewLifecycleOwner(), banners -> {
            if (banners == null || banners.isEmpty()) return;

            bannerAdapter.submitList(banners);
            // Post lên main thread để đảm bảo RecyclerView đã measure xong
            binding.vpBanners.post(() -> {
                if (bannerAdapter.getRealCount() == 0) return;
                int realCount = bannerAdapter.getRealCount();
                binding.vpBanners.setCurrentItem(resolveBannerLoopPosition(0), false);
                updateBannerIndicators(0);
                // Tự động chạy timer banner
                bannerHandler.removeCallbacks(bannerRunnable);
                bannerHandler.postDelayed(bannerRunnable, 10000);
            });
        });

        homeViewModel.getUtilities().observe(getViewLifecycleOwner(), u -> {
            if (u != null) utilityAdapter.submitList(u);
        });

        homeViewModel.getCategories().observe(getViewLifecycleOwner(), c -> {
            if (c != null) categoryAdapter.submitList(c);
        });

        homeViewModel.getFlashSales().observe(getViewLifecycleOwner(), fs -> {
            if (fs != null) flashSaleAdapter.submitList(fs);
        });

        homeViewModel.getRecipes().observe(getViewLifecycleOwner(), r -> {
            if (r != null) recipeAdapter.submitList(r);
        });

        // Products từ API
        homeViewModel.getProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                productAdapter.submitList(products);
                // Sau khi list thay đổi, đo lại vị trí tabBar vì chiều cao RecyclerView đổi
                binding.tabContainer.post(() -> {
                    View hsv = (View) binding.tabContainer.getParent();
                    tabBarTop = getViewTopInScrollContent(hsv);
                });
            }
        });

        // Loading indicator
        homeViewModel.getLoadingProducts().observe(getViewLifecycleOwner(), isLoading -> {
            if (binding == null) return;
            if (homeRefreshPending && !Boolean.TRUE.equals(isLoading)) {
                homeRefreshPending = false;
                PullToRefreshHelper.finish(homeRefreshLayout);
            }
            binding.progressProducts.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            // Hide RecyclerView if it's the initial load (products list is empty)
            boolean isInitialLoad = isLoading && productAdapter.getItemCount() == 0;
            binding.rvProducts.setVisibility(isInitialLoad ? View.INVISIBLE : View.VISIBLE);

            // Hide btnLoadMore while loading
            if (isLoading) {
                binding.btnLoadMore.setVisibility(View.GONE);
            } else {
                Boolean isLastPage = homeViewModel.getIsLastPage().getValue();
                binding.btnLoadMore.setVisibility(Boolean.TRUE.equals(isLastPage) ? View.GONE : View.VISIBLE);
            }
        });

        // Last Page state
        homeViewModel.getIsLastPage().observe(getViewLifecycleOwner(), isLastPage -> {
            if (binding == null || Boolean.TRUE.equals(homeViewModel.getLoadingProducts().getValue())) return;
            binding.btnLoadMore.setVisibility(Boolean.TRUE.equals(isLastPage) ? View.GONE : View.VISIBLE);
        });

        // Lỗi
        homeViewModel.getProductError().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.isEmpty())
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show();
        });

        // Tab thay đổi → cập nhật style CẢ inline tabs VÀ sticky tabs
        homeViewModel.getCurrentTab().observe(getViewLifecycleOwner(), tab -> {
            updateTabStyles(tab);
            scrollToActiveTab(tab);

            // Cuộn dọc ScrollView lên phần sản phẩm của tab mới nếu đang ở dưới
            if (binding != null) {
                int anchorTop = getViewTopInScrollContent(binding.productAnchor);
                int density = (int) getResources().getDisplayMetrics().density;
                int stickyTabsHeight = (int) (48 * density);
                int targetScrollY = anchorTop - (stickyHeaderHeight + stickyTabsHeight);

                if (tabBarTop != Integer.MAX_VALUE) {
                    int threshold = tabBarTop - stickyHeaderHeight;
                    if (binding.homeScrollView.getScrollY() > threshold) {
                        binding.homeScrollView.scrollTo(0, targetScrollY);
                    }
                }
            }
        });
    }

    /**
     * Tự động cuộn HorizontalScrollView đến tab đang được chọn.
     * Đặc biệt khi chọn "Đánh giá cao", cuộn đến cuối.
     */
    private void scrollToActiveTab(String activeTab) {
        if (activeTab == null) return;

        View activeView = null;
        View stickyActiveView = null;
        switch (activeTab) {
            case HomeViewModel.TAB_POPULAR:
                activeView = binding.tabPopular;
                stickyActiveView = stickyTabsBinding.stickyTabPopular;
                break;
            case HomeViewModel.TAB_TRENDING:
                activeView = binding.tabTrending;
                stickyActiveView = stickyTabsBinding.stickyTabTrending;
                break;
            case HomeViewModel.TAB_NEWEST:
                activeView = binding.tabNewest;
                stickyActiveView = stickyTabsBinding.stickyTabNewest;
                break;
            case HomeViewModel.TAB_PRICE_DESC:
            case HomeViewModel.TAB_PRICE_ASC:
                activeView = binding.tabBestPrice;
                stickyActiveView = stickyTabsBinding.stickyTabBestPrice;
                break;
            case HomeViewModel.TAB_TOP_RATED:
                activeView = binding.tabTopRated;
                stickyActiveView = stickyTabsBinding.stickyTabTopRated;
                break;
        }

        if (activeView != null && inlineTabsHsv != null) {
            final View inlineParent = (View) activeView.getParent();
            inlineTabsHsv.post(() -> {
                int scrollX = inlineParent.getLeft() - (inlineTabsHsv.getWidth() / 2) + (inlineParent.getWidth() / 2);
                inlineTabsHsv.smoothScrollTo(scrollX, 0);
            });
        }

        if (stickyActiveView != null && stickyTabsHsv != null) {
            final View stickyParent = (View) stickyActiveView.getParent();
            stickyTabsHsv.post(() -> {
                int scrollX = stickyParent.getLeft() - (stickyTabsHsv.getWidth() / 2) + (stickyParent.getWidth() / 2);
                stickyTabsHsv.smoothScrollTo(scrollX, 0);
            });
        }
    }

    /**
     * Cập nhật style active/inactive đồng thời cho inline tabs và sticky tabs.
     */
    private void updateTabStyles(String activeTab) {
        if (binding == null || activeTab == null) return;

        boolean isPriceTab = HomeViewModel.TAB_PRICE_DESC.equals(activeTab) || HomeViewModel.TAB_PRICE_ASC.equals(activeTab);

        // Inline tabs (không tính Giá tốt, xử lý riêng bên dưới)
        TextView[] inlineTabs = {
                binding.tabPopular, binding.tabTrending, binding.tabNewest,
                null /* bestPrice handled separately */, binding.tabTopRated
        };
        View[] inlineIndicators = {
                binding.tabPopularIndicator, binding.tabTrendingIndicator, binding.tabNewestIndicator,
                binding.tabBestPriceIndicator, binding.tabTopRatedIndicator
        };

        // Sticky tabs
        TextView[] stickyTabs = {
                stickyTabsBinding.stickyTabPopular, stickyTabsBinding.stickyTabTrending,
                stickyTabsBinding.stickyTabNewest, null /* bestPrice */, stickyTabsBinding.stickyTabTopRated
        };
        View[] stickyIndicators = {
                stickyTabsBinding.stickyTabPopularIndicator, stickyTabsBinding.stickyTabTrendingIndicator,
                stickyTabsBinding.stickyTabNewestIndicator, stickyTabsBinding.stickyTabBestPriceIndicator,
                stickyTabsBinding.stickyTabTopRatedIndicator
        };

        for (int i = 0; i < TAB_KEYS.length; i++) {
            boolean isActive = TAB_KEYS[i].equals(activeTab);

            int color = androidx.core.content.ContextCompat.getColor(requireContext(),
                    isActive ? R.color.primary_main : R.color.neutral_60);
            android.graphics.Typeface typeface = isActive
                    ? androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_semibold)
                    : androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_regular);
            int indicatorVis = isActive ? View.VISIBLE : View.INVISIBLE;

            // Inline (skip null = bestPrice)
            if (inlineTabs[i] != null) {
                inlineTabs[i].setTextColor(color);
                inlineTabs[i].setTypeface(typeface);
            }
            inlineIndicators[i].setVisibility(indicatorVis);

            // Sticky (skip null = bestPrice)
            if (stickyTabs[i] != null) {
                stickyTabs[i].setTextColor(color);
                stickyTabs[i].setTypeface(typeface);
            }
            stickyIndicators[i].setVisibility(indicatorVis);
        }

        // ── Xử lý riêng tab Giá tốt và các mũi tên ────────────────────────────
        int priceColor = androidx.core.content.ContextCompat.getColor(requireContext(),
                isPriceTab ? R.color.primary_main : R.color.neutral_60);
        android.graphics.Typeface priceTf = isPriceTab
                ? androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_semibold)
                : androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_regular);
        binding.tabBestPrice.setTextColor(priceColor);
        binding.tabBestPrice.setTypeface(priceTf);
        stickyTabsBinding.stickyTabBestPrice.setTextColor(priceColor);
        stickyTabsBinding.stickyTabBestPrice.setTypeface(priceTf);

        binding.tabBestPriceIndicator.setVisibility(isPriceTab ? View.VISIBLE : View.INVISIBLE);
        stickyTabsBinding.stickyTabBestPriceIndicator.setVisibility(isPriceTab ? View.VISIBLE : View.INVISIBLE);

        // Cập nhật mũi tên inline
        android.widget.ImageView inlineUp   = binding.getRoot().findViewById(R.id.homeImgPriceArrowUp);
        android.widget.ImageView inlineDown = binding.getRoot().findViewById(R.id.homeImgPriceArrowDown);
        // Cập nhật mũi tên sticky
        android.widget.ImageView stickyUp   = stickyTabsBinding.getRoot().findViewById(R.id.stickyImgPriceArrowUp);
        android.widget.ImageView stickyDown = stickyTabsBinding.getRoot().findViewById(R.id.stickyImgPriceArrowDown);

        if (inlineUp != null && inlineDown != null) {
            if (!isPriceTab) {
                // Cả hai mũi tên, màu xám
                inlineUp.setVisibility(View.VISIBLE);
                inlineDown.setVisibility(View.VISIBLE);
                inlineUp.setColorFilter(priceColor);
                inlineDown.setColorFilter(priceColor);
            } else if (HomeViewModel.TAB_PRICE_DESC.equals(activeTab)) {
                // Giá cao→thấp: chỉ mũi tên ↓
                inlineUp.setVisibility(View.GONE);
                inlineDown.setVisibility(View.VISIBLE);
                inlineDown.setColorFilter(priceColor);
            } else {
                // Giá thấp→cao: chỉ mũi tên ↑
                inlineUp.setVisibility(View.VISIBLE);
                inlineDown.setVisibility(View.GONE);
                inlineUp.setColorFilter(priceColor);
            }
        }
        if (stickyUp != null && stickyDown != null) {
            if (!isPriceTab) {
                stickyUp.setVisibility(View.VISIBLE);
                stickyDown.setVisibility(View.VISIBLE);
                stickyUp.setColorFilter(priceColor);
                stickyDown.setColorFilter(priceColor);
            } else if (HomeViewModel.TAB_PRICE_DESC.equals(activeTab)) {
                stickyUp.setVisibility(View.GONE);
                stickyDown.setVisibility(View.VISIBLE);
                stickyDown.setColorFilter(priceColor);
            } else {
                stickyUp.setVisibility(View.VISIBLE);
                stickyDown.setVisibility(View.GONE);
                stickyUp.setColorFilter(priceColor);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        refreshCartBadge();
        if (bannerAdapter != null && bannerAdapter.getRealCount() > 0)
            bannerHandler.postDelayed(bannerRunnable, 10000);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshCartBadge();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    @Override
    public void onDestroyView() {
        if (binding != null && binding.homeScrollView != null) {
            savedScrollY = binding.homeScrollView.getScrollY();
        }
        super.onDestroyView();
        binding = null;
        stickyTabsBinding = null;
    }
}
