package com.bookstore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CardDetails(
        @NotBlank String cardHolderName,
        @NotBlank @Pattern(regexp = "^[0-9]{13,19}$", message = "must be 13-19 digits") String cardNumber,
        @NotNull @Min(1) @Max(12) Integer expiryMonth,
        @NotNull @Min(2024) Integer expiryYear,
        @NotBlank @Pattern(regexp = "^[0-9]{3,4}$", message = "must be 3 or 4 digits") String cvv) {

    @Override
    public String toString() {
        return "CardDetails[cardHolderName=" + cardHolderName + ", cardNumber=****, cvv=***]";
    }
}
