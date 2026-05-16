package com.app.ecommerce.payment.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single payment attempt for an order.
 *
 * Design decisions:
 *
 * 1. IDEMPOTENCY KEY (providerPaymentId):
 *    Each attempt gets a unique ID sent to the mock provider.
 *    When the provider sends a webhook, it includes this ID.
 *    We look up the attempt by providerPaymentId — so duplicate webhooks
 *    for the same attempt are handled safely (status already set, no re-transition).
 *
 * 2. ONE ACTIVE ATTEMPT AT A TIME:
 *    Before creating a new attempt, we check for any PENDING attempt on the order.
 *    This prevents double-charging if the client clicks "Pay" twice.
 *
 * 3. FAILED ATTEMPTS ARE KEPT:
 *    They are never deleted, providing a full audit trail.
 *    Only a new attempt can be started if the previous one failed.
 */
@Entity
@Table(name = "payment_attempts",
       indexes = {
           @Index(name = "idx_payment_attempt_provider_id", columnList = "providerPaymentId", unique = true),
           @Index(name = "idx_payment_attempt_order_id", columnList = "orderId")
       })
@Getter
@NoArgsConstructor
public class PaymentAttempt {

    @Id
    private String id;

    @Column(nullable = false)
    private String orderId;

    /**
     * Unique ID sent to (and returned by) the payment provider.
     * This is the idempotency key for webhook processing.
     */
    @Column(nullable = false, unique = true)
    private String providerPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentAttemptStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant resolvedAt;

    public static PaymentAttempt create(String orderId, BigDecimal amount) {
        PaymentAttempt attempt = new PaymentAttempt();
        attempt.id = UUID.randomUUID().toString();
        attempt.orderId = orderId;
        attempt.providerPaymentId = "pay_" + UUID.randomUUID().toString().replace("-", "");
        attempt.status = PaymentAttemptStatus.PENDING;
        attempt.amount = amount;
        attempt.createdAt = Instant.now();
        return attempt;
    }

    public void markConfirmed() {
        assertPending();
        this.status = PaymentAttemptStatus.CONFIRMED;
        this.resolvedAt = Instant.now();
    }

    public void markFailed() {
        assertPending();
        this.status = PaymentAttemptStatus.FAILED;
        this.resolvedAt = Instant.now();
    }

    public boolean isPending() {
        return status == PaymentAttemptStatus.PENDING;
    }

    public boolean isResolved() {
        return status == PaymentAttemptStatus.CONFIRMED || status == PaymentAttemptStatus.FAILED;
    }

    private void assertPending() {
        if (!isPending()) {
            throw new IllegalStateException(
                    "PaymentAttempt " + id + " is already resolved with status " + status
            );
        }
    }
}