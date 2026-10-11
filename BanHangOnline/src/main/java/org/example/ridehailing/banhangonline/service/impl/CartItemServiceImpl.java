package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.ridehailing.banhangonline.entity.CartItem;
import org.example.ridehailing.banhangonline.entity.Product;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.repository.CartItemRepository;
import org.example.ridehailing.banhangonline.repository.ProductRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.exception.BusinessRuleException;
import org.example.ridehailing.banhangonline.service.CartItemService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CartItemServiceImpl implements CartItemService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartItemServiceImpl(
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<CartItem> getMyCart(Long actorUserId) {
        getEnabledUser(actorUserId);
        return cartItemRepository.findByUser_Id(actorUserId);
    }

    @Override
    @Transactional
    public CartItem addOrUpdateItem(Long actorUserId, Long productId, Integer quantity) {
        User user = getEnabledUser(actorUserId);
        requirePositiveId(productId, "productId");
        requirePositiveQuantity(quantity);
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm đang hoạt động."));
        ensureProductCanBeAdded(product, quantity);

        CartItem item = cartItemRepository.findByUser_IdAndProduct_Id(actorUserId, productId)
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setUser(user);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    return newItem;
                });
        int newQuantity;
        try {
            newQuantity = Math.addExact(item.getQuantity(), quantity);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("Tổng số lượng sản phẩm trong giỏ vượt giới hạn cho phép.");
        }
        ensureProductCanBeAdded(product, newQuantity);
        item.setQuantity(newQuantity);
        return cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public CartItem updateQuantity(Long actorUserId, Long cartItemId, Integer quantity) {
        getEnabledUser(actorUserId);
        requirePositiveId(cartItemId, "cartItemId");
        requirePositiveQuantity(quantity);
        CartItem item = getOwnedCartItem(actorUserId, cartItemId);
        Product product = productRepository.findByIdAndActiveTrue(item.getProduct().getId())
                .orElseThrow(() -> new EntityNotFoundException("Sản phẩm không còn được bán."));
        ensureProductCanBeAdded(product, quantity);
        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void removeItem(Long actorUserId, Long cartItemId) {
        getEnabledUser(actorUserId);
        cartItemRepository.delete(getOwnedCartItem(actorUserId, cartItemId));
    }

    @Override
    @Transactional
    public void clearCart(Long actorUserId) {
        getEnabledUser(actorUserId);
        cartItemRepository.deleteAll(cartItemRepository.findByUser_Id(actorUserId));
    }

    private CartItem getOwnedCartItem(Long actorUserId, Long cartItemId) {
        requirePositiveId(cartItemId, "cartItemId");
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy dòng giỏ hàng."));
        if (!item.getUser().getId().equals(actorUserId)) {
            throw new AccessDeniedException("Dòng giỏ hàng không thuộc về người dùng hiện tại.");
        }
        return item;
    }

    private User getEnabledUser(Long userId) {
        requirePositiveId(userId, "actorUserId");
        return userRepository.findById(userId)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản đang hoạt động."));
    }

    private void ensureProductCanBeAdded(Product product, int quantity) {
        if (product.getShop() == null || !"ACTIVE".equals(product.getShop().getStatus())) {
            throw new IllegalArgumentException("Không thể mua sản phẩm từ cửa hàng không hoạt động.");
        }
        if (product.getStockQuantity() == null || product.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("Số lượng yêu cầu vượt quá tồn kho.");
        }
    }

    private void requirePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Số lượng phải là số nguyên dương.");
        }
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
