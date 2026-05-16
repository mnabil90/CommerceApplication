package com.app.ecommerce.payment.exception;

public class PaymentAttemptNotFoundException extends RuntimeException {
    public PaymentAttemptNotFoundException(String message) {
        super(message);
    }
}