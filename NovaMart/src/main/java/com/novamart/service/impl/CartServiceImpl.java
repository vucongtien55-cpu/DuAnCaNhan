package com.novamart.service.impl;

import com.novamart.dto.request.AddToCartRequest;
import com.novamart.dto.response.CartResponse;
import com.novamart.entity.*;
import com.novamart.exception.BadRequestException;
import com.novamart.exception.ResourceNotFoundException;
import com.novamart.repository.*;
import com.novamart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CartResponse getMyCart(String customerEmail) {
        Cart cart = getOrCreateCart(customerEmail);
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(String customerEmail, AddToCartRequest request) {
        Cart cart = getOrCreateCart(customerEmail);
        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể sản phẩm"));

        // Kiểm tra trạng thái đang bán của sản phẩm và biến thể
        if (!variant.isActive() || !"ACTIVE".equals(variant.getProduct().getStatus())) {
            throw new BadRequestException("Sản phẩm này hiện đã ngưng kinh doanh!");
        }

        // Kiểm tra số lượng kho sẵn có
        Inventory inventory = inventoryRepository.findByVariantId(variant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin kho"));

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId());
        int currentInCart = existingItemOpt.map(CartItem::getQuantity).orElse(0);
        int newTotalQuantity = currentInCart + request.getQuantity();

        if (newTotalQuantity > inventory.getQuantityAvailable()) {
            throw new BadRequestException("Tồn kho không đủ! Bạn đã có " + currentInCart + " sản phẩm trong giỏ.");
        }

        if (existingItemOpt.isPresent()) {
            // Đã có trong giỏ -> Cộng dồn số lượng
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(newTotalQuantity);
            cartItemRepository.save(existingItem);
        } else {
            // Chưa có -> Thêm dòng mới
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .build();
            cartItemRepository.save(newItem);
        }

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItemQuantity(String customerEmail, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCart(customerEmail);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Bạn không có quyền sửa giỏ hàng này");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            Inventory inventory = inventoryRepository.findByVariantId(item.getVariant().getId()).orElse(null);
            if (inventory != null && quantity > inventory.getQuantityAvailable()) {
                throw new BadRequestException("Số lượng tồn kho không đủ (còn lại: " + inventory.getQuantityAvailable() + ")");
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(String customerEmail, Long cartItemId) {
        Cart cart = getOrCreateCart(customerEmail);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BadRequestException("Bạn không có quyền xóa sản phẩm không thuộc giỏ hàng của mình!");
        }
        cartItemRepository.delete(item);

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(String customerEmail) {
        Cart cart = getOrCreateCart(customerEmail);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    private Cart getOrCreateCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        // Nhóm các CartItem theo từng Shop để hiển thị giao diện Marketplace
        Map<Shop, List<CartItem>> groupedByShop = items.stream()
                .collect(Collectors.groupingBy(item -> item.getVariant().getProduct().getShop()));

        List<CartResponse.ShopCartGroup> shopGroups = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map.Entry<Shop, List<CartItem>> entry : groupedByShop.entrySet()) {
            Shop shop = entry.getKey();
            List<CartItem> shopItems = entry.getValue();

            List<CartResponse.CartItemResponse> itemResponses = new ArrayList<>();
            for (CartItem item : shopItems) {
                BigDecimal unitPrice = item.getVariant().getPrice();
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                grandTotal = grandTotal.add(lineTotal);

                List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(item.getVariant().getProduct().getId());
                String primaryImg = images.isEmpty() ? "" : images.get(0).getImageUrl();

                itemResponses.add(CartResponse.CartItemResponse.builder()
                        .cartItemId(item.getId())
                        .variantId(item.getVariant().getId())
                        .productName(item.getVariant().getProduct().getName())
                        .variantName(item.getVariant().getVariantName())
                        .sku(item.getVariant().getSku())
                        .imageUrl(primaryImg)
                        .unitPrice(unitPrice)
                        .quantity(item.getQuantity())
                        .lineTotal(lineTotal)
                        .build());
            }

            shopGroups.add(CartResponse.ShopCartGroup.builder()
                    .shopId(shop.getId())
                    .shopName(shop.getName())
                    .items(itemResponses)
                    .build());
        }

        return CartResponse.builder()
                .cartId(cart.getId())
                .grandTotal(grandTotal)
                .shopGroups(shopGroups)
                .build();
    }
}