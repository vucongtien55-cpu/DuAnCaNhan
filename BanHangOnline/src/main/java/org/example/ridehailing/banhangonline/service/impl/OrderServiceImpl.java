package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.CartItem;
import org.example.ridehailing.banhangonline.entity.Order;
import org.example.ridehailing.banhangonline.entity.OrderItem;
import org.example.ridehailing.banhangonline.entity.Product;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.ShopOrder;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.exception.BusinessRuleException;
import org.example.ridehailing.banhangonline.repository.CartItemRepository;
import org.example.ridehailing.banhangonline.repository.OrderRepository;
import org.example.ridehailing.banhangonline.repository.ProductRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.service.OrderService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final Set<String> PAYMENT_STATUSES = Set.of("PENDING", "PAID", "FAILED", "CANCELLED");
    private static final int ADDRESS_MAX_LENGTH = 300;
    private static final int PHONE_MAX_LENGTH = 20;
    private static final int NOTE_MAX_LENGTH = 1000;
    private static final BigDecimal SHIPPING_FEE = BigDecimal.ZERO.setScale(2);
    private static final Set<String> ORDER_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED",
            "CANCELLED", "PARTIALLY_CANCELLED");

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Order checkout(Long customerId, String shippingAddress, String recipientPhone, String customerNote) {
        User customer = getEnabledUser(customerId);
        validateCheckoutDetails(shippingAddress, recipientPhone, customerNote);

        List<CartItem> cartItems = cartItemRepository.findByUser_Id(customerId);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng không có sản phẩm đang được bán.");
        }

        Map<Long, ShopOrder> shopOrders = new LinkedHashMap<>();
        Map<Long, Product> productsToUpdate = new LinkedHashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            validateCartLine(cartItem, product);
            Shop shop = product.getShop();
            ShopOrder shopOrder = shopOrders.computeIfAbsent(shop.getId(), ignored -> createShopOrder(shop));

            BigDecimal unitPrice = product.getPrice().setScale(2, RoundingMode.UNNECESSARY);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .setScale(2, RoundingMode.UNNECESSARY);

            OrderItem orderItem = new OrderItem();
            orderItem.setShopOrder(shopOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setLineTotal(lineTotal);
            shopOrder.getItems().add(orderItem);

            shopOrder.setSubtotal(shopOrder.getSubtotal().add(lineTotal));
            shopOrder.setTotalAmount(shopOrder.getSubtotal().add(shopOrder.getShippingFee()));
            subtotal = subtotal.add(lineTotal);
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productsToUpdate.put(product.getId(), product);
        }

        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString().replace("-", ""));
        order.setCustomer(customer);
        order.setStatus("PENDING");
        order.setSubtotal(subtotal);
        order.setShippingFee(SHIPPING_FEE);
        order.setTotalAmount(subtotal.add(SHIPPING_FEE));
        order.setShippingAddress(shippingAddress.trim());
        order.setRecipientPhone(recipientPhone.trim());
        order.setCustomerNote(customerNote == null || customerNote.isBlank() ? null : customerNote.trim());
        order.setUpdatedAt(LocalDateTime.now());
        for (ShopOrder shopOrder : shopOrders.values()) {
            shopOrder.setOrder(order);
            order.getShopOrders().add(shopOrder);
        }

        productRepository.saveAll(productsToUpdate.values());
        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);
        return savedOrder;
    }

    @Override
    public Order getCustomerOrder(Long customerId, Long orderId) {
        requirePositiveId(customerId, "customerId");
        requirePositiveId(orderId, "orderId");
        return orderRepository.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng của khách hàng."));
    }

    @Override
    public List<Order> getCustomerOrders(Long customerId) {
        getEnabledUser(customerId);
        return orderRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
    }

    @Override
    public List<Order> getCustomerOrders(Long customerId, String status) {
        getEnabledUser(customerId);
        if (status == null || status.isBlank()) {
            return orderRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);
        }
        return orderRepository.findByCustomer_IdAndStatusOrderByCreatedAtDesc(
                customerId, normalizeStatus(status));
    }

    @Override
    public Order getCustomerOrderByNumber(Long customerId, String orderNumber) {
        getEnabledUser(customerId);
        String normalizedNumber = validateOrderNumber(orderNumber);
        return orderRepository.findByOrderNumberAndCustomer_Id(normalizedNumber, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng của khách hàng."));
    }

    @Override
    public List<Order> getOrdersByPaymentStatus(Long actorUserId, String paymentStatus) {
        requireAdministrator(actorUserId);
        String normalizedStatus = normalizeStatus(paymentStatus);
        if (!PAYMENT_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ.");
        }
        return orderRepository.findByPayment_StatusOrderByCreatedAtDesc(normalizedStatus);
    }

    @Override
    public Order getOrderByNumber(Long actorUserId, String orderNumber) {
        requireAdministrator(actorUserId);
        return orderRepository.findByOrderNumber(validateOrderNumber(orderNumber))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng."));
    }

    @Override
    @Transactional
    public Order cancelPendingOrder(Long customerId, Long orderId) {
        Order order = getCustomerOrder(customerId, orderId);
        String currentStatus = OrderStatusAggregator.aggregate(order.getShopOrders());
        if (!"PENDING".equals(order.getStatus())
                || !"PENDING".equals(currentStatus)
                || order.getShopOrders().stream().anyMatch(shopOrder -> !"PENDING".equals(shopOrder.getStatus()))) {
            throw new IllegalArgumentException(
                    "Chỉ có thể hủy toàn bộ đơn khi tất cả cửa hàng vẫn đang chờ xử lý.");
        }
        if (order.getPayment() != null) {
            throw new IllegalArgumentException(
                    "Không thể hủy đơn đã có yêu cầu thanh toán khi chưa có luồng hủy thanh toán.");
        }

        List<Product> productsToRestore = new ArrayList<>();
        for (ShopOrder shopOrder : order.getShopOrders()) {
            shopOrder.setStatus("CANCELLED");
            for (OrderItem item : shopOrder.getItems()) {
                Product product = item.getProduct();
                try {
                    product.setStockQuantity(Math.addExact(product.getStockQuantity(), item.getQuantity()));
                } catch (ArithmeticException exception) {
                    throw new BusinessRuleException("Tồn kho vượt giới hạn khi hủy đơn hàng.");
                }
                productsToRestore.add(product);
            }
        }
        productRepository.saveAll(productsToRestore);
        order.setStatus(OrderStatusAggregator.aggregate(order.getShopOrders()));
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    private ShopOrder createShopOrder(Shop shop) {
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setShop(shop);
        shopOrder.setStatus("PENDING");
        shopOrder.setSubtotal(BigDecimal.ZERO.setScale(2));
        shopOrder.setShippingFee(SHIPPING_FEE);
        shopOrder.setTotalAmount(SHIPPING_FEE);
        return shopOrder;
    }

    private void validateCartLine(CartItem cartItem, Product product) {
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalArgumentException("Sản phẩm \"" + product.getName() + "\" đã ngừng bán.");
        }
        if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("Giỏ hàng chứa số lượng sản phẩm không hợp lệ.");
        }
        if (product.getPrice() == null || product.getPrice().signum() <= 0) {
            throw new IllegalArgumentException("Sản phẩm có giá không hợp lệ.");
        }
        if (product.getStockQuantity() == null || product.getStockQuantity() < cartItem.getQuantity()) {
            throw new IllegalArgumentException("Sản phẩm \"" + product.getName() + "\" không đủ tồn kho.");
        }
        if (product.getShop() == null || !"ACTIVE".equals(product.getShop().getStatus())) {
            throw new IllegalArgumentException("Cửa hàng bán sản phẩm \"" + product.getName() + "\" không hoạt động.");
        }
    }

    private void validateCheckoutDetails(String address, String phone, String note) {
        if (address == null || address.isBlank() || address.trim().length() > ADDRESS_MAX_LENGTH) {
            throw new IllegalArgumentException("Địa chỉ giao hàng phải có từ 1 đến 300 ký tự.");
        }
        if (phone == null || phone.isBlank() || phone.trim().length() > PHONE_MAX_LENGTH) {
            throw new IllegalArgumentException("Số điện thoại người nhận phải có từ 1 đến 20 ký tự.");
        }
        if (note != null && note.length() > NOTE_MAX_LENGTH) {
            throw new IllegalArgumentException("Ghi chú đơn hàng không được vượt quá 1000 ký tự.");
        }
    }

    private User getEnabledUser(Long userId) {
        requirePositiveId(userId, "customerId");
        return userRepository.findById(userId)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản đang hoạt động."));
    }

    private void requireAdministrator(Long actorUserId) {
        User actor = getEnabledUser(actorUserId);
        if (!"ADMIN".equals(actor.getRole())) {
            throw new AccessDeniedException("Chỉ quản trị viên được truy cập dữ liệu đơn hàng toàn hệ thống.");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || !ORDER_STATUSES.contains(status.trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }
        return status.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String validateOrderNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank() || orderNumber.trim().length() > 40) {
            throw new IllegalArgumentException("Mã đơn hàng không hợp lệ.");
        }
        return orderNumber.trim();
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
