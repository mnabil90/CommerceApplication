package com.app.ecommerce.order.model;

import com.app.ecommerce.cart.model.Cart;
import com.app.ecommerce.cart.model.CartItem;
import com.app.ecommerce.order.exception.InvalidOrderTransitionException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Order aggregate root.
 *
 * All state transitions are gated through {@link #transitionTo(OrderStatus)}.
 * No service or controller can set status directly — encapsulation enforces invariants.
 *
 * Key decisions:
 *  - Order captures a snapshot of cart items at checkout time.
 *    Cart changes after checkout are irrelevant.
 *  - Total amount is computed and stored at creation — immutable thereafter.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    private String id;

    @Column(nullable = false)
    private String cartId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderLine> lines = new ArrayList<>();

    /**
     * Create an Order from a locked Cart.
     * This is the only way to construct a valid Order.
     */
    public static Order fromCart(Cart cart) {
        Order order = new Order();
        order.id = UUID.randomUUID().toString();
        order.cartId = cart.getId();
        order.status = OrderStatus.CREATED;
        order.createdAt = Instant.now();
        order.updatedAt = Instant.now();

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            order.lines.add(OrderLine.of(order, item));
            total = total.add(item.getSubtotal());
        }
        order.totalAmount = total;
        return order;
    }

    /**
     * Central transition gate. Every state change goes through here.
     * Throws {@link InvalidOrderTransitionException} if the transition is illegal.
     */
    public void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidOrderTransitionException(
                    "Order " + id + ": invalid transition from " + status + " to " + next
            );
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}