package com.app.ecommerce.payment;

import com.app.ecommerce.cart.model.Cart;
import com.app.ecommerce.cart.service.CartService;
import com.app.ecommerce.order.model.Order;
import com.app.ecommerce.order.model.OrderStatus;
import com.app.ecommerce.order.repository.OrderRepository;
import com.app.ecommerce.checkout.service.CheckoutService;
import com.app.ecommerce.payment.model.PaymentAttempt;
import com.app.ecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PaymentIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void happyPathPaymentWebhookAndDuplicateWebhookAreHandledIdempotently() {
        Cart cart = cartService.createCart();
        cartService.addItem(cart.getId(), "sku-123", 2, new BigDecimal("15.00"));

        Order order = checkoutService.checkout(cart.getId());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);

        PaymentAttempt attempt = paymentService.startPayment(order.getId());
        assertThat(attempt.isPending()).isTrue();

        paymentService.handleWebhook(attempt.getProviderPaymentId(), true);
        paymentService.handleWebhook(attempt.getProviderPaymentId(), true);

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }
}
