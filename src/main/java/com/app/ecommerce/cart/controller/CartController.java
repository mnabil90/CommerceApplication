package com.app.ecommerce.cart.controller;

import com.app.ecommerce.cart.model.Cart;
import com.app.ecommerce.cart.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/carts")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse createCart() {
        Cart cart = cartService.createCart();
        return CartResponse.from(cart);
    }

    @GetMapping("/carts/{cartId}")
    public CartResponse getCart(@PathVariable String cartId) {
        return CartResponse.from(cartService.getCart(cartId));
    }

    @PostMapping("/carts/{cartId}/items")
    @ResponseStatus(HttpStatus.OK)
    public CartResponse addItem(
            @PathVariable String cartId,
            @RequestBody @Valid AddItemRequest request) {
        Cart cart = cartService.addItem(cartId, request.productId(), request.quantity(), request.price());
        return CartResponse.from(cart);
    }

    public record AddItemRequest(
            @NotBlank String productId,
            @Min(1) int quantity,
            @NotNull @DecimalMin("0.01") BigDecimal price
    ) {}

    public record CartResponse(String id, String status, List<CartItemDto> items, BigDecimal total) {
        static CartResponse from(Cart cart) {
            List<CartItemDto> itemDtos = cart.getItems().stream()
                    .map(i -> new CartItemDto(i.getProductId(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal()))
                    .collect(Collectors.toList());
            BigDecimal total = itemDtos.stream()
                    .map(CartItemDto::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CartResponse(cart.getId(), cart.getStatus().name(), itemDtos, total);
        }
    }

    public record CartItemDto(String productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
}