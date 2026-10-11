package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.Shop;

import java.util.List;

public interface ShopService {

    // Tra cứu cửa hàng theo mã.
    Shop getShopById(Long shopId);

    // Tra cứu cửa hàng công khai, chỉ trả cửa hàng đang hoạt động.
    Shop getActiveShopById(Long shopId);

    // Lấy danh sách cửa hàng của chủ sở hữu.
    List<Shop> getShopsByOwner(Long ownerId);

    // Lấy các cửa hàng được gán cho nhân viên đang hoạt động.
    List<Shop> getShopsForEmployee(Long employeeId);

    // Lọc shop theo trạng thái cho ADMIN.
    List<Shop> getShopsByStatus(Long actorUserId, String status);

    // Tìm kiếm công khai chỉ trong cửa hàng đang hoạt động.
    List<Shop> searchActiveShops(String keyword);

    // Tạo cửa hàng cho một người dùng đang hoạt động.
    Shop createShop(Long ownerId, Shop shop);

    // Cập nhật thông tin cửa hàng; chỉ chủ sở hữu được sửa.
    Shop updateShop(Long actorUserId, Long shopId, Shop updates);

    // Ngừng hoạt động cửa hàng; chỉ chủ sở hữu được thực hiện.
    void deactivateShop(Long actorUserId, Long shopId);
}
