package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.ShopTransaction;

import java.util.List;

public interface ShopTransactionService {

    // Lấy giao dịch shop; người gọi phải là chủ shop hoặc nhân viên được cấp quyền.
    List<ShopTransaction> getShopTransactions(Long actorUserId, Long shopId);

    // Lọc giao dịch theo loại trong shop đã được phân quyền.
    List<ShopTransaction> getShopTransactions(Long actorUserId, Long shopId, String type);

    // Lấy giao dịch liên quan đến một đơn của shop.
    List<ShopTransaction> getShopOrderTransactions(Long actorUserId, Long shopId, Long shopOrderId);

    // Tạo giao dịch đang chờ cho một shop, không nhận trạng thái từ client.
    ShopTransaction createPendingTransaction(
            Long actorUserId, Long shopId, Long shopOrderId, String type, java.math.BigDecimal amount,
            String transactionReference);
}
