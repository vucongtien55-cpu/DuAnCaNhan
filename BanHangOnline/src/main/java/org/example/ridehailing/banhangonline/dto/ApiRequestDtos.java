package org.example.ridehailing.banhangonline.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Request payloads define which client-provided fields are accepted and validated.
public final class ApiRequestDtos {

    private ApiRequestDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @Size(max = 20) String phone) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 1, max = 72) String password) {
    }

    public record PasswordResetRequest(
            @NotBlank @Email @Size(max = 180) String email) {
    }

    public record PasswordResetCompletionRequest(
            @NotBlank @Size(max = 128) String token,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    public record PasswordChangeRequest(
            @NotBlank @Size(min = 1, max = 72) String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    public record ProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 20) String phone,
            @Size(max = 255) String avatarUrl) {
    }

    public record ProductRequest(
            @NotBlank @Size(max = 180) String name,
            @Size(max = 4000) String description,
            @NotNull @DecimalMin(value = "0.00", inclusive = false)
            @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull @Min(0) Integer stockQuantity,
            @Size(max = 255) String imageUrl,
            Long categoryId) {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 160)
            @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
            @Size(max = 1000) String description,
            @Size(max = 255) String imageUrl,
            Long parentCategoryId) {
    }

    public record ShopRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 2000) String description,
            @Size(max = 300) String address,
            @Size(max = 20) String phone,
            @Email @Size(max = 180) String email,
            @Size(max = 255) String logoUrl) {
    }

    public record StaffRequest(@NotNull @Positive Long userId) {
    }

    public record ShopCustomerRequest(@NotNull @Positive Long customerId) {
    }

    public record ShopCustomerStatusRequest(
            @NotBlank @Pattern(regexp = "(?i)ACTIVE|INACTIVE|BLOCKED") String status) {
    }

    public record CustomerNoteRequest(
            @NotNull @Positive Long customerId,
            @NotBlank @Size(max = 2000) String note,
            LocalDateTime appointmentAt) {
    }

    public record CartItemRequest(
            @NotNull @Positive Long productId,
            @NotNull @Min(1) Integer quantity) {
    }

    public record CartQuantityRequest(@NotNull @Min(1) Integer quantity) {
    }

    public record CheckoutRequest(
            @NotBlank @Size(max = 300) String shippingAddress,
            @NotBlank @Size(max = 20) String recipientPhone,
            @Size(max = 1000) String customerNote) {
    }

    public record ShopOrderStatusRequest(@NotBlank @Size(max = 30) String status) {
    }

    public record AssignStaffRequest(@NotNull @Positive Long staffId) {
    }

    public record PaymentRequest(
            @NotBlank @Pattern(regexp = "(?i)COD|CARD|BANK_TRANSFER|E_WALLET") String method) {
    }

    public record ShopTransactionRequest(
            Long shopOrderId,
            @NotBlank @Pattern(regexp = "(?i)SALE|REFUND|PAYOUT|FEE|ADJUSTMENT") String type,
            @NotNull @DecimalMin(value = "0.00", inclusive = false)
            @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @Size(max = 120) String transactionReference) {
    }

    public record ReviewRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 2000) String comment) {
    }
}
