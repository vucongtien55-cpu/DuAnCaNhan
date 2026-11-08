package com.novamart.controller;

import com.novamart.dto.request.CheckoutRequest;
import com.novamart.dto.response.ApiResponse;
import com.novamart.dto.response.OrderResponse;
import com.novamart.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Customer: Tiến hành Checkout tạo Đơn tổng và tự động tách các Đơn con theo Shop
    @PostMapping("/orders/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(Authentication authentication,
                                                               @Valid @RequestBody CheckoutRequest request) {
        OrderResponse order = orderService.checkout(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Đặt hàng thành công!", order));
    }

    // Customer: Xem lịch sử đơn hàng cá nhân
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(Authentication authentication,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrders(authentication.getName(), pageable)));
    }

    // Customer: Xem chi tiết đơn hàng theo mã đơn tổng
    @GetMapping("/orders/{orderCode}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByCode(Authentication authentication,
                                                                     @PathVariable String orderCode) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderByCode(authentication.getName(), orderCode)));
    }

    // Seller: Danh sách đơn hàng con cần xử lý của Shop mình
    @GetMapping("/seller/shop-orders")
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    public ResponseEntity<ApiResponse<Page<OrderResponse.ShopOrderResponse>>> getMyShopOrders(Authentication authentication,
                                                                                              @RequestParam(defaultValue = "0") int page,
                                                                                              @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyShopOrders(authentication.getName(), pageable)));
    }

    // Seller: Cập nhật trạng thái xử lý đơn hàng con (CONFIRMED, PROCESSING, SHIPPING, DELIVERED)
    @PatchMapping("/seller/shop-orders/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    public ResponseEntity<ApiResponse<OrderResponse.ShopOrderResponse>> updateShopOrderStatus(Authentication authentication,
                                                                                              @PathVariable Long id,
                                                                                              @RequestParam String status) {
        OrderResponse.ShopOrderResponse shopOrder = orderService.updateShopOrderStatus(authentication.getName(), id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái đơn hàng thành công", shopOrder));
    }

    // Customer / Admin: Hủy đơn hàng tổng (ORD-xxx)
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(Authentication authentication,
                                                         @PathVariable Long id,
                                                         @RequestParam(defaultValue = "Khách hàng yêu cầu hủy đơn") String reason) {
        orderService.cancelOrder(authentication.getName(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", null));
    }

    // Customer / Seller / Admin: Hủy đơn hàng con cụ thể của một Shop
    @PostMapping("/orders/shop-orders/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelShopOrder(Authentication authentication,
                                                             @PathVariable Long id,
                                                             @RequestParam(defaultValue = "Yêu cầu hủy đơn con") String reason) {
        orderService.cancelShopOrder(authentication.getName(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng con của Shop thành công", null));
    }
}