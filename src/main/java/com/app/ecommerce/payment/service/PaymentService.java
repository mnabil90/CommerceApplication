package com.app.ecommerce.payment.service;

import com.app.ecommerce.mock.MockPaymentProvider;
import com.app.ecommerce.order.exception.OrderNotFoundException;
import com.app.ecommerce.order.model.Order;
import com.app.ecommerce.order.model.OrderStatus;
import com.app.ecommerce.order.repository.OrderRepository;
import com.app.ecommerce.payment.exception.PaymentAttemptNotFoundException;
import com.app.ecommerce.payment.model.PaymentAttempt;
import com.app.ecommerce.payment.model.PaymentAttemptStatus;
import com.app.ecommerce.payment.repository.PaymentAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PaymentService — orchestrates payment attempts and webhook processing.
 *
 * == Safety Guarantees ==
 *
 * 1. NO DOUBLE ACTIVE PAYMENTS:
 *    Before starting a payment, we check for any PENDING attempt on the order.
 *    If one exists, we return it (idempotent) rather than creating a duplicate.
 *
 * 2. IDEMPOTENT WEBHOOK PROCESSING:
 *    Webhook carries a providerPaymentId. We find the PaymentAttempt by that ID.
 *    If it's already resolved (CONFIRMED/FAILED), we silently return — no re-transition.
 *    This handles the "duplicate webhook sent twice" scenario correctly.
 *
 * 3. PESSIMISTIC LOCK ON ORDER:
 *    We lock the Order row during webhook processing to prevent concurrent updates
 *    if two webhook copies arrive simultaneously (race condition protection).
 *
 * 4. STATE MACHINE ENFORCED BY AGGREGATE:
 *    PaymentService calls order.transitionTo(), which enforces valid transitions.
 *    The service doesn't know or care about transition legality — the Order does.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final MockPaymentProvider mockPaymentProvider;

    /**
     * Starts a payment for an order.
     *
     * @return the created (or existing pending) PaymentAttempt
     */
    @Transactional
    public PaymentAttempt startPayment(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        // Guard: only CREATED or PAYMENT_FAILED orders can initiate a new payment
        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("Order " + orderId + " is already paid");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order " + orderId + " is cancelled");
        }

        // Guard: if there's already a pending attempt, return it — no double charge
        return paymentAttemptRepository
                .findByOrderIdAndStatus(orderId, PaymentAttemptStatus.PENDING)
                .orElseGet(() -> initNewPaymentAttempt(order));
    }

    private PaymentAttempt initNewPaymentAttempt(Order order) {
        PaymentAttempt attempt = PaymentAttempt.create(order.getId(), order.getTotalAmount());
        paymentAttemptRepository.save(attempt);

        // Transition order to PENDING_PAYMENT
        order.transitionTo(OrderStatus.PENDING_PAYMENT);
        orderRepository.save(order);

        // Notify mock provider asynchronously (fire-and-forget in this simple impl)
        mockPaymentProvider.initiatePayment(attempt.getProviderPaymentId(), order.getTotalAmount());

        log.info("Payment attempt {} started for order {}", attempt.getProviderPaymentId(), order.getId());
        return attempt;
    }

    /**
     * Processes an incoming webhook from the payment provider.
     *
     * This method is idempotent:
     *  - If the attempt is already resolved, nothing happens.
     *  - The order state only changes once per attempt, regardless of how many
     *    times the webhook arrives.
     */
    @Transactional
    public void handleWebhook(String providerPaymentId, boolean confirmed) {
        PaymentAttempt attempt = paymentAttemptRepository
                .findByProviderPaymentId(providerPaymentId)
                .orElseThrow(() -> new PaymentAttemptNotFoundException(
                        "No payment attempt found for providerPaymentId: " + providerPaymentId));

        // IDEMPOTENCY CHECK: if already resolved, this is a duplicate webhook — ignore it
        if (attempt.isResolved()) {
            log.info("Duplicate webhook received for providerPaymentId={}, status={}. Ignoring.",
                    providerPaymentId, attempt.getStatus());
            return;
        }

        // Lock the order row for the duration of this transaction
        Order order = orderRepository.findByIdForUpdate(attempt.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + attempt.getOrderId()));

        if (confirmed) {
            attempt.markConfirmed();
            order.transitionTo(OrderStatus.PAID);
            log.info("Payment confirmed for order {}", order.getId());
        } else {
            attempt.markFailed();
            order.transitionTo(OrderStatus.PAYMENT_FAILED);
            log.info("Payment failed for order {}", order.getId());
        }

        paymentAttemptRepository.save(attempt);
        orderRepository.save(order);
    }
}