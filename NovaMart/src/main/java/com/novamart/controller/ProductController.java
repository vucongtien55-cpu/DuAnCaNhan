package com.novamart.controller;

import com.novamart.dto.request.CreateProductRequest;
import com.novamart.dto.response.ApiResponse;
import com.novamart.dto.response.ProductResponse;
import com.novamart.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ProductController {

    @Autowired
    private ProductService productService;

    // Public API: Lấy danh sách sản phẩm đang bán (có phân trang)
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getActiveProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success(productService.getActiveProducts(pageable)));
    }

    // Public API: Xem chi tiết sản phẩm theo ID
    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    // Seller API: Đăng sản phẩm mới kèm các biến thể và tồn kho ban đầu
    @PostMapping("/seller/products")
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(Authentication authentication,
                                                                      @Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = productService.createProduct(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Đăng sản phẩm thành công", product));
    }

    // Seller API: Lấy danh sách sản phẩm của gian hàng mình
    @GetMapping("/seller/products")
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getMyShopProducts(Authentication authentication,
                                                                                @RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.success(productService.getMyShopProducts(authentication.getName(), pageable)));
    }

    // Seller / Admin API: Cập nhật trạng thái sản phẩm (DRAFT, ACTIVE, INACTIVE)
    @PatchMapping("/seller/products/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_SELLER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(Authentication authentication,
                                                                            @PathVariable Long id,
                                                                            @RequestParam String status) {
        ProductResponse product = productService.updateProductStatus(authentication.getName(), id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái sản phẩm thành công", product));
    }
}