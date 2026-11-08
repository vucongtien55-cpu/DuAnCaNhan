package com.novamart.repository;

import com.novamart.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Tìm thông tin tồn kho theo ID của biến thể sản phẩm
    Optional<Inventory> findByVariantId(Long variantId);

    // Khóa bi quan (Pessimistic Write Lock) ngăn chặn race condition khi đặt hàng / trừ kho
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.variant.id = :variantId")
    Optional<Inventory> findByVariantIdWithLock(@Param("variantId") Long variantId);
}