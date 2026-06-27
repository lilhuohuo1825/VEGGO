package com.veggo.app.presentation.promotion;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.core.utils.Constants;
import com.veggo.app.data.remote.api.ProductApi;
import com.veggo.app.data.remote.api.PromotionApi;
import com.veggo.app.data.remote.dto.PromotionDto;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ScrollView;

import com.veggo.app.adapter.ProductAdapter;
import com.veggo.app.data.mapper.ProductMapper;
import com.veggo.app.data.remote.dto.HomeProductResponse;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.domain.model.Product;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PromotionDetailActivity extends BaseActivity {

    public static final String EXTRA_PROMOTION_ID = "extra_promotion_id";

    // Tab keys
    private static final String TAB_POPULAR    = "popular";
    private static final String TAB_TRENDING   = "trending";
    private static final String TAB_NEWEST     = "newest";
    private static final String TAB_PRICE_DESC = "price_desc"; // giá cao → thấp, mũi tên ↓
    private static final String TAB_PRICE_ASC  = "price_asc";  // giá thấp → cao, mũi tên ↑
    private static final String TAB_TOP_RATED  = "top_rated";

    private static final int PAGE_SIZE = 24;

    // ─── Views ───────────────────────────────────────────────────────────
    private ImageView imgBanner;
    private TextView tvTitle, tvCode, tvDescription;
    private TextView tvDiscount, tvMinOrder;
    private TextView tvTime, tvUsage, tvLimitPerUser;
    private RecyclerView rvPromotionProducts;
    private TextView tvProductGridTitle;
    private LinearLayout btnLoadMore;
    private ProductAdapter productAdapter;
    private androidx.core.widget.NestedScrollView scrollView;

    // Tabs (only for all-product promos)
    private View layoutProductTabs;
    private android.widget.HorizontalScrollView tabHsv;
    private TextView tabPopular, tabTrending, tabNewest, tabPrice, tabTopRated;
    private View tabPriceContainer;
    private LinearLayout priceArrowGroup;
    private ImageView imgPriceArrowUp, imgPriceArrowDown;
    private View tabPopularIndicator, tabTrendingIndicator, tabNewestIndicator, tabPriceIndicator, tabTopRatedIndicator;

    // Sticky Tabs (floating overlay)
    private View stickyProductTabs;
    private android.widget.HorizontalScrollView stickyTabHsv;
    private TextView stickyTabPopular, stickyTabTrending, stickyTabNewest, stickyTabPrice, stickyTabTopRated;
    private View stickyTabPriceContainer;
    private LinearLayout stickyPriceArrowGroup;
    private ImageView stickyImgPriceArrowUp, stickyImgPriceArrowDown;
    private View stickyTabPopularIndicator, stickyTabTrendingIndicator, stickyTabNewestIndicator, stickyTabPriceIndicator, stickyTabTopRatedIndicator;

    private int tabBarTop = Integer.MAX_VALUE;
    private int headerHeight;

    // ─── State ───────────────────────────────────────────────────────────
    private String promotionId;

    /**
     * true  → PROMO010 / PROMO011: applies to ALL products, shows tabs + pagination (24/page)
     * false → PROMO012 / PROMO013 / PROMO014: filtered products, no tabs, show all at once
     */
    private boolean isAllProductsPromo = false;

    private String currentTab = TAB_POPULAR;
    private List<Product> allLoadedProducts = new ArrayList<>();
    private int currentSkip = 0;
    private boolean isLastPage = false;

    // ─────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion_detail);

        initViews();

        promotionId = getIntent().getStringExtra(EXTRA_PROMOTION_ID);
        if (promotionId != null && !promotionId.isEmpty()) {
            // PROMO010 / PROMO011 → all products + tabs + pagination
            isAllProductsPromo = "PROMO010".equals(promotionId) || "PROMO011".equals(promotionId);
            fetchPromotionDetail(promotionId);
            fetchPromotionProducts();
        } else {
            Toast.makeText(this, "Không tìm thấy mã khuyến mãi", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        ImageView imgBack = findViewById(R.id.imgBack);
        imgBack.setOnClickListener(v -> finish());

        scrollView             = findViewById(R.id.scrollView);
        rvPromotionProducts    = findViewById(R.id.rvPromotionProducts);
        tvProductGridTitle     = findViewById(R.id.tvProductGridTitle);
        btnLoadMore            = findViewById(R.id.btnLoadMoreContainer);

        productAdapter = new ProductAdapter();
        rvPromotionProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvPromotionProducts.setAdapter(productAdapter);
        productAdapter.setOnProductClickListener(product -> {
            android.content.Intent intent = new android.content.Intent(
                    PromotionDetailActivity.this,
                    com.veggo.app.presentation.product.ProductDetailActivity.class);
            intent.putExtra(
                    com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_PRODUCT_ID,
                    product.getId());
            startActivity(intent);
        });

        btnLoadMore.setOnClickListener(v -> loadMoreProducts());

        // Tabs
        layoutProductTabs    = findViewById(R.id.layoutProductTabs);
        View includedTabs    = findViewById(R.id.includedProductTabs);
        if (includedTabs != null) {
            includedTabs.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            includedTabs.setElevation(0f);
            if (includedTabs.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) includedTabs.getLayoutParams();
                lp.topMargin = 0;
                includedTabs.setLayoutParams(lp);
            }
        }
        
        // Inline views
        tabHsv               = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabsHsv) : findViewById(R.id.stickyTabsHsv);
        tabPopular           = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabPopular) : findViewById(R.id.stickyTabPopular);
        tabTrending          = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabTrending) : findViewById(R.id.stickyTabTrending);
        tabNewest            = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabNewest) : findViewById(R.id.stickyTabNewest);
        tabPrice             = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabBestPrice) : findViewById(R.id.stickyTabBestPrice);
        tabTopRated          = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabTopRated) : findViewById(R.id.stickyTabTopRated);
        tabPriceContainer    = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabBestPriceContainer) : findViewById(R.id.stickyTabBestPriceContainer);
        priceArrowGroup      = includedTabs != null ? includedTabs.findViewById(R.id.stickyPriceArrowGroup) : findViewById(R.id.stickyPriceArrowGroup);
        imgPriceArrowUp      = includedTabs != null ? includedTabs.findViewById(R.id.stickyImgPriceArrowUp) : findViewById(R.id.stickyImgPriceArrowUp);
        imgPriceArrowDown    = includedTabs != null ? includedTabs.findViewById(R.id.stickyImgPriceArrowDown) : findViewById(R.id.stickyImgPriceArrowDown);
        tabPopularIndicator  = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabPopularIndicator) : findViewById(R.id.stickyTabPopularIndicator);
        tabTrendingIndicator = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabTrendingIndicator) : findViewById(R.id.stickyTabTrendingIndicator);
        tabNewestIndicator   = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabNewestIndicator) : findViewById(R.id.stickyTabNewestIndicator);
        tabPriceIndicator    = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabBestPriceIndicator) : findViewById(R.id.stickyTabBestPriceIndicator);
        tabTopRatedIndicator = includedTabs != null ? includedTabs.findViewById(R.id.stickyTabTopRatedIndicator) : findViewById(R.id.stickyTabTopRatedIndicator);

        tabPopular.setOnClickListener(v  -> selectTab(TAB_POPULAR));
        tabTrending.setOnClickListener(v -> selectTab(TAB_TRENDING));
        tabNewest.setOnClickListener(v   -> selectTab(TAB_NEWEST));
        tabTopRated.setOnClickListener(v -> selectTab(TAB_TOP_RATED));
        
        View.OnClickListener priceToggle = v -> {
            // Toggle: inactive / price_desc → price_asc → price_desc …
            if (TAB_PRICE_DESC.equals(currentTab)) {
                selectTab(TAB_PRICE_ASC);
            } else {
                selectTab(TAB_PRICE_DESC);
            }
        };
        tabPriceContainer.setOnClickListener(priceToggle);
        tabPrice.setOnClickListener(priceToggle);
        if (priceArrowGroup != null) {
            priceArrowGroup.setOnClickListener(priceToggle);
        }

        headerHeight = (int) (56 * getResources().getDisplayMetrics().density);

        // Sticky Tabs (floating overlay)
        stickyProductTabs          = findViewById(R.id.stickyProductTabs);
        stickyTabHsv               = stickyProductTabs.findViewById(R.id.stickyTabsHsv);
        stickyTabPopular           = stickyProductTabs.findViewById(R.id.stickyTabPopular);
        stickyTabTrending          = stickyProductTabs.findViewById(R.id.stickyTabTrending);
        stickyTabNewest            = stickyProductTabs.findViewById(R.id.stickyTabNewest);
        stickyTabPrice             = stickyProductTabs.findViewById(R.id.stickyTabBestPrice);
        stickyTabTopRated          = stickyProductTabs.findViewById(R.id.stickyTabTopRated);
        stickyTabPriceContainer    = stickyProductTabs.findViewById(R.id.stickyTabBestPriceContainer);
        stickyPriceArrowGroup      = stickyProductTabs.findViewById(R.id.stickyPriceArrowGroup);
        stickyImgPriceArrowUp      = stickyProductTabs.findViewById(R.id.stickyImgPriceArrowUp);
        stickyImgPriceArrowDown    = stickyProductTabs.findViewById(R.id.stickyImgPriceArrowDown);
        stickyTabPopularIndicator  = stickyProductTabs.findViewById(R.id.stickyTabPopularIndicator);
        stickyTabTrendingIndicator = stickyProductTabs.findViewById(R.id.stickyTabTrendingIndicator);
        stickyTabNewestIndicator   = stickyProductTabs.findViewById(R.id.stickyTabNewestIndicator);
        stickyTabPriceIndicator    = stickyProductTabs.findViewById(R.id.stickyTabBestPriceIndicator);
        stickyTabTopRatedIndicator = stickyProductTabs.findViewById(R.id.stickyTabTopRatedIndicator);

        // Bind click listeners for sticky tabs
        stickyTabPopular.setOnClickListener(v  -> selectTab(TAB_POPULAR));
        stickyTabTrending.setOnClickListener(v -> selectTab(TAB_TRENDING));
        stickyTabNewest.setOnClickListener(v   -> selectTab(TAB_NEWEST));
        stickyTabTopRated.setOnClickListener(v -> selectTab(TAB_TOP_RATED));
        stickyTabPriceContainer.setOnClickListener(priceToggle);
        stickyTabPrice.setOnClickListener(priceToggle);
        if (stickyPriceArrowGroup != null) {
            stickyPriceArrowGroup.setOnClickListener(priceToggle);
        }

        // Horizontal scroll synchronization
        if (tabHsv != null && stickyTabHsv != null) {
            tabHsv.setOnScrollChangeListener((v, scrollX, scrollY, oldX, oldY) -> {
                if (stickyTabHsv.getScrollX() != scrollX) {
                    stickyTabHsv.scrollTo(scrollX, 0);
                }
            });
            stickyTabHsv.setOnScrollChangeListener((v, scrollX, scrollY, oldX, oldY) -> {
                if (tabHsv.getScrollX() != scrollX) {
                    tabHsv.scrollTo(scrollX, 0);
                }
            });
        }

        // Calculate tabBarTop (inline tabs top position)
        layoutProductTabs.post(() -> {
            tabBarTop = getViewTopInScrollContent(layoutProductTabs);
        });

        // Vertical scroll listener to toggle visibility of stickyProductTabs
        scrollView.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldX, oldY) -> {
            if (isAllProductsPromo && tabBarTop != Integer.MAX_VALUE) {
                boolean shouldStick = scrollY > tabBarTop;
                stickyProductTabs.setVisibility(shouldStick ? View.VISIBLE : View.GONE);
            }
        });

        imgBanner      = findViewById(R.id.imgBanner);
        tvTitle        = findViewById(R.id.tvTitle);
        tvCode         = findViewById(R.id.tvCode);
        tvDescription  = findViewById(R.id.tvDescription);
        tvDiscount     = findViewById(R.id.tvDiscount);
        tvMinOrder     = findViewById(R.id.tvMinOrder);
        tvTime         = findViewById(R.id.tvTime);
        tvUsage        = findViewById(R.id.tvUsage);
        tvLimitPerUser = findViewById(R.id.tvLimitPerUser);

        ImageView btnCopyCode = findViewById(R.id.btnCopyCode);
        if (btnCopyCode != null) {
            btnCopyCode.post(() -> {
                android.view.ViewGroup.LayoutParams params = btnCopyCode.getLayoutParams();
                params.width = btnCopyCode.getHeight();
                btnCopyCode.setLayoutParams(params);
            });
            btnCopyCode.setOnClickListener(v -> {
                if (tvCode != null) {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Promotion Code", tvCode.getText().toString());
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(PromotionDetailActivity.this, "Đã sao chép mã khuyến mãi thành công!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Tab logic
    // ─────────────────────────────────────────────────────────────────────

    private void selectTab(String tab) {
        if (currentTab.equals(tab) && !(TAB_PRICE_DESC.equals(tab) || TAB_PRICE_ASC.equals(tab))) {
            return;
        }
        currentTab = tab;
        updateTabStyles();
        scrollToActiveTab(tab);
        
        if (isAllProductsPromo) {
            View anchor = findViewById(R.id.productAnchor);
            if (anchor != null) {
                int anchorTop = getViewTopInScrollContent(anchor);
                int density = (int) getResources().getDisplayMetrics().density;
                int stickyTabsHeight = (int) (48 * density);
                int targetScrollY = anchorTop - stickyTabsHeight;
                
                if (tabBarTop != Integer.MAX_VALUE) {
                    if (scrollView.getScrollY() > tabBarTop) {
                        scrollView.scrollTo(0, targetScrollY);
                    }
                }
            }
            
            currentSkip = 0;
            isLastPage = false;
            fetchPromotionProductsFromApi(true);
        }
    }

    private View getTabContainerParent(View activeView) {
        if (activeView == null) return null;
        View parent = (View) activeView.getParent();
        if (parent != null && parent.getId() == R.id.stickyTabBestPriceContainer) {
            parent = (View) parent.getParent();
        }
        return parent;
    }

    private void scrollToActiveTab(String activeTab) {
        if (activeTab == null) return;

        View activeView = null;
        View stickyActiveView = null;
        switch (activeTab) {
            case TAB_POPULAR:
                activeView = tabPopular;
                stickyActiveView = stickyTabPopular;
                break;
            case TAB_TRENDING:
                activeView = tabTrending;
                stickyActiveView = stickyTabTrending;
                break;
            case TAB_NEWEST:
                activeView = tabNewest;
                stickyActiveView = stickyTabNewest;
                break;
            case TAB_PRICE_DESC:
            case TAB_PRICE_ASC:
                activeView = tabPrice;
                stickyActiveView = stickyTabPrice;
                break;
            case TAB_TOP_RATED:
                activeView = tabTopRated;
                stickyActiveView = stickyTabTopRated;
                break;
        }

        if (activeView != null && tabHsv != null) {
            final View parentView = getTabContainerParent(activeView);
            if (parentView != null) {
                tabHsv.post(() -> {
                    int scrollX = parentView.getLeft() - (tabHsv.getWidth() / 2) + (parentView.getWidth() / 2);
                    tabHsv.smoothScrollTo(scrollX, 0);
                });
            }
        }

        if (stickyActiveView != null && stickyTabHsv != null) {
            final View stickyParentView = getTabContainerParent(stickyActiveView);
            if (stickyParentView != null) {
                stickyTabHsv.post(() -> {
                    int scrollX = stickyParentView.getLeft() - (stickyTabHsv.getWidth() / 2) + (stickyParentView.getWidth() / 2);
                    stickyTabHsv.smoothScrollTo(scrollX, 0);
                });
            }
        }
    }

    private void updateTabStyles() {
        boolean isPriceTab = TAB_PRICE_DESC.equals(currentTab) || TAB_PRICE_ASC.equals(currentTab);

        // ── Non-price tabs ───────────────────────────────────────────────
        String[]   tabKeys  = { TAB_POPULAR,    TAB_TRENDING,    TAB_NEWEST,    TAB_TOP_RATED };
        
        // Inline
        TextView[] tabViews = { tabPopular,      tabTrending,     tabNewest,     tabTopRated };
        View[]     indics   = { tabPopularIndicator, tabTrendingIndicator, tabNewestIndicator, tabTopRatedIndicator };
        
        // Sticky
        TextView[] stickyTabViews = { stickyTabPopular, stickyTabTrending, stickyTabNewest, stickyTabTopRated };
        View[]     stickyIndics   = { stickyTabPopularIndicator, stickyTabTrendingIndicator, stickyTabNewestIndicator, stickyTabTopRatedIndicator };

        for (int i = 0; i < tabKeys.length; i++) {
            boolean isActive = tabKeys[i].equals(currentTab);
            int color = ContextCompat.getColor(this, isActive ? R.color.primary_main : R.color.neutral_60);
            android.graphics.Typeface tf = isActive
                    ? ResourcesCompat.getFont(this, R.font.inter_semibold)
                    : ResourcesCompat.getFont(this, R.font.inter_regular);
            
            if (tabViews[i] != null) {
                tabViews[i].setTextColor(color);
                tabViews[i].setTypeface(tf);
            }
            if (indics[i] != null) {
                indics[i].setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
            }
            
            if (stickyTabViews[i] != null) {
                stickyTabViews[i].setTextColor(color);
                stickyTabViews[i].setTypeface(tf);
            }
            if (stickyIndics[i] != null) {
                stickyIndics[i].setVisibility(isActive ? View.VISIBLE : View.INVISIBLE);
            }
        }

        // ── Price tab ────────────────────────────────────────────────────
        int priceColor = ContextCompat.getColor(this, isPriceTab ? R.color.primary_main : R.color.neutral_60);
        android.graphics.Typeface priceTf = isPriceTab
                ? ResourcesCompat.getFont(this, R.font.inter_semibold)
                : ResourcesCompat.getFont(this, R.font.inter_regular);
                
        if (tabPrice != null) {
            tabPrice.setTextColor(priceColor);
            tabPrice.setTypeface(priceTf);
        }
        if (tabPriceIndicator != null) {
            tabPriceIndicator.setVisibility(isPriceTab ? View.VISIBLE : View.INVISIBLE);
        }
        
        if (stickyTabPrice != null) {
            stickyTabPrice.setTextColor(priceColor);
            stickyTabPrice.setTypeface(priceTf);
        }
        if (stickyTabPriceIndicator != null) {
            stickyTabPriceIndicator.setVisibility(isPriceTab ? View.VISIBLE : View.INVISIBLE);
        }

        // Arrows update
        updatePriceArrows(isPriceTab, priceColor);
    }

    private void updatePriceArrows(boolean isPriceTab, int priceColor) {
        if (imgPriceArrowUp != null && imgPriceArrowDown != null) {
            if (!isPriceTab) {
                imgPriceArrowUp.setVisibility(View.VISIBLE);
                imgPriceArrowDown.setVisibility(View.VISIBLE);
                imgPriceArrowUp.setColorFilter(priceColor);
                imgPriceArrowDown.setColorFilter(priceColor);
            } else if (TAB_PRICE_DESC.equals(currentTab)) {
                imgPriceArrowUp.setVisibility(View.GONE);
                imgPriceArrowDown.setVisibility(View.VISIBLE);
                imgPriceArrowDown.setColorFilter(priceColor);
            } else {
                imgPriceArrowUp.setVisibility(View.VISIBLE);
                imgPriceArrowDown.setVisibility(View.GONE);
                imgPriceArrowUp.setColorFilter(priceColor);
            }
        }
        
        if (stickyImgPriceArrowUp != null && stickyImgPriceArrowDown != null) {
            if (!isPriceTab) {
                stickyImgPriceArrowUp.setVisibility(View.VISIBLE);
                stickyImgPriceArrowDown.setVisibility(View.VISIBLE);
                stickyImgPriceArrowUp.setColorFilter(priceColor);
                stickyImgPriceArrowDown.setColorFilter(priceColor);
            } else if (TAB_PRICE_DESC.equals(currentTab)) {
                stickyImgPriceArrowUp.setVisibility(View.GONE);
                stickyImgPriceArrowDown.setVisibility(View.VISIBLE);
                stickyImgPriceArrowDown.setColorFilter(priceColor);
            } else {
                stickyImgPriceArrowUp.setVisibility(View.VISIBLE);
                stickyImgPriceArrowDown.setVisibility(View.GONE);
                stickyImgPriceArrowUp.setColorFilter(priceColor);
            }
        }
    }

    private void loadMoreProducts() {
        if (!isLastPage) {
            if (isAllProductsPromo) {
                fetchPromotionProductsFromApi(false);
            } else {
                fetchFilteredPromotionProducts(false);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // API
    // ─────────────────────────────────────────────────────────────────────

    private void fetchPromotionDetail(String id) {
        PromotionApi api = ApiClient.createService(PromotionApi.class);
        api.getPromotionById(id).enqueue(new Callback<PromotionDto>() {
            @Override
            public void onResponse(@NonNull Call<PromotionDto> call, @NonNull Response<PromotionDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindData(response.body());
                } else {
                    Toast.makeText(PromotionDetailActivity.this, "Không thể tải chi tiết khuyến mãi", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PromotionDto> call, @NonNull Throwable t) {
                Toast.makeText(PromotionDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPromotionProducts() {
        currentSkip = 0;
        isLastPage = false;
        if (isAllProductsPromo) {
            // PROMO010 / PROMO011: lấy TOÀN BỘ sản phẩm (không giới hạn) → nay đổi thành API phân trang
            fetchPromotionProductsFromApi(true);
        } else {
            // PROMO012 / PROMO013 / PROMO014: sản phẩm lọc theo khuyến mãi, hiển thị tất cả -> nay đổi thành API phân trang
            fetchFilteredPromotionProducts(true);
        }
    }

    private void fetchFilteredPromotionProducts(boolean isRefresh) {
        PromotionApi promotionApi = ApiClient.createService(PromotionApi.class);
        promotionApi.getPromotionProducts(promotionId, PAGE_SIZE, currentSkip).enqueue(new Callback<HomeProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<HomeProductResponse> call, @NonNull Response<HomeProductResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ProductDto> dtos = response.body().getData();
                    List<Product> newProducts = new ArrayList<>();
                    if (dtos != null) {
                        for (ProductDto dto : dtos) {
                            newProducts.add(ProductMapper.fromDto(dto));
                        }
                    }

                    isLastPage = newProducts.size() < PAGE_SIZE;
                    currentSkip += newProducts.size();

                    layoutProductTabs.setVisibility(View.GONE);

                    if (isRefresh) {
                        allLoadedProducts.clear();
                    }
                    allLoadedProducts.addAll(newProducts);
                    productAdapter.submitList(new ArrayList<>(allLoadedProducts));

                    if (allLoadedProducts.isEmpty()) {
                        tvProductGridTitle.setText("Chưa có sản phẩm áp dụng");
                    }
                    
                    btnLoadMore.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
                } else {
                    Toast.makeText(PromotionDetailActivity.this, "Không thể tải sản phẩm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<HomeProductResponse> call, @NonNull Throwable t) {
                Toast.makeText(PromotionDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchPromotionProductsFromApi(boolean isRefresh) {
        ProductApi productApi = ApiClient.createService(ProductApi.class);
        productApi.getHomeProducts(currentTab, PAGE_SIZE, currentSkip).enqueue(new Callback<HomeProductResponse>() {
            @Override
            public void onResponse(@NonNull Call<HomeProductResponse> call, @NonNull Response<HomeProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<ProductDto> dtos = response.body().getData();
                    List<Product> newProducts = new ArrayList<>();
                    if (dtos != null) {
                        for (ProductDto dto : dtos) {
                            newProducts.add(ProductMapper.fromDto(dto));
                        }
                    }

                    isLastPage = newProducts.size() < PAGE_SIZE;
                    currentSkip += newProducts.size();

                    layoutProductTabs.setVisibility(View.VISIBLE);
                    
                    if (isRefresh) {
                        allLoadedProducts.clear();
                    }
                    allLoadedProducts.addAll(newProducts);
                    productAdapter.submitList(new ArrayList<>(allLoadedProducts));

                    if (allLoadedProducts.isEmpty()) {
                        tvProductGridTitle.setText("Chưa có sản phẩm");
                    }
                    
                    btnLoadMore.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
                    
                    layoutProductTabs.post(() -> {
                        tabBarTop = getViewTopInScrollContent(layoutProductTabs);
                    });


                } else {
                    Toast.makeText(PromotionDetailActivity.this, "Không thể tải sản phẩm", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<HomeProductResponse> call, @NonNull Throwable t) {
                Toast.makeText(PromotionDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Bind promotion data
    // ─────────────────────────────────────────────────────────────────────

    private void bindData(PromotionDto dto) {
        tvTitle.setText(dto.getName() != null ? dto.getName() : "");
        tvCode.setText(dto.getCode() != null ? dto.getCode() : dto.getPromotionId());
        tvDescription.setText(dto.getDescription() != null ? dto.getDescription() : "");

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String promoId = dto.getPromotionId();

        if ("PROMO012".equals(promoId)) {
            tvDiscount.setText("Giảm 10%");
            tvMinOrder.setText("cho danh mục nấm rơm");
            tvMinOrder.setVisibility(View.VISIBLE);
        } else if ("PROMO013".equals(promoId)) {
            tvDiscount.setText("Giảm 30.000đ");
            tvMinOrder.setText("áp dụng cho đơn tối thiểu 200.000đ");
            tvMinOrder.setVisibility(View.VISIBLE);
        } else if ("PROMO014".equals(promoId)) {
            tvDiscount.setText("Mua 1 tặng 1");
            tvMinOrder.setText("áp dụng cho đơn từ 0đ");
            tvMinOrder.setVisibility(View.VISIBLE);
        } else if ("PROMO010".equals(promoId)) {
            tvDiscount.setText("Giảm 20%");
            tvMinOrder.setText("áp dụng cho đơn từ 150.000đ");
            tvMinOrder.setVisibility(View.VISIBLE);
        } else if ("PROMO011".equals(promoId)) {
            tvDiscount.setText("Giảm tối đa 25.000đ phí vận chuyển");
            tvMinOrder.setText("áp dụng cho đơn từ 99.000đ");
            tvMinOrder.setVisibility(View.VISIBLE);
        } else {
            tvMinOrder.setVisibility(View.VISIBLE);
            if (dto.getDiscountValue() != null) {
                tvDiscount.setText(formatDiscount(dto, currencyFormat));
            } else {
                tvDiscount.setText("");
            }
            if (dto.getMinOrderValue() != null) {
                tvMinOrder.setText("Đơn tối thiểu " + currencyFormat.format(dto.getMinOrderValue()));
            } else {
                tvMinOrder.setText("Mọi đơn hàng");
            }
        }

        String timeStr = formatTime(dto.getStartDate(), dto.getEndDate());
        tvTime.setText(timeStr.isEmpty() ? "Không giới hạn" : timeStr);
        tvUsage.setText(formatLimit(dto.getUsageLimit()));
        tvLimitPerUser.setText(formatLimit(dto.getUserLimit()));

        String imageUrl = getPromotionImageUrl(dto);
        String fullImageUrl = imageUrl == null || imageUrl.trim().isEmpty()
                ? null
                : buildPromotionBannerProxyUrl(dto);
        if (fullImageUrl != null && !fullImageUrl.isEmpty()) {
            if (promotionId != null && promotionId.matches("PROMO01[7-9]|PROMO02[0-1]")) {
                imgBanner.post(() -> {
                    int w = imgBanner.getWidth();
                    if (w > 0) {
                        int h = (int) (w / 1.414);
                        android.view.ViewGroup.LayoutParams params = imgBanner.getLayoutParams();
                        params.height = h;
                        imgBanner.setLayoutParams(params);
                        imgBanner.setScaleType(ImageView.ScaleType.FIT_XY);
                    }
                });
            } else {
                imgBanner.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
            Glide.with(this)
                    .load(fullImageUrl)
                    .fitCenter()
                    .placeholder(R.drawable.banner_nam_rom)
                    .error(R.drawable.banner_nam_rom)
                    .into(imgBanner);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String formatDiscount(PromotionDto dto, NumberFormat currencyFormat) {
        double value = dto.getDiscountValue() == null ? 0 : dto.getDiscountValue();
        String type = dto.getDiscountType() == null ? "" : dto.getDiscountType().trim().toLowerCase(Locale.US);
        if ("percent".equals(type) || "percentage".equals(type)) {
            return "Giảm " + formatPlainNumber(value) + "%";
        }
        if ("buy1get1".equals(type)) {
            return "Mua 1 tặng 1";
        }
        return "Giảm " + currencyFormat.format(value);
    }

    private String formatPlainNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private String formatLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return "Không giới hạn";
        }
        return String.valueOf(limit);
    }

    private String formatTime(String startStr, String endStr) {
        if (startStr == null && endStr == null) return "";
        SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        SimpleDateFormat outFmt = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
        String start = "", end = "";
        try {
            if (startStr != null) { Date d = inFmt.parse(startStr); if (d != null) start = outFmt.format(d); }
            if (endStr != null)   { Date d = inFmt.parse(endStr);   if (d != null) end   = outFmt.format(d); }
        } catch (ParseException e) { return startStr + " - " + endStr; }
        if (!start.isEmpty() && !end.isEmpty()) return start + " - " + end;
        if (!start.isEmpty()) return "Từ " + start;
        if (!end.isEmpty())   return "Đến " + end;
        return "";
    }

    private String getPromotionImageUrl(PromotionDto p) {
        PromotionDto.BannerDataDto bd = p.getBannerData();
        if (bd != null) {
            if (bd.getSrc()      != null && !bd.getSrc().isEmpty())      return bd.getSrc();
            if (bd.getImageUrl() != null && !bd.getImageUrl().isEmpty()) return bd.getImageUrl();
        }
        return p.getImageUrl();
    }

    private String buildPromotionBannerProxyUrl(PromotionDto dto) {
        String id = dto.getPromotionId() != null && !dto.getPromotionId().trim().isEmpty()
                ? dto.getPromotionId()
                : dto.getCode();
        if (id == null || id.trim().isEmpty()) {
            return buildFullImageUrl(getPromotionImageUrl(dto));
        }
        return removeTrailingSlash(Constants.API_BASE_URL) + "/promotions/" + id.trim() + "/banner-image";
    }

    private String buildFullImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) return null;
        String url = imageUrl.trim();
        String base = Constants.API_BASE_URL;
        String serverRoot = "";
        try {
            String[] parts = base.split("//");
            if (parts.length > 1) serverRoot = parts[0] + "//" + parts[1].split("/")[0];
        } catch (Exception e) { serverRoot = removeTrailingSlash(base); }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            if (url.contains("localhost") || url.contains("127.0.0.1")) {
                String ip = serverRoot.replace("http://", "").replace("https://", "");
                return url.replace("localhost:5001", ip).replace("127.0.0.1:5001", ip)
                          .replace("localhost", ip).replace("127.0.0.1", ip);
            }
            return url;
        }
        int apiIdx = base.indexOf("/api/");
        String root = apiIdx >= 0 ? base.substring(0, apiIdx) : removeTrailingSlash(base);
        if (url.startsWith("/")) return root + url;
        return removeTrailingSlash(base) + "/" + url;
    }

    private int getViewTopInScrollContent(View view) {
        int top = 0;
        View current = view;
        while (current != null && current != scrollView) {
            top += (int) current.getY();
            if (current.getParent() instanceof View) {
                current = (View) current.getParent();
            } else {
                break;
            }
        }
        return top;
    }

    private String removeTrailingSlash(String s) {
        if (s == null) return "";
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }
}