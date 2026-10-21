package com.bookstore.dto.response;

import com.bookstore.entity.Payment;
import com.bookstore.enums.PaymentMethod;
import com.bookstore.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long orderId,
        PaymentMethod method,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String transactionId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getOrder().getId(), p.getMethod(), p.getAmount(), p.getCurrency(),
                p.getStatus(), p.getTransactionId(), p.getFailureReason(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
