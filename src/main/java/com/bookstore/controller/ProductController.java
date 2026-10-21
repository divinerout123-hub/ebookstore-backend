package com.bookstore.controller;

import com.bookstore.dto.response.PageResponse;
import com.bookstore.dto.response.ProductResponse;
import com.bookstore.service.ProductService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public PageResponse<ProductResponse> getAllProducts(
            @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return productService.getAllProducts(pageable);
    }

    // Fixed paths are declared before "/{productId}" for readability; Spring prefers literal matches anyway.
    @GetMapping("/search")
    public PageResponse<ProductResponse> searchProducts(
            @RequestParam @NotBlank String keyword,
            @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return productService.searchProducts(keyword, pageable);
    }

    @GetMapping("/category/{categoryId}")
    public PageResponse<ProductResponse> getProductsByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return productService.getProductsByCategory(categoryId, pageable);
    }

    @GetMapping("/recommended/{userId}")
    public List<ProductResponse> getRecommendedProducts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return productService.getRecommendedProducts(userId, limit);
    }

    @GetMapping("/{productId}")
    public ProductResponse getProductById(@PathVariable Long productId) {
        return productService.getProductById(productId);
    }
}
