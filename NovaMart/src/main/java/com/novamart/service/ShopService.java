package com.novamart.service;

import com.novamart.dto.request.ApplyShopRequest;
import com.novamart.dto.response.ShopResponse;

import java.util.List;

public interface ShopService {
    // Seller gửi yêu cầu mở gian hàng mới
    ShopResponse applyShop(String userEmail, ApplyShopRequest request);

    // Seller lấy thông tin gian hàng của mình
    ShopResponse getMyShop(String userEmail);

    // Public: Xem thông tin gian hàng theo slug công khai
    ShopResponse getShopBySlug(String slug);

    // Admin lấy danh sách gian hàng theo trạng thái (ví dụ: PENDING)
    List<ShopResponse> getShopsByStatus(String status);

    // Admin phê duyệt gian hàng và cấp quyền ROLE_SELLER cho người dùng
    ShopResponse approveShop(Long shopId);

    // Admin từ chối phê duyệt gian hàng kèm lý do
    ShopResponse rejectShop(Long shopId, String reason);

    // Admin tạm khóa gian hàng vi phạm
    ShopResponse suspendShop(Long shopId, String reason);
}