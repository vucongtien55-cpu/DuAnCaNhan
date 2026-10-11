package org.example.ridehailing.banhangonline.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Response payloads expose only API-approved data, never persistence entities directly.
public final class ApiResponseDtos {

    private ApiResponseDtos() {
    }

    public record AuthResponse(
            String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {
    }

    public record MessageResponse(String message) {
    }

    public record PageResponse<T>(
            List<T> content, int page, int size, long totalElements, int totalPages,
            boolean first, boolean last) {
    }

    public record UserResponse(
            Long id, String fullName, String email, String role, String phone, String avatarUrl) {
    }

    public record AdminUserResponse(
            Long id, String fullName, String email, String role, String phone,
            Boolean enabled, Boolean locked, Integer failedLoginAttempts) {
    }

    public record ProductResponse(
            Long id, String name, String description, BigDecimal price, Integer stockQuantity,
            String imageUrl, Boolean active, Long categoryId, Long shopId) {
    }

    public record CategoryResponse(
            Long id, String name, String slug, String description, String imageUrl,
            Long parentCategoryId, Boolean active) {
    }

    public record ShopResponse(
            Long id, String name, String description, String address, String phone, String email,
            String logoUrl, String status, Long ownerId) {
    }

    public record ShopStaffResponse(Long id, Long shopId, Long userId, String role, Boolean active) {
    }

    public record ShopCustomerResponse(
            Long id, Long shopId, Long customerId, String status, LocalDateTime createdAt) {
    }

    public record CustomerNoteResponse(
            Long id, Long shopId, Long customerId, Long staffId, Long createdById,
            String note, LocalDateTime appointmentAt, LocalDateTime createdAt) {
    }

    public record CartItemResponse(
            Long id, Long productId, String productName, BigDecimal unitPrice, Integer quantity) {
    }

    public record OrderResponse(
            Long id, String orderNumber, Long customerId, String status,
            BigDecimal subtotal, BigDecimal shippingFee, BigDecimal totalAmount,
            String shippingAddress, String recipientPhone, String customerNote,
            List<ShopOrderResponse> shopOrders, LocalDateTime createdAt) {
    }

    public record ShopOrderResponse(
            Long id, Long orderId, Long shopId, Long assignedStaffId, String status,
            BigDecimal subtotal, BigDecimal shippingFee, BigDecimal totalAmount,
            List<OrderItemResponse> items, LocalDateTime createdAt) {
    }

    public record OrderItemResponse(
            Long id, Long productId, String productName, Integer quantity,
            BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public record PaymentResponse(
            Long id, Long orderId, String method, String status, BigDecimal amount,
            String transactionReference, LocalDateTime paidAt, LocalDateTime createdAt) {
    }

    public record ShopTransactionResponse(
            Long id, Long shopId, Long shopOrderId, String type, String status,
            BigDecimal amount, String transactionReference, LocalDateTime createdAt) {
    }

    public record ReviewResponse(
            Long id, Long userId, Long productId, Integer rating, String comment, LocalDateTime createdAt) {
    }
}
