package com.bookstore.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull Long userId,
        @NotNull Long shippingAddressId,
        Long billingAddressId,
        @Size(max = 500) String notes) {
}
