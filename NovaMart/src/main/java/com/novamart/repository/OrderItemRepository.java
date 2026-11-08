package com.novamart.repository;

import com.novamart.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Lấy danh sách các sản phẩm chi tiết có trong một đơn hàng con của Shop
    List<OrderItem> findByShopOrderId(Long shopOrderId);
}