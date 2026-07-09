package com.veggo.app.core.utils;

import com.veggo.app.data.remote.dto.CartDto;

public final class CartCountUtils {

    private CartCountUtils() {
    }

    /**
     * Số loại sản phẩm trong giỏ (mỗi dòng SKU + khối lượng = 1),
     * không phải tổng quantity của tất cả dòng.
     */
    public static int countLineItems(CartDto cartDto) {
        if (cartDto == null || cartDto.getItems() == null) {
            return 0;
        }
        return cartDto.getItems().size();
    }
}
