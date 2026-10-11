package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.OrderItem;

import java.util.List;

public interface OrderItemService {

    // Lấy chi tiết đơn của shop; người gọi phải là chủ shop hoặc nhân viên.
    List<OrderItem> getShopOrderItems(Long actorUserId, Long shopId, Long shopOrderId);

    // Lấy lịch sử các đơn của một sản phẩm trong shop được phép quản lý.
    List<OrderItem> getProductSalesHistory(Long actorUserId, Long shopId, Long productId);
}
