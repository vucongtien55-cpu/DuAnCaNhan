package com.novamart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
//File chi tiết đơn hàng
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    // ID và mã đơn hàng tổng
    private Long orderId;
    private String orderCode;

    // Thông tin người nhận snapshot
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;

    // Các khoản tiền tổng của đơn
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal shippingTotal;
    private BigDecimal grandTotal;

    // Trạng thái đơn tổng và phương thức thanh toán
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private String note;
    private LocalDateTime createdAt;

    // Danh sách đơn con phân theo từng Shop
    private List<ShopOrderResponse> shopOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopOrderResponse {
        private Long shopOrderId;
        private String shopOrderCode;
        private Long shopId;
        private String shopName;
        private BigDecimal subtotal;
        private BigDecimal shippingFee;
        private BigDecimal discountAmount;
        private BigDecimal sellerAmount;
        private String status;
        private String cancelReason;
        private List<OrderItemResponse> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long orderItemId;
        private String productName;
        private String variantName;
        private String sku;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal lineTotal;
    }
}