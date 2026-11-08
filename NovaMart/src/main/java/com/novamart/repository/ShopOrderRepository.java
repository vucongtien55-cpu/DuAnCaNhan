package com.novamart.repository;

import com.novamart.entity.ShopOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    // Tìm đơn hàng con của Shop theo mã shopOrderCode
    Optional<ShopOrder> findByShopOrderCode(String shopOrderCode);

    // Lấy tất cả các đơn hàng con thuộc về một đơn hàng tổng
    List<ShopOrder> findByOrderId(Long orderId);

    // Lấy danh sách đơn hàng con cần xử lý của một Shop, sắp xếp từ mới đến cũ
    Page<ShopOrder> findByShopIdOrderByCreatedAtDesc(Long shopId, Pageable pageable);
}