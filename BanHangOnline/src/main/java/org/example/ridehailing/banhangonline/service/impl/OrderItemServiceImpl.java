package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.OrderItem;
import org.example.ridehailing.banhangonline.entity.Product;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.ShopOrder;
import org.example.ridehailing.banhangonline.repository.OrderItemRepository;
import org.example.ridehailing.banhangonline.repository.ProductRepository;
import org.example.ridehailing.banhangonline.repository.ShopOrderRepository;
import org.example.ridehailing.banhangonline.repository.ShopRepository;
import org.example.ridehailing.banhangonline.service.OrderItemService;
import org.example.ridehailing.banhangonline.service.ShopAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ShopRepository shopRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final ProductRepository productRepository;
    private final ShopAuthorizationService shopAuthorizationService;

    public OrderItemServiceImpl(
            OrderItemRepository orderItemRepository,
            ShopRepository shopRepository,
            ShopOrderRepository shopOrderRepository,
            ProductRepository productRepository,
            ShopAuthorizationService shopAuthorizationService) {
        this.orderItemRepository = orderItemRepository;
        this.shopRepository = shopRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.productRepository = productRepository;
        this.shopAuthorizationService = shopAuthorizationService;
    }

    @Override
    public List<OrderItem> getShopOrderItems(Long actorUserId, Long shopId, Long shopOrderId) {
        authorizeShop(actorUserId, shopId);
        requirePositiveId(shopOrderId, "shopOrderId");
        shopOrderRepository.findByIdAndShop_Id(shopOrderId, shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn thuộc cửa hàng này."));
        return orderItemRepository.findByShopOrder_Id(shopOrderId);
    }

    @Override
    public List<OrderItem> getProductSalesHistory(Long actorUserId, Long shopId, Long productId) {
        authorizeShop(actorUserId, shopId);
        requirePositiveId(productId, "productId");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm."));
        if (product.getShop() == null || !product.getShop().getId().equals(shopId)) {
            throw new EntityNotFoundException("Không tìm thấy sản phẩm thuộc cửa hàng này.");
        }
        return orderItemRepository.findByProduct_IdOrderByShopOrder_CreatedAtDesc(productId);
    }

    private void authorizeShop(Long actorUserId, Long shopId) {
        requirePositiveId(shopId, "shopId");
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cửa hàng."));
        shopAuthorizationService.requireShopMember(actorUserId, shop);
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
