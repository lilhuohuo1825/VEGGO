package com.veggo.app.presentation.cart;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.CartItemRequestDto;
import com.veggo.app.domain.repository.CartRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartViewModel extends ViewModel {
    private final CartRepository cartRepository;
    private final MutableLiveData<CartDto> cart = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public CartViewModel(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
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
