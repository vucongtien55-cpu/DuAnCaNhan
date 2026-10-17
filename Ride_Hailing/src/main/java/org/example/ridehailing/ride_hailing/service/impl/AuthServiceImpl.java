package org.example.ridehailing.ride_hailing.service.impl;

import org.example.ridehailing.ride_hailing.dto.request.LoginRequest;
import org.example.ridehailing.ride_hailing.dto.request.RegisterRequest;
import org.example.ridehailing.ride_hailing.dto.response.UserResponse;
import org.example.ridehailing.ride_hailing.entity.DriverStatus;
import org.example.ridehailing.ride_hailing.entity.User;
import org.example.ridehailing.ride_hailing.entity.UserRole;
import org.example.ridehailing.ride_hailing.entity.Wallet;
import org.example.ridehailing.ride_hailing.repository.UserRepository;
import org.example.ridehailing.ride_hailing.repository.WalletRepository;
import org.example.ridehailing.ride_hailing.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Số điện thoại này đã được đăng ký!");
        }

        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(request.getRole())
                .driverStatus(request.getRole() == UserRole.DRIVER ? DriverStatus.OFFLINE : null)
                .build();

        User savedUser = userRepository.save(user);

        if (savedUser.getRole() == UserRole.DRIVER) {
            Wallet wallet = Wallet.builder()
                    .driverId(savedUser.getId())
                    .balance(BigDecimal.ZERO)
                    .build();
            walletRepository.save(wallet);
        }

        return mapToUserResponse(savedUser);
    }

    @Override
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("Số điện thoại hoặc mật khẩu không chính xác!"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Số điện thoại hoặc mật khẩu không chính xác!");
        }

        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .driverStatus(user.getDriverStatus())
                .currentLat(user.getCurrentLat())
                .currentLng(user.getCurrentLng())
                .createdAt(user.getCreatedAt())
                .build();
    }
}