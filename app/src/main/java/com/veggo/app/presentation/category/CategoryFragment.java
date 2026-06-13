package com.veggo.app.presentation.category;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.veggo.app.core.ui.BaseFragment;
import com.veggo.app.core.ui.ViewModelFactory;
import com.veggo.app.databinding.FragmentCategoryBinding;
import com.veggo.app.di.AppModule;

public class CategoryFragment extends BaseFragment {
    private FragmentCategoryBinding binding;
    private CategoryViewModel viewModel;
    private CategoryAdapter categoryAdapter;
    private SubcategoryAdapter subcategoryAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupViewModel();
        setupRecyclerViews();
        observeViewModel();
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(AppModule.provideCategoryRepository(requireContext()));
        viewModel = new ViewModelProvider(this, factory).get(CategoryViewModel.class);
    }

    private void setupRecyclerViews() {
        categoryAdapter = new CategoryAdapter();
        binding.rvSidebar.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSidebar.setAdapter(categoryAdapter);
        categoryAdapter.setOnCategoryClickListener(category -> {
            viewModel.selectCategory(category.categoryId);
        });

        subcategoryAdapter = new SubcategoryAdapter();
        binding.rvSubcategories.setAdapter(subcategoryAdapter);
    }

    private void observeViewModel() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            categoryAdapter.setCategories(categories);
            if (viewModel.getSelectedCategoryId().getValue() == null && !categories.isEmpty()) {
                viewModel.selectCategory(categories.get(0).categoryId);
            }
        });

        viewModel.getSelectedCategoryId().observe(getViewLifecycleOwner(), id -> {
            categoryAdapter.setSelectedCategoryId(id);
        });

        viewModel.getSelectedCategory().observe(getViewLifecycleOwner(), category -> {
            if (category != null) {
                binding.tvCategoryTitle.setText(category.categoryName);
                subcategoryAdapter.setSubcategories(category.subcategories);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
