package com.veggo.app.presentation.home;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.veggo.app.presentation.about.AboutUsActivity;
import com.veggo.app.databinding.FragmentHomeBinding;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    
    private ProductAdapter productAdapter;
    private UtilityAdapter utilityAdapter;
    private BannerAdapter bannerAdapter;
    private FlashSaleAdapter flashSaleAdapter;
    private RecipeAdapter recipeAdapter;
    private CategoryAdapter categoryAdapter;

    private List<TextView> productTabs = new ArrayList<>();
    private View[] bannerIndicators;

    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final Runnable bannerRunnable = new Runnable() {
        @Override
        public void run() {
            if (binding != null && binding.vpBanners != null && bannerAdapter != null && bannerAdapter.getRealCount() > 0) {
                int nextItem = binding.vpBanners.getCurrentItem() + 1;
                binding.vpBanners.setCurrentItem(nextItem, true);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupHeader();
        setupRecyclerViews();
        setupBannerIndicators();
        setupProductTabs();
        setupStickyHeader();
        setupHeaderActions();
        observeViewModel();

        // Gọi ViewModel để lấy dữ liệu từ file JSON trong thư mục assets
        homeViewModel.loadHomeData(requireContext());
    }

    private void setupHeader() {
        View.OnClickListener aboutUsClick = v -> {
            Intent intent = new Intent(requireContext(), AboutUsActivity.class);
            startActivity(intent);
        };

        binding.imgLogo.setOnClickListener(aboutUsClick);
        binding.stickyHeader.imgStickyLogo.setOnClickListener(aboutUsClick);

        View.OnClickListener openCategoryClick = v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCategoryScreen();
            }
        };
        binding.btnMenuCategory.setOnClickListener(openCategoryClick);
        binding.stickyHeader.btnStickyMenuCategory.setOnClickListener(openCategoryClick);

        View.OnClickListener openSearchClick = v -> {
            Intent intent = new Intent(requireContext(), com.veggo.app.presentation.search.SearchActivity.class);
            startActivity(intent);
        };
        binding.layoutSearch.getRoot().setOnClickListener(openSearchClick);
        binding.layoutSearch.edtSearch.setFocusable(false);
        binding.layoutSearch.edtSearch.setOnClickListener(openSearchClick);
        
        binding.stickyHeader.layoutStickySearch.setOnClickListener(openSearchClick);
        if (binding.stickyHeader.layoutStickySearch.findViewById(R.id.edtSearch) != null) {
            binding.stickyHeader.layoutStickySearch.findViewById(R.id.edtSearch).setFocusable(false);
            binding.stickyHeader.layoutStickySearch.findViewById(R.id.edtSearch).setOnClickListener(openSearchClick);
        }

        binding.btnChatbot.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.veggo.app.presentation.chatbot.ChatbotActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerViews() {
        // Banners
        bannerAdapter = new BannerAdapter();
        binding.vpBanners.setAdapter(bannerAdapter);
        
        binding.vpBanners.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (bannerAdapter.getRealCount() > 0) {
                    updateBannerIndicators(position % bannerAdapter.getRealCount());
                }
                // Reset timer khi người dùng chuyển trang thủ công hoặc tự động
                bannerHandler.removeCallbacks(bannerRunnable);
                bannerHandler.postDelayed(bannerRunnable, 10000);
            }
        });

        // Utilities
        utilityAdapter = new UtilityAdapter();
        binding.rvUtilities.setAdapter(utilityAdapter);
        utilityAdapter.setOnUtilityClickListener(utility -> {
            Intent intent = null;
            switch (utility.getId()) {
                case "1": // Tủ lạnh
                    intent = new Intent(requireContext(), com.veggo.app.presentation.profile.SmartFridgeActivity.class);
                    break;
                case "2": // Trợ lý AI
                    intent = new Intent(requireContext(), com.veggo.app.presentation.chatbot.ChatbotActivity.class);
                    break;
                case "3": // Khẩu vị
                    intent = new Intent(requireContext(), com.veggo.app.presentation.profile.TastePreferencesActivity.class);
                    break;
                case "4": // Điểm xanh
                    intent = new Intent(requireContext(), com.veggo.app.presentation.profile.CarbonPointsActivity.class);
                    break;
                case "5": // Blog
                    intent = new Intent(requireContext(), com.veggo.app.presentation.blog.BlogHomeActivity.class);
                    break;
            }
            if (intent != null) {
                startActivity(intent);
            }
        });

        // Categories
        categoryAdapter = new CategoryAdapter();
        binding.rvCategories.setAdapter(categoryAdapter);
        categoryAdapter.setOnCategoryClickListener(category -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCategoryDetail(category.getId(), null);
            }
        });

        binding.btnViewMoreCategories.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCategoryDetail(null, null);
            }
        });

        // Flash Sale
        flashSaleAdapter = new FlashSaleAdapter();
        binding.rvFlashSale.setAdapter(flashSaleAdapter);
        flashSaleAdapter.setOnFlashSaleClickListener(flashSale -> {
            Intent intent = new Intent(requireContext(), com.veggo.app.presentation.product.ProductDetailActivity.class);
            intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_PRODUCT_ID, flashSale.getId());
            startActivity(intent);
        });

        // Recipes
        recipeAdapter = new RecipeAdapter();
        binding.rvRecipes.setAdapter(recipeAdapter);

        // Products Grid
        productAdapter = new ProductAdapter();
        binding.rvProducts.setAdapter(productAdapter);
        productAdapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(requireContext(), com.veggo.app.presentation.product.ProductDetailActivity.class);
            intent.putExtra(com.veggo.app.presentation.product.ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
            startActivity(intent);
        });
    }

    private void setupBannerIndicators() {
        bannerIndicators = new View[]{
                binding.indicator1,
                binding.indicator2,
                binding.indicator3,
                binding.indicator4,
                binding.indicator5
        };
        updateBannerIndicators(0);
    }

    private void updateBannerIndicators(int position) {
        if (bannerIndicators == null) return;
        
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < bannerIndicators.length; i++) {
            if (bannerIndicators[i] == null) continue;
            
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bannerIndicators[i].getLayoutParams();
            if (i == position) {
                bannerIndicators[i].setBackgroundResource(R.drawable.bg_indicator_selected);
                params.width = (int) (14 * density);
                params.height = (int) (4 * density);
            } else {
                bannerIndicators[i].setBackgroundResource(R.drawable.bg_indicator_unselected);
                params.width = (int) (4 * density);
                params.height = (int) (4 * density);
            }
            bannerIndicators[i].setLayoutParams(params);
        }
    }

    private void setupProductTabs() {
        productTabs.clear();
        productTabs.add(binding.tabToday);
        productTabs.add(binding.tabDiscount);
        productTabs.add(binding.tabCheapest);
        productTabs.add(binding.tabNewest);
        productTabs.add(binding.tabPopular);

        for (int i = 0; i < productTabs.size(); i++) {
            TextView tab = productTabs.get(i);
            final int index = i;
            tab.setOnClickListener(v -> onProductTabClicked(index));
        }
    }

    private void setupStickyHeader() {
        // Chiều cao của header gốc (64dp) để làm ngưỡng hiển thị
        int threshold = (int) (64 * getResources().getDisplayMetrics().density);
        
        binding.homeScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY >= threshold) {
                binding.stickyHeader.getRoot().setVisibility(View.VISIBLE);
            } else {
                binding.stickyHeader.getRoot().setVisibility(View.GONE);
            }
        });
    }

    private void setupHeaderActions() {
        View header = binding.getRoot();
        View stickyHeader = binding.stickyHeader.getRoot();

        View.OnClickListener openCart = v -> ((MainActivity) requireActivity()).openCartScreen();
        View.OnClickListener openNotifications = v -> ((MainActivity) requireActivity()).openNotificationsScreen();

        View cartButton = header.findViewById(R.id.homeCartButton);
        View notifyButton = header.findViewById(R.id.homeNotifyButton);
        if (cartButton != null) {
            cartButton.setOnClickListener(openCart);
        }
        if (notifyButton != null) {
            notifyButton.setOnClickListener(openNotifications);
        }

        View stickyCartButton = stickyHeader.findViewById(R.id.homeStickyCartButton);
        View stickyNotifyButton = stickyHeader.findViewById(R.id.homeStickyNotifyButton);
        if (stickyCartButton != null) {
            stickyCartButton.setOnClickListener(openCart);
        }
        if (stickyNotifyButton != null) {
            stickyNotifyButton.setOnClickListener(openNotifications);
        }
    }
    private void onProductTabClicked(int index) {
        View[] indicators = {
            binding.tabTodayIndicator,
            binding.tabDiscountIndicator,
            binding.tabCheapestIndicator,
            binding.tabNewestIndicator,
            binding.tabPopularIndicator
        };
        for (int i = 0; i < productTabs.size(); i++) {
            TextView tab = productTabs.get(i);
            if (i == index) {
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_main));
                tab.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_semibold));
                if (i < indicators.length && indicators[i] != null) {
                    indicators[i].setVisibility(View.VISIBLE);
                }
            } else {
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_60));
                tab.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.inter_regular));
                if (i < indicators.length && indicators[i] != null) {
                    indicators[i].setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void observeViewModel() {
        homeViewModel.getBanners().observe(getViewLifecycleOwner(), banners -> {
            if (banners != null && !banners.isEmpty()) {
                bannerAdapter.submitList(banners, () -> {
                    // Thiết lập vị trí ban đầu ở giữa để có thể cuộn vô tận 2 chiều
                    int initialPos = (Integer.MAX_VALUE / 2) - ((Integer.MAX_VALUE / 2) % banners.size());
                    binding.vpBanners.setCurrentItem(initialPos, false);
                    updateBannerIndicators(0);
                });
            }
        });

        homeViewModel.getUtilities().observe(getViewLifecycleOwner(), utilities -> {
            if (utilities != null) {
                utilityAdapter.submitList(utilities);
            }
        });

        homeViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                categoryAdapter.submitList(categories);
            }
        });

        homeViewModel.getFlashSales().observe(getViewLifecycleOwner(), flashSales -> {
            if (flashSales != null) {
                flashSaleAdapter.submitList(flashSales);
            }
        });

        homeViewModel.getRecipes().observe(getViewLifecycleOwner(), recipes -> {
            if (recipes != null) {
                recipeAdapter.submitList(recipes);
            }
        });

        homeViewModel.getProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                productAdapter.submitList(products);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bannerAdapter != null && bannerAdapter.getRealCount() > 0) {
            bannerHandler.postDelayed(bannerRunnable, 10000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        bannerHandler.removeCallbacks(bannerRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
