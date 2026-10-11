package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.CartItemService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@Transactional
public class CartController {

    private final CartItemService cartItemService;
    private final CurrentUserService currentUserService;

    public CartController(CartItemService cartItemService, CurrentUserService currentUserService) {
        this.cartItemService = cartItemService;
        this.currentUserService = currentUserService;
    }

    // Trả về giỏ hàng của principal hiện tại, không nhận userId từ client.
    @GetMapping
    public List<ApiResponseDtos.CartItemResponse> getMyCart() {
        return ApiDtoMapper.map(
                cartItemService.getMyCart(currentUserService.requireCurrentUserId()), ApiDtoMapper::toResponse);
    }

    // Thêm sản phẩm hoặc cộng số lượng nếu sản phẩm đã nằm trong giỏ.
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.CartItemResponse addItem(
            @Valid @RequestBody ApiRequestDtos.CartItemRequest request) {
        return ApiDtoMapper.toResponse(cartItemService.addOrUpdateItem(
                currentUserService.requireCurrentUserId(), request.productId(), request.quantity()));
    }

    // Cập nhật số lượng dòng hàng thuộc giỏ của principal hiện tại.
    @PutMapping("/items/{cartItemId}")
    public ApiResponseDtos.CartItemResponse updateQuantity(
            @PathVariable Long cartItemId, @Valid @RequestBody ApiRequestDtos.CartQuantityRequest request) {
        return ApiDtoMapper.toResponse(cartItemService.updateQuantity(
                currentUserService.requireCurrentUserId(), cartItemId, request.quantity()));
    }

    // Xóa một dòng sản phẩm khỏi giỏ hàng cá nhân.
    @DeleteMapping("/items/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable Long cartItemId) {
        cartItemService.removeItem(currentUserService.requireCurrentUserId(), cartItemId);
    }

    // Xóa toàn bộ giỏ của principal hiện tại.
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart() {
        cartItemService.clearCart(currentUserService.requireCurrentUserId());
    }
}
