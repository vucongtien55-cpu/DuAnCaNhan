package com.novamart.controller;

import com.novamart.dto.response.ApiResponse;
import com.novamart.dto.response.CategoryResponse;
import com.novamart.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * Public API: Lấy danh sách tất cả các danh mục sản phẩm đang hoạt động (active = true)
     * Dùng cho hiển thị menu, trang chủ hoặc bộ lọc sản phẩm.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllActiveCategories() {
        List<CategoryResponse> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công", categories));
    }

    /**
     * Public API: Lấy thông tin chi tiết một danh mục theo đường dẫn slug định danh
     * Example: GET /api/v1/categories/thoi-trang-nam
     */
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        CategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin danh mục thành công", category));
    }

    /**
     * Admin API: Tạo danh mục sản phẩm mới (Hỗ trợ cấu trúc danh mục Cha - Con)
     * Yêu cầu quyền ROLE_ADMIN.
     *
     * @param name Tên danh mục (bắt buộc)
     * @param description Mô tả danh mục (tùy chọn)
     * @param parentId ID của danh mục cha (tùy chọn, để null nếu là danh mục gốc)
     */
    @PostMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@RequestParam String name,
                                                                        @RequestParam(required = false) String description,
                                                                        @RequestParam(required = false) Long parentId) {
        CategoryResponse category = categoryService.createCategory(name, description, parentId);
        return ResponseEntity.ok(ApiResponse.success("Tạo danh mục sản phẩm thành công", category));
    }
}
