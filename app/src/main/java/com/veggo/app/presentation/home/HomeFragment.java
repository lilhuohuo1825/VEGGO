package com.veggo.app.presentation.home;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
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
    }

    private void setupRecyclerViews() {
        // Banners
        bannerAdapter = new BannerAdapter();
        binding.vpBanners.setAdapter(bannerAdapter);
        
        binding.vpBanners.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateBannerIndicators(position);
            }
        });

        // Utilities
        utilityAdapter = new UtilityAdapter();
        binding.rvUtilities.setAdapter(utilityAdapter);

        // Categories
        categoryAdapter = new CategoryAdapter();
        binding.rvCategories.setAdapter(categoryAdapter);

        // Flash Sale
        flashSaleAdapter = new FlashSaleAdapter();
        binding.rvFlashSale.setAdapter(flashSaleAdapter);

        // Recipes
        recipeAdapter = new RecipeAdapter();
        binding.rvRecipes.setAdapter(recipeAdapter);

        // Products Grid
        productAdapter = new ProductAdapter();
        binding.rvProducts.setAdapter(productAdapter);
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

    private void onProductTabClicked(int index) {
        for (int i = 0; i < productTabs.size(); i++) {
            TextView tab = productTabs.get(i);
            if (i == index) {
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_main));
                tab.setTypeface(null, Typeface.BOLD);
                // TODO: Gọi ViewModel load dữ liệu theo tab i
                // homeViewModel.loadProductsByTab(i);
            } else {
                tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_60));
                tab.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void observeViewModel() {
        homeViewModel.getBanners().observe(getViewLifecycleOwner(), banners -> {
            if (banners != null) {
                bannerAdapter.submitList(banners);
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}