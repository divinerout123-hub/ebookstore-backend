package com.bookstore.service.impl;

import com.bookstore.dto.request.AddCartItemRequest;
import com.bookstore.dto.request.UpdateCartItemRequest;
import com.bookstore.dto.response.CartResponse;
import com.bookstore.entity.Cart;
import com.bookstore.entity.CartItem;
import com.bookstore.entity.Product;
import com.bookstore.exception.ConflictException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.CartItemRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.ProductRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.AccessChecker;
import com.bookstore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AccessChecker accessChecker;

    @Override
    public CartResponse getCart(Long userId) {
        accessChecker.checkUser(userId);
        return CartResponse.from(getOrCreateCart(userId));
    }

    @Override
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        accessChecker.checkUser(userId);
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> ResourceNotFoundException.of("Product", request.productId()));

        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst().orElse(null);
        int newQuantity = request.quantity() + (existing != null ? existing.getQuantity() : 0);
        ensureStock(product, newQuantity);

        if (existing != null) {
            existing.setQuantity(newQuantity);
        } else {
            CartItem item = CartItem.builder().cart(cart).product(product).quantity(newQuantity).build();
            cartItemRepository.saveAndFlush(item);
            cart.getItems().add(item);
        }
        return CartResponse.from(cart);
    }

    @Override
    public CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        accessChecker.checkUser(userId);
        Cart cart = getOrCreateCart(userId);
        CartItem item = findItem(cart, itemId);
        ensureStock(item.getProduct(), request.quantity());
        item.setQuantity(request.quantity());
        return CartResponse.from(cart);
    }

    @Override
    public CartResponse removeItem(Long userId, Long itemId) {
        accessChecker.checkUser(userId);
        Cart cart = getOrCreateCart(userId);
        CartItem item = findItem(cart, itemId);
        cart.getItems().remove(item);
        return CartResponse.from(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
            return cartRepository.saveAndFlush(Cart.builder().user(user).build());
        });
    }

    private CartItem findItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Cart item", itemId));
    }

    private void ensureStock(Product product, int quantity) {
        if (quantity > product.getStockQuantity()) {
            throw new ConflictException("Only " + product.getStockQuantity() + " copies of '"
                    + product.getTitle() + "' are in stock");
        }
    }
}
