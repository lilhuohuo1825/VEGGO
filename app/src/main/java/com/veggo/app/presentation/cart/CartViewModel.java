package com.veggo.app.presentation.cart;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.CartItemRequestDto;
import com.veggo.app.data.remote.dto.PromotionDto;
import com.veggo.app.domain.repository.CartRepository;
import com.veggo.app.domain.repository.PromotionRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartViewModel extends ViewModel {
    private final CartRepository cartRepository;
    private final PromotionRepository promotionRepository;
    private final MutableLiveData<CartDto> cart = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> customerId = new MutableLiveData<>();
    private final MutableLiveData<PromotionDto> selectedPromotion = new MutableLiveData<>();
    private final MediatorLiveData<Long> shippingFee = new MediatorLiveData<>();
    private final MutableLiveData<List<PromotionDto>> availablePromotions = new MutableLiveData<>(new ArrayList<>());

    private final MediatorLiveData<CartSummary> cartSummary = new MediatorLiveData<>();

    public CartViewModel(CartRepository cartRepository, PromotionRepository promotionRepository) {
        this.cartRepository = cartRepository;
        this.promotionRepository = promotionRepository;
        
        cartSummary.addSource(cart, c -> calculateSummary());
        cartSummary.addSource(selectedPromotion, p -> calculateSummary());
        cartSummary.addSource(shippingFee, f -> calculateSummary());
        cartSummary.addSource(customerId, id -> calculateSummary());
        
        shippingFee.addSource(cart, cartDto -> calculateShippingFee());
    }

    public LiveData<PromotionDto> getSelectedPromotion() {
        return selectedPromotion;
    }

    public void setCustomerId(String id) {
        this.customerId.setValue(id);
    }

    public LiveData<CartSummary> getCartSummary() {
        return cartSummary;
    }

    public static class CartSummary {
        public long subtotal;
        public long shippingFee;
        public long discount;
        public long total;
        public java.util.Map<String, Long> itemDiscounts = new java.util.HashMap<>();
    }

    private void calculateShippingFee() {
        CartDto currentCart = cart.getValue();
        if (currentCart == null || currentCart.getItems() == null || currentCart.getItems().isEmpty()) {
            shippingFee.setValue(0L);
            return;
        }
        
        long subtotal = 0;
        for (CartDto.CartItemDto item : currentCart.getItems()) {
            if (item.getProduct() != null) {
                subtotal += item.getProduct().getPrice() * item.getQuantity();
            }
        }
        
        if (subtotal >= 300000) {
            shippingFee.setValue(0L);
        } else {
            shippingFee.setValue(30000L);
        }
    }

    private void calculateSummary() {
        CartDto currentCart = cart.getValue();
        PromotionDto promo = selectedPromotion.getValue();
        Long currentShipping = shippingFee.getValue();
        String currentUserId = customerId.getValue();
        
        CartSummary summary = new CartSummary();
        summary.shippingFee = (currentShipping != null) ? currentShipping : 0;
        
        if (currentCart == null || currentCart.getItems() == null) {
            cartSummary.setValue(summary);
            return;
        }

        for (CartDto.CartItemDto item : currentCart.getItems()) {
            if (item.getProduct() != null) {
                summary.subtotal += item.getProduct().getPrice() * item.getQuantity();
            }
        }

        if (promo != null && summary.subtotal >= promo.getMinOrderValue()) {
            // Check usage limits
            boolean isEligible = true;
            PromotionDto.UsageDto usage = promo.getUsage();
            
            if (usage != null) {
                // Global usage limit
                if (promo.getUsageLimit() > 0 && usage.getOrderIds() != null) {
                    if (usage.getOrderIds().size() >= promo.getUsageLimit()) {
                        isEligible = false;
                    }
                }
                
                // Per user limit
                if (isEligible && promo.getUserLimit() > 0 && usage.getUserIds() != null && currentUserId != null) {
                    long userUsageCount = usage.getUserIds().stream()
                            .filter(id -> id.equals(currentUserId))
                            .count();
                    if (userUsageCount >= promo.getUserLimit()) {
                        isEligible = false;
                    }
                }
            }

            if (isEligible) {
                applyPromotion(promo, currentCart, summary);
            } else {
                // If not eligible anymore (e.g. limit reached), clear selection
                selectedPromotion.postValue(null);
                error.postValue("Promotion is no longer valid or usage limit reached.");
            }
        }

        summary.total = Math.max(0, summary.subtotal + summary.shippingFee - summary.discount);
        cartSummary.setValue(summary);
    }

    private void applyPromotion(PromotionDto promo, CartDto currentCart, CartSummary summary) {
        if ("Order".equalsIgnoreCase(promo.getScope())) {
            if ("percent".equalsIgnoreCase(promo.getDiscountType())) {
                summary.discount = (long) (summary.subtotal * (promo.getDiscountValue() / 100.0));
            } else if ("fixed".equalsIgnoreCase(promo.getDiscountType())) {
                summary.discount = promo.getDiscountValue();
            }
            if (promo.getMaxDiscountValue() > 0 && summary.discount > promo.getMaxDiscountValue()) {
                summary.discount = promo.getMaxDiscountValue();
            }
        } else if ("Shipping".equalsIgnoreCase(promo.getScope())) {
            if ("fixed".equalsIgnoreCase(promo.getDiscountType())) {
                summary.discount = Math.min(summary.shippingFee, (long) promo.getDiscountValue());
            }
        } else {
            // Category or Brand scope
            List<PromotionDto.TargetDto> targets = promo.getTargets();
            if (targets != null) {
                for (CartDto.CartItemDto item : currentCart.getItems()) {
                    if (item.getProduct() == null) continue;
                    boolean isTargeted = false;
                    for (PromotionDto.TargetDto target : targets) {
                        if ("Category".equalsIgnoreCase(target.getTargetType())) {
                            if (target.getTargetRef() != null && target.getTargetRef().contains(item.getProduct().getCategoryId())) {
                                isTargeted = true;
                                break;
                            }
                        } else if ("Subcategory".equalsIgnoreCase(target.getTargetType())) {
                            if (target.getTargetRef() != null && target.getTargetRef().contains(item.getProduct().getSubcategoryId())) {
                                isTargeted = true;
                                break;
                            }
                        }
                    }

                    if (isTargeted) {
                        long itemTotal = item.getProduct().getPrice() * item.getQuantity();
                        long itemDiscount = 0;
                        if ("percent".equalsIgnoreCase(promo.getDiscountType())) {
                            itemDiscount = (long) (itemTotal * (promo.getDiscountValue() / 100.0));
                        } else if ("fixed".equalsIgnoreCase(promo.getDiscountType())) {
                            itemDiscount = promo.getDiscountValue();
                        }
                        summary.discount += itemDiscount;
                        summary.itemDiscounts.put(item.getSku(), itemDiscount);
                    }
                }
            }
            if (promo.getMaxDiscountValue() > 0 && summary.discount > promo.getMaxDiscountValue()) {
                summary.discount = promo.getMaxDiscountValue();
            }
        }
    }

    public LiveData<List<PromotionDto>> getAvailablePromotions() {
        return availablePromotions;
    }

    public void fetchPromotions() {
        promotionRepository.getAllPromotions().enqueue(new Callback<List<PromotionDto>>() {
            @Override
            public void onResponse(Call<List<PromotionDto>> call, Response<List<PromotionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    availablePromotions.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<PromotionDto>> call, Throwable t) {
                // Handle failure
            }
        });
    }

    public void selectPromotion(PromotionDto promotion) {
        selectedPromotion.setValue(promotion);
    }

    public LiveData<CartDto> getCartData() {
        return cart;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void fetchCart(String customerId) {
        isLoading.setValue(true);
        cartRepository.getCart(customerId).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    cart.setValue(response.body());
                } else {
                    error.setValue("Failed to fetch cart");
                }
            }

            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }

    public void addItem(String customerId, String sku, int quantity, double selectedWeight) {
        isLoading.setValue(true);
        cartRepository.addItem(customerId, new CartItemRequestDto(sku, quantity, selectedWeight)).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    cart.setValue(response.body());
                } else {
                    error.setValue("Failed to add item");
                }
            }

            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }

    public void removeItem(String customerId, String sku) {
        isLoading.setValue(true);
        cartRepository.removeItem(customerId, sku).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    cart.setValue(response.body());
                } else {
                    error.setValue("Failed to remove item");
                }
            }

            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }

    public void updateQuantity(String customerId, String sku, int quantity) {
        isLoading.setValue(true);
        cartRepository.updateItemQuantity(customerId, sku, quantity).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    cart.setValue(response.body());
                } else {
                    error.setValue("Failed to update quantity");
                }
            }

            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }

    public void clearCart(String customerId) {
        isLoading.setValue(true);
        cartRepository.clearCart(customerId).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    CartDto emptyCart = new CartDto();
                    emptyCart.setCustomerId(customerId);
                    cart.setValue(emptyCart);
                } else {
                    error.setValue("Failed to clear cart");
                }
            }

            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }
}
