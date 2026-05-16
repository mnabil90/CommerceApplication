# CommerceApplication

## Overview

This Spring Boot application implements a minimal commerce flow with carts, checkout, orders, payment attempts, and webhook-based payment confirmation.

Key features:
- Clear domain model with `Order` state machine
- Payment handling with idempotent payment attempt creation and webhook processing
- Mock payment provider support for local testing
- Required endpoints for cart, checkout, order, payment initiation, and webhook handling

## Setup

Requirements:
- Java 21
- Maven

To build and run tests:

```bash
./mvnw test
```

To start the application:

```bash
./mvnw spring-boot:run
```

The application uses an in-memory H2 database by default.

## Available Endpoints

Base URL: `http://localhost:8080`

1. `POST /carts`
   - Create a new cart
2. `GET /carts/{cartId}`
   - Read cart contents and status
3. `POST /carts/{cartId}/items`
   - Add an item to a cart
   - Body: `{ "productId": "sku-123", "quantity": 1, "price": 9.99 }`
4. `POST /carts/{cartId}/checkout`
   - Checkout a locked cart and create an order
5. `GET /orders/{orderId}`
   - Read order state
6. `POST /orders/{orderId}/payment/start`
   - Start payment for an order
7. `POST /payments/webhook`
   - Payment provider webhook callback
   - Body: `{ "providerPaymentId": "...", "result": "CONFIRMED" }`

## Domain Model & State Machine

`Order` enforces valid state transitions through `Order.transitionTo(...)`.

Allowed transitions:
- `CREATED` → `PENDING_PAYMENT`
- `PENDING_PAYMENT` → `PAID` or `PAYMENT_FAILED`
- `PAYMENT_FAILED` → `PENDING_PAYMENT`
- `PAID` and `CANCELLED` are terminal states

## Payment Handling

Payment flow:
- `POST /orders/{orderId}/payment/start` creates a `PaymentAttempt`
- If an active pending attempt exists, the same attempt is returned (idempotency)
- Webhook processing uses `providerPaymentId` to locate the attempt
- Duplicate webhook callbacks are ignored if the attempt is already resolved

## Testing

Implemented tests:
- `OrderStateTransitionTest` validates domain transition logic and rejects invalid transitions
- `PaymentIntegrationTest` covers the happy path of checkout, payment start, webhook confirmation, and duplicate webhook handling

Run tests with:

```bash
./mvnw test
```

## Postman

A Postman collection is included at `CommerceApplication.postman_collection.json`.
Use `{{baseUrl}}` with value `http://localhost:8080` when importing.

## Assumptions

- The mock provider is simulated in-process; webhook delivery is handled via the `/payments/webhook` endpoint.
- Carts are locked at checkout to prevent modifications after order creation.
- A single pending payment attempt is allowed per order.
