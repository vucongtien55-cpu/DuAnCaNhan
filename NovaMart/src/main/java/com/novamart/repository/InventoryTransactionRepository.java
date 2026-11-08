package com.novamart.repository;

import com.novamart.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    // Lấy lịch sử biến động kho của biến thể sản phẩm, sắp xếp theo thời gian mới nhất
    List<InventoryTransaction> findByVariantIdOrderByCreatedAtDesc(Long variantId);
}