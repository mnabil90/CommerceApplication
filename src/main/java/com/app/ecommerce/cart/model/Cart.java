package com.app.ecommerce.cart.model;

import com.app.ecommerce.cart.exception.CartAlreadyLockedException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Cart aggregate root.
 *
 * Invariants enforced here:
 *  - Items can only be added/modified while cart is OPEN
 *  - A cart can only be checked out once (LOCKED state)
 */
@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor
public class Cart {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> items = new ArrayList<>();

    /** Factory method — always starts as OPEN */
    public static Cart create() {
        Cart cart = new Cart();
        cart.id = UUID.randomUUID().toString();
        cart.status = CartStatus.OPEN;
        return cart;
    }

    /**
     * Add or update an item.
     * Replaces quantity if the product already exists.
     */
    public void addItem(String productId, int quantity, java.math.BigDecimal price) {
        assertOpen();
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price == null || price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }

        items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.updateQuantityAndPrice(quantity, price),
                        () -> items.add(CartItem.of(this, productId, quantity, price))
                );
    }

    /**
     * Lock the cart for checkout. Idempotent if already locked.
     */
    public void lock() {
        assertOpen();
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot checkout an empty cart");
        }
        this.status = CartStatus.LOCKED;
    }

    public boolean isLocked() {
        return status == CartStatus.LOCKED;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    private void assertOpen() {
        if (status != CartStatus.OPEN) {
            throw new CartAlreadyLockedException("Cart " + id + " is locked and cannot be modified");
        }
    }
}