package com.bookstore.service;

import com.bookstore.dto.response.PageResponse;
import com.bookstore.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    PageResponse<ProductResponse> getAllProducts(Pageable pageable);

    ProductResponse getProductById(Long productId);

    PageResponse<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);

    PageResponse<ProductResponse> searchProducts(String keyword, Pageable pageable);

    List<ProductResponse> getRecommendedProducts(Long userId, int limit);
}
