package com.novamart.repository;

import com.novamart.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm kiếm người dùng theo email (dùng cho đăng nhập và xác thực JWT)
    Optional<User> findByEmail(String email);

    // Kiểm tra email đã tồn tại trong hệ thống chưa (dùng khi đăng ký tài khoản)
    Boolean existsByEmail(String email);
}