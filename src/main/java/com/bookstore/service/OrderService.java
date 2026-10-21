package com.bookstore.service;

import com.bookstore.dto.request.CancelOrderRequest;
import com.bookstore.dto.request.CreateOrderRequest;
import com.bookstore.dto.response.OrderResponse;
import com.bookstore.dto.response.PageResponse;
import com.bookstore.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(Long orderId);

    PageResponse<OrderResponse> getUserOrders(Long userId, OrderStatus status, Pageable pageable);

    OrderResponse cancelOrder(Long orderId, CancelOrderRequest request);
}
