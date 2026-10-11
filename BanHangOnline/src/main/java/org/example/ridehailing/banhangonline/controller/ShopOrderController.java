package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.ShopOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/shops/{shopId}/orders")
@Transactional
public class ShopOrderController {

    private final ShopOrderService shopOrderService;
    private final CurrentUserService currentUserService;

    public ShopOrderController(ShopOrderService shopOrderService, CurrentUserService currentUserService) {
        this.shopOrderService = shopOrderService;
        this.currentUserService = currentUserService;
    }

    // Lấy đơn hàng của shop do principal hiện tại quản lý.
    @GetMapping
    public List<ApiResponseDtos.ShopOrderResponse> getOrders(
            @PathVariable Long shopId, @RequestParam(required = false) String status) {
        List<org.example.ridehailing.banhangonline.entity.ShopOrder> orders = status == null
                ? shopOrderService.getShopOrders(currentUserService.requireCurrentUserId(), shopId)
                : shopOrderService.getShopOrdersByStatus(
                        currentUserService.requireCurrentUserId(), shopId, status);
        return ApiDtoMapper.map(orders, ApiDtoMapper::toResponse);
    }

    // Danh sách đơn đã giao riêng cho nhân viên hiện tại.
    @GetMapping("/assigned-to-me")
    public List<ApiResponseDtos.ShopOrderResponse> getMyAssignedOrders(
            @PathVariable Long shopId, @RequestParam(required = false) String status) {
        return ApiDtoMapper.map(shopOrderService.getMyAssignedOrders(
                currentUserService.requireCurrentUserId(), shopId, status), ApiDtoMapper::toResponse);
    }

    // Cập nhật trạng thái sau khi service kiểm tra quy trình chuyển trạng thái.
    @PatchMapping("/{shopOrderId}/status")
    public ApiResponseDtos.ShopOrderResponse updateStatus(
            @PathVariable Long shopId,
            @PathVariable Long shopOrderId,
            @Valid @RequestBody ApiRequestDtos.ShopOrderStatusRequest request) {
        return ApiDtoMapper.toResponse(shopOrderService.updateOrderStatus(
                currentUserService.requireCurrentUserId(), shopId, shopOrderId, request.status()));
    }

    // Giao đơn cho nhân viên đang hoạt động thuộc đúng shop.
    @PutMapping("/{shopOrderId}/staff")
    public ApiResponseDtos.ShopOrderResponse assignStaff(
            @PathVariable Long shopId,
            @PathVariable Long shopOrderId,
            @Valid @RequestBody ApiRequestDtos.AssignStaffRequest request) {
        return ApiDtoMapper.toResponse(shopOrderService.assignOrder(
                currentUserService.requireCurrentUserId(), shopId, shopOrderId, request.staffId()));
    }
}
