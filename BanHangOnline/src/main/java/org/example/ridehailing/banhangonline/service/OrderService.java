package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.Order;

import java.util.List;

public interface OrderService {

    // Checkout toàn bộ giỏ hàng thành đơn đa shop; customerId phải lấy từ SecurityContext.
    Order checkout(Long customerId, String shippingAddress, String recipientPhone, String customerNote);

    // Lấy đơn hàng của chính khách hàng.
    Order getCustomerOrder(Long customerId, Long orderId);

    // Lấy lịch sử đặt hàng của chính khách hàng.
    List<Order> getCustomerOrders(Long customerId);

    // Lọc đơn của chính khách hàng theo trạng thái fulfillment.
    List<Order> getCustomerOrders(Long customerId, String status);

    // Tra cứu đơn theo số đơn trong phạm vi chính khách hàng.
    Order getCustomerOrderByNumber(Long customerId, String orderNumber);

    // Lọc các đơn theo trạng thái thanh toán, chỉ ADMIN được truy cập.
    List<Order> getOrdersByPaymentStatus(Long actorUserId, String paymentStatus);

    // Tra cứu mã đơn toàn hệ thống, chỉ ADMIN được truy cập.
    Order getOrderByNumber(Long actorUserId, String orderNumber);

    // Hủy đơn khi đơn còn chờ xử lý và hoàn lại tồn kho.
    Order cancelPendingOrder(Long customerId, Long orderId);
}
