package com.novamart.service.impl;

import com.novamart.dto.response.PaymentResponse;
import com.novamart.entity.Order;
import com.novamart.entity.Payment;
import com.novamart.entity.RoleName;
import com.novamart.entity.User;
import com.novamart.exception.BadRequestException;
import com.novamart.exception.ResourceNotFoundException;
import com.novamart.repository.OrderRepository;
import com.novamart.repository.PaymentRepository;
import com.novamart.repository.UserRepository;
import com.novamart.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Payment createPayment(Long orderId, String method, BigDecimal amount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        Payment payment = Payment.builder()
                .order(order)
                .method(method)
                .status("PENDING")
                .amount(amount)
                .build();

        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public PaymentResponse confirmMockPayment(String userEmail, Long orderId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isOwner = order.getCustomer().getId().equals(user.getId());

        // Chống lỗ hổng IDOR: Chỉ chủ đơn hàng hoặc Admin mới được xác nhận thanh toán
        if (!isOwner && !isAdmin) {
            throw new BadRequestException("Bạn không có quyền xác nhận thanh toán cho đơn hàng này!");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));

        if ("SUCCESS".equals(payment.getStatus())) {
            throw new BadRequestException("Đơn hàng này đã được thanh toán trước đó!");
        }

        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        payment.setProviderReference("MOCK-PAY-" + System.currentTimeMillis());
        Payment savedPayment = paymentRepository.save(payment);

        // Cập nhật trạng thái đơn hàng sang PROCESSING khi thanh toán thành công
        if ("PENDING".equals(order.getStatus())) {
            order.setStatus("PROCESSING");
            orderRepository.save(order);
        }

        return mapToResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse markCodAsCollected(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán"));

        if (!"COD".equals(payment.getMethod())) {
            throw new BadRequestException("Phương thức thanh toán của đơn hàng không phải là COD!");
        }

        payment.setStatus("SUCCESS");
        payment.setPaidAt(LocalDateTime.now());
        payment.setProviderReference("COD-COLLECTED-" + System.currentTimeMillis());
        Payment savedPayment = paymentRepository.save(payment);

        // Cập nhật trạng thái đơn hàng
        Order order = payment.getOrder();
        if (order != null && "PENDING".equals(order.getStatus())) {
            order.setStatus("PROCESSING");
            orderRepository.save(order);
        }

        return mapToResponse(savedPayment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .orderCode(payment.getOrder() != null ? payment.getOrder().getOrderCode() : null)
                .method(payment.getMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .providerReference(payment.getProviderReference())
                .build();
    }
}
