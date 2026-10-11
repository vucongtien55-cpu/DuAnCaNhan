package org.example.ridehailing.banhangonline.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.ridehailing.banhangonline.entity.Order;
import org.example.ridehailing.banhangonline.entity.Payment;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.exception.DuplicateResourceException;
import org.example.ridehailing.banhangonline.repository.OrderRepository;
import org.example.ridehailing.banhangonline.repository.PaymentRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.example.ridehailing.banhangonline.service.PaymentService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private static final Set<String> SUPPORTED_METHODS = Set.of("COD", "CARD", "BANK_TRANSFER", "E_WALLET");
    private static final Set<String> PAYMENT_STATUSES = Set.of("PENDING", "PAID", "FAILED", "CANCELLED");

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Payment createPendingPayment(Long customerId, Long orderId, String method) {
        requireEnabledCustomer(customerId);
        requirePositiveId(orderId, "orderId");
        String normalizedMethod = normalizeMethod(method);
        Order order = orderRepository.findByIdAndCustomer_Id(orderId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng của khách hàng."));
        if (!"PENDING".equals(order.getStatus())
                || !"PENDING".equals(OrderStatusAggregator.aggregate(order.getShopOrders()))) {
            throw new IllegalArgumentException(
                    "Chỉ có thể tạo thanh toán khi mọi cửa hàng vẫn đang chờ xử lý.");
        }
        if (paymentRepository.findByOrder_Id(orderId).isPresent()) {
            throw new DuplicateResourceException("Đơn hàng đã có yêu cầu thanh toán.");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(normalizedMethod);
        payment.setStatus("PENDING");
        payment.setAmount(order.getTotalAmount());
        return paymentRepository.save(payment);
    }

    @Override
    public Payment getCustomerPayment(Long customerId, Long orderId) {
        requireEnabledCustomer(customerId);
        requirePositiveId(orderId, "orderId");
        return paymentRepository.findByOrder_IdAndOrder_Customer_Id(orderId, customerId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thanh toán của đơn hàng."));
    }

    @Override
    public List<Payment> getPaymentsByStatus(Long actorUserId, String status) {
        requireAdministrator(actorUserId);
        if (status == null || !PAYMENT_STATUSES.contains(status.trim().toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ.");
        }
        return paymentRepository.findByStatus(status.trim().toUpperCase(java.util.Locale.ROOT));
    }

    private void requireAdministrator(Long actorUserId) {
        requirePositiveId(actorUserId, "actorUserId");
        User actor = userRepository.findById(actorUserId)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new AccessDeniedException("Tài khoản quản trị không tồn tại hoặc đã bị vô hiệu hóa."));
        if (!"ADMIN".equals(actor.getRole())) {
            throw new AccessDeniedException("Chỉ quản trị viên được truy vấn thanh toán toàn hệ thống.");
        }
    }

    private void requireEnabledCustomer(Long customerId) {
        requirePositiveId(customerId, "customerId");
        if (userRepository.findById(customerId)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .isEmpty()) {
            throw new AccessDeniedException("Tài khoản khách hàng không tồn tại hoặc đã bị vô hiệu hóa.");
        }
    }

    private String normalizeMethod(String method) {
        if (method == null || !SUPPORTED_METHODS.contains(method.trim().toUpperCase())) {
            throw new IllegalArgumentException("Phương thức thanh toán không được hỗ trợ.");
        }
        return method.trim().toUpperCase();
    }

    private void requirePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " phải là số dương.");
        }
    }
}
