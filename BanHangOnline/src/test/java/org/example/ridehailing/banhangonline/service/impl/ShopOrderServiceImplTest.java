package org.example.ridehailing.banhangonline.service.impl;

import org.example.ridehailing.banhangonline.entity.Order;
import org.example.ridehailing.banhangonline.entity.OrderItem;
import org.example.ridehailing.banhangonline.entity.Payment;
import org.example.ridehailing.banhangonline.entity.Product;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.entity.ShopOrder;
import org.example.ridehailing.banhangonline.repository.OrderRepository;
import org.example.ridehailing.banhangonline.repository.ProductRepository;
import org.example.ridehailing.banhangonline.repository.ShopOrderRepository;
import org.example.ridehailing.banhangonline.repository.ShopRepository;
import org.example.ridehailing.banhangonline.repository.ShopStaffRepository;
import org.example.ridehailing.banhangonline.service.ShopAuthorizationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShopOrderServiceImplTest {

    @Test
    void cancellingOneShopOrderRestoresOnlyItsInventoryAndMarksParentPartiallyCancelled() {
        TestContext context = new TestContext();
        Order order = new Order();
        Shop shopOrderProduct = new Shop();
        shopOrderProduct.setId(3L);
        Product product = new Product();
        product.setStockQuantity(5);
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);

        ShopOrder toCancel = shopOrder(order, shopOrderProduct, "PENDING");
        toCancel.getItems().add(item);
        ShopOrder remaining = shopOrder(order, new Shop(), "PROCESSING");
        order.getShopOrders().addAll(List.of(toCancel, remaining));
        when(context.shopRepository.findById(3L)).thenReturn(Optional.of(shopOrderProduct));
        when(context.shopOrderRepository.findByIdAndShop_Id(7L, 3L)).thenReturn(Optional.of(toCancel));

        ShopOrder result = context.service.updateOrderStatus(9L, 3L, 7L, "CANCELLED");

        assertEquals("CANCELLED", result.getStatus());
        assertEquals(7, product.getStockQuantity());
        assertEquals("PARTIALLY_CANCELLED", order.getStatus());
        verify(context.productRepository).saveAll(anyIterable());
        verify(context.orderRepository).save(order);
    }

    @Test
    void parentBecomesCancelledOnlyAfterEveryShopOrderIsCancelled() {
        TestContext context = new TestContext();
        Order order = new Order();
        Shop shop = new Shop();
        shop.setId(3L);
        ShopOrder toCancel = shopOrder(order, shop, "PENDING");
        ShopOrder alreadyCancelled = shopOrder(order, new Shop(), "CANCELLED");
        order.getShopOrders().addAll(List.of(toCancel, alreadyCancelled));
        when(context.shopRepository.findById(3L)).thenReturn(Optional.of(shop));
        when(context.shopOrderRepository.findByIdAndShop_Id(7L, 3L)).thenReturn(Optional.of(toCancel));

        context.service.updateOrderStatus(9L, 3L, 7L, "CANCELLED");

        assertEquals("CANCELLED", order.getStatus());
    }

    @Test
    void doesNotCancelShopOrderAfterPaymentHasStarted() {
        TestContext context = new TestContext();
        Order order = new Order();
        order.setPayment(new Payment());
        Shop shop = new Shop();
        shop.setId(3L);
        ShopOrder toCancel = shopOrder(order, shop, "PENDING");
        order.getShopOrders().add(toCancel);
        when(context.shopRepository.findById(3L)).thenReturn(Optional.of(shop));
        when(context.shopOrderRepository.findByIdAndShop_Id(7L, 3L)).thenReturn(Optional.of(toCancel));

        assertThrows(IllegalArgumentException.class,
                () -> context.service.updateOrderStatus(9L, 3L, 7L, "CANCELLED"));

        assertEquals("PENDING", toCancel.getStatus());
    }

    @Test
    void failedPaymentDoesNotMarkOrderPaidOrAllowUntrackedCancellation() {
        TestContext context = new TestContext();
        Order order = new Order();
        Payment payment = new Payment();
        payment.setStatus("FAILED");
        order.setPayment(payment);
        Shop shop = new Shop();
        shop.setId(3L);
        ShopOrder toCancel = shopOrder(order, shop, "PENDING");
        order.getShopOrders().add(toCancel);
        when(context.shopRepository.findById(3L)).thenReturn(Optional.of(shop));
        when(context.shopOrderRepository.findByIdAndShop_Id(7L, 3L)).thenReturn(Optional.of(toCancel));

        assertThrows(IllegalArgumentException.class,
                () -> context.service.updateOrderStatus(9L, 3L, 7L, "CANCELLED"));

        assertEquals("FAILED", order.getPayment().getStatus());
        assertEquals("PENDING", order.getStatus());
        assertEquals("PENDING", toCancel.getStatus());
    }

    @Test
    void completedShopOrderCannotBeCancelled() {
        TestContext context = new TestContext();
        Order order = new Order();
        Shop shop = new Shop();
        shop.setId(3L);
        ShopOrder delivered = shopOrder(order, shop, "DELIVERED");
        order.getShopOrders().add(delivered);
        when(context.shopRepository.findById(3L)).thenReturn(Optional.of(shop));
        when(context.shopOrderRepository.findByIdAndShop_Id(7L, 3L)).thenReturn(Optional.of(delivered));

        assertThrows(IllegalArgumentException.class,
                () -> context.service.updateOrderStatus(9L, 3L, 7L, "CANCELLED"));

        assertEquals("DELIVERED", delivered.getStatus());
        verifyNoInteractions(context.productRepository);
    }

    @Test
    void doesNotAdvanceShopOrderWhenParentIsAlreadyCancelled() {
        TestContext context = new TestContext();
        Order order = new Order();
        order.setStatus("CANCELLED");
        Shop shop = new Shop();
        shop.setId(3L);
        ShopOrder pending = shopOrder(order, shop, "PENDING");
        order.getShopOrders().add(pending);
        when(context.shopRepository.findById(3L)).thenReturn(Optional.of(shop));
        when(context.shopOrderRepository.findByIdAndShop_Id(7L, 3L)).thenReturn(Optional.of(pending));

        assertThrows(IllegalStateException.class,
                () -> context.service.updateOrderStatus(9L, 3L, 7L, "CONFIRMED"));

        assertEquals("PENDING", pending.getStatus());
    }

    private ShopOrder shopOrder(Order order, Shop shop, String status) {
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setOrder(order);
        shopOrder.setShop(shop);
        shopOrder.setStatus(status);
        return shopOrder;
    }

    private static final class TestContext {
        private final ShopOrderRepository shopOrderRepository = mock(ShopOrderRepository.class);
        private final ShopRepository shopRepository = mock(ShopRepository.class);
        private final ShopStaffRepository shopStaffRepository = mock(ShopStaffRepository.class);
        private final ProductRepository productRepository = mock(ProductRepository.class);
        private final OrderRepository orderRepository = mock(OrderRepository.class);
        private final ShopAuthorizationService shopAuthorizationService = mock(ShopAuthorizationService.class);
        private final ShopOrderServiceImpl service = new ShopOrderServiceImpl(
                shopOrderRepository, shopRepository, shopStaffRepository, productRepository,
                orderRepository, shopAuthorizationService);
    }
}
