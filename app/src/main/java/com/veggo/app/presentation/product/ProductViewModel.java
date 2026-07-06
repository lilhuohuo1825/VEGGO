package com.veggo.app.presentation.product;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.CartItemRequestDto;
import com.veggo.app.domain.model.Consultation;
import com.veggo.app.domain.model.Product;
import com.veggo.app.domain.model.Recipe;
import com.veggo.app.domain.model.Review;
import com.veggo.app.domain.repository.CartRepository;
import com.veggo.app.domain.repository.ConsultationRepository;
import com.veggo.app.domain.repository.ProductRepository;
import com.veggo.app.domain.repository.RecipeRepository;
import com.veggo.app.domain.repository.ReviewRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductViewModel extends ViewModel {
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ConsultationRepository consultationRepository;
    private final RecipeRepository recipeRepository;
    private final CartRepository cartRepository;
    private final MutableLiveData<String> productId = new MutableLiveData<>();
    private final LiveData<Product> product;
    private final LiveData<List<Review>> localProductReviews;
    private final MutableLiveData<List<Recipe>> relatedRecipes = new MutableLiveData<>();
    private final MutableLiveData<List<Review>> productReviews = new MutableLiveData<>();
    private final MutableLiveData<List<Consultation>> consultations = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSubmittingQuestion = new MutableLiveData<>(false);
    private final LiveData<List<Product>> relatedProducts;
    private String activeReviewSku;
    private String activeConsultationSku;

    private final MutableLiveData<Boolean> isAddingToCart = new MutableLiveData<>(false);
    private final MutableLiveData<String> cartError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> addToCartSuccess = new MutableLiveData<>(false);

    public ProductViewModel(ProductRepository productRepository,
                            ReviewRepository reviewRepository,
                            ConsultationRepository consultationRepository,
                            RecipeRepository recipeRepository,
                            CartRepository cartRepository) {
        this.productRepository = productRepository;
        this.reviewRepository = reviewRepository;
        this.consultationRepository = consultationRepository;
        this.recipeRepository = recipeRepository;
        this.cartRepository = cartRepository;
        this.product = Transformations.switchMap(productId, productRepository::observeProductById);
        this.localProductReviews = Transformations.switchMap(productId, productRepository::getProductReviews);
        this.relatedProducts = Transformations.switchMap(this.product, p -> {
            if (p != null) {
                return productRepository.observeRelatedProducts(
                        p.getId(), p.getCategoryId(), p.getSubcategoryId());
            }
            return productRepository.observeProducts(10);
        });
    }

    private void fetchRemoteReviews(String sku) {
        if (reviewRepository == null) return;
        reviewRepository.getReviewsBySku(sku, new ReviewRepository.Callback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> result) {
                productReviews.postValue(result);
            }

            @Override
            public void onError(Throwable t) {
                productReviews.postValue(new ArrayList<>());
            }
        });
    }

    private void fetchRemoteConsultations(String sku) {
        if (consultationRepository == null) return;
        consultationRepository.getConsultationsBySku(sku, new ConsultationRepository.Callback<List<Consultation>>() {
            @Override
            public void onSuccess(List<Consultation> result) {
                consultations.postValue(result);
            }

            @Override
            public void onError(Throwable t) {
                consultations.postValue(new ArrayList<>());
            }
        });
    }

    public void triggerReviewFetch(Product product) {
        if (product == null) return;
        String sku = product.getSku();
        if (sku != null && !sku.isEmpty()) {
            if (sku.equals(activeReviewSku)) {
                return;
            }
            activeReviewSku = sku;
            fetchRemoteReviews(sku);
        } else {
            productReviews.postValue(new ArrayList<>());
        }
    }

    public void toggleReviewLike(String sku, String reviewId, String customerId) {
        if (reviewRepository == null || sku == null || sku.isEmpty()
                || reviewId == null || reviewId.isEmpty()) {
            return;
        }
        reviewRepository.toggleReviewLike(sku, reviewId, customerId, new ReviewRepository.Callback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> result) {
                productReviews.postValue(result);
            }

            @Override
            public void onError(Throwable t) {
                // Keep current list on error.
            }
        });
    }

    public void triggerConsultationFetch(Product product) {
        if (product == null) return;
        String sku = product.getSku();
        if (sku != null && !sku.isEmpty()) {
            if (sku.equals(activeConsultationSku)) {
                return;
            }
            activeConsultationSku = sku;
            fetchRemoteConsultations(sku);
        } else {
            activeConsultationSku = null;
            consultations.postValue(new ArrayList<>());
        }
    }

    public void triggerRelatedRecipesFetch(Product product) {
        if (product == null || product.getName() == null || product.getName().trim().isEmpty()) {
            relatedRecipes.postValue(new ArrayList<>());
            return;
        }
        if (recipeRepository == null) {
            relatedRecipes.postValue(new ArrayList<>());
            return;
        }
        recipeRepository.getRelatedRecipes(product.getName(), new RecipeRepository.Callback<List<Recipe>>() {
            @Override
            public void onSuccess(List<Recipe> result) {
                relatedRecipes.postValue(result != null ? result : new ArrayList<>());
            }

            @Override
            public void onError(Throwable t) {
                relatedRecipes.postValue(new ArrayList<>());
            }
        });
    }

    public void submitQuestion(String sku, String question, String customerId, String customerName,
                               String productName, String customerAvatarUrl,
                               ConsultationRepository.Callback<List<Consultation>> callback) {
        if (consultationRepository == null) {
            callback.onError(new IllegalStateException("ConsultationRepository not available"));
            return;
        }
        String trimmedQuestion = question != null ? question.trim() : "";
        if (trimmedQuestion.isEmpty()) {
            callback.onError(new IllegalArgumentException("Question cannot be empty"));
            return;
        }

        isSubmittingQuestion.postValue(true);
        String resolvedName = customerName != null && !customerName.trim().isEmpty()
                ? customerName.trim()
                : null;
        String resolvedCustomerId = customerId != null && !customerId.trim().isEmpty()
                ? customerId.trim()
                : null;
        String resolvedAvatarUrl = customerAvatarUrl != null ? customerAvatarUrl.trim() : "";

        consultationRepository.submitQuestion(
                sku,
                trimmedQuestion,
                resolvedCustomerId,
                resolvedName,
                productName,
                resolvedAvatarUrl,
                wrapConsultationCallback(callback, true)
        );
    }

    public void toggleQuestionLike(String sku, String questionId, String customerId, String customerName,
                                 ConsultationRepository.Callback<List<Consultation>> callback) {
        if (consultationRepository == null) {
            callback.onError(new IllegalStateException("ConsultationRepository not available"));
            return;
        }
        consultationRepository.toggleQuestionLike(
                sku,
                questionId,
                customerId,
                customerName,
                wrapConsultationCallback(callback, false)
        );
    }

    public void submitReply(String sku, String questionId, String content, String customerId,
                            String customerName, String customerAvatarUrl,
                            ConsultationRepository.Callback<List<Consultation>> callback) {
        if (consultationRepository == null) {
            callback.onError(new IllegalStateException("ConsultationRepository not available"));
            return;
        }
        String trimmedContent = content != null ? content.trim() : "";
        if (trimmedContent.isEmpty()) {
            callback.onError(new IllegalArgumentException("Reply cannot be empty"));
            return;
        }
        isSubmittingQuestion.postValue(true);
        consultationRepository.submitReply(
                sku,
                questionId,
                trimmedContent,
                customerId,
                customerName,
                customerAvatarUrl != null ? customerAvatarUrl.trim() : "",
                wrapConsultationCallback(callback, true)
        );
    }

    private ConsultationRepository.Callback<List<Consultation>> wrapConsultationCallback(
            ConsultationRepository.Callback<List<Consultation>> callback,
            boolean affectsSubmittingState
    ) {
        return new ConsultationRepository.Callback<List<Consultation>>() {
            @Override
            public void onSuccess(List<Consultation> result) {
                if (affectsSubmittingState) {
                    isSubmittingQuestion.postValue(false);
                }
                activeConsultationSku = null;
                consultations.postValue(result);
                callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable t) {
                if (affectsSubmittingState) {
                    isSubmittingQuestion.postValue(false);
                }
                callback.onError(t);
            }
        };
    }

    public void addToCart(String customerId, String sku, int quantity, double selectedWeight) {
        if (cartRepository == null) {
            cartError.setValue("Cart repository is not available");
            return;
        }
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

    public LiveData<List<Review>> getLocalProductReviews() {
        return localProductReviews;
    }

    public LiveData<List<Consultation>> getConsultations() {
        return consultations;
    }

    public LiveData<Boolean> isSubmittingQuestion() {
        return isSubmittingQuestion;
    }

    public LiveData<List<Product>> getRelatedProducts() {
        return relatedProducts;
    }

    public void addProduct(Product product) {
        if (productRepository instanceof com.veggo.app.data.repository.ProductRepositoryImpl) {
            com.veggo.app.data.repository.ProductRepositoryImpl repo = (com.veggo.app.data.repository.ProductRepositoryImpl) productRepository;
            new Thread(() -> {
                repo.saveProduct(com.veggo.app.data.mapper.ProductMapper.toEntity(product));

                List<com.veggo.app.data.local.entity.RecipeEntity> recipes = new ArrayList<>();
                recipes.add(new com.veggo.app.data.local.entity.RecipeEntity("r1", "Beef Salad with Watermelon", "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=500", "15min", "6,3 €", 4.5f, 120, false, product.getId()));
                recipes.add(new com.veggo.app.data.local.entity.RecipeEntity("r2", "Pan-Seared Steak", "https://images.unsplash.com/photo-1558030006-45c25be991f1?q=80&w=500", "25min", "12,5 €", 4.8f, 350, true, product.getId()));
                recipes.add(new com.veggo.app.data.local.entity.RecipeEntity("r3", "Healthy Beef Bowl", "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?q=80&w=500", "20min", "8,0 €", 4.2f, 210, false, product.getId()));
                repo.saveRecipes(recipes);

                List<com.veggo.app.data.local.entity.ReviewEntity> reviews = new ArrayList<>();
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
