package com.novamart.repository;

import com.novamart.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // Tìm biến thể sản phẩm theo mã SKU độc nhất
    Optional<ProductVariant> findBySku(String sku);

    // Kiểm tra mã SKU đã tồn tại trong hệ thống chưa
    Boolean existsBySku(String sku);

    // Lấy tất cả các biến thể thuộc về một sản phẩm gốc
    List<ProductVariant> findByProductId(Long productId);
}