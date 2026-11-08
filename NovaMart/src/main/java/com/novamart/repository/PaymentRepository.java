package com.novamart.repository;

import com.novamart.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Tìm thông tin giao dịch thanh toán theo ID của đơn hàng tổng
    Optional<Payment> findByOrderId(Long orderId);
}