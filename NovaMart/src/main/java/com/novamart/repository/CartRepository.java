package com.novamart.repository;

import com.novamart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Tìm giỏ hàng hiện tại của người dùng theo ID người dùng
    Optional<Cart> findByUserId(Long userId);
}