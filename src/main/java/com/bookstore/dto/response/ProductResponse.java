package com.bookstore.dto.response;

import com.bookstore.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProductResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String publisher,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        Long categoryId,
        CategoryResponse category,
        String language,
        Integer pages,
        LocalDate publishedDate,
        String coverImageUrl,
        Double rating,
        Instant createdAt) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getIsbn(), p.getTitle(), p.getAuthor(), p.getPublisher(),
                p.getDescription(), p.getPrice(), p.getStockQuantity(), p.getCategory().getId(),
                CategoryResponse.from(p.getCategory()), p.getLanguage(), p.getPages(), p.getPublishedDate(),
                p.getCoverImageUrl(), p.getRating(), p.getCreatedAt());
    }
}
