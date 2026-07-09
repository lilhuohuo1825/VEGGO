package com.veggo.app.presentation.product;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.veggo.app.R;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.preferences.AppPreferences;
import com.veggo.app.core.preferences.PreferencesManager;
import com.veggo.app.core.utils.CurrencyFormatter;
import com.veggo.app.core.utils.ProductImageUtils;
import com.veggo.app.data.mapper.ProductMapper;
import com.veggo.app.data.remote.api.ProductApi;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.CartItemRequestDto;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.di.AppModule;
import com.veggo.app.domain.model.Product;
import com.veggo.app.presentation.checkout.PendingCheckoutStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class AddToCartBottomSheetHelper {

    public interface Listener {
        void onAddedToCart();
    }

    public static final class Options {
        private final long priceOverride;
        private final long originalPriceOverride;
        private final String confirmText;

        public Options(long priceOverride, long originalPriceOverride, String confirmText) {
            this.priceOverride = priceOverride;
            this.originalPriceOverride = originalPriceOverride;
            this.confirmText = confirmText;
        }

        public static Options forFlashSale(long price, long originalPrice) {
            return new Options(price, originalPrice, "Thêm vào giỏ");
        }
    }

    private AddToCartBottomSheetHelper() {
    }

    public static void show(Context context, Product product, @Nullable Listener listener) {
        show(context, product, null, listener);
    }

    public static void show(Context context, Product product, @Nullable Options options, @Nullable Listener listener) {
        if (context == null || product == null) {
            return;
        }
        showDialog(context, product, options, listener);
    }

    public static void showForProductId(
            Context context,
            String productId,
            @Nullable Options options,
            @Nullable Listener listener
    ) {
        if (context == null || productId == null || productId.trim().isEmpty()) {
            return;
        }

        ProductApi productApi = ApiClient.createService(ProductApi.class);
        productApi.getProductById(productId).enqueue(new Callback<ProductDto>() {
            @Override
            public void onResponse(Call<ProductDto> call, Response<ProductDto> response) {
                ProductDto dto = response.body();
                if (!response.isSuccessful() || dto == null) {
                    Toast.makeText(context, "Không tải được thông tin sản phẩm", Toast.LENGTH_SHORT).show();
                    return;
                }
                Product product = ProductMapper.fromDto(dto);
                if (options != null && options.priceOverride > 0) {
                    long original = options.originalPriceOverride > 0
                            ? options.originalPriceOverride
                            : product.getOriginalPrice();
                    product = product.withPricing(options.priceOverride, original);
                }
                showDialog(context, product, options, listener);
            }

            @Override
            public void onFailure(Call<ProductDto> call, Throwable t) {
                Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static void showDialog(
            Context context,
            Product product,
            @Nullable Options options,
            @Nullable Listener listener
    ) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_add_to_cart_bottom_sheet, null, false);
        bottomSheetDialog.setContentView(view);

        ImageView ivThumb = view.findViewById(R.id.ivProductThumb);
        TextView tvName = view.findViewById(R.id.tvProductNamePopup);
        TextView tvPrice = view.findViewById(R.id.tvPricePopup);
        TextView tvOriginalPrice = view.findViewById(R.id.tvOriginalPricePopup);
        TextView tvPriceUnit = view.findViewById(R.id.tvPriceUnitPopup);
        TextView tvQuantity = view.findViewById(R.id.tvQuantityPopup);
        TextView tvTotal = view.findViewById(R.id.tvTotalPopup);
        View btnDecrease = view.findViewById(R.id.tvDecrease);
        View btnIncrease = view.findViewById(R.id.tvIncrease);
        View btnConfirm = view.findViewById(R.id.btnConfirmAddToCart);
        View btnClose = view.findViewById(R.id.btnClose);
        TextView tvWeightTitle = view.findViewById(R.id.tvWeightTitle);
        ChipGroup weightGroup = view.findViewById(R.id.cgWeight);
        TextView btnConfirmText = view.findViewById(R.id.btnConfirmAddToCart);

        ProductImageUtils.loadProductImage(context, ivThumb, product);
        tvName.setText(product.getName());

        long displayPrice = resolvePrice(product, options);
        long displayOriginalPrice = resolveOriginalPrice(product, options);
        tvPrice.setText(CurrencyFormatter.formatVnd(displayPrice));
        bindOriginalPrice(tvOriginalPrice, displayOriginalPrice, displayPrice);

        String confirmLabel = options != null && options.confirmText != null && !options.confirmText.isEmpty()
                ? options.confirmText
                : "Thêm vào giỏ";
        if (btnConfirmText != null) {
            btnConfirmText.setText(confirmLabel);
        }

        final int[] quantity = {1};
        final double[] selectedWeight = {1.0};
        boolean hasWeightOptions = hasWeightOptions(product);
        if (tvPriceUnit != null) {
            tvPriceUnit.setVisibility(hasWeightOptions ? View.VISIBLE : View.GONE);
            tvPriceUnit.setText("/ kg");
        }

        bindWeightOptions(context, tvWeightTitle, weightGroup, product.getWeightOptions(), selectedWeight, () ->
                updateTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions));
        updateTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions);

        btnDecrease.setOnClickListener(v -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                tvQuantity.setText(String.valueOf(quantity[0]));
                updateTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions);
            }
        });

        btnIncrease.setOnClickListener(v -> {
            quantity[0]++;
            tvQuantity.setText(String.valueOf(quantity[0]));
            updateTotal(tvTotal, displayPrice, quantity[0], selectedWeight[0], hasWeightOptions);
        });

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String sku = product.getSku();
            if (sku == null || sku.trim().isEmpty()) {
                Toast.makeText(context, "Không tìm thấy SKU sản phẩm", Toast.LENGTH_SHORT).show();
                return;
            }

            String customerId = resolveCustomerId(context);
            double normalizedWeight = selectedWeight[0] > 0 ? selectedWeight[0] : 1.0;
            AppModule.provideCartRepository(context)
                    .addItem(customerId, new CartItemRequestDto(sku, quantity[0], normalizedWeight))
                    .enqueue(new Callback<CartDto>() {
                        @Override
                        public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(context, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                                if (listener != null) {
                                    listener.onAddedToCart();
                                }
                            } else {
                                Toast.makeText(context, "Không thể thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<CartDto> call, Throwable t) {
                            Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private static long resolvePrice(Product product, @Nullable Options options) {
        if (options != null && options.priceOverride > 0) {
            return options.priceOverride;
        }
        return product.getPrice();
    }

    private static long resolveOriginalPrice(Product product, @Nullable Options options) {
        if (options != null && options.originalPriceOverride > 0) {
            return options.originalPriceOverride;
        }
        return product.getOriginalPrice();
    }

    private static void bindOriginalPrice(TextView originalPriceView, long originalPrice, long salePrice) {
        if (originalPriceView == null) {
            return;
        }
        if (originalPrice > salePrice && salePrice > 0) {
            originalPriceView.setVisibility(View.VISIBLE);
            originalPriceView.setText(CurrencyFormatter.formatVnd(originalPrice));
            originalPriceView.setPaintFlags(originalPriceView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            originalPriceView.setVisibility(View.GONE);
        }
    }

    private static boolean hasWeightOptions(Product product) {
        if (product == null || product.getWeightOptions() == null) {
            return false;
        }
        for (Double option : product.getWeightOptions()) {
            if (option != null && option > 0) {
                return true;
            }
        }
        return false;
    }

    private static void updateTotal(TextView totalView, long unitPrice, int quantity, double selectedWeight, boolean hasWeightOptions) {
        if (totalView == null) {
            return;
        }
        double multiplier = hasWeightOptions ? selectedWeight : 1.0;
        long total = Math.round(unitPrice * multiplier * quantity);
        totalView.setText(CurrencyFormatter.formatVnd(total));
    }

    private static void bindWeightOptions(
            Context context,
            TextView titleView,
            ChipGroup chipGroup,
            List<Double> weightOptions,
            double[] selectedWeight,
            Runnable onSelectionChanged
    ) {
        if (chipGroup == null) {
            return;
        }

        chipGroup.removeAllViews();

        List<Double> resolvedOptions = new ArrayList<>();
        if (weightOptions != null) {
            for (Double option : weightOptions) {
                if (option != null && option > 0) {
                    resolvedOptions.add(option);
                }
            }
        }

        if (resolvedOptions.isEmpty()) {
            selectedWeight[0] = 1.0;
            chipGroup.setVisibility(View.GONE);
            if (titleView != null) {
                titleView.setVisibility(View.GONE);
            }
            return;
        }

        chipGroup.setVisibility(View.VISIBLE);
        if (titleView != null) {
            titleView.setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < resolvedOptions.size(); i++) {
            double option = resolvedOptions.get(i);
            Chip chip = new Chip(context);
            chip.setCheckable(true);
            chip.setText(formatWeightOption(option));
            chip.setChipBackgroundColorResource(R.color.chip_choice_background_color);
            chip.setChipStrokeColorResource(R.color.chip_choice_stroke_color);
            chip.setChipStrokeWidth(1f);
            chip.setTextColor(ContextCompat.getColor(context, R.color.chip_choice_text_color));
            chip.setChecked(i == 0);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedWeight[0] = option;
                    if (onSelectionChanged != null) {
                        onSelectionChanged.run();
                    }
                }
            });
            chipGroup.addView(chip);

            if (i == 0) {
                selectedWeight[0] = option;
            }
        }
    }

    private static String formatWeightOption(double weight) {
        if (weight >= 1.0) {
            if (Math.abs(weight - Math.round(weight)) < 0.0001) {
                return String.format(Locale.US, "%.0fkg", weight);
            }
            return String.format(Locale.US, "%skg", trimTrailingZeros(weight));
        }

        int grams = (int) Math.round(weight * 1000d);
        return grams + "g";
    }

    private static String trimTrailingZeros(double value) {
        String text = String.format(Locale.US, "%.3f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static String resolveCustomerId(Context context) {
        AppPreferences appPreferences = new AppPreferences(context);
        String customerId = appPreferences.getCustomerId();
        if (customerId == null || customerId.trim().isEmpty()) {
            customerId = new PreferencesManager(context).getUserId();
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            customerId = new PendingCheckoutStore(context).guestId();
        }
        return customerId;
    }
}
