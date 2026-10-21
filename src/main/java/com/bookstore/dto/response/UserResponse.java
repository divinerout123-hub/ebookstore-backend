package com.bookstore.dto.response;

import com.bookstore.entity.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        Instant createdAt,
        Instant updatedAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(), u.getPhone(),
                u.getRole().name(), u.getCreatedAt(), u.getUpdatedAt());
    }
}
