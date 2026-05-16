package com.app.ecommerce.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock Payment Provider.
 *
 * Simulates the behavior of a real payment gateway (e.g., Stripe, Adyen).
 *
 * In production this would be an HTTP client to an external service.
 * Here we:
 *  1. Store initiated payments in-memory
 *  2. Expose a trigger endpoint (via MockProviderController) to confirm/fail
 *  3. Call back our own /payments/webhook endpoint
 *
 * The RestTemplate call back to ourselves simulates the async webhook delivery
 * that a real provider would make.
 */
@Slf4j
@Component
public class MockPaymentProvider {

    private final String webhookUrl;
    private final RestTemplate restTemplate;

    /**
     * In-memory store of initiated payments: providerPaymentId → amount.
     * Represents the provider's side of the ledger.
     */
    private final Map<String, BigDecimal> initiatedPayments = new ConcurrentHashMap<>();

    public MockPaymentProvider(
            @Value("${mock.provider.webhook-url}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Called by PaymentService when starting a payment.
     * Registers the payment intent on the "provider side".
     */
    public void initiatePayment(String providerPaymentId, BigDecimal amount) {
        initiatedPayments.put(providerPaymentId, amount);
        log.info("[MockProvider] Payment initiated: id={}, amount={}", providerPaymentId, amount);
    }

    /**
     * Called manually via MockProviderController to trigger a result.
     * This simulates the provider deciding the payment outcome and sending a webhook.
     *
     * Decision to call our own webhook synchronously here (not async):
     * Keeps demo flow simple. In reality the provider would POST to our webhook URL
     * from their own infrastructure.
     */
    public void triggerResult(String providerPaymentId, boolean confirmed) {
        if (!hasPayment(providerPaymentId)) {
            throw new IllegalArgumentException("Unknown payment: " + providerPaymentId);
        }

        log.info("[MockProvider] Triggering {} for payment {}",
                confirmed ? "CONFIRMED" : "FAILED", providerPaymentId);

        WebhookPayload payload = new WebhookPayload(providerPaymentId, confirmed ? "CONFIRMED" : "FAILED");

        try {
            restTemplate.postForObject(webhookUrl, payload, Void.class);
        } catch (Exception e) {
            log.error("[MockProvider] Webhook delivery failed: {}", e.getMessage());
            throw new RuntimeException("Webhook delivery failed", e);
        }
    }

    public boolean hasPayment(String providerPaymentId) {
        return initiatedPayments.containsKey(providerPaymentId);
    }

    /**
     * Webhook payload — mirrors what a real provider would send.
     */
    public record WebhookPayload(String providerPaymentId, String result) {}
}