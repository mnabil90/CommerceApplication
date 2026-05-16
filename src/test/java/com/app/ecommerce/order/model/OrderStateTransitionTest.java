package com.app.ecommerce.order.model;

import com.app.ecommerce.cart.model.Cart;
import com.app.ecommerce.order.exception.InvalidOrderTransitionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateTransitionTest {

    @Test
    void validTransitionsFollowOrderStateMachine() {
        Cart cart = Cart.create();
        cart.addItem("product-1", 1, new BigDecimal("12.50"));

        Order order = Order.fromCart(cart);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);

        order.transitionTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        order.transitionTo(OrderStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void invalidTransitionFromCreatedToPaidThrows() {
        Cart cart = Cart.create();
        cart.addItem("product-1", 1, new BigDecimal("12.50"));

        Order order = Order.fromCart(cart);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PAID))
                .isInstanceOf(InvalidOrderTransitionException.class)
                .hasMessageContaining("invalid transition");
    }
}
