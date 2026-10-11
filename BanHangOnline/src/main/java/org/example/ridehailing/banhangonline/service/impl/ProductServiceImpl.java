package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.Category;
import org.example.ridehailing.banhangonline.entity.Product;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.ShopStaff;
import org.example.ridehailing.banhangonline.repository.CategoryRepository;
import org.example.ridehailing.banhangonline.repository.ProductRepository;
import org.example.ridehailing.banhangonline.repository.ShopRepository;
import org.example.ridehailing.banhangonline.repository.ShopStaffRepository;
import org.example.ridehailing.banhangonline.service.ProductService;
import org.example.ridehailing.banhangonline.service.ShopAuthorizationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final int PRODUCT_NAME_MAX_LENGTH = 180;
    private static final int PRODUCT_DESCRIPTION_MAX_LENGTH = 4000;
    private static final int IMAGE_URL_MAX_LENGTH = 255;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final ShopStaffRepository shopStaffRepository;
    private final ShopAuthorizationService shopAuthorizationService;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ShopRepository shopRepository,
            ShopStaffRepository shopStaffRepository,
            ShopAuthorizationService shopAuthorizationService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.shopRepository = shopRepository;
        this.shopStaffRepository = shopStaffRepository;
        this.shopAuthorizationService = shopAuthorizationService;
    }

    @Override
    public Product getActiveProductById(Long productId) {
        requirePositiveId(productId, "productId");
        return productRepository.findByIdAndActiveTrueAndShop_Status(productId, "ACTIVE")
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm đang hoạt động."));
    }

    @Override
    public Product getPublicProductById(Long productId) {
        return getActiveProductById(productId);
    }

    @Override
    public List<Product> getActiveProductsByShop(Long shopId) {
        requirePositiveId(shopId, "shopId");
        if (shopRepository.findByIdAndStatus(shopId, "ACTIVE").isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy cửa hàng đang hoạt động.");
        }
        return productRepository.findByShop_IdAndActiveTrueAndShop_Status(shopId, "ACTIVE");
    }

    @Override
    public List<Product> getManagedProductsByShop(Long actorUserId, Long shopId) {
        requirePositiveId(shopId, "shopId");
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cửa hàng."));
        shopAuthorizationService.requireShopMember(actorUserId, shop);
        return productRepository.findByShop_Id(shopId);
    }

    @Override
    public List<Product> getActiveProductsByCategory(Long categoryId) {
        requirePositiveId(categoryId, "categoryId");
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục."));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new EntityNotFoundException("Không tìm thấy danh mục đang hoạt động.");
        }
        return productRepository.findByCategory_IdAndActiveTrueAndShop_Status(categoryId, "ACTIVE");
    }

    @Override
    public List<Product> searchActiveProductsByName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Từ khóa tìm kiếm không được để trống.");
        }
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrueAndShop_Status(
                keyword.trim(), "ACTIVE");
    }

    @Override
    public List<Product> getActiveProductsByPriceRange(BigDecimal minimumPrice, BigDecimal maximumPrice) {
        validateNonNegativePrice(minimumPrice, "minimumPrice");
        validateNonNegativePrice(maximumPrice, "maximumPrice");
        if (minimumPrice.compareTo(maximumPrice) > 0) {
            throw new IllegalArgumentException("Giá thấp nhất không được lớn hơn giá cao nhất.");
        }
        return productRepository.findByPriceBetweenAndActiveTrueAndShop_Status(
                minimumPrice, maximumPrice, "ACTIVE");
    }

    @Override
    public Page<Product> searchPublicProducts(
            String keyword,
            Long categoryId,
            BigDecimal minimumPrice,
            BigDecimal maximumPrice,
            Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("Thông tin phân trang không hợp lệ.");
        }
        if (keyword != null && keyword.length() > PRODUCT_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Từ khóa không được vượt quá 180 ký tự.");
        }
        if (categoryId != null) {
            requirePositiveId(categoryId, "categoryId");
            if (categoryRepository.findById(categoryId)
                    .filter(category -> Boolean.TRUE.equals(category.getActive())).isEmpty()) {
                throw new EntityNotFoundException("Không tìm thấy danh mục đang hoạt động.");
            }
        }
        if (minimumPrice != null) validateNonNegativePrice(minimumPrice, "minimumPrice");
        if (maximumPrice != null) validateNonNegativePrice(maximumPrice, "maximumPrice");
        if (minimumPrice != null && maximumPrice != null
                && minimumPrice.compareTo(maximumPrice) > 0) {
            throw new IllegalArgumentException("Giá thấp nhất không được lớn hơn giá cao nhất.");
        }

        Specification<Product> specification = (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.isTrue(root.get("active")));
            predicates.add(builder.equal(root.get("shop").get("status"), "ACTIVE"));
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("name")),
                        "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%"));
            }
            if (categoryId != null) predicates.add(builder.equal(root.get("category").get("id"), categoryId));
            if (minimumPrice != null) predicates.add(builder.greaterThanOrEqualTo(root.get("price"), minimumPrice));
            if (maximumPrice != null) predicates.add(builder.lessThanOrEqualTo(root.get("price"), maximumPrice));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return productRepository.findAll(specification, pageable);
    }

    @Override
    public List<Product> getActiveProductsAtOrBelowStock(Integer maximumStock) {
        if (maximumStock == null || maximumStock < 0) {
            throw new IllegalArgumentException("Giới hạn tồn kho phải lớn hơn hoặc bằng 0.");
        }
        return productRepository.findByStockQuantityLessThanEqualAndActiveTrueAndShop_Status(
                maximumStock, "ACTIVE");
    }

    @Override
    @Transactional
    public Product createProduct(Long actorUserId, Long shopId, Product product) {
        requirePositiveId(actorUserId, "actorUserId");
        requirePositiveId(shopId, "shopId");
        validateProduct(product);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cửa hàng."));
        verifyShopAccess(actorUserId, shop);

        product.setId(null);
        product.setName(product.getName().trim());
        product.setShop(shop);
        product.setCategory(resolveActiveCategory(product.getCategory()));
        product.setActive(true);
        product.setVersion(null);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Long actorUserId, Long productId, Product updates) {
        requirePositiveId(actorUserId, "actorUserId");
        requirePositiveId(productId, "productId");
        validateProduct(updates);

        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm."));
        verifyShopAccess(actorUserId, existing.getShop());

        existing.setName(updates.getName().trim());
        existing.setDescription(updates.getDescription());
        existing.setPrice(updates.getPrice().setScale(2, RoundingMode.UNNECESSARY));
        existing.setStockQuantity(updates.getStockQuantity());
        existing.setImageUrl(updates.getImageUrl());
        existing.setCategory(resolveActiveCategory(updates.getCategory()));
        return productRepository.save(existing);
    }

    @Override
    @Transactional
    public void deactivateProduct(Long actorUserId, Long productId) {
        requirePositiveId(actorUserId, "actorUserId");
        requirePositiveId(productId, "productId");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm."));
        verifyShopAccess(actorUserId, product.getShop());
        product.setActive(false);
        productRepository.save(product);
    }

    private void verifyShopAccess(Long actorUserId, Shop shop) {
        if (shop.getOwner() != null && actorUserId.equals(shop.getOwner().getId())) {
            return;
        }

        ShopStaff membership = shopStaffRepository.findByShop_IdAndUser_Id(shop.getId(), actorUserId)
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền quản lý sản phẩm của cửa hàng này."));
        if (!Boolean.TRUE.equals(membership.getActive())) {
            throw new AccessDeniedException("Tài khoản nhân viên tại cửa hàng này đã bị vô hiệu hóa.");
        }
    }

    private Category resolveActiveCategory(Category requestedCategory) {
        if (requestedCategory == null) {
            return null;
        }

        Long categoryId = requestedCategory.getId();
        requirePositiveId(categoryId, "categoryId");
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy danh mục."));
        if (!Boolean.TRUE.equals(category.getActive())) {
            throw new IllegalArgumentException("Không thể gán sản phẩm vào danh mục đã ngừng hoạt động.");
        }
        return category;
    }

    private void validateProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Thông tin sản phẩm không được để trống.");
        }
        if (product.getName() == null || product.getName().isBlank()
                || product.getName().trim().length() > PRODUCT_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Tên sản phẩm phải có từ 1 đến 180 ký tự.");
        }
        if (product.getDescription() != null
                && product.getDescription().length() > PRODUCT_DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("Mô tả sản phẩm không được vượt quá 4000 ký tự.");
        }
        if (product.getImageUrl() != null && product.getImageUrl().length() > IMAGE_URL_MAX_LENGTH) {
            throw new IllegalArgumentException("URL hình ảnh không được vượt quá 255 ký tự.");
        }
        validatePrice(product.getPrice());
        if (product.getStockQuantity() == null || product.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Số lượng tồn kho phải lớn hơn hoặc bằng 0.");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Giá sản phẩm phải lớn hơn 0.");
        }
        validatePricePrecision(price);
    }

    private void validateNonNegativePrice(BigDecimal price, String fieldName) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " phải lớn hơn hoặc bằng 0.");
        }
        validatePricePrecision(price);
    }

    private void validatePricePrecision(BigDecimal price) {
        try {
            if (price.setScale(2, RoundingMode.UNNECESSARY).precision() > 12) {
                throw new IllegalArgumentException("Giá sản phẩm vượt quá giới hạn 12 chữ số.");
            }
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Giá sản phẩm chỉ được có tối đa 2 chữ số thập phân.", exception);
        }
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
