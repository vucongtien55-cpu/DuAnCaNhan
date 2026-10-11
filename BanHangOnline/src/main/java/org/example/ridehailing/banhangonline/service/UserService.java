package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.User;

import java.util.List;

public interface UserService {

    // Đăng ký tài khoản khách hàng mới; không nhận role từ dữ liệu đăng ký.
    User registerCustomer(String fullName, String email, String rawPassword, String phone);

    // Lấy thông tin tài khoản, chỉ cho phép chính người dùng hoặc ADMIN.
    User getUser(Long actorUserId, Long userId);

    // Cập nhật thông tin hồ sơ của chính người dùng.
    User updateProfile(Long actorUserId, String fullName, String phone, String avatarUrl);

    // Lọc tài khoản cho ADMIN theo vai trò, trạng thái hoặc kết hợp cả hai.
    List<User> searchUsers(Long actorUserId, String role, Boolean enabled);

    // Liệt kê tài khoản đang bị khóa cho ADMIN.
    List<User> getLockedUsers(Long actorUserId);

    // Mở khóa tài khoản và ghi lại tài khoản ADMIN thực hiện thao tác.
    User unlockAccount(Long actorUserId, Long accountId);
}
