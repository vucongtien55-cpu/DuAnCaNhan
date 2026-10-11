package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.ShopCustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/shops/{shopId}/customers")
@Transactional
public class ShopCustomerController {

    private final ShopCustomerService shopCustomerService;
    private final CurrentUserService currentUserService;

    public ShopCustomerController(
            ShopCustomerService shopCustomerService, CurrentUserService currentUserService) {
        this.shopCustomerService = shopCustomerService;
        this.currentUserService = currentUserService;
    }

    // Chỉ thành viên của shop được xem danh sách khách của shop đó.
    @GetMapping
    public List<ApiResponseDtos.ShopCustomerResponse> getCustomers(
            @PathVariable Long shopId, @RequestParam(required = false) String status) {
        return ApiDtoMapper.map(
                shopCustomerService.getCustomers(currentUserService.requireCurrentUserId(), shopId, status),
                ApiDtoMapper::toResponse);
    }

    // Thêm khách hàng; quyền thành viên shop được kiểm tra trong service.
    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.ShopCustomerResponse addCustomer(
            @PathVariable Long shopId, @Valid @RequestBody ApiRequestDtos.ShopCustomerRequest request) {
        return ApiDtoMapper.toResponse(shopCustomerService.addCustomer(
                currentUserService.requireCurrentUserId(), shopId, request.customerId()));
    }

    // Cập nhật trạng thái liên kết khách hàng-shop.
    @PutMapping("/{customerId}")
    public ApiResponseDtos.ShopCustomerResponse updateStatus(
            @PathVariable Long shopId,
            @PathVariable Long customerId,
            @Valid @RequestBody ApiRequestDtos.ShopCustomerStatusRequest request) {
        return ApiDtoMapper.toResponse(shopCustomerService.updateCustomerStatus(
                currentUserService.requireCurrentUserId(), shopId, customerId, request.status()));
    }
}
