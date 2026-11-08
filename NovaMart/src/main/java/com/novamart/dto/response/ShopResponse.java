package com.novamart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
//File thông tin gian hàng
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopResponse {

    // ID Gian hàng
    private Long id;

    // ID người sở hữu (Seller)
    private Long ownerId;

    // Tên gian hàng
    private String name;

    // Slug đường dẫn định danh
    private String slug;

    // Mô tả thông tin gian hàng
    private String description;

    // URL Logo gian hàng
    private String logoUrl;

    // Số điện thoại liên hệ hotline
    private String contactPhone;

    // Trạng thái (PENDING, ACTIVE, REJECTED, SUSPENDED)
    private String status;

    // Ghi chú của Admin (nếu bị duyệt từ chối hoặc tạm khóa)
    private String adminNote;

    // Thời điểm duyệt gian hàng
    private LocalDateTime approvedAt;
}