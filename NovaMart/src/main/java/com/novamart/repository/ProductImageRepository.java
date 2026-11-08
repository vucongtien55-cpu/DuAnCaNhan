package com.novamart.repository;

import com.novamart.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // Lấy danh sách hình ảnh của sản phẩm được sắp xếp theo thứ tự hiển thị
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    // Xóa toàn bộ ảnh của một sản phẩm khi cập nhật lại ảnh
    void deleteByProductId(Long productId);
}