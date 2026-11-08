package com.novamart.service;

import com.novamart.dto.response.InventoryResponse;
import com.novamart.dto.response.InventoryTransactionResponse;
import com.novamart.entity.Inventory;

import java.util.List;

public interface InventoryService {

    // Lấy entity tồn kho (nội bộ trong Service)
    Inventory getInventoryByVariantId(Long variantId);

    // Lấy thông tin tồn kho DTO của một biến thể sản phẩm
    InventoryResponse getInventoryResponseByVariantId(Long variantId);

    // Seller thực hiện nhập thêm hàng hoặc điều chỉnh kho hàng (có kiểm tra quyền sở hữu)
    InventoryResponse adjustStock(String sellerEmail, Long variantId, int changeQuantity, String note);

    // Giữ chỗ tồn kho khi khách hàng tiến hành checkout đơn hàng (kèm Pessimistic Lock)
    void reserveStock(Long variantId, int quantity, Long orderId);

    // Giải phóng tồn kho bị giữ chỗ khi đơn hàng bị hủy
    void releaseStock(Long variantId, int quantity, Long orderId);

    // Xem lịch sử biến động kho hàng của biến thể sản phẩm (có kiểm tra quyền sở hữu)
    List<InventoryTransactionResponse> getStockHistory(String sellerEmail, Long variantId);
}
