package com.novamart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
//file tạo địa chỉ mới
public class CreateAddressRequest {

    // Tên người nhận hàng
    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    // Số điện thoại người nhận
    @NotBlank(message = "Số điện thoại nhận không được để trống")
    private String recipientPhone;

    // Tỉnh / Thành phố
    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String province;

    // Quận / Huyện
    @NotBlank(message = "Quận/Huyện không được để trống")
    private String district;

    // Phường / Xã
    @NotBlank(message = "Phường/Xã không được để trống")
    private String ward;

    // Địa chỉ chi tiết (số nhà, tên đường...)
    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String detail;

    // Đặt làm địa chỉ mặc định hay không
    private boolean isDefault = false;
}