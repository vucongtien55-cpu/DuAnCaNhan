package com.novamart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
//File chi tiết giỏ hàng
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    // ID giỏ hàng
    private Long cartId;

    // Tổng tiền tạm tính toàn bộ giỏ hàng
    private BigDecimal grandTotal;

    // Danh sách sản phẩm trong giỏ được nhóm theo từng Shop
    private List<ShopCartGroup> shopGroups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopCartGroup {
        private Long shopId;
        private String shopName;
        private List<CartItemResponse> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long cartItemId;
        private Long variantId;
        private String productName;
        private String variantName;
        private String sku;
        private String imageUrl;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal lineTotal; // Thành tiền (đơn giá * số lượng)
    }
}