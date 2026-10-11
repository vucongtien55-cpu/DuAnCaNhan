package org.example.ridehailing.banhangonline.repository;

import org.example.ridehailing.banhangonline.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // Tìm một sản phẩm đang hoạt động theo mã.
    Optional<Product> findByIdAndActiveTrue(Long productId);

    // Tìm một sản phẩm công khai chỉ khi cả sản phẩm và cửa hàng đều hoạt động.
    Optional<Product> findByIdAndActiveTrueAndShop_Status(Long productId, String shopStatus);

    // Lấy toàn bộ sản phẩm của cửa hàng.
    List<Product> findByShop_Id(Long shopId);

    // Chỉ hiển thị sản phẩm của shop đang hoạt động trong catalog công khai.
    List<Product> findByShop_IdAndActiveTrueAndShop_Status(Long shopId, String shopStatus);

    // Loại sản phẩm của shop đã ngừng hoạt động khỏi danh mục công khai.
    List<Product> findByCategory_IdAndActiveTrueAndShop_Status(Long categoryId, String shopStatus);

    // Tìm tên trong các sản phẩm đang bán của shop đang hoạt động.
    List<Product> findByNameContainingIgnoreCaseAndActiveTrueAndShop_Status(String name, String shopStatus);

    // Lọc giá trên sản phẩm bán từ shop đang hoạt động.
    List<Product> findByPriceBetweenAndActiveTrueAndShop_Status(
            BigDecimal minimumPrice, BigDecimal maximumPrice, String shopStatus);

    // Chỉ tìm hàng sắp hết từ shop đang hoạt động.
    List<Product> findByStockQuantityLessThanEqualAndActiveTrueAndShop_Status(
            Integer maximumStock, String shopStatus);
}
