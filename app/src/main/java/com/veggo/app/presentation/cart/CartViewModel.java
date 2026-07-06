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

    public void removeItem(String customerId, String sku, double selectedWeight) {
        isLoading.setValue(true);
        cartRepository.removeItem(customerId, sku, selectedWeight).enqueue(new Callback<CartDto>() {
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

    public void updateQuantity(String customerId, String sku, int quantity, double selectedWeight) {
        isLoading.setValue(true);
        cartRepository.updateItemQuantity(customerId, sku, quantity, selectedWeight).enqueue(new Callback<CartDto>() {
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

    public void replaceCartItem(String customerId, String sku, double previousWeight, int quantity, double newWeight) {
        if (weightsEqual(previousWeight, newWeight)) {
            updateQuantity(customerId, sku, quantity, newWeight);
            return;
        }

        isLoading.setValue(true);
        cartRepository.removeItem(customerId, sku, previousWeight).enqueue(new Callback<CartDto>() {
            @Override
            public void onResponse(Call<CartDto> call, Response<CartDto> response) {
                if (!response.isSuccessful()) {
                    isLoading.setValue(false);
                    error.setValue("Failed to update cart item");
                    return;
                }

                cartRepository.addItem(customerId, new CartItemRequestDto(sku, quantity, newWeight))
                        .enqueue(new Callback<CartDto>() {
                            @Override
                            public void onResponse(Call<CartDto> addCall, Response<CartDto> addResponse) {
                                isLoading.setValue(false);
                                if (addResponse.isSuccessful() && addResponse.body() != null) {
                                    cart.setValue(addResponse.body());
                                } else {
                                    error.setValue("Failed to update cart item");
                                    fetchCart(customerId);
                                }
                            }

                            @Override
                            public void onFailure(Call<CartDto> addCall, Throwable t) {
                                isLoading.setValue(false);
                                error.setValue(t.getMessage());
                                fetchCart(customerId);
                            }
                        });
            }

            @Override
            public void onFailure(Call<CartDto> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }

    private static boolean weightsEqual(double left, double right) {
        return Math.abs(left - right) < 0.0005d;
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
