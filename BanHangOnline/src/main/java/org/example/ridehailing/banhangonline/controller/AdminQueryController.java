package org.example.ridehailing.banhangonline.controller;

import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.CategoryService;
import org.example.ridehailing.banhangonline.service.OrderService;
import org.example.ridehailing.banhangonline.service.PaymentService;
import org.example.ridehailing.banhangonline.service.ShopService;
import org.example.ridehailing.banhangonline.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Transactional(readOnly = true)
public class AdminQueryController {

    private final UserService userService;
    private final OrderService orderService;
    private final CategoryService categoryService;
    private final PaymentService paymentService;
    private final ShopService shopService;
    private final CurrentUserService currentUserService;

    public AdminQueryController(
            UserService userService,
            OrderService orderService,
            CategoryService categoryService,
            PaymentService paymentService,
            ShopService shopService,
            CurrentUserService currentUserService) {
        this.userService = userService;
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.paymentService = paymentService;
        this.shopService = shopService;
        this.currentUserService = currentUserService;
    }

    // Tìm tài khoản toàn hệ thống theo vai trò/trạng thái; service xác minh ADMIN.
    @GetMapping("/users")
    public List<ApiResponseDtos.AdminUserResponse> searchUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled) {
        return userService.searchUsers(currentUserService.requireCurrentUserId(), role, enabled).stream()
                .map(user -> new ApiResponseDtos.AdminUserResponse(
                        user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                        user.getPhone(), user.getEnabled(), user.getLocked(), user.getFailedLoginAttempts()))
                .toList();
    }

    @GetMapping("/users/locked")
    public List<ApiResponseDtos.AdminUserResponse> getLockedUsers() {
        return userService.getLockedUsers(currentUserService.requireCurrentUserId()).stream()
                .map(user -> new ApiResponseDtos.AdminUserResponse(
                        user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                        user.getPhone(), user.getEnabled(), user.getLocked(), user.getFailedLoginAttempts()))
                .toList();
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{userId}/unlock")
    public ApiResponseDtos.AdminUserResponse unlockUser(@PathVariable Long userId) {
        var user = userService.unlockAccount(currentUserService.requireCurrentUserId(), userId);
        return new ApiResponseDtos.AdminUserResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getPhone(), user.getEnabled(), user.getLocked(), user.getFailedLoginAttempts());
    }

    // Lọc đơn theo trạng thái thanh toán; không thay đổi trạng thái fulfillment.
    @GetMapping("/orders")
    public List<ApiResponseDtos.OrderResponse> getOrdersByPaymentStatus(@RequestParam String paymentStatus) {
        return ApiDtoMapper.map(orderService.getOrdersByPaymentStatus(
                currentUserService.requireCurrentUserId(), paymentStatus), ApiDtoMapper::toResponse);
    }

    // Liệt kê payment theo trạng thái; chỉ ADMIN được xem giao dịch toàn hệ thống.
    @GetMapping("/payments")
    public List<ApiResponseDtos.PaymentResponse> getPaymentsByStatus(@RequestParam String status) {
        return ApiDtoMapper.map(paymentService.getPaymentsByStatus(
                currentUserService.requireCurrentUserId(), status), ApiDtoMapper::toResponse);
    }

    // Tìm cửa hàng theo trạng thái để quản trị các shop đang hoạt động/ngừng hoạt động.
    @GetMapping("/shops")
    public List<ApiResponseDtos.ShopResponse> getShopsByStatus(@RequestParam String status) {
        return ApiDtoMapper.map(shopService.getShopsByStatus(
                currentUserService.requireCurrentUserId(), status), ApiDtoMapper::toResponse);
    }

    // Tra cứu đơn toàn hệ thống theo mã, chỉ dành cho quản trị viên.
    @GetMapping("/orders/by-number/{orderNumber}")
    public ApiResponseDtos.OrderResponse getOrderByNumber(@PathVariable String orderNumber) {
        return ApiDtoMapper.toResponse(orderService.getOrderByNumber(
                currentUserService.requireCurrentUserId(), orderNumber));
    }

    // Liệt kê danh mục gốc kể cả đã ngừng hoạt động.
    @GetMapping("/categories/roots")
    public List<ApiResponseDtos.CategoryResponse> getAllRootCategories() {
        return ApiDtoMapper.map(categoryService.getAllRootCategories(
                currentUserService.requireCurrentUserId()), ApiDtoMapper::toResponse);
    }

    // Liệt kê danh mục con kể cả đã ngừng hoạt động.
    @GetMapping("/categories/{categoryId}/children")
    public List<ApiResponseDtos.CategoryResponse> getAllCategoryChildren(@PathVariable Long categoryId) {
        return ApiDtoMapper.map(categoryService.getAllChildren(
                currentUserService.requireCurrentUserId(), categoryId), ApiDtoMapper::toResponse);
    }

    // Tìm cả danh mục đã ngừng hoạt động để hỗ trợ quản trị dữ liệu danh mục.
    @GetMapping("/categories/search")
    public List<ApiResponseDtos.CategoryResponse> searchAllCategories(@RequestParam String name) {
        return ApiDtoMapper.map(categoryService.searchAllCategories(
                currentUserService.requireCurrentUserId(), name), ApiDtoMapper::toResponse);
    }
}
