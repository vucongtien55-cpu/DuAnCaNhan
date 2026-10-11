package org.example.ridehailing.banhangonline.service;

import org.example.ridehailing.banhangonline.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    // Tìm sản phẩm đang được bày bán theo mã sản phẩm.
    Product getActiveProductById(Long productId);

    // Tìm sản phẩm công khai thuộc cửa hàng còn hoạt động.
    Product getPublicProductById(Long productId);

    // Lấy các sản phẩm đang hoạt động của một cửa hàng.
    List<Product> getActiveProductsByShop(Long shopId);

    // Lấy cả sản phẩm đã ngừng bán; chỉ thành viên đúng shop được xem.
    List<Product> getManagedProductsByShop(Long actorUserId, Long shopId);

    // Lấy các sản phẩm đang hoạt động thuộc danh mục.
    List<Product> getActiveProductsByCategory(Long categoryId);

    // Tìm các sản phẩm đang hoạt động theo tên.
    List<Product> searchActiveProductsByName(String keyword);

    // Lọc sản phẩm đang hoạt động theo khoảng giá.
    List<Product> getActiveProductsByPriceRange(BigDecimal minimumPrice, BigDecimal maximumPrice);

    Page<Product> searchPublicProducts(
            String keyword, Long categoryId, BigDecimal minimumPrice, BigDecimal maximumPrice, Pageable pageable);

    // Lấy sản phẩm đang hoạt động có tồn kho không vượt quá giới hạn.
    List<Product> getActiveProductsAtOrBelowStock(Integer maximumStock);

    // Tạo sản phẩm cho cửa hàng; actorUserId phải lấy từ danh tính đã xác thực ở tầng gọi.
    Product createProduct(Long actorUserId, Long shopId, Product product);

    // Cập nhật thông tin sản phẩm; không cho phép chuyển sản phẩm sang cửa hàng khác.
    Product updateProduct(Long actorUserId, Long productId, Product product);

    // Ngừng bán mềm để không làm mất lịch sử tham chiếu sản phẩm.
    void deactivateProduct(Long actorUserId, Long productId);
}
