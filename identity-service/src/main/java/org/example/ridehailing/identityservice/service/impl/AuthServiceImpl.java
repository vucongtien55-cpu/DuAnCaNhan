package org.example.ridehailing.identityservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.ridehailing.identityservice.config.JwtProvider;
import org.example.ridehailing.identityservice.dto.request.LoginRequest;
import org.example.ridehailing.identityservice.dto.request.RegisterRequest;
import org.example.ridehailing.identityservice.dto.response.AuthResponse;
import org.example.ridehailing.identityservice.entity.User;
import org.example.ridehailing.identityservice.repository.UserRepository;
import org.example.ridehailing.identityservice.service.AuthService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(request.getRole() != null ? request.getRole() : User.Role.CUSTOMER)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtProvider.generateToken(savedUser.getUsername(), savedUser.getRole().name(), savedUser.getId());

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .role(savedUser.getRole().name())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tài khoản hoặc mật khẩu không đúng!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Tài khoản hoặc mật khẩu không đúng!");
        }

        String token = jwtProvider.generateToken(user.getUsername(), user.getRole().name(), user.getId());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}