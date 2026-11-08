package com.novamart.service;

import com.novamart.dto.request.CreateProductRequest;
import com.novamart.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    // Public: Xem danh sách sản phẩm đang bán có phân trang
    Page<ProductResponse> getActiveProducts(Pageable pageable);

    // Public: Xem chi tiết sản phẩm theo ID
    ProductResponse getProductById(Long productId);

    // Seller: Đăng sản phẩm mới kèm các biến thể (variants) và khởi tạo tồn kho
    ProductResponse createProduct(String sellerEmail, CreateProductRequest request);

    // Seller: Lấy danh sách sản phẩm thuộc Shop của mình
    Page<ProductResponse> getMyShopProducts(String sellerEmail, Pageable pageable);

    // Seller / Admin: Thay đổi trạng thái sản phẩm (DRAFT, ACTIVE, INACTIVE)
    ProductResponse updateProductStatus(String sellerEmail, Long productId, String status);
}