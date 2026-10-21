package com.bookstore.dto.response;

import com.bookstore.entity.Order;
import com.bookstore.entity.Payment;
import com.bookstore.enums.OrderStatus;
import com.bookstore.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        OrderStatus status,
        List<OrderItemResponse> items,
        AddressResponse shippingAddress,
        AddressResponse billingAddress,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal tax,
        BigDecimal totalAmount,
        String notes,
        String cancellationReason,
        Long paymentId,
        Instant createdAt,
        Instant updatedAt) {

    public static OrderResponse from(Order o) {
        Long paymentId = o.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getId)
                .findFirst().orElse(null);
        return new OrderResponse(o.getId(), o.getUser().getId(), o.getStatus(),
                o.getItems().stream().map(OrderItemResponse::from).toList(),
                o.getShippingAddress() != null ? AddressResponse.from(o.getShippingAddress()) : null,
                o.getBillingAddress() != null ? AddressResponse.from(o.getBillingAddress()) : null,
                o.getSubtotal(), o.getShippingCost(), o.getTax(), o.getTotalAmount(), o.getNotes(),
                o.getCancellationReason(), paymentId, o.getCreatedAt(), o.getUpdatedAt());
    }
}
