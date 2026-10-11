package org.example.ridehailing.banhangonline.service.impl;

import org.example.ridehailing.banhangonline.entity.Payment;
import org.example.ridehailing.banhangonline.entity.Order;
import org.example.ridehailing.banhangonline.entity.ShopOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusAggregatorTest {

    @Test
    void returnsCancelledOnlyWhenEveryShopOrderIsCancelled() {
        assertEquals("CANCELLED", OrderStatusAggregator.aggregate(
                List.of(shopOrder("CANCELLED"), shopOrder("CANCELLED"))));
    }

    @Test
    void returnsPartiallyCancelledWhenAtLeastOneShopOrderRemains() {
        assertEquals("PARTIALLY_CANCELLED", OrderStatusAggregator.aggregate(
                List.of(shopOrder("CANCELLED"), shopOrder("PROCESSING"))));
    }

    @Test
    void choosesSlowestFulfillmentStateWhenShopsAreAtDifferentStages() {
        assertEquals("CONFIRMED", OrderStatusAggregator.aggregate(
                List.of(shopOrder("DELIVERED"), shopOrder("PROCESSING"), shopOrder("CONFIRMED"))));
    }

    @Test
    void fulfillmentAggregateIsIndependentFromFailedPaymentState() {
        Order order = new Order();
        order.setShopOrders(List.of(shopOrder("SHIPPED"), shopOrder("DELIVERED")));
        Payment payment = new Payment();
        payment.setStatus("FAILED");
        order.setPayment(payment);

        order.setStatus(OrderStatusAggregator.aggregate(order.getShopOrders()));

        assertEquals("SHIPPED", order.getStatus());
        assertEquals("FAILED", order.getPayment().getStatus());
    }

    @Test
    void rejectsAnOrderWithoutShopOrders() {
        assertThrows(IllegalStateException.class, () -> OrderStatusAggregator.aggregate(List.of()));
    }

    private ShopOrder shopOrder(String status) {
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setStatus(status);
        return shopOrder;
    }
}
