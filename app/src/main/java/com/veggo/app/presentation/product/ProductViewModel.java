package com.veggo.app.presentation.product;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.CartItemRequestDto;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.model.Recipe;
import com.veggo.app.domain.model.Review;
import com.veggo.app.domain.repository.CartRepository;
import com.veggo.app.domain.repository.ProductRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductViewModel extends ViewModel {
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final MutableLiveData<String> productId = new MutableLiveData<>();
    private final LiveData<Product> product;
    private final LiveData<List<Recipe>> relatedRecipes;
    private final LiveData<List<Review>> productReviews;
    private final LiveData<List<com.veggo.app.assets.AssetModels.Question>> consultations;
    private final LiveData<List<Product>> relatedProducts;
    
    private final MutableLiveData<Boolean> isAddingToCart = new MutableLiveData<>(false);
    private final MutableLiveData<String> cartError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> addToCartSuccess = new MutableLiveData<>(false);

    public ProductViewModel(ProductRepository productRepository, CartRepository cartRepository) {
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.product = Transformations.switchMap(productId, productRepository::observeProductById);
        this.relatedRecipes = Transformations.switchMap(productId, productRepository::getRelatedRecipes);
        this.productReviews = Transformations.switchMap(productId, productRepository::getProductReviews);
        this.consultations = Transformations.switchMap(productId, productRepository::getConsultations);
        this.relatedProducts = productRepository.observeProducts(10); // Limit to 10 products to avoid SQLiteBlobTooBigException
    }

    public void addToCart(String customerId, String sku, int quantity, double selectedWeight) {
        isAddingToCart.setValue(true);
        double normalizedWeight = selectedWeight > 0 ? selectedWeight : 1.0;
        cartRepository.addItem(customerId, new CartItemRequestDto(sku, quantity, normalizedWeight)).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                isAddingToCart.setValue(false);
                if (response.isSuccessful()) {
                    addToCartSuccess.setValue(true);
                } else {
                    cartError.setValue("Failed to add to cart");
                }
            }

            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                isAddingToCart.setValue(false);
                cartError.setValue(t.getMessage());
            }
        });
    }

    public LiveData<Boolean> getIsAddingToCart() { return isAddingToCart; }
    public LiveData<String> getCartError() { return cartError; }
    public LiveData<Boolean> getAddToCartSuccess() { return addToCartSuccess; }
    public void resetAddToCartStatus() { addToCartSuccess.setValue(false); cartError.setValue(null); }

    public void setProductId(String id) {
        productId.postValue(id);
    }

    public String getProductId() {
        return productId.getValue();
    }

    public LiveData<Product> getProduct() {
        return product;
    }

    public LiveData<List<Recipe>> getRelatedRecipes() {
        return relatedRecipes;
    }

    public LiveData<List<Review>> getProductReviews() {
        return productReviews;
    }

    public LiveData<List<com.veggo.app.assets.AssetModels.Question>> getConsultations() {
        return consultations;
    }

    public LiveData<List<Product>> getRelatedProducts() {
        return relatedProducts;
    }

    public void addProduct(Product product) {
        if (productRepository instanceof com.veggo.app.data.repository.ProductRepositoryImpl) {
            com.veggo.app.data.repository.ProductRepositoryImpl repo = (com.veggo.app.data.repository.ProductRepositoryImpl) productRepository;
            new Thread(() -> {
                repo.saveProduct(com.veggo.app.data.mapper.ProductMapper.toEntity(product));
                
                // Thêm dữ liệu mẫu cho Recipe và Review khi add mock product
                List<com.veggo.app.data.local.entity.RecipeEntity> recipes = new java.util.ArrayList<>();
                recipes.add(new com.veggo.app.data.local.entity.RecipeEntity("r1", "Beef Salad with Watermelon", "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=500", "15min", "6,3 €", 4.5f, 120, false, product.getId()));
                recipes.add(new com.veggo.app.data.local.entity.RecipeEntity("r2", "Pan-Seared Steak", "https://images.unsplash.com/photo-1558030006-45c25be991f1?q=80&w=500", "25min", "12,5 €", 4.8f, 350, true, product.getId()));
                recipes.add(new com.veggo.app.data.local.entity.RecipeEntity("r3", "Healthy Beef Bowl", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?q=80&w=500", "20min", "8,0 €", 4.2f, 210, false, product.getId()));
                repo.saveRecipes(recipes);

                List<com.veggo.app.data.local.entity.ReviewEntity> reviews = new java.util.ArrayList<>();
                reviews.add(new com.veggo.app.data.local.entity.ReviewEntity("rev1", product.getId(), "Ngọc Hân", "2 giờ trước", 5.0f, "Thịt rất tươi, đóng gói cẩn thận. Giao hàng nhanh!", "https://images.unsplash.com/photo-1518020382113-a7e8fc38eac9?q=80&w=200", "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=500"));
                reviews.add(new com.veggo.app.data.local.entity.ReviewEntity("rev2", product.getId(), "Trần Anh", "5 giờ trước", 4.5f, "Giá hơi cao nhưng chất lượng xứng đáng.", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200", null));
                reviews.add(new com.veggo.app.data.local.entity.ReviewEntity("rev3", product.getId(), "Lê Minh", "1 ngày trước", 4.0f, "Ngon, sẽ ủng hộ tiếp.", "https://images.unsplash.com/photo-1527980965255-d3b416303d12?q=80&w=200", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?q=80&w=500,https://images.unsplash.com/photo-1558030006-45c25be991f1?q=80&w=500"));
                repo.saveReviews(reviews);
                
                try {
                    Thread.sleep(100);
                    productId.postValue(product.getId());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
