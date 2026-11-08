package com.novamart.repository;

import com.novamart.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Tìm sản phẩm theo ID gian hàng và slug
    Optional<Product> findByShopIdAndSlug(Long shopId, String slug);

    // Lấy danh sách sản phẩm theo trạng thái có phân trang (dùng cho Admin hoặc hiển thị)
    Page<Product> findByStatus(String status, Pageable pageable);

    // Lấy danh sách sản phẩm thuộc về một Shop cụ thể
    Page<Product> findByShopId(Long shopId, Pageable pageable);

    // Lấy danh sách sản phẩm theo danh mục và trạng thái có phân trang
    Page<Product> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);
}