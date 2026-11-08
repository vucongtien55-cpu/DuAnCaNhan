package com.novamart.service;

import com.novamart.dto.request.AddToCartRequest;
import com.novamart.dto.response.CartResponse;

public interface CartService {

    // Xem chi tiết giỏ hàng hiện tại của khách hàng (đã gom nhóm theo Shop)
    CartResponse getMyCart(String customerEmail);

    // Thêm biến thể sản phẩm vào giỏ (cộng dồn nếu đã tồn tại)
    CartResponse addToCart(String customerEmail, AddToCartRequest request);

    // Cập nhật số lượng một mặt hàng trong giỏ
    CartResponse updateCartItemQuantity(String customerEmail, Long cartItemId, int quantity);

    // Xóa một mặt hàng khỏi giỏ
    CartResponse removeCartItem(String customerEmail, Long cartItemId);

    // Xóa sạch giỏ hàng
    void clearCart(String customerEmail);
}