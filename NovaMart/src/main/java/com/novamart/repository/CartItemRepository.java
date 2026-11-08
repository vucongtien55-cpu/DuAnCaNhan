package com.novamart.repository;

import com.novamart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Lấy danh sách tất cả mặt hàng có trong một giỏ hàng
    List<CartItem> findByCartId(Long cartId);

    // Tìm dòng mặt hàng cụ thể theo giỏ hàng và biến thể (dùng khi cộng dồn số lượng)
    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

    // Xóa sạch tất cả mặt hàng trong giỏ sau khi đặt hàng thành công
    void deleteByCartId(Long cartId);
}