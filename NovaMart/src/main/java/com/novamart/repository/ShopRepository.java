package com.novamart.repository;

import com.novamart.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    // Tìm gian hàng theo ID của chủ sở hữu (Seller)
    Optional<Shop> findByOwnerId(Long ownerId);

    // Tìm gian hàng theo đường dẫn định danh slug (dùng cho xem trang Shop công khai)
    Optional<Shop> findBySlug(String slug);

    // Kiểm tra tên gian hàng đã tồn tại chưa
    Boolean existsByName(String name);

    // Kiểm tra slug gian hàng đã tồn tại chưa
    Boolean existsBySlug(String slug);

    // Kiểm tra xem người dùng đã sở hữu gian hàng nào chưa (giới hạn 1 Shop/Seller)
    Boolean existsByOwnerId(Long ownerId);

    // Lấy danh sách gian hàng theo trạng thái (PENDING, ACTIVE, REJECTED, SUSPENDED)
    List<Shop> findByStatus(String status);
}