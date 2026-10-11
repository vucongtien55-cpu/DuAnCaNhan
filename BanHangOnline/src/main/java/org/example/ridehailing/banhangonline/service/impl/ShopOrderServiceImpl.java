package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.Order;
import org.example.ridehailing.banhangonline.entity.OrderItem;
import org.example.ridehailing.banhangonline.entity.Product;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.ShopOrder;
import org.example.ridehailing.banhangonline.entity.ShopStaff;
import org.example.ridehailing.banhangonline.exception.BusinessRuleException;
import org.example.ridehailing.banhangonline.repository.OrderRepository;
import org.example.ridehailing.banhangonline.repository.ProductRepository;
import org.example.ridehailing.banhangonline.repository.ShopOrderRepository;
import org.example.ridehailing.banhangonline.repository.ShopRepository;
import org.example.ridehailing.banhangonline.repository.ShopStaffRepository;
import org.example.ridehailing.banhangonline.service.ShopAuthorizationService;
import org.example.ridehailing.banhangonline.service.ShopOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ShopOrderServiceImpl implements ShopOrderService {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "PENDING", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("PROCESSING", "CANCELLED"),
            "PROCESSING", Set.of("SHIPPED", "CANCELLED"),
            "SHIPPED", Set.of("DELIVERED")
    );

    private final ShopOrderRepository shopOrderRepository;
    private final ShopRepository shopRepository;
    private final ShopStaffRepository shopStaffRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ShopAuthorizationService shopAuthorizationService;

    public ShopOrderServiceImpl(
            ShopOrderRepository shopOrderRepository,
            ShopRepository shopRepository,
            ShopStaffRepository shopStaffRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            ShopAuthorizationService shopAuthorizationService) {
        this.shopOrderRepository = shopOrderRepository;
        this.shopRepository = shopRepository;
        this.shopStaffRepository = shopStaffRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.shopAuthorizationService = shopAuthorizationService;
    }

    @Override
    public List<ShopOrder> getShopOrders(Long actorUserId, Long shopId) {
        authorizeShop(actorUserId, shopId);
        return shopOrderRepository.findByShop_IdOrderByCreatedAtDesc(shopId);
    }

    @Override
    public List<ShopOrder> getShopOrdersByStatus(Long actorUserId, Long shopId, String status) {
        authorizeShop(actorUserId, shopId);
        return shopOrderRepository.findByShop_IdAndStatusOrderByCreatedAtDesc(
                shopId, normalizeStatus(status));
    }

    @Override
    public List<ShopOrder> getMyAssignedOrders(Long actorUserId, Long shopId, String status) {
        Shop shop = authorizeShop(actorUserId, shopId);
        ShopStaff staff = shopStaffRepository.findByShop_IdAndUser_Id(shop.getId(), actorUserId)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tư cách nhân viên đang hoạt động."));
        if (status == null || status.isBlank()) {
            return shopOrderRepository.findByShop_IdAndAssignedStaff_IdOrderByCreatedAtDesc(
                    shopId, staff.getId());
        }
        return shopOrderRepository.findByShop_IdAndAssignedStaff_IdAndStatusOrderByCreatedAtDesc(
                shopId, staff.getId(), normalizeStatus(status));
    }

    @Override
    @Transactional
    public ShopOrder updateOrderStatus(Long actorUserId, Long shopId, Long shopOrderId, String status) {
        authorizeShop(actorUserId, shopId);
        ShopOrder shopOrder = getShopOrder(shopId, shopOrderId);
        ShopStaff actorMembership = shopStaffRepository.findByShop_IdAndUser_Id(shopId, actorUserId)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElse(null);
        if (actorMembership != null && (shopOrder.getAssignedStaff() == null
                || !shopOrder.getAssignedStaff().getId().equals(actorMembership.getId()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Nhân viên chỉ được cập nhật đơn hàng được giao cho mình.");
        }
        String nextStatus = normalizeStatus(status);
        Set<String> allowedNextStatuses = ALLOWED_TRANSITIONS.get(shopOrder.getStatus());
        if (allowedNextStatuses == null || !allowedNextStatuses.contains(nextStatus)) {
            throw new IllegalArgumentException(
                    "Không thể chuyển trạng thái đơn từ " + shopOrder.getStatus() + " sang " + nextStatus + ".");
        }
        Order order = shopOrder.getOrder();
        if ("CANCELLED".equals(order.getStatus())
                && !"CANCELLED".equals(OrderStatusAggregator.aggregate(order.getShopOrders()))) {
            throw new IllegalStateException(
                    "Đơn tổng đã bị hủy nhưng trạng thái các đơn shop không nhất quán; cần kiểm tra dữ liệu.");
        }
        if ("CANCELLED".equals(nextStatus)) {
            if (order.getPayment() != null) {
                throw new IllegalArgumentException(
                        "Không thể hủy đơn shop đã có yêu cầu thanh toán khi chưa có luồng hủy thanh toán.");
            }
            restoreShopOrderStock(shopOrder);
        }
        shopOrder.setStatus(nextStatus);
        shopOrderRepository.save(shopOrder);
        order.setStatus(OrderStatusAggregator.aggregate(order.getShopOrders()));
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        return shopOrder;
    }

    @Override
    @Transactional
    public ShopOrder assignOrder(Long actorUserId, Long shopId, Long shopOrderId, Long staffId) {
        Shop shop = authorizeShop(actorUserId, shopId);
        shopAuthorizationService.requireShopOwner(actorUserId, shop);
        ShopOrder shopOrder = getShopOrder(shopId, shopOrderId);
        requirePositiveId(staffId, "staffId");

        ShopStaff staff = shopStaffRepository.findById(staffId)
                .filter(candidate -> candidate.getShop().getId().equals(shop.getId()))
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên đang hoạt động của cửa hàng."));
        shopOrder.setAssignedStaff(staff);
        return shopOrderRepository.save(shopOrder);
    }

    private Shop authorizeShop(Long actorUserId, Long shopId) {
        requirePositiveId(shopId, "shopId");
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy cửa hàng."));
        shopAuthorizationService.requireShopMember(actorUserId, shop);
        return shop;
    }

    private ShopOrder getShopOrder(Long shopId, Long shopOrderId) {
        requirePositiveId(shopOrderId, "shopOrderId");
        return shopOrderRepository.findByIdAndShop_Id(shopOrderId, shopId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn thuộc cửa hàng này."));
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không được để trống.");
        }
        return status.trim().toUpperCase();
    }

    // Chỉ hoàn kho các sản phẩm thuộc đơn shop bị hủy; lỗi số học sẽ rollback toàn transaction.
    private void restoreShopOrderStock(ShopOrder shopOrder) {
        List<Product> productsToRestore = new ArrayList<>();
        for (OrderItem item : shopOrder.getItems()) {
            Product product = item.getProduct();
            if (product.getStockQuantity() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalStateException("Không thể hoàn tồn kho từ dữ liệu đơn hàng không hợp lệ.");
            }
            try {
                product.setStockQuantity(Math.addExact(product.getStockQuantity(), item.getQuantity()));
            } catch (ArithmeticException exception) {
                throw new BusinessRuleException("Tồn kho vượt giới hạn khi hủy đơn shop.");
            }
            productsToRestore.add(product);
        }
        productRepository.saveAll(productsToRestore);
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
