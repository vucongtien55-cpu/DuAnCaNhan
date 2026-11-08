package com.novamart.service;

import com.novamart.dto.response.PaymentResponse;
import com.novamart.entity.Payment;

import java.math.BigDecimal;

public interface PaymentService {

    // Tạo bản ghi thanh toán khởi tạo cho đơn hàng tổng
    Payment createPayment(Long orderId, String method, BigDecimal amount);

    // Xác nhận thanh toán trực tuyến mô phỏng thành công (MOCK_ONLINE - kiểm tra quyền sở hữu đơn)
    PaymentResponse confirmMockPayment(String userEmail, Long orderId);

    // Admin / Shipper xác nhận đã thu tiền đơn hàng COD
    PaymentResponse markCodAsCollected(Long orderId);
}
