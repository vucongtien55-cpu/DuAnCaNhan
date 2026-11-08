package com.novamart.service.impl;

import com.novamart.dto.request.CreateProductRequest;
import com.novamart.dto.response.ProductResponse;
import com.novamart.entity.*;
import com.novamart.exception.BadRequestException;
import com.novamart.exception.ResourceNotFoundException;
import com.novamart.repository.*;
import com.novamart.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryTransactionRepository transactionRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Page<ProductResponse> getActiveProducts(Pageable pageable) {
        return productRepository.findByStatus("ACTIVE", pageable)
                .map(this::mapToResponse);
    }

    @Override
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(String sellerEmail, CreateProductRequest request) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản"));

        // Lấy Shop của Seller và đảm bảo Shop đang hoạt động (ACTIVE)
        Shop shop = shopRepository.findByOwnerId(seller.getId())
                .orElseThrow(() -> new BadRequestException("Bạn chưa có gian hàng để đăng sản phẩm"));

        if (!"ACTIVE".equals(shop.getStatus())) {
            throw new BadRequestException("Gian hàng của bạn chưa được duyệt hoặc đang bị khóa!");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        String slug = generateSlug(request.getName());

        // 1. Tạo sản phẩm gốc
        Product product = Product.builder()
                .shop(shop)
                .category(category)
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .status("ACTIVE")
                .build();
        Product savedProduct = productRepository.save(product);

        // 2. Lưu danh sách ảnh sản phẩm
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            int order = 0;
            for (String imgUrl : request.getImageUrls()) {
                ProductImage image = ProductImage.builder()
                        .product(savedProduct)
                        .imageUrl(imgUrl)
                        .isPrimary(order == 0) // Ảnh đầu tiên làm ảnh chính
                        .displayOrder(order++)
                        .build();
                productImageRepository.save(image);
            }
        }

        // 3. Lưu biến thể và khởi tạo bản ghi kho hàng (Inventory) tương ứng
        for (CreateProductRequest.VariantRequest vReq : request.getVariants()) {
            if (variantRepository.existsBySku(vReq.getSku())) {
                throw new BadRequestException("Mã SKU " + vReq.getSku() + " đã tồn tại trên hệ thống!");
            }

            ProductVariant variant = ProductVariant.builder()
                    .product(savedProduct)
                    .sku(vReq.getSku())
                    .variantName(vReq.getVariantName())
                    .price(vReq.getPrice())
                    .compareAtPrice(vReq.getCompareAtPrice())
                    .active(true)
                    .build();
            ProductVariant savedVariant = variantRepository.save(variant);

            // Khởi tạo số lượng kho ban đầu
            Inventory inventory = Inventory.builder()
                    .variant(savedVariant)
                    .quantityAvailable(vReq.getInitialStock())
                    .quantityReserved(0)
                    .build();
            inventoryRepository.save(inventory);

            // Ghi lịch sử biến động kho ban đầu nếu có tồn kho khởi tạo
            if (vReq.getInitialStock() > 0) {
                InventoryTransaction tx = InventoryTransaction.builder()
                        .variant(savedVariant)
                        .changeQuantity(vReq.getInitialStock())
                        .transactionType("RESTOCK")
                        .referenceType("INITIAL")
                        .note("Khởi tạo tồn kho ban đầu khi tạo sản phẩm")
                        .createdBy(seller)
                        .build();
                transactionRepository.save(tx);
            }
        }

        return mapToResponse(savedProduct);
    }

    @Override
    public Page<ProductResponse> getMyShopProducts(String sellerEmail, Pageable pageable) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        Shop shop = shopRepository.findByOwnerId(seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gian hàng"));

        return productRepository.findByShopId(shop.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ProductResponse updateProductStatus(String sellerEmail, Long productId, String status) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        boolean isAdmin = seller.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOwner = product.getShop().getOwner().getId().equals(seller.getId());

        // Chống lỗ hổng IDOR: Chỉ chủ gian hàng hoặc Admin mới có quyền cập nhật trạng thái sản phẩm
        if (!isOwner && !isAdmin) {
            throw new BadRequestException("Bạn không có quyền thay đổi trạng thái sản phẩm của gian hàng khác!");
        }

        product.setStatus(status);
        return mapToResponse(productRepository.save(product));
    }

    private ProductResponse mapToResponse(Product product) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        List<String> imageUrls = images.stream().map(ProductImage::getImageUrl).collect(Collectors.toList());

        List<ProductVariant> variants = variantRepository.findByProductId(product.getId());

        // Tính giá nhỏ nhất và lớn nhất trong các biến thể để hiển thị khoảng giá
        BigDecimal minPrice = variants.stream().map(ProductVariant::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = variants.stream().map(ProductVariant::getPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        List<ProductResponse.VariantResponse> vResponses = variants.stream().map(v -> {
            Inventory inv = inventoryRepository.findByVariantId(v.getId()).orElse(null);
            int stock = inv != null ? inv.getQuantityAvailable() : 0;
            return ProductResponse.VariantResponse.builder()
                    .id(v.getId())
                    .sku(v.getSku())
                    .variantName(v.getVariantName())
                    .price(v.getPrice())
                    .compareAtPrice(v.getCompareAtPrice())
                    .active(v.isActive())
                    .stockAvailable(stock)
                    .build();
        }).collect(Collectors.toList());

        return ProductResponse.builder()
                .id(product.getId())
                .shopId(product.getShop().getId())
                .shopName(product.getShop().getName())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .status(product.getStatus())
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .imageUrls(imageUrls)
                .variants(vResponses)
                .build();
    }

    private String generateSlug(String input) {
        if (input == null) return "";
        // Xử lý chữ đ và Đ trước khi chuẩn hóa Unicode NFD
        String normalized = input.replace("đ", "d").replace("Đ", "D");
        String nowhitespace = Pattern.compile("[\\s+]").matcher(normalized).replaceAll("-");
        String decomposed = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w-]").matcher(decomposed).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-");
    }
}
