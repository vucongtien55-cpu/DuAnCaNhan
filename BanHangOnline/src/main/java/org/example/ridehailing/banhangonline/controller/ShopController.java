package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.entity.Shop;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.ShopService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
@Transactional
public class ShopController {

    private final ShopService shopService;
    private final CurrentUserService currentUserService;

    public ShopController(ShopService shopService, CurrentUserService currentUserService) {
        this.shopService = shopService;
        this.currentUserService = currentUserService;
    }

    // Tra cứu cửa hàng theo mã.
    @GetMapping("/{shopId}")
    public ApiResponseDtos.ShopResponse getShop(@PathVariable Long shopId) {
        return ApiDtoMapper.toResponse(shopService.getActiveShopById(shopId));
    }

    // Tìm cửa hàng theo tên.
    @GetMapping
    public List<ApiResponseDtos.ShopResponse> searchShops(@RequestParam String name) {
        return ApiDtoMapper.map(shopService.searchActiveShops(name), ApiDtoMapper::toResponse);
    }

    @GetMapping("/mine")
    public List<ApiResponseDtos.ShopResponse> getMyShops() {
        return ApiDtoMapper.map(shopService.getShopsByOwner(
                currentUserService.requireCurrentUserId()), ApiDtoMapper::toResponse);
    }

    @GetMapping("/assigned-to-me")
    public List<ApiResponseDtos.ShopResponse> getShopsAssignedToMe() {
        return ApiDtoMapper.map(shopService.getShopsForEmployee(
                currentUserService.requireCurrentUserId()), ApiDtoMapper::toResponse);
    }

    // Tạo cửa hàng cho principal hiện tại; ownerId không được lấy từ request.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.ShopResponse createShop(
            @Valid @RequestBody ApiRequestDtos.ShopRequest request) {
        return ApiDtoMapper.toResponse(shopService.createShop(
                currentUserService.requireCurrentUserId(), toShop(request)));
    }

    // Cập nhật cửa hàng; service xác nhận người gọi là chủ sở hữu.
    @PutMapping("/{shopId}")
    public ApiResponseDtos.ShopResponse updateShop(
            @PathVariable Long shopId, @Valid @RequestBody ApiRequestDtos.ShopRequest request) {
        return ApiDtoMapper.toResponse(shopService.updateShop(
                currentUserService.requireCurrentUserId(), shopId, toShop(request)));
    }

    // Ngừng hoạt động cửa hàng; service xác nhận người gọi là chủ sở hữu.
    @DeleteMapping("/{shopId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateShop(@PathVariable Long shopId) {
        shopService.deactivateShop(currentUserService.requireCurrentUserId(), shopId);
    }

    private Shop toShop(ApiRequestDtos.ShopRequest request) {
        Shop shop = new Shop();
        shop.setName(request.name());
        shop.setDescription(request.description());
        shop.setAddress(request.address());
        shop.setPhone(request.phone());
        shop.setEmail(request.email());
        shop.setLogoUrl(request.logoUrl());
        return shop;
    }
}
