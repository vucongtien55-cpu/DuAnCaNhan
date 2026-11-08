package com.novamart.repository;

import com.novamart.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Tìm đơn hàng tổng theo mã đơn hiển thị (orderCode)
    Optional<Order> findByOrderCode(String orderCode);

    // Lấy lịch sử đơn hàng của khách hàng, sắp xếp mới nhất xếp trước có phân trang
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
}