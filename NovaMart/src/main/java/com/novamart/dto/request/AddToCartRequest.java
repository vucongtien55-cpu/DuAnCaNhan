package com.novamart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
//File thêm sản phẩm vào giỏ hàng
public class AddToCartRequest {

    // ID biến thể sản phẩm chọn mua
    @NotNull(message = "Vui lòng chọn biến thể sản phẩm")
    private Long variantId;

    // Số lượng chọn mua (tối thiểu 1)
    @Min(value = 1, message = "Số lượng mua tối thiểu là 1")
    private int quantity;
}