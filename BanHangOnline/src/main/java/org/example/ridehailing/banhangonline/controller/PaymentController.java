package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/orders/{orderId}/payment")
@Transactional
public class PaymentController {

    private final PaymentService paymentService;
    private final CurrentUserService currentUserService;

    public PaymentController(PaymentService paymentService, CurrentUserService currentUserService) {
        this.paymentService = paymentService;
        this.currentUserService = currentUserService;
    }

    // Tạo yêu cầu thanh toán cho đơn thuộc principal hiện tại.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.PaymentResponse createPayment(
            @PathVariable Long orderId, @Valid @RequestBody ApiRequestDtos.PaymentRequest request) {
        return ApiDtoMapper.toResponse(paymentService.createPendingPayment(
                currentUserService.requireCurrentUserId(), orderId, request.method()));
    }

    // Xem thanh toán nếu đơn thuộc principal hiện tại.
    @GetMapping
    public ApiResponseDtos.PaymentResponse getPayment(@PathVariable Long orderId) {
        return ApiDtoMapper.toResponse(paymentService.getCustomerPayment(
                currentUserService.requireCurrentUserId(), orderId));
    }
}
