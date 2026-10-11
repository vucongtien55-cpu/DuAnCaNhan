package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    // Lấy đơn hàng của cửa hàng, đơn mới nhất đứng trước.
    List<ShopOrder> findByShop_IdOrderByCreatedAtDesc(Long shopId);

    // Lọc đơn hàng của cửa hàng theo trạng thái, đơn mới nhất đứng trước.
    List<ShopOrder> findByShop_IdAndStatusOrderByCreatedAtDesc(Long shopId, String status);

    // Lấy đơn được giao cho nhân viên hiện tại trong đúng shop.
    List<ShopOrder> findByShop_IdAndAssignedStaff_IdOrderByCreatedAtDesc(Long shopId, Long staffId);

    // Lọc đơn của nhân viên theo trạng thái trong đúng shop.
    List<ShopOrder> findByShop_IdAndAssignedStaff_IdAndStatusOrderByCreatedAtDesc(
            Long shopId, Long staffId, String status);

    // Giới hạn truy vấn theo cả mã đơn shop và shop để tránh truy cập chéo shop.
    java.util.Optional<ShopOrder> findByIdAndShop_Id(Long shopOrderId, Long shopId);
}
