package com.app.ecommerce.checkout.service;

import com.app.ecommerce.cart.exception.CartNotFoundException;
import com.app.ecommerce.cart.model.Cart;
import com.app.ecommerce.cart.repository.CartRepository;
import com.app.ecommerce.order.model.Order;
import com.app.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout service.
 *
 * Responsibility: convert a locked cart into a new Order.
 *
 * Idempotency note:
 *   If the same cart is checked out twice (e.g. double-click), the second call
 *   returns the existing order rather than creating a duplicate.
 *   This is safe because the cart is already LOCKED, so its contents haven't changed.
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Order checkout(String cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found: " + cartId));

        // Idempotency: if an order already exists for this cart, return it
        return orderRepository.findByCartId(cartId)
                .orElseGet(() -> createOrder(cart));
    }

    private Order createOrder(Cart cart) {
        cart.lock(); // enforces cart is OPEN and non-empty
        cartRepository.save(cart);

        Order order = Order.fromCart(cart);
        return orderRepository.save(order);
    }
}