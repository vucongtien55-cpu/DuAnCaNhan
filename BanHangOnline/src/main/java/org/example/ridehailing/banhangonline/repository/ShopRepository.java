package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    // Tìm tất cả cửa hàng thuộc về một chủ sở hữu.
    List<Shop> findByOwner_Id(Long ownerId);

    // Kiểm tra người dùng còn sở hữu cửa hàng ở trạng thái chỉ định hay không.
    boolean existsByOwner_IdAndStatus(Long ownerId, String status);

    // Tra cứu cửa hàng theo mã và trạng thái để API công khai không lộ cửa hàng đã ngừng hoạt động.
    Optional<Shop> findByIdAndStatus(Long shopId, String status);

    // Tìm cửa hàng theo trạng thái.
    List<Shop> findByStatus(String status);

    // Chỉ tìm kiếm cửa hàng đang hoạt động cho trang catalog công khai.
    List<Shop> findByNameContainingIgnoreCaseAndStatus(String name, String status);
}
