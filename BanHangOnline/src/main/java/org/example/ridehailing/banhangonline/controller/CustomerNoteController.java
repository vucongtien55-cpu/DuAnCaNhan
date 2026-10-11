package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.CustomerNoteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/shops/{shopId}")
@Transactional
public class CustomerNoteController {

    private final CustomerNoteService customerNoteService;
    private final CurrentUserService currentUserService;

    public CustomerNoteController(
            CustomerNoteService customerNoteService, CurrentUserService currentUserService) {
        this.customerNoteService = customerNoteService;
        this.currentUserService = currentUserService;
    }

    // Lấy lịch sử chăm sóc khách hàng trong phạm vi shop được phép truy cập.
    @GetMapping("/customers/{customerId}/notes")
    public List<ApiResponseDtos.CustomerNoteResponse> getCustomerHistory(
            @PathVariable Long shopId, @PathVariable Long customerId) {
        return ApiDtoMapper.map(customerNoteService.getCustomerHistory(
                currentUserService.requireCurrentUserId(), shopId, customerId), ApiDtoMapper::toResponse);
    }

    // Chỉ liệt kê ghi chú do principal hiện tại tạo trong shop hiện tại.
    @GetMapping("/customer-notes/created-by-me")
    public List<ApiResponseDtos.CustomerNoteResponse> getMyCreatedNotes(@PathVariable Long shopId) {
        return ApiDtoMapper.map(customerNoteService.getMyCreatedNotes(
                currentUserService.requireCurrentUserId(), shopId), ApiDtoMapper::toResponse);
    }

    // Chỉ liệt kê ghi chú được giao cho principal hiện tại trong shop hiện tại.
    @GetMapping("/customer-notes/assigned-to-me")
    public List<ApiResponseDtos.CustomerNoteResponse> getMyAssignedNotes(@PathVariable Long shopId) {
        return ApiDtoMapper.map(customerNoteService.getMyAssignedNotes(
                currentUserService.requireCurrentUserId(), shopId), ApiDtoMapper::toResponse);
    }

    // Tra cứu lịch hẹn trong khoảng thời gian ISO-8601.
    @GetMapping("/appointments")
    public List<ApiResponseDtos.CustomerNoteResponse> getAppointments(
            @PathVariable Long shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiDtoMapper.map(customerNoteService.getAppointments(
                currentUserService.requireCurrentUserId(), shopId, from, to), ApiDtoMapper::toResponse);
    }

    // Ghi nhận hoạt động chăm sóc và đặt lịch hẹn nếu được yêu cầu.
    @PostMapping("/customer-notes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.CustomerNoteResponse createNote(
            @PathVariable Long shopId, @Valid @RequestBody ApiRequestDtos.CustomerNoteRequest request) {
        return ApiDtoMapper.toResponse(customerNoteService.createNote(
                currentUserService.requireCurrentUserId(), shopId, request.customerId(),
                request.note(), request.appointmentAt()));
    }
}
