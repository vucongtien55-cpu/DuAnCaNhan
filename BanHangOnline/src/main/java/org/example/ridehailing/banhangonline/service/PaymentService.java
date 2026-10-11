package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.Payment;

import java.util.List;

public interface PaymentService {

    // Tạo yêu cầu thanh toán đang chờ cho đơn của khách hàng.
    Payment createPendingPayment(Long customerId, Long orderId, String method);

    // Lấy thông tin thanh toán của đơn hàng thuộc khách hàng.
    Payment getCustomerPayment(Long customerId, Long orderId);

    // Liệt kê giao dịch theo trạng thái; chỉ ADMIN được truy cập toàn hệ thống.
    List<Payment> getPaymentsByStatus(Long actorUserId, String status);
}
