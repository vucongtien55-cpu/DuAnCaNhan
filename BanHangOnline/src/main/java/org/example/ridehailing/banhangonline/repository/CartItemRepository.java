package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Lấy toàn bộ sản phẩm trong giỏ hàng của người dùng.
    List<CartItem> findByUser_Id(Long userId);

    // Tìm một sản phẩm cụ thể trong giỏ hàng của người dùng.
    Optional<CartItem> findByUser_IdAndProduct_Id(Long userId, Long productId);
}
