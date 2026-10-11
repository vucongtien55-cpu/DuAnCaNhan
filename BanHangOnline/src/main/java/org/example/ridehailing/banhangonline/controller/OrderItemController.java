package org.example.ridehailing.banhangonline.controller;

import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.OrderItemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/shops/{shopId}")
@Transactional
public class OrderItemController {

    private final OrderItemService orderItemService;
    private final CurrentUserService currentUserService;

    public OrderItemController(OrderItemService orderItemService, CurrentUserService currentUserService) {
        this.orderItemService = orderItemService;
        this.currentUserService = currentUserService;
    }

    // Lấy các mặt hàng trong đơn thuộc shop được principal quản lý.
    @GetMapping("/orders/{shopOrderId}/items")
    public List<ApiResponseDtos.OrderItemResponse> getOrderItems(
            @PathVariable Long shopId, @PathVariable Long shopOrderId) {
        return ApiDtoMapper.map(orderItemService.getShopOrderItems(
                currentUserService.requireCurrentUserId(), shopId, shopOrderId), ApiDtoMapper::toResponse);
    }

    // Xem lịch sử bán sản phẩm trong đúng phạm vi shop.
    @GetMapping("/products/{productId}/sales")
    public List<ApiResponseDtos.OrderItemResponse> getProductSales(
            @PathVariable Long shopId, @PathVariable Long productId) {
        return ApiDtoMapper.map(orderItemService.getProductSalesHistory(
                currentUserService.requireCurrentUserId(), shopId, productId), ApiDtoMapper::toResponse);
    }
}
