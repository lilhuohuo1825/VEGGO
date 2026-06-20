package com.veggo.app.domain.repository;

import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.data.remote.dto.CartItemRequestDto;

import retrofit2.Call;

public interface CartRepository {
    Call<CartDto> getCart(String customerId);
    Call<CartDto> addItem(String customerId, CartItemRequestDto item);
    Call<CartDto> updateItemQuantity(String customerId, String sku, int quantity);
    Call<CartDto> removeItem(String customerId, String sku);
    Call<CartDto> clearCart(String customerId);
}
