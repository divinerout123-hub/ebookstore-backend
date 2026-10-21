package com.bookstore.dto.response;

import com.bookstore.entity.Category;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        Long parentCategoryId,
        Instant createdAt) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(),
                c.getParent() != null ? c.getParent().getId() : null, c.getCreatedAt());
    }
}
