package com.novamart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
//File tạo sản phẩm và biến thể
public class CreateProductRequest {

    // ID danh mục chứa sản phẩm
    @NotNull(message = "Vui lòng chọn danh mục sản phẩm")
    private Long categoryId;

    // Tên sản phẩm
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    // Mô tả chi tiết sản phẩm
    private String description;

    // Danh sách URL ảnh sản phẩm
    private List<String> imageUrls;

    // Danh sách biến thể của sản phẩm (màu sắc, kích thước...)
    @NotEmpty(message = "Sản phẩm phải có ít nhất 1 biến thể")
    @Valid
    private List<VariantRequest> variants;

    @Data
    public static class VariantRequest {
        // Mã SKU độc nhất của biến thể
        @NotBlank(message = "Mã SKU không được để trống")
        private String sku;

        // Tên biến thể (Ví dụ: Đen - 128GB)
        @NotBlank(message = "Tên biến thể không được để trống")
        private String variantName;

        // Giá bán thực tế (bắt buộc > 0)
        @NotNull(message = "Giá bán không được để trống")
        @Min(value = 1, message = "Giá bán phải lớn hơn 0")
        private BigDecimal price;

        // Giá niêm yết (gốc) nếu có
        private BigDecimal compareAtPrice;

        // Số lượng tồn kho khởi tạo
        @Min(value = 0, message = "Số lượng kho không được âm")
        private int initialStock = 0;
    }
}