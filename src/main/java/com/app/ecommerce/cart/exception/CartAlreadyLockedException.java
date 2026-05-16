package com.app.ecommerce.cart.exception;

public class CartAlreadyLockedException extends RuntimeException {
    public CartAlreadyLockedException(String message) {
        super(message);
    }
}