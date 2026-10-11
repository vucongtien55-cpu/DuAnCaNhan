package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Lấy lịch sử mua sản phẩm, đơn gần nhất đứng trước.
    List<OrderItem> findByProduct_IdOrderByShopOrder_CreatedAtDesc(Long productId);

    // Lấy các sản phẩm thuộc một đơn hàng của cửa hàng.
    List<OrderItem> findByShopOrder_Id(Long shopOrderId);

    // Xác minh khách đã nhận sản phẩm trước khi cho phép đăng đánh giá.
    boolean existsByProduct_IdAndShopOrder_Order_Customer_IdAndShopOrder_Status(
            Long productId, Long customerId, String shopOrderStatus);
}
