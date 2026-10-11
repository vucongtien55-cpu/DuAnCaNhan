package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.ShopStaff;

import java.util.List;

public interface ShopStaffService {

    // Lấy nhân viên đang hoạt động của cửa hàng.
    List<ShopStaff> getActiveStaff(Long actorUserId, Long shopId);

    // Chủ shop có thể xem cả nhân viên đã ngừng hoạt động.
    List<ShopStaff> getStaff(Long actorUserId, Long shopId, Boolean active);

    // Thêm nhân viên vào cửa hàng; chỉ chủ cửa hàng được mời nhân viên.
    ShopStaff addStaff(Long actorUserId, Long shopId, Long userId);

    // Ngừng tư cách nhân viên; chỉ chủ cửa hàng được thực hiện.
    void deactivateStaff(Long actorUserId, Long shopId, Long staffId);
}
