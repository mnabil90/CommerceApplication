package com.app.ecommerce.order.controller;

import com.app.ecommerce.checkout.service.CheckoutService;
import com.app.ecommerce.order.exception.OrderNotFoundException;
import com.app.ecommerce.order.model.Order;
import com.app.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderRepository orderRepository;

    @PostMapping("/carts/{cartId}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(@PathVariable String cartId) {
        Order order = checkoutService.checkout(cartId);
        return OrderResponse.from(order);
    }

    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrder(@PathVariable String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return OrderResponse.from(order);
    }

    public record OrderResponse(
            String id,
            String cartId,
            String status,
            BigDecimal totalAmount,
            Instant createdAt,
            Instant updatedAt,
            List<OrderLineDto> lines
    ) {
        static OrderResponse from(Order order) {
            List<OrderLineDto> lineDtos = order.getLines().stream()
                    .map(l -> new OrderLineDto(l.getProductId(), l.getQuantity(), l.getUnitPrice(), l.getSubtotal()))
                    .collect(Collectors.toList());
            return new OrderResponse(
                    order.getId(),
                    order.getCartId(),
                    order.getStatus().name(),
                    order.getTotalAmount(),
                    order.getCreatedAt(),
                    order.getUpdatedAt(),
                    lineDtos
            );
        }
    }

    public record OrderLineDto(String productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
}