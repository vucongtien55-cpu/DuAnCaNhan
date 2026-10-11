package org.example.ridehailing.banhangonline.service.impl;

import org.example.ridehailing.banhangonline.entity.Order;
import org.example.ridehailing.banhangonline.entity.OrderItem;
import org.example.ridehailing.banhangonline.entity.Payment;
import org.example.ridehailing.banhangonline.entity.Product;
import org.example.ridehailing.banhangonline.entity.ShopOrder;
import org.example.ridehailing.banhangonline.repository.CartItemRepository;
import org.example.ridehailing.banhangonline.repository.OrderRepository;
import org.example.ridehailing.banhangonline.repository.ProductRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    @Test
    void cancelWholeOrderRestoresInventoryAndMarksEveryShopCancelled() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        CartItemRepository cartItemRepository = mock(CartItemRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        OrderServiceImpl service = new OrderServiceImpl(
                orderRepository, cartItemRepository, productRepository, userRepository);

        Order order = new Order();
        ShopOrder firstShopOrder = shopOrderWithProduct(order, "PENDING", 4, 2);
        ShopOrder secondShopOrder = shopOrderWithProduct(order, "PENDING", 10, 3);
        order.getShopOrders().addAll(List.of(firstShopOrder, secondShopOrder));
        when(orderRepository.findByIdAndCustomer_Id(12L, 8L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order cancelled = service.cancelPendingOrder(8L, 12L);

        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals(List.of("CANCELLED", "CANCELLED"),
                cancelled.getShopOrders().stream().map(ShopOrder::getStatus).toList());
        assertEquals(List.of(6, 13), cancelled.getShopOrders().stream()
                .map(shopOrder -> shopOrder.getItems().getFirst().getProduct().getStockQuantity()).toList());
        verify(productRepository).saveAll(org.mockito.ArgumentMatchers.anyIterable());
    }

    @Test
    void cancelPendingOrderDoesNotRestoreStockAfterShopStartedProcessing() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        CartItemRepository cartItemRepository = mock(CartItemRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        OrderServiceImpl service = new OrderServiceImpl(
                orderRepository, cartItemRepository, productRepository, userRepository);

        Order order = new Order();
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setStatus("PROCESSING");
        order.setShopOrders(List.of(shopOrder));
        when(orderRepository.findByIdAndCustomer_Id(12L, 8L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> service.cancelPendingOrder(8L, 12L));

        verify(orderRepository).findByIdAndCustomer_Id(12L, 8L);
        verifyNoInteractions(productRepository);
    }

    @Test
    void failedPaymentDoesNotChangeFulfillmentAggregateToPaid() {
        Order order = new Order();
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setStatus("DELIVERED");
        order.getShopOrders().add(shopOrder);
        Payment payment = new Payment();
        payment.setStatus("FAILED");
        order.setPayment(payment);

        String fulfillmentStatus = OrderStatusAggregator.aggregate(order.getShopOrders());

        assertEquals("DELIVERED", fulfillmentStatus);
        assertNotEquals("PAID", fulfillmentStatus);
    }

    private ShopOrder shopOrderWithProduct(Order order, String status, int stock, int quantity) {
        Product product = new Product();
        product.setStockQuantity(stock);
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);

        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setOrder(order);
        shopOrder.setStatus(status);
        shopOrder.getItems().add(item);
        return shopOrder;
    }
}
