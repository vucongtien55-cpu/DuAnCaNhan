package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.ShopTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopTransactionRepository extends JpaRepository<ShopTransaction, Long> {

    // Lấy các giao dịch của cửa hàng, giao dịch mới nhất đứng trước.
    List<ShopTransaction> findByShop_IdOrderByCreatedAtDesc(Long shopId);

    // Lấy các giao dịch của đơn hàng cửa hàng, giao dịch mới nhất đứng trước.
    List<ShopTransaction> findByShopOrder_IdOrderByCreatedAtDesc(Long shopOrderId);

    // Lọc giao dịch của cửa hàng theo loại, giao dịch mới nhất đứng trước.
    List<ShopTransaction> findByShop_IdAndTypeOrderByCreatedAtDesc(Long shopId, String type);

    // Tìm giao dịch theo mã tham chiếu.
    Optional<ShopTransaction> findByTransactionReference(String transactionReference);
}
