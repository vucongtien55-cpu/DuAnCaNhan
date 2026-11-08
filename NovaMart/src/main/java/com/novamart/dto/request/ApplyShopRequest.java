package com.novamart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
//File đăng ký mở gian hàng
public class ApplyShopRequest {

    // Tên gian hàng muốn mở
    @NotBlank(message = "Tên gian hàng không được để trống")
    private String name;

    // Mô tả thông tin gian hàng
    private String description;

    // URL Logo gian hàng
    private String logoUrl;

    // Số điện thoại hotline gian hàng
    @NotBlank(message = "Số điện thoại liên hệ không được để trống")
    private String contactPhone;
}