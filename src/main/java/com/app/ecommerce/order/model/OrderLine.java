package com.app.ecommerce.order.model;

import com.app.ecommerce.cart.model.CartItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Snapshot of a CartItem at the moment of checkout.
 * Immutable after creation — price history is preserved correctly.
 */
@Entity
@Table(name = "order_lines")
@Getter
@NoArgsConstructor
public class OrderLine {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    static OrderLine of(Order order, CartItem cartItem) {
        OrderLine line = new OrderLine();
        line.id = UUID.randomUUID().toString();
        line.order = order;
        line.productId = cartItem.getProductId();
        line.quantity = cartItem.getQuantity();
        line.unitPrice = cartItem.getUnitPrice();
        return line;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}