package com.novamart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
//File chi tiết sản phẩm
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    // ID sản phẩm
    private Long id;

    // Thông tin Shop sở hữu
    private Long shopId;
    private String shopName;

    // Thông tin danh mục
    private Long categoryId;
    private String categoryName;

    // Tên và slug sản phẩm
    private String name;
    private String slug;

    // Mô tả sản phẩm
    private String description;

    // Trạng thái (DRAFT, ACTIVE, INACTIVE...)
    private String status;

    // Khoảng giá hiển thị (giá nhỏ nhất và lớn nhất trong các biến thể)
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Danh sách URL ảnh sản phẩm
    private List<String> imageUrls;

    // Danh sách biến thể của sản phẩm
    private List<VariantResponse> variants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantResponse {
        private Long id;
        private String sku;
        private String variantName;
        private BigDecimal price;
        private BigDecimal compareAtPrice;
        private boolean active;
        private int stockAvailable; // Số lượng tồn kho sẵn sàng bán
    }
}