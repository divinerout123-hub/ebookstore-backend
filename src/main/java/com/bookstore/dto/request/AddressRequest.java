package com.bookstore.dto.request;

import com.bookstore.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotNull Long userId,
        @NotNull AddressType type,
        @NotBlank String fullName,
        String phone,
        @NotBlank String line1,
        String line2,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String postalCode,
        @NotBlank @Size(min = 2, max = 2) String country,
        Boolean isDefault) {
}
