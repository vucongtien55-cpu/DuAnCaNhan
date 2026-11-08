package com.novamart.service;

import com.novamart.dto.request.CheckoutRequest;
import com.novamart.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    // Customer: Tiến hành Checkout tạo Đơn hàng tổng & tách các Đơn hàng con theo từng Shop
    OrderResponse checkout(String customerEmail, CheckoutRequest request);

    // Customer: Lịch sử đơn hàng cá nhân
    Page<OrderResponse> getMyOrders(String customerEmail, Pageable pageable);

    // Customer: Xem chi tiết đơn hàng theo mã đơn tổng
    OrderResponse getOrderByCode(String customerEmail, String orderCode);

    // Seller: Lấy danh sách các đơn hàng con (ShopOrder) thuộc Shop của mình
    Page<OrderResponse.ShopOrderResponse> getMyShopOrders(String sellerEmail, Pageable pageable);

    // Seller: Cập nhật trạng thái xử lý đơn hàng con (CONFIRMED -> PROCESSING -> SHIPPING -> DELIVERED)
    OrderResponse.ShopOrderResponse updateShopOrderStatus(String sellerEmail, Long shopOrderId, String newStatus);

    // Customer / Admin: Hủy toàn bộ đơn hàng tổng (hủy tất cả đơn con và hoàn kho)
    void cancelOrder(String userEmail, Long orderId, String reason);

    // Customer / Seller / Admin: Hủy một đơn hàng con cụ thể của Shop
    void cancelShopOrder(String userEmail, Long shopOrderId, String reason);
}