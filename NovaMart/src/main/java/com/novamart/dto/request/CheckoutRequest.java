package com.novamart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
//File đặt hàng
public class CheckoutRequest {

    // ID địa chỉ nhận hàng của khách
    @NotNull(message = "Vui lòng chọn địa chỉ giao hàng")
    private Long addressId;

    // Phương thức thanh toán (COD hoặc MOCK_ONLINE)
    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;

    // Ghi chú đơn hàng cho người bán (nếu có)
    private String note;
}