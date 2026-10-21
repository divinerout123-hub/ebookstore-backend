package com.bookstore.dto.response;

import com.bookstore.entity.Address;
import com.bookstore.enums.AddressType;

import java.time.Instant;

public record AddressResponse(
        Long id,
        Long userId,
        AddressType type,
        String fullName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean isDefault,
        Instant createdAt) {

    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getUser().getId(), a.getType(), a.getFullName(), a.getPhone(),
                a.getLine1(), a.getLine2(), a.getCity(), a.getState(), a.getPostalCode(), a.getCountry(),
                a.isDefaultAddress(), a.getCreatedAt());
    }
}
