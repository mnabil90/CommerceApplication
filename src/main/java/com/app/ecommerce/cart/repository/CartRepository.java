package com.app.ecommerce.cart.repository;


import com.app.ecommerce.cart.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, String> {
}