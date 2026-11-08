package com.novamart.repository;

import com.novamart.entity.Role;
import com.novamart.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Tìm kiếm vai trò theo tên enum (ROLE_CUSTOMER, ROLE_SELLER, ROLE_ADMIN)
    Optional<Role> findByName(RoleName name);
}