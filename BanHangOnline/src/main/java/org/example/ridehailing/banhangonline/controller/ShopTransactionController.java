package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.ShopTransactionService;
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
@RequestMapping("/api/shops/{shopId}/transactions")
@Transactional
public class ShopTransactionController {

    private final ShopTransactionService transactionService;
    private final CurrentUserService currentUserService;

    public ShopTransactionController(
            ShopTransactionService transactionService, CurrentUserService currentUserService) {
        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
    }

    // Lấy các giao dịch trong shop do principal hiện tại quản lý.
    @GetMapping
    public List<ApiResponseDtos.ShopTransactionResponse> getTransactions(
            @PathVariable Long shopId, @RequestParam(required = false) String type) {
        return ApiDtoMapper.map(transactionService.getShopTransactions(
                currentUserService.requireCurrentUserId(), shopId, type), ApiDtoMapper::toResponse);
    }

    // Lấy giao dịch của một đơn shop sau khi kiểm tra phạm vi shop.
    @GetMapping("/orders/{shopOrderId}")
    public List<ApiResponseDtos.ShopTransactionResponse> getOrderTransactions(
            @PathVariable Long shopId, @PathVariable Long shopOrderId) {
        return ApiDtoMapper.map(transactionService.getShopOrderTransactions(
                currentUserService.requireCurrentUserId(), shopId, shopOrderId), ApiDtoMapper::toResponse);
    }

    // Chủ sở hữu shop mới được ghi nhận giao dịch chờ.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.ShopTransactionResponse createTransaction(
            @PathVariable Long shopId, @Valid @RequestBody ApiRequestDtos.ShopTransactionRequest request) {
        return ApiDtoMapper.toResponse(transactionService.createPendingTransaction(
                currentUserService.requireCurrentUserId(), shopId, request.shopOrderId(),
                request.type(), request.amount(), request.transactionReference()));
    }
}
