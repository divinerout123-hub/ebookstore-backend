package com.bookstore.dto.response;

import com.bookstore.entity.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long productId,
        String productTitle,
        String coverImageUrl,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {

    public static CartItemResponse from(CartItem i) {
        BigDecimal price = i.getProduct().getPrice();
        return new CartItemResponse(i.getId(), i.getProduct().getId(), i.getProduct().getTitle(),
                i.getProduct().getCoverImageUrl(), i.getQuantity(), price,
                price.multiply(BigDecimal.valueOf(i.getQuantity())));
    }
}
