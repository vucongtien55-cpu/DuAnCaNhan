package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Tìm giao dịch thanh toán theo mã đơn hàng.
    Optional<Payment> findByOrder_Id(Long orderId);

    // Lọc giao dịch thanh toán theo trạng thái.
    List<Payment> findByStatus(String status);

    // Tìm giao dịch theo mã tham chiếu từ cổng thanh toán.
    Optional<Payment> findByTransactionReference(String transactionReference);

    // Tìm thanh toán của đơn nếu đơn đó thuộc đúng khách hàng.
    Optional<Payment> findByOrder_IdAndOrder_Customer_Id(Long orderId, Long customerId);
}
