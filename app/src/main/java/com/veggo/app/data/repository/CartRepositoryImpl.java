package com.veggo.app.data.repository;

import com.veggo.app.data.remote.api.CartApi;
import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.CartItemRequestDto;
import com.veggo.app.domain.repository.CartRepository;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;

public class CartRepositoryImpl implements CartRepository {
    private final CartApi cartApi;

    public CartRepositoryImpl(CartApi cartApi) {
        this.cartApi = cartApi;
    }

    @Override
    public Call<CartDto> getCart(String customerId) {
        return cartApi.getCart(customerId);
    }

    @Override
    public Call<CartDto> addItem(String customerId, CartItemRequestDto item) {
        return cartApi.addItem(customerId, item);
    }

    @Override
    public Call<CartDto> updateItemQuantity(String customerId, String sku, int quantity) {
        Map<String, Integer> body = new HashMap<>();
        body.put("quantity", quantity);
        return cartApi.updateItemQuantity(customerId, sku, body);
    }

    @Override
    public Call<CartDto> removeItem(String customerId, String sku) {
        return cartApi.removeItem(customerId, sku);
    }

    @Override
    public Call<CartDto> clearCart(String customerId) {
        return cartApi.clearCart(customerId);
    }
}
