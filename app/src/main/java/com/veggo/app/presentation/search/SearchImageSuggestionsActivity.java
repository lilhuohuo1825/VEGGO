package com.veggo.app.presentation.search;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.adapter.ProductAdapter;
import com.veggo.app.core.network.ApiClient;
import com.veggo.app.core.ui.BaseActivity;
import com.veggo.app.data.mapper.ProductMapper;
import com.veggo.app.data.remote.api.FridgeApi;
import com.veggo.app.data.remote.dto.AiRecognitionItemDto;
import com.veggo.app.data.remote.dto.AiRecognitionRequestDto;
import com.veggo.app.data.remote.dto.ProductDto;
import com.veggo.app.domain.model.Product;
import com.veggo.app.presentation.product.AddToCartBottomSheetHelper;
import com.veggo.app.presentation.product.ProductDetailActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SearchImageSuggestionsActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";

    private ProductAdapter productAdapter;
    private View loadingState;
    private View emptyState;
    private TextView emptyBody;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_image_suggestions);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadingState = findViewById(R.id.loadingState);
        emptyState = findViewById(R.id.emptyState);
        emptyBody = findViewById(R.id.tvEmptyBody);

        RecyclerView recyclerView = findViewById(R.id.rvSuggestedProducts);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter();
        recyclerView.setAdapter(productAdapter);
        productAdapter.setOnProductClickListener(this::openProductDetail);
        productAdapter.setOnAddProductClickListener(product ->
                AddToCartBottomSheetHelper.show(this, product, null)
        );

        String rawUri = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        if (rawUri == null || rawUri.trim().isEmpty()) {
            showEmpty("Không đọc được ảnh. Vui lòng thử lại.");
            return;
        }
        analyzeImage(Uri.parse(rawUri));
    }

    private void analyzeImage(@NonNull Uri imageUri) {
        showLoading();
        new Thread(() -> {
            try {
                String base64Image = encodeImageForApi(imageUri);
                AiRecognitionRequestDto request = new AiRecognitionRequestDto(base64Image);
                FridgeApi api = ApiClient.createService(FridgeApi.class);
                retrofit2.Response<List<AiRecognitionItemDto>> response = api.recognizeIngredient(request).execute();

                runOnUiThread(() -> {
                    if (!response.isSuccessful() || response.body() == null) {
                        showEmpty(readApiErrorMessage(response, "VEGGO chưa phân tích được ảnh này."));
                        return;
                    }
                    List<Product> products = extractMatchedProducts(response.body());
                    if (products.isEmpty()) {
                        showEmpty("Không có sản phẩm nào trên VEGGO khớp với hình ảnh này.");
                        return;
                    }
                    showProducts(products);
                });
            } catch (Exception error) {
                runOnUiThread(() -> showEmpty("Lỗi kết nối AI: " + error.getMessage()));
            }
        }).start();
    }

    @NonNull
    private String encodeImageForApi(@NonNull Uri imageUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        if (inputStream == null) {
            throw new IOException("Không đọc được ảnh");
        }

        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(inputStream);
        } finally {
            inputStream.close();
        }
        if (bitmap == null) {
            throw new IOException("Ảnh không hợp lệ");
        }

        int maxDim = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (maxDim > 1000) {
            float scale = 1000f / maxDim;
            bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    Math.max(1, Math.round(bitmap.getWidth() * scale)),
                    Math.max(1, Math.round(bitmap.getHeight() * scale)),
                    true
            );
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }

    @NonNull
    private List<Product> extractMatchedProducts(@NonNull List<AiRecognitionItemDto> items) {
        Map<String, Product> uniqueProducts = new LinkedHashMap<>();
        for (AiRecognitionItemDto item : items) {
            if (item == null || item.getMatchedProducts() == null) {
                continue;
            }
            for (ProductDto dto : item.getMatchedProducts()) {
                if (dto == null) {
                    continue;
                }
                Product product = ProductMapper.fromDto(dto);
                String key = product.getId() != null && !product.getId().trim().isEmpty()
                        ? product.getId()
                        : product.getSku();
                if (key == null || key.trim().isEmpty()) {
                    key = product.getName();
                }
                if (key != null && !key.trim().isEmpty()) {
                    uniqueProducts.putIfAbsent(key, product);
                }
            }
        }
        return new ArrayList<>(uniqueProducts.values());
    }

    private void openProductDetail(@NonNull Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
        startActivity(intent);
    }

    private void showLoading() {
        loadingState.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    private void showProducts(@NonNull List<Product> products) {
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        productAdapter.setProducts(products);
    }

    private void showEmpty(@NonNull String message) {
        loadingState.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        if (emptyBody != null) {
            emptyBody.setText(message);
        }
        productAdapter.setProducts(new ArrayList<>());
    }

    private String readApiErrorMessage(retrofit2.Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw.contains("\"message\"")) {
                    int start = raw.indexOf("\"message\"");
                    int valueStart = raw.indexOf(':', start) + 1;
                    int firstQuote = raw.indexOf('"', valueStart);
                    int secondQuote = raw.indexOf('"', firstQuote + 1);
                    if (firstQuote >= 0 && secondQuote > firstQuote) {
                        return raw.substring(firstQuote + 1, secondQuote);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
