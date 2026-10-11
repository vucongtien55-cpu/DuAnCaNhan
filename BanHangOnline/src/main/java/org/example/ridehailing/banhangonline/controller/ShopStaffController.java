package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.ShopStaffService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/shops/{shopId}/staff")
@Transactional
public class ShopStaffController {

    private final ShopStaffService shopStaffService;
    private final CurrentUserService currentUserService;

    public ShopStaffController(ShopStaffService shopStaffService, CurrentUserService currentUserService) {
        this.shopStaffService = shopStaffService;
        this.currentUserService = currentUserService;
    }

    // Chủ shop hoặc nhân viên hiện tại lấy danh sách nhân viên đang hoạt động.
    @GetMapping
    public List<ApiResponseDtos.ShopStaffResponse> getStaff(
            @PathVariable Long shopId, @RequestParam(required = false) Boolean active) {
        return ApiDtoMapper.map(
                shopStaffService.getStaff(currentUserService.requireCurrentUserId(), shopId, active),
                ApiDtoMapper::toResponse);
    }

    // Chỉ chủ shop mới được thêm nhân viên; userId là người được thêm.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.ShopStaffResponse addStaff(
            @PathVariable Long shopId, @Valid @RequestBody ApiRequestDtos.StaffRequest request) {
        return ApiDtoMapper.toResponse(shopStaffService.addStaff(
                currentUserService.requireCurrentUserId(), shopId, request.userId()));
    }

    // Chỉ chủ shop mới được ngừng tư cách nhân viên.
    @DeleteMapping("/{staffId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateStaff(@PathVariable Long shopId, @PathVariable Long staffId) {
        shopStaffService.deactivateStaff(currentUserService.requireCurrentUserId(), shopId, staffId);
    }
}
