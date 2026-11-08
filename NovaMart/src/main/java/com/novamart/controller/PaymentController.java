package com.novamart.controller;

import com.novamart.dto.response.ApiResponse;
import com.novamart.dto.response.PaymentResponse;
import com.novamart.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Demo/Mock API: Xác nhận thanh toán trực tuyến mô phỏng (MOCK_ONLINE - kiểm tra quyền sở hữu đơn)
    @PostMapping("/mock/confirm/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmMockPayment(Authentication authentication,
                                                                           @PathVariable Long orderId) {
        PaymentResponse payment = paymentService.confirmMockPayment(authentication.getName(), orderId);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận thanh toán mô phỏng thành công", payment));
    }

    // Admin / Shipper API: Xác nhận đã thu tiền đơn COD
    @PatchMapping("/admin/cod-collected/{orderId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> markCodAsCollected(@PathVariable Long orderId) {
        PaymentResponse payment = paymentService.markCodAsCollected(orderId);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận thu tiền đơn COD", payment));
    }
}
