package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.entity.Category;
import org.example.ridehailing.banhangonline.entity.Product;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.CategoryService;
import org.example.ridehailing.banhangonline.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@Transactional
public class CatalogController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CurrentUserService currentUserService;

    public CatalogController(
            ProductService productService,
            CategoryService categoryService,
            CurrentUserService currentUserService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.currentUserService = currentUserService;
    }

    // Đọc thông tin một sản phẩm đang được bày bán.
    @GetMapping("/products/{productId}")
    public ApiResponseDtos.ProductResponse getProduct(@PathVariable Long productId) {
        return ApiDtoMapper.toResponse(productService.getPublicProductById(productId));
    }

    // Liệt kê sản phẩm đang bán theo shop.
    @GetMapping("/shops/{shopId}/products")
    public List<ApiResponseDtos.ProductResponse> getShopProducts(@PathVariable Long shopId) {
        return ApiDtoMapper.map(productService.getActiveProductsByShop(shopId), ApiDtoMapper::toResponse);
    }

    // Shop member có thể xem cả sản phẩm đã ngừng bán để quản trị tồn tại.
    @GetMapping("/shops/{shopId}/manage/products")
    public List<ApiResponseDtos.ProductResponse> getManagedShopProducts(@PathVariable Long shopId) {
        return ApiDtoMapper.map(productService.getManagedProductsByShop(
                currentUserService.requireCurrentUserId(), shopId), ApiDtoMapper::toResponse);
    }

    // Tìm sản phẩm đang bán theo tên.
    @GetMapping("/products")
    public List<ApiResponseDtos.ProductResponse> searchProducts(@RequestParam String name) {
        return ApiDtoMapper.map(productService.searchActiveProductsByName(name), ApiDtoMapper::toResponse);
    }

    // Lọc sản phẩm đang bán theo khoảng giá.
    @GetMapping("/products/by-price")
    public List<ApiResponseDtos.ProductResponse> productsByPrice(
            @RequestParam BigDecimal minimum,
            @RequestParam BigDecimal maximum) {
        return ApiDtoMapper.map(
                productService.getActiveProductsByPriceRange(minimum, maximum), ApiDtoMapper::toResponse);
    }

    @GetMapping("/products/search")
    public ApiResponseDtos.PageResponse<ApiResponseDtos.ProductResponse> searchCatalog(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minimum,
            @RequestParam(required = false) BigDecimal maximum,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("Số trang phải từ 0 trở lên và kích thước trang từ 1 đến 50.");
        }
        Sort productSort = switch (sort.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "price-asc" -> Sort.by(Sort.Direction.ASC, "price").and(Sort.by("id"));
            case "price-desc" -> Sort.by(Sort.Direction.DESC, "price").and(Sort.by("id"));
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));
            default -> throw new IllegalArgumentException("Kiểu sắp xếp sản phẩm không được hỗ trợ.");
        };
        Page<Product> result = productService.searchPublicProducts(
                name, categoryId, minimum, maximum, PageRequest.of(page, size, productSort));
        return new ApiResponseDtos.PageResponse<>(
                ApiDtoMapper.map(result.getContent(), ApiDtoMapper::toResponse),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast());
    }

    // Tìm các mặt hàng sắp hết tồn kho.
    @GetMapping("/products/low-stock")
    public List<ApiResponseDtos.ProductResponse> lowStockProducts(@RequestParam Integer maximumStock) {
        return ApiDtoMapper.map(
                productService.getActiveProductsAtOrBelowStock(maximumStock), ApiDtoMapper::toResponse);
    }

    // Lọc sản phẩm đang bán theo danh mục.
    @GetMapping("/categories/{categoryId}/products")
    public List<ApiResponseDtos.ProductResponse> getCategoryProducts(@PathVariable Long categoryId) {
        return ApiDtoMapper.map(
                productService.getActiveProductsByCategory(categoryId), ApiDtoMapper::toResponse);
    }

    // Tạo sản phẩm thuộc shop; danh tính người thao tác lấy từ SecurityContext.
    @PostMapping("/shops/{shopId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.ProductResponse createProduct(
            @PathVariable Long shopId, @Valid @RequestBody ApiRequestDtos.ProductRequest request) {
        return ApiDtoMapper.toResponse(productService.createProduct(
                currentUserService.requireCurrentUserId(), shopId, toProduct(request)));
    }

    // Cập nhật các trường được cho phép của sản phẩm.
    @PutMapping("/products/{productId}")
    public ApiResponseDtos.ProductResponse updateProduct(
            @PathVariable Long productId, @Valid @RequestBody ApiRequestDtos.ProductRequest request) {
        return ApiDtoMapper.toResponse(productService.updateProduct(
                currentUserService.requireCurrentUserId(), productId, toProduct(request)));
    }

    // Ngừng bán sản phẩm mà không xóa lịch sử tham chiếu.
    @DeleteMapping("/products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateProduct(@PathVariable Long productId) {
        productService.deactivateProduct(currentUserService.requireCurrentUserId(), productId);
    }

    // Liệt kê danh mục gốc đang hoạt động.
    @GetMapping("/categories")
    public List<ApiResponseDtos.CategoryResponse> getRootCategories() {
        return ApiDtoMapper.map(categoryService.getActiveRootCategories(), ApiDtoMapper::toResponse);
    }

    // Tìm danh mục đang hoạt động theo slug.
    @GetMapping("/categories/slug/{slug}")
    public ApiResponseDtos.CategoryResponse getCategoryBySlug(@PathVariable String slug) {
        return ApiDtoMapper.toResponse(categoryService.getActiveCategoryBySlug(slug));
    }

    // Lấy các danh mục con đang hoạt động.
    @GetMapping("/categories/{categoryId}/children")
    public List<ApiResponseDtos.CategoryResponse> getCategoryChildren(@PathVariable Long categoryId) {
        return ApiDtoMapper.map(categoryService.getActiveChildren(categoryId), ApiDtoMapper::toResponse);
    }

    // Tìm kiếm danh mục đang hoạt động theo tên.
    @GetMapping("/categories/search")
    public List<ApiResponseDtos.CategoryResponse> searchCategories(@RequestParam String name) {
        return ApiDtoMapper.map(categoryService.searchActiveCategoriesByName(name), ApiDtoMapper::toResponse);
    }

    // Tạo danh mục, quyền ADMIN được kiểm tra trong service.
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.CategoryResponse createCategory(
            @Valid @RequestBody ApiRequestDtos.CategoryRequest request) {
        return ApiDtoMapper.toResponse(categoryService.createCategory(
                currentUserService.requireCurrentUserId(), toCategory(request), request.parentCategoryId()));
    }

    // Cập nhật danh mục, quyền ADMIN được kiểm tra trong service.
    @PutMapping("/categories/{categoryId}")
    public ApiResponseDtos.CategoryResponse updateCategory(
            @PathVariable Long categoryId, @Valid @RequestBody ApiRequestDtos.CategoryRequest request) {
        return ApiDtoMapper.toResponse(categoryService.updateCategory(
                currentUserService.requireCurrentUserId(), categoryId,
                toCategory(request), request.parentCategoryId()));
    }

    // Ngừng hoạt động danh mục, quyền ADMIN được kiểm tra trong service.
    @DeleteMapping("/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateCategory(@PathVariable Long categoryId) {
        categoryService.deactivateCategory(currentUserService.requireCurrentUserId(), categoryId);
    }

    private Product toProduct(ApiRequestDtos.ProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
        if (request.categoryId() != null) {
            Category category = new Category();
            category.setId(request.categoryId());
            product.setCategory(category);
        }
        return product;
    }

    private Category toCategory(ApiRequestDtos.CategoryRequest request) {
        Category category = new Category();
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        return category;
    }
}
