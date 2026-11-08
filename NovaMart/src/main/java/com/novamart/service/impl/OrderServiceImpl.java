package com.novamart.service.impl;

import com.novamart.dto.request.CheckoutRequest;
import com.novamart.dto.response.OrderResponse;
import com.novamart.entity.*;
import com.novamart.exception.BadRequestException;
import com.novamart.exception.ResourceNotFoundException;
import com.novamart.repository.*;
import com.novamart.service.InventoryService;
import com.novamart.service.OrderService;
import com.novamart.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShopOrderRepository shopOrderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PaymentService paymentService;

    @Override
    @Transactional // Đảm bảo toàn bộ luồng Checkout là một Transaction nguyên tử
    public OrderResponse checkout(String customerEmail, CheckoutRequest request) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ giao hàng"));

        Cart cart = cartRepository.findByUserId(customer.getId())
                .orElseThrow(() -> new BadRequestException("Giỏ hàng của bạn đang trống"));

        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Giỏ hàng của bạn đang trống!");
        }

        // Nhóm các dòng hàng theo từng Shop để tiến hành tách đơn
        Map<Shop, List<CartItem>> itemsByShop = cartItems.stream()
                .collect(Collectors.groupingBy(item -> item.getVariant().getProduct().getShop()));

        String fullAddressText = String.format("%s, %s, %s, %s",
                address.getDetail(), address.getWard(), address.getDistrict(), address.getProvince());

        String orderCode = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // 1. Khởi tạo Đơn hàng tổng (Order)
        Order order = Order.builder()
                .orderCode(orderCode)
                .customer(customer)
                .recipientName(address.getRecipientName())
                .recipientPhone(address.getRecipientPhone())
                .shippingAddress(fullAddressText)
                .subtotal(BigDecimal.ZERO)
                .discountTotal(BigDecimal.ZERO)
                .shippingTotal(BigDecimal.ZERO)
                .grandTotal(BigDecimal.ZERO)
                .status("PENDING")
                .note(request.getNote())
                .build();
        Order savedOrder = orderRepository.save(order);

        BigDecimal overallSubtotal = BigDecimal.ZERO;

        // 2. Tách đơn con (ShopOrder) và tạo dòng sản phẩm chi tiết (OrderItem) cho từng Shop
        for (Map.Entry<Shop, List<CartItem>> entry : itemsByShop.entrySet()) {
            Shop shop = entry.getKey();
            List<CartItem> shopCartItems = entry.getValue();

            BigDecimal shopSubtotal = BigDecimal.ZERO;
            String shopOrderCode = orderCode + "-S" + shop.getId();

            ShopOrder shopOrder = ShopOrder.builder()
                    .order(savedOrder)
                    .shop(shop)
                    .shopOrderCode(shopOrderCode)
                    .subtotal(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .sellerAmount(BigDecimal.ZERO)
                    .status("PENDING")
                    .build();
            ShopOrder savedShopOrder = shopOrderRepository.save(shopOrder);

            for (CartItem ci : shopCartItems) {
                BigDecimal unitPrice = ci.getVariant().getPrice(); // Lấy giá từ DB
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
                shopSubtotal = shopSubtotal.add(lineTotal);

                // Giữ chỗ tồn kho trong transaction (đã có Pessimistic Lock)
                inventoryService.reserveStock(ci.getVariant().getId(), ci.getQuantity(), savedOrder.getId());

                // Snapshot lịch sử sản phẩm tại thời điểm đặt mua
                OrderItem orderItem = OrderItem.builder()
                        .shopOrder(savedShopOrder)
                        .variant(ci.getVariant())
                        .productNameSnapshot(ci.getVariant().getProduct().getName())
                        .variantSnapshot(ci.getVariant().getVariantName())
                        .skuSnapshot(ci.getVariant().getSku())
                        .unitPrice(unitPrice)
                        .quantity(ci.getQuantity())
                        .lineTotal(lineTotal)
                        .build();
                orderItemRepository.save(orderItem);
            }

            savedShopOrder.setSubtotal(shopSubtotal);
            savedShopOrder.setSellerAmount(shopSubtotal);
            shopOrderRepository.save(savedShopOrder);

            overallSubtotal = overallSubtotal.add(shopSubtotal);
        }

        // Cập nhật tổng tiền đơn tổng
        savedOrder.setSubtotal(overallSubtotal);
        savedOrder.setGrandTotal(overallSubtotal);
        orderRepository.save(savedOrder);

        // 3. Khởi tạo bản ghi thanh toán (Payment)
        paymentService.createPayment(savedOrder.getId(), request.getPaymentMethod(), overallSubtotal);

        // 4. Xóa sạch giỏ hàng sau khi đặt thành công
        cartItemRepository.deleteByCartId(cart.getId());

        return mapToResponse(savedOrder);
    }

    @Override
    public Page<OrderResponse> getMyOrders(String customerEmail, Pageable pageable) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Override
    public OrderResponse getOrderByCode(String customerEmail, String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        User user = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOwner = order.getCustomer().getId().equals(user.getId());

        // Chống lỗ hổng IDOR: Chỉ chủ đơn hàng hoặc Admin mới có quyền xem thông tin chi tiết
        if (!isOwner && !isAdmin) {
            throw new BadRequestException("Bạn không có quyền xem thông tin đơn hàng này!");
        }

        return mapToResponse(order);
    }

    @Override
    public Page<OrderResponse.ShopOrderResponse> getMyShopOrders(String sellerEmail, Pageable pageable) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Truy vấn trực tiếp Shop của Seller qua ShopRepository thay vì quét toàn bộ bảng shopOrderRepository.findAll()
        Shop shop = shopRepository.findByOwnerId(seller.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa sở hữu gian hàng nào"));

        return shopOrderRepository.findByShopIdOrderByCreatedAtDesc(shop.getId(), pageable)
                .map(this::mapToShopOrderResponse);
    }

    @Override
    @Transactional
    public OrderResponse.ShopOrderResponse updateShopOrderStatus(String sellerEmail, Long shopOrderId, String newStatus) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        ShopOrder shopOrder = shopOrderRepository.findById(shopOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng của Shop"));

        boolean isAdmin = seller.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isShopOwner = shopOrder.getShop().getOwner().getId().equals(seller.getId());

        // Chống lỗ hổng IDOR: Chỉ chủ gian hàng của đơn con hoặc Admin mới được cập nhật trạng thái
        if (!isShopOwner && !isAdmin) {
            throw new BadRequestException("Bạn không có quyền cập nhật trạng thái đơn hàng của gian hàng khác!");
        }

        shopOrder.setStatus(newStatus);
        ShopOrder updated = shopOrderRepository.save(shopOrder);

        // Cập nhật trạng thái tổng thể của Đơn tổng nếu tất cả đơn con hoàn thành
        updateParentOrderStatus(shopOrder.getOrder());

        return mapToShopOrderResponse(updated);
    }

    @Override
    @Transactional
    public void cancelOrder(String userEmail, Long orderId, String reason) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        boolean isBuyer = order.getCustomer().getId().equals(user.getId());
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        // Chống lỗ hổng IDOR: Chỉ người đặt đơn hoặc Admin mới có quyền hủy toàn bộ đơn tổng
        if (!isBuyer && !isAdmin) {
            throw new BadRequestException("Bạn không có quyền hủy đơn hàng này!");
        }

        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BadRequestException("Không thể hủy đơn hàng ở trạng thái này!");
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);

        List<ShopOrder> shopOrders = shopOrderRepository.findByOrderId(order.getId());
        for (ShopOrder shopOrder : shopOrders) {
            if (!"DELIVERED".equals(shopOrder.getStatus()) && !"CANCELLED".equals(shopOrder.getStatus())) {
                shopOrder.setStatus("CANCELLED");
                shopOrder.setCancelReason(reason);
                shopOrderRepository.save(shopOrder);

                List<OrderItem> items = orderItemRepository.findByShopOrderId(shopOrder.getId());
                for (OrderItem item : items) {
                    if (item.getVariant() != null) {
                        inventoryService.releaseStock(item.getVariant().getId(), item.getQuantity(), order.getId());
                    }
                }
            }
        }
    }

    @Override
    @Transactional
    public void cancelShopOrder(String userEmail, Long shopOrderId, String reason) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        ShopOrder shopOrder = shopOrderRepository.findById(shopOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng của Shop"));

        boolean isBuyer = shopOrder.getOrder().getCustomer().getId().equals(user.getId());
        boolean isSeller = shopOrder.getShop().getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        // Chống lỗ hổng IDOR: Chỉ người mua, chủ Shop tương ứng hoặc Admin mới có quyền hủy đơn
        if (!isBuyer && !isSeller && !isAdmin) {
            throw new BadRequestException("Bạn không có quyền hủy đơn hàng này!");
        }

        if ("DELIVERED".equals(shopOrder.getStatus()) || "CANCELLED".equals(shopOrder.getStatus())) {
            throw new BadRequestException("Không thể hủy đơn hàng ở trạng thái này!");
        }

        shopOrder.setStatus("CANCELLED");
        shopOrder.setCancelReason(reason);
        shopOrderRepository.save(shopOrder);

        // Hoàn lại số lượng tồn kho đã giữ chỗ
        List<OrderItem> items = orderItemRepository.findByShopOrderId(shopOrder.getId());
        for (OrderItem item : items) {
            if (item.getVariant() != null) {
                inventoryService.releaseStock(item.getVariant().getId(), item.getQuantity(), shopOrder.getOrder().getId());
            }
        }

        updateParentOrderStatus(shopOrder.getOrder());
    }

    private void updateParentOrderStatus(Order order) {
        List<ShopOrder> shopOrders = shopOrderRepository.findByOrderId(order.getId());
        boolean allCompleted = !shopOrders.isEmpty() && shopOrders.stream().allMatch(so -> "DELIVERED".equals(so.getStatus()));
        boolean allCancelled = !shopOrders.isEmpty() && shopOrders.stream().allMatch(so -> "CANCELLED".equals(so.getStatus()));

        if (allCompleted) {
            order.setStatus("COMPLETED");
        } else if (allCancelled) {
            order.setStatus("CANCELLED");
        } else {
            order.setStatus("PROCESSING");
        }
        orderRepository.save(order);
    }

    private OrderResponse mapToResponse(Order order) {
        List<ShopOrder> shopOrders = shopOrderRepository.findByOrderId(order.getId());
        List<OrderResponse.ShopOrderResponse> shopOrderResponses = shopOrders.stream()
                .map(this::mapToShopOrderResponse)
                .collect(Collectors.toList());

        // Lấy thông tin thanh toán bổ sung cho OrderResponse
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        String paymentMethod = payment != null ? payment.getMethod() : null;
        String paymentStatus = payment != null ? payment.getStatus() : null;

        return OrderResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .subtotal(order.getSubtotal())
                .discountTotal(order.getDiscountTotal())
                .shippingTotal(order.getShippingTotal())
                .grandTotal(order.getGrandTotal())
                .status(order.getStatus())
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .shopOrders(shopOrderResponses)
                .build();
    }

    private OrderResponse.ShopOrderResponse mapToShopOrderResponse(ShopOrder shopOrder) {
        List<OrderItem> items = orderItemRepository.findByShopOrderId(shopOrder.getId());
        List<OrderResponse.OrderItemResponse> itemResponses = items.stream().map(item ->
                OrderResponse.OrderItemResponse.builder()
                        .orderItemId(item.getId())
                        .productName(item.getProductNameSnapshot())
                        .variantName(item.getVariantSnapshot())
                        .sku(item.getSkuSnapshot())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .lineTotal(item.getLineTotal())
                        .build()
        ).collect(Collectors.toList());

        return OrderResponse.ShopOrderResponse.builder()
                .shopOrderId(shopOrder.getId())
                .shopOrderCode(shopOrder.getShopOrderCode())
                .shopId(shopOrder.getShop().getId())
                .shopName(shopOrder.getShop().getName())
                .subtotal(shopOrder.getSubtotal())
                .shippingFee(shopOrder.getShippingFee())
                .discountAmount(shopOrder.getDiscountAmount())
                .sellerAmount(shopOrder.getSellerAmount())
                .status(shopOrder.getStatus())
                .cancelReason(shopOrder.getCancelReason())
                .items(itemResponses)
                .build();
    }
}
