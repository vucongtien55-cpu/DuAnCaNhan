package org.example.ridehailing.banhangonline.controller;

import jakarta.validation.Valid;
import org.example.ridehailing.banhangonline.dto.ApiDtoMapper;
import org.example.ridehailing.banhangonline.dto.ApiRequestDtos;
import org.example.ridehailing.banhangonline.dto.ApiResponseDtos;
import org.example.ridehailing.banhangonline.security.CurrentUserService;
import org.example.ridehailing.banhangonline.service.UserService;
import org.example.ridehailing.banhangonline.service.PasswordRecoveryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/users")
@Transactional
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final PasswordRecoveryService passwordRecoveryService;

    public UserController(
            UserService userService,
            CurrentUserService currentUserService,
            PasswordRecoveryService passwordRecoveryService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.passwordRecoveryService = passwordRecoveryService;
    }

    // Đăng ký khách hàng; vai trò luôn do service gán, không nhận từ payload.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDtos.UserResponse register(@Valid @RequestBody ApiRequestDtos.RegisterRequest request) {
        return ApiDtoMapper.toResponse(userService.registerCustomer(
                request.fullName(), request.email(), request.password(), request.phone()));
    }

    // Trả hồ sơ của principal hiện tại, không nhận userId từ client.
    @GetMapping("/me")
    public ApiResponseDtos.UserResponse getMyProfile() {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiDtoMapper.toResponse(userService.getUser(userId, userId));
    }

    // Cập nhật hồ sơ của principal hiện tại.
    @PutMapping("/me")
    public ApiResponseDtos.UserResponse updateMyProfile(
            @Valid @RequestBody ApiRequestDtos.ProfileRequest request) {
        return ApiDtoMapper.toResponse(userService.updateProfile(
                currentUserService.requireCurrentUserId(),
                request.fullName(), request.phone(), request.avatarUrl()));
    }

    @PutMapping("/me/password")
    public ApiResponseDtos.MessageResponse changeMyPassword(
            @Valid @RequestBody ApiRequestDtos.PasswordChangeRequest request) {
        passwordRecoveryService.changePassword(
                currentUserService.requireCurrentUserId(), request.currentPassword(), request.newPassword());
        return new ApiResponseDtos.MessageResponse("Đã thay đổi mật khẩu.");
    }
}
