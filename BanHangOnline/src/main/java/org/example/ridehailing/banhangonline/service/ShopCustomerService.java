package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.ShopCustomer;

import java.util.List;

public interface ShopCustomerService {

    // Lấy khách hàng thuộc shop; người gọi phải là chủ shop hoặc nhân viên của shop.
    List<ShopCustomer> getCustomers(Long actorUserId, Long shopId);

    // Lọc danh sách khách của shop theo trạng thái tùy chọn.
    List<ShopCustomer> getCustomers(Long actorUserId, Long shopId, String status);

    // Thêm khách hàng vào danh sách chăm sóc của shop.
    ShopCustomer addCustomer(Long actorUserId, Long shopId, Long customerId);

    // Đổi trạng thái khách hàng trong danh sách của shop.
    ShopCustomer updateCustomerStatus(Long actorUserId, Long shopId, Long customerId, String status);
}
