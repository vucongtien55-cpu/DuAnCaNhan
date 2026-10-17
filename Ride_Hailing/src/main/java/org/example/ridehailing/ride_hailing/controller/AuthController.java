package org.example.ridehailing.ride_hailing.controller;

import org.example.ridehailing.ride_hailing.dto.request.LoginRequest;
import org.example.ridehailing.ride_hailing.dto.request.RegisterRequest;
import org.example.ridehailing.ride_hailing.dto.response.UserResponse;
import org.example.ridehailing.ride_hailing.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        UserResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}