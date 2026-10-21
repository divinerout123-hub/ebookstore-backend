package com.bookstore.service.impl;

import com.bookstore.dto.request.CardDetails;
import com.bookstore.dto.request.PaymentRequest;
import com.bookstore.dto.response.PaymentResponse;
import com.bookstore.entity.Order;
import com.bookstore.entity.Payment;
import com.bookstore.enums.OrderStatus;
import com.bookstore.enums.PaymentMethod;
import com.bookstore.enums.PaymentStatus;
import com.bookstore.exception.BadRequestException;
import com.bookstore.exception.ConflictException;
import com.bookstore.exception.PaymentDeclinedException;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.PaymentRepository;
import com.bookstore.security.AccessChecker;
import com.bookstore.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Payment processing. The gateway is simulated: card payments are validated locally (Luhn + expiry),
 * wallets/bank methods always succeed, and cash-on-delivery stays PENDING until delivery.
 * Replace {@link #authorizeCard} with a real gateway integration for production use.
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AccessChecker accessChecker;

    /** A declined payment is persisted as FAILED, so the exception must not roll back the transaction. */
    @Override
    @Transactional(noRollbackFor = PaymentDeclinedException.class)
    public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                Payment payment = existing.get();
                accessChecker.checkUser(payment.getOrder().getUser().getId());
                return PaymentResponse.from(payment);
            }
        }

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> ResourceNotFoundException.of("Order", request.orderId()));
        accessChecker.checkUser(order.getUser().getId());

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Order in status " + order.getStatus() + " is not payable");
        }
        if (request.amount().compareTo(order.getTotalAmount()) != 0) {
            throw new BadRequestException("Amount must equal the order total of " + order.getTotalAmount());
        }
        boolean cardMethod = request.method() == PaymentMethod.CREDIT_CARD
                || request.method() == PaymentMethod.DEBIT_CARD;
        if (cardMethod && request.card() == null) {
            throw new BadRequestException("Card details are required for " + request.method());
        }

        Payment payment = Payment.builder()
                .order(order)
                .method(request.method())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .idempotencyKey(idempotencyKey)
                .status(PaymentStatus.PENDING)
                .build();

        if (request.method() == PaymentMethod.CASH_ON_DELIVERY) {
            order.setStatus(OrderStatus.PROCESSING);
            return PaymentResponse.from(paymentRepository.save(payment));
        }

        String declineReason = cardMethod ? authorizeCard(request.card()) : null;
        if (declineReason != null) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(declineReason);
            paymentRepository.save(payment);
            throw new PaymentDeclinedException("Payment declined: " + declineReason);
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId("txn_" + UUID.randomUUID().toString().replace("-", ""));
        order.setStatus(OrderStatus.PAID);
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", paymentId));
        accessChecker.checkUser(payment.getOrder().getUser().getId());
        return PaymentResponse.from(payment);
    }

    /** Returns a decline reason, or null when the card is accepted. */
    private String authorizeCard(CardDetails card) {
        if (YearMonth.of(card.expiryYear(), card.expiryMonth()).isBefore(YearMonth.now())) {
            return "Card has expired";
        }
        if (!passesLuhn(card.cardNumber())) {
            return "Invalid card number";
        }
        return null;
    }

    private boolean passesLuhn(String number) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (doubleDigit && (digit *= 2) > 9) {
                digit -= 9;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
