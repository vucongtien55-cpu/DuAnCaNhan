package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.ShopOrder;

import java.util.List;

public interface ShopOrderService {

    // Lấy đơn thuộc cửa hàng mà người thực hiện được phép quản lý.
    List<ShopOrder> getShopOrders(Long actorUserId, Long shopId);

    // Lọc đơn của cửa hàng theo trạng thái.
    List<ShopOrder> getShopOrdersByStatus(Long actorUserId, Long shopId, String status);

    // Lấy đơn được giao cho nhân viên hiện tại trong shop.
    List<ShopOrder> getMyAssignedOrders(Long actorUserId, Long shopId, String status);

    // Chuyển đơn sang trạng thái hợp lệ tiếp theo.
    ShopOrder updateOrderStatus(Long actorUserId, Long shopId, Long shopOrderId, String status);

    // Giao đơn cho nhân viên còn hoạt động thuộc chính cửa hàng.
    ShopOrder assignOrder(Long actorUserId, Long shopId, Long shopOrderId, Long staffId);
}
