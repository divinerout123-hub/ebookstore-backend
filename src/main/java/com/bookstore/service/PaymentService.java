package com.bookstore.service;

import com.bookstore.dto.request.PaymentRequest;
import com.bookstore.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request, String idempotencyKey);

    PaymentResponse getPayment(Long paymentId);
}
