package com.app.ecommerce.cart.service;

import com.app.ecommerce.cart.exception.CartNotFoundException;
import com.app.ecommerce.cart.model.Cart;
import com.app.ecommerce.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    @Transactional
    public Cart createCart() {
        Cart cart = Cart.create();
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart addItem(String cartId, String productId, int quantity, BigDecimal price) {
        Cart cart = findOrThrow(cartId);
        cart.addItem(productId, quantity, price);
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public Cart getCart(String cartId) {
        return findOrThrow(cartId);
    }

    private Cart findOrThrow(String cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found: " + cartId));
    }
}