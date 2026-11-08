package com.novamart.controller;

import com.novamart.dto.request.AddToCartRequest;
import com.novamart.dto.response.ApiResponse;
import com.novamart.dto.response.CartResponse;
import com.novamart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    // Xem chi tiết giỏ hàng cá nhân (đã gom nhóm sản phẩm theo từng Shop)
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart(Authentication authentication) {
        CartResponse cart = cartService.getMyCart(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin giỏ hàng thành công", cart));
    }

    // Thêm biến thể sản phẩm vào giỏ hàng
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(Authentication authentication,
                                                               @Valid @RequestBody AddToCartRequest request) {
        CartResponse cart = cartService.addToCart(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm sản phẩm vào giỏ hàng", cart));
    }

    // Cập nhật số lượng một mặt hàng trong giỏ
    @PatchMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItemQuantity(Authentication authentication,
                                                                            @PathVariable Long id,
                                                                            @RequestParam int quantity) {
        CartResponse cart = cartService.updateCartItemQuantity(authentication.getName(), id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng thành công", cart));
    }

    // Xóa một mặt hàng khỏi giỏ hàng
    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(Authentication authentication,
                                                                    @PathVariable Long id) {
        CartResponse cart = cartService.removeCartItem(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm khỏi giỏ hàng", cart));
    }

    // Xóa sạch giỏ hàng
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication authentication) {
        cartService.clearCart(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sạch giỏ hàng", null));
    }
}