package com.bookstore.dto.response;

import com.bookstore.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productTitle,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {

    public static OrderItemResponse from(OrderItem i) {
        return new OrderItemResponse(i.getId(), i.getProduct().getId(), i.getProductTitle(), i.getQuantity(),
                i.getUnitPrice(), i.getSubtotal());
    }
}
