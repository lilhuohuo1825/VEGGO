package com.veggo.app.presentation.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veggo.app.databinding.ActivitySearchBinding;
import com.veggo.app.di.AppModule;
import com.veggo.app.assets.AssetModels;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.repository.ProductRepository;
import com.veggo.app.core.preferences.AppPreferences;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.veggo.app.core.ui.PullToRefreshHelper;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.presentation.profile.LoginRequiredActivity;
import com.veggo.app.presentation.profile.FridgeQuickScanHelper;
import com.veggo.app.presentation.product.ProductDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    public static final String EXTRA_START_VOICE = "extra_start_voice";

    private ActivitySearchBinding binding;
    private SearchViewModel viewModel;
    private GlobalSearchAdapter searchAdapter;
    private CategorySearchAdapter categoryAdapter;
    private ProductRepository productRepository;
    private com.veggo.app.domain.repository.CategoryRepository categoryRepository;
    private com.veggo.app.speech.SearchVoiceInputController voiceInputController;
    private SwipeRefreshLayout searchRefreshLayout;
    private List<AssetModels.Category> allCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productRepository = AppModule.provideProductRepository(this);
        categoryRepository = AppModule.provideCategoryRepository(this);
        ViewModelFactory factory = new ViewModelFactory(productRepository, categoryRepository, null, null, null, null);
        viewModel = new ViewModelProvider(this, factory).get(SearchViewModel.class);

        setupViews();
        setupPullToRefresh();
        setupVoiceSearch();
        observeData();

        if (getIntent().getBooleanExtra(EXTRA_START_VOICE, false)) {
            binding.layoutSearch.edtSearch.post(() -> {
                if (voiceInputController != null) {
                    voiceInputController.startVoiceInputIfPermitted();
                }
            });
        }
    }

    private void setupPullToRefresh() {
        searchRefreshLayout = PullToRefreshHelper.wrap(binding.searchContentHost, () -> {
            viewModel.refresh();
            binding.getRoot().postDelayed(() -> PullToRefreshHelper.finish(searchRefreshLayout), 400);
        });
        if (searchRefreshLayout != null) {
            searchRefreshLayout.setOnChildScrollUpCallback((parent, child) -> {
                View scrollTarget = binding.rvSearchResults.getVisibility() == View.VISIBLE
                        ? binding.rvSearchResults
                        : binding.rvCategories;
                return scrollTarget.canScrollVertically(-1);
            });
        }
    }

    private void setupVoiceSearch() {
        voiceInputController = com.veggo.app.speech.SearchVoiceInputController.attach(
                this,
                binding.layoutSearch.getRoot(),
                binding.layoutSearch.edtSearch,
                () -> viewModel.setSearchQuery(binding.layoutSearch.edtSearch.getText().toString().trim())
        );
        binding.layoutSearch.btnCamera.setOnClickListener(v ->
                startActivity(FridgeQuickScanHelper.createSearchSuggestionCameraIntent(this))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (voiceInputController != null) {
            voiceInputController.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (voiceInputController != null) {
            voiceInputController.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (voiceInputController != null) {
            voiceInputController.release();
            voiceInputController = null;
        }
        super.onDestroy();
    }

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Setup Category List (Empty State)
        categoryAdapter = new CategorySearchAdapter();
        binding.rvCategories.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        binding.rvCategories.setAdapter(categoryAdapter);
        categoryAdapter.setOnCategoryClickListener(category -> {
            Intent intent = new Intent(this, com.veggo.app.MainActivity.class);
            intent.putExtra(com.veggo.app.MainActivity.EXTRA_CATEGORY_ID, category.categoryId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // Setup Search Results
        searchAdapter = new GlobalSearchAdapter();
        binding.rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSearchResults.setAdapter(searchAdapter);

        searchAdapter.setOnItemClickListener(new GlobalSearchAdapter.OnItemClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(SearchActivity.this, ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onFeatureClick(SearchViewModel.FeatureResult feature) {
                handleFeatureNavigation(feature);
            }

            @Override
            public void onCategoryClick(AssetModels.Category category) {
                Intent intent = new Intent(SearchActivity.this, com.veggo.app.MainActivity.class);
                intent.putExtra(com.veggo.app.MainActivity.EXTRA_CATEGORY_ID, category.categoryId);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        binding.layoutSearch.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeData() {
        viewModel.getCategories().observe(this, categories -> {
            allCategories = categories != null ? categories : new ArrayList<>();
            categoryAdapter.setData(allCategories);
            updateResults();
        });

        viewModel.getSearchQuery().observe(this, query -> {
            if (query.isEmpty()) {
                binding.rvCategories.setVisibility(android.view.View.VISIBLE);
                binding.rvSearchResults.setVisibility(android.view.View.GONE);
                binding.searchEmptyState.setVisibility(android.view.View.GONE);
            } else {
                binding.rvCategories.setVisibility(android.view.View.GONE);
                binding.rvSearchResults.setVisibility(android.view.View.VISIBLE);
                updateResults();
            }
        });

        viewModel.getProductResults().observe(this, products -> {
            updateResults();
        });

        viewModel.getFeatureResults().observe(this, features -> {
            updateResults();
        });
    }

    private void updateResults() {
        List<Product> products = viewModel.getProductResults().getValue();
        List<SearchViewModel.FeatureResult> features = viewModel.getFeatureResults().getValue();
        String query = viewModel.getSearchQuery().getValue();
        List<AssetModels.Category> categories = viewModel.filterCategories(allCategories, query);
        searchAdapter.setResults(products, features, categories);
        boolean hasQuery = query != null && !query.trim().isEmpty();
        if (!hasQuery) {
            binding.rvCategories.setVisibility(android.view.View.VISIBLE);
            binding.rvSearchResults.setVisibility(android.view.View.GONE);
            binding.searchEmptyState.setVisibility(android.view.View.GONE);
            return;
        }
        boolean hasProducts = products != null && !products.isEmpty();
        boolean hasFeatures = features != null && !features.isEmpty();
        boolean hasCategories = !categories.isEmpty();
        boolean showEmpty = !hasProducts && !hasFeatures && !hasCategories;
        binding.rvCategories.setVisibility(android.view.View.GONE);
        binding.rvSearchResults.setVisibility(showEmpty ? android.view.View.GONE : android.view.View.VISIBLE);
        binding.searchEmptyState.setVisibility(showEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void handleFeatureNavigation(SearchViewModel.FeatureResult feature) {
        Intent intent = null;
        switch (feature.getType()) {
            case "refrigerator":
                if (!new AppPreferences(this).isLoggedIn()) {
                    LoginRequiredActivity.open(this, "tủ lạnh thông minh");
                    return;
                }
                intent = new Intent(this, com.veggo.app.presentation.profile.SmartFridgeActivity.class);
                break;
            case "ai_assistant":
                intent = new Intent(this, com.veggo.app.presentation.chatbot.ChatbotActivity.class);
                break;
            case "taste_profile":
                if (!new AppPreferences(this).isLoggedIn()) {
                    LoginRequiredActivity.open(this, "khẩu vị của tôi");
                    return;
                }
                intent = new Intent(this, com.veggo.app.presentation.profile.TastePreferencesActivity.class);
                break;
            case "green_points":
                if (!new AppPreferences(this).isLoggedIn()) {
                    LoginRequiredActivity.open(this, "điểm carbon");
                    return;
                }
                intent = new Intent(this, com.veggo.app.presentation.profile.CarbonPointsActivity.class);
                break;
            case "articles":
                intent = new Intent(this, com.veggo.app.presentation.blog.BlogHomeActivity.class);
                break;
        }

        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Tính năng " + feature.getName() + " đang được phát triển", Toast.LENGTH_SHORT).show();
        }
    }
}
