package com.novamart.service.impl;

import com.novamart.dto.request.LoginRequest;
import com.novamart.dto.request.RegisterRequest;
import com.novamart.dto.response.AuthResponse;
import com.novamart.entity.Role;
import com.novamart.entity.RoleName;
import com.novamart.entity.User;
import com.novamart.exception.BadRequestException;
import com.novamart.repository.RoleRepository;
import com.novamart.repository.UserRepository;
import com.novamart.security.JwtTokenProvider;
import com.novamart.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        // Chuẩn hóa email về dạng chữ thường và loại bỏ khoảng trắng thừa
        String emailClean = request.getEmail().toLowerCase().trim();

        // Kiểm tra xem email đã được đăng ký trước đó chưa
        if (userRepository.existsByEmail(emailClean)) {
            throw new BadRequestException("Email này đã được sử dụng trên hệ thống!");
        }

        // Tìm vai trò mặc định Khách hàng (ROLE_CUSTOMER), nếu chưa có trong DB thì tự khởi tạo
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name(RoleName.ROLE_CUSTOMER)
                        .displayName("Khách hàng")
                        .description("Tài khoản khách mua hàng")
                        .build()));

        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);

        // Mã hóa mật khẩu bằng BCrypt trước khi lưu vào database
        User user = User.builder()
                .email(emailClean)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .status("ACTIVE")
                .roles(roles)
                .build();

        userRepository.save(user);
        return "Đăng ký tài khoản thành công!";
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String emailClean = request.getEmail().toLowerCase().trim();

        // Thực hiện xác thực người dùng qua Spring Security AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(emailClean, request.getPassword())
        );

        // Tạo JWT Access Token dựa trên email
        String token = tokenProvider.generateToken(authentication.getName());

        User user = userRepository.findByEmail(emailClean)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin tài khoản"));

        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roles)
                .build();
    }
}