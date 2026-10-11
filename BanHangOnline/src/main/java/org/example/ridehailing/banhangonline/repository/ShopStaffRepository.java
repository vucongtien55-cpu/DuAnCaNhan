package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.ShopStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopStaffRepository extends JpaRepository<ShopStaff, Long> {

    // Lấy danh sách nhân viên của cửa hàng.
    List<ShopStaff> findByShop_Id(Long shopId);

    // Lấy danh sách nhân viên đang hoạt động của cửa hàng.
    List<ShopStaff> findByShop_IdAndActiveTrue(Long shopId);

    // Lấy danh sách nhân viên đã ngừng hoạt động của cửa hàng.
    List<ShopStaff> findByShop_IdAndActiveFalse(Long shopId);

    List<ShopStaff> findByUser_IdAndActiveTrue(Long userId);

    // Tìm tư cách thành viên của một người dùng tại cửa hàng.
    Optional<ShopStaff> findByShop_IdAndUser_Id(Long shopId, Long userId);

    // Kiểm tra người dùng còn là nhân viên hoạt động ở bất kỳ cửa hàng nào hay không.
    boolean existsByUser_IdAndActiveTrue(Long userId);
}
