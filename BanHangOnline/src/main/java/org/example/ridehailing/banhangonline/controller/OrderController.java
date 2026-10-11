package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Transactional
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public OrderController(OrderService orderService, CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    // Checkout toàn bộ giỏ của principal hiện tại.
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.OrderResponse checkout(
            @Valid @RequestBody ApiRequestDtos.CheckoutRequest request) {
        return ApiDtoMapper.toResponse(orderService.checkout(
                currentUserService.requireCurrentUserId(),
                request.shippingAddress(), request.recipientPhone(), request.customerNote()));
    }

    // Lấy lịch sử đơn hàng của principal hiện tại.
    @GetMapping
    public List<ApiResponseDtos.OrderResponse> getMyOrders(@RequestParam(required = false) String status) {
        return ApiDtoMapper.map(
                orderService.getCustomerOrders(currentUserService.requireCurrentUserId(), status),
                ApiDtoMapper::toResponse);
    }

    // Tìm theo mã nhưng vẫn giới hạn kết quả trong đơn của principal hiện tại.
    @GetMapping("/by-number/{orderNumber}")
    public ApiResponseDtos.OrderResponse getMyOrderByNumber(@PathVariable String orderNumber) {
        return ApiDtoMapper.toResponse(orderService.getCustomerOrderByNumber(
                currentUserService.requireCurrentUserId(), orderNumber));
    }

    // Truy vấn chỉ trả về đơn thuộc principal hiện tại.
    @GetMapping("/{orderId}")
    public ApiResponseDtos.OrderResponse getMyOrder(@PathVariable Long orderId) {
        return ApiDtoMapper.toResponse(orderService.getCustomerOrder(
                currentUserService.requireCurrentUserId(), orderId));
    }

    // Hủy đơn đang chờ của principal hiện tại.
    @PostMapping("/{orderId}/cancel")
    public ApiResponseDtos.OrderResponse cancelOrder(@PathVariable Long orderId) {
        return ApiDtoMapper.toResponse(orderService.cancelPendingOrder(
                currentUserService.requireCurrentUserId(), orderId));
    }
}
