package org.example.ridehailing.banhangonline.service.impl;

import org.example.ridehailing.banhangonline.entity.ShopOrder;

import java.util.List;
import java.util.Locale;
import java.util.Set;

// Tóm tắt tiến độ giao hàng của đơn tổng; trạng thái thanh toán được lưu riêng trên Payment.
final class OrderStatusAggregator {

    private static final List<String> PROGRESS_ORDER = List.of(
            "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED");
    private static final Set<String> VALID_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");

    private OrderStatusAggregator() {
    }

    static String aggregate(List<ShopOrder> shopOrders) {
        if (shopOrders == null || shopOrders.isEmpty()) {
            throw new IllegalStateException("Không thể tổng hợp trạng thái đơn hàng không có đơn của shop.");
        }

        List<String> statuses = shopOrders.stream()
                .map(ShopOrder::getStatus)
                .map(status -> status == null ? "" : status.trim().toUpperCase(Locale.ROOT))
                .toList();
        if (statuses.stream().anyMatch(status -> !VALID_STATUSES.contains(status))) {
            throw new IllegalStateException("Đơn hàng chứa trạng thái ShopOrder không được hỗ trợ.");
        }

        long cancelledCount = statuses.stream().filter("CANCELLED"::equals).count();
        if (cancelledCount == statuses.size()) {
            return "CANCELLED";
        }
        if (cancelledCount > 0) {
            return "PARTIALLY_CANCELLED";
        }

        return PROGRESS_ORDER.stream()
                .filter(statuses::contains)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Không thể tổng hợp trạng thái đơn hàng."));
    }
}
