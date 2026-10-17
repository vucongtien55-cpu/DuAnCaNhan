package org.example.ridehailing.ride_hailing.repository;

import org.example.ridehailing.ride_hailing.entity.DriverStatus;
import org.example.ridehailing.ride_hailing.entity.User;
import org.example.ridehailing.ride_hailing.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm người dùng theo số điện thoại (dùng cho Đăng nhập / Kiểm tra trùng lặp)
    Optional<User> findByPhoneNumber(String phoneNumber);

    // Kiểm tra số điện thoại đã tồn tại trong hệ thống hay chưa
    boolean existsByPhoneNumber(String phoneNumber);

    // Lấy danh sách tài xế theo trạng thái hoạt động (ONLINE, OFFLINE, BUSY)
    List<User> findByRoleAndDriverStatus(UserRole role, DriverStatus driverStatus);
}