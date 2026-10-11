package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm người dùng theo email.
    Optional<User> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    // Kiểm tra email đã được đăng ký hay chưa.
    boolean existsByEmail(String email);

    // Lọc người dùng theo vai trò.
    List<User> findByRole(String role);

    // Lọc người dùng theo vai trò và trạng thái hoạt động.
    List<User> findByRoleAndEnabled(String role, Boolean enabled);

    // Lọc người dùng theo trạng thái hoạt động.
    List<User> findByEnabled(Boolean enabled);

    List<User> findByLockedTrue();
}
