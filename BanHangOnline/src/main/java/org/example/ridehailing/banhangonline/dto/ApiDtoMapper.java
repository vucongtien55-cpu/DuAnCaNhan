package org.example.ridehailing.banhangonline.dto;

import org.example.ridehailing.banhangonline.entity.CartItem;
import org.example.ridehailing.banhangonline.entity.Category;
import org.example.ridehailing.banhangonline.entity.CustomerNote;
import org.example.ridehailing.banhangonline.entity.Order;
import org.example.ridehailing.banhangonline.entity.OrderItem;
import org.example.ridehailing.banhangonline.entity.Payment;
import org.example.ridehailing.banhangonline.entity.Product;
import org.example.ridehailing.banhangonline.entity.Review;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.ShopCustomer;
import org.example.ridehailing.banhangonline.entity.ShopOrder;
import org.example.ridehailing.banhangonline.entity.ShopStaff;
import org.example.ridehailing.banhangonline.entity.ShopTransaction;
import org.example.ridehailing.banhangonline.entity.User;

import java.util.List;

// Explicit mapping avoids exposing entity relations and writable database fields in API responses.
public final class ApiDtoMapper {

    private ApiDtoMapper() {
    }

    public static ApiResponseDtos.UserResponse toResponse(User user) {
        return new ApiResponseDtos.UserResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getPhone(), user.getAvatarUrl());
    }

    public static ApiResponseDtos.ProductResponse toResponse(Product product) {
        return new ApiResponseDtos.ProductResponse(
                product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getStockQuantity(), product.getImageUrl(), product.getActive(),
                product.getCategory() == null ? null : product.getCategory().getId(),
                product.getShop() == null ? null : product.getShop().getId());
    }

    public static ApiResponseDtos.CategoryResponse toResponse(Category category) {
        return new ApiResponseDtos.CategoryResponse(
                category.getId(), category.getName(), category.getSlug(), category.getDescription(),
                category.getImageUrl(),
                category.getParentCategory() == null ? null : category.getParentCategory().getId(),
                category.getActive());
    }

    public static ApiResponseDtos.ShopResponse toResponse(Shop shop) {
        return new ApiResponseDtos.ShopResponse(
                shop.getId(), shop.getName(), shop.getDescription(), shop.getAddress(), shop.getPhone(),
                shop.getEmail(), shop.getLogoUrl(), shop.getStatus(),
                shop.getOwner() == null ? null : shop.getOwner().getId());
    }

    public static ApiResponseDtos.ShopStaffResponse toResponse(ShopStaff staff) {
        return new ApiResponseDtos.ShopStaffResponse(
                staff.getId(), staff.getShop().getId(), staff.getUser().getId(),
                staff.getRole(), staff.getActive());
    }

    public static ApiResponseDtos.ShopCustomerResponse toResponse(ShopCustomer customer) {
        return new ApiResponseDtos.ShopCustomerResponse(
                customer.getId(), customer.getShop().getId(), customer.getCustomer().getId(),
                customer.getStatus(), customer.getCreatedAt());
    }

    public static ApiResponseDtos.CustomerNoteResponse toResponse(CustomerNote note) {
        return new ApiResponseDtos.CustomerNoteResponse(
                note.getId(), note.getShop().getId(), note.getCustomer().getId(),
                note.getStaff() == null ? null : note.getStaff().getId(),
                note.getCreatedBy() == null ? null : note.getCreatedBy().getId(),
                note.getNote(), note.getAppointmentAt(), note.getCreatedAt());
    }

    public static ApiResponseDtos.CartItemResponse toResponse(CartItem item) {
        return new ApiResponseDtos.CartItemResponse(
                item.getId(), item.getProduct().getId(), item.getProduct().getName(),
                item.getProduct().getPrice(), item.getQuantity());
    }

    public static ApiResponseDtos.OrderResponse toResponse(Order order) {
        return new ApiResponseDtos.OrderResponse(
                order.getId(), order.getOrderNumber(), order.getCustomer().getId(), order.getStatus(),
                order.getSubtotal(), order.getShippingFee(), order.getTotalAmount(),
                order.getShippingAddress(), order.getRecipientPhone(), order.getCustomerNote(),
                order.getShopOrders().stream().map(ApiDtoMapper::toResponse).toList(), order.getCreatedAt());
    }

    public static ApiResponseDtos.ShopOrderResponse toResponse(ShopOrder order) {
        return new ApiResponseDtos.ShopOrderResponse(
                order.getId(), order.getOrder().getId(), order.getShop().getId(),
                order.getAssignedStaff() == null ? null : order.getAssignedStaff().getId(),
                order.getStatus(), order.getSubtotal(), order.getShippingFee(), order.getTotalAmount(),
                order.getItems().stream().map(ApiDtoMapper::toResponse).toList(), order.getCreatedAt());
    }

    public static ApiResponseDtos.OrderItemResponse toResponse(OrderItem item) {
        return new ApiResponseDtos.OrderItemResponse(
                item.getId(), item.getProduct().getId(), item.getProduct().getName(),
                item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
    }

    public static ApiResponseDtos.PaymentResponse toResponse(Payment payment) {
        return new ApiResponseDtos.PaymentResponse(
                payment.getId(), payment.getOrder().getId(), payment.getMethod(), payment.getStatus(),
                payment.getAmount(), payment.getTransactionReference(), payment.getPaidAt(),
                payment.getCreatedAt());
    }

    public static ApiResponseDtos.ShopTransactionResponse toResponse(ShopTransaction transaction) {
        return new ApiResponseDtos.ShopTransactionResponse(
                transaction.getId(), transaction.getShop().getId(),
                transaction.getShopOrder() == null ? null : transaction.getShopOrder().getId(),
                transaction.getType(), transaction.getStatus(), transaction.getAmount(),
                transaction.getTransactionReference(), transaction.getCreatedAt());
    }

    public static ApiResponseDtos.ReviewResponse toResponse(Review review) {
        return new ApiResponseDtos.ReviewResponse(
                review.getId(), review.getUser().getId(), review.getProduct().getId(),
                review.getRating(), review.getComment(), review.getCreatedAt());
    }

    public static <T, R> List<R> map(List<T> values, java.util.function.Function<T, R> mapper) {
        return values.stream().map(mapper).toList();
    }
}
