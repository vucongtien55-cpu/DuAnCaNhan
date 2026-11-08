package com.novamart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
//File địa chỉ nhận hàng
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    // ID bản ghi địa chỉ
    private Long id;

    // Tên người nhận hàng
    private String recipientName;

    // Số điện thoại người nhận
    private String recipientPhone;

    // Tỉnh / Thành phố
    private String province;

    // Quận / Huyện
    private String district;

    // Phường / Xã
    private String ward;

    // Địa chỉ chi tiết (số nhà, đường)
    private String detail;

    // Cờ đánh dấu đây có phải địa chỉ mặc định không
    private boolean isDefault;
}