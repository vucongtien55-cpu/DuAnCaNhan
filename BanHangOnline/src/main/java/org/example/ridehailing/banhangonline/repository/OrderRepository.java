package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Lấy đơn hàng của khách hàng, đơn mới nhất đứng trước.
    List<Order> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    // Lọc đơn hàng của khách theo trạng thái, đơn mới nhất đứng trước.
    List<Order> findByCustomer_IdAndStatusOrderByCreatedAtDesc(Long customerId, String status);

    // Lọc đơn hàng theo trạng thái thanh toán, đơn mới nhất đứng trước.
    List<Order> findByPayment_StatusOrderByCreatedAtDesc(String paymentStatus);

    // Tìm đơn theo mã trong phạm vi khách hàng.
    Optional<Order> findByOrderNumberAndCustomer_Id(String orderNumber, Long customerId);

    // Tra cứu theo mã toàn hệ thống; chỉ sử dụng sau khi service xác minh ADMIN.
    Optional<Order> findByOrderNumber(String orderNumber);

    // Chỉ tải đơn hàng nếu thuộc đúng khách hàng gọi thao tác.
    Optional<Order> findByIdAndCustomer_Id(Long orderId, Long customerId);
}
