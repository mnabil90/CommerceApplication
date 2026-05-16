package com.app.ecommerce.payment.controller;

import com.app.ecommerce.payment.model.PaymentAttempt;
import com.app.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/orders/{orderId}/payment/start")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentAttemptResponse startPayment(@PathVariable String orderId) {
        PaymentAttempt attempt = paymentService.startPayment(orderId);
        return PaymentAttemptResponse.from(attempt);
    }

    // ── POST /payments/webhook ───────────────────────────────────────────────
    //
    // Called by the mock provider (and potentially retried).
    // Must be idempotent — processing the same payload twice is safe.

    @PostMapping("/payments/webhook")
    @ResponseStatus(HttpStatus.OK)
    public void handleWebhook(@RequestBody @Valid WebhookRequest request) {
        boolean confirmed = "CONFIRMED".equalsIgnoreCase(request.result());
        paymentService.handleWebhook(request.providerPaymentId(), confirmed);
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record WebhookRequest(
            @NotBlank String providerPaymentId,
            @NotBlank String result   // "CONFIRMED" or "FAILED"
    ) {}

    public record PaymentAttemptResponse(
            String id,
            String orderId,
            String providerPaymentId,
            String status,
            BigDecimal amount,
            Instant createdAt
    ) {
        static PaymentAttemptResponse from(PaymentAttempt attempt) {
            return new PaymentAttemptResponse(
                    attempt.getId(),
                    attempt.getOrderId(),
                    attempt.getProviderPaymentId(),
                    attempt.getStatus().name(),
                    attempt.getAmount(),
                    attempt.getCreatedAt()
            );
        }
    }
}