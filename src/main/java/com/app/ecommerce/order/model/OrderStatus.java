package com.app.ecommerce.order.model;

import java.util.Set;

/**
 * Order state machine.
 *
 * Valid transitions:
 *   CREATED          → PENDING_PAYMENT
 *   PENDING_PAYMENT  → PAID | PAYMENT_FAILED
 *   PAYMENT_FAILED   → PENDING_PAYMENT   (retry)
 *   PAID             → (terminal)
 *   CANCELLED        → (terminal)
 *
 * Transition logic is owned by the Order aggregate — not by services.
 * This enum encodes allowed next states to make invalid transitions impossible to miss.
 */
public enum OrderStatus {

    CREATED(Set.of("PENDING_PAYMENT", "CANCELLED")),
    PENDING_PAYMENT(Set.of("PAID", "PAYMENT_FAILED", "CANCELLED")),
    PAYMENT_FAILED(Set.of("PENDING_PAYMENT", "CANCELLED")),
    PAID(Set.of()),          // terminal
    CANCELLED(Set.of());     // terminal

    private final Set<String> allowedNext;

    OrderStatus(Set<String> allowedNext) {
        this.allowedNext = allowedNext;
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext.contains(next.name());
    }
}