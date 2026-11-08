package com.novamart.repository;

import com.novamart.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // Lấy danh sách tất cả địa chỉ nhận hàng của một người dùng
    List<Address> findByUserId(Long userId);

    // Tìm địa chỉ theo ID và bảo đảm thuộc về đúng người dùng (chống truy cập trái phép)
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    // Tìm địa chỉ đang được đặt làm mặc định của người dùng
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
}