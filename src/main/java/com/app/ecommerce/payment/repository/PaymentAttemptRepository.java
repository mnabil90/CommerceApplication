package com.app.ecommerce.payment.repository;

import com.app.ecommerce.payment.model.PaymentAttempt;
import com.app.ecommerce.payment.model.PaymentAttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, String> {

    /**
     * Used to check if there's already an active payment in-flight for an order.
     * Prevents double-charging.
     */
    Optional<PaymentAttempt> findByOrderIdAndStatus(String orderId, PaymentAttemptStatus status);

    /**
     * Used during webhook processing to find the attempt by the provider's reference.
     * This is the idempotency lookup.
     */
    Optional<PaymentAttempt> findByProviderPaymentId(String providerPaymentId);
}