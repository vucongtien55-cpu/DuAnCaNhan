package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.CartItem;

import java.util.List;

public interface CartItemService {

    // Lấy giỏ hàng của chính người dùng.
    List<CartItem> getMyCart(Long actorUserId);

    // Thêm sản phẩm vào giỏ hoặc cập nhật số lượng nếu đã có.
    CartItem addOrUpdateItem(Long actorUserId, Long productId, Integer quantity);

    // Cập nhật số lượng một dòng trong giỏ của chính người dùng.
    CartItem updateQuantity(Long actorUserId, Long cartItemId, Integer quantity);

    // Xóa một dòng khỏi giỏ của chính người dùng.
    void removeItem(Long actorUserId, Long cartItemId);

    // Xóa toàn bộ giỏ hàng của chính người dùng.
    void clearCart(Long actorUserId);
}
