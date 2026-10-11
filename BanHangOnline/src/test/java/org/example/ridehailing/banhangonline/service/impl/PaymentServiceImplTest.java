package org.example.ridehailing.banhangonline.service.impl;

import org.example.ridehailing.banhangonline.entity.Order;
import org.example.ridehailing.banhangonline.entity.ShopOrder;
import org.example.ridehailing.banhangonline.entity.User;
import org.example.ridehailing.banhangonline.repository.OrderRepository;
import org.example.ridehailing.banhangonline.repository.PaymentRepository;
import org.example.ridehailing.banhangonline.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest {

    @Test
    void doesNotStartPaymentForPartiallyCancelledOrder() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PaymentServiceImpl service = new PaymentServiceImpl(
                paymentRepository, orderRepository, userRepository);

        User customer = new User();
        customer.setEnabled(true);
        Order order = new Order();
        order.setStatus("PARTIALLY_CANCELLED");
        order.setShopOrders(List.of(shopOrder("CANCELLED"), shopOrder("PENDING")));
        when(userRepository.findById(8L)).thenReturn(Optional.of(customer));
        when(orderRepository.findByIdAndCustomer_Id(12L, 8L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class,
                () -> service.createPendingPayment(8L, 12L, "CARD"));

        verifyNoInteractions(paymentRepository);
    }

    private ShopOrder shopOrder(String status) {
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setStatus(status);
        return shopOrder;
    }
}
