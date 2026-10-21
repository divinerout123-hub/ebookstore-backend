package com.bookstore.dto.response;

import com.bookstore.entity.Cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
        Long id,
        Long userId,
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal totalAmount,
        Instant updatedAt) {

    public static CartResponse from(Cart c) {
        List<CartItemResponse> items = c.getItems().stream().map(CartItemResponse::from).toList();
        int totalItems = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal total = items.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(c.getId(), c.getUser().getId(), items, totalItems, total, c.getUpdatedAt());
    }
}
