package com.veggo.app.data.mapper;

import com.veggo.app.data.remote.dto.CartDto;
import com.veggo.app.domain.model.CartItem;
import com.veggo.app.domain.model.Product;

import java.util.ArrayList;
import java.util.List;

public final class CartMapper {
    private CartMapper() {
    }

    public static List<CartItem> fromDto(CartDto dto) {
        List<CartItem> items = new ArrayList<>();
        if (dto != null && dto.getItems() != null) {
            for (CartDto.CartItemDto itemDto : dto.getItems()) {
                if (itemDto.getProduct() != null) {
                    Product product = ProductMapper.fromDto(itemDto.getProduct());
                    items.add(new CartItem(
                            product,
                            itemDto.getQuantity(),
                            itemDto.getSelectedWeight()
                    ));
                }
            }
        }
        return items;
    }
}
