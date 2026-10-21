package com.bookstore.service.impl;

import com.bookstore.dto.response.PageResponse;
import com.bookstore.dto.response.ProductResponse;
import com.bookstore.entity.Product;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.CategoryRepository;
import com.bookstore.repository.ProductRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.AccessChecker;
import com.bookstore.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AccessChecker accessChecker;

    @Override
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        return PageResponse.from(productRepository.findAll(pageable), ProductResponse::from);
    }

    @Override
    public ProductResponse getProductById(Long productId) {
        return productRepository.findById(productId)
                .map(ProductResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
    }

    @Override
    public PageResponse<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw ResourceNotFoundException.of("Category", categoryId);
        }
        return PageResponse.from(productRepository.findByCategoryId(categoryId, pageable), ProductResponse::from);
    }

    @Override
    public PageResponse<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return PageResponse.from(productRepository.search(keyword.trim(), pageable), ProductResponse::from);
    }

    /** Books from categories the user has bought from first, topped up with the best-rated unbought books. */
    @Override
    public List<ProductResponse> getRecommendedProducts(Long userId, int limit) {
        accessChecker.checkUser(userId);
        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.of("User", userId);
        }
        Pageable top = PageRequest.of(0, limit);
        Map<Long, Product> result = new LinkedHashMap<>();
        productRepository.findRecommendedByPurchaseHistory(userId, top).forEach(p -> result.put(p.getId(), p));
        if (result.size() < limit) {
            for (Product p : productRepository.findTopRatedNotPurchased(userId, top)) {
                if (result.size() >= limit) {
                    break;
                }
                result.putIfAbsent(p.getId(), p);
            }
        }
        return new ArrayList<>(result.values()).stream().map(ProductResponse::from).toList();
    }
}
