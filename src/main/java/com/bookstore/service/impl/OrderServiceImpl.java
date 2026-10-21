package com.bookstore.service.impl;

import com.bookstore.dto.request.CancelOrderRequest;
import com.bookstore.dto.request.CreateOrderRequest;
import com.bookstore.dto.response.OrderResponse;
import com.bookstore.dto.response.PageResponse;
import com.bookstore.entity.Address;
import com.bookstore.entity.Cart;
import com.bookstore.entity.CartItem;
import com.bookstore.entity.Order;
import com.bookstore.entity.OrderItem;
import com.bookstore.entity.Product;
import com.bookstore.entity.User;
import com.bookstore.enums.OrderStatus;
import com.bookstore.enums.PaymentStatus;
import com.bookstore.exception.BadRequestException;
import com.bookstore.exception.ConflictException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.AddressRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.ProductRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.security.AccessChecker;
import com.bookstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Set<OrderStatus> CANCELLABLE =
            EnumSet.of(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.PROCESSING);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AccessChecker accessChecker;

    @Value("${app.order.tax-rate}")
    private BigDecimal taxRate;

    @Value("${app.order.shipping-cost}")
    private BigDecimal shippingCost;

    @Value("${app.order.free-shipping-threshold}")
    private BigDecimal freeShippingThreshold;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        accessChecker.checkUser(request.userId());
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", request.userId()));

        Cart cart = cartRepository.findByUserId(user.getId())
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        Address shipping = loadOwnedAddress(request.shippingAddressId(), user.getId());
        Address billing = request.billingAddressId() == null
                ? shipping : loadOwnedAddress(request.billingAddressId(), user.getId());

        Order order = Order.builder()
                .user(user)
                .shippingAddress(shipping)
                .billingAddress(billing)
                .notes(request.notes())
                .build();

        // Lock products in id order to avoid deadlocks between concurrent checkouts.
        List<CartItem> cartItems = cart.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .toList();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            Product product = productRepository.findByIdForUpdate(ci.getProduct().getId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", ci.getProduct().getId()));
            if (product.getStockQuantity() < ci.getQuantity()) {
                throw new ConflictException("Insufficient stock for '" + product.getTitle() + "': requested "
                        + ci.getQuantity() + ", available " + product.getStockQuantity());
            }
            product.setStockQuantity(product.getStockQuantity() - ci.getQuantity());

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            order.addItem(OrderItem.builder()
                    .product(product)
                    .productTitle(product.getTitle())
                    .quantity(ci.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(lineTotal)
                    .build());
        }

        BigDecimal shippingFee = subtotal.compareTo(freeShippingThreshold) >= 0 ? BigDecimal.ZERO : shippingCost;
        BigDecimal tax = subtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        order.setSubtotal(subtotal);
        order.setShippingCost(shippingFee);
        order.setTax(tax);
        order.setTotalAmount(subtotal.add(shippingFee).add(tax));

        Order saved = orderRepository.save(order);
        cart.getItems().clear();
        return OrderResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = findOrder(orderId);
        accessChecker.checkUser(order.getUser().getId());
        return OrderResponse.from(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(Long userId, OrderStatus status, Pageable pageable) {
        accessChecker.checkUser(userId);
        if (!userRepository.existsById(userId)) {
            throw ResourceNotFoundException.of("User", userId);
        }
        Page<Order> page = status == null
                ? orderRepository.findByUserId(userId, pageable)
                : orderRepository.findByUserIdAndStatus(userId, status, pageable);
        return PageResponse.from(page, OrderResponse::from);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, CancelOrderRequest request) {
        Order order = findOrder(orderId);
        accessChecker.checkUser(order.getUser().getId());
        if (!CANCELLABLE.contains(order.getStatus())) {
            throw new ConflictException("Order in status " + order.getStatus() + " cannot be cancelled");
        }

        order.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                .forEach(item -> productRepository.findByIdForUpdate(item.getProduct().getId())
                        .ifPresent(p -> p.setStockQuantity(p.getStockQuantity() + item.getQuantity())));
        order.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .forEach(p -> p.setStatus(PaymentStatus.REFUNDED));

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request != null ? request.reason() : null);
        return OrderResponse.from(order);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
    }

    private Address loadOwnedAddress(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> ResourceNotFoundException.of("Address", addressId));
        if (!address.getUser().getId().equals(userId)) {
            throw new BadRequestException("Address " + addressId + " does not belong to the user");
        }
        return address;
    }
}
